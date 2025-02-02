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
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateBed extends BlockState{

	public BlockStateBed(String name, int dataVersion) {
		super(name, dataVersion, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/bed/red";
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
		Model model = new Model("bed", null, false);
		list.add(model);
		models.add(list);
		
		float rotY = 0f;
		String val = properties.get("facing").asString();
		if (val == null)
			val = "up";
		if (val.equals("north")) {
			rotY = 0f;
		} else if (val.equals("east")) {
			rotY = 90f;
		} else if (val.equals("south")) {
			rotY = 180f;
		} else if (val.equals("west")) {
			rotY = 270f;
		}
		
		String partStr = properties.get("part").asString();
		if (partStr == null)
			partStr = "foot";
		
		String color = "red";
		String colorTokens[] = name.split("_");
		if(colorTokens.length == 2) {
			color = colorTokens[0];
		} else if(colorTokens.length == 3) {
			color = colorTokens[0] + "_" + colorTokens[1];
		}
		color = color.split(":")[1];
		
		model.addTexture("#texture", "minecraft:entity/bed/" + color);
		
		if (partStr.equals("foot")) {
			// Foot
			model.addEntityCube(new float[] { 0f, 0f, 3f, 16f, 16f, 9f }, new float[] { 0f, 5.5f, 11f, 11f }, "#texture", 90f, 0f);
			
			// Feet
			model.addEntityCube(new float[] { 0f, 0f, 13f, 3f, 3f, 16f }, new float[] { 12.5f, 0f, 15.5f, 1.5f }, "#texture");
			model.addEntityCube(new float[] { 0f, 0f, 13f, 3f, 3f, 16f }, new float[] { 12.5f, 1.5f, 15.5f, 3.0f }, "#texture", 0f, 270f);
		} else if (partStr.equals("head")) {
			// Head
			model.addEntityCube(new float[] { 0f, 0f, 3f, 16f, 16f, 9f }, new float[] { 0f, 0f, 11f, 5.5f }, "#texture", 90f, 0f);
			
			// Feet
			model.addEntityCube(new float[] { 0f, 0f, 13f, 3f, 3f, 16f } , new float[] { 12.5f, 3f, 15.5f, 4.5f } , "#texture", 0f, 90f);
			model.addEntityCube(new float[] { 0f, 0f, 13f, 3f, 3f, 16f }, new float[] { 12.5f, 4.5f, 15.5f, 6f } , "#texture", 0f, 180f);
		}
		
		model.rotate(0, rotY, false);
		
		BakedBlockState bakedState = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, false, false, false, true, 1, null,
				needsConnectionInfo());
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}

}
