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

package nl.bramstout.mcworldexporter.commands;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class CommandGetBiome extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		JsonArray biomeArray = new JsonArray();
		if(command.has("biomeIds")) {
			// Biome id mode
			for(JsonElement el : command.getAsJsonArray("biomeIds")) {
				int biomeId = el.getAsInt();
				Biome biome = BiomeRegistry.getBiome(biomeId);
				
				JsonObject biomeObj = new JsonObject();
				biomeObj.addProperty("id", biome.getId());
				biomeObj.addProperty("name", biome.getName());
				JsonObject biomeColours = new JsonObject();
				for(Entry<String, Color> entry : biome.getColors()) {
					JsonArray colour = new JsonArray();
					colour.add(entry.getValue().getR());
					colour.add(entry.getValue().getG());
					colour.add(entry.getValue().getB());
					colour.add(entry.getValue().getA());
					biomeColours.add(entry.getKey(), colour);
				}
				biomeObj.add("colours", biomeColours);
				biomeArray.add(biomeObj);
			}
		}
		if(command.has("biomeNames")){
			// Biome name mode
			for(JsonElement el : command.getAsJsonArray("biomeNames")) {
				int biomeId = BiomeRegistry.getIdForName(el.getAsString());
				Biome biome = BiomeRegistry.getBiome(biomeId);
				
				JsonObject biomeObj = new JsonObject();
				biomeObj.addProperty("id", biome.getId());
				biomeObj.addProperty("name", biome.getName());
				JsonObject biomeColours = new JsonObject();
				for(Entry<String, Color> entry : biome.getColors()) {
					JsonArray colour = new JsonArray();
					colour.add(entry.getValue().getR());
					colour.add(entry.getValue().getG());
					colour.add(entry.getValue().getB());
					colour.add(entry.getValue().getA());
					biomeColours.add(entry.getKey(), colour);
				}
				biomeObj.add("colours", biomeColours);
				biomeArray.add(biomeObj);
			}
		}
		if(command.has("blockLocations") && MCWorldExporter.getApp().getWorld() != null) {
			// Location mode
			
			for(JsonElement el : command.getAsJsonArray("blockLocations")) {
				JsonObject locObj = el.getAsJsonObject();
				int blockX = 0;
				int blockY = 0;
				int blockZ = 0;
				if(locObj.has("x"))
					blockX = locObj.get("x").getAsInt();
				if(locObj.has("y"))
					blockY = locObj.get("y").getAsInt();
				if(locObj.has("z"))
					blockZ = locObj.get("z").getAsInt();
				
				int biomeId = MCWorldExporter.getApp().getWorld().getBiomeId(blockX, blockY, blockZ);
				Biome biome = BiomeRegistry.getBiome(biomeId);
				
				JsonObject biomeObj = new JsonObject();
				biomeObj.addProperty("id", biome.getId());
				biomeObj.addProperty("name", biome.getName());
				biomeObj.addProperty("x", blockX);
				biomeObj.addProperty("y", blockY);
				biomeObj.addProperty("z", blockZ);
				JsonObject biomeColours = new JsonObject();
				for(Entry<String, Color> entry : biome.getColors()) {
					JsonArray colour = new JsonArray();
					colour.add(entry.getValue().getR());
					colour.add(entry.getValue().getG());
					colour.add(entry.getValue().getB());
					colour.add(entry.getValue().getA());
					biomeColours.add(entry.getKey(), colour);
				}
				biomeObj.add("colours", biomeColours);
				biomeArray.add(biomeObj);
			}
		}
		
		JsonObject res = new JsonObject();
		res.add("biomes", biomeArray);
		return res;
	}

}
