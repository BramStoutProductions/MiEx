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
import math

BEDROCK_STATES_TO_JAVA : dict[str, list[str]] = {
    "minecraft:cardinal_direction": [ "facing" ],
    "minecraft:block_face": [ "facing" ],
    "age_bit": [ "stage" ],
    "bamboo_leaf_size": [ "leaves" ],
    "bamboo_stalk_thickness": [ "age" ],
    "ground_sign_direction": [ "rotation" ],
    "direction": [ "facing" ],
    "head_piece_bit": [ "part" ],
    "infiniburn_bit": [],
    "growth": [ "age", "flower_amount" ],
    "toggle_bit": [ "powered", "enabled" ],
    "brewing_stand_slot_a_bit": [ "has_bottle_0" ],
    "brewing_stand_slot_b_bit": [ "has_bottle_1" ],
    "brewing_stand_slot_c_bit": [ "has_bottle_2" ],
    "button_pressed_bit": [ "powered" ],
    "cauldron_liquid": [],
    "coral_fan_direction": [],
    "coral_hang_type_bit": [],
    "coral_direction": [ "facing" ],
    "redstone_signal": [ "power", "powered" ],
    "open_bit": [ "open", "powered" ],
    "upper_block_bit": [ "half" ],
    "moisturized_amount": [ "moisture" ],
    "update_bit": [],
    "attachment": [ "face" ],
    "liquid_depth": [ "level" ],
    "direction": [ "facing" ],
    "hanging": [ "vertical_direction" ],
    "rail_direction": [ "shape" ],
    "rail_data_bit": [ "powered" ],
    "output_subtract_bit": [ "mode" ],
    "output_lit_bit": [ "powered" ],
    "stability": [ "distance" ],
    "active": [ "shrieking" ],
    "cluster_count": [ "pickles" ],
    "minecraft:vertical_half": [ "type" ],
    "height": [ "layers" ],
    "upside_down_bit": [ "half" ],
    "weirdo_direction": [ "facing" ],
    "structure_block_type": [ "mode" ],
    "allow_underwater_bit": [],
    "explode_bit": [ "unstable" ],
    "cracked_state": [ "hatch" ],
    "wall_post_bit": [ "up" ],
    "stone_slab_type": None,
    "stone_slab_type_3": None
}

BEDROCK_GENERIC_VALUES_TO_JAVA : dict[str, list[str]] = {
    "false": ["0"],
    "true": ["1"],
    "0": ["false"],
    "1": ["true"],
    "short": ["low"]
}

BEDROCK_VALUES_TO_JAVA : dict[str, dict[str, dict[str, str]]] = {
    "bamboo_leaf_size": {
        "no_leaves": { "leaves": "none" },
        "small_leaves": { "leaves": "small" },
        "large_leaves": { "leaves": "large" }
    },
    "bamboo_stalk_thickness": {
        "thin": { "age": "0" },
        "thick": { "age": "1" }
    },
    "facing_direction": {
        "0": { "facing": "dowm" },
        "1": { "facing": "up" },
        "2": { "facing": "north" },
        "3": { "facing": "south" },
        "4": { "facing": "west" },
        "5": { "facing": "east" }
    },
    "direction": {
        "0": { "facing": "south" },
        "1": { "facing": "west" },
        "2": { "facing": "north" },
        "3": { "facing": "east" }
    },
    "head_piece_bit": {
        "false": { "part": "foot" },
        "0": { "part": "foot" },
        "true": { "part": "head" },
        "1": { "part": "head" }
    },
    "attachment": {
        "standing": { "attachment": "floor" },
        "hanging": { "attachment": "ceiling" },
        "side": { "attachment": "single_wall" },
        "multiple": { "attachment": "double_wall" }
    },
    "big_dripleaf_tilt": {
        "none": { "tilt": "none" },
        "unstable": { "tilt": "unstable" },
        "partial_tilt": { "tilt": "partial" },
        "full_tilt": { "tilt": "full" }
    },
    "extinguished": {
        "false": { "lit": "true" },
        "0": { "lit": "true" },
        "true": { "lit": "false" },
        "1": { "lit": "false" }
    },
    "fill_level": {
        "0": { "level": "0" },
        "1": { "level": "1" },
        "2": { "level": "1" },
        "3": { "level": "1" },
        "4": { "level": "2" },
        "5": { "level": "2" },
        "6": { "level": "3" },
    },
    "coral_direction": {
        "0": { "facing": "west" },
        "1": { "facing": "east" },
        "2": { "facing": "north" },
        "3": { "facing": "south" }
    },
    "door_hinge_bit": {
        "false": { "hinge": "left" },
        "0": { "hinge": "left" },
        "true": { "hinge": "true" },
        "1": { "hinge": "true" }
    },
    "upper_block_bit": {
        "false": { "half": "lower" },
        "0": { "half": "lower" },
        "true": { "half": "upper" },
        "1": { "half": "upper" }
    },
    "lever_direction": {
        "east": { "face": "wall", "facing": "east" },
        "west": { "face": "wall", "facing": "west" },
        "south": { "face": "wall", "facing": "south" },
        "north": { "face": "wall", "facing": "north" },
        "down_east_west": { "face": "ceiling", "facing": "east" },
        "down_north_south": { "face": "ceiling", "facing": "south" },
        "up_east_west": { "face": "floor", "facing": "east" },
        "up_north_south": { "face": "floor", "facing": "south" }
    },
    "huge_mushroom_bits": {
        "0":  { "east": "false", "west": "false", "north": "false", "south": "false", "up": "false", "down": "false" },
        "1":  { "east": "false", "west": "true", "north": "true", "south": "false", "up": "true", "down": "false" },
        "2":  { "east": "false", "west": "false", "north": "true", "south": "false", "up": "true", "down": "false" },
        "3":  { "east": "true", "west": "false", "north": "true", "south": "false", "up": "true", "down": "false" },
        "4":  { "east": "false", "west": "true", "north": "false", "south": "false", "up": "true", "down": "false" },
        "5":  { "east": "false", "west": "false", "north": "false", "south": "false", "up": "true", "down": "false" },
        "6":  { "east": "true", "west": "false", "north": "false", "south": "false", "up": "true", "down": "false" },
        "7":  { "east": "false", "west": "true", "north": "false", "south": "true", "up": "true", "down": "false" },
        "8":  { "east": "false", "west": "false", "north": "false", "south": "true", "up": "true", "down": "false" },
        "9":  { "east": "true", "west": "false", "north": "false", "south": "true", "up": "true", "down": "false" },
        "10": { "east": "true", "west": "true", "north": "true", "south": "true", "up": "false", "down": "false" },
        "11": { "east": "false", "west": "false", "north": "false", "south": "false", "up": "false", "down": "false" },
        "12": { "east": "false", "west": "false", "north": "false", "south": "false", "up": "false", "down": "false" },
        "13": { "east": "false", "west": "false", "north": "false", "south": "false", "up": "false", "down": "false" },
        "14": { "east": "true", "west": "true", "north": "true", "south": "true", "up": "true", "down": "true" },
        "15": { "east": "true", "west": "true", "north": "true", "south": "true", "up": "true", "down": "true" },
    },
    "dripstone_thickness": {
        "merge": { "thickness": "tip_merge" },
        "tip": { "thickness": "tip" },
        "frustum": { "thickness": "frustum" },
        "middle": { "thickness": "middle" },
        "base": { "thickness": "base" },
    },
    "hanging": {
        "false": { "vertical_direction": "up" },
        "0": { "vertical_direction": "up" },
        "true": { "vertical_direction": "down" },
        "1": { "vertical_direction": "down" }
    },
    "rail_direction": {
        "0": { "shape": "north_south" },
        "1": { "shape": "east_west" },
        "2": { "shape": "ascending_east" },
        "3": { "shape": "ascending_west" },
        "4": { "shape": "ascending_north" },
        "5": { "shape": "ascending_south" },
        "6": { "shape": "south_east" },
        "7": { "shape": "south_west" },
        "8": { "shape": "north_west" },
        "9": { "shape": "north_east" }
    },
    "output_subtract_bit": {
        "false": { "mode": "compare" },
        "0": { "mode": "compare" },
        "true": { "mode": "subtract" },
        "1": { "mode": "subtract" }
    },
    "sculk_sensor_phase": {
        "0": { "sculk_sensor_phase": "active" },
        "1": { "sculk_sensor_phase": "cooldown" },
        "2": { "sculk_sensor_phase": "inactive" }
    },
    "weirdo_direction": {
        "0": { "facing": "east", "shape": "straight" },
        "1": { "facing": "west", "shape": "straight" },
        "2": { "facing": "south", "shape": "straight" },
        "3": { "facing": "north", "shape": "straight" }
    },
    "upside_down_bit": {
        "false": { "half": "bottom" },
        "0": { "half": "bottom" },
        "true": { "half": "top" },
        "1": { "half": "top" }
    },
    "turtle_egg_count": {
        "one_egg": { "eggs": "1" },
        "two_egg": { "eggs": "2" },
        "three_egg": { "eggs": "3" },
        "four_egg": { "eggs": "4" },
    },
    "cracked_state": {
        "no_cracks": { "hatch": "0" },
        "cracked": { "hatch": "1" },
        "max_cracked": { "hatch": "2" }
    }
}

