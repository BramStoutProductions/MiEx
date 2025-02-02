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

public class ConnectedTextureVerticalHorizontal extends ConnectedTexture{
	
	public ConnectedTextureVerticalHorizontal(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction down = up.getOpposite();
		Direction left = getLeft(up, face);
		Direction right = left.getOpposite();
		
		int tile = getTile(face, x, y, z, up, down);
		
		if(tile == 3) {
			// Check if we can connect vertically.
			int tileLeft = getTile(face, x + left.x, y + left.y, z + left.z, up, down);
			int tileRight = getTile(face, x + right.x, y + right.y, z + right.z, up, down);
			
			boolean leftConnected = tileLeft == 3;
			boolean rightConnected = tileRight == 3;
			
			if(leftConnected && rightConnected)
				tile = 5;
			else if(leftConnected)
				tile = 6;
			else if(rightConnected)
				tile = 4;
		}
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		
		return tiles.get(tile);
	}
	
	private int getTile(ModelFace face, int x, int y, int z, Direction up, Direction down) {
		boolean upConnected = connects(face, x, y, z, up.x, up.y, up.z);
		boolean downConnected = connects(face, x, y, z, down.x, down.y, down.z);
		
		int tile = 3;
		
		if(upConnected && downConnected)
			tile = 1;
		else if(upConnected)
			tile = 0;
		else if(downConnected)
			tile = 2;
		
		return tile;
	}
	
}
