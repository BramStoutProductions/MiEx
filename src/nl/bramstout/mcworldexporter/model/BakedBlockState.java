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
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;

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
	private boolean doubleSided;
	private boolean randomAnimationXZOffset;
	private boolean randomAnimationYOffset;
	private boolean lodNoUVScale;
	private boolean lodNoScale;
	private int lodPriority;
	private TintLayers tint;
	private boolean needsConnectionInfo;
	private BlockAnimationHandler animationHandler;
	
	public BakedBlockState(String name, List<List<Model>> models, 
							boolean transparentOcclusion, boolean leavesOcclusion, boolean detailedOcclusion,
							boolean individualBlocks, boolean liquid, boolean caveBlock,
							boolean randomOffset, boolean randomYOffset,
							boolean doubleSided, boolean randomAnimationXZOffset, boolean randomAnimationYOffset,
							boolean lodNoUVScale, boolean lodNoScale, int lodPriority, TintLayers tint, 
							boolean needsConnectionInfo, BlockAnimationHandler animationHandler) {
		this.name = name;
		this.models = models;
		this.occludes = 0;
		List<Model> modelList = null;
		for(int i = 0; i < models.size(); ++i) {
			modelList = models.get(i);
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
		this.doubleSided = doubleSided;
		this.randomAnimationXZOffset = randomAnimationXZOffset;
		this.randomAnimationYOffset = randomAnimationYOffset;
		this.lodNoUVScale = lodNoUVScale;
		this.lodNoScale = lodNoScale;
		this.lodPriority = lodPriority;
		this.tint = tint;
		this.needsConnectionInfo = needsConnectionInfo;
		this.animationHandler = animationHandler;
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
			random = -1.0f;
			modelList = models.get(i);
			if(modelList.size() == 1) {
				res.add(modelList.get(0));
			} else if(modelList.size() > 1) {
				if(random == -1.0f)
					random = Noise.get(x, y+i, z+i);
				float totalWeight = 0;
				for(int j = 0; j < modelList.size(); ++j)
					totalWeight += modelList.get(j).getWeight();
				float index = random * totalWeight;
				for(int j = 0; j < modelList.size(); ++j) {
					m = modelList.get(j);
					index -= m.getWeight();
					if(index <= 0f) {
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
	
	public String getDefaultTexture() {
		for(int i = 0; i < models.size(); ++i)
			if(models.get(i).size() > 0)
				return models.get(i).get(0).getDefaultTexture();
		return null;
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
	
	public boolean isLodNoScale() {
		return lodNoScale;
	}
	
	public int getLodPriority() {
		return lodPriority;
	}
	
	public TintLayers getTint() {
		return tint;
	}
	
	public boolean isNeedsConnectionInfo() {
		return needsConnectionInfo;
	}
	
	public boolean isSolidBlock() {
		return (this.occludes & 0xFFFFFFL) == 0xFFFFFFL && !this.transparentOcclusion && !this.leavesOcclusion;
	}
	
	public BlockAnimationHandler getAnimationHandler() {
		return animationHandler;
	}

}
