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

import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;

public class ChunkHytale extends Chunk{

	private Object mutex;
	
	public ChunkHytale(Region region, int chunkX, int chunkZ) {
		super(region, chunkX, chunkZ);
		this.mutex = new Object();
	}

	@Override
	protected void _load() throws Exception {
		if (blocks != null)
			return;
		
		synchronized (mutex) {
			if (blocks != null)
				return;
			
			loadError = true;
		
			int localHytaleX = (this.chunkX - region.getXCoordinate() * 64) >> 1;
			int localHytaleZ = (this.chunkZ - region.getZCoordinate() * 64) >> 1;
			HytaleChunk hytaleChunk = ((RegionHytale)region).getHytaleChunk(localHytaleX, localHytaleZ);
			
			if(hytaleChunk == null) 
				return;
			
			// Hytale worlds always go from 0 to 320,
			// meaning that sections always go from 0 to 20,
			// since each section is 16 blocks tall.
			int minSectionY = 0;
			int maxSectionY = 20;
			chunkSectionOffset = minSectionY;
			blocks = new int[maxSectionY - chunkSectionOffset + 1][];
			biomes = new int[maxSectionY - chunkSectionOffset + 1][];
			
			int localOffsetX = ((this.chunkX - region.getXCoordinate() * 64) & 1) * 16;
			int localOffsetZ = ((this.chunkZ - region.getZCoordinate() * 64) & 1) * 16;
			
			for(int sectionY = minSectionY; sectionY < maxSectionY; ++sectionY) {
				int[] sectionBlocks = new int[16*16*16];
				blocks[sectionY - chunkSectionOffset] = sectionBlocks;
				
				int localOffsetY = sectionY * 16;
				
				for(int by = 0; by < 16; ++by) {
					for(int bz = 0; bz < 16; ++bz) {
						for(int bx = 0; bx < 16; ++bx) {
							int blockId = hytaleChunk.getBlock(bx + localOffsetX, by + localOffsetY, bz + localOffsetZ);
							sectionBlocks[by * 16 * 16 + bz * 16 + bx] = blockId;
						}
					}
				}
				
				int[] sectionBiomes = new int[4*4*4];
				biomes[sectionY - chunkSectionOffset] = sectionBiomes;
				for(int by = 0; by < 4; ++by) {
					for(int bz = 0; bz < 4; ++bz) {
						for(int bx = 0; bx < 4; ++bx) {
							int biomeId = hytaleChunk.getBiome(bx * 4 + localOffsetX, 
											by * 4 + localOffsetY, bz * 4 + localOffsetZ);
							sectionBiomes[by * 4 * 4 + bz * 4 + bx] = biomeId;
						}
					}
				}
			}
			
			calculateHeightmap();
			this.lastAccess = System.currentTimeMillis();
		}
		if(this.blocks != null)
			loadError = false;
	}

	@Override
	protected void _loadEntities() throws Exception {}

	@Override
	public void unload() {
		synchronized(mutex) {
			blocks = null;
			biomes = null;
			chunkSectionOffset = 0;
		}
	}

}
