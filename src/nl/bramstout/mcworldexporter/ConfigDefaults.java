/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;

public class ConfigDefaults {
	
	public static List<String> liquid = new ArrayList<String>();
	public static List<String> waterlogged = new ArrayList<String>();
	public static List<String> transparentOcclusion = new ArrayList<String>();
	public static List<String> leavesOcclusion = new ArrayList<String>();
	public static List<String> detailedOcclusion = new ArrayList<String>();
	public static List<String> noOcclusion = new ArrayList<String>();
	public static List<String> bannedMaterials = new ArrayList<String>();
	public static List<String> individualBlocks = new ArrayList<String>();
	public static List<String> caveBlocks = new ArrayList<String>();
	public static List<String> randomOffset = new ArrayList<String>();
	public static List<String> randomYOffset = new ArrayList<String>();
	public static List<String> grassColormapBlocks = new ArrayList<String>();
	public static List<String> foliageColormapBlocks = new ArrayList<String>();
	public static List<String> waterColormapBlocks = new ArrayList<String>();
	public static List<String> forceBiomeColor = new ArrayList<String>();
	public static List<String> forceNoBiomeColor = new ArrayList<String>();
	public static List<String> doubleSided = new ArrayList<String>();
	public static List<String> randomAnimationXZOffset = new ArrayList<String>();
	public static List<String> randomAnimationYOffset = new ArrayList<String>();
	public static List<String> lodNoUVScale = new ArrayList<String>();
	public static Map<String, Integer> lodPriority = new HashMap<String, Integer>();
	
	public static boolean runOptimiser;
	public static boolean runRaytracingOptimiser;
	public static boolean runFaceOptimiser ;
	public static float fgFullnessThreshold;
	public static float bgFullnessThreshold;
	public static int chunkSize;
	public static int biomeBlendRadius;
	public static int removeCavesSearchRadius;
	public static int removeCavesSearchEnergy;
	public static int removeCavesSurfaceRadius;
	public static int removeCavesAirCost;
	public static int removeCavesCaveBlockCost;
	public static float animatedTexturesFrameTimeMultiplier;
	public static float blockSizeInUnits;
	public static int atlasMaxResolution;
	public static int atlasMaxTileResolution;
	public static boolean exportVertexColorAsDisplayColor;
	public static boolean exportDisplayColor;
	public static float vertexColorGamma;
	public static boolean calculateAmbientOcclusion;
	public static boolean exportAmbientOcclusionAsDisplayOpacity;
	public static boolean calculateCornerUVs;
	public static String renderGamut;
	public static int memoryPerThread;
	public static boolean forceDoubleSidedOnEverything;
	public static float minCubeSize;
	public static int maxMaterialNameLength;
	
