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

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelBone;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelLocator;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.molang.MolangValue;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangObject;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationState;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;

public class EntityHandlerBedrockEdition extends EntityHandler{

	private String name;
	@SuppressWarnings("unused")
	private JsonObject data;
	private Map<String, String> textures;
	private Map<String, String> geometry;
	private Map<String, String> materials;
	private Map<String, MolangScript> renderControllers;
	@SuppressWarnings("unused")
	private boolean enableAttachables;
	private Map<String, String> animations;
	private AnimationController animationController;
	private List<MolangScript> initMolang;
	private MolangScript scaleXExpression;
	private MolangScript scaleYExpression;
	private MolangScript scaleZExpression;
	
	public EntityHandlerBedrockEdition(String name, JsonObject data) {
		this.name = name;
		this.data = data;
		
		this.textures = new HashMap<String, String>();
		this.geometry = new HashMap<String, String>();
		this.materials = new HashMap<String, String>();
		this.renderControllers = new HashMap<String, MolangScript>();
		this.enableAttachables = false;
		animations = new HashMap<String, String>();
		animationController = null;
		this.initMolang = new ArrayList<MolangScript>();
		scaleXExpression = MolangParser.parse("1.0");
		scaleYExpression = MolangParser.parse("1.0");
		scaleZExpression = MolangParser.parse("1.0");
		
		if(data != null) {
			JsonObject description = data.getAsJsonObject("description");
			if(description.has("textures")) {
				JsonObject textures = description.getAsJsonObject("textures");
				for(Entry<String, JsonElement> entry : textures.entrySet()) {
					this.textures.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			if(description.has("materials")) {
				JsonObject materials = description.getAsJsonObject("materials");
				for(Entry<String, JsonElement> entry : materials.entrySet()) {
					this.materials.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			if(description.has("geometry")) {
				JsonObject geometry = description.getAsJsonObject("geometry");
				for(Entry<String, JsonElement> entry : geometry.entrySet()) {
					String geometryName = entry.getValue().getAsString();
					if(!geometryName.contains(":"))
						geometryName = "minecraft:" + geometryName;
					this.geometry.put(entry.getKey(), geometryName);
				}
			}
			if(description.has("render_controllers")) {
				JsonArray renderControllers = description.getAsJsonArray("render_controllers");
				for(JsonElement el : renderControllers.asList()) {
					if(el.isJsonPrimitive()) {
						this.renderControllers.put(el.getAsString(), MolangParser.parse("true"));
					}else if(el.isJsonObject()) {
						for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
							this.renderControllers.put(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
						}
					}
				}
			}
			if(description.has("enable_attachables")) {
				this.enableAttachables = description.get("enable_attachables").getAsBoolean();
			}
			if(description.has("animations")) {
				for(Entry<String, JsonElement> entry : description.getAsJsonObject("animations").entrySet()) {
					animations.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			if(description.has("scripts")) {
				JsonObject obj = description.getAsJsonObject("scripts");
				if(obj.has("animate")) {
					animationController = new AnimationController("");
					AnimationState defaultState = new AnimationState("default", 0f);
					for(JsonElement el : obj.getAsJsonArray("animate").asList()) {
						if(el.isJsonPrimitive())
							defaultState.addAnimation(el.getAsString(), MolangParser.parse("1.0"));
						else if(el.isJsonObject()) {
							for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
								defaultState.addAnimation(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
							}
						}
					}
					animationController.getStates().add(defaultState);
				}
				if(obj.has("pre_animation")) {
					if(animationController == null)
						animationController = new AnimationController("");
					for(JsonElement el : obj.getAsJsonArray("pre_animation").asList()) {
						animationController.getPreAnimationScripts().add(MolangParser.parse(el.getAsString()));
					}
				}
				if(obj.has("initialize")) {
					for(JsonElement el : obj.getAsJsonArray("initialize").asList()) {
						initMolang.add(MolangParser.parse(el.getAsString()));
					}
				}
				if(obj.has("scale")) {
					scaleXExpression = MolangParser.parse(obj.get("scale").getAsString());
					scaleYExpression = scaleXExpression;
					scaleZExpression = scaleXExpression;
				}
				if(obj.has("scaleX")) {
					scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
				}
				if(obj.has("scaleY")) {
					scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
				}
				if(obj.has("scaleZ")) {
					scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
				}
			}
			if(description.has("animation_controllers")) {
				AnimationState defaultState = null;
				if(animationController == null) {
					animationController = new AnimationController("");
				}
				
				if(animationController.getStates().isEmpty()) {
					defaultState = new AnimationState("default", 0f);
					animationController.getStates().add(defaultState);
				}else
					defaultState = animationController.getStates().get(0);
				
				for(JsonElement el : description.getAsJsonArray("animation_controllers").asList()) {
					JsonObject obj = el.getAsJsonObject();
					for(Entry<String, JsonElement> entry : obj.entrySet()) {
						animations.put("miex_animation_controller_" + entry.getKey(), entry.getValue().getAsString());
						defaultState.addAnimation("miex_animation_controller_" + entry.getKey(), MolangParser.parse("1.0"));
					}
				}
			}
		}
	}
	
	@Override
	public Model getModel(Entity entity) {
		Map<String, MolangValue> globals = new HashMap<String, MolangValue>();
		MolangObject materialsObj = new MolangObject();
		for(Entry<String, String> entry : materials.entrySet()) {
			materialsObj.getFields().put(entry.getKey().toLowerCase(), new MolangValue(entry.getValue()));
		}
		globals.put("material", new MolangValue(materialsObj));
		
		MolangObject texturesObj = new MolangObject();
		for(Entry<String, String> entry : textures.entrySet()) {
			texturesObj.getFields().put(entry.getKey().toLowerCase(), new MolangValue(entry.getValue()));
		}
		globals.put("texture", new MolangValue(texturesObj));
		
		MolangObject geometryObj = new MolangObject();
		for(Entry<String, String> entry : geometry.entrySet()) {
			geometryObj.getFields().put(entry.getKey().toLowerCase(), new MolangValue(entry.getValue()));
		}
		globals.put("geometry", new MolangValue(geometryObj));
		
		NbtTagCompound entityProperties = (NbtTagCompound) entity.getProperties().copy();
		entityProperties.addElement(NbtTagFloat.newNonPooledInstance("Pitch", entity.getPitch()));
		entityProperties.addElement(NbtTagFloat.newNonPooledInstance("Yaw", entity.getYaw()));
		entityProperties.addElement(NbtTagFloat.newNonPooledInstance("HeadPitch", entity.getHeadPitch()));
		entityProperties.addElement(NbtTagFloat.newNonPooledInstance("HeadYaw", entity.getHeadYaw()));
		entityProperties.addElement(NbtTagList.newNonPooledInstance("Motion", new NbtTag[] {
				NbtTagFloat.newNonPooledInstance("dx", entity.getDx()), 
				NbtTagFloat.newNonPooledInstance("dy", entity.getDy()), 
				NbtTagFloat.newNonPooledInstance("dz", entity.getDz())}));
		MolangQuery molangQuery = new MolangQuery(name, entityProperties, entity.getX(), entity.getY(), entity.getZ());
		MolangContext molangContext = new MolangContext(molangQuery, entity.getRandom());
		
		Model model = null;
		
		int renderControllerCounter = 0;
		
		for(Entry<String, MolangScript> renderControllerEntry : this.renderControllers.entrySet()) {
			if(!renderControllerEntry.getValue().eval(molangContext).asBoolean(molangContext))
				continue;
			RenderControllerBedrockEdition renderController = ResourcePackBedrockEdition.getRenderController(renderControllerEntry.getKey());
			if(renderController == null)
				continue;
			String geometryName = renderController.getGeometry(molangQuery, entity.getVariables(), globals, entity.getRandom());
			List<String> textureNames = renderController.getTextures(molangQuery, entity.getVariables(), globals, entity.getRandom());
			Map<String, String> materialNames = renderController.getMaterials(molangQuery, entity.getVariables(), globals, entity.getRandom());
			List<Color> tints = renderController.getTints(molangQuery, entity.getVariables(), globals, entity.getRandom());
			
			for(int i = 0; i < textureNames.size(); ++i) {
				textureNames.set(i, TranslationRegistry.FILE_PATH_MAPPING_BEDROCK.unmap(textureNames.get(i)));
			}
			
			int modelId = ModelRegistry.getIdForName(geometryName, true);
			Model model2 = ModelRegistry.getModel(modelId);
			if(model2 == null)
				continue;
			
			if(model == null) {
				model = new Model(model2);
				for(Entry<String, String> material : materialNames.entrySet()) {
					model.getTextures().put(material.getKey(), BedrockMaterials.getTexture(material.getValue(), textureNames, tints));
				}
			}else{
				// Combine the models.
				String texPrefix = "#miex_" + renderControllerCounter + "_";
				for(Entry<String, String> material : materialNames.entrySet()) {
					model.getTextures().put(texPrefix + material.getKey(), 
							BedrockMaterials.getTexture(material.getValue(), textureNames, tints));
				}
				int faceIndexOffset = model.getFaces().size();
				for(ModelFace face : model2.getFaces()) {
					ModelFace face2 = new ModelFace(face);
					if(materialNames.containsKey(face2.getTexture())) {
						face2.setTexture(texPrefix + face2.getTexture());
					}else {
						// Default to the '*' material
						face2.setTexture(texPrefix + "*");
					}
					model.getFaces().add(face2);
				}
				List<ModelBone> reparents = new ArrayList<ModelBone>();
				for(ModelBone bone : model2.getBones()) {
					ModelBone bone2 = model.getBone(bone.getName());
					if(bone2 != null) {
						for(Integer faceIndex : bone.faceIds) {
							bone2.faceIds.add(faceIndex.intValue() + faceIndexOffset);
						}
					}else {
						bone2 = new ModelBone(bone);
						for(int i = 0; i < bone2.faceIds.size(); ++i) {
							bone2.faceIds.set(i, bone2.faceIds.get(i).intValue() + faceIndexOffset);
						}
						model.getBones().add(bone2);
						if(bone2.getParent() != null)
							reparents.add(bone2);
					}
				}
				for(ModelBone bone : reparents) {
					ModelBone parentBone = model.getBone(bone.getParent().getName());
					bone.setParent(parentBone);
				}
				for(ModelLocator locator : model2.getLocators()) {
					ModelLocator locator2 = model.getLocator(locator.getName());
					if(locator2 == null) {
						locator2 = new ModelLocator(locator);
						if(locator2.bone != null) {
							locator2.bone = model.getBone(locator2.bone.getName());
						}
						model.getLocators().add(locator2);
					}
				}
			}
			
			renderControllerCounter++;
		}
		
		if(model == null)
			return null;
		
		// Entities in Bedrock are rotates by 180 degrees compared to in MiEx.
		model.transform(Matrix.rotateY(180f));
		
		return model;
	}

	@Override
	public void setup(Entity entity) {
		NbtTag posTag = entity.getProperties().get("Pos");
		if(posTag != null && posTag instanceof NbtTagList) {
			NbtTagList posList = (NbtTagList) posTag;
			entity.setX(posList.get(0).asFloat());
			entity.setY(posList.get(1).asFloat());
			entity.setZ(posList.get(2).asFloat());
		}
		
		NbtTag rotationTag = entity.getProperties().get("Rotation");
		if(rotationTag != null && rotationTag instanceof NbtTagList) {
			NbtTagList rotationList = (NbtTagList) rotationTag;
			entity.setYaw(rotationList.get(0).asFloat());
			entity.setPitch(rotationList.get(1).asFloat());
		}
		NbtTag uniqueIdTag = entity.getProperties().get("UniqueID");
		if(uniqueIdTag != null)
			entity.setUniqueId(uniqueIdTag.asLong());
		NbtTagIntArray uuidTag = (NbtTagIntArray) entity.getProperties().get("UUID");
		if(uuidTag != null) {
			if(uuidTag.getData().length >= 4) {
				long val1 = ((long) uuidTag.getData()[0]) & 0xFFFFFFFFl;
				long val2 = ((long) uuidTag.getData()[1]) & 0xFFFFFFFFl;
				long val3 = ((long) uuidTag.getData()[2]) & 0xFFFFFFFFl;
				long val4 = ((long) uuidTag.getData()[3]) & 0xFFFFFFFFl;
				val1 |= val2 << 32;
				val3 |= val4 << 32;
				entity.setUniqueId(val1 + val3);
			}
		}
		
		entity.setAnimationController(animationController);
		for(Entry<String, String> entry : animations.entrySet()) {
			Animation animation = ResourcePacks.getAnimation(entry.getValue());
			if(animation != null)
				entity.getAnimations().put(entry.getKey(), animation);
		}
		entity.setInitMolangScripts(initMolang);
		entity.setScaleXExpression(scaleXExpression);
		entity.setScaleYExpression(scaleYExpression);
		entity.setScaleZExpression(scaleZExpression);
	}

	@Override
	public EntityAIHandler getAIHandler(Entity entity) {
		return ResourcePacks.getEntityAIHandler(name);
	}

}
