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

package nl.bramstout.mcworldexporter.pbr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrAttribute;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNode;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.TextureGroup;

public class PbrGenerator {
	
	private static ExecutorService threadPool = Executors.newWorkStealingPool(ThreadPool.getNumThreads());
	
	public List<ResourcePack> resourcePacks;
	public ResourcePack saveToResourcePack;
	public List<String> utilitySuffixes;
	
	private Map<String, PbrNodeGraph> graphs;
	private Queue<Texture> queue;
	private List<File> temporaryFiles;
	
	public PbrGenerator() {
		resourcePacks = new ArrayList<ResourcePack>();
		saveToResourcePack = null;
		utilitySuffixes = new ArrayList<String>();
		
		graphs = new HashMap<String, PbrNodeGraph>();
		queue = new ConcurrentLinkedQueue<Texture>();
		temporaryFiles = new ArrayList<File>();
	}
	
	public void process() {
		try {
			// Find all textures in the resource packs
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Finding textures");
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.25f);
			
			List<Entry<String, File>> textures = new ArrayList<Entry<String, File>>();
			for(int i = resourcePacks.size() - 1; i >= 0; --i) {
				textures.clear();
				resourcePacks.get(i).getTextures(textures, TextureGroup.BLOCKS, TextureGroup.ENTITY, TextureGroup.ITEMS);
				for(int j = 0; j < textures.size(); ++j) {
					Texture texture = new Texture();
					texture.texture = textures.get(j).getKey();
					
					boolean isUtility = false;
					for(int k = 0; k < utilitySuffixes.size(); ++k) {
						if(texture.texture.toLowerCase().endsWith(utilitySuffixes.get(k).toLowerCase())) {
							isUtility = true;
							break;
						}
					}
					if(isUtility)
						continue;
					
					int dotIndex = textures.get(j).getValue().getName().lastIndexOf((int) '.');
					if(dotIndex < 0)
						continue;
					texture.extension = textures.get(j).getValue().getName().substring(dotIndex);
					texture.resourcePack = resourcePacks.get(i);
					queue.add(texture);
				}
			}
			
			// Now go through the queue.
			MCWorldExporter.getApp().getUI().getProgressBar().setNumChunks(queue.size());
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Processing textures");
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
			
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for(int i = 0; i < ThreadPool.getNumThreads(); ++i) {
				futures.add(threadPool.submit(new PbrProcessor(this)));
			}
			for(Future<?> future : futures) {
				try {
					future.get();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		for(File file : temporaryFiles) {
			try {
				if(file.exists())
					file.delete();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
	}
	
	private static class Texture{
		
		public String texture;
		public String extension;
		public ResourcePack resourcePack;
		
	}
	
	private static class PbrProcessor implements Runnable{
		
		private PbrGenerator generator;
		
		public PbrProcessor(PbrGenerator generator) {
			this.generator = generator;
		}

		@Override
		public void run() {
			while(true) {
				Texture texture = generator.queue.poll();
				if(texture == null)
					return;
				
				try {
					PbrContext context = new PbrContext();
					context.texture = texture.texture;
					context.textureExtension = texture.extension;
					context.resourcePack = texture.resourcePack;
					context.resourcePacks = generator.resourcePacks;
					context.saveToResourcePack = generator.saveToResourcePack;
					
					PbrNodeGraph currentGraph = null;
					for(PbrNodeGraph graph : generator.graphs.values()) {
						if(graph.isInSelection(texture.texture)) {
							if(currentGraph == null)
								currentGraph = graph;
							else if(graph.priority > currentGraph.priority)
								currentGraph = graph;
						}
					}
					
					if(currentGraph == null)
						continue;
					
					currentGraph.process(context);
					
					if(!context.temporaryFiles.isEmpty()) {
						synchronized(generator.temporaryFiles) {
							generator.temporaryFiles.addAll(context.temporaryFiles);
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				MCWorldExporter.getApp().getUI().getProgressBar().finishedChunk();
			}
		}
		
	}
	
	public void init() {
		for(int i = 0; i < resourcePacks.size(); ++i) {
			File pbrFolder = new File(resourcePacks.get(i).getFolder(), "pbr");
			if(!pbrFolder.exists())
				continue;
			for(File namespace : pbrFolder.listFiles()) {
				if(namespace.isDirectory())
					parseNamespaceFolder(namespace, i);
			}
		}
	}
	
	private void parseNamespaceFolder(File folder, int currentIndex) {
		String namespace = folder.getName();
		File nodegraphsFolder = new File(folder, "nodegraphs");
		if(!nodegraphsFolder.exists())
			return;
		
		parseNodegraphFolder(nodegraphsFolder, namespace + ":", currentIndex);
	}
	
	private void parseNodegraphFolder(File folder, String parent, int currentIndex) {
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				parseNodegraphFolder(file, parent + file.getName() + "/", currentIndex);
			}else if(file.isFile()) {
				String id = parent + file.getName().split("\\.")[0];
				
				if(graphs.containsKey(id))
					continue;
				parseNodeGraph(file, id, currentIndex);
			}
		}
	}
	
	public PbrNodeGraph getGraph(String id) {
		PbrNodeGraph graph = graphs.getOrDefault(id, null);
		if(graph == null) {
			for(int i = 0; i < resourcePacks.size(); ++i) {
				File file = resourcePacks.get(i).getResource(id, "nodegraphs", "pbr", ".json");
				if(!file.exists())
					continue;
				parseNodeGraph(file, id, i);
				return graphs.get(id);
			}
		}
		return graph;
	}
	
	private void parseNodeGraph(File file, String id, int currentIndex) {
		try {
			JsonObject data = Json.read(file).getAsJsonObject();
			
			PbrNodeGraph graph = new PbrNodeGraph();
			
			parseNodeGraph(data, graph, id, currentIndex);
			
			graphs.put(id, graph);
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void parseNodeGraph(JsonObject data, PbrNodeGraph graph, String id, int currentIndex) {
		if(data.has("include")) {
			for(JsonElement el : data.getAsJsonArray("include").asList()) {
				String includeId = el.getAsString();
				if(!includeId.contains(":"))
					includeId = "minecraft:" + includeId;
				
				int startIndex = 0;
				if(includeId.equals(id))
					startIndex = currentIndex + 1;
				
				boolean found = false;
				for(int i = startIndex; i < resourcePacks.size(); ++i) {
					File file = resourcePacks.get(i).getResource(includeId, "nodegraphs", "pbr", ".json");
					if(!file.exists())
						continue;
					
					found = true;
					try {
						JsonObject includeData = Json.read(file).getAsJsonObject();
						
						parseNodeGraph(includeData, graph, id, i);
					}catch(Exception ex) {
						throw new RuntimeException("Could not include graph: " + includeId, ex);
					}
					break;
				}
				if(!found)
					throw new RuntimeException("Could not include graph: " + includeId);
			}
		}
		
		if(data.has("executeFirst")) {
			graph.executeNodeGraphs.clear();
			for(JsonElement el : data.getAsJsonArray("executeFirst").asList()) {
				String graphId = el.getAsString();
				if(!graphId.contains(":"))
					graphId = "minecraft:" + graphId;
				if(graphId == id)
					throw new RuntimeException("Cannot use id which is the same as the current id.");
				
				PbrNodeGraph graph2 = getGraph(graphId);
				graph.executeNodeGraphs.add(graph2);
			}
		}
		
		if(data.has("executeFirst.add")) {
			for(JsonElement el : data.getAsJsonArray("executeFirst").asList()) {
				String graphId = el.getAsString();
				if(!graphId.contains(":"))
					graphId = "minecraft:" + graphId;
				if(graphId == id)
					throw new RuntimeException("Cannot use id which is the same as the current id.");
				
				PbrNodeGraph graph2 = getGraph(graphId);
				graph.executeNodeGraphs.add(graph2);
			}
		}
		
		if(data.has("priority"))
			graph.priority = data.get("priority").getAsInt();
		
		if(data.has("selection")) {
			graph.selection.clear();
			for(JsonElement el : data.getAsJsonArray("selection").asList()) {
				graph.selection.add(el.getAsString());
			}
		}
		
		if(data.has("nodes")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("nodes").entrySet()) {
				parseNodeCreate(entry.getValue().getAsJsonObject(), entry.getKey(), graph);
			}
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("nodes").entrySet()) {
				parseNodeAttributes(entry.getValue().getAsJsonObject(), entry.getKey(), graph);
			}
		}
		
		if(data.has("outputs")) {
			graph.outputs.clear();
			for(JsonElement el : data.getAsJsonArray("outputs").asList()) {
				String nodeName = el.getAsString();
				PbrNode node = graph.getNode(nodeName);
				if(node == null)
					throw new RuntimeException("Cannot find node with name: " + nodeName);
				graph.outputs.add(node);
			}
		}
		
		if(data.has("outputs.add")) {
			for(JsonElement el : data.getAsJsonArray("outputs").asList()) {
				String nodeName = el.getAsString();
				PbrNode node = graph.getNode(nodeName);
				if(node == null)
					throw new RuntimeException("Cannot find node with name: " + nodeName);
				graph.outputs.add(node);
			}
		}
	}
	
	private void parseNodeCreate(JsonObject data, String name, PbrNodeGraph graph) {
		if(graph.getNode(name) != null)
			return; // No need to make it again
		if(data.has("type")) {
			String type = data.get("type").getAsString();
			
			if(type.equals("SubGraph")) {
				// Sub graphs are a special case.
				// Be basically want to copy and paste the nodes from the graph
				// into here, but with a prefix on the name.
				String prefix = name + ":";
				String subGraphId = null;
				if(data.has("attributes")) {
					JsonObject attributes = data.getAsJsonObject("attributes");
					if(attributes.has("graphName"))
						subGraphId = attributes.get("graphName").getAsString();
				}
				if(subGraphId == null)
					return;
				
				List<PbrNode> subGraphNodes = graph.subGraphNodes.getOrDefault(name, null);
				if(subGraphNodes != null) {
					for(PbrNode node : subGraphNodes)
						graph.deleteNode(node);
					subGraphNodes.clear();
				}else {
					subGraphNodes = new ArrayList<PbrNode>();
				}
				
				PbrNodeGraph subGraph = getGraph(subGraphId);
				if(subGraph == null)
					throw new RuntimeException("Could not find node graph with name: " + subGraphId);
				
				subGraph = subGraph.copy();
				for(PbrNode node : subGraph.nodes) {
					node.setName(prefix + node.getName());
					subGraphNodes.add(node);
					graph.nodes.add(node);
				}
				
				graph.subGraphNodes.put(name, subGraphNodes);
				
			}else {
				PbrNode node = PbrNodeRegistry.createNode(type, name, graph);
				graph.nodes.add(node);
			}
		}
	}
	
	private void parseNodeAttributes(JsonObject data, String name, PbrNodeGraph graph) {
		if(graph.subGraphNodes.containsKey(name)) {
			// This is a sub graph node, which is a special case
			String prefix = name + ":";
			
			if(data.has("attributes")) {
				JsonObject attributes = data.getAsJsonObject("attributes");
				for(Entry<String, JsonElement> entry : attributes.entrySet()) {
					int dotIndex = entry.getKey().lastIndexOf((int) '.');
					String nodeName = prefix + entry.getKey().substring(0, dotIndex);
					String attrName = entry.getKey().substring(dotIndex + 1);
					
					PbrNode node = graph.getNode(nodeName);
					if(node == null)
						throw new RuntimeException("Could not find node with name: " + nodeName);
					PbrAttribute attr = node.getAttribute(attrName);
					if(attr == null)
						throw new RuntimeException("Could not find attribute with name: " + attrName + " on node: " + nodeName);
					
					setAttribute(entry.getValue(), node, attr, graph);
				}
			}
		}else {
			PbrNode node = graph.getNode(name);
			if(node == null)
				throw new RuntimeException("Could not find node with name: " + name);
			if(data.has("attributes")) {
				JsonObject attributes = data.getAsJsonObject("attributes");
				for(Entry<String, JsonElement> entry : attributes.entrySet()) {
					PbrAttribute attr = node.getAttribute(entry.getKey());
					if(attr == null)
						throw new RuntimeException("Could not find attribute with name: " + entry.getKey() + " on node: " + name);
					setAttribute(entry.getValue(), node, attr, graph);
				}
			}
		}
	}
	
	private void setAttribute(JsonElement data, PbrNode node, PbrAttribute attr, PbrNodeGraph graph) {
		if(data.isJsonPrimitive()) {
			JsonPrimitive prim = data.getAsJsonPrimitive();
			if(prim.isBoolean()) {
				attr.setValue(Boolean.valueOf(prim.getAsBoolean()), null);
				return;
			}else if(prim.isNumber()) {
				attr.setValue(prim.getAsNumber(), null);
				return;
			}else if(prim.isString()) {
				attr.setValue(prim.getAsString(), null);
				return;
			}
		}else if(data.isJsonArray()) {
			RGBA rgba = new RGBA();
			JsonArray array = data.getAsJsonArray();
			if(array.size() == 1) {
				float v = array.get(0).getAsFloat();
				rgba.r = v;
				rgba.g = v;
				rgba.b = v;
				rgba.a = v;
			}else if(array.size() == 2) {
				float r = array.get(0).getAsFloat();
				float g = array.get(1).getAsFloat();
				rgba.r = r;
				rgba.g = g;
			}else if(array.size() == 3) {
				float r = array.get(0).getAsFloat();
				float g = array.get(1).getAsFloat();
				float b = array.get(2).getAsFloat();
				rgba.r = r;
				rgba.g = g;
				rgba.b = b;
			}else if(array.size() >= 4) {
				float r = array.get(0).getAsFloat();
				float g = array.get(1).getAsFloat();
				float b = array.get(2).getAsFloat();
				float a = array.get(3).getAsFloat();
				rgba.r = r;
				rgba.g = g;
				rgba.b = b;
				rgba.a = a;
			}
			attr.setValue(rgba, null);
			return;
		}else if(data.isJsonObject()) {
			JsonObject obj = data.getAsJsonObject();
			if(obj.has("conn")) {
				String inputName = obj.get("conn").getAsString();
				
				int dotIndex = inputName.lastIndexOf((int) '.');
				String nodeName = inputName;
				String attrName = null;
				if(dotIndex >= 0) {
					nodeName = inputName.substring(0, dotIndex);
					attrName = inputName.substring(dotIndex + 1);
				}
				
				PbrNode node2 = graph.getNode(nodeName);
				if(node2 == null)
					throw new RuntimeException("Could not find node with name: " + nodeName);
				PbrAttribute attr2 = null;
				if(attrName == null) {
					List<PbrAttribute> attrs2 = node2.getAttributes();
					for(PbrAttribute attr22 : attrs2) {
						if(attr22.isOutput()) {
							attr2 = attr22;
							break;
						}
					}
				}else {
					attr2 = node2.getAttribute(attrName);
				}
				if(attr2 == null)
					throw new RuntimeException("Could not find attribute with name: " + attrName + " on node: " + nodeName);
				
				attr.connect(attr2);
				
				return;
			}
		}
		throw new RuntimeException("Invalid value for node: " + node.getName() + " and attribute: " + attr.getName());
	}

}