	static {
		liquid.addAll(Arrays.asList(
				"minecraft:water", "minecraft:lava"));
		waterlogged.addAll(Arrays.asList(
				"minecraft:bubble_column", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass", "minecraft:tall_seagrass",
				"minecraft:brain_coral", "minecraft:brain_coral_fan", "minecraft:brain_coral_wall_fan", "minecraft:bubble_coral",
				"minecraft:bubble_coral_fan", "minecraft:bubble_coral_wall_fan", "minecraft:dead_brain_coral", 
				"minecraft:dead_brain_coral_fan", "minecraft:dead_brain_coral_wall_fan", "minecraft:dead_bubble_coral", 
				"minecraft:dead_bubble_coral_fan", "minecraft:dead_bubble_coral_wall_fan", "minecraft:dead_fire_coral", 
				"minecraft:dead_fire_coral_fan", "minecraft:dead_fire_coral_wall_fan", "minecraft:dead_horn_coral", 
				"minecraft:dead_horn_coral_fan", "minecraft:dead_horn_coral_wall_fan", "minecraft:dead_tube_coral", 
				"minecraft:dead_tube_coral_fan", "minecraft:dead_tube_coral_wall_fan", "minecraft:fire_coral", 
				"minecraft:fire_coral_fan", "minecraft:fire_coral_wall_fan", "minecraft:horn_coral", "minecraft:horn_coral_fan", 
				"minecraft:horn_coral_wall_fan", "minecraft:tube_coral", "minecraft:tube_coral_fan", "minecraft:tube_coral_wall_fan"));
		transparentOcclusion.addAll(Arrays.asList());
		leavesOcclusion.addAll(Arrays.asList());
		detailedOcclusion.addAll(Arrays.asList(
				"minecraft:acacia_fence", "minecraft:acacia_fence_gate", "minecraft:acacia_trapdoor", "minecraft:birch_fence", 
				"minecraft:birch_fence_gate", "minecraft:birch_trapdoor", "minecraft:black_carpet", 
				"minecraft:black_stained_glass_pane", "minecraft:blue_carpet", "minecraft:blue_stained_glass_pane", 
				"minecraft:brick_wall", "minecraft:brown_carpet", "minecraft:brown_stained_glass_pane", "minecraft:cactus", 
				"minecraft:chest", "minecraft:trapped_chest", "minecraft:cobbled_deepslate_wall", "minecraft:cobblestone_wall", 
				"minecraft:crimson_fence", "minecraft:crimson_fence_gate", "minecraft:crimson_trapdoor", "minecraft:cyan_carpet", 
				"minecraft:cyan_stained_glass_pane", "minecraft:dark_oak_fence", "minecraft:dark_oak_fence_gate",
				"minecraft:dark_oak_trapdoor", "minecraft:deepslate_brick_wall", "minecraft:deepslate_tile_wall",
				"minecraft:diorite_wall", "minecraft:dirt_path", "minecraft:end_stone_brick_wall", "minecraft:farmland",
				"minecraft:glass_pane", "minecraft:granite_wall", "minecraft:gray_carpet", "minecraft:gray_stained_glass_pane",
				"minecraft:green_carpet", "minecraft:green_stained_glass_pane", "minecraft:hopper", "minecraft:iron_bars",
				"minecraft:iron_trapdoor", "minecraft:jungle_fence", "minecraft:jungle_fence_gate", "minecraft:jungle_trapdoor",
				"minecraft:light_blue_carpet", "minecraft:light_blue_stained_glass_pane", "minecraft:light_gray_carpet",
				"minecraft:light_gray_stained_glass_pane", "minecraft:lime_carpet", "minecraft:lime_stained_glass_pane",
				"minecraft:magenta_carpet", "minecraft:magenta_stained_glass_pane", "minecraft:moss_carpet",
				"minecraft:mossy_cobblestone_wall", "minecraft:mossy_stone_brick_wall", "minecraft:nether_brick_fence",
				"minecraft:nether_brick_wall", "minecraft:nether_portal", "minecraft:oak_fence", "minecraft:oak_fence_gate",
				"minecraft:oak_trapdoor", "minecraft:orange_carpet", "minecraft:orange_stained_glass_pane", "minecraft:pink_carpet",
				"minecraft:pink_stained_glass_pane", "minecraft:piston_head", "minecraft:polished_blackstone_brick_wall",
				"minecraft:polished_blackstone_wall", "minecraft:polished_deepslate_wall", "minecraft:prismarine_wall",
				"minecraft:purple_carpet", "minecraft:purple_stained_glass_pane", "minecraft:red_carpet", 
				"minecraft:red_nether_brick_wall", "minecraft:red_sandstone_wall", "minecraft:red_stained_glass_pane",
				"minecraft:repeater", "minecraft:sandstone_wall", "minecraft:scaffolding", "minecraft:sculk_sensor", "minecraft:snow", 
				"minecraft:soul_sand", "minecraft:spruce_fence", "minecraft:spruce_fence_gate", "minecraft:spruce_trapdoor",
				"minecraft:stone_brick_wall", "minecraft:warped_fence", "minecraft:warped_fence_gate", "minecraft:warped_trapdoor",
				"minecraft:white_carpet", "minecraft:white_stained_glass_pane", "minecraft:yellow_carpet",
				"minecraft:yellow_stained_glass_pane", "minecraft:moss_carpet", "minecraft:pale_moss_carpet"));
		noOcclusion.addAll(Arrays.asList(
				"minecraft:azalea", "minecraft:beacon"));
		bannedMaterials.addAll(Arrays.asList(
				"minecraft:block/grass_block_side_overlay"));
		individualBlocks.addAll(Arrays.asList(
				"minecraft:chest", "minecraft:iron_door", "minecraft:oak_door", "minecraft:spruce_door", 
				"minecraft:birch_door", "minecraft:jungle_door", "minecraft:acacia_door", "minecraft:dark_oak_door", 
				"minecraft:mangrove_door", "minecraft:crimson_door", "minecraft:warped_door"));
		caveBlocks.addAll(Arrays.asList(
				"stone", "dirt", "cobblestone", "gravel", "gold_ore", "iron_ore", "coal_ore", "lapis_ore", "cobweb", 
				"planks", "grass", "grass_block", "short_grass", "tall_grass", "brown_mushroom", "red_mushroom", "mossy_cobblestone", 
				"mob_spawner", "sand", "glass", "glass_pane", "redstone_repeater", "redstone_lamp", "lever", "redstone_wire",
				"obsidian", "chest", "diamond_ore", "rail", "redstone_ore", "oak_fence", "emerald_ore", "copper_ore",
				"deepslate_gold_ore", "deepslate_iron_ore", "deepslate_coal_ore", "deepslate_lapis_ore",
				"deepslate_diamond_ore", "deepslate_redstone_ore", "deepslate_emerald_ore", "deepslate_copper_ore",
				"deepslate", "granite", "tuff", "diorite", "pointed_dripstone", "dripstone_block", "amethyst_cluster",
				"amethyst_block", "calcite", "smooth_basalt", "basalt", "glow_lichen", "andesite", "small_amethyst_bud",
				"large_amethyst_bud", "medium_amethyst_bud", "oak_planks", "budding_amethyst", "bedrock", "oak_log",
				"infested_deepslate", "chain", "spawner", "infested_stone", "wall_torch", "raw_iron_block", "raw_copper_block",
				"oak_leaves", "azalea_leaves", "flowering_azalea_leaves", "azalea", "flowering_azalea", "rooted_dirt",
				"hanging_roots", "moss_block", "moss_carpet", "vine", "clay", "small_dripleaf", "big_dripleaf",
				"cave_vines", "cave_vines_plant", "spore_blossom", "sculk_catalyst", "sculk_shrieker", "sculk_vein",
				"sculk", "sculk_sensor", "magma_block", "bubble_column", "chest", "cobbled_deepslate", "polished_deepslate",
	            "deepslate_bricks", "deepslate_tiles", "chiseled_deepslate", "chiseled_deepslate_bricks", "chiseled_deepslate_tiles",
	            "chiseled_deepslate_slab", "chiseled_deepslate_stairs", "cobbled_deepslate_wall", "polished_deepslate_wall",
	            "polished_deepslate_stairs", "polished_deepslate_slab", "deepslate_brick_wall", "deepslate_brick_slab",
	            "deepslate_brick_stairs", "deepslate_tile_wall", "deepslate_tile_slab", "deepslate_tile_stairs", "gray_wool",
	            "polished_basalt", "smooth_basalt", "dark_oak_log", "dark_oak_planks", "dark_oak_fence", "ladder", "candle",
	            "soul_lantern", "reinforced_deepslate", "soul_fire", "comparator", "redstone_torch", "redstone_wall_torch",
	            "redstone_block", "lectern", "target", "furnace", "sticky_piston", "piston_head", "gray_carpet", "blue_wool", "cyan_wool",
	            "light_blue_wool", "campfire", "iron_trapdoor", "note_block", "stone_pressure_plate", "skeleton_skull", "white_candle",
	            "blue_carpet", "cyan_carpet", "light_blue_carpet", "cracked_deepslate_bricks", "cracked_deepslate_tiles", "cobbled_deepslate_stairs",
	            "cobbled_deepslate_slab", "repeater", "soul_sand", "torch", "wall_torch", "ice", "packed_ice", "blue_ice"));
		randomOffset.addAll(Arrays.asList(
				"grass", "fern", "short_grass", "tall_grass", "large_fern", "dandelion", "poppy", "blue_orchid",
		        "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
		        "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "sunflower",
		        "lilac", "rose_bush", "peony", "lily_pad", "oak_sapling", "spruce_sapling",
		        "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling", 
		        "kelp", "kelp_plant", "crimson_fungus", "warped_fungus", "brown_mushroom", "red_mushroom"));
		randomYOffset.addAll(Arrays.asList(
				"grass", "fern", "short_grass", "tall_grass", "large_fern"));
		grassColormapBlocks.addAll(Arrays.asList(
				"grass_block", "fern", "grass", "short_grass", "tall_grass", "large_fern", "lily_pad", "sugar_cane"));
		foliageColormapBlocks.addAll(Arrays.asList(
				"acacia_leaves", "birch_leaves", "dark_oak_leaves", "jungle_leaves", "mangrove_leaves", "oak_leaves", "spruce_leaves", "vine"));
		waterColormapBlocks.addAll(Arrays.asList(
				"water", "water_cauldron"));
		forceBiomeColor.addAll(Arrays.asList(
				"block/grass_block_side"));
		forceNoBiomeColor.addAll(Arrays.asList(
				"minecraft:powder_snow_cauldron", "minecraft:lava_cauldron", "minecraft:stonecutter", "minecraft:pale_oak_leaves"));
		doubleSided.addAll(Arrays.asList(
				"water", "grass", "fern", "short_grass", "tall_grass", "large_fern", "dandelion", "poppy", "blue_orchid",
		        "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
		        "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "sunflower",
		        "lilac", "rose_bush", "peony", "lily_pad", "oak_sapling", "spruce_sapling",
		        "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling", 
		        "kelp", "kelp_plant", "crimson_fungus", "warped_fungus", "brown_mushroom", "red_mushroom",
		        "acacia_leaves", "azalea_leaves", "cherry_leaves", "birch_leaves", "dark_oak_leaves",
		        "jungle_leaves", "oak_leaves", "slime_block", "spawner", "spruce_leaves",
		        "mangrove_leaves", "flowering_azalea_leaves"));
		randomAnimationXZOffset.addAll(Arrays.asList(
				"kelp", "kelp_plant", "seagrass", "tall_seagrass", "fire", "soul_fire"));
		randomAnimationYOffset.addAll(Arrays.asList(
				"fire", "soul_fire"));
		lodNoUVScale.addAll(Arrays.asList());
		
		lodPriority.put("grass_block", 200);
		lodPriority.put("dirt", 2);
		lodPriority.put("acacia_leaves", 10);
		lodPriority.put("azalea_leaves", 10);
		lodPriority.put("birch_leaves", 10);
		lodPriority.put("dark_oak_leaves", 10);
		lodPriority.put("jungle_leaves", 10);
		lodPriority.put("oak_leaves", 10);
		lodPriority.put("spruce_leaves", 10);
		lodPriority.put("mangrove_leaves", 10);
		lodPriority.put("flowering_azalea_leaves", 10);
		
		runOptimiser = true;
		runRaytracingOptimiser = true;
		runFaceOptimiser = true;
		fgFullnessThreshold = 0.15f;
		bgFullnessThreshold = 0.01f;
		chunkSize = 16;
		biomeBlendRadius = 8;
		removeCavesSearchRadius = 4;
		removeCavesSearchEnergy = 5;
		removeCavesSurfaceRadius = 16;
		removeCavesAirCost = 1;
		removeCavesCaveBlockCost = 5;
		animatedTexturesFrameTimeMultiplier = 1.0f;
		blockSizeInUnits = 16.0f;
		atlasMaxResolution = 4096;
		atlasMaxTileResolution = 256;
		exportVertexColorAsDisplayColor = false;
		exportDisplayColor = true;
		vertexColorGamma = 1f;
		calculateAmbientOcclusion = false;
		exportAmbientOcclusionAsDisplayOpacity = false;
		calculateCornerUVs = false;
		renderGamut = "ACEScg";
		memoryPerThread = 1024;
		forceDoubleSidedOnEverything = false;
		minCubeSize = -1.0f;
		maxMaterialNameLength = -1;
	}
	
