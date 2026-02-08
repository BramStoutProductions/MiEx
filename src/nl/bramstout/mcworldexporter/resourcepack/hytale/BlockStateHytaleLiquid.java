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
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.builtins.BlockStateLiquid;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.hytale.BlockStateVariant.CubeTextures;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateHytaleLiquid extends BlockStateLiquid{

	private List<CubeTextures> cubeTextures;
	private float cubeTexturesTotalWeight;
	
	public BlockStateHytaleLiquid(String name, int dataVersion, JsonObject data) {
		super(name, dataVersion);
		this.cubeTextures = new ArrayList<CubeTextures>();
		this.cubeTexturesTotalWeight = 0f;
		
		if(data.has("Textures")) {
			this.cubeTextures.clear();
			this.cubeTexturesTotalWeight = 0f;
			JsonArray texturesArray = data.getAsJsonArray("Textures");
			for(JsonElement el : texturesArray.asList()) {
				if(el.isJsonObject()) {
					CubeTextures tex = new CubeTextures(el.getAsJsonObject());
					this.cubeTextures.add(tex);
					this.cubeTexturesTotalWeight += tex.weight;
				}
			}
		}
	}
	
	public String getDefaultTexture() {
		if(this.cubeTextures.size() > 0) {
			String up = this.cubeTextures.get(0).up;
			if(up != null)
				return up;
		}
		return "";
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer, boolean runBlockConnections) {
		if(blockConnections != null && runBlockConnections) {
			properties = (NbtTagCompound) properties.copy();
			String newName = blockConnections.map(name, properties, x, y, z, layer);
			if(newName != null && !newName.equals(name)) {
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
				properties.free();
				return BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, layer, runBlockConnections);
			}
		}
		
		CubeTextures textures = null;
		float rand = Noise.get(x, y, z) * this.cubeTexturesTotalWeight;
		for(CubeTextures tex : this.cubeTextures) {
			rand -= tex.weight;
			if(rand <= 0f) {
				textures = tex;
				break;
			}
		}
		if(textures == null && this.cubeTextures.size() > 0)
			textures = this.cubeTextures.get(0);
		
		String stillTexture = "";
		String flowTexture = "";
		if(textures != null) {
			stillTexture = textures.up;
			flowTexture = textures.up;
		}
		
		BakedBlockState bakedState = new BakedBlockStateHytaleLiquid(name, stillTexture, flowTexture);
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}

}
