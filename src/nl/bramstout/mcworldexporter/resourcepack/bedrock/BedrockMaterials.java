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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.export.GeneratedTextures;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class BedrockMaterials {
	
	private static enum BlendMode{
		
		OVERLAY, OVERLAY_TINT, ADDITIVE, ADDITIVE_TINT
		
	}
	
	private static class MaterialLayer{
		
		private BlendMode blendMode;
		private int tintIndex;
		
		public MaterialLayer(JsonObject data) {
			blendMode = BlendMode.OVERLAY;
			tintIndex = -1;
			
			if(data != null) {
				if(data.has("blendMode")) {
					try {
						blendMode = BlendMode.valueOf(data.get("blendMode").getAsString().toUpperCase());
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
				if(data.has("tintIndex")) {
					tintIndex = data.get("tintIndex").getAsInt();
				}
			}
		}
		
		public boolean isIdentity(List<Color> tints) {
			if(blendMode == BlendMode.OVERLAY)
				return true;
			if(tints == null)
				return true;
			if(tints.size() == 0)
				return true;
			Color tint = tints.get(Math.max(Math.min(tints.size() - 1, tintIndex), 9));
			if(Math.abs(tint.getR() - 1f) < 0.0001f && 
					Math.abs(tint.getG() - 1f) < 0.0001f && 
					Math.abs(tint.getB() - 1f) < 0.0001f && 
					Math.abs(tint.getA() - 1f) < 0.0001f)
				return true;
			return false;
		}
		
		public BlendMode getBlendMode() {
			return blendMode;
		}
		
		public int getTintIndex() {
			return tintIndex;
		}
		
		@Override
		public int hashCode() {
			return tintIndex;
		}
		
	}
	
	private static class BedrockMaterial{
		
		private String name;
		private List<MaterialLayer> layers;
		
		public BedrockMaterial(String name, BedrockMaterial parent, JsonObject data) {
			this.name = name;
			layers = new ArrayList<MaterialLayer>();
			if(parent != null) {
				layers.addAll(parent.layers);
			}
			if(data != null) {
				if(data.has("layers")) {
					for(JsonElement el : data.getAsJsonArray("layers").asList()) {
						if(el.isJsonObject()) {
							layers.add(new MaterialLayer(el.getAsJsonObject()));
						}
					}
				}
			}
			if(layers.isEmpty())
				layers.add(new MaterialLayer(null));
		}
		
		public String getTexture(List<String> textures, List<Color> tints) {
			if(textures.size() == 0)
				return "";
			if(layers.size() == 1) {
				if(layers.get(0).isIdentity(tints)) {
					return textures.get(0);
				}
			}
			
			//File exportDir = Exporter.currentExportFile.getParentFile();
			//File chunksFolder = new File(exportDir, Exporter.currentExportFile.getName().replace(".usd", "_chunks"));
			//File imgDir = new File(chunksFolder, "images");
			//imgDir.mkdirs();
			
			String imgName = "entity/img_" + Integer.toHexString(textures.hashCode()) + Integer.toHexString(tints.hashCode()) + 
										Integer.toHexString(layers.hashCode());
			//File imgFile = new File(imgDir, imgName + ".png");
			//if(imgFile.exists())
			//	return "./" + chunksFolder.getName() + "/" + imgDir.getName() + "/" + imgName;
			if(GeneratedTextures.textureExists(imgName))
				return GeneratedTextures.getTextureId(imgName);
			
			BufferedImage resImg = null;
			for(int i = 0; i < textures.size(); ++i) {
				MaterialLayer layer = layers.get(Math.min(i, layers.size() - 1));
				
				String texture = textures.get(i);
				File file = ResourcePacks.getTexture(texture);
				//if(file == null)
				//	file = new File(exportDir, texture);
				if(!file.exists())
					continue;
				
				BufferedImage img = ImageReader.readImage(file);
				if(img == null)
					continue;
				
				if(resImg == null) {
					resImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
				}
				
				Color tint = new Color(1f, 1f, 1f);
				int tintIndex = Math.min(layer.getTintIndex(), tints != null ? (tints.size() - 1) : -1);
				if(tintIndex < 0)
					tintIndex = Math.min(i, tints != null ? (tints.size() - 1) : -1);
				if(tintIndex >= 0)
					tint = tints.get(tintIndex);
				
				switch(layer.getBlendMode()) {
				case OVERLAY:
					compositeOverlay(resImg, img);
					break;
				case OVERLAY_TINT:
					compositeOverlayTint(resImg, img, tint);
					break;
				case ADDITIVE:
					compositeAdditive(resImg, img);
					break;
				case ADDITIVE_TINT:
					compositeAdditiveTint(resImg, img, tint);
					break;
				}
			}
			
			if(resImg == null) {
				String texturesStr = "[";
				for(String texture : textures)
					texturesStr += texture + ",";
				
				System.out.println("Bedrock material has no layers: " + name + " with textures " + texturesStr + "]");
				resImg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
			}
			
			//try {
			//	ImageIO.write(resImg, "PNG", imgFile);
			//}catch(Exception ex) {
			//	ex.printStackTrace();
			//	return "";
			//}
			
			//return "./" + chunksFolder.getName() + "/" + imgDir.getName() + "/" + imgName;
			return GeneratedTextures.writeTexture(imgName, resImg);
		}
		
		private static void compositeOverlay(BufferedImage bg, BufferedImage fg) {
			int width = Math.min(bg.getWidth(), fg.getWidth());
			int height = Math.min(bg.getHeight(), fg.getHeight());
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					Color colorBG = new Color(bg.getRGB(i, j), true, false);
					Color colorFG = new Color(fg.getRGB(i, j), true, false);
					colorBG.composite(colorFG);
					bg.setRGB(i, j, colorBG.getRGB());
				}
			}
		}
		
		private static void compositeOverlayTint(BufferedImage bg, BufferedImage fg, Color tint) {
			int width = Math.min(bg.getWidth(), fg.getWidth());
			int height = Math.min(bg.getHeight(), fg.getHeight());
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					Color colorBG = new Color(bg.getRGB(i, j), true, false);
					Color colorFG = new Color(fg.getRGB(i, j), true, false);
					colorFG.mult(tint);
					colorBG.composite(colorFG);
					bg.setRGB(i, j, colorBG.getRGB());
				}
			}
		}
		
		private static void compositeAdditive(BufferedImage bg, BufferedImage fg) {
			int width = Math.min(bg.getWidth(), fg.getWidth());
			int height = Math.min(bg.getHeight(), fg.getHeight());
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					Color colorFG = new Color(fg.getRGB(i, j), true, false);
					Color colorBG = new Color(colorFG);
					colorBG.setA(1.0f);
					colorBG.composite(colorFG);
					bg.setRGB(i, j, colorBG.getRGB());
				}
			}
		}
		
		private static void compositeAdditiveTint(BufferedImage bg, BufferedImage fg, Color tint) {
			int width = Math.min(bg.getWidth(), fg.getWidth());
			int height = Math.min(bg.getHeight(), fg.getHeight());
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					Color colorFG = new Color(fg.getRGB(i, j), true, false);
					Color colorBG = new Color(colorFG);
					colorFG.mult(tint);
					colorBG.setA(1.0f);
					colorBG.composite(colorFG);
					bg.setRGB(i, j, colorBG.getRGB());
				}
			}
		}
		
	}
	
	private static Map<String, BedrockMaterial> registry = new HashMap<String, BedrockMaterial>();
	private static BedrockMaterial defaultMaterial = new BedrockMaterial("", null, null);
	
	public static void load() {
		Map<String, BedrockMaterial> registry = new HashMap<String, BedrockMaterial>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File materialsFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/bedrock/bedrock_materials.json");
			if(!materialsFile.exists())
				continue;
			try {
				JsonObject data = Json.read(materialsFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					BedrockMaterial mat = parse(entry, data);
					
					registry.put(mat.name, mat);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BedrockMaterials.registry = registry;
	}
	
	private static BedrockMaterial parse(Entry<String, JsonElement> matData, JsonObject data) {
		String name = matData.getKey();
		String parentName = null;
		int colonIndex = name.indexOf(':');
		if(colonIndex >= 0) {
			parentName = name.substring(colonIndex + 1);
			name = name.substring(0, colonIndex);
		}
		
		BedrockMaterial parent = null;
		if(parentName != null) {
			for(Entry<String, JsonElement> entry : data.entrySet()) {
				String name2 = entry.getKey();
				int colonIndex2 = name2.indexOf(':');
				if(colonIndex2 >= 0)
					name2 = name2.substring(0, colonIndex2);
				
				if(name2.equalsIgnoreCase(parentName)) {
					parent = parse(entry, data);
					break;
				}
			}
		}
		
		return new BedrockMaterial(name, parent, matData.getValue().getAsJsonObject());
	}
	
	public static String getTexture(String material, List<String> textures, List<Color> tints) {
		BedrockMaterial bedrockMaterial = registry.getOrDefault(material, defaultMaterial);
		return bedrockMaterial.getTexture(textures, tints);
	}
	
}
