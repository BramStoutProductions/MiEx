import os
import json


ROT = [0, 90, 180, 270]
ROT_TUPLE : list[tuple[int, float, float, float]] = []
_index = 0
for roll in ROT:
    for pitch in ROT:
        for yaw in ROT:
            ROT_TUPLE.append((_index, pitch, yaw, roll))
            _index += 1

def rotateVector(vector: tuple[float, float, float], rot:tuple[int, float, float, float]) -> tuple[float, float, float]:
    res = (vector[0], vector[1], vector[2])
    if rot[1] == 90:
        res = (vector[0], -vector[2], vector[1])
    elif rot[1] == 180:
        res = (vector[0], -vector[1], -vector[2])
    elif rot[1] == 270:
        res = (vector[0], vector[2], -vector[1])
    
    if rot[2] == 90:
        res = (vector[2], vector[1], -vector[0])
    elif rot[2] == 180:
        res = (-vector[0], vector[1], -vector[2])
    elif rot[2] == 270:
        res = (-vector[2], vector[1], vector[0])
    
    if rot[3] == 90:
        res = (-vector[1], vector[0], vector[2])
    elif rot[3] == 180:
        res = (-vector[0], -vector[1], vector[2])
    elif rot[3] == 270:
        res = (vector[1], -vector[0], vector[2])
    return res

def copyMapping(mapping: dict):
    res = {
        "name": mapping.get("name", "")
    }
    if "constants" in mapping:
        res["constants"] = {}
        for key, value in mapping["constants"].items():
            res["constants"][key] = value
    return res

def setConstant(mapping: dict, key: str, value: str):
    if "constants" not in mapping:
        mapping["constants"] = {}
    mapping["constants"][key] = value

def setCondition(mapping: dict, key: str, value: str):
    if "condition" not in mapping:
        mapping["condition"] = {}
    mapping["condition"][key] = value

def setOptionalCondition(mapping: dict, key: str, value: str):
    if "optionalCondition" not in mapping:
        mapping["optionalCondition"] = {}
    mapping["optionalCondition"][key] = value

def mapSlab(mapping: dict) -> list[dict]:
    res = []

    for rot in ROT_TUPLE:
        mapping2 = copyMapping(mapping)
        if rot[0] == 0:
            setOptionalCondition(mapping2, "rotation", str(rot[0]))
        else:
            setCondition(mapping2, "rotation", str(rot[0]))
        
        down = rotateVector((0, -1, 0), rot)
        if down[1] <= 0:
            setConstant(mapping, "type", "bottom")
        else:
            setConstant(mapping, "type", "top")

        res.append(mapping2)

    return res

def mapStairs(mapping: dict) -> list[dict]:
    res = []

    for rot in ROT_TUPLE:
        mapping2 = copyMapping(mapping)
        setCondition(mapping2, "rotation", str(rot[0]))
        
        down = rotateVector((0, -1, 0), rot)
        north = rotateVector((-1, 0, 0), rot)
        if down[1] == 0:
            tmp = down
            down = north
            north = tmp
            north = rotateVector(north, (0, 0, 270, 0))

        if down[1] <= 0:
            setConstant(mapping, "half", "bottom")
        else:
            setConstant(mapping, "half", "top")
        
        if north[2] == -1:
            setConstant(mapping, "facing", "north")
        elif north[2] == 1:
            setConstant(mapping, "facing", "south")
        elif north[0] == 1:
            setConstant(mapping, "facing", "east")
        elif north[0] == -1:
            setConstant(mapping, "facing", "west")
        else:
            setConstant(mapping, "facing", "north")
        
        res.append(mapping2)

    return res

def mapFacing(mapping: dict) -> list[dict]:
    res = []

    for rot in ROT_TUPLE:
        mapping2 = copyMapping(mapping)
        setCondition(mapping2, "rotation", str(rot[0]))
        
        down = rotateVector((0, -1, 0), rot)
        north = rotateVector((-1, 0, 0), rot)
        if down[1] == 0:
            tmp = down
            down = north
            north = tmp

        if north[2] == -1:
            setConstant(mapping, "facing", "north")
        elif north[2] == 1:
            setConstant(mapping, "facing", "south")
        elif north[0] == 1:
            setConstant(mapping, "facing", "east")
        elif north[0] == -1:
            setConstant(mapping, "facing", "west")
        else:
            setConstant(mapping, "facing", "north")
        
        res.append(mapping2)

    return res

def mapFacingUp(mapping: dict) -> list[dict]:
    res = []

    for rot in ROT_TUPLE:
        mapping2 = copyMapping(mapping)
        setCondition(mapping2, "rotation", str(rot[0]))
        
        up = rotateVector((0, 1, 0), rot)

        if up[2] == -1:
            setConstant(mapping, "facing", "north")
        elif up[2] == 1:
            setConstant(mapping, "facing", "south")
        elif up[0] == 1:
            setConstant(mapping, "facing", "east")
        elif up[0] == -1:
            setConstant(mapping, "facing", "west")
        else:
            setConstant(mapping, "facing", "up")
        
        res.append(mapping2)

    return res

def map(mapping: dict) -> list[dict]:
    name : str = mapping.get("name", "")
    if name.endswith("_slab"):
        return mapSlab(mapping)
    elif name.endswith("_stairs"):
        return mapStairs(mapping)
    
    constants : dict = mapping.get("constants", {})
    if "facing" in constants:
        if constants["facing"] == "up":
            return mapFacingUp(mapping)
        return mapFacing(mapping)

    return [mapping]

def addInMissingBlocks(translations: dict):
    itemsFolder = "C:\\Users\\me\\OneDrive\\Documenten\\_SOFTWARE_\\Hytale\\decomp\\2026.01.13\\assets\\Server\\Item\\Items"
    def search(folder, blocks: list[str]):
        for f in os.listdir(folder):
            fp = folder + "/" + f
            if os.path.isdir(fp):
                search(fp, blocks)
            elif os.path.isfile(fp):
                if f.startswith("Prototype"):
                    continue
                if f.endswith(".json"):
                    blocks.append("hytale:" + f.partition(".")[0])
    
    allBlocks = []
    search(itemsFolder, allBlocks)

    for block in allBlocks:
        if block not in translations:
            translations[block] = [ { "name": "minecraft:air" } ]

def run():
    translations: dict = {}
    with open("miex_blocks_in.json", "r", encoding='utf-8') as f:
        data = json.load(f)
        translations = data[0]["translations"]

    newTranslations : dict = {}
    for hytaleBlock, mappings in translations.items():
        resMappings : list[dict] = []
        for mapping in mappings:
            resMappings.extend(map(mapping))
        newTranslations[hytaleBlock] = resMappings
    
    addInMissingBlocks(newTranslations)

    outData = [
        {
            "translations": newTranslations
        }
    ]

    with open("miex_blocks.json", "w", encoding='utf-8') as f:
        json.dump(outData, f, indent=4)

run()