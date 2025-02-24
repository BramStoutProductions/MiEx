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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.nbt.NbtDataInputStream;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.anvil.chunkreader.ChunkReader;
import nl.bramstout.mcworldexporter.world.anvil.entityreader.EntityReader;

public class ChunkAnvil extends Chunk {

	private int dataOffset;
	private int dataSize;
	private int entityDataOffset;
	private int entityDataSize;
	private Object mutex;

	public ChunkAnvil(int chunkX, int chunkZ, RegionAnvil region, int dataOffset, int dataSize, int entityDataOffset, int entityDataSize) {
		super(region, chunkX, chunkZ);
		this.region = region;
		this.dataOffset = dataOffset;
		this.dataSize = dataSize;
		this.entityDataOffset = entityDataOffset;
		this.entityDataSize = entityDataSize;
		this.mutex = new Object();
	}
	
	public int getDataSize() {
		return dataSize;
	}

	@Override
	public void _load() throws Exception {
		if (blocks != null)
			return;

		if(dataSize == 0) {
			loadError = true;
			return;
		}
		
		synchronized (mutex) {
			if (blocks != null)
				return;

			loadError = true;
			if(dataSize == 0)
				return;
			
			FileChannel fileChannel = ((RegionAnvil)region).getRegionChannel();
			if(fileChannel == null) {
				loadError = false;
				return;
			}
			MappedByteBuffer buffer = null;
			try {
				buffer = fileChannel.map(MapMode.READ_ONLY, dataOffset, dataSize);
			}catch(Exception ex) {
				return;
			}
			int len = buffer.getInt();
			int compressionType = buffer.get();
			byte[] byteBuffer = new byte[len - 1];
			buffer.get(byteBuffer, 0, len - 1);

			InputStream is = null;
			if (compressionType == 1)
				is = new GZIPInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
			else if (compressionType == 2)
				is = new InflaterInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1), new Inflater(), 1024 * 16);
			else
				throw new Exception("Could not load chunk. Some chunks might not be loaded. Invalid compression type: " + compressionType);

			NbtDataInputStream dis = new NbtDataInputStream(is);
			NbtTagCompound rootTag = (NbtTagCompound) NbtTag.readFromStream(dis);
			dis.close();

			int dataVersion = 0;
			NbtTag dataVersionTag = rootTag.get("DataVersion");
			if (dataVersionTag != null && dataVersionTag.getId() == 3)
				dataVersion = dataVersionTag.asInt();
			
			this.dataVersion = dataVersion;

			ChunkReader chunkReader = ChunkReader.getChunkReader(dataVersion);
			if(chunkReader == null) {
				rootTag.free();
				throw new Exception("Chunk format not supported. Some chunks might not be loaded. Data version: " + dataVersion);
			}
			try {
				chunkReader.readChunk(this, rootTag, dataVersion);
			}catch(Exception ex) {
				rootTag.free();
				throw ex;
			}
			
			rootTag.free();
			
			calculateHeightmap();
			this.lastAccess = System.currentTimeMillis();
		}
		loadError = false;
	}
	
	@Override
	protected void _loadEntities() throws Exception {
		entities = new ArrayList<Entity>();
		// Entities
		if(entityDataSize > 0) {
			FileChannel fileChannel = ((RegionAnvil)region).getEntityChannel();
			if(fileChannel == null) {
				// Older chunk versions store the entities in the main chunk file.
				// If the entity file channel returns null, it means there isn't
				// one, so we are dealing with an older chunk version, where the
				// data is in the main chunk file.
				fileChannel = ((RegionAnvil)region).getRegionChannel();
			}
			
			MappedByteBuffer buffer = fileChannel.map(MapMode.READ_ONLY, entityDataOffset, entityDataSize);
			int len = buffer.getInt();
			byte compressionType = buffer.get();
			byte[] byteBuffer = new byte[len - 1];
			buffer.get(byteBuffer, 0, len - 1);

			InputStream is = null;
			if (compressionType == 1)
				is = new GZIPInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
			else if (compressionType == 2)
				is = new InflaterInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
			else
				throw new Exception("Could not load chunk");

			NbtDataInputStream dis = new NbtDataInputStream(is);
			NbtTagCompound rootTag = (NbtTagCompound) NbtTag.readFromStream(dis);
			dis.close();

			int dataVersion = 0;
			NbtTag dataVersionTag = rootTag.get("DataVersion");
			if (dataVersionTag != null && dataVersionTag.getId() == 3)
				dataVersion = dataVersionTag.asInt();

			
			EntityReader reader = EntityReader.getEntityReader(dataVersion);
			if(reader == null) {
				rootTag.free();
				throw new Exception("Chunk format not supported");
			}
			
			try {
				reader.readEntities(this, rootTag);
			}catch(Exception ex) {
				rootTag.free();
				throw ex;
			}
			
			rootTag.free();
		}
	}

	@Override
	public void unload() {
		synchronized(mutex) {
			blocks = null;
			biomes = null;
			chunkSectionOffset = 0;
		}
	}

}
