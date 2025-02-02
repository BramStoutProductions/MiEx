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

package nl.bramstout.mcworldexporter.world.bedrock;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.World;

public class BedrockBiomes {
	
	private static Map<Integer, String> translationMap = new HashMap<Integer, String>();
	
	public static void load() {
		Map<Integer, String> translationMap = new HashMap<Integer, String>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/bedrock/bedrock_biomes.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement entry : data.asList()) {
					int biomeId = entry.getAsJsonObject().get("id").getAsInt();
					
					String biomeName = entry.getAsJsonObject().get("name").getAsString();
					if(!biomeName.contains(":"))
						biomeName = "minecraft:" + biomeName;
					
					translationMap.put(biomeId, biomeName);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BedrockBiomes.translationMap = translationMap;
	}
	
	public static String getName(int id) {
		String name = translationMap.getOrDefault(id, null);
		if(name == null) {
			World.handleError(new RuntimeException("No biome name for Bedrock id " + id));
			return "minecraft:plains";
		}
		return name;
	}
	
}
