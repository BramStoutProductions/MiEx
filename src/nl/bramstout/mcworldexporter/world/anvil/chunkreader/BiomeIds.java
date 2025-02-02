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

package nl.bramstout.mcworldexporter.world.anvil.chunkreader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class BiomeIds {
	
	private static class Mapping{
		public int minDataVersion;
		public int maxDataVersion;
		public Map<Integer, String> translationMap = new HashMap<Integer, String>();
	}
	
	private static List<Mapping> mappings = new ArrayList<Mapping>();
	
	public static void load() {
		List<Mapping> mappings = new ArrayList<Mapping>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/java/biome_ids.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement entry : data.asList()) {
					if(!entry.isJsonObject())
						continue;
					
					int minDataVersion = entry.getAsJsonObject().get("minDataVersion").getAsInt();
					int maxDataVersion = entry.getAsJsonObject().get("maxDataVersion").getAsInt();
					
					Mapping mapping = null;
					for(Mapping mapping2 : mappings) {
						if(mapping2.minDataVersion == minDataVersion && mapping2.maxDataVersion == maxDataVersion) {
							mapping = mapping2;
							break;
						}
					}
					if(mapping == null) {
						mapping = new Mapping();
						mapping.minDataVersion = minDataVersion;
						mapping.maxDataVersion = maxDataVersion;
						mappings.add(0, mapping);
					}
					
					JsonArray mappingArray = entry.getAsJsonObject().getAsJsonArray("mapping");
					for(JsonElement el : mappingArray.asList()) {
						int biomeId = el.getAsJsonObject().get("id").getAsInt();
						
						String biomeName = el.getAsJsonObject().get("name").getAsString();
						if(!biomeName.contains(":"))
							biomeName = "minecraft:" + biomeName;
						
						mapping.translationMap.put(biomeId, biomeName);
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BiomeIds.mappings = mappings;
	}
	
	public static String getNameForId(int id, int dataVersion) {
		Integer idInstance = Integer.valueOf(id);
		for(int i = 0; i < mappings.size(); ++i) {
			Mapping mapping = mappings.get(i);
			if(dataVersion < mapping.minDataVersion || dataVersion > mapping.maxDataVersion)
				continue;
			String name = mapping.translationMap.getOrDefault(idInstance, null);
			if(name != null)
				return name;
		}
		World.handleError(new Exception("No mapping for biome id " + id + " for data version " + dataVersion));
		return "plains";
	}
	
	public static int getRuntimeIdForId(int id, int dataVersion) {
		String name = getNameForId(id, dataVersion);
		name = TranslationRegistry.BIOME_JAVA.map(name);
		return BiomeRegistry.getIdForName(name);
	}
	
}
