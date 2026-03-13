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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;
import nl.bramstout.mcworldexporter.ui.Renderer2D;
import nl.bramstout.mcworldexporter.world.Chunk;

public class CommandGetChunk extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		boolean heightmap = command.has("heightmap") && command.get("heightmap").getAsBoolean();
		boolean image = command.has("image") && command.get("image").getAsBoolean();
		boolean blocks = command.has("blocks") && command.get("blocks").getAsBoolean();
		boolean biomes = command.has("biomes") && command.get("biomes").getAsBoolean();
		boolean entities = command.has("entities") && command.get("entities").getAsBoolean();
		
		JsonArray chunks = new JsonArray();
		List<Task> tasks = new ArrayList<Task>();
		
		if(command.has("chunks") && MCWorldExporter.getApp().getWorld() != null) {
			for(JsonElement el : command.getAsJsonArray("chunks")) {
				final JsonObject commandChunk = el.getAsJsonObject();
				
				// Multithread it for speed, in case the command got given a lot of chunks.
				Task task = Renderer2D.threadPool.submit(()->{
					int chunkX = 0;
					int chunkZ = 0;
					if(commandChunk.has("chunkX"))
						chunkX = commandChunk.get("chunkX").getAsInt();
					if(commandChunk.has("chunkZ"))
						chunkZ = commandChunk.get("chunkZ").getAsInt();
					
					
					try {
						JsonObject chunkObj = new JsonObject();
						Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(chunkX, chunkZ);
						if(chunk == null)
							return;
						
						chunkObj.addProperty("chunkX", chunk.getChunkX());
						chunkObj.addProperty("chunkZ", chunk.getChunkZ());
						chunkObj.addProperty("dataversion", chunk.getDataVersion());
						
						if(heightmap || image || blocks || biomes) {
							chunk.load();
							chunkObj.addProperty("layerCount", chunk.getLayerCount());
							
							if(heightmap) {
								short[] heightmapData = chunk._getHeightmap();
								if(heightmapData != null) {
									chunkObj.addProperty("heightmap", Util.toBase64(heightmapData));
								}
							}
							if(image) {
								BufferedImage img = chunk.getChunkImage();
								int[] imgData = new int[16*16];
								if(img != null) {
									for(int j = 0; j < 16; ++j) {
										for(int i = 0; i < 16; ++i) {
											imgData[j*16+i] = img.getRGB(i, j);
										}
									}
								}
								chunkObj.addProperty("image", Util.toBase64(imgData));
							}
							if(blocks || biomes) {
								JsonArray sections = new JsonArray();
								int[][][] blocksData = chunk._getBlocks();
								int[][] biomesData = chunk._getBiomes();
								if(blocksData != null && blocksData.length > 0) {
									int numSections = blocksData[0].length;
									for(int i = 0; i < numSections; ++i) {
										JsonObject sectionObj = new JsonObject();
										sectionObj.addProperty("sectionY", chunk._getChunkSectionOffset() + i);
										
										if(blocks) {
											JsonArray blockLayers = new JsonArray();
											for(int layerI = 0; layerI < blocksData.length; ++layerI) {
												int[] sectionBlocks = blocksData[layerI][i];
												if(sectionBlocks != null)
													blockLayers.add(Util.toBase64(sectionBlocks));
												else
													blockLayers.add("");
											}
											sectionObj.add("blocks", blockLayers);
										}
										if(biomes) {
											if(biomesData != null && biomesData[i] != null) {
												sectionObj.addProperty("biomes", Util.toBase64(biomesData[i]));
											}else {
												sectionObj.addProperty("biomes", "");
											}
										}
										
										sections.add(sectionObj);
									}
								}
								chunkObj.add("sections", sections);
							}
						}
						
						if(entities) {
							JsonArray entitiesArray = new JsonArray();
							chunk.loadEntities();
							for(Entity entity : chunk.getEntities()) {
								JsonObject entityObj = new JsonObject();
								entityObj.addProperty("id", entity.getId());
								entityObj.addProperty("x", entity.getX());
								entityObj.addProperty("y", entity.getY());
								entityObj.addProperty("z", entity.getZ());
								entityObj.addProperty("pitch", entity.getPitch());
								entityObj.addProperty("yaw", entity.getYaw());
								entityObj.addProperty("headPitch", entity.getHeadPitch());
								entityObj.addProperty("headYaw", entity.getHeadYaw());
								entityObj.add("data", entity.getProperties().asJson());
								entitiesArray.add(entityObj);
							}
							chunkObj.add("entities", entitiesArray);
						}
						
						synchronized(chunks) {
							chunks.add(chunkObj);
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				});
				
				tasks.add(task);
			}
		}
		
		for(Task task : tasks) {
			task.waitUntilTaskIsDoneFast();
		}
		
		
		JsonObject res = new JsonObject();
		res.add("chunks", chunks);
		return res;
	}

}
