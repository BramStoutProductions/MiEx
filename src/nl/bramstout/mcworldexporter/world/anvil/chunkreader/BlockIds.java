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
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class BlockIds {
	
	public static class BlockDefinition{
		public String name;
		public NbtTagCompound properties;
		
		public BlockDefinition() {
			name = "";
			properties = NbtTagCompound.newInstance("");
		}
	}
	
	private static class Mapping{
		public int minDataVersion;
		public int maxDataVersion;
		public Map<Integer, BlockDefinition[]> translationMap = new HashMap<Integer, BlockDefinition[]>();
	}
	
	private static List<Mapping> mappings = new ArrayList<Mapping>();
	
	public static void load() {
		mappings.clear();
		ID_CACHE.clear();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/java/block_ids.json");
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
						int blockId = el.getAsJsonObject().get("id").getAsInt();
						
						String defaultBlockName = el.getAsJsonObject().get("name").getAsString();
						if(!defaultBlockName.contains(":"))
							defaultBlockName = "minecraft:" + defaultBlockName;
						
						JsonObject defaultBlockState = null;
						if(el.getAsJsonObject().has("blockState"))
							defaultBlockState = el.getAsJsonObject().getAsJsonObject("blockState");
						
						BlockDefinition[] definitions = mapping.translationMap.getOrDefault(Integer.valueOf(blockId), null);
						if(definitions == null) {
							definitions = new BlockDefinition[16];
							mapping.translationMap.put(Integer.valueOf(blockId), definitions);
						}
						for(int j = 0; j < definitions.length; ++j) {
							if(definitions[j] == null)
								definitions[j] = new BlockDefinition();
							definitions[j].name = defaultBlockName;
							parseBlockState(definitions[j], defaultBlockState);
						}
						
						if(el.getAsJsonObject().has("variants")) {
							for(JsonElement variant : el.getAsJsonObject().getAsJsonArray("variants").asList()) {
								if(!variant.isJsonObject())
									continue;
								int dataValue = -1;
								if(variant.getAsJsonObject().has("dataValue"))
									dataValue = variant.getAsJsonObject().get("dataValue").getAsInt();
								if(dataValue < 0 || dataValue >= definitions.length)
									continue;
								
								BlockDefinition definition = definitions[dataValue];
								
								if(variant.getAsJsonObject().has("name")) {
									definition.name = variant.getAsJsonObject().get("name").getAsString();
									if(!definition.name.contains(":"))
										definition.name = "minecraft:" + definition.name;
								}
								
								if(variant.getAsJsonObject().has("blockState"))
									parseBlockState(definition, variant.getAsJsonObject().getAsJsonObject("blockState"));
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private static void parseBlockState(BlockDefinition definition, JsonObject blockState) {
		if(blockState == null)
			return;
		for(Entry<String, JsonElement> entry : blockState.entrySet()) {
			if(!entry.getValue().isJsonPrimitive())
				continue;
			String value = entry.getValue().getAsString();
			definition.properties.addElement(NbtTagString.newInstance(entry.getKey(), value));
		}
	}

	private static BlockDefinition AIR_BLOCK = new BlockDefinition();
	static {
		AIR_BLOCK.name = "minecraft:air";
	}
	
	public static BlockDefinition getBlockDefinitionForId(int id, int data, int dataVersion) {
		Integer idInstance = Integer.valueOf(id);
		for(int i = 0; i < mappings.size(); ++i) {
			Mapping mapping = mappings.get(i);
			if(dataVersion < mapping.minDataVersion || dataVersion > mapping.maxDataVersion)
				continue;
			BlockDefinition[] definitions = mapping.translationMap.getOrDefault(idInstance, null);
			if(definitions == null)
				continue;
			return definitions[Math.min(Math.max(data, 0), 15)];
		}
		World.handleError(new RuntimeException("Could not find block name for id " + id + " and data " + data + " with data version " + dataVersion));
		System.out.println("Could not find block name for id " + id + " and data " + data + " with data version " + dataVersion);
		return AIR_BLOCK;
	}
	
	private static Map<Long, Integer> ID_CACHE = new HashMap<Long, Integer>();
	
	public static void clear() {
		synchronized(ID_CACHE) {
			ID_CACHE.clear();
		}
	}
	
	public static int getRuntimeIdForId(int id, int data, int dataVersion, BlockTranslatorManager blockTranslatorManager, 
										Reference<char[]> charBuffer) {
		long idKey = ((long) id) | (((long) data) << 32) | (((long) dataVersion) << 40);
		Long idKeyInstance = Long.valueOf(idKey);
		Integer runtimeId = ID_CACHE.getOrDefault(idKeyInstance, null);
		if(runtimeId != null) {
			return runtimeId.intValue();
		}
		synchronized(ID_CACHE) {
			runtimeId = ID_CACHE.getOrDefault(idKeyInstance, null);
			if(runtimeId != null)
				return runtimeId.intValue();
			
			BlockDefinition block = getBlockDefinitionForId(id, data, dataVersion);
			NbtTagCompound properties = (NbtTagCompound) block.properties.copy();
			String newName = blockTranslatorManager.map(block.name, properties);
			int runtimeId2 = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
			
			ID_CACHE.put(idKeyInstance, runtimeId2);
			
			return runtimeId2;
		}
	}
	
}
