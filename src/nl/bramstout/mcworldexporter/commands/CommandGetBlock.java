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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.LayeredBlock;

public class CommandGetBlock extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		JsonArray blockArray = new JsonArray();
		if(command.has("blockIds")) {
			// Block id mode
			for(JsonElement el : command.getAsJsonArray("blockIds")) {
				int blockId = el.getAsInt();
				Block block = BlockRegistry.getBlock(blockId);
				
				JsonObject blockObj = new JsonObject();
				blockObj.addProperty("id", block.getId());
				blockObj.addProperty("name", block.getName());
				blockObj.addProperty("dataversion", block.getDataVersion());
				blockObj.add("properties", block.getProperties().asJson());
				blockArray.add(blockObj);
			}
		}
		if(command.has("blockLocations") && MCWorldExporter.getApp().getWorld() != null) {
			// Location mode
			
			LayeredBlock layeredBlock = new LayeredBlock();
			
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
				
				if(locObj.has("layer") && !locObj.get("layer").isJsonNull()) {
					int layer = locObj.get("layer").getAsInt();
					
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ, layer);
					Block block = BlockRegistry.getBlock(blockId);
					
					JsonObject blockObj = new JsonObject();
					blockObj.addProperty("id", block.getId());
					blockObj.addProperty("name", block.getName());
					blockObj.addProperty("dataversion", block.getDataVersion());
					blockObj.addProperty("x", blockX);
					blockObj.addProperty("y", blockY);
					blockObj.addProperty("z", blockZ);
					blockObj.addProperty("layer", layer);
					blockObj.add("properties", block.getProperties().asJson());
					blockArray.add(blockObj);
				}else {
					MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ, layeredBlock);
					JsonArray blockObjArray = new JsonArray();
					
					for(int i = 0; i < layeredBlock.getLayerCount(); ++i) {
						int blockId = layeredBlock.getBlock(i);
						Block block = BlockRegistry.getBlock(blockId);
						
						JsonObject blockObj = new JsonObject();
						blockObj.addProperty("id", block.getId());
						blockObj.addProperty("name", block.getName());
						blockObj.addProperty("dataversion", block.getDataVersion());
						blockObj.addProperty("x", blockX);
						blockObj.addProperty("y", blockY);
						blockObj.addProperty("z", blockZ);
						blockObj.addProperty("layer", i);
						blockObj.add("properties", block.getProperties().asJson());
						blockObjArray.add(blockObj);
					}
					
					blockArray.add(blockObjArray);
				}
			}
		}
		
		JsonObject res = new JsonObject();
		res.add("blocks", blockArray);
		return res;
	}

}
