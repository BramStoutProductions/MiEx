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

package nl.bramstout.mcworldexporter.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.export.BlendedBiome.WeightedColor;
import nl.bramstout.mcworldexporter.export.ChunkExporter.MeshKey;
import nl.bramstout.mcworldexporter.lighting.BlockLightingCache;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNoiseFloat;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler.RandomOffsetMethod;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintValue;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AnimatedBlock {
	
	public static class AnimatedBlockId{
		public int blockId;
		public int x;
		public int y;
		public int z;
		public int layer;
		
		public AnimatedBlockId(int blockId, int x, int y, int z, int layer) {
			this.blockId = blockId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.layer = layer;
		}
		
		@Override
		public int hashCode() {
			int hash = x;
			hash = hash * 31 + y;
			hash = hash * 31 + z;
			hash = hash * 31 + blockId;
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == this)
				return true;
			if(obj instanceof AnimatedBlockId) {
				return ((AnimatedBlockId)obj).blockId == blockId && 
						((AnimatedBlockId)obj).x == x && 
						((AnimatedBlockId)obj).y == y && 
						((AnimatedBlockId)obj).z == z &&
						((AnimatedBlockId)obj).layer == layer;
			}
			return false;
		}
	}
	
	private String name;
	private AnimatedBlockId id;
	private BlendedBiome blendedBiome;
	private float duration;
	private FloatArray positions;
	private IntArray blockPositions;
	private FloatArray timeOffsets;
	private byte blockLightEmission;
	private VertexColorSet[] colorSets;
	
	public AnimatedBlock(String name, AnimatedBlockId id, BlendedBiome blendedBiome, BlockAnimationHandler animationHandler) {
		this.name = name;
		this.id = id;
		if(animationHandler != null) {
			this.duration = animationHandler.getDuration();
		}
		this.positions = new FloatArray();
		this.blockPositions = new IntArray();
		this.timeOffsets = new FloatArray();
		if(blendedBiome != null)
			this.blendedBiome = new BlendedBiome(blendedBiome);
		this.blockLightEmission = -1;
	}
	
	public void addBlock(float x, float y, float z, int bx, int by, int bz, float timeOffset, BlockLightingCache lightingCache) {
		this.positions.add(x);
		this.positions.add(y);
		this.positions.add(z);
		this.blockPositions.add(bx);
		this.blockPositions.add(by);
		this.blockPositions.add(bz);
		this.timeOffsets.add(timeOffset);
		
		if(lightingCache != null) {
			// We have lighting to save.
			// Create a dummy face and get the lighting for it.
			ModelFace face = new ModelFace(new float[] {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f}, 
					new float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f }, "", 0, Direction.NORTH, false, ModelFace.SHADING_MODE_STANDARD);
			VertexColorSet.VertexColorFace[] vertexColors = lightingCache.getLightingForFace(face, bx, by, bz, null);
			if(vertexColors != null) {
				// Add the lighting data.
				if(colorSets == null) {
					colorSets = new VertexColorSet[0];
				}
				for(int i = 0; i < vertexColors.length; ++i) {
					// Find the color set that matches.
					int colorSetI = -1;
					for(int j = 0; j < colorSets.length; ++j) {
						if(colorSets[j].getName().equals(vertexColors[i].name)) {
							colorSetI = j;
							break;
						}
					}
					if(colorSetI < 0) {
						// Create a new color set if we have no match.
						colorSets = Arrays.copyOf(colorSets, colorSets.length+1);
						colorSetI = colorSets.length - 1;
						colorSets[colorSetI] = new VertexColorSet(vertexColors[i].name, vertexColors[i].componentCount, 8);
						
						// If we already have some blocks in here, add in default values for them.
						int numBlocks = positions.size()/3 - 1;
						if(numBlocks > 0) {
							int colorIndex = colorSets[colorSetI].addValue(1f, 1f, 1f, 1f);
							for(int j = 0; j < numBlocks; ++j)
								colorSets[colorSetI].addIndex(colorIndex);
						}
					}
					
					// Add in the data.
					int colorIndex = colorSets[colorSetI].addValue(vertexColors[i].r0, vertexColors[i].g0, vertexColors[i].b0, vertexColors[i].a0);
					colorSets[colorSetI].addIndex(colorIndex);
				}
			}
		}
	}
	
	public void addBlock(float x, float y, float z, int bx, int by, int bz, 
			boolean withRandomTimeOffsetXZ, boolean withRandomTimeOffsetY, RandomOffsetMethod randomOffsetMethod, 
			float randomOffsetNoiseScale, BlockLightingCache lightingCache) {
		float timeOffset = 0f;
		if(withRandomTimeOffsetXZ || withRandomTimeOffsetY) {
			// Add some offset to it so that it doesn't perfectly line up with
			// other random values used for this block.
			if(randomOffsetMethod == RandomOffsetMethod.RANDOM) {
				timeOffset = Noise.get(withRandomTimeOffsetXZ ? (bx + 7) : 0, 
						withRandomTimeOffsetY ? (by + 9) : 0, withRandomTimeOffsetXZ ? (bz + 11) : 0) * this.duration;
			}else if(randomOffsetMethod == RandomOffsetMethod.NOISE) {
				float rx = (float) bx;
				float ry = (float) by;
				float rz = (float) bz;
				if(randomOffsetNoiseScale > 0.0001f) {
					rx /= randomOffsetNoiseScale;
					ry /= randomOffsetNoiseScale;
					rz /= randomOffsetNoiseScale;
				}
				timeOffset = ModifierNodeNoiseFloat.evalNoise(withRandomTimeOffsetXZ ? rx : 0, 
						withRandomTimeOffsetY ? ry : 0, withRandomTimeOffsetXZ ? rz : 0) * this.duration;
			}
			
			// Make sure that it's a multiple of a frame.
			timeOffset = (float) Math.floor(timeOffset * Config.animationFrameRate) / Config.animationFrameRate;
		}
		addBlock(x, y, z, bx, by, bz, timeOffset, lightingCache);
	}
	
	public String getName() {
		return name;
	}
	
	public AnimatedBlockId getId() {
		return id;
	}
	
	public float getDuration() {
		return duration;
	}
	
	public FloatArray getPositions() {
		return positions;
	}
	
	public IntArray getBlockPositions() {
		return blockPositions;
	}
	
	public FloatArray getTimeOffsets() {
		return timeOffsets;
	}
	
	public VertexColorSet[] getColorSets() {
		return colorSets;
	}
	
	public byte getBlockLightEmission() {
		if(blockLightEmission == -1) {
			Block block = BlockRegistry.getBlock(id.blockId);
			int stateId = BlockStateRegistry.getIdForName(block.getName(), block.getDataVersion());
			BlockState state = BlockStateRegistry.getState(stateId);
			
			BakedBlockState bakedState = state.getHandler().getAnimatedBakedBlockState(block.getProperties(), 
					id.x, id.y, id.z, id.layer, state, false, null, 0);
			
			blockLightEmission = bakedState.getEmissiveLightLevel();
			if(blockLightEmission < 0)
				blockLightEmission = 0;
		}
		return blockLightEmission;
	}

	public void getMeshes(float frame, Map<MeshKey, Mesh> meshes) {
		Block block = BlockRegistry.getBlock(id.blockId);
		int stateId = BlockStateRegistry.getIdForName(block.getName(), block.getDataVersion());
		BlockState state = BlockStateRegistry.getState(stateId);
		BlockAnimationHandler animationHandler = BlockStateRegistry.getBakedStateForBlock(id.blockId, id.x, id.y, id.z, id.layer, false).getAnimationHandler();
		if(animationHandler == null || state.getHandler() == null)
			// Shouldn't ever happen.
			return;
		
		BakedBlockState bakedState = state.getHandler().getAnimatedBakedBlockState(block.getProperties(), 
				id.x, id.y, id.z, id.layer, state, false, animationHandler, frame);
		
		byte blockLightEmission = bakedState.getEmissiveLightLevel();
		
		List<Model> models = new ArrayList<Model>();
		bakedState.getModels(id.x, id.y, id.z, models);
		
		for(Model model : models) {
			for(ModelFace face : model.getFaces()) {
				addFace(meshes, state.getName(), id.blockId, face, model.getTexture(face.getTexture()), 
						model.getExtraData(), bakedState.getTint(), model.isDoubleSided(), blendedBiome,
						model.isAnimatesTopology(), model.isAnimatesPoints(), model.isAnimatesUVs(), 
						model.isAnimatesVertexColors(), blockLightEmission);
			}
		}
	}
	
	private Color[] faceTint = new Color[1];
	private void addFace(Map<MeshKey, Mesh> meshes, String blockName, int blockId, ModelFace face, String texture, 
			String extraData, TintLayers tintLayers, boolean doubleSided, BlendedBiome blendedBiome,
			boolean animatesTopology, boolean animatesPoints, boolean animatesUVs, boolean animatesVertexColors,
			byte blockLightEmission) {
		if(texture == null || texture.equals(""))
			return;
		
		String matTexture = texture;
		MeshKey meshKey = new MeshKey();
		meshKey.texture = texture;
		meshKey.blockLightEmission = blockLightEmission;
		meshKey.doubleSided = face.isDoubleSided();
		Color[] tint = null;
		if(tintLayers != null) {
			int tintIndex = face.getTintIndex();
			if(tintIndex < 0 && Config.forceBiomeColor.contains(texture))
				tintIndex = 0;
			TintValue tintValue = tintLayers.getLayer(tintIndex);
			if(tintValue != null) {
				tint = faceTint;
				for(int i = 0; i < tint.length; ++i) {
					WeightedColor color = tintValue.getColor(blendedBiome);
					if(color == null) {
						tint = null;
						break;
					}
					tint[i] = color.get(i);
					if(tint[i] == null) {
						tint = null;
						break;
					}
				}
			}
		}
		if(tint != null) {
			// If the face doesn't have a tintIndex, get rid of the tint.
			// This is also how Minecraft does it.
			// But don't do it, if we want to force the biome colour anyways.
			if((face.getTintIndex() < 0 && !Config.forceBiomeColor.contains(texture)) || 
					Config.forceNoBiomeColor.contains(blockName))
				tint = null;
			else
				meshKey.hasTint = true;
		}
		Atlas.AtlasItem atlas = Atlas.getAtlasItem(texture);
		if(atlas != null) {
			meshKey.atlasItem = atlas;
			texture = atlas.atlas;
		}
		if(!face.getShadingMode().equals(ModelFace.SHADING_MODE_STANDARD))
			meshKey.shadingMode = face.getShadingMode();
		
		Mesh mesh = meshes.getOrDefault(meshKey, null);
		if(mesh == null) {
			boolean animatedTexture = false;
			MCMeta mcmeta = ResourcePacks.getMCMeta(texture);
			if(mcmeta != null)
				animatedTexture = mcmeta.isAnimate() || mcmeta.isInterpolate();
			
			mesh = new Mesh(meshKey.createName(), MeshPurpose.UNDEFINED, texture, matTexture, animatedTexture, doubleSided, 
							face.getShadingMode(), 1024, 8);
			mesh.setExtraData(extraData);
			meshes.put(meshKey, mesh);
		}
		if(animatesTopology)
			mesh.setAnimatesTopology(true);
		if(animatesPoints)
			mesh.setAnimatesPoints(true);
		if(animatesUVs)
			mesh.setAnimatesUVs(true);
		if(animatesVertexColors)
			mesh.setAnimatesVertexColors(true);
		
		mesh.addFace(face, -0.5f, 0f, -0.5f, 0f, 0f, 0f, 0f, 1, 1, 1, 1, atlas, tint, null, 0, null, null);
	}
	
	public void write(LargeDataOutputStream dos, float worldScale, float worldOffsetXZ) throws IOException{
		dos.writeUTF(name);
		dos.writeInt(id.blockId);
		dos.writeInt(id.x);
		dos.writeInt(id.y);
		dos.writeInt(id.z);
		dos.writeFloat(duration);
		dos.writeByte(getBlockLightEmission());
		
		dos.writeInt(positions.size());
		for(int i = 0; i < positions.size(); i += 3) {
			dos.writeFloat(positions.get(i) * worldScale + worldOffsetXZ);
			dos.writeFloat(positions.get(i+1) * worldScale);
			dos.writeFloat(positions.get(i+2) * worldScale + worldOffsetXZ);
		}
		
		dos.writeInt(blockPositions.size());
		for(int i = 0; i < blockPositions.size(); ++i)
			dos.writeInt(blockPositions.get(i));
		
		dos.writeInt(timeOffsets.size());
		for(int i = 0; i < timeOffsets.size(); ++i)
			dos.writeFloat(timeOffsets.get(i));
		
		blendedBiome.write(dos);
		
		if(colorSets == null) {
			dos.writeInt(0);
		}else {
			dos.writeInt(colorSets.length);
			for(int i = 0; i < colorSets.length; ++i) {
				colorSets[i].write(dos);
			}
		}
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		name = dis.readUTF();
		if(id == null)
			id = new AnimatedBlockId(0,0,0,0, 0);
		id.blockId = dis.readInt();
		id.x = dis.readInt();
		id.y = dis.readInt();
		id.z = dis.readInt();
		duration = dis.readFloat();
		blockLightEmission = dis.readByte();
		
		positions.resize(dis.readInt());
		for(int i = 0; i < positions.size(); ++i)
			positions.set(i, dis.readFloat());
		
		blockPositions.resize(dis.readInt());
		for(int i = 0; i < blockPositions.size(); ++i)
			blockPositions.set(i, dis.readInt());
		
		timeOffsets.resize(dis.readInt());
		for(int i = 0; i < timeOffsets.size(); ++i)
			timeOffsets.set(i, dis.readFloat());
		
		blendedBiome = new BlendedBiome();
		blendedBiome.read(dis);
		
		int numColorSets = dis.readInt();
		if(numColorSets == 0) {
			colorSets = null;
		}else {
			colorSets = new VertexColorSet[numColorSets];
			for(int i = 0; i < colorSets.length; ++i) {
				colorSets[i] = new VertexColorSet(dis);
			}
		}
	}

}
