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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map.Entry;

import org.bson.BsonArray;
import org.bson.BsonBinary;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.translation.BiomeTranslation;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.World;

/**
 * Hytale chunks are 32x32 rather than MC's 16x16.
 * To make reading chunk data easier and faster,
 * the data is read into this class first,
 * which stores the 32x32 chunk. The 16x16 ChunkHytale
 * class then copies its portion of data over from
 * this class.
 */
public class HytaleChunk {
	
	private static BsonDocumentCodec bsonCodec = new BsonDocumentCodec();
	private static DecoderContext bsonDecoderContext = DecoderContext.builder().build();

	private int x;
	private int z;
	private long lastAccessed;
	private volatile boolean loaded;
	private volatile boolean loading;
	private Section[] sections;
	private EnvironmentChunk environmentChunk;
	
	public HytaleChunk(int x, int z) {
		this.x = x;
		this.z = z;
		this.lastAccessed = System.currentTimeMillis();
		this.loaded = false;
		this.loading = false;
		this.sections = null;
		this.environmentChunk = null;
	}
	
	public int getX() {
		return x;
	}
	
	public int getZ() {
		return z;
	}
	
	public long getLastAccessed() {
		return lastAccessed;
	}
	
	public void registerAccess() {
		this.lastAccessed = System.currentTimeMillis();
	}
	
	public boolean isLoading() {
		return loading;
	}
	
	public void load(byte[] data) {
		if(this.loaded)
			return;
		this.loading = true;
		this.loaded = true;
		if(data == null) {
			this.loading = false;
			return;
		}
		
		BsonDocument bson = bsonCodec.decode(new BsonBinaryReader(ByteBuffer.wrap(data)), bsonDecoderContext);
		BsonDocument components = bson.getDocument("Components");
		
		if(components.containsKey("ChunkColumn")) {
			BsonDocument chunkColumn = components.getDocument("ChunkColumn");
			if(chunkColumn.containsKey("Sections")) {
				BsonArray sectionsData = chunkColumn.getArray("Sections");
				
				sections = new Section[sectionsData.size()];
				for(int i = 0; i < sections.length; ++i) {
					sections[i] = new Section(sectionsData.get(i).asDocument());
				}
			}
		}
		if(components.containsKey("EnvironmentChunk")) {
			BsonDocument environmentChunk = components.getDocument("EnvironmentChunk");
			int version = environmentChunk.getInt32("Version", new BsonInt32(0)).getValue();
			this.environmentChunk = EnvironmentChunk.getEnvironmentChunk(version);
			try {
				this.environmentChunk.read(environmentChunk);
			}catch(Exception ex) {
				World.handleError(ex);
			}
		}
		
		if(components.containsKey("BlockComponents")) {
			Reference<char[]> charBuffer = new Reference<char[]>();
			BsonDocument blockComponents = components.getDocument("BlockComponents");
			for(Entry<String, BsonValue> entry : blockComponents.entrySet()) {
				try {
					int index = Integer.parseInt(entry.getKey());
					int x = index & 31;
					int z = (index >> 5) & 31;
					int y = index >> 10;
					
					int sectionY = y >> 5;
					if(sectionY < 0 || sectionY >= sections.length)
						continue;
					
					NbtTagCompound properties = NbtTagCompound.newInstance("");
					
					BsonDocument value = entry.getValue().asDocument();
					BsonDocument value2 = value.getDocument("Components", null);
					if(value2 == null)
						continue;
					for(BsonValue value3 : value2.values()) {
						NbtTag componentProps = NbtTag.fromBsonValue(value3);
						if(componentProps != null && componentProps instanceof NbtTagCompound) {
							properties.addAllElements((NbtTagCompound) componentProps);
							componentProps.free();
						}
					}
					if(properties.getSize() == 0) {
						properties.free();
						continue;
					}
					
					// Add the properties to the block.
					Section section = this.sections[sectionY];
					if(section == null)
						continue;
					if(section.blocks == null)
						continue;

					int blockIndex = (((y - sectionY * 32) * 32) + z) * 32 + x;
					int blockId = section.blocks[blockIndex];
					Block block = BlockRegistry.getBlock(blockId);
					properties.addAllElements(block.getProperties());
					blockId = BlockRegistry.getIdForName(block.getName(), properties, 0, charBuffer);
					section.blocks[blockIndex] = blockId;
					
					properties.free();
					
				}catch(Exception ex) {
					World.handleError(ex);
				}
			}
		}
		
		// Just doing a quick check to see if there are any more
		// components in the chunk. This is purely for diagnostic reasons
		// and will be removed at some point.
		for(Entry<String, BsonValue> entry : components.entrySet()) {
			if(entry.getKey().equals("ChunkColumn") || 
					entry.getKey().equals("WorldChunk") || 
					entry.getKey().equals("BlockHealthChunk") || 
					entry.getKey().equals("ChunkSpawnedNPCData") || 
					entry.getKey().equals("EnvironmentChunk") || 
					entry.getKey().equals("BlockChunk") || 
					entry.getKey().equals("EntityChunk"))
				continue;
			if(entry.getKey().equals("BlockComponentChunk")) {
				//BsonValue val = entry.getValue();
				//printValue("", val, 5, 0);
				continue;
			}
			System.out.println(entry.getKey());
		}
		
		this.loading = false;
	}
	
