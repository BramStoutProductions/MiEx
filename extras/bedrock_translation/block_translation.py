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


# PrismarineJS has a repository that provides mappings from Bedrock Edition to Java Edition.
# https://github.com/PrismarineJS/minecraft-data
# These mappings come from the GreyserMC project/
# https://github.com/GeyserMC/mappings
#
# This script takes the PrismarineJS mapping files and re-exports them into
# the schema that MiEx can read.
import json
import os
import os.path

MINECRAFT_DATA_DIR = "C:/Users/me/OneDrive/Documenten/_SOFTWARE_/MCWorldExporter/minecraft-data"

MINECRAFT_DATA_BEDROCK_DIR = MINECRAFT_DATA_DIR + "/data/bedrock"

class BlockDef:

    def __init__(self, dataStr : str) -> None:
        self.name : str = ""
        self.state : dict[str, str] = {}
        self.defaultValues : set[str] = set()

        openBrackedIndex = dataStr.find("[")
        if openBrackedIndex < 0:
            openBrackedIndex = len(dataStr)
        self.name = dataStr[0:openBrackedIndex]
        if (len(dataStr) - openBrackedIndex) > 2:
            stateStr = dataStr[openBrackedIndex + 1 : len(dataStr) - 1]

            states = stateStr.split(",")
            for state in states:
                equalIndex = state.find("=")
                if equalIndex < 0:
                    continue
                stateName = state[0:equalIndex]
                stateValue = state[equalIndex + 1:len(state)]
                self.state[stateName] = stateValue
    
    def __eq__(self, value: object) -> bool:
        if not isinstance(value, BlockDef):
            return False
        if self.name != value.name:
            return False
        if len(self.state) != len(value.state):
            return False
        for key in self.state:
            if key not in value.state:
                return False
            if self.state[key] != value.state[key]:
                return False
        return True
    
    def __hash__(self) -> int:
        res = hash(self.name)
        for key, value in self.state.items():
            res = res * 31 + hash(key)
            res = res * 31 + hash(value)
        return res

def parseBlockB2J(data : dict[str, str], defaultStates : dict[str, dict[str, str]], outMapping : dict[str, dict[BlockDef, BlockDef]]):
    for key, value in data.items():
        bedrockDef : BlockDef = BlockDef(key)
        javaDef : BlockDef = BlockDef(value)

        # We want to keep track of which states are the default value.
        # These get put into the optional condition.
        if bedrockDef.name in defaultStates:
            defaultState = defaultStates[bedrockDef.name]
            for stateName, stateValue in defaultState.items():
                if stateName not in bedrockDef.state:
                    continue
                if bedrockDef.state[stateName] == stateValue:
                    bedrockDef.defaultValues.add(stateName)

        if bedrockDef.name not in outMapping:
            outMapping[bedrockDef.name] = {}
        outMapping[bedrockDef.name][bedrockDef] = javaDef

def parseBlocks(data : list[dict[str, int]], outStateIds : dict[str, int]):
    for blockData in data:
        if "name" not in blockData:
            continue
        if "defaultState" not in blockData:
            continue
        blockName = blockData["name"]
        blockState = blockData["defaultState"]
        if ":" not in blockName:
            blockName = "minecraft:" + blockName
        outStateIds[blockName] = blockState

def parseBlockStates(data : list[dict], stateIds : dict[str, int], outDefaultStates : dict[str, dict[str, str]]):
    dataLength = len(data)
    for blockName, stateId in stateIds.items():
        if stateId < 0 or stateId >= dataLength:
            continue
        blockState : dict = data[stateId]
        if "states" not in blockState:
            continue

        defaultState : dict[str, str] = {}

        blockState : dict = blockState["states"]
        for stateName, stateData in blockState.items():
            if "value" not in stateData:
                continue

            defaultState[stateName] = str(stateData["value"])
        
        if blockName not in outDefaultStates:
            outDefaultStates[blockName] = {}
        outDefaultStates[blockName].update(defaultState)

