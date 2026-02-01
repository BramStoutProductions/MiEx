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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Quaternion;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;
import nl.bramstout.mcworldexporter.resourcepack.hytale.BlockAnimationHandlerHytale.NodeAnimation;

public class ModelHandlerHytale extends ModelHandler{
	
	private JsonObject data;
	private List<Node> nodes;
	
	public ModelHandlerHytale(JsonObject data) {
		this.data = data;
		this.nodes = new ArrayList<Node>();
		
		if(this.data.has("nodes")) {
			JsonArray nodesArray = this.data.getAsJsonArray("nodes");
			for(JsonElement val : nodesArray.asList()) {
				if(val.isJsonObject()) {
					this.nodes.add(new Node(val.getAsJsonObject()));
				}
			}
		}
	}

	@Override
	public void getGeometry(Model model) {
		this.getGeometry(model, null, 0);
	}
	
	@Override
	public void getGeometry(Model model, BlockAnimationHandler animationHandler, float frame) {
		// Each shape in the model can indicate whether it's single sided
		// or double sided, but a MiEx Model can only indicate it for
		// the entire model and not per face. So, if all shapes share
		// the same doubleSided value, then we can safely set that
		// in the MiEx Model, but if the doubleSided value is varying,
		// then we need to force the model to be single sided, 
		// since we can represent a double sided model in a single sided
		// model (by duplicating the faces), but we can't represent
		// a single sided model in a double sided model.
		int doubleSided = 2;
		for(Node node : nodes) {
			int nodeDoubleSided = node.getDoubleSided();
			if(nodeDoubleSided == -2)
				continue;
			if(doubleSided == 2)
				doubleSided = nodeDoubleSided;
			else if(nodeDoubleSided == -1) {
				doubleSided = -1;
				break;
			}else if(doubleSided != nodeDoubleSided) {
				doubleSided = -1;
				break;
			}
		}
		model.setDoubleSided(doubleSided == 1);
		boolean forceSingleSided = doubleSided <= 0;
		
		BlockAnimationHandlerHytale animationHandlerHytale = null;
		if(animationHandler instanceof BlockAnimationHandlerHytale)
			animationHandlerHytale = (BlockAnimationHandlerHytale) animationHandler;
		
		for(Node node : nodes)
			// Hytale models are 32x32 and centered on X and Z at the origin,
			// so we already move it over and scale it down to this MiEx's
			// 16x16 with the bottom left front corner at origin.
			node.addGeometry(model, Matrix.translate(8f, 0f, 8f).mult(Matrix.scale(0.5f, 0.5f, 0.5f)), 
					forceSingleSided, animationHandlerHytale, frame);
	}
	
	private static class Node{
		
		@SuppressWarnings("unused")
		public String id;
		public String name;
		public Vector3f position;
		public Quaternion orientation;
		public Vector3f offset;
		public Shape shape;
		public List<Node> children;
		
		public Node(JsonObject data) {
			if(data.has("id"))
				this.id = data.get("id").getAsString();
			if(data.has("name"))
				this.name = data.get("name").getAsString();
			this.position = new Vector3f();
			if(data.has("position")) {
				JsonObject pos = data.getAsJsonObject("position");
				if(pos.has("x"))
					this.position.x = pos.get("x").getAsFloat();
				if(pos.has("y"))
					this.position.y = pos.get("y").getAsFloat();
				if(pos.has("z"))
					this.position.z = pos.get("z").getAsFloat();
			}
			this.orientation = new Quaternion();
			if(data.has("orientation")) {
				JsonObject orientation = data.getAsJsonObject("orientation");
				if(orientation.has("x"))
					this.orientation.x = orientation.get("x").getAsFloat();
				if(orientation.has("y"))
					this.orientation.y = orientation.get("y").getAsFloat();
				if(orientation.has("z"))
					this.orientation.z = orientation.get("z").getAsFloat();
				if(orientation.has("w"))
					this.orientation.w = orientation.get("w").getAsFloat();
			}
			
			this.shape = null;
			if(data.has("shape")) {
				JsonObject shape = data.getAsJsonObject("shape");
				String type = "";
				if(shape.has("type"))
					type = shape.get("type").getAsString();
				
				if(type.equals("box"))
					this.shape = new ShapeBox(shape);
				else if(type.equals("quad"))
					this.shape = new ShapeQuad(shape);
				else this.shape = new ShapeNone(shape); 
			}
			if(this.shape != null)
				this.offset = this.shape.offset;
			else
				this.offset = new Vector3f();
			
			this.children = new ArrayList<Node>();
			if(data.has("children")) {
				JsonArray children = data.getAsJsonArray("children");
				for(JsonElement val : children.asList()) {
					if(val.isJsonObject()) {
						this.children.add(new Node(val.getAsJsonObject()));
					}
				}
			}
		}
		
