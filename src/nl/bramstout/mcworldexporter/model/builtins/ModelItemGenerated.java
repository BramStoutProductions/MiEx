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

import java.awt.image.BufferedImage;
import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ModelItemGenerated extends Model{
	
	public ModelItemGenerated(String name) {
		super(name, null, false);
		this.id = ModelRegistry.getNextId(this);
		
		extraData = "{\"item\":\"true\"}";
		itemFrameScale = new Vector3f(1f, 1f, 1f);
	}
	
	@Override
	public Model postConstruct(Model topLevelModel) {
		Model model = new Model(this);
		model.getTextures().putAll(topLevelModel.getTextures());
		generateModel(model);
		return model;
	}
	
	private void generateModel(Model model) {
		String texture = model.getTexture("#layer0");
		if(texture.isEmpty()) {
			if(model.getTextures().isEmpty())
				return;
			texture = model.getTextures().values().iterator().next();
			if(texture.startsWith("#"))
				texture = getTexture(texture);
		}
		
		File texFile = ResourcePacks.getTexture(texture);
		if(!texFile.exists())
			return;
		
		BufferedImage image = ImageReader.readImage(texFile);
		if(image == null)
			return;

		float voxelWidth = 16f / ((float) image.getWidth());
		float voxelHeight = 16f / ((float) image.getHeight());
		
		if(!image.getColorModel().hasAlpha()) {
			// No alpha, so make a very simple model
			float[] minMaxPoints = new float[] { 0, 0, 7.5f, 16, 16, 8.5f };
			JsonObject faceData = new JsonObject();
			faceData.addProperty("texture", "#layer0");
			JsonArray uvData = new JsonArray();
			uvData.add(0);
			uvData.add(0);
			uvData.add(16);
			uvData.add(16);
			faceData.add("uv", uvData);
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.SOUTH, faceData, false));
			uvData.set(0, new JsonPrimitive(16));
			uvData.set(2, new JsonPrimitive(0));
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.NORTH, faceData, false));
			uvData.set(0, new JsonPrimitive(0));
			uvData.set(2, new JsonPrimitive(voxelWidth));
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.WEST, faceData, false));
			uvData.set(0, new JsonPrimitive(16f - voxelWidth));
			uvData.set(2, new JsonPrimitive(16));
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.EAST, faceData, false));
			uvData.set(0, new JsonPrimitive(0));
			uvData.set(1, new JsonPrimitive(16f - voxelHeight));
			uvData.set(3, new JsonPrimitive(16));
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.UP, faceData, false));
			uvData.set(1, new JsonPrimitive(0));
			uvData.set(3, new JsonPrimitive(voxelHeight));
			model.getFaces().add(new ModelFace(minMaxPoints, Direction.DOWN, faceData, false));
		}
		
		float[] minMaxPoints = new float[] { 0, 0, 7.5f, 16, 16, 8.5f };
		JsonObject faceData = new JsonObject();
		faceData.addProperty("texture", "#layer0");
		JsonArray uvData = new JsonArray();
		uvData.add(0);
		uvData.add(0);
		uvData.add(16);
		uvData.add(16);
		faceData.add("uv", uvData);
		for(int j = 0; j < image.getHeight(); ++j) {
			for(int i = 0; i < image.getWidth(); ++i) {
				float x = i * voxelWidth;
				float y = (image.getHeight() - j - 1) * voxelHeight;
				float u = x;
				float v = j * voxelHeight;
				
				int currentA = image.getRGB(i, j) >>> 24;
				if(currentA <= 0)
					continue;
				
				minMaxPoints[0] = x;
				minMaxPoints[1] = y;
				minMaxPoints[3] = x + voxelWidth;
				minMaxPoints[4] = y + voxelHeight;
				uvData.set(0, new JsonPrimitive(u));
				uvData.set(1, new JsonPrimitive(v));
				uvData.set(2, new JsonPrimitive(u + voxelWidth));
				uvData.set(3, new JsonPrimitive(v + voxelHeight));
				
				boolean west = i == 0 || ((image.getRGB(i - 1, j) >>> 24) <= 0);
				boolean east = i == (image.getWidth() - 1) || ((image.getRGB(i + 1, j) >>> 24) <= 0);
				boolean up = j == 0 || ((image.getRGB(i, j - 1) >>> 24) <= 0);
				boolean down = j == (image.getHeight() -1) || ((image.getRGB(i, j + 1) >>> 24) <= 0);
				
				model.getFaces().add(new ModelFace(minMaxPoints, Direction.SOUTH, faceData, false));
				model.getFaces().add(new ModelFace(minMaxPoints, Direction.NORTH, faceData, false));
				if(east)
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.EAST, faceData, false));
				if(west)
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.WEST, faceData, false));
				if(up)
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.UP, faceData, false));
				if(down)
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.DOWN, faceData, false));
			}
		}
	}

}