def edgeCaseTorch(block : 'Block') -> list['Block']:
    if block.bedrockIdentifier.resource == "unlit_redstone_torch":
        block.getJavaBlockState("lit").value = "false"
    if block.bedrockIdentifier.resource == "torch" or \
                block.bedrockIdentifier.resource == "redstone_torch" or \
                block.bedrockIdentifier.resource == "unlit_redstone_torch":
        block2 = block.clone()
        block2.getBedrockBlockState("torch_facing_direction").mapping = None
        block2.getBedrockBlockState("torch_facing_direction").value = "top"
        
        if block.bedrockIdentifier.resource == "torch":
            block.javaIdentifiers = set([ Identifier("wall_torch", "Wall Torch") ])
        else:
            block.javaIdentifiers = set([ Identifier("redstone_wall_torch", "Redstone Wall Torch") ])
        
        return [block, block2]

def edgeCaseSeagrass(block : 'Block'):
    if block.bedrockIdentifier.resource == "seagrass":
        if block.getBedrockBlockState("sea_grass_type").value == "double_bot":
            block.getJavaBlockState("half").value = "lower"
            block.javaIdentifiers = set([ Identifier("tall_seagrass", "Tall Seagrass") ])
        elif block.getBedrockBlockState("sea_grass_type").value == "double_top":
            block.getJavaBlockState("half").value = "upper"
            block.javaIdentifiers = set([ Identifier("tall_seagrass", "Tall Seagrass") ])

def edgeCaseVines(block : 'Block'):
    if block.bedrockIdentifier.resource == "vine":
        vineDirectionBits = block.getBedrockBlockState("vine_direction_bits").value
        if vineDirectionBits == None:
            return
        vineDirectionBits = int(vineDirectionBits)
        south = vineDirectionBits & 1
        west = (vineDirectionBits >> 1) & 1
        north = (vineDirectionBits >> 2) & 1
        east = (vineDirectionBits >> 3) & 1
        if south > 0:
            block.getJavaBlockState("south").value = "true"
        if west > 0:
            block.getJavaBlockState("west").value = "true"
        if north > 0:
            block.getJavaBlockState("north").value = "true"
        if east > 0:
            block.getJavaBlockState("east").value = "true"

def edgeCaseBeetroots(block : 'Block'):
    if block.bedrockIdentifier.resource == "beetroot":
        block.getBedrockBlockState("growth").mapping = {
            "0": { "age": "0" },
            "1": { "age": "0" },
            "2": { "age": "1" },
            "3": { "age": "1" },
            "4": { "age": "2" },
            "5": { "age": "2" },
            "6": { "age": "2" },
            "7": { "age": "3" }
        }

def edgeCaseGrindstone(block : 'Block'):
    if block.bedrockIdentifier.resource == "grindstone":
        block.getBedrockBlockState("attachment").mapping = {
            "standing": { "face": "floor" },
            "hanging": { "face": "ceiling" },
            "side": { "face": "wall" },
            "multiple": { "face": "wall" }
        }
        block.getJavaBlockState("face").mapping = {}

