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

package nl.bramstout.mcworldexporter.resourcepack.java;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
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
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;

public class ResourcePackJavaEdition extends ResourcePack{

	private List<String> overlays;
	private List<File> rootFolders;
	private List<File> reversedRootFolders;
	private Map<String, PaintingVariant> paintingVariants;
	private List<EntitySpawner> entitySpawners;
	private Map<String, Font> fonts;
	
	public ResourcePackJavaEdition(File folder) {
		super(folder.getName(), folder.getName(), folder);
		overlays = new ArrayList<String>();
		rootFolders = new ArrayList<File>();
		rootFolders.add(folder);
		reversedRootFolders = Util.reverseList(rootFolders);
		paintingVariants = new HashMap<String, PaintingVariant>();
		entitySpawners = new ArrayList<EntitySpawner>();
		fonts = new HashMap<String, Font>();
	}

	@Override
	public void load() {
		overlays.clear();
		rootFolders.clear();
		rootFolders.add(getFolder());
		paintingVariants.clear();
		entitySpawners.clear();
		
		File packMcMetaFile = new File(getFolder(), "pack.mcmeta");
		if(packMcMetaFile.exists()) {
			try {
				JsonObject obj = Json.read(packMcMetaFile).getAsJsonObject();
				if(obj.has("overlays")) {
					JsonObject overlays = obj.getAsJsonObject("overlays");
					if(overlays.has("entries")) {
						JsonArray entries = overlays.getAsJsonArray("entries");
						for(JsonElement el : entries.asList()) {
							if(el.isJsonObject()) {
								// TODO: Maybe check with pack formats to decide
								// whether it needs to do this overlay or not.
								JsonObject entry = el.getAsJsonObject();
								if(entry.has("directory")) {
									String directory = entry.get("directory").getAsString();
									File directoryFolder = new File(getFolder(), directory);
									if(directoryFolder.exists() && directoryFolder.isDirectory()) {
										this.overlays.add(directory);
										this.rootFolders.add(directoryFolder);
									}
								}
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		reversedRootFolders = Util.reverseList(rootFolders);
		
		for(File rootFolder : getFolders()) {
			File assetsFolder = new File(rootFolder, "assets");
			if(assetsFolder.exists() && assetsFolder.isDirectory()) {
				for(File namespace : assetsFolder.listFiles()) {
					parseAssetsNamespace(namespace, namespace.getName());
				}
			}
			
			File dataFolder = new File(rootFolder, "data");
			if(dataFolder.exists() && dataFolder.isDirectory()) {
				for(File namespace : dataFolder.listFiles()) {
					parseDataNamespace(namespace, namespace.getName());
				}
			}
		}
	}
	
	private void parseAssetsNamespace(File folder, String namespace) {
		File fontFolder = new File(folder, "font");
		if(fontFolder.exists() && fontFolder.isDirectory())
			parseFonts(fontFolder, namespace);
	}
	
	private void parseFonts(File folder, String namespace) {
		for(File f : folder.listFiles()) {
			if(!f.isFile())
				continue;
			if(!f.getName().endsWith(".json"))
				continue;
			String id = namespace + ":" + f.getName().replace(".json", "");
			try {
				JsonObject obj = Json.read(f).getAsJsonObject();
				fonts.put(id, new FontJava(obj));
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void parseDataNamespace(File folder, String namespace) {
		File paintingVariantFolder = new File(folder, "painting_variant");
		if(paintingVariantFolder.exists() && paintingVariantFolder.isDirectory())
			parsePaintingVariants(paintingVariantFolder, namespace);
		
		File biomeFolder = new File(folder, "worldgen/biome");
		if(biomeFolder.exists() && biomeFolder.isDirectory())
			parseBiomes(biomeFolder, namespace, "");
	}
	
	private void parsePaintingVariants(File folder, String namespace) {
		for(File f : folder.listFiles()) {
			String id = namespace + ":" + f.getName().replace(".json", "");
			try {
				JsonObject obj = Json.read(f).getAsJsonObject();
				String assetId = null;
				if(obj.has("asset_id"))
					assetId = obj.get("asset_id").getAsString();
				int width = 1;
				int height = 1;
				if(obj.has("width"))
					width = obj.get("width").getAsInt();
				if(obj.has("height"))
					height = obj.get("height").getAsInt();
				if(assetId != null)
					paintingVariants.put(id, new PaintingVariant(id, assetId, width, height));
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private void parseBiomes(File folder, String namespace, String parent) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				parseBiomes(f, namespace, parent + f.getName() + "/");
			}else if(f.isFile()) {
				String id = namespace + ":" + parent + f.getName().replace(".json", "");
				
				try {
					JsonObject obj = Json.read(f).getAsJsonObject();
					if(obj.has("spawners")) {
						List<EntitySpawner> spawners = EntitySpawnerJavaEdition.parseSpawnRules(obj.getAsJsonObject("spawners"), id);
						entitySpawners.addAll(spawners);
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public List<File> getFolders(){
		return rootFolders;
	}
	
	public List<File> getFoldersReversed(){
		return reversedRootFolders;
	}
	
	@Override
	public File getResource(String name, String type, String category, String extension) {
		int typeSeparatorIndex = name.indexOf(';');
		if(typeSeparatorIndex >= 0) {
			// MiEx specific stuff, in some cases (like with Optifine connected textures)
			// textures might not be located in the textures folder, but the optifine folder.
			// This means that the "type" is different. In order to facilitate this,
			// resource identifiers can override the default type by prefixing the
			// resource identifier with the type and then a semicolon.
			type = name.substring(0, typeSeparatorIndex);
			name = name.substring(typeSeparatorIndex + 1);
		}
		if (!name.contains(":"))
			name = "minecraft:" + name;
		//String[] tokens = name.split(":");
		int namespaceIndex = name.indexOf(':');
		//String path = tokens[tokens.length-1];
		//for(int i = 2; i < tokens.length; ++i)
		//	path = path + "/" + tokens[i];
		String namespace = name.substring(0, namespaceIndex);
		String path = name.substring(namespaceIndex + 1);
		
		String subPath = category + "/" + namespace + "/" + type + "/" + path + extension;
		File file = null;
		for(File rootFolder : getFolders()) {
			File file2 = new File(rootFolder, subPath);
			if(file == null || file2.exists())
				file = file2;
		}
		return file;
	}
	
	@Override
	public File getTexture(String name) {
		File texFile = getResource(name, "textures", "assets", ".exr");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name, "textures", "assets", ".png");
		if(texFile.exists())
			return texFile;
		return null;
	}

	@Override
	public BlockStateHandler getBlockStateHandler(String name) {
		File blockStateFile = getResource(name, "blockstates", "assets", ".json");
		if(blockStateFile.exists()) {
			try {
				JsonObject data = Json.read(blockStateFile).getAsJsonObject();
				return new BlockStateHandlerJavaEdition(name, data);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		// Blocks can get renamed and maybe this resource pack still uses the old name,
		// so try the old names for this block.
		List<String> aliases = TranslationRegistry.BLOCK_JAVA.getAliasesForBlock(name);
		if(aliases != null) {
			for(int i = 0; i < aliases.size(); ++i) {
				blockStateFile = getResource(aliases.get(i), "blockstates", "assets", ".json");
				if(blockStateFile.exists()) {
					try {
						JsonObject data = Json.read(blockStateFile).getAsJsonObject();
						return new BlockStateHandlerJavaEdition(name, data);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public ModelHandler getModelHandler(String name) {
		File modelFile = getResource(name, "models", "assets", ".obj");
		if(modelFile.exists()) {
			try {
				return new ModelHandlerObj(modelFile);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		modelFile = getResource(name, "models", "assets", ".json");
		if(modelFile.exists()) {
			try {
				JsonObject data = Json.read(modelFile).getAsJsonObject();
				return new ModelHandlerJavaEdition(data);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public EntityHandler getEntityHandler(String name) {
		return null;
	}
	
	@Override
	public EntityAIHandler getEntityAIHandler(String name) {
		return null;
	}
	
	@Override
	public ItemHandler getItemHandler(String name, NbtTagCompound data) {
		// If item has the item_model component tag, then update it from that.
		NbtTag componentsTag = data.get("components");
		if(componentsTag != null && componentsTag instanceof NbtTagCompound) {
			NbtTag itemModelTag = ((NbtTagCompound) componentsTag).get("minecraft:item_model");
			if(itemModelTag != null) {
				name = itemModelTag.asString();
			}
		}
		
		File itemHandlerFile = getResource(name, "items", "assets", ".json");
		if(itemHandlerFile.exists()) {
			try {
				JsonObject jsonData = Json.read(itemHandlerFile).getAsJsonObject();
				return new ItemHandlerJavaEdition(jsonData);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// Fallback to handle it based on item models.
		int sepIndex = name.indexOf(':');
		String namespace = null;
		if(sepIndex >= 0) {
			namespace = name.substring(0, sepIndex);
			name = name.substring(sepIndex+1);
		}else {
			namespace = "minecraft";
		}

		name = namespace + ":item/" + name;
		itemHandlerFile = getResource(name, "models", "assets", ".json");
		if(itemHandlerFile.exists()) {
			try {
				JsonObject jsonData = Json.read(itemHandlerFile).getAsJsonObject();
				// If this model file has overrides, then see it as an item handler.
				// Otherwise, we'll return null in the hopes that another resource pack
				// provides an item handler. If this is the base resource pack, then
				// that means that no other resource pack has specified an item handler
				// so we'll fall back to one based on this model file.
				if(jsonData.has("overrides") || getName().equals("base_resource_pack")) {
					return new ItemHandlerFallback(name, jsonData);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}else {
			name = name.replace(":item/", ":block/");
			itemHandlerFile = getResource(name, "models", "assets", ".json");
			if(itemHandlerFile.exists()) {
				try {
					JsonObject jsonData = Json.read(itemHandlerFile).getAsJsonObject();
					if(jsonData.has("overrides") || getName().equals("base_resource_pack")) {
						return new ItemHandlerFallback(name, jsonData);
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static boolean supportsResourcePack(File folder) {
		if(new File(folder, "assets").exists())
			return true;
		if(new File(folder, "data").exists())
			return true;
		return false;
	}

	@Override
	public Biome getBiome(String name, int id) {
		File biomeFile = getResource(name, "worldgen/biome", "data", ".json");
		if(!biomeFile.exists())
			return null;
		
		try {
			return new BiomeJavaEdition(name, id, Json.read(biomeFile).getAsJsonObject());
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void parseTags(Map<String, List<String>> tagToResourceIdentifiers) {
		for(File rootFolder : getFolders()) {
			File dataFolder = new File(rootFolder, "/data");
			if(!dataFolder.exists())
				continue;
			for(File namespace : dataFolder.listFiles()) {
				if(!namespace.isDirectory())
					continue;
				processNamespace(namespace.getName(), namespace, tagToResourceIdentifiers);
			}
		}
	}
	
	private void processNamespace(String namespace, File namespaceFolder, 
									Map<String, List<String>> tagToResourceIdentifiers) {
		File tagsFolder = new File(namespaceFolder, "tags");
		if(!tagsFolder.exists())
			return;
		processFolder(namespace, "", tagsFolder, tagToResourceIdentifiers);
	}
	
	private void processFolder(String namespace, String parent, File folder, 
									Map<String, List<String>> tagToResourceIdentifiers) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				processFolder(namespace, parent + file.getName() + "/", file, tagToResourceIdentifiers);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				processTagsFile(namespace + ":" + parent + file.getName().split("\\.")[0], 
								file, tagToResourceIdentifiers);
			}
		}
	}
	
	private void processTagsFile(String resourceIdentifier, File file, 
									Map<String, List<String>> tagToResourceIdentifiers) {
		try {
			JsonObject data = Json.read(file).getAsJsonObject();
			
			boolean replace = false;
			if(data.has("replace"))
				replace = data.get("replace").getAsBoolean();
			
			List<String> values = new ArrayList<String>();
			
			if(data.has("values")) {
				JsonArray jsonValues = data.get("values").getAsJsonArray();
				for(JsonElement el : jsonValues.asList()) {
					String name = null;
					if(el.isJsonPrimitive()) {
						name = el.getAsString();
					}else if(el.isJsonObject()) {
						if(el.getAsJsonObject().has("id")) {
							name = el.getAsJsonObject().get("id").getAsString();
						}
					}
					if(name != null) {
						if(!name.contains(":")) {
							if(name.startsWith("#"))
								name = "#minecraft:" + name.substring(1);
							else
								name = "minecraft:" + name;
						}
						values.add(name);
					}
				}
			}
			
			if(replace) {
				tagToResourceIdentifiers.put(resourceIdentifier, values);
			}else {
				List<String> existingList = tagToResourceIdentifiers.getOrDefault(resourceIdentifier, null);
				if(existingList == null) {
					tagToResourceIdentifiers.put(resourceIdentifier, values);
				}else {
					existingList.addAll(values);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void parseBannerPatterns(Map<String, String> patternMap) {
		for(File rootFolder : getFolders()) {
			File dataFolder = new File(rootFolder, "data");
			if(!dataFolder.exists() || !dataFolder.isDirectory())
				continue;
			for(File namespace : dataFolder.listFiles()) {
				File bannerPatternFolder = new File(namespace, "banner_pattern");
				if(!bannerPatternFolder.exists() || !bannerPatternFolder.isDirectory())
					continue;
				for(File bannerPattern : bannerPatternFolder.listFiles()) {
					if(!bannerPattern.isFile() || !bannerPattern.getName().endsWith(".json"))
						continue;
					try {
						JsonObject data = Json.read(bannerPattern).getAsJsonObject();
						if(data.has("asset_id")) {
							String id = data.get("asset_id").getAsString();
							if(!id.contains(":"))
								id = "minecraft:" + id;
							String[] tokens = id.split(":");
							patternMap.put(namespace.getName() + ":" + bannerPattern.getName().replace(".json", ""), 
									tokens[0] + ":entity/banner/" + tokens[1]);
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	private Map<String, MCMeta> mcmetaCache = new HashMap<String, MCMeta>();
	
	@Override
	public MCMeta getMCMeta(String texture) {
		MCMeta cachedValue = mcmetaCache.getOrDefault(texture, null);
		if(cachedValue == null) {
			synchronized(mcmetaCache) {
				cachedValue = mcmetaCache.getOrDefault(texture, null);
				if(cachedValue == null) {
					cachedValue = _getMCMeta(texture);
					mcmetaCache.put(texture, cachedValue);
				}
			}
		}
		return cachedValue;
	}
	
	private MCMeta _getMCMeta(String texture) {
		File texFile = getTexture(texture);
		if(texFile == null)
			return null;
		File mcmetaFile = new File(texFile.getPath() + ".mcmeta");
		JsonObject data = null;
		if(mcmetaFile.exists()) {
			try {
				data = Json.read(mcmetaFile).getAsJsonObject();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return new MCMetaJavaEdition(texFile, data);
	}
	
	@Override
	public Animation getAnimation(String animation) {
		return null;
	}
	
	@Override
	public PaintingVariant getPaintingVariant(String id) {
		return paintingVariants.getOrDefault(id, null);
	}
	
	@Override
	public void getEntitySpawners(List<EntitySpawner> spawners) {
		spawners.addAll(entitySpawners);
	}

	@Override
	public Font getFont(String id) {
		return fonts.getOrDefault(id, null);
	}
	
	@Override
	public void getTextures(List<Entry<String, File>> out, TextureGroup... groups) {
		Set<String> encounteredResourceIds = new HashSet<String>();
		for(File rootFolder : getFoldersReversed()) {
			File assetsFolder = new File(rootFolder, "assets");
			if(assetsFolder.exists() && assetsFolder.isDirectory()) {
				for(File namespaceFolder : assetsFolder.listFiles()) {
					if(namespaceFolder.isDirectory()) {
						getTexturesInNamespace(out, groups, namespaceFolder, encounteredResourceIds);
					}
				}
			}
		}
	}
	
	private void getTexturesInNamespace(List<Entry<String, File>> out, TextureGroup[] groups, File namespaceFolder,
										Set<String> encounteredResourceIds) {
		File texturesFolder = new File(namespaceFolder, "textures");
		if(texturesFolder.exists() && texturesFolder.isDirectory()) {
			List<String> folderNames = new ArrayList<String>();
			for(TextureGroup group : groups)
				getFoldersForTextureGroup(folderNames, group);
			
			for(int i = 0; i < folderNames.size(); ++i) {
				File subFolder = new File(texturesFolder, folderNames.get(i));
				if(subFolder.exists() && subFolder.isDirectory()) {
					getTexturesInFolder(out, subFolder, namespaceFolder.getName() + ":" + subFolder.getName() + "/", 
										encounteredResourceIds);
				}
			}
		}
		
		boolean hasBlocksGroup = false;
		for(TextureGroup grp : groups) {
			if(grp == TextureGroup.BLOCKS) {
				hasBlocksGroup = true;
				break;
			}
		}
		if(hasBlocksGroup) {
			File optifineCTMFolder = new File(namespaceFolder, "optifine/ctm");
			if(optifineCTMFolder.isDirectory() && optifineCTMFolder.isDirectory()) {
				getTexturesInFolder(out, optifineCTMFolder, "optifine;" + namespaceFolder.getName() + ":ctm/", 
									encounteredResourceIds);
			}
		}
	}
	
	private void getTexturesInFolder(List<Entry<String, File>> out, File folder, String parent, 
										Set<String> encounteredResourceIds) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				getTexturesInFolder(out, file, parent + file.getName() + "/", encounteredResourceIds);
			}else if(file.isFile()) {
				int dotIndex = file.getName().lastIndexOf((int) '.');
				if(dotIndex < 0)
					continue;
				String extension = file.getName().substring(dotIndex);
				if(extension.equalsIgnoreCase(".exr") || extension.equalsIgnoreCase(".tga") || extension.equalsIgnoreCase(".png")) {
					String id = parent + file.getName().substring(0, dotIndex);
					
					if(encounteredResourceIds.contains(id))
						continue;
					encounteredResourceIds.add(id);
					
					out.add(new Pair<String, File>(id, file));
				}
			}
		}
	}
	
	private void getFoldersForTextureGroup(List<String> out, TextureGroup group) {
		switch(group) {
		case BLOCKS:
			out.add("block");
			out.add("painting");
			break;
		case EFFECTS:
			out.add("effect");
			out.add("mob_effect");
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
			out.add("font");
			out.add("gui");
			break;
		case ITEMS:
			out.add("item");
			out.add("map");
			break;
		case UTILITY:
			out.add("misc");
			out.add("colormap");
			break;
		}
	}

}
