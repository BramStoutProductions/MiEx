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

package nl.bramstout.mcworldexporter.atlas;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class Atlas {
	
	public static class AtlasItem{
		public String name;
		public String atlas;
		public float x;
		public float y;
		public float width;
		public float height;
		public int padding;
		
		public AtlasItem(String name, JsonObject data){
			this.name = name;
			if(data == null)
				return;
			atlas = data.get("atlas").getAsString();
			x = data.get("x").getAsFloat();
			y = data.get("y").getAsFloat();
			width = data.get("width").getAsFloat();
			height = data.get("height").getAsFloat();
			padding = 1;
			if(data.has("padding"))
				padding = data.get("padding").getAsInt();
		}
		
		public AtlasItem(AtlasItem other) {
			this.name = other.name;
			this.atlas = other.atlas;
			this.x = other.x;
			this.y = other.y;
			this.width = other.width;
			this.height = other.height;
			this.padding = other.padding;
		}
		
		public boolean isInItem(float u, float v) {
			u *= width;
			v *= height;
			u -= x;
			v -= (height - y - ((float) padding));
			return u >= 0f && u <= ((float) padding) &&
					v >= 0f && v <= ((float) padding);
		}
		
		public float uToLocal(float u) {
			return (u * width) - x;
		}
		
		public float vToLocal(float v) {
			return (v * height) - (height - y - ((float) padding));
		}
		
		public float uToAtlas(float u) {
			return (u + x) / width;
		}
		
		public float vToAtlas(float v) {
			return (v + (height - y - ((float) padding))) / height;
		}
		
	}
	
	private static HashMap<String, AtlasItem> items = new HashMap<String, AtlasItem>();
	private static HashMap<String, String> atlasToTemplateTexture = new HashMap<String, String>();
	private static HashMap<String, List<AtlasItem>> atlases = new HashMap<String, List<AtlasItem>>();
	
	public static void readAtlasConfig() {
		items.clear();
		atlasToTemplateTexture.clear();
		atlases.clear();
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			try {
				if(i < (resourcePacks.size()-1)) {
					// The current resource pack could override a texture that is in an atlas
					// in a lower priority resource pack, so we need to make sure to get rid of
					// those.
					// We don't need to do it for the base resource pack, since the items map
					// is already empty.
					for(File rootFolder : resourcePacks.get(i).getFolders()) {
						File assetsFolder = new File(rootFolder, "assets");
						if(assetsFolder.exists() && assetsFolder.isDirectory()) {
							for(File namespace : assetsFolder.listFiles())
								if(namespace.isDirectory())
									processNamespace(namespace);
						}
					}
				}
				
				
				File atlasFile = new File(resourcePacks.get(i).getFolder(), "miex_atlas.json");
				if(atlasFile.exists()) {
					JsonObject data = Json.read(atlasFile).getAsJsonObject();
					for (Entry<String, JsonElement> entry : data.entrySet()) {
						try {
							if(Config.bannedMaterials.contains(entry.getKey()))
								continue;
							if(Config.ignoreAtlas.contains(entry.getKey()))
								continue;
							// If it's null or an empty object, then that means that texture
							// shouldn't be part of an atlas.
							if(entry.getValue().isJsonNull() || entry.getValue().getAsJsonObject().isEmpty()) {
								items.remove(entry.getKey());
							}else {
								AtlasItem item = new AtlasItem(entry.getKey(), entry.getValue().getAsJsonObject());
								items.put(entry.getKey(), item);
								atlasToTemplateTexture.put(item.atlas, entry.getKey());
							}
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		for(Entry<String, AtlasItem> entry : items.entrySet()) {
			List<AtlasItem> item = atlases.getOrDefault(entry.getValue().atlas, null);
			if(item == null) {
				item = new ArrayList<AtlasItem>();
				atlases.put(entry.getValue().atlas, item);
			}
			item.add(entry.getValue());
		}
	}
	
	private static void processNamespace(File namespaceFolder) {
		File texturesFolder = new File(namespaceFolder, "textures");
		if(texturesFolder.exists() && texturesFolder.isDirectory()) {
			processFolder(texturesFolder, namespaceFolder.getName(), "");
		}
		File optifineFolder = new File(namespaceFolder, "optifine");
		if(optifineFolder.exists() && optifineFolder.isDirectory()) {
			processFolder(optifineFolder, "optifine;" + namespaceFolder.getName(), "");
		}
	}
	
	private static void processFolder(File folder, String namespace, String parent) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory())
				processFolder(f, namespace, parent + f.getName() + "/");
			else if(f.isFile()) {
				if(f.getName().endsWith(".png")) {
					// We've found a texture, so now we need to remove it from the atlas.
					// If the current resource pack does put it into an atlas, it'll add
					// it back in.
					String resourceName = namespace + ":" + parent + f.getName().replace(".png", "");
					items.remove(resourceName);
				}
			}
		}
	}
	
	public static AtlasItem getAtlasItem(String texture) {
		return items.get(texture);
	}
	
	public static String getTemplateTextureForAtlas(String atlas, String defaultValue) {
		return atlasToTemplateTexture.getOrDefault(atlas, defaultValue);
	}
	
	public static List<AtlasItem> getItems(String atlas){
		return atlases.get(atlas);
	}
	
}
