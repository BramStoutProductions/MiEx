package nl.bramstout.mcworldexporter.resourcepack;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.entity.builtins.EntityBuiltinsRegistry;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.bedrock.ResourcePackBedrockEdition;
import nl.bramstout.mcworldexporter.resourcepack.java.ResourcePackJavaEdition;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class ResourcePacks {
	
	private static List<ResourcePack> resourcePacks = new ArrayList<ResourcePack>();
	private static List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>();
	private static Object mutex = new Object();
	private static Map<String, Integer> defaultColours = new HashMap<String, Integer>();
	
	public static void init() {
		synchronized(mutex) {
			resourcePacks.clear();
			
			File resourcePacksFolder = new File(FileUtil.getResourcePackDir());
			if(!resourcePacksFolder.isDirectory())
				return;
			
			for(File f : resourcePacksFolder.listFiles()) {
				if(!f.isDirectory())
					continue;
				if(ResourcePackBedrockEdition.supportsResourcePack(f))
					resourcePacks.add(new ResourcePackBedrockEdition(f));
				else
					resourcePacks.add(new ResourcePackJavaEdition(f));
			}
			
			List<ResourcePack> newActiveResourcePacks = new ArrayList<ResourcePack>();
			for(ResourcePack pack : activeResourcePacks) {
				if(pack.getName().equals("base_resource_pack"))
					continue;
				ResourcePack newPack = getResourcePack(pack.getUUID());
				if(newPack != null)
					newActiveResourcePacks.add(newPack);
			}
			newActiveResourcePacks.add(getResourcePack("base_resource_pack"));
			
			activeResourcePacks = newActiveResourcePacks;
			for(int i = activeResourcePacks.size()-1; i >= 0; --i)
				activeResourcePacks.get(i).load();
		}
	}
	
	public static void load() {
		synchronized(mutex) {
			resourcePacks.clear();
			
			File resourcePacksFolder = new File(FileUtil.getResourcePackDir());
			if(!resourcePacksFolder.isDirectory())
				return;
			
			for(File f : resourcePacksFolder.listFiles()) {
				if(!f.isDirectory())
					continue;
				if(ResourcePackBedrockEdition.supportsResourcePack(f))
					resourcePacks.add(new ResourcePackBedrockEdition(f));
				else
					resourcePacks.add(new ResourcePackJavaEdition(f));
			}
			
			List<ResourcePack> newActiveResourcePacks = new ArrayList<ResourcePack>();
			for(ResourcePack pack : activeResourcePacks) {
				if(pack.getName().equals("base_resource_pack"))
					continue;
				ResourcePack newPack = getResourcePack(pack.getUUID());
				if(newPack != null)
					newActiveResourcePacks.add(newPack);
			}
			newActiveResourcePacks.add(getResourcePack("base_resource_pack"));
			
			// If the two lists have the same size, then nothing has
			// changed regarding the active resource packs, so we
			// can just replace the list so that we have the new instances
			// of the resource pack classes.
			if(newActiveResourcePacks.size() == activeResourcePacks.size()) {
				activeResourcePacks = newActiveResourcePacks;
				for(int i = activeResourcePacks.size()-1; i >= 0; --i)
					activeResourcePacks.get(i).load();
			}else {
				setActiveResourcePacks(newActiveResourcePacks);
			}
		}
	}
	
	public static void setActiveResourcePackUUIDs(List<String> packs) {
		List<ResourcePack> packs2 = new ArrayList<ResourcePack>();
		for(String uuid : packs) {
			ResourcePack pack = getResourcePack(uuid);
			if(pack != null)
				packs2.add(pack);
		}
		setActiveResourcePacks(packs2);
	}
	
	public static void setActiveResourcePacks(List<ResourcePack> packs) {
		ResourcePackBedrockEdition.reset();
		activeResourcePacks.clear();
		for(ResourcePack pack : packs) {
			if(pack == null)
				continue;
			// The base resource pack should always be at the very end.
			// So we skip it here and add it later.
			if (pack.getName().equals("base_resource_pack"))
				continue;
			// Make sure that it's the right instance.
			// pack could be an old invalid instance.
			ResourcePack pack2 = getResourcePack(pack.getUUID());
			activeResourcePacks.add(pack2);
		}
		ResourcePack baseResourcePack = getResourcePack("base_resource_pack");
		if(baseResourcePack != null)
			activeResourcePacks.add(baseResourcePack);
		
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Loading resource packs");
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
		int progressCounter = 0;
		boolean hasLoadError = false;
		for(int i = activeResourcePacks.size()-1; i >= 0; --i) {
			try {
				activeResourcePacks.get(i).load();
			}catch(Exception ex) {
				hasLoadError = true;
				System.out.println("Failed to load resource pack " + activeResourcePacks.get(i).getFolder().getName());
				ex.printStackTrace();
			}
			progressCounter++;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(
					((float) progressCounter) / ((float) activeResourcePacks.size()));
		}
		
		synchronized(mutex) {
			defaultColours.clear();
		}
		Atlas.readAtlasConfig();
		Config.load();
		BlockStateRegistry.clearBlockStateRegistry();
		ModelRegistry.clearModelRegistry();
		BiomeRegistry.recalculateTints();
		MCWorldExporter.getApp().getUI().update();
		MCWorldExporter.getApp().getUI().fullReRender();
		if(MCWorldExporter.getApp().getWorld() != null)
			MCWorldExporter.getApp().getWorld().reloadFromResourcepack();
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
		
		if(hasLoadError) {
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), 
					"One or more of the resource packs failed to load properly. Please check the log.", 
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public static List<ResourcePack> getActiveResourcePacks() {
		return activeResourcePacks;
	}
	
	public static List<ResourcePack> getResourcePacks(){
		return resourcePacks;
	}
	
	public static ResourcePack getBaseResourcePack() {
		return activeResourcePacks.get(activeResourcePacks.size() - 1);
	}
	
	public static ResourcePack getResourcePack(String uuid) {
		ResourcePack pack = null;
		for(int i = 0; i < resourcePacks.size(); ++i) {
			pack = resourcePacks.get(i);
			if(pack.getUUID().equals(uuid))
				return pack;
		}
		return null;
	}
	
	public static File getFile(String resource, String type, String extension, String category) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			File file = pack.getResource(resource, type, category, extension);
			if(file != null && file.exists())
				return file;
		}
		return null;
	}
	
	public static File getTexture(String resource) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			File file = pack.getTexture(resource);
			if(file != null)
				return file;
		}
		return null;
	}
	
	public static JsonObject getJSONData(String resource, String type, String category) {
		File file = getFile(resource, type, ".json", category);
		if (file == null || !file.exists())
			return null;
		try {
			return Json.read(file).getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean hasOverride(String resource, String type, String extension, String category) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			if(pack.getName().equals("base_resource_pack"))
				continue;
			File file = pack.getResource(resource, type, category, extension);
			if(file != null && file.exists())
				return true;
		}
		return false;
	}
	
	public static Biome getBiome(String name, int id) {
		BiomeCombined resBiome = new BiomeCombined(name, id);
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			Biome biome = pack.getBiome(name, id);
			if(biome != null)
				resBiome.getSubBiomes().add(biome);
		}
		resBiome.calculateTints();
		if(resBiome.getSubBiomes().isEmpty())
			World.handleError(new Exception("No biome file available for " + name));
		return resBiome;
	}
	
	public static MCMeta getMCMeta(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			MCMeta mcmeta = pack.getMCMeta(name);
			if(mcmeta != null)
				return mcmeta;
		}
		return null;
	}
	
	public static BlockStateHandler getBlockStateHandler(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			BlockStateHandler blockStateHandler = pack.getBlockStateHandler(name);
			if(blockStateHandler != null)
				return blockStateHandler;
		}
		return null;
	}
	
	public static ModelHandler getModelHandler(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			ModelHandler modelHandler = pack.getModelHandler(name);
			if(modelHandler != null)
				return modelHandler;
		}
		return null;
	}
	
	public static EntityHandler getEntityHandler(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			EntityHandler entityHandler = pack.getEntityHandler(name);
			if(entityHandler != null)
				return entityHandler;
		}
		
		return EntityBuiltinsRegistry.builtins.getOrDefault(name, null);
	}
	
	public static EntityAIHandler getEntityAIHandler(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			EntityAIHandler entityAIHandler = pack.getEntityAIHandler(name);
			if(entityAIHandler != null)
				return entityAIHandler;
		}
		return null;
	}
	
	public static Animation getAnimation(String name) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			Animation animation = pack.getAnimation(name);
			if(animation != null)
				return animation;
		}
		return null;
	}
	
	public static PaintingVariant getPaintingVariant(String id) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			PaintingVariant variant = pack.getPaintingVariant(id);
			if(variant != null)
				return variant;
		}
		return null;
	}
	
	public static List<EntitySpawner> getEntitySpawners(){
		List<EntitySpawner> spawners = new ArrayList<EntitySpawner>();
		
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			pack.getEntitySpawners(spawners);
		}
		
		return spawners;
	}
	
	public static int getDefaultColour(String texture) {
		Integer colour = defaultColours.getOrDefault(texture, null);
		if(colour == null) {
			synchronized(mutex) {
				colour = defaultColours.getOrDefault(texture, null);
				if(colour != null)
					return colour;
			}
			colour = 0;
			try {
				File texFile = getTexture(texture);
				if(texFile != null && texFile.exists()) {
					BufferedImage tex = ImageReader.readImage(texFile);
					if(tex != null) {
						float r = 0.0f;
						float g = 0.0f;
						float b = 0.0f;
						float weight = 0.0f;
						for(int i = 0; i < tex.getWidth(); ++i) {
							for(int j = 0; j < tex.getHeight(); ++j) {
								Color color = new Color(tex.getRGB(i, j));
								if(color.getAlpha() > 0) {
									r += color.getRed();
									g += color.getGreen();
									b += color.getBlue();
									weight += 1.0f;
								}
							}
						}
						if(weight > 0.0f) {
							r /= weight;
							g /= weight;
							b /= weight;
						}
						colour = new Color((int)r, (int)g, (int)b).getRGB();
					}
				}
			} catch (Exception ex) {
				//System.out.println(texture);
				//ex.printStackTrace();
			}
			synchronized(mutex) {
				defaultColours.put(texture, colour);
			}
		}
		return colour;
	}
	
	public static Font getFont(String id) {
		ResourcePack pack = null;
		for(int i = 0; i < activeResourcePacks.size(); ++i) {
			pack = activeResourcePacks.get(i);
			Font font = pack.getFont(id);
			if(font != null)
				return font;
		}
		return null;
	}
	
}
