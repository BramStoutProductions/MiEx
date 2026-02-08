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

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureTransitionHytale extends ConnectedTexture{

	public ConnectedTextureTransitionHytale(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, int layer, ModelFace face) {
		Direction up = getUp(face);
		Direction left = getLeft(up, face);
		Direction down = up.getOpposite();
		Direction right = left.getOpposite();
		
		Direction sampleDir = up;
		if(Math.abs(getUVRotation() - 90f) < 45f)
			sampleDir = right;
		else if(Math.abs(getUVRotation() - 180f) < 45f)
			sampleDir = down;
		else if(Math.abs(getUVRotation() - 270f) < 45f)
			sampleDir = left;
		
		int sampleAboveId = MCWorldExporter.getApp().getWorld().getBlockId(x + sampleDir.x, y + sampleDir.y + 1, z + sampleDir.z, 0);
		BakedBlockState sampleAboveState = BlockStateRegistry.getBakedStateForBlock(
											sampleAboveId, x + sampleDir.x, y + sampleDir.y + 1, z + sampleDir.z, 0);
		if(!sampleAboveState.isAir() && !sampleAboveState.isTransparentOcclusion())
			// Require air above the neighbouring block, so that we can actually see it.
			return DELETE_FACE;
		
		boolean connects = this.connects(face, x, y, z, layer, sampleDir.x, sampleDir.y, sampleDir.z);
		
		if(!connects || tiles.isEmpty())
			return DELETE_FACE;
		String tile = tiles.get(0);
		if(tile == null)
			return DELETE_FACE;
		return tile;
	}
	
	@Override
	public boolean isOverlay() {
		return true;
	}

}