	/*private void printValue(String name, BsonValue value, int depthLeft, int indent) {
		if(depthLeft < 0)
			return;
		if(value.isArray()) {
			System.out.println(" ".repeat(indent*4) + name + "[");
			for(BsonValue value2 : value.asArray().getValues()) {
				printValue("", value2, depthLeft-1, indent+1);
			}
			System.out.println(" ".repeat(indent*4) + "],");
		}else if(value.isDocument()) {
			System.out.println(" ".repeat(indent*4) + name + "{");
			for(Entry<String, BsonValue> entry : value.asDocument().entrySet()) {
				printValue(entry.getKey() + ": ", entry.getValue(), depthLeft-1, indent+1);
			}
			System.out.println(" ".repeat(indent*4) + "},");
		}else {
			System.out.println(" ".repeat(indent*4) + name + value.toString());
		}
	}*/
	
	public int getBlock(int localX, int localY, int localZ) {
		if(this.sections == null) {
			//System.out.println("Sections is null " + Boolean.toString(loading) + " " + Boolean.toString(loaded));
			return 0;
		}
		
		int sectionIndex = localY >> 5;
		int sectionY = localY & 31;
		if(sectionIndex < 0 || sectionIndex >= this.sections.length)
			return 0;
		
		Section section = this.sections[sectionIndex];
		if(section == null)
			return 0;
		return section.getBlockId(localX, sectionY, localZ);
	}
	
	public int getBiome(int localX, int localY, int localZ) {
		if(this.environmentChunk == null)
			return 0;
		return this.environmentChunk.getBiomeId(localX, localY, localZ);
	}
	
	public boolean canLoad() {
		return !this.loaded;
	}
	
	public static class Section{
		
		private int[] blocks;
		
		public Section(BsonDocument data) {
			blocks = null;
			
			BsonDocument components = data.getDocument("Components");
			
			BsonDocument blockSection = components.getDocument("Block", null);
			if(blockSection != null) {
				int version = blockSection.getInt32("Version", new BsonInt32(0)).getValue();
				
				BlockSectionReader reader = BlockSectionReader.getReader(version);
				if(reader != null) {
					try {
						reader.read(this, blockSection);
					}catch(Exception ex) {
						World.handleError(ex);
					}
				}
			}
			
			BsonDocument fluidSection = components.getDocument("Fluid", null);
			if(fluidSection != null) {
				int version = fluidSection.getInt32("Version", new BsonInt32(0)).getValue();
				
				FluidSectionReader reader = FluidSectionReader.getReader(version);
				if(reader != null) {
					try {
						reader.read(this, fluidSection);
					}catch(Exception ex) {
						World.handleError(ex);
					}
				}
			}
			
			for(String key : components.keySet()) {
				if(!key.equals("Block") && !key.equals("Fluid") &&
						!key.equals("ChunkSection") && !key.equals("BlockPhysics")) {
					System.out.println(key);
				}
			}
		}
		
		public int getBlockId(int localX, int localY, int localZ) {
			if(blocks == null)
				return 0;
			return blocks[((localY * 32) + localZ) * 32 + localX];
		}
		
	}
	
	private static abstract class BlockSectionReader{
		
