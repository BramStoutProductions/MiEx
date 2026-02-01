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
import java.io.File;
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

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.image.ImageWriter;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class AtlasCreator {
	
	public String resourcePack;
	public List<String> excludeTextures;
	public List<String> utilityTextures;
	public int repeats;
	public int padding;
	public List<ResourcePack> sourceResourcePacks;
	
	public AtlasCreator() {
		resourcePack = "base_resource_pack";
		excludeTextures = new ArrayList<String>();
		utilityTextures = new ArrayList<String>();
		repeats = 4;
		padding = 1;
		sourceResourcePacks = new ArrayList<ResourcePack>();
	}
	
	private static class AtlasData{
		String name;
		List<Atlas.AtlasItem> items;
		int size;
		int repeats;
		int padding;
		BufferedImage img;
		PbrImage pbrImg;
		Set<String> textures;
		int scale;
		boolean lockedSize;
		
		public AtlasData() {
			name = "";
			items = new ArrayList<Atlas.AtlasItem>();
			size = 256;
			repeats = 4;
			padding = 0;
			textures = new HashSet<String>();
			scale = 1;
			lockedSize = false;
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			pbrImg = null;
			// Fill it with black
			for(int j = 0; j < size; ++j)
				for(int i = 0; i < size; ++i)
					img.setRGB(i, j, 0xFF000000);
		}
		
		public void addItem(Atlas.AtlasItem item, BufferedImage img, PbrImage pbrImg) {
			items.add(item);
			textures.add(item.name);
			
			int itemX = (int) ((item.x / item.width) * ((float) size));
			int itemY = (int) ((item.y / item.height) * ((float) size));
			
			int supposedWidth = (int) (((float) size) / item.width);
			int supposedHeight = (int) (((float) size) / item.height);
			int supposedMaxRes = Math.max(supposedWidth, supposedHeight);
			
			int width = 0;
			int height = 0;
			if(pbrImg != null) {
				width = pbrImg.getWidth();
				height = pbrImg.getHeight();
			}else {
				width = img.getWidth();
				height = img.getHeight();
			}
			int maxRes = Math.max(width, height);
			float scaling = ((float) maxRes) / ((float) supposedMaxRes);
			if(scaling > scale) {
				scaleAtlas((int) Math.ceil(scaling));
			}
			
			drawTexture(itemX, itemY, item.padding, padding, scaling, img, pbrImg);
		}
		
		public void scaleAtlas(int newScale) {
			if(size * newScale > (16 * 1024)) {
				newScale = ((16 * 1024)+(size-1)) / size;
			}
			if(newScale == scale)
				return;
			if(pbrImg != null) {
				PbrImage newImg = new PbrImageRaster(size * newScale, size * newScale);
				float scaleDiff = ((float) scale) / ((float) newScale);
				RGBA rgba = new RGBA();
				for(int j = 0; j < size * newScale; ++j) {
					for(int i = 0; i < size * newScale; ++i) {
						pbrImg.sample((int) (((float) i) * scaleDiff), (int) (((float) j) * scaleDiff), Boundary.EMPTY, rgba);
						newImg.write(i, j, Boundary.EMPTY, rgba);
					}
				}
				pbrImg = newImg;
				scale = newScale;
				return;
			}
			BufferedImage newImg = new BufferedImage(size * newScale, size * newScale, BufferedImage.TYPE_INT_ARGB);
			float scaleDiff = ((float) scale) / ((float) newScale);
			for(int j = 0; j < size * newScale; ++j) {
				for(int i = 0; i < size * newScale; ++i) {
					newImg.setRGB(i, j, img.getRGB((int) (((float) i) * scaleDiff), (int) (((float) j) * scaleDiff)));
				}
			}
			img = newImg;
			scale = newScale;
		}
		
		public boolean place(String texture, int width, int height, BufferedImage tex, PbrImage pbrTex) {
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
			//int minIntersectionHeight = Integer.MAX_VALUE;
			int minIntersectionY = Integer.MAX_VALUE;
			for(int j = height * this.padding; j <= size - (height*(this.repeats + this.padding));) {
				//minIntersectionHeight = Integer.MAX_VALUE;
				minIntersectionY = Integer.MAX_VALUE;
				for(int i = width * this.padding; i <= size - (width*(this.repeats + this.padding));) {
					intersected = intersect(i, j, width, height);
					if(intersected != null) {
						// We intersected with another texture,
						// so move to the end of that texture.
						// Also record the minimum height.
						//minIntersectionHeight = Math.min(minIntersectionHeight, 
						//					((int) ((1.0f / intersected.height) * ((float) size))) * 
						//					(intersected.padding + this.padding));
						int intersectedX = (int) ((intersected.x / intersected.width) * ((float) size));
						int intersectedY = (int) ((intersected.y / intersected.height) * ((float) size));
						int intersectedWidth = (int) ((((float) size) / intersected.width) * 
												(intersected.padding + this.padding));
						int intersectedHeight = (int) ((((float) size) / intersected.height) * 
												(intersected.padding + this.padding));
						intersectedWidth += width * this.padding;
						intersectedHeight += height * this.padding;
						i = Math.max(i + 1, intersectedX + intersectedWidth);
						minIntersectionY = Math.min(minIntersectionY, intersectedY + intersectedHeight);
						//i += Math.max(((int) (((float) size) / intersected.width)) * (intersected.padding + this.padding), 1);
						continue;
					}
					// We found a space for this texture!
					Atlas.AtlasItem item = new Atlas.AtlasItem(texture, null);
					item.width = ((float) size) / ((float) width);
					item.height = ((float) size) / ((float) height);
					item.x = (((float) i) / ((float) size)) * item.width;
					item.y = (((float) j) / ((float) size)) * item.height;
					item.padding = this.repeats;
					items.add(item);
					drawTexture(i, j, item.padding, this.padding, scaling, tex, pbrTex);
					return true;
				}
				//j += Math.max(minIntersectionHeight, 1);
				if(minIntersectionY == Integer.MAX_VALUE)
					minIntersectionY = j + 1;
				j = Math.max(j + 1, minIntersectionY);
			}
			
			// We couldn't find a place, so let's increase the atlas size;
			// But, we are going to put a maximum size of 4096 pixels.
			if(lockedSize)
				return false;
			if(size >= Config.atlasMaxResolution) {
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
			if(pbrImg != null) {
				PbrImage newImg = new PbrImageRaster(newSize * scale, newSize * scale);
				RGBA rgba = new RGBA();
				for(int j = 0; j < newSize * scale; ++j) {
					for(int i = 0; i < newSize * scale; ++i) {
						if(i < (size * scale) && j < (size * scale)) {
							pbrImg.sample(i, j, Boundary.EMPTY, rgba);
							newImg.write(i, j, Boundary.EMPTY, rgba);
						}else {
							rgba.set(0f, 1f);
							pbrImg.write(i, j, Boundary.EMPTY, rgba);
						}
					}
				}
				pbrImg = newImg;
			}else {
				BufferedImage newImg = new BufferedImage(newSize * scale, newSize * scale, BufferedImage.TYPE_INT_ARGB);
				for(int j = 0; j < newSize * scale; ++j) {
					for(int i = 0; i < newSize * scale; ++i) {
						if(i < (size * scale) && j < (size * scale))
							newImg.setRGB(i, j, img.getRGB(i, j));
						else
							newImg.setRGB(i, j, 0xFF000000);
					}
				}
				img = newImg;
			}
			
			size = newSize;
			
			// Try placing it again
			if(pbrTex != null)
				return place(texture, pbrTex.getWidth(), pbrTex.getHeight(), tex, pbrTex);
			return place(texture, tex.getWidth(), tex.getHeight(), tex, pbrTex);
		}
		
		private Atlas.AtlasItem intersect(int x, int y, int width, int height) {
			x -= width * padding;
			y -= height * padding;
			width *= (repeats + padding + padding);
			height *= (repeats + padding + padding);
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
				itemX -= itemWidth * padding;
				itemY -= itemHeight * padding;
				itemWidth *= (item.padding + padding + padding);
				itemHeight *= (item.padding + padding + padding);
				
				if(x >= (itemX + itemWidth) || (x + width) <= itemX ||
					y >= (itemY + itemHeight) || (y + height) <= itemY)
					continue;
				return item;
			}
			return null;
		}
		
		private void drawTexture(int x, int y, int repeats, int padding, float scaling, BufferedImage image, PbrImage pbrImg) {
			if(this.pbrImg == null && pbrImg != null) {
				// We need to switch to a PBR image.
				this.pbrImg = new PbrImageRaster(this.img, true);
				this.img = null;
			}
			if(this.pbrImg != null && pbrImg == null) {
				// Let's convert the non-pbr image to a pbr image.
				pbrImg = new PbrImageRaster(image, true);
			}
			if(pbrImg != null) {
				int tileWidth = (int) (((float) pbrImg.getWidth()) / scaling) * scale;
				int tileHeight = (int) (((float) pbrImg.getHeight()) / scaling) * scale;
				x -= (tileWidth * padding) / scale;
				y -= (tileHeight * padding) / scale;
				int width = tileWidth * (repeats + padding + padding);
				int height = tileHeight * (repeats + padding + padding);
				RGBA rgba = new RGBA();
				for(int j = 0; j < height; ++j) {
					for(int i = 0; i < width; ++i) {
						pbrImg.sample(((int) ((((float) i) / ((float) scale)) * scaling)) % pbrImg.getWidth(), 
									((int) ((((float) j) / ((float) scale)) * scaling)) % pbrImg.getHeight(), Boundary.EMPTY, rgba);
						
						
						// Some images have RGB values that aren't 0
						// at fully transparent areas which can show up weirdly
						// in some applications.
						if(rgba.a < 0.001f)
							rgba.set(0f);
						this.pbrImg.write(x * scale + i, y * scale + j, Boundary.EMPTY, rgba);
					}
				}
			}else {
				int tileWidth = (int) (((float) image.getWidth()) / scaling) * scale;
				int tileHeight = (int) (((float) image.getHeight()) / scaling) * scale;
				x -= (tileWidth * padding) / scale;
				y -= (tileHeight * padding) / scale;
				int width = tileWidth * (repeats + padding + padding);
				int height = tileHeight * (repeats + padding + padding);
				for(int j = 0; j < height; ++j) {
					for(int i = 0; i < width; ++i) {
						int rgb = image.getRGB(((int) ((((float) i) / ((float) scale)) * scaling)) % image.getWidth(), 
									((int) ((((float) j) / ((float) scale)) * scaling)) % image.getHeight());
						int alpha = rgb >>> 24;
						// Some images have RGB values that aren't 0
						// at fully transparent areas which can show up weirdly
						// in some applications.
						if(alpha == 0)
							rgb = 0;
						img.setRGB(x * scale + i, y * scale + j, rgb);
					}
				}
			}
		}
	}
	
	private static class AtlasKey{
		
		public String group;
		//public Materials.MaterialTemplate materialTemplate;
		
		public AtlasKey(String group){//, Materials.MaterialTemplate materialTemplate) {
			this.group = group;
			//this.materialTemplate = materialTemplate;
		}
		
		@Override
		public int hashCode() {
			//return Objects.hash(group, materialTemplate);
			return group.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof AtlasKey))
				return false;
			AtlasKey other = (AtlasKey) obj;
			return group.equals(other.group);// && 
					//materialTemplate.equals(other.materialTemplate);
		}
		
	};
	
	private Map<AtlasKey, Set<String>> atlases;
	private Map<AtlasKey, List<AtlasData>> finalAtlases;
	private Set<String> excludeFromAtlas;
	private Set<String> usedAtlasNames;
	
	public void process() {
		atlases = new HashMap<AtlasKey, Set<String>>();
		finalAtlases = new HashMap<AtlasKey, List<AtlasData>>();
		excludeFromAtlas = new HashSet<String>();
		usedAtlasNames = new HashSet<String>();
		File resourcePackFolder = new File(FileUtil.getResourcePackDir(), resourcePack);
		if(!resourcePackFolder.exists())
			resourcePackFolder.mkdirs();
		File atlasJsonFile = new File(resourcePackFolder, "miex_atlas.json");
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Generating atlases: Reading existing atlas data");
		System.out.println("Generating atlases into resource pack " + resourcePack);
		
		// If we already have atlasses generated for the resource pack,
		// then we don't want to generate new ones that put the textures
		// into completely different places.
		// Therefore, we want to load in the atlas json file and prefill
		// the atlas items.
		if(atlasJsonFile.exists()) {
			try {
				System.out.println("Existing miex_atlas.json, reading in.");
				JsonObject data = Json.read(atlasJsonFile).getAsJsonObject();
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
						AtlasKey atlasKey = new AtlasKey(item.name.contains(":item") ? "items" : "blocks");
						
						AtlasData atlas = null;
						
						List<AtlasData> atlases2 = finalAtlases.getOrDefault(atlasKey, null);
						if(atlases2 == null) {
							atlases2 = new ArrayList<AtlasData>();
							finalAtlases.put(atlasKey, atlases2);
						}
						for(AtlasData atlasTmp : atlases2) {
							if(atlasTmp.name.equals(item.atlas)) {
								atlas = atlasTmp;
								break;
							}
						}
						if(atlas == null) {
							atlas = new AtlasData();
							atlas.repeats = this.repeats;
							atlas.padding = this.padding;
							atlas.name = item.atlas;
							
							// Get the size of it
							String[] tokens = atlas.name.split(":");
							File texFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".png");
							if(texFile.exists()) {
								try {
									BufferedImage img = ImageReader.readImage(texFile);
									if(img != null) {
										atlas.size = img.getWidth();
										atlas.img = new BufferedImage(atlas.size, atlas.size, BufferedImage.TYPE_INT_ARGB);
										// Fill it with black
										for(int j = 0; j < atlas.size; ++j)
											for(int i = 0; i < atlas.size; ++i)
												img.setRGB(i, j, 0xFF000000);
									}
								}catch(Exception ex) {
									ex.printStackTrace();
								}
							}else {
								texFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".exr");
								if(texFile.exists()) {
									try {
										PbrImage img = ImageReader.readPbrImage(texFile, false);
										if(img != null) {
											atlas.size = img.getWidth();
											atlas.img = null;
											atlas.pbrImg = img;
											RGBA rgba = new RGBA(0f, 1f);
											for(int j = 0; j < atlas.size; ++j)
												for(int i = 0; i < atlas.size; ++i)
													img.write(i, j, Boundary.EMPTY, rgba);
										}
									}catch(Exception ex) {
										ex.printStackTrace();
									}
								}
							}
							atlases2.add(atlas);
							usedAtlasNames.add(atlas.name);
						}
						
						atlas.lockedSize = false;
						try {
							File texFile = ResourcePacks.getTexture(item.name);
							if(texFile != null && texFile.exists()) {
								BufferedImage img = null;
								PbrImage pbrImg = null;
								if(texFile.getName().endsWith(".exr")) {
									pbrImg = ImageReader.readPbrImage(texFile, false);
								}else {
									img = ImageReader.readImage(texFile);
								};
								atlas.addItem(item, img, pbrImg);
							}
						}catch(Exception ex) {
							ex.printStackTrace();
						}
						atlas.lockedSize = true;
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.2f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Generating atlases: Finding textures");
		
		List<ResourcePack> resourcePacks = sourceResourcePacks;
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			for(File rootFolder : resourcePacks.get(i).getFoldersReversed()) {
				File assetsFolder = new File(rootFolder, "assets");
				if(assetsFolder.exists() && assetsFolder.isDirectory()) {
					// Java Edition resource packs.
					for(File f : assetsFolder.listFiles()) {
						if(!f.isDirectory())
							continue;
						if(f.getName().equalsIgnoreCase("miex"))
							continue;
						processNamespace(f.getName(), assetsFolder);
					}
				}
				File texturesFolder = new File(rootFolder, "textures");
				if(texturesFolder.exists() && texturesFolder.isDirectory()) {
					// Bedrock Edition resource packs
					File blocksFolder = new File(texturesFolder, "blocks");
					if(blocksFolder.exists())
						processFolder("blocks", "minecraft", texturesFolder, "blocks");
					
					File customFolder = new File(texturesFolder, "custom");
					if(customFolder.exists())
						processFolder("custom", "minecraft", texturesFolder, "blocks");
					
	
					File itemsFolder = new File(texturesFolder, "items");
					if(itemsFolder.exists())
						processFolder("items", "minecraft", texturesFolder, "items");
				}
				File commonFolder = new File(rootFolder, "Common");
				if(commonFolder.exists() && commonFolder.isDirectory()) {
					File blocksFolder = new File(commonFolder, "Blocks");
					if(blocksFolder.exists())
						processFolder("Blocks", "hytale", commonFolder, "blocks");
					File blockTexturesFolder = new File(commonFolder, "BlockTextures");
					if(blockTexturesFolder.exists())
						processFolder("BlockTextures", "hytale", commonFolder, "blocks");
					File resourcesFolder = new File(commonFolder, "Resources");
					if(resourcesFolder.exists())
						processFolder("Resources", "hytale", commonFolder, "blocks");
				}
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.3f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Generating atlases: Creating atlases");
		
		// We have processed all files and ordered them by template,
		// now we need to add them into the actual atlasses.
		String atlasPrefix = "miex:block/atlas_" + Integer.toHexString(resourcePack.hashCode()) + "_";
		float numAtlases = (float) atlases.size();
		float counter = 0f;
		for(Entry<AtlasKey, Set<String>> entry : atlases.entrySet()) {
			if(entry.getValue().size() <= 3) {
				// If it's three or less textures,
				// then it's not worth turning it into an atlas.
				for(String texture : entry.getValue())
					excludeFromAtlas.add(texture);
				counter += 1f;
				continue;
			}
			
			List<AtlasData> atlases2 = finalAtlases.getOrDefault(entry.getKey(), null);
			if(atlases2 == null) {
				atlases2 = new ArrayList<AtlasData>();
				finalAtlases.put(entry.getKey(), atlases2);
			}
			int atlasStartIndex = 0;
			
			float numTextures = (float) entry.getValue().size();
			float texCounter = 0f;
			for(String texture : entry.getValue()) {
				float progress = ((texCounter / numTextures) + counter) / numAtlases;
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.3f + progress * 0.3f);
				texCounter += 1f;
				if(excludeFromAtlas.contains(texture))
					continue;
				
				int atlasIndex = atlasStartIndex;
				boolean inAtlas = false;
				for(int i = 0; i < atlases2.size(); ++i) {
					if(atlases2.get(i).textures.contains(texture)) {
						inAtlas = true;
						break;
					}
				}
				if(inAtlas)
					continue;
				
				File texFile = ResourcePacks.getTexture(texture);
				if(texFile == null || !texFile.exists())
					continue;
				
				try {
					BufferedImage img = null;
					PbrImage pbrImg = null;
					int width = 0;
					int height = 0;
					if(texFile.getName().endsWith(".exr")) {
						pbrImg = ImageReader.readPbrImage(texFile, false);
						width = pbrImg.getWidth();
						height = pbrImg.getHeight();
					}else {
						img = ImageReader.readImage(texFile);
						width = img.getWidth();
						height = img.getHeight();
					}
					
					boolean placed = false;
					for(; atlasIndex < atlases2.size(); ++atlasIndex) {						
						// Place it.
						boolean success = atlases2.get(atlasIndex).place(texture, width, height, img, pbrImg);
						if(success) {
							atlasStartIndex = atlasIndex;
							placed = true;
							break;
						}
						
						// It couldn't place it, so check if the atlas is full.
						if(atlases2.get(atlasIndex).items.size() < 4) {
							// Texture was probably to big, so we exclude it
							// from the atlases.
							excludeFromAtlas.add(texture);
							placed = true;
						}
					}
					if(!placed) {
						// It couldn't place the atlas, so add in another atlas.
						AtlasData atlas = new AtlasData();
						atlas.repeats = this.repeats;
						atlas.padding = this.padding;
						for(int i = 0; i < 10000; ++i) {
							atlas.name = atlasPrefix + Integer.toString(i);
							if(!usedAtlasNames.contains(atlas.name)) {
								usedAtlasNames.add(atlas.name);
								break;
							}
						}
						atlases2.add(atlas);
						
						boolean success = atlas.place(texture, width, height, img, pbrImg);
						if(!success) {
							// Still couldn't place it in an empty atlas, so exclude it.
							excludeFromAtlas.add(texture);
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			counter += 1f;
		}
		
		int totalAtlases = 0;
		for(Entry<AtlasKey, List<AtlasData>> entry : finalAtlases.entrySet()) {
			for(AtlasData atlas : entry.getValue()) {
				totalAtlases++;
				finishUpAtlas(atlas, resourcePackFolder);
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.6f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Generating atlases: Making utility atlases");
		float totalUtilityTextures = utilityTextures.size() * totalAtlases;
		
		// Make the utility atlases
		float counter2 = 0;
		for(String utilitySuffix : utilityTextures) {
			for(Entry<AtlasKey, List<AtlasData>> entry : finalAtlases.entrySet()) {
				for(AtlasData atlas : entry.getValue()) {
					counter2 += 1f;
					MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.6f + (counter2 / totalUtilityTextures) * 0.25f);
					AtlasData utilityAtlas = new AtlasData();
					utilityAtlas.name = atlas.name + utilitySuffix;
					utilityAtlas.repeats = atlas.repeats;
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
						utilityItem.name = utilityItem.name + utilitySuffix;
						
						try {
							File texFile = ResourcePacks.getTexture(utilityItem.name);
							if(texFile != null && texFile.exists()) {
								BufferedImage img = null;
								PbrImage pbrImg = null;
								if(texFile.getName().endsWith(".exr")) {
									pbrImg = ImageReader.readPbrImage(texFile, false);
								}else {
									img = ImageReader.readImage(texFile);
								}
								if(img != null || pbrImg != null) {
									utilityAtlas.addItem(utilityItem, img, pbrImg);
									written = true;
								}
							}
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
					
					if(written)
						finishUpAtlas(utilityAtlas, resourcePackFolder);
				}
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.95f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Generating atlases: Writing atlas.json");
		
		// We now have a bunch of atlasses, so let's write out the json file.
		JsonObject root = new JsonObject();
		for(String texture : excludeFromAtlas) {
			root.add(texture, null);
		}
		for(Entry<AtlasKey, List<AtlasData>> entry : finalAtlases.entrySet()) {
			for(AtlasData atlas : entry.getValue()) {
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
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		System.out.println("Atlases generated successfully.");
	}
	
	private void finishUpAtlas(AtlasData atlas, File resourcePackFolder) {
		String[] tokens = atlas.name.split(":");
		File atlasFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".png");
		if(atlas.pbrImg != null)
			atlasFile = new File(resourcePackFolder, "assets/" + tokens[0] + "/textures/" + tokens[1] + ".exr");
		atlasFile.getParentFile().mkdirs();
		try {
			if(atlas.pbrImg != null) {
				ImageWriter.writeImage(atlasFile, atlas.pbrImg);
			}else {
				ImageIO.write(atlas.img, "png", atlasFile);
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void processNamespace(String namespace, File assetsFolder) {
		File namespaceFolder = new File(assetsFolder, namespace);
		File blocksFolder = new File(namespaceFolder, "textures/block");
		if(blocksFolder.exists())
			processFolder("block", namespace, new File(namespaceFolder, "textures"), "blocks");
		
		File customFolder = new File(namespaceFolder, "textures/custom");
		if(customFolder.exists())
			processFolder("custom", namespace, new File(namespaceFolder, "textures"), "blocks");
		
		File optifineFolder = new File(namespaceFolder, "optifine/ctm");
		if(optifineFolder.exists())
			processFolder("ctm", "optifine;" + namespace, new File(namespaceFolder, "optifine"), "blocks");
		

		File itemsFolder = new File(namespaceFolder, "textures/item");
		if(itemsFolder.exists())
			processFolder("item", namespace, new File(namespaceFolder, "textures"), "items");
	}
	
	public void processFolder(String folder, String namespace, File texturesFolder, String group) {
		File folderFile = new File(texturesFolder, folder);
		for(String fileStr : folderFile.list()) {
			File file = new File(folderFile, fileStr);
			if(file.isDirectory())
				processFolder(folder + "/" + fileStr, namespace, texturesFolder, group);
			else if(!file.isFile())
				continue;
			if(!file.getName().toLowerCase().endsWith(".png"))
				continue;
			
			String resourceName = namespace + ":" + folder + "/" + fileStr.split("\\.")[0];
			// Ignore animated textures.
			if(new File(folderFile, fileStr + ".mcmeta").exists()) {
				MCMeta mcmeta = ResourcePacks.getMCMeta(resourceName);
				if(mcmeta != null && (mcmeta.isAnimate() || mcmeta.isInterpolate())) {
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
			
			//Materials.MaterialTemplate template = Materials.getMaterial(resourceName, false, "");
			AtlasKey atlasKey = new AtlasKey(group);//, template);
			Set<String> atlas = atlases.get(atlasKey);
			if(atlas == null) {
				atlas = new HashSet<String>();
				atlases.put(atlasKey, atlas);
			}
			atlas.add(resourceName);
		}
	}
	
}