def edgeCaseButton(block : 'Block'):
    if block.bedrockIdentifier.resource.endswith("_button"):
        block.getBedrockBlockState("facing_direction").mapping = {
            "0": { "face": "ceiling", "facing": "north" },
            "1": { "face": "floor", "facing": "north" },
            "2": { "face": "wall", "facing": "north" },
            "3": { "face": "wall", "facing": "south" },
            "4": { "face": "wall", "facing": "west" },
            "5": { "face": "wall", "facing": "east" }
        }

def edgeCaseAnvil(block : 'Block'):
    if block.bedrockIdentifier.resource == "anvil":
        damage = block.getBedrockBlockState("damage").value
        if damage is None:
            return
        if damage == "undamaged":
            block.javaIdentifiers = set([ Identifier("anvil", "Anvil") ])
        elif damage == "slightly_damaged":
            block.javaIdentifiers = set([ Identifier("chipped_anvil", "Chipped Anvil") ])
        elif damage == "very_damaged":
            block.javaIdentifiers = set([ Identifier("chipped_anvil", "Chipped Anvil") ])
        elif damage == "broken":
            block.javaIdentifiers = set([ Identifier("damaged_anvil", "Damaged Anvil") ])

def edgeCaseBed(block : 'Block') -> list['Block']:
    if block.bedrockIdentifier.resource == "bed":
        colours : dict[int, str] = {
            0: "white_bed",
            1: "orange_bed",
            2: "magenta_bed",
            3: "light_blue_bed",
            4: "yellow_bed",
            5: "lime_bed",
            6: "pink_bed",
            7: "gray_bed",
            8: "light_gray_bed",
            9: "cyan_bed",
            10: "purple_bed",
            11: "blue_bed",
            12: "brown_bed",
            13: "green_bed",
            14: "red_bed",
            15: "black_bed"
        }
        block.bedrockBlockStates.add(BlockState("color", [], "0"))
        res = []
        for colour, javaName in colours.items():
            block2 = block.clone()
            block2.getBedrockBlockState("color").value = colour
            block2.javaIdentifiers = set([ Identifier(javaName, javaName) ])
            res.append(block2)
        return res

def edgeCaseBanner(block : 'Block') -> list['Block']:
    if block.bedrockIdentifier.resource == "banner" or block.bedrockIdentifier.resource == "standing_banner" or \
        block.bedrockIdentifier.resource == "wall_banner":
        colours : dict[int, str] = {
            0: "black_",
            1: "red_",
            2: "green_",
            3: "brown_",
            4: "blue_",
            5: "purple_",
            6: "cyan_",
            7: "light_gray_",
            8: "gray_",
            9: "pink_",
            10: "lime_",
            11: "yellow_",
            12: "light_blue_",
            13: "magenta_",
            14: "orange_",
            15: "white_"
        }
        suffix = "banner"
        if block.bedrockIdentifier.resource == "wall_banner":
            suffix = "wall_banner"
        block.bedrockBlockStates.add(BlockState("Base", [], "0"))
        res = []
        for colour, javaName in colours.items():
            block2 = block.clone()
            block2.getBedrockBlockState("Base").value = colour
            block2.javaIdentifiers = set([ Identifier(javaName + suffix, javaName + suffix) ])
            res.append(block2)
        return res
    
def edgeCaseChiseledBookshelf(block : 'Block'):
    if block.bedrockIdentifier.resource == "chiseled_bookshelf":
        booksStored = block.getBedrockBlockState("books_stored").value
        if booksStored == None:
            return
        booksStored = int(booksStored)
        slot0 = booksStored & 1
        slot1 = (booksStored >> 1) & 1
        slot2 = (booksStored >> 2) & 1
        slot3 = (booksStored >> 3) & 1
        slot4 = (booksStored >> 4) & 1
        slot5 = (booksStored >> 5) & 1
        if slot0 > 0:
            block.getJavaBlockState("slot_0_occupied").value = "true"
        if slot1 > 0:
            block.getJavaBlockState("slot_1_occupied").value = "true"
        if slot2 > 0:
            block.getJavaBlockState("slot_2_occupied").value = "true"
        if slot3 > 0:
            block.getJavaBlockState("slot_3_occupied").value = "true"
        if slot4 > 0:
            block.getJavaBlockState("slot_4_occupied").value = "true"
        if slot5 > 0:
            block.getJavaBlockState("slot_5_occupied").value = "true"

def edgeCasePiston(block : 'Block'):
    if block.bedrockIdentifier.resource == "piston" or block.bedrockIdentifier.resource == "sticky_piston":
        block.bedrockBlockStates.add(BlockState("State", [], "0"))
        block.getBedrockBlockState("State").mapping = {
            "0": { "extended": "false" },
            "1": { "extended": "false" },
            "2": { "extended": "true" },
            "3": { "extended": "true" }
        }
        block.getJavaBlockState("extended").mapping = {}

def edgeCaseShulkerBox(block : 'Block'):
    if block.bedrockIdentifier.resource.endswith("shulker_box"):
        block.getJavaBlockState("facing").mapping = {}

def edgeCaseBigDripleaf(block : 'Block'):
    if block.bedrockIdentifier.resource.endswith("big_dripleaf"):
        head = block.getBedrockBlockState("big_dripleaf_head").value
        if head is None:
            return
        if head == "0":
            block.javaIdentifiers = set([ Identifier("big_dripleaf_stem", "Big Dripleaf Stem") ])
        elif head == "1":
            block.javaIdentifiers = set([ Identifier("big_dripleaf", "Big Dripleaf") ])

def edgeCaseCacke(block : 'Block'):
    if block.bedrockIdentifier.resource == "cake":
        block.javaIdentifiers = set([ Identifier("cake", "Cake") ])

def edgeCaseCoral(block : 'Block'):
    if block.bedrockIdentifier.resource == "coral_block" or block.bedrockIdentifier.resource.startswith("coral_fan_hang"):
        dead_bit = block.getBedrockBlockState("dead_bit").value
        if dead_bit is None:
            return
        if dead_bit == "true":
            block.javaIdentifiers = set([ Identifier("dead_" + next(iter(block.javaIdentifiers)).resource, "")])

