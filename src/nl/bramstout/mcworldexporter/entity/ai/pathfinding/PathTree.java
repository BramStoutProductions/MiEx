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
