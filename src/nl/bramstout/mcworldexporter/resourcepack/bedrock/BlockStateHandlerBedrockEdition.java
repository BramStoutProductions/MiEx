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

package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelBone;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangExpression;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;

public class BlockStateHandlerBedrockEdition extends BlockStateHandler{

	private String name;
	private List<BlockStatePart> parts;
	private boolean _needsConnectionInfo;
	private boolean doubleSided;
	private boolean noOcclusion;
	
	public BlockStateHandlerBedrockEdition(String name, JsonObject data) {
		this.name = name;
		this.parts = new ArrayList<BlockStatePart>();
		this._needsConnectionInfo = false;
		if(data == null)
			return;
		
		this.doubleSided = Config.doubleSided.contains(name) | Config.forceDoubleSidedOnEverything;
		
		if(data.has("components")) {
			parts.add(new BlockStatePart(null, data.get("components").getAsJsonObject()));
		}
		if(data.has("permutations")) {
			JsonArray permutations = data.get("permutations").getAsJsonArray();
			for(JsonElement el : permutations.asList()) {
				JsonObject permutationObj = el.getAsJsonObject();
				if(permutationObj.has("condition")) {
					String conditionString = permutationObj.get("condition").getAsString();
					if(conditionString.contains("neighbor") || conditionString.contains("relative")) {
						this._needsConnectionInfo = true;
					}
					if(permutationObj.has("components")) {
						parts.add(new BlockStatePart(conditionString, permutationObj.get("components").getAsJsonObject()));
					}
				}
			}
		}
		
		this.noOcclusion = Config.noOcclusion.contains(name);
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, BlockState state) {
		String geometry = "minecraft:geometry.full_block";
		Map<String, MolangExpression> boneVisibility = new HashMap<String, MolangExpression>();
		Map<String, String> materialInstances = new HashMap<String, String>();
		Vector3f translation = null;
		Vector3f rotation = null;
		Vector3f rotationPivot = null;
		Vector3f scale = null;
		Vector3f scalePivot = null;
		
		MolangQuery molangQuery = new MolangQuery(name, properties, x, y, z);
		
		for(BlockStatePart part : parts) {
			MolangContext molangContext = new MolangContext(molangQuery, new Random(
					x + ((((long) y) & 0xFFFFFFFFl) << 32) + ((((long) y) & 0xFFFFFFFFl) << 16)));
			if(!part.getCondition().eval(molangContext).asBoolean(molangContext)) {
				continue;
			}
			if(part.getGeometry() != null)
				geometry = part.getGeometry();
			if(part.getBoneVisibility() != null)
				boneVisibility.putAll(part.getBoneVisibility());
			if(part.getMaterialInstances() != null)
				materialInstances.putAll(part.getMaterialInstances());
			if(part.getTranslation() != null)
				translation = part.getTranslation();
			if(part.getRotation() != null)
				rotation = part.getRotation();
			if(part.getRotationPivot() != null)
				rotationPivot = part.getRotationPivot();
			if(part.getScale() != null)
				scale = part.getScale();
			if(part.getScalePivot() != null)
				scalePivot = part.getScalePivot();
		}
		
		int modelId = ModelRegistry.getIdForName(geometry, doubleSided);
		Model model = ModelRegistry.getModel(modelId);
		
		model = new Model(model); // Make a copy of it
		
		List<ModelFace> removeFaces = new ArrayList<ModelFace>();
		for(Entry<String, MolangExpression> entry : boneVisibility.entrySet()) {
			MolangContext molangContext = new MolangContext(molangQuery, new Random(
					x + ((((long) y) & 0xFFFFFFFFl) << 32) + ((((long) y) & 0xFFFFFFFFl) << 16)));
			if(!entry.getValue().eval(molangContext).asBoolean(molangContext)) {
				// Hide this bone and thus the faces of this bone
				ModelBone bone = null;
				for(ModelBone bone2 : model.getBones()) {
					if(bone2.getName().equals(entry.getKey())) {
						bone = bone2;
						break;
					}
				}
				if(bone == null)
					continue;
				for(Integer faceId : bone.faceIds) {
					if(faceId.intValue() < 0 || faceId.intValue() >= model.getFaces().size())
						continue;
					removeFaces.add(model.getFaces().get(faceId.intValue()));
				}
			}
		}
		// We hide the faces by just removing them from this copy of the model.
		model.getFaces().removeAll(removeFaces);
		
		Map<String, String> otherMaterialInstances = ResourcePackBedrockEdition.getBlockTextureMapping(name);
		if(otherMaterialInstances != null) {
			// Check if it's overriding all textures, if so clear any face specific overrides
			// we already have.
			if(otherMaterialInstances.containsKey("*"))
				materialInstances.clear();
			materialInstances.putAll(otherMaterialInstances);
		}
		
		int variationsEncountered = 0;
		for(Entry<String, String> entry : materialInstances.entrySet()) {
			if(!entry.getValue().contains("/")) {
				List<String> paths = ResourcePackBedrockEdition.getTerrainTexture(entry.getValue());
				if(paths == null) {
					// Not a path but a variable, so add a # before it
					materialInstances.put(entry.getKey(), "#" + entry.getValue());
				}else if(paths.size() > 0){
					// Points to a path, so let's put that path in here
					// Also make sure to unmap it, in case it's a vanilla texture
					
					String path = paths.get(0);
					if(path.contains("[|]")) {
						// This path is a list of variations.
						// So let's separate it out.
						String[] variations = path.split("(\\[\\|\\])");
						// Each variation also has a weight with it.
						String[] pathVariations = new String[variations.length];
						float[] weightVariations = new float[variations.length];
						float totalWeight = 0f;
						for(int i = 0; i < variations.length; ++i) {
							String variation = variations[i];
							int sep = variation.indexOf('|');
							float weight = 1f;
							if(sep > 0) {
								try {
									weight = Float.parseFloat(variation.substring(0, sep));
								}catch(Exception ex) {}
							}
							String pathVariation = variation.substring(sep+1);
							pathVariations[i] = pathVariation;
							weightVariations[i] = weight;
							totalWeight += weight;
						}
						float random = Noise.get(x + variationsEncountered, y, z);
						variationsEncountered++;
						
						random *= totalWeight;
						
						boolean hit = false;
						for(int i = 0; i < variations.length; ++i) {
							random -= weightVariations[i];
							if(random <= 0f) {
								path = pathVariations[i];
								hit = true;
								break;
							}
						}
						if(!hit)
							path = pathVariations[0];
					}
					
					materialInstances.put(entry.getKey(), 
							TranslationRegistry.FILE_PATH_MAPPING_BEDROCK.unmap(path));
				}
			}
		}
		
		model.getTextures().putAll(materialInstances);
		
		model.applyBones();
		// Bedrock puts the origin at the bottom centre of the block,
		// while MiEx and Java Edition put it at the lower left corner.
		// So we need to translate the model to compensate.
		model.transform(Matrix.translate(8f, 0f, 8f));
		
		if(translation != null || rotation != null || scale != null) {
			if(translation == null)
				translation = new Vector3f();
			if(rotation == null)
				rotation = new Vector3f();
			if(rotationPivot == null)
				rotationPivot = new Vector3f(8f, 8f, 8f);
			if(scale == null)
				scale = new Vector3f(1f);
			if(scalePivot == null)
				scalePivot = new Vector3f(8f, 8f, 8f);
			Matrix transformMatrix = 
					Matrix.translate(translation).mult(
					Matrix.translate(rotationPivot).mult(
					Matrix.rotate(rotation).mult(
					Matrix.translate(rotationPivot.multiply(-1f)).mult(
					Matrix.translate(scalePivot).mult(
					Matrix.scale(scale).mult(
					Matrix.translate(scalePivot.multiply(-1f))))))));
			model.transform(transformMatrix);
		}
		
		if(noOcclusion) {
			for(ModelFace face : model.getFaces())
				face.noOcclusion();
		}
		
		List<List<Model>> models = new ArrayList<List<Model>>();
		List<Model> models2 = new ArrayList<Model>();
		models2.add(model);
		models.add(models2);
		
		Tint tint = state.getTint();
		TintLayers tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(name, models, state.isTransparentOcclusion(), 
									state.isLeavesOcclusion(), state.isDetailedOcclusion(), 
									state.isIndividualBlocks(), state.hasLiquid(properties), state.getLiquidName(properties),
									state.isCaveBlock(), state.hasRandomOffset(), 
									state.hasRandomYOffset(),
									state.isDoubleSided(), state.hasRandomAnimationXZOffset(), 
									state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.isLodNoScale(),
									state.getLodPriority(), tintColor, state.needsConnectionInfo(), null);
	}
	