		public Matrix getTransform(NodeAnimation animation, float frame) {
			if(animation == null)
				return Matrix.translate(position).mult(this.orientation.toMatrix().mult(Matrix.translate(offset)));
			// We have animation, so apply it.
			Vector3f animPosition = new Vector3f(0f, 0f, 0f);
			if(animation.getPosition() != null)
				animation.getPosition().eval(frame, animPosition);
			Quaternion animOrientation = new Quaternion();
			if(animation.getOrientation() != null)
				animation.getOrientation().eval(frame, animOrientation);
			return Matrix.translate(animPosition).mult(Matrix.translate(position).mult(this.orientation.toMatrix().mult(
					animOrientation.toMatrix().mult(Matrix.translate(offset)))));
		}
		
		public void addGeometry(Model model, Matrix parentTransform, boolean forceSingleSided, 
								BlockAnimationHandlerHytale animationHandler, float frame) {
			NodeAnimation animation = null;
			if(animationHandler != null)
				animation = animationHandler.getNodeAnimation(name);
			Matrix transform = parentTransform.mult(getTransform(animation, frame));
			if(this.shape != null) {
				this.shape.addGeometry(model, transform, forceSingleSided, animation, frame);
			}
			for(Node child : this.children) {
				child.addGeometry(model, transform, forceSingleSided, animationHandler, frame);
			}
		}
		
		/**
		 * Returns 0 for single sided,
		 * Returns 1 for double sided,
		 * Returns -1 for varying,
		 * Return -2 for no shape.
		 */
		public int getDoubleSided() {
			int shapeDoubleSided = -2;
			if(this.shape != null)
				shapeDoubleSided = this.shape.doubleSided ? 1 : 0;
			for(Node child : this.children) {
				int doubleSided = child.getDoubleSided();
				if(doubleSided == -2)
					continue;
				if(doubleSided == -1)
					return -1;
				if(shapeDoubleSided == -2)
					shapeDoubleSided = doubleSided;
				else if(doubleSided != shapeDoubleSided)
					return -1;
			}
			return shapeDoubleSided;
		}
		
	}
	
	private static class FaceUVs{
		
		public float offsetU;
		public float offsetV;
		public boolean mirrorU;
		public boolean mirrorV;
		public float angle;
		
		public FaceUVs(JsonObject data) {
			this.offsetU = 0f;
			this.offsetV = 0f;
			this.mirrorU = false;
			this.mirrorV = false;
			this.angle = 0f;
			if(data == null)
				return;
			if(data.has("offset")) {
				JsonObject offset = data.getAsJsonObject("offset");
				if(offset.has("x"))
					this.offsetU = offset.get("x").getAsFloat();
				if(offset.has("y"))
					this.offsetV = offset.get("y").getAsFloat();
			}
			if(data.has("mirror")) {
				JsonObject mirror = data.getAsJsonObject("mirror");
				if(mirror.has("x"))
					this.mirrorU = mirror.get("x").getAsBoolean();
				if(mirror.has("y"))
					this.mirrorV = mirror.get("y").getAsBoolean();
			}
			if(data.has("angle"))
				this.angle = data.get("angle").getAsFloat();
		}
		
	}
	
	private static enum Faces{
		SOUTH("front", "+Z", "#south", Direction.SOUTH), 
		NORTH("back", "-Z", "#north", Direction.NORTH), 
		EAST("right", "+X", "#east", Direction.EAST), 
		WEST("left", "-X", "#west", Direction.WEST), 
		TOP("top", "+Y", "#up", Direction.UP), 
		BOTTOM("bottom", "-Y", "#down", Direction.DOWN);
		
		private String id;
		private String normalId;
		private String tex;
		private Direction dir;
		
		Faces(String id, String normalId, String tex, Direction dir){
			this.id = id;
			this.normalId = normalId;
			this.tex = tex;
			this.dir = dir;
		}
	}
	
	private static abstract class Shape{
		
		public Vector3f offset;
		public Vector3f stretch;
		public Vector3f size;
		public boolean visible;
		public boolean doubleSided;
		public boolean reverseDirections;
		@SuppressWarnings("unused")
		public String shadingMode;
		@SuppressWarnings("unused")
		public String unwrapMode;
		public Map<String, FaceUVs> textureLayout;
		public Faces[] faces;
		
