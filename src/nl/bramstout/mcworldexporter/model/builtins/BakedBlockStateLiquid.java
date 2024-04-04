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

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.TAG_String;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BakedBlockStateLiquid extends BakedBlockState{
	
	public BakedBlockStateLiquid(String name) {
		super(name, new ArrayList<List<Model>>(), true, false, false, false, true, false, false, false, false, false, 
				Config.waterColormapBlocks.contains(name), // Only apply the water biome colour when we say to in the config.
				true, Config.randomAnimationXZOffset.contains(name), Config.randomAnimationYOffset.contains(name), false, 2);
	}
	
	public void getModels(int x, int y, int z, List<Model> res){
		Model model = new Model(getName(), null, true);
		res.add(model);
		
		String[] nameTokens = getName().split(":");
		model.addTexture("still", nameTokens[0] + ":block/" + nameTokens[1] + "_still");
		model.addTexture("flow", nameTokens[0] + ":block/" + nameTokens[1] + "_flow");
		
		float height00 = getHeight(getLevel(x-1, y, z-1));
		float height10 = getHeight(getLevel(x  , y, z-1));
		float height20 = getHeight(getLevel(x+1, y, z-1));
		float height01 = getHeight(getLevel(x-1, y, z));
		float height11 = getHeight(getLevel(x  , y, z));
		float height21 = getHeight(getLevel(x+1, y, z));
		float height02 = getHeight(getLevel(x-1, y, z+1));
		float height12 = getHeight(getLevel(x  , y, z+1));
		float height22 = getHeight(getLevel(x+1, y, z+1));
		
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
			angle -= 180.0f;
		}
		
		float[] minMaxPoints = new float[] { 0, 0, 0, 16, Math.min(Math.min(Math.min(cheight00, cheight01), cheight10), cheight11), 16 };
		float[] minMaxUVs = new float[] { 4, 4, 12, 12 };
		float[] minMaxUVsStill = new float[] { 0, 0, 16, 16 };
		
		if(height11 < 16f) {
			ModelFace topFace = model.addFace(minMaxPoints, gradientLength > 0f ? minMaxUVs : minMaxUVsStill, Direction.UP, gradientLength > 0f ? "#flow" : "#still", angle);
			topFace.getPoints()[0*3+1] = cheight01;
			topFace.getPoints()[1*3+1] = cheight11;
			topFace.getPoints()[2*3+1] = cheight10;
			topFace.getPoints()[3*3+1] = cheight00;
		}
		BakedBlockState blockBelow = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y - 1, z));
		if(blockBelow == null || !(blockBelow.hasLiquid() || blockBelow.isTransparentOcclusion() || blockBelow.isLeavesOcclusion()))
			model.addFace(minMaxPoints, minMaxUVs, Direction.DOWN, "#still");
		
		if(height10 <= 0f) {
			ModelFace northFace = model.addFace(minMaxPoints, minMaxUVs, Direction.NORTH, "#flow");
			northFace.getPoints()[2*3+1] = cheight00;
			northFace.getPoints()[3*3+1] = cheight10;
			northFace.getUVs()[2*2+1] = cheight00 * 0.5f + 4.0f;
			northFace.getUVs()[3*2+1] = cheight10 * 0.5f + 4.0f;
		}
		
		if(height12 <= 0f) {
			ModelFace southFace = model.addFace(minMaxPoints, minMaxUVs, Direction.SOUTH, "#flow");
			southFace.getPoints()[2*3+1] = cheight11;
			southFace.getPoints()[3*3+1] = cheight01;
			southFace.getUVs()[2*2+1] = cheight11 * 0.5f + 4.0f;
			southFace.getUVs()[3*2+1] = cheight01 * 0.5f + 4.0f;
		}
		
		if(height01 <= 0f) {
			ModelFace westFace = model.addFace(minMaxPoints, minMaxUVs, Direction.WEST, "#flow");
			westFace.getPoints()[2*3+1] = cheight01;
			westFace.getPoints()[3*3+1] = cheight00;
			westFace.getUVs()[2*2+1] = cheight01 * 0.5f + 4.0f;
			westFace.getUVs()[3*2+1] = cheight00 * 0.5f + 4.0f;
		}
		
		if(height21 <= 0f) {
			ModelFace eastFace = model.addFace(minMaxPoints, minMaxUVs, Direction.EAST, "#flow");
			eastFace.getPoints()[2*3+1] = cheight10;
			eastFace.getPoints()[3*3+1] = cheight11;
			eastFace.getUVs()[2*2+1] = cheight10 * 0.5f + 4.0f;
			eastFace.getUVs()[3*2+1] = cheight11 * 0.5f + 4.0f;
		}
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
		BakedBlockState blockAbove = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(x, y + 1, z));
		if(blockAbove != null && blockAbove.hasLiquid()) {
			return 8;
		}
		
		TAG_String levelTag = (TAG_String) block.getProperties().getElement("level");
		if(levelTag != null) {
			try {
				level = Integer.parseInt(levelTag.value);
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
						blockAbove = BlockStateRegistry.getBakedStateForBlock(MCWorldExporter.getApp().getWorld().getBlockId(i, j, k));
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
