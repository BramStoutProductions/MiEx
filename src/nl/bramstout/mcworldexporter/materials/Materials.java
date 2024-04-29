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

package nl.bramstout.mcworldexporter.materials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.Atlas;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class Materials {
	
	public static class ShadingAttribute{
		
		public String name;
		public String type;
		public Object value;
		public String connection;
		public String expression;
		
		public ShadingAttribute() {
			this("", null, null, null, null);
		}
		
		public ShadingAttribute(ShadingAttribute other) {
			this(other.name, other.type, other.value, other.connection, other.expression);
		}
		
		public ShadingAttribute(String name, String type, Object value, String connection, String expression) {
			this.name = name;
			this.type = type;
			this.value = value;
			this.connection = connection;
			this.expression = expression;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof ShadingAttribute))
				return false;
			ShadingAttribute other = (ShadingAttribute) obj;
			if(!name.equals(other.name))
				return false;
			if(type == null) {
				if(other.type != null)
					return false;
			}else {
				if(!type.equals(other.type))
					return false;
			}
			if(value == null) {
				if(other.value != null)
					return false;
			}else {
				if(!value.equals(other.value))
					return false;
			}
			if(connection == null) {
				if(other.connection != null)
					return false;
			}else {
				if(!connection.equals(other.connection))
					return false;
			}
			if(expression == null) {
				if(other.expression != null)
					return false;
			}else {
				if(!expression.equals(other.expression))
					return false;
			}
			return true;
		}
		
		public void override(ShadingAttribute other, MaterialNetwork context) {
			if(other.type != null)
				type = other.type;
			if(other.value != null) {
				this.value = other.value;
				this.connection = null;
				this.expression = null;
			}
			if(other.connection != null) {
				this.connection = other.connection;
				this.value = null;
				this.expression = null;
				
				// To make it easier to compose templates, we have an operator
				// to specify the value/connection/expression of some other attribute.
				// This makes it easy to insert a node somewhere in a network.
				// For that you use the pattern ${nodeName.attrName}
				if(other.connection.startsWith("${")) {
					String tokens[] = other.connection.substring(2, other.connection.length()-1).split("\\.");
					if(tokens.length == 2) {
						ShadingNode node = context.getNode(tokens[0]);
						if(node != null) {
							ShadingAttribute attr = node.getAttribute(tokens[1]);
							if(attr != null) {
								this.connection = null;
								if(attr.value != null)
									this.value = attr.value;
								else if(attr.connection != null)
									this.connection = attr.connection;
								else if(attr.expression != null)
									this.expression = attr.expression;
							}
						}
					}
				}
			}
			if(other.expression != null) {
				this.expression = other.expression;
				this.value = null;
				this.connection = null;
			}
		}
		
	}
	
	public static class ShadingNode{
		
		public String name;
		public String type;
		public List<ShadingAttribute> attributes;
		
		public ShadingNode() {
			this("", null);
		}
		
		public ShadingNode(String name, String type) {
			this.name = name;
			this.type = type;
			attributes = new ArrayList<ShadingAttribute>();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof ShadingNode))
				return false;
			ShadingNode other = (ShadingNode) obj;
			if(!name.equals(other.name))
				return false;
			if(type == null) {
				if(other.type != null)
					return false;
			}else {
				if(!type.equals(other.type))
					return false;
			}
			if(!attributes.equals(other.attributes))
				return false;
			return true;
		}
		
		public void override(ShadingNode other, MaterialNetwork context) {
			if(other.type != null)
				this.type = other.type;
			for(ShadingAttribute otherAttr : other.attributes) {
				boolean found = false;
				for(ShadingAttribute attr : attributes) {
					if(otherAttr.name.equals(attr.name)) {
						attr.override(otherAttr, context);
						found = true;
						break;
					}
				}
				if(!found) {
					ShadingAttribute attr = new ShadingAttribute(otherAttr.name, otherAttr.type, null, null, null);
					attr.override(otherAttr, context);
					attributes.add(attr);
				}
			}
		}
		
		public ShadingAttribute getAttribute(String name) {
			for(ShadingAttribute attr : attributes)
				if(attr.name.equals(name))
					return attr;
			return null;
		}
		
		public void getReferencedNodes(List<String> out) {
			for(ShadingAttribute attr : attributes) {
				if(attr.connection != null) {
					out.add(attr.connection.split("\\.")[0].split("\\[")[0]);
				}
			}
		}
		
	}
	
	public static class MaterialNetwork{
		
		public String condition;
		public List<ShadingNode> nodes;
		
		public MaterialNetwork() {
			this("");
		}
		
		public MaterialNetwork(String condition) {
			this.condition = condition;
			this.nodes = new ArrayList<ShadingNode>();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof MaterialNetwork))
				return false;
			MaterialNetwork other = (MaterialNetwork) obj;
			if(!condition.equals(other.condition))
				return false;
			if(!nodes.equals(other.nodes))
				return false;
			return true;
		}
		
		public void override(MaterialNetwork other) {
			for(ShadingNode otherNode : other.nodes) {
				boolean found = false;
				for(ShadingNode node : nodes) {
					if(otherNode.name.equals(node.name)) {
						node.override(otherNode, this);
						found = true;
						break;
					}
				}
				if(!found) {
					ShadingNode node = new ShadingNode(otherNode.name, otherNode.type);
					node.override(otherNode, this);
					nodes.add(node);
				}
			}
		}
		
		public ShadingNode getNode(String name) {
			for(ShadingNode node : nodes)
				if(node.name.equals(name))
					return node;
			return null;
		}
		
		public boolean evaluateCondition(String texture, boolean hasBiomeColor, String currentWorkingDirectory) {
			for(String condition : this.condition.split("&&")) {
				boolean invert = false;
				if(condition.startsWith("!")) {
					invert = true;
					condition = condition.substring(1);
				}
				if(condition.equals("@biomeColor@")) {
					if(invert) {
						if(hasBiomeColor)
							return false;
						continue;
					}else {
						if(!hasBiomeColor)
							return false;
						continue;
					}
				}
				String fullPath = condition.replace("@texture@", texture);
				boolean checkAlpha = false;
				boolean checkAnimated = false;
				boolean checkInterpolated = false;
				if(fullPath.endsWith(".a")) {
					checkAlpha = true;
					fullPath = fullPath.substring(0, fullPath.length() - 2);
				}
				if(fullPath.endsWith(".animated")) {
					checkAnimated = true;
					fullPath = fullPath.substring(0, fullPath.length() - 9);
				}
				if(fullPath.endsWith(".interpolated")) {
					checkInterpolated = true;
					fullPath = fullPath.substring(0, fullPath.length() - 13);
				}
				File file = getTextureFile(fullPath, currentWorkingDirectory);
				if(invert && (!checkAlpha && !checkAnimated && !checkInterpolated)) {
					if(file.exists())
						return false;
				}else {
					if(!file.exists())
						return false;
				}
				
				if(checkAlpha) {
					if(invert) {
						if(FileUtil.hasAlpha(file))
							return false;
					}else {
						if(!FileUtil.hasAlpha(file))
							return false;
					}
				}
				if(checkAnimated || checkInterpolated) {
					MCMeta mcmeta = new MCMeta(texture);
					if(checkAnimated) {
						if(invert) {
							if(!(!mcmeta.isAnimate() || mcmeta.isInterpolate()))
								return false;
						}else {
							if(!mcmeta.isAnimate() || mcmeta.isInterpolate())
								return false;
						}
					}
					if(checkInterpolated) {
						if(invert) {
							if(mcmeta.isInterpolate())
								return false;
						}else {
							if(!mcmeta.isInterpolate())
								return false;
						}
					}
				}
			}
			return true;
		}
		
	}
	
	public static class MaterialTemplate{
		
		public int priority;
		public List<String> selection;
		public Map<String, String> shadingGroup;
		public List<MaterialNetwork> networks;
		public String name;
		
		public MaterialTemplate(String name) {
			this(name, 0);
		}
		
		public MaterialTemplate(String name, int priority) {
			this.name = name;
			this.priority = priority;
			this.selection = new ArrayList<String>();
			this.shadingGroup = new HashMap<String, String>();
			this.networks = new ArrayList<MaterialNetwork>();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof MaterialTemplate))
				return false;
			MaterialTemplate other = (MaterialTemplate) obj;
			if(priority != other.priority)
				return false;
			if(!shadingGroup.equals(other.shadingGroup))
				return false;
			if(!networks.equals(other.networks))
				return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		
		public boolean isInSelection(String texture) {
			if(texture.isEmpty())
				return false;
			for(String selStr : selection) {
				if(selStr.isEmpty())
					continue;
				int texIndex = 0;
				int selIndex = 0;
				int lastWildcard = -1;
				boolean rechecked = false;
				while(true) {
					if(texIndex >= texture.length())
						break;
					
					if(texture.codePointAt(texIndex) == selStr.codePointAt(Math.min(selIndex, selStr.length()-1))) {
						texIndex++;
						selIndex++;
						rechecked = false;
					}else {
						texIndex++;
						if(rechecked)
							rechecked = false;
						else {
							texIndex--;
							rechecked = true;
						}
						if(selStr.codePointAt(Math.min(selIndex, selStr.length()-1)) == '*')
							lastWildcard = Math.min(selIndex, selStr.length()-1);
						if(lastWildcard >= 0) {
							selIndex = lastWildcard + 1;
						}else {
							break;
						}
					}
				}
				if(texIndex == texture.length() && selIndex == selStr.length())
					return true;
			}
			return false;
		}
		
		public MaterialTemplate flatten(String texture, boolean hasBiomeColor, String currentWorkingDirectory) {
			MaterialTemplate material = new MaterialTemplate(name, 0);
			material.shadingGroup = shadingGroup;
			material.networks.add(new MaterialNetwork());
			for(MaterialNetwork network : networks) {
				if(!network.evaluateCondition(texture, hasBiomeColor, currentWorkingDirectory))
					continue;
				material.networks.get(0).override(network);
			}
			return material;
		}
		
		public void combine(MaterialTemplate other) {
			for(Entry<String, String> shadingGroupConn : other.shadingGroup.entrySet()) {
				shadingGroup.put(shadingGroupConn.getKey(), shadingGroupConn.getValue());
			}
			networks.addAll(other.networks);
		}
		
	}
	
	private static List<List<MaterialTemplate>> templates = null;
	public static MaterialNetwork sharedNodes = new MaterialNetwork();
	
	public static MaterialTemplate getMaterial(String texture, boolean hasBiomeColor, String currentWorkingDirectory) {
		if(templates == null)
			reload();
		
		// If we are using templates, then they are created based on which template
		// the textures are in, but those templates don't have a search string
		// for the right atlasses (they can't know what name they would be anyways).
		// So, if this texture is an atlas, then get one of the original textures
		// that was put into this atlas. Then we use that texture to find the right
		// template to use.
		String templateTexture = Atlas.getTemplateTextureForAtlas(texture, texture);
		
		for(List<MaterialTemplate> templateList : templates) {
			MaterialTemplate currentTemplate = null;
			for(MaterialTemplate template : templateList) {
				if(currentTemplate == null || template.priority > currentTemplate.priority)
					if(template.isInSelection(templateTexture))
						currentTemplate = template;
			}
			if(currentTemplate != null)
				return currentTemplate.flatten(texture, hasBiomeColor, currentWorkingDirectory);
		}
		return null;
	}
	
	public static void reload() {
		templates = new ArrayList<List<MaterialTemplate>>();
		sharedNodes.nodes.clear();
		
		List<MaterialNetwork> sharedNetworks = new ArrayList<MaterialNetwork>();
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int resourcePackIndex = 0; resourcePackIndex < resourcePacks.size(); resourcePackIndex++) {
			String resourcePack = resourcePacks.get(resourcePackIndex);
			List<MaterialTemplate> templateList = new ArrayList<MaterialTemplate>();
			
			File materialsDir = new File(FileUtil.getResourcePackDir() + resourcePack + "/materials/minecraft/templates");
			loadFromDir(materialsDir, sharedNetworks, resourcePacks, resourcePackIndex, templateList);
			templates.add(templateList);
		}
		for(int i = sharedNetworks.size()-1; i >= 0; --i) {
			sharedNodes.override(sharedNetworks.get(i));
		}
	}
	
	private static void loadFromDir(File dir, List<MaterialNetwork> sharedNetworks, List<String> resourcePacks, 
									int resourcePackIndex, List<MaterialTemplate> templateList) {
		File[] files = dir.listFiles();
		if(files == null)
			return;
		for(File f : files) {
			if(f.isDirectory()) {
				loadFromDir(f, sharedNetworks, resourcePacks, resourcePackIndex, templateList);
				continue;
			}
			if(!f.isFile() || !f.getName().endsWith(".json"))
				continue;
			if(f.getName().equals("shared.json")) {
				try {
					JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(f)))).getAsJsonObject();
					MaterialNetwork network = parseNetwork(data);
					if(network != null)
						sharedNetworks.add(network);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}else {
				try {
					MaterialTemplate template = parseTemplateFile(f, resourcePacks, resourcePackIndex);
					if(template != null)
						templateList.add(template);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	private static MaterialTemplate parseTemplateFile(File file, List<String> resourcePacks, int resourcePackIndex) throws Exception {
		JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(file)))).getAsJsonObject();
		
		MaterialTemplate template = null;
		if(data.has("include")) {
			for(JsonElement el : data.get("include").getAsJsonArray().asList()) {
				File includeFile = null;
				
				// If we are including some file, start the check at the start of the
				// resource pack list. However, if we include a file with the same
				// name, then we are overriding that file, so we want to start at
				// the next resource pack from this one, otherwise we might end up
				// in a loop.
				int startIndex = 0;
				if(el.getAsString().equalsIgnoreCase(file.getPath().replace('\\', '/').split("/materials/minecraft/templates/")[1].split("\\.")[0]))
					startIndex = resourcePackIndex + 1;
				
				int includeIndex;
				for(includeIndex = startIndex; includeIndex < resourcePacks.size(); ++includeIndex) {
					File testFile = new File(FileUtil.getResourcePackDir() + resourcePacks.get(includeIndex) + 
							"/materials/minecraft/templates/" + el.getAsString() + ".json");
					if(testFile.exists()) {
						includeFile = testFile;
						break;
					}
				}
				if(includeFile != null) {
					MaterialTemplate includeTemplate = parseTemplateFile(includeFile, resourcePacks, includeIndex);
					if(template == null)
						template = includeTemplate;
					else
						template.combine(includeTemplate);
				}
			}
		}
		if(template == null)
			template = new MaterialTemplate(file.getCanonicalPath());
		else
			template.name = file.getCanonicalPath();
		
		if(data.has("priority")) {
			template.priority = data.get("priority").getAsInt();
		}
		
		if(data.has("selection")) {
			template.selection.clear();
			for(JsonElement el : data.get("selection").getAsJsonArray().asList()) {
				template.selection.add(el.getAsString());
			}
		}
		
		if(data.has("shadingGroup")) {
			for(Entry<String, JsonElement> el : data.get("shadingGroup").getAsJsonObject().entrySet()) {
				template.shadingGroup.put(el.getKey(), el.getValue().getAsString());
			}
		}
		
		if(data.has("network")) {
			for(Entry<String, JsonElement> el : data.get("network").getAsJsonObject().entrySet()) {
				MaterialNetwork network = parseNetwork(el.getValue().getAsJsonObject());
				if(network != null) {
					network.condition = el.getKey();
					template.networks.add(network);
				}
			}
		}
		
		return template;
	}
	
	private static MaterialNetwork parseNetwork(JsonObject obj) {
		MaterialNetwork network = new MaterialNetwork();
		for(Entry<String, JsonElement> el : obj.entrySet()) {
			ShadingNode node = parseShadingNode(el.getValue().getAsJsonObject());
			if(node != null) {
				node.name = el.getKey();
				boolean found = false;
				for(ShadingNode existingNode : network.nodes) {
					if(existingNode.name.equals(node.name)) {
						existingNode.override(node, network);
						found = true;
						break;
					}
				}
				if(!found)
					network.nodes.add(node);
			}
		}
		
		return network;
	}
	
	private static ShadingNode parseShadingNode(JsonObject obj) {
		ShadingNode node = new ShadingNode();
		
		if(obj.has("type"))
			node.type = obj.get("type").getAsString();
		
		if(obj.has("attributes")) {
			for(Entry<String, JsonElement> el : obj.get("attributes").getAsJsonObject().entrySet()) {
				ShadingAttribute attr = parseShadingAttribute(el.getValue().getAsJsonObject());
				if(attr != null) {
					attr.name = el.getKey();
					node.attributes.add(attr);
				}
			}
		}
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	private static ShadingAttribute parseShadingAttribute(JsonObject obj) {
		ShadingAttribute attr = new ShadingAttribute();
		
		if(obj.has("type"))
			attr.type = obj.get("type").getAsString();
		
		if(obj.has("value")) {
			attr.value = null;
			attr.connection = null;
			attr.expression = null;
			
			JsonElement value = obj.get("value");
			if(value.isJsonArray()) {
				JsonArray array = value.getAsJsonArray();
				JsonPrimitive firstValue = array.get(0).getAsJsonPrimitive();
				if(firstValue.isString()) {
					attr.value = new ArrayList<String>();
					for(int i = 0; i < array.size(); ++i)
						((ArrayList<String>)attr.value).add(array.get(i).getAsString());
				} else if(firstValue.isNumber()) {
					attr.value = new ArrayList<Float>();
					for(int i = 0; i < array.size(); ++i)
						((ArrayList<Float>)attr.value).add(array.get(i).getAsFloat());
				}
			}else if(value.isJsonPrimitive()){
				JsonPrimitive primitive = value.getAsJsonPrimitive();
				if(primitive.isBoolean())
					attr.value = Boolean.valueOf(primitive.getAsBoolean());
				else if(primitive.isString())
					attr.value = primitive.getAsString();
				else if(primitive.isNumber())
					attr.value = Float.valueOf(primitive.getAsFloat());
			}
		}
		
		if(obj.has("connection")) {
			attr.value = null;
			attr.expression = null;
			attr.connection = obj.get("connection").getAsString();
		}
		
		if(obj.has("expression")) {
			attr.value = null;
			attr.connection = null;
			attr.expression = obj.get("expression").getAsString();
		}
		
		return attr;
	}
	
	public static File getTextureFile(String texture, String currentWorkingDirectory) {
		if(texture.startsWith(".")) {
			File file = new File(currentWorkingDirectory, texture + ".exr");
			if(file.exists())
				return file;
			return new File(currentWorkingDirectory, texture + ".png");
		}
		if(texture.contains(".")) {
			String[] tokens = texture.split("\\.");
			String extension = tokens[tokens.length-1];
			String filename = tokens[0];
			for(int i = 1; i < tokens.length-1; ++i)
				filename = filename + "." + tokens[i];
			return ResourcePack.getFile(filename, "textures", "." + extension, "assets");
		}
		// First check if there is an exr, if so use it.
		File file = ResourcePack.getFile(texture, "textures", ".exr", "assets");
		if(file.exists())
			return file;
		
		return ResourcePack.getFile(texture, "textures", ".png", "assets");
	}
	
	public static void getFrameIdSamples(List<Float> timeCodes, List<Float> values, float startFrame, float endFrame, 
			MCMeta animData, float frameTimeMultiplier, boolean isFloat2,
			Map<String, String> args) {
		int i = 0;
		try {
			i = Integer.valueOf(args.getOrDefault("offset", "0")).intValue();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		float prevValue = 0.0f;
		for(float timeCode = startFrame; timeCode <= endFrame;) {
			int frameIndex = i % (animData.getFrames().length/2);
			int frameId = animData.getFrames()[frameIndex*2];
			int frameTime = animData.getFrames()[frameIndex*2 + 1];
			float frameTimeF = ((float) frameTime) * frameTimeMultiplier;
			
			if(args.getOrDefault("reverse", "false").equals("true"))
				frameId = animData.getFrameCount() - frameId - 1;
			
			float value = ((float) frameId) / ((float) animData.getFrameCount());
			if(args.getOrDefault("powerof2", "false").equals("true"))
				value *= animData.getPowerOfTwoScaleCompensation();
				
			// Because USD will always linearly interpolate,
			// but we don't want any motion blur on the textures
			// we need to put in two time samples really close
			// to each other. We also need to offset it
			// since the shutter start could be exactly on the
			// frame, but the shutter could also be centered
			// on the frame and so it start just before the frame.
			// We assume that the shutter angle won't go past 180
			// degrees, so we only need to have it be
			// 0.25 of a frame before it. We do 0.3 just to be save
			timeCodes.add(timeCode - 0.301f);
			if(isFloat2) {
				values.add(0.0f);
				values.add(prevValue);
			}else {
				values.add(prevValue);
			}
			
			timeCodes.add(timeCode - 0.3f);
			if(isFloat2) {
				values.add(0.0f);
				values.add(value);
			}else {
				values.add(value);
			}
			
			timeCode += frameTimeF;
			prevValue = value;
			i++;
		}
	}
	
	public static void getFrameIdSamplesPerFrame(List<Float> values, float startFrame, float endFrame, 
			MCMeta animData, float frameTimeMultiplier, boolean isFloat2,
			Map<String, String> args) {
		int i = 0;
		try {
			i = Integer.valueOf(args.getOrDefault("offset", "0")).intValue();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		for(float timeCode = startFrame; timeCode <= endFrame;) {
			int frameIndex = i % (animData.getFrames().length/2);
			int frameId = animData.getFrames()[frameIndex*2];
			int frameTime = animData.getFrames()[frameIndex*2 + 1];
			float frameTimeF = ((float) frameTime) * frameTimeMultiplier;
			
			if(args.getOrDefault("reverse", "false").equals("true"))
				frameId = animData.getFrameCount() - frameId - 1;
			
			float value = ((float) frameId) / ((float) animData.getFrameCount());
			if(args.getOrDefault("powerof2", "false").equals("true"))
				value *= animData.getPowerOfTwoScaleCompensation();
				
			// Repeat the value for each actual frame that this animation frame
			// should be displayed.
			for(int j = 0; j < frameTimeF; ++j) {
				if(isFloat2) {
					values.add(0.0f);
					values.add(value);
				}else {
					values.add(value);
				}
			}
			
			timeCode += frameTimeF;
			i++;
		}
	}
		
}
