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

package nl.bramstout.mcworldexporter.entity.attachments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.entity.attachments.Attachment.AttachmentLocation;
import nl.bramstout.mcworldexporter.expression.ExprContext;
import nl.bramstout.mcworldexporter.expression.ExprValue;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueDict;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ArrayPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.ObjectPart;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInModel.Part;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.World;

public class AttachmentHandlerBuiltIn extends AttachmentHandler{
	
	private static Map<String, AttachmentHandlerBuiltIn> handlerRegistry = new HashMap<String, AttachmentHandlerBuiltIn>();
	
	public static AttachmentHandlerBuiltIn getHandler(String attachmentName) {
		return handlerRegistry.getOrDefault(attachmentName, null);
	}
	
	public static void load() {
		Map<String, AttachmentHandlerBuiltIn> handlerRegistry = new HashMap<String, AttachmentHandlerBuiltIn>();
		
		for(int i = ResourcePacks.getActiveResourcePacks().size() - 1; i >= 0; --i) {
			File builtInFolder = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), "builtins");
			if(!builtInFolder.exists() || !builtInFolder.isDirectory())
				continue;
			for(File namespaceFolder : builtInFolder.listFiles()) {
				if(!namespaceFolder.isDirectory())
					continue;
				File blockstatesFolder = new File(namespaceFolder, "attachments");
				if(blockstatesFolder.exists() && blockstatesFolder.isDirectory())
					loadFolder(blockstatesFolder, namespaceFolder.getName() + ":", i, handlerRegistry);
			}
		}
		
		AttachmentHandlerBuiltIn.handlerRegistry = handlerRegistry;
	}
	
	private static void loadFolder(File folder, String parent, int resourcePackIndex, 
			Map<String, AttachmentHandlerBuiltIn> handlerRegistry) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadFolder(file, parent + file.getName() + "/", resourcePackIndex, handlerRegistry);
			}else if(file.isFile()) {
				if(!file.getName().endsWith(".json"))
					continue;
				String name = file.getName();
				int dotIndex = name.lastIndexOf('.');
				name = name.substring(0, dotIndex);
				AttachmentHandlerBuiltIn handler = loadFile(file, parent + name, resourcePackIndex);
				for(String entityName : handler.attachmentNames)
					handlerRegistry.put(entityName, handler);
			}
		}
	}
	
	private static AttachmentHandlerBuiltIn loadFile(File file, String name, int resourcePackIndex) {
		JsonObject data = Json.read(file).getAsJsonObject();
		AttachmentHandlerBuiltIn handler = new AttachmentHandlerBuiltIn(name, resourcePackIndex, data);
		return handler;
	}
	
	private static AttachmentHandlerBuiltIn loadFile(String name, int startResourcePackIndex) {
		int colonIndex = name.indexOf(':');
		String namespace = name.substring(0, colonIndex);
		String path = name.substring(colonIndex + 1);
		for(int i = startResourcePackIndex; i < ResourcePacks.getActiveResourcePacks().size(); ++i) {
			File file = new File(ResourcePacks.getActiveResourcePacks().get(i).getFolder(), 
									"builtins/" + namespace + "/attachments/" + path + ".json");
			if(file.exists()) {
				return loadFile(file, name, i);
			}
		}
		return null;
	}
	
	private List<String> attachmentNames;
	private Map<AttachmentLocation, BuiltInModel> models;
	
	public AttachmentHandlerBuiltIn(String name, int resourcePackIndex, JsonObject data) {
		attachmentNames = new ArrayList<String>();
		models = new HashMap<AttachmentLocation, BuiltInModel>();
		
		if(data.has("include")) {
			JsonArray includeArray = data.getAsJsonArray("include");
			for(JsonElement el : includeArray.asList()) {
				String includeName = el.getAsString();
				if(!includeName.contains(":"))
					includeName = "minecraft:" + includeName;
				
				AttachmentHandlerBuiltIn includedHandler = null;
				if(includeName.equalsIgnoreCase(name))
					includedHandler = loadFile(includeName, resourcePackIndex + 1);
				else
					includedHandler = loadFile(includeName, 0);
				
				if(includedHandler != null) {
					attachmentNames.addAll(includedHandler.attachmentNames);
					
					for(Entry<AttachmentLocation, BuiltInModel> entry : includedHandler.models.entrySet()) {
						BuiltInModel model = models.getOrDefault(entry.getKey(), null);
						if(model == null) {
							model = new BuiltInModel();
							models.put(entry.getKey(), model);
						}
						
						if(model.rootPart == null)
							model.rootPart = entry.getValue().rootPart;
						else {
							if(model.rootPart instanceof ArrayPart) {
								((ArrayPart) model.rootPart).addChild(entry.getValue().rootPart);
							}else if(model.rootPart instanceof ObjectPart) {
								Part tmpPart = model.rootPart;
								model.rootPart = new ArrayPart(null);
								((ArrayPart) model.rootPart).addChild(tmpPart);
								((ArrayPart) model.rootPart).addChild(entry.getValue().rootPart);
							}
						}
						
						model.localGenerators.putAll(entry.getValue().localGenerators);
						model.localFunctions.putAll(entry.getValue().localFunctions);
						if(!entry.getValue().defaultTexture.isEmpty())
							model.defaultTexture = entry.getValue().defaultTexture;
					}
				}
			}
		}
		
		if(data.has("attachments")) {
			attachmentNames.clear();
			for(JsonElement el : data.getAsJsonArray("attachments").asList()) {
				String attachmentName = el.getAsString();
				if(!attachmentName.contains(":"))
					attachmentName = "minecraft:" + attachmentName;
				attachmentNames.add(attachmentName);
			}
		}
		
		for(Entry<String, JsonElement> entry : data.entrySet()) {
			AttachmentLocation loc = AttachmentLocation.fromName(entry.getKey());
			if(loc == AttachmentLocation.NONE && !entry.getKey().equals("none"))
				continue;
			if(!entry.getValue().isJsonObject()) {
				models.remove(loc);
				continue;
			}
			
			BuiltInModel model = models.getOrDefault(loc, null);
			if(model == null) {
				model = new BuiltInModel();
				models.put(loc, model);
			}
			
			model.parse(entry.getValue().getAsJsonObject());
		}
	}
	
	@Override
	public Model getModel(Attachment attachment, AttachmentLocation location) {
		BuiltInModel modelHandler = models.getOrDefault(location, null);
		if(modelHandler == null) {
			return new Model(attachment.getName(), null, false);
		}
		
		Model model = new Model(attachment.getName(), null, modelHandler.doubleSided);
		ExprContext context = new ExprContext(attachment.getName(), attachment.getProperties(), false, 
				0, 0, 0, 0, 0, 0, 0f, 0, 0,
				model, new ExprValue(new ExprValueDict()), ExprValue.VALUE_BUILTINS, 
				modelHandler.localGenerators, modelHandler.localFunctions);
		try {
			modelHandler.rootPart.eval(context);
		}catch(Exception ex) {
			World.handleError(new RuntimeException("Error while evaluating entity " + attachment.getName(), ex));
		}
		model.addRootBone();
		return model;
	}

}
