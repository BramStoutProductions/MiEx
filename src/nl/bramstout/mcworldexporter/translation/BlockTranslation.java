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

package nl.bramstout.mcworldexporter.translation;

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
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class BlockTranslation {
	
	private static class Condition{
		
		Map<String, String> values;
		boolean optional;
		
		public Condition() {
			this.values = new HashMap<String, String>();
			this.optional = false;
		}
		
		public int matches(NbtTagCompound properties) {
			int matches = 0;
			for(Entry<String, String> value : values.entrySet()) {
				NbtTag valueTag = properties.get(value.getKey());
				if(valueTag == null) {
					if(optional)
						continue;
					return -1;
				}
				String valueStr = valueTag.asString();
				if(valueStr.equals("false"))
					valueStr = "0";
				if(valueStr.equals("true"))
					valueStr = "1";
				
				if(!valueStr.equals(value.getValue())) {
					return optional ? 0 : -1;
				}
				matches++;
			}
			return matches;
		}
		
	}
	
	private static class Mapping{
		
		String name;
		String nameMapping;
		Map<String, Map<String, String>> valueMapping;
		
		public Mapping(String name) {
			this.name = name;
			nameMapping = null;
			valueMapping = null;
		}
		
		public void map(NbtTagCompound properties) {
			if(nameMapping != null) {
				NbtTag property = properties.get(name);
				if(property != null) {
					NbtTag copy = property.copy();
					copy.setName(nameMapping);
					properties.addElement(copy);
				}
			}
			if(valueMapping != null) {
				NbtTag property = properties.get(name);
				if(property == null)
					return;
				
				Map<String, String> valueMap = valueMapping.get(property.asString());
				if(valueMap == null)
					return;
				for(Entry<String, String> value : valueMap.entrySet()) {
					NbtTagString valueTag = NbtTagString.newInstance(value.getKey(), value.getValue());
					properties.addElement(valueTag);
				}
			}
		}
		
	}
	
	private static class Constant{
		
		private String name;
		private String value;
		
		public Constant(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		public void map(NbtTagCompound properties) {
			NbtTagString valueTag = NbtTagString.newInstance(name, value);
			properties.addElement(valueTag);
		}
		
	}
	
	private static class BlockItem{
		
		Condition condition;
		Condition optionalCondition;
		String javaName;
		List<Mapping> mappings;
		List<Constant> constants;
		
		public BlockItem() {
			this.condition = new Condition();
			this.optionalCondition = new Condition();
			this.optionalCondition.optional = true;
			this.javaName = null;
			this.mappings = new ArrayList<Mapping>();
			this.constants = new ArrayList<Constant>();
		}
		
		public int matches(NbtTagCompound properties) {
			int matchStrength1 = condition.matches(properties);
			int matchStrength2 = optionalCondition.matches(properties);
			if(matchStrength1 < 0 || matchStrength2 < 0)
				return -1;
			return matchStrength1 + matchStrength2;
		}
		
		public void map(NbtTagCompound properties) {
			for(Mapping mapping : mappings)
				mapping.map(properties);
			for(Constant constant : constants)
				constant.map(properties);
		}
		
	}
	
	private static class BlockTranslator{
		
		public Map<String, List<BlockItem>> translationMap;
		public int minDataVersion;
		public int maxDataVersion;
		
		public BlockTranslator(){
			this.translationMap = new HashMap<String, List<BlockItem>>();
			this.minDataVersion = Integer.MIN_VALUE;
			this.maxDataVersion = Integer.MAX_VALUE;
		}
		
		public boolean has(String name) {
			return translationMap.containsKey(name);
		}
		
		/**
		 * Maps the block to Java Edition. It returns the new name
		 * and it will modify properties directly.
		 * 
		 * @param name
		 * @param properties
		 * @return
		 */
		public String map(String name, NbtTagCompound properties) {
			List<BlockItem> items = translationMap.getOrDefault(name, null);
			if(items == null)
				return name;
			
			BlockItem map = null;
			int maxMatchStrength = -1;
			for(BlockItem item : items) {
				int matchStrength = item.matches(properties);
				if(matchStrength > maxMatchStrength) {
					map = item;
					maxMatchStrength = matchStrength;
				}
			}
			
			if(map == null)
				return name;
			
			map.map(properties);
			if(map.javaName == null)
				return name;
			return map.javaName;
		}
		
	}
	
	public static class BlockTranslatorManager{
		
		private List<BlockTranslator> translators;
		
		public BlockTranslatorManager() {
			translators = new ArrayList<BlockTranslator>();
		}
		
		/**
		 * Maps the block to Java Edition. It returns the new name
		 * and it will modify properties directly.
		 * 
		 * @param name
		 * @param properties
		 * @return
		 */
		public String map(String name, NbtTagCompound properties) {
			for(int i = 0; i < translators.size(); ++i) {
				BlockTranslator translator = translators.get(i);
				if(translator.has(name))
					return translator.map(name, properties);
			}
			return name;
		}
	}
	
	private String sourceName;
	private List<BlockTranslator> translators;
	private Map<String, List<String>> blockNameAliases;
	
	public BlockTranslation(String sourceName) {
		this.sourceName = sourceName;
		this.translators = new ArrayList<BlockTranslator>();
		this.blockNameAliases = new HashMap<String, List<String>>();
	}
	
	public void load() {
		List<BlockTranslator> translators = new ArrayList<BlockTranslator>();
		Map<String, List<String>> blockNameAliases = new HashMap<String, List<String>>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_blocks.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement translatorData : data.asList()) {
					if(!translatorData.isJsonObject())
						continue;
					int minDataVersion = Integer.MIN_VALUE;
					int maxDataVersion = Integer.MAX_VALUE;
					if(translatorData.getAsJsonObject().has("minDataVersion"))
						minDataVersion = translatorData.getAsJsonObject().get("minDataVersion").getAsInt();
					if(translatorData.getAsJsonObject().has("maxDataVersion"))
						maxDataVersion = translatorData.getAsJsonObject().get("maxDataVersion").getAsInt();
					if(translatorData.getAsJsonObject().has("translations")) {
						BlockTranslator translator = new BlockTranslator();
						translator.minDataVersion = minDataVersion;
						translator.maxDataVersion = maxDataVersion;
						translators.add(0, translator);
						
						for(Entry<String, JsonElement> entry : translatorData.getAsJsonObject().getAsJsonObject("translations").entrySet()) {
							String blockName = entry.getKey();
							if(!blockName.contains(":"))
								blockName = "minecraft:" + blockName;
							
							List<BlockItem> items = translator.translationMap.get(blockName);
							if(items == null) {
								items = new ArrayList<BlockItem>();
								translator.translationMap.put(blockName, items);
							}
							
							JsonArray blocks = entry.getValue().getAsJsonArray();
							for(JsonElement blockEl : blocks.asList()) {
								JsonObject block = blockEl.getAsJsonObject();
								BlockItem item = new BlockItem();
								if(block.has("name")) {
									item.javaName = block.get("name").getAsString();
									if(!item.javaName.contains(":"))
										item.javaName = "minecraft:" + item.javaName;
								}
								if(block.has("condition")) {
									for(Entry<String, JsonElement> condition : block.get("condition").getAsJsonObject().entrySet()) {
										String valueStr = condition.getValue().getAsString();
										if(valueStr.equals("false"))
											valueStr = "0";
										if(valueStr.equals("true"))
											valueStr = "1";
										item.condition.values.put(condition.getKey(), valueStr);
									}
								}
								if(block.has("optionalCondition")) {
									for(Entry<String, JsonElement> condition : block.get("optionalCondition").getAsJsonObject().entrySet()) {
										String valueStr = condition.getValue().getAsString();
										if(valueStr.equals("false"))
											valueStr = "0";
										if(valueStr.equals("true"))
											valueStr = "1";
										item.optionalCondition.values.put(condition.getKey(), valueStr);
									}
								}
								if(block.has("mapping")) {
									for(Entry<String, JsonElement> mapping : block.get("mapping").getAsJsonObject().entrySet()) {
										Mapping map = new Mapping(mapping.getKey());
										if(mapping.getValue().isJsonPrimitive()) {
											map.nameMapping = mapping.getValue().getAsString();
										}else if(mapping.getValue().isJsonObject()) {
											map.valueMapping = new HashMap<String, Map<String, String>>();
											JsonObject valueMap = mapping.getValue().getAsJsonObject();
											for(Entry<String, JsonElement> valueMapping : valueMap.entrySet()) {
												Map<String, String> values = new HashMap<String, String>();
												if(valueMapping.getValue().isJsonObject()) {
													for(Entry<String, JsonElement> value : valueMapping.getValue().getAsJsonObject().entrySet()) {
														values.put(value.getKey(), value.getValue().getAsString());
													}
												}
												map.valueMapping.put(valueMapping.getKey(), values);
											}
										}
										item.mappings.add(map);
									}
								}
								if(block.has("constants")) {
									for(Entry<String, JsonElement> constant : block.get("constants").getAsJsonObject().entrySet()) {
										item.constants.add(new Constant(constant.getKey(), constant.getValue().getAsString()));
									}
								}
								items.add(item);
								
								if(item.javaName != null) {
									List<String> aliases = blockNameAliases.getOrDefault(item.javaName, null);
									if(aliases == null) {
										aliases = new ArrayList<String>();
										blockNameAliases.put(item.javaName, aliases);
									}
									if(!aliases.contains(blockName))
										aliases.add(blockName);
								}
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		this.translators = translators;
		this.blockNameAliases = blockNameAliases;
	}
	
	public BlockTranslatorManager getTranslator(int dataVersion) {
		BlockTranslatorManager manager = new BlockTranslatorManager();
		for(BlockTranslator translator : translators)
			if(dataVersion >= translator.minDataVersion && dataVersion <= translator.maxDataVersion)
				manager.translators.add(translator);
		return manager;
	}
	
	public List<String> getAliasesForBlock(String name){
		return blockNameAliases.getOrDefault(name, null);
	}
	
}