def edgeCaseDoublePlant(block : 'Block'):
    if block.bedrockIdentifier.resource == "double_plant":
        double_plant_type = block.getBedrockBlockState("double_plant_type")
        if double_plant_type is None:
            return
        if double_plant_type == "syringa":
            block.javaIdentifiers = set([ Identifier("lilac", "Lilac") ])
        if double_plant_type == "paeonia":
            block.javaIdentifiers = set([ Identifier("peony", "Peony") ])

def edgeCaseMultiFaceDirectionBits(block : 'Block'):
    if block.getBedrockBlockState("multi_face_direction_bits") is not None:
        bits = block.getBedrockBlockState("multi_face_direction_bits").value
        if bits is None:
            return
        bits = int(bits)
        down = bits & 1
        up = (bits >> 1) & 1
        south = (bits >> 2) & 1
        west = (bits >> 3) & 1
        north = (bits >> 4) & 1
        east = (bits >> 5) & 1
        if down > 0:
            block.getJavaBlockState("down").value = "true"
        if up > 0:
            block.getJavaBlockState("up").value = "true"
        if south > 0:
            block.getJavaBlockState("south").value = "true"
        if west > 0:
            block.getJavaBlockState("west").value = "true"
        if north > 0:
            block.getJavaBlockState("north").value = "true"
        if east > 0:
            block.getJavaBlockState("east").value = "true"

def edgeCaseChiselType(block : 'Block'):
    if block.getBedrockBlockState("chisel_type") is not None:
        chisel_type = block.getBedrockBlockState("chisel_type").value
        if chisel_type == "lines":
            block.javaIdentifiers = set([ Identifier(next(iter(block.javaIdentifiers)).resource.replace("block", "pillar"), "") ])

def edgeCaseDoubleSlab(block : 'Block'):
    if block.bedrockIdentifier.resource.endswith("double_slab"):
        block.getJavaBlockState("type").mapping = None
        block.getJavaBlockState("type").value = "double"

def edgeCaseSnowLayer(block : 'Block'):
    if block.bedrockIdentifier.resource == "snow_layer":
        block.getBedrockBlockState("height").mapping = {
            "0": { "layers": "1" },
            "1": { "layers": "2" },
            "2": { "layers": "3" },
            "3": { "layers": "4" },
            "4": { "layers": "5" },
            "5": { "layers": "6" },
            "6": { "layers": "7" },
            "7": { "layers": "8" }
        }

def edgeCaseSponge(block : 'Block'):
    if block.bedrockIdentifier.resource == "sponge":
        sponge_type = block.getBedrockBlockState("sponge_type").value
        if sponge_type != "wet":
            block.javaIdentifiers = set([ Identifier("sponge", "Sponge") ])

def edgeCaseBrick(block : 'Block'):
    if block.bedrockIdentifier.resource == "cobblestone_wall":
        wall_block_type = block.getBedrockBlockState("wall_block_type").value
        if wall_block_type == "brick":
            block.javaIdentifiers = set([ Identifier("brick_wall", "Brick Wall") ])
    if block.bedrockIdentifier.resource.endswith("stone_block_slab"):
        wall_block_type = block.getBedrockBlockState("stone_slab_type").value
        if wall_block_type == "brick":
            block.javaIdentifiers = set([ Identifier("brick_slab", "Brick Slab") ])

def edgeCaseFern(block : 'Block'):
    if block.bedrockIdentifier.resource == "tallgrass":
        tall_grass_type = block.getBedrockBlockState("tall_grass_type").value
        if tall_grass_type == "2":
            block.javaIdentifiers = set([ Identifier("fern", "Fern") ])

def edgeCaseLiquid(block : 'Block'):
    if block.bedrockIdentifier.resource == "flowing_water" or block.bedrockIdentifier.resource == "flowing_lava":
        block.javaIdentifiers = set([ Identifier(block.bedrockIdentifier.resource.replace("flowing_", ""), "Liquid") ])

BLOCK_EDGE_CASES : list = [
    edgeCaseTorch, edgeCaseSeagrass, edgeCaseVines, edgeCaseBeetroots,
    edgeCaseGrindstone, edgeCaseButton, edgeCaseAnvil, edgeCaseBed,
    edgeCaseBanner, edgeCaseChiseledBookshelf, edgeCasePiston,
    edgeCaseShulkerBox, edgeCaseBigDripleaf, edgeCaseCacke, edgeCaseCoral,
    edgeCaseDoublePlant, edgeCaseMultiFaceDirectionBits, edgeCaseChiselType,
    edgeCaseDoubleSlab, edgeCaseSnowLayer, edgeCaseSponge, edgeCaseBrick,
    edgeCaseFern, edgeCaseLiquid
]

def getNameTokens(string : str) -> list[str]:
    return string.lower().replace(" ", "_").split("_")

def commonTokens(string1 : str, string2 : str) -> str:
    string1Tokens = getNameTokens(string1)
    string2Tokens = getNameTokens(string2)
    common : list[str] = []
    for string1Token in string1Tokens:
        if string1Token in string2Tokens:
            common.append(string1Token)
    res = ""
    for i in range(len(common)):
        if i > 0:
            res = res + "_"
        res = res + common[i]
    return res

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

def getAllBlockArticles() -> list[str]:
    mainArticle : str = wiki.getArticle("Block")
    foundList = False
    itemRegex = re.compile("\\[\\[([\\w\\s]+)\\|*[\\w\\s]*\\]\\]")
    resSet : list[str] = []
    for line in mainArticle.splitlines():
        if "== List of blocks ==" in line or "=== Technical blocks ===" in line:
            foundList = True
            continue
        if foundList == False:
            continue
        if line.startswith("*") == False:
            if "}}" in line:
                foundList = False
            continue

        match = itemRegex.search(line)
        if match is not None:
            resSet.append(match.group(1).replace(" ", "_"))
    return resSet

class Identifier:

    def __init__(self, resource, label) -> None:
        self.resource : str = resource
        self.label : str = label

    def clone(self) -> 'Identifier':
        return Identifier(self.resource, self.label)

    def __eq__(self, value: object) -> bool:
        if isinstance(value, Identifier):
            return self.resource == value.resource
        return self.resource == value
    
    def __hash__(self) -> int:
        return hash(self.resource)