		public abstract void read(Section section, BsonDocument data) throws IOException;
		
		private static final BlockSectionReader V6 = new BlockSectionReaderV6();
		
		public static BlockSectionReader getReader(int version) {
			if(version == 6)
				return V6;
			return null;
		}
		
	}
	
	private static class BlockSectionReaderV6 extends BlockSectionReader{

		@Override
		public void read(Section section, BsonDocument bson) throws IOException {
			BsonBinary data = bson.getBinary("Data", null);
			if(data == null)
				return;
			
			ByteArrayBEDataInputStream bis = new ByteArrayBEDataInputStream(data.getData());
			@SuppressWarnings("unused")
			int blockMigrationVersion = bis.readInt();
			
			
			// Block names
			int blockNamePaletteTypeId = bis.readByte();
			Palette blockNamePalette = Palette.getPallet(blockNamePaletteTypeId);
			if(blockNamePalette != null)
				blockNamePalette.read(PaletteDataDecoder.UTF, bis);
			
			
			// Ticking blocks
			if(blockNamePaletteTypeId != 0) {
				@SuppressWarnings("unused")
				int count = bis.readUnsignedShort();
				int len = bis.readUnsignedShort();
				for(int i = 0; i < len; ++i) {
					bis.readLong();
				}
			}
			
			
			// Filler data
			int fillerPaletteTypeId = bis.readByte();
			Palette fillerPalette = Palette.getPallet(fillerPaletteTypeId);
			if(fillerPalette != null)
				fillerPalette.read(PaletteDataDecoder.UNSIGNED_SHORT, bis);
			
			
			// Rotation data
			int rotationPaletteTypeId = bis.readByte();
			Palette rotationPalette = Palette.getPallet(rotationPaletteTypeId);
			if(rotationPalette != null)
				rotationPalette.read(PaletteDataDecoder.UNSIGNED_BYTE, bis);
			
			
			// Create actual block data
			if(blockNamePalette != null) {
				section.blocks = new int[32*32*32];
				
				Reference<char[]> charBuffer = new Reference<char[]>();
				BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_HYTALE.getTranslator(0);
				
				for(int i = 0; i < section.blocks.length; ++i) {
					String blockName = (String) blockNamePalette.get(i);
					
					int filler = fillerPalette != null ? 
									((Integer) fillerPalette.get(i)).intValue() : 0;
					int rotation = rotationPalette != null ? 
							((Integer) rotationPalette.get(i)).intValue() : 0;
					
					NbtTagCompound properties = null;
					
					boolean isAir = blockName.isEmpty() || blockName.equals("Empty");
					if(isAir) {
						blockName = "minecraft:air";
					}else {
						properties = NbtTagCompound.newNonPooledInstance("");
						
						if(blockName.charAt(0) == '*') {
							// This is a block with a block state stored in the name,
							// so let's parse it.
							int sep = blockName.indexOf("_State_Definitions");
							String stateStr = null;
							if(sep > 0) {
								stateStr = blockName.substring(sep + 7);
								blockName = blockName.substring(1, sep);
							}else {
								blockName = blockName.substring(1);
							}
							
							if(stateStr != null) {
								String[] tokens = stateStr.split("_");
								for(int tokenI = 0; tokenI < tokens.length/2; ++tokenI) {
									NbtTagString propTag = NbtTagString.newNonPooledInstance(
															tokens[tokenI*2], tokens[tokenI*2+1]);
									properties.addElement(propTag);
								}
							}
						}
						
						// Make sure that we have a namespace.
						if(blockName.indexOf(':') == -1)
							blockName = "hytale:" + blockName;
						
						NbtTagInt fillerTag = NbtTagInt.newNonPooledInstance("filler", filler);
						properties.addElement(fillerTag);
						NbtTagInt rotationTag = NbtTagInt.newNonPooledInstance("rotation", rotation);
						properties.addElement(rotationTag);
						
						blockName = blockTranslatorManager.map(blockName, properties);
						
						// If the block got translated into air,
						// then set the properties to null so that all air blocks
						// end up being the exact same.
						if(blockName.equals("minecraft:air"))
							properties = null;
					}
					
					int blockId = BlockRegistry.getIdForName(blockName, properties, 0, charBuffer);
					section.blocks[i] = blockId;
				}
			}
		}
		
	}
	
