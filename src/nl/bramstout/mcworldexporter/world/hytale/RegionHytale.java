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

package nl.bramstout.mcworldexporter.world.hytale;

import java.io.File;
import java.io.IOException;

import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class RegionHytale extends Region{
	
	private ChunkHytale[] chunks = null;
	private Object mutex;
	private IndexedStorageFileReader reader = null;
	private HytaleChunk[] hytaleChunkCache;

	public RegionHytale(World world, File regionFile, int x, int z) {
		super(world, regionFile, x, z);
		this.hytaleChunkCache = new HytaleChunk[16];
		this.mutex = new Object();
	}
	
	@Override
	public int getStride() {
		return 64;
	}
	
	private IndexedStorageFileReader getReader() throws IOException {
		if(world.isPaused())
			return null;
		synchronized(mutex){
			if(reader == null || !reader.isOpen()) {
				reader = new IndexedStorageFileReader(regionFile);
			}
			return reader;
		}
	}
	
	private int localHytaleChunkPosToIndex(int x, int z) {
		return ((z & 31) << 5) | (x & 31);
	}
	
	private void waitForLoad(HytaleChunk chunk, int localX, int localZ) {
		chunk.registerAccess();
		if(chunk.canLoad() || chunk.isLoading()) {
			synchronized(chunk) {
				if(chunk.canLoad()) {
					try {
						byte[] data = getReader().getBlob(localHytaleChunkPosToIndex(localX, localZ));
						chunk.load(data);
					}catch(Exception ex) {
						ex.printStackTrace();
						chunk.load(null);
					}
				}
			}
		}
		if(chunk.canLoad() || chunk.isLoading()) {
			System.out.println("not loaded");
		}
		chunk.registerAccess();
	}
	
	private HytaleChunk initialSearch(int localX, int localZ) {
		for(HytaleChunk chunk : hytaleChunkCache) {
			if(chunk == null)
				continue;
			if(chunk.getX() == localX && chunk.getZ() == localZ) {
				waitForLoad(chunk, localX, localZ);
				return chunk;
			}
		}
		return null;
	}
	
	private HytaleChunk syncSearch(int localX, int localZ) {
		HytaleChunk chunk = null;
		synchronized(hytaleChunkCache) {
			int lastAccessedIndex = 0;
			long lastAccessedTime = Long.MAX_VALUE;
			int index = 0;
			for(HytaleChunk chunk2 : hytaleChunkCache) {
				if(chunk2 == null) {
					lastAccessedIndex = index;
					lastAccessedTime = 0;
					index++;
					continue;
				}
				if(chunk2.getX() == localX && chunk2.getZ() == localZ) {
					chunk = chunk2;
					break;
				}
				if(chunk2.getLastAccessed() < lastAccessedTime) {
					lastAccessedIndex = index;
					lastAccessedTime = chunk2.getLastAccessed();
				}
				index++;
			}
			
			if(chunk == null) {
				chunk = hytaleChunkCache[lastAccessedIndex] = new HytaleChunk(localX, localZ);
			}
		}
		return chunk;
	}
	
	public HytaleChunk getHytaleChunk(int localX, int localZ) {
		HytaleChunk chunk = initialSearch(localX, localZ);
		if(chunk != null)
			return chunk;
		
		chunk = syncSearch(localX, localZ);
		
		waitForLoad(chunk, localX, localZ);
		return chunk;
	}

	@Override
	public void load() throws Exception {
		synchronized(mutex) {
			if(chunks != null)
				return;
			
			getReader();
			
			// A Hytale region is 32x32 Hytale chunks,
			// which is 64x64 MC chunks
			chunks = new ChunkHytale[64*64];
			
			int regionXOffset = this.x * 64;
			int regionZOffset = this.z * 64;
			
			for(int i = 0; i < 64; ++i) {
				for(int j = 0; j < 64; ++j) {
					int index = localHytaleChunkPosToIndex(i/2, j/2);
					if(reader.hasBlob(index)) {
						chunks[j * 64 + i] = new ChunkHytale(this, i + regionXOffset, j + regionZOffset);
					}
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
		try {
			if(reader != null && reader.isOpen())
				reader.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		reader = null;
		
		for(int i = 0; i < hytaleChunkCache.length; ++i)
			hytaleChunkCache[i] = null;
	}

	@Override
	public void unloadEntities() {}

	@Override
	public Chunk getChunk(int worldChunkX, int worldChunkZ) throws Exception {
		if(world.isPaused())
			return null;
		if(chunks == null)
			load();
		worldChunkX -= this.x * 64;
		worldChunkZ -= this.z * 64;
		
		return chunks[worldChunkX + worldChunkZ * 64];
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

	@Override
	public void pause() {
		unloadEntities();
		unload();
	}

}
