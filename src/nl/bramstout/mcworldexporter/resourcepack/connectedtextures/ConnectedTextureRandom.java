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

package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class ConnectedTextureRandom extends ConnectedTexture{

	public static enum Symmetry{
		NONE, OPPOSITE, ALL
	}
	
	private List<Float> weights;
	private float totalWeight;
	private Symmetry symmetry;
	private boolean linked;
	
	public ConnectedTextureRandom(String name, int priority) {
		super(name, priority);
		weights = new ArrayList<Float>();
		totalWeight = 1f;
		symmetry = Symmetry.NONE;
		linked = false;
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		int rx = 0;
		int ry = 0;
		int rz = 0;
		if(symmetry == Symmetry.NONE) {
			// We want each face to get it's own random number.
			rx = face.getDirection().x * 26;
			ry = face.getDirection().y * 26;
			rz = face.getDirection().z * 26;
		} else if(symmetry == Symmetry.OPPOSITE) {
			// We want opposite faces to get the same random number.
			Direction randDir = face.getDirection();
			switch(randDir) {
			case SOUTH:
				randDir = Direction.NORTH;
				break;
			case WEST:
				randDir = Direction.EAST;
				break;
			case DOWN:
				randDir = Direction.UP;
				break;
			default:
				break;
			}
			rx = randDir.x * 26;
			ry = randDir.y * 26;
			rz = randDir.z * 26; 
		}
		
		float rand = Noise.get(x+rx, y+ry, z+rz);
		if(linked) {
			// If linked is true, then we want to use the same
			// random number for our neighbouring block.
			// In this case it's either the block above
			// or below us. To keep things simple, we'll
			// always use the random number of the lower
			// block, so all we have to do is check if
			// below us is the same block and if so,
			// use its position for the random number.
			int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			int belowId = MCWorldExporter.getApp().getWorld().getBlockId(x, y-1, z);
			boolean match = thisId == belowId;
			if(!match) {
				// Check if the names match
				String thisName = BlockRegistry.getBlock(thisId).getName();
				String belowName = BlockRegistry.getBlock(belowId).getName();
				// Check the names and also replace upper with lower in case,
				// whether it's an upper or lower block is specified in the name.
				match = thisName.replace("upper", "lower").equals(belowName);
			}
			if(match) {
				// Get the noise for the block below us.
				rand = Noise.get(x+rx, y+ry-1, z+rz);
			}
		}
		
		// Get the tile based on the random value.
		rand *= totalWeight;
		float val = 0f;
		int currentTile = -1;
		for(int i = 0; i < Math.min(weights.size(), tiles.size()); ++i) {
			if(val < rand)
				currentTile = i;
			else
				break;
			val += weights.get(i);
		}
		if(currentTile < 0 || currentTile >= tiles.size())
			return null;
		return tiles.get(currentTile);
	}
	
	public List<Float> getWeights(){
		return weights;
	}
	
	public float getTotalWeight() {
		return totalWeight;
	}
	
	public void setTotalWeight(float totalWeight) {
		this.totalWeight = totalWeight;
	}
	
	public Symmetry getSymmetry() {
		return symmetry;
	}
	
	public void setSymmetry(Symmetry symmetry) {
		this.symmetry = symmetry;
	}
	
	public boolean isLinked() {
		return linked;
	}
	
	public void setLinked(boolean linked) {
		this.linked = linked;
	}

}
