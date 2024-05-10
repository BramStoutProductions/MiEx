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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures;

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
	public static List<String> doubleSided = new ArrayList<String>();
	public static List<String> randomAnimationXZOffset = new ArrayList<String>();
	public static List<String> randomAnimationYOffset = new ArrayList<String>();
	public static List<String> lodNoUVScale = new ArrayList<String>();
	public static Map<String, Integer> lodPriority = new HashMap<String, Integer>();
	
	public static boolean removeCaves = false;
	public static boolean fillInCaves = false;
	public static boolean onlyIndividualBlocks = false;
	public static boolean runOptimiser = true;
	public static boolean runRaytracingOptimiser = true;
	public static boolean runFaceOptimiser = true;
	public static float fgFullnessThreshold = 0.25f;
	public static float bgFullnessThreshold = 0.05f;
	public static int chunkSize = 4;
	public static int defaultChunkSize = 4;
	public static int biomeBlendRadius = 4;
	public static int removeCavesSearchRadius = 4;
	public static int removeCavesSearchEnergy = 5;
	public static float animatedTexturesFrameTimeMultiplier = 1.0f;
	public static float blockSizeInUnits = 16.0f;
	public static int atlasMaxResolution = 4096;
	public static int atlasMaxTileResolution = 256;
	
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
		
		liquid.clear();
		waterlogged.clear();
		transparentOcclusion.clear();
		leavesOcclusion.clear();
		detailedOcclusion.clear();
		noOcclusion.clear();
		bannedMaterials.clear();
		individualBlocks.clear();
		caveBlocks.clear();
		randomOffset.clear();
		randomYOffset.clear();
		grassColormapBlocks.clear();
		foliageColormapBlocks.clear();
		waterColormapBlocks.clear();
		forceBiomeColor.clear();
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
		doubleSided.clear();
		randomAnimationXZOffset.clear();
		randomAnimationYOffset.clear();
		lodNoUVScale.clear();
		lodPriority.clear();
		
		fgFullnessThreshold = 0.25f;
		bgFullnessThreshold = 0.05f;
		boolean updateChunkSize = defaultChunkSize == chunkSize;
		if(updateChunkSize)
			chunkSize = 4;
		biomeBlendRadius = 4;
		removeCavesSearchRadius = 4;
		removeCavesSearchEnergy = 5;
		animatedTexturesFrameTimeMultiplier = 1.0f;
		blockSizeInUnits = 16.0f;
		atlasMaxResolution = 4096;
		atlasMaxTileResolution = 256;
		
		Color.GAMUT = ColorGamut.ACEScg;
		
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			try {
				String configName = FileUtil.getResourcePackDir() + resourcePacks.get(i) + "/miex_config.json";
				if(!new File(configName).exists())
					continue;
				JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(new File(configName))))).getAsJsonObject();
				
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
				
				parseList("doubleSided", data, doubleSided);
				
				parseList("randomAnimationXZOffset", data, randomAnimationXZOffset);
				
				parseList("randomAnimationYOffset", data, randomAnimationYOffset);
				
				parseMap("lodPriority", data, lodPriority);
				
				parseList("lodNoUVScale", data, lodNoUVScale);
				
				if(data.has("runOptimiser"))
					runOptimiser = data.get("runOptimiser").getAsBoolean();
				
				if(data.has("runRaytracingOptimiser"))
					runRaytracingOptimiser = data.get("runRaytracingOptimiser").getAsBoolean();
				
				if(data.has("runFaceOptimiser"))
					runFaceOptimiser = data.get("runFaceOptimiser").getAsBoolean();
				
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
				
				if(data.has("animatedTexturesFrameTimeMultiplier"))
					animatedTexturesFrameTimeMultiplier = data.get("animatedTexturesFrameTimeMultiplier").getAsFloat();
				
				if(data.has("renderGamut")) {
					String gamutName = data.get("renderGamut").getAsString();
					boolean foundGamut = false;
					for(ColorGamut gamut : ColorGamut.values()) {
						if(gamut.name().equalsIgnoreCase(gamutName)) {
							Color.GAMUT = gamut;
							foundGamut = true;
							break;
						}
					}
					if(foundGamut == false)
						System.out.println("Found invalid render gamut in config: " + gamutName);
				}
				
				if(data.has("blockSizeInUnits"))
					blockSizeInUnits = data.get("blockSizeInUnits").getAsFloat();
				
				if(data.has("atlasMaxResolution"))
					atlasMaxResolution = data.get("atlasMaxResolution").getAsInt();
				
				if(data.has("atlasMaxTileResolution"))
					atlasMaxTileResolution = data.get("atlasMaxTileResolution").getAsInt();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
