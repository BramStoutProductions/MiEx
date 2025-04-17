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

package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;
import nl.bramstout.mcworldexporter.resourcepack.PaintingVariant;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.TextureGroup;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;

public class ResourcePackBedrockEdition extends ResourcePack{

	private static Map<String, List<String>> terrain_textures = new HashMap<String, List<String>>();
	private static Map<String, List<String>> item_textures = new HashMap<String, List<String>>();
	private static Map<String, MCMeta> mcmetas = new HashMap<String, MCMeta>();
	private static Map<String, Map<String, String>> blockTextureMappings = new HashMap<String, Map<String, String>>();
	public static List<String> transparentBlocks = new ArrayList<String>();
	private static Map<String, RenderControllerBedrockEdition> renderControllers = new HashMap<String, RenderControllerBedrockEdition>();
	
	private List<File> rootFolders;
	private List<File> reversedRootFolders;
	private Map<String, Biome> biomes;
	private Map<String, BlockStateHandler> blockStateHandlers;
	private Map<String, ModelHandler> modelHandlers;
	private Map<String, List<String>> tags;
	private Map<String, EntityHandler> entityHandlers;
	private Map<String, EntityAIHandler> entityAIHandlers;
	private Map<String, Animation> animations;
	private List<EntitySpawner> entitySpawners;
	private Map<String, Long> minEngineVersions;
	
	public ResourcePackBedrockEdition(File folder) {
		super(getName(folder), getUUID(folder), folder);
		rootFolders = new ArrayList<File>();
		rootFolders.add(folder);
		reversedRootFolders = Util.reverseList(rootFolders);
		biomes = new HashMap<String, Biome>();
		blockStateHandlers = new HashMap<String, BlockStateHandler>();
		modelHandlers = new HashMap<String, ModelHandler>();
		tags = new HashMap<String, List<String>>();
		entityHandlers = new HashMap<String, EntityHandler>();
		entityAIHandlers = new HashMap<String, EntityAIHandler>();
		animations = new HashMap<String, Animation>();
		entitySpawners = new ArrayList<EntitySpawner>();
		minEngineVersions = new HashMap<String, Long>();
	}
	
	@Override
	public List<File> getFolders() {
		return rootFolders;
	}
	
	@Override
	public List<File> getFoldersReversed() {
		return reversedRootFolders;
	}
	
	public static void reset() {
		terrain_textures.clear();
		item_textures.clear();
		mcmetas.clear();
		blockTextureMappings.clear();
		transparentBlocks.clear();
		renderControllers.clear();
	}
	
