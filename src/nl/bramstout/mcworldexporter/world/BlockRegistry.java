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

package nl.bramstout.mcworldexporter.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.StringMap;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class BlockRegistry {
	
	private static List<Block> registeredBlocks = new ArrayList<Block>();
	private static StringMap<Integer> nameToId = new StringMap<Integer>();
	private static Object mutex = new Object();
	private static AtomicInteger changeCounter = new AtomicInteger();
	
	public static NbtTagCompound EMPTY_COMPOUND = NbtTagCompound.newNonPooledInstance("");
	
	private static final char[] INT_TO_CHAR = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', 
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};
	
	static {
		// Make sure that air always gets an id of 0
		getIdForName("minecraft:air", NbtTagCompound.newNonPooledInstance(""), Integer.MAX_VALUE, new Reference<char[]>());
	}
	
	private static int getUniqueName(String name, NbtTagCompound properties, int dataVersion, Reference<char[]> buffer) {
		if(name.equals("air") || name.equals("minecraft:air")) {
			// Make air always refer to the same block no matter the data version.
			dataVersion = 0;
		}
		int fullLength = name.length() + 8 + 8;
		boolean containsNamespace = name.contains(":");
		if(!containsNamespace)
			fullLength += 10;
		
		if(buffer.value == null || buffer.value.length < fullLength) {
			buffer.value = new char[fullLength];
		}
		
		if(!containsNamespace) {
			buffer.value[0] = 'm';
			buffer.value[1] = 'i';
			buffer.value[2] = 'n';
			buffer.value[3] = 'e';
			buffer.value[4] = 'c';
			buffer.value[5] = 'r';
			buffer.value[6] = 'a';
			buffer.value[7] = 'f';
			buffer.value[8] = 't';
			buffer.value[9] = ':';
		}
		int iOffset = containsNamespace ? 0 : 10;
		for(int i = 0; i < name.length(); ++i) {
			buffer.value[iOffset + i] = name.charAt(i);
		}
		iOffset += name.length();
		
		int propertyHash = properties.hashCode();
		for(int i = 0; i < 8; ++i) {
			buffer.value[iOffset + i] = INT_TO_CHAR[(propertyHash >>> (i * 4)) & 0xF];
		}
		
		iOffset += 8;
		for(int i = 0; i < 8; ++i) {
			buffer.value[iOffset + i] = INT_TO_CHAR[(dataVersion >>> (i * 4)) & 0xF];
		}
		
		return fullLength;
	}
	
	public static int getIdForName(String name, NbtTagCompound properties, int dataVersion, Reference<char[]> charBuffer) {
		//if(!name.contains(":"))
		//	name = "minecraft:" + name;
		if(properties == null)
			properties = EMPTY_COMPOUND;
		//String idName = name + Integer.toHexString(properties.hashCode());
		//String idName = getUniqueName(name, properties);
		int nameLength = getUniqueName(name, properties, dataVersion, charBuffer);
		Integer id = nameToId.getOrNull(charBuffer.value, nameLength);
		if(id == null) {
			synchronized(mutex) {
				id = nameToId.getOrNull(charBuffer.value, nameLength);
				if(id == null) {
					id = registeredBlocks.size();
					registeredBlocks.add(getBlockFromName(name, properties, id.intValue(), dataVersion));
					nameToId.put(new String(charBuffer.value, 0, nameLength), id);
				}
			}
		}
		return id.intValue();
	}
	
	public static Block getBlock(int id) {
		return registeredBlocks.get(id < 0 ? 0 : id);
	}
	
	private static Block getBlockFromName(String name, NbtTagCompound properties, int id, int dataVersion) {
		return new Block(name, properties, id, dataVersion);
	}
	
	public static void clearBlockRegistry() {
		changeCounter.addAndGet(1);
		synchronized(mutex) {
			registeredBlocks.clear();
			nameToId.clear();
		}
		BlockStateRegistry.clearBlockStateRegistry();
		ModelRegistry.clearModelRegistry();
		// Make sure that air always gets an id of 0
		getIdForName("minecraft:air", NbtTagCompound.newNonPooledInstance(""), Integer.MAX_VALUE, new Reference<char[]>());
	}
	
	public static int getChangeCounter() {
		return changeCounter.get();
	}
	
}
