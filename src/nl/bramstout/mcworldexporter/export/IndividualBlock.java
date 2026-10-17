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
import java.util.Arrays;

import nl.bramstout.mcworldexporter.lighting.BlockLightingCache;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class IndividualBlock {
	
	private IndividualBlockId id;
	private FloatArray positions;
	private VertexColorSet[] colorSets;
	
	public IndividualBlock(IndividualBlockId id) {
		this.id = id;
		this.positions = new FloatArray();
		this.colorSets = null;
	}
	
	public IndividualBlockId getId() {
		return id;
	}
	
	public FloatArray getPositions() {
		return positions;
	}
	
	public VertexColorSet[] getColorSets() {
		return colorSets;
	}
	
	public void addBlock(int wx, int wy, int wz, int layer, 
						float offsetX, float offsetY, float offsetZ,
						float worldOffsetX, float worldOffsetY, float worldOffsetZ,
						BlockLightingCache lightingCache) {
		positions.add(wx*16 + offsetX - worldOffsetX * 16);
		positions.add(wy*16 + offsetY - worldOffsetY * 16 + 8.0f);
		positions.add(wz*16 + offsetZ - worldOffsetZ * 16);
		
		if(lightingCache != null) {
			// We have lighting to save.
			// Create a dummy face and get the lighting for it.
			ModelFace face = new ModelFace(new float[] {8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f}, 
					new float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f }, "", 0, Direction.NORTH, false, ModelFace.SHADING_MODE_STANDARD);
			VertexColorSet.VertexColorFace[] vertexColors = lightingCache.getLightingForFace(face, wx, wy, wz, null);
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
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeInt(id.getBlockId());
		dos.writeInt(id.getX());
		dos.writeInt(id.getY());
		dos.writeInt(id.getZ());
		dos.writeInt(id.getLayer());
		
		dos.writeInt(positions.size());
		for(int i = 0; i < positions.size(); i += 3) {
			dos.writeFloat(positions.get(i));
			dos.writeFloat(positions.get(i+1));
			dos.writeFloat(positions.get(i+2));
		}
		
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
		int blockId = dis.readInt();
		int x = dis.readInt();
		int y = dis.readInt();
		int z = dis.readInt();
		int layer = dis.readInt();
		id = new IndividualBlockId(blockId, x, y, z, layer);
		
		positions.resize(dis.readInt());
		for(int i = 0; i < positions.size(); ++i)
			positions.set(i, dis.readFloat());
		
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