	private boolean isMostRecent(String identifier, String versionString) {
		String[] versionTokens = versionString.split(".");
		long version = 0;
		for(int i = 0; i < versionTokens.length; ++i) {
			try {
				long number = Long.parseLong(versionTokens[i]);
				long offset = 64 - (12 * (i + 1));
				if(offset < 0)
					break;
				version |= (number & 0xFFF) << offset; 
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		Long currentVersion = minEngineVersions.getOrDefault(identifier, null);
		if(currentVersion == null) {
			minEngineVersions.put(identifier, Long.valueOf(version));
			return true;
		}
		if(version > currentVersion.longValue()) {
			minEngineVersions.put(identifier, Long.valueOf(version));
			return true;
		}
		return false;
	}

	@Override
	public void load() {
		biomes.clear();
		blockStateHandlers.clear();
		modelHandlers.clear();
		tags.clear();
		entityHandlers.clear();
		entityAIHandlers.clear();
		animations.clear();
		entitySpawners.clear();
		minEngineVersions.clear();
		
		File terrain_texture = new File(getFolder(), "textures/terrain_texture.json");
		if(terrain_texture.exists()) {
			try {
				JsonObject data = Json.read(terrain_texture).getAsJsonObject();
				
				JsonObject textureData = data.get("texture_data").getAsJsonObject();
				for(Entry<String, JsonElement> el : textureData.entrySet()) {
					List<String> paths = new ArrayList<String>();
					JsonElement textures = el.getValue().getAsJsonObject().get("textures");
					if(textures.isJsonArray()) {
						for(JsonElement path : textures.getAsJsonArray().asList()) {
							if(path.isJsonObject()) {
								if(path.getAsJsonObject().has("path")) {
									paths.add(path.getAsJsonObject().get("path").getAsString());
								}else if(path.getAsJsonObject().has("variants")) {
									JsonArray variations = path.getAsJsonObject().get("variants").getAsJsonArray();
									for(JsonElement variation : variations.asList()) {
										if(variation.isJsonObject()) {
											JsonObject variationObj = variation.getAsJsonObject();
											if(variationObj.has("path")) {
												paths.add(variationObj.get("path").getAsString());
											}
										}
									}
								}
							}else if(path.isJsonPrimitive()) {
								paths.add(path.getAsString());
							}
						}
					}else if(textures.isJsonPrimitive()) {
						paths.add(textures.getAsString());
					}
					terrain_textures.put(el.getKey(), paths);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		File item_texture = new File(getFolder(), "textures/terrain_texture.json");
		if(item_texture.exists()) {
			try {
				JsonObject data = Json.read(item_texture).getAsJsonObject();
				
				JsonObject textureData = data.get("texture_data").getAsJsonObject();
				for(Entry<String, JsonElement> el : textureData.entrySet()) {
					List<String> paths = new ArrayList<String>();
					JsonElement textures = el.getValue().getAsJsonObject().get("textures");
					if(textures.isJsonArray()) {
						for(JsonElement path : textures.getAsJsonArray().asList()) {
							if(path.isJsonObject()) {
								if(path.getAsJsonObject().has("path")) {
									paths.add(path.getAsJsonObject().get("path").getAsString());
								}else if(path.getAsJsonObject().has("variants")) {
									JsonArray variations = path.getAsJsonObject().get("variants").getAsJsonArray();
									for(JsonElement variation : variations.asList()) {
										if(variation.isJsonObject()) {
											JsonObject variationObj = variation.getAsJsonObject();
											if(variationObj.has("path")) {
												paths.add(variationObj.get("path").getAsString());
											}
										}
									}
								}
							}else if(path.isJsonPrimitive()) {
								paths.add(path.getAsString());
							}
						}
					}else if(textures.isJsonPrimitive()) {
						paths.add(textures.getAsString());
					}
					item_textures.put(el.getKey(), paths);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		File flipbook_textures = new File(getFolder(), "textures/flipbook_textures.json");
		if(flipbook_textures.exists()) {
			try {
				JsonArray data = Json.read(flipbook_textures).getAsJsonArray();
				
				for(JsonElement el : data.asList()) {
					String path = el.getAsJsonObject().get("flipbook_texture").getAsString();
					
					File textureFile = new File(getFolder(), path + ".exr");
					if(!textureFile.exists())
						textureFile = new File(getFolder(), path + ".png");
					if(!textureFile.exists())
						textureFile = new File(getFolder(), path + ".tga");
					if(!textureFile.exists())
						continue;
					
					mcmetas.put(path, new MCMetaBedrockEdition(textureFile, el.getAsJsonObject()));
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		File blocksFile = new File(getFolder(), "blocks.json");
		if(blocksFile.exists()) {
			try {
				JsonObject data = Json.read(blocksFile).getAsJsonObject();
				
				for(Entry<String, JsonElement> el : data.entrySet()) {
					if(!el.getValue().isJsonObject())
						continue;
					if(!el.getValue().getAsJsonObject().has("textures"))
						continue;
					Map<String, String> paths = new HashMap<String, String>();
					JsonElement textures = el.getValue().getAsJsonObject().get("textures");
					if(textures.isJsonObject()) {
						for(Entry<String, JsonElement> path : textures.getAsJsonObject().entrySet()) {
							String varName = path.getKey();
							if(!varName.startsWith("#") && !varName.equals("*"))
								varName = "#" + varName;
							paths.put(varName, path.getValue().getAsString());
						}
					}else if(textures.isJsonPrimitive()) {
						paths.put("*", textures.getAsString());
					}
					String blockName = el.getKey();
					if(blockName.contains(":"))
						blockName = blockName.substring(blockName.indexOf(':') + 1);
					blockTextureMappings.put(blockName, paths);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		File biomesFolder = new File(getFolder(), "biomes");
		if(biomesFolder.exists() && biomesFolder.isDirectory()) {
			for(File f : biomesFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(data.has("minecraft:biome")) {
							JsonObject biomeObj = data.getAsJsonObject("minecraft:biome");
							JsonObject descriptionObj = biomeObj.getAsJsonObject("description");
							String biomeName = descriptionObj.get("identifier").getAsString();
							if(!biomeName.contains(":"))
								biomeName = "minecraft:" + biomeName;
							biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
							biomes.put(biomeName, new BiomeBedrockEdition(biomeName, 0, biomeObj.getAsJsonObject("components")));
							
							JsonObject components = biomeObj.getAsJsonObject("components");
							if(components.has("minecraft:tags")) {
								JsonElement el = components.get("minecraft:tags");
								if(el.isJsonArray()) {
									for(JsonElement tag : el.getAsJsonArray().asList()) {
										String tagName = tag.getAsString();
										if(!tagName.contains(":"))
											tagName = "minecraft:" + tagName;
										tagName.replace(":", ":worldgen/biome");
										
										registerTag(tagName, biomeName);
									}
								}else if(el.isJsonObject()) {
									for(JsonElement tag : el.getAsJsonObject().getAsJsonArray("tags").asList()) {
										String tagName = tag.getAsString();
										if(!tagName.contains(":"))
											tagName = "minecraft:" + tagName;
										tagName.replace(":", ":worldgen/biome");
										
										registerTag(tagName, biomeName);
									}
								}
							}else {
								for(Entry<String, JsonElement> entry : components.entrySet()) {
									if(!entry.getKey().startsWith("minecraft:")) {
										String tagName = entry.getKey();
										if(!tagName.contains(":"))
											tagName = "minecraft:" + tagName;
										tagName.replace(":", ":worldgen/biome");
										registerTag(tagName, biomeName);
									}
								}
							}
						}else {
							for(Entry<String, JsonElement> entry : data.entrySet()) {
								String biomeName = entry.getKey();
								if(!biomeName.contains(":"))
									biomeName = "minecraft:" + biomeName;
								biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
								JsonObject components = entry.getValue().getAsJsonObject();
								biomes.put(biomeName, new BiomeBedrockEdition(biomeName, 0, components));
								
								if(components.has("minecraft:tags")) {
									JsonElement el = components.get("minecraft:tags");
									if(el.isJsonArray()) {
										for(JsonElement tag : el.getAsJsonArray().asList()) {
											String tagName = tag.getAsString();
											if(!tagName.contains(":"))
												tagName = "minecraft:" + tagName;
											tagName.replace(":", ":worldgen/biome");
											
											registerTag(tagName, biomeName);
										}
									}else if(el.isJsonObject()) {
										for(JsonElement tag : el.getAsJsonObject().getAsJsonArray("tags").asList()) {
											String tagName = tag.getAsString();
											if(!tagName.contains(":"))
												tagName = "minecraft:" + tagName;
											tagName.replace(":", ":worldgen/biome");
											
											registerTag(tagName, biomeName);
										}
									}
								}else {
									for(Entry<String, JsonElement> entry2 : components.entrySet()) {
										if(!entry2.getKey().startsWith("minecraft:")) {
											String tagName = entry2.getKey();
											if(!tagName.contains(":"))
												tagName = "minecraft:" + tagName;
											tagName.replace(":", ":worldgen/biome");
											registerTag(tagName, biomeName);
										}
									}
								}
							}
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File biomesClient = new File(getFolder(), "biomes_client.json");
		if(biomesClient.exists()) {
			try {
				JsonObject obj = Json.read(biomesClient).getAsJsonObject();
				if(obj.has("biomes")) {
					JsonObject biomesObj = obj.get("biomes").getAsJsonObject();
					for(Entry<String, JsonElement> entry : biomesObj.entrySet()) {
						String biomeName = entry.getKey();
						if(!biomeName.contains(":"))
							biomeName = "minecraft:" + biomeName;
						biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
						int waterSurfaceColor = 0xFF44AFF5;
						JsonObject biomeObj = entry.getValue().getAsJsonObject();
						if(biomeObj.has("water_surface_color")) {
							waterSurfaceColor = Integer.parseInt(biomeObj.get("water_surface_color")
																.getAsString().replace("#", ""), 16);
							waterSurfaceColor |= 0xFF000000;
						}
						
						Biome biome = biomes.getOrDefault(biomeName, null);
						if(biome == null) {
							biome = new BiomeBedrockEdition(biomeName, 0, null);
							biomes.put(biomeName, biome);
						}
						biome.setWaterColour(new Color(waterSurfaceColor));
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_BEDROCK.getTranslator(Integer.MAX_VALUE);
		
		File blocksFolder = new File(getFolder(), "blocks");
		if(blocksFolder.exists() && blocksFolder.isDirectory()) {
			for(File f : blocksFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("minecraft:block"))
							continue;
						JsonObject blockData = data.get("minecraft:block").getAsJsonObject();
						if(!blockData.has("description"))
							continue;
						JsonObject descriptionObj = blockData.get("description").getAsJsonObject();
						if(!descriptionObj.has("identifier"))
							continue;
						
						String blockName = descriptionObj.get("identifier").getAsString();
						if(!blockName.contains(":"))
							blockName = "minecraft:" + blockName;
						NbtTagCompound properties = NbtTagCompound.newInstance("properties");
						blockName = blockTranslatorManager.map(blockName, properties);
						properties.free();
						
						BlockStateHandlerBedrockEdition handler = new BlockStateHandlerBedrockEdition(blockName, blockData);
						blockStateHandlers.put(blockName, handler);
						
						if(handler.hasTransparency())
							transparentBlocks.add(blockName);
						
						if(blockData.has("components")) {
							for(String tag : blockData.get("components").getAsJsonObject().keySet()) {
								if(tag.startsWith("tag:")) {
									tag = tag.substring(4);
									if(!tag.contains(":"))
										tag = "minecraft:" + tag;
									registerTag(tag, blockName);
								}
							}
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File modelsFolder = new File(getFolder(), "models");
		if(modelsFolder.exists() && modelsFolder.isDirectory())
			processModelFolder(modelsFolder);
		
		File renderControllersFolder = new File(getFolder(), "render_controllers");
		if(renderControllersFolder.exists() && renderControllersFolder.isDirectory()) {
			for(File f : renderControllersFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("render_controllers"))
							continue;
						
						for(Entry<String, JsonElement> entry : data.get("render_controllers").getAsJsonObject().entrySet()) {
							renderControllers.put(entry.getKey(), new RenderControllerBedrockEdition(entry.getValue().getAsJsonObject()));
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File animationsFolder = new File(getFolder(), "animations");
		if(animationsFolder.exists() && animationsFolder.isDirectory()) {
			for(File f : animationsFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("animations"))
							continue;
						
						for(Entry<String, JsonElement> entry : data.getAsJsonObject("animations").entrySet()) {
							animations.put(entry.getKey(), new AnimationBedrock(entry.getKey(), entry.getValue().getAsJsonObject()));
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File animationControllersFolder = new File(getFolder(), "animation_controllers");
		if(animationControllersFolder.exists() && animationControllersFolder.isDirectory()) {
			for(File f : animationControllersFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("animation_controllers"))
							continue;
						
						for(Entry<String, JsonElement> entry : data.getAsJsonObject("animation_controllers").entrySet()) {
							animations.put(entry.getKey(), new AnimationControllerBedrock(entry.getKey(), 
															entry.getValue().getAsJsonObject()));
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File clientEntitiesFolder = new File(getFolder(), "entity");
		if(clientEntitiesFolder.exists() && clientEntitiesFolder.isDirectory()) {
			for(File f : clientEntitiesFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("minecraft:client_entity"))
							continue;
						JsonObject entityData = data.get("minecraft:client_entity").getAsJsonObject();
						if(!entityData.has("description"))
							continue;
						JsonObject descriptionObj = entityData.get("description").getAsJsonObject();
						if(!descriptionObj.has("identifier"))
							continue;
						
						String entityName = descriptionObj.get("identifier").getAsString();
						if(!entityName.contains(":"))
							entityName = "minecraft:" + entityName;
						
						String minEngineVersion = "0";
						if(descriptionObj.has("min_engine_version")) {
							minEngineVersion = descriptionObj.get("min_engine_version").getAsString();
						}
						if(!isMostRecent(entityName, minEngineVersion))
							continue;
						
						EntityHandlerBedrockEdition handler = new EntityHandlerBedrockEdition(entityName, entityData);
						entityHandlers.put(entityName, handler);
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File entitiesFolder = new File(getFolder(), "entities");
		if(entitiesFolder.exists() && entitiesFolder.isDirectory()) {
			for(File f : entitiesFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("minecraft:entity"))
							continue;
						JsonObject entityData = data.get("minecraft:entity").getAsJsonObject();
						if(!entityData.has("description"))
							continue;
						JsonObject descriptionObj = entityData.get("description").getAsJsonObject();
						if(!descriptionObj.has("identifier"))
							continue;
						
						String entityName = descriptionObj.get("identifier").getAsString();
						if(!entityName.contains(":"))
							entityName = "minecraft:" + entityName;
						
						EntityAIHandlerBedrockEdition handler = new EntityAIHandlerBedrockEdition(entityName, entityData);
						entityAIHandlers.put(entityName, handler);
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
		
		File spawnRulesFolder = new File(getFolder(), "spawn_rules");
		if(spawnRulesFolder.exists() && spawnRulesFolder.isDirectory()) {
			for(File f : spawnRulesFolder.listFiles()) {
				if(f.isFile() && f.getName().endsWith(".json")) {
					try {
						JsonObject data = Json.read(f).getAsJsonObject();
						if(!data.has("minecraft:spawn_rules"))
							continue;
						entitySpawners.addAll(EntitySpawnerHandlerBedrock.parseSpawnRules(data.getAsJsonObject("minecraft:spawn_rules")));
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	private void processModelFolder(File folder) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory())
				processModelFolder(file);
			else if(file.isFile()) {
				try {
					JsonObject data = Json.read(file).getAsJsonObject();
					for(Entry<String, JsonElement> entry : data.entrySet()) {
						if(entry.getKey().equals("minecraft:geometry")) {
							for(JsonElement el : entry.getValue().getAsJsonArray().asList()) {
								JsonObject modelObj = el.getAsJsonObject();
								if(modelObj.has("description")) {
									JsonObject description = modelObj.get("description").getAsJsonObject();
									if(description.has("identifier")) {
										String identifier = description.get("identifier").getAsString();
										String parent = null;
										if(identifier.contains(":")) {
											String[] tokens = identifier.split(":");
											identifier = tokens[0];
											parent = tokens[1];
										}
										if(!identifier.contains(":"))
											identifier = "minecraft:" + identifier;
										modelHandlers.put(identifier, new ModelHandlerBedrockEdition(parent, modelObj));
									}
								}
							}
						} else if(entry.getKey().startsWith("geometry.")) {
							JsonObject modelObj = entry.getValue().getAsJsonObject();
							String identifier = entry.getKey();
							String parent = null;
							if(identifier.contains(":")) {
								String[] tokens = identifier.split(":");
								identifier = tokens[0];
								parent = tokens[1];
							}
							if(!identifier.contains(":"))
								identifier = "minecraft:" + identifier;
							modelHandlers.put(identifier, new ModelHandlerBedrockEdition(parent, modelObj));
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void registerTag(String tag, String resourceId) {
		List<String> ids = tags.getOrDefault(tag, null);
		if(ids == null) {
			ids = new ArrayList<String>();
			tags.put(tag, ids);
		}
		ids.add(resourceId);
	}
	
	@Override
	public File getResource(String name, String type, String category, String extension) {
		if(name.contains(";")) {
			// MiEx specific stuff, in some cases (like with Optifine connected textures)
			// textures might not be located in the textures folder, but the optifine folder.
			// This means that the "type" is different. In order to facilitate this,
			// resource identifiers can override the default type by prefixing the
			// resource identifier with the type and then a semicolon.
			String[] tokens = name.split(";");
			if(tokens.length == 2) {
				type = tokens[0];
				name = tokens[1];
			}
		}
		
		String namespaced_name = name;
		if(!namespaced_name.contains(":"))
			namespaced_name = "minecraft:" + namespaced_name;
		String path = TranslationRegistry.FILE_PATH_MAPPING_BEDROCK.map(type + ";" + namespaced_name);
		File file = new File(getFolder(), path + extension);
		if(file.exists())
			return file;
		
		String[] tokens = namespaced_name.split(":");
		path = type + "/" + tokens[tokens.length - 1];
		
		file = new File(getFolder(), path + extension);
		if(file.exists())
			return file;
		
		List<String> paths = terrain_textures.getOrDefault(name, null);
		if(paths != null) {
			path = paths.get(0);
			file = new File(getFolder(), path + extension);
			if(file.exists())
				return file;
		}
		
		return new File(getFolder(), name + extension);
	}

	@Override
	public BlockStateHandler getBlockStateHandler(String name) {
		return blockStateHandlers.getOrDefault(name, null);
	}
	
	@Override
	public ModelHandler getModelHandler(String name) {
		if(!name.contains(":"))
			name = "minecraft:" + name;
		if(name.equals("minecraft:geometry.full_block"))
			return new ModelHandlerFullBlock();
		if(name.equals("minecraft:geometry.cross"))
			return new ModelHandlerCross();
		return modelHandlers.getOrDefault(name, null);
	}
	
	@Override
	public EntityHandler getEntityHandler(String name) {
		return entityHandlers.getOrDefault(name, null);
	}
	
	@Override
	public EntityAIHandler getEntityAIHandler(String name) {
		return entityAIHandlers.getOrDefault(name, null);
	}
	
	@Override
	public ItemHandler getItemHandler(String name, NbtTagCompound data) {
		return null;
	}
	
	@Override
	public Biome getBiome(String name, int id) {
		Biome biome = biomes.getOrDefault(name, null);
		if(biome == null)
			return null;
		return new BiomeBedrockEdition((BiomeBedrockEdition) biome, id);
	}
	
	@Override
	public File getTexture(String name) {
		File texFile = getResource(name, "textures", "assets", ".exr");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name, "textures", "assets", ".tga");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name, "textures", "assets", ".png");
		if(texFile.exists())
			return texFile;
		return null;
	}

	@Override
	public void parseTags(Map<String, List<String>> tagToResourceIdentifiers) {
		for(Entry<String, List<String>> entry : tags.entrySet()) {
			List<String> ids = tagToResourceIdentifiers.getOrDefault(entry.getKey(), null);
			if(ids == null) {
				ids = new ArrayList<String>();
				tagToResourceIdentifiers.put(entry.getKey(), ids);
			}
			ids.addAll(entry.getValue());
		}
	}
	
	@Override
	public void parseBannerPatterns(Map<String, String> patternMap) {
		
	}

	@Override
	public MCMeta getMCMeta(String texture) {
		// First get the texture for it, in order to get the bedrock path for it
		File textureFile = getTexture(texture);
		if(textureFile == null)
			return null;
		String relativePath = textureFile.getAbsolutePath().substring(getFolder().getAbsolutePath().length()+1);
		int extensionDot = relativePath.lastIndexOf('.');
		relativePath = relativePath.substring(0, extensionDot);
		
		return mcmetas.getOrDefault(relativePath, null);
	}
	
	public static boolean supportsResourcePack(File folder) {
		if(new File(folder, "manifest.json").exists())
			return true;
		return false;
	}
	
	private static String getName(File folder) {
		File manifestFile = new File(folder, "manifest.json");
		if(!manifestFile.exists())
			return folder.getName();
		try {
			JsonObject data = Json.read(manifestFile).getAsJsonObject();
			if(data.has("header")) {
				JsonObject header = data.get("header").getAsJsonObject();
				if(header.has("name"))
					return header.get("name").getAsString();
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return folder.getName();
	}
	
	private static String getUUID(File folder) {
		File manifestFile = new File(folder, "manifest.json");
		if(!manifestFile.exists())
			return folder.getName();
		try {
			JsonObject data = Json.read(manifestFile).getAsJsonObject();
			if(data.has("header")) {
				JsonObject header = data.get("header").getAsJsonObject();
				if(header.has("uuid"))
					return header.get("uuid").getAsString();
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return folder.getName();
	}
	
	public static Map<String, String> getBlockTextureMapping(String blockName){
		if(blockName.contains(":"))
			blockName = blockName.substring(blockName.indexOf(':') + 1);
		return blockTextureMappings.getOrDefault(blockName, null);
	}
	
	public static List<String> getTerrainTexture(String textureName){
		return terrain_textures.getOrDefault(textureName, null);
	}
	
	private static RenderControllerBedrockEdition defaultRenderController = new DefaultRenderControllerBedrockEdition();
	public static RenderControllerBedrockEdition getRenderController(String name) {
		return renderControllers.getOrDefault(name, defaultRenderController);
	}
	
	@Override
	public Animation getAnimation(String animation) {
		return animations.getOrDefault(animation, null);
	}
	
	@Override
	public PaintingVariant getPaintingVariant(String id) {
		return null;
	}
	
	@Override
	public void getEntitySpawners(List<EntitySpawner> spawners) {
		spawners.addAll(entitySpawners);
	}

	@Override
	public Font getFont(String id) {
		return null;
	}
	
	@Override
	public void getTextures(List<Entry<String, File>> out, TextureGroup... groups) {
		File texturesFolder = new File(getFolder(), "textures");
		if(!texturesFolder.exists() || !texturesFolder.isDirectory())
			return;
		
		List<String> folderNames = new ArrayList<String>();
		for(TextureGroup group : groups)
			getFoldersForTextureGroup(folderNames, group);
		
		for(int i = 0; i < folderNames.size(); ++i) {
			File subFolder = new File(texturesFolder, folderNames.get(i));
			if(subFolder.exists() && subFolder.isDirectory()) {
				getTexturesInFolder(out, subFolder, "minecraft:" + subFolder.getName() + "/");
			}
		}
	}
	
	private void getTexturesInFolder(List<Entry<String, File>> out, File folder, String parent) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				getTexturesInFolder(out, file, parent + file.getName() + "/");
			}else if(file.isFile()) {
				int dotIndex = file.getName().lastIndexOf((int) '.');
				if(dotIndex < 0)
					continue;
				String extension = file.getName().substring(dotIndex);
				if(extension.equalsIgnoreCase(".exr") || extension.equalsIgnoreCase(".tga") || extension.equalsIgnoreCase(".png")) {
					String id = parent + file.getName().substring(0, dotIndex);
					out.add(new Pair<String, File>(id, file));
				}
			}
		}
	}
	
	private void getFoldersForTextureGroup(List<String> out, TextureGroup group) {
		switch(group) {
		case BLOCKS:
			out.add("blocks");
			out.add("painting");
			break;
		case EFFECTS:
			out.add("particles");
			out.add("particle");
			break;
		case ENTITY:
			out.add("entity");
			out.add("models");
			out.add("trims");
			break;
		case ENVIRONMENT:
			out.add("environment");
			break;
		case GUI:
			out.add("ui");
			out.add("gui");
			break;
		case ITEMS:
			out.add("items");
			out.add("map");
			break;
		case UTILITY:
			out.add("misc");
			out.add("colormap");
			break;
		}
	}

}