		public Shape(JsonObject data){
			this.offset = new Vector3f();
			if(data.has("offset")) {
				JsonObject offset = data.getAsJsonObject("offset");
				if(offset.has("x"))
					this.offset.x = offset.get("x").getAsFloat();
				if(offset.has("y"))
					this.offset.y = offset.get("y").getAsFloat();
				if(offset.has("z"))
					this.offset.z = offset.get("z").getAsFloat();
			}
			this.stretch = new Vector3f(1f);
			if(data.has("stretch")) {
				JsonObject stretch = data.getAsJsonObject("stretch");
				if(stretch.has("x"))
					this.stretch.x = stretch.get("x").getAsFloat();
				if(stretch.has("y"))
					this.stretch.y = stretch.get("y").getAsFloat();
				if(stretch.has("z"))
					this.stretch.z = stretch.get("z").getAsFloat();
			}
			int numFlips = 0;
			if(this.stretch.x < 0f)
				numFlips++;
			if(this.stretch.y < 0f)
				numFlips++;
			if(this.stretch.z < 0f)
				numFlips++;
			reverseDirections = numFlips == 1 || numFlips == 3;
			
			this.size = new Vector3f();
			if(data.has("settings")) {
				JsonObject settings = data.getAsJsonObject("settings");
				if(settings.has("size")) {
					JsonObject size = settings.getAsJsonObject("size");
					if(size.has("x"))
						this.size.x = size.get("x").getAsFloat();
					if(size.has("y"))
						this.size.y = size.get("y").getAsFloat();
					if(size.has("z"))
						this.size.z = size.get("z").getAsFloat();
				}
			}
			
			this.visible = true;
			if(data.has("visible"))
				this.visible = data.get("visible").getAsBoolean();
			
			this.doubleSided = false;
			if(data.has("doubleSided"))
				this.doubleSided = data.get("doubleSided").getAsBoolean();
			
			this.shadingMode = "";
			if(data.has("shadingMode"))
				this.shadingMode = data.get("shadingMode").getAsString();
			
			this.unwrapMode = "custom";
			if(data.has("unwrapMode"))
				this.unwrapMode = data.get("unwrapMode").getAsString();
			
			this.textureLayout = new HashMap<String, FaceUVs>();
			if(data.has("textureLayout")) {
				JsonObject textureLayout = data.getAsJsonObject("textureLayout");
				for(Entry<String, JsonElement> entry : textureLayout.entrySet()) {
					if(entry.getValue().isJsonObject()) {
						this.textureLayout.put(entry.getKey(), new FaceUVs(entry.getValue().getAsJsonObject()));
					}
				}
			}
		}
		
