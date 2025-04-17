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

package nl.bramstout.mcworldexporter.model.builtins;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.expression.ExprContext;
import nl.bramstout.mcworldexporter.expression.ExprParser;
import nl.bramstout.mcworldexporter.expression.ExprValue;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueFunction;
import nl.bramstout.mcworldexporter.expression.Expression;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class BuiltInModel {

	public static abstract class Part{
		
		public abstract void eval(ExprContext context);
		
		public static Part parsePart(JsonElement data) {
			if(data.isJsonArray())
				return new ArrayPart(data.getAsJsonArray());
			else if(data.isJsonObject())
				return new ObjectPart(data.getAsJsonObject());
			return null;
		}
		
	}
	
	public static class ArrayPart extends Part{
		
		private Part[] children;
		
		public ArrayPart(JsonArray data) {
			if(data == null) {
				children = new Part[0];
			}
			children = new Part[data.size()];
			int j = 0;
			for(int i = 0; i < data.size(); ++i) {
				Part part = Part.parsePart(data.get(i));
				if(part != null) {
					children[j] = part;
					j++;
				}
			}
			if(j != children.length)
				children = Arrays.copyOf(children, j);
		}
		
		public void addChild(Part part) {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = part;
		}
		
		@Override
		public void eval(ExprContext context) {
			for(int i = 0; i < children.length; ++i) {
				children[i].eval(context);
			}
		}
		
	}
	
	public static class ElementFace{
		
		private Direction dir;
		private Expression minU;
		private Expression minV;
		private Expression maxU;
		private Expression maxV;
		private Expression texture;
		private Expression rotation;
		private boolean affineRotation;
		private Expression tintIndex;
		private Expression entityUVsMinU;
		private Expression entityUVsMinV;
		private Expression entityUVsMaxU;
		private Expression entityUVsMaxV;
		
		public ElementFace(Direction dir, Expression texture, Expression entityUVsMinU, Expression entityUVsMinV, 
							Expression entityUVsMaxU, Expression entityUVsMaxV, JsonObject data) {
			this.dir = dir;
			this.entityUVsMinU = entityUVsMinU;
			this.entityUVsMinV = entityUVsMinV;
			this.entityUVsMaxU = entityUVsMaxU;
			this.entityUVsMaxV = entityUVsMaxV;
			this.texture = texture;
			this.affineRotation = false;
			if(data == null)
				return;
			
			if(data.has("uv") && data.getAsJsonArray("uv").size() == 4) {
				JsonArray uv = data.getAsJsonArray("uv");
				minU = ExprParser.parseMultiExpression(uv.get(0).getAsString());
				minV = ExprParser.parseMultiExpression(uv.get(1).getAsString());
				maxU = ExprParser.parseMultiExpression(uv.get(2).getAsString());
				maxV = ExprParser.parseMultiExpression(uv.get(3).getAsString());
			}
			
			if(data.has("texture")) {
				this.texture = ExprParser.parseMultiExpression(data.get("texture").getAsString());
			}
			if(data.has("rotation")) {
				rotation = ExprParser.parseMultiExpression(data.get("rotation").getAsString());
			}
			
			if(data.has("affineRotation")) {
				affineRotation = data.get("affineRotation").getAsBoolean();
			}
			if(data.has("tintindex")) {
				tintIndex = ExprParser.parseMultiExpression(data.get("tintindex").getAsString());
			}
		}
		
		public void addFaceToModel(ExprContext context, float[] bounds, JsonObject rotateData) {
			JsonObject faceData = new JsonObject();
			
			if(entityUVsMinU != null) {
				float minU = entityUVsMinU.eval(context).asFloat();
				float minV = entityUVsMinV.eval(context).asFloat();
				float maxU = entityUVsMaxU.eval(context).asFloat();
				float maxV = entityUVsMaxV.eval(context).asFloat();
				// Use entity UVs
				float width = Math.abs(bounds[3] - bounds[0]);
				float height = Math.abs(bounds[4] - bounds[1]);
				float depth = Math.abs(bounds[5] - bounds[2]);
				float uvWidth = maxU - minU;
				float uvHeight = maxV - minV;
				uvWidth /= (depth + width + depth + width);
				uvHeight /= (depth + height);
				float uvX = 0;
				float uvY = 0;
				float uvW = 0;
				float uvH = 0;
				
				switch(dir) {
				case DOWN:
					uvX = (depth + width) * uvWidth + minU;
					uvY = (0) * uvHeight + minV;
					uvW = width * uvWidth;
					uvH = depth * uvHeight;
					break;
				case UP:
					uvX = (depth) * uvWidth + minU;
					uvY = (0) * uvHeight + minV;
					uvW = width * uvWidth;
					uvH = depth * uvHeight;
					break;
				case NORTH:
					uvX = (depth + width + depth) * uvWidth + minU;
					uvY = (depth) * uvHeight + minV;
					uvW = width * uvWidth;
					uvH = height * uvHeight;
					break;
				case SOUTH:
					uvX = (depth) * uvWidth + minU;
					uvY = (depth) * uvHeight + minV;
					uvW = width * uvWidth;
					uvH = height * uvHeight;
					break;
				case EAST:
					uvX = (depth + width) * uvWidth + minU;
					uvY = (depth) * uvHeight + minV;
					uvW = depth * uvWidth;
					uvH = height * uvHeight;
					break;
				case WEST:
					uvX = (0) * uvWidth + minU;
					uvY = (depth) * uvHeight + minV;
					uvW = depth * uvWidth;
					uvH = height * uvHeight;
					break;
				}
				JsonArray uv = new JsonArray();
				uv.add(uvX);
				uv.add(uvY);
				uv.add(uvX + uvW);
				uv.add(uvY + uvH);
				faceData.add("uv", uv);
			}
			
			if(minU != null) {
				JsonArray uv = new JsonArray();
				uv.add(minU.eval(context).asFloat());
				uv.add(minV.eval(context).asFloat());
				uv.add(maxU.eval(context).asFloat());
				uv.add(maxV.eval(context).asFloat());
				faceData.add("uv", uv);
			}
			if(texture != null) {
				String textureStr = texture.eval(context).asString();
				if(!textureStr.startsWith("#"))
					textureStr = "#" + textureStr;
				faceData.addProperty("texture", textureStr);
			}
			if(rotation != null) {
				faceData.addProperty("rotation", rotation.eval(context).asInt());
			}
			if(affineRotation) {
				faceData.addProperty("rotationMiEx", true);
			}
			if(tintIndex != null) {
				faceData.addProperty("tintindex", tintIndex.eval(context).asInt());
			}
			ModelFace face = new ModelFace(bounds, dir, faceData, context.model.isDoubleSided());
			if(face.isValid()) {
				face.rotate(rotateData);
				context.model.getFaces().add(face);
			}
		}
		
	}
	
	public static class Element{
		
		private Expression minX;
		private Expression minY;
		private Expression minZ;
		private Expression maxX;
		private Expression maxY;
		private Expression maxZ;
		
		private String rotateAxis;
		private Expression rotatePivotX;
		private Expression rotatePivotY;
		private Expression rotatePivotZ;
		private Expression rotateAngle;
		private boolean rotateRescale = false;
		
		private Expression entityUVsMinU;
		private Expression entityUVsMinV;
		private Expression entityUVsMaxU;
		private Expression entityUVsMaxV;
		
		private ElementFace[] faces;
		
		public Element(JsonObject data) {
			faces = new ElementFace[0];
			
			if(data.has("from")) {
				JsonArray from = data.getAsJsonArray("from");
				if(from.size() > 0) {
					minX = ExprParser.parseMultiExpression(from.get(0).getAsString());
					minY = minX;
					minZ = minX;
				}
				if(from.size() > 1) {
					minY = ExprParser.parseMultiExpression(from.get(1).getAsString());
					minZ = minY;
				}
				if(from.size() > 2) {
					minZ = ExprParser.parseMultiExpression(from.get(2).getAsString());
				}
			}
			if(data.has("to")) {
				JsonArray to = data.getAsJsonArray("to");
				if(to.size() > 0) {
					maxX = ExprParser.parseMultiExpression(to.get(0).getAsString());
					maxY = maxX;
					maxZ = maxX;
				}
				if(to.size() > 1) {
					maxY = ExprParser.parseMultiExpression(to.get(1).getAsString());
					maxZ = maxY;
				}
				if(to.size() > 2) {
					maxZ = ExprParser.parseMultiExpression(to.get(2).getAsString());
				}
			}
			
			if(data.has("entityUVs")) {
				JsonArray uvs = data.getAsJsonArray("entityUVs");
				if(uvs.size() == 4) {
					entityUVsMinU = ExprParser.parseMultiExpression(uvs.get(0).getAsString());
					entityUVsMinV = ExprParser.parseMultiExpression(uvs.get(1).getAsString());
					entityUVsMaxU = ExprParser.parseMultiExpression(uvs.get(2).getAsString());
					entityUVsMaxV = ExprParser.parseMultiExpression(uvs.get(3).getAsString());
				}
			}
			
			if(data.has("rotation")) {
				JsonObject rotation = data.getAsJsonObject("rotation");
				if(rotation.has("origin")) {
					JsonArray origin = rotation.getAsJsonArray("origin");
					if(origin.size() > 0) {
						rotatePivotX = ExprParser.parseMultiExpression(origin.get(0).getAsString());
						rotatePivotY = rotatePivotX;
						rotatePivotZ = rotatePivotX;
					}
					if(origin.size() > 1) {
						rotatePivotY = ExprParser.parseMultiExpression(origin.get(1).getAsString());
						rotatePivotZ = rotatePivotY;
					}
					if(origin.size() > 2) {
						rotatePivotZ = ExprParser.parseMultiExpression(origin.get(2).getAsString());
					}
				}
				if(rotation.has("axis")) {
					rotateAxis = rotation.get("axis").getAsString();
				}
				if(rotation.has("angle")) {
					rotateAngle = ExprParser.parseMultiExpression(rotation.get("angle").getAsString());
				}
				if(rotation.has("rescale")) {
					rotateRescale = rotation.get("rescale").getAsBoolean();
				}
			}
			Expression defaultTexture = null;
			if(data.has("texture"))
				defaultTexture = ExprParser.parseMultiExpression(data.get("texture").getAsString());
			if(data.has("faces")) {
				for(Entry<String, JsonElement> entry : data.getAsJsonObject("faces").entrySet()) {
					Direction dir = Direction.getDirection(entry.getKey());
					if(entry.getValue().isJsonObject()) {
						addFace(new ElementFace(dir, defaultTexture, entityUVsMinU, entityUVsMinV, entityUVsMaxU, 
												entityUVsMaxV, entry.getValue().getAsJsonObject()));
					}
				}
			}else {
				for (Direction dir : Direction.CACHED_VALUES) {
					addFace(new ElementFace(dir, defaultTexture, entityUVsMinU, entityUVsMinV, entityUVsMaxU, entityUVsMaxV, null));
				}
			}
		}
		
		private void addFace(ElementFace face) {
			faces = Arrays.copyOf(faces, faces.length + 1);
			faces[faces.length - 1] = face;
		}
		
		public void addElementToModel(ExprContext context) {
			float minX = 0;
			float minY = 0;
			float minZ = 0;
			float maxX = 0;
			float maxY = 0;
			float maxZ = 0;
			
			if(this.minX != null)
				minX = this.minX.eval(context).asFloat();
			if(this.minY != null)
				minY = this.minY.eval(context).asFloat();
			if(this.minZ != null)
				minZ = this.minZ.eval(context).asFloat();
			if(this.maxX != null)
				maxX = this.maxX.eval(context).asFloat();
			if(this.maxY != null)
				maxY = this.maxY.eval(context).asFloat();
			if(this.maxZ != null)
				maxZ = this.maxZ.eval(context).asFloat();
			
			JsonObject rotateData = null;
			if(rotateAxis != null && (rotateAxis.equals("x") || rotateAxis.equals("y") || rotateAxis.equals("z"))) {
				rotateData = new JsonObject();
				rotateData.addProperty("axis", rotateAxis);
				float originX = 8f;
				float originY = 8f;
				float originZ = 8f;
				if(rotatePivotX != null)
					originX = rotatePivotX.eval(context).asFloat();
				if(rotatePivotY != null)
					originY = rotatePivotY.eval(context).asFloat();
				if(rotatePivotZ != null)
					originZ = rotatePivotZ.eval(context).asFloat();
				JsonArray origin = new JsonArray();
				origin.add(originX);
				origin.add(originY);
				origin.add(originZ);
				rotateData.add("origin", origin);
				float angle = 0;
				if(rotateAngle != null)
					angle = rotateAngle.eval(context).asFloat();
				rotateData.addProperty("angle", angle);
				rotateData.addProperty("rescale", rotateRescale);
			}
			
			float[] bounds = new float[] { minX, minY, minZ, maxX, maxY, maxZ };
			for(ElementFace face : faces) {
				face.addFaceToModel(context, bounds, rotateData);
			}
		}
		
	}
	
	public static class Transformation{
		
		private Expression translateX;
		private Expression translateY;
		private Expression translateZ;
		
		private Expression rotateX;
		private Expression rotateY;
		private Expression rotateZ;
		private boolean uvLock;
		private boolean mcRotationMode;
		
		private Expression quaternionX;
		private Expression quaternionY;
		private Expression quaternionZ;
		private Expression quaternionW;
		
		private Expression axisAngleX;
		private Expression axisAngleY;
		private Expression axisAngleZ;
		private Expression axisAngleA;
		
		private Expression scaleX;
		private Expression scaleY;
		private Expression scaleZ;
		
		private Expression pivotX;
		private Expression pivotY;
		private Expression pivotZ;
		
		public Transformation(JsonObject data) {
			if(data == null)
				return;
			
			if(data.has("translate")) {
				JsonArray array = data.getAsJsonArray("translate");
				if(array.size() > 0) {
					translateX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					translateY = translateX;
					translateZ = translateX;
				}
				if(array.size() > 1) {
					translateY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					translateZ = translateY;
				}
				if(array.size() > 2) {
					translateZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
				}
			}
			
			if(data.has("rotate")) {
				JsonArray array = data.getAsJsonArray("rotate");
				if(array.size() > 0) {
					rotateX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					rotateY = rotateX;
					rotateZ = rotateX;
				}
				if(array.size() > 1) {
					rotateY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					rotateZ = rotateY;
				}
				if(array.size() > 2) {
					rotateZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
				}
			}
			if(data.has("mcRotationMode")) {
				mcRotationMode = data.get("mcRotationMode").getAsBoolean();
			}
			if(data.has("uvLock")) {
				uvLock = data.get("uvLock").getAsBoolean();
			}
			
			if(data.has("quaternion")) {
				JsonArray array = data.getAsJsonArray("quaternion");
				if(array.size() > 0) {
					quaternionX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					quaternionY = quaternionX;
					quaternionZ = quaternionX;
					quaternionW = quaternionX;
				}
				if(array.size() > 1) {
					quaternionY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					quaternionZ = quaternionY;
					quaternionW = quaternionY;
				}
				if(array.size() > 2) {
					quaternionZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
					quaternionW = quaternionZ;
				}
				if(array.size() > 3) {
					quaternionW = ExprParser.parseMultiExpression(array.get(3).getAsString());
				}
			}
			
			if(data.has("axisAngle")) {
				JsonArray array = data.getAsJsonArray("axisAngle");
				if(array.size() > 0) {
					axisAngleX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					axisAngleY = axisAngleX;
					axisAngleZ = axisAngleX;
					axisAngleA = axisAngleX;
				}
				if(array.size() > 1) {
					axisAngleY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					axisAngleZ = axisAngleY;
					axisAngleA = axisAngleY;
				}
				if(array.size() > 2) {
					axisAngleZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
					axisAngleA = axisAngleZ;
				}
				if(array.size() > 3) {
					axisAngleA = ExprParser.parseMultiExpression(array.get(3).getAsString());
				}
			}
			
			if(data.has("scale")) {
				JsonArray array = data.getAsJsonArray("scale");
				if(array.size() > 0) {
					scaleX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					scaleY = scaleX;
					scaleZ = scaleX;
				}
				if(array.size() > 1) {
					scaleY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					scaleZ = scaleY;
				}
				if(array.size() > 2) {
					scaleZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
				}
			}
			
			if(data.has("pivot")) {
				JsonArray array = data.getAsJsonArray("pivot");
				if(array.size() > 0) {
					pivotX = ExprParser.parseMultiExpression(array.get(0).getAsString());
					pivotY = pivotX;
					pivotZ = pivotX;
				}
				if(array.size() > 1) {
					pivotY = ExprParser.parseMultiExpression(array.get(1).getAsString());
					pivotZ = pivotY;
				}
				if(array.size() > 2) {
					pivotZ = ExprParser.parseMultiExpression(array.get(2).getAsString());
				}
			}
		}
		
		public static Transformation parseTransformation(JsonElement data) {
			if(data.isJsonArray())
				return new TransformationArray(data.getAsJsonArray());
			else if(data.isJsonObject())
				return new Transformation(data.getAsJsonObject());
			return null;
		}
		
		public void apply(ExprContext context, Model model, int faceStartIndex, int faceEndIndex) {
			float translateX = 0f;
			float translateY = 0f;
			float translateZ = 0f;
			float rotateX = 0f;
			float rotateY = 0f;
			float rotateZ = 0f;
			float quaternionX = 0f;
			float quaternionY = 0f;
			float quaternionZ = 0f;
			float quaternionW = 1f;
			float axisAngleX = 0f;
			float axisAngleY = 0f;
			float axisAngleZ = 0f;
			float axisAngleA = 0f;
			float scaleX = 1f;
			float scaleY = 1f;
			float scaleZ = 1f;
			float pivotX = 8f;
			float pivotY = 8f;
			float pivotZ = 8f;
			
			if(this.translateX != null)
				translateX = this.translateX.eval(context).asFloat();
			if(this.translateY != null)
				translateY = this.translateY.eval(context).asFloat();
			if(this.translateZ != null)
				translateZ = this.translateZ.eval(context).asFloat();
			
			if(this.rotateX != null)
				rotateX = this.rotateX.eval(context).asFloat();
			if(this.rotateY != null)
				rotateY = this.rotateY.eval(context).asFloat();
			if(this.rotateZ != null)
				rotateZ = this.rotateZ.eval(context).asFloat();
			
			if(this.quaternionX != null)
				quaternionX = this.quaternionX.eval(context).asFloat();
			if(this.quaternionY != null)
				quaternionY = this.quaternionY.eval(context).asFloat();
			if(this.quaternionZ != null)
				quaternionZ = this.quaternionZ.eval(context).asFloat();
			if(this.quaternionW != null)
				quaternionW = this.quaternionW.eval(context).asFloat();
			
			if(this.axisAngleX != null)
				axisAngleX = this.axisAngleX.eval(context).asFloat();
			if(this.axisAngleY != null)
				axisAngleY = this.axisAngleY.eval(context).asFloat();
			if(this.axisAngleZ != null)
				axisAngleZ = this.axisAngleZ.eval(context).asFloat();
			if(this.axisAngleA != null)
				axisAngleA = this.axisAngleA.eval(context).asFloat();
			
			if(this.scaleX != null)
				scaleX = this.scaleX.eval(context).asFloat();
			if(this.scaleY != null)
				scaleY = this.scaleY.eval(context).asFloat();
			if(this.scaleZ != null)
				scaleZ = this.scaleZ.eval(context).asFloat();
			
			if(this.pivotX != null)
				pivotX = this.pivotX.eval(context).asFloat();
			if(this.pivotY != null)
				pivotY = this.pivotY.eval(context).asFloat();
			if(this.pivotZ != null)
				pivotZ = this.pivotZ.eval(context).asFloat();
			
			Matrix quaternionMatrix = null;
			if(quaternionX != 0f || quaternionY != 0f || quaternionZ != 0f || quaternionW != 1f)
				quaternionMatrix = Matrix.translate(pivotX, pivotY, pivotZ).mult(
					Matrix.quaternion(quaternionX, quaternionY, quaternionZ, quaternionW).mult(
							Matrix.translate(-pivotX, -pivotY, -pivotZ)));
			Matrix axisAngleMatrix = null;
			if(axisAngleX != 0f || axisAngleY != 0f || axisAngleZ != 0f || axisAngleA != 0f)
				axisAngleMatrix = Matrix.translate(pivotX, pivotY, pivotZ).mult(
					Matrix.axisAngle(axisAngleX, axisAngleY, axisAngleZ, axisAngleA).mult(
							Matrix.translate(-pivotX, -pivotY, -pivotZ)));
			
			for(int i = faceStartIndex; i < faceEndIndex; ++i) {
				ModelFace face = model.getFaces().get(i);
				if(scaleX != 1f || scaleY != 1f || scaleZ != 1f)
					face.scale(scaleX, scaleY, scaleZ, pivotX, pivotY, pivotZ);
				
				if(mcRotationMode) {
					if(rotateX != 0f || rotateY != 0f)
						face.rotate(rotateX, rotateY, uvLock);
				}else {
					if(rotateX != 0f || rotateY != 0f || rotateZ != 0f)
						face.rotate(rotateX, rotateY, rotateZ, pivotX, pivotY, pivotZ);
				}
				
				if(quaternionMatrix != null)
					face.transform(quaternionMatrix);
				
				if(axisAngleMatrix != null)
					face.transform(axisAngleMatrix);
				
				if(translateX != 0f || translateY != 0f || translateZ != 0f)
					face.translate(translateX, translateY, translateZ);
			}
		}
		
	}
	
	public static class TransformationArray extends Transformation{
		
		private Transformation[] transformations;
		
		public TransformationArray(JsonArray data){
			super(null);
			transformations = new Transformation[0];
			for(JsonElement el : data.asList()) {
				Transformation transformation = Transformation.parseTransformation(el);
				if(transformation != null) {
					transformations = Arrays.copyOf(transformations, transformations.length + 1);
					transformations[transformations.length - 1] = transformation;
				}
			}
		}
		
		@Override
		public void apply(ExprContext context, Model model, int faceStartIndex, int faceEndIndex) {
			for(Transformation transformation : transformations)
				transformation.apply(context, model, faceStartIndex, faceEndIndex);
		}
		
	}
	
	public static class ObjectPart extends Part{
		
		private Expression condition;
		private Expression loopInit;
		private Expression loopCondition;
		private Expression loopIncrement;
		private Expression[] subExpressions;
		private Element[] elements;
		private Part[] children;
		private Transformation transformation;
		
		public ObjectPart(JsonObject data) {
			children = new Part[0];
			subExpressions = new Expression[0];
			elements = new Element[0];
			transformation = null;
			if(data.has("condition"))
				condition = ExprParser.parseMultiExpression(data.get("condition").getAsString());
			if(data.has("loopInit"))
				loopInit = ExprParser.parseMultiExpression(data.get("loopInit").getAsString());
			if(data.has("loopCondition"))
				loopCondition = ExprParser.parseMultiExpression(data.get("loopCondition").getAsString());
			if(data.has("loopIncrement"))
				loopIncrement = ExprParser.parseMultiExpression(data.get("loopIncrement").getAsString());
			if(data.has("variables")) {
				JsonElement variablesData = data.get("variables");
				if(variablesData.isJsonArray()) {
					for(JsonElement el : variablesData.getAsJsonArray().asList()) {
						addSubExpression(ExprParser.parseMultiExpression(el.getAsString()));
					}
				}else if(variablesData.isJsonPrimitive()) {
					addSubExpression(ExprParser.parseMultiExpression(variablesData.getAsString()));
				}
			}
			if(data.has("textures"))
				addChild(new TexturePart(data.getAsJsonObject("textures")));
			if(data.has("generators"))
				addGenerator(data.get("generators"));
			if(data.has("elements")) {
				for(JsonElement el : data.getAsJsonArray("elements").asList()) {
					if(el.isJsonObject())
						addElement(new Element(el.getAsJsonObject()));
				}
			}
			if(data.has("children"))
				addChild(Part.parsePart(data.get("children")));
			if(data.has("transform")) {
				transformation = Transformation.parseTransformation(data.get("transform"));
			}
		}
		
		private void addElement(Element element) {
			elements = Arrays.copyOf(elements, elements.length + 1);
			elements[elements.length - 1] = element;
		}
		
		private void addSubExpression(Expression expr) {
			subExpressions = Arrays.copyOf(subExpressions, subExpressions.length + 1);
			subExpressions[subExpressions.length - 1] = expr;
		}
		
		private void addChild(Part part) {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = part;
		}
		
		private void addGenerator(JsonElement data) {
			if(data.isJsonArray()) {
				for(JsonElement el : data.getAsJsonArray().asList()) {
					addGenerator(el);
				}
			}else if(data.isJsonObject()) {
				addChild(new GeneratorPart(data.getAsJsonObject()));
			}
		}
		
		@Override
		public void eval(ExprContext context) {
			int faceStartIndex = context.model.getFaces().size();
			if(condition != null) {
				if(!condition.eval(context).asBool())
					return;
			}
			if(loopInit != null) {
				loopInit.eval(context);
			}
			if(loopCondition != null) {
				int counter = 0;
				while(loopCondition.eval(context).asBool()) {
					for(int i = 0; i < subExpressions.length; ++i) {
						subExpressions[i].eval(context);
					}
					for(int i = 0; i < elements.length; ++i) {
						elements[i].addElementToModel(context);
					}
					for(int i = 0; i < children.length; ++i) {
						children[i].eval(context);
					}
					if(loopIncrement != null) {
						loopIncrement.eval(context);
					}
					counter++;
					if(counter > 10000)
						throw new RuntimeException("Too many iterations in loop. Current limit if 10000 iterations");
				}
			}else {
				for(int i = 0; i < subExpressions.length; ++i) {
					subExpressions[i].eval(context);
				}
				for(int i =0 ; i < elements.length; ++i) {
					elements[i].addElementToModel(context);
				}
				for(int i = 0; i < children.length; ++i) {
					children[i].eval(context);
				}
			}
			if(transformation != null) {
				transformation.apply(context, context.model, faceStartIndex, context.model.getFaces().size());
			}
		}
		
	}
	
	public static class TexturePart extends Part{
		
		private Map<String, Expression> textures;
		
		public TexturePart(JsonObject data) {
			textures = new HashMap<String, Expression>();
			for(Entry<String, JsonElement> entry : data.entrySet()) {
				String name = entry.getKey();
				if(!name.startsWith("#"))
					name = "#" + name;
				textures.put(name, ExprParser.parseMultiExpression(entry.getValue().getAsString()));
			}
		}
		
		@Override
		public void eval(ExprContext context) {
			for(Entry<String, Expression> entry : textures.entrySet()) {
				context.model.addTexture(entry.getKey(), entry.getValue().eval(context).asString());
			}
		}
		
	}
	
	public static class GeneratorPart extends Part{
		
		private String generator;
		private Map<String, Expression> arguments;
		
		public GeneratorPart(JsonObject data) {
			String type = "";
			if(data.has("type"))
				type = data.get("type").getAsString();
			generator = type;
			arguments = new HashMap<String, Expression>();
			if(data.has("args")) {
				for(Entry<String, JsonElement> entry : data.getAsJsonObject("args").entrySet()) {
					arguments.put(entry.getKey(), ExprParser.parseMultiExpression(entry.getValue().getAsString()));
				}
			}
		}
		
		@Override
		public void eval(ExprContext context) {
			BuiltInGenerator generator = context.localGenerators.getOrDefault(this.generator, null);
			if(generator == null) {
				generator = BuiltInGenerator.getGenerator(this.generator);
			}
			if(generator != null)
				generator.eval(context, arguments);
		}
		
	}
	
	public static class CustomBuiltInGenerator extends BuiltInGenerator{

		private Part part;
		
		public CustomBuiltInGenerator(JsonElement data) {
			this.part = Part.parsePart(data);
		}
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			ExprContext context2 = new ExprContext(context);
			for(Entry<String, Expression> entry : arguments.entrySet()) {
				context2.variables.put(entry.getKey(), entry.getValue().eval(context));
			}
			part.eval(context2);
		}
		
	}
	
	
	
	public Part rootPart;
	public Map<String, BuiltInGenerator> localGenerators;
	public Map<String, ExprValue> localFunctions;
	public String defaultTexture;
	
	public BuiltInModel() {
		rootPart = null;
		localGenerators = new HashMap<String, BuiltInGenerator>();
		localFunctions = new HashMap<String, ExprValue>();
		defaultTexture = "";
	}
	
	public void parse(JsonObject data) {
		if(data.has("generators")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("generators").entrySet()) {
				localGenerators.put(entry.getKey(), new CustomBuiltInGenerator(entry.getValue()));
			}
		}
		
		if(data.has("defaultTexture")) {
			defaultTexture = data.get("defaultTexture").getAsString();
			if(!defaultTexture.contains(":"))
				defaultTexture = "minecraft:" + defaultTexture;
		}
		
		if(data.has("functions")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("functions").entrySet()) {
				String code = "";
				if(entry.getValue().isJsonPrimitive())
					code = entry.getValue().getAsString();
				else if(entry.getValue().isJsonArray()) {
					int i = 0;
					for(JsonElement el : entry.getValue().getAsJsonArray().asList()) {
						code += (i > 0 ? ";" : "") + el.getAsString();
						i++;
					}
				}
				localFunctions.put(entry.getKey(), new ExprValue(new ExprValueFunction(ExprParser.parseMultiExpression(code))));
			}
		}
		
		if(data.has("handler")) {
			Part handlerPart = Part.parsePart(data.get("handler"));
			if(rootPart == null)
				rootPart = handlerPart;
			else {
				if(rootPart instanceof ArrayPart) {
					((ArrayPart) rootPart).addChild(handlerPart);
				}else if(rootPart instanceof ObjectPart) {
					Part tmpPart = rootPart;
					rootPart = new ArrayPart(null);
					((ArrayPart) rootPart).addChild(tmpPart);
					((ArrayPart) rootPart).addChild(handlerPart);
				}
			}
		}
	}
	
}
