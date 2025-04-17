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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Cache;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BakedBlockStateLiquid extends BakedBlockState{
	
	
	private Cache<Model> modelCache;
	private String stillTexture;
	private String flowTexture;
	
	public BakedBlockStateLiquid(String name) {
		super(name, new ArrayList<List<Model>>(), true, false, false, false, true, false, false, false, false, false, 
				Config.waterColormapBlocks.contains(name), // Only apply the water biome colour when we say to in the config.
				true, Config.randomAnimationXZOffset.contains(name), Config.randomAnimationYOffset.contains(name), false, 2, null, true);
		String[] nameTokens = getName().split(":");
		stillTexture = nameTokens[0] + ":block/" + nameTokens[1] + "_still";
		flowTexture = nameTokens[0] + ":block/" + nameTokens[1] + "_flow";
		modelCache = new Cache<Model>();
	}
	
	public void getModels(int x, int y, int z, List<Model> res){
		int level00 = getLevel(x-1, y, z-1);
		int level10 = getLevel(x  , y, z-1);
		int level20 = getLevel(x+1, y, z-1);
		int level01 = getLevel(x-1, y, z);
		int level11 = getLevel(x  , y, z);
		int level21 = getLevel(x+1, y, z);
		int level02 = getLevel(x-1, y, z+1);
		int level12 = getLevel(x  , y, z+1);
		int level22 = getLevel(x+1, y, z+1);
		int isWaterLogged = 0;
		int currentBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
		Block currentBlock = BlockRegistry.getBlock(currentBlockId);
		if(!currentBlock.isLiquid() || currentBlock.isWaterlogged()) {
			isWaterLogged = 1;
		}
		
		int blockBelow = 0;
		BakedBlockState blockBelowState = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y - 1, z), x, y-1, z);
		if(blockBelowState == null || 
				!(blockBelowState.hasLiquid() || blockBelowState.isTransparentOcclusion() ||
				blockBelowState.isLeavesOcclusion()))
			blockBelow = 1;
		
		long l00 = level00 + 2;
		long l10 = level10 + 2;
		long l20 = level20 + 2;
		long l01 = level01 + 2;
		long l11 = level11 + 2;
		long l21 = level21 + 2;
		long l02 = level02 + 2;
		long l12 = level12 + 2;
		long l22 = level22 + 2;
		long bb = blockBelow + (isWaterLogged << 1);
		
		long key = (l00 & 0b1111L) | 
					((l10 & 0b1111L) << 4) |
					((l20 & 0b1111L) << 8) |
					((l01 & 0b1111L) << 12) |
					((l11 & 0b1111L) << 16) |
					((l21 & 0b1111L) << 20) |
					((l02 & 0b1111L) << 24) |
					((l12 & 0b1111L) << 28) |
					((l22 & 0b1111L) << 32) |
					((bb & 0b1111L) << 36);
		
		Model model = modelCache.getOrDefault(key, null);
		if(model == null) {
			synchronized(modelCache) {
				model = modelCache.getOrDefault(key, null);
				if(model == null) {
					model = generateModel(level00, level10, level20,
										level01, level11, level21,
										level02, level12, level22, 
										blockBelow, isWaterLogged);
					modelCache.put(key, model);
				}
			}
		}
		res.add(model);
	}
	
	private Model generateModel(int level00, int level10, int level20, 
								int level01, int level11, int level21,
								int level02, int level12, int level22, 
								int blockBelow, int isWaterLogged) {
		Model model = new Model(getName(), null, true);
		
		model.addTexture("#still", stillTexture);
		model.addTexture("#flow", flowTexture);
		
		float height00 = getHeight(level00);
		float height10 = getHeight(level10);
		float height20 = getHeight(level20);
		float height01 = getHeight(level01);
		float height11 = getHeight(level11);
		float height21 = getHeight(level21);
		float height02 = getHeight(level02);
		float height12 = getHeight(level12);
		float height22 = getHeight(level22);
		
		/*
		 * Bottom left, a.k.a. x-0.5, z-0.5
		 */
		float cheight00 = getCornerHeight(height11, height00, height10, height01);
		/*
		 * Top left, a.k.a. x-0.5, z+0.5
		 */
		float cheight01 = getCornerHeight(height11, height02, height12, height01);
		/*
		 * Top right, a.k.a. x+0.5, z+0.5
		 */
		float cheight11 = getCornerHeight(height11, height22, height12, height21);
		/*
		 * Bottom right, a.k.a. x+0.5, z-0.5
		 */
		float cheight10 = getCornerHeight(height11, height20, height10, height21);
		
		
		// Calculate the direction in which to have the water flow.
		// We do this by calculating the gradient, which gives us the direction
		// to the lowest height. Using atan2 we get an angle out of that, which
		// we can use to rotate the texture.
		float gradientX0 = height11 - height01;
		if(height01 < 0f)
			gradientX0 = 0f;
		
		float gradientX1 = height21 - height11;
		if(height21 < 0f)
			gradientX1 = 0f;
		
		float gradientZ0 = height11 - height10;
		if(height10 < 0f)
			gradientZ0 = 0f;
		
		float gradientZ1 = height12 - height11;
		if(height12 < 0f)
			gradientZ1 = 0f;
		
		float gradientX = gradientX0 + gradientX1;
		float gradientZ = gradientZ0 + gradientZ1;
		float gradientLength = (float) Math.sqrt(gradientX * gradientX + gradientZ * gradientZ);
		float angle = 0;
		if(gradientLength > 0f) {
			gradientX /= gradientLength;
			gradientZ /= gradientLength;
			angle = (float) Math.toDegrees(Math.atan2(gradientX, -gradientZ));
			// Change the angle to be in steps of 22.5 degrees
			angle = (float) Math.floor(angle / 22.5f + 0.5f) * 22.5f;
		}
		
		float[] minMaxPoints = new float[] { 0, 0, 0, 16, Math.min(Math.min(Math.min(cheight00, cheight01), cheight10), cheight11), 16 };
		float[] minMaxUVs = new float[] { 4, 4, 12, 12 };
		float[] minMaxUVsStill = new float[] { 0, 0, 16, 16 };
		
		if(height11 < 16f) {
			ModelFace topFace = model.addFace(minMaxPoints, gradientLength > 0f ? minMaxUVs : minMaxUVsStill, Direction.UP, gradientLength > 0f ? "#flow" : "#still", angle, 0);
			topFace.getPoints()[0*3+1] = cheight01;
			topFace.getPoints()[1*3+1] = cheight11;
			topFace.getPoints()[2*3+1] = cheight10;
			topFace.getPoints()[3*3+1] = cheight00;
		}
		//BakedBlockState blockBelow = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y - 1, z), x, y-1, z);
		//if(blockBelow == null || !(blockBelow.hasLiquid() || blockBelow.isTransparentOcclusion() || blockBelow.isLeavesOcclusion()))
		if(blockBelow > 0)
			model.addFace(minMaxPoints, minMaxUVs, Direction.DOWN, "#still", 0);
		
		if(height10 <= 0f) {
			ModelFace northFace = model.addFace(minMaxPoints, minMaxUVs, Direction.NORTH, "#flow", 0);
			northFace.getPoints()[2*3+1] = cheight00;
			northFace.getPoints()[3*3+1] = cheight10;
			northFace.getUVs()[2*2+1] = cheight00 * 0.5f + 4.0f;
			northFace.getUVs()[3*2+1] = cheight10 * 0.5f + 4.0f;
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
		
		if(height12 <= 0f) {
			ModelFace southFace = model.addFace(minMaxPoints, minMaxUVs, Direction.SOUTH, "#flow", 0);
			southFace.getPoints()[2*3+1] = cheight11;
			southFace.getPoints()[3*3+1] = cheight01;
			southFace.getUVs()[2*2+1] = cheight11 * 0.5f + 4.0f;
			southFace.getUVs()[3*2+1] = cheight01 * 0.5f + 4.0f;
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
		
		if(height01 <= 0f) {
			ModelFace westFace = model.addFace(minMaxPoints, minMaxUVs, Direction.WEST, "#flow", 0);
			westFace.getPoints()[2*3+1] = cheight01;
			westFace.getPoints()[3*3+1] = cheight00;
			westFace.getUVs()[2*2+1] = cheight01 * 0.5f + 4.0f;
			westFace.getUVs()[3*2+1] = cheight00 * 0.5f + 4.0f;
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
		
		if(height21 <= 0f) {
			ModelFace eastFace = model.addFace(minMaxPoints, minMaxUVs, Direction.EAST, "#flow", 0);
			eastFace.getPoints()[2*3+1] = cheight10;
			eastFace.getPoints()[3*3+1] = cheight11;
			eastFace.getUVs()[2*2+1] = cheight10 * 0.5f + 4.0f;
			eastFace.getUVs()[3*2+1] = cheight11 * 0.5f + 4.0f;
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
		
		return model;
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
			level = 0; // Waterlogged blocks have a source block in them, which is 0
		
		// Check the block above. If there is a liquid block above, then this block should be
		// a full liquid block. Which is the size of an entire block (a source block is less tall).
		BakedBlockState blockAbove = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y + 1, z), x, y + 1, z);
		if(blockAbove != null && blockAbove.hasLiquid()) {
			return 8;
		}
		
		NbtTag levelTag = block.getProperties().get("level");
		if(levelTag != null) {
			try {
				level = Integer.parseInt(levelTag.asString());
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if(level == 0 && (blockAbove == null || (!blockAbove.hasLiquid() && blockAbove.getOccludes() == 0 && 
								!blockAbove.isTransparentOcclusion() && !blockAbove.isLeavesOcclusion()))) {
			// A full source block is a block surrounded by either blocks or other source blocks.
			// This is for oceans where there are underground structures. Otherwise, it would generate
			// the top faces of the liquid, which we don't want since everything is under water.
			boolean fullSourceBlock = true;
			for(int j = y; j <= y+1; ++j) {
				for(int k = z-1; k <= z+1; ++k) {
					for(int i = x-1; i <= x+1; ++i) {
						blockAbove = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(i, j, k), i, j, k);
						if(blockAbove == null || (!blockAbove.hasLiquid() && blockAbove.getOccludes() == 0 && 
								!blockAbove.isTransparentOcclusion() && !blockAbove.isLeavesOcclusion())) {
							fullSourceBlock = false;
							break;
						}
					}
					if(!fullSourceBlock)
						break;
				}
				if(!fullSourceBlock)
					break;
			}
			if(fullSourceBlock)
				return 8;
		}
		
		return level;
	}
	
	public static float getHeight(int level) {
		if(level == 0)
			return 14.166666f; // source block
		if(level == -1)
			return 0f; // air block
		if(level < -1)
			return -1f; // normal block
		if(level > 7)
			return 16f; // falling liquid block
		
		return 2f + (12f/7f) * (7-level); // inbetween
	}
	
	public float getCornerHeight(float height00, float height01, float height11, float height10) {
		float totalWeight = 0f;
		float res = 0f;
		boolean sourceBlock = false;
		
		if(height00 >= 16f || height01 >= 16f || height10 >= 16f || height11 >= 16f)
			return 16f;
		
		if(height00 == 14.166666f) {
			// Plus the part below, it results in a weight of 12
			res += height00 * 11f;
			totalWeight += 11f;
			sourceBlock = true;
		}
		if(height01 == 14.166666f) {
			res += height01 * 12f;
			totalWeight += 12f;
			sourceBlock = true;
		}
		if(height10 == 14.166666f) {
			res += height10 * 12f;
			totalWeight += 12f;
			sourceBlock = true;
		}
		if(height11 == 14.166666f) {
			res += height11 * 12f;
			totalWeight += 12f;
			sourceBlock = true;
		}
		
		if(sourceBlock) {
			if(height00 == 0f) {
				totalWeight += 1f;
			}
			if(height01 == 0f) {
				totalWeight += 1f;
			}
			if(height11 == 0f) {
				totalWeight += 1f;
			}
			if(height10 == 0f) {
				totalWeight += 1f;
			}
		}else {
			if(height00 >= 0f) {
				res += height00;
				totalWeight += 1f;
			}
			if(height01 >= 0f) {
				res += height01;
				totalWeight += 1f;
			}
			if(height11 >= 0f) {
				res += height11;
				totalWeight += 1f;
			}
			if(height10 >= 0f) {
				res += height10;
				totalWeight += 1f;
			}
		}
		
		if(totalWeight == 0f)
			return 0f;
		
		return res / totalWeight;
	}
	
	public BakedBlockStateLiquid getLiquidState() {
		return null;
	}

}
