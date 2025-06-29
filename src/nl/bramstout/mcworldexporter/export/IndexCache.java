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

package nl.bramstout.mcworldexporter.export;

import java.util.Arrays;

public class IndexCache {
	
	private static class LeafNode{
		
		long[] keys;
		int[] values;
		int size;
		int maxSize;
		
		public LeafNode(int initialCapacity, int maxSize) {
			keys = new long[initialCapacity];
			values = new int[initialCapacity];
			size = 0;
			this.maxSize = maxSize;
		}
		
		private int getIndex(long key, boolean getInsertIndex) {
			int left = 0;
			int right = size - 1;
			int middle = 0;
			int index = -1;
			while(left <= right) {
				middle = (left + right) >>> 1;
				if(keys[middle] < key)
					left = middle + 1;
				else if(keys[middle] > key)
					right = middle - 1;
				else {
					index = middle;
					break;
				}
			}
			if(getInsertIndex) {
				index = middle;
				if(size > 0 && key > keys[middle])
					index++;
			}
			return index;
		}
		
		public int getOrDefault(long key, int defaultValue) {
			int index = getIndex(key, false);
			if(index < 0)
				return defaultValue;
			return values[index];
		}
		
		public boolean put(long key, int value) {
			int index = getIndex(key, true);
			
			if(index < size && keys[index] == key) {
				// Matches an already existing item, so update it.
				values[index] = value;
				return true;
			}
			if(size == maxSize)
				return false;
			
			// We need to insert it.
			if(size == keys.length) {
				// We are at capacity, so increase the size.
				keys = Arrays.copyOf(keys, keys.length * 2);
				values = Arrays.copyOf(values, values.length * 2);
			}
			
			// First move over all keys.
			for(int i = size; i > index; i--) {
				keys[i] = keys[i-1];
				values[i] = values[i-1];
			}
			keys[index] = key;
			values[index] = value;
			size++;
			return true;
		}
		
		public void split(LeafNode otherNode) {
			int origSize = size;
			size = size / 2;
			otherNode.size = origSize - size;
			if(otherNode.size > otherNode.keys.length) {
				otherNode.keys = new long[otherNode.size];
				otherNode.values = new int[otherNode.size];
			}
			for(int i = 0; i < otherNode.size; ++i) {
				otherNode.keys[i] = keys[i + size];
				otherNode.values[i] = values[i + size];
			}
		}
		
	}
	
	private LeafNode[] leafNodes;
	private long[] keys;
	private int numNodes;
	private int maxSize;
	private int initialCapacity;
	
	public IndexCache() {
		maxSize = 128;
		initialCapacity = 32;
		leafNodes = new LeafNode[16];
		leafNodes[0] = new LeafNode(initialCapacity, maxSize);
		keys = new long[16];
		keys[0] = 0;
		numNodes = 1;
	}
	
	private int getIndex(long key) {
		int left = 0;
		int right = numNodes - 1;
		int middle = 0;
		while(left <= right) {
			middle = (left + right) >>> 1;
			if(keys[middle] < key)
				left = middle + 1;
			else if(keys[middle] > key)
				right = middle - 1;
			else
				break;
		}
		if(keys[middle] > key && middle > 0)
			middle -= 1;
		return middle;
	}
	
	public int getOrDefault(long key, int defaultValue) {
		int index = getIndex(key);
		return leafNodes[index].getOrDefault(key, defaultValue);
	}
	
	public void put(long key, int value) {
		int index = getIndex(key);
		boolean success = leafNodes[index].put(key, value);
		if(!success) {
			// Leaf node has hit max size, so split it.
			if(numNodes == leafNodes.length) {
				// Hit max capacity, so increase that.
				leafNodes = Arrays.copyOf(leafNodes, leafNodes.length * 2);
				keys = Arrays.copyOf(keys, keys.length * 2);
				maxSize = (maxSize * 3) / 2;
				for(int i = 0; i < numNodes; ++i)
					leafNodes[i].maxSize = maxSize;
			}
			
			// Move over the elements to make space.
			for(int i = numNodes-1; i > index; --i) {
				leafNodes[i+1] = leafNodes[i];
				keys[i+1] = keys[i];
			}
			leafNodes[index+1] = new LeafNode(maxSize, maxSize);
			leafNodes[index].split(leafNodes[index+1]);
			keys[index] = leafNodes[index].keys[0];
			keys[index+1] = leafNodes[index+1].keys[0];
			numNodes++;
		}else {
			keys[index] = leafNodes[index].keys[0];
		}
	}
	
	public void clear() {
		maxSize = 128;
		initialCapacity = 32;
		leafNodes = new LeafNode[16];
		leafNodes[0] = new LeafNode(initialCapacity, maxSize);
		keys = new long[16];
		keys[0] = 0;
		numNodes = 1;
	}
	
}
