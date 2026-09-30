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


# PrismarineJS has a repository that provides data about Minecraft blocks.
# https://github.com/PrismarineJS/minecraft-data
#
# This script takes the PrismarineJS data and creates a file with the default
# block properties.
#
# In Minecraft Java Edition 26.3, block states don't need to specify all of
# the properties anymore, but instead can just be specified with just a name
# and the properties will be filled in with their default values.
# MiEx doesn't know these default values and so we need this file here.

import json
import os
import os.path
from typing import Any

MINECRAFT_DATA_DIR = "C:/Users/me/OneDrive/Documenten/_SOFTWARE_/MCWorldExporter/minecraft-data"

MINECRAFT_DATA_JAVA_DIR = MINECRAFT_DATA_DIR + "/data/pc"

def getPropertyPermutations(property : dict[str,Any]) -> list[tuple[str,str]]:
    name = property.get("name", "")
    type = property.get("type", "bool")
    if type == "bool":
        return [ (name, "true"), (name, "false") ]
    elif type == "enum":
        values : list[str] = property.get("values", [])
        res = []
        for value in values:
            res.append((name, value))
        return res
    elif type == "int":
        values : list[str] = property.get("values", [])
        if len(values) == 0:
            values = []
            for i in range(property.get("num_values", 0)):
                values.append(str(i))
        res = []
        for value in values:
            res.append((name, value))
        return res
    return []

def getBlockStatePermutations(properties: list[dict[str,Any]]) -> list[dict[str, str]]:
    permutations : list[list[tuple[str, str]]] = []
    numPermutations = 1
    for property in properties:
        perms = getPropertyPermutations(property)
        if len(perms) > 0:
            numPermutations *= len(perms)
            permutations.append(perms)

    blockPermutations = []
    for i in range(numPermutations):
        state : dict[str, Any] = {}
        permI = i
        for perm in reversed(permutations):
            permJ = permI % len(perm)
            prop = perm[permJ]
            state[prop[0]] = prop[1]
            permI = int(permI / len(perm))
        blockPermutations.append(state)
    return blockPermutations
    

def parseBlocks(data :list[dict[str,Any]], defaultStates: dict[str, dict[str, str]]):
    for block in data:
        id = block.get("name", "")
        minStateId = block.get("minStateId", 0)
        maxStateId = block.get("maxStateId", 0)
        defaultStateId = block.get("defaultState", 0)
        defaultStateId -= minStateId
        properties : list[dict[str,Any]] = block.get("states", [])
        if len(properties) == 0:
            continue
        permutations = getBlockStatePermutations(properties)
        defaultPermutation = permutations[defaultStateId]
        defaultStates[id] = defaultPermutation


def run():
    if not os.path.exists(MINECRAFT_DATA_JAVA_DIR) or not os.path.isdir(MINECRAFT_DATA_JAVA_DIR):
        print("No valid minecraft-data/data/pc directory found")
        return
    
    defaultStates : dict[str, dict[str, str]] = {}
    for dirName in os.listdir(MINECRAFT_DATA_JAVA_DIR):
        dirPath = MINECRAFT_DATA_JAVA_DIR + "/" + dirName
        blocksPath = dirPath + "/blocks.json"
        if not os.path.exists(blocksPath):
            continue

        with open(blocksPath, encoding="UTF-8") as fp:
            parseBlocks(json.load(fp), defaultStates)

    outData = defaultStates
    with open("miex_default_block_properties.json", "w", encoding='utf-8') as f:
        json.dump(outData, f, indent=4)

run()