class BlockStateOption:

    def __init__(self, value, description) -> None:
        self.value : str = value
        self.description : str = description
        self.javaValue : str = None
    
    def clone(self) -> 'BlockStateOption':
        clone = BlockStateOption(self.value, self.description)
        clone.javaValue = self.javaValue
        return clone
    
    def isSolved(self):
        return self.javaValue is not None
    
    def trySolving(self, javaValues : list['BlockStateOption']):
        # Simplest case, it's the same name
        for javaValue in javaValues:
            if javaValue.value == self.value:
                self.javaValue = javaValue.value
                return
        
        if self.value in BEDROCK_GENERIC_VALUES_TO_JAVA:
            # Try one of the other values
            for value in BEDROCK_GENERIC_VALUES_TO_JAVA[self.value]:
                for javaValue in javaValues:
                    if javaValue.value == value:
                        self.javaValue = javaValue.value
                        return
        
        # Now try finding a match with the best similarity
        bestMatch = None
        bestSimilarity = 0.0
        nameTokens = getNameTokens(self.value + "_" + self.description)
        for javaValue in javaValues:
            javaNameTokens = getNameTokens(javaValue.value + "_" + javaValue.description)
            similarity = getSimilarity(nameTokens, javaNameTokens)
            if similarity > bestSimilarity:
                bestMatch = javaValue
                bestSimilarity = similarity
        
        if bestMatch is not None:
            self.javaValue = bestMatch.value
            return
        
        # Next simplest case, it's the same description
        for javaValue in javaValues:
            if javaValue.description == self.description:
                self.javaValue = javaValue.value
                return

        # No good match, so just use the first possible value
        self.javaValue = javaValues[0].value
    
    def __eq__(self, value: object) -> bool:
        if isinstance(value, BlockStateOption):
            return self.value == value.value
        return self.value == value
    
    def __hash__(self) -> int:
        return hash(self.value)

class BlockState:

    def __init__(self, name, values, defaultValue) -> None:
        self.name : str = name
        self.values : list[BlockStateOption] = values
        self.defaultValue : str = defaultValue
        self.mapping : dict[str, dict[str,str]] = None
        self.value : str = None
    
    def clone(self) -> 'BlockState':
        values = []
        for value in self.values:
            values.append(value.clone())
        clone = BlockState(self.name, values, self.defaultValue)
        clone.mapping = self.mapping
        if self.value is not None:
            clone.value = str(self.value)
        return clone

    def getOption(self, name : str) -> BlockStateOption:
        for option in self.values:
            if option.value == name:
                return option
        return None

    def isSolved(self):
        return self.mapping is not None
    
    def isIdentityMapping(self):
        """Returns true if the mapping doesn't actually change anything"""
        if self.mapping is None:
            return True
        if isinstance(self.mapping, dict):
            for value, javaState in self.mapping.items():
                for javaName, javaValue in javaState.items():
                    if javaName != self.name or javaValue != value:
                        return False
            return True
        else:
            return self.mapping == self.name
    
    def simplifyMapping(self):
        if self.mapping is None:
            return
        javaName2 = None
        if isinstance(self.mapping, dict):
            for bedrockValue, javaState in self.mapping.items():
                if len(javaState) > 1:
                    return
                for javaName, javaValue in javaState.items():
                    if javaName2 is None:
                        javaName2 = javaName
                    else:
                        if javaName2 != javaName:
                            return
                    if javaValue != bedrockValue:
                        return
        
        # This mapping is just a rename, so we just have to put in the new name.
        self.mapping = javaName2

    def trySolving(self, javaBlockStates : set['BlockState']):
        # If we have a manual mapping, then use that
        if self.name in BEDROCK_VALUES_TO_JAVA:
            self.mapping = BEDROCK_VALUES_TO_JAVA[self.name]
            # Set the appropriate java block state mappings to say that those are solved
            for map in self.mapping.values():
                for javaName in map.keys():
                    for javaBlockState in javaBlockStates:
                        if javaBlockState.name == javaName:
                            javaBlockState.mapping = {}
            return

        # First try the simple case, same name and same possible values
        for javaBlockState in javaBlockStates:
            if javaBlockState.name == self.name:
                if len(javaBlockState.values) == len(self.values):
                    same = True
                    for value in self.values:
                        for javaValue in javaBlockState.values:
                            if javaValue.value == value:
                                value.javaValue = javaValue.value
                                break
                        if value.javaValue is None:
                            same = False
                            break
                    if same:
                        # Same name and same possible values,
                        self.mapping = {}
                        for value in self.values:
                            self.mapping[value.value] = { javaBlockState.name: value.javaValue }

                        javaBlockState.mapping = {}
                        return
                    else:
                        # Make sure to reset value.javaValue
                        for value in self.values:
                            value.javaValue = None
        
        if self.name in BEDROCK_STATES_TO_JAVA:
            # If it's none, then it means it needs to remain unsolved
            if BEDROCK_STATES_TO_JAVA[self.name] is None:
                return
            # Try one of the alternate names
            for name in BEDROCK_STATES_TO_JAVA[self.name]:
                for javaBlockState in javaBlockStates:
                    if javaBlockState.name == name:
                        self.mapping = {}
                        for value in self.values:
                            value.trySolving(javaBlockState.values)
                            self.mapping[value.value] = { javaBlockState.name: value.javaValue }
                        javaBlockState.mapping = {}
                        return
            if len(BEDROCK_STATES_TO_JAVA[self.name]) == 0:
                # Empty list means don't map it
                self.mapping = {}
                return

        # Check the similarity between different names
        bestJavaBlockStates : list[BlockState] = []
        bestSimilarity = 0.0
        nameTokens = getNameTokens(self.name)
        for javaBlockState in javaBlockStates:
            javaNameTokens = getNameTokens(javaBlockState.name)
            similarity = getSimilarity(nameTokens, javaNameTokens)
            if similarity > bestSimilarity:
                bestJavaBlockStates = [ javaBlockState ]
                bestSimilarity = similarity
            elif similarity == bestSimilarity:
                bestJavaBlockStates.append(javaBlockState)
        
        if bestSimilarity > 0.0 and len(bestJavaBlockStates) == 1:
            javaBlockState = bestJavaBlockStates[0]
            # We have found a block state to map with, so now it's mapping the values
            self.mapping = {}
            for value in self.values:
                value.trySolving(javaBlockState.values)
                self.mapping[value.value] = { javaBlockState.name: value.javaValue }

            javaBlockState.mapping = {}

            return
        
        print("No good mapping found for block state", self.name, "best options are", bestJavaBlockStates)
    
    def __eq__(self, value: object) -> bool:
        if isinstance(value, BlockState):
            return self.name == value.name
        return self.name == value
    
    def __hash__(self) -> int:
        return hash(self.name)

