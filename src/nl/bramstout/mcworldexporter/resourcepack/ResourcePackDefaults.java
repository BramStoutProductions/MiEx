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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

import nl.bramstout.mcworldexporter.BuiltInFiles;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.ConfigDefaults;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.launcher.HytaleVersion;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherHytale;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;
import nl.bramstout.mcworldexporter.launcher.MinecraftVersion;
import nl.bramstout.mcworldexporter.model.Direction;

public class ResourcePackDefaults {
	
	public static void setupDefaults() {
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Setting up...");
		try {
			BuiltInFiles.setupBuiltInFiles(false);
			
			ConfigDefaults.createBaseConfigFile();
			
			// Get the base resource pack from the minecraft install if needed.
			if(!(new File(FileUtil.getResourcePackDir() + "base_resource_pack/packInfo.json").exists())) {
				MCWorldExporter.getApp().getUI().setEnabled(false);
				try {
					updateBaseResourcePack(true);
					MCWorldExporter.getApp().getUI().getResourcePackManager().reset(true);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				MCWorldExporter.getApp().getUI().setEnabled(true);
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
	}
	
	public static void updateBaseResourcePack(boolean updateToNewest) {
		try {
			System.out.println("Installing base_resource_pack.");
			List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
			for(Launcher launcher : LauncherRegistry.getLaunchers()) {
				versions.addAll(launcher.getVersions());
			}
			List<String> versionLabels = new ArrayList<String>();
			for(MinecraftVersion version : versions)
				versionLabels.add(version.getLabel());
			
			
			if(versions.isEmpty()) {
				System.out.println("Could not find a Minecraft Java Edition install.");
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not find a Minecraft Java Edition install with valid installed versions and so cannot automatically create a base_resource_pack. Either launch the latest version of Minecraft, manually create the base_resource_pack or specify the MIEX_MINECRAFT_ROOT_DIR environment variable and start MiEx again.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			Object selectedValue = versionLabels.get(0);
			Date selectedReleaseDate = versions.get(0).getReleaseTime();
			// Let's default to the latest release data.
			for(MinecraftVersion version : versions) {
				if(version.getReleaseTime().after(selectedReleaseDate)) {
					selectedValue = version.getLabel();
					selectedReleaseDate = version.getReleaseTime();
				}
			}
			
			if(!updateToNewest) {
				// We're not doing a forced update to the latest version,
				// so let the user select which version.
				selectedValue = JOptionPane.showInputDialog(MCWorldExporter.getApp().getUI(),
			             "Update to version", "Version",
			             JOptionPane.INFORMATION_MESSAGE, null,
			             versionLabels.toArray(), selectedValue);
				if(selectedValue == null)
					return;
			}
			
			String selectedVersion = (String) selectedValue;
			
			File versionJar = null;
			for(MinecraftVersion version : versions) {
				if(version.getLabel().equals(selectedVersion)) {
					versionJar = version.getJarFile();
					break;
				}
			}
			if(versionJar == null) {
				System.out.println("No version jar found for selected version " + selectedVersion);
				return;
			}
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.3f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Updating base resource pack");
			
			int worldVersion = getWorldVersionFromJar(versionJar);
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.35f);
			
			System.out.println("Extracting base_resource_pack from " + versionJar.getAbsolutePath());
			extractResourcePackFromJar(versionJar, new File(FileUtil.getResourcePackDir(), "base_resource_pack"));
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.45f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Patching base resource pack");
			Patcher.load();
			Patcher.patch();
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.7f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Infering miex_config.json");
			ResourcePackDefaults.inferMiExConfigFromResourcePack(new File(FileUtil.getResourcePackDir(), "base_resource_pack"));
		    
		    // Write out packInfo file
		    JsonWriter writer = new JsonWriter(new FileWriter(new File(FileUtil.getResourcePackDir() + "base_resource_pack/packInfo.json")));
		    writer.beginObject();
		    writer.name("version");
		    writer.value(selectedVersion);
		    writer.name("worldVersion");
		    writer.value(worldVersion);
		    writer.endObject();
		    writer.close();
		    
		    MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
		    MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		    
		    System.out.println("base_resource_pack updated successfully.");
		    JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "base_resource_pack updated successfully");
		}catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not update base_resource_pack", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void updateBaseResourcePackHytale(boolean updateToNewest) {
		try {
			System.out.println("Installing base_resource_pack_hytale.");
			List<HytaleVersion> versions = new ArrayList<HytaleVersion>();
			for(Launcher launcher : LauncherRegistry.getLaunchers()) {
				if(launcher instanceof LauncherHytale)
					versions.addAll(((LauncherHytale)launcher).getHytaleVersions());
			}
			List<String> versionLabels = new ArrayList<String>();
			for(HytaleVersion version : versions)
				versionLabels.add(version.getLabel());
			
			
			if(versions.isEmpty()) {
				System.out.println("Could not find a Hytale install.");
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not find a Hytale install with valid installed versions and so cannot automatically create a base_resource_pack_hytale. Either launch the latest version of Hytale, manually create the base_resource_pack_hytale or specify the MIEX_HYTALE_ROOT_DIR environment variable and start MiEx again.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Take index 1 to prefer the release over pre-release
			Object selectedValue = versionLabels.get(Math.min(1, versionLabels.size() - 1));
			
			if(!updateToNewest) {
				// We're not doing a forced update to the latest version,
				// so let the user select which version.
				selectedValue = JOptionPane.showInputDialog(MCWorldExporter.getApp().getUI(),
			             "Update to version", "Version",
			             JOptionPane.INFORMATION_MESSAGE, null,
			             versionLabels.toArray(), versionLabels.get(0));
				if(selectedValue == null)
					return;
			}
			
			String selectedVersion = (String) selectedValue;
			
			File versionAssets = null;
			for(HytaleVersion version : versions) {
				if(version.getLabel().equals(selectedVersion)) {
					versionAssets = version.getAssetsFile();
					break;
				}
			}
			if(versionAssets == null) {
				System.out.println("No Assets.zip found for selected version " + selectedVersion);
				return;
			}
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Updating base resource pack hytale");
			
			System.out.println("Extracting base_resource_pack_hytale from " + versionAssets.getAbsolutePath());
			extractResourcePackHytaleFromZip(versionAssets, new File(FileUtil.getResourcePackDir(), "base_resource_pack_hytale"));
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.9f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Infering miex_config.json");
			ResourcePackDefaults.inferMiExConfigFromResourcePack(new File(FileUtil.getResourcePackDir(), "base_resource_pack_hytale"));
		    
		    MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
		    MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		    
		    System.out.println("base_resource_pack_hytale updated successfully.");
		    JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "base_resource_pack_hytale updated successfully");
		}catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not update base_resource_pack_hytale", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void extractSourcesIntoResourcePack(List<ResourcePackSource> sources, File resourcePack) {
		try {
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Installing World Resource Pack");
			
			System.out.println("Extracting " + resourcePack.getName() + " from various sources.");
			float numSources = (float) sources.size();
			float progress = 0f;
			for(ResourcePackSource source : sources) {
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress((progress / numSources) * 0.9f);
				
				extractResourcePackFromSource(source, resourcePack, (progress / numSources) * 0.9f, 0.9f / numSources);
				progress += 1f;
			}
			
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.9f);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Infering miex_config.json");
			ResourcePackDefaults.inferMiExConfigFromResourcePack(resourcePack);
			
			// Write out packInfo file
			JsonObject packInfoObj = new JsonObject();
			JsonArray sourcesArray = new JsonArray();
			for(ResourcePackSource source : sources) {
				for(String sourceUuids : source.getSourceUuids()) {
					sourcesArray.add(sourceUuids);
				}
			}
			packInfoObj.add("sources", sourcesArray);
			Json.writeJson(new File(resourcePack, "packInfo.json"), packInfoObj);
			
		    MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
		    MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		    
		    System.out.println(resourcePack.getName() + " installed successfully.");
		    
		    JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Resource Pack installed successfully");
		}catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not install resource pack", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static int getWorldVersionFromJar(File jarFile) throws IOException{
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarFile));
		try {
		    ZipEntry entry = null;
		    
		    while ((entry = zipIn.getNextEntry()) != null) {
		    	String entryName = entry.getName();
		    	if(!entryName.equals("version.json"))
		    		continue;
		    	
		    	JsonObject data = JsonParser.parseReader(new InputStreamReader(zipIn)).getAsJsonObject();
		    	if(data.has("world_version")) {
		    		zipIn.close();
		    		return data.get("world_version").getAsInt();
		    	}
		    	
		        zipIn.closeEntry();
		    }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	    zipIn.close();
	    return 0;
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
		    	
		    	try {
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
		    	}catch(Exception ex) {
		    		ex.printStackTrace();
		    	}
		    }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	    zipIn.close();
	}
	
	public static void extractResourcePackHytaleFromZip(File zipFile, File resourcePackDir) throws IOException {
		long bytesRead = 0;
		long totalSize = Files.size(zipFile.toPath());
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
		try {
		    ZipEntry entry = null;
		    byte[] bytesIn = new byte[64*1024*1024];
		    
		    while ((entry = zipIn.getNextEntry()) != null) {
		    	String entryName = entry.getName();
		    	bytesRead += entry.getCompressedSize();
		    	float progress = (float) ((((double) bytesRead) / ((double)totalSize)) * 0.8 + 0.1);
		    	MCWorldExporter.getApp().getUI().getProgressBar().setProgress(progress);
		    	
		    	if(!entryName.startsWith("Common/Blocks/") && 
		    			!entryName.startsWith("Common/Blocks/") && 
		    			!entryName.startsWith("Common/BlockTextures/") && 
		    			!entryName.startsWith("Common/Characters/") && 
		    			!entryName.startsWith("Common/Cosmetics/") && 
		    			!entryName.startsWith("Common/Items/") && 
		    			!entryName.startsWith("Common/NPC/") && 
		    			!entryName.startsWith("Common/Resources/") && 
		    			!entryName.startsWith("Common/TintGradients/") && 
		    			!entryName.startsWith("Server/BlockTypeList/") && 
		    			!entryName.startsWith("Server/Entity/") && 
		    			!entryName.startsWith("Server/Environments/") && 
		    			!entryName.startsWith("Server/Item/") && 
		    			!entryName.startsWith("Server/Models/") && 
		    			!entryName.startsWith("manifest"))
		    		continue;
		    	
		    	try {
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
		    	}catch(Exception ex) {
		    		ex.printStackTrace();
		    	}
		    }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	    zipIn.close();
	}
	
	private static void extractResourcePackFromSource(ResourcePackSource source, File resourcePack, float startProgress, float progressRange) {
		System.out.println("  Extracting source " + source.getName());
		float numFiles = (float) source.getSources().size();
		float progress = startProgress;
		for(File file : source.getSources()) {
			extractResourcePackFromSourceFile(file, resourcePack, progress, progressRange / numFiles);
			progress += progressRange / numFiles;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(progress);
		}
	}
	
	private static void extractResourcePackFromSourceFile(File source, File resourcePack, float startProgress, float progressRange) {
		// source can be one of a few things,
		// it could be a folder,
		// it could be a jar file,
		// it could be a zip file.
		// Folders we copy as is.
		// Zip files we extract all.
		// Jar files we only extract the assets and data folder.
		System.out.println("    Extracting file " + source.getPath());
		
		// TODO: We will have manifest.json and pack.mcmeta files, which will need to be merged.
		
		if(source.isDirectory()) {
			extractFolderToResourcePack(source, resourcePack, startProgress, progressRange);
		}else if(source.isFile()) {
			if(source.getName().endsWith(".zip")) {
				extractZipToResourcePack(source, resourcePack, startProgress, progressRange);
			}else if(source.getName().endsWith(".jar")) {
				extractJarToResourcePack(source, resourcePack, startProgress, progressRange);
			}
		}
	}
	
	private static void extractFolderToResourcePack(File source, File dst, float startProgress, float progressRange) {
		if(source.isDirectory()) {
			File[] files = source.listFiles();
			float numFiles = (float) files.length;
			float progress = startProgress;
			for(File file : files) {
				extractFolderToResourcePack(file, new File(dst, file.getName()), progress, progressRange / numFiles);
				progress += progressRange / numFiles;
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(progress);
			}
		}else if(source.isFile()) {
			try {
				if(!dst.getParentFile().exists())
					dst.getParentFile().mkdirs();
				Files.copy(source.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private static void extractZipToResourcePack(File source, File dst, float startProgress, float progressRange) {
		try {
			long bytesRead = 0;
			long totalSize = Files.size(source.toPath());
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(source));
			try {
			    ZipEntry entry = null;
			    byte[] bytesIn = new byte[64*1024*1024];
			    
			    while ((entry = zipIn.getNextEntry()) != null) {
			    	String entryName = entry.getName();
			    	if(entryName.startsWith("__MACOSX") || entryName.contains(".DS_Store"))
			    		continue;
			    	
			    	long size = entry.getCompressedSize();
			    	if(size == -1L)
			    		size = entry.getSize();
			    	if(size >= 0L)
			    		bytesRead += size;
			    	float progress = (float) ((((double) bytesRead) / ((double)totalSize)) * progressRange + startProgress);
			    	MCWorldExporter.getApp().getUI().getProgressBar().setProgress(progress);
			    	
			    	try {
				        File outFile = new File(dst, entryName);
				        if (!entry.isDirectory()) {
				        	File dir = outFile.getParentFile();
				        	dir.mkdirs();
				            OutputStream os = new FileOutputStream(outFile);
				            try {
				            	int read = 0;
					            while ((read = zipIn.read(bytesIn)) != -1) {
					                os.write(bytesIn, 0, read);
					                if(size == -1L)
					                	// Couldn't get the size, so increment bytesRead here.
					                	bytesRead += read;
					            }
				            }catch(Exception ex) {
				            	ex.printStackTrace();
				            }
				            os.close();
				        }
				        zipIn.closeEntry();
			    	}catch(Exception ex) {
			    		ex.printStackTrace();
			    	}
			    }
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		    zipIn.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void extractJarToResourcePack(File source, File dst, float startProgress, float progressRange) {
		try {
			long bytesRead = 0;
			long totalSize = Files.size(source.toPath());
			ZipInputStream zipIn = new ZipInputStream(new FileInputStream(source));
			try {
			    ZipEntry entry = null;
			    byte[] bytesIn = new byte[64*1024*1024];
			    
			    while ((entry = zipIn.getNextEntry()) != null) {
			    	String entryName = entry.getName();
			    	if(entryName.startsWith("__MACOSX") || entryName.contains(".DS_Store"))
			    		continue;
			    	
			    	long size = entry.getCompressedSize();
			    	if(size == -1)
			    		size = entry.getSize();
			    	if(size >= 0)
			    		bytesRead += size;
			    	float progress = (float) ((((double) bytesRead) / ((double)totalSize)) * progressRange + startProgress);
			    	MCWorldExporter.getApp().getUI().getProgressBar().setProgress(progress);
			    	
			    	if(!entryName.startsWith("assets/") && !entryName.startsWith("data/"))
			    		continue;
			    	
			    	try {
				        File outFile = new File(dst, entryName);
				        if (!entry.isDirectory()) {
				        	File dir = outFile.getParentFile();
				        	dir.mkdirs();
				            OutputStream os = new FileOutputStream(outFile);
				            try {
				            	int read = 0;
					            while ((read = zipIn.read(bytesIn)) != -1) {
					                os.write(bytesIn, 0, read);
					                if(size == -1)
					                	// Couldn't get the size, so increment bytesRead here.
					                	bytesRead += read;
					            }
				            }catch(Exception ex) {
				            	ex.printStackTrace();
				            }
				            os.close();
				        }
				        zipIn.closeEntry();
			    	}catch(Exception ex) {
			    		ex.printStackTrace();
			    	}
			    }
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		    zipIn.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
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
		
		textureCache.clear();
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.98f);
		
		File configFile = new File(resourcePack, "miex_config.json");
		
		// Time to write it out.
		// It could be that there is already a miex_config.json file,
		// in that case we want to append our stuff to it rather than
		// override it.
		JsonObject configRoot = new JsonObject();
		if(configFile.exists()) {
			try {
				configRoot = Json.read(configFile).getAsJsonObject();
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
				if(checkResourceIdentifier(el.getAsString(), val)) {
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
				if(checkResourceIdentifier(el.getAsString(), val)) {
					found = true;
					break;
				}
			}
			if(!found)
				leavesOcclusion.add(val);
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
		
		
		FileWriter writer = null;
		try {
			if(!configFile.getParentFile().exists())
				configFile.getParentFile().mkdirs();
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();
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
		
		File blockTintsFile = new File(resourcePack, "miex_block_tints.json");
		
		JsonObject blockTintsRoot = new JsonObject();
		if(blockTintsFile.exists()) {
			try {
				blockTintsRoot = Json.read(blockTintsFile).getAsJsonObject();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		for(String block : configData.grassColormapBlocks) {
			if(blockTintsRoot.has(block))
				continue;
			if(block.startsWith("minecraft:") && blockTintsRoot.has(block.substring(10)))
				continue;
			JsonObject obj = new JsonObject();
			obj.add("tint", new JsonPrimitive("minecraft:grass"));
			blockTintsRoot.add(block, obj);
		}
		
		for(String block : configData.foliageColormapBlocks) {
			if(blockTintsRoot.has(block))
				continue;
			if(block.startsWith("minecraft:") && blockTintsRoot.has(block.substring(10)))
				continue;
			JsonObject obj = new JsonObject();
			obj.add("tint", new JsonPrimitive("minecraft:foliage"));
			blockTintsRoot.add(block, obj);
		}
		
		writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();
			String jsonString = gson.toJson(blockTintsRoot);
			writer = new FileWriter(blockTintsFile);
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
	
	private static boolean checkResourceIdentifier(String a, String b) {
		if(a.startsWith("minecraft:"))
			a = a.substring(10);
		if(b.startsWith("minecraft:"))
			b = b.substring(10);
		return a.equalsIgnoreCase(b);
	}
	
	private static void miexConfigProcessFolder(String namespace, File folder, String parent, Map<String, List<String>> models) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory())
				miexConfigProcessFolder(namespace, f, parent + f.getName() + "/", models);
			else if(f.isFile() && f.getName().endsWith(".json")) {
				try {
					JsonElement data = Json.read(f);
					
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
	
	private static class ModelFaceData{
		
		private String texture;
		private float uMin;
		private float vMin;
		private float uMax;
		private float vMax;
		private int tintIndex;
		private boolean canOcclude;
		
		public ModelFaceData(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, 
								boolean hasRotation, Direction dir, JsonObject data) {
			texture = "";
			uMin = 0;
			vMin = 0;
			uMax = 1;
			vMax = 1;
			tintIndex = -1;
			if(data.has("texture"))
				texture = data.get("texture").getAsString();
			if(data.has("uv")) {
				JsonArray uvArray = data.getAsJsonArray("uv");
				uMin = Math.min(uvArray.get(0).getAsFloat(), uvArray.get(2).getAsFloat()) / 16f;
				vMin = Math.min(uvArray.get(1).getAsFloat(), uvArray.get(3).getAsFloat()) / 16f;
				uMax = Math.max(uvArray.get(0).getAsFloat(), uvArray.get(2).getAsFloat()) / 16f;
				vMax = Math.max(uvArray.get(1).getAsFloat(), uvArray.get(3).getAsFloat()) / 16f;
			}else {
				switch(dir) {
				case NORTH:
				case SOUTH:
					uMin = minX / 16f;
					vMin = minY / 16f;
					uMax = maxX / 16f;
					vMax = maxY / 16f;
					break;
				case EAST:
				case WEST:
					uMin = minZ / 16f;
					vMin = minY / 16f;
					uMax = maxZ / 16f;
					vMax = maxY / 16f;
					break;
				case UP:
				case DOWN:
					uMin = minX / 16f;
					vMin = minZ / 16f;
					uMax = maxX / 16f;
					vMax = maxZ / 16f;
					break;
				}
			}
			
			if(data.has("tintindex"))
				tintIndex = data.get("tintindex").getAsInt();
			
			canOcclude = false;
			if(!hasRotation) {
				switch(dir) {
				case NORTH:
					canOcclude = Math.abs(0f - minZ) < 0.0011f;
					break;
				case SOUTH:
					canOcclude = Math.abs(16f - maxZ) < 0.0011f;
					break;
				case EAST:
					canOcclude = Math.abs(16f - maxX) < 0.0011f;
					break;
				case WEST:
					canOcclude = Math.abs(0f - minX) < 0.0011f;
					break;
				case UP:
					canOcclude = Math.abs(16f - maxY) < 0.0011f;
					break;
				case DOWN:
					canOcclude = Math.abs(0f - minY) < 0.0011f;
					break;
				}
			}
		}
		
		public String getTexture() {
			return texture;
		}
		
		public float getMinU() {
			return uMin;
		}
		
		public float getMinV() {
			return vMin;
		}
		
		public float getMaxU() {
			return uMax;
		}
		
		public float getMaxV() {
			return vMax;
		}
		
		public int getTintIndex() {
			return tintIndex;
		}
		
		public boolean getCanOcclude() {
			return canOcclude;
		}
		
	}
	
	private static class ModelData{
		
		private Map<String, String> textures;
		private List<ModelFaceData> faces;
		
		public ModelData() {
			textures = new HashMap<String, String>();
			faces = new ArrayList<ModelFaceData>();
		}
		
		public Map<String, String> getTextures(){
			return textures;
		}
		
		public List<ModelFaceData> getFaces(){
			return faces;
		}
		
		public String resolveTexture(String texture, int counter) {
			String res = textures.getOrDefault(texture, "");
			if(res.startsWith("#")) {
				if(counter >= 16)
					return "";
				return resolveTexture(res, counter + 1);
			}
			return res;
		}
		
	}
	
	private static void miexConfigParseModel(String modelName, File resourcePack, ModelData modelData, List<String> visitedModels) {
		if(!modelName.contains(":"))
			modelName = "minecraft:" + modelName;
		// If this model file has a parent attribute, this function gets recursively called.
		// If the data isn't valid, then it could create a cycle that causes a stackoverflow.
		// This detects that and prevents it from happening.
		if(visitedModels.contains(modelName))
			return;
		visitedModels.add(modelName);
		
		String[] modelTokens = modelName.split(":");
		
		String modelPath = "assets/" + modelTokens[0] + "/models/" + modelTokens[1] + ".json";
		File modelFile = new File(resourcePack, modelPath);
		if(!modelFile.exists())
			modelFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + modelPath);
		if(!modelFile.exists())
			return;
		
		// We got the model file, now read it in.
		try {
			JsonObject data = Json.read(modelFile).getAsJsonObject();
			
			if(data.has("parent")) {
				// It has a parent, so process that one as well
				miexConfigParseModel(data.get("parent").getAsString(), resourcePack, modelData, visitedModels);
			}
			
			if(data.has("textures")) {
				for(Entry<String, JsonElement> entry : data.get("textures").getAsJsonObject().entrySet()) {
					String texture = entry.getValue().getAsString();
					if(!texture.startsWith("#") && !texture.contains(":"))
						texture = "minecraft:" + texture;
					modelData.getTextures().put("#" + entry.getKey(), texture);
				}
			}
			
			if(data.has("elements")) {
				modelData.getFaces().clear();
				for(JsonElement el : data.get("elements").getAsJsonArray().asList()) {
					if(!el.isJsonObject())
						continue;
					JsonArray from = el.getAsJsonObject().get("from").getAsJsonArray();
					JsonArray to = el.getAsJsonObject().get("to").getAsJsonArray();

					float minX = Math.min(from.get(0).getAsFloat(), to.get(0).getAsFloat());
					float minY = Math.min(from.get(1).getAsFloat(), to.get(1).getAsFloat());
					float minZ = Math.min(from.get(2).getAsFloat(), to.get(2).getAsFloat());
					float maxX = Math.max(from.get(0).getAsFloat(), to.get(0).getAsFloat());
					float maxY = Math.max(from.get(1).getAsFloat(), to.get(1).getAsFloat());
					float maxZ = Math.max(from.get(2).getAsFloat(), to.get(2).getAsFloat());
					
					boolean hasRotation = false;
					if(el.getAsJsonObject().has("rotation")) {
						if(el.getAsJsonObject().getAsJsonObject("rotation").has("angle")) {
							if(Math.abs(el.getAsJsonObject().getAsJsonObject("rotation").get("angle").getAsFloat()) >= 1f) {
								hasRotation = true;
							}
						}
					}
					
					if(el.getAsJsonObject().has("faces")) {
						for(Entry<String, JsonElement> entry : el.getAsJsonObject().get("faces").getAsJsonObject().entrySet()) {
							if(entry.getValue().isJsonObject()) {
								Direction dir = Direction.getDirection(entry.getKey());
								modelData.getFaces().add(new ModelFaceData(minX, minY, minZ, maxX, maxY, maxZ, 
														hasRotation, dir, entry.getValue().getAsJsonObject()));
							}
						}
					}
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		Iterator<ModelFaceData> faceIt = modelData.getFaces().iterator();
		while(faceIt.hasNext()) {
			ModelFaceData face = faceIt.next();
			String texture = modelData.resolveTexture(face.getTexture(), 0);
			if(Config.bannedMaterials.contains(texture)) {
				faceIt.remove();
			}
		}
	}
	
	private static void miexConfigProcessModel(String blockName, String modelName, File resourcePack, MiExConfigData configData) {
		ModelData model = new ModelData();
		List<String> visitedModels = new ArrayList<String>();
		miexConfigParseModel(modelName, resourcePack, model, visitedModels);
		
		boolean usesBiomeColours = false;
		for(ModelFaceData face : model.getFaces()) {
			if(face.getTintIndex() >= 0) {
				usesBiomeColours = true;
				break;
			}
		}
		
		if(usesBiomeColours) {
			// Now let's figure out if it's the grass biome colours or foliage.
			// Pretty much everything is grass colours, except leaves.
			// So we assume that if it has "leaves" in the name, it's foliage colours
			// otherwise it's grass colours.
			if(!Config.forceNoBiomeColor.contains(blockName) && !ConfigDefaults.forceNoBiomeColor.contains(blockName)) {
				if(blockName.toLowerCase().contains("leaves")) {
					configData.foliageColormapBlocks.add(blockName);
				} else {
					configData.grassColormapBlocks.add(blockName);
				}
			}
		}
		
		miexConfigProcessTextures(blockName, resourcePack, model, configData);
	}
	
	private static Map<String, BufferedImage> textureCache = new HashMap<String, BufferedImage>();
	
	private static BufferedImage getTexture(String texture, File resourcePack) {
		BufferedImage img = textureCache.getOrDefault(texture, null);
		if(img != null)
			return img;
		
		String[] textureTokens = texture.split(":");
		if(textureTokens.length != 2)
			return null;
		
		String texturePath = "assets/" + textureTokens[0] + "/textures/" + textureTokens[1] + ".png";
		File textureFile = new File(resourcePack, texturePath);
		if(!textureFile.exists())
			textureFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + texturePath);
		if(!textureFile.exists())
			return null;
		
		img = ImageReader.readImage(textureFile);
		textureCache.put(texture, img);
		return img;
	}
	
	private static boolean hasMCMeta(String texture, File resourcePack) {
		String[] textureTokens = texture.split(":");
		if(textureTokens.length != 2)
			return false;
		
		String texturePath = "assets/" + textureTokens[0] + "/textures/" + textureTokens[1] + ".png.mcmeta";
		File textureFile = new File(resourcePack, texturePath);
		if(!textureFile.exists())
			textureFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + texturePath);
		if(!textureFile.exists())
			return false;
		return true;
	}
	
	private static boolean hasAlpha(BufferedImage texture, ModelFaceData face, boolean hasMCMeta) {
		if(!texture.getColorModel().hasAlpha())
			return false;
		
		float animFrameCount = 1f;
		if(hasMCMeta) {
			animFrameCount = texture.getHeight() / texture.getWidth();
		}
		
		int pixelXMin = (int) (face.getMinU() * texture.getWidth());
		int pixelYMin = (int) (face.getMinV() * texture.getHeight() / animFrameCount);
		int pixelXMax = (int) (face.getMaxU() * texture.getWidth());
		int pixelYMax = (int) (face.getMaxV() * texture.getHeight() / animFrameCount);
		
		for(int j = pixelYMin; j < pixelYMax; ++j) {
			for(int i = pixelXMin; i < pixelXMax; ++i) {
				int x = i % texture.getWidth();
				if(x < 0)
					x += texture.getWidth();
				int y = j % texture.getHeight();
				if(y < 0)
					y += texture.getHeight();
				
				Color color = new Color(texture.getRGB(x, y), true);
				
				if(color.getAlpha() < 255) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static void miexConfigProcessTextures(String blockName, File resourcePack, 
													ModelData model, MiExConfigData configData) {
		boolean hasAlpha = false;
		for(ModelFaceData face : model.getFaces()) {
			if(!face.getCanOcclude())
				continue;
			
			String texture = model.resolveTexture(face.getTexture(), 0);
			BufferedImage textureImg = getTexture(texture, resourcePack);
			if(textureImg == null)
				continue;
			
			boolean hasMCMeta = hasMCMeta(texture, resourcePack);
			
			hasAlpha = hasAlpha(textureImg, face, hasMCMeta);
			if(hasAlpha)
				break;
		}
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