	@Override
	public BakedBlockState getAnimatedBakedBlockState(NbtTagCompound properties, int x, int y, int z, BlockState state,
			BlockAnimationHandler animationHandler, float frame) {
		return getBakedBlockState(properties, x, y, z, state);
	}

	@Override
	public String getDefaultTexture() {
		Map<String, String> materialInstances = new HashMap<String, String>();
		for(BlockStatePart part : parts) {
			if(part.getMaterialInstances() != null)
				materialInstances.putAll(part.getMaterialInstances());
		}
		
		Map<String, String> otherMaterialInstances = ResourcePackBedrockEdition.getBlockTextureMapping(name);
		if(otherMaterialInstances != null) {
			// Check if it's overriding all textures, if so clear any face specific overrides
			// we already have.
			if(otherMaterialInstances.containsKey("*"))
				materialInstances.clear();
			materialInstances.putAll(otherMaterialInstances);
		}
		
		for(Entry<String, String> entry : materialInstances.entrySet()) {
			if(!entry.getValue().contains("/")) {
				List<String> paths = ResourcePackBedrockEdition.getTerrainTexture(entry.getValue());
				if(paths.size() > 0){
					return TranslationRegistry.FILE_PATH_MAPPING_BEDROCK.unmap(paths.get(0));
				}
			}
		}
		return "";
	}

	@Override
	public boolean needsConnectionInfo() {
		return _needsConnectionInfo;
	}
	
	public boolean hasTransparency() {
		for(BlockStatePart part : parts)
			if(part.hasTransparency())
				return true;
		return false;
	}

}
