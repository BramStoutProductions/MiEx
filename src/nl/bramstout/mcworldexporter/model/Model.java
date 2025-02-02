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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class Model {

	private String name;
	protected int id;
	protected Model parentModel;
	private int weight;
	private long occludes;
	protected String extraData;
	protected boolean doubleSided;
	protected String defaultTexture;
	protected Vector3f itemFrameTranslation;
	protected Vector3f itemFrameRotation;
	protected Vector3f itemFrameScale;

	protected Map<String, String> textures;
	protected List<ModelFace> faces;
	protected List<ModelBone> bones;
	protected List<ModelLocator> locators;

	public Model(Model other) {
		this.name = other.name;
		this.id = other.id;
		this.parentModel = other.parentModel;
		this.textures = new HashMap<String, String>(other.textures);
		this.faces = new ArrayList<ModelFace>();
		this.weight = other.weight;
		this.occludes = other.occludes;
		this.extraData = other.extraData;
		this.defaultTexture = other.defaultTexture;
		this.itemFrameTranslation = new Vector3f(other.itemFrameTranslation);
		this.itemFrameRotation = new Vector3f(other.itemFrameRotation);
		this.itemFrameScale = new Vector3f(other.itemFrameScale);
		for (int i = 0; i < other.faces.size(); ++i) {
			this.faces.add(new ModelFace(other.faces.get(i)));
		}
		this.bones = new ArrayList<ModelBone>();
		for(int i = 0; i < other.bones.size(); ++i) {
			this.bones.add(new ModelBone(other.bones.get(i)));
		}
		// Make sure to update the parent references.
		ModelBone bone = null;
		for(int i = 0; i < this.bones.size(); ++i) {
			bone = this.bones.get(i);
			if(bone.getParent() != null) {
				String parentName = bone.getParent().getName();
				ModelBone parent = getBone(parentName);
				bone.setParent(parent);
			}
		}
		this.locators = new ArrayList<ModelLocator>();
		for(int i = 0; i < other.locators.size(); ++i)
			this.locators.add(new ModelLocator(other.locators.get(i)));
		// Make sure to update the bone references
		ModelLocator locator = null;
		for(int i = 0; i < this.locators.size(); ++i) {
			locator = this.locators.get(i);
			if(locator.bone != null) {
				bone = getBone(locator.bone.getName());
				locator.bone = bone;
			}
		}
	}

	public Model(String name, ModelHandler handler, boolean doubleSided) {
		this.name = name;
		this.parentModel = null;
		this.textures = new HashMap<String, String>();
		this.faces = new ArrayList<ModelFace>();
		this.bones = new ArrayList<ModelBone>();
		this.locators = new ArrayList<ModelLocator>();
		this.weight = 1;
		this.occludes = 0;
		this.extraData = "";
		this.doubleSided = doubleSided;
		this.defaultTexture = null;
		this.itemFrameTranslation = new Vector3f(0f, 0f, 0f);
		this.itemFrameRotation = new Vector3f(0f, 0f, 0f);
		this.itemFrameScale = new Vector3f(0.25f, 0.25f, 0.25f);

		this.id = ModelRegistry.getNextId(this);

		if(handler != null)
			handler.getGeometry(this);
		
		occludes = 0;
		for(int i = 0; i < faces.size(); ++i)
			occludes |= faces.get(i).getOccludes();

	}
	
	/**
	 * Some built in models change based on settings
	 * specified by higher level models (like ModelItemGenerated).
	 * To properly handle it, this function gets called.
	 * Normally, it does nothing, but in the event that it's
	 * important, we can call it.
	 * @param topLevelModel
	 * @return
	 */
	public Model postConstruct(Model topLevelModel) {
		if(parentModel != null) {
			Model parentPostConstruct = parentModel.postConstruct(topLevelModel);
			if(parentPostConstruct != parentModel)
				return parentPostConstruct;
		}
		return this;
	}
	
	public String getDefaultTexture() {
		if(defaultTexture != null)
			return defaultTexture;
		if(textures.size() == 0)
			defaultTexture = "";
		else if(faces.size() == 0) {
			String key = (String) textures.keySet().iterator().next();
			defaultTexture = getTexture(key);
		}else {
			ModelFace face = faces.get(0);
			for(ModelFace face2 : faces) {
				if(face2.getDirection() == Direction.UP) {
					face = face2;
					break;
				}
			}
			defaultTexture = getTexture(face.getTexture());
		}
		return defaultTexture;
	}

	public String getTexture(String name) {
		String path = textures.get(name);
		if (path == null)
			return textures.getOrDefault("*", "");
		if (path.startsWith("#"))
			return getTexture(path);
		return path;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public List<ModelFace> getFaces() {
		return faces;
	}
	
	public List<ModelBone> getBones(){
		return bones;
	}
	
	public ModelBone getBone(String name) {
		for(ModelBone bone : bones)
			if(bone.getName().equalsIgnoreCase(name))
				return bone;
		return null;
	}
	
	public List<ModelLocator> getLocators(){
		return locators;
	}
	
	public ModelLocator getLocator(String name) {
		for(ModelLocator locator : locators)
			if(locator.getName().equalsIgnoreCase(name))
				return locator;
		return null;
	}
	
	public Map<String, String> getTextures(){
		return textures;
	}
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	public String getExtraData() {
		return extraData;
	}
	
	public Vector3f getItemFrameTranslation() {
		return itemFrameTranslation;
	}
	
	public Vector3f getItemFrameRotation() {
		return itemFrameRotation;
	}
	
	public Vector3f getItemFrameScale() {
		return itemFrameScale;
	}

	public float[] getBoundingBox() {
		float[] res = new float[] { 0f, 0f, 0f, 0f, 0f, 0f};
		for(ModelFace face : faces) {
			for(int i = 0; i < 12; i += 3) {
				res[0] = Math.min(res[0], face.getPoints()[i]);
				res[1] = Math.min(res[1], face.getPoints()[i + 1]);
				res[2] = Math.min(res[2], face.getPoints()[i + 2]);
				
				res[3] = Math.max(res[3], face.getPoints()[i]);
				res[4] = Math.max(res[4], face.getPoints()[i + 1]);
				res[5] = Math.max(res[5], face.getPoints()[i + 2]);
			}
		}
		return res;
	}
	
	public void setItemFrameTransform(Vector3f translation, Vector3f rotation, Vector3f scale) {
		this.itemFrameTranslation = translation;
		this.itemFrameRotation = rotation;
		this.itemFrameScale = scale;
	}
	
	/**
	 * Applies the transformation set on the bones
	 */
	public void applyBones() {
		this.occludes = 0;
		for(ModelBone bone : bones) {
			Matrix transformation = bone.getMatrix();
			for(Integer faceId : bone.faceIds) {
				int faceIdI = faceId.intValue();
				if(faceIdI < 0 || faceIdI >= faces.size())
					continue;
				faces.get(faceIdI).transform(transformation);
			}
		}
		for(ModelFace face : faces)
			this.occludes |= face.getOccludes();
	}
	
	public void applyItemFrameTransformation() {
		scale(itemFrameScale.x, itemFrameScale.y, itemFrameScale.z);
		translate(itemFrameTranslation.x, itemFrameTranslation.y, itemFrameTranslation.z);
		rotate(itemFrameRotation.x, itemFrameRotation.y, itemFrameRotation.z);
	}
	
	public void transform(Matrix transformationMatrix) {
		this.occludes = 0;
		for (ModelFace face : faces) {
			face.transform(transformationMatrix);
			this.occludes |= face.getOccludes();
		}
	}
	
	public void rotate(float rotateX, float rotateY, boolean uvLock) {
		this.occludes = 0;
		for (ModelFace face : faces) {
			face.rotate(rotateX, rotateY, uvLock);
			this.occludes |= face.getOccludes();
		}
	}
	
	public void rotate(float rotateX, float rotateY, float rotateZ) {
		this.occludes = 0;
		for (ModelFace face : faces) {
			face.rotate(rotateX, rotateY, rotateZ);
			this.occludes |= face.getOccludes();
		}
	}
	
	public void scale(float scale) {
		this.occludes = 0;
		for(ModelFace face : faces)
			face.scale(scale);
	}
	
	public void scale(float scaleX, float scaleY, float scaleZ) {
		this.occludes = 0;
		for(ModelFace face : faces)
			face.scale(scaleX, scaleY, scaleZ);
	}
	
	public void flip(boolean x, boolean y, boolean z) {
		this.occludes = 0;
		for(ModelFace face : faces) {
			face.flip(x, y, z);
			this.occludes |= face.getOccludes();
		}
	}
	
	public void translate(float x, float y, float z) {
		this.occludes = 0;
		for(ModelFace face : faces)
			face.translate(x, y, z);
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public long getOccludes() {
		return occludes;
	}
	
	public boolean isDoubleSided() {
		return doubleSided;
	}

	public void addTexture(String name, String value) {
		textures.put(name, value);
	}
	
	public void addModel(Model other) {
		String texPrefix = "#" + Integer.toHexString(other.getId()) + "_";
		for(Entry<String, String> entry : other.getTextures().entrySet()) {
			String tex = entry.getValue();
			if(tex.startsWith("#"))
				tex = texPrefix + tex.substring(1);
			textures.put(texPrefix + entry.getKey().substring(1), tex);
		}
		for(ModelFace face : other.getFaces()) {
			ModelFace copy = new ModelFace(face);
			if(face.getTexture().startsWith("#")) {
				copy.setTexture(texPrefix + face.getTexture().substring(1));
			}
			faces.add(copy);
		}
	}
	
	public void addRootBone() {
		if(!bones.isEmpty())
			return;
		ModelBone bone = new ModelBone("root");
		for(int i = 0; i < faces.size(); ++i)
			bone.faceIds.add(i);
		bones.add(bone);
	}

	public ModelFace addFace(float[] minMaxPoints, Direction dir, String texture) {
		JsonObject faceData = new JsonObject();
		faceData.addProperty("texture", texture);

		ModelFace modelFace = new ModelFace(minMaxPoints, dir, faceData, doubleSided);
		if (modelFace.isValid()) {
			this.occludes |= modelFace.getOccludes();
			faces.add(modelFace);
		}
		return modelFace;
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, -1);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, int tintIndex) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, tintIndex);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
							float rotX, float rotY) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, rotX, rotY, 0f, -1);
	}

	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float rotX, float rotY, int tintIndex) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, rotX, rotY, 0f, tintIndex);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float uvRot, int tintIndex) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, uvRot, tintIndex);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float rotX, float rotY, float uvRot, int tintIndex) {
		JsonObject faceData = new JsonObject();
		faceData.addProperty("texture", texture);
		faceData.addProperty("tintindex", tintIndex);
		JsonArray uvData = new JsonArray();
		for (int i = 0; i < minMaxUVs.length; ++i)
			uvData.add(minMaxUVs[i]);
		faceData.add("uv", uvData);
		if(uvRot != 0f) {
			faceData.addProperty("rotation", uvRot);
			faceData.addProperty("rotationMiEx", true);
		}

		ModelFace modelFace = new ModelFace(minMaxPoints, dir, faceData, doubleSided);
		if (modelFace.isValid()) {
			if(rotX != 0f || rotY != 0f)
				modelFace.rotate(rotX, rotY, false);
			this.occludes |= modelFace.getOccludes();
			faces.add(modelFace);
		}
		return modelFace;
	}
	
	public void addEntityCube(float[] minMaxPoints, float[] minMaxUVs, String texture) {
		addEntityCube(minMaxPoints, minMaxUVs, texture, 0f, 0f, Direction.CACHED_VALUES);
	}

	public void addEntityCube(float[] minMaxPoints, float[] minMaxUVs, String texture, Direction... directions) {
		addEntityCube(minMaxPoints, minMaxUVs, texture, 0f, 0f, directions);
	}

	public void addEntityCube(float[] minMaxPoints, float[] minMaxUVs, String texture, float rotX, float rotY) {
		addEntityCube(minMaxPoints, minMaxUVs, texture, rotX, rotY, Direction.CACHED_VALUES);
	}

	public void addEntityCube(float[] minMaxPoints, float[] minMaxUVs, String texture, float rotX, float rotY, 
			Direction... directions) {
		float width = Math.abs(minMaxPoints[3] - minMaxPoints[0]);
		float height = Math.abs(minMaxPoints[4] - minMaxPoints[1]);
		float depth = Math.abs(minMaxPoints[5] - minMaxPoints[2]);
		float uvWidth = minMaxUVs[2] - minMaxUVs[0];
		float uvHeight = minMaxUVs[3] - minMaxUVs[1];
		uvWidth /= (depth + width + depth + width);
		uvHeight /= (depth + height);
		float uvX = 0;
		float uvY = 0;
		float uvW = 0;
		float uvH = 0;

		List<Direction> directionsList = Arrays.asList(directions);

		if (directionsList.contains(Direction.UP)) {
			// TOP
			uvX = (depth) * uvWidth + minMaxUVs[0];
			uvY = (0) * uvHeight + minMaxUVs[1];
			uvW = width * uvWidth;
			uvH = depth * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.UP, texture, rotX, rotY);
		}

		if (directionsList.contains(Direction.DOWN)) {
			// BOTTOM
			uvX = (depth + width) * uvWidth + minMaxUVs[0];
			uvY = (0) * uvHeight + minMaxUVs[1];
			uvW = width * uvWidth;
			uvH = depth * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY + uvH, uvX + uvW, uvY }, Direction.DOWN, texture, rotX, rotY);
		}

		if (directionsList.contains(Direction.WEST)) {
			// WEST
			uvX = (0) * uvWidth + minMaxUVs[0];
			uvY = (depth) * uvHeight + minMaxUVs[1];
			uvW = depth * uvWidth;
			uvH = height * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.WEST, texture, rotX, rotY);
		}

		if (directionsList.contains(Direction.SOUTH)) {
			// SOUTH
			uvX = (depth) * uvWidth + minMaxUVs[0];
			uvY = (depth) * uvHeight + minMaxUVs[1];
			uvW = width * uvWidth;
			uvH = height * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.SOUTH, texture, rotX, rotY);
		}

		if (directionsList.contains(Direction.EAST)) {
			// EAST
			uvX = (depth + width) * uvWidth + minMaxUVs[0];
			uvY = (depth) * uvHeight + minMaxUVs[1];
			uvW = depth * uvWidth;
			uvH = height * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.EAST, texture, rotX, rotY);
		}

		if (directionsList.contains(Direction.NORTH)) {
			// NORTH
			uvX = (depth + width + depth) * uvWidth + minMaxUVs[0];
			uvY = (depth) * uvHeight + minMaxUVs[1];
			uvW = width * uvWidth;
			uvH = height * uvHeight;
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.NORTH, texture, rotX, rotY);
		}
	}

	public void calculateOccludes() {
		occludes = 0;
		for(int i = 0; i < faces.size(); ++i)
			occludes |= faces.get(i).getOccludes();
	}

	public void setParentModel(Model parentModel) {
		this.parentModel = parentModel;
	}

}
