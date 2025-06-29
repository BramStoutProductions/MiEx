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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.iq80.leveldb.DB;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;

public class ChunkBedrock extends Chunk{
	
	private Object mutex;
	private int dimensionId;
	private List<Entity> blockEntitiesAsNormalEntities;

	public ChunkBedrock(Region region, int chunkX, int chunkZ, int dimensionId) {
		super(region, chunkX, chunkZ);
		this.mutex = new Object();
		this.dimensionId = dimensionId;
		this.blockEntitiesAsNormalEntities = new ArrayList<Entity>();
	}

	@Override
	protected void _load() throws Exception {
		if (blocks != null)
			return;
		
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_BEDROCK.getTranslator(0);
		
		synchronized (mutex) {
			if (blocks != null)
				return;

			loadError = true;
			
			DB worldDB = ((WorldBedrock)region.getWorld()).getWorldDB();
			if(worldDB == null) {
				loadError = false;
				return;
			}
			byte[][] subChunkData = new byte[48][];
			int minSectionY = 127;
			int maxSectionY = -128;
			for(int y = -16; y < 32; ++y) {
				byte[] key = BedrockUtils.bytes(chunkX, chunkZ, (byte)0x2F, (byte)y);
				if(dimensionId != 0)
					key = BedrockUtils.bytes(chunkX, chunkZ, dimensionId, (byte)0x2F, (byte)y);
				byte[] data = worldDB.get(key);
				if(data != null) {
					subChunkData[y + 16] = data;
					minSectionY = Math.min(minSectionY, y);
					maxSectionY = Math.max(maxSectionY, y);
				}
			}
			if(minSectionY > maxSectionY)
				return; // No chunk sections for this chunk.
			chunkSectionOffset = minSectionY;
			blocks = new int[maxSectionY - chunkSectionOffset + 1][];
			biomes = new int[maxSectionY - chunkSectionOffset + 1][];
			
			int[] paletteMap = null;
			int[] sectionBlocks = null;
			int[] sectionData = new int[4096];
			int bitsPerId = 0;
			int idsPerInt = 0;
			int intIndex = 0;
			int idIndex = 0;
			int paletteIndex = 0;
			String blockName = "";
			NbtTagCompound blockProperties = null;
			Reference<char[]> charBuffer = new Reference<char[]>();
			
			byte[] key = BedrockUtils.bytes(chunkX, chunkZ, (byte)0x2B);
			if(dimensionId != 0)
				key = BedrockUtils.bytes(chunkX, chunkZ, dimensionId, (byte)0x2B);
			byte[] biomeData = worldDB.get(key);
			int[] sectionBiomes = null;
			ByteArrayDataInputStream biomeDis = null;
			if(biomeData != null) {
				biomeDis = new ByteArrayDataInputStream(biomeData);
				biomeDis.skipBytes(256*2); // Heightmap data.
			}
			
			for(int y2 = minSectionY; y2 <= maxSectionY; ++y2) {
				byte[] data = subChunkData[y2 + 16];
				if(data == null)
					continue;
				ByteArrayDataInputStream dis = new ByteArrayDataInputStream(data);
				int versionNumber = dis.readUnsignedByte();
				if(versionNumber != 1 && versionNumber != 8 && versionNumber != 9)
					throw new Exception("Chunk section format not supported. " + 
										"Some chunks might not be loaded. Data version: " + 
										versionNumber);
				int storageCount = 1;
				if(versionNumber == 8 || versionNumber == 9)
					storageCount = dis.readUnsignedByte();
				if(storageCount <= 0)
					continue;
				
				int y = y2;
				if(versionNumber == 9) {
					y = dis.readByte(); // Y index
					if(y < minSectionY || y > maxSectionY)
						continue;
				}
				
				sectionBlocks = new int[16*16*16];
				blocks[y - chunkSectionOffset] = sectionBlocks;
				
				for(int blockStorageI = 0; blockStorageI < storageCount; ++blockStorageI) {
					int blockVersionNumber = dis.readUnsignedByte();
					bitsPerId = blockVersionNumber >>> 1;
					if(bitsPerId == 0)
						continue;
					if(bitsPerId > 32)
						throw new Exception("Invalid block storage format");
					idsPerInt = 32 / bitsPerId;
					intIndex = 0;
					idIndex = 0;
					paletteIndex = 0;
					
					int numBytes = (int) Math.ceil(4096f / ((float) idsPerInt));
					dis.readInts(sectionData, 0, numBytes);
					
					int paletteSize = dis.readInt();
					if(paletteMap == null || paletteMap.length < paletteSize)
						paletteMap = new int[paletteSize];
					
					for (int i = 0; i < paletteSize; ++i) {
						NbtTag block = NbtTag.readFromStream(dis);
						if(!(block instanceof NbtTagCompound)) {
							paletteMap[i] = 0;
							continue;
						}
						blockName = ((NbtTagString) ((NbtTagCompound) block).get("name")).getData();
						blockProperties = (NbtTagCompound) ((NbtTagCompound) block).get("states");
						boolean needsFreeing = false;
						if(blockProperties == null) {
							blockProperties = NbtTagCompound.newInstance("states");
							needsFreeing = true;
						}
						
						blockName = blockTranslatorManager.map(blockName, blockProperties);
						
						if(blockProperties.getSize() == 0) {
							if(needsFreeing) {
								blockProperties.free();
								needsFreeing = false;
							}
							blockProperties = null; // If there are no properties, then default to null
													// so that we don't have two possible values for
													// a block with no properties. Which the BlockRegistry
													// would see as different blocks.
						}
						
						paletteMap[i] = BlockRegistry.getIdForName(blockName, blockProperties, 0, charBuffer);
						block.free();
						if(needsFreeing)
							blockProperties.free();
					}
				
					if(blockStorageI == 0) {
						int i = 0;
						for(int bx = 0; bx < 16; ++bx) {
							for(int bz = 0; bz < 16; ++bz) {
								for(int by = 0; by < 16; ++by) {
									intIndex = i / idsPerInt;
									idIndex = i % idsPerInt;
									paletteIndex = (sectionData[intIndex] >>> (idIndex * bitsPerId)) & (-1 >>> (32 - bitsPerId));
									sectionBlocks[by * 16 * 16 + bz * 16 + bx] = paletteMap[paletteIndex];
									i++;
								}
							}
						}
					}else {
						// The following blocks are on extra layers, so we need to combine them together
						int i = 0;
						int overlayBlockId = 0;
						int currentBlockId = 0;
						Block currentBlock = null;
						Block overlayBlock = null;
						NbtTagCompound newProperties = null;
						NbtTagString waterloggedTag = null;
						for(int bx = 0; bx < 16; ++bx) {
							for(int bz = 0; bz < 16; ++bz) {
								for(int by = 0; by < 16; ++by) {
									intIndex = i / idsPerInt;
									idIndex = i % idsPerInt;
									paletteIndex = (sectionData[intIndex] >>> (idIndex * bitsPerId)) & (-1 >>> (32 - bitsPerId));
									currentBlockId = sectionBlocks[by * 16 * 16 + bz * 16 + bx];
									overlayBlockId = paletteMap[paletteIndex];
									if(currentBlockId <= 0) {
										// We don't yet have a block there, so just set it.
										sectionBlocks[by * 16 * 16 + bz * 16 + bx] = overlayBlockId;
									}else {
										// There is a block there, so if this overlay block is water
										// set the waterlogged property
										overlayBlock = BlockRegistry.getBlock(overlayBlockId);
										if(overlayBlock.getName().equals("minecraft:water")) {
											currentBlock = BlockRegistry.getBlock(currentBlockId);
											newProperties = (NbtTagCompound) currentBlock.getProperties().copy();
											waterloggedTag = NbtTagString.newInstance("waterlogged", "true");
											newProperties.addElement(waterloggedTag);
											currentBlockId = BlockRegistry.getIdForName(currentBlock.getName(), 
																	newProperties, 0, charBuffer);
											newProperties.free();
										}
										sectionBlocks[by * 16 * 16 + bz * 16 + bx] = currentBlockId;
									}
									i++;
								}
							}
						}
					}
				}
				
				// Biome data
				if(biomeData != null) {
					sectionBiomes = new int[4*4*4];
					biomes[y - chunkSectionOffset] = sectionBiomes;
					
					int blockVersionNumber = biomeDis.readUnsignedByte();
					bitsPerId = blockVersionNumber >>> 1;
					
					if(bitsPerId == 0) {
						if(blockVersionNumber == 0) {
							Arrays.fill(sectionBiomes, BiomeRegistry.getIdForName("minecraft:plains"));
						}else {
							int id = biomeDis.readInt();
							String biomeName = BedrockBiomes.getName(id);
							biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
							Arrays.fill(sectionBiomes, BiomeRegistry.getIdForName(biomeName));
						}
					}else if(bitsPerId > 32) {
						Arrays.fill(sectionBiomes, BiomeRegistry.getIdForName("minecraft:plains"));
					}else {
						idsPerInt = Math.max(32 / bitsPerId, 1);
						intIndex = 0;
						idIndex = 0;
						paletteIndex = 0;
						int numBytes = (int) Math.ceil(4096f / ((float) idsPerInt));
						biomeDis.readInts(sectionData, 0, numBytes);
						
						int paletteSize = biomeDis.readInt();
						if(paletteMap == null || paletteMap.length < paletteSize)
							paletteMap = new int[paletteSize];
						
						for (int i = 0; i < paletteSize; ++i) {
							if((blockVersionNumber & 1) == 0) {
								NbtTag block = NbtTag.readFromStream(biomeDis);
								int id = 0;
								if(block != null)
									id = block.asByte();
								String biomeName = BedrockBiomes.getName(id);
								biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
								paletteMap[i] = BiomeRegistry.getIdForName(biomeName);
								if(block != null)
									block.free();
							}else {
								int id = biomeDis.readInt();
								String biomeName = BedrockBiomes.getName(id);
								biomeName = TranslationRegistry.BIOME_BEDROCK.map(biomeName);
								paletteMap[i] = BiomeRegistry.getIdForName(biomeName);
							}
						}
						
						for(int bx = 0; bx < 4; ++bx) {
							for(int bz = 0; bz < 4; ++bz) {
								for(int by = 0; by < 4; ++by) {
									int i = (bx * 4) * 16 * 16 + (bz * 4) * 16 + (by * 4);
									intIndex = i / idsPerInt;
									idIndex = i % idsPerInt;
									paletteIndex = (sectionData[intIndex] >>> (idIndex * bitsPerId)) & (-1 >>> (32 - bitsPerId));
									sectionBiomes[by * 4 * 4 + bz * 4 + bx] = paletteMap[paletteIndex];
								}
							}
						}
					}
				}
			}
			
			key = BedrockUtils.bytes(chunkX, chunkZ, (byte)0x31);
			if(dimensionId != 0)
				key = BedrockUtils.bytes(chunkX, chunkZ, dimensionId, (byte)0x31);
			byte[] blockEntitiesData = worldDB.get(key);
			blockEntitiesAsNormalEntities.clear();
			if(blockEntitiesData != null) {
				NbtTagCompound blockEntity = null;
				NbtTag tmpTag = null;
				String blockEntityName = "";
				int blockEntityX = 0;
				int blockEntityY = 0;
				int blockEntityZ = 0;
				int blockId = 0;
				int blockEntitySectionY = 0;
				int currentBlockId = 0;
				Block currentBlock = null;
				ByteArrayDataInputStream dis = new ByteArrayDataInputStream(blockEntitiesData);
				while(dis.available() > 0) {
					NbtTag tag = NbtTag.readFromStream(dis);
					blockEntity = (NbtTagCompound) tag;
					tmpTag = blockEntity.get("id");
					if(tmpTag == null) {
						tag.free();
						continue;
					}
					blockEntityName = ((NbtTagString) tmpTag).getData();
					blockEntityX = blockEntity.get("x").asInt();
					blockEntityY = blockEntity.get("y").asInt();
					blockEntityZ = blockEntity.get("z").asInt();
					blockEntityX -= chunkX * 16;
					blockEntityZ -= chunkZ * 16;
					// Make sure that the block entity is actually in the chunk.
					if(blockEntityX < 0 || blockEntityX > 15 || blockEntityZ < 0 || blockEntityZ > 15) {
						tag.free();
						continue;
					}
					
					currentBlockId = getBlockIdLocal(blockEntityX, blockEntityY, blockEntityZ);
					currentBlock = BlockRegistry.getBlock(currentBlockId);
					blockEntity.addAllElements(currentBlock.getProperties());
					if(currentBlockId > 0)
						blockEntityName = currentBlock.getName();
					
					// Special code for item frames
					if(blockEntityName.equals("minecraft:frame") || blockEntityName.equals("minecraft:glow_frame")) {
						if(blockEntityName.equals("minecraft:frame"))
							blockEntityName = "minecraft:item_frame";
						if(blockEntityName.equals("minecraft:glow_frame"))
							blockEntityName = "minecraft:glow_item_frame";
						Entity entity = EntityRegistry.getEntity(blockEntityName, blockEntity);
						if(entity != null) {
							blockEntitiesAsNormalEntities.add(entity);
							
							blockEntitySectionY = (blockEntityY < 0 ? (blockEntityY - 15) : blockEntityY) / 16;
							blockEntityY -= blockEntitySectionY * 16;
							
							blockEntitySectionY -= chunkSectionOffset;
							if(blockEntitySectionY < blocks.length)
								if(blocks[blockEntitySectionY] != null)
									blocks[blockEntitySectionY][blockEntityY * 16 * 16 + blockEntityZ * 16 + blockEntityX] = 0;
							
							tag.free();
							continue;
						}
					}
					
					blockEntityName = blockTranslatorManager.map(blockEntityName, blockEntity);
					
					// Special code for chests
					if(blockEntityName.equals("minecraft:chest") || blockEntityName.equals("minecraft:trapped_chest") || 
							blockEntityName.equals("minecraft:ender_chest")) {
						NbtTag pairX = blockEntity.get("pairx");
						NbtTag pairZ = blockEntity.get("pairz");
						NbtTag facing = blockEntity.get("facing");
						if(pairX != null && pairZ != null && facing != null) {
							int dx = Integer.parseInt(pairX.asString()) - (chunkX * 16);
							int dz = Integer.parseInt(pairZ.asString()) - (chunkZ * 16);
							dx -= blockEntityX;
							dz -= blockEntityZ;
							if(facing.asString().equals("north")) {
								if(dx > 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "left"));
								else if(dx < 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "right"));
							}else if(facing.asString().equals("south")) {
								if(dx > 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "right"));
								else if(dx < 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "left"));
							}else if(facing.asString().equals("east")) {
								if(dz > 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "left"));
								else if(dz < 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "right"));
							}else if(facing.asString().equals("west")) {
								if(dz > 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "right"));
								else if(dz < 0)
									blockEntity.addElement(NbtTagString.newInstance("type", "left"));
							}
						}
					}
					
					blockId = BlockRegistry.getIdForName(blockEntityName, blockEntity, 0, charBuffer);
					blockEntitySectionY = (blockEntityY < 0 ? (blockEntityY - 15) : blockEntityY) / 16;
					blockEntityY -= blockEntitySectionY * 16;
					
					tag.free();
					
					blockEntitySectionY -= chunkSectionOffset;
					if(blockEntitySectionY < 0 || blockEntitySectionY >= blocks.length)
						continue;
					if(blocks[blockEntitySectionY] == null)
						blocks[blockEntitySectionY] = new int[16*16*16];
					
					blocks[blockEntitySectionY][blockEntityY * 16 * 16 + blockEntityZ * 16 + blockEntityX] = blockId;
				}
			}
			
			calculateHeightmap();
			this.lastAccess = System.currentTimeMillis();
		}
		
		loadError = false;
	}
	
	@Override
	protected void _loadEntities() throws Exception {
		isLoading = true;
		try {
			_load();
			this.lastAccess = System.currentTimeMillis();
			loadedChunks.push(this);
		} finally {
			isLoading = false;
		}
		
		this.entities = new ArrayList<Entity>();
		this.entities.addAll(blockEntitiesAsNormalEntities);
		
		DB worldDB = ((WorldBedrock)region.getWorld()).getWorldDB();
		if(worldDB == null) {
			this.entities = null;
			return;
		}
		
		byte[] key = BedrockUtils.bytes(chunkX, chunkZ, (byte)0x32);
		if(dimensionId != 0)
			key = BedrockUtils.bytes(chunkX, chunkZ, dimensionId, (byte)0x32);
		byte[] entitiesData = worldDB.get(key);
		if(entitiesData != null) {
			ByteArrayDataInputStream dis = new ByteArrayDataInputStream(entitiesData);
			while(dis.available() > 0) {
				NbtTag tag = NbtTag.readFromStream(dis);
				NbtTagCompound entityTag = (NbtTagCompound) tag;
				String name = ((NbtTagString)entityTag.get("identifier")).getData();
				Entity entity = EntityRegistry.getEntity(name, entityTag);
				if(entity != null)
					entities.add(entity);
				tag.free();
			}
		}
		
		key = BedrockUtils.bytes("digp", chunkX, chunkZ);
		if(dimensionId != 0)
			key = BedrockUtils.bytes("digp", chunkX, chunkZ, dimensionId);
		byte[] entitiesKeysData = worldDB.get(key);
		if(entitiesKeysData != null && entitiesKeysData.length > 0) {
			int numEntities = entitiesKeysData.length / 8;
			for(int i = 0; i < numEntities; ++i) {
				int j = i * 8;
				key = BedrockUtils.bytes("actorprefix", 
						entitiesKeysData[j], entitiesKeysData[j + 1], entitiesKeysData[j + 2], entitiesKeysData[j + 3],
						entitiesKeysData[j + 4], entitiesKeysData[j + 5], entitiesKeysData[j + 6], entitiesKeysData[j + 7]);
				loadEntity(worldDB, key);
			}
		}
	}
	
	private void loadEntity(DB worldDB, byte[] key) {
		byte[] entityData = worldDB.get(key);
		if(entityData == null || entityData.length == 0)
			return;
		
		ByteArrayDataInputStream dis = new ByteArrayDataInputStream(entityData);
		try {
			NbtTag dataTag = null;
			try {
				dataTag = NbtTag.readFromStream(dis);
			}catch(Exception ex) {
				// Invalid data, so just ignore it.
				return;
			}
			if(!(dataTag instanceof NbtTagCompound)) {
				dataTag.free();
				return;
			}
			
			NbtTagCompound data = (NbtTagCompound) dataTag;
			
			NbtTagString identifierTag = (NbtTagString) data.get("identifier");
			if(identifierTag == null) {
				dataTag.free();
				return;
			}
			
			Entity entity = EntityRegistry.getEntity(identifierTag.getData(), data);
			if(entity == null) {
				dataTag.free();
				return;
			}

			entities.add(entity);
			dataTag.free();
		}catch(Exception ex) {
			ex.printStackTrace();
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