		public void addGeometry(Model model, Matrix transform, boolean forceSingleSided, 
								NodeAnimation animation, float frame) {
			if(animation != null && animation.getShapeVisible() != null) {
				Vector3f animVisible = new Vector3f();
				animation.getShapeVisible().eval(frame, animVisible);
				if(animVisible.x < 0.999f)
					return;
			}else {
				if(this.visible == false)
					return;
			}
			
			float stretchX = this.stretch.x;
			float stretchY = this.stretch.y;
			float stretchZ = this.stretch.z;
			if(animation != null && animation.getShapeStretch() != null) {
				Vector3f animStretch = new Vector3f();
				animation.getShapeStretch().eval(frame, animStretch);
				stretchX *= animStretch.x;
				stretchY *= animStretch.y;
				stretchZ *= animStretch.z;
			}
			
			float[] minMaxPoints = new float[] {
					0f - this.size.x * 0.5f * stretchX, 
					0f - this.size.y * 0.5f * stretchY, 
					0f - this.size.z * 0.5f * stretchZ,
					0f + this.size.x * 0.5f * stretchX, 
					0f + this.size.y * 0.5f * stretchY, 
					0f + this.size.z * 0.5f * stretchZ
			};
			
			for(Faces face : this.faces) {
				ModelFace modelFace = model.addFace(minMaxPoints, null, face.dir, face.tex, 0f, -1);
				
				FaceUVs uvs = this.textureLayout.getOrDefault(face.id, null);
				if(uvs == null) {
					uvs = new FaceUVs(null);
				}
				
				float offsetU = uvs.offsetU;
				float offsetV = uvs.offsetV;
				if(animation != null && animation.getShapeUvOffset() != null) {
					Vector3f animUvOffset = new Vector3f();
					animation.getShapeUvOffset().eval(frame, animUvOffset);
					offsetU += animUvOffset.x;
					offsetV += animUvOffset.y;
				}
				
				float minU = offsetU;
				float minV = offsetV;
				float maxU = minU;
				float maxV = minV;
				switch(face) {
				case NORTH:
				case SOUTH:
					maxU = minU + this.size.x * (uvs.mirrorU ? -1f : 1f);
					maxV = minV + this.size.y * (uvs.mirrorV ? -1f : 1f);
					break;
				case EAST:
				case WEST:
					maxU = minU + this.size.z * (uvs.mirrorU ? -1f : 1f);
					maxV = minV + this.size.y * (uvs.mirrorV ? -1f : 1f);
					break;
				case TOP:
				case BOTTOM:
					maxU = minU + this.size.x * (uvs.mirrorU ? -1f : 1f);
					maxV = minV + this.size.z * (uvs.mirrorV ? -1f : 1f);
					break;
				}
				// We need to flip minV and maxV to get the UVs to look right.
				float tmp = minV;
				minV = maxV;
				maxV = tmp;
				if(minU == maxU || minV == maxV) {
					throw new RuntimeException("UVs are zero");
				}

				modelFace.getUVs()[0] = minU;
				modelFace.getUVs()[1] = minV;

				modelFace.getUVs()[2] = maxU;
				modelFace.getUVs()[3] = minV;

				modelFace.getUVs()[4] = maxU;
				modelFace.getUVs()[5] = maxV;
				
				modelFace.getUVs()[6] = minU;
				modelFace.getUVs()[7] = maxV;
				
				if(uvs.angle != 0) {
					float[] oldUVs = Arrays.copyOf(modelFace.getUVs(), modelFace.getUVs().length);
					float rotation = uvs.angle;
					float cosR = (float) Math.cos(Math.toRadians(rotation));
					float sinR = (float) Math.sin(Math.toRadians(rotation));
					float pivotX = minU;
					float pivotY = maxV;
					for(int i = 0; i < 8; i += 2) {
						modelFace.getUVs()[i] = (oldUVs[i] - pivotX) * cosR + (oldUVs[i+1] - pivotY) * -sinR + pivotX;
						modelFace.getUVs()[i+1] = (oldUVs[i] - pivotX) * sinR + (oldUVs[i+1] - pivotY) * cosR + pivotY;
					}
				}
				
				if(reverseDirections)
					modelFace.reverseDirection();
				modelFace.transform(transform);
				
				if(forceSingleSided && this.doubleSided) {
					ModelFace backModelFace = new ModelFace(modelFace);
					backModelFace.reverseDirection();
					model.getFaces().add(backModelFace);
				}
			}
		}
		
	}
	
	private static class ShapeNone extends Shape{
		
		public ShapeNone(JsonObject data) {
			super(data);
			this.faces = new Faces[0];
		}
		
	}
	
	private static class ShapeBox extends Shape{
		
		public ShapeBox(JsonObject data) {
			super(data);
			this.faces = Faces.values();
			if(data.has("textureLayout")) {
				JsonObject textureLayout = data.getAsJsonObject("textureLayout");
				if(textureLayout.size() != this.faces.length) {
					// We aren't exporting out every face.
					this.faces = new Faces[textureLayout.size()];
					int i = 0;
					for(String key : textureLayout.keySet()) {
						for(Faces face : Faces.values()) {
							if(face.id.equals(key)) {
								this.faces[i] = face;
								break;
							}
						}
						if(this.faces[i] == null)
							this.faces[i] = Faces.NORTH;
						i++;
					}
				}
			}
			
			if(this.size.x < 0.001f || this.size.y < 0.001f || this.size.z < 0.001f) {
				throw new RuntimeException("Zero size");
			}
		}
		
	}
	
	private static class ShapeQuad extends Shape{
		
		public String normal;
		
		public ShapeQuad(JsonObject data) {
			super(data);
			
			this.normal = "+Z";
			if(data.has("settings")) {
				JsonObject settings = data.getAsJsonObject("settings");
				if(settings.has("normal")) {
					this.normal = settings.get("normal").getAsString();
				}
			}
			if(this.size.x < 0.001f || this.size.y < 0.001f) {
				throw new RuntimeException("Zero size");
			}
			for(Faces face : Faces.values()) {
				if(face.normalId.equals(this.normal)) {
					this.faces = new Faces[] {face};
					
					switch(face) {
					case NORTH:
					case SOUTH:
						break;
					case EAST:
					case WEST:
						this.size.z = this.size.x;
						this.size.x = 0f;
					case TOP:
					case BOTTOM:
						this.size.z = this.size.y;
						this.size.y = 0f;
					}
					
					break;
				}
			}
			if(this.faces == null) {
				this.faces = new Faces[0];
			}
		}
		
	}

}
