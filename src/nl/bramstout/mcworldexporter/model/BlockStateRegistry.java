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

package nl.bramstout.mcworldexporter.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.model.builtins.BakedBlockStateLiquid;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockStateRegistry;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.java.BlockStateHandlerJavaEdition;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class BlockStateRegistry {
	
	private static List<BlockState> registeredStates = new ArrayList<BlockState>();
	private static Map<String, Integer> nameToId = new HashMap<String, Integer>();
	private static Object mutex = new Object();
	private static int counter = 0;
	private static List<BakedBlockState> bakedBlockStates = new ArrayList<BakedBlockState>();
	private static List<Boolean> needsConnectionInfo = new ArrayList<Boolean>();
	private static Object mutex2 = new Object();
	public static List<String> missingBlockStates = new ArrayList<String>();
	
	public static int getIdForName(String name, int dataVersion) {
		if(!name.contains(":"))
			name = "minecraft:" + name;
		String idName = name + Integer.toHexString(dataVersion);
		Integer id = nameToId.get(idName);
		if(id == null) {
			synchronized(mutex) {
				id = nameToId.get(idName);
				if(id == null) {
					BlockState state = getStateFromName(name, dataVersion);
					registeredStates.add(state);
					nameToId.put(idName, state.getId());
					return state.getId();
				}
			}
		}
		return id.intValue();
	}
	
	public static int getNextId() {
		synchronized(mutex) {
			return counter++;
		}
	}
	
	public static BlockState getState(int id) {
		return registeredStates.get(id < 0 ? 0 : id);
	}
	
	private static BlockState getStateFromName(String name, int dataVersion) {
		if(BuiltInBlockStateRegistry.builtins.containsKey(name) || BuiltInBlockState.hasHandler(name)) {
			if(!ResourcePacks.hasOverride(name, "blockstates", ".json", "assets"))
				return BuiltInBlockStateRegistry.newBlockState(name, dataVersion);
		}
		BlockStateHandler handler = ResourcePacks.getBlockStateHandler(name);
		if(handler == null) {
			synchronized(missingBlockStates) {
				missingBlockStates.add(name);
			}
			World.handleError(new RuntimeException("No blockstate file for " + name));
			
			// Make sure that there is a valid handler anyways.
			handler = new BlockStateHandlerJavaEdition(name, null);
		}
		return new BlockState(name, dataVersion, handler);
	}
	
	public static BakedBlockState getBakedStateForBlock(int blockId, int x, int y, int z) {
		return getBakedStateForBlock(blockId, x, y, z, true);
	}
	
	public static BakedBlockState getBakedStateForBlock(int blockId, int x, int y, int z, boolean runBlockConnections) {
		if(blockId < 0)
			blockId = 0;
		
		if(!runBlockConnections) {
			Block block = BlockRegistry.getBlock(blockId);
			int stateId = getIdForName(block.getName(), block.getDataVersion());
			BlockState state = getState(stateId);
			return state.getBakedBlockState(block.getProperties(), x, y, z, false);
		}
		
		if(blockId >= bakedBlockStates.size()) {
			synchronized(mutex2) {
				for(int i = bakedBlockStates.size(); i < blockId + 1; ++i) {
					bakedBlockStates.add(null);
					needsConnectionInfo.add(false);
				}
				
				Block block = BlockRegistry.getBlock(blockId);
				int stateId = getIdForName(block.getName(), block.getDataVersion());
				BlockState state = getState(stateId);
				
				BakedBlockState bakedState = state.getBakedBlockState(block.getProperties(), x, y, z, true);
				if(!state.needsConnectionInfo())
					bakedBlockStates.set(blockId, bakedState);
				needsConnectionInfo.set(blockId, state.needsConnectionInfo());
				return bakedState;
			}
		}
		BakedBlockState bakedState = null;
		try {
			bakedState = bakedBlockStates.get(blockId);
			if(bakedState != null)
				return bakedState;
			
			if(needsConnectionInfo.get(blockId).booleanValue()) {
				Block block = BlockRegistry.getBlock(blockId);
				int stateId = getIdForName(block.getName(), block.getDataVersion());
				BlockState state = getState(stateId);
				return state.getBakedBlockState(block.getProperties(), x, y, z, true);
			}
		}catch(Exception ex) {} // Empty catch because due to race condition it could fail, but that's fine.
		
		synchronized(mutex2) {
			bakedState = bakedBlockStates.get(blockId);
			if(bakedState != null)
				return bakedState;
			
			if(needsConnectionInfo.get(blockId).booleanValue()) {
				Block block = BlockRegistry.getBlock(blockId);
				int stateId = getIdForName(block.getName(), block.getDataVersion());
				BlockState state = getState(stateId);
				return state.getBakedBlockState(block.getProperties(), x, y, z, true);
			}
			
			Block block = BlockRegistry.getBlock(blockId);
			int stateId = getIdForName(block.getName(), block.getDataVersion());
			BlockState state = getState(stateId);
			
			bakedState = state.getBakedBlockState(block.getProperties(), x, y, z, true);
			if(!state.needsConnectionInfo())
				bakedBlockStates.set(blockId, bakedState);
			needsConnectionInfo.set(blockId, state.needsConnectionInfo());
			return bakedState;
		}
	}
	
	public static void clearBlockStateRegistry() {
		synchronized(mutex) {
			registeredStates.clear();
			nameToId.clear();
			counter = 0;
		}
		synchronized(mutex2) {
			bakedBlockStates.clear();
			needsConnectionInfo.clear();
			BakedBlockState.BAKED_WATER_STATE = new BakedBlockStateLiquid("minecraft:water");
		}
		synchronized(missingBlockStates) {
			missingBlockStates.clear();
		}
	}
	
}
