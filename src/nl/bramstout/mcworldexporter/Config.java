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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.entity.builtins.EntityBuiltinsRegistry;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockStateRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.bedrock.BedrockMaterials;
import nl.bramstout.mcworldexporter.resourcepack.bedrock.ResourcePackBedrockEdition;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.anvil.chunkreader.BiomeIds;
import nl.bramstout.mcworldexporter.world.anvil.chunkreader.BlockIds;
import nl.bramstout.mcworldexporter.world.bedrock.BedrockBiomes;

public class Config {
	
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
	public static List<String> ignoreAtlas = new ArrayList<String>();
	public static Map<String, Integer> lodPriority = new HashMap<String, Integer>();
	
	public static boolean removeCaves = false;
	public static boolean fillInCaves = false;
	public static boolean onlyIndividualBlocks = false;
	public static boolean runOptimiser;
	public static boolean runRaytracingOptimiser;
	public static boolean runFaceOptimiser;
	public static boolean raytracingOptimiserUseMeshSubsets;
	public static float fgFullnessThreshold;
	public static float bgFullnessThreshold;
	public static int chunkSize;
	public static int defaultChunkSize;
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
	public static boolean subdivideModelsForCorners;
	public static String renderGamut;
	public static int memoryPerThread;
	public static boolean forceDoubleSidedOnEverything;
	public static float minCubeSize;
	public static int maxMaterialNameLength;
	public static boolean useGeometerySubsets;
	
