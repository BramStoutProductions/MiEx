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
	public static List<String> doubleSided = new ArrayList<String>();
	public static List<String> randomAnimationXZOffset = new ArrayList<String>();
	public static List<String> randomAnimationYOffset = new ArrayList<String>();
	public static List<String> lodNoUVScale = new ArrayList<String>();
	public static Map<String, Integer> lodPriority = new HashMap<String, Integer>();
	
	public static boolean removeCaves = false;
	public static boolean fillInCaves = false;
	public static boolean onlyIndividualBlocks = false;
	public static float fgFullnessThreshold = 0.25f;
	public static float bgFullnessThreshold = 0.05f;
	public static int chunkSize = 4;
	public static int biomeBlendRadius = 4;
	public static int removeCavesSearchRadius = 4;
	public static int removeCavesSearchEnergy = 5;
	public static float animatedTexturesFrameTimeMultiplier = 1.0f;
	
	private static void parseList(String key, JsonObject data, List<String> list) {
		if(data.has(key + ".remove")) {
			for(JsonElement e : data.get(key + ".remove").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				list.remove(name);
			}
		}
		if(data.has(key + ".add")) {
			for(JsonElement e : data.get(key + ".add").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				if(!list.contains(name))
					list.add(name);
			}
		}
		if(data.has(key)) {
			list.clear();
			for(JsonElement e : data.get(key).getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				list.add(name);
			}
		}
	}
	
	private static void parseMap(String key, JsonObject data, Map<String, Integer> map) {
		if(data.has(key + ".remove")) {
			for(JsonElement e : data.get(key + ".remove").getAsJsonArray().asList()) {
				String name = e.getAsString();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				map.remove(key);
			}
		}
		if(data.has(key + ".add")) {
			for(Entry<String, JsonElement> e : data.get(key + ".add").getAsJsonObject().entrySet()) {
				String name = e.getKey();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				int val = e.getValue().getAsInt();
				map.put(name, val);
			}
		}
		if(data.has(key)) {
			map.clear();
			for(Entry<String, JsonElement> e : data.get(key).getAsJsonObject().entrySet()) {
				String name = e.getKey();
				if(!name.contains(":"))
					name = "minecraft:" + name;
				int val = e.getValue().getAsInt();
				map.put(name, val);
			}
		}
	}
	
	public static void load() {
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
		doubleSided.clear();
		randomAnimationXZOffset.clear();
		randomAnimationYOffset.clear();
		lodNoUVScale.clear();
		lodPriority.clear();
		
		fgFullnessThreshold = 0.25f;
		bgFullnessThreshold = 0.05f;
		chunkSize = 4;
		biomeBlendRadius = 4;
		removeCavesSearchRadius = 4;
		removeCavesSearchEnergy = 5;
		animatedTexturesFrameTimeMultiplier = 1.0f;
		
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
				
				parseList("doubleSided", data, doubleSided);
				
				parseList("randomAnimationXZOffset", data, randomAnimationXZOffset);
				
				parseList("randomAnimationYOffset", data, randomAnimationYOffset);
				
				parseMap("lodPriority", data, lodPriority);
				
				parseList("lodNoUVScale", data, lodNoUVScale);
				
				if(data.has("fgFullnessThreshold"))
					fgFullnessThreshold = data.get("fgFullnessThreshold").getAsFloat();
	
				if(data.has("bgFullnessThreshold"))
					bgFullnessThreshold = data.get("bgFullnessThreshold").getAsFloat();
				
				if(data.has("chunkSize"))
					chunkSize = data.get("chunkSize").getAsInt();
				
				if(data.has("biomeBlendRadius"))
					biomeBlendRadius = data.get("biomeBlendRadius").getAsInt();
				
				if(data.has("removeCavesSearchRadius"))
					removeCavesSearchRadius = data.get("removeCavesSearchRadius").getAsInt();
				
				if(data.has("removeCavesSearchEnergy"))
					removeCavesSearchEnergy = data.get("removeCavesSearchEnergy").getAsInt();
				
				if(data.has("animatedTexturesFrameTimeMultiplier"))
					animatedTexturesFrameTimeMultiplier = data.get("animatedTexturesFrameTimeMultiplier").getAsFloat();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
