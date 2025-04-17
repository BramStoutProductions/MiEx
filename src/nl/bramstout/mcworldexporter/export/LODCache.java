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

package nl.bramstout.mcworldexporter.export;

import java.util.Arrays;

import nl.bramstout.mcworldexporter.world.Chunk;

public class LODCache {

	// lod sizes: 2, 4, 8, 16 = 4 levels
	private static int NUM_CACHE_LEVELS = 4;
	
	private static class LODCacheChunk{
		
		public int[][] cache;
		private int minY;
		private int height;
		
		public LODCacheChunk(int minY, int height) {
			cache = new int[NUM_CACHE_LEVELS][];
			// We add some padding because the exporter also
			// checks neighbour blocks for occlusion
			this.minY = ((minY >> 4) << 4) - 16;
			this.height = (((height + 15) >> 4) << 4) + 32;
		}
		
		public boolean get(int cx, int cy, int cz, int lodSize, int lodSizeY, int[] out) {
			int lodLevel = Integer.numberOfTrailingZeros(lodSize);
			int lodLevelY = Integer.numberOfTrailingZeros(lodSizeY);
			if(cache[lodLevel-1] == null)
				return false;
			
			cy -= minY;
			cx >>= lodLevel;
			cy >>= lodLevelY;
			cz >>= lodLevel;
			int width = 16 >> lodLevel;
			int offset = (cy * width * width + cz * width + cx) * 4;
			int[] cacheChunk = cache[lodLevel-1];
			if(cacheChunk[offset] == -2)
				return false;
			out[0] = cacheChunk[offset];
			out[1] = cacheChunk[offset + 1];
			out[2] = cacheChunk[offset + 2];
			out[3] = cacheChunk[offset + 3];
			return true;
		}
		
		public void set(int cx, int cy, int cz, int lodSize, int lodSizeY, int blockId, int blockX, int blockY, int blockZ) {
			int lodLevel = Integer.numberOfTrailingZeros(lodSize);
			int lodLevelY = Integer.numberOfTrailingZeros(lodSizeY);
			int width = 16 >> lodLevel;
			if(cache[lodLevel-1] == null) {
				cache[lodLevel-1] = new int[width*width*((this.height >> lodLevelY) + 1)*4];
				Arrays.fill(cache[lodLevel-1], -2);
			}
			
			cy -= minY;
			cx >>= lodLevel;
			cy >>= lodLevelY;
			cz >>= lodLevel;
			int offset = (cy * width * width + cz * width + cx) * 4;
			int[] cacheChunk = cache[lodLevel-1];
			cacheChunk[offset] = blockId;
			cacheChunk[offset + 1] = blockX;
			cacheChunk[offset + 2] = blockY;
			cacheChunk[offset + 3] = blockZ;
		}
		
	}
	
	private int chunkX;
	private int chunkZ;
	private int chunkSize;
	private LODCacheChunk chunks[];
	
	public LODCache(int chunkX, int chunkZ, int chunkSize, int minY, int height) {
		// We add a padding of 1 chunk because we need the neighbour information
		this.chunkX = chunkX - 1;
		this.chunkZ = chunkZ - 1;
		this.chunkSize = chunkSize + 2;
		this.chunks = new LODCacheChunk[this.chunkSize*this.chunkSize];
		for(int i = 0; i < this.chunks.length; ++i)
			this.chunks[i] = new LODCacheChunk(minY, height);
	}
	
	public boolean get(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodSizeY, int[] out) {
		int x = chunk.getChunkX() - chunkX;
		int z = chunk.getChunkZ() - chunkZ;
		return chunks[z * this.chunkSize + x].get(cx, cy, cz, lodSize, lodSizeY, out);
	}
	
	public void set(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodSizeY, int blockId, int blockX, int blockY, int blockZ) {
		int x = chunk.getChunkX() - chunkX;
		int z = chunk.getChunkZ() - chunkZ;
		chunks[z * this.chunkSize + x].set(cx, cy, cz, lodSize, lodSizeY, blockId, blockX, blockY, blockZ);
	}
	
}
