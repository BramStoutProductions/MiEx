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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;
import nl.bramstout.mcworldexporter.preview.ChunkPreviewer;
import nl.bramstout.mcworldexporter.ui.Renderer2D;
import nl.bramstout.mcworldexporter.world.Chunk;

public class CommandChunkPreview extends Command{

	@Override
	public JsonObject run(JsonObject command) {
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
						
						ChunkPreviewer previewer = new ChunkPreviewer(chunk);
						if(commandChunk.has("minX"))
							previewer.minX = commandChunk.get("minX").getAsInt();
						if(commandChunk.has("minY"))
							previewer.minY = commandChunk.get("minY").getAsInt();
						if(commandChunk.has("minZ"))
							previewer.minZ = commandChunk.get("minZ").getAsInt();
						if(commandChunk.has("maxX"))
							previewer.maxX = commandChunk.get("maxX").getAsInt();
						if(commandChunk.has("maxY"))
							previewer.maxY = commandChunk.get("maxY").getAsInt();
						if(commandChunk.has("maxZ"))
							previewer.maxZ = commandChunk.get("maxZ").getAsInt();
						if(commandChunk.has("lodSize"))
							previewer.lodSize = commandChunk.get("lodSize").getAsInt();
						if(commandChunk.has("lodYSize"))
							previewer.lodYSize = commandChunk.get("lodYSize").getAsInt();
						if(commandChunk.has("worldOffsetX"))
							previewer.worldOffsetX = commandChunk.get("worldOffsetX").getAsInt();
						if(commandChunk.has("worldOffsetY"))
							previewer.worldOffsetY = commandChunk.get("worldOffsetY").getAsInt();
						if(commandChunk.has("worldOffsetZ"))
							previewer.worldOffsetZ = commandChunk.get("worldOffsetZ").getAsInt();
						if(commandChunk.has("originOnBlockCenter"))
							previewer.originOnBlockCenter = commandChunk.get("originOnBlockCenter").getAsBoolean();
						if(commandChunk.has("singleMeshNoTextures"))
							previewer.singleMeshNoTextures = commandChunk.get("singleMeshNoTextures").getAsBoolean();
						
						previewer.generatePreviewMeshes();
						
						chunkObj.addProperty("chunkX", chunk.getChunkX());
						chunkObj.addProperty("chunkZ", chunk.getChunkZ());
						
						JsonArray meshesArray = new JsonArray();
						List<Mesh> meshes = previewer.getMeshes();
						for(Mesh mesh : meshes) {
							meshesArray.add(mesh.toJson());
						}
						chunkObj.add("meshes", meshesArray);
						
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
