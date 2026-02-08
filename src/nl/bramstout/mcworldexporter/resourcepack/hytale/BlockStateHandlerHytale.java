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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;

public class BlockStateHandlerHytale extends BlockStateHandler{
	
	private Map<String, BlockStateVariant> variants;
	private String group;
	private ConnectedBlockRuleSet connectedBlockRuleSet;
	
	public BlockStateHandlerHytale(String name, JsonObject data, ResourcePackHytale rp) {
		this.variants = new HashMap<String, BlockStateVariant>();
		this.group = "";
		this.connectedBlockRuleSet = null;
		if(data == null)
			return;
		
		if(data.has("Parent")) {
			String parentId = "hytale:" + data.get("Parent").getAsString();
			BlockStateHandlerHytale parent = (BlockStateHandlerHytale)rp.getBlockStateHandler(parentId);
			if(parent != null) {
				for(Entry<String, BlockStateVariant> entry : parent.variants.entrySet()) {
					this.variants.put(entry.getKey(), new BlockStateVariant(entry.getKey(), entry.getValue()));
				}
			}
		}
		
		if(data.has("BlockType")) {
			JsonObject blockType = data.getAsJsonObject("BlockType");
			if(blockType.has("Group") && blockType.get("Group").getAsString().equals("@Tech"))
				// Blocks in the "@Tech" group, should be ignored.
				return;
			if(blockType.has("Group")) {
				this.group = blockType.get("Group").getAsString();
				if(this.group.indexOf(':') == -1)
					this.group = "hytale:" + this.group;
			}
			
			if(this.variants.containsKey(""))
				this.variants.get("").load(blockType);
			else
				this.variants.put("", new BlockStateVariant("", blockType));
			
			if(blockType.has("State")) {
				JsonObject state = blockType.getAsJsonObject("State");
				if(state.has("Definitions")) {
					JsonObject definitions = state.getAsJsonObject("Definitions");
					for(Entry<String, JsonElement> entry : definitions.entrySet()) {
						if(entry.getValue().isJsonObject()) {
							BlockStateVariant variant = new BlockStateVariant(entry.getKey(), this.variants.get(""));
							variant.load(entry.getValue().getAsJsonObject());
							this.variants.put(entry.getKey(), variant);
						}
					}
				}
			}
			
			if(blockType.has("ConnectedBlockRuleSet")) {
				JsonElement cbrsData = blockType.get("ConnectedBlockRuleSet");
				if(cbrsData.isJsonObject()) {
					this.connectedBlockRuleSet = ConnectedBlockRuleSet.parse(cbrsData.getAsJsonObject());
				}
			}
		}
	}

	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer, BlockState state) {
		return getAnimatedBakedBlockState(properties, x, y, z, layer, state, null, 0f);
	}
	
	@Override
	public BakedBlockState getAnimatedBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer, BlockState state,
			BlockAnimationHandler animationHandler, float frame) {
		String variantName = "";
		NbtTag variantProp = properties.get("Definitions");
		if(variantProp != null)
			variantName = variantProp.asString();
		
		/*if(this.connectedBlockRuleSet != null) {
			String newVariantName = this.connectedBlockRuleSet.getVariant(properties, x, y, z);
			if(newVariantName != null)
				variantName = newVariantName;
		}*/
		
		BlockStateVariant variant = this.variants.getOrDefault(variantName, null);
		if(variant == null)
			variant = this.variants.getOrDefault("", null);
		
		if(variant == null) {
			return new BakedBlockState(state.getName(), new ArrayList<List<Model>>(), state.isTransparentOcclusion(), 
					state.isLeavesOcclusion(), state.isDetailedOcclusion(), state.isIndividualBlocks(), 
					state.isLiquid(), state.isCaveBlock(), state.hasRandomOffset(), 
					state.hasRandomYOffset(), state.isDoubleSided(), state.hasRandomAnimationXZOffset(),
					state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.isLodNoScale(), state.getLodPriority(), 
					null, state.needsConnectionInfo(), null);
		}
		return variant.getBakedBlockState(properties, x, y, z, state, animationHandler, frame);
	}

	@Override
	public String getDefaultTexture() {
		BlockStateVariant variant = this.variants.getOrDefault("", null);
		if(variant != null)
			return variant.getDefaultTexture();
		return "";
	}

	@Override
	public boolean needsConnectionInfo() {
		for(BlockStateVariant variant : this.variants.values()) {
			if(variant.needsConnectionInfo())
				return true;
		}
		return false;
	}
	
	public boolean hasBiomeTint() {
		for(BlockStateVariant variant : this.variants.values()) {
			if(variant.hasBiomeTint())
				return true;
		}
		return false;
	}
	
	public boolean isTransparent() {
		for(BlockStateVariant variant : this.variants.values()) {
			if(variant.isTransparent())
				return true;
		}
		return false;
	}
	
	public String getGroup() {
		return group;
	}
	
	public Map<String, BlockStateVariant> getVariants(){
		return variants;
	}
	
	public ConnectedBlockRuleSet getConnectedBlockRuleSet() {
		return connectedBlockRuleSet;
	}

}
