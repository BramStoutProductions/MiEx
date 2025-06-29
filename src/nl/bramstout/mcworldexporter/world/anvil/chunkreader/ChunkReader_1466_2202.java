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

package nl.bramstout.mcworldexporter.world.anvil.chunkreader;

import java.util.Arrays;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLongArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;

/**
 * Chunks from 18w06a (1.13) to 19w36a (1.15) 
 */
public class ChunkReader_1466_2202 extends ChunkReader{
	
	@Override
	public void readChunk(Chunk chunk, NbtTagCompound rootTag, int dataVersion) {
		NbtTagCompound levelTag = (NbtTagCompound) rootTag.get("Level");
		
		String status = "full";
		NbtTagString statusTag = (NbtTagString) levelTag.get("Status");
		if(statusTag != null)
			status = statusTag.getData();
		if (!status.contains("full") && !status.contains("post")) {
			//chunk._setBlocks(new int[1][]);
			//chunk._setBiomes(new int[1][]);
			return;
		}
		
		Reference<char[]> charBuffer = new Reference<char[]>();
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_JAVA.getTranslator(dataVersion);

		NbtTagList sections = (NbtTagList) levelTag.get("Sections");
		int minSectionY = Integer.MAX_VALUE;
		int maxSectionY = Integer.MIN_VALUE;
		NbtTagCompound section = null;
		for (NbtTag tag : sections.getData()) {
			section = (NbtTagCompound) tag;
			minSectionY = Math.min(minSectionY, ((NbtTagByte) section.get("Y")).getData());
			maxSectionY = Math.max(maxSectionY, ((NbtTagByte) section.get("Y")).getData());
		}
		if(minSectionY == Integer.MAX_VALUE || maxSectionY == Integer.MIN_VALUE) {
			return;
		}
		
		chunk._setChunkSectionOffset(minSectionY);

		chunk._setBlocks(new int[maxSectionY - chunk._getChunkSectionOffset() + 1][]);
		chunk._setBiomes(new int[maxSectionY - chunk._getChunkSectionOffset() + 1][]);

		NbtTagIntArray biomesTag = (NbtTagIntArray) levelTag.get("Biomes");
		
		int sectionY;
		NbtTagList palette = null;
		int[] paletteMap = null;
		String blockName = "";
		NbtTagCompound blockProperties = null;
		int i = 0;
		NbtTagLongArray sectionData;
		int[] sectionBlocks = null;
		int[] sectionBiomes = null;
		int bitsPerId = 0;
		int longIndex = 0;
		int longSubIndex = 0;
		int idIndex = 0;
		long paletteIndex = 0;

		for (NbtTag tag : sections.getData()) {
			section = (NbtTagCompound) tag;
			sectionY = ((NbtTagByte) section.get("Y")).getData();
			
			palette = (NbtTagList) section.get("Palette");
			if(palette == null)
				continue;
			if (paletteMap == null || palette.getSize() > paletteMap.length)
				paletteMap = new int[palette.getSize()];
			i = 0;
			for (NbtTag block : palette.getData()) {
				blockName = ((NbtTagString) ((NbtTagCompound) block).get("Name")).getData();
				if(blockName.equals("cave_air") || blockName.equals("minecraft:cave_air") || 
						blockName.equals("void_air") || blockName.equals("minecraft:void_air"))
					blockName = "minecraft:air";
				blockProperties = (NbtTagCompound) ((NbtTagCompound) block).get("Properties");
				boolean freeBlockProperties = false;
				if(blockProperties == null) {
					blockProperties = NbtTagCompound.newInstance("");
					freeBlockProperties = true;
				}
				blockName = blockTranslatorManager.map(blockName, blockProperties);
				paletteMap[i] = BlockRegistry.getIdForName(blockName, blockProperties, dataVersion, charBuffer);
				if(freeBlockProperties)
					blockProperties.free();
				++i;
			}
			
			// If this section is entirely air, let's keep the section array still null for efficiency
			if(palette.getSize() == 1 && paletteMap[0] == 0)
				continue;

			sectionData = (NbtTagLongArray) section.get("BlockStates");
			sectionBlocks = new int[16*16*16];
			chunk._getBlocks()[sectionY - chunk._getChunkSectionOffset()] = sectionBlocks;

			if (sectionData == null) {
				Arrays.fill(sectionBlocks, paletteMap[0]);
			} else {
				bitsPerId = sectionData.getData().length * 64 / 4096;
				idIndex = 0;
				paletteIndex = 0;
				for (i = 0; i < 16 * 16 * 16; ++i) {
					longIndex = idIndex / 64;
					longSubIndex = idIndex % 64;
					paletteIndex = (sectionData.getData()[longIndex] >>> longSubIndex) & (-1l >>> (64 - bitsPerId));
					if(longSubIndex + bitsPerId > 64) {
						// Index data overflowed into the next long, so get those few bits
						paletteIndex |= (sectionData.getData()[longIndex + 1] & (-1l >>> (64 - ((longSubIndex + bitsPerId) - 64)))) << 
										(64 - longSubIndex);
					}
					sectionBlocks[i] = paletteMap[(int) paletteIndex];
					idIndex += bitsPerId;
				}
			}
			
			if(biomesTag != null){
				sectionBiomes = new int[4*4*4];
				chunk._getBiomes()[sectionY - chunk._getChunkSectionOffset()] = sectionBiomes;

				i = 0;
				for(int y = 0; y < 4; ++y) {
					for(int z = 0; z < 4; ++z) {
						for(int x = 0; x < 4; ++x) {
							int index = (z * 4 + 2) * 16 + (x * 4 + 2);
							sectionBiomes[i] = BiomeIds.getRuntimeIdForId(biomesTag.getData()[index], dataVersion);
							
							i++;
						}
					}
				}
			}
		}
		
		NbtTagList blockEntities = (NbtTagList) levelTag.get("TileEntities");
		if(blockEntities != null) {
			NbtTagCompound blockEntity = null;
			String blockEntityName = "";
			int blockEntityX = 0;
			int blockEntityY = 0;
			int blockEntityZ = 0;
			int blockId = 0;
			int blockEntitySectionY = 0;
			int currentBlockId = 0;
			Block currentBlock = null;
			for(NbtTag tag : blockEntities.getData()) {
				blockEntity = (NbtTagCompound) tag;
				NbtTagByte keepPackedTag = (NbtTagByte)blockEntity.get("keepPacked");
				if(keepPackedTag == null || keepPackedTag.getData() > 0)
					continue;
				blockEntityName = ((NbtTagString) blockEntity.get("id")).getData();
				blockEntityX = blockEntity.get("x").asInt();
				blockEntityY = blockEntity.get("y").asInt();
				blockEntityZ = blockEntity.get("z").asInt();
				blockEntityX -= chunk.getChunkX() * 16;
				blockEntityZ -= chunk.getChunkZ() * 16;
				// Make sure that the block entity is actually in the chunk.
				if(blockEntityX < 0 || blockEntityX > 15 || blockEntityZ < 0 || blockEntityZ > 15)
					continue;
				
				currentBlockId = chunk.getBlockIdLocal(blockEntityX, blockEntityY, blockEntityZ);
				currentBlock = BlockRegistry.getBlock(currentBlockId);
				blockEntity.addAllElements(currentBlock.getProperties());
				if(currentBlockId > 0)
					blockEntityName = currentBlock.getName();
				
				blockEntityName = blockTranslatorManager.map(blockEntityName, blockEntity);
				
				blockId = BlockRegistry.getIdForName(blockEntityName, blockEntity, dataVersion, charBuffer);
				blockEntitySectionY = (blockEntityY < 0 ? (blockEntityY - 15) : blockEntityY) / 16;
				blockEntityY -= blockEntitySectionY * 16;
				
				blockEntitySectionY -= chunk._getChunkSectionOffset();
				if(blockEntitySectionY >= chunk._getBlocks().length)
					continue;
				if(chunk._getBlocks()[blockEntitySectionY] == null)
					chunk._getBlocks()[blockEntitySectionY] = new int[16*16*16];
				
				chunk._getBlocks()[blockEntitySectionY][blockEntityY * 16 * 16 + blockEntityZ * 16 + blockEntityX] = blockId;
			}
		}
	}

	@Override
	public boolean supportDataVersion(int dataVersion) {
		return dataVersion >= 1466 && dataVersion <= 2202;
	}
	
}
