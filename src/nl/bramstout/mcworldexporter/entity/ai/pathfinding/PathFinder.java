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

package nl.bramstout.mcworldexporter.entity.ai.pathfinding;

import nl.bramstout.mcworldexporter.export.IndexCache;

public class PathFinder {
	
	private PathTree tree;
	private PathNode start;
	private PathNode goal;
	private IndexCache visitedNodes;
	private PathFinderHook hook; 
	
	public PathFinder(PathFinderHook hook) {
		this.tree = null;
		this.start = null;
		this.goal = null;
		this.visitedNodes = null;
		this.hook = hook;
	}
	
	public Path pathFind(PathNode start, PathNode goal) {
		this.tree = new PathTree();
		this.start = start;
		this.goal = goal;
		this.visitedNodes = new IndexCache();
		
		this.tree.addNode(this.start);
		int counter = 0;
		int maxIterations = 1000;
		while(this.tree.getSize() > 0) {
			counter++;
			PathNode currentNode = getLowestCostNode();
			if(currentNode == null)
				break;
			
			if(currentNode.equals(this.goal) || counter >= maxIterations) {
				Path path = getPath(currentNode);
				this.tree = null;
				this.visitedNodes = null;
				return path;
			}
			
			long currentNodeId = getNodeId(currentNode.getX(), currentNode.getY(), currentNode.getZ());
			this.visitedNodes.put(currentNodeId, 1);
			
			// Check neighbours
			for(int dx = -1; dx <= 1; ++dx) {
				for(int dy = -1; dy <= 1; ++dy) {
					for(int dz = -1; dz <= 1; ++dz) {
						if(dx == 0 && dy == 0 && dz == 0)
							continue; // Not a neighbour
						
						int x = currentNode.getX() + dx;
						int y = currentNode.getY() + dy;
						int z = currentNode.getZ() + dz;
						long nodeId = getNodeId(x, y, z);
						if(visitedNodes.getOrDefault(nodeId, 0) != 0)
							continue; // Already visited
						
						float cost = hook.getCost(x, y, z, dx, dy, dz);
						if(cost < 0f)
							continue; // Hook indicates this neighbour node should not be visited.
						cost += currentNode.getCost();
						float totalCost = cost + hook.getAdditionalCost(x, y, z, this.goal.getX(), this.goal.getY(), this.goal.getZ());
						this.tree.addNode(new PathNode(x, y, z, cost, totalCost, currentNode));
					}
				}
			}
		}
		
		this.tree = null;
		this.visitedNodes = null;
		
		return null;
	}
	
	private long getNodeId(int x, int y, int z) {
		x -= start.getX();
		y -= start.getY();
		z -= start.getZ();
		long lx = x & 0xFFFFF | ((x >>> 31) << 20);
		long ly = y & 0xFFFFF | ((y >>> 31) << 20);
		long lz = z & 0xFFFFF | ((z >>> 31) << 20);
		return lx | (lz << 21) | (ly << 42);
	}
	
	private Path getPath(PathNode goalNode) {
		Path path = new Path();
		PathNode current = goalNode;
		while(current != null) {
			path.addNode(current);
			current = current.getPrevNode();
		}
		path.reverse();
		return path;
	}
	
	private PathNode getLowestCostNode() {
		int size = tree.getSize();
		float bestCost = Float.MAX_VALUE;
		int bestIndex = -1;
		for(int i = 0; i < size; ++i) {
			PathNode node = tree.getNode(i);
			if(node.getTotalCost() < bestCost) {
				bestIndex = i;
				bestCost = node.getTotalCost();
			}
		}
		if(bestIndex < 0)
			return null;
		return tree.removeNode(bestIndex);
	}
	
}
