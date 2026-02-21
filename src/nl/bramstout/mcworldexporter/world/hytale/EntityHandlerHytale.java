package nl.bramstout.mcworldexporter.world.hytale;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonString;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.export.BlendedBiome;
import nl.bramstout.mcworldexporter.export.BlendedBiome.WeightedColor;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintValue;
import nl.bramstout.mcworldexporter.resourcepack.hytale.EntityModel;
import nl.bramstout.mcworldexporter.resourcepack.hytale.ResourcePackHytale;
import nl.bramstout.mcworldexporter.translation.BlockTranslation.BlockTranslatorManager;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;

public class EntityHandlerHytale extends EntityHandler{

	private BsonDocument components;
	private float x;
	private float y;
	private float z;
	private float pitch;
	private float yaw;
	private float roll;
	private float scale;
	private float headPitch;
	private float headYaw;
	private float headRoll;
	private String id;
	
	public EntityHandlerHytale(BsonDocument data) {
		components = data.getDocument("Components", null);
		id = null;
		scale = 1f;
		
		if(components != null) {
			BsonDocument transform = components.getDocument("Transform", null);
			if(transform != null) {
				BsonDocument position = transform.getDocument("Position", null);
				if(position != null) {
					x = (float) position.getDouble("X", new BsonDouble(0.0)).doubleValue();
					y = (float) position.getDouble("Y", new BsonDouble(0.0)).doubleValue();
					z = (float) position.getDouble("Z", new BsonDouble(0.0)).doubleValue();
				}
				BsonDocument rotation = transform.getDocument("Rotation", null);
				if(rotation != null) {
					pitch = (float) rotation.getDouble("Pitch", new BsonDouble(0.0)).doubleValue();
					yaw = (float) rotation.getDouble("Yaw", new BsonDouble(0.0)).doubleValue();
					roll = (float) rotation.getDouble("Roll", new BsonDouble(0.0)).doubleValue();
					pitch = (float) Math.toDegrees(pitch);
					yaw = (float) Math.toDegrees(yaw);
					roll = (float) Math.toDegrees(roll);
					yaw = 180f - yaw;
					pitch = -pitch;
					//roll = -roll;
				}
			}
			
			BsonDocument headRotation = components.getDocument("HeadRotation", null);
			if(headRotation != null) {
				BsonDocument rotation = headRotation.getDocument("Rotation", null);
				if(rotation != null) {
					headPitch = (float) rotation.getDouble("Pitch", new BsonDouble(0.0)).doubleValue();
					headYaw = (float) rotation.getDouble("Yaw", new BsonDouble(0.0)).doubleValue();
					headRoll = (float) rotation.getDouble("Roll", new BsonDouble(0.0)).doubleValue();
					headPitch = (float) Math.toDegrees(headPitch);
					headYaw = (float) Math.toDegrees(headYaw);
					headRoll = (float) Math.toDegrees(headRoll);
					headYaw = 180f - headYaw;
					headPitch = -headPitch;
					headRoll = -headRoll;
				}
			}
			
			BsonDocument entityScale = components.getDocument("EntityScale", null);
			if(entityScale != null) {
				scale = (float) entityScale.getDouble("Scale", new BsonDouble(1.0)).doubleValue();
			}
			
			BsonDocument npc = components.getDocument("NPC", null);
			if(npc != null) {
				BsonString roleName = npc.getString("RoleName", null);
				if(roleName != null) {
					id = roleName.getValue();
				}
			}
			
			
			if(id == null && components.containsKey("BlockEntity"))
				id = "hytale:BlockEntity";
			
			if(id == null && components.containsKey("Item"))
				id = "hytale:ItemEntity";

			if(id == null && components.containsKey("Player"))
				id = "hytale:Player";
			
			if(id == null && components.containsKey("Minecart"))
				id = "hytale:Minecart";
			
			if(id == null && components.containsKey("Model"))
				id = "hytale:ModelEntity";
		}
		
		if(id == null)
			id = "hytale:entity";
		if(id.indexOf(':') == -1)
			id = "hytale:" + id;
	}
	