class Block:

    def __init__(self, identifier : Identifier) -> None:
        self.bedrockIdentifier : Identifier = identifier
        self.javaIdentifiers : set[Identifier] = set()
        self.bedrockBlockStates : set[BlockState] = set()
        self.javaBlockStates : set[BlockState] = set()
    
    def clone(self) -> 'Block':
        clone = Block(self.bedrockIdentifier.clone())
        for javaIdentifier in self.javaIdentifiers:
            clone.javaIdentifiers.add(javaIdentifier.clone())
        for bedrockBlockState in self.bedrockBlockStates:
            clone.bedrockBlockStates.add(bedrockBlockState.clone())
        for javaBlockState in self.javaBlockStates:
            clone.javaBlockStates.add(javaBlockState.clone())
        return clone

    def solveBlockStates(self):
        for blockState in self.bedrockBlockStates:
            blockState.trySolving(self.javaBlockStates)
            blockState.simplifyMapping()
        
    def getPermutations(self) -> list['Block']:
        """Returns all possible permutations of bedrock block states
        that haven't been mapped to java block states.
        
        Generally, they haven't been mapped, because Java Edition then
        stores that block state via different resource identifiers,
        rather than a block state."""
        res : list[Block] = []

        unsolvedBlockStates : list[BlockState] = self.getUnsolvedBedrockBlockStates()
        lenUnsolvedBlockStates = len(unsolvedBlockStates)
        def addPermutations(block : Block, depth : int):
            if depth >= lenUnsolvedBlockStates:
                res.append(block)
                return
            # None
            block2 = block.clone()
            block2.getBedrockBlockState(unsolvedBlockStates[depth].name).value = None
            addPermutations(block2, depth + 1)

            # Possible values
            for value in unsolvedBlockStates[depth].values:
                block2 = block.clone()
                block2.getBedrockBlockState(unsolvedBlockStates[depth].name).value = str(value.value)
                addPermutations(block2, depth + 1)
            
        addPermutations(self, 0)

        return res

    def solveJavaIdentifier(self):
        if len(self.javaIdentifiers) <= 1:
            return # There is already only one option, so no need to do anything.
            
        # Find the best match based on similarity.
        # We include the values of unsolved block states to
        # increase the chances of finding the right match
        nameString = self.bedrockIdentifier.resource + "_" + self.bedrockIdentifier.label
        unsolvedBlockStates : list[BlockState] = self.getUnsolvedBedrockBlockStates()
        for unsovledBlockState in unsolvedBlockStates:
            if unsovledBlockState.value is not None:
                # We duplicate the state value because it's more important.
                nameString = nameString + "_" + unsovledBlockState.value \
                    + "_" + unsovledBlockState.value + "_" + unsovledBlockState.value
        nameTokens = getNameTokens(nameString)
        bestMatch = None
        bestSimilarity = 0.0
        for javaIdentifier in self.javaIdentifiers:
            javaNameTokens = getNameTokens(javaIdentifier.resource + "_" + javaIdentifier.label)
            similarity = getSimilarity(nameTokens, javaNameTokens)
            if similarity > bestSimilarity:
                bestMatch = javaIdentifier
                bestSimilarity = similarity
        
        if bestMatch is not None:
            self.javaIdentifiers = set()
            self.javaIdentifiers.add(bestMatch)
            return
        
        # Simple case first, there is a resource identifier that matches
        for javaIdentifier in self.javaIdentifiers:
            if javaIdentifier.resource == self.bedrockIdentifier.resource:
                self.javaIdentifiers = set()
                self.javaIdentifiers.add(javaIdentifier)
                return

        # Next simple case, there is a label that matches
        for javaIdentifier in self.javaIdentifiers:
            if javaIdentifier.label == self.bedrockIdentifier.label:
                self.javaIdentifiers = set()
                self.javaIdentifiers.add(javaIdentifier)
                return

        print("Could not find mapping for block", self.bedrockIdentifier.resource)
    
    def solveJavaBlockStates(self):
        unsolvedBlockStates : list[BlockState] = self.getUnsolvedJavaBlockStates()

        nameTokens = getNameTokens(self.bedrockIdentifier.resource + "_" + self.bedrockIdentifier.label)
        for blockState in unsolvedBlockStates:
            # We need to find a value for it.
            # First see if something matches with the block state name
            stateNameTokens = getNameTokens(blockState.name)
            similarity = getSimilarity(nameTokens, stateNameTokens)
            bestMatch = None
            bestSimilarity = 0.0
            if similarity > 0.5:
                # We have a match, if one of the supported values is 1 or true,
                # then set that
                for value in blockState.values:
                    if value.value == "1" or value.value == "true":
                        bestMatch = value
                        bestSimilarity = similarity * 4.0
                        break
            
            for value in blockState.values:
                valueNameTokens = getNameTokens(value.value)
                similarity = getSimilarity(nameTokens, valueNameTokens)
                if similarity > bestSimilarity:
                    bestMatch = value
                    bestSimilarity = similarity
            if bestMatch is not None:
                blockState.value = str(bestMatch.value)
            else:
                blockState.value = blockState.defaultValue

    def getBedrockBlockState(self, name) -> BlockState:
        for blockState in self.bedrockBlockStates:
            if blockState.name == name:
                return blockState
        return None
    
    def getJavaBlockState(self, name) -> BlockState:
        for blockState in self.javaBlockStates:
            if blockState.name == name:
                return blockState
        return None

    def getUnsolvedBedrockBlockStates(self) -> list[BlockState]:
        res = []
        for blockState in self.bedrockBlockStates:
            if not blockState.isSolved():
                res.append(blockState)
        return res

    def getUnsolvedJavaBlockStates(self) -> list[BlockState]:
        res = []
        for blockState in self.javaBlockStates:
            if not blockState.isSolved():
                res.append(blockState)
        return res
    
    def toDict(self) -> dict:
        condition : dict[str, str] = {}
        unsolvedBedrockBlockStates = self.getUnsolvedBedrockBlockStates()
        for blockState in unsolvedBedrockBlockStates:
            if blockState.value is not None:
                condition[blockState.name] = blockState.value
        
        if len(self.javaIdentifiers) == 0:
            return None
        javaName = next(iter(self.javaIdentifiers)).resource

        mapping : dict[str, dict[str, dict[str, str]]] = {}
        for blockState in self.bedrockBlockStates:
            if blockState.isSolved():
                if len(blockState.mapping) > 0 and not blockState.isIdentityMapping():
                    mapping[blockState.name] = blockState.mapping
        
        constants : dict[str, str] = {}
        unsolvedJavaBlockStates = self.getUnsolvedJavaBlockStates()
        for blockState in unsolvedJavaBlockStates:
            constants[blockState.name] = blockState.value
        
        if javaName == self.bedrockIdentifier.resource and len(mapping) == 0 and len(constants) == 0:
            return None # We're not changing anything, so don't export a mapping

        res = {}
        if len(condition) > 0:
            res["condition"] = condition
        res["javaName"] = javaName
        if len(mapping) > 0:
            res["mapping"] = mapping
        if len(constants) > 0:
            res["constants"] = constants

        return res


