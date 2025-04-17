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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.molang.MolangValue;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangArray;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangObject;

public class RenderControllerBedrockEdition {
	
	private Map<String, List<MolangScript>> textureArrays;
	private Map<String, List<MolangScript>> geometryArrays;
	private Map<String, List<MolangScript>> materialArrays;
	
	private MolangScript geometry;
	private Map<String, MolangScript> materials;
	private List<MolangScript> textures;
	private List<List<MolangScript>> tints;
	
	public RenderControllerBedrockEdition(JsonObject data) {
		textureArrays = new HashMap<String, List<MolangScript>>();
		geometryArrays = new HashMap<String, List<MolangScript>>();
		materialArrays = new HashMap<String, List<MolangScript>>();
		
		geometry = null;
		materials = new HashMap<String, MolangScript>();
		textures = new ArrayList<MolangScript>();
		tints = new ArrayList<List<MolangScript>>();
		
		if(data.has("arrays")) {
			JsonObject arraysObj = data.get("arrays").getAsJsonObject();
			if(arraysObj.has("textures")) {
				parseArrays(arraysObj.getAsJsonObject("textures"), textureArrays);
			}
			if(arraysObj.has("geometries")) {
				parseArrays(arraysObj.getAsJsonObject("geometries"), geometryArrays);
			}
			if(arraysObj.has("materials")) {
				parseArrays(arraysObj.getAsJsonObject("materials"), materialArrays);
			}
		}
		
		if(data.has("geometry"))
			this.geometry = MolangParser.parse(data.get("geometry").getAsString());
		
		if(data.has("materials")) {
			JsonArray materialsArray = data.getAsJsonArray("materials");
			for(JsonElement el : materialsArray.asList()) {
				JsonObject materialsObj = el.getAsJsonObject();
				for(Entry<String, JsonElement> entry : materialsObj.entrySet()) {
					materials.put(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
				}
			}
		}
		if(data.has("textures")) {
			JsonArray texturesObj = data.getAsJsonArray("textures");
			for(JsonElement el : texturesObj.asList()) {
				textures.add(MolangParser.parse(el.getAsString()));
			}
		}
		if(data.has("color")) {
			JsonObject colorObj = data.getAsJsonObject("color");
			List<MolangScript> tint = new ArrayList<MolangScript>();
			if(colorObj.has("r"))
				tint.add(MolangParser.parse(colorObj.get("r").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("g"))
				tint.add(MolangParser.parse(colorObj.get("g").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("b"))
				tint.add(MolangParser.parse(colorObj.get("b").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("a"))
				tint.add(MolangParser.parse(colorObj.get("a").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			
			tints.add(tint);
		}else
			tints.add(null);
		if(data.has("overlay_color")) {
			JsonObject colorObj = data.getAsJsonObject("overlay_color");
			List<MolangScript> tint = new ArrayList<MolangScript>();
			if(colorObj.has("r"))
				tint.add(MolangParser.parse(colorObj.get("r").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("g"))
				tint.add(MolangParser.parse(colorObj.get("g").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("b"))
				tint.add(MolangParser.parse(colorObj.get("b").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			if(colorObj.has("a"))
				tint.add(MolangParser.parse(colorObj.get("a").getAsString()));
			else
				tint.add(MolangParser.parse("1.0"));
			
			tints.add(tint);
		}else
			tints.add(null);
	}
	
	private void parseArrays(JsonObject arraysObj, Map<String, List<MolangScript>> map) {
		for(Entry<String, JsonElement> entry : arraysObj.entrySet()) {
			List<MolangScript> array = new ArrayList<MolangScript>();
			JsonArray arrayObj = entry.getValue().getAsJsonArray();
			for(JsonElement el : arrayObj.asList()) {
				if(el.isJsonPrimitive())
					array.add(MolangParser.parse(el.getAsString()));
			}
			if(array.size() > 0)
				map.put(entry.getKey().toLowerCase(), array);
		}
	}
	
	public String getGeometry(MolangQuery query, MolangValue variablesDict, Map<String, MolangValue> globals, Random random) {
		if(this.geometry == null)
			return null;
		
		MolangContext context = new MolangContext(query, random);
		context.setVariableDict(variablesDict);
		for(Entry<String, MolangValue> global : globals.entrySet()) {
			context.setGlobal(global.getKey(), global.getValue());
		}
		
		// First evaluate the arrays
		for(Entry<String, List<MolangScript>> entry : geometryArrays.entrySet()) {
			String[] nameTokens = entry.getKey().toLowerCase().split("\\.");
			MolangValue arrayVal = null;
			for(int i = 0; i < nameTokens.length-1; ++i) {
				String token = nameTokens[i];
				if(arrayVal == null) {
					arrayVal = context.getGlobal(token);
					if(arrayVal == null) {
						arrayVal = new MolangValue(new MolangObject());
						context.setGlobal(token, arrayVal);
					}
				}else {
					if(arrayVal.getImpl() instanceof MolangObject) {
						MolangObject obj = (MolangObject) arrayVal.getImpl();
						arrayVal = obj.getFields().getOrDefault(token, null);
						if(arrayVal == null) {
							arrayVal = new MolangValue(new MolangObject());
							obj.getFields().put(token, arrayVal);
						}
					}else {
						// Not supported
						arrayVal = null;
						break;
					}
				}
			}
			if(arrayVal == null || !(arrayVal.getImpl() instanceof MolangObject))
				continue;
			
			MolangObject obj = (MolangObject) arrayVal.getImpl();
			
			List<MolangValue> array = new ArrayList<MolangValue>();
			for(MolangScript script : entry.getValue()) {
				array.add(script.eval(context.copy()));
			}
			
			obj.getFields().put(nameTokens[nameTokens.length-1], new MolangValue(new MolangArray(array)));
		}
		
		return geometry.eval(context).asString(context);
	}
	
	public List<String> getTextures(MolangQuery query, MolangValue variablesDict, Map<String, MolangValue> globals, Random random){
		if(this.textures.isEmpty())
			return null;
		
		MolangContext context = new MolangContext(query, random);
		context.setVariableDict(variablesDict);
		for(Entry<String, MolangValue> global : globals.entrySet()) {
			context.setGlobal(global.getKey(), global.getValue());
		}
		
		// First evaluate the arrays
		for(Entry<String, List<MolangScript>> entry : textureArrays.entrySet()) {
			String[] nameTokens = entry.getKey().toLowerCase().split("\\.");
			MolangValue arrayVal = null;
			for(int i = 0; i < nameTokens.length-1; ++i) {
				String token = nameTokens[i];
				if(arrayVal == null) {
					arrayVal = context.getGlobal(token);
					if(arrayVal == null) {
						arrayVal = new MolangValue(new MolangObject());
						context.setGlobal(token, arrayVal);
					}
				}else {
					if(arrayVal.getImpl() instanceof MolangObject) {
						MolangObject obj = (MolangObject) arrayVal.getImpl();
						arrayVal = obj.getFields().getOrDefault(token, null);
						if(arrayVal == null) {
							arrayVal = new MolangValue(new MolangObject());
							obj.getFields().put(token, arrayVal);
						}
					}else {
						// Not supported
						arrayVal = null;
						break;
					}
				}
			}
			if(arrayVal == null || !(arrayVal.getImpl() instanceof MolangObject))
				continue;
			
			MolangObject obj = (MolangObject) arrayVal.getImpl();
			
			List<MolangValue> array = new ArrayList<MolangValue>();
			for(MolangScript script : entry.getValue()) {
				array.add(script.eval(context.copy()));
			}
			
			obj.getFields().put(nameTokens[nameTokens.length-1], new MolangValue(new MolangArray(array)));
		}
		
		List<String> res = new ArrayList<String>();
		
		for(MolangScript script : textures) {
			res.add(script.eval(context).asString(context));
		}
		
		return res;
	}
	
	public Map<String, String> getMaterials(MolangQuery query, MolangValue variablesDict, Map<String, MolangValue> globals, Random random){
		if(this.materials.isEmpty()) {
			Map<String, String> res = new HashMap<String, String>();
			res.put("*", "Materials.default");
			return res;
		}
		
		MolangContext context = new MolangContext(query, random);
		context.setVariableDict(variablesDict);
		for(Entry<String, MolangValue> global : globals.entrySet()) {
			context.setGlobal(global.getKey(), global.getValue());
		}
		
		// First evaluate the arrays
		for(Entry<String, List<MolangScript>> entry : materialArrays.entrySet()) {
			String[] nameTokens = entry.getKey().toLowerCase().split("\\.");
			MolangValue arrayVal = null;
			for(int i = 0; i < nameTokens.length-1; ++i) {
				String token = nameTokens[i];
				if(arrayVal == null) {
					arrayVal = context.getGlobal(token);
					if(arrayVal == null) {
						arrayVal = new MolangValue(new MolangObject());
						context.setGlobal(token, arrayVal);
					}
				}else {
					if(arrayVal.getImpl() instanceof MolangObject) {
						MolangObject obj = (MolangObject) arrayVal.getImpl();
						arrayVal = obj.getFields().getOrDefault(token, null);
						if(arrayVal == null) {
							arrayVal = new MolangValue(new MolangObject());
							obj.getFields().put(token, arrayVal);
						}
					}else {
						// Not supported
						arrayVal = null;
						break;
					}
				}
			}
			if(arrayVal == null || !(arrayVal.getImpl() instanceof MolangObject))
				continue;
			
			MolangObject obj = (MolangObject) arrayVal.getImpl();
			
			List<MolangValue> array = new ArrayList<MolangValue>();
			for(MolangScript script : entry.getValue()) {
				array.add(script.eval(context.copy()));
			}
			
			obj.getFields().put(nameTokens[nameTokens.length-1], new MolangValue(new MolangArray(array)));
		}
		
		Map<String, String> res = new HashMap<String, String>();
		
		for(Entry<String, MolangScript> script : materials.entrySet()) {
			res.put(script.getKey(), script.getValue().eval(context).asString(context));
		}
		
		return res;
	}
	
	public List<Color> getTints(MolangQuery query, MolangValue variablesDict, Map<String, MolangValue> globals, Random random){
		if(this.tints.isEmpty())
			return null;
		
		MolangContext context = new MolangContext(query, random);
		context.setVariableDict(variablesDict);
		for(Entry<String, MolangValue> global : globals.entrySet()) {
			context.setGlobal(global.getKey(), global.getValue());
		}
		context.setGlobal("this", new MolangValue(1f));
		
		
		
		List<Color> res = new ArrayList<Color>();
		
		for(List<MolangScript> tint : tints) {
			if(tint == null) {
				res.add(new Color(1f, 1f, 1f, 1f));
				continue;
			}
			Color color = new Color();
			color.set(tint.get(0).eval(context).asNumber(context), 
					tint.get(1).eval(context).asNumber(context), 
					tint.get(2).eval(context).asNumber(context), 
					tint.get(3).eval(context).asNumber(context));
			res.add(color);
		}
		
		return res;
	}
	
}
