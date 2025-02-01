# BSD 3-Clause License
# 
# Copyright (c) 2024, Bram Stout Productions
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# 3. Neither the name of the copyright holder nor the names of its
#    contributors may be used to endorse or promote products derived from
#    this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import urllib.request
import os
import os.path
import json
import math

def getPaths() -> dict[str, dict]:
    try:
        request = urllib.request.Request("https://api.faithfulpack.net/v2/paths/raw")
        request.add_header("User-Agent", "BSP_WikiParser/1.0 (https://bramstout.nl/en/MiEx; business@bramstout.nl) python/3.10")
        fp = urllib.request.urlopen(request)
        jsonData = json.load(fp)
        fp.close()
        return jsonData
    except Exception as e:
        print(e)
    return {}

def getPathsForVersion(paths : dict[str, dict], version : str) -> dict[int, str]:
    res : dict[int, str] = {}
    for path in paths.values():
        if "use" not in path:
            continue
        if "name" not in path:
            continue
        if "versions" not in path:
            continue
        
        matchesVersion = False
        for version2 in path["versions"]:
            if version in version2:
                matchesVersion = True
                break
        if not matchesVersion:
            continue
        
        use : int = int(''.join(filter(str.isdigit, path["use"])))
        res[use] = path["name"]
    return res

def javaPathToResourceId(path : str) -> str:
    # Example: assets/minecraft/textures/block/stone.png

    # Strip away the root folder e.g. "assets/"
    path = path.partition("/")[2]

    # Strip away namespace
    namespace, sep, path = path.partition("/")

    # Strip away type
    type, sep, path = path.partition("/")

    # Strip away extension
    path = path.rpartition(".")[0]

    return type + ";" + namespace + ":" + path

def bedrockPathToResourceId(path : str) -> str:
    # Example: textures/block/stone.png

    # Strip away type
    type, sep, path = path.partition("/")

    # Strip away extension
    path = path.rpartition(".")[0]

    return type + ";" + "minecraft:" + path

def generateMapping(javaPaths : dict[int, str], bedrockPaths : dict[int, str]) -> dict[str, str]:
    res : dict[str, str] = {}

    for use, bedrockPath in bedrockPaths.items():
        if use not in javaPaths:
            if not bedrockPath.startswith("textures/ui/") and not bedrockPath.startswith("textures/gui/"):
                print("No mapping found for path: " + bedrockPath)
            continue
        javaPath = javaPaths[use]
        javaResourceId = javaPathToResourceId(javaPath)
        
        bedrockResourceId = bedrockPathToResourceId(bedrockPath)

        if javaResourceId != bedrockResourceId:
            res[javaResourceId] = bedrockPath.rpartition(".")[0]


    return res

def run():
    paths = getPaths()

    javaPaths = getPathsForVersion(paths, "java")
    bedrockPaths = getPathsForVersion(paths, "bedrock")

    filePathMapping = generateMapping(javaPaths, bedrockPaths)

    with open("./miex_file_path_mapping.json", 'w', encoding='utf-8') as f:
        json.dump(filePathMapping, f, indent=4)

run()