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
import json
import xml.etree.ElementTree as ET
import urllib.parse

def getArticle(page : str) -> str:
    try:
        request = urllib.request.Request("https://minecraft.wiki/api.php?action=query&titles=" + page + "&export=true&format=json&redirects=true")
        request.add_header("User-Agent", "BSP_WikiParser/1.0 (https://bramstout.nl/en/MiEx; business@bramstout.nl) python/3.10")
        fp = urllib.request.urlopen(request)
        jsonData = json.load(fp)
        fp.close()
        articleStr : str = jsonData["query"]["export"]["*"]
        articleStr : str = articleStr.encode('utf-8')
        root = ET.fromstring(articleStr)
        for item in root.iter():
            if item.tag.endswith("text"):
                return item.text
    except Exception as e:
        print(e)
    return ""

def getArticles(pages : list[str]) -> list[tuple[str, str]]:
    try:
        page = ""
        for i in range(len(pages)):
            if i > 0:
                page = page + "|"
            page = page + urllib.parse.quote_plus(pages[i])
        request = urllib.request.Request("https://minecraft.wiki/api.php?action=query&titles=" + page + "&export=true&format=json&redirects=true")
        request.add_header("User-Agent", "BSP_WikiParser/1.0 (https://bramstout.nl/en/MiEx; business@bramstout.nl) python/3.10")
        fp = urllib.request.urlopen(request)
        jsonData = json.load(fp)
        fp.close()
        articleStr : str = jsonData["query"]["export"]["*"]
        articleStr : str = articleStr.encode('utf-8')
        root = ET.fromstring(articleStr)
        res : list[tuple[str, str]] = []
        for item in root.iter():
            if item.tag.endswith("page"):
                title = None
                text = None
                for pageItem in item.iter():
                    if pageItem.tag.endswith("title"):
                        title = pageItem.text
                    elif pageItem.tag.endswith("text"):
                        text = pageItem.text
                if title is None or text is None:
                    print("No title or text for", title, text)
                res.append((title, text))
        return res
    except Exception as e:
        print(e)
    return []