package nl.bramstout.mcworldexporter.pbr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.pbr.nodes.PbrAttribute;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNode;

public class PbrNodeGraph {
	
	public int priority;
	public List<String> selection;
	public List<PbrNodeGraph> executeNodeGraphs;
	public List<PbrNode> nodes;
	public List<PbrNode> outputs;
	public Map<String, List<PbrNode>> subGraphNodes;
	
	public PbrNodeGraph() {
		priority = 0;
		selection = new ArrayList<String>();
		executeNodeGraphs = new ArrayList<PbrNodeGraph>();
		nodes = new ArrayList<PbrNode>();
		outputs = new ArrayList<PbrNode>();
		subGraphNodes = new HashMap<String, List<PbrNode>>();
	}
	
	public void process(PbrContext context) {
		for(int i = 0; i < executeNodeGraphs.size(); ++i) {
			executeNodeGraphs.get(i).process(context);
		}
		
		List<PbrAttribute> attrs = new ArrayList<PbrAttribute>();
		for(int i = 0; i < nodes.size(); ++i) {
			PbrNode node = nodes.get(i);
			attrs.clear();
			node.getAttributes(attrs);
			for(int j = 0; j < attrs.size(); ++j) {
				PbrAttribute attr = attrs.get(j);
				if(attr.isContextSensitive())
					attr.notifyChange(context);
			}
		}
		
		for(int i = 0; i < outputs.size(); ++i) {
			PbrNode node = outputs.get(i);
			node.evaluate(null, context);
		}
	}
	
	public PbrNode getNode(String name) {
		PbrNode node = null;
		for(int i = 0; i < nodes.size(); ++i) {
			node = nodes.get(i);
			if(node.getName().equals(name))
				return node;
		}
		return null;
	}
	
	public PbrNodeGraph copy() {
		PbrNodeGraph graph = new PbrNodeGraph();
		graph.priority = priority;
		graph.selection = selection;
		for(PbrNodeGraph subGraph : executeNodeGraphs)
			graph.executeNodeGraphs.add(subGraph.copy());
		for(PbrNode node : nodes)
			graph.nodes.add(node.newInstanceOfSameType(graph));
		for(PbrNode node : graph.nodes) {
			PbrNode origNode = getNode(node.getName());
			node.copyFrom(origNode, graph);
		}
		for(PbrNode output : outputs) {
			graph.outputs.add(graph.getNode(output.getName()));
		}
		for(Entry<String, List<PbrNode>> entry : subGraphNodes.entrySet()) {
			List<PbrNode> nodes2 = new ArrayList<PbrNode>();
			for(PbrNode node : entry.getValue()) {
				nodes2.add(graph.getNode(node.getName()));
			}
			graph.subGraphNodes.put(entry.getKey(), nodes2);
		}
		return graph;
	}
	
	public void deleteNode(PbrNode node) {
		List<PbrAttribute> attrs = node.getAttributes();
		for(PbrAttribute attr : attrs) {
			if(attr.hasIncomingConnection())
				attr.disconnect();
			for(PbrAttribute outAttr : attr.getOutputs())
				outAttr.disconnect();
		}
		this.nodes.remove(node);
		this.outputs.remove(node);
	}
	
	
	public boolean isInSelection(String texture) {
		if(texture.isEmpty())
			return false;
		for(String selStr : selection) {
			if(selStr.isEmpty())
				continue;
			int texIndex = 0;
			int selIndex = 0;
			int lastWildcard = -1;
			boolean rechecked = false;
			while(true) {
				if(texIndex >= texture.length())
					break;
				
				if(texture.codePointAt(texIndex) == selStr.codePointAt(Math.min(selIndex, selStr.length()-1))) {
					texIndex++;
					selIndex++;
					rechecked = false;
				}else {
					texIndex++;
					if(rechecked)
						rechecked = false;
					else {
						texIndex--;
						rechecked = true;
					}
					if(selStr.codePointAt(Math.min(selIndex, selStr.length()-1)) == '*')
						lastWildcard = Math.min(selIndex, selStr.length()-1);
					if(lastWildcard >= 0) {
						selIndex = lastWildcard + 1;
					}else {
						break;
					}
				}
			}
			if(texIndex == texture.length() && selIndex == selStr.length())
				return true;
		}
		return false;
	}
	
}
