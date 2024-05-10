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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_String;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;

public class BlockState {

	protected String name;
	protected int id;
	protected boolean transparentOcclusion;
	protected boolean leavesOcclusion;
	protected boolean detailedOcclusion;
	protected boolean individualBlocks;
	protected boolean caveBlock;
	protected boolean randomOffset;
	protected boolean randomYOffset;
	protected List<BlockStatePart> parts;
	protected boolean grassColormap;
	protected boolean foliageColormap;
	protected boolean waterColormap;
	protected boolean doubleSided;
	protected boolean randomAnimationXZOffset;
	protected boolean randomAnimationYOffset;
	protected boolean lodNoUVScale;
	protected int lodPriority;
	protected boolean _needsConnectionInfo;
	
	public BlockState(String name, JsonObject data) {
		this.name = name;
		this.id = BlockStateRegistry.getNextId();
		this.parts = new ArrayList<BlockStatePart>();
		this.transparentOcclusion = Config.transparentOcclusion.contains(name);
		this.leavesOcclusion = Config.leavesOcclusion.contains(name);
		this.detailedOcclusion = Config.detailedOcclusion.contains(name);
		this.individualBlocks = Config.individualBlocks.contains(name);
		this.caveBlock = Config.caveBlocks.contains(name);
		this.randomOffset = Config.randomOffset.contains(name);
		this.randomYOffset = Config.randomYOffset.contains(name);
		grassColormap = Config.grassColormapBlocks.contains(name);
		foliageColormap = Config.foliageColormapBlocks.contains(name);
		waterColormap = Config.waterColormapBlocks.contains(name);
		doubleSided = Config.doubleSided.contains(name);
		randomAnimationXZOffset = Config.randomAnimationXZOffset.contains(name);
		randomAnimationYOffset = Config.randomAnimationYOffset.contains(name);
		lodNoUVScale = Config.lodNoUVScale.contains(name);
		lodPriority = Config.lodPriority.getOrDefault(name, 1);
		
		if(data == null)
			return;
		
		if(data.has("variants")) {
			for(Entry<String, JsonElement> variant : data.get("variants").getAsJsonObject().entrySet()) {
				parts.add(new BlockStateVariant(variant.getKey(), variant.getValue(), doubleSided));
			}
		} else if(data.has("multipart")) {
			for(JsonElement part : data.get("multipart").getAsJsonArray().asList()) {
				parts.add(new BlockStateMultiPart(part, doubleSided));
			}
		}
		
		_needsConnectionInfo = false;
		for(BlockStatePart part : parts) {
			if(part.needsConnectionInfo()) {
				_needsConnectionInfo = true;
				break;
			}
		}
		
		if(Config.noOcclusion.contains(name)) {
			for(BlockStatePart part : parts)
				part.noOcclusion();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
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
	
	public boolean isCaveBlock() {
		return caveBlock;
	}
	
	public boolean hasRandomOffset() {
		return randomOffset;
	}
	
	public boolean hasRandomYOffset() {
		return randomYOffset;
	}
	
	public boolean isDoubleSided() {
		return doubleSided;
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
	
	public int getLodPriority() {
		return lodPriority;
	}
	
	public boolean needsConnectionInfo() {
		return _needsConnectionInfo;
	}
	
	public BakedBlockState getBakedBlockState(TAG_Compound properties, int x, int y, int z) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		for(BlockStatePart part : parts) {
			if(part.usePart(properties, x, y, z)) {
				models.add(part.models);
			}
		}
		Tint tint = Tints.getTint(getName());
		Color tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, randomOffset, randomYOffset,
				grassColormap, foliageColormap, waterColormap, doubleSided, randomAnimationXZOffset,
				randomAnimationYOffset, lodNoUVScale, lodPriority, tintColor);
	}
	
	protected boolean hasLiquid(TAG_Compound properties) {
		TAG_String waterloggedTag = (TAG_String) properties.getElement("waterlogged");
		
		if(waterloggedTag == null) {
			if(Config.waterlogged.contains(name))
				return true;
			return false;
		}
		return waterloggedTag.value.equalsIgnoreCase("true");
	}
	
	public String getDefaultTexture() {
		if(parts.size() == 0)
			return "";
		return parts.get(0).getDefaultTexture();
	}

	public boolean hasTint() {
		return grassColormap || foliageColormap || waterColormap;
	}
	
}
