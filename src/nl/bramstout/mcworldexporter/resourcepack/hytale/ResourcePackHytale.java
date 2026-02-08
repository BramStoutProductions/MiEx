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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockStateRegistry;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockStateRegistry.IBlockStateConstructor;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.parallel.Async.AsyncGroup;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;
import nl.bramstout.mcworldexporter.resourcepack.PaintingVariant;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.resourcepack.TextureGroup;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintValue;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectLogic;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTexture;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextureTransitionHytale;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures.BlockStateConstraint;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures.MatchBlock;

public class ResourcePackHytale extends ResourcePack{

	private List<File> rootFolders;
	private List<File> reversedRootFolders;
	private Map<String, File> biomeFiles;
	private Map<String, File> blockStateFiles;
	private Map<String, BlockAnimationHandler> blockAnimationFiles;
	
	public ResourcePackHytale(File folder) {
		super(folder.getName(), folder.getName(), folder);
		rootFolders = new ArrayList<File>();
		rootFolders.add(folder);
		reversedRootFolders = Util.reverseList(rootFolders);
		biomeFiles = new HashMap<String, File>();
		blockStateFiles = new HashMap<String, File>();
		blockAnimationFiles = new HashMap<String, BlockAnimationHandler>();
	}
	
	public static boolean supportsResourcePack(File folder) {
		File manifest = new File(folder, "manifest.json");
		File common = new File(folder, "Common");
		File server = new File(folder, "Server");
		if(manifest.exists() && 
				((common.exists() && common.isDirectory()) || (server.exists() && server.isDirectory())))
			return true;
		return false;
	}

	@Override
	public List<File> getFolders() {
		return rootFolders;
	}

	@Override
	public List<File> getFoldersReversed() {
		return reversedRootFolders;
	}

	@Override
	public void load() {
		AsyncGroup asyncGroup = new AsyncGroup();
		this.textureFilesCache.clear();
		File commonFolder = new File(getFolder(), "Common");
		if(commonFolder.exists()) {
			searchBlockAnimations(commonFolder, "", asyncGroup, 0);
		}
		
		File blockStateFolder = new File(getFolder(), "Server/Item/Items");
		if(blockStateFolder.exists()) {
			searchBlockStates(blockStateFolder, asyncGroup, 0);
		}
		
		File biomeFolder = new File(getFolder(), "Server/Environments");
		if(biomeFolder.exists()) {
			searchBiomes(biomeFolder);
		}
		
		asyncGroup.waitUntilDone();
	}
	
	@Override
	public void postLoad() {
		AsyncGroup asyncGroup = new AsyncGroup();
		File fluidBlockStateFolder = new File(getFolder(), "Server/Item/Block/Fluids");
		if(fluidBlockStateFolder.exists()) {
			searchFluidBlockStates(fluidBlockStateFolder);
		}
		
		for(Entry<String, File> blockStateFile : blockStateFiles.entrySet()) {
			parseBlockState(blockStateFile.getKey(), blockStateFile.getValue(), asyncGroup);
		}
		asyncGroup.waitUntilDone();
		
		Tint waterTint = new Tint((JsonObject)null);
		TintLayers waterBaseTint = new TintLayers();
		waterBaseTint.setLayer(0, new TintValue(null, "minecraft:water"));
		waterTint.setBaseTint(waterBaseTint);
		Tints.setTint("hytale:Water", waterTint, true);
		Tints.setTint("hytale:Water_Finite", waterTint, true);
		Tints.setTint("hytale:Water_Source", waterTint, true);
	}
	
