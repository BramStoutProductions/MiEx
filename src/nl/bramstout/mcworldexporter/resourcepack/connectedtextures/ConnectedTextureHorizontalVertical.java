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

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureHorizontalVertical extends ConnectedTexture{

	public ConnectedTextureHorizontalVertical(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction left = getLeft(up, face);
		Direction right = left.getOpposite();
		Direction down = up.getOpposite();
		
		int tile = getTile(face, x, y, z, left, right);
		
		if(tile == 3) {
			// Check if we can connect vertically.
			int tileUp = getTile(face, x + up.x, y + up.y, z + up.z, left, right);
			int tileDown = getTile(face, x + down.x, y + down.y, z + down.z, left, right);
			
			boolean upConnected = tileUp == 3;
			boolean downConnected = tileDown == 3;
			
			if(upConnected && downConnected)
				tile = 5;
			else if(upConnected)
				tile = 4;
			else if(downConnected)
				tile = 6;
		}
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		return tiles.get(tile);
	}
	
	private int getTile(ModelFace face, int x, int y, int z, Direction left, Direction right) {
		boolean leftConnected = connects(face, x, y, z, left.x, left.y, left.z);
		boolean rightConnected = connects(face, x, y, z, right.x, right.y, right.z);
		
		int tile = 3;
		
		if(leftConnected && rightConnected)
			tile = 1;
		else if(leftConnected)
			tile = 2;
		else if(rightConnected)
			tile = 0;
		
		return tile;
	}
	
}
