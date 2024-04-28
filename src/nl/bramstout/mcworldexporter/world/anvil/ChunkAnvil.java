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
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityRegistry;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Int;
import nl.bramstout.mcworldexporter.nbt.TAG_List;
import nl.bramstout.mcworldexporter.nbt.TAG_Long_Array;
import nl.bramstout.mcworldexporter.nbt.TAG_String;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;

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
			MappedByteBuffer buffer = fileChannel.map(MapMode.READ_ONLY, dataOffset, dataSize);
			int len = buffer.getInt();
			int compressionType = buffer.get();
			byte[] byteBuffer = new byte[len - 1];
			buffer.get(byteBuffer, 0, len - 1);

			InputStream is = null;
			if (compressionType == 1)
				is = new GZIPInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
			else if (compressionType == 2)
				is = new InflaterInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
			else
				throw new Exception("Could not load chunk. Some chunks might not be loaded. Invalid compression type: " + compressionType);

			DataInputStream dis = new DataInputStream(is);
			TAG_Compound rootTag = (TAG_Compound) NBT_Tag.make(dis);
			dis.close();

			int dataVersion = 0;
			NBT_Tag dataVersionTag = rootTag.getElement("DataVersion");
			if (dataVersionTag != null && dataVersionTag.ID() == 3)
				dataVersion = ((TAG_Int) dataVersionTag).value;

			// Only support chunk formats from 21w43a and later
			if (dataVersion < 2844)
				throw new Exception("Chunk format not supported. Some chunks might not be loaded. Data version: " + dataVersion);

			if (!((TAG_String) rootTag.getElement("Status")).value.contains("full")) {
				blocks = new int[1][];
				biomes = new short[1][];
				return;
			}

			chunkSectionOffset = ((TAG_Int) rootTag.getElement("yPos")).value;

			TAG_List sections = (TAG_List) rootTag.getElement("sections");
			int maxSectionY = chunkSectionOffset;
			TAG_Compound section = null;
			for (NBT_Tag tag : sections.elements) {
				section = (TAG_Compound) tag;
				maxSectionY = Math.max(maxSectionY, ((TAG_Byte) section.getElement("Y")).value);
			}

			blocks = new int[maxSectionY - chunkSectionOffset + 1][];
			biomes = new short[maxSectionY - chunkSectionOffset + 1][];

			int sectionY;
			TAG_Compound blockStates = null;
			TAG_Compound biomesTag = null;
			TAG_List palette = null;
			int[] paletteMap = null;
			String blockName = "";
			TAG_Compound blockProperties = null;
			int i = 0;
			TAG_Long_Array sectionData;
			int[] sectionBlocks = null;
			short[] sectionBiomes = null;
			int bitsPerId = 0;
			int idsPerLong = 0;
			int longIndex = 0;
			int idIndex = 0;
			long paletteIndex = 0;

			for (NBT_Tag tag : sections.elements) {
				section = (TAG_Compound) tag;
				sectionY = ((TAG_Byte) section.getElement("Y")).value;
				blockStates = (TAG_Compound) section.getElement("block_states");
				
				biomesTag = (TAG_Compound) section.getElement("biomes");
				if(blockStates != null){
					palette = (TAG_List) blockStates.getElement("palette");
					if (paletteMap == null || palette.elements.length > paletteMap.length)
						paletteMap = new int[palette.elements.length];
					i = 0;
					for (NBT_Tag block : palette.elements) {
						blockName = ((TAG_String) ((TAG_Compound) block).getElement("Name")).value;
						blockProperties = (TAG_Compound) ((TAG_Compound) block).getElement("Properties");
						paletteMap[i] = BlockRegistry.getIdForName(blockName, blockProperties);
						++i;
					}
					
					// If this section is entirely air, let's keep the section array still null for efficiency
					if(palette.elements.length == 1 && paletteMap[0] == 0)
						continue;
	
					sectionData = (TAG_Long_Array) blockStates.getElement("data");
					sectionBlocks = new int[16*16*16];
					blocks[sectionY - chunkSectionOffset] = sectionBlocks;
	
					if (sectionData == null) {
						Arrays.fill(sectionBlocks, paletteMap[0]);
					} else {
						bitsPerId = Math.max(32 - Integer.numberOfLeadingZeros(palette.elements.length - 1), 4);
						idsPerLong = 64 / bitsPerId;
						longIndex = 0;
						idIndex = 0;
						paletteIndex = 0;
						for (i = 0; i < 16 * 16 * 16; ++i) {
							longIndex = i / idsPerLong;
							idIndex = i % idsPerLong;
							paletteIndex = (sectionData.data[longIndex] >>> (idIndex * bitsPerId)) & (-1l >>> (64 - bitsPerId));
							sectionBlocks[i] = paletteMap[(int) paletteIndex];
						}
					}
				}
				
				if(biomesTag != null){
					palette = (TAG_List) biomesTag.getElement("palette");
					if (paletteMap == null || palette.elements.length > paletteMap.length)
						paletteMap = new int[palette.elements.length];
					i = 0;
					for (NBT_Tag block : palette.elements) {
						blockName = ((TAG_String) block).value;
						paletteMap[i] = BiomeRegistry.getIdForName(blockName);
						++i;
					}
	
					sectionData = (TAG_Long_Array) biomesTag.getElement("data");
					sectionBiomes = new short[4*4*4];
					biomes[sectionY - chunkSectionOffset] = sectionBiomes;
	
					if (sectionData == null) {
						Arrays.fill(sectionBiomes, (short) paletteMap[0]);
					} else {
						bitsPerId = Math.max(32 - Integer.numberOfLeadingZeros(palette.elements.length - 1), 1);
						idsPerLong = 64 / bitsPerId;
						longIndex = 0;
						idIndex = 0;
						paletteIndex = 0;
						for (i = 0; i < 4 * 4 * 4; ++i) {
							longIndex = i / idsPerLong;
							idIndex = i % idsPerLong;
							paletteIndex = (sectionData.data[longIndex] >>> (idIndex * bitsPerId)) & (-1l >>> (64 - bitsPerId));
							sectionBiomes[i] = (short) paletteMap[(int) paletteIndex];
						}
					}
				}
			}
			
			TAG_List blockEntities = (TAG_List) rootTag.getElement("block_entities");
			if(blockEntities != null) {
				TAG_Compound blockEntity = null;
				String blockEntityName = "";
				int blockEntityX = 0;
				int blockEntityY = 0;
				int blockEntityZ = 0;
				int blockId = 0;
				int blockEntitySectionY = 0;
				int currentBlockId = 0;
				Block currentBlock = null;
				for(NBT_Tag tag : blockEntities.elements) {
					blockEntity = (TAG_Compound) tag;
					TAG_Byte keepPackedTag = (TAG_Byte)blockEntity.getElement("keepPacked");
					if(keepPackedTag == null || keepPackedTag.value > 0)
						continue;
					blockEntityName = ((TAG_String) blockEntity.getElement("id")).value;
					blockEntityX = ((TAG_Int) blockEntity.getElement("x")).value;
					blockEntityY = ((TAG_Int) blockEntity.getElement("y")).value;
					blockEntityZ = ((TAG_Int) blockEntity.getElement("z")).value;
					blockEntityX -= chunkX * 16;
					blockEntityZ -= chunkZ * 16;
					// Make sure that the block entity is actually in the chunk.
					if(blockEntityX < 0 || blockEntityX >= 15 || blockEntityZ < 0 || blockEntityZ >= 15)
						continue;
					
					currentBlockId = getBlockIdLocal(blockEntityX, blockEntityY, blockEntityZ);
					currentBlock = BlockRegistry.getBlock(currentBlockId);
					blockEntity.elements.addAll(currentBlock.getProperties().elements);
					if(currentBlockId > 0)
						blockEntityName = currentBlock.getName();
					
					blockId = BlockRegistry.getIdForName(blockEntityName, blockEntity);
					blockEntitySectionY = (blockEntityY < 0 ? (blockEntityY - 15) : blockEntityY) / 16;
					blockEntityY -= blockEntitySectionY * 16;
					
					blockEntitySectionY -= chunkSectionOffset;
					if(blockEntitySectionY >= blocks.length)
						continue;
					if(blocks[blockEntitySectionY] == null)
						blocks[blockEntitySectionY] = new int[16*16*16];
					
					blocks[blockEntitySectionY][blockEntityY * 16 * 16 + blockEntityZ * 16 + blockEntityX] = blockId;
				}
			}
			
			// Entities
			if(entityDataSize > 0) {
				fileChannel = ((RegionAnvil)region).getEntityChannel();
				buffer = fileChannel.map(MapMode.READ_ONLY, entityDataOffset, entityDataSize);
				len = buffer.getInt();
				compressionType = buffer.get();
				byteBuffer = new byte[len - 1];
				buffer.get(byteBuffer, 0, len - 1);
	
				is = null;
				if (compressionType == 1)
					is = new GZIPInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
				else if (compressionType == 2)
					is = new InflaterInputStream(new ByteArrayInputStream(byteBuffer, 0, len - 1));
				else
					throw new Exception("Could not load chunk");
	
				dis = new DataInputStream(is);
				rootTag = (TAG_Compound) NBT_Tag.make(dis);
				dis.close();
	
				dataVersion = 0;
				dataVersionTag = rootTag.getElement("DataVersion");
				if (dataVersionTag != null && dataVersionTag.ID() == 3)
					dataVersion = ((TAG_Int) dataVersionTag).value;
	
				// Only support chunk formats from 21w43a and later
				if (dataVersion < 2844)
					throw new Exception("Chunk format not supported");
				
				TAG_List entitiesTag = (TAG_List) rootTag.getElement("Entities");
				for(NBT_Tag tag : entitiesTag.elements) {
					TAG_Compound entityTag = (TAG_Compound) tag;
					String name = ((TAG_String)entityTag.getElement("id")).value;
					Entity entity = EntityRegistry.newEntity(name, entityTag);
					if(entity != null)
						entities.add(entity);
				}
			}
			
			
			calculateHeightmap();
		}
		loadError = false;
	}

	@Override
	public void unload() {
		synchronized(mutex) {
			blocks = null;
			biomes = null;
			entities.clear();
			chunkSectionOffset = 0;
		}
	}

}