	private static void parseList(String key, JsonObject data, List<String> list) {
		if(data.has(key + ".remove")) {
			for(JsonElement e : data.get(key + ".remove").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						list.remove(name2);
				}else {
					if(!name.contains(":"))
						name = "minecraft:" + name;
					list.remove(name);
				}
			}
		}
		if(data.has(key + ".add")) {
			for(JsonElement e : data.get(key + ".add").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						if(!list.contains(name2))
							list.add(name2);
				}else {
					if(!name.contains(":"))
						name = "minecraft:" + name;
					if(!list.contains(name))
						list.add(name);
				}
			}
		}
		if(data.has(key)) {
			list.clear();
			for(JsonElement e : data.get(key).getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						list.add(name2);
				}else {
					if(!name.contains(":"))
						name = "minecraft:" + name;
					list.add(name);
				}
			}
		}
	}
	
	private static void parseMap(String key, JsonObject data, Map<String, Integer> map) {
		if(data.has(key + ".remove")) {
			for(JsonElement e : data.get(key + ".remove").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						map.remove(name2);
				}else{
					if(!name.contains(":"))
						name = "minecraft:" + name;
					map.remove(key);
				}
			}
		}
		if(data.has(key + ".add")) {
			for(Entry<String, JsonElement> e : data.get(key + ".add").getAsJsonObject().entrySet()) {
				String name = e.getKey();
				int val = e.getValue().getAsInt();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						map.put(name2, val);
				}else{
					if(!name.contains(":"))
						name = "minecraft:" + name;
					map.put(name, val);
				}
			}
		}
		if(data.has(key)) {
			map.clear();
			for(Entry<String, JsonElement> e : data.get(key).getAsJsonObject().entrySet()) {
				String name = e.getKey();
				int val = e.getValue().getAsInt();
				if(name.startsWith("#")) {
					List<String> names = Tags.getNamesInTag(name);
					for(String name2 : names)
						map.put(name2, val);
				}else {
					if(!name.contains(":"))
						name = "minecraft:" + name;
					map.put(name, val);
				}
			}
		}
	}
	
	public static void load() {
		Tags.load();
		Tints.load();
		ConnectedTextures.load();
		TranslationRegistry.load();
		BedrockBiomes.load();
		BedrockMaterials.load();
		BiomeIds.load();
		BlockIds.load();
		
		boolean updateChunkSize = defaultChunkSize == chunkSize;
		int oldChunkSize = chunkSize;
		
		ConfigDefaults.loadDefaults();
		
		if(!updateChunkSize)
			chunkSize = oldChunkSize;
		// We only tint faces with biome colours if they have tintindex specified
		// in the block model file, but in MiEx we don't use the grass_block_side_overlay
		// as a seprate mesh like Minecraft does. Instead, we composite it over the texture
		// in the material. This means that we need the biome tint on the side of the
		// block, which Minecraft by default doesn't provide. In previous versions of MiEx
		// we apply biome colours on every face. By having it now only on tintindex to make
		// it closer to how Minecraft does it, it will break the grass side.
		// Most people don't delete their miex_config.json when updating which would really
		// break things. Therefore, we add it in here just to avoid giving people headaches.
		forceBiomeColor.add("minecraft:block/grass_block_side");
		
		
		Color.GAMUT = ColorGamut.ACEScg;
		for(ColorGamut gamut : ColorGamut.values()) {
			if(gamut.name().equalsIgnoreCase(renderGamut)) {
				Color.GAMUT = gamut;
				break;
			}
		}
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			try {
				File configFile = new File(resourcePacks.get(i).getFolder(), "miex_config.json");
				if(!configFile.exists())
					continue;
				JsonObject data = Json.read(configFile).getAsJsonObject();
				
				parseList("liquid", data, liquid);
				
				parseList("waterlogged", data, waterlogged);
				
				parseList("transparentOcclusion", data, transparentOcclusion);
				
				parseList("leavesOcclusion", data, leavesOcclusion);
				
				parseList("detailedOcclusion", data, detailedOcclusion);
				
				parseList("noOcclusion", data, noOcclusion);
				
				parseList("bannedMaterials", data, bannedMaterials);
				
				parseList("individualBlocks", data, individualBlocks);
				
				parseList("caveBlocks", data, caveBlocks);
				
				parseList("randomOffset", data, randomOffset);
				
				parseList("randomYOffset", data, randomYOffset);
				
				parseList("grassColormapBlocks", data, grassColormapBlocks);
				
				parseList("foliageColormapBlocks", data, foliageColormapBlocks);
				
				parseList("waterColormapBlocks", data, waterColormapBlocks);
				
				parseList("forceBiomeColor", data, forceBiomeColor);
				
				parseList("forceNoBiomeColor", data, forceNoBiomeColor);
				
				parseList("doubleSided", data, doubleSided);
				
				parseList("randomAnimationXZOffset", data, randomAnimationXZOffset);
				
				parseList("randomAnimationYOffset", data, randomAnimationYOffset);
				
				parseMap("lodPriority", data, lodPriority);
				
				parseList("lodNoUVScale", data, lodNoUVScale);
				
				parseList("ignoreAtlas", data, ignoreAtlas);
				
				if(data.has("runOptimiser"))
					runOptimiser = data.get("runOptimiser").getAsBoolean();
				
				if(data.has("runRaytracingOptimiser"))
					runRaytracingOptimiser = data.get("runRaytracingOptimiser").getAsBoolean();
				
				if(data.has("runFaceOptimiser"))
					runFaceOptimiser = data.get("runFaceOptimiser").getAsBoolean();
				
				if(data.has("raytracingOptimiserUseMeshSubsets"))
					raytracingOptimiserUseMeshSubsets = data.get("raytracingOptimiserUseMeshSubsets").getAsBoolean();
				
				if(data.has("fgFullnessThreshold"))
					fgFullnessThreshold = data.get("fgFullnessThreshold").getAsFloat();
	
				if(data.has("bgFullnessThreshold"))
					bgFullnessThreshold = data.get("bgFullnessThreshold").getAsFloat();
				
				if(data.has("chunkSize") && updateChunkSize) {
					chunkSize = data.get("chunkSize").getAsInt();
					defaultChunkSize = chunkSize;
				}
				
				if(data.has("biomeBlendRadius"))
					biomeBlendRadius = data.get("biomeBlendRadius").getAsInt();
				
				if(data.has("removeCavesSearchRadius"))
					removeCavesSearchRadius = data.get("removeCavesSearchRadius").getAsInt();
				
				if(data.has("removeCavesSearchEnergy"))
					removeCavesSearchEnergy = data.get("removeCavesSearchEnergy").getAsInt();
				
				if(data.has("removeCavesSurfaceRadius"))
					removeCavesSurfaceRadius = data.get("removeCavesSurfaceRadius").getAsInt();
				
				if(data.has("removeCavesAirCost"))
					removeCavesAirCost = data.get("removeCavesAirCost").getAsInt();
				
				if(data.has("removeCavesCaveBlockCost"))
					removeCavesCaveBlockCost = data.get("removeCavesCaveBlockCost").getAsInt();
				
				if(data.has("animatedTexturesFrameTimeMultiplier"))
					animatedTexturesFrameTimeMultiplier = data.get("animatedTexturesFrameTimeMultiplier").getAsFloat();
				
				if(data.has("renderGamut")) {
					renderGamut = data.get("renderGamut").getAsString();
					boolean foundGamut = false;
					for(ColorGamut gamut : ColorGamut.values()) {
						if(gamut.name().equalsIgnoreCase(renderGamut)) {
							Color.GAMUT = gamut;
							foundGamut = true;
							break;
						}
					}
					if(foundGamut == false)
						System.out.println("Found invalid render gamut in config: " + renderGamut);
				}
				
				if(data.has("blockSizeInUnits"))
					blockSizeInUnits = data.get("blockSizeInUnits").getAsFloat();
				
				if(data.has("atlasMaxResolution"))
					atlasMaxResolution = data.get("atlasMaxResolution").getAsInt();
				
				if(data.has("atlasMaxTileResolution"))
					atlasMaxTileResolution = data.get("atlasMaxTileResolution").getAsInt();
				
				if(data.has("exportVertexColorAsDisplayColor"))
					exportVertexColorAsDisplayColor = data.get("exportVertexColorAsDisplayColor").getAsBoolean();
				
				if(data.has("exportDisplayColor"))
					exportDisplayColor = data.get("exportDisplayColor").getAsBoolean();
				
				if(data.has("vertexColorGamma"))
					vertexColorGamma = data.get("vertexColorGamma").getAsFloat();
				
				if(data.has("calculateAmbientOcclusion"))
					calculateAmbientOcclusion = data.get("calculateAmbientOcclusion").getAsBoolean();
				
				if(data.has("exportAmbientOcclusionAsDisplayOpacity"))
					exportAmbientOcclusionAsDisplayOpacity = data.get("exportAmbientOcclusionAsDisplayOpacity").getAsBoolean();
				
				if(data.has("calculateCornerUVs"))
					calculateCornerUVs = data.get("calculateCornerUVs").getAsBoolean();
				
				if(data.has("subdivideModelsForCorners"))
					subdivideModelsForCorners = data.get("subdivideModelsForCorners").getAsBoolean();
				
				if(data.has("memoryPerThread"))
					memoryPerThread = data.get("memoryPerThread").getAsInt();
				
				if(data.has("forceDoubleSidedOnEverything"))
					forceDoubleSidedOnEverything = data.get("forceDoubleSidedOnEverything").getAsBoolean();
				
				if(data.has("minCubeSize"))
					minCubeSize = data.get("minCubeSize").getAsFloat();
				
				if(data.has("maxMaterialNameLength"))
					maxMaterialNameLength = data.get("maxMaterialNameLength").getAsInt();
				
				if(data.has("useGeometerySubsets"))
					useGeometerySubsets = data.get("useGeometerySubsets").getAsBoolean();
				
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		transparentOcclusion.addAll(ResourcePackBedrockEdition.transparentBlocks);
		BuiltInBlockStateRegistry.load();
		EntityBuiltinsRegistry.load();
	}
	
}
