import os
import os.path
import json
import math

JAVA_RESOURCE_PACK = "W:/OneDrive/Documenten/_SOFTWARE_/MCWorldExporter/MCWorldExporter/resources/base_resource_pack"
BEDROCK_RESOURCE_PACK = "W:/OneDrive/Documenten/_SOFTWARE_/MCWorldExporter/bedrock-samples-main/resource_pack"

REPLACES : list[tuple[str, str]] = [
    ("dye_", "dye_powder_"),
    ("chainmail", "chain"),
    ("wooden", "wood"),
    ("golden", "gold"),
    ("overlay", "dyed"),
    ("_overlay", ""),
    ("item", ""),
    ("bottom", "lower"),
    ("top", "upper"),
    ("_on", "_powered"),
    ("attached", "connected"),
    ("leaves", "leave"),
    ("stalk", "stem"),
    ("end", "top"),
    ("stage0", "stage_0"),
    ("stage1", "stage_1"),
    ("stage2", "stage_2"),
    ("stage3", "stage_3"),
    ("stage4", "stage_4"),
    ("stage5", "stage_5"),
    ("stage6", "stage_6"),
    ("stage7", "stage_7"),
    ("stage8", "stage_8"),
    ("stage9", "stage_9"),
    ("tip", "top"),
    ("tube", "blue"),
    ("brain", "pink"),
    ("bubble", "purple"),
    ("fire", "red"),
    ("horn", "yellow"),
    ("bricks", "brick"),
    ("_fire", ""),
    ("carved", "face"),
    ("plant", "body"),
    ("lit", "berries"),
    ("lit", "head_berries"),
    ("plant_lit", "body_berries"),
    ("chiseled", "carved"),
    ("_stained_", "_"),
    ("00", "0"),
    ("01", "1"),
    ("02", "2"),
    ("03", "3"),
    ("04", "4"),
    ("05", "5"),
    ("06", "6"),
    ("07", "7"),
    ("08", "8"),
    ("09", "9"),
    ("00", "item"),("01", "item"),("02", "item"),("03", "item"),("04", "item"),
    ("05", "item"),("06", "item"),("07", "item"),("08", "item"),("09", "item"),
    ("10", "item"),("11", "item"),("12", "item"),("13", "item"),("14", "item"),
    ("15", "item"),("16", "item"),("17", "item"),("18", "item"),("19", "item"),
    ("20", "item"),("21", "item"),("22", "item"),("23", "item"),("24", "item"),
    ("25", "item"),("26", "item"),("27", "item"),("28", "item"),("29", "item"),
    ("30", "item"),("31", "item"),("32", "item"),("33", "item"),("34", "item"),
    ("35", "item"),("36", "item"),("37", "item"),("38", "item"),("39", "item"),
    ("scute", "shell_piece"),
    ("fish_bucket", "bucket"),
    ("slime_ball", "slimeball"),
    ("guster_banner", "banner"),
    ("mojang_banner", "banner"),
    ("piglin_banner", "banner"),
    ("flow_banner", "banner"),
    ("globe_banner", "banner"),
    ("creeper_banner", "banner"),
    ("skull_banner", "banner"),
    ("flower_banner", "banner"),
    ("_brick", "brick"),
    ("_shell", ""),
    ("music_disc", "record"),
    ("_slice", ""),
    ("ink_sac", "dye_powder"),
    ("glass_bottle", "potion_bottle"),
    ("glass_bottle", "potion_bottle_empty"),
    ("firework_star", "fireworks_charge"),
    ("firework_star_overlay", "fireworks_charge"),
    ("dragon", "dragons"),
    ("cod", "fish"),
    ("oak_sign", "sign"),
    ("oak", "wood"),
    ("salmon", "fish_salmon"),
    ("mason", "stonemason"),
    ("collar", "tame"),
    ("cold", "suffocated"),
    ("black", "blackrabbit"),
    ("arrow", "arrows"),
    ("zombified", "zombie"),
    ("weak", "sneezy"),
    ("evoker_fangs", "fangs"),
    ("illusioner", "pillager"),
    ("fishing_hook", "fishhook"),
    ("all_black", "allblackcat"),
    ("black", "blackcat"),
    ("british_shorthair", "britishshorthair"),
    ("desert", "biome-desert-zombie"),
    ("plains", "biome-plains-zombie"),
    ("savanna", "biome-savanna-zombie"),
    ("swamp", "biome-swamp-zombie"),
    ("taiga", "biome-taiga-zombie"),
    ("snow", "biome-snow-zombie"),
    ("jungle", "biome-jungle-zombie"),
    ("wood", "armor_stand"),
    ("log", "log_side"),
    ("stripped", "side_stripped"),
    ("stonecutter", "stonecutter2"),
    ("smooth_stone", "stone_slab_top"),
    ("_block", ""),
    ("short_grass", "tallgrass"),
    ("corner", "normal_turned"),
    ("pillar", "lines"),
    ("powered", "golden"),
    ("powered_rail_on", "rail_golden_powered"),
    ("on", "powered"),
    ("oak_trapdoor", "trapdoor"),
    ("note_block", "noteblock"),
    ("nether_portal", "portal"),
    ("kelp", "kelp_top"),
    ("kelp_plant", "kelp_a"),
    ("jack_o_lantern", "pumpkin_face_on"),
    ("flowering", "flowers"),
    ("fletching", "fletcher"),
    ("fletching_table_side", "fletcher_table_side1"),
    ("fletching_table_front", "fletcher_table_side2"),
    ("moist", "wet"),
    ("dirt_path", "grass_path"),
    ("dark_oak", "big_oak"),
    ("cut", "carved"),
    ("composter", "compost"),
    ("carved_pumpkin", "pumpkin_face_off"),
    ("side", "side1"),
    ("big_dripleaf_tip", "big_dripleaf_side2"),
    ("stage0", "sapling"),
    ("large_leaves", "small_leaf"),
    ("stem", "log_side"),
    ("azure_bluet", "flower_houstonia"),
    ("blast_furnace_front", "blast_furnace_front_off"),
    ("chain", "chain2"),
    ("cobweb", "web"),
    ("dark_oak", "roofed_oak"),
    ("dead_bush", "deadbush"),
    ("farmland", "farmland_dry"),
    ("podzol", "dirt_podzol"),
    ("poppy", "flower_rose"),
    ("prismarine", "prismarine_rough"),
    ("quartz", "quartz_block"),
    ("quartz_pillar", "quartz_block_lines"),
    ("rail", "rail_normal"),
    ("shulker_box", "shulker_top_undyed"),
    ("sugar_cane", "reeds"),
    ("sunflower", "double_plant_sunflower"),
    ("terracotta", "hardened_clay"),
    ("terracotta", "hardened_clay_stained"),
    ("tripwire", "trip_wire"),
    ("tripwire_hook", "trip_wire_source"),
    ("plant", "base"),
    ("dark_oak", "darkoak"),
    ("anvil_top", "anvil_top_damaged_0"),
    ("wool", "wool_colored"),
    ("comparator", "comparator_off"),
    ("crimson_nylium", "crimson_nylium_top"),
    ("crimson_stem", "crimson_log"),
    ("chipped_anvil_top", "anvil_top_damaged_1"),
    ("damaged_anvil_top", "anvil_top_damaged_2"),
    ("end_portal_frame", "endframe"),
    ("end_stone_bricks", "end_bricks"),
    ("grass", "tallgrass"),
    ("grass_block_side_overlay", "grass_side"),
    ("jigsaw_bottom", "jigsaw_back"),
    ("light_gray", "silver"),
    ("light_gray_wool", "wool_colored_silver"),
    ("light_gray_terracotta", "glazed_terracotta_silver"),
    ("light_gray_stained_glass_pane_top", "glass_pane_top_silver"),
    ("light_gray_stained_glass", "glass_silver"),
    ("light_gray_shulker_box", "shulker_top_silver"),
    ("light_gray_glazed_terracotta", "glazed_terracotta_silver"),
    ("light_gray_concrete_powder", "concrete_powder_silver"),
    ("lilac", "double_plant_syringa"),
    ("lily_pad", "waterlily"),
    ("magenta_wool", "wool_colored_magenta"),
    ("melon_stem", "melon_stem_disconnected"),
    ("mossy_stone_bricks", "stonebrick_mossy"),
    ("mushroom_stem", "mushroom_block_skin_stem"),
    ("nether_quartz_ore", "quartz_ore"),
    ("peony","double_plant_paeonia" ),
    ("pumpkin_stem", "pumpkin_stem_disconnected"),
    ("pointed_dripstone_up_tip_merge", "pointed_dripstone_up_merge"),
    ("pointed_dripstone_down_tip_merge", "pointed_dripstone_down_merge"),
    ("polished_andesite", "stone_andesite_smooth"),
    ("redstone_lamp", "redstone_lamp_off"),
    ("repeater", "repeater_off"),
    ("rooted_dirt", "dirt_with_roots"),
    ("spawner", "mob_spawner"),
    ("stone_bricks", "stonebrick"),
    ("turtle_egg", "turtle_egg_not_cracked"),
    ("warped_nylium", "warped_nylium_top"),
    ("warped_stem", "warped_stem_side"),
    ("crimson_door_top", "crimson_door_upper"),
    ("end_portal_frame_side", "endframe_side"),
    ("oak_door_bottom", "door_wood_lower"),
    ("oak_door_top", "door_wood_upper"),
    ("redstone_dust_dot", "redstone_dust_cross"),
    ("redstone_dust_line0", "redstone_dust_line"),
    ("redstone_dust_line1", "redstone_dust_line"),
    ("warped_door_top", "warped_door_upper"),
    ("tall_grass_top", "double_plant_grass_top")
]
BASE_PATH_REPLACES : list[tuple[str, str]] = [
    ("villager", "villager2"),
    ("villager/profession", "villager2/professions"),
    ("villager/profession_level", "villager2/levels"),
    ("villager/type", "villager2/biomes"),
    ("wither", "wither_boss"),
    ("shield", "shield_patterns"),
    ("illager", "vex"),
    ("hoglin", "zoglin"),
    ("end_crystal", "endercrystal"),
    ("enderdragon", "dragon"),
    ("entity", "models/armor"),
    ("entity/decorated_pot", "blocks"),
    ("entity/conduit", "blocks")
]
ADDITIONAL_TOKENS : list[str] = [
    "on",
    "raw",
    "conduit",
    "top",
    "base",
    "double_plant",
    "horizontal",
    "side",
    "flower",
    "stone"
]
# Any resourceId that starts with the following strings get ignored
IGNORE_LIST : list[str] = [
    "textures;minecraft:particle/",
    "textures;minecraft:trims",
    "textures;minecraft:mob_effect",
    "textures;minecraft:misc",
    "textures;minecraft:gui",
    "textures;minecraft:painting",
    "textures;minecraft:map",
    "textures;minecraft:font",
    "textures;minecraft:environment",
    "textures;minecraft:entity/player",
    "textures;minecraft:entity/chest/christmas",
    "textures;minecraft:effect"
]

