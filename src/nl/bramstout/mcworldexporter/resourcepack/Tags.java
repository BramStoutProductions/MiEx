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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.FileUtil;

public class Tags {
	
	private static Map<String, List<String>> tagToResourceIdentifiers = new HashMap<String, List<String>>();
	
	public static void load() {
		tagToResourceIdentifiers.clear();
		
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int i = resourcePacks.size()-1; i > 0; i--) {
			String resourcePack = resourcePacks.get(i);
			File dataFolder = new File(FileUtil.getResourcePackDir(), resourcePack + "/data");
			if(!dataFolder.exists())
				continue;
			for(File namespace : dataFolder.listFiles()) {
				if(!namespace.isDirectory())
					continue;
				processNamespace(namespace.getName(), namespace);
			}
		}
		
		// The tagToResourceIdentifiers is now filled with the tags, but
		// tags may reference other tags. Those references are in there
		// as well. We could resolve it when we query the resource identifiers
		// that are in a tag, but that would be a waste of time.
		// So, we pre-resolve all of it here.
		boolean needAnotherIteration = true;
		while(needAnotherIteration) {
			needAnotherIteration = false;
			
			for(String key : tagToResourceIdentifiers.keySet()) {
				List<String> values = tagToResourceIdentifiers.get(key);
				if(values == null)
					continue;
				List<String> newValues = new ArrayList<String>();
				for(String value : values) {
					if(value.startsWith("#")) {
						List<String> referencedValues = tagToResourceIdentifiers.getOrDefault(value.substring(1), null);
						if(referencedValues == null)
							continue;
						newValues.addAll(referencedValues);
						
						// We just included the values from a referenced
						// tag, but that referenced tag could also be
						// referencing other tags, so we want to do another
						// iteration just to make sure we pre-resolved
						// all tags.
						needAnotherIteration = true;
					}else {
						newValues.add(value);
					}
				}
				tagToResourceIdentifiers.put(key, newValues);
			}
		}
		
	}
	
	private static void processNamespace(String namespace, File namespaceFolder) {
		File tagsFolder = new File(namespaceFolder, "tags");
		if(!tagsFolder.exists())
			return;
		processFolder(namespace, "", tagsFolder);
	}
	
	private static void processFolder(String namespace, String parent, File folder) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				processFolder(namespace, parent + file.getName() + "/", file);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				processTagsFile(namespace + ":" + parent + file.getName().split("\\.")[0], file);
			}
		}
	}
	
	private static void processTagsFile(String resourceIdentifier, File file) {
		try {
			JsonReader reader = new JsonReader(new BufferedReader(new FileReader(file)));
			JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
			
			boolean replace = false;
			if(data.has("replace"))
				replace = data.get("replace").getAsBoolean();
			
			List<String> values = new ArrayList<String>();
			
			if(data.has("values")) {
				JsonArray jsonValues = data.get("values").getAsJsonArray();
				for(JsonElement el : jsonValues.asList()) {
					String name = null;
					if(el.isJsonPrimitive()) {
						name = el.getAsString();
					}else if(el.isJsonObject()) {
						if(el.getAsJsonObject().has("id")) {
							name = el.getAsJsonObject().get("id").getAsString();
						}
					}
					if(name != null) {
						if(!name.contains(":")) {
							if(name.startsWith("#"))
								name = "#minecraft:" + name.substring(1);
							else
								name = "minecraft:" + name;
						}
						values.add(name);
					}
				}
			}
			
			if(replace) {
				tagToResourceIdentifiers.put(resourceIdentifier, values);
			}else {
				List<String> existingList = tagToResourceIdentifiers.getOrDefault(resourceIdentifier, null);
				if(existingList == null) {
					tagToResourceIdentifiers.put(resourceIdentifier, values);
				}else {
					existingList.addAll(values);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> emptyList = new ArrayList<String>();
	public static List<String> getNamesInTag(String tag){
		if(tag.startsWith("#"))
			tag = tag.substring(1);
		if(!tag.contains(":"))
			tag = "minecraft:" + tag;
		List<String> res = tagToResourceIdentifiers.getOrDefault(tag, null);
		if(res == null)
			return emptyList;
		return res;
	}
	
	
	/**
	 * This checks if the resource identifier is in the list,
	 * but if it's a tag, then it will check if the names that
	 * the tag refers to is in the list.
	 * 
	 * @param name
	 * @param listToCheck
	 * @return
	 */
	public static boolean isInList(String name, List<String> listToCheck) {
		if(name.startsWith("#")) {
			List<String> names = getNamesInTag(name.substring(1));
			for(String name2 : names)
				if(listToCheck.contains(name2))
					return true;
			return false;
		}else {
			return listToCheck.contains(name);
		}
	}
	
}
