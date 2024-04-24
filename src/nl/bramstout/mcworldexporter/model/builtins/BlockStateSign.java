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
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_String;

public class BlockStateSign extends BlockState{

	public BlockStateSign(String name) {
		super(name, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:block/sign";
	}
	
	public BakedBlockState getBakedBlockState(TAG_Compound properties) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		
		List<Model> list = new ArrayList<Model>();
		Model model = new Model("sign", null, false);
		list.add(model);
		models.add(list);
		
		boolean isWall = name.contains("wall");
		float rotY = 0f;
		if (isWall) {
			String val = properties.getElement("facing").asString();
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
			String val = properties.getElement("rotation").asString();
			if (val == null)
				val = "0";
			int ival = 0;
			try {
				ival = Integer.parseInt(val);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			rotY = (((float) ival) / 16f) * 360f;
		}
		
		model.addTexture("texture", "minecraft:block/" + name.replace("_wall_", "_"));
		//model.addTexture("#font", "minecraft:font/ascii");
		
		float scale = 16f / 24f;
		float offsetX = 8f;
		float offsetY = isWall ? 8.3333f : 0f;
		float offsetZ = isWall ? 1f : 8f;

		// sign part
		float minX = -12f * scale + offsetX;
		float minY = (isWall ? -6f : 14f) * scale + offsetY;
		float minZ = -1f * scale + offsetZ;
		float maxX = 12f * scale + offsetX;
		float maxY = (isWall ? 6f : 26f) * scale + offsetY;
		float maxZ = 1f * scale + offsetZ;

		model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 0f, 13f, 7f }, "#texture");

		// Post part
		if (!isWall) {
			minX = -1f * scale + offsetX;
			minY = 0f * scale + offsetY;
			minZ = -1f * scale + offsetZ;
			maxX = 1f * scale + offsetX;
			maxY = 14f * scale + offsetY;
			maxZ = 1f * scale + offsetZ;
			
			model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 7f, 2f, 15f }, "#texture");
		}
		
		model.rotate(0, rotY, false);
		
		String line1 = "";
		String line2 = "";
		String line3 = "";
		String line4 = "";
		
		NBT_Tag text1 = properties.getElement("Text1");
		NBT_Tag text2 = properties.getElement("Text2");
		NBT_Tag text3 = properties.getElement("Text3");
		NBT_Tag text4 = properties.getElement("Text4");
		
		if(text1 != null)
			line1 = ((TAG_String) text1).value;
		if(text2 != null)
			line2 = ((TAG_String) text2).value;
		if(text3 != null)
			line3 = ((TAG_String) text3).value;
		if(text4 != null)
			line4 = ((TAG_String) text4).value;
		
		String text = line1 + "\n" + line2 + "\n" + line3 + "\n" + line4;
		model.setExtraData("{ text: \"" + text + "\" }");
		
		return new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, false, false, false, true, 0, -1);
	}

}
