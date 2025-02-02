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

public class CaveCache {

	private static class CaveCacheChunk{
		
		public byte[][] cache;
		private int minY;
		private int height;
		
		public CaveCacheChunk(int minY, int height) {
			// We add some padding because the exporter also
			// checks neighbour blocks for occlusion
			this.minY = minY - 2;
			this.height = height + 4;
			
			cache = new byte[(this.height / 16) + 1][];
		}
		
		public byte get(int cx, int cy, int cz) {
			cy -= minY;
			int sectionY = cy >> 4;
			cy &= 15;
			if(cache[sectionY] == null)
				return 0;
			cx >>>= 1;
			cy >>>= 1;
			cz >>>= 1;
			return cache[sectionY][cy * 8*8 + cz * 8 + cx];
		}
		
		public void set(int cx, int cy, int cz, byte data) {
			cy -= minY;
			int sectionY = cy >> 4;
			cy &= 15;
			cx >>>= 1;
			cy >>>= 1;
			cz >>>= 1;
			if(cache[sectionY] == null)
				cache[sectionY] = new byte[8*8*8];
			
			cache[sectionY][cy * 8*8 + cz * 8 + cx] = data;
		}
		
	}
	
	private int chunkX;
	private int chunkZ;
	private int chunkSize;
	private CaveCacheChunk chunks[];
	
	public CaveCache(int chunkX, int chunkZ, int chunkSize, int minY, int height) {
		// We add a padding of 1 chunk because we need the neighbour information
		this.chunkX = (chunkX - 1) * 16;
		this.chunkZ = (chunkZ - 1) * 16;
		this.chunkSize = chunkSize + 2;
		this.chunks = new CaveCacheChunk[this.chunkSize*this.chunkSize];
		for(int i = 0; i < this.chunks.length; ++i)
			this.chunks[i] = new CaveCacheChunk(minY, height);
	}
	
	public byte get(int wx, int wy, int wz) {
		wx -= chunkX;
		wz -= chunkZ;
		int x = wx / 16;
		int z = wz / 16;
		return chunks[z * this.chunkSize + x].get(wx & 15, wy, wz & 15);
	}
	
	public void set(int wx, int wy, int wz, byte data) {
		wx -= chunkX;
		wz -= chunkZ;
		int x = wx / 16;
		int z = wz / 16;
		chunks[z * this.chunkSize + x].set(wx & 15, wy, wz & 15, data);
	}
	
}
