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

package nl.bramstout.mcworldexporter.resourcepack;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class ResourcePack {

	private static List<String> activeResourcePacks = new ArrayList<String>();
	private static BufferedImage grassColorMap = null;
	private static BufferedImage foliageColorMap = null;
	private static Object mutex = new Object();
	private static Map<String, Integer> defaultColours = new HashMap<String, Integer>();

	public static void setActiveResourcePacks(List<String> names) {
		activeResourcePacks.clear();
		for(String name : names) {
			if (name.equals("base_resource_pack"))
				continue;
			if(!(new File(FileUtil.getResourcePackDir(), name)).exists())
				continue;
			activeResourcePacks.add(name);
		}
		grassColorMap = null;
		foliageColorMap = null;
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
	}

	public static List<String> getActiveResourcePacks() {
		return activeResourcePacks;
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
				BufferedImage tex = ImageIO.read(getFile(texture, "textures", ".png", "assets"));
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

	public static BufferedImage getGrassColorMap() {
		if (grassColorMap != null)
			return grassColorMap;
		synchronized(mutex) {
			if (grassColorMap != null)
				return grassColorMap;
			try {
				grassColorMap = ImageIO.read(getFile("minecraft:colormap/grass", "textures", ".png", "assets"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return grassColorMap;
		}
	}
	
	public static BufferedImage getFoliageColorMap() {
		if (foliageColorMap != null)
			return foliageColorMap;
		synchronized(mutex) {
			if (foliageColorMap != null)
				return foliageColorMap;
			try {
				foliageColorMap = ImageIO.read(getFile("minecraft:colormap/foliage", "textures", ".png", "assets"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return foliageColorMap;
		}
	}

	public static File getFile(String resource, String type, String extension, String category) {
		String path = getFilePath(resource, type, extension, category);
		File file = new File(path);
		for (String pack : activeResourcePacks) {
			file = new File(path.replace("/base_resource_pack/", "/" + pack + "/"));
			if (file.exists())
				return file;
		}
		return new File(path);
	}

	public static String getFilePath(String resource, String type, String extension, String category) {
		if(resource.contains(";")) {
			// MiEx specific stuff, in some cases (like with Optifine connected textures)
			// textures might not be located in the textures folder, but the optifine folder.
			// This means that the "type" is different. In order to facilitate this,
			// resource identifiers can override the default type by prefixing the
			// resource identifier with the type and then a semicolon.
			String[] tokens = resource.split(";");
			if(tokens.length == 2) {
				type = tokens[0];
				resource = tokens[1];
			}
		}
		if (!resource.contains(":"))
			resource = "minecraft:" + resource;
		String[] tokens = resource.split(":");
		String path = tokens[1];
		for(int i = 2; i < tokens.length; ++i)
			path = path + "/" + tokens[i];
		return FileUtil.getResourcePackDir() + "/base_resource_pack/" + category + "/" + tokens[0] + "/" + type + "/" + path + extension;
	}

	public static JsonObject getJSONData(String resource, String type, String category) {
		File file = getFile(resource, type, ".json", category);
		if (!file.exists())
			return null;
		try {
			JsonReader reader = new JsonReader(new BufferedReader(new FileReader(file)));
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean hasOverride(String resource, String type, String extension, String category) {
		if (activeResourcePacks.isEmpty())
			return false;
		String path = getFilePath(resource, type, extension, category);
		for (String pack : activeResourcePacks) {
			File file = new File(path.replace("/base_resource_pack/", "/" + pack + "/"));
			if (file.exists())
				return true;
		}
		return false;
	}
	
	public static void setupDefaults() {
		String[] RESOURCES_OVERRIDE_IF_MISSING = new String[] {
			"base_resource_pack/miex_config.json",
			"base_resource_pack/miex_block_tints.json",
			"base_resource_pack/materials/minecraft/templates/base.json",
			"base_resource_pack/materials/minecraft/templates/emission.json",
			"base_resource_pack/materials/minecraft/templates/grass_block_side.json",
			"base_resource_pack/materials/minecraft/templates/grass_block_snow.json",
			"UsdPreviewSurface/materials/minecraft/templates/base.json",
			"UsdPreviewSurface/materials/minecraft/templates/grass_block_side.json",
			"UsdPreviewSurface/materials/minecraft/templates/water.json"
		};
		String[] RESOURCES_OVERRIDE_IF_DIR_EMPTY = new String[] {
		};
		
		try {
			
			// Get the base resource pack from the minecraft install if needed.
			if(!(new File(FileUtil.getResourcePackDir() + "base_resource_pack/packInfo.json").exists())) {
				MCWorldExporter.getApp().getUI().setEnabled(false);
				try {
					System.out.println("Installing base_resource_pack");
					ResourcePack.updateBaseResourcePack(true);
					MCWorldExporter.getApp().getUI().getResourcePackManager().reset();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				MCWorldExporter.getApp().getUI().setEnabled(true);
			}
			
			byte[] bytesIn = new byte[64*1024*1024];
			
			// Setup any files that are missing
			for(String resrc : RESOURCES_OVERRIDE_IF_MISSING) {
				File outFile = new File(FileUtil.getResourcePackDir() + resrc);
				if(!outFile.exists()) {
					InputStream in = ResourcePack.class.getClassLoader().getResourceAsStream("default_data/" + resrc);
					if(in == null)
						continue;
					
					System.out.println("Installing " + resrc);
		        	File dir = outFile.getParentFile();
		        	dir.mkdirs();
		            OutputStream os = new FileOutputStream(outFile);
		            try {
		            	int read = 0;
			            while ((read = in.read(bytesIn)) != -1) {
			                os.write(bytesIn, 0, read);
			            }
		            }catch(Exception ex) {
		            	ex.printStackTrace();
		            }
		            os.close();
				}
			}
			
			// Setup any files that are missing
			// We only do it if the directory is empty
			// Let's first parse the list to combine it based on directories
			Map<String, List<String>> folders = new HashMap<String, List<String>>();
			for(String path : RESOURCES_OVERRIDE_IF_DIR_EMPTY) {
				int pathSep = path.lastIndexOf('/');
				String folder = path.substring(0, pathSep);
				List<String> list = folders.getOrDefault(folder, null);
				if(list == null) {
					list = new ArrayList<String>();
					folders.put(folder, list);
				}
				list.add(path);
			}
			for(Entry<String, List<String>> folder : folders.entrySet()) {
				// Skip if the directory isn't empty
				File dirFile = new File(FileUtil.getResourcePackDir() + folder.getKey());
				if(dirFile.exists()) {
					File[] list = dirFile.listFiles();
					if(list != null && list.length > 0)
						continue;
				}
				
				for(String resrc : folder.getValue()) {
					File outFile = new File(FileUtil.getResourcePackDir() + resrc);
					
					InputStream in = ResourcePack.class.getClassLoader().getResourceAsStream("default_data/" + resrc);
					if(in == null)
						continue;
					
					System.out.println("Installing " + resrc);
					
		        	File dir = outFile.getParentFile();
		        	dir.mkdirs();
		            OutputStream os = new FileOutputStream(outFile);
		            try {
		            	int read = 0;
			            while ((read = in.read(bytesIn)) != -1) {
			                os.write(bytesIn, 0, read);
			            }
		            }catch(Exception ex) {
		            	ex.printStackTrace();
		            }
		            os.close();
				}
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static String getJarFile(String versionsFolder, String versionName) {
		String versionFolder = versionsFolder + versionName;
		String versionJar = versionFolder + "/" + versionName + ".jar";
		if(new File(versionJar).exists())
			return versionJar;
		// It could also be located somewhere else, so let's try a few things.
		versionJar = versionFolder + "/bin/minecraft.jar";
		if(new File(versionJar).exists())
			return versionJar;
		
		// In some cases, it might be in a sub folder with the version name
		if(new File(versionFolder + "/" + versionName).exists())
			versionFolder = versionFolder + "/" + versionName;
		
		if(!(new File(versionFolder)).exists())
			return null;
		
		// If the jar doesn't exist, just pick the first jar file in the versions folder.
		for(File f : new File(versionFolder).listFiles()) {
			if(f.getName().endsWith(".jar"))
				return f.getPath();
		}
		return null;
	}
	
	public static void updateBaseResourcePack(boolean updateToNewest) {
		try {
			Map<String, String> versionsFolders = new HashMap<String, String>();
			List<String> versions = new ArrayList<String>();
			{
				String prefixID = "MC/";
				String versionsFolder = FileUtil.getMinecraftVersionsDir();
				String versionManifest = versionsFolder + "version_manifest_v2.json";
				
				versionsFolders.put(prefixID, versionsFolder);
				
				if(new File(versionManifest).exists()) {
					JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(new File(versionManifest))))).getAsJsonObject();
					for(JsonElement e : data.get("versions").getAsJsonArray().asList()) {
						String name = e.getAsJsonObject().get("id").getAsString();
						String versionFolder = versionsFolder + name;
						if(!(new File(versionFolder).exists()))
							continue;
						if(!(new File(versionFolder).isDirectory()))
							continue;
						String versionJar = versionFolder + "/" + name + ".jar";
						if(!(new File(versionJar).exists()))
							continue;
						versions.add(prefixID + name);
					}
				}else if(new File(versionsFolder.substring(0, versionsFolder.length()-1)).exists()){
					// If we don't have a version manifest file, but we do have a versions folder,
					// just add in all folders
					File versionsFolderFile = new File(versionsFolder.substring(0, versionsFolder.length()-1));
					if(versionsFolderFile.exists() && versionsFolderFile.isDirectory()) {
						for(File f : versionsFolderFile.listFiles()) {
							if(f.isDirectory()) {
								if(getJarFile(versionsFolder, f.getName()) != null) {
									versions.add(prefixID + f.getName());
								}
							}
						}
					}
				}
			}
			
			// Check multimc launchers
			if(!FileUtil.getMultiMCRootDir().equals("")) {
				String prefixId = "MultiMC/";
				
				File multimcVersionsFolder = new File(FileUtil.getMultiMCRootDir(), "libraries/com/mojang/minecraft");
				if(multimcVersionsFolder.exists()) {
					versionsFolders.put(prefixId, multimcVersionsFolder.getPath());
					for(File f : multimcVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(prefixId + f.getName());
					}
				}
			}
			
			
			// Check technic launchers
			if(!FileUtil.getTechnicRootDir().equals("")) {
				String prefixId = "Technic/";
				
				File technicVersionsFolder = new File(FileUtil.getTechnicRootDir(), "modpacks");
				if(technicVersionsFolder.exists()) {
					versionsFolders.put(prefixId, technicVersionsFolder.getPath());
					for(File f : technicVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(prefixId + f.getName());
					}
				}
			}
			
			// Check modrinth launchers
			if(!FileUtil.getModrinthRootDir().equals("")) {
				String prefixId = "Modrinth/";
				
				File modrinthVersionsFolder = new File(FileUtil.getModrinthRootDir(), "meta/versions");
				if(modrinthVersionsFolder.exists()) {
					versionsFolders.put(prefixId, modrinthVersionsFolder.getPath());
					for(File f : modrinthVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(prefixId + f.getName());
					}
				}
			}
			
			if(versions.isEmpty()) {
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not find a Minecraft Java Edition install with valid installed versions and so cannot automatically create a base_resource_pack. Either launch the latest version of Minecraft, manually create the base_resource_pack or specify the MIEX_MINECRAFT_VERSIONS_DIR environment variable and start MiEx again.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			Object selectedValue = versions.get(0);
			
			if(!updateToNewest) {
				// We're not doing a forced update to the latest version,
				// so let the user select which version.
				selectedValue = JOptionPane.showInputDialog(MCWorldExporter.getApp().getUI(),
			             "Update to version", "Version",
			             JOptionPane.INFORMATION_MESSAGE, null,
			             versions.toArray(), versions.get(0));
				if(selectedValue == null)
					return;
			}
			
			String selectedVersion = (String) selectedValue;
			
			String versionJar = getJarFile(versionsFolders.get(selectedVersion.split("\\/")[0] + "/"), selectedVersion.split("\\/")[1]);
			
			extractResourcePackFromJar(new File(versionJar), new File(FileUtil.getResourcePackDir(), "base_resource_pack"));
		    
		    // Write out packInfo file
		    JsonWriter writer = new JsonWriter(new FileWriter(new File(FileUtil.getResourcePackDir() + "base_resource_pack/packInfo.json")));
		    writer.beginObject();
		    writer.name("version");
		    writer.value(selectedVersion);
		    writer.endObject();
		    writer.close();
		    
		    JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "base_resource_pack updated successfully");
		}catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not update base_resource_pack", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void extractResourcePackFromJar(File jarFile, File resourcePackDir) throws IOException {
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarFile));
		try {
		    ZipEntry entry = null;
		    byte[] bytesIn = new byte[64*1024*1024];
		    
		    while ((entry = zipIn.getNextEntry()) != null) {
		    	String entryName = entry.getName();
		    	if(!entryName.startsWith("assets/") && !entryName.startsWith("data/"))
		    		continue;
		    	
		        File outFile = new File(resourcePackDir, entryName);
		        if (!entry.isDirectory()) {
		        	File dir = outFile.getParentFile();
		        	dir.mkdirs();
		            OutputStream os = new FileOutputStream(outFile);
		            try {
		            	int read = 0;
			            while ((read = zipIn.read(bytesIn)) != -1) {
			                os.write(bytesIn, 0, read);
			            }
		            }catch(Exception ex) {
		            	ex.printStackTrace();
		            }
		            os.close();
		        }
		        zipIn.closeEntry();
		    }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	    zipIn.close();
	}
	
	private static class MiExConfigData{
		Set<String> transparentOcclusion = new HashSet<String>();
		Set<String> leavesOcclusion = new HashSet<String>();
		Set<String> grassColormapBlocks = new HashSet<String>();
		Set<String> foliageColormapBlocks = new HashSet<String>();
	}
	
	public static void inferMiExConfigFromResourcePack(File resourcePack) {
		// Go through all of the block states and gather the models per block state.
		Map<String, List<String>> models = new HashMap<String, List<String>>();
		File assetsFolder = new File(resourcePack, "assets");
		if(assetsFolder.exists() && assetsFolder.isDirectory()) {
			for(File namespace : assetsFolder.listFiles()) {
				if(!namespace.isDirectory())
					continue;
				File blockStates = new File(namespace, "blockstates");
				if(!blockStates.exists() || !blockStates.isDirectory())
					continue;
				miexConfigProcessFolder(namespace.getName(), blockStates, "", models);
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.93f);
		
		MiExConfigData configData = new MiExConfigData();
		
		// Now parse the models
		float counter = 0;
		float numBlocks = (float) models.size();
		for(Entry<String, List<String>> block : models.entrySet()) {
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.94f + 0.05f * (counter / numBlocks));
			counter++;
			for(String model : block.getValue()) {
				miexConfigProcessModel(block.getKey(), model, resourcePack, configData);
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.98f);
		
		File configFile = new File(resourcePack, "miex_config.json");
		
		// Time to write it out.
		// It could be that there is already a miex_config.json file,
		// in that case we want to append our stuff to it rather than
		// override it.
		JsonObject configRoot = new JsonObject();
		if(configFile.exists()) {
			try {
				configRoot = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(configFile)))).getAsJsonObject();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		JsonArray transparentOcclusion = new JsonArray();
		if(configRoot.has("transparentOcclusion.add"))
			transparentOcclusion = configRoot.get("transparentOcclusion.add").getAsJsonArray();
		if(configRoot.has("transparentOcclusion"))
			transparentOcclusion = configRoot.get("transparentOcclusion").getAsJsonArray();
		for(String val : configData.transparentOcclusion) {
			boolean found = false;
			for(JsonElement el : transparentOcclusion.asList()) {
				if(el.getAsString().equals(val)) {
					found = true;
					break;
				}
			}
			if(!found)
				transparentOcclusion.add(val);
		}
		
		JsonArray leavesOcclusion = new JsonArray();
		if(configRoot.has("leavesOcclusion.add"))
			leavesOcclusion = configRoot.get("leavesOcclusion.add").getAsJsonArray();
		if(configRoot.has("leavesOcclusion"))
			leavesOcclusion = configRoot.get("leavesOcclusion").getAsJsonArray();
		for(String val : configData.leavesOcclusion) {
			boolean found = false;
			for(JsonElement el : leavesOcclusion.asList()) {
				if(el.getAsString().equals(val)) {
					found = true;
					break;
				}
			}
			if(!found)
				leavesOcclusion.add(val);
		}
		
		JsonArray grassColormapBlocks = new JsonArray();
		if(configRoot.has("grassColormapBlocks.add"))
			grassColormapBlocks = configRoot.get("grassColormapBlocks.add").getAsJsonArray();
		if(configRoot.has("grassColormapBlocks"))
			grassColormapBlocks = configRoot.get("grassColormapBlocks").getAsJsonArray();
		for(String val : configData.grassColormapBlocks) {
			boolean found = false;
			for(JsonElement el : grassColormapBlocks.asList()) {
				if(el.getAsString().equals(val)) {
					found = true;
					break;
				}
			}
			if(!found)
				grassColormapBlocks.add(val);
		}
		
		JsonArray foliageColormapBlocks = new JsonArray();
		if(configRoot.has("foliageColormapBlocks.add"))
			foliageColormapBlocks = configRoot.get("foliageColormapBlocks.add").getAsJsonArray();
		if(configRoot.has("foliageColormapBlocks"))
			foliageColormapBlocks = configRoot.get("foliageColormapBlocks").getAsJsonArray();
		for(String val : configData.foliageColormapBlocks) {
			boolean found = false;
			for(JsonElement el : foliageColormapBlocks.asList()) {
				if(el.getAsString().equals(val)) {
					found = true;
					break;
				}
			}
			if(!found)
				foliageColormapBlocks.add(val);
		}
		
		// Normally we want to add the blocks to the normal vanilla blocks,
		// but if the miex_config file already there fully overrides the list
		// then we have to add it to that.
		if(configRoot.has("transparentOcclusion"))
			configRoot.add("transparentOcclusion", transparentOcclusion);
		else
			configRoot.add("transparentOcclusion.add", transparentOcclusion);
		
		if(configRoot.has("leavesOcclusion"))
			configRoot.add("leavesOcclusion", leavesOcclusion);
		else
			configRoot.add("leavesOcclusion.add", leavesOcclusion);
		
		if(configRoot.has("grassColormapBlocks"))
			configRoot.add("grassColormapBlocks", grassColormapBlocks);
		else
			configRoot.add("grassColormapBlocks.add", grassColormapBlocks);
		
		if(configRoot.has("foliageColormapBlocks"))
			configRoot.add("foliageColormapBlocks", foliageColormapBlocks);
		else
			configRoot.add("foliageColormapBlocks.add", foliageColormapBlocks);
		
		FileWriter writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			String jsonString = gson.toJson(configRoot);
			writer = new FileWriter(configFile);
			writer.write(jsonString);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(writer != null) {
			try {
				writer.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private static void miexConfigProcessFolder(String namespace, File folder, String parent, Map<String, List<String>> models) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory())
				miexConfigProcessFolder(namespace, f, parent + f.getName() + "/", models);
			else if(f.isFile() && f.getName().endsWith(".json")) {
				try {
					JsonElement data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(f))));
					
					List<String> blockModels = new ArrayList<String>();
					miexConfigFindModels(data, blockModels);
					
					models.put(namespace + ":" + parent + f.getName().replace(".json", ""), blockModels);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	private static void miexConfigFindModels(JsonElement parent, List<String> models) {
		// No need to properly parse it, we just need to find the values for the "model" keys
		if(parent.isJsonObject()) {
			for(Entry<String, JsonElement> entry : parent.getAsJsonObject().entrySet()) {
				if(entry.getKey().equalsIgnoreCase("model") && entry.getValue().isJsonPrimitive()) {
					models.add(entry.getValue().getAsString());
				} else {
					miexConfigFindModels(entry.getValue(), models);
				}
			}
		}else if(parent.isJsonArray()) {
			for(JsonElement el : parent.getAsJsonArray().asList())
				miexConfigFindModels(el, models);
		}
	}
	
	private static void miexConfigProcessModel(String blockName, String modelName, File resourcePack, MiExConfigData configData) {
		if(!modelName.contains(":"))
			modelName = "minecraft:" + modelName;
		String[] modelTokens = modelName.split(":");
		
		String modelPath = "assets/" + modelTokens[0] + "/models/" + modelTokens[1] + ".json";
		File modelFile = new File(resourcePack, modelPath);
		if(!modelFile.exists())
			modelFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + modelPath);
		if(!modelFile.exists())
			return;
		
		// We got the model file, now read it in.
		try {
			JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(modelFile)))).getAsJsonObject();
			
			if(data.has("parent")) {
				// It has a parent, so process that one as well
				miexConfigProcessModel(blockName, data.get("parent").getAsString(), resourcePack, configData);
			}
			
			if(data.has("textures")) {
				for(Entry<String, JsonElement> entry : data.get("textures").getAsJsonObject().entrySet()) {
					miexConfigProcessTexture(blockName, entry.getValue().getAsString(), resourcePack, configData);
				}
			}
			
			boolean usesBiomeColours = false;
			
			if(data.has("elements")) {
				for(JsonElement el : data.get("elements").getAsJsonArray().asList()) {
					if(!el.isJsonObject())
						continue;
					if(el.getAsJsonObject().has("faces")) {
						for(Entry<String, JsonElement> entry : el.getAsJsonObject().get("faces").getAsJsonObject().entrySet()) {
							if(entry.getValue().isJsonObject()) {
								if(entry.getValue().getAsJsonObject().has("tintindex")) {
									// This model has a tint index, and so it uses biome colours
									usesBiomeColours = true;
									break;
								}
							}
						}
					}
					if(usesBiomeColours)
						break;
				}
			}
			
			if(usesBiomeColours) {
				// Now let's figure out if it's the grass biome colours or foliage.
				// Pretty much everything is grass colours, except leaves.
				// So we assume that if it has "leaves" in the name, it's foliage colours
				// otherwise it's grass colours.
				if(blockName.toLowerCase().contains("leaves")) {
					configData.foliageColormapBlocks.add(blockName);
				}else {
					configData.grassColormapBlocks.add(blockName);
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void miexConfigProcessTexture(String blockName, String texture, File resourcePack, MiExConfigData configData) {
		if(!texture.contains(":"))
			texture = "minecraft:" + texture;
		String[] textureTokens = texture.split(":");
		if(textureTokens.length != 2)
			return;
		
		String texturePath = "assets/" + textureTokens[0] + "/textures/" + textureTokens[1] + ".png";
		File textureFile = new File(resourcePack, texturePath);
		if(!textureFile.exists())
			textureFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + texturePath);
		if(!textureFile.exists())
			return;
		
		boolean hasAlpha = FileUtil.hasAlpha(textureFile);
		if(hasAlpha) {
			// It has alpha, so we need to add it to either transparent occlusion or leaves occlusion
			boolean leavesOcclusion = blockName.toLowerCase().contains("leaves");
			if(leavesOcclusion) {
				configData.leavesOcclusion.add(blockName);
			}else {
				configData.transparentOcclusion.add(blockName);
			}
		}
	}

}