FILE_PATH_MAPPING : dict[str, str] = {
    "textures;minecraft:block/stripped_cherry_log": "textures/blocks/stripped_cherry_log_side",
    "textures;minecraft:block/stripped_mangrove_log": "textures/blocks/stripped_mangrove_log_side"
}

def parseNamespace(folder : str, resourceIds : list[str]):
    namespace = os.path.basename(folder)
    parseFolder(folder + "/textures", "textures;" + namespace, "", resourceIds)

def parseFolder(folder : str, namespace : str, parent : str, resourceIds : list[str]):
    for file in os.listdir(folder):
        fp = folder + "/" + file
        if os.path.isdir(fp):
            parseFolder(fp, namespace, parent + file + "/", resourceIds)
        elif os.path.isfile(fp):
            parseFile(fp, namespace, parent, resourceIds)

def parseFile(file : str, namespace : str, parent : str, resourceIds : list[str]):
    if not (file.endswith(".png")):
        return
    
    resourceId = namespace + ":" + parent + os.path.splitext(os.path.basename(file))[0]

    if resourceId in FILE_PATH_MAPPING:
        return

    for ignorePrefix in IGNORE_LIST:
        if resourceId.startswith(ignorePrefix):
            return

    resourceIds.append(resourceId)

def solveResourceId(resourceId : str, solveIteration : int):
    bedrockPath = getBedrockPath(resourceId)

    if os.path.exists(bedrockPath + ".png"):
        # Trivial repathing, so we don't have to write it down
        return True
    if os.path.exists(bedrockPath + ".tga"):
        # Trivial repathing, so we don't have to write it down
        return True
    
    bedrockPath2 = bedrockPath.replace("/block/", "/blocks/").replace("/item/", "/items/")
    if os.path.exists(bedrockPath2 + ".png") or os.path.exists(bedrockPath2 + ".tga"):
        addMapping(resourceId, bedrockPath2)
        return True
    
    basePath, filename = os.path.split(bedrockPath2)

    if os.path.exists(basePath):
        for subFolder in os.listdir(basePath):
            if os.path.isdir(basePath + "/" + subFolder):
                if os.path.exists(basePath + "/" + subFolder + "/" + filename + ".png") or \
                        os.path.exists(basePath + "/" + subFolder + "/" + filename + ".tga"):
                    addMapping(resourceId, basePath + "/" + subFolder + "/" + filename)
                    return True 
    
    for replace in REPLACES:
        filename2 = filename.replace(replace[0], replace[1])
        if os.path.exists(basePath + "/" + filename2 + ".png") or \
                os.path.exists(basePath + "/" + filename2 + ".tga"):
            addMapping(resourceId, basePath + "/" + filename2)
            return True 

    if solveIteration <= 2:
        if trySimilarity(resourceId, basePath, filename, basePath, False if solveIteration == 2 else True):
            return True

    if solveIteration == 1:
        for basePathReplace in BASE_PATH_REPLACES:
            searchBasePath = basePath.replace(basePathReplace[0], basePathReplace[1])
            if searchBasePath == basePath:
                continue # If nothing changed, then no need to check it again
            if trySimilarity(resourceId, basePath, filename, searchBasePath, False):
                return True
            
        # Try sub paths
        if os.path.exists(basePath):
            for subFolder in os.listdir(basePath):
                if os.path.isdir(basePath + "/" + subFolder):
                    if trySimilarity(resourceId, basePath, filename, basePath + "/" + subFolder, False):
                        return True
            
        # Try in parent folders
        searchBasePath = os.path.dirname(basePath)
        for i in range(2):
            if trySimilarity(resourceId, basePath, filename, searchBasePath, True):
                return True
            searchBasePath = os.path.dirname(searchBasePath)

    return False

