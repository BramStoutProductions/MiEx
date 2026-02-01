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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class BiomeHytale extends Biome{
	
	private JsonObject data;

	public BiomeHytale(String name, int id, JsonObject data) {
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
			if(data.has("Parent")) {
				String parentId = data.get("Parent").getAsString();
				if(parentId.indexOf(':') == -1)
					parentId = "hytale:" + parentId;
				int parentBiomeId = BiomeRegistry.getIdForName(parentId);
				Biome parentBiome = BiomeRegistry.getBiome(parentBiomeId);
				if(parentBiome != null) {
					for(Entry<String, Color> entry : parentBiome.getColors()) {
						biomeColours.put(entry.getKey(), entry.getValue());
					}
				}
			}
			
			for(Entry<String, JsonElement> entry : data.entrySet()) {
				if(entry.getKey().endsWith("Tint")) {
					String colorMap = entry.getKey().substring(0, entry.getKey().length() - 4).toLowerCase();
					if(!colorMap.contains(":"))
						colorMap = "minecraft:" + colorMap;
					int color = parseColor(entry.getValue());
					if(color != -1)
						biomeColours.put(colorMap, new Color(color));
				}
			}
		}
	}

}
