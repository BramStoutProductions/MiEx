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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.LongKeyMap;
import nl.bramstout.mcworldexporter.export.IntArray;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.BlockState.DefaultTexture;
import nl.bramstout.mcworldexporter.model.ModelFace.FaceData;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class Model {

	private String name;
	protected int id;
	protected Model parentModel;
	private float weight;
	private long occludes;
	protected String extraData;
	protected boolean doubleSided;
	protected DefaultTexture defaultTexture;
	protected Map<String, Matrix> displayTransforms;
	private ModelHandler handler;
	private boolean animatesTopology;
	private boolean animatesPoints;
	private boolean animatesUVs;
	private boolean animatesVertexColors;
	private IntArray newFaces;
	protected boolean moveable;
	protected ModelSource source;
	protected String animation;

	protected LongKeyMap<String> textures;
	protected List<ModelFace> faces;
	protected List<ModelBone> bones;
	protected List<ModelLocator> locators;

	public Model(Model other) {
		this.name = other.name;
		this.id = other.id;
		this.parentModel = other.parentModel;
		this.textures = new LongKeyMap<String>(other.textures);
		this.faces = new ArrayList<ModelFace>();
		this.weight = other.weight;
		this.occludes = other.occludes;
		this.extraData = other.extraData;
		this.defaultTexture = other.defaultTexture;
		this.source = other.source;
		this.animation = other.animation;
		this.displayTransforms = new HashMap<String, Matrix>(other.displayTransforms);
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
		
		this.newFaces = null;
		if(other.newFaces != null)
			this.newFaces = new IntArray(other.newFaces);
		
		this.moveable = true;
	}

	public Model(String name, ModelHandler handler, boolean doubleSided, boolean storeInRegistry) {
		this.name = name;
		this.parentModel = null;
		this.textures = new LongKeyMap<String>();
		this.faces = new ArrayList<ModelFace>();
		this.bones = new ArrayList<ModelBone>();
		this.locators = new ArrayList<ModelLocator>();
		this.weight = 1;
		this.occludes = 0;
		this.extraData = "";
		this.doubleSided = doubleSided;
		this.defaultTexture = null;
		this.displayTransforms = new HashMap<String, Matrix>();
		this.source = ModelSource.UNDEFINED;
		this.handler = handler;
		this.newFaces = null;
		this.animation = null;
		
		if(storeInRegistry)
			this.id = ModelRegistry.getNextId(this);
		else
			this.id = ModelRegistry.getNextUniqueId();

		if(handler != null)
			handler.getGeometry(this);
		
		occludes = 0;
		for(int i = 0; i < faces.size(); ++i)
			occludes |= faces.get(i).getOccludes();

		this.moveable = true;
	}
	
	public Model(String name, ModelHandler handler, boolean doubleSided, BlockAnimationHandler animationHandler, float frame, int id) {
		this.name = name;
		this.parentModel = null;
		this.textures = new LongKeyMap<String>();
		this.faces = new ArrayList<ModelFace>();
		this.bones = new ArrayList<ModelBone>();
		this.locators = new ArrayList<ModelLocator>();
		this.weight = 1;
		this.occludes = 0;
		this.extraData = "";
		this.doubleSided = doubleSided;
		this.defaultTexture = null;
		this.displayTransforms = new HashMap<String, Matrix>();
		this.handler = handler;
		this.source = ModelSource.UNDEFINED;
		this.animation = null;

		this.id = id;

		if(handler != null)
			handler.getGeometry(this, animationHandler, frame);
		
		occludes = 0;
		for(int i = 0; i < faces.size(); ++i)
			occludes |= faces.get(i).getOccludes();

		this.moveable = true;
	}
	
	public void setImmoveable() {
		this.moveable = false;
	}
	
	public Model getAnimatedVersion(BlockAnimationHandler animationHandler, float frame) {
		return new Model(name, handler, doubleSided, animationHandler, frame, this.id);
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
	
	public DefaultTexture getDefaultTexture() {
		if(defaultTexture != null)
			return defaultTexture;
		if(textures.size() == 0)
			defaultTexture = new DefaultTexture("", false);
		else if(faces.size() == 0) {
			long key = textures.size() > 0 ? textures.getKey(0) : 0;
			defaultTexture = new DefaultTexture(getTexture(key), true);
		}else {
			ModelFace face = faces.get(0);
			for(ModelFace face2 : faces) {
				if(face2.getDirection() == Direction.UP) {
					face = face2;
					break;
				}
			}
			defaultTexture = new DefaultTexture(getTexture(face.getTexture()), face.getTintIndex() >= 0);
		}
		return defaultTexture;
	}

	public String getTexture(long name) {
		int texIndex = textures.getIndex(name);
		if (texIndex == -1) {
			texIndex = textures.getIndex(TextureRegistry.ASTERISK);
			if(texIndex == -1)
				return TextureRegistry.getTextureFromId(name);
			long texLong = textures.getLongValue(texIndex);
			if(texLong != 0)
				return getTexture(texLong);
			String texStr = textures.getObjValue(texIndex);
			return texStr;
		}
		long texLong = textures.getLongValue(texIndex);
		if(texLong != 0)
			return getTexture(texLong);
		String texStr = textures.getObjValue(texIndex);
		return texStr;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}
	
	public ModelSource getSource() {
		return source;
	}
	
	public void setModelSource(ModelSource source) {
		this.source = source;
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
	
	public LongKeyMap<String> getTextures(){
		return textures;
	}
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	public String getExtraData() {
		return extraData;
	}
	
	public Map<String, Matrix> getDisplayTransforms(){
		return displayTransforms;
	}

	public boolean isAnimatesTopology() {
		return animatesTopology;
	}

	public void setAnimatesTopology(boolean animatesTopology) {
		this.animatesTopology = animatesTopology;
	}

	public boolean isAnimatesPoints() {
		return animatesPoints;
	}

	public void setAnimatesPoints(boolean animatesPoints) {
		this.animatesPoints = animatesPoints;
	}

	public boolean isAnimatesUVs() {
		return animatesUVs;
	}

	public void setAnimatesUVs(boolean animatesUVs) {
		this.animatesUVs = animatesUVs;
	}

	public boolean isAnimatesVertexColors() {
		return animatesVertexColors;
	}

	public void setAnimatesVertexColors(boolean animatesVertexColors) {
		this.animatesVertexColors = animatesVertexColors;
	}
	
	public String getAnimation() {
		return animation;
	}
	
	public void setAnimation(String animation){
		this.animation = animation;
	}
	public void captureNewFaces() {
		if(this.newFaces == null)
			this.newFaces = new IntArray();
		this.newFaces.clear();
	}
	
	public void disableCaptureNewFaces() {
		this.newFaces = null;
	}
	
	public IntArray getNewFaces() {
		return newFaces;
	}

	public float[] getBoundingBox() {
		float[] res = new float[] { 10000f, 10000f, 10000f, -10000f, -10000f, -10000f};
		if(faces.size() == 0) {
			for(int i = 0; i < 6; ++i)
				res[i] = 0.0f;
		}
		for(ModelFace face : faces) {
			res[0] = Math.min(res[0], face.point0X);
			res[1] = Math.min(res[1], face.point0Y);
			res[2] = Math.min(res[2], face.point0Z);
			
			res[3] = Math.max(res[3], face.point0X);
			res[4] = Math.max(res[4], face.point0Y);
			res[5] = Math.max(res[5], face.point0Z);
			
			res[0] = Math.min(res[0], face.point1X);
			res[1] = Math.min(res[1], face.point1Y);
			res[2] = Math.min(res[2], face.point1Z);
			
			res[3] = Math.max(res[3], face.point1X);
			res[4] = Math.max(res[4], face.point1Y);
			res[5] = Math.max(res[5], face.point1Z);
			
			res[0] = Math.min(res[0], face.point2X);
			res[1] = Math.min(res[1], face.point2Y);
			res[2] = Math.min(res[2], face.point2Z);
			
			res[3] = Math.max(res[3], face.point2X);
			res[4] = Math.max(res[4], face.point2Y);
			res[5] = Math.max(res[5], face.point2Z);
			
			res[0] = Math.min(res[0], face.point3X);
			res[1] = Math.min(res[1], face.point3Y);
			res[2] = Math.min(res[2], face.point3Z);
			
			res[3] = Math.max(res[3], face.point3X);
			res[4] = Math.max(res[4], face.point3Y);
			res[5] = Math.max(res[5], face.point3Z);
		}
		return res;
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
		Matrix fixedMatrix = displayTransforms.getOrDefault(ItemHandler.DISP_CONTEXT_FIXED, null);
		if(fixedMatrix != null) {
			transform(fixedMatrix);
		}
	}
	
	public void applyTransformation(String displayContext) {
		Matrix matrix = displayTransforms.getOrDefault(displayContext, null);
		if(matrix != null)
			transform(matrix);
	}
	
	public boolean hasDisplayTransformation(String displayContext) {
		return displayTransforms.containsKey(displayContext);
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
	
	public void rotate(float rotateX, float rotateY, float rotateZ, boolean uvLock) {
		this.occludes = 0;
		for (ModelFace face : faces) {
			face.rotate(rotateX, rotateY, rotateZ, uvLock);
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
	
	public void scale(float scale, Vector3f pivot) {
		this.occludes = 0;
		for(ModelFace face : faces)
			face.scale(scale, scale, scale, pivot.x, pivot.y, pivot.z);
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

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public long getOccludes() {
		return occludes;
	}
	
	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}

	public void addTexture(String name, String value) {
		long id = TextureRegistry.getIdFromTexture(name, 0);
		long texLong = 0;
		String texStr = value;
		if(value.startsWith("#")) {
			texLong = TextureRegistry.getIdFromTexture(value, 0);
			texStr = null;
		}
		textures.put(id, texLong, texStr);
	}
	
	public void addModel(Model other) {
		addModel(other, false);
	}
	
	public void addModel(Model other, boolean move) {
		if(move && other.moveable == false)
			move = false;
		if(other.getFaces().isEmpty())
			return;
		// It could be that we have mixed doubleSidedness.
		// We can always represent a doubleSided mesh as singleSided,
		// by duplicating the faces and reversing them.
		if(faces.isEmpty())
			// If we don't have any faces yet, then we can easily
			// just copy over the doubleSidedness.
			doubleSided = other.doubleSided;
		boolean convertToSingleSided = false;
		if(!doubleSided && other.doubleSided) {
			// This mesh is a singleSided mesh, but the other mesh
			// is double sided, therefore, we need to make sure
			// that we also had a reverse face for each face in the other mesh.
			convertToSingleSided = true;
		}
		if(doubleSided && !other.doubleSided) {
			// This mesh is doubleSided, but the other mesh
			// isn't doubleSided. So, we need to make this mesh singleSided.
			doubleSided = false;
			int numFaces = faces.size();
			for(int i = 0; i < numFaces; ++i) {
				faces.get(i).setDoubleSided(false);
				ModelFace backFace = new ModelFace(faces.get(i));
				backFace.reverseDirection();
				faces.add(backFace);
			}
		}
		
		int texPrefix = other.getId();
		//String texPrefix = "#" + Integer.toHexString(other.getId()) + "_";
		for(int i = 0; i < other.getTextures().size(); ++i) {
			long key = other.getTextures().getKey(i);
			long texLong = other.getTextures().getLongValue(i);
			String texStr = other.getTextures().getObjValue(i);
			if(texLong != 0)
				texLong = TextureRegistry.prefixId(texLong, texPrefix);
			textures.put(TextureRegistry.prefixId(key, texPrefix), texLong, texStr);
		}
		for(ModelFace face : other.getFaces()) {
			ModelFace copy = face;
			if(!move)
				copy = new ModelFace(face);
			copy.setTexture(TextureRegistry.prefixId(face.getTexture(), texPrefix));
			if(this.newFaces != null)
				this.newFaces.add(faces.size());
			faces.add(copy);
			if(convertToSingleSided) {
				copy.setDoubleSided(false);
				ModelFace backFace = new ModelFace(copy);
				backFace.reverseDirection();
				if(this.newFaces != null)
					this.newFaces.add(faces.size());
				faces.add(backFace);
			}
		}
		if(move) {
			other.faces.clear();
			other.bones.clear();
			other.locators.clear();
		}
	}
	
	public void addModel(Model other, List<Color> tints) {
		addModel(other, tints, false);
	}
	
	public void addModel(Model other, List<Color> tints, boolean move) {
		if(move && other.moveable == false)
			move = false;
		if(other.getFaces().isEmpty())
			return;
		// It could be that we have mixed doubleSidedness.
		// We can always represent a doubleSided mesh as singleSided,
		// by duplicating the faces and reversing them.
		if(faces.isEmpty())
			// If we don't have any faces yet, then we can easily
			// just copy over the doubleSidedness.
			doubleSided = other.doubleSided;
		boolean convertToSingleSided = false;
		if(!doubleSided && other.doubleSided) {
			// This mesh is a singleSided mesh, but the other mesh
			// is double sided, therefore, we need to make sure
			// that we also had a reverse face for each face in the other mesh.
			convertToSingleSided = true;
		}
		if(doubleSided && !other.doubleSided) {
			// This mesh is doubleSided, but the other mesh
			// isn't doubleSided. So, we need to make this mesh singleSided.
			doubleSided = false;
			int numFaces = faces.size();
			for(int i = 0; i < numFaces; ++i) {
				faces.get(i).setDoubleSided(false);
				ModelFace backFace = new ModelFace(faces.get(i));
				backFace.reverseDirection();
				faces.add(backFace);
			}
		}
		
		int texPrefix = other.getId();
		//String texPrefix = "#" + Integer.toHexString(other.getId()) + "_";
		for(int i = 0; i < other.getTextures().size(); ++i) {
			long key = other.getTextures().getKey(i);
			long texLong = other.getTextures().getLongValue(i);
			String texStr = other.getTextures().getObjValue(i);
			if(texLong != 0)
				texLong = TextureRegistry.prefixId(texLong, texPrefix);
			textures.put(TextureRegistry.prefixId(key, texPrefix), texLong, texStr);
		}
		for(ModelFace face : other.getFaces()) {
			ModelFace copy = face;
			if(!move)
				copy = new ModelFace(face);
			copy.setTexture(TextureRegistry.prefixId(face.getTexture(), texPrefix));
			if(tints != null) {
				if(copy.getTintIndex() >= 0 && copy.getTintIndex() < tints.size()) {
					Color color = tints.get(face.getTintIndex());
					copy.setFaceColour(color.getR(), color.getG(), color.getB());
					copy.setTintIndex(-1);
				}
			}
			if(this.newFaces != null)
				this.newFaces.add(faces.size());
			faces.add(copy);
			
			if(convertToSingleSided) {
				copy.setDoubleSided(false);
				ModelFace backFace = new ModelFace(copy);
				backFace.reverseDirection();
				if(this.newFaces != null)
					this.newFaces.add(faces.size());
				faces.add(backFace);
			}
		}
		if(move) {
			other.faces.clear();
			other.bones.clear();
			other.locators.clear();
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
		return addFace(minMaxPoints, dir, texture, ModelFace.SHADING_MODE_STANDARD);
	}

	public ModelFace addFace(float[] minMaxPoints, Direction dir, String texture, String shadingMode) {
		FaceData faceData = new FaceData();
		faceData.texture = texture;

		ModelFace modelFace = new ModelFace(minMaxPoints, dir, faceData, doubleSided, shadingMode);
		if (modelFace.isValid()) {
			this.occludes |= modelFace.getOccludes();
			if(this.newFaces != null)
				this.newFaces.add(faces.size());
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
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, int tintIndex, String shadingMode) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, tintIndex, shadingMode);
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
								float rotX, float rotY, int tintIndex, String shadingMode) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, rotX, rotY, 0f, tintIndex, shadingMode);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float uvRot, int tintIndex) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, uvRot, tintIndex);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float uvRot, int tintIndex, String shadingMode) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, uvRot, tintIndex, shadingMode);
	}
	

	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float rotX, float rotY, float uvRot, int tintIndex) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, rotX, rotY, uvRot, tintIndex, ModelFace.SHADING_MODE_STANDARD);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, 
								float rotX, float rotY, float uvRot, int tintIndex, String shadingMode) {
		FaceData faceData = new FaceData();
		faceData.texture = texture;
		faceData.tintindex = tintIndex;
		if(minMaxUVs != null && minMaxUVs.length >= 4) {
			faceData.uv0 = minMaxUVs[0];
			faceData.uv1 = minMaxUVs[1];
			faceData.uv2 = minMaxUVs[2];
			faceData.uv3 = minMaxUVs[3];
			faceData.hasUV = true;
		}
		if(uvRot != 0f) {
			faceData.rotation = uvRot;
			faceData.rotationMiEx = true;
		}

		ModelFace modelFace = new ModelFace(minMaxPoints, dir, faceData, doubleSided, shadingMode);
		if (modelFace.isValid()) {
			if(rotX != 0f || rotY != 0f)
				modelFace.rotate(rotX, rotY, false);
			this.occludes |= modelFace.getOccludes();
			if(this.newFaces != null)
				this.newFaces.add(faces.size());
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
	
	/**
	 * Moves all faces directly on the unit cube's faces
	 * slightly inwards.
	 */
	public void moveTransparentFaces() {
		for(ModelFace face : faces)
			face.moveTransparentFace(false, true);
	}
	
	public boolean shouldMoveTransparentFaces() {
		for(ModelFace face : faces)
			if(face.shouldMoveTransparentFace())
				return true;
		return false;
	}
	
	public void calculateOcclusions() {
		for(ModelFace face : faces)
			face.calculateOcclusion();
		calculateOccludes();
	}
	
	public void allowOcclusionIfFullyOccluded() {
		for(ModelFace face : faces)
			face.allowOcclusionIfFullyOccluded();
	}

	public void calculateOccludes() {
		occludes = 0;
		for(int i = 0; i < faces.size(); ++i)
			occludes |= faces.get(i).getOccludes();
	}

	public void setParentModel(Model parentModel) {
		this.parentModel = parentModel;
	}
	
	public JsonObject toJson() {
		JsonObject res = new JsonObject();
		
		res.addProperty("name", name);
		res.addProperty("id", id);
		res.addProperty("occludes", occludes);
		res.addProperty("extraData", extraData);
		res.addProperty("doubleSided", doubleSided);
		if(animation != null)
			res.addProperty("animation", animation);
		JsonObject texturesObject = new JsonObject();
		for(int i = 0; i < textures.size(); ++i) {
			String key = TextureRegistry.getTextureFromId(textures.getKey(i));
			long texLong = textures.getLongValue(i);
			String texStr = textures.getObjValue(i);
			if(texLong != 0)
				texStr = TextureRegistry.getTextureFromId(texLong);
			texturesObject.addProperty(key, texStr);
		}
		res.add("textures", texturesObject);
		JsonArray facesArray = new JsonArray();
		for(ModelFace face : faces) {
			facesArray.add(face.toJson());
		}
		res.add("faces", facesArray);
		JsonArray bonesArray = new JsonArray();
		for(ModelBone bone : bones) {
			bonesArray.add(bone.toJson());
		}
		res.add("bones", bonesArray);
		JsonArray locatorsArray = new JsonArray();
		for(ModelLocator locator : locators) {
			locatorsArray.add(locator.toJson());
		}
		res.add("locators", locatorsArray);
		
		return res;
	}

}
