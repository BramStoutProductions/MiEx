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

public class Path {
	
	private PathNode[] nodes;
	private int size;
	
	public Path() {
		nodes = new PathNode[16];
		size = 0;
	}
	
	public void reverse() {
		int halfSize = size >> 1;
		int offset = size - halfSize;
		PathNode tmp = null;
		for(int i = 0; i < halfSize; ++i) {
			// Swap
			tmp = nodes[i];
			nodes[i] = nodes[i + offset];
			nodes[i + offset] = tmp;
		}
	}
	
	public int getSize() {
		return size;
	}
	
	public PathNode getNode(int index) {
		if(index < 0 || index >= size)
			throw new ArrayIndexOutOfBoundsException();
		return nodes[index];
	}
	
	private void reserveSpace() {
		if(nodes.length > (size + 1))
			return;
		nodes = Arrays.copyOf(nodes, nodes.length * 2);
	}
	
	public void addNode(PathNode node) {
		reserveSpace();
		nodes[size] = node;
		size++;
	}
	
	public PathNode popNode() {
		if(size <= 0)
			throw new ArrayIndexOutOfBoundsException("Array already empty");
		size--;
		return nodes[size];
	}
	
	public void popNodes(int numNodes) {
		numNodes = Math.min(numNodes, size);
		size -= numNodes;
	}
	
	public PathNode peek() {
		if(size <= 0)
			throw new ArrayIndexOutOfBoundsException();
		return nodes[size-1];
	}
	
	public int getClosestNode(float x, float y, float z) {
		if(size <= 0)
			return -1;
		x -= 0.5f;
		z -= 0.5f;
		int closestIndex = 0;
		float closestDistance = Float.MAX_VALUE;
		for(int i = 0; i < size; ++i) {
			float distance = (nodes[i].getX() - x) * (nodes[i].getX() - x) + 
								(nodes[i].getY() - y) * (nodes[i].getY() - y) + 
								(nodes[i].getZ() - z) * (nodes[i].getZ() - z);
			if(distance < closestDistance) {
				closestIndex = i;
				closestDistance = distance;
			}
		}
		return closestIndex;
	}

}
