package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Random;
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
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
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
		
		this.doubleSided = Config.doubleSided.contains(name);
		
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
		
		for(Entry<String, String> entry : materialInstances.entrySet()) {
			if(!entry.getValue().contains("/")) {
				List<String> paths = ResourcePackBedrockEdition.getTerrainTexture(entry.getValue());
				if(paths == null) {
					// Not a path but a variable, so add a # before it
					materialInstances.put(entry.getKey(), "#" + entry.getValue());
				}else if(paths.size() > 0){
					// Points to a path, so let's put that path in here
					// Also make sure to unmap it, in case it's a vanilla texture
					materialInstances.put(entry.getKey(), 
							TranslationRegistry.FILE_PATH_MAPPING_BEDROCK.unmap(paths.get(0)));
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
		
		Tint tint = Tints.getTint(state.getName());
		Color tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(name, models, state.isTransparentOcclusion(), 
									state.isLeavesOcclusion(), state.isDetailedOcclusion(), 
									state.isIndividualBlocks(), state.hasLiquid(properties), 
									state.isCaveBlock(), state.hasRandomOffset(), 
									state.hasRandomYOffset(), state.isGrassColormap(), 
									state.isFoliageColormap(), state.isWaterColormap(), 
									state.isDoubleSided(), state.hasRandomAnimationXZOffset(), 
									state.hasRandomAnimationYOffset(), state.isLodNoUVScale(),
									state.getLodPriority(), tintColor, state.needsConnectionInfo());
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