def run():
    if not os.path.exists(MINECRAFT_DATA_BEDROCK_DIR) or not os.path.isdir(MINECRAFT_DATA_BEDROCK_DIR):
        print("No valid minecraft-data/data/bedrock directory found")
        return
    
    defaultStates : dict[str, dict[str, str]] = {}
    for dirName in os.listdir(MINECRAFT_DATA_BEDROCK_DIR):
        dirPath = MINECRAFT_DATA_BEDROCK_DIR + "/" + dirName
        blocksPath = dirPath + "/blocks.json"
        if not os.path.exists(blocksPath):
            continue
        blockStatesPath = dirPath + "/blockStates.json"
        if not os.path.exists(blockStatesPath):
            continue

        defaultStateIds : dict[str, int] = {}
        with open(blocksPath, encoding="UTF-8") as fp:
            parseBlocks(json.load(fp), defaultStateIds)
        
        with open(blockStatesPath, encoding="UTF-8") as fp:
            parseBlockStates(json.load(fp), defaultStateIds, defaultStates)

    mapping : dict[str, dict[BlockDef, BlockDef]] = {}

    # First read in the minecraft-data mapping files.
    # We merge the mappings from all different versions into
    # one big mapping.
    for dirName in os.listdir(MINECRAFT_DATA_BEDROCK_DIR):
        dirPath = MINECRAFT_DATA_BEDROCK_DIR + "/" + dirName
        blocksB2JPath = dirPath + "/blocksB2J.json"
        if not os.path.exists(blocksB2JPath):
            continue

        with open(blocksB2JPath, encoding='UTF-8') as fp:
            parseBlockB2J(json.load(fp), defaultStates, mapping)
    
    # Go through mapping to remove mappings that don't change anything
    toRemove = []
    for key, value in mapping.items():
        toRemove2 = []
        for key2, value2 in value.items():
            if key2 == value2:
                # The block names and block states are the same,
                # so no need to store this mapping
                toRemove2.append(key2)
        for key2 in toRemove2:
            value.pop(key2)
        if len(value) == 0:
            # The list of mappings is empty, so we can remove it
            toRemove.append(key)
    for key in toRemove:
        mapping.pop(key)
    
    # Go through mapping and apply some patches to fix certain things
    for value in mapping.values():
        for bedrockDef, javaDef in value.items():
            if "stairs" in javaDef.name:
                # Stairs by default get the shape "outer_right", but should have the shape "straight"
                if "shape" in javaDef.state:
                    javaDef.state["shape"] = "straight"
            elif bedrockDef.name == "minecraft:trip_wire":
                # We need to get rid of "suspended_bit" from condition
                if "suspended_bit" in bedrockDef.state:
                    bedrockDef.state.pop("suspended_bit")
            elif bedrockDef.name == "minecraft:snow_layer":
                # Remove covered_bit from condition
                if "covered_bit" in bedrockDef.state:
                    bedrockDef.state.pop("covered_bit")
            elif bedrockDef.name == "minecraft:leaves":
                # Remove update_bit and persistent_bit
                if "update_bit" in bedrockDef.state:
                    bedrockDef.state.pop("update_bit")
                if "persistent_bit" in bedrockDef.state:
                    bedrockDef.state.pop("persistent_bit")
            elif bedrockDef.name == "minecraft:chest" or bedrockDef.name == "minecraft:trapped_chest":
                # Chests by default are set to the type "right", but should be "single"
                if "type" in javaDef.state:
                    javaDef.state["type"] = "single"

    # Now we need to write the mapping file out into a json file
    translations : dict = {}

    for key, value in mapping.items():
        mappings : list = []
        for bedrockDef, javaDef in value.items():
            map = {
                "name": javaDef.name
            }
            if len(bedrockDef.state) > 0:
                if len(bedrockDef.defaultValues) == 0:
                    map["condition"] = bedrockDef.state
                else:
                    # Some states are optional, so separate it out into an optional condition
                    conditionMap : dict[str, str] = {}
                    optionalConditionMap : dict[str, str] = {}
                    for stateName, stateValue in bedrockDef.state.items():
                        if stateName in bedrockDef.defaultValues:
                            optionalConditionMap[stateName] = stateValue
                        else:
                            conditionMap[stateName] = stateValue
                    map["condition"] = conditionMap
                    map["optionalCondition"] = optionalConditionMap
            if len(javaDef.state) > 0:
                map["constants"] = javaDef.state
            mappings.append(map)

        translations[key] = mappings

    outData = [
        {
            "translations": translations
        }
    ]
    with open("miex_blocks.json", "w", encoding='utf-8') as f:
        json.dump(outData, f, indent=4)

run()