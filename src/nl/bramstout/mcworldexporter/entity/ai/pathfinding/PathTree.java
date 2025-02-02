package nl.bramstout.mcworldexporter.entity.ai.pathfinding;

import java.util.Arrays;

public class PathTree {
	
	private PathNode[] nodes;
	private int size;
	
	public PathTree() {
		nodes = new PathNode[16];
		size = 0;
	}
	
	private void reserve() {
		if(size >= nodes.length)
			nodes = Arrays.copyOf(nodes, size * 2);
	}
	
	public void addNode(PathNode node) {
		// First check if we already have it, then update it with the new cost,
		// if the new cost is lower.
		for(int i = 0; i < size; ++i) {
			if(nodes[i].equals(node)) {
				if(node.getTotalCost() < nodes[i].getTotalCost()) {
					nodes[i].setCost(node.getCost(), node.getTotalCost());
					return;
				}
			}
		}
		
		reserve();
		nodes[size] = node;
		size++;
	}
	
	public PathNode removeNode(int index) {
		PathNode node = nodes[index];
		for(int i = index + 1; i < size; ++i)
			nodes[i-1] = nodes[i];
		size--;
		return node;
	}
	
	public int getSize() {
		return size;
	}
	
	public PathNode getNode(int index) {
		return nodes[index];
	}

}
