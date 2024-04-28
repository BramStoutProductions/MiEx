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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.MapCreator;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Int;
import nl.bramstout.mcworldexporter.nbt.TAG_String;

public class EntityItemFrame extends EntityHangable{

	private String itemName;
	private float itemRotation;
	private int mapId;
	
	public EntityItemFrame(String name, TAG_Compound properties) {
		super(name, properties);
		
		itemName = null;
		itemRotation = 0f;
		mapId = -1;

		NBT_Tag itemTag = properties.getElement("Item");
		if (itemTag != null) {
			itemName = ((TAG_String) ((TAG_Compound) itemTag).getElement("id")).value;
			if(!itemName.contains(":"))
				itemName = "minecraft:" + itemName;
			
			if(itemName.equals("minecraft:filled_map")) {
				TAG_Compound mapTag = (TAG_Compound) ((TAG_Compound) itemTag).getElement("tag");
				if(mapTag != null)
					mapId = ((TAG_Int) mapTag.getElement("map")).value;
			}
		}

		NBT_Tag rotationTag = properties.getElement("ItemRotation");
		if (rotationTag != null) {
			itemRotation = ((TAG_Byte) rotationTag).value * 45f;
			
			if(mapId >= 0)
				itemRotation *= 2f;
		}
	}

	@Override
	public List<Model> getModels() {
		List<Model> models = new ArrayList<Model>();
		
		Model model = new Model("item_frame", null, false);
		model.addTexture("back", "minecraft:block/birch_planks");
		model.addTexture("front", "minecraft:block/" + name.replace("minecraft:", ""));

		float minX = 2;
		float minY = 2;
		float minZ = 0;
		float maxX = 14;
		float maxY = 14;
		float maxZ = 1;
		if(mapId >= 0) {
			minX = 0;
			minY = 0;
			minZ = 0;
			maxX = 16;
			maxY = 16;
			maxZ = 1;
		}

		// Front Item Frame
		model.addFace(new float[] { minX + 1, minY + 1, minZ, maxX - 1, maxY - 1, maxZ - 0.5f },
						new float[] { 1, 1, 15, 15 }, Direction.SOUTH, "#front");

		// Back Item Frame
		model.addFace(new float[] { minX + 1, minY + 1, minZ, maxX - 1, maxY - 1, maxZ - 0.5f },
						new float[] { 1, 1, 15, 15 }, Direction.NORTH, "#front");

		// Left edge
		addBar(minX, minY, minZ, minX + 1, maxY, maxZ, model);
		// Right edge
		addBar(maxX - 1, minY, minZ, maxX, maxY, maxZ, model);
		// Top edge
		addBar(minX + 1, maxY - 1, minZ, maxX - 1, maxY, maxZ, model);
		// Bottom edge
		addBar(minX + 1, minY, minZ, maxX - 1, minY + 1, maxZ, model);

		model.rotate(facing.rotX, facing.rotY, false);
		
		models.add(model);
		
		if (itemName != null) {
			int modelId = ModelRegistry.getIdForItemName(itemName);
			Model itemModel = ModelRegistry.getModel(modelId);
			if(mapId >= 0)
				itemModel = MapCreator.createMapModel(mapId);
				
			if (itemModel != null) {
				itemModel = new Model(itemModel);
				
				float scale = 0.5f;
				if(itemModel.getExtraData() == null || !itemModel.getExtraData().equals("{\"item\":\"true\"}"))
					scale = 0.25f;
				if(mapId >= 0)
					scale = 1.0f;

				itemModel.scale(scale);
				
				if((itemModel.getExtraData() == null || !itemModel.getExtraData().equals("{\"item\":\"true\"}")) && mapId < 0)
					itemModel.rotate(0, 180, false);
				if(itemRotation != 0f)
					itemModel.rotate(0, 0, -itemRotation);
				itemModel.translate(0, 0, -7);
				
				itemModel.rotate(facing.rotX, facing.rotY, false);
				
				models.add(itemModel);

			}
		}
		
		return models;
	}
	
	private void addBar(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Model model) {
		float[] minMaxPoints = new float[] { minX, minY, minZ, maxX, maxY, maxZ };
		// Front
		model.addFace(minMaxPoints, new float[] { minX, minY, maxX, maxY }, Direction.SOUTH, "#back");

		// Back
		model.addFace(minMaxPoints, new float[] { minX, minY, maxX, maxY }, Direction.NORTH, "#back");
		
		// Top
		model.addFace(minMaxPoints, new float[] { minX, minZ, maxX, maxZ }, Direction.UP, "#back");
		
		// Bottom
		model.addFace(minMaxPoints, new float[] { minX, minZ, maxX, maxZ }, Direction.DOWN, "#back");
		
		// Left
		model.addFace(minMaxPoints, new float[] { minZ, minY, maxZ, maxY }, Direction.WEST, "#back");
		
		// Right
		model.addFace(minMaxPoints, new float[] { minZ, minY, maxZ, maxY }, Direction.EAST, "#back");
	}

}
