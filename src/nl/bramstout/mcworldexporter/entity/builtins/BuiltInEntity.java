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

package nl.bramstout.mcworldexporter.entity.builtins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.expression.ExprContext;
import nl.bramstout.mcworldexporter.expression.ExprParser;
import nl.bramstout.mcworldexporter.expression.ExprValue;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueDict;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionMulti;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ArrayPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ObjectPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.Part;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.World;

public class BuiltInEntity extends EntityHandler{

	private static Map<String, BuiltInEntityHandler> handlerRegistry = new HashMap<String, BuiltInEntityHandler>();
	
	public static BuiltInEntityHandler getHandler(String entityName) {
		return handlerRegistry.getOrDefault(entityName, null);
	}
	
	public static void load() {
		Map<String, BuiltInEntityHandler> handlerRegistry = new HashMap<String, BuiltInEntityHandler>();
		
		for(int i = ResourcePacks.getActiveResourcePacks().size() - 1; i >= 0; --i) {
			File builtInFolder = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), "builtins");
			if(!builtInFolder.exists() || !builtInFolder.isDirectory())
				continue;
			for(File namespaceFolder : builtInFolder.listFiles()) {
				if(!namespaceFolder.isDirectory())
					continue;
				File blockstatesFolder = new File(namespaceFolder, "entities");
				if(blockstatesFolder.exists() && blockstatesFolder.isDirectory())
					loadFolder(blockstatesFolder, namespaceFolder.getName() + ":", i, handlerRegistry);
			}
		}
		
		BuiltInEntity.handlerRegistry = handlerRegistry;
	}
	
	private static void loadFolder(File folder, String parent, int resourcePackIndex, 
			Map<String, BuiltInEntityHandler> handlerRegistry) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadFolder(file, parent + file.getName() + "/", resourcePackIndex, handlerRegistry);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				String name = file.getName();
				int dotIndex = name.lastIndexOf('.');
				name = name.substring(0, dotIndex);
				BuiltInEntityHandler handler = loadFile(file, parent + name, resourcePackIndex);
				for(String entityName : handler.entityNames)
					handlerRegistry.put(entityName, handler);
			}
		}
	}
	
	private static BuiltInEntityHandler loadFile(File file, String name, int resourcePackIndex) {
		JsonObject data = Json.read(file).getAsJsonObject();
		BuiltInEntityHandler handler = new BuiltInEntityHandler(name, resourcePackIndex, data);
		return handler;
	}
	
	private static BuiltInEntityHandler loadFile(String name, int startResourcePackIndex) {
		int colonIndex = name.indexOf(':');
		String namespace = name.substring(0, colonIndex);
		String path = name.substring(colonIndex + 1);
		for(int i = startResourcePackIndex; i < ResourcePacks.getActiveResourcePacks().size(); ++i) {
			File file = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), 
									"builtins/" + namespace + "/entities/" + path + ".json");
			if(file.exists()) {
				return loadFile(file, name, i);
			}
		}
		return null;
	}
	
	public static class BuiltInEntityHandler{
		
		private List<String> entityNames;
		private boolean isLocationDependent;
		private ExpressionMulti posXExpr;
		private ExpressionMulti posYExpr;
		private ExpressionMulti posZExpr;
		private ExpressionMulti rotXExpr;
		private ExpressionMulti rotYExpr;
		private BuiltInModel model;
		
		public BuiltInEntityHandler(String name, int resourcePackIndex, JsonObject data) {
			entityNames = new ArrayList<String>();
			isLocationDependent = false;
			model = new BuiltInModel();
			
			if(data.has("include")) {
				JsonArray includeArray = data.getAsJsonArray("include");
				for(JsonElement el : includeArray.asList()) {
					String includeName = el.getAsString();
					if(!includeName.contains(":"))
						includeName = "minecraft:" + includeName;
					
					BuiltInEntityHandler includedHandler = null;
					if(includeName.equalsIgnoreCase(name))
						includedHandler = loadFile(includeName, resourcePackIndex + 1);
					else
						includedHandler = loadFile(includeName, 0);
					
					if(includedHandler != null) {
						entityNames.addAll(includedHandler.entityNames);
						isLocationDependent = isLocationDependent || includedHandler.isLocationDependent;
						
						if(model.rootPart == null)
							model.rootPart = includedHandler.model.rootPart;
						else {
							if(model.rootPart instanceof ArrayPart) {
								((ArrayPart) model.rootPart).addChild(includedHandler.model.rootPart);
							}else if(model.rootPart instanceof ObjectPart) {
								Part tmpPart = model.rootPart;
								model.rootPart = new ArrayPart(null);
								((ArrayPart) model.rootPart).addChild(tmpPart);
								((ArrayPart) model.rootPart).addChild(includedHandler.model.rootPart);
							}
						}
						
						model.localGenerators.putAll(includedHandler.model.localGenerators);
						model.localFunctions.putAll(includedHandler.model.localFunctions);
						if(!includedHandler.model.defaultTexture.isEmpty())
							model.defaultTexture = includedHandler.model.defaultTexture;
					}
				}
			}
			
			if(data.has("entities")) {
				entityNames.clear();
				for(JsonElement el : data.getAsJsonArray("entities").asList()) {
					String blockName = el.getAsString();
					if(!blockName.contains(":"))
						blockName = "minecraft:" + blockName;
					entityNames.add(blockName);
				}
			}
			
			if(data.has("isLocationDependent"))
				isLocationDependent = data.get("isLocationDependent").getAsBoolean();
			
			if(data.has("posX"))
				posXExpr = ExprParser.parseMultiExpression(data.get("posX").getAsString());
			if(data.has("posY"))
				posYExpr = ExprParser.parseMultiExpression(data.get("posY").getAsString());
			if(data.has("posZ"))
				posZExpr = ExprParser.parseMultiExpression(data.get("posZ").getAsString());
			if(data.has("rotX"))
				rotXExpr = ExprParser.parseMultiExpression(data.get("rotX").getAsString());
			if(data.has("rotY"))
				rotYExpr = ExprParser.parseMultiExpression(data.get("rotY").getAsString());
			
			model.parse(data);
		}
		
	}
	
	private String name;
	private BuiltInEntityHandler handler;
	
	public BuiltInEntity(String name, BuiltInEntityHandler handler) {
		this.name = name;
		this.handler = handler;
	}
	
	@Override
	public Model getModel(Entity entity) {
		Model model = new Model(entity.getId(), null, true);
		ExprContext context = new ExprContext(entity.getId(), entity.getProperties(), handler.isLocationDependent, 
				(int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()), 
				entity.getX(), entity.getY(), entity.getZ(), 
				model, new ExprValue(new ExprValueDict()), ExprValue.VALUE_BUILTINS, 
				handler.model.localGenerators, handler.model.localFunctions);
		try {
			handler.model.rootPart.eval(context);
		}catch(Exception ex) {
			World.handleError(new RuntimeException("Error while evaluating entity " + name, ex));
		}
		model.addRootBone();
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
		if(entity.getProperties().get("TileX") != null) {
			float blockX = entity.getProperties().get("TileX").asFloat() + 0.5f;
			float blockY = entity.getProperties().get("TileY").asFloat();
			float blockZ = entity.getProperties().get("TileZ").asFloat() + 0.5f;
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
		if(entity.getProperties().get("x") != null) {
			float blockX = entity.getProperties().get("x").asFloat();
			float blockY = entity.getProperties().get("y").asFloat();
			float blockZ = entity.getProperties().get("z").asFloat();
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
		if(entity.getProperties().get("block_pos") != null) {
			NbtTagIntArray pos = (NbtTagIntArray) entity.getProperties().get("block_pos");
			int blockX = pos.getData()[0];
			int blockY = pos.getData()[1];
			int blockZ = pos.getData()[2];
			entity.setX(((float) blockX) + 0.5f);
			entity.setY(blockY);
			entity.setZ(((float) blockZ) + 0.5f);
		}
		NbtTag rotTag = entity.getProperties().get("Rotation");
		if(rotTag != null && rotTag instanceof NbtTagList) {
			NbtTagList rotList = (NbtTagList) rotTag;
			entity.setYaw(rotList.get(0).asFloat());
			entity.setPitch(rotList.get(1).asFloat());
		}
		
		ExprContext context = new ExprContext(entity.getId(), entity.getProperties(), handler.isLocationDependent, 
				(int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()), 
				entity.getX(), entity.getY(), entity.getZ(), 
				null, new ExprValue(new ExprValueDict()), ExprValue.VALUE_BUILTINS, 
				handler.model.localGenerators, handler.model.localFunctions);
		
		if(handler.posXExpr != null)
			entity.setX(handler.posXExpr.eval(context).asFloat());
		if(handler.posYExpr != null)
			entity.setY(handler.posYExpr.eval(context).asFloat());
		if(handler.posZExpr != null)
			entity.setZ(handler.posZExpr.eval(context).asFloat());
		if(handler.rotXExpr != null)
			entity.setPitch(handler.rotXExpr.eval(context).asFloat());
		if(handler.rotYExpr != null)
			entity.setYaw(handler.rotYExpr.eval(context).asFloat());
	}

	@Override
	public EntityAIHandler getAIHandler(Entity entity) {
		return null;
	}

}
