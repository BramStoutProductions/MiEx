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

import java.awt.image.BufferedImage;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints;

public class BiomeJavaEdition extends Biome{
	
	private JsonObject data;
	
	public BiomeJavaEdition(String name, int id, JsonObject data) {
		super(name, id);
		this.data = data;
		calculateTints();
	}
	
	private int parseColor(JsonElement colorObj) {
		if(colorObj.isJsonPrimitive()) {
			JsonPrimitive colorPrim = colorObj.getAsJsonPrimitive();
			if(colorPrim.isNumber()) {
				return colorPrim.getAsInt();
			}else if(colorPrim.isString()) {
				String colorStr = colorPrim.getAsString();
				if(colorStr.startsWith("#")) {
					return Integer.parseUnsignedInt(colorStr.substring(1), 16);
				}else {
					return Integer.parseUnsignedInt(colorStr, 16);
				}
			}
			return -1;
		}else if(colorObj.isJsonArray()) {
			JsonArray colorArray = colorObj.getAsJsonArray();
			float r = 1f;
			float g = 1f;
			float b = 1f;
			if(colorArray.size() >= 1) {
				r = g = b = colorArray.get(0).getAsFloat();
			}else if(colorArray.size() >= 2) {
				g = b = colorArray.get(1).getAsFloat();
			}else if(colorArray.size() >= 3) {
				b = colorArray.get(2).getAsFloat();
			}
			r = (float) Math.pow(r, 1.0 / 2.2);
			g = (float) Math.pow(g, 1.0 / 2.2);
			b = (float) Math.pow(b, 1.0 / 2.2);
			int ri = ((int) (r * 255.0f + 0.5f)) & 0xFF;
			int gi = ((int) (g * 255.0f + 0.5f)) & 0xFF;
			int bi = ((int) (b * 255.0f + 0.5f)) & 0xFF;
			return (ri << 16) | (gi << 8) | bi;
		}
		return -1;
	}
	
	@Override
	public void calculateTints() {
		biomeColours.clear();
		
		if(data != null) {
			String grassColourModifier = null;
			JsonObject effectsObj = data.getAsJsonObject("effects");
			if(effectsObj != null) {
				for(Entry<String, JsonElement> entry : effectsObj.entrySet()) {
					if(entry.getKey().endsWith("_color")) {
						String colorMap = entry.getKey().substring(0, entry.getKey().length() - 6);
						if(!colorMap.contains(":"))
							colorMap = "minecraft:" + colorMap;
						int color = parseColor(entry.getValue());
						if(color != -1)
							biomeColours.put(colorMap, new Color(color));
					}
				}
				
				JsonElement grassColourModifierObj = effectsObj.get("grass_color_modifier");
				if(grassColourModifierObj != null)
					grassColourModifier = grassColourModifierObj.getAsString();
			}
			
			float downfall = 0.5f;
			float temperature = 0.5f;
			
			JsonElement downfallObj = data.get("downfall");
			if(downfallObj != null)
				downfall = downfallObj.getAsFloat();
			
			JsonElement temperatureObj = data.get("temperature");
			if(temperatureObj != null)
				temperature = temperatureObj.getAsFloat();
			
			float tintX = Math.max(Math.min(temperature, 1.0f), 0.0f);
			float tintY = 1.0f - (Math.max(Math.min(downfall, 1.0f), 0.0f) * tintX);
			tintX = 1.0f - tintX;
			
			for(String colorMap : ResourcePacks.getColorMaps()) {
				if(!biomeColours.containsKey(colorMap)) {
					BufferedImage colorMapImg = Tints.getColorMap(colorMap);
					if(colorMapImg != null) {
						int tintXI = (int) (tintX * ((float) (colorMapImg.getWidth()-1)));
						int tintYI = (int) (tintY * ((float) (colorMapImg.getHeight()-1)));
						int colorI = colorMapImg.getRGB(tintXI, tintYI);
						
						if(colorMap.equals("minecraft:grass") && grassColourModifier != null) {
							if(grassColourModifier.equalsIgnoreCase("dark_forest")) {
								colorI &= 0xFEFEFE;
								int gcR = (colorI >> 16) & 0xFF;
								int gcG = (colorI >> 8) & 0xFF;
								int gcB = colorI & 0xFF;
								gcR = (gcR + 0x28) / 2;
								gcG = (gcG + 0x34) / 2;
								gcB = (gcB + 0x0A) / 2;
								colorI = (gcR << 16) | (gcG << 8) | gcB;
							}else if(grassColourModifier.equalsIgnoreCase("swamp")) {
								colorI = 0x6A7039;
							}
						}
						
						Color color = new Color(colorI);
						biomeColours.put(colorMap, color);
					}
				}
			}
		}
	}

}
