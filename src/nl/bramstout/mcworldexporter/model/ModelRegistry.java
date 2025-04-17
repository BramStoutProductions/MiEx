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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.model.builtins.BuiltInModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.java.ModelHandlerJavaEdition;
import nl.bramstout.mcworldexporter.world.World;

public class ModelRegistry {

	private static List<Model> registeredModels = new ArrayList<Model>();
	private static Map<String, Integer> nameToId = new HashMap<String, Integer>();
	private static Object mutex = new Object();
	private static int counter = 0;
	public static List<String> missingModels = new ArrayList<String>();
	
	public static int getIdForName(String name, boolean doubleSided) {
		if(!name.contains("geometry.")) {
			if(!name.contains("/"))
				name = "block/" + name;
			if(!name.contains(":"))
				name = "minecraft:" + name;
		}
		String idName = name;
		if(doubleSided)
			idName = idName + "DS";
		Integer id = nameToId.get(idName);
		if(id == null) {
			synchronized(mutex) {
				id = nameToId.get(idName);
				if(id == null) {
					Model model = getModelFromName(name, doubleSided);
					nameToId.put(idName, model.getId());
					return model.getId();
				}
			}
		}
		return id.intValue();
	}
	
	public static int getNextId(Model model) {
		synchronized(mutex) {
			counter++;
			if(registeredModels.size() <= counter)
				for(int i = registeredModels.size(); i <= counter; ++i)
					registeredModels.add(null);
			registeredModels.set(counter, model);
			return counter;
		}
	}
	
	public static Model getModel(int id) {
		return registeredModels.get(id);
	}
	
	private static Model getModelFromName(String name, boolean doubleSided) {
		if(BuiltInModelRegistry.builtins.containsKey(name)) {
			if(!ResourcePacks.hasOverride(name, "models", ".json", "assets"))
				return BuiltInModelRegistry.newModel(name);
		}
		ModelHandler handler = ResourcePacks.getModelHandler(name);
		if(handler == null) {
			synchronized(missingModels) {
				missingModels.add(name);
			}
			World.handleError(new RuntimeException("No model file for " + name));
			
			// Make sure that there is a valid handler anyways.
			handler = new ModelHandlerJavaEdition(null);
		}
		return new Model(name, handler, doubleSided);
	}
	
	public static void clearModelRegistry() {
		synchronized(mutex) {
			registeredModels.clear();
			nameToId.clear();
			counter = 0;
		}
		synchronized(missingModels) {
			missingModels.clear();
		}
	}
	
}
