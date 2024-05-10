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

package nl.bramstout.mcworldexporter.atlas;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class AtlasCreator {
	
	public String resourcePack;
	public List<String> excludeTextures;
	public List<String> utilityTextures;
	public int padding;
	
	public AtlasCreator() {
		resourcePack = "base_resource_pack";
		excludeTextures = new ArrayList<String>();
		utilityTextures = new ArrayList<String>();
		padding = 4;
	}
	
	private static class AtlasData{
		String name;
		List<Atlas.AtlasItem> items;
		int size;
		int padding;
		BufferedImage img;
		Set<String> textures;
		boolean full;
		int fullCounter;
		
		public AtlasData() {
			name = "";
			items = new ArrayList<Atlas.AtlasItem>();
			size = 256;
			padding = 4;
			textures = new HashSet<String>();
			full = false;
			fullCounter = 4;
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			// Fill it with black
			for(int j = 0; j < size; ++j)
				for(int i = 0; i < size; ++i)
					img.setRGB(i, j, 0xFF000000);
		}
		
		public void addItem(Atlas.AtlasItem item, BufferedImage img) {
			items.add(item);
			textures.add(item.name);
			
			int itemX = (int) ((item.x / item.width) * ((float) size));
			int itemY = (int) ((item.y / item.height) * ((float) size));
			
			int width = img.getWidth();
			int height = img.getHeight();
			int maxRes = Math.max(width, height);
			int scaling = 1;
			if(maxRes > Config.atlasMaxTileResolution) {
				// The texture is too big, so we need to downscale it.
				float fscaling = ((float) maxRes) / ((float) Config.atlasMaxTileResolution);
				scaling = (int) Math.ceil(fscaling);
				width /= scaling;
				height /= scaling;
			}
			
			drawTexture(itemX, itemY, item.padding, scaling, img);
		}
		
		public boolean place(String texture, int width, int height, BufferedImage tex) {
			// If we already have it, great!
			if(textures.contains(texture))
				return true;
			
			int maxRes = Math.max(width, height);
			int scaling = 1;
			if(maxRes > Config.atlasMaxTileResolution) {
				// The texture is too big, so we need to downscale it.
				float fscaling = ((float) maxRes) / ((float) Config.atlasMaxTileResolution);
				scaling = (int) Math.ceil(fscaling);
				width /= scaling;
				height /= scaling;
			}
			
			// Try to find a place that doesn't intersect with any existing textures.
			Atlas.AtlasItem intersected = null;
			int minIntersectionHeight = Integer.MAX_VALUE;
			for(int j = 0; j <= size - (height*this.padding);) {
				minIntersectionHeight = Integer.MAX_VALUE;
				for(int i = 0; i <= size - (width*this.padding);) {
					intersected = intersect(i, j, width, height);
					if(intersected != null) {
						// We intersected with another texture,
						// so move to the end of that texture.
						// Also record the minimum height.
						minIntersectionHeight = Math.min(minIntersectionHeight, 
											((int) ((1.0f / intersected.height) * ((float) size))) * intersected.padding);
						i += Math.max(((int) (((float) size) / intersected.width)) * intersected.padding, 1);
						continue;
					}
					// We found a space for this texture!
					Atlas.AtlasItem item = new Atlas.AtlasItem(texture, null);
					item.width = ((float) size) / ((float) width);
					item.height = ((float) size) / ((float) height);
					item.x = (((float) i) / ((float) size)) * item.width;
					item.y = (((float) j) / ((float) size)) * item.height;
					item.padding = this.padding;
					items.add(item);
					drawTexture(i, j, item.padding, scaling, tex);
					return true;
				}
				j += Math.max(minIntersectionHeight, 1);
			}
			
			// We couldn't find a place, so let's increase the atlas size;
			// But, we are going to put a maximum size of 4096 pixels.
			if(size >= Config.atlasMaxResolution) {
				fullCounter--;
				if(fullCounter <= 0) {
					fullCounter = 0;
					// If we couldn't place a texture x times,
					// we consider this atlas full
					full = true;
				}
				return false;
			}
			
			int newSize = size << 1;
			// We need to go through all atlas items and update the values;
			int numAtlases = items.size();
			Atlas.AtlasItem item = null;
			float itemX;
			float itemY;
			float itemWidth;
			float itemHeight;
			for(int i = 0; i < numAtlases; ++i) {
				item = items.get(i);
				itemX = (item.x / item.width) * ((float) size);
				itemY = (item.y / item.height) * ((float) size);
				itemWidth = ((float) size) / item.width;
				itemHeight = ((float) size) / item.height;
				
				item.width = ((float) newSize) / itemWidth;
				item.height = ((float) newSize) / itemHeight;
				item.x = (itemX / ((float) newSize)) * item.width;
				item.y = (itemY / ((float) newSize)) * item.height;
			}
			
			// Update the buffered image;
			BufferedImage newImg = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_ARGB);
			for(int j = 0; j < newSize; ++j) {
				for(int i = 0; i < newSize; ++i) {
					if(i < size && j < size)
						newImg.setRGB(i, j, img.getRGB(i, j));
					else
						newImg.setRGB(i, j, 0xFF000000);
				}
			}
			img = newImg;
			
			size = newSize;
			
			// Try placing it again
			return place(texture, tex.getWidth(), tex.getHeight(), tex);
		}
		
		private Atlas.AtlasItem intersect(int x, int y, int width, int height) {
			width *= padding;
			height *= padding;
			int numAtlases = items.size();
			Atlas.AtlasItem item = null;
			int itemX;
			int itemY;
			int itemWidth;
			int itemHeight;
			for(int i = 0; i < numAtlases; ++i) {
				item = items.get(i);
				itemX = (int) ((item.x / item.width) * ((float) size));
				itemY = (int) ((item.y / item.height) * ((float) size));
				itemWidth = (int) (((float) size) / item.width);
				itemHeight = (int) (((float) size) / item.height);
				itemWidth *= item.padding;
				itemHeight *= item.padding;
				
				if(x >= (itemX + itemWidth) || (x + width) <= itemX ||
					y >= (itemY + itemHeight) || (y + height) <= itemY)
					continue;
				return item;
			}
			return null;
		}
		
		private void drawTexture(int x, int y, int padding, int scaling, BufferedImage image) {
			int width = (image.getWidth() / scaling) * padding;
			int height = (image.getHeight() / scaling) * padding;
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					img.setRGB(x + i, y + j, image.getRGB((i * scaling) % image.getWidth(), (j * scaling) % image.getHeight()));
				}
			}
		}
	}
	
	private Map<Materials.MaterialTemplate, Set<String>> atlases;
	private List<AtlasData> finalAtlases;
	private Set<String> excludeFromAtlas;
	private int atlasCounter;
	
	public void process() {
		atlases = new HashMap<Materials.MaterialTemplate, Set<String>>();
		finalAtlases = new ArrayList<AtlasData>();
		excludeFromAtlas = new HashSet<String>();
		File resourcePackFolder = new File(FileUtil.getResourcePackDir(), resourcePack);
		if(!resourcePackFolder.exists())
			resourcePackFolder.mkdirs();
		File atlasJsonFile = new File(resourcePackFolder, "miex_atlas.json");
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
		
		// If we already have atlasses generated for the resource pack,
		// then we don't want to generate new ones that put the textures
		// into completely different places.
		// Therefore, we want to load in the atlas json file and prefill
		// the atlas items.
		if(atlasJsonFile.exists()) {
			try {
				JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(atlasJsonFile)))).getAsJsonObject();
				for (Entry<String, JsonElement> entry : data.entrySet()) {
					try {
						// If it's null or an empty object, then that means that texture
						// shouldn't be part of an atlas.
						if(entry.getValue().isJsonNull() || entry.getValue().getAsJsonObject().isEmpty()) {
							excludeFromAtlas.add(entry.getKey());
							continue;
						}
						Atlas.AtlasItem item = new Atlas.AtlasItem(entry.getKey(), entry.getValue().getAsJsonObject())	;
						
						// Find the atlasData for it
						AtlasData atlas = null;
						for(AtlasData atlasTmp : finalAtlases) {
							if(atlasTmp.name.equals(item.atlas)) {
								atlas = atlasTmp;
								break;
							}
						}
						if(atlas == null) {
							atlas = new AtlasData();
							atlas.padding = this.padding;
							atlas.name = item.atlas;
							
							// Get the size of it
							String[] tokens = atlas.name.split(":");
							File texFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".png");
							try {
								BufferedImage img = ImageIO.read(texFile);
								atlas.size = img.getWidth();
								atlas.img = new BufferedImage(atlas.size, atlas.size, BufferedImage.TYPE_INT_ARGB);
								// Fill it with black
								for(int j = 0; j < atlas.size; ++j)
									for(int i = 0; i < atlas.size; ++i)
										img.setRGB(i, j, 0xFF000000);
							}catch(Exception ex) {
								ex.printStackTrace();
							}
							finalAtlases.add(atlas);
						}
						
						try {
							File texFile = ResourcePack.getFile(item.name, "textures", ".png", "assets");
							if(texFile.exists()) {
								BufferedImage img = ImageIO.read(texFile);
								atlas.addItem(item, img);
							}
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.2f);
		
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File assetsFolder = new File(FileUtil.getResourcePackDir(), resourcePacks.get(i) + "/assets");
			if(!assetsFolder.exists() || !assetsFolder.isDirectory())
				continue;
			for(File f : assetsFolder.listFiles()) {
				if(!f.isDirectory())
					continue;
				if(f.getName().equalsIgnoreCase("miex"))
					continue;
				processNamespace(f.getName(), assetsFolder);
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.3f);
		
		// We have processed all files and ordered them by template,
		// now we need to add them into the actual atlasses.
		String atlasPrefix = "miex:block/atlas_" + Integer.toHexString(resourcePack.hashCode()) + "_";
		atlasCounter = 0;
		float numAtlases = (float) atlases.size();
		float counter = 0f;
		for(Entry<Materials.MaterialTemplate, Set<String>> entry : atlases.entrySet()) {
			if(entry.getValue().size() <= 3) {
				// If it's three or less textures,
				// then it's not worth turning it into an atlas.
				for(String texture : entry.getValue())
					excludeFromAtlas.add(texture);
				counter += 1f;
				continue;
			}
			
			AtlasData atlas = new AtlasData();
			atlas.padding = this.padding;
			atlas.name = atlasPrefix + ++atlasCounter;
			
			float numTextures = (float) entry.getValue().size();
			float texCounter = 0f;
			for(String texture : entry.getValue()) {
				float progress = ((texCounter / numTextures) + counter) / numAtlases;
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.3f + progress * 0.6f);
				texCounter += 1f;
				if(excludeFromAtlas.contains(texture))
					continue;
				
				File texFile = ResourcePack.getFile(texture, "textures", ".png", "assets");
				if(!texFile.exists())
					continue;
				try {
					BufferedImage img = ImageIO.read(texFile);
					int width = img.getWidth();
					int height = img.getHeight();
					
					// Place it.
					boolean success = atlas.place(texture, width, height, img);
					
					if(!success) {
						// It couldn't place it, so check if the atlas is full.
						if(atlas.full) {
							// The atlas is too full, so write it out
							// and set up a new atlas.
							finishUpAtlas(atlas, resourcePackFolder);
							finalAtlases.add(atlas);
							
							atlas = new AtlasData();
							atlas.padding = this.padding;
							atlas.name = atlasPrefix + ++atlasCounter;
						}else {
							// Texture was probably to big, so we exclude it
							// from the atlases.
							excludeFromAtlas.add(texture);
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			finishUpAtlas(atlas, resourcePackFolder);
			finalAtlases.add(atlas);
			
			counter += 1f;
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.9f);
		
		// Make the utility atlases
		for(String utilitySuffix : utilityTextures) {
			for(AtlasData atlas : finalAtlases) {
				AtlasData utilityAtlas = new AtlasData();
				utilityAtlas.name = atlas.name + "_" + utilitySuffix;
				utilityAtlas.padding = atlas.padding;
				utilityAtlas.size = atlas.size;
				utilityAtlas.img = new BufferedImage(utilityAtlas.size, utilityAtlas.size, BufferedImage.TYPE_INT_ARGB);
				// Fill it with transparency.
				// Any block that doesn't have this utility texture will be transparent,
				// so the alpha channel can be used as an optional mask in the material.
				for(int j = 0; j < utilityAtlas.size; ++j)
					for(int i = 0; i < utilityAtlas.size; ++i)
						utilityAtlas.img.setRGB(i, j, 0x00000000);
				
				boolean written = false;
				
				for(Atlas.AtlasItem item : atlas.items) {
					Atlas.AtlasItem utilityItem = new Atlas.AtlasItem(item);
					utilityItem.name = utilityItem.name + "_" + utilitySuffix;
					
					try {
						File texFile = ResourcePack.getFile(utilityItem.name, "textures", ".png", "assets");
						if(texFile.exists()) {
							BufferedImage img = ImageIO.read(texFile);
							utilityAtlas.addItem(utilityItem, img);
							written = true;
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
				
				if(written)
					finishUpAtlas(utilityAtlas, resourcePackFolder);
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.95f);
		
		// We now have a bunch of atlasses, so let's write out the json file.
		JsonObject root = new JsonObject();
		for(String texture : excludeFromAtlas) {
			root.add(texture, null);
		}
		for(AtlasData atlas : finalAtlases) {
			for(Atlas.AtlasItem item : atlas.items) {
				JsonObject obj = new JsonObject();
				obj.addProperty("atlas", atlas.name);
				obj.addProperty("x", Float.valueOf(item.x));
				obj.addProperty("y", Float.valueOf(item.y));
				obj.addProperty("width", Float.valueOf(item.width));
				obj.addProperty("height", Float.valueOf(item.height));
				obj.addProperty("padding", Integer.valueOf(item.padding));
				root.add(item.name, obj);
			}
		}
		FileWriter writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			String jsonString = gson.toJson(root);
			writer = new FileWriter(atlasJsonFile);
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
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.0f);
	}
	
	private void finishUpAtlas(AtlasData atlas, File resourcePackFolder) {
		String[] tokens = atlas.name.split(":");
		File atlasFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".png");
		atlasFile.getParentFile().mkdirs();
		try {
			ImageIO.write(atlas.img, "png", atlasFile);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void processNamespace(String namespace, File assetsFolder) {
		File namespaceFolder = new File(assetsFolder, namespace);
		File blocksFolder = new File(namespaceFolder, "textures/block");
		if(blocksFolder.exists())
			processFolder("block", namespace, new File(namespaceFolder, "textures"));
		
		File optifineFolder = new File(namespaceFolder, "optifine/ctm");
		if(optifineFolder.exists())
			processFolder("ctm", "optifine;" + namespace, new File(namespaceFolder, "optifine"));
	}
	
	public void processFolder(String folder, String namespace, File texturesFolder) {
		File folderFile = new File(texturesFolder, folder);
		for(String fileStr : folderFile.list()) {
			File file = new File(folderFile, fileStr);
			if(file.isDirectory())
				processFolder(folder + "/" + fileStr, namespace, texturesFolder);
			else if(!file.isFile())
				continue;
			if(!file.getName().toLowerCase().endsWith(".png"))
				continue;
			
			String resourceName = namespace + ":" + folder + "/" + fileStr.split("\\.")[0];
			// Ignore animated textures.
			if(new File(folderFile, fileStr + ".mcmeta").exists()) {
				MCMeta mcmeta = new MCMeta(resourceName);
				if(mcmeta.isAnimate() || mcmeta.isInterpolate()) {
					excludeFromAtlas.add(resourceName);
					continue;
				}
			}
			
			boolean skip = false;
			for(String excludeTexture : excludeTextures) {
				if(resourceName.equalsIgnoreCase(excludeTexture)) {
					skip = true;
					break;
				}
			}
			if(skip) {
				excludeFromAtlas.add(resourceName);
				continue;
			}
			for(String utilityTexture : utilityTextures) {
				if(resourceName.toLowerCase().endsWith(utilityTexture.toLowerCase())) {
					skip = true;
					break;
				}
			}
			if(skip)
				continue;
			
			Materials.MaterialTemplate template = Materials.getMaterial(resourceName, false, "");
			Set<String> atlas = atlases.get(template);
			if(atlas == null) {
				atlas = new HashSet<String>();
				atlases.put(template, atlas);
			}
			atlas.add(resourceName);
		}
	}
	
}
