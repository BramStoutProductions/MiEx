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

import wiki
import re
import json

def parseTable(tableStr : str) -> list[list[str]]:
    tableTokens = tableStr.split("\n|")
    
    res = []
    i = -1
    length = len(tableTokens)
    currentItem = None
    while i < (length-1):
        i += 1

        if tableTokens[i] == "-" or tableTokens[i] == "}":
            if currentItem is not None:
                res.append(currentItem)
            currentItem = []
            continue

        if currentItem is None:
            continue

        currentItem.append(tableTokens[i].replace("\n", "").strip())
    
    return res

class Biome:

    def __init__(self, resource : str, id : int, label : str) -> None:
        self.resource : str = resource
        self.id : str = id
        self.label : str = label

def parseJavaTable(table : list[list[str]]) -> list[Biome]:
    res : list[Biome] = []
    for item in table:
        name = item[1].replace("<code>", "").replace("</code>", "")
        id = int(item[2])
        labelSearch = re.search("\\[\\[([^\\]]+)\\]\\]", item[0])
        label = labelSearch.group(1)
        res.append(Biome(name, id, label))
    return res

def parseBedrockTable(table : list[list[str]]) -> list[Biome]:
    res : list[Biome] = []
    for item in table:
        name = item[1].replace("<code>", "").replace("</code>", "")
        id = int(item[2])
        labelSearch = re.search("\\[\\[([^\\]]+)\\]\\]", item[0])
        label = labelSearch.group(1)
        res.append(Biome(name, id, label))
    return res

def getNameTokens(string : str) -> list[str]:
    return string.lower().replace(" ", "_").split("_")

def getSimilarity(tokensA : list[str], tokensB : list[str]) -> float:
    res : float = 0.0

    for tokenB in tokensB:
        match = False
        for tokenA in tokensA:
            if tokenA == tokenB:
                res += 1.0
                match = True
        if match == False:
            for tokenA in tokensA:
                if tokenA in tokenB or tokenB in tokenA:
                    res += 0.1
                    match = True
                    break
        if match == False:
            # If there is no match, reduce the similarity by
            # a very small amount.
            # Let's say you have 'broken anvil' and 'anvil'
            # and you want to match it with 'anvil'.
            # Without this small subtraction, both will have
            # the same similarity.
            res -= 0.00001

    return res

def run():
    article = wiki.getArticle("Biome/ID")
    tables = re.findall("{\\|(?:[^}]|}})+}", article)
    
    javaTableStr = tables[0]
    bedrockTableStr = tables[1]

    javaTableTokens = parseTable(javaTableStr)
    bedrockTableTokens = parseTable(bedrockTableStr)

    javaBiomes = parseJavaTable(javaTableTokens)
    bedrockBiomes = parseBedrockTable(bedrockTableTokens)

    bedrockBiomeRegistry : list[dict] = []
    for biome in bedrockBiomes:
        bedrockBiomeRegistry.append({
            "id": biome.id,
            "name": biome.resource
        })
    
    with open("bedrock_biomes.json", "w", encoding='utf-8') as f:
        json.dump(bedrockBiomeRegistry, f, indent=4)
    
    bedrockBiomeMap : dict[str, str] = {}
    for biome in bedrockBiomes:
        bestJavaBiome = None
        bestSimilarity = 0.0
        bedrockNameTokens = getNameTokens(biome.resource + "_" + biome.label)
        for javaBiome in javaBiomes:
            javaNameTokens = getNameTokens(javaBiome.resource + "_" + javaBiome.label)
            similarity = getSimilarity(bedrockNameTokens, javaNameTokens)
            if similarity > bestSimilarity:
                bestJavaBiome = javaBiome
                bestSimilarity = similarity
        
        if bestJavaBiome is not None:
            bedrockBiomeMap[biome.resource] = bestJavaBiome.resource
        else:
            print("Could not find java biome for ", biome.resource)
    
    with open("miex_biomes.json", "w", encoding='utf-8') as f:
        json.dump(bedrockBiomeMap, f, indent=4)


run()