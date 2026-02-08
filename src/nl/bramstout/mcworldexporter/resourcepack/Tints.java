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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.export.BlendedBiome;
import nl.bramstout.mcworldexporter.export.BlendedBiome.WeightedColor;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class Tints {
	
	public static class TintValue{
		public WeightedColor color;
		public String biomeColor;
		
		public TintValue(Color color, String biomeColor) {
			this.color = color == null ? null : new WeightedColor(color);
			this.biomeColor = biomeColor;
		}
		
		public TintValue(TintValue other) {
			this.color = other.color;
			this.biomeColor = other.biomeColor;
		}
		
		public WeightedColor getColor(BlendedBiome biome) {
			if(color != null)
				return color;
			if(biomeColor != null)
				return biome.getColor(biomeColor);
			return null;
		}
	}
	
	public static class TintLayers{
		/**
		 * Tint index -1 means no tint, but in some cases we might
		 * want to force a tint anyways, in which case we want to specify
		 * the -1 index.
		 * Tint index 0+ means a specific tint layer.
		 * 
		 * Because -1 is a valid index, we offset all of the indices by +1.
		 */
		private TintValue[] layers;
		/**
		 * Used when there is no non-null TintValue for the given tint index.
		 * However, it's never used for -1 tint index. That requires setting
		 * specifically.
		 */
		private TintValue defaultValue;
		
		public TintLayers() {
			layers = null;
			defaultValue = null;
		}
		
		public TintLayers(TintLayers other) {
			layers = null;
			defaultValue = null;
			if(other.layers != null) {
				layers = new TintValue[other.layers.length];
				for(int i = 0; i < layers.length; ++i) {
					if(other.layers[i] != null)
						layers[i] = new TintValue(other.layers[i]);
				}
			}
			if(other.defaultValue != null)
				defaultValue = new TintValue(other.defaultValue);
		}
		
		public TintValue getLayer(int layerId) {
			layerId += 1; // Offset ids by +1
			if(layers == null || layers.length == 0) {
				if(layerId == 0)
					return null;
				return defaultValue;
			}
			if(layerId < 0 || layerId >= layers.length)
				return defaultValue;
			TintValue layer = layers[layerId];
			if(layer == null && layerId > 0)
				return defaultValue;
			return layer;
		}
		
		public void setLayer(int layerId, TintValue value) {
			layerId += 1;
			if(layerId < 0)
				return;
			if(layers == null)
				layers = new TintValue[layerId + 1];
			if(layerId >= layers.length)
				layers = Arrays.copyOf(layers, layerId+1);
			layers[layerId] = value;
			
			// Make sure that we always have a default value.
			if(layerId == 1 || defaultValue == null)
				defaultValue = value;
		}
		
		public void setDefaultValue(TintValue value) {
			this.defaultValue = value;
		}
		
		public TintValue getGenericTint() {
			if(layers != null) {
				for(int i = 0; i < layers.length; ++i)
					if(layers[i] != null)
						return layers[i];
			}
			return defaultValue;
		}
		
		public boolean isEmpty() {
			return defaultValue == null;
		}
	}
	
	public static class Tint{
		
		private TintLayers baseTint;
		private Map<List<Map<String, String>>, TintLayers> stateTints;
		
		public Tint(JsonObject data) {
			baseTint = new TintLayers();
			stateTints = null;
			if(data == null)
				return;
			if(data.has("tint")) {
				JsonElement tintData = data.get("tint");
				if(tintData.isJsonObject()) {
					JsonObject tints = tintData.getAsJsonObject();
					for(Entry<String, JsonElement> entry : tints.entrySet()) {
						if(!entry.getKey().equals("*")) {
							try {
								int layerId = Integer.parseInt(entry.getKey());
								baseTint.setLayer(layerId, getColorFromElement(entry.getValue()));
							}catch(Exception ex) {}
						}
					}
					if(tints.has("*")) {
						baseTint.setDefaultValue(getColorFromElement(tints.get("*")));
					}
				}else {
					baseTint.setDefaultValue(getColorFromElement(tintData));
				}
			}
			if(data.has("states")) {
				stateTints = new HashMap<List<Map<String, String>>, TintLayers>();
				JsonObject statesData = data.get("states").getAsJsonObject();
				for(Entry<String, JsonElement> entry : statesData.asMap().entrySet()) {
					List<Map<String, String>> checks = new ArrayList<Map<String, String>>();
					for(String checkToken : entry.getKey().split("\\|\\|")) {
						Map<String, String> check = new HashMap<String, String>();
						for(String checkToken2 : checkToken.split(",")) {
							if(checkToken2.contains("=")) {
								String[] tokens = checkToken2.split("=");
								check.put(tokens[0], tokens[1]);
							}
						}
						checks.add(check);
					}
					TintLayers layers = new TintLayers();
					if(entry.getValue().isJsonObject()) {
						JsonObject tints = entry.getValue().getAsJsonObject();
						for(Entry<String, JsonElement> entry2 : tints.entrySet()) {
							if(!entry2.getKey().equals("*")) {
								try {
									int layerId = Integer.parseInt(entry2.getKey());
									layers.setLayer(layerId, getColorFromElement(entry2.getValue()));
								}catch(Exception ex) {}
							}
						}
						if(tints.has("*")) {
							layers.setDefaultValue(getColorFromElement(tints.get("*")));
						}
					}else {
						layers.setDefaultValue(getColorFromElement(entry.getValue()));
					}
					stateTints.put(checks, layers);
				}
			}
		}
		
		public Tint(Tint other) {
			baseTint = new TintLayers(other.baseTint);
			stateTints = null;
			
			if(other.stateTints != null) {
				stateTints = new HashMap<List<Map<String, String>>, TintLayers>();
				for(Entry<List<Map<String, String>>, TintLayers> state : stateTints.entrySet()) {
					if(state.getValue() == null)
						stateTints.put(state.getKey(), null);
					else
						stateTints.put(state.getKey(), new TintLayers(state.getValue()));
				}
			}
		}
		
		private TintValue getColorFromElement(JsonElement tintData) {
			if(tintData.isJsonPrimitive()) {
				JsonPrimitive tintPrim = tintData.getAsJsonPrimitive();
				if(tintPrim.isString()) {
					String tintString = tintPrim.getAsString();
					if(tintString.contains(":")) {
						// Biome colour being referenced.
						return new TintValue(null, tintString);
					}else {
						// Hardcoded tint
						if(tintString.startsWith("#"))
							tintString = tintString.substring(1);
						try {
							int rgb = Integer.parseUnsignedInt(tintString, 16);
							return new TintValue(new Color(rgb), null);
						}catch(Exception ex) {}
					}
				}else if(tintPrim.isNumber()) {
					// Hardcoded tint
					return new TintValue(new Color(tintPrim.getAsInt()), null);
				}
			}
			return null;
		}
		
		public TintLayers getTint(NbtTagCompound properties) {
			if(stateTints == null || properties == null)
				return baseTint;
			
			for(Entry<List<Map<String, String>>, TintLayers> state : stateTints.entrySet()) {
				if(useTint(properties, state.getKey())) {
					return state.getValue();
				}
			}
			return baseTint;
		}
		
		private boolean useTint(NbtTagCompound properties, List<Map<String, String>> checks) {
			if(checks.isEmpty())
				return true;
			
			for(Map<String, String> check : checks) {
				boolean res = doCheck(properties, check);
				if(res)
					return true;
			}
			return false;
		}
		
		private boolean doCheck(NbtTagCompound properties, Map<String, String> check) {
			int numItems = properties.getSize();
			for(int i = 0; i < numItems; ++i) {
				NbtTag tag = properties.get(i);
				String value = check.get(tag.getName());
				if(value != null) {
					String propValue = tag.asString();
					if(propValue != null) {
						if(!value.equals(propValue)) {
							if(!((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))))
								return false;
						}
					}
				}
			}
			return true;
		}
		
		public void setBaseTint(TintLayers tint) {
			this.baseTint = tint;
		}
		
		public void setStateTint(List<Map<String, String>> key, TintLayers tint) {
			if(this.stateTints == null)
				this.stateTints = new HashMap<List<Map<String, String>>, TintLayers>();
			this.stateTints.put(key, tint);
		}
		
	}
	
	private static Map<String, Tint> tintRegistry = new HashMap<String, Tint>();
	private static Map<String, BufferedImage> colorMaps = new HashMap<String, BufferedImage>();
	private static Object mutex = new Object();
	
	public static void load() {
		tintRegistry.clear();
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File tintsFile = new File(resourcePacks.get(i).getFolder(), "miex_block_tints.json");
			if(!tintsFile.exists())
				continue;
			try {
				JsonObject data = Json.read(tintsFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String blockName = entry.getKey();
					if(blockName.startsWith("#")) {
						List<String> blockNames = Tags.getNamesInTag(blockName);
						for(String blockName2 : blockNames) {
							tintRegistry.put(blockName2, new Tint(entry.getValue().getAsJsonObject()));
						}
					}else {
						if(!blockName.contains(":"))
							blockName = "minecraft:" + blockName;
						tintRegistry.put(blockName, new Tint(entry.getValue().getAsJsonObject()));
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		synchronized(mutex) {
			colorMaps.clear();
		}
	}
	
	public static Tint getTint(String name) {
		return tintRegistry.getOrDefault(name, null);
	}
	
	public static void setTint(String name, Tint tint, boolean mergeBehindIfExists) {
		if(tintRegistry.containsKey(name) && mergeBehindIfExists) {
			// Tint already exists, so let's merge it.
			Tint tint2 = tintRegistry.get(name);
			
			if(tint2.baseTint.isEmpty() && tint.baseTint != null)
				tint2.baseTint = tint.baseTint;
			
			if(tint.stateTints != null) {
				for(Entry<List<Map<String, String>>, TintLayers> state : tint.stateTints.entrySet()) {
					if(tint2.stateTints == null) {
						tint2.stateTints = new HashMap<List<Map<String, String>>, TintLayers>();
					}
					if(!tint2.stateTints.containsKey(state.getKey())) {
						tint2.stateTints.put(state.getKey(), state.getValue());
					}
				}
			}
		}else {
			tintRegistry.put(name, tint);
		}
	}
	
	public static BufferedImage getColorMap(String name) {
		BufferedImage img = colorMaps.getOrDefault(name, null);
		if(img != null)
			return img;
		synchronized(mutex) {
			img = colorMaps.getOrDefault(name, null);
			if(img != null)
				return img;
			
			int sep = name.indexOf(":");
			String namespace = sep < 0 ? "minecraft:" : name.substring(0, sep+1);
			String texName = "colormap/" + (sep < 0 ? name : name.substring(sep+1));
			
			try {
				File mapFile = ResourcePacks.getTexture(namespace + texName);
				if(mapFile != null && mapFile.exists()) {
					img = ImageReader.readImage(mapFile);
					colorMaps.put(name, img);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return img;
	}
	
}