	private static abstract class FluidSectionReader{
		
		public abstract void read(Section section, BsonDocument data) throws IOException;
		
		private static final FluidSectionReader V0 = new FluidSectionReaderV0();
		
		public static FluidSectionReader getReader(int version) {
			if(version == 0)
				return V0;
			return null;
		}
		
	}
	
	private static class FluidSectionReaderV0 extends FluidSectionReader{

		@Override
		public void read(Section section, BsonDocument bson) throws IOException {
			BsonBinary data = bson.getBinary("Data", null);
			if(data == null)
				return;
			
			ByteArrayBEDataInputStream bis = new ByteArrayBEDataInputStream(data.getData());
			
			
			// Fluid names
			int blockNamePaletteTypeId = bis.readByte();
			Palette blockNamePalette = Palette.getPallet(blockNamePaletteTypeId);
			if(blockNamePalette != null)
				blockNamePalette.read(PaletteDataDecoder.UTF, bis);
			
			byte[] fluidLevels = null;
			if(bis.readBoolean()) {
				fluidLevels = new byte[32*32*32];
				bis.readFully(fluidLevels);
			}
			
			// Create actual block data and write it to the block data.
			// But skip is we only have Empty blocks or no blocks.
			if(blockNamePalette != null && !blockNamePalette.isEmpty("Empty")) {
				if(section.blocks == null) {
					section.blocks = new int[32*32*32];
				}
				
				Reference<char[]> charBuffer = new Reference<char[]>();
				BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_HYTALE.getTranslator(0);
				
				for(int i = 0; i < section.blocks.length; ++i) {
					String blockName = (String) blockNamePalette.get(i);
					
					int fluidLevel = 0;
					if(fluidLevels != null)
						fluidLevel = fluidLevels[i];
					
					NbtTagCompound properties = null;
					
					boolean isAir = blockName.isEmpty() || blockName.equals("Empty");
					if(!isAir) {
						properties = NbtTagCompound.newNonPooledInstance("");
						
						if(blockName.charAt(0) == '*') {
							// This is a block with a block state stored in the name,
							// so let's parse it.
							int sep = blockName.indexOf("_State_Definitions");
							String stateStr = null;
							if(sep > 0) {
								stateStr = blockName.substring(sep + 7);
								blockName = blockName.substring(1, sep);
							}else {
								blockName = blockName.substring(1);
							}
							
							if(stateStr != null) {
								String[] tokens = stateStr.split("_");
								for(int tokenI = 0; tokenI < tokens.length/2; ++tokenI) {
									NbtTagString propTag = NbtTagString.newNonPooledInstance(
															tokens[tokenI*2], tokens[tokenI*2+1]);
									properties.addElement(propTag);
								}
							}
						}
						
						// Make sure that we have a namespace.
						if(blockName.indexOf(':') == -1)
							blockName = "hytale:" + blockName;
						
						NbtTagInt fillerTag = NbtTagInt.newNonPooledInstance("level", fluidLevel);
						properties.addElement(fillerTag);
						
						blockName = blockTranslatorManager.map(blockName, properties);
						
						// If the block got translated into air,
						// then set the properties to null so that all air blocks
						// end up being the exact same.
						if(blockName.equals("minecraft:air"))
							continue;
						
						// Internally, each cell in the grid can only store one block,
						// there is no concept of multiple layers like a fluid layer.
						// So, if there currently is an air block then we can just overwrite it,
						// but otherwise we need to merge the two blocks.
						boolean needsMerge = false;
						Block existingBlock = null;
						if(section.blocks[i] != 0) {
							existingBlock = BlockRegistry.getBlock(section.blocks[i]);
							if(!existingBlock.getName().equals("minecraft:air"))
								needsMerge = true;
						}
						
						if(needsMerge && existingBlock != null) {
							blockName = existingBlock.getName();
							properties.addAllElements(existingBlock.getProperties());
						}
						
						int blockId = BlockRegistry.getIdForName(blockName, properties, 0, charBuffer);
						section.blocks[i] = blockId;
					}
				}
			}
		}
		
	}
	
	private static abstract class PaletteDataDecoder{
		public abstract Object decode(ByteArrayBEDataInputStream bis) throws IOException;
		
