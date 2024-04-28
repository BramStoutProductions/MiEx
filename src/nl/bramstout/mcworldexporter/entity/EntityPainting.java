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

package nl.bramstout.mcworldexporter.entity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_String;

public class EntityPainting extends EntityHangable{

	String motive;
	
	public EntityPainting(String name, TAG_Compound properties) {
		super(name, properties);
		
		NBT_Tag motiveTag = properties.getElement("Motive");
		if(motiveTag == null)
			motiveTag = properties.getElement("variant");
		motive = "back";
		if(motiveTag != null)
			motive = ((TAG_String) motiveTag).value;
		if(!motive.contains(":"))
			motive = "minecraft:" + motive;
		
		NBT_Tag facingTag = properties.getElement("Facing");
		if(facingTag == null)
			facingTag = properties.getElement("facing");
		byte facingByte = 0;
		if(facingTag != null)
			facingByte = ((TAG_Byte) facingTag).value;
		if(facingByte == 4)
			facing = Direction.DOWN;
		else if(facingByte == 5)
			facing = Direction.UP;
		else if(facingByte == 2)
			facing = Direction.NORTH;
		else if(facingByte == 0)
			facing = Direction.SOUTH;
		else if(facingByte == 1)
			facing = Direction.WEST;
		else if(facingByte == 3)
			facing = Direction.EAST;
	}

	@Override
	public List<Model> getModels() {
		Model model = new Model("painting", null, false);
		model.addTexture("back", "minecraft:painting/back");
		model.addTexture("front", "minecraft:painting/" + motive.replace("minecraft:", ""));
		
		Size size = new Size(paintingSizes.getOrDefault(motive, new Size(1,1)));
		float sizeX = size.x;
		float sizeY = size.y;
		float sizeZ = 1f;
		float offsetX = getOffset((int) sizeX);
		float offsetY = getOffset((int) sizeY);
		float offsetZ = 0f;
		sizeX *= 16f;
		sizeY *= 16f;
		offsetX *= 16f;
		offsetY *= 16f;
		
		float[] minMaxPoints = new float[] {offsetX, offsetY, offsetZ, offsetX + sizeX, offsetY + sizeY, offsetZ + sizeZ};
		
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.SOUTH, "#front");

		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.NORTH, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.UP, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.DOWN, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.EAST, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.WEST, "#back");
		
		model.rotate(facing.rotX, facing.rotY, false);
		
		return Arrays.asList(model);
	}
	
	private int getOffset(int width) {
		if(width >= 3)
			return 3 - width;
		return 0;
	}
	
	private static class Size{
		public float x;
		public float y;
		
		public Size(float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		public Size(Size other) {
			this.x = other.x;
			this.y = other.y;
		}
	}
	
	private static HashMap<String, Size> paintingSizes = new HashMap<String, Size>();
	static {
		paintingSizes.put("minecraft:alban", new Size(1,1));
		paintingSizes.put("minecraft:aztec", new Size(1,1));
		paintingSizes.put("minecraft:aztec2", new Size(1,1));
		paintingSizes.put("minecraft:bomb", new Size(1,1));
		paintingSizes.put("minecraft:kebab", new Size(1,1));
		paintingSizes.put("minecraft:plant", new Size(1,1));
		paintingSizes.put("minecraft:wasteland", new Size(1,1));
		paintingSizes.put("minecraft:courbet", new Size(2,1));
		paintingSizes.put("minecraft:pool", new Size(2,1));
		paintingSizes.put("minecraft:sea", new Size(2,1));
		paintingSizes.put("minecraft:creebet", new Size(2,1));
		paintingSizes.put("minecraft:sunset", new Size(2,1));
		paintingSizes.put("minecraft:graham", new Size(1,2));
		paintingSizes.put("minecraft:wanderer", new Size(1,2));
		paintingSizes.put("minecraft:bust", new Size(2,2));
		paintingSizes.put("minecraft:match", new Size(2,2));
		paintingSizes.put("minecraft:skull_and_roses", new Size(2,2));
		paintingSizes.put("minecraft:stage", new Size(2,2));
		paintingSizes.put("minecraft:void", new Size(2,2));
		paintingSizes.put("minecraft:wither", new Size(2,2));
		paintingSizes.put("minecraft:fighters", new Size(4,2));
		paintingSizes.put("minecraft:donkey_kong", new Size(4,3));
		paintingSizes.put("minecraft:skeleton", new Size(4,3));
		paintingSizes.put("minecraft:burning_skull", new Size(4,4));
		paintingSizes.put("minecraft:pigscene", new Size(4,4));
		paintingSizes.put("minecraft:pointer", new Size(4,4));
	}

}