	public boolean isInChunk(Chunk chunk) {
		int minX = chunk.getChunkX() * 16;
		int minZ = chunk.getChunkZ() * 16;
		int maxX = minX + 16;
		int maxZ = minZ + 16;
		int x = (int) Math.floor(this.x);
		int z = (int) Math.floor(this.z);
		return x >= minX && x < maxX && z >= minZ && z < maxZ;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public void setup(Entity entity) {
		entity.setX(x);
		entity.setY(y);
		entity.setZ(z);
		entity.setPitch(0f);
		entity.setYaw(0f);
	}
	
	@Override
	public EntityAIHandler getAIHandler(Entity entity) {
		return null;
	}
	
	@Override
	public Model getModel(Entity entity) {
		Model model = new Model(id, null, true);
		
		BsonDocument blockEntity = components.getDocument("BlockEntity", null);
		if(blockEntity != null) {
			getModelBlockEntity(blockEntity, model);
		}else {
			BsonDocument modelData = components.getDocument("Model", null);
			if(modelData != null) {
				getModelModel(modelData, model);
			}else {
				BsonDocument itemData = components.getDocument("Item", null);
				if(itemData != null) {
					getModelItemEntity(itemData, model);
				}
			}
		}
		
		model.scale(scale, new Vector3f(0f, 0f, 0f));
		
		model.addRootBone();
		return model;
	}
	
	private void getModelItemEntity(BsonDocument itemData, Model model) {
		BsonDocument itemData2 = itemData.getDocument("Item", null);
		if(itemData2 == null)
			return;
		BsonString itemNameKey = itemData2.getString("Id", null);
		if(itemNameKey == null)
			return;
		String itemName = itemNameKey.getValue();
		
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_HYTALE.getTranslator(0);
		
		NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("");
		
		if(itemName.charAt(0) == '*') {
			// This is a block with a block state stored in the name,
			// so let's parse it.
			int sep = itemName.indexOf("_State_Definitions");
			String stateStr = null;
			if(sep > 0) {
				stateStr = itemName.substring(sep + 7);
				itemName = itemName.substring(1, sep);
			}else {
				itemName = itemName.substring(1);
			}
			
			if(stateStr != null) {
				sep = stateStr.indexOf('_');
				if(sep != -1) {
					String name = stateStr.substring(0, sep);
					String val = stateStr.substring(sep + 1);
					NbtTagString propTag = NbtTagString.newNonPooledInstance(name, val);
					properties.addElement(propTag);
				}
			}
		}
		
		// Make sure that we have a namespace.
		if(itemName.indexOf(':') == -1)
			itemName = "hytale:" + itemName;
		
		itemName = blockTranslatorManager.map(itemName, properties);
		
		ItemHandler itemHandler = ResourcePacks.getItemHandler(itemName, properties);
		if(itemHandler == null)
			return;
		Model model2 = itemHandler.getModel(itemName, properties, ItemHandler.DISP_CONTEXT_NONE);
		if(model2 == null)
			return;
		model.addModel(model2);
		
		model.translate(-8f, 0f, -8f);
		model.transform(Matrix.rotateX(pitch).mult(Matrix.rotateZ(roll).mult(Matrix.rotateY(yaw))));
	}
	
	private void getModelBlockEntity(BsonDocument blockEntity, Model model) {
		BsonString blockTypeKey = blockEntity.getString("BlockTypeKey", null);
		if(blockTypeKey == null)
			return;
		String blockName = blockTypeKey.getValue();
		
		BlockTranslatorManager blockTranslatorManager = TranslationRegistry.BLOCK_HYTALE.getTranslator(0);
		
		NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("");
		
		if(blockName.charAt(0) == '*') {
			// This is a block with a block state stored in the name,
			// so let's parse it.
			int sep = blockName.indexOf("_State_Definitions");
			String stateStr = null;
			if(sep > 0) {
				stateStr = blockName.substring(sep + 7);
				blockName = blockName.substring(1, sep);
			}else {
				blockName = blockName.substring(1);
			}
			
			if(stateStr != null) {
				sep = stateStr.indexOf('_');
				if(sep != -1) {
					String name = stateStr.substring(0, sep);
					String val = stateStr.substring(sep + 1);
					NbtTagString propTag = NbtTagString.newNonPooledInstance(name, val);
					properties.addElement(propTag);
				}
			}
		}
		
		// Make sure that we have a namespace.
		if(blockName.indexOf(':') == -1)
			blockName = "hytale:" + blockName;
		
		blockName = blockTranslatorManager.map(blockName, properties);
		
		Reference<char[]> charBuffer = new Reference<char[]>();
		int blockId = BlockRegistry.getIdForName(blockName, properties, Integer.MAX_VALUE, charBuffer);
		BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, (int) x, (int) y, (int) z, 0);
		
		List<Color> tints = null;
		if(state.getTint() != null) {
			int tintIndex = 0;
			TintValue tintValue = state.getTint().getLayer(tintIndex);
			if(tintValue != null) {
				int biomeId = MCWorldExporter.getApp().getWorld().getBiomeId((int) x, (int) y, (int) z);
				Biome biome = BiomeRegistry.getBiome(biomeId);
				BlendedBiome blendedBiome = new BlendedBiome();
				blendedBiome.addBiome(biome, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f);
				blendedBiome.normalise();
				WeightedColor color = tintValue.getColor(blendedBiome);
				if(color != null) {
					Color tint = color.get(0);
					if(tint != null) {
						tints = new ArrayList<Color>();
						tints.add(tint);
					}
				}
			}
		}
		
		List<Model> models = new ArrayList<Model>();
		state.getModels((int) x, (int) y, (int) z, models);
		
		for(Model model2 : models)
			model.addModel(model2, tints);
		model.translate(-8f, -8f, -8f);
		model.transform(Matrix.rotateZ(headRoll).mult(Matrix.rotateX(headPitch).mult(Matrix.rotateY(headYaw))));
		model.translate(0f, 8f, 0f);
		// Block entities seem to be half scale
		model.scale(0.5f, new Vector3f(0,0,0));
	}
	
