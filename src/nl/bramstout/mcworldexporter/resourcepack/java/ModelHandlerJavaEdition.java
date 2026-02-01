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

package nl.bramstout.mcworldexporter.resourcepack.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class ModelHandlerJavaEdition extends ModelHandler{

	private JsonObject data;
	
	public ModelHandlerJavaEdition(JsonObject data) {
		this.data = data;
	}
	
	@Override
	public void getGeometry(Model model) {
		if (data == null)
			return;

		Map<String, String> textures = new HashMap<String, String>();
		if (data.has("textures")) {
			for (Entry<String, JsonElement> element : data.get("textures").getAsJsonObject().entrySet()) {
				String texName = element.getValue().getAsString();
				if (!texName.contains(":") && !texName.startsWith("#"))
					texName = "minecraft:" + texName;
				String varName = element.getKey();
				if(!varName.startsWith("#"))
					varName = "#" + varName;
				textures.put(varName, texName);
			}
		}
		model.getTextures().putAll(textures);
		
		if (data.has("parent")) {
			String parentName = data.get("parent").getAsString();
			int parentModelId = ModelRegistry.getIdForName(parentName, model.isDoubleSided());
			Model parentModel = ModelRegistry.getModel(parentModelId);
			if (parentModel != null) {
				model.setParentModel(parentModel);
				model.getTextures().putAll(parentModel.getTextures());
				model.getTextures().putAll(textures);
				
				// In case of a model that depends on settings of this model,
				// we call this function. Most likely, it will return itself
				// and so nothing changes, but in the event of one of the parent
				// models being ModelItemGenerated, it will return a different
				// model based on the settings of the current model.
				parentModel = parentModel.postConstruct(model);
				
				model.getDisplayTransforms().putAll(parentModel.getDisplayTransforms());
				model.setExtraData(parentModel.getExtraData());
				for (ModelFace face : parentModel.getFaces()) {
					model.getFaces().add(face);
				}
			}
		}
		
		if(data.has("display")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("display").entrySet()) {
				Matrix matrix = new Matrix();
				if(entry.getValue().isJsonObject()) {
					matrix = Matrix.translate(-8f, -8f, -8f);
					JsonObject transformData = entry.getValue().getAsJsonObject();
					if(transformData.has("scale")) {
						JsonArray scaleData = transformData.getAsJsonArray("scale");
						if(scaleData.size() >= 3) {
							matrix = Matrix.scale(scaleData.get(0).getAsFloat(), scaleData.get(1).getAsFloat(), 
												scaleData.get(2).getAsFloat()).mult(matrix);
						}
					}
					if(transformData.has("rotation")) {
						JsonArray rotationData = transformData.getAsJsonArray("rotation");
						if(rotationData.size() >= 3) {
							matrix = Matrix.rotate(rotationData.get(0).getAsFloat(), rotationData.get(1).getAsFloat(), 
													rotationData.get(2).getAsFloat()).mult(matrix);
						}
					}
					if(transformData.has("translation")) {
						JsonArray translationData = transformData.getAsJsonArray("translation");
						if(translationData.size() >= 3) {
							matrix = Matrix.translate(translationData.get(0).getAsFloat(), translationData.get(1).getAsFloat(), 
									translationData.get(2).getAsFloat()).mult(matrix);
						}
					}
					matrix = Matrix.translate(8f, 8f, 8f).mult(matrix);
				}
				model.getDisplayTransforms().put(entry.getKey(), matrix);
			}
		}

		if (data.has("elements")) {
			model.getFaces().clear();
			for (JsonElement elementTmp : data.get("elements").getAsJsonArray().asList()) {
				try {

					JsonObject element = elementTmp.getAsJsonObject();
					JsonArray from = element.get("from").getAsJsonArray();
					JsonArray to = element.get("to").getAsJsonArray();

					float minX = from.get(0).getAsFloat();
					float minY = from.get(1).getAsFloat();
					float minZ = from.get(2).getAsFloat();
					float maxX = to.get(0).getAsFloat();
					float maxY = to.get(1).getAsFloat();
					float maxZ = to.get(2).getAsFloat();
					
					if(Config.minCubeSize > 0.0f) {
						float dx = maxX - minX;
						if(Math.abs(dx) < Config.minCubeSize)
							maxX = minX + Config.minCubeSize * Math.signum(dx);
						
						float dy = maxY - minY;
						if(Math.abs(dy) < Config.minCubeSize)
							maxY = minY + Config.minCubeSize * Math.signum(dy);
						
						float dz = maxZ - minZ;
						if(Math.abs(dz) < Config.minCubeSize)
							maxZ = minZ + Config.minCubeSize * Math.signum(dz);
					}

					boolean flatX = (maxX - minX) < 0.01f;
					boolean flatY = (maxY - minY) < 0.01f;
					boolean flatZ = (maxZ - minZ) < 0.01f;
					if(!model.isDoubleSided()) {
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
							
							JsonElement texturePrim = face.getValue().getAsJsonObject().get("texture");
							if(texturePrim != null) {
								if(!texturePrim.getAsString().startsWith("#")) {
									face.getValue().getAsJsonObject().addProperty("texture", "#" + texturePrim.getAsString());
								}
							}

							ModelFace modelFace = new ModelFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, dir,
									face.getValue().getAsJsonObject(), model.isDoubleSided());
							if (modelFace.isValid()) {
								modelFace.rotate(rotateData);
								model.getFaces().add(modelFace);
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
							if (model.getTextures().keySet().size() > 0) {
								faceData = new JsonObject();
								faceData.addProperty("texture", (String) (model.getTextures().keySet().toArray()[0]));
							}
							ModelFace modelFace = new ModelFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, dir,
									faceData, model.isDoubleSided());
							if (modelFace.isValid()) {
								modelFace.rotate(rotateData);
								model.getFaces().add(modelFace);
							}
						}
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void getGeometry(Model model, BlockAnimationHandler handler, float frame) {
		this.getGeometry(model);
	}

}
