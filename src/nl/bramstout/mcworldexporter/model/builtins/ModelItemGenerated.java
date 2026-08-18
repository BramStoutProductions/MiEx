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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelFace.FaceData;
import nl.bramstout.mcworldexporter.model.TextureRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ModelItemGenerated extends Model{
	
	public ModelItemGenerated(String name) {
		super(name, null, false, true);
		
		extraData = "{\"item\":\"true\"}";
	}

	@Override
	public Model postConstruct(Model topLevelModel) {
		//Model model = new Model(this);
		//model.getTextures().putAll(topLevelModel.getTextures());
		//model.getDisplayTransforms().putAll(topLevelModel.getDisplayTransforms());
		//generateModel(model);
		//return model;
		if(topLevelModel.getFaces().isEmpty())
			generateModel(topLevelModel);
		return this;
	}
	
	private static class LayeredItem{
		
		public int width;
		public int height;
		public List<ItemLayer> layers;
		private boolean[] mask;
		
		public LayeredItem(int width, int height, List<ItemLayer> layers) {
			this.width = width;
			this.height = height;
			this.layers = layers;
			mask = new boolean[width * height];
			for(int j = 0; j < height; ++j) {
				for(int i = 0; i < width; ++i) {
					mask[j * width + i] = false;
					for(ItemLayer layer : layers) {
						if(!layer.isTransparent(i, j, width, height)) {
							mask[j * width + i] = true;
							break;
						}
					}
				}
			}
		}
		
		public boolean sampleMask(int i, int j) {
			return mask[j * width + i];
		}
		
	}
	
	private static class ItemLayer{
		
		public String layerName;
		public int layer;
		public BufferedImage image;
		
		public ItemLayer(String layerName, int layer, BufferedImage image) {
			this.layerName = layerName;
			this.layer = layer;
			this.image = image;
		}
		
		public boolean isTransparent(int i, int j, int width, int height) {
			if(width != image.getWidth() || height != image.getHeight()) {
				// Resample i and j
				i = (int) ((((float) i) / ((float) width)) * ((float) image.getWidth()));
				j = (int) ((((float) j) / ((float) height)) * ((float) image.getHeight()));
			}
			return (image.getRGB(i, j) >>> 24) <= 0;
		}
		
	}
	
	private LayeredItem constructImage(Model model) {
		int maxWidth = 0;
		int maxHeight = 0;
		
		// Find all layers
		List<ItemLayer> layers = new ArrayList<ItemLayer>();
		for(int i = 0; i < model.getTextures().size(); ++i) {
			long layerNameId = model.getTextures().getKey(i);
			String layerName = TextureRegistry.getTextureFromId(layerNameId);
			if(!layerName.startsWith("#layer"))
				continue;
			int layerIndex = -1;
			try {
				layerIndex = Integer.parseInt(layerName.substring(6));
			}catch(Exception ex) {}
			if(layerIndex < 0)
				continue;
			String texPath = model.getTexture(layerNameId);
			File texFile = ResourcePacks.getTexture(texPath);
			if(texFile == null || !texFile.exists())
				continue;
			BufferedImage image = ImageReader.readImage(texFile);
			if(image == null)
				continue;
			maxWidth = Math.max(maxWidth, image.getWidth());
			maxHeight = Math.max(maxHeight, image.getHeight());
			layers.add(new ItemLayer(layerName, layerIndex, image));
		}
		if(layers.isEmpty())
			return null;
		
		// Sort layers.
		layers.sort(new Comparator<ItemLayer>() {

			@Override
			public int compare(ItemLayer o1, ItemLayer o2) {
				// Puts them in reverse order.
				return o2.layer - o1.layer;
			}
			
		});
		
		return new LayeredItem(maxWidth, maxHeight, layers);
	}
	
	private void voxeliseImage(Model model, LayeredItem item) {
		float voxelWidth = 16f / ((float) item.width);
		float voxelHeight = 16f / ((float) item.height);
		
		float[] minMaxPoints = new float[] { 0, 0, 7.5f, 16, 16, 8.5f };
		FaceData faceData = new FaceData();
		faceData.texture = "#layer0";
		faceData.uv0 = 0;
		faceData.uv1 = 0;
		faceData.uv2 = 16;
		faceData.uv3 = 16;
		faceData.hasUV = true;
		int prevLayerIndex = -1;
		
		for(int j = 0; j < item.height; ++j) {
			for(int i = 0; i < item.width; ++i) {
				float x = i * voxelWidth;
				float y = (item.height - j - 1) * voxelHeight;
				float u = x;
				float v = j * voxelHeight;
				
				// Find the first layer with a non-transparent pixel.
				for(ItemLayer layer : item.layers) {
					if(layer.isTransparent(i, j, item.width, item.height))
						continue;
					
					// This layer has a non-transparent pixel, so that's
					// what we'll use.
					if(layer.layer != prevLayerIndex) {
						// Update the texture.
						prevLayerIndex = layer.layer;
						faceData.texture = layer.layerName;
						faceData.tintindex = layer.layer;
					}
					
					minMaxPoints[0] = x;
					minMaxPoints[1] = y;
					minMaxPoints[3] = x + voxelWidth;
					minMaxPoints[4] = y + voxelHeight;
					faceData.uv0 = u;
					faceData.uv1 = v;
					faceData.uv2 = u + voxelWidth;
					faceData.uv3 = v + voxelHeight;
					
					boolean west = i == 0 || !item.sampleMask(i - 1, j);
					boolean east = i == (item.width - 1) || !item.sampleMask(i + 1, j);
					boolean up = j == 0 || !item.sampleMask(i, j - 1);
					boolean down = j == (item.height -1) || !item.sampleMask(i, j + 1);
					
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.SOUTH, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					model.getFaces().add(new ModelFace(minMaxPoints, Direction.NORTH, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					if(east)
						model.getFaces().add(new ModelFace(minMaxPoints, Direction.EAST, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					if(west)
						model.getFaces().add(new ModelFace(minMaxPoints, Direction.WEST, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					if(up)
						model.getFaces().add(new ModelFace(minMaxPoints, Direction.UP, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					if(down)
						model.getFaces().add(new ModelFace(minMaxPoints, Direction.DOWN, faceData, false, ModelFace.SHADING_MODE_STANDARD));
					
					break;
				}
			}
		}
	}
	
	private void generateModel(Model model) {
		LayeredItem layeredItem = constructImage(model);
		if(layeredItem == null)
			return;
		voxeliseImage(model, layeredItem);
	}

}