	public static void loadDefaults() {
		for(Field defaultField : ConfigDefaults.class.getDeclaredFields()) {
			if(!Modifier.isStatic(defaultField.getModifiers()))
				continue;
			if(!Modifier.isPublic(defaultField.getModifiers()))
				continue;
			
			try {
				Field field = Config.class.getDeclaredField(defaultField.getName());
				
				if(!Modifier.isStatic(field.getModifiers()))
					continue;
				if(!Modifier.isPublic(field.getModifiers()))
					continue;
				
				if(!field.getType().equals(defaultField.getType()))
					continue;
				
				copyField(defaultField, field);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private static void copyField(Field sourceField, Field destField) throws IllegalArgumentException, IllegalAccessException {
		Class<?> type = sourceField.getType();
		if(type.equals(float.class)) {
			destField.setFloat(null, sourceField.getFloat(null));
		}else if(type.equals(double.class)) {
			destField.setDouble(null, sourceField.getDouble(null));
		}else if(type.equals(int.class)) {
			destField.setInt(null, sourceField.getInt(null));
		}else if(type.equals(short.class)) {
			destField.setShort(null, sourceField.getShort(null));
		}else if(type.equals(byte.class)) {
			destField.setByte(null, sourceField.getByte(null));
		}else if(type.equals(long.class)) {
			destField.setLong(null, sourceField.getLong(null));
		}else if(type.equals(boolean.class)) {
			destField.setBoolean(null, sourceField.getBoolean(null));
		}else if(type.equals(String.class)){
			destField.set(null, sourceField.get(null));
		}else if(type.equals(List.class)) {
			@SuppressWarnings("unchecked")
			List<Object> sourceList = (List<Object>) sourceField.get(null);
			@SuppressWarnings("unchecked")
			List<Object> destList = (List<Object>) destField.get(null);
			destList.clear();
			destList.addAll(sourceList);
		}else if(type.equals(Map.class)) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> sourceMap = (Map<Object, Object>) sourceField.get(null);
			@SuppressWarnings("unchecked")
			Map<Object, Object> destMap = (Map<Object, Object>) destField.get(null);
			destMap.clear();
			destMap.putAll(sourceMap);;
		}else {
			throw new RuntimeException("Invalid field type");
		}
	}
	
	public static void createBaseConfigFile() {
		// Uses the defaults written in here to create the miex_config.json in the base resource pack.
		File resourcePacksFolder = new File(FileUtil.getResourcePackDir());
		if(!resourcePacksFolder.isDirectory())
			return;
		File baseResourcePackFolder = new File(resourcePacksFolder, "base_resource_pack");
		if(!baseResourcePackFolder.exists())
			baseResourcePackFolder.mkdirs();
		File configFile = new File(baseResourcePackFolder, "miex_config.json");
		
		File defaultsConfigFile = new File(baseResourcePackFolder, "miex_config_defaults.json");
		if(defaultsConfigFile.exists()) {
			try {
				JsonObject rootData = Json.read(defaultsConfigFile).getAsJsonObject();
				for(Field field : ConfigDefaults.class.getDeclaredFields()) {
					if(!Modifier.isStatic(field.getModifiers()))
						continue;
					if(!Modifier.isPublic(field.getModifiers()))
						continue;
					if(!rootData.has(field.getName()))
						continue;
					try {
						setField(rootData.get(field.getName()), field);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		boolean needsToInferConfig = false;
		
		JsonObject rootData = new JsonObject();
		if(configFile.exists()) {
			try {
				rootData = Json.read(configFile).getAsJsonObject();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}else {
			needsToInferConfig = true;
		}
		
		boolean changedSomething = false;
		for(Field field : ConfigDefaults.class.getDeclaredFields()) {
			if(!Modifier.isStatic(field.getModifiers()))
				continue;
			if(!Modifier.isPublic(field.getModifiers()))
				continue;
			if(rootData.has(field.getName()))
				continue;
			try {
				addField(rootData, field);
				changedSomething = true;
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if(changedSomething || needsToInferConfig) {
			FileWriter writer = null;
			try {
				Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
				String jsonString = gson.toJson(rootData);
				writer = new FileWriter(configFile);
				writer.write(jsonString);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(writer != null) {
				try {
					writer.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		if(needsToInferConfig) {
			ConfigDefaults.loadDefaults();
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.25f);
		    MCWorldExporter.getApp().getUI().getProgressBar().setText("Setting up default config file...");
			ResourcePackDefaults.inferMiExConfigFromResourcePack(baseResourcePackFolder);
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
		    MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		}
	}
	
	private static void addField(JsonObject rootData, Field field) throws IllegalArgumentException, IllegalAccessException {
		Class<?> type = field.getType();
		if(type.equals(float.class)) {
			rootData.addProperty(field.getName(), field.getFloat(null));
		}else if(type.equals(double.class)) {
			rootData.addProperty(field.getName(), field.getDouble(null));
		}else if(type.equals(int.class)) {
			rootData.addProperty(field.getName(), field.getInt(null));
		}else if(type.equals(short.class)) {
			rootData.addProperty(field.getName(), field.getShort(null));
		}else if(type.equals(byte.class)) {
			rootData.addProperty(field.getName(), field.getByte(null));
		}else if(type.equals(long.class)) {
			rootData.addProperty(field.getName(), field.getLong(null));
		}else if(type.equals(boolean.class)) {
			rootData.addProperty(field.getName(), field.getBoolean(null));
		}else if(type.equals(String.class)) {
			rootData.addProperty(field.getName(), (String) field.get(null));
		}else if(type.equals(List.class)) {
			ParameterizedType parameter = (ParameterizedType) field.getGenericType();
			Type[] parameterTypes = parameter.getActualTypeArguments();
			if(parameterTypes.length != 1)
				return;
			Class<?> parameterType = (Class<?>) parameterTypes[0];
			
			if(parameterType.equals(String.class)) {
				@SuppressWarnings("unchecked")
				List<String> data = (List<String>) field.get(null);
				
				JsonArray arrayObj = new JsonArray();
				for(String str : data) {
					if(!str.contains(":"))
						str = "minecraft:" + str;
					arrayObj.add(str);
				}
				
				rootData.add(field.getName(), arrayObj);
			}
			
		}else if(type.equals(Map.class)) {
			ParameterizedType parameter = (ParameterizedType) field.getGenericType();
			Type[] parameterTypes = parameter.getActualTypeArguments();
			if(parameterTypes.length != 2)
				return;
			Class<?> keyType = (Class<?>) parameterTypes[0];
			Class<?> valueType = (Class<?>) parameterTypes[1];
			
			if(keyType.equals(String.class) && valueType.equals(Integer.class)) {
				@SuppressWarnings("unchecked")
				Map<String, Integer> data = (Map<String, Integer>) field.get(null);
				
				JsonObject obj = new JsonObject();
				for(Entry<String, Integer> entry : data.entrySet()) {
					String key = entry.getKey();
					if(!key.contains(":"))
						key = "minecraft:" + key;
					obj.addProperty(key, entry.getValue());
				}
				
				rootData.add(field.getName(), obj);
			}
		}
	}
	
	private static void setField(JsonElement value, Field field) throws IllegalArgumentException, IllegalAccessException {
		Class<?> type = field.getType();
		if(type.equals(float.class)) {
			field.setFloat(null, value.getAsFloat());
		}else if(type.equals(double.class)) {
			field.setDouble(null, value.getAsDouble());
		}else if(type.equals(int.class)) {
			field.setInt(null,  value.getAsInt());
		}else if(type.equals(short.class)) {
			field.setShort(null, value.getAsShort());
		}else if(type.equals(byte.class)) {
			field.setByte(null, value.getAsByte());
		}else if(type.equals(long.class)) {
			field.setLong(null, value.getAsLong());
		}else if(type.equals(boolean.class)) {
			field.setBoolean(null, value.getAsBoolean());
		}else if(type.equals(String.class)) {
			field.set(null, value.getAsString());
		}else if(type.equals(List.class)) {
			ParameterizedType parameter = (ParameterizedType) field.getGenericType();
			Type[] parameterTypes = parameter.getActualTypeArguments();
			if(parameterTypes.length != 1)
				return;
			Class<?> parameterType = (Class<?>) parameterTypes[0];
			
			if(parameterType.equals(String.class)) {
				@SuppressWarnings("unchecked")
				List<String> data = (List<String>) field.get(null);
				data.clear();
				for(JsonElement el : value.getAsJsonArray().asList()) {
					data.add(el.getAsString());
				}
			}
			
		}else if(type.equals(Map.class)) {
			ParameterizedType parameter = (ParameterizedType) field.getGenericType();
			Type[] parameterTypes = parameter.getActualTypeArguments();
			if(parameterTypes.length != 2)
				return;
			Class<?> keyType = (Class<?>) parameterTypes[0];
			Class<?> valueType = (Class<?>) parameterTypes[1];
			
			if(keyType.equals(String.class) && valueType.equals(Integer.class)) {
				@SuppressWarnings("unchecked")
				Map<String, Integer> data = (Map<String, Integer>) field.get(null);
				
				for(Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
					data.put(entry.getKey(), entry.getValue().getAsInt());
				}
			}
		}
	}
	
}
