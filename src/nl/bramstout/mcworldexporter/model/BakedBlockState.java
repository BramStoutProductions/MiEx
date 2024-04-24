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

package nl.bramstout.mcworldexporter.model;

import java.util.List;

import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.model.builtins.BakedBlockStateLiquid;

public class BakedBlockState {
	
	private String name;
	private List<List<Model>> models;
	private long occludes;
	private boolean transparentOcclusion;
	private boolean leavesOcclusion;
	private boolean detailedOcclusion;
	private boolean individualBlocks;
	private boolean liquid;
	private boolean caveBlock;
	private boolean randomOffset;
	private boolean randomYOffset;
	private boolean air;
	private boolean grassColormap;
	private boolean foliageColormap;
	private boolean waterColormap;
	private boolean doubleSided;
	private boolean randomAnimationXZOffset;
	private boolean randomAnimationYOffset;
	private boolean lodNoUVScale;
	private int lodPriority;
	private int redstonePowerLevel;
	
	public BakedBlockState(String name, List<List<Model>> models, 
							boolean transparentOcclusion, boolean leavesOcclusion, boolean detailedOcclusion,
							boolean individualBlocks, boolean liquid, boolean caveBlock,
							boolean randomOffset, boolean randomYOffset,
							boolean grassColormap, boolean foliageColormap, boolean waterColormap,
							boolean doubleSided, boolean randomAnimationXZOffset, boolean randomAnimationYOffset,
							boolean lodNoUVScale, int lodPriority, int redstonePowerLevel) {
		this.name = name;
		this.models = models;
		this.occludes = 0;
		for(List<Model> modelList : models) {
			long tmpOccludes = 0xFFFFFFFFFFFFFFFL;
			for(Model model : modelList) {
				tmpOccludes &= model.getOccludes();
			}
			this.occludes |= tmpOccludes;
		}
		this.transparentOcclusion = transparentOcclusion;
		this.leavesOcclusion = leavesOcclusion;
		this.detailedOcclusion = detailedOcclusion;
		this.individualBlocks = individualBlocks;
		this.liquid = liquid;
		this.caveBlock = caveBlock;
		this.randomOffset = randomOffset;
		this.randomYOffset = randomYOffset;
		this.air = name.equals("minecraft:air") || name.equals("minecraft:cave_air") || name.equals("minecraft:void_air");
		this.grassColormap = grassColormap;
		this.foliageColormap = foliageColormap;
		this.waterColormap = waterColormap;
		this.doubleSided = doubleSided;
		this.randomAnimationXZOffset = randomAnimationXZOffset;
		this.randomAnimationYOffset = randomAnimationYOffset;
		this.lodNoUVScale = lodNoUVScale;
		this.lodPriority = lodPriority;
		this.redstonePowerLevel = redstonePowerLevel;
	}
	
	public long getOccludes() {
		return occludes;
	}
	
	public String getName() {
		return name;
	}
	
	public void getModels(int x, int y, int z, List<Model> res){
		float random = -1.0f;
		List<Model> modelList;
		Model m;
		for(int i = 0; i < models.size(); ++i) {
			modelList = models.get(i);
			if(modelList.size() == 1) {
				res.add(modelList.get(0));
			} else if(modelList.size() > 1) {
				if(random == -1.0f)
					random = Noise.get(x, y, z);
				float totalWeight = 0;
				for(int j = 0; j < modelList.size(); ++j)
					totalWeight += modelList.get(j).getWeight();
				float index = random * totalWeight;
				for(int j = 0; j < modelList.size(); ++j) {
					m = modelList.get(j);
					index -= m.getWeight();
					if(index < 0) {
						res.add(m);
						break;
					}
				}
			}
		}
	}
	
	public void getDefaultModels(List<Model> res){
		List<Model> modelList;
		for(int i = 0; i < models.size(); ++i) {
			modelList = models.get(i);
			if(modelList.size() >= 1) {
				res.add(modelList.get(0));
			}
		}
	}
	
	public boolean isTransparentOcclusion() {
		return transparentOcclusion;
	}
	
	public boolean isLeavesOcclusion() {
		return leavesOcclusion;
	}
	
	public boolean isDetailedOcclusion() {
		return detailedOcclusion;
	}
	
	public boolean isIndividualBlocks() {
		return individualBlocks;
	}
	
	public boolean hasLiquid() {
		return liquid;
	}
	
	public boolean isCaveBlock() {
		return caveBlock;
	}
	
	public boolean hasRandomOffset() {
		return randomOffset;
	}
	
	public boolean hasRandomYOffset() {
		return randomYOffset;
	}
	
	public boolean isAir() {
		return air;
	}
	
	public static BakedBlockStateLiquid BAKED_WATER_STATE = new BakedBlockStateLiquid("minecraft:water");
	public BakedBlockStateLiquid getLiquidState() {
		if(liquid)
			return BAKED_WATER_STATE;
		return null;
	}
	
	public boolean isGrassColormap() {
		return grassColormap;
	}
	
	public boolean isFoliageColormap() {
		return foliageColormap;
	}
	
	public boolean isWaterColormap() {
		return waterColormap;
	}
	
	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public boolean isRandomAnimationXZOffset() {
		return randomAnimationXZOffset;
	}
	
	public boolean isRandomAnimationYOffset() {
		return randomAnimationYOffset;
	}
	
	public boolean isLodNoUVScale() {
		return lodNoUVScale;
	}
	
	public int getLodPriority() {
		return lodPriority;
	}
	
	public int getRedstonePowerLevel() {
		return redstonePowerLevel;
	}

}
