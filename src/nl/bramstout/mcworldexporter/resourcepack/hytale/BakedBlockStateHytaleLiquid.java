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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.builtins.BakedBlockStateLiquid;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BakedBlockStateHytaleLiquid extends BakedBlockStateLiquid{

	public BakedBlockStateHytaleLiquid(String name, String stillTexture, String flowTexture) {
		super(name);
		this.stillTexture = stillTexture;
		this.flowTexture = flowTexture;
	}
	
	public void getModels(int x, int y, int z, List<Model> res){
		int levelNorth = getLevel(x  , y, z-1);
		int levelWest = getLevel(x-1, y, z);
		int levelCenter = getLevel(x  , y, z);
		int levelEast = getLevel(x+1, y, z);
		int levelSouth = getLevel(x  , y, z+1);
		int isWaterLogged = 0;
		int currentBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
		Block currentBlock = BlockRegistry.getBlock(currentBlockId);
		if(!currentBlock.isLiquid() || currentBlock.isWaterlogged()) {
			isWaterLogged = 1;
		}
		int blockBelow = 0;
		BakedBlockState blockBelowState = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y - 1, z), x, y-1, z);
		if(blockBelowState == null || 
				(!blockBelowState.hasLiquid() || blockBelowState.isTransparentOcclusion() ||
				blockBelowState.isLeavesOcclusion()))
			blockBelow = 1;
		
		float heightCenter = getHeight(levelCenter);
		float heightNorth = getHeight(levelNorth);
		float heightSouth = getHeight(levelSouth);
		float heightEast = getHeight(levelEast);
		float heightWest = getHeight(levelWest);
		
		Model model = new Model(getName(), null, true);
		
		model.addTexture("#still", stillTexture);
		model.addTexture("#flow", flowTexture);
		
		float[] minMaxPoints = new float[] { 0, 0, 0, 16, heightCenter, 16 };
		float[] minMaxUVs = new float[] { 0, 0, 16, 16 };
		
		if(heightCenter < 16f) {
			model.addFace(minMaxPoints, minMaxUVs, Direction.UP, "#still", 0);
		}
		
		if(blockBelow > 0)
			model.addFace(minMaxPoints, minMaxUVs, Direction.DOWN, "#still", 0);
		
		if(heightNorth < heightCenter) {
			ModelFace northFace = model.addFace(minMaxPoints, minMaxUVs, Direction.NORTH, "#flow", 0);
			northFace.getPoints()[0*3+1] = Math.max(heightNorth, 0f);
			northFace.getPoints()[1*3+1] = Math.max(heightNorth, 0f);
			northFace.getUVs()[0*2+1] = Math.max(heightNorth, 0f);
			northFace.getUVs()[1*2+1] = Math.max(heightNorth, 0f);
			northFace.getUVs()[2*2+1] = heightCenter;
			northFace.getUVs()[3*2+1] = heightCenter;
			if(isWaterLogged == 0) {
				// Move the face a tiny bit outwards in case there happens
				// to be a block
				northFace.translate(0, 0, -0.01f);
			}else {
				// Move the face a tiny bit inwards to make the water not
				// show up where it shouldn't be.
				northFace.translate(0, 0, 0.01f);
			}
		}
		
		if(heightSouth < heightCenter) {
			ModelFace southFace = model.addFace(minMaxPoints, minMaxUVs, Direction.SOUTH, "#flow", 0);
			southFace.getPoints()[0*3+1] = Math.max(heightSouth, 0f);
			southFace.getPoints()[1*3+1] = Math.max(heightSouth, 0f);
			southFace.getUVs()[0*2+1] = Math.max(heightSouth, 0f);
			southFace.getUVs()[1*2+1] = Math.max(heightSouth, 0f);
			southFace.getUVs()[2*2+1] = heightCenter;
			southFace.getUVs()[3*2+1] = heightCenter;
			if(isWaterLogged == 0) {
				// Move the face a tiny bit outwards in case there happens
				// to be a block
				southFace.translate(0, 0, 0.01f);
			}else {
				// Move the face a tiny bit inwards to make the water not
				// show up where it shouldn't be.
				southFace.translate(0, 0, -0.01f);
			}
		}
		
		if(heightEast < heightCenter) {
			ModelFace eastFace = model.addFace(minMaxPoints, minMaxUVs, Direction.EAST, "#flow", 0);
			eastFace.getPoints()[0*3+1] = Math.max(heightEast, 0f);
			eastFace.getPoints()[1*3+1] = Math.max(heightEast, 0f);
			eastFace.getUVs()[0*2+1] = Math.max(heightEast, 0f);
			eastFace.getUVs()[1*2+1] = Math.max(heightEast, 0f);
			eastFace.getUVs()[2*2+1] = heightCenter;
			eastFace.getUVs()[3*2+1] = heightCenter;
			if(isWaterLogged == 0) {
				// Move the face a tiny bit outwards in case there happens
				// to be a block
				eastFace.translate(0.01f, 0, 0);
			}else {
				// Move the face a tiny bit inwards to make the water not
				// show up where it shouldn't be.
				eastFace.translate(-0.01f, 0, 0);
			}
		}
		
		if(heightWest < heightCenter) {
			ModelFace westFace = model.addFace(minMaxPoints, minMaxUVs, Direction.WEST, "#flow", 0);
			westFace.getPoints()[0*3+1] = Math.max(heightWest, 0f);
			westFace.getPoints()[1*3+1] = Math.max(heightWest, 0f);
			westFace.getUVs()[0*2+1] = Math.max(heightWest, 0f);
			westFace.getUVs()[1*2+1] = Math.max(heightWest, 0f);
			westFace.getUVs()[2*2+1] = heightCenter;
			westFace.getUVs()[3*2+1] = heightCenter;
			if(isWaterLogged == 0) {
				// Move the face a tiny bit outwards in case there happens
				// to be a block
				westFace.translate(-0.01f, 0, 0);
			}else {
				// Move the face a tiny bit inwards to make the water not
				// show up where it shouldn't be.
				westFace.translate(0.01f, 0, 0);
			}
		}
		
		if(!model.getFaces().isEmpty())
			res.add(model);
	}
	
	private int getLevel(int x, int y, int z) {
		int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
		Block block = BlockRegistry.getBlock(blockId);
		

		int level = -2;
		
		if(block == null)
			return -1; // -1 for air
		if(block.getName().equals("minecraft:air"))
			return -1;
		if(!block.hasLiquid())
			return -2; // -2 for non liquid blocks
		
		if(block.isWaterlogged())
			level = 8; // Waterlogged blocks have a source block in them, which is 8
		
		// Check the block above. If there is a liquid block above, then this block should be
		// a full liquid block. Which is the size of an entire block (a source block is less tall).
		BakedBlockState blockAbove = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y + 1, z), x, y + 1, z);
		if(blockAbove != null && blockAbove.hasLiquid()) {
			return 9;
		}
		
		NbtTag levelTag = block.getProperties().get("level");
		if(levelTag != null) {
			try {
				level = Integer.parseInt(levelTag.asString());
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if(block.getName().contains("_Source")) {
			// Source blocks are always level 8
			level = 8;
		}
		
		return level;
	}
	
	public static float getHeight(int level) {
		if(level == 0)
			return 0f; // source block
		if(level == -1)
			return 0f; // air block
		if(level < -1)
			return -1f; // normal block
		if(level > 8)
			return 16f; // falling liquid block
		
		return level * 2f - 1f;
	}

}
