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

import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class RegionBedrock extends Region{
	
	private ChunkBedrock[] chunks = null;
	private Object mutex;
	private int dimensionId;

	public RegionBedrock(World world, File regionFile, int x, int z, int dimensionId) {
		super(world, regionFile, x, z);
		this.mutex = new Object();
		this.dimensionId = dimensionId;
	}
	
	@Override
	public void pause() {
		unloadEntities();
		unload();
	}

	@Override
	public void load() throws Exception {
		synchronized(mutex) {
			if(chunks != null)
				return;
			
			chunks = new ChunkBedrock[32*32];
			
			int i = 0;
			int regionXOffset = this.x * 32;
			int regionZOffset = this.z * 32;
			for(int z = 0; z < 32; ++z) {
				for(int x = 0; x < 32; ++x) {
					
					chunks[i] = new ChunkBedrock(this, x + regionXOffset, z + regionZOffset, dimensionId);
					++i;
				}
			}
		}
	}

	@Override
	public void unload() {
		if(chunks != null) {
			for(Chunk chunk : chunks) {
				if(chunk == null)
					continue;
				chunk.unload();
			}
		}
		chunks = null;
	}
	
	@Override
	public void unloadEntities() {
		if(chunks != null) {
			for(Chunk chunk : chunks) {
				if(chunk == null)
					continue;
				chunk.unloadEntities();
			}
		}
	}

	@Override
	public Chunk getChunk(int worldChunkX, int worldChunkZ) throws Exception {
		if(world.isPaused())
			return null;
		if(chunks == null)
			load();
		worldChunkX -= this.x * 32;
		worldChunkZ -= this.z * 32;
		
		return chunks[worldChunkX + worldChunkZ * 32];
	}

	@Override
	public void forceReRender() {
		if(chunks == null)
			return;
		for(Chunk chunk : chunks) {
			if(chunk == null)
				continue;
			chunk.setShouldRender(true);
			chunk.setFullReRender(true);
		}
	}

}