		public static final PaletteDataDecoder UTF = new PaletteUtfDataDecoder();
		public static final PaletteDataDecoder UNSIGNED_BYTE = new PaletteUnsignedByteDataDecoder();
		public static final PaletteDataDecoder UNSIGNED_SHORT = new PaletteUnsignedShortDataDecoder();
	}
	private static class PaletteUtfDataDecoder extends PaletteDataDecoder{
		@Override
		public Object decode(ByteArrayBEDataInputStream bis) throws IOException {
			return bis.readHytaleUTF();
		}
	}
	private static class PaletteUnsignedByteDataDecoder extends PaletteDataDecoder{
		@Override
		public Object decode(ByteArrayBEDataInputStream bis) throws IOException {
			return Integer.valueOf(bis.readUnsignedByte());
		}
	}
	private static class PaletteUnsignedShortDataDecoder extends PaletteDataDecoder{
		@Override
		public Object decode(ByteArrayBEDataInputStream bis) throws IOException {
			return Integer.valueOf(bis.readUnsignedShort());
		}
	}
	
	private static abstract class Palette{
		
		protected Object[] palette = null;
		protected short[] data = null;
		
		public Object get(int index) {
			if(palette == null || data == null)
				return null;
			return palette[data[index]];
		}
		
		public boolean isEmpty(Object emptyPaletteData) {
			if(palette == null || data == null)
				return true;
			for(Object obj : palette)
				if(obj != null && !obj.equals(emptyPaletteData))
					return false;
			return true;
		}
		
		public abstract void read(PaletteDataDecoder dataDecoder, ByteArrayBEDataInputStream bis) throws IOException;
		
		public static Palette getPallet(int typeId) {
			if(typeId == 1)
				return new HalfBytePalette();
			else if(typeId == 2)
				return new BytePalette();
			else if(typeId == 3)
				return new ShortPalette();
			return null;
		}
	}
	
	private static class HalfBytePalette extends Palette{
		
		@Override
		public void read(PaletteDataDecoder dataDecoder, ByteArrayBEDataInputStream bis) throws IOException {
			int blockCount = bis.readShort();
			
			palette = new Object[blockCount];
			
			for(int i = 0; i < blockCount; ++i) {
				int paletteIndex = bis.readByte();
				if(paletteIndex >= palette.length)
					palette = Arrays.copyOf(palette, paletteIndex+1);
				
				// Decode block state data
				Object dataObj = dataDecoder.decode(bis);
				palette[paletteIndex] = dataObj;
				
				@SuppressWarnings("unused")
				int usageCount = bis.readShort();
			}
			
			byte[] ids = new byte[16384];
			bis.readFully(ids);
			
			data = new short[32*32*32];
			for(int i = 0; i < data.length; ++i) {
				int index = i >> 1;
				int shift = (index & 1) * 4;
				int paletteIndex = (ids[index] >> shift) & 0xF;
				
				if(paletteIndex < palette.length) {
					data[i] = (short) paletteIndex;
				}
			}
		}
	}
	
	private static class BytePalette extends Palette{
		@Override
		public void read(PaletteDataDecoder dataDecoder, ByteArrayBEDataInputStream bis) throws IOException {
			int blockCount = bis.readShort();
			
			palette = new Object[blockCount];
			
			for(int i = 0; i < blockCount; ++i) {
				int paletteIndex = bis.readByte();
				if(paletteIndex >= palette.length)
					palette = Arrays.copyOf(palette, paletteIndex+1);
				
				// Decode block state data
				Object dataObj = dataDecoder.decode(bis);
				palette[paletteIndex] = dataObj;
				
				@SuppressWarnings("unused")
				int usageCount = bis.readShort();
			}
			
			byte[] ids = new byte[32768];
			bis.readFully(ids);
			
			data = new short[32*32*32];
			for(int i = 0; i < data.length; ++i) {
				int paletteIndex = ids[i];
				
				if(paletteIndex < palette.length) {
					data[i] = (short) paletteIndex;
				}
			}
		}
	}
	
