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

package nl.bramstout.mcworldexporter.resourcepack.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;

public class BlockStateHandlerJavaEdition extends BlockStateHandler{

	private List<BlockStatePart> parts;
	
	public BlockStateHandlerJavaEdition(String name, JsonObject data) {
		this.parts = new ArrayList<BlockStatePart>();
		
		if(data == null)
			return;
		
		boolean doubleSided = Config.doubleSided.contains(name) | Config.forceDoubleSidedOnEverything;
		
		if(data.has("variants")) {
			for(Entry<String, JsonElement> variant : data.get("variants").getAsJsonObject().entrySet()) {
				parts.add(new BlockStateVariant(variant.getKey(), variant.getValue(), doubleSided));
			}
		} else if(data.has("multipart")) {
			for(JsonElement part : data.get("multipart").getAsJsonArray().asList()) {
				parts.add(new BlockStateMultiPart(part, doubleSided));
			}
		}
		
		if(Config.noOcclusion.contains(name)) {
			for(BlockStatePart part : parts)
				part.noOcclusion();
		}
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, BlockState state) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			if(part.usePart(properties, x, y, z)) {
				models.add(part.models);
			}
		}
		Tint tint = state.getTint();
		TintLayers tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(state.getName(), models, state.isTransparentOcclusion(), 
				state.isLeavesOcclusion(), state.isDetailedOcclusion(), state.isIndividualBlocks(), 
				state.hasLiquid(properties), state.isCaveBlock(), state.hasRandomOffset(), 
				state.hasRandomYOffset(), state.isDoubleSided(), state.hasRandomAnimationXZOffset(),
				state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.getLodPriority(), tintColor, state.needsConnectionInfo());
	}

	@Override
	public String getDefaultTexture() {
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			String defaultTexture = part.getDefaultTexture();
			if(defaultTexture != null)
				return defaultTexture;
		}
		return "";
	}

	@Override
	public boolean needsConnectionInfo() {
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			if(part.needsConnectionInfo()) {
				return true;
			}
		}
		return false;
	}

}
