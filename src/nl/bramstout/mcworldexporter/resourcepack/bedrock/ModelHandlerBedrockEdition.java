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
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelBone;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelLocator;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ModelHandlerBedrockEdition extends ModelHandler{

	private String parent;
	private JsonObject data;
	private int textureWidth;
	private int textureHeight;
	
	public ModelHandlerBedrockEdition(String parent, JsonObject data) {
		this.parent = parent;
		this.data = data;
		this.textureWidth = 64;
		this.textureHeight = 64;
		
		if(parent != null) {
			ModelHandler parentHandler = ResourcePacks.getModelHandler(parent);
			if(parentHandler instanceof ModelHandlerBedrockEdition) {
				textureWidth = ((ModelHandlerBedrockEdition) parentHandler).textureWidth;
				textureHeight = ((ModelHandlerBedrockEdition) parentHandler).textureHeight;
			}
		}
		
		if(data.has("description")) {
			JsonObject description = data.get("description").getAsJsonObject();
			if(description.has("texture_width"))
				textureWidth = description.get("texture_width").getAsInt();
			if(description.has("texture_height"))
				textureHeight = description.get("texture_height").getAsInt();
		}else{
			if(data.has("texturewidth"))
				textureWidth = data.get("texturewidth").getAsInt();
			if(data.has("textureheight"))
				textureHeight = data.get("textureheight").getAsInt();
		}
	}
	
	@Override
	public void getGeometry(Model model) {
		// Bedrock Edition geometry is always double sided.
		// If the model isn't already double sided and we need
		// double sided to be true to correctly show the model,
		// then set double sided to be true.
		// The preference is for doubleSided to be false for blocks,
		// to match Java Edition, so we only want to set it to true,
		// when we need it.
		if(!model.isDoubleSided())
			if(needsDoubleSided())
				model.setDoubleSided(true);
		
		if(parent != null) {
			ModelHandler parentHandler = ResourcePacks.getModelHandler(parent);
			if(parentHandler instanceof ModelHandlerBedrockEdition) {
				((ModelHandlerBedrockEdition) parentHandler).addBones(textureWidth, textureHeight, model);
			}else {
				parentHandler.getGeometry(model);
			}
		}
		
		addBones(textureWidth, textureHeight, model);
	}
	
	@Override
	public void getGeometry(Model model, BlockAnimationHandler handler, float frame) {
		this.getGeometry(model);
	}
	
	private boolean needsDoubleSided() {
		if(!data.has("bones"))
			return false;
		
		// If we have a cube that is missing one of its faces,
		// then there's a good chance that we'll be able to see
		// the back side of the faces and so we need doubleSided
		// to be true. If none of the cubes are missing a face,
		// then there's no way to see the back side of a face,
		// and then we don't need doubleSided to be true.
		// The preference is for doubleSided to be false for blocks,
		// to match Java Edition.
		JsonArray bones = data.get("bones").getAsJsonArray();
		for(JsonElement boneEl : bones.asList()) {
			JsonObject boneObj = boneEl.getAsJsonObject();
			
			if(boneObj.has("cubes")) {
				for(JsonElement cubeEl : boneObj.get("cubes").getAsJsonArray().asList()) {
					JsonObject cubeObj = cubeEl.getAsJsonObject();
					
					// If the cube is flipped, then we can also see the back side,
					// but if we flip it twice, then it's the same as a 180 degree
					// rotation and we no longer can see the back side.
					// So if there was one or three flips, then we can also
					// see the back side.
					Vector3f size = readVector3f(cubeObj.get("size"));
					int numNegatives = 0;
					if(size.x < 0f)
						numNegatives++;
					if(size.y < 0f)
						numNegatives++;
					if(size.z < 0f)
						numNegatives++;
					if(cubeObj.has("mirror"))
						if(cubeObj.get("mirror").getAsBoolean())
							numNegatives++;
					if(numNegatives == 1 || numNegatives == 3)
						return true;
					
					JsonElement uv = cubeObj.get("uv");
					if(uv == null || uv.isJsonArray()) {
						continue;
					}else if(uv.isJsonObject()) {
						JsonObject uvObj = uv.getAsJsonObject();
						
						if(!uvObj.has("north"))
							return true;
						if(!uvObj.has("south"))
							return true;
						if(!uvObj.has("east"))
							return true;
						if(!uvObj.has("west"))
							return true;
						if(!uvObj.has("up"))
							return true;
						if(!uvObj.has("down"))
							return true;
					}
				}
			}
		}
		return false;
	}
	
	private void addBones(int textureWidth, int textureHeight, Model model) {
		if(!data.has("bones"))
			return;
		Map<ModelBone, String> parents = new HashMap<ModelBone, String>();
		
		JsonArray bones = data.get("bones").getAsJsonArray();
		for(JsonElement boneEl : bones.asList()) {
			JsonObject boneObj = boneEl.getAsJsonObject();
			
			String name = boneObj.get("name").getAsString().toLowerCase();
			
			ModelBone bone = model.getBone(name);
			if(bone == null) {
				bone = new ModelBone(name);
				model.getBones().add(bone);
			}
			
			String parent = null;
			if(boneObj.has("parent"))
				parent = boneObj.get("parent").getAsString().toLowerCase();
			parents.put(bone, parent);
			
			if(boneObj.has("pivot")) {
				bone.pivot = readVector3f(boneObj.get("pivot"));
				bone.pivot.x *= -1f;
			}
			if(boneObj.has("rotation"))
				bone.rotation = readVector3f(boneObj.get("rotation"));
			
			if(boneObj.has("locators")) {
				for(Entry<String, JsonElement> entry : boneObj.getAsJsonObject("locators").entrySet()) {
					String locatorName = entry.getKey().toLowerCase();
					ModelLocator locator = model.getLocator(locatorName);
					if(locator == null) {
						locator = new ModelLocator(locatorName);
						model.getLocators().add(locator);
					}
					locator.bone = bone;
					if(entry.getValue().isJsonArray()) {
						JsonArray array = entry.getValue().getAsJsonArray();
						if(array.size() == 1 || array.size() == 2) {
							locator.offset = new Vector3f(array.get(0).getAsFloat());
						}else if(array.size() >= 3) {
							locator.offset = new Vector3f(array.get(0).getAsFloat(), 
															array.get(1).getAsFloat(), 
															array.get(2).getAsFloat());
						}
					}else if(entry.getValue().isJsonObject()) {
						JsonObject obj = entry.getValue().getAsJsonObject();
						if(obj.has("offset")) {
							JsonArray array = obj.getAsJsonArray("offset");
							if(array.size() == 1 || array.size() == 2) {
								locator.offset = new Vector3f(array.get(0).getAsFloat());
							}else if(array.size() >= 3) {
								locator.offset = new Vector3f(array.get(0).getAsFloat(), 
																array.get(1).getAsFloat(), 
																array.get(2).getAsFloat());
							}
						}
						if(obj.has("rotation")) {
							JsonArray array = obj.getAsJsonArray("rotation");
							if(array.size() == 1 || array.size() == 2) {
								locator.rotation = new Vector3f(array.get(0).getAsFloat());
							}else if(array.size() >= 3) {
								locator.rotation = new Vector3f(array.get(0).getAsFloat(), 
																array.get(1).getAsFloat(), 
																array.get(2).getAsFloat());
							}
						}
						if(obj.has("ignore_inherited_scale"))
							locator.ignoreInheritedScale = obj.get("ignore_inherited_scale").getAsBoolean();
					}
				}
			}
		}
		
		for(Entry<ModelBone, String> entry : parents.entrySet()) {
			for(ModelBone bone : model.getBones()) {
				if(bone.getName().equalsIgnoreCase(entry.getValue())) {
					entry.getKey().setParent(bone);
					break;
				}
			}
		}
		
		// The bone's pivot which is stored in ModelBone.translation
		// is specified in world space coordinates in the model file,
		// yet we want it in local space coordinates.
		// So do a quick little conversion here.
		for(ModelBone bone : model.getBones()) {
			if(bone.getParent() != null)
				continue;
			pivotToLocalSpace(bone, model);
		}
		
		for(JsonElement boneEl : bones.asList()) {
			JsonObject boneObj = boneEl.getAsJsonObject();
			
			String name = boneObj.get("name").getAsString().toLowerCase();
			
			ModelBone bone = null;
			for(ModelBone bone2 : model.getBones()) {
				if(bone2.getName().equals(name)) {
					bone = bone2;
					break;
				}
			}
			
			boolean mirror = false;
			if(boneObj.has("mirror"))
				mirror = boneObj.get("mirror").getAsBoolean();
			
			float inflate = 0f;
			if(boneObj.has("inflate"))
				inflate = boneObj.get("inflate").getAsFloat();
			
			Vector3f baseRotation = readVector3f(boneObj.get("bind_pose_rotation"));
			
			if(boneObj.has("cubes")) {
				for(JsonElement cubeEl : boneObj.get("cubes").getAsJsonArray().asList()) {
					JsonObject cubeObj = cubeEl.getAsJsonObject();
					
					addCube(cubeObj, model, bone, mirror, inflate, baseRotation, bone.getWorldSpacePivot(),
							textureWidth, textureHeight);
				}
			}
		}
	}
	
	private void pivotToLocalSpace(ModelBone bone, Model model) {
		/*if(bone.getParent() != null) {
			bone.translation = bone.translation.subtract(bone.getParent().getWorldSpacePivot());
		}
		for(ModelBone bone2 : model.getBones()) {
			if(bone2.getParent() != bone)
				continue;
			pivotToLocalSpace(bone2, model);
		}*/
	}
	
	private void addCube(JsonObject cubeObj, Model model, ModelBone bone, boolean mirror, float inflate, 
							Vector3f baseRotation, Vector3f baseTranslation, int textureWidth, int textureHeight) {
		float textureScaleU = 16f / ((float) textureWidth);
		float textureScaleV = 16f / ((float) textureHeight);
		
		Vector3f origin = readVector3f(cubeObj.get("origin"));
		Vector3f size = readVector3f(cubeObj.get("size"));
		if(size.x <= 0.0000001f && size.y <= 0.0000001f && size.z <= 0.0000001f)
			return;
		origin.x = -(origin.x + size.x);
		Vector3f rotation = baseRotation;
		if(cubeObj.has("rotation"))
			rotation = readVector3f(cubeObj.get("rotation"));
		Vector3f pivot = new Vector3f(origin.add(size.multiply(0.5f)));
		if(cubeObj.has("pivot")) {
			pivot = readVector3f(cubeObj.get("pivot"));
		}
		pivot.x *= -1f;
		//pivot.z *= -1f;
		
		if(cubeObj.has("inflate"))
			inflate = cubeObj.get("inflate").getAsFloat();
		
		if(cubeObj.has("mirror"))
			mirror = cubeObj.get("mirror").getAsBoolean();
		
		Vector3f uvSize = new Vector3f(size);
		
		origin = origin.subtract(inflate);
		size = size.add(inflate * 2f);
		
		if(Config.minCubeSize > 0.0f) {
			if(Math.abs(size.x) < Config.minCubeSize)
				size.x = Config.minCubeSize * Math.signum(size.x);
			
			if(Math.abs(size.y) < Config.minCubeSize)
				size.y = Config.minCubeSize * Math.signum(size.y);
			
			if(Math.abs(size.z) < Config.minCubeSize)
				size.z = Config.minCubeSize * Math.signum(size.z);
		}
		
		List<ModelFace> faces = new ArrayList<ModelFace>();
		
		JsonElement uv = cubeObj.get("uv");
		if(uv == null || uv.isJsonArray()) {
			float u0 = 0f;
			float v0 = 0f;
			if(uv != null) {
				if(uv.getAsJsonArray().size() >= 2) {
					u0 = uv.getAsJsonArray().get(0).getAsFloat();
					v0 = uv.getAsJsonArray().get(1).getAsFloat();
				}
			}
			
			// NORTH
			float[] points = new float[] {
					origin.x, origin.y, origin.z,
					origin.x + size.x, origin.y + size.y, origin.z
			};
			float[] uvs = new float[] {
				u0 + uvSize.z * 2f + uvSize.x, v0 + uvSize.z,
				u0 + uvSize.z * 2f + uvSize.x * 2f, v0 + uvSize.z + uvSize.y
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.NORTH, "#north"));
			
			// SOUTH
			points = new float[] {
					origin.x, origin.y, origin.z + size.z,
					origin.x + size.x, origin.y + size.y, origin.z + size.z
			};
			uvs = new float[] {
				u0 + uvSize.z, v0 + uvSize.z,
				u0 + uvSize.z + uvSize.x, v0 + uvSize.z + uvSize.y
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.SOUTH, "#south"));
			
			// WEST
			points = new float[] {
					origin.x, origin.y, origin.z,
					origin.x, origin.y + size.y, origin.z + size.z
			};
			uvs = new float[] {
				u0, v0 + uvSize.z,
				u0 + uvSize.z, v0 + uvSize.z + uvSize.y
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.WEST, "#west"));
			
			// EAST
			points = new float[] {
					origin.x + size.x, origin.y, origin.z,
					origin.x + size.x, origin.y + size.y, origin.z + size.z
			};
			uvs = new float[] {
				u0 + uvSize.z + uvSize.x, v0 + uvSize.z,
				u0 + uvSize.z * 2f + uvSize.x, v0 + uvSize.z + uvSize.y
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.EAST, "#east"));
			
			// DOWN
			points = new float[] {
					origin.x, origin.y, origin.z,
					origin.x + size.x, origin.y, origin.z + size.z
			};
			uvs = new float[] {
				u0 + uvSize.z + uvSize.x, v0 + uvSize.z,
				u0 + uvSize.z + uvSize.x * 2f, v0
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.DOWN, "#down"));
			
			// UP
			points = new float[] {
					origin.x, origin.y + size.y, origin.z,
					origin.x + size.x, origin.y + size.y, origin.z + size.z
			};
			uvs = new float[] {
				u0 + uvSize.z, v0,
				u0 + uvSize.z + uvSize.x, v0 + uvSize.z
			};
			uvs[0] *= textureScaleU;
			uvs[1] *= textureScaleV;
			uvs[2] *= textureScaleU;
			uvs[3] *= textureScaleV;
			bone.faceIds.add(model.getFaces().size());
			faces.add(model.addFace(points, uvs, Direction.UP, "#up"));
		}else if(uv.isJsonObject()) {
			JsonObject uvObj = uv.getAsJsonObject();
			
			if(uvObj.has("north")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("north");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.x;
				float uvHeight = uvSize.y;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#north";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.NORTH, texture));
			}
			if(uvObj.has("south")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("south");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.x;
				float uvHeight = uvSize.y;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#south";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.SOUTH, texture));
			}
			if(uvObj.has("west")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("west");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.z;
				float uvHeight = uvSize.y;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#west";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.WEST, texture));
			}
			if(uvObj.has("east")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("east");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.z;
				float uvHeight = uvSize.y;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#east";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.EAST, texture));
			}
			if(uvObj.has("up")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("up");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.x;
				float uvHeight = uvSize.z;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#up";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.UP, texture));
			}
			if(uvObj.has("down")) {
				JsonObject uvObj2 = uvObj.getAsJsonObject("down");
				float u0 = uvObj2.get("uv").getAsJsonArray().get(0).getAsFloat();
				float v0 = uvObj2.get("uv").getAsJsonArray().get(1).getAsFloat();
				float uvWidth = uvSize.x;
				float uvHeight = uvSize.z;
				if(uvObj2.has("uv_size")) {
					uvWidth = uvObj2.get("uv_size").getAsJsonArray().get(0).getAsFloat();
					uvHeight = uvObj2.get("uv_size").getAsJsonArray().get(1).getAsFloat();
				}
				String texture = "#down";
				if(uvObj2.has("material_instance")) {
					texture = "#" + uvObj2.get("material_instance").getAsString();
				}
				float[] points = new float[] {
						origin.x, origin.y, origin.z,
						origin.x + size.x, origin.y + size.y, origin.z + size.z
				};
				float[] uvs = new float[] {
					u0, v0,
					u0 + uvWidth, v0 + uvHeight
				};
				uvs[0] *= textureScaleU;
				uvs[1] *= textureScaleV;
				uvs[2] *= textureScaleU;
				uvs[3] *= textureScaleV;
				bone.faceIds.add(model.getFaces().size());
				faces.add(model.addFace(points, uvs, Direction.DOWN, texture));
			}
		}
		
		float centerX = origin.x + size.x / 2f;
		float centerY = origin.y + size.y / 2f;
		float centerZ = origin.z + size.z / 2f;
		
		// Bedrock has its models rotated 180 degrees, so we need
		// to rotate each cube 180 degrees to compensate
		/*for(ModelFace face : faces) {
			face.rotate(0f, 180f, 0f, centerX, centerY, centerZ);
		}*/
		
		if(mirror) {
			for(ModelFace face : faces) {
				face.mirror(true, false, false, centerX, centerY, centerZ);
			}
		}
		
		if(rotation.x != 0f || rotation.y != 0f || rotation.z != 0f) {
			for(ModelFace face : faces) {
				face.rotate(rotation.x, rotation.y, rotation.z, pivot.x, pivot.y, pivot.z);
			}
		}
		
		/*for(ModelFace face : faces) {
			face.translate(baseTranslation.x, -baseTranslation.y, -baseTranslation.z);
		}*/
	}
	
	private Vector3f readVector3f(JsonElement el) {
		if(el == null)
			return new Vector3f();
		
		JsonArray array = el.getAsJsonArray();
		if(array.size() <= 0)
			return new Vector3f();
		if(array.size() < 3)
			return new Vector3f(array.get(0).getAsFloat());
		return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
	}

}
