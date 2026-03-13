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

package nl.bramstout.mcworldexporter.commands;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState.DefaultTexture;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.LayeredBlock;

public class CommandGetBakedBlockState extends Command{

	private JsonObject getBlockStateObj(BakedBlockState state, int blockX, int blockY, int blockZ, int layer, int blockId) {
		JsonObject blockStateObj = new JsonObject();
		blockStateObj.addProperty("x", blockX);
		blockStateObj.addProperty("y", blockY);
		blockStateObj.addProperty("z", blockZ);
		blockStateObj.addProperty("layer", layer);
		blockStateObj.addProperty("id", blockId);
		blockStateObj.addProperty("name", state.getName());
		blockStateObj.addProperty("occludes", state.getOccludes());
		blockStateObj.addProperty("transparentOcclusion", state.isTransparentOcclusion());
		blockStateObj.addProperty("leavesOcclusion", state.isLeavesOcclusion());
		blockStateObj.addProperty("detailedOcclusion", state.isDetailedOcclusion());
		blockStateObj.addProperty("individualBlocks", state.isIndividualBlocks());
		blockStateObj.addProperty("liquid", state.hasLiquid());
		blockStateObj.addProperty("caveBlock", state.isCaveBlock());
		blockStateObj.addProperty("randomOffset", state.hasRandomOffset());
		blockStateObj.addProperty("randomYOffset", state.hasRandomYOffset());
		blockStateObj.addProperty("air", state.isAir());
		blockStateObj.addProperty("doubleSided", state.isDoubleSided());
		blockStateObj.addProperty("randomAnimationXZOffset", state.isRandomAnimationXZOffset());
		blockStateObj.addProperty("randomAnimationYOffset", state.isRandomAnimationYOffset());
		blockStateObj.addProperty("lodNoUVScale", state.isLodNoUVScale());
		blockStateObj.addProperty("lodNoScale", state.isLodNoScale());
		blockStateObj.addProperty("lodPriority", state.getLodPriority());
		blockStateObj.addProperty("separateMeshForBlock", state.getSeparateMeshForBlock());
		if(state.getTint() == null)
			blockStateObj.add("tint", JsonNull.INSTANCE);
		else
			blockStateObj.add("tint", state.getTint().toJson());
		JsonArray modelsArray = new JsonArray();
		List<Model> models = new ArrayList<Model>();
		state.getModels(blockX, blockY, blockZ, models);
		for(Model model : models) {
			modelsArray.add(model.toJson());
		}
		blockStateObj.add("models", modelsArray);
		DefaultTexture defaultTexture = state.getDefaultTexture();
		JsonObject defaultTextureObj = new JsonObject();
		if(defaultTexture != null) {
			defaultTextureObj.addProperty("texture", defaultTexture.texture);
			defaultTextureObj.addProperty("applyTint", defaultTexture.applyTint);
		}
		blockStateObj.add("defaultTexture", defaultTextureObj);
		return blockStateObj;
	}
	
	@Override
	public JsonObject run(JsonObject command) {
		JsonArray blockStateArray = new JsonArray();
		if(command.has("blockIds")) {
			// Block id mode
			for(JsonElement el : command.getAsJsonArray("blockIds")) {
				int blockId = 0;
				int blockX = 0;
				int blockY = 0;
				int blockZ = 0;
				int layer = 0;
				if(el.isJsonObject()) {
					JsonObject locObj = el.getAsJsonObject();
					if(locObj.has("id"))
						blockId = locObj.get("id").getAsInt();
					if(locObj.has("x"))
						blockX = locObj.get("x").getAsInt();
					if(locObj.has("y"))
						blockY = locObj.get("y").getAsInt();
					if(locObj.has("z"))
						blockZ = locObj.get("z").getAsInt();
					if(locObj.has("layer"))
						layer = locObj.get("layer").getAsInt();
				}else if(el.isJsonPrimitive()) {
					blockId = el.getAsInt();
				}
				BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, blockX, blockY, blockZ, layer);
				
				JsonObject blockStateObj = getBlockStateObj(state, blockX, blockY, blockZ, layer, blockId);
				blockStateArray.add(blockStateObj);
			}
		}
		if(command.has("blockNames")) {
			// Block name mode
			for(JsonElement el : command.getAsJsonArray("blockIds")) {
				String blockName = "";
				NbtTagCompound properties = null;
				int blockX = 0;
				int blockY = 0;
				int blockZ = 0;
				int layer = 0;
				if(el.isJsonObject()) {
					JsonObject locObj = el.getAsJsonObject();
					if(locObj.has("name"))
						blockName = locObj.get("name").getAsString();
					if(locObj.has("properties")) {
						NbtTag propertiesTag = NbtTag.fromJsonValue(locObj.get("properties"));
						if(propertiesTag instanceof NbtTagCompound)
							properties = (NbtTagCompound) propertiesTag;
					}
					if(locObj.has("x"))
						blockX = locObj.get("x").getAsInt();
					if(locObj.has("y"))
						blockY = locObj.get("y").getAsInt();
					if(locObj.has("z"))
						blockZ = locObj.get("z").getAsInt();
					if(locObj.has("layer"))
						layer = locObj.get("layer").getAsInt();
				}else if(el.isJsonPrimitive()) {
					blockName = el.getAsString();
				}
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(blockName, properties, 0, charBuffer);
				BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, blockX, blockY, blockZ, layer);
				
				JsonObject blockStateObj = getBlockStateObj(state, blockX, blockY, blockZ, layer, blockId);
				blockStateArray.add(blockStateObj);
			}
		}
		if(command.has("blockLocations") && MCWorldExporter.getApp().getWorld() != null) {
			// Location mode
			
			LayeredBlock layeredBlock = new LayeredBlock();
			
			for(JsonElement el : command.getAsJsonArray("blockLocations")) {
				JsonObject locObj = el.getAsJsonObject();
				int blockX = 0;
				int blockY = 0;
				int blockZ = 0;
				if(locObj.has("x"))
					blockX = locObj.get("x").getAsInt();
				if(locObj.has("y"))
					blockY = locObj.get("y").getAsInt();
				if(locObj.has("z"))
					blockZ = locObj.get("z").getAsInt();
				
				if(locObj.has("layer") && !locObj.get("layer").isJsonNull()) {
					int layer = locObj.get("layer").getAsInt();
					
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ, layer);
					
					BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, blockX, blockY, blockZ, layer);
					
					JsonObject blockStateObj = getBlockStateObj(state, blockX, blockY, blockZ, layer, blockId);
					blockStateArray.add(blockStateObj);
				}else {
					MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ, layeredBlock);
					JsonArray blockStateObjArray = new JsonArray();
					
					for(int i = 0; i < layeredBlock.getLayerCount(); ++i) {
						int blockId = layeredBlock.getBlock(i);
						
						BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, blockX, blockY, blockZ, i);
						
						JsonObject blockStateObj = getBlockStateObj(state, blockX, blockY, blockZ, i, blockId);
						blockStateObjArray.add(blockStateObj);
					}
					
					blockStateArray.add(blockStateObjArray);
				}
			}
		}
		
		JsonObject res = new JsonObject();
		res.add("blockStates", blockStateArray);
		return res;
	}

}