BLOCKS : dict[str, Block] = {}

def parseBlockStates(path : str) -> tuple[set[BlockState], set[BlockState]]:
    article : str = wiki.getArticle(path)
    foundJavaEdition = False
    foundBedrockEdition = False
    foundBlockState = False
    depth = 0
    startIndex = 0
    i = 0
    length = len(article)

    javaBlockStateStrings : list[str] = []
    bedrockBlockStateStrings : list[str] = []

    while i < length:
        if article[i:i+6].lower() == "{{je}}":
            foundJavaEdition = True
            foundBedrockEdition = False
            i += 6
            continue
        elif article[i:i+9].lower() == "{{el|je}}":
            foundJavaEdition = True
            foundBedrockEdition = False
            i += 9
            continue
        elif article[i:i+16].lower() == "{{edition|java}}":
            foundJavaEdition = True
            foundBedrockEdition = False
            i += 16
            continue
        elif article[i:i+6].lower() == "{{be}}":
            foundBedrockEdition = True
            foundJavaEdition = False
            i += 6
            continue
        elif article[i:i+9].lower() == "{{el|be}}":
            foundBedrockEdition = True
            foundJavaEdition = False
            i += 9
            continue
        elif article[i:i+19].lower() == "{{edition|bedrock}}":
            foundBedrockEdition = True
            foundJavaEdition = False
            i += 19
            continue
        if article[i:i+6] == "{{bst|":
            startIndex = i + 6
            foundBlockState = True
            i += 6
            depth = 0
            continue

        if foundBlockState:
            if article[i:i+2] == "{{":
                depth += 1
            if article[i:i+2] == "}}":
                depth -= 1
                if depth < 0:
                    if foundJavaEdition:
                        javaBlockStateStrings.append(article[startIndex:i])
                    if foundBedrockEdition:
                        bedrockBlockStateStrings.append(article[startIndex:i])
                    foundBlockState = False

        i += 1
    
    def parseString(string : str, resSet : set[BlockState], isBedrock : bool):
        def removePipes(matchObj):
            return matchObj.group(0).replace("|", "_")
        string = string.replace("\n", "")
        string = re.sub("{{[\\w\\|\\s\\-=]+}}", removePipes, string)
        string = re.sub("\\[\\[[\\w\\|\\s\\-=]+\\]\\]", removePipes, string)
        tokens = string.split("|")
        itemCountPerValue = 2
        for token in tokens:
            if "showaux=1" in token or token.startswith("bits=") or token == "Unsupported":
                itemCountPerValue = 3
                break
        stateName : str = ""
        stateValues : list[BlockStateOption] = []
        stateDefaultValue : str = ""
        i = 0
        j = 0
        length = len(tokens)
        while i < length:
            if "=" in tokens[i]:
                i += 1
                continue
            if j == 0:
                stateName = tokens[i].strip()
                i += 1
                j += 1
                continue
            if j == 1:
                stateDefaultValue = tokens[i].strip()
                i += 1
                j += 1
                continue
            if j > 1:
                if (i + 2) <= length:
                    values = tokens[i]
                    for value in values.split(","):
                        if " - " in value:
                            # It's a range
                            try:
                                rangeTokens = value.split(" - ")
                                startI = int(rangeTokens[0])
                                endI = int(rangeTokens[1])
                                for k in range(startI, endI+1):
                                    stateValues.append(BlockStateOption(str(k), tokens[i+1]))
                            except:
                                pass
                        elif " to " in value:
                            # It's a range
                            try:
                                rangeTokens = value.split(" to ")
                                startI = int(rangeTokens[0])
                                endI = int(rangeTokens[1])
                                for k in range(startI, endI+1):
                                    stateValues.append(BlockStateOption(str(k), tokens[i+1]))
                            except:
                                pass
                        else:
                            stateValues.append(BlockStateOption(value.strip(), tokens[i+1]))
                            if isBedrock:
                                # In bedrock, booleans are often stored as a TAG_Byte whose
                                # value is either 0 or 1, so also add those in.
                                if value.strip() == "false":
                                    stateValues.append(BlockStateOption("0", tokens[i+1]))
                                elif value.strip() == "true":
                                    stateValues.append(BlockStateOption("1", tokens[i+1]))
                i += itemCountPerValue
                continue
        
        if stateName == "waterlogged":
            return # We ignore waterlogged because Bedrock saves that on a separate layer in the chunk data.

        resSet.add(BlockState(stateName, stateValues, stateDefaultValue))


    javaBlockStates = set()
    bedrockBlockStates = set()
    for string in javaBlockStateStrings:
        parseString(string, javaBlockStates, False)
    for string in bedrockBlockStateStrings:
        parseString(string, bedrockBlockStates, True)
    return (javaBlockStates, bedrockBlockStates)


