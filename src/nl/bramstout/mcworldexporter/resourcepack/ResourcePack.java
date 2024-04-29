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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import nl.bramstout.mcworldexporter.Atlas;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
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
		for(String name : names)
			if (!name.equals("base_resource_pack"))
				activeResourcePacks.add(name);
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
			"base_resource_pack/materials/minecraft/templates/base.json",
			"base_resource_pack/materials/minecraft/templates/emission.json",
			"base_resource_pack/materials/minecraft/templates/grass_block_side.json",
			"base_resource_pack/materials/minecraft/templates/grass_block_snow.json",
			"base_resource_pack/materials/minecraft/templates/redstone_wire.json",
			"UsdPreviewSurface/materials/minecraft/templates/base.json"
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
	
	private static String recurseVersionFolder(File folder) {
		/*
		 * Recursively iterate the given folder
		 * until a JAR file is found. If a directory is 
		 * found in the given folder, the function will 
		 * recurse the directory in search for the JAR file
		 */
		if (!folder.exists()) {
			return null;
		}	
		File[] contents = folder.listFiles();
		if (contents != null) {
			for (File f : folder.listFiles()) {
				if (f.isDirectory()) {
					String res = recurseVersionFolder(f);
					if (res != null)
						return res;
				}
				if (f.getName().endsWith(".jar"))
					return f.getPath();
			}
		}
		return null;
	}
	
	private static String getJarFile(String versionsFolder, String versionName) {
		String versionFolder = versionsFolder + versionName;
		File versionFolderF = new File(versionFolder);
		if (!versionFolderF.exists() || !versionFolderF.isDirectory()) {
			// Try adding a "/" to the middle of the path
			versionFolder = versionsFolder + "/" + versionName;
			versionFolderF = new File(versionFolder);
			if (!versionFolderF.exists()) {
				return null;
			}
		}	
		
		// This will return a string if the file is found, and 
		// null if not found
		return recurseVersionFolder(versionFolderF);
	}
	
	public static void updateBaseResourcePack(boolean updateToNewest) {
		try {
			String versionsFolder = FileUtil.getMinecraftVersionsDir();
			File versionsFolderF = new File(versionsFolder);
			List<String> versions = new ArrayList<String>();
			
			if (versionsFolderF.exists()) {
				String versionManifest = versionsFolder + "version_manifest_v2.json";
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
						versions.add(name);
					}
				}else if(new File(versionsFolder.substring(0, versionsFolder.length()-1)).exists()){
					// If we don't have a version manifest file, but we do have a versions folder,
					// just add in all folders
					File versionsFolderFile = new File(versionsFolder.substring(0, versionsFolder.length()-1));
					if(versionsFolderFile.exists() && versionsFolderFile.isDirectory()) {
						for(File f : versionsFolderFile.listFiles()) {
							if(f.isDirectory()) {
								if(getJarFile(versionsFolder, f.getName()) != null) {
									versions.add(f.getName());
								}
							}
						}
					}
				}
			} else {
				File multimcVersionsFolder = new File(FileUtil.getMultiMCRootDir(), "libraries/com/mojang/minecraft");
				File technicVersionsFolder = new File(FileUtil.getTechnicRootDir(), "modpacks");
				File modrinthVersionsFolder = new File(FileUtil.getModrinthRootDir(), "meta/versions");
				if (multimcVersionsFolder.exists()) {
					// Check multimc launchers
					versionsFolder = multimcVersionsFolder.getPath();
					for(File f : multimcVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(f.getName());
					}
				} else if (technicVersionsFolder.exists()) {
					// Check technic launchers
					versionsFolder = technicVersionsFolder.getPath();
					for(File f : technicVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(f.getName());
					}
				} else if(modrinthVersionsFolder.exists()) {
					// Check modrinth launchers
					versionsFolder = modrinthVersionsFolder.getPath();
					for(File f : modrinthVersionsFolder.listFiles()) {
						if(f.isDirectory())
							versions.add(f.getName());
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
			
			String versionJar = getJarFile(versionsFolder, (String) selectedValue);
			
			extractResourcePackFromJar(new File(versionJar), new File(FileUtil.getResourcePackDir(), "base_resource_pack"));
		    
		    // Write out packInfo file
		    JsonWriter writer = new JsonWriter(new FileWriter(new File(FileUtil.getResourcePackDir() + "base_resource_pack/packInfo.json")));
		    writer.beginObject();
		    writer.name("version");
		    writer.value((String) selectedValue);
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

}
