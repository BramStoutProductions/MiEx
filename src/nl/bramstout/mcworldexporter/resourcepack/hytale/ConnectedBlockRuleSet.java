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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public abstract class ConnectedBlockRuleSet {
	
	protected String type;
	protected String materialName;
	
	public ConnectedBlockRuleSet(String type) {
		this.type = type;
		materialName = "";
	}
	
	public abstract String getVariant(NbtTagCompound properties, int x, int y, int z);
	
	protected Block getOtherBlock(int x, int y, int z) {
		int otherBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
		Block otherBlock = BlockRegistry.getBlock(otherBlockId);
		int otherBlockStateId = BlockStateRegistry.getIdForName(otherBlock.getName(), otherBlock.getDataVersion());
		BlockState otherBlockState = BlockStateRegistry.getState(otherBlockStateId);
		BlockStateHandler otherHandlerTmp = otherBlockState.getHandler();
		if(otherHandlerTmp == null || !(otherHandlerTmp instanceof BlockStateHandlerHytale))
			return null;
		BlockStateHandlerHytale otherHandler = (BlockStateHandlerHytale) otherHandlerTmp;
		
		ConnectedBlockRuleSet otherCBRS = otherHandler.getConnectedBlockRuleSet();
		if(otherCBRS == null)
			return null;
		
		if(!otherCBRS.materialName.equals(materialName) || !otherCBRS.type.equals(type))
			return null;
		
		// Both blocks have a matching connected block rule set.
		return otherBlock;
	}
	
	protected String getVariant(JsonObject data, String name) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonObject()) {
				JsonObject el2 = el.getAsJsonObject();
				if(el2.has("State")) {
					JsonElement el3 = el2.get("State");
					if(el3.isJsonPrimitive()) {
						return el3.getAsString();
					}
				}
			}else if(el.isJsonPrimitive()) {
				return el.getAsString();
			}
		}
		return null;
	}
	
	public static ConnectedBlockRuleSet parse(JsonObject data) {
		String type = "";
		if(data.has("Type"))
			type = data.get("Type").getAsString();
		
		if(type.equals("Roof")) {
			return new ConnectedBlockRuleSetRoof(data);
		}else if(type.equals("Stair")) {
			return new ConnectedBlockRuleSetStair(data);
		}
		
		return null;
	}
	
	public static class ConnectedBlockRuleSetRoof extends ConnectedBlockRuleSet{
		
		private ConnectedBlockRuleSetStair regular;
		private ConnectedBlockRuleSetStair hollow;
		private String topperVariant;
		private int size;
		
		public ConnectedBlockRuleSetRoof(JsonObject data) {
			super("Roof");
			regular = null;
			hollow = null;
			topperVariant = null;
			
			if(data.has("Regular")) {
				JsonElement regularEl = data.get("Regular");
				if(regularEl.isJsonObject()) {
					regular = new ConnectedBlockRuleSetStair(regularEl.getAsJsonObject());
					regular.type = type;
				}
			}
			if(data.has("Hollow")) {
				JsonElement hollowEl = data.get("Hollow");
				if(hollowEl.isJsonObject()) {
					hollow = new ConnectedBlockRuleSetStair(hollowEl.getAsJsonObject());
					hollow.type = type;
				}
			}
			if(data.has("Topper"))
				topperVariant = getVariant(data, "Topper");
			
			size = 1;
			if(data.has("Width"))
				size = data.get("Width").getAsInt();
			if(regular != null)
				regular.size = size;
			if(hollow != null)
				hollow.size = size;
		}
		
		@Override
		public String getVariant(NbtTagCompound properties, int x, int y, int z) {
			if(regular != null) {
				String variant = regular.getVariant(properties, x, y, z);
				if(variant != null)
					return variant;
			}
			if(hollow != null) {
				String variant = hollow.getVariant(properties, x, y, z);
				return variant;
			}
			if(topperVariant != null) {
				if(topperCheck(x, y, z, Direction.NORTH) && 
						topperCheck(x, y, z, Direction.SOUTH) &&
						topperCheck(x, y, z, Direction.EAST) &&
						topperCheck(x, y, z, Direction.WEST)) {
					return topperVariant;
				}
			}
			
			return null;
		}
		
		private boolean topperCheck(int x, int y, int z, Direction dir) {
			Block otherBlock = getOtherBlock(
					x + dir.x * size, y - 1, z + dir.z * size);
			if(otherBlock == null)
				return false;
			
			Matrix otherRotMatrix = BlockStateVariant.getRotationMatrix(otherBlock.getProperties());
			Direction otherForwardDir = Direction.NORTH.transform(otherRotMatrix);
			
			return otherForwardDir == dir.getOpposite();
		}
		
	}
	
	public static class ConnectedBlockRuleSetStair extends ConnectedBlockRuleSet{
		
		@SuppressWarnings("unused")
		private String straightVariant;
		private String cornerRightVariant;
		private String cornerLeftVariant;
		private String invertedCornerRightVariant;
		private String invertedCornerLeftVariant;
		protected int size;
		
		public ConnectedBlockRuleSetStair(JsonObject data) {
			super("Stair");
			if(data.has("Straight"))
				straightVariant = getVariant(data, "Straight");
			if(data.has("Corner_Right"))
				cornerRightVariant = getVariant(data, "Corner_Right");
			if(data.has("Corner_Left"))
				cornerLeftVariant = getVariant(data, "Corner_Left");
			if(data.has("Inverted_Corner_Right"))
				invertedCornerRightVariant = getVariant(data, "Inverted_Corner_Right");
			if(data.has("Inverted_Corner_Left"))
				invertedCornerLeftVariant = getVariant(data, "Inverted_Corner_Left");
			if(data.has("MaterialName"))
				materialName = data.get("MaterialName").getAsString();
			size = 1;
			if(data.has("Width"))
				size = data.get("Width").getAsInt();
		}
		
		@Override
		public String getVariant(NbtTagCompound properties, int x, int y, int z) {
			Matrix rotMatrix = BlockStateVariant.getRotationMatrix(properties);
			Direction forwardDir = Direction.NORTH.transform(rotMatrix);
			Direction rightDir = forwardDir.getRight();
			Direction leftDir = rightDir.getOpposite();
			Direction backwardDir = forwardDir.getOpposite();
			
			// First check block behind it
			Block otherBlock = getOtherBlock(
					x + forwardDir.x * size, y + forwardDir.y * size, z + forwardDir.z * size);
			if(otherBlock != null) {
				// Both blocks have a matching connected block rule set.
				// Now figure out the direction.
				Matrix otherRotMatrix = BlockStateVariant.getRotationMatrix(otherBlock.getProperties());
				Direction otherForwardDir = Direction.NORTH.transform(otherRotMatrix);
				if(otherForwardDir == leftDir)
					return cornerLeftVariant;
				if(otherForwardDir == rightDir)
					return cornerRightVariant;
			}
			
			// Now check block to infront for the inverted corners.
			otherBlock = getOtherBlock(
					x + backwardDir.x * size, y + backwardDir.y * size, z + backwardDir.z * size);
			if(otherBlock != null) {
				Matrix otherRotMatrix = BlockStateVariant.getRotationMatrix(otherBlock.getProperties());
				Direction otherForwardDir = Direction.NORTH.transform(otherRotMatrix);
				if(otherForwardDir == leftDir)
					return invertedCornerLeftVariant;
				if(otherForwardDir == rightDir)
					return invertedCornerRightVariant;
			}
			return null;
		}
		
	}

}