def addMapping(resourceId : str, bedrockPath : str):
    FILE_PATH_MAPPING[resourceId] = bedrockPath[(len(BEDROCK_RESOURCE_PACK)+1):]

def trySimilarity(resourceId : str, basePath : str, filename : str, searchBasePath : str, ignoreAlreadyMapped : bool) -> bool:
    if not os.path.exists(searchBasePath):
        return False
    
    tokens = filename.split("_")

    def calcSimilarity(tokensA : list[str], tokensB : list[str]) -> float:
        similarity : float = 0.0
        for tokenA in tokensA:
            if tokenA in tokensB:
                similarity += 1.0
        for tokenB in tokensB:
            if tokenB in tokensA:
                similarity += 1.0
        return similarity * math.exp(-abs(len(tokensA) - len(tokensB))) * math.exp(-max(len(tokensA), len(tokensB)) * 0.5)
    
    bestSimilarity = 0
    bestFilename = None
    similarityCount = 0

    for filenameTest in os.listdir(searchBasePath):
        if os.path.isfile(searchBasePath + "/" + filenameTest):
            filenameTest = os.path.splitext(filenameTest)[0]

            if ignoreAlreadyMapped:
                if (searchBasePath + "/" + filenameTest) in MAPPED_BEDROCK_PATHS:
                    continue

            testTokens = filenameTest.split("_")
            
            similarity = calcSimilarity(tokens, testTokens)

            if similarity > bestSimilarity:
                bestSimilarity = similarity
                bestFilename = filenameTest
                similarityCount = 1
            elif similarity == bestSimilarity:
                similarityCount += 1
            
            for replace in REPLACES:
                filename2 = filename.replace(replace[0], replace[1])
                
                tokens2 = filename2.split("_")
            
                similarity = calcSimilarity(tokens2, testTokens)

                if similarity > bestSimilarity:
                    bestSimilarity = similarity
                    bestFilename = filenameTest
                    similarityCount = 1
            
            for additionalToken in ADDITIONAL_TOKENS:
                tokens2 = list(tokens)
                tokens2.extend(additionalToken.split("_"))
                if len(tokens2) != len(testTokens):
                    continue
            
                similarity = calcSimilarity(tokens2, testTokens)

                if similarity > bestSimilarity and similarity > (len(tokens2)*1.9* math.exp(-len(tokens2))):
                    bestSimilarity = similarity
                    bestFilename = filenameTest
                    similarityCount = 1

    
    if bestFilename is not None:
        if similarityCount <= 1:
            addMapping(resourceId, searchBasePath + "/" + bestFilename)
            return True
    return False

