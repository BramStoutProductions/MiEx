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

package nl.bramstout.mcworldexporter.world.anvil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;

public class RegionAnvil extends Region{

	private ChunkAnvil[] chunks = null;
	private Object mutex;
	private FileChannel regionFileChannel;
	private FileChannel entityFileChannel;
	
	public RegionAnvil(File regionFile, int x, int z) {
		super(regionFile, x, z);
		this.mutex = new Object();
		this.regionFileChannel = null;
		this.entityFileChannel = null;
	}

	@Override
	public void load() throws Exception {
		synchronized(mutex) {
			if(chunks != null)
				return;
			
			FileInputStream fis= new FileInputStream(regionFile);
	
			byte[] offsetArray = new byte[4096];
			fis.read(offsetArray);
			fis.close();
			ByteBuffer buffer = ByteBuffer.wrap(offsetArray);
			
			byte[] entityOffsetArray = new byte[4096];
			
			File entityFile = new File(regionFile.getAbsolutePath().replace("\\", "/").replace("/region/", "/entities/"));
			if(entityFile.exists()) {
				fis = new FileInputStream(entityFile);
				fis.read(entityOffsetArray);
				fis.close();
			}
			ByteBuffer entityBuffer = ByteBuffer.wrap(entityOffsetArray);
			
			chunks = new ChunkAnvil[32*32];
			
			int regionXOffset = this.x * 32;
			int regionZOffset = this.z * 32;
			
			int i = 0;
			int z = 0;
			int x = 0;
			int data;
			int dataOffset;
			int dataSize;
			int entityData;
			int entityDataOffset;
			int entityDataSize;
			for(z = 0; z < 32; ++z) {
				for(x = 0; x < 32; ++x) {
					data = buffer.getInt();
					dataOffset = (data >> 8) * 4096;
					dataSize = (data & 0xFF) * 4096;
					
					entityData = entityBuffer.getInt();
					entityDataOffset = (entityData >> 8) * 4096;
					entityDataSize = (entityData & 0xFF) * 4096;
					
					chunks[i] = new ChunkAnvil(x + regionXOffset, z + regionZOffset, this, 
							dataOffset, dataSize, entityDataOffset, entityDataSize);
					++i;
				}
			}
		}
	}

	@Override
	public void unload() {
		chunks = null;
		try {
			if(regionFileChannel != null)
				regionFileChannel.close();
			if(entityFileChannel != null)
				entityFileChannel.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public FileChannel getRegionChannel() throws IOException{
		synchronized(mutex) {
			if(regionFileChannel == null)
				regionFileChannel = FileChannel.open(regionFile.toPath(), StandardOpenOption.READ);
			return regionFileChannel;
		}
	}
	
	public FileChannel getEntityChannel() throws IOException{
		synchronized(mutex) {
			if(entityFileChannel == null)
				entityFileChannel = FileChannel.open(new File(regionFile.getAbsolutePath().replace("\\", "/").replace("/region/", "/entities/")).toPath(), StandardOpenOption.READ);
			return entityFileChannel;
		}
	}

	@Override
	public Chunk getChunk(int worldChunkX, int worldChunkZ) throws Exception {
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
