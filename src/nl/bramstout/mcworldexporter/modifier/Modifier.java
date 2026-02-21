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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.resourcepack.Tags;

public class Modifier {
	
	private boolean allBlocks;
	private List<String> blocks;
	private String group;
	private int priority;
	protected Map<String, ModifierNode> nodes;
	private List<String> runNodes;
	
	public Modifier(JsonObject data, int rpPriority) {
		this.allBlocks = false;
		this.blocks = new ArrayList<String>();
		this.group = "";
		this.priority = rpPriority << 16;
		this.nodes = new HashMap<String, ModifierNode>();
		this.runNodes = new ArrayList<String>();
		
		if(data.has("blocks")) {
			JsonElement blocksEl = data.get("blocks");
			if(blocksEl.isJsonPrimitive()) {
				String block = blocksEl.getAsString();
				if(block.equals("*")) {
					allBlocks = true;
				}else {
					if(block.charAt(0) == '#') {
						// It's a tag
						List<String> tags = Tags.getNamesInTag(block);
						blocks.addAll(tags);
					}else {
						if(block.indexOf(':') == -1)
							block = "minecraft:" + block;
						blocks.add(block);
					}
				}
			}else {
				JsonArray blocksArray = blocksEl.getAsJsonArray();
				for(JsonElement el : blocksArray.asList()) {
					String block = el.getAsString();
					if(block.charAt(0) == '#') {
						// It's a tag
						List<String> tags = Tags.getNamesInTag(block);
						blocks.addAll(tags);
					}else {
						if(block.indexOf(':') == -1)
							block = "minecraft:" + block;
						blocks.add(block);
					}
				}
			}
		}
		
		if(data.has("group"))
			this.group = data.get("group").getAsString();
		
		if(data.has("priority"))
			this.priority = (rpPriority << 16) + data.get("priority").getAsInt();
		
		if(data.has("runNodes")) {
			JsonElement el = data.get("runNodes");
			if(el.isJsonPrimitive()) {
				runNodes.add(el.getAsString());
			}else if(el.isJsonArray()) {
				for(JsonElement el2 : el.getAsJsonArray().asList()) {
					runNodes.add(el2.getAsString());
				}
			}
		}
		
		if(data.has("nodes")) {
			JsonObject nodesObj = data.getAsJsonObject("nodes");
			for(Entry<String, JsonElement> entry : nodesObj.entrySet()) {
				if(entry.getValue().isJsonObject()) {
					ModifierNode node = ModifierNodeRegistry.createNode(entry.getKey(), entry.getValue().getAsJsonObject());
					if(node != null)
						nodes.put(entry.getKey(), node);
				}
			}
		}
	}
	
	public boolean isAllBlocks() {
		return allBlocks;
	}
	
	public List<String> getBlocks(){
		return blocks;
	}
	
	public String getGroup() {
		return group;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public ModifierNode getNode(String name) {
		return nodes.getOrDefault(name, null);
	}
	
	public void run(ModifierContext context) {
		context.currentModifier = this;
		for(String runNode : runNodes) {
			ModifierNode node = nodes.getOrDefault(runNode, null);
			if(node != null) {
				node.evaluate(context);
			}
		}
	}

}
