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
import java.util.ArrayList;
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
import com.google.gson.stream.JsonWriter;

import nl.bramstout.mcworldexporter.BuiltInFiles;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.ConfigDefaults;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.launcher.Launcher;
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
					System.out.println("Installing base_resource_pack");
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
			List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
			for(Launcher launcher : LauncherRegistry.getLaunchers()) {
				versions.addAll(launcher.getVersions());
			}
			List<String> versionLabels = new ArrayList<String>();
			for(MinecraftVersion version : versions)
				versionLabels.add(version.getLabel());
			
			
			if(versions.isEmpty()) {
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not find a Minecraft Java Edition install with valid installed versions and so cannot automatically create a base_resource_pack. Either launch the latest version of Minecraft, manually create the base_resource_pack or specify the MIEX_MINECRAFT_VERSIONS_DIR environment variable and start MiEx again.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			Object selectedValue = versionLabels.get(0);
			
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
		    
		    JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "base_resource_pack updated successfully");
		}catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not update base_resource_pack", "Error", JOptionPane.ERROR_MESSAGE);
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
		
		JsonArray grassColormapBlocks = new JsonArray();
		if(configRoot.has("grassColormapBlocks.add"))
			grassColormapBlocks = configRoot.get("grassColormapBlocks.add").getAsJsonArray();
		if(configRoot.has("grassColormapBlocks"))
			grassColormapBlocks = configRoot.get("grassColormapBlocks").getAsJsonArray();
		
		JsonArray foliageColormapBlocks = new JsonArray();
		if(configRoot.has("foliageColormapBlocks.add"))
			foliageColormapBlocks = configRoot.get("foliageColormapBlocks.add").getAsJsonArray();
		if(configRoot.has("foliageColormapBlocks"))
			foliageColormapBlocks = configRoot.get("foliageColormapBlocks").getAsJsonArray();
		
		JsonArray waterColormapBlocks = new JsonArray();
		if(configRoot.has("waterColormapBlocks.add"))
			waterColormapBlocks = configRoot.get("waterColormapBlocks.add").getAsJsonArray();
		if(configRoot.has("waterColormapBlocks"))
			waterColormapBlocks = configRoot.get("waterColormapBlocks").getAsJsonArray();
		
		List<String> colormapBlocks = new ArrayList<String>();
		for(JsonElement el : grassColormapBlocks.asList()) {
			colormapBlocks.add(el.getAsString());
		}
		for(JsonElement el : foliageColormapBlocks.asList()) {
			colormapBlocks.add(el.getAsString());
		}
		for(JsonElement el : waterColormapBlocks.asList()) {
			colormapBlocks.add(el.getAsString());
		}
		
		for(String val : configData.grassColormapBlocks) {
			boolean found = false;
			for(String str : colormapBlocks) {
				if(checkResourceIdentifier(str, val)) {
					found = true;
					break;
				}
			}
			if(!found)
				grassColormapBlocks.add(val);
		}
		
		for(String val : configData.foliageColormapBlocks) {
			boolean found = false;
			for(String str : colormapBlocks) {
				if(checkResourceIdentifier(str, val)) {
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
					canOcclude = Math.abs(0f - minZ) < 0.01f;
					break;
				case SOUTH:
					canOcclude = Math.abs(16f - maxZ) < 0.01f;
					break;
				case EAST:
					canOcclude = Math.abs(16f - maxX) < 0.01f;
					break;
				case WEST:
					canOcclude = Math.abs(0f - minX) < 0.01f;
					break;
				case UP:
					canOcclude = Math.abs(16f - maxY) < 0.01f;
					break;
				case DOWN:
					canOcclude = Math.abs(0f - minY) < 0.01f;
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
	
	private static void miexConfigParseModel(String modelName, File resourcePack, ModelData modelData) {
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
			JsonObject data = Json.read(modelFile).getAsJsonObject();
			
			if(data.has("parent")) {
				// It has a parent, so process that one as well
				miexConfigParseModel(data.get("parent").getAsString(), resourcePack, modelData);
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
		miexConfigParseModel(modelName, resourcePack, model);
		
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