def parseBlockArticle(articleName : str, article : str):
    foundIds = False
    foundJavaEdition = False
    foundBedrockEdition = False
    displayName = None
    identifier = None
    blockStatePath = None
    foundBlockStates = False
    skipItem = False

    javaValues : list[tuple[str,str]] = []
    bedrockValues : list[tuple[str,str]] = []

    for line in article.splitlines():
        if "=== ID ===" in line:
            foundIds = True
            continue
        if "=== Block states ===" in line:
            foundBlockStates = True
            continue
        if foundBlockStates:
            if line.startswith("{{"):
                if "see also" in line.lower():
                    if "}}{{" in line.replace(" ", ""):
                        line = line.split("}}")[1]
                    else:
                        continue
                if "{{in|" in line.lower():
                    continue
                blockStatePath = line.replace("{{", "").replace("}}", "").replace(" ", "%20")
                if blockStatePath.startswith(":"):
                    blockStatePath = blockStatePath[1:]
                elif blockStatePath.startswith("/"):
                    blockStatePath = articleName.replace(" ", "%20") + blockStatePath
                blockStatePath = blockStatePath.split("#")[0]
                if blockStatePath.lower().startswith("main"):
                    blockStatePath = blockStatePath.split("|")[1] + "/BS"
                foundBlockStates = False
            continue
        if foundIds == False:
            continue
        if line.startswith("== "):
            foundIds = False
            continue
        if "{{ID table" in line:
            displayName = None
            identifier = None
        if line.startswith("|edition="):
            if "java" in line:
                foundJavaEdition = True
                foundBedrockEdition = False
                skipItem = False
            if "bedrock" in line:
                foundBedrockEdition = True
                foundJavaEdition = False
                skipItem = False
        if line.startswith("|displayname="):
            displayName = line[(line.find("=")+1):].replace("}}", "").split("{{")[0].strip()
        if line.startswith("|nameid="):
            identifier = line[(line.find("=")+1):].replace("}}", "").split("{{")[0].strip()
        if line.startswith("|notnamespaced"):
            skipItem = True
        if displayName is not None and identifier is not None and (foundJavaEdition or foundBedrockEdition):
            if skipItem is False:
                if foundJavaEdition:
                    javaValues.append((identifier, displayName))
                if foundBedrockEdition:
                    bedrockValues.append((identifier, displayName))
            skipItem = False
            displayName = None
            identifier = None
    
    javaBlockStates = set()
    bedrockBlockStates = set()
    if blockStatePath is not None:
        javaBlockStates, bedrockBlockStates = parseBlockStates(blockStatePath)

    for bedrockIdentifier, bedrockDisplayName in bedrockValues:
        block = Block(Identifier(bedrockIdentifier, bedrockDisplayName))
        for javaIdentifier, javaDisplayName in javaValues:
            block.javaIdentifiers.add(Identifier(javaIdentifier, javaDisplayName))
        for blockState in javaBlockStates:
            block.javaBlockStates.add(blockState.clone())
        for blockState in bedrockBlockStates:
            block.bedrockBlockStates.add(blockState.clone())
        if bedrockIdentifier not in BLOCKS:
            BLOCKS[bedrockIdentifier] = block
        else:
            existingBlock = BLOCKS[bedrockIdentifier]
            if existingBlock.bedrockIdentifier.label != bedrockDisplayName:
                existingBlock.bedrockIdentifier.label = commonTokens(existingBlock.bedrockIdentifier.label, bedrockDisplayName)
            existingBlock.javaIdentifiers.update(block.javaIdentifiers)
            existingBlock.javaBlockStates.update(block.javaBlockStates)
            existingBlock.bedrockBlockStates.update(block.bedrockBlockStates)

def run():
    # First retrieve the data from the Wiki
    blockArticles = getAllBlockArticles()
    numArticles = len(blockArticles)
    numChunks = int(math.ceil(float(numArticles) / 30.0))
    for chunk in range(numChunks):
        startIndex = chunk * 30
        endIndex = min((chunk+1) * 30, numArticles)
        articleTexts : list[tuple[str,str]] = wiki.getArticles(blockArticles[startIndex:endIndex])
        for articleText in articleTexts:
            parseBlockArticle(articleText[0], articleText[1])
        print(endIndex, "/", numArticles)

    # Now let's actually go through the parsed data.
    # First, let's get rid of any blocks without block states and whose names already match
    bedrockIdentifiers = set(BLOCKS.keys())
    for bedrockIdentifier in bedrockIdentifiers:
        block = BLOCKS[bedrockIdentifier]
        if len(block.javaBlockStates) == 0 and len(block.bedrockBlockStates) == 0:
            if len(block.javaIdentifiers) == 1:
                if block.bedrockIdentifier in block.javaIdentifiers:
                    BLOCKS.pop(bedrockIdentifier)
    
    fullBlocks : list[Block] = []
    for block in BLOCKS.values():
        block.solveBlockStates()
        permutations = block.getPermutations()
        for permutation in permutations:
            permutation.solveJavaIdentifier()
            permutation.solveJavaBlockStates()
            alreadyAdded = False
            for edgeCase in BLOCK_EDGE_CASES:
                res = edgeCase(permutation)
                if res is not None:
                    for item in res:
                        fullBlocks.append(item)
                    alreadyAdded = True

            if not alreadyAdded:
                if len(permutation.bedrockBlockStates) == 0 and len(permutation.javaBlockStates) == 0:
                    if permutation.bedrockIdentifier in permutation.javaIdentifiers:
                        continue # Same resource name and no block states to convert, so leave it out
                fullBlocks.append(permutation)

    translationMap : dict[str, list] = {}
    for block in fullBlocks:
        translation = block.toDict()
        if translation is not None:
            if block.bedrockIdentifier.resource not in translationMap:
                translationMap[block.bedrockIdentifier.resource] = []
            translationMap[block.bedrockIdentifier.resource].append(translation)

    with open("./miex_bedrock_blocks.json", 'w', encoding='utf-8') as f:
        json.dump(translationMap, f, indent=4)

run()