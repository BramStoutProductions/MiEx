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

package nl.bramstout.mcworldexporter.locators;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tags;

public abstract class Locators {
	
	private static class Selection{
		
		private List<String> ids;
		private Map<String, List<String>> state;
		
		public Selection(JsonElement data) {
			ids = new ArrayList<String>();
			state = new HashMap<String, List<String>>();
			if(data.isJsonObject()) {
				if(data.getAsJsonObject().has("id")) {
					String id = data.getAsJsonObject().get("id").getAsString();
					if(id.startsWith("#")) {
						ids.addAll(Tags.getNamesInTag(id));
					}else {
						if(id.indexOf(':') == -1)
							id = "minecraft:" + id;
						ids.add(id);
					}
				}
				if(data.getAsJsonObject().has("state")) {
					for(Entry<String, JsonElement> entry : data.getAsJsonObject().getAsJsonObject("state").entrySet()) {
						List<String> values = new ArrayList<String>();
						if(entry.getValue().isJsonArray()) {
							for(JsonElement el : entry.getValue().getAsJsonArray()) {
								values.add(el.getAsString());
							}
						}else if(entry.getValue().isJsonPrimitive()) {
							values.add(entry.getValue().getAsString());
						}
						state.put(entry.getKey(), values);
					}
				}
			}else if(data.isJsonPrimitive()) {
				String id = data.getAsString();
				if(id.startsWith("#")) {
					ids.addAll(Tags.getNamesInTag(id));
				}else {
					if(id.indexOf(':') == -1)
						id = "minecraft:" + id;
					ids.add(id);
				}
			}
		}
		
		public boolean isInSelection(String name, NbtTagCompound properties) {
			if(!ids.contains(name))
				return false;
			
			if(properties == null && !state.isEmpty())
				return false;
			
			for(Entry<String, List<String>> entry : state.entrySet()) {
				NbtTag propValue = properties.get(entry.getKey());
				if(propValue == null)
					return false;
				
				if(!entry.getValue().contains(propValue.asString()))
					return false;
			}
			return true;
		}
		
	}
	
	private String name;
	private List<Selection> selections;
	
	public Locators(String name, JsonObject data) {
		this.name = name;
		this.selections = new ArrayList<Selection>();
		
		if(data.has("blocks")) {
			for(JsonElement el : data.getAsJsonArray("blocks")) {
				this.selections.add(new Selection(el));
			}
		}
	}
	
	public boolean isInSelection(String name, NbtTagCompound properties) {
		for(int i = 0; i < selections.size(); ++i) {
			Selection sel = selections.get(i);
			if(sel.isInSelection(name, properties))
				return true;
		}
		return false;
	}
	
	public String getName() {
		return name;
	}
	
	
	
	private static Map<String, List<Locators>> locatorsForBlockRegistry = new HashMap<String, List<Locators>>();
	
	public static void load() {
		for(int i = ResourcePacks.getActiveResourcePacks().size() - 1; i >= 0; --i) {
			File locatorsFolder = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), "locators");
			if(!locatorsFolder.exists() || !locatorsFolder.isDirectory())
				continue;
			for(File namespaceFolder : locatorsFolder.listFiles()) {
				if(!namespaceFolder.isDirectory())
					continue;
				File pointsFolder = new File(namespaceFolder, "points");
				if(pointsFolder.exists() && pointsFolder.isDirectory())
					loadPointsFolder(pointsFolder, namespaceFolder.getName() + ":");
			}
		}
	}
	
	private static void loadPointsFolder(File folder, String parent) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadPointsFolder(file, parent + file.getName() + "/");
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				String name = file.getName();
				int dotIndex = name.lastIndexOf('.');
				name = name.substring(0, dotIndex);
				List<Locators> locators = loadPointsFile(file, parent + name);
				for(Locators locator : locators) {
					for(Selection selection : locator.selections) {
						for(String blockName : selection.ids) {
							List<Locators> locatorsList = locatorsForBlockRegistry.getOrDefault(blockName, null);
							if(locatorsList == null) {
								locatorsList = new ArrayList<Locators>();
								locatorsForBlockRegistry.put(blockName, locatorsList);
							}
							if(!locatorsList.contains(locator))
								locatorsList.add(locator);
						}
					}
				}
			}
		}
	}
	
	private static List<Locators> loadPointsFile(File file, String name) {
		JsonArray data = Json.read(file).getAsJsonArray();
		List<Locators> locators = new ArrayList<Locators>();
		for(JsonElement el : data) {
			if(el.isJsonObject()) {
				locators.add(new PointLocators(name, el.getAsJsonObject()));
			}
		}
		return locators;
	}
	
	public static boolean hasLocatorsForBlock(String name) {
		return locatorsForBlockRegistry.containsKey(name);
	}
	
	public static List<Locators> getLocatorForBlock(String name, NbtTagCompound properties){
		List<Locators> locators = locatorsForBlockRegistry.getOrDefault(name, null);
		if(locators == null)
			return null;
		
		List<Locators> locators2 = new ArrayList<Locators>();
		for(int i = 0; i < locators.size(); ++i) {
			Locators locator = locators.get(i);
			if(locator.isInSelection(name, properties)) {
				locators2.add(locator);
			}
		}
		if(locators2.isEmpty())
			return null;
		return locators2;
	}
	
}
