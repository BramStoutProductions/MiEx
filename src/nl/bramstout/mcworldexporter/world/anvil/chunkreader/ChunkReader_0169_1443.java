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
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;

/**
 * Chunks from 1.9 to pre-1.13
 */
public class ChunkReader_0169_1443 extends ChunkReader{
	
	@Override
	public void readChunk(Chunk chunk, NbtTagCompound rootTag, int dataVersion) {
		Reference<char[]> charBuffer = new Reference<char[]>();
		
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_JAVA.getTranslator(dataVersion);
		
		NbtTagCompound levelTag = (NbtTagCompound) rootTag.get("Level");

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

		NbtTagByteArray biomesTag = (NbtTagByteArray) levelTag.get("Biomes");
		
		int sectionY;
		int i = 0;
		NbtTagByteArray blocksData;
		NbtTagByteArray addData;
		NbtTagByteArray dataData;
		int[] sectionBlocks = null;
		int[] sectionBiomes = null;

		for (NbtTag tag : sections.getData()) {
			section = (NbtTagCompound) tag;
			sectionY = ((NbtTagByte) section.get("Y")).getData();

			blocksData = (NbtTagByteArray) section.get("Blocks");
			addData = (NbtTagByteArray) section.get("Add");
			dataData = (NbtTagByteArray) section.get("Data");
			sectionBlocks = new int[16*16*16];
			chunk._getBlocks()[sectionY - chunk._getChunkSectionOffset()] = sectionBlocks;

			if (blocksData == null) {
				Arrays.fill(sectionBlocks, 0);
			} else {
				for (i = 0; i < 16 * 16 * 16; ++i) {
					int id = Byte.toUnsignedInt(blocksData.getData()[i]);
					if(addData != null)
						id |= ((Byte.toUnsignedInt(addData.getData()[i/2]) >> ((i%2) * 4)) & 0xF) << 8;
					int data = 0;
					if(dataData != null)
						data = (Byte.toUnsignedInt(dataData.getData()[i/2]) >> ((i%2) * 4)) & 0xF;
					
					sectionBlocks[i] = BlockIds.getRuntimeIdForId(id, data, dataVersion, blockTranslatorManager, charBuffer);
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
							sectionBiomes[i] = BiomeIds.getRuntimeIdForId(Byte.toUnsignedInt(biomesTag.getData()[index]), dataVersion);
							
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
				blockEntityX = ((NbtTagInt) blockEntity.get("x")).getData();
				blockEntityY = ((NbtTagInt) blockEntity.get("y")).getData();
				blockEntityZ = ((NbtTagInt) blockEntity.get("z")).getData();
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
		return dataVersion >= 169 && dataVersion <= 1443;
	}
	
}
