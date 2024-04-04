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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;

public class BlockRegistry {
	
	private static List<Block> registeredBlocks = new ArrayList<Block>();
	private static Map<String, Integer> nameToId = new HashMap<String, Integer>();
	private static Object mutex = new Object();
	private static AtomicInteger changeCounter = new AtomicInteger();
	
	static {
		// Make sure that air always gets an id of 0
		getIdForName("minecraft:air", new TAG_Compound(""));
	}
	
	public static int getIdForName(String name, TAG_Compound properties) {
		if(!name.contains(":"))
			name = "minecraft:" + name;
		if(properties == null)
			properties = new TAG_Compound("");
		String idName = name + properties.toString();
		Integer id = nameToId.get(idName);
		if(id == null) {
			synchronized(mutex) {
				id = nameToId.get(idName);
				if(id == null) {
					id = registeredBlocks.size();
					registeredBlocks.add(getBlockFromName(name, properties, id.intValue()));
					nameToId.put(idName, id);
				}
			}
		}
		return id.intValue();
	}
	
	public static Block getBlock(int id) {
		return registeredBlocks.get(id);
	}
	
	private static Block getBlockFromName(String name, TAG_Compound properties, int id) {
		return new Block(name, properties, id);
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
		getIdForName("minecraft:air", new TAG_Compound(""));
	}
	
	public static int getChangeCounter() {
		return changeCounter.get();
	}
	
}