def getBedrockPath(resourceId : str):
    categoryTokens = resourceId.split(";")
    category = categoryTokens[0]
    namespaceTokens = categoryTokens[1].split(":")
    namespace = namespaceTokens[0]
    fileName = namespaceTokens[1]

    path = category + "/" + fileName

    return BEDROCK_RESOURCE_PACK + "/" + path

MAPPED_BEDROCK_PATHS : list[str] = []

def run():
    resourceIds : list[str] = []
    parseNamespace(JAVA_RESOURCE_PACK + "/assets/minecraft", resourceIds)

    unsolvedResourceIds : list[str] = []
    progress = 0.0
    lastProgress = 0.0
    for solveIteration in range(3):
        progressIterStep = 1.0 / 3.0
        progressStep = progressIterStep / float(len(resourceIds))
        for resourceId in resourceIds:
            progress += progressStep
            if progress - lastProgress > 0.01:
                lastProgress = progress
                print(str(int(progress * 100)) + "%")

            
            result = solveResourceId(resourceId, solveIteration)
            if result == True:
                if resourceId in FILE_PATH_MAPPING:
                    MAPPED_BEDROCK_PATHS.append(FILE_PATH_MAPPING[resourceId])
            else:
                unsolvedResourceIds.append(resourceId)
        resourceIds = unsolvedResourceIds
        unsolvedResourceIds = []
    
    for resourceId in resourceIds:
        print("Non-trivial pathing for", resourceId)

    with open("./miex_file_path_mapping.json", 'w', encoding='utf-8') as f:
        json.dump(FILE_PATH_MAPPING, f, indent=4)

run()