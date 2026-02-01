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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.world.Chunk;

public class AmbientOcclusion {
	
	private static class Corner{
		public int x;
		public int y;
		public int z;
		
		public Corner(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private static class AmbientOcclusionCorner{
		int occlusion;
		
		List<Corner> corners;
		
		public AmbientOcclusionCorner(int x, int y, int z, Direction dir) {
			corners = new ArrayList<Corner>();
			occlusion = 0;
			
			// Precalculate the four corners used for occlusion.
			// For each point, we are going to sample the four corners
			// touching it.
			// These positions are the positions of corners,
			// so it's twice the resolution of the normal block grid.
			// We add 2 so that the origin of the grid is the bottom front left
			// corner of block -1,-1,-1
			int cx = x + Math.min(dir.x, 0);
			int cy = y + Math.min(dir.y, 0);
			int cz = z + Math.min(dir.z, 0);
			int cr = x * dir.rightX + y * dir.rightY + z * dir.rightZ;
			int cu = x * dir.upX + y * dir.upY + z * dir.upZ;
			for(int right = -2; right < 2; ++right) {
				for(int up = -2; up < 2; ++up) {
					if(cr != 1 && (right == -2 || right == 1))
						continue;
					if(cu != 1 && (up == -2 || up == 1))
						continue;
					corners.add(new Corner(
							cx + dir.rightX * right + dir.upX * up + 2,
							cy + dir.rightY * right + dir.upY * up + 2,
							cz + dir.rightZ * right + dir.upZ * up + 2
							));
				}
			}
		}
		
		private int sampleCorner(BakedBlockState[] blocks, int x, int y, int z) {
			int contribution = 0;
			long occlusion = 0;
			BakedBlockState state = blocks[(z>>1)*9+(y>>1)*3+(x>>1)];
			if(state != null)
				occlusion = state.getOccludes();
			
			int corner = (y&1)*4+(z&1)*2+(x&1);
			if(corner == 0) {
				// -X -Y -Z
				// NORTH, WEST, DOWN
				contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0001) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0001) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0001) != 0 ? 1 : 0;
			}else if(corner == 1) {
				// +X -Y -Z
				// NORTH, EAST, DOWN
				contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0010) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0001) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0010) != 0 ? 1 : 0;
			}else if(corner == 2) {
				// -X -Y +Z
				// SOUTH, WEST, DOWN
				contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0001) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0010) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0100) != 0 ? 1 : 0;
			}else if(corner == 3) {
				// +X -Y +Z
				// SOUTH, EAST, DOWN
				contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0010) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0010) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b1000) != 0 ? 1 : 0;
			}else if(corner == 4) {
				// -X +Y -Z
				// NORTH, WEST, UP
				contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0100) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0100) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0001) != 0 ? 1 : 0;
			}else if(corner == 5) {
				// +X +Y -Z
				// NORTH, EAST, UP
				contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b1000) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0100) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0010) != 0 ? 1 : 0;
			}else if(corner == 6) {
				// -X +Y +Z
				// SOUTH, WEST, UP
				contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0100) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b1000) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0100) != 0 ? 1 : 0;
			}else if(corner == 7) {
				// +X +Y +Z
				// SOUTH, EAST, UP
				contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b1000) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b1000) != 0 ? 1 : 0;
				contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b1000) != 0 ? 1 : 0;
			}
			
			return contribution;
		}
		
		public void calculateAmbientOcclusion(BakedBlockState[] blocks) {
			occlusion = 0;
			for(int i = 0; i < corners.size(); ++i) {
				Corner corner = corners.get(i);
				occlusion += sampleCorner(blocks, corner.x, corner.y, corner.z);
			}
		}
		
		public float getAmbientOcclusion() {
			// Occlusion can be a value from 0-(corners.size()),
			// which we want to remap to 0-1,
			// where 1 is no occlusion and 0 is full occlusion.
			return 1f - (((float) occlusion) / ((float) (corners.size()*3)));
		}
	}
	
	private static class AmbientOcclusionFace{
		
		AmbientOcclusionCorner corner00;
		AmbientOcclusionCorner corner01;
		AmbientOcclusionCorner corner02;
		AmbientOcclusionCorner corner10;
		AmbientOcclusionCorner corner11;
		AmbientOcclusionCorner corner12;
		AmbientOcclusionCorner corner20;
		AmbientOcclusionCorner corner21;
		AmbientOcclusionCorner corner22;
		
		public AmbientOcclusionFace(int x, int y, int z, Direction dir) {
			switch(dir) {
			case NORTH:
			case SOUTH:
				corner00 = new AmbientOcclusionCorner(x-1, y-1, z, dir);
				corner01 = new AmbientOcclusionCorner(x-1, y+0, z, dir);
				corner02 = new AmbientOcclusionCorner(x-1, y+1, z, dir);
				corner10 = new AmbientOcclusionCorner(x+0, y-1, z, dir);
				corner11 = new AmbientOcclusionCorner(x+0, y+0, z, dir);
				corner12 = new AmbientOcclusionCorner(x+0, y+1, z, dir);
				corner20 = new AmbientOcclusionCorner(x+1, y-1, z, dir);
				corner21 = new AmbientOcclusionCorner(x+1, y+0, z, dir);
				corner22 = new AmbientOcclusionCorner(x+1, y+1, z, dir);
				break;
			case EAST:
			case WEST:
				corner00 = new AmbientOcclusionCorner(x, y-1, z-1, dir);
				corner01 = new AmbientOcclusionCorner(x, y+0, z-1, dir);
				corner02 = new AmbientOcclusionCorner(x, y+1, z-1, dir);
				corner10 = new AmbientOcclusionCorner(x, y-1, z+0, dir);
				corner11 = new AmbientOcclusionCorner(x, y+0, z+0, dir);
				corner12 = new AmbientOcclusionCorner(x, y+1, z+0, dir);
				corner20 = new AmbientOcclusionCorner(x, y-1, z+1, dir);
				corner21 = new AmbientOcclusionCorner(x, y+0, z+1, dir);
				corner22 = new AmbientOcclusionCorner(x, y+1, z+1, dir);
				break;
			case UP:
			case DOWN:
				corner00 = new AmbientOcclusionCorner(x-1, y, z-1, dir);
				corner01 = new AmbientOcclusionCorner(x-1, y, z+0, dir);
				corner02 = new AmbientOcclusionCorner(x-1, y, z+1, dir);
				corner10 = new AmbientOcclusionCorner(x+0, y, z-1, dir);
				corner11 = new AmbientOcclusionCorner(x+0, y, z+0, dir);
				corner12 = new AmbientOcclusionCorner(x+0, y, z+1, dir);
				corner20 = new AmbientOcclusionCorner(x+1, y, z-1, dir);
				corner21 = new AmbientOcclusionCorner(x+1, y, z+0, dir);
				corner22 = new AmbientOcclusionCorner(x+1, y, z+1, dir);
				break;
			}
		}
		
		public void calculateAmbientOcclusion(BakedBlockState[] blocks) {
			corner00.calculateAmbientOcclusion(blocks);
			corner01.calculateAmbientOcclusion(blocks);
			corner02.calculateAmbientOcclusion(blocks);
			corner10.calculateAmbientOcclusion(blocks);
			corner11.calculateAmbientOcclusion(blocks);
			corner12.calculateAmbientOcclusion(blocks);
			corner20.calculateAmbientOcclusion(blocks);
			corner21.calculateAmbientOcclusion(blocks);
			corner22.calculateAmbientOcclusion(blocks);
		}
		
		public float getAmbientOcclusionForPoint(float hor, float vert) {
			hor *= 2f;
			vert *= 2f;
			float edge0 = 0f;
			float edge1 = 0f;
			float edge2 = 0f;
			if(hor <= 1f) {
				edge0 = lerp(corner00.getAmbientOcclusion(), corner10.getAmbientOcclusion(), hor);
				edge1 = lerp(corner01.getAmbientOcclusion(), corner11.getAmbientOcclusion(), hor);
				edge2 = lerp(corner02.getAmbientOcclusion(), corner12.getAmbientOcclusion(), hor);
			}else {
				edge0 = lerp(corner10.getAmbientOcclusion(), corner20.getAmbientOcclusion(), hor-1f);
				edge1 = lerp(corner11.getAmbientOcclusion(), corner21.getAmbientOcclusion(), hor-1f);
				edge2 = lerp(corner12.getAmbientOcclusion(), corner22.getAmbientOcclusion(), hor-1f);
			}
			if(vert <= 1f)
				return lerp(edge0, edge1, vert);
			else
				return lerp(edge1, edge2, vert-1f);
		}
		
	}
	
	private static class AmbientOcclusionDirection{
		
		AmbientOcclusionFace face0;
		AmbientOcclusionFace face1;
		AmbientOcclusionFace face2;
		
		public AmbientOcclusionDirection(Direction dir) {
			switch(dir) {
			case NORTH:
			case SOUTH:
				face0 = new AmbientOcclusionFace(1, 1, 0, dir);
				face1 = new AmbientOcclusionFace(1, 1, 1, dir);
				face2 = new AmbientOcclusionFace(1, 1, 2, dir);
				break;
			case EAST:
			case WEST:
				face0 = new AmbientOcclusionFace(0, 1, 1, dir);
				face1 = new AmbientOcclusionFace(1, 1, 1, dir);
				face2 = new AmbientOcclusionFace(2, 1, 1, dir);
				break;
			case UP:
			case DOWN:
				face0 = new AmbientOcclusionFace(1, 0, 1, dir);
				face1 = new AmbientOcclusionFace(1, 1, 1, dir);
				face2 = new AmbientOcclusionFace(1, 2, 1, dir);
				break;
			}
		}
		
		public void calculateAmbientOcclusion(BakedBlockState[] blocks) {
			face0.calculateAmbientOcclusion(blocks);
			face1.calculateAmbientOcclusion(blocks);
			face2.calculateAmbientOcclusion(blocks);
		}
		
		public float getAmbientOcclusionForPoint(float hor, float vert, float depth) {
			depth *= 2f;
			if(depth <= 1f)
				return lerp(face0.getAmbientOcclusionForPoint(hor, vert), face1.getAmbientOcclusionForPoint(hor, vert), depth);
			else
				return lerp(face1.getAmbientOcclusionForPoint(hor, vert), face2.getAmbientOcclusionForPoint(hor, vert), depth-1f);
		}
		
	}
	
	private static float lerp(float a, float b, float t) {
		return a * (1f-t) + b * t;
	}
	
	AmbientOcclusionDirection dirNorth;
	AmbientOcclusionDirection dirSouth;
	AmbientOcclusionDirection dirEast;
	AmbientOcclusionDirection dirWest;
	AmbientOcclusionDirection dirUp;
	AmbientOcclusionDirection dirDown;
	BakedBlockState[] blocks;
	int[] blockId;
	boolean hasCalculated;
	ChunkExporter exporter;
	Chunk chunk;
	int cx;
	int cy;
	int cz;
	int lodSize;
	int lodYSize;
	
	public AmbientOcclusion() {
		dirNorth = new AmbientOcclusionDirection(Direction.NORTH);
		dirSouth = new AmbientOcclusionDirection(Direction.SOUTH);
		dirEast = new AmbientOcclusionDirection(Direction.EAST);
		dirWest = new AmbientOcclusionDirection(Direction.WEST);
		dirUp = new AmbientOcclusionDirection(Direction.UP);
		dirDown = new AmbientOcclusionDirection(Direction.DOWN);
		blocks = new BakedBlockState[3*3*3];
		blockId = new int[4];
		hasCalculated = false;
	}
	
	public void calculateAmbientOcclusion(ChunkExporter exporter, Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize) {
		// This function is called, even for blocks that end up being fully occluded.
		// So, to help speed things up, we do lazy evaluation.
		// We store the values and set hasCalculated to indicate that
		// we still need to do the calculation.
		// Then, when we actually need the values and we haven't calculated
		// them yet, we will calculate them first.
		// This way, we only calculate ambient occlusion for blocks that actually show up.
		
		hasCalculated = false;
		this.exporter = exporter;
		this.chunk = chunk;
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.lodSize = lodSize;
		this.lodYSize = lodYSize;
	}
	
	private void _calculateAmbientOcclusion() {
		// First get all neighbouring blocks.
		for(int z = 0; z < 3; ++z) {
			for(int y = 0; y < 3; ++y) {
				for(int x = 0; x < 3; ++x) {
					int ox = (x-1) * lodSize;
					int oy = (y-1) * lodYSize;
					int oz = (z-1) * lodSize;
					exporter.getLODBlockId(chunk, cx + ox, cy + oy, cz + oz, lodSize, lodYSize, blockId);
					
					if(blockId[0] <= 0)
						blocks[z*9+y*3+x] = null;
					else {
						blocks[z*9+y*3+x] = BlockStateRegistry.getBakedStateForBlock(blockId[0], blockId[1], blockId[2], blockId[3]);
						if(blocks[z*9+y*3+x].isTransparentOcclusion())
							blocks[z*9+y*3+x] = null;
					}
				}
			}
		}
		
		// Now update occlusion
		dirNorth.calculateAmbientOcclusion(blocks);
		dirSouth.calculateAmbientOcclusion(blocks);
		dirEast.calculateAmbientOcclusion(blocks);
		dirWest.calculateAmbientOcclusion(blocks);
		dirUp.calculateAmbientOcclusion(blocks);
		dirDown.calculateAmbientOcclusion(blocks);
		
		hasCalculated = true;
	}
	
	public float getAmbientOcclusionForPoint(float x, float y, float z, Direction dir) {
		if(!hasCalculated)
			this._calculateAmbientOcclusion();
		
		switch(dir) {
		case NORTH:
			return dirNorth.getAmbientOcclusionForPoint(x, y, z);
		case SOUTH:
			return dirSouth.getAmbientOcclusionForPoint(x, y, z);
		case EAST:
			return dirEast.getAmbientOcclusionForPoint(z, y, x);
		case WEST:
			return dirWest.getAmbientOcclusionForPoint(z, y, x);
		case UP:
			return dirUp.getAmbientOcclusionForPoint(x, z, y);
		case DOWN:
			return dirDown.getAmbientOcclusionForPoint(x, z, y);
		}
		return 1f;
	}

}