	private void getModelModel(BsonDocument modelData, Model model) {
		BsonDocument modelData2 = modelData.getDocument("Model", null);
		if(modelData2 == null)
			return;
		String modelId = modelData2.getString("Id", new BsonString("")).getValue();
		if(modelId.indexOf(':') == -1)
			modelId = "hytale:" + modelId;
		float modelScale = (float) modelData2.getDouble("Scale", new BsonDouble(1.0)).doubleValue();
		
		// TODO: Setup attachments
		// TODO: Setup gradients
		
		EntityModel entityModel = ResourcePackHytale.getHytaleEntityModel(modelId);
		if(entityModel == null)
			return;
		
		int model2Id = ModelRegistry.getIdForName(entityModel.getModel(), model.isDoubleSided());
		Model model2 = ModelRegistry.getModel(model2Id);
		if(model2 == null)
			return;
		// Make sure to make a copy of it, so that we can edit it.
		model2 = new Model(model2);
		
		float textureWidth = 32f;
		float textureHeight = 32f;
		
		File textureFile = ResourcePacks.getTexture(entityModel.getTexture());
		if(textureFile != null) {
			long textureSize = FileUtil.getImageSize(textureFile);
			textureWidth = (float) (textureSize >> 32);
			textureHeight = (float) (textureSize & 0xFFFFFFFFL);
		}
		float texScaleU = 16f / textureWidth;
		float texScaleV = 16f / textureHeight;
		
		model2.addTexture("#north", entityModel.getTexture());
		model2.addTexture("#south", entityModel.getTexture());
		model2.addTexture("#west", entityModel.getTexture());
		model2.addTexture("#east", entityModel.getTexture());
		model2.addTexture("#up", entityModel.getTexture());
		model2.addTexture("#down", entityModel.getTexture());
		
		for(ModelFace face : model2.getFaces()) {
			// UVs for custom models are based on the texture resolution,
			// but since we don't know what the texture is when loading in
			// the model, we needed to defer it to now. So, now we can update
			// the UVs to take into account the texture resolution.
			face.getUVs()[0] = face.getUVs()[0] * texScaleU;
			face.getUVs()[1] = 16f - face.getUVs()[1] * texScaleV;
			face.getUVs()[2] = face.getUVs()[2] * texScaleU;
			face.getUVs()[3] = 16f - face.getUVs()[3] * texScaleV;
			face.getUVs()[4] = face.getUVs()[4] * texScaleU;
			face.getUVs()[5] = 16f - face.getUVs()[5] * texScaleV;
			face.getUVs()[6] = face.getUVs()[6] * texScaleU;
			face.getUVs()[7] = 16f - face.getUVs()[7] * texScaleV;
		}
		// Block models are from (0,0,0) to (16,16,16)
		// but entity models should be from (-8,0,-8) to (8, 16, 8)
		// So translate to make that work.
		model2.translate(-8f, 0f, -8f);
		model2.scale(modelScale, new Vector3f(0,0,0));
		
		model.addModel(model2);
		model.transform(Matrix.rotateX(pitch).mult(Matrix.rotateZ(roll).mult(Matrix.rotateY(yaw))));
	}

}
