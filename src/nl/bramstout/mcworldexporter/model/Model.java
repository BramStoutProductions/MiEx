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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Model {

	private String name;
	protected int id;
	private int weight;
	private long occludes;
	protected String extraData;
	protected boolean doubleSided;

	protected Map<String, String> textures;
	protected List<ModelFace> faces;

	public Model(Model other) {
		this.name = other.name;
		this.id = other.id;
		this.textures = other.textures;
		this.faces = new ArrayList<ModelFace>();
		this.weight = other.weight;
		this.occludes = other.occludes;
		this.extraData = other.extraData;
		for (ModelFace face : other.faces) {
			this.faces.add(new ModelFace(face));
		}
	}

	public Model(String name, JsonObject data, boolean doubleSided) {
		this.name = name;
		this.textures = new HashMap<String, String>();
		this.faces = new ArrayList<ModelFace>();
		this.weight = 1;
		this.occludes = 0;
		this.extraData = "";
		this.doubleSided = doubleSided;
		if (data == null)
			return;

		if (data.has("parent")) {
			String parentName = data.get("parent").getAsString();
			int parentModelId = ModelRegistry.getIdForName(parentName, doubleSided);
			Model parentModel = ModelRegistry.getModel(parentModelId);
			if (parentModel != null) {
				this.extraData = parentModel.extraData;
				textures.putAll(parentModel.textures);
				for (ModelFace face : parentModel.faces) {
					this.faces.add(face);
					this.occludes |= face.getOccludes();
				}
			}
		}

		this.id = ModelRegistry.getNextId();

		if (data.has("textures")) {
			for (Entry<String, JsonElement> element : data.get("textures").getAsJsonObject().entrySet()) {
				String texName = element.getValue().getAsString();
				if (!texName.contains(":") && !texName.startsWith("#"))
					texName = name.split(":")[0] + ":" + texName;
				textures.put(element.getKey(), texName);
			}
		}

		if (data.has("elements")) {
			this.faces = new ArrayList<ModelFace>();
			this.occludes = 0;
			for (JsonElement elementTmp : data.get("elements").getAsJsonArray().asList()) {
				try {

					JsonObject element = elementTmp.getAsJsonObject();
					JsonArray from = element.get("from").getAsJsonArray();
					JsonArray to = element.get("to").getAsJsonArray();

					float minX = Math.min(from.get(0).getAsFloat(), to.get(0).getAsFloat());
					float minY = Math.min(from.get(1).getAsFloat(), to.get(1).getAsFloat());
					float minZ = Math.min(from.get(2).getAsFloat(), to.get(2).getAsFloat());
					float maxX = Math.max(from.get(0).getAsFloat(), to.get(0).getAsFloat());
					float maxY = Math.max(from.get(1).getAsFloat(), to.get(1).getAsFloat());
					float maxZ = Math.max(from.get(2).getAsFloat(), to.get(2).getAsFloat());

					boolean flatX = (maxX - minX) < 0.01f;
					boolean flatY = (maxY - minY) < 0.01f;
					boolean flatZ = (maxZ - minZ) < 0.01f;
					if(!doubleSided) {
						flatX = false;
						flatY = false;
						flatZ = false;
					}
					boolean deleteSideX = minX > 8.0f;
					boolean deleteSideY = minY > 8.0f;
					boolean deleteSideZ = minZ > 8.0f;

					if ((flatX && flatY) || (flatX && flatZ) || (flatY && flatZ))
						continue;

					JsonObject rotateData = null;
					if (element.has("rotation"))
						rotateData = element.get("rotation").getAsJsonObject();

					if (element.has("faces")) {
						JsonObject faceObj = element.get("faces").getAsJsonObject();
						if (!(faceObj.has("west") && faceObj.has("east")))
							flatX = false;
						if (!(faceObj.has("down") && faceObj.has("up")))
							flatY = false;
						if (!(faceObj.has("north") && faceObj.has("south")))
							flatZ = false;

						for (Entry<String, JsonElement> face : faceObj.entrySet()) {
							Direction dir = Direction.getDirection(face.getKey());

							if (dir == Direction.EAST) {
								if (flatX && !deleteSideX)
									continue;
							} else if (dir == Direction.WEST) {
								if (flatX && deleteSideX)
									continue;
							} else if (dir == Direction.UP) {
								if (flatY && !deleteSideY)
									continue;
							} else if (dir == Direction.DOWN) {
								if (flatY && deleteSideY)
									continue;
							} else if (dir == Direction.SOUTH) {
								if (flatZ && !deleteSideZ)
									continue;
							} else if (dir == Direction.NORTH) {
								if (flatZ && deleteSideZ)
									continue;
							}

							ModelFace modelFace = new ModelFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, dir,
									face.getValue().getAsJsonObject(), doubleSided);
							if (modelFace.isValid()) {
								modelFace.rotate(rotateData);
								this.occludes |= modelFace.getOccludes();
								faces.add(modelFace);
							}
						}
					} else {
						for (Direction dir : Direction.CACHED_VALUES) {
							if (dir == Direction.EAST) {
								if (flatX && deleteSideX)
									continue;
							} else if (dir == Direction.WEST) {
								if (flatX && !deleteSideX)
									continue;
							} else if (dir == Direction.UP) {
								if (flatY && deleteSideY)
									continue;
							} else if (dir == Direction.DOWN) {
								if (flatY && !deleteSideY)
									continue;
							} else if (dir == Direction.SOUTH) {
								if (flatZ && deleteSideZ)
									continue;
							} else if (dir == Direction.NORTH) {
								if (flatZ && !deleteSideZ)
									continue;
							}

							JsonObject faceData = null;
							if (textures.keySet().size() > 0) {
								faceData = new JsonObject();
								faceData.addProperty("texture", (String) (textures.keySet().toArray()[0]));
							}
							ModelFace modelFace = new ModelFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, dir,
									faceData, doubleSided);
							if (modelFace.isValid()) {
								modelFace.rotate(rotateData);
								this.occludes |= modelFace.getOccludes();
								faces.add(modelFace);
							}
						}
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}
	
	public String getDefaultTexture() {
		if(textures.size() == 0)
			return "";
		String key = (String) textures.keySet().iterator().next();
		return getTexture(key);
	}

	public String getTexture(String name) {
		if (name.startsWith("#"))
			name = name.substring(1);
		String path = textures.get(name);
		if (path == null)
			return "";
		if (path.startsWith("#"))
			return getTexture(path.substring(1));
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
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	public String getExtraData() {
		return extraData;
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
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f);
	}

	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, float rotX, float rotY) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, rotX, rotY, 0f);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, float uvRot) {
		return addFace(minMaxPoints, minMaxUVs, dir, texture, 0f, 0f, uvRot);
	}
	
	public ModelFace addFace(float[] minMaxPoints, float[] minMaxUVs, Direction dir, String texture, float rotX, float rotY, float uvRot) {
		JsonObject faceData = new JsonObject();
		faceData.addProperty("texture", texture);
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
			addFace(minMaxPoints, new float[] { uvX, uvY, uvX + uvW, uvY + uvH }, Direction.DOWN, texture, rotX, rotY);
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

}