	private static class ShortPalette extends Palette{
		@Override
		public void read(PaletteDataDecoder dataDecoder, ByteArrayBEDataInputStream bis) throws IOException {
			int blockCount = bis.readShort();
			
			palette = new Object[blockCount];
			
			for(int i = 0; i < blockCount; ++i) {
				int paletteIndex = bis.readByte();
				if(paletteIndex >= palette.length)
					palette = Arrays.copyOf(palette, paletteIndex+1);
				
				// Decode block state data
				Object dataObj = dataDecoder.decode(bis);
				palette[paletteIndex] = dataObj;
				
				@SuppressWarnings("unused")
				int usageCount = bis.readShort();
			}
			
			byte[] ids = new byte[65536];
			bis.readFully(ids);
			
			data = new short[32*32*32];
			for(int i = 0; i < data.length; ++i) {
				int paletteIndexUpper = ((int) ids[i*2]) & 0xFF;
				int paletteIndexLower = ((int) ids[i*2+1]) & 0xFF;
				int paletteIndex = paletteIndexUpper << 8 | paletteIndexLower;
				
				if(paletteIndex < palette.length) {
					data[i] = (short) paletteIndex;
				}
			}
		}
	}
	
	private static abstract class EnvironmentChunk{
		
		public abstract void read(BsonDocument data) throws IOException;
		
		public abstract int getBiomeId(int localX, int localY, int localZ);
		
		public static EnvironmentChunk getEnvironmentChunk(int version) {
			if(version == 0)
				return new EnvironmentChunkV0();
			return null;
		}
		
	}
	
	private static class EnvironmentChunkV0 extends EnvironmentChunk{
		
		private int[] palette;
		private int[] data;
		
		@Override
		public void read(BsonDocument bson) throws IOException {
			BsonBinary data = bson.getBinary("Data", null);
			if(data == null)
				return;
			
			ByteArrayBEDataInputStream bis = new ByteArrayBEDataInputStream(data.getData());
			BiomeTranslation biomeTranslatorManager = TranslationRegistry.BIOME_HYTALE;
			
			int biomeCount = bis.readInt();
			
			palette = new int[biomeCount];
			
			for(int i = 0; i < biomeCount; ++i) {
				int paletteIndex = bis.readInt();
				if(paletteIndex >= palette.length)
					palette = Arrays.copyOf(palette, paletteIndex+1);
				
				// Decode block state data
				String biomeName = bis.readHytaleUTF();
				
				if(biomeName.indexOf(':') == -1)
					biomeName = "hytale:" + biomeName;
				
				biomeName = biomeTranslatorManager.map(biomeName);
				
				int biomeId = BiomeRegistry.getIdForName(biomeName);
				
				palette[paletteIndex] = biomeId;
			}
			
			int numColumns = 32*32;
			this.data = new int[32*32*320];
			
			Column columnObj = new Column();
			for(int column = 0; column < numColumns; ++column) {
				columnObj.read(bis);
				
				for(int i = 0; i < columnObj.idsSize; ++i) {
					int id = columnObj.ids[i];
					if(id < 0 || id >= this.palette.length)
						continue;
					
					int minY = 0;
					int maxY = 320;
					if(i > 0)
						minY = columnObj.yTable[i-1] + 1;
					if(i < columnObj.yTableSize)
						maxY = columnObj.yTable[i]+1;
					if(minY < 0)
						minY = 0;
					if(maxY > 320)
						maxY = 320;
					
					for(int y = minY; y < maxY; ++y)
						this.data[column * 320 + y] = id;
				}
			}
		}
		
		@Override
		public int getBiomeId(int localX, int localY, int localZ) {
			return this.palette[this.data[((localZ * 32) + localX) * 320 + localY]];
		}
		
		private static class Column{
			
			public int[] ids = null;
			public int[] yTable = null;
			public int idsSize = 0;
			public int yTableSize = 0;
			
			public void read(ByteArrayBEDataInputStream bis) throws IOException {
				int numIds = bis.readInt();
				
				if(yTable == null || yTable.length < numIds)
					yTable = new int[numIds];
				yTableSize = numIds;
				
				for(int i = 0; i < yTableSize; ++i)
					yTable[i] = bis.readInt();
				
				if(ids == null || ids.length < (numIds+1))
					ids = new int[numIds+1];
				idsSize = numIds+1;
				
				for(int i = 0; i < idsSize; ++i)
					ids[i] = bis.readInt();
			}
			
		}
		
	}
	
}
