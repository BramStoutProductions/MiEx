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

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagLongArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagShort;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class BuiltInBlockState extends BlockState{

	public static class Context{
		
		public String name;
		public NbtTagCompound properties;
		public boolean isLocationDependent;
		public int x;
		public int y;
		public int z;
		public Model model;
		public Value thisBlock;
		public Map<String, Value> variables;
		public Value globals;
		public Map<String, Value> builtins;
		public Map<String, BuiltInGenerator> localGenerators;
		public Map<String, Value> localFunctions;
		public Value returnValue;
		
		public Context(String name, NbtTagCompound properties, boolean isLocationDependent, int x, int y, int z, Model model, 
						Value globals, Map<String, Value> builtins, Map<String, BuiltInGenerator> localGenerators,
						Map<String, Value> localFunctions) {
			this.name = name;
			this.properties = properties;
			this.isLocationDependent = isLocationDependent;
			this.x = x;
			this.y = y;
			this.z = z;
			this.model = model;
			this.thisBlock = new Value(new ValueThisBlock(this));
			this.variables = new HashMap<String, Value>();
			this.globals = globals;
			this.builtins = builtins;
			this.localGenerators = localGenerators;
			this.localFunctions = localFunctions;
			this.returnValue = null;
		}
		
		public Context(Context other) {
			this.name = other.name;
			this.properties = other.properties;
			this.isLocationDependent = other.isLocationDependent;
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
			this.model = other.model;
			this.thisBlock = new Value(new ValueThisBlock(this));
			this.variables = new HashMap<String, Value>();
			this.globals = other.globals;
			this.builtins = other.builtins;
			this.localGenerators = other.localGenerators;
			this.localFunctions = other.localFunctions;
			this.returnValue = null;
		}
		
	}
	
	private static abstract class Part{
		
		public abstract void eval(Context context);
		
		public static Part parsePart(JsonElement data) {
			if(data.isJsonArray())
				return new ArrayPart(data.getAsJsonArray());
			else if(data.isJsonObject())
				return new ObjectPart(data.getAsJsonObject());
			return null;
		}
		
	}
	
	private static class ArrayPart extends Part{
		
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
		
		private void addChild(Part part) {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = part;
		}
		
		@Override
		public void eval(Context context) {
			for(int i = 0; i < children.length; ++i) {
				children[i].eval(context);
			}
		}
		
	}
	
	private static class ElementFace{
		
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
				minU = new ExpressionMulti(uv.get(0).getAsString());
				minV = new ExpressionMulti(uv.get(1).getAsString());
				maxU = new ExpressionMulti(uv.get(2).getAsString());
				maxV = new ExpressionMulti(uv.get(3).getAsString());
			}
			
			if(data.has("texture")) {
				this.texture = new ExpressionMulti(data.get("texture").getAsString());
			}
			if(data.has("rotation")) {
				rotation = new ExpressionMulti(data.get("rotation").getAsString());
			}
			
			if(data.has("affineRotation")) {
				affineRotation = data.get("affineRotation").getAsBoolean();
			}
			if(data.has("tintindex")) {
				tintIndex = new ExpressionMulti(data.get("tintindex").getAsString());
			}
		}
		
		public void addFaceToModel(Context context, float[] bounds, JsonObject rotateData) {
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
	
	private static class Element{
		
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
					minX = new ExpressionMulti(from.get(0).getAsString());
					minY = minX;
					minZ = minX;
				}
				if(from.size() > 1) {
					minY = new ExpressionMulti(from.get(1).getAsString());
					minZ = minY;
				}
				if(from.size() > 2) {
					minZ = new ExpressionMulti(from.get(2).getAsString());
				}
			}
			if(data.has("to")) {
				JsonArray to = data.getAsJsonArray("to");
				if(to.size() > 0) {
					maxX = new ExpressionMulti(to.get(0).getAsString());
					maxY = maxX;
					maxZ = maxX;
				}
				if(to.size() > 1) {
					maxY = new ExpressionMulti(to.get(1).getAsString());
					maxZ = maxY;
				}
				if(to.size() > 2) {
					maxZ = new ExpressionMulti(to.get(2).getAsString());
				}
			}
			
			if(data.has("entityUVs")) {
				JsonArray uvs = data.getAsJsonArray("entityUVs");
				if(uvs.size() == 4) {
					entityUVsMinU = new ExpressionMulti(uvs.get(0).getAsString());
					entityUVsMinV = new ExpressionMulti(uvs.get(1).getAsString());
					entityUVsMaxU = new ExpressionMulti(uvs.get(2).getAsString());
					entityUVsMaxV = new ExpressionMulti(uvs.get(3).getAsString());
				}
			}
			
			if(data.has("rotation")) {
				JsonObject rotation = data.getAsJsonObject("rotation");
				if(rotation.has("origin")) {
					JsonArray origin = rotation.getAsJsonArray("origin");
					if(origin.size() > 0) {
						rotatePivotX = new ExpressionMulti(origin.get(0).getAsString());
						rotatePivotY = rotatePivotX;
						rotatePivotZ = rotatePivotX;
					}
					if(origin.size() > 1) {
						rotatePivotY = new ExpressionMulti(origin.get(1).getAsString());
						rotatePivotZ = rotatePivotY;
					}
					if(origin.size() > 2) {
						rotatePivotZ = new ExpressionMulti(origin.get(2).getAsString());
					}
				}
				if(rotation.has("axis")) {
					rotateAxis = rotation.get("axis").getAsString();
				}
				if(rotation.has("angle")) {
					rotateAngle = new ExpressionMulti(rotation.get("angle").getAsString());
				}
				if(rotation.has("rescale")) {
					rotateRescale = rotation.get("rescale").getAsBoolean();
				}
			}
			Expression defaultTexture = null;
			if(data.has("texture"))
				defaultTexture = new ExpressionMulti(data.get("texture").getAsString());
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
		
		public void addElementToModel(Context context) {
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
	
	private static class Transformation{
		
		private Expression translateX;
		private Expression translateY;
		private Expression translateZ;
		
		private Expression rotateX;
		private Expression rotateY;
		private Expression rotateZ;
		private boolean uvLock;
		private boolean mcRotationMode;
		
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
					translateX = new ExpressionMulti(array.get(0).getAsString());
					translateY = translateX;
					translateZ = translateX;
				}
				if(array.size() > 1) {
					translateY = new ExpressionMulti(array.get(1).getAsString());
					translateZ = translateY;
				}
				if(array.size() > 2) {
					translateZ = new ExpressionMulti(array.get(2).getAsString());
				}
			}
			
			if(data.has("rotate")) {
				JsonArray array = data.getAsJsonArray("rotate");
				if(array.size() > 0) {
					rotateX = new ExpressionMulti(array.get(0).getAsString());
					rotateY = rotateX;
					rotateZ = rotateX;
				}
				if(array.size() > 1) {
					rotateY = new ExpressionMulti(array.get(1).getAsString());
					rotateZ = rotateY;
				}
				if(array.size() > 2) {
					rotateZ = new ExpressionMulti(array.get(2).getAsString());
				}
			}
			if(data.has("mcRotationMode")) {
				mcRotationMode = data.get("mcRotationMode").getAsBoolean();
			}
			if(data.has("uvLock")) {
				uvLock = data.get("uvLock").getAsBoolean();
			}
			
			if(data.has("scale")) {
				JsonArray array = data.getAsJsonArray("scale");
				if(array.size() > 0) {
					scaleX = new ExpressionMulti(array.get(0).getAsString());
					scaleY = scaleX;
					scaleZ = scaleX;
				}
				if(array.size() > 1) {
					scaleY = new ExpressionMulti(array.get(1).getAsString());
					scaleZ = scaleY;
				}
				if(array.size() > 2) {
					scaleZ = new ExpressionMulti(array.get(2).getAsString());
				}
			}
			
			if(data.has("pivot")) {
				JsonArray array = data.getAsJsonArray("pivot");
				if(array.size() > 0) {
					pivotX = new ExpressionMulti(array.get(0).getAsString());
					pivotY = pivotX;
					pivotZ = pivotX;
				}
				if(array.size() > 1) {
					pivotY = new ExpressionMulti(array.get(1).getAsString());
					pivotZ = pivotY;
				}
				if(array.size() > 2) {
					pivotZ = new ExpressionMulti(array.get(2).getAsString());
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
		
		public void apply(Context context, Model model, int faceStartIndex, int faceEndIndex) {
			float translateX = 0f;
			float translateY = 0f;
			float translateZ = 0f;
			float rotateX = 0f;
			float rotateY = 0f;
			float rotateZ = 0f;
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
				
				if(translateX != 0f || translateY != 0f || translateZ != 0f)
					face.translate(translateX, translateY, translateZ);
			}
		}
		
	}
	
	private static class TransformationArray extends Transformation{
		
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
		public void apply(Context context, Model model, int faceStartIndex, int faceEndIndex) {
			for(Transformation transformation : transformations)
				transformation.apply(context, model, faceStartIndex, faceEndIndex);
		}
		
	}
	
	private static class ObjectPart extends Part{
		
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
				condition = new ExpressionMulti(data.get("condition").getAsString());
			if(data.has("loopInit"))
				loopInit = new ExpressionMulti(data.get("loopInit").getAsString());
			if(data.has("loopCondition"))
				loopCondition = new ExpressionMulti(data.get("loopCondition").getAsString());
			if(data.has("loopIncrement"))
				loopIncrement = new ExpressionMulti(data.get("loopIncrement").getAsString());
			if(data.has("variables")) {
				JsonElement variablesData = data.get("variables");
				if(variablesData.isJsonArray()) {
					for(JsonElement el : variablesData.getAsJsonArray().asList()) {
						addSubExpression(new ExpressionMulti(el.getAsString()));
					}
				}else if(variablesData.isJsonPrimitive()) {
					addSubExpression(new ExpressionMulti(variablesData.getAsString()));
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
		public void eval(Context context) {
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
	
	private static class TexturePart extends Part{
		
		private Map<String, Expression> textures;
		
		public TexturePart(JsonObject data) {
			textures = new HashMap<String, Expression>();
			for(Entry<String, JsonElement> entry : data.entrySet()) {
				String name = entry.getKey();
				if(!name.startsWith("#"))
					name = "#" + name;
				textures.put(name, new ExpressionMulti(entry.getValue().getAsString()));
			}
		}
		
		@Override
		public void eval(Context context) {
			for(Entry<String, Expression> entry : textures.entrySet()) {
				context.model.addTexture(entry.getKey(), entry.getValue().eval(context).asString());
			}
		}
		
	}
	
	private static class GeneratorPart extends Part{
		
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
					arguments.put(entry.getKey(), new ExpressionMulti(entry.getValue().getAsString()));
				}
			}
		}
		
		@Override
		public void eval(Context context) {
			BuiltInGenerator generator = context.localGenerators.getOrDefault(this.generator, null);
			if(generator == null) {
				generator = BuiltInGenerator.getGenerator(this.generator);
			}
			if(generator != null)
				generator.eval(context, arguments);
		}
		
	}
	
	public static class Value{
		
		private ValueImpl impl;
		private boolean immutable;
		
		public Value(ValueImpl impl) {
			this(impl, false);
		}
		
		public Value(ValueImpl impl, boolean immutable) {
			this.impl = impl;
			this.immutable = immutable;
		}
		
		public Value(NbtTag tag) {
			this.immutable = true;
			switch(tag.getId()) {
			case NbtTagByte.ID:
				this.impl = new ValueInt(((NbtTagByte) tag).getData());
				break;
			case NbtTagByteArray.ID:
				this.impl = new ValueNull();
				break;
			case NbtTagCompound.ID:
				this.impl = new ValueNbtCompound((NbtTagCompound) tag);
				break;
			case NbtTagDouble.ID:
				this.impl = new ValueFloat(((NbtTagFloat) tag).getData());
				break;
			case NbtTagFloat.ID:
				this.impl = new ValueFloat(((NbtTagFloat) tag).getData());
				break;
			case NbtTagInt.ID:
				this.impl = new ValueInt(((NbtTagInt) tag).getData());
				break;
			case NbtTagIntArray.ID:
				this.impl = new ValueNull();
				break;
			case NbtTagList.ID:
				this.impl = new ValueNbtList((NbtTagList) tag);
				break;
			case NbtTagLong.ID:
				this.impl = new ValueInt(((NbtTagLong) tag).getData());
				break;
			case NbtTagLongArray.ID:
				this.impl = new ValueNull(); 
				break;
			case NbtTagShort.ID:
				this.impl = new ValueInt(((NbtTagShort) tag).getData());
				break;
			case NbtTagString.ID:
				this.impl = new ValueString(((NbtTagString) tag).getData());
				break;
			default:
				this.impl = new ValueNull();
				break;
			}
		}
		
		public boolean isNull() {
			return impl instanceof ValueNull;
		}
		
		public ValueImpl getImpl() {
			return impl;
		}
		
		public void set(Value other) {
			if(this.immutable)
				throw new RuntimeException("Value is immutable");
			this.impl = other.impl;
		}
		
		public boolean asBool() {
			return impl.asBool();
		}
		
		public long asInt() {
			return impl.asInt();
		}
		
		public float asFloat() {
			return impl.asFloat();
		}
		
		public String asString() {
			return impl.asString();
		}
		
		public Value add(Value other) {
			return new Value(impl.add(other));
		}
		
		public Value sub(Value other) {
			return new Value(impl.sub(other));
		}
		
		public Value mult(Value other) {
			return new Value(impl.mult(other));
		}
		
		public Value div(Value other) {
			return new Value(impl.div(other));
		}
		
		public boolean equal(Value other) {
			return impl.equal(other);
		}
		
		public boolean lessThan(Value other) {
			return impl.lessThan(other);
		}
		
		public boolean greaterThan(Value other) {
			return impl.greaterThan(other);
		}
		
		public Value member(String name) {
			return impl.member(name);
		}
		
		public Value call(Context context) {
			return impl.call(context);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Value)
				return equal((Value) obj);
			return false;
		}
		
		@Override
		public int hashCode() {
			return impl.hashCode();
		}
		
		public Map<String, Value> getChildren(){
			return impl.getChildren();
		}
		
	}
	
	public static abstract class ValueImpl{
		
		public abstract boolean asBool();
		public abstract long asInt();
		public abstract float asFloat();
		public abstract String asString();
		
		public abstract ValueImpl add(Value other);
		public abstract ValueImpl sub(Value other);
		public abstract ValueImpl mult(Value other);
		public abstract ValueImpl div(Value other);
		
		public abstract boolean equal(Value other);
		public abstract boolean lessThan(Value other);
		public abstract boolean greaterThan(Value other);
		
		public abstract Value member(String name);
		public abstract Value call(Context context);
		
		public abstract Map<String, Value> getChildren();
		
	}
	
	public static class ValueNull extends ValueImpl{
		
		@Override
		public boolean asBool() {
			return false;
		}
		
		@Override
		public float asFloat() {
			return 0;
		}
		
		@Override
		public long asInt() {
			return 0;
		}
		
		@Override
		public String asString() {
			return "";
		}
		
		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(Value other) {
			return other.impl instanceof ValueNull;
		}
		
		@Override
		public boolean greaterThan(Value other) {
			return false;
		}
		
		@Override
		public boolean lessThan(Value other) {
			return false;
		}
		
		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueInt extends ValueImpl{
		
		private long value;
		
		public ValueInt(long value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value > 0;
		}
		
		@Override
		public float asFloat() {
			return value;
		}
		
		@Override
		public long asInt() {
			return value;
		}
		
		@Override
		public String asString() {
			return Long.toString(value);
		}
		
		@Override
		public ValueImpl add(Value other) {
			return new ValueInt(value + other.asInt());
		}
		
		@Override
		public ValueImpl div(Value other) {
			return new ValueInt(value / other.asInt());
		}
		
		@Override
		public ValueImpl mult(Value other) {
			return new ValueInt(value * other.asInt());
		}
		
		@Override
		public ValueImpl sub(Value other) {
			return new ValueInt(value - other.asInt());
		}
		
		@Override
		public boolean equal(Value other) {
			return !(other.impl instanceof ValueNull) && value == other.asInt();
		}
		
		@Override
		public boolean greaterThan(Value other) {
			return !(other.impl instanceof ValueNull) && value > other.asInt();
		}
		
		@Override
		public boolean lessThan(Value other) {
			return !(other.impl instanceof ValueNull) && value < other.asInt();
		}
		
		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueFloat extends ValueImpl{
		
		private float value;
		
		public ValueFloat(float value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value >= 0.5f;
		}
		
		@Override
		public float asFloat() {
			return value;
		}
		
		@Override
		public long asInt() {
			return (long) value;
		}
		
		@Override
		public String asString() {
			return Float.toString(value);
		}
		
		@Override
		public ValueImpl add(Value other) {
			return new ValueFloat(value + other.asFloat());
		}
		
		@Override
		public ValueImpl div(Value other) {
			return new ValueFloat(value / other.asFloat());
		}
		
		@Override
		public ValueImpl mult(Value other) {
			return new ValueFloat(value * other.asFloat());
		}
		
		@Override
		public ValueImpl sub(Value other) {
			return new ValueFloat(value - other.asFloat());
		}
		
		@Override
		public boolean equal(Value other) {
			return !(other.impl instanceof ValueNull) && value == other.asFloat();
		}
		
		@Override
		public boolean greaterThan(Value other) {
			return !(other.impl instanceof ValueNull) && value > other.asFloat();
		}
		
		@Override
		public boolean lessThan(Value other) {
			return !(other.impl instanceof ValueNull) && value < other.asFloat();
		}
		
		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueBool extends ValueImpl{
		
		private boolean value;
		
		public ValueBool(boolean value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value;
		}
		
		@Override
		public float asFloat() {
			return value ? 1f : 0f;
		}
		
		@Override
		public long asInt() {
			return value ? 1 : 0;
		}
		
		@Override
		public String asString() {
			return value ? "true" : "false";
		}
		
		@Override
		public ValueImpl add(Value other) {
			return new ValueBool((asFloat() + other.asFloat()) > 0.5f);
		}
		
		@Override
		public ValueImpl div(Value other) {
			return new ValueBool((asFloat() / other.asFloat()) > 0.5f);
		}
		
		@Override
		public ValueImpl mult(Value other) {
			return new ValueBool((asFloat() * other.asFloat()) > 0.5f);
		}
		
		@Override
		public ValueImpl sub(Value other) {
			return new ValueBool((asFloat() - other.asFloat()) > 0.5f);
		}
		
		@Override
		public boolean equal(Value other) {
			return !(other.impl instanceof ValueNull) && value == other.asBool();
		}
		
		@Override
		public boolean greaterThan(Value other) {
			return !(other.impl instanceof ValueNull) && asFloat() > other.asFloat();
		}
		
		@Override
		public boolean lessThan(Value other) {
			return !(other.impl instanceof ValueNull) && asFloat() < other.asFloat();
		}
		
		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueString extends ValueNativeClass{
		
		private String value;
		
		public ValueString(String value) {
			super();
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			String lV = value.toLowerCase();
			if(lV.startsWith("t") || lV.startsWith("y") || lV.startsWith("1"))
				return true;
			return asFloat() > 0.5f;
		}
		
		@Override
		public float asFloat() {
			try {
				return Float.parseFloat(value);
			}catch(Exception ex) {}
			return 0;
		}
		
		@Override
		public long asInt() {
			try {
				return Long.parseLong(value);
			}catch(Exception ex) {}
			return (long) asFloat();
		}
		
		@Override
		public String asString() {
			return value;
		}
		
		@Override
		public ValueImpl add(Value other) {
			return new ValueString(value + other.asString());
		}
		
		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(Value other) {
			return !(other.impl instanceof ValueNull) && value.equals(other.asString());
		}
		
		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		private static final String EMPTY_STRING = "";
		
		@NativeFunction
		public Value length(Context context) {
			return new Value(new ValueInt(value.length()));
		}
		
		@NativeFunction
		public Value endsWith(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.endsWith(arg0)));
		}
		
		@NativeFunction
		public Value startsWith(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.startsWith(arg0)));
		}
		
		@NativeFunction
		public Value equals(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.equals(arg0)));
		}
		
		@NativeFunction
		public Value equalsIgnoreCase(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.equalsIgnoreCase(arg0)));
		}
		
		@NativeFunction
		public Value contains(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.contains(arg0)));
		}
		
		@NativeFunction
		public Value containsIgnoreCase(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueBool(value.toLowerCase().contains(arg0.toLowerCase())));
		}
		
		@NativeFunction
		public Value indexOf(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueInt(value.indexOf(arg0)));
		}
		
		@NativeFunction
		public Value lastIndexOf(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new Value(new ValueInt(value.lastIndexOf(arg0)));
		}
		
		@NativeFunction
		public Value replace(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			String arg1 = EMPTY_STRING;
			if(arg1V != null)
				arg1 = arg1V.asString();
			return new Value(new ValueString(value.replace(arg0, arg1)));
		}
		
		@NativeFunction
		public Value substring(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			int arg0 = 0;
			if(arg0V != null)
				arg0 = (int) arg0V.asInt();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			int arg1 = value.length();
			if(arg1V != null)
				arg1 = (int) arg1V.asInt();
			return new Value(new ValueString(value.substring(arg0, arg1)));
		}
		
		@NativeFunction
		public Value toLowerCase(Context context) {
			return new Value(new ValueString(value.toLowerCase()));
		}
		
		@NativeFunction
		public Value toUpperCase(Context context) {
			return new Value(new ValueString(value.toUpperCase()));
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueDict extends ValueImpl{
		
		private Map<String, Value> value;
		
		public ValueDict() {
			this.value = new HashMap<String, Value>();
		}
		
		public Map<String, Value> getValue(){
			return value;
		}

		@Override
		public boolean asBool() {
			return !value.isEmpty();
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public Value member(String name) {
			Value val = value.getOrDefault(name, null);
			if(val == null) {
				val = new Value(new ValueNull());
				value.put(name, val);
			}
			return val;
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return value;
		}
		
	}
	
	public static class ValueNbtCompound extends ValueImpl{
		
		private NbtTagCompound value;
		
		public ValueNbtCompound(NbtTagCompound value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return value.getSize() > 0;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public Value member(String name) {
			NbtTag val = value.get(name);
			if(val == null) {
				return new Value(new ValueNull());
			}
			return new Value(val);
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			Map<String, Value> map = new HashMap<String, Value>();
			for(int i = 0; i < value.getSize(); ++i) {
				NbtTag tag = value.get(i);
				map.put(tag.getName(), new Value(tag));
			}
			return map;
		}
		
	}
	
	public static class ValueNbtList extends ValueImpl{
		
		private NbtTagList value;
		
		public ValueNbtList(NbtTagList value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return value.getSize() > 0;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public Value member(String name) {
			if(name.equalsIgnoreCase("length")) {
				return new Value(new ValueNativeFunction(this, lengthMethod));
			}
			int index = -1;
			try {
				index = Integer.parseInt(name);
			}catch(Exception ex) {}
			if(index < 0 || index >= value.getSize())
				return new Value(new ValueNull());
			NbtTag val = value.get(index);
			if(val == null) {
				return new Value(new ValueNull());
			}
			return new Value(val);
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		private static Method lengthMethod = null;
		static {
			try {
				lengthMethod = ValueNbtList.class.getDeclaredMethod("length", Context.class);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		
		public Value length(Context context) {
			return new Value(new ValueInt(value.getSize()));
		}
		
		@Override
		public Map<String, Value> getChildren() {
			Map<String, Value> map = new HashMap<String, Value>();
			for(int i = 0; i < value.getSize(); ++i) {
				map.put(Integer.toString(i), new Value(value.get(i)));
			}
			return map;
		}
		
	}
	
	public static class ValueThisBlock extends ValueImpl{
		
		private Context value;
		
		public ValueThisBlock(Context value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return true;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public Value member(String name) {
			if(name.equals("name"))
				return new Value(new ValueString(value.name));
			else if(name.equals("state"))
				return new Value(value.properties);
			else if(name.equals("x")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new Value(new ValueInt(value.x));
			}
			else if(name.equals("y")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new Value(new ValueInt(value.y));
			}
			else if(name.equals("z")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new Value(new ValueInt(value.z));
			}
			return new Value(new ValueNull());
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueFunction extends ValueImpl{
		
		private Expression value;
		
		public ValueFunction(Expression value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return true;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			return value.eval(context);
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	private static class ValueBlock extends ValueImpl{
		
		private Block block;
		private int x;
		private int y;
		private int z;
		
		public ValueBlock(Block block, int x, int y, int z) {
			this.block = block;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public Value member(String name) {
			if(name.equals("name"))
				return new Value(new ValueString(block.getName()));
			else if(name.equals("state"))
				return new Value(block.getProperties());
			else if(name.equals("x")) {
				return new Value(new ValueInt(x));
			}
			else if(name.equals("y")) {
				return new Value(new ValueInt(y));
			}
			else if(name.equals("z")) {
				return new Value(new ValueInt(z));
			}
			return new Value(new ValueNull());
		}
		
		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(Value other) {
			if(other.impl instanceof ValueBlock) {
				return block == ((ValueBlock)other.impl).block;
			}
			return false;
		}
		
		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
	private static @interface NativeFunction{}
	
	private static class ValueNativeFunction extends ValueImpl{
		
		private Object obj;
		private Method func;
		
		public ValueNativeFunction(Object obj, Method func) {
			this.obj = obj;
			this.func = func;
		}
		
		@Override
		public Value call(Context context) {
			try {
				return (Value) func.invoke(obj, context);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return new Value(new ValueNull());
		}
		
		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Value member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	private static abstract class ValueNativeClass extends ValueImpl{
		
		private Map<String, Value> funcs;
		
		public ValueNativeClass() {
			funcs = new HashMap<String, Value>();
			for(Method method : this.getClass().getMethods()) {
				if(method.isAnnotationPresent(NativeFunction.class)) {
					if(Value.class.isAssignableFrom(method.getReturnType())) {
						funcs.put(method.getName(), new Value(new ValueNativeFunction(this, method)));
					}
				}
			}
		}
		
		@Override
		public Value member(String name) {
			Value val = funcs.getOrDefault(name, null);
			if(val != null)
				return val;
			return new Value(new ValueNull());
		}
		
		@Override
		public Value call(Context context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl add(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl div(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean greaterThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl mult(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ValueImpl sub(Value other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, Value> getChildren() {
			return new HashMap<String, Value>();
		}
		
	}
	
	public static class ValueClassMath extends ValueNativeClass{
		
		@NativeFunction
		public Value abs(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			if(arg0V.impl instanceof ValueInt) {
				long arg0I = arg0V.asInt();
				return new Value(new ValueInt(Math.abs(arg0I)));
			}
			return new Value(new ValueFloat(Math.abs(arg0)));
		}
		
		@NativeFunction
		public Value acos(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.acos(arg0)));
		}
		
		@NativeFunction
		public Value asin(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.asin(arg0)));
		}
		
		@NativeFunction
		public Value atan(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.atan(arg0)));
		}
		
		@NativeFunction
		public Value atan2(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			return new Value(new ValueFloat((float) Math.atan2(arg0, arg1)));
		}
		
		@NativeFunction
		public Value ceil(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.ceil(arg0)));
		}
		
		@NativeFunction
		public Value clamp(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			Value arg2V = context.variables.getOrDefault("arg2", null);
			float arg2 = 0f;
			if(arg2V != null)
				arg2 = arg2V.asFloat();
			if(arg0V.impl instanceof ValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				long arg2I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				if(arg2V != null)
					arg2I = arg2V.asInt();
				return new Value(new ValueInt(Math.min(Math.max(arg0I, arg1I), arg2I)));
			}
			return new Value(new ValueFloat(Math.min(Math.max(arg0, arg1), arg2)));
		}
		
		@NativeFunction
		public Value cos(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.cos(arg0)));
		}
		
		@NativeFunction
		public Value cosh(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.cosh(arg0)));
		}
		
		@NativeFunction
		public Value exp(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.exp(arg0)));
		}
		
		@NativeFunction
		public Value floor(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.floor(arg0)));
		}
		
		@NativeFunction
		public Value log(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.log(arg0)));
		}
		
		@NativeFunction
		public Value log10(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.log10(arg0)));
		}
		
		@NativeFunction
		public Value max(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			if(arg0V.impl instanceof ValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				return new Value(new ValueInt(Math.max(arg0I, arg1I)));
			}
			return new Value(new ValueFloat(Math.max(arg0, arg1)));
		}
		
		@NativeFunction
		public Value min(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			if(arg0V.impl instanceof ValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				return new Value(new ValueInt(Math.min(arg0I, arg1I)));
			}
			return new Value(new ValueFloat(Math.min(arg0, arg1)));
		}
		
		@NativeFunction
		public Value pow(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			return new Value(new ValueFloat((float) Math.pow(arg0, arg1)));
		}
		
		@NativeFunction
		public Value round(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.round(arg0)));
		}
		
		@NativeFunction
		public Value signum(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.signum(arg0)));
		}
		
		@NativeFunction
		public Value sin(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.sin(arg0)));
		}
		
		@NativeFunction
		public Value sinh(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.sinh(arg0)));
		}
		
		@NativeFunction
		public Value sqrt(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.sqrt(arg0)));
		}
		
		@NativeFunction
		public Value tan(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.tan(arg0)));
		}
		
		@NativeFunction
		public Value tanh(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.tanh(arg0)));
		}
		
		@NativeFunction
		public Value toDegrees(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.toDegrees(arg0)));
		}
		
		@NativeFunction
		public Value toRadians(Context context) {
			Value arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new Value(new ValueFloat((float) Math.toRadians(arg0)));
		}
		
	}
	
	private static class ValueClassDict extends ValueNativeClass{
		
		@NativeFunction
		public Value length(Context context) {
			Value arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ValueDict) && !(arg0.impl instanceof ValueNbtCompound) &&
					!(arg0.impl instanceof ValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			if(arg0.impl instanceof ValueDict)
				return new Value(new ValueInt(((ValueDict) arg0.impl).value.size()));
			if(arg0.impl instanceof ValueNbtCompound)
				return new Value(new ValueInt(((ValueNbtCompound) arg0.impl).value.getSize()));
			if(arg0.impl instanceof ValueNbtList)
				return new Value(new ValueInt(((ValueNbtList) arg0.impl).value.getSize()));
			return new Value(new ValueInt(0));
		}
		
		@NativeFunction
		public Value hasKey(Context context) {
			Value arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ValueDict) && !(arg0.impl instanceof ValueNbtCompound) &&
					!(arg0.impl instanceof ValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			Value arg1V = context.variables.getOrDefault("arg1", null);
			String arg1 = null;
			if(arg1V != null)
				arg1 = arg1V.asString();
			if(arg0.impl instanceof ValueDict)
				return new Value(new ValueBool(((ValueDict) arg0.impl).value.containsKey(arg1)));
			if(arg0.impl instanceof ValueNbtCompound)
				return new Value(new ValueBool(((ValueNbtCompound) arg0.impl).value.get(arg1) != null));
			if(arg0.impl instanceof ValueNbtList) {
				int index = -1;
				try {
					index = Integer.parseInt(arg1);
				}catch(Exception ex) {};
				return new Value(new ValueBool(index >= 0 && index < ((ValueNbtList) arg0.impl).value.getSize()));
			}
			return new Value(new ValueBool(false));
		}
		
		@NativeFunction
		public Value keys(Context context) {
			Value arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ValueDict) && !(arg0.impl instanceof ValueNbtCompound) &&
					!(arg0.impl instanceof ValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			if(arg0.impl instanceof ValueDict) {
				ValueDict val = new ValueDict();
				int i = 0;
				for(String key : ((ValueDict) arg0.impl).value.keySet()) {
					val.value.put(Integer.toString(i), new Value(new ValueString(key)));
					i++;
				}
				return new Value(val);
			}
			if(arg0.impl instanceof ValueNbtCompound) {
				ValueDict val = new ValueDict();
				for(int i = 0; i < ((ValueNbtCompound) arg0.impl).value.getSize(); i++) {
					val.value.put(Integer.toString(i), new Value(new ValueString(((ValueNbtCompound) arg0.impl).value.get(i).getName())));
					i++;
				}
				return new Value(val);
			}
			if(arg0.impl instanceof ValueNbtList) {
				ValueDict val = new ValueDict();
				for(int i = 0; i < ((ValueNbtList) arg0.impl).value.getSize(); i++) {
					val.value.put(Integer.toString(i), new Value(new ValueString(Integer.toString(i))));
					i++;
				}
				return new Value(val);
			}
			return new Value(new ValueDict());
		}
		
	}
	
	private static class ValueClassBlock extends ValueNativeClass{
		
		@NativeFunction
		public Value getBlockRelative(Context context) {
			if(!context.isLocationDependent)
				throw new RuntimeException("Blocks may only be queried if block state is location dependent");
			int x = 0;
			int y = 0;
			int z = 0;
			Value arg0V = context.variables.getOrDefault("arg0", null);
			if(arg0V != null)
				x = (int) arg0V.asInt();
			Value arg1V = context.variables.getOrDefault("arg1", null);
			if(arg1V != null)
				y = (int) arg1V.asInt();
			Value arg2V = context.variables.getOrDefault("arg2", null);
			if(arg2V != null)
				z = (int) arg2V.asInt();
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(context.x + x, context.y + y, context.z + z);
			Block block = BlockRegistry.getBlock(blockId);
			return new Value(new ValueBlock(block, context.x + x, context.y + y, context.z + z));
		}
		
		@NativeFunction 
		public Value blockHasTag(Context context){
			Value arg0V = context.variables.getOrDefault("arg0", null);
			if(arg0V == null || !(arg0V.impl instanceof ValueBlock))
				throw new RuntimeException("Argument 0 is either not given or not a ValueBlock");
			Block block = ((ValueBlock) arg0V.impl).block;
			String tag = "";
			Value arg1V = context.variables.getOrDefault("arg1", null);
			if(arg1V != null)
				tag = arg1V.asString();
			return new Value(new ValueBool(Tags.isInList(block.getName(), Tags.getNamesInTag(tag))));
		}
		
	}
	
	private static final Value VALUE_MATH = new Value(new ValueClassMath(), true);
	private static final Value VALUE_DICT = new Value(new ValueClassDict(), true);
	private static final Value VALUE_BLOCK = new Value(new ValueClassBlock(), true);
	
	private static final Map<String, Value> VALUE_BUILTINS = new HashMap<String, Value>();
	static {
		VALUE_BUILTINS.put("Math", VALUE_MATH);
		VALUE_BUILTINS.put("Dict", VALUE_DICT);
		VALUE_BUILTINS.put("Block", VALUE_BLOCK);
	}
	
	private static class CodeIterator{
		
		private String code;
		private int index;
		
		public CodeIterator(String code) {
			this.code = code;
			this.index = 0;
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getCode() {
			return code;
		}
		
		public int peek() {
			if(index >= code.length())
				return 0;
			return code.codePointAt(index);
		}
		
		public int peek(int offset) {
			if((index + offset) < 0 || (index + offset) >= code.length())
				return 0;
			return code.codePointAt(index + offset);
		}
		
		public boolean hasToken(String token) {
			int upperBound = Math.min(code.length(), index + token.length());
			for(int i = index; i < upperBound; ++i) {
				if(code.codePointAt(i) != token.codePointAt(i - index))
					return false;
			}
			if(upperBound < code.length()) {
				// We got the token, but the token in the actual code
				// might still have more characters and therefore actually
				// be a different token, so let's make sure that the next
				// character isn't a token character.
				int nextCodePoint = code.codePointAt(upperBound);
				return !Character.isDigit(nextCodePoint) && !Character.isAlphabetic(nextCodePoint) && nextCodePoint != '_';
			}
			return true;
		}
		
		public void next() {
			next(1);
		}
		
		public void next(int offset) {
			index += offset;
			if(index > code.length())
				index = code.length();
			// Keep moving until we don't have a whitespace anymore.
			while(true) {
				int codePoint = peek();
				if(codePoint == 0 || !Character.isWhitespace(codePoint))
					break;
				index++;
			}
		}
		
		public void skipWhitespaces() {
			// Keep moving until we don't have a whitespace anymore.
			while(true) {
				int codePoint = peek();
				if(codePoint == 0 || !Character.isWhitespace(codePoint))
					break;
				index++;
			}
		}
		
		public String getToken() {
			int start = index;
			int i = start;
			for(i = start; i < code.length(); ++i) {
				int codePoint = code.codePointAt(i);
				if(!Character.isDigit(codePoint) && !Character.isAlphabetic(codePoint) && codePoint != '_')
					break;
			}
			index = i;
			skipWhitespaces();
			return code.substring(start, i);
		}
		
	}
	
	public static abstract class Expression{
		
		public abstract Value eval(Context context);
		
		public static Expression parseExpression(CodeIterator code) {
			code.skipWhitespaces();
			int codePoint = code.peek();
			if(codePoint == 0)
				return new ExpressionConstant(new Value(new ValueNull()));
			
			Expression expr = parseExpressionPart(code, null, false);
			codePoint = code.peek();
			while(codePoint != 0) {
				expr = parseExpressionPart(code, expr, false);
				codePoint = code.peek();
			}
			return expr;
		}
		
		private static Expression parseSubExpression(CodeIterator code) {
			int codePoint = code.peek();
			if(codePoint == 0)
				return new ExpressionConstant(new Value(new ValueNull()));
			
			
			Expression expr = null;
			while(true) {
				expr = parseExpressionPart(code, expr, false);
				codePoint = code.peek();
				
				if(codePoint == '.' || codePoint == '[' || codePoint == '(')
					continue;
				
				break;
			}
			return expr;
		}
		
		private static Expression parseSubExpression2(CodeIterator code) {
			int codePoint = code.peek();
			if(codePoint == 0)
				return new ExpressionConstant(new Value(new ValueNull()));
			
			
			Expression expr = null;
			while(true) {
				Expression expr2 = parseExpressionPart(code, expr, true);
				if(expr2 == null)
					break;
				expr = expr2;
				codePoint = code.peek();
				
				if(codePoint == 0)
					break;
			}
			return expr;
		}
		
		private static Expression parseExpressionPart(CodeIterator code, Expression prevExpr, boolean noException) {
			int codePoint = code.peek();
			if(codePoint == '!') {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=' && prevExpr != null) {
					code.next();
					return new ExpressionNotEqual(prevExpr, parseSubExpression(code));
				}
				return new ExpressionInvert(parseSubExpression(code));
			}
			if(codePoint == '-' && (Character.isDigit(code.peek(1)) || code.peek(1) == '.')) {
				// It's a digit
				code.next();
				String token = code.getToken();
				codePoint = code.peek();
				if(codePoint == '.') {
					// It's a float.
					code.next();
					String fraction = code.getToken();
					float val = 0f;
					try {
						val = Float.parseFloat(token + "." + fraction);
					}catch(Exception ex) {}
					return new ExpressionConstant(new Value(new ValueFloat(-val)));
				}
				// It's an int
				long val = 0;
				try {
					val = Long.parseLong(token);
				}catch(Exception ex) {}
				return new ExpressionConstant(new Value(new ValueInt(-val)));
			}
			if(codePoint == '0' && (code.peek(1) == 'x' || code.peek(1) == 'X')) {
				// Hexidecimal
				code.next(); // skip 0
				code.next(); // skip x
				String token = code.getToken();
				long val = 0;
				try {
					val = Long.parseUnsignedLong(token, 16);
				}catch(Exception ex) {}
				return new ExpressionConstant(new Value(new ValueInt(val)));
			}
			if(codePoint == '0' && (code.peek(1) == 'b' || code.peek(1) == 'b')) {
				// Binary
				code.next(); // skip 0
				code.next(); // skip x
				String token = code.getToken();
				long val = 0;
				try {
					val = Long.parseUnsignedLong(token, 2);
				}catch(Exception ex) {}
				return new ExpressionConstant(new Value(new ValueInt(val)));
			}
			if(codePoint == '.' && Character.isDigit(code.peek(1))) {
				// Float
				code.next();
				String fraction = code.getToken();
				float val = 0f;
				try {
					val = Float.parseFloat("0." + fraction);
				}catch(Exception ex) {}
				return new ExpressionConstant(new Value(new ValueFloat(val)));
			}
			if(Character.isDigit(codePoint)) {
				// Normal number
				String token = code.getToken();
				codePoint = code.peek();
				if(codePoint == '.') {
					// It's a float.
					code.next();
					String fraction = code.getToken();
					float val = 0f;
					try {
						val = Float.parseFloat(token + "." + fraction);
					}catch(Exception ex) {}
					return new ExpressionConstant(new Value(new ValueFloat(val)));
				}
				// It's an int
				long val = 0;
				try {
					val = Long.parseLong(token);
				}catch(Exception ex) {}
				return new ExpressionConstant(new Value(new ValueInt(val)));
			}
			if(code.hasToken("true")) {
				// True boolean value
				code.next("true".length());
				return new ExpressionConstant(new Value(new ValueBool(true)));
			}
			if(code.hasToken("false")) {
				// True boolean value
				code.next("false".length());
				return new ExpressionConstant(new Value(new ValueBool(false)));
			}
			if(code.hasToken("null")) {
				// True boolean value
				code.next("null".length());
				return new ExpressionConstant(new Value(new ValueNull()));
			}
			if(code.hasToken("return")) {
				code.next("return".length());
				return new ExpressionReturn(parseSubExpression(code));
			}
			if(codePoint == '\'' || codePoint == '"') {
				int terminationChar = codePoint;
				int startChar = code.getIndex();
				code.next();
				while(true) {
					int c = code.peek();
					if(c == '\\') {
						// Skip this char and the next one
						code.index += 2;
						continue;
					}
					if(c == 0 || c == terminationChar) {
						break;
					}
					code.index++;
				}
				startChar = startChar + 1;
				int endChar = code.getIndex();
				String val = "";
				if(endChar > startChar)
					val = code.getCode().substring(startChar, endChar);
				code.next();
				
				// Handle some escaped characters
				if(terminationChar == '\'')
					val = val.replace("\\'", "'");
				else if(terminationChar == '"')
					val = val.replace("\\\"", "\"");
				val = val.replace("\\\n", "\n");
				
				return new ExpressionConstant(new Value(new ValueString(val)));
			}
			if(codePoint == '{') {
				Map<String, Expression> val = new HashMap<String, Expression>();
				
				code.next();
				while(true) {
					int c = code.peek();
					if(c == 0 || c == '}') {
						code.next();
						break;
					}
					if(c == ',') {
						code.next();
						continue;
					}
					Expression nameExpr = parseSubExpression2(code);
					String name = "";
					if(nameExpr instanceof ExpressionConstant) {
						Value nameVal = ((ExpressionConstant) nameExpr).value;
						name = nameVal.asString();
					}
					c = code.peek();
					if(c == ':') {
						code.next();
					}
					Expression valExpr = parseSubExpression2(code);
					
					val.put(name, valExpr);
				}
				
				return new ExpressionConstantDict(val);
			}
			if(codePoint == '[' && prevExpr == null) {
				// Array literal
				List<Expression> val = new ArrayList<Expression>();
				
				code.next();
				while(true) {
					int c = code.peek();
					if(c == 0 || c == ']') {
						code.next();
						break;
					}
					if(c == ',') {
						code.next();
						continue;
					}
					
					Expression valExpr = parseSubExpression2(code);
					
					val.add(valExpr);
				}
				
				return new ExpressionConstantArray(val);
			}
			if(codePoint == '[' && prevExpr != null) {
				// Array/Dict access
				code.next(); // [
				Expression keyExpr = parseSubExpression2(code);
				code.next(); // ]
				return new ExpressionGetIndex(prevExpr, keyExpr);
			}
			if(codePoint == '(' && prevExpr != null) {
				// Function call
				List<Expression> val = new ArrayList<Expression>();
				
				code.next();
				while(true) {
					int c = code.peek();
					if(c == 0 || c == ')') {
						code.next();
						break;
					}
					if(c == ',') {
						code.next();
						continue;
					}
					
					Expression valExpr = parseSubExpression2(code);
					
					val.add(valExpr);
				}
				
				return new ExpressionCall(prevExpr, val);
			}
			if(codePoint == '(' && prevExpr == null) {
				// Sub expression
				code.next(); // (
				Expression expr = parseSubExpression2(code);
				code.next(); // )
				return expr;
			}
			if(codePoint == '=' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Equals
					code.next();
					return new ExpressionEqual(prevExpr, parseSubExpression(code));
				}
				// Assignment
				return new ExpressionStoreVariable(prevExpr, parseSubExpression2(code));
			}
			if(codePoint == '<' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Less than or equal
					code.next();
					return new ExpressionLessThanOrEqual(prevExpr, parseSubExpression(code));
				}
				// Less than
				return new ExpressionLessThan(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '>' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Greater than or equal
					code.next();
					return new ExpressionGreaterThanOrEqual(prevExpr, parseSubExpression(code));
				}
				// Greater than
				return new ExpressionGreaterThan(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '&' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '&') {
					// And
					code.next();
					return new ExpressionAnd(prevExpr, parseSubExpression(code));
				}
			}
			if(codePoint == '|' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '|') {
					// And
					code.next();
					return new ExpressionOr(prevExpr, parseSubExpression(code));
				}
			}
			if(codePoint == '?' && prevExpr != null) {
				// Ternary
				code.next(); // ?
				Expression ifTrue = parseSubExpression(code);
				code.next(); // :
				Expression ifFalse = parseSubExpression(code);
				return new ExpressionTernary(prevExpr, ifTrue, ifFalse);
			}
			if(codePoint == '+' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Asignment
					code.next();
					return new ExpressionAddStoreVariable(prevExpr, parseSubExpression2(code));
				}
				return new ExpressionAdd(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '-' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Asignment
					code.next();
					return new ExpressionSubStoreVariable(prevExpr, parseSubExpression2(code));
				}
				return new ExpressionSub(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '*' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Asignment
					code.next();
					return new ExpressionMultStoreVariable(prevExpr, parseSubExpression2(code));
				}
				return new ExpressionMult(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '/' && prevExpr != null) {
				code.next();
				codePoint = code.peek();
				if(codePoint == '=') {
					// Asignment
					code.next();
					return new ExpressionDivStoreVariable(prevExpr, parseSubExpression2(code));
				}
				return new ExpressionDiv(prevExpr, parseSubExpression(code));
			}
			if(codePoint == '.' && prevExpr != null) {
				// Get member
				code.next();
				String memberName = code.getToken();
				return new ExpressionGetMember(prevExpr, memberName);
			}
			if(Character.isLetter(codePoint) || codePoint == '_') {
				// Variable access
				String token = code.getToken();
				return new ExpressionGetVariable(token);
			}
			if(noException)
				return null;
			throw new RuntimeException("Invalid syntax at index " + code.getIndex() + " for code \"" + code.getCode() + "\"");
		}
		
	}
	
	public static class ExpressionMulti extends Expression{
		
		private String code;
		private Expression[] statements;
		
		public ExpressionMulti(String code) {
			this.code = code;
			statements = new Expression[0];
			code = code.trim();
			if(code.isEmpty())
				return;
			
			while(true) {
				// Extract the first statement from code.
				int semicolonIndex = code.indexOf((int) ';');
				if(semicolonIndex < 0)
					semicolonIndex = code.length();
				String statement = code.substring(0, semicolonIndex);
				statement = statement.trim();
				
				if(!statement.isEmpty()) {
					// Add it if it's a valid statement.
					Expression expr = Expression.parseExpression(new CodeIterator(statement));
					if(expr != null)
						addStatement(expr);
				}
				// If we've reached the end, stop.
				if(semicolonIndex >= code.length())
					break;
				// Take the first statement out of the code and loop back.
				code = code.substring(semicolonIndex + 1);
				// If there is no code left, stop.
				if(code.isEmpty())
					break;
			}
		}
		
		private void addStatement(Expression expr) {
			statements = Arrays.copyOf(statements, statements.length + 1);
			statements[statements.length-1] = expr;
		}
		
		@Override
		public Value eval(Context context) {
			try {
				Value prevReturnVal = context.returnValue;
				context.returnValue = null;
				Value val = null;
				for(int i = 0; i < statements.length; ++i) {
					val = statements[i].eval(context);
					if(context.returnValue != null) {
						val = context.returnValue;
						break;
					}
				}
				context.returnValue = prevReturnVal;
				if(val == null)
					val = new Value(new ValueNull());
				return val;
			}catch(Exception ex) {
				throw new RuntimeException("Exception while evaluating expression \"" + code + "\"", ex);
			}
		}
		
	}
	
	private static class ExpressionConstant extends Expression{
		
		private Value value;
		
		public ExpressionConstant(Value value) {
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			return value;
		}
		
	}
	
	private static class ExpressionConstantDict extends Expression{
		
		private Map<String, Expression> value;
		
		public ExpressionConstantDict(Map<String, Expression> value) {
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = new Value(new ValueDict());
			for(Entry<String, Expression> entry : value.entrySet()) {
				val.member(entry.getKey()).set(entry.getValue().eval(context));
			}
			return val;
		}
		
	}
	
	private static class ExpressionConstantArray extends Expression{
		
		private List<Expression> value;
		
		public ExpressionConstantArray(List<Expression> value) {
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = new Value(new ValueDict());
			for(int i = 0; i < value.size(); ++i) {
				val.member(Integer.toString(i)).set(value.get(i).eval(context));
			}
			return val;
		}
		
	}
	
	private static class ExpressionStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionStoreVariable(Expression variable, Expression value) {
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = variable.eval(context);
			val.set(value.eval(context));
			return val;
		}
		
	}
	
	private static class ExpressionAddStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionAddStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = variable.eval(context);
			val.set(val.add(value.eval(context)));
			return val;
		}
		
	}
	
	private static class ExpressionSubStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionSubStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = variable.eval(context);
			val.set(val.sub(value.eval(context)));
			return val;
		}
		
	}
	
	private static class ExpressionMultStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionMultStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = variable.eval(context);
			val.set(val.mult(value.eval(context)));
			return val;
		}
		
	}
	
	private static class ExpressionDivStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionDivStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value val = variable.eval(context);
			val.set(val.div(value.eval(context)));
			return val;
		}
		
	}
	
	private static class ExpressionAdd extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionAdd(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return left.eval(context).add(right.eval(context));
		}
		
	}
	
	private static class ExpressionSub extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionSub(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return left.eval(context).sub(right.eval(context));
		}
		
	}
	
	private static class ExpressionMult extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionMult(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return left.eval(context).mult(right.eval(context));
		}
		
	}
	
	private static class ExpressionDiv extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionDiv(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return left.eval(context).div(right.eval(context));
		}
		
	}
	
	private static class ExpressionInvert extends Expression{
		
		private Expression expr;
		
		public ExpressionInvert(Expression expr) {
			this.expr = expr;
		}
		
		@Override
		public Value eval(Context context) {
			return new Value(new ValueBool(!expr.eval(context).asBool()));
		}
		
	}
	
	private static class ExpressionEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return new Value(new ValueBool(left.eval(context).equal(right.eval(context))));
		}
		
	}
	
	private static class ExpressionNotEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionNotEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return new Value(new ValueBool(!(left.eval(context).equal(right.eval(context)))));
		}
		
	}
	
	private static class ExpressionLessThan extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionLessThan(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return new Value(new ValueBool(left.eval(context).lessThan(right.eval(context))));
		}
		
	}
	
	private static class ExpressionGreaterThan extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionGreaterThan(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			return new Value(new ValueBool(left.eval(context).greaterThan(right.eval(context))));
		}
		
	}
	
	private static class ExpressionLessThanOrEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionLessThanOrEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			Value leftValue = left.eval(context);
			Value rightValue = right.eval(context);
			
			return new Value(new ValueBool(leftValue.equal(rightValue) || leftValue.lessThan(rightValue)));
		}
		
	}
	
	private static class ExpressionGreaterThanOrEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionGreaterThanOrEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			Value leftValue = left.eval(context);
			Value rightValue = right.eval(context);
			
			return new Value(new ValueBool(leftValue.equal(rightValue) || leftValue.greaterThan(rightValue)));
		}
		
	}
	
	private static class ExpressionAnd extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionAnd(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			Value leftValue = left.eval(context);
			Value rightValue = right.eval(context);
			
			return new Value(new ValueBool(leftValue.asBool() && rightValue.asBool()));
		}
		
	}
	
	private static class ExpressionOr extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionOr(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			Value leftValue = left.eval(context);
			Value rightValue = right.eval(context);
			
			return new Value(new ValueBool(leftValue.asBool() || rightValue.asBool()));
		}
		
	}
	
	private static class ExpressionTernary extends Expression{
		
		private Expression condition;
		private Expression left;
		private Expression right;
		
		public ExpressionTernary(Expression condition, Expression left, Expression right) {
			this.condition = condition;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public Value eval(Context context) {
			if(condition.eval(context).asBool())
				return left.eval(context);
			return right.eval(context);
		}
		
	}
	
	private static class ExpressionGetVariable extends Expression{
		
		private String name;
		
		public ExpressionGetVariable(String name) {
			this.name = name;
		}
		
		@Override
		public Value eval(Context context) {
			if(name.equalsIgnoreCase("thisBlock"))
				return context.thisBlock;
			if(name.equalsIgnoreCase("global")) {
				return context.globals;
			}
			
			Value val = context.builtins.getOrDefault(name, null);
			if(val != null)
				return val;
			
			val = context.localFunctions.getOrDefault(name, null);
			if(val != null)
				return val;
			
			val = context.variables.getOrDefault(name, null);
			if(val == null) {
				val = new Value(new ValueNull());
				context.variables.put(name, val);
			}
			return val;
		}
		
	}
	
	private static class ExpressionGetMember extends Expression{
		
		private Expression parent;
		private String name;
		
		public ExpressionGetMember(Expression parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		@Override
		public Value eval(Context context) {
			Value parentValue = parent.eval(context);
			
			return parentValue.member(name);
		}
		
	}
	
	private static class ExpressionGetIndex extends Expression{
		
		private Expression parent;
		private Expression index;
		
		public ExpressionGetIndex(Expression parent, Expression index) {
			this.parent = parent;
			this.index = index;
		}
		
		@Override
		public Value eval(Context context) {
			Value parentValue = parent.eval(context);
			
			return parentValue.member(index.eval(context).asString());
		}
		
	}
	
	private static class ExpressionCall extends Expression{
		
		private Expression function;
		private List<Expression> arguments;
		
		public ExpressionCall(Expression function, List<Expression> arguments) {
			this.function = function;
			this.arguments = arguments;
		}
		
		@Override
		public Value eval(Context context) {
			Context newContext = new Context(context);
			
			for(int i = 0; i < arguments.size(); ++i) {
				newContext.variables.put("arg" + i, arguments.get(i).eval(context));
			}
			return function.eval(context).call(newContext);
		}
		
	}
	
	private static class ExpressionReturn extends Expression{
		
		private Expression value;
		
		public ExpressionReturn(Expression value) {
			this.value = value;
		}
		
		@Override
		public Value eval(Context context) {
			Value returnVal = value.eval(context);
			context.returnValue = returnVal;
			return returnVal;
		}
		
	}
	
	private static class CustomBuiltInGenerator extends BuiltInGenerator{

		private Part part;
		
		public CustomBuiltInGenerator(JsonElement data) {
			this.part = Part.parsePart(data);
		}
		
		@Override
		public void eval(Context context, Map<String, Expression> arguments) {
			Context context2 = new Context(context);
			for(Entry<String, Expression> entry : arguments.entrySet()) {
				context2.variables.put(entry.getKey(), entry.getValue().eval(context));
			}
			part.eval(context2);
		}
		
	}
	
	private static Map<String, BuiltInBlockStateHandler> handlerRegistry = new HashMap<String, BuiltInBlockStateHandler>();
	
	public static BuiltInBlockStateHandler getHandler(String blockName) {
		return handlerRegistry.getOrDefault(blockName, null);
	}
	
	public static boolean hasHandler(String blockName) {
		return handlerRegistry.containsKey(blockName);
	}
	
	public static void load() {
		Map<String, BuiltInBlockStateHandler> handlerRegistry = new HashMap<String, BuiltInBlockStateHandler>();
		
		for(int i = ResourcePacks.getActiveResourcePacks().size() - 1; i >= 0; --i) {
			File builtInFolder = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), "builtins");
			if(!builtInFolder.exists() || !builtInFolder.isDirectory())
				continue;
			for(File namespaceFolder : builtInFolder.listFiles()) {
				if(!namespaceFolder.isDirectory())
					continue;
				File blockstatesFolder = new File(namespaceFolder, "blockstates");
				if(blockstatesFolder.exists() && blockstatesFolder.isDirectory())
					loadFolder(blockstatesFolder, namespaceFolder.getName() + ":", i, handlerRegistry);
			}
		}
		
		BuiltInBlockState.handlerRegistry = handlerRegistry;
	}
	
	private static void loadFolder(File folder, String parent, int resourcePackIndex, 
			Map<String, BuiltInBlockStateHandler> handlerRegistry) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadFolder(file, parent + file.getName() + "/", resourcePackIndex, handlerRegistry);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				String name = file.getName();
				int dotIndex = name.lastIndexOf('.');
				name = name.substring(0, dotIndex);
				BuiltInBlockStateHandler handler = loadFile(file, parent + name, resourcePackIndex);
				for(String blockName : handler.blockNames)
					handlerRegistry.put(blockName, handler);
			}
		}
	}
	
	private static BuiltInBlockStateHandler loadFile(File file, String name, int resourcePackIndex) {
		JsonObject data = Json.read(file).getAsJsonObject();
		BuiltInBlockStateHandler handler = new BuiltInBlockStateHandler(name, resourcePackIndex, data);
		return handler;
	}
	
	private static BuiltInBlockStateHandler loadFile(String name, int startResourcePackIndex) {
		int colonIndex = name.indexOf(':');
		String namespace = name.substring(0, colonIndex);
		String path = name.substring(colonIndex + 1);
		for(int i = startResourcePackIndex; i < ResourcePacks.getActiveResourcePacks().size(); ++i) {
			File file = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), 
									"builtins/" + namespace + "/blockstates/" + path + ".json");
			if(file.exists()) {
				return loadFile(file, name, i);
			}
		}
		return null;
	}
	
	public static class BuiltInBlockStateHandler{
		
		private List<String> blockNames;
		private boolean isLocationDependent;
		private Part rootPart;
		private Map<String, BuiltInGenerator> localGenerators;
		private Map<String, Value> localFunctions;
		private String defaultTexture;
		
		public BuiltInBlockStateHandler(String name, int resourcePackIndex, JsonObject data) {
			blockNames = new ArrayList<String>();
			isLocationDependent = false;
			rootPart = null;
			localGenerators = new HashMap<String, BuiltInGenerator>();
			localFunctions = new HashMap<String, Value>();
			defaultTexture = "";
			
			if(data.has("include")) {
				JsonArray includeArray = data.getAsJsonArray("include");
				for(JsonElement el : includeArray.asList()) {
					String includeName = el.getAsString();
					if(!includeName.contains(":"))
						includeName = "minecraft:" + includeName;
					
					BuiltInBlockStateHandler includedHandler = null;
					if(includeName.equalsIgnoreCase(name))
						includedHandler = loadFile(includeName, resourcePackIndex + 1);
					else
						includedHandler = loadFile(includeName, 0);
					
					if(includedHandler != null) {
						blockNames.addAll(includedHandler.blockNames);
						isLocationDependent = isLocationDependent || includedHandler.isLocationDependent;
						
						if(rootPart == null)
							rootPart = includedHandler.rootPart;
						else {
							if(rootPart instanceof ArrayPart) {
								((ArrayPart) rootPart).addChild(includedHandler.rootPart);
							}else if(rootPart instanceof ObjectPart) {
								Part tmpPart = rootPart;
								rootPart = new ArrayPart(null);
								((ArrayPart) rootPart).addChild(tmpPart);
								((ArrayPart) rootPart).addChild(includedHandler.rootPart);
							}
						}
						
						localGenerators.putAll(includedHandler.localGenerators);
						localFunctions.putAll(includedHandler.localFunctions);
						if(!includedHandler.defaultTexture.isEmpty())
							defaultTexture = includedHandler.defaultTexture;
					}
				}
			}
			
			if(data.has("blocks")) {
				blockNames.clear();
				for(JsonElement el : data.getAsJsonArray("blocks").asList()) {
					String blockName = el.getAsString();
					if(!blockName.contains(":"))
						blockName = "minecraft:" + blockName;
					blockNames.add(blockName);
				}
			}
			
			if(data.has("isLocationDependent"))
				isLocationDependent = data.get("isLocationDependent").getAsBoolean();
			
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
					localFunctions.put(entry.getKey(), new Value(new ValueFunction(new ExpressionMulti(code))));
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
	
	
	private BuiltInBlockStateHandler handler;
	
	public BuiltInBlockState(String name, int dataVersion, BuiltInBlockStateHandler handler) {
		super(name, dataVersion, null);
		this.handler = handler;
		this._needsConnectionInfo = this._needsConnectionInfo || handler.isLocationDependent;
	}
	
	@Override
	public String getDefaultTexture() {
		return handler.defaultTexture;
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, boolean runBlockConnections) {
		if(blockConnections != null && runBlockConnections) {
			properties = (NbtTagCompound) properties.copy();
			String newName = blockConnections.map(name, properties, x, y, z);
			if(newName != null && !newName.equals(name)) {
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
				properties.free();
				return BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, runBlockConnections);
			}
		}
		List<List<Model>> models = new ArrayList<List<Model>>();
		if(handler.rootPart != null) {
			Model model = new Model(name, null, isDoubleSided());
			Context context = new Context(name, properties, needsConnectionInfo(), x, y, z, model, 
					new Value(new ValueDict()), VALUE_BUILTINS, handler.localGenerators, handler.localFunctions);
			try {
				handler.rootPart.eval(context);
			}catch(Exception ex) {
				World.handleError(new RuntimeException("Error while evaluating block state " + name, ex));
			}
			if(!model.getFaces().isEmpty()) {
				model.calculateOccludes();
				List<Model> models2 = new ArrayList<Model>();
				models2.add(model);
				models.add(models2);
			}
		}
		
		Tint tint = Tints.getTint(name);
		Color tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		BakedBlockState bakedState = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, randomOffset, randomYOffset, grassColormap, foliageColormap, 
				waterColormap, doubleSided, randomAnimationXZOffset, randomAnimationYOffset, lodNoUVScale, lodPriority, tintColor, 
				needsConnectionInfo());
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}

}
