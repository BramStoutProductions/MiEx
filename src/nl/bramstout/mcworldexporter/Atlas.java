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

package nl.bramstout.mcworldexporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class Atlas {
	
	public static class AtlasItem{
		public String atlas;
		public float x;
		public float y;
		public float width;
		public float height;
		
		public AtlasItem(JsonObject data){
			atlas = data.get("atlas").getAsString();
			x = data.get("x").getAsFloat();
			y = data.get("y").getAsFloat();
			width = data.get("width").getAsFloat();
			height = data.get("height").getAsFloat();
		}
	}
	
	private static HashMap<String, AtlasItem> items = new HashMap<String, AtlasItem>();
	
	public static void readAtlasConfig() {
		items.clear();
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			try {
				String atlasName = FileUtil.getResourcePackDir() + resourcePacks.get(i) + "/miex_atlas.json";
				if(new File(atlasName).exists()) {
					JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(new File(atlasName))))).getAsJsonObject();
					for (Entry<String, JsonElement> entry : data.entrySet()) {
						try {
							if(Config.bannedMaterials.contains(entry.getKey()))
								continue;
							// If it's null or an empty object, then that means that texture
							// shouldn't be part of an atlas.
							if(entry.getValue().isJsonNull() || entry.getValue().getAsJsonObject().isEmpty())
								items.remove(entry.getKey());
							else
								items.put(entry.getKey(), new AtlasItem(entry.getValue().getAsJsonObject()));
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static AtlasItem getAtlasItem(String texture) {
		return items.get(texture);
	}
	
}