	private void searchBlockAnimations(File folder, String parent, AsyncGroup asyncGroup, int depth) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				if(depth == 1) {
					asyncGroup.runTask(()->{
						searchBlockAnimations(f, parent + f.getName() + "/", asyncGroup, depth+1);
					});
				}else {
					searchBlockAnimations(f, parent + f.getName() + "/", asyncGroup, depth+1);
				}
			}else if(f.isFile() && f.getName().endsWith(".blockyanim")) {
				asyncGroup.runTask(()->{
					String animName = parent + f.getName().substring(0, f.getName().length()-11);
					animName = "hytale:" + animName;
					BlockAnimationHandlerHytale handler = new BlockAnimationHandlerHytale(Json.read(f).getAsJsonObject());
					synchronized(blockAnimationFiles) {
						blockAnimationFiles.put(animName, handler);
					}
				});
			}
		}
	}
	
	private void searchBlockStates(File folder, AsyncGroup asyncGroup, int depth) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				if(depth == 0) {
					asyncGroup.runTask(()->{
						searchBlockStates(f, asyncGroup, depth+1);
					});
				}else {
					searchBlockStates(f, asyncGroup, depth+1);
				}
			}else if(f.isFile() && f.getName().endsWith(".json")) {
				String blockName = f.getName().substring(0, f.getName().length()-5);
				blockName = "hytale:" + blockName;
				blockStateFiles.put(blockName, f);
			}
		}
	}
	
	private void parseBlockState(String blockName, File f, AsyncGroup asyncGroup) {
		// The block state file will indicate what biome tints there are
		// on the block, so we need to add them to the tints registry.
		
		asyncGroup.runTask(()->{
			BlockStateHandler blockStateHandler = ResourcePacks.getBlockStateHandler(blockName);
			if(blockStateHandler != null && blockStateHandler instanceof BlockStateHandlerHytale) {
				if(((BlockStateHandlerHytale)blockStateHandler).hasBiomeTint()) {
					Tint tint = new Tint((JsonObject)null);
					TintLayers tintLayers = new TintLayers();
					tintLayers.setLayer(0, new TintValue(null, "hytale:tint"));
					tint.setBaseTint(tintLayers);
					Tints.setTint(blockName, tint, true);
				}
				if(((BlockStateHandlerHytale)blockStateHandler).isTransparent()) {
					synchronized(Config.transparentOcclusion) {
						Config.transparentOcclusion.add(blockName);
					}
				}
				
				synchronized(asyncGroup) {
					for(Entry<String, BlockStateVariant> variant : ((BlockStateHandlerHytale)blockStateHandler).getVariants().entrySet()) {
						addTransitionTextures(blockName, variant.getKey(), variant.getValue(), 
								((BlockStateHandlerHytale)blockStateHandler).getVariants().size());
					}
				}
			}
		});
	}
	
	private void addTransitionTextures(String blockName, String variantName, BlockStateVariant variant, int numVariants) {
		if(variant.getTransitionTexture() == null || variant.getTransitionToGroups() == null)
			return;
		
		for(float uvRotation = 0f; uvRotation < 315f; uvRotation += 90f) {
			ConnectedTexture connectedTexture = new ConnectedTextureTransitionHytale(blockName, 0);
			connectedTexture.setUVRotation(uvRotation);
			connectedTexture.getTiles().add(variant.getTransitionTexture());
			
			ConnectLogic.ConnectLogicBlockNames connectLogic = new ConnectLogic.ConnectLogicBlockNames();
			connectLogic.ignoreSameBlock = true;
			connectLogic.hytaleSpecificLogic = true;
			BlockStateConstraint constraint = new BlockStateConstraint();
			if(numVariants > 1) {
				// No need to specify a constraint if there is only one variant (the main variant).
				if(variantName.equals("")) {
					constraint.propertiesToBeUndefined.add("Definitions");
				}else {
					constraint.checks.put("Definitions", Arrays.asList(variantName));
				}
			}
			connectLogic.blocks.add(new MatchBlock(blockName, constraint));
			connectedTexture.setConnectLogic(connectLogic);
			
			connectedTexture.getFacesToConnect().add(Direction.UP);
			
			if(variant.getBiomeTintUp() >= 0) {
				connectedTexture.setTintIndex(Integer.valueOf(variant.getBiomeTintUp()));
				connectedTexture.setTintBlock(blockName);
			}else if(variant.getTintUp() != null && variant.getTintUp().length > 0) {
				connectedTexture.setTint(variant.getTintUp()[0]);
			}
			
			for(String group : variant.getTransitionToGroups()) {
				for(String block : Tags.getNamesInTag(group)) {
					ConnectedTextures.registerConnectedTextureByBlock(block, new BlockStateConstraint(), connectedTexture);
				}
			}
		}
	}
	
	@SuppressWarnings("unused")
	private TintLayers getTintLayersForBlockVariant(JsonObject data) {
		int biomeTintUp = -1;
		int biomeTintDown = -1;
		int biomeTintNorth = -1;
		int biomeTintSouth = -1;
		int biomeTintEast = -1;
		int biomeTintWest = -1;
		
		if(data.has("BiomeTint")) {
			int biomeTint = data.get("BiomeTint").getAsInt();
			biomeTintUp = biomeTint;
			biomeTintDown = biomeTint;
			biomeTintNorth = biomeTint;
			biomeTintSouth = biomeTint;
			biomeTintEast = biomeTint;
			biomeTintWest = biomeTint;
		}
		
		if(data.has("BiomeTintUp"))
			biomeTintUp = data.get("BiomeTintUp").getAsInt();

		if(data.has("BiomeTintDown"))
			biomeTintDown = data.get("BiomeTintDown").getAsInt();

		if(data.has("BiomeTintNorth"))
			biomeTintNorth = data.get("BiomeTintNorth").getAsInt();

		if(data.has("BiomeTintSouth"))
			biomeTintSouth = data.get("BiomeTintSouth").getAsInt();

		if(data.has("BiomeTintEast"))
			biomeTintEast = data.get("BiomeTintEast").getAsInt();

		if(data.has("BiomeTintWest"))
			biomeTintWest = data.get("BiomeTintWest").getAsInt();
		
		int biomeTint = Math.max(Math.max(Math.max(biomeTintUp, biomeTintDown), biomeTintNorth),
								Math.max(Math.max(biomeTintSouth, biomeTintEast), biomeTintWest));
		
		if(biomeTint == -1)
			return null;
		
		TintLayers tintLayers = new TintLayers();
		if(biomeTint > 0)
			tintLayers.setLayer(0, new TintValue(null, "hytale:tint"));
		
		return tintLayers;
	}
	
	private static class HytaleLiquidBlockStateConstructor implements IBlockStateConstructor{
		
		public JsonObject data;
		
		public HytaleLiquidBlockStateConstructor(File file) {
			data = readData(file);
		}
		
		@Override
		public BlockState construct(String name, int dataVersion) {
			return new BlockStateHytaleLiquid(name, dataVersion, data);
		}
		
		private static JsonObject readData(File file) {
			JsonObject data = Json.read(file).getAsJsonObject();
			
			if(data.has("Parent")) {
				String parentName = data.get("Parent").getAsString();
				File parentFile = ResourcePacks.getFile("Item/Block/Fluids/" + parentName, "", ".json", "Server");
				if(parentFile != null && parentFile.exists()) {
					JsonObject parentData = readData(parentFile);
					for(Entry<String, JsonElement> entry : parentData.entrySet()) {
						if(!data.has(entry.getKey())) {
							data.add(entry.getKey(), entry.getValue());
						}
					}
				}
			}
			
			return data;
		}
		
	}
	
	private void searchFluidBlockStates(File folder) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				searchFluidBlockStates(f);
			}else if(f.isFile() && f.getName().endsWith(".json")) {
				String blockName = f.getName().substring(0, f.getName().length()-5);
				blockName = "hytale:" + blockName;
				//blockStateFiles.put(blockName, f);
				Config.liquid.add(blockName);
				BuiltInBlockStateRegistry.builtins.put(blockName, new HytaleLiquidBlockStateConstructor(f));
			}
		}
	}
	
	private void searchBiomes(File folder) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				searchBiomes(f);
			}else if(f.isFile() && f.getName().endsWith(".json")) {
				String biomeName = f.getName().substring(0, f.getName().length()-5);
				biomeName = "hytale:" + biomeName;
				biomeFiles.put(biomeName, f);
			}
		}
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
		
		String cleanName = name;
		int sep = cleanName.indexOf(':');
		if(sep != -1)
			cleanName = cleanName.substring(sep+1);
		
		String path = category + "/" + cleanName;
		
		File file = new File(getFolder(), path + extension);
		return file;
	}

	private Map<String, File> textureFilesCache = new HashMap<String, File>();
	private File NO_TEX_FILE = new File("NO_TEX");
	@Override
	public File getTexture(String name) {
		File file = textureFilesCache.getOrDefault(name, NO_TEX_FILE);
		if(file != NO_TEX_FILE)
			return file;
		synchronized(textureFilesCache) {
			file = textureFilesCache.getOrDefault(name, NO_TEX_FILE);
			if(file != NO_TEX_FILE)
				return file;
			file = getTextureImpl(name);
			textureFilesCache.put(name, file);
			return file;
		}
	}
	
	private File getTextureImpl(String name) {
		String name2 = name;
		int lastDot = name.lastIndexOf('.');
		if(lastDot != -1)
			name2 = name2.substring(0, lastDot-1);
		
		File texFile = getResource(name2, "", "Common", ".exr");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name2, "", "Common", ".tga");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name2, "", "Common", ".png");
		if(texFile.exists())
			return texFile;
		
		texFile = getResource(name, "", "Common", ".exr");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name, "", "Common", ".tga");
		if(texFile.exists())
			return texFile;
		texFile = getResource(name, "", "Common", ".png");
		if(texFile.exists())
			return texFile;
		
		texFile = getResource(name, "", "Common", "");
		if(texFile.exists())
			return texFile;
		
		return null;
	}

	@Override
	public BlockStateHandler getBlockStateHandler(String name) {
		File blockStateFile = blockStateFiles.getOrDefault(name, null);
		if(blockStateFile != null) {
			try {
				JsonObject data = Json.read(blockStateFile).getAsJsonObject();
				return new BlockStateHandlerHytale(name, data, this);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public ModelHandler getModelHandler(String name) {
		File modelFile = getResource(name, "", "Common", "");
		if(modelFile.exists()) {
			try {
				JsonObject data = Json.read(modelFile).getAsJsonObject();
				return new ModelHandlerHytale(data);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public BlockAnimationHandler getBlockAnimationHandler(String name) {
		return blockAnimationFiles.getOrDefault(name, null);
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
		return null;
	}

	@Override
	public Biome getBiome(String name, int id) {
		File biomeFile = biomeFiles.getOrDefault(name, null);
		if(biomeFile == null)
			return null;
		
		try {
			return new BiomeHytale(name, id, Json.read(biomeFile).getAsJsonObject());
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void parseTags(Map<String, List<String>> tagToResourceIdentifiers) {
		AsyncGroup asyncGroup = new AsyncGroup();
		for(Entry<String, File> blockStateFile : blockStateFiles.entrySet()) {
			
			final String blockStateName = blockStateFile.getKey();
			
			asyncGroup.runTask(()->{
				BlockStateHandler blockStateHandler = ResourcePacks.getBlockStateHandler(blockStateName);
				if(blockStateHandler != null && blockStateHandler instanceof BlockStateHandlerHytale) {
					String group = ((BlockStateHandlerHytale) blockStateHandler).getGroup();
					if(group.isEmpty())
						return;
					if(group.indexOf(':') == -1)
						group = "hytale:" + group;
					
					synchronized(tagToResourceIdentifiers) {
						List<String> blockList = tagToResourceIdentifiers.getOrDefault(group, null);
						if(blockList == null) {
							blockList = new ArrayList<String>();
								tagToResourceIdentifiers.put(group, blockList);
						}
						blockList.add(blockStateFile.getKey());
					}
				}
			});
		}
		asyncGroup.waitUntilDone();
	}

	@Override
	public void parseBannerPatterns(Map<String, String> patternMap) {}

	@Override
	public MCMeta getMCMeta(String texture) {
		return null;
	}

	@Override
	public Animation getAnimation(String animation) {
		return null;
	}

	@Override
	public PaintingVariant getPaintingVariant(String id) {
		return null;
	}

	@Override
	public void getEntitySpawners(List<EntitySpawner> spawners) {}

	@Override
	public Font getFont(String id) {
		return null;
	}

	@Override
	public void getTextures(List<Entry<String, File>> out, TextureGroup... groups) {}

	@Override
	public void getColorMaps(Set<String> colorMaps) {}

}
