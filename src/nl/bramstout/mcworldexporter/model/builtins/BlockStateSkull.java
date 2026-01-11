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

package nl.bramstout.mcworldexporter.model.builtins;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateSkull extends BlockState{

	public BlockStateSkull(String name, int dataVersion) {
		super(name, dataVersion, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/player/player";
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, boolean runBlockConnections) {
		if(blockConnections != null && runBlockConnections) {
			properties = (NbtTagCompound) properties.copy();
			String newName = blockConnections.map(name, properties, x, y, z);
			if(newName != null && !newName.equals(name)) {
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
				properties.free();
				return BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, runBlockConnections);
			}
		}
		
		List<List<Model>> models = new ArrayList<List<Model>>();
		
		List<Model> list = new ArrayList<Model>();
		Model model = new Model("skull", null, false);
		list.add(model);
		models.add(list);
		
		boolean isWall = name.contains("_wall_");

		float rotY = 0f;
		if (isWall) {
			String val = properties.get("facing").asString();
			if (val == null)
				val = "north";
			if (val.equals("north")) {
				rotY = 180f;
			} else if (val.equals("east")) {
				rotY = 270f;
			} else if (val.equals("south")) {
				rotY = 0f;
			} else if (val.equals("west")) {
				rotY = 90f;
			}
		} else {
			String val = properties.get("rotation").asString();
			if (val == null)
				val = "0";
			int ival = 0;
			try {
				ival = Integer.parseInt(val);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			rotY = (((float) ival) / 16f) * 360f + 180f;
			if(rotY >= 360f)
				rotY -= 360f;
		}

		String texture = "minecraft:entity/player/player";
		if (name.contains("wither_skeleton"))
			texture = "minecraft:entity/skeleton/wither_skeleton";
		else if (name.contains("skeleton"))
			texture = "minecraft:entity/skeleton/skeleton";
		else if (name.contains("zombie"))
			texture = "minecraft:entity/zombie/zombie";
		else if (name.contains("player"))
			texture = "minecraft:entity/player/player";
		else if (name.contains("creeper"))
			texture = "minecraft:entity/creeper/creeper";
		else if (name.contains("dragon"))
			texture = "minecraft:entity/enderdragon/dragon";

		if(name.contains("player")) {
			// Check for skin
			NbtTag tag = properties.get("ExtraType");
			
			if(tag != null && tag.getId() == 8) {
				texture = "minecraft:entity/player/" + tag.asString();
			} else {
				tag = properties.get("SkullOwner");
				
				if(tag != null && tag.getId() == 10) {
					NbtTag tag2 = ((NbtTagCompound) tag).get("Name");
					
					if(tag2 != null && tag2.getId() == 8) {
						texture = "minecraft:entity/player/" + tag2.asString();
					}
				}
			}
		}
		
		model.addTexture("#texture", texture);
		
		if (name.contains("dragon")) {
			float scale = 12f / 16f;
			float offsetX = 8f;
			float offsetY = isWall ? 4f : 0f;
			float offsetZ = isWall ? 6f : 9.5f;

			// Main head
			addCube(model, -8f, 0f, -8f, 8f, 16f, 8f, 7f, 12.125f, 11f, 14.125f, scale,
					offsetX, offsetY, offsetZ, 0f, 0f);
			// Mouth top
			addCube(model, -6f, 4f, 6f, 6f, 9f, 22f, 11f, 11.9375f, 14.5f, 13.25f, scale,
					offsetX, offsetY, offsetZ, 0f, 0f);
			// Mouth bottom
			addCube(model, -6f, -1.5f, 5.5f, 6f, 2.5f, 21.5f, 11f, 10.6875f,
					14.5f, 11.9375f, scale, offsetX, offsetY, offsetZ, -10f, 0f);
			// Nose left
			addCube(model, 3f, 9f, 18f, 5f, 11f, 22f, 7f, 15.625f, 7.75f, 16f, scale,
					offsetX, offsetY, offsetZ, 0f, 0f);
			// Nose right
			addCube(model, -5f, 9f, 18f, -3f, 11f, 22f, 7f, 15.625f, 7.75f, 16f, scale,
					offsetX, offsetY, offsetZ, 0f, 180f);
			// Ear left
			addCube(model, 3f, 16f, -4f, 5f, 20f, 2f, 0f, 15.375f, 1f, 16f, scale,
					offsetX, offsetY, offsetZ, 0f, 0f);
			// Ear left
			addCube(model, -5f, 16f, -4f, -3f, 20f, 2f, 3f, 15.375f, 4f, 16f, scale,
					offsetX, offsetY, offsetZ, 0f, 0f);
		} else {
			if (name.contains("wither_skeleton") || name.contains("skeleton")
					|| name.contains("creeper")) {
				if (isWall)
					model.addEntityCube(new float[] { 4f, 4f, 0f, 12f, 12f, 8f }, new float[] { 0f, 0f, 8f, 8f }, "#texture");
				else
					model.addEntityCube(new float[] { 4f, 0f, 4f, 12f, 8f, 12f }, new float[] { 0f, 0f, 8f, 8f }, "#texture");
			} else {
				if (isWall)
					model.addEntityCube(new float[] { 4f, 4f, 0f, 12f, 12f, 8f }, new float[] { 0f, 0f, 8f, 4f }, "#texture");
				else
					model.addEntityCube(new float[] { 4f, 0f, 4f, 12f, 8f, 12f }, new float[] { 0f, 0f, 8f, 4f }, "#texture");
				
				if(name.contains("player")) {
					// Helmet part of the skin
					if (isWall)
						model.addEntityCube(new float[] { 3.75f, 3.75f, -0.25f, 12.25f, 12.25f, 8.25f }, 
								new float[] { 8f, 0f, 8f, 4f }, "#texture");
					else
						model.addEntityCube(new float[] { 3.75f, -0.25f, 3.75f, 12.25f, 8.25f, 12.25f }, 
								new float[] { 8f, 0f, 16f, 4f }, "#texture");
				}
			}
		}
		
		model.rotate(0, rotY, false);
		
		BakedBlockState bakedState = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, true, 0, null,
				needsConnectionInfo());
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}
	
	private void addCube(Model model, float pMinX, float pMinY, float pMinZ, float pMaxX, float pMaxY, float pMaxZ, 
			float uvMinU, float uvMinV, float uvMaxU, float uvMaxV, float scale, float offsetX, float offsetY, float offsetZ, 
			float rotX, float rotY) {
		pMinX = pMinX * scale + offsetX;
		pMinY = pMinY * scale + offsetY;
		pMinZ = pMinZ * scale + offsetZ;
		pMaxX = pMaxX * scale + offsetX;
		pMaxY = pMaxY * scale + offsetY;
		pMaxZ = pMaxZ * scale + offsetZ;
		model.addEntityCube(new float[] { pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ }, 
				new float[] { uvMinU, 16f - uvMaxV, uvMaxU, 16f - uvMinV }, "#texture", rotX, rotY);
	}

}
