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

package nl.bramstout.mcworldexporter.modifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class Modifiers {

	private List<Modifier> blockModifiers;
	private List<Modifier> faceModifiers;
	
	private Modifiers() {
		this.blockModifiers = null;
		this.faceModifiers = null;
	}
	
	private void addBlockModifier(Modifier modifier) {
		if(this.blockModifiers == null)
			this.blockModifiers = new ArrayList<Modifier>();
		
		// First check if there already is a modifier in the same group.
		// If so and this modifier has a higher priority, then replace
		// it. If so and this modifier doesn't have a higher priority,
		// keep the one that we already have.
		for(int i = 0; i < blockModifiers.size(); ++i) {
			if(blockModifiers.get(i).getGroup().equals(modifier.getGroup())) {
				// We've got a match, so check the priority;
				if(modifier.getPriority() > blockModifiers.get(i).getPriority()) {
					blockModifiers.set(i, modifier);
				}
				return;
			}
		}
		// No match with an existing block modifier, so just add it in.
		this.blockModifiers.add(modifier);
	}
	
	private void addFaceModifiers(Modifier modifier) {
		if(this.faceModifiers == null)
			this.faceModifiers = new ArrayList<Modifier>();

		// First check if there already is a modifier in the same group.
		// If so and this modifier has a higher priority, then replace
		// it. If so and this modifier doesn't have a higher priority,
		// keep the one that we already have.
		for(int i = 0; i < faceModifiers.size(); ++i) {
			if(faceModifiers.get(i).getGroup().equals(modifier.getGroup())) {
				// We've got a match, so check the priority;
				if(modifier.getPriority() > faceModifiers.get(i).getPriority()) {
					faceModifiers.set(i, modifier);
				}
				return;
			}
		}
		// No match with an existing face modifier, so just add it in.
		this.faceModifiers.add(modifier);
	}
	
	public boolean hasBlockModifiers() {
		return this.blockModifiers != null;
	}
	
	public boolean hasFaceModifiers() {
		return this.faceModifiers != null;
	}
	
	public boolean hasModifiers() {
		return hasBlockModifiers() || hasFaceModifiers();
	}
	
	public void runBlockModifiers(ModifierContext context) {
		if(this.blockModifiers == null)
			return;
		for(Modifier modifier : this.blockModifiers)
			modifier.run(context);
	}
	
	public void runFaceModifiers(ModifierContext context) {
		if(this.faceModifiers == null)
			return;
		for(Modifier modifier : this.faceModifiers)
			modifier.run(context);
	}
	
	private static List<Modifiers> modifiersForBlockId = new ArrayList<Modifiers>();
	
	public static Modifiers getModifiersForBlockId(int blockId) {
		if(blockId >= modifiersForBlockId.size()) {
			synchronized(modifiersForBlockId) {
				if(blockId >= modifiersForBlockId.size()) {
					for(int i = modifiersForBlockId.size(); i <= blockId; ++i) {
						modifiersForBlockId.add(loadModifiersForBlockId(i));
					}
				}
			}
		}
		Modifiers modifiers = modifiersForBlockId.get(blockId);
		return modifiers;
	}
	
	private static Modifiers defaultModifiers = null;
	private static Map<String, Modifiers> modifiersRegistry = new HashMap<String, Modifiers>();
	
	private static Modifiers loadModifiersForBlockId(int blockId) {
		Block block = BlockRegistry.getBlock(blockId);
		return modifiersRegistry.getOrDefault(block.getName(), defaultModifiers);
	}
	
	public static void load() {
		modifiersRegistry.clear();
		modifiersForBlockId.clear();
		defaultModifiers = null;
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File modifiersFolder = new File(resourcePacks.get(i).getFolder(), "modifiers");
			if(!modifiersFolder.exists())
				continue;
			loadNamespaces(modifiersFolder, resourcePacks.size() - i);
		}
		if(defaultModifiers != null) {
			for(Modifiers modifiers : modifiersRegistry.values()) {
				if(defaultModifiers.blockModifiers != null)
					for(Modifier modifier : defaultModifiers.blockModifiers)
						modifiers.addBlockModifier(modifier);
				if(defaultModifiers.faceModifiers != null)
					for(Modifier modifier : defaultModifiers.faceModifiers)
						modifiers.addFaceModifiers(modifier);
			}
		}
	}
	
	private static void loadNamespaces(File modifiersFolder, int rpPriority) {
		for(File namespaceFolder : modifiersFolder.listFiles()) {
			File blockModifiersFolder = new File(namespaceFolder, "block");
			if(blockModifiersFolder.exists()) {
				loadModifiers(blockModifiersFolder, "block", rpPriority);
			}
			
			File faceModifiersFolder = new File(namespaceFolder, "face");
			if(faceModifiersFolder.exists()) {
				loadModifiers(faceModifiersFolder, "face", rpPriority);
			}
		}
	}
	
	private static void loadModifiers(File file, String type, int rpPriority) {
		if(file.isDirectory()) {
			for(File f : file.listFiles()) {
				loadModifiers(f, type, rpPriority);
			}
			return;
		}
		if(!file.isFile() || !file.getName().endsWith(".json"))
			return;
		
		try {
			JsonObject data = Json.read(file).getAsJsonObject();
			Modifier modifier = new Modifier(data, rpPriority);
			
			if(modifier.isAllBlocks()) {
				if(defaultModifiers == null)
					defaultModifiers = new Modifiers();
				if(type.equals("block"))
					defaultModifiers.addBlockModifier(modifier);
				else if(type.equals("face"))
					defaultModifiers.addFaceModifiers(modifier);
			}else {
				for(String blockName : modifier.getBlocks()) {
					Modifiers modifiers = modifiersRegistry.getOrDefault(blockName, null);
					if(modifiers == null) {
						modifiers = new Modifiers();
						modifiersRegistry.put(blockName, modifiers);
					}
					if(type.equals("block"))
						modifiers.addBlockModifier(modifier);
					else if(type.equals("face"))
						modifiers.addFaceModifiers(modifier);
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
