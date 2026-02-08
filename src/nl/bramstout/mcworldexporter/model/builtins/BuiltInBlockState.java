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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.expression.ExprContext;
import nl.bramstout.mcworldexporter.expression.ExprValue;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueDict;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ArrayPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ObjectPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.Part;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.world.World;

public class BuiltInBlockState extends BlockState{
	
	private static Map<String, BuiltInBlockStateHandler> handlerRegistry = new HashMap<String, BuiltInBlockStateHandler>();
	
	public static BuiltInBlockStateHandler getHandler(String blockName) {
		return handlerRegistry.getOrDefault(blockName, null);
	}
	
	public static boolean hasHandler(String blockName) {
		return handlerRegistry.containsKey(blockName);
	}
	
	public static void load() {
		Map<String, BuiltInBlockStateHandler> handlerRegistry = new HashMap<String, BuiltInBlockStateHandler>();
		
		for(int i = ResourcePacks.getActiveResourcePacks().size() - 1; i >= 0; --i) {
			File builtInFolder = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), "builtins");
			if(!builtInFolder.exists() || !builtInFolder.isDirectory())
				continue;
			for(File namespaceFolder : builtInFolder.listFiles()) {
				if(!namespaceFolder.isDirectory())
					continue;
				File blockstatesFolder = new File(namespaceFolder, "blockstates");
				if(blockstatesFolder.exists() && blockstatesFolder.isDirectory())
					loadFolder(blockstatesFolder, namespaceFolder.getName() + ":", i, handlerRegistry);
			}
		}
		
		BuiltInBlockState.handlerRegistry = handlerRegistry;
	}
	
	private static void loadFolder(File folder, String parent, int resourcePackIndex, 
			Map<String, BuiltInBlockStateHandler> handlerRegistry) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadFolder(file, parent + file.getName() + "/", resourcePackIndex, handlerRegistry);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				String name = file.getName();
				int dotIndex = name.lastIndexOf('.');
				name = name.substring(0, dotIndex);
				BuiltInBlockStateHandler handler = loadFile(file, parent + name, resourcePackIndex);
				for(String blockName : handler.blockNames)
					handlerRegistry.put(blockName, handler);
			}
		}
	}
	
	private static BuiltInBlockStateHandler loadFile(File file, String name, int resourcePackIndex) {
		JsonObject data = Json.read(file).getAsJsonObject();
		BuiltInBlockStateHandler handler = new BuiltInBlockStateHandler(name, resourcePackIndex, data);
		return handler;
	}
	
	private static BuiltInBlockStateHandler loadFile(String name, int startResourcePackIndex) {
		int colonIndex = name.indexOf(':');
		String namespace = name.substring(0, colonIndex);
		String path = name.substring(colonIndex + 1);
		for(int i = startResourcePackIndex; i < ResourcePacks.getActiveResourcePacks().size(); ++i) {
			File file = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), 
									"builtins/" + namespace + "/blockstates/" + path + ".json");
			if(file.exists()) {
				return loadFile(file, name, i);
			}
		}
		return null;
	}
	
	public static class BuiltInBlockAnimationHandler extends BlockAnimationHandler{
		
		public BuiltInBlockAnimationHandler(float duration, boolean positionDependent,
											boolean randomOffsetXZ, boolean randomOffsetY) {
			this.duration = duration;
			this.positionDependent = positionDependent;
			this.randomOffsetXZ = randomOffsetXZ;
			this.randomOffsetY = randomOffsetY;
		}
		
	}
	
	public static class BuiltInBlockStateHandler extends BlockStateHandler{
		
		private List<String> blockNames;
		private boolean isLocationDependent;
		private boolean isAnimated;
		private BuiltInModel model;
		private BuiltInBlockAnimationHandler animationHandler;
		
		public BuiltInBlockStateHandler(String name, int resourcePackIndex, JsonObject data) {
			blockNames = new ArrayList<String>();
			isLocationDependent = false;
			isAnimated = false;
			model = new BuiltInModel();
			animationHandler = null;
			
			if(data.has("include")) {
				JsonArray includeArray = data.getAsJsonArray("include");
				for(JsonElement el : includeArray.asList()) {
					String includeName = el.getAsString();
					if(!includeName.contains(":"))
						includeName = "minecraft:" + includeName;
					
					BuiltInBlockStateHandler includedHandler = null;
					if(includeName.equalsIgnoreCase(name))
						includedHandler = loadFile(includeName, resourcePackIndex + 1);
					else
						includedHandler = loadFile(includeName, 0);
					
					if(includedHandler != null) {
						blockNames.addAll(includedHandler.blockNames);
						isLocationDependent = isLocationDependent || includedHandler.isLocationDependent;
						isAnimated = isAnimated || includedHandler.isAnimated;
						if(includedHandler.animationHandler != null)
							animationHandler = includedHandler.animationHandler;
						
						if(model.rootPart == null)
							model.rootPart = includedHandler.model.rootPart;
						else {
							if(model.rootPart instanceof ArrayPart) {
								((ArrayPart) model.rootPart).addChild(includedHandler.model.rootPart);
							}else if(model.rootPart instanceof ObjectPart) {
								Part tmpPart = model.rootPart;
								model.rootPart = new ArrayPart(null);
								((ArrayPart) model.rootPart).addChild(tmpPart);
								((ArrayPart) model.rootPart).addChild(includedHandler.model.rootPart);
							}
						}
						
						model.localGenerators.putAll(includedHandler.model.localGenerators);
						model.localFunctions.putAll(includedHandler.model.localFunctions);
						if(!includedHandler.model.defaultTexture.isEmpty())
							model.defaultTexture = includedHandler.model.defaultTexture;
					}
				}
			}
			
			if(data.has("blocks")) {
				blockNames.clear();
				for(JsonElement el : data.getAsJsonArray("blocks").asList()) {
					String blockName = el.getAsString();
					if(!blockName.contains(":"))
						blockName = "minecraft:" + blockName;
					blockNames.add(blockName);
				}
			}
			
			if(data.has("isLocationDependent"))
				isLocationDependent = data.get("isLocationDependent").getAsBoolean();
			
			if(data.has("isAnimated")) {
				isAnimated = data.get("isAnimated").getAsBoolean();
			}
			if(isAnimated) {
				float duration = 1f;
				boolean randomOffsetXZ = false;
				boolean randomOffsetY = false;
				if(animationHandler != null) {
					duration = animationHandler.getDuration();
					randomOffsetXZ = animationHandler.hasRandomOffsetXZ();
					randomOffsetY = animationHandler.hasRandomOffsetY();
				}
				
				if(data.has("animationDuration"))
					duration = data.get("animationDuration").getAsFloat();
				if(data.has("animationRandomOffsetXZ"))
					randomOffsetXZ = data.get("animationRandomOffsetXZ").getAsBoolean();
				if(data.has("animationRandomOffsetY"))
					randomOffsetY = data.get("animationRandomOffsetY").getAsBoolean();
				
				animationHandler = new BuiltInBlockAnimationHandler(duration, isLocationDependent, randomOffsetXZ, randomOffsetY);
			}
			
			model.parse(data);
		}

		@Override
		public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer,
				BlockState state) {
			return getAnimatedBakedBlockState(properties, x, y, z, layer, state, null, 0f);
		}

		@Override
		public BakedBlockState getAnimatedBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer,
				BlockState state, BlockAnimationHandler animationHandler, float frame) {
			List<List<Model>> models = new ArrayList<List<Model>>();
			if(this.model.rootPart != null) {
				Model model = new Model(state.getName(), null, state.isDoubleSided());
				ExprContext context = new ExprContext(state.getName(), properties, needsConnectionInfo(), x, y, z, (float) x, (float) y, (float) z, 
						frame, model, new ExprValue(new ExprValueDict()), ExprValue.VALUE_BUILTINS, 
						this.model.localGenerators, this.model.localFunctions);
				try {
					this.model.rootPart.eval(context);
				}catch(Exception ex) {
					World.handleError(new RuntimeException("Error while evaluating block state " + state.getName(), ex));
				}
				if(!model.getFaces().isEmpty()) {
					model.calculateOccludes();
					List<Model> models2 = new ArrayList<Model>();
					models2.add(model);
					models.add(models2);
				}
			}
			
			Tint tint = Tints.getTint(state.getName());
			TintLayers tintColor = null;
			if(tint != null)
				tintColor = tint.getTint(properties);
			BakedBlockState bakedState = new BakedBlockState(state.getName(), models, state.isTransparentOcclusion(), 
					state.isLeavesOcclusion(), state.isDetailedOcclusion(), state.isIndividualBlocks(), 
					state.isLiquid(), state.isCaveBlock(), state.hasRandomOffset(), 
					state.hasRandomYOffset(), state.isDoubleSided(), state.hasRandomAnimationXZOffset(),
					state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.isLodNoScale(), state.getLodPriority(), 
					tintColor, state.needsConnectionInfo(), 
					animationHandler == null ? (this.isAnimated ? this.animationHandler : null) : animationHandler);
			
			return bakedState;
		}

		@Override
		public String getDefaultTexture() {
			return model.defaultTexture;
		}

		@Override
		public boolean needsConnectionInfo() {
			return isLocationDependent;
		}
		
	}
	
	
	public BuiltInBlockState(String name, int dataVersion, BuiltInBlockStateHandler handler) {
		super(name, dataVersion, handler);
	}

}
