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
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateChest extends BlockState{

	public BlockStateChest(String name, int dataVersion) {
		super(name, dataVersion, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/chest/normal";
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
		Model model = new Model("chest", null, false);
		float rotY = 0f;
		String val = null;
		NbtTag facing = properties.get("facing");
		if(facing != null)
			val = facing.asString();
		if (val == null)
			val = "north";
		if (val.equals("north")) {
			rotY = 0f;
		} else if (val.equals("east")) {
			rotY = 90f;
		} else if (val.equals("south")) {
			rotY = 180f;
		} else if (val.equals("west")) {
			rotY = 270f;
		}
		
		NbtTag typeTag = properties.get("type");
		String type = "";
		if(typeTag != null)
			type = "_" + typeTag.asString();
		if(type.equals("_single"))
			type = "";
		
		String tex_basename = "normal";
		if(name.contains("trapped"))
			tex_basename = "trapped";
		if(name.contains("ender_chest"))
			tex_basename = "ender";
		
		String tex = "minecraft:entity/chest/" + tex_basename + type;
		model.addTexture("#texture", tex);
		
		if(type.equals("")) {
			// Bottom
			model.addEntityCube(new float[] {1f, 10f, 1f, 15f, 0f, 15f}, new float[] {0f, 4.75f, 14f, 10.75f}, "#texture");
			// Top
			model.addEntityCube(new float[] {1f, 14f, 1f, 15f, 9f, 15f}, new float[] {0f, 0f, 14f, 4.75f}, "#texture");
			// Lock
			model.addEntityCube(new float[] {7f, 8f, 0f, 9f, 12f, 1f}, new float[] {0f, 0f, 1.5f, 1.25f}, "#texture");
		} else if(type.equals("_left")) {
			// Bottom
			model.addEntityCube(new float[] {0f, 10f, 1f, 15f, 0f, 15f}, new float[] {0f, 4.75f, 14.5f, 10.75f}, "#texture",
					Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST);
			// Top
			model.addEntityCube(new float[] {0f, 14f, 1f, 15f, 9f, 15f}, new float[] {0f, 0f, 14.5f, 4.75f}, "#texture",
					Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST);
			// Lock
			model.addEntityCube(new float[] {0f, 8f, 0f, 1f, 12f, 1f}, new float[] {0f, 0f, 1.0f, 1.25f}, "#texture",
					Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST);
			model.flip(true, false, false);
		} else if(type.equals("_right")) {
			// Bottom
			model.addEntityCube(new float[] {1f, 10f, 1f, 16f, 0f, 15f}, new float[] {0f, 4.75f, 14.5f, 10.75f}, "#texture",
					Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST);
			// Top
			model.addEntityCube(new float[] {1f, 14f, 1f, 16f, 9f, 15f}, new float[] {0f, 0f, 14.5f, 4.75f}, "#texture",
					Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST);
			// Lock
			model.addEntityCube(new float[] {15f, 8f, 0f, 16f, 12f, 1f}, new float[] {0f, 0f, 1.0f, 1.25f}, "#texture",
					Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST);
			model.flip(true, false, false);
		}
		model.rotate(0, rotY, false);
		list.add(model);
		models.add(list);
		
		BakedBlockState res = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, true, false, false, true, 1, null,
				needsConnectionInfo());
		
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // free the copy that we made.
		}
		
		return res;
	}

}
