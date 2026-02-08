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
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints;

public class BiomeBedrockEdition extends Biome{

	private JsonObject data;
	
	public BiomeBedrockEdition(String name, int id, JsonObject data) {
		super(name, id);
		this.data = data;
		calculateTints();
	}
	
	public BiomeBedrockEdition(BiomeBedrockEdition other, int newId) {
		super(other.getName(), newId);
		this.data = other.data;
		for(Entry<String, Color> entry : other.biomeColours) {
			biomeColours.put(entry.getKey(), new Color(entry.getValue()));
		}
	}
	
	@Override
	public void calculateTints() {
		if(data != null) {			
			float downfall = 0.5f;
			float temperature = 0.5f;
			
			if(data.has("minecraft:climate")) {
				JsonObject climateObj = data.get("minecraft:climate").getAsJsonObject();
				JsonElement downfallObj = climateObj.get("downfall");
				if(downfallObj != null)
					downfall = downfallObj.getAsFloat();
				
				JsonElement temperatureObj = climateObj.get("temperature");
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
							
							Color color = new Color(colorI);
							biomeColours.put(colorMap, color);
						}
					}
				}
			}
			if(data.has("minecraft:map_tints")) {
				JsonObject mapTints = data.getAsJsonObject("minecraft:map_tints");
				for(Entry<String, JsonElement> entry : mapTints.entrySet()) {
					String colormapName = entry.getKey();
					if(!colormapName.contains(":"))
						colormapName = "minecraft:" + colormapName;
					if(entry.getValue().isJsonObject()) {
						if(entry.getValue().getAsJsonObject().has("tint")) {
							JsonElement tint = entry.getValue().getAsJsonObject().get("tint");
							biomeColours.put(colormapName, parseTint(tint));
						}
					}else {
						biomeColours.put(colormapName, parseTint(entry.getValue()));
					}
				}
			}
		}
	}
	
	public static Color parseTint(JsonElement tint) {
		if(tint.isJsonArray()) {
			JsonArray tintArray = tint.getAsJsonArray();
			float r = 1f;
			float g = 1f;
			float b = 1f;
			if(tintArray.size() > 0) {
				if(tintArray.get(0).isJsonPrimitive() && tintArray.get(0).getAsJsonPrimitive().isNumber()) {
					r = g = b = tintArray.get(0).getAsFloat();
				}
			}
			if(tintArray.size() > 1) {
				if(tintArray.get(1).isJsonPrimitive() && tintArray.get(1).getAsJsonPrimitive().isNumber()) {
					g = b = tintArray.get(1).getAsFloat();
				}
			}
			if(tintArray.size() > 2) {
				if(tintArray.get(2).isJsonPrimitive() && tintArray.get(2).getAsJsonPrimitive().isNumber()) {
					g = b = tintArray.get(2).getAsFloat();
				}
			}
			return new Color(r, g, b);
		}else if(tint.isJsonPrimitive()) {
			JsonPrimitive tintPrim = tint.getAsJsonPrimitive();
			if(tintPrim.isNumber()) {
				return new Color(tintPrim.getAsInt());
			}else if(tintPrim.isString()) {
				String tintStr = tintPrim.getAsString();
				if(tintStr.startsWith("#"))
					tintStr = tintStr.substring(1);
				try {
					return new Color(Integer.parseUnsignedInt(tintStr, 16));
				}catch(Exception ex) {}
			}
		}
		return new Color(1f, 1f, 1f);
	}
	
}
