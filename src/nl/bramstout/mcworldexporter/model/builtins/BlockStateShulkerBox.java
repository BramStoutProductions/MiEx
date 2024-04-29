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

import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;

public class BlockStateShulkerBox extends BlockState{

	public BlockStateShulkerBox(String name) {
		super(name, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/shulker/shulker";
	}
	
	@Override
	public BakedBlockState getBakedBlockState(TAG_Compound properties, int x, int y, int z) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		
		List<Model> list = new ArrayList<Model>();
		Model model = new Model("shulker_box", null, false);
		list.add(model);
		models.add(list);
		
		float rotX = 0f;
		float rotY = 0f;
		String val = properties.getElement("facing").asString();
		if (val == null)
			val = "up";
		if (val.equals("down")) {
			rotX = 180f;
		} else if (val.equals("north")) {
			rotX = 90f;
			rotY = 0f;
		} else if (val.equals("east")) {
			rotX = 90f;
			rotY = 90f;
		} else if (val.equals("south")) {
			rotX = 90f;
			rotY = 180f;
		} else if (val.equals("west")) {
			rotX = 90f;
			rotY = -90f;
		}
		
		String color = null;
		String colorTokens[] = name.split("_");
		if(colorTokens.length == 3) {
			color = colorTokens[0];
		} else if(colorTokens.length == 4) {
			color = colorTokens[0] + "_" + colorTokens[1];
		}
		
		model.addTexture("texture", "minecraft:entity/shulker/" + (color == null ? "shulker" : "shulker_" + color));
		
		// Bottom
		model.addEntityCube(new float[] { 0f, 0f, 0f, 16f, 8f, 16f } , new float[] { 0f, 7f, 16f, 13f }, "#texture",
				Direction.DOWN, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST);
		// Top
		// Making the top cube a tiny bit smaller, since they overlap and this helps prevent render bugs
		model.addEntityCube(new float[] { 0.01f, 4f, 0.01f, 15.99f, 16f, 15.99f } , new float[] { 0f, 0f, 16f, 7f } , "#texture");
		
		model.rotate(rotX, rotY, false);
		
		return new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, false, false, false, true, 1, -1);
	}

}
