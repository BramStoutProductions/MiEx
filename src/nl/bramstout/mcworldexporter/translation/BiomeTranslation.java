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

package nl.bramstout.mcworldexporter.translation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class BiomeTranslation {
	
	private Map<String, String> translationMap;
	private String sourceName;
	
	public BiomeTranslation(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = new HashMap<String, String>();
	}
	
	public void load() {
		Map<String, String> translationMap = new HashMap<String, String>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_biomes.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonObject data = Json.read(translationFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String biomeName = entry.getKey();
					if(!biomeName.contains(":"))
						biomeName = "minecraft:" + biomeName;
					
					String javaBiomeName = entry.getValue().getAsString();
					if(!javaBiomeName.contains(":"))
						javaBiomeName = "minecraft:" + javaBiomeName;
					
					translationMap.put(biomeName, javaBiomeName);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		this.translationMap = translationMap;
	}
	
	/**
	 * Maps the block to Java Edition. It returns the new name
	 * and it will modify properties directly.
	 * 
	 * @param name
	 * @param properties
	 * @return
	 */
	public String map(String name) {
		return translationMap.getOrDefault(name, name);
	}
	
}
