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

public class IndexCacheDoubleLong {
	
	private static class LeafNode{
		
		long[] keys1;
		long[] keys2;
		int[] values;
		int size;
		int maxSize;
		
		public LeafNode(int initialCapacity, int maxSize) {
			keys1 = new long[initialCapacity];
			keys2 = new long[initialCapacity];
			values = new int[initialCapacity];
			size = 0;
			this.maxSize = maxSize;
		}
		
		private int getIndex(long key1, long key2, boolean getInsertIndex) {
			int left = 0;
			int right = size - 1;
			int middle = 0;
			int index = -1;
			while(left <= right) {
				middle = (left + right) >>> 1;
				if(keys1[middle] < key1 || (keys1[middle] == key1 && keys2[middle] < key2))
					left = middle + 1;
				else if(keys1[middle] > key1 || (keys1[middle] == key1 && keys2[middle] > key2))
					right = middle - 1;
				else {
					index = middle;
					break;
				}
			}
			if(getInsertIndex) {
				index = middle;
				if(size > 0 && (key1 > keys1[middle] || (key1 == keys1[middle] && key2 > keys2[middle])))
					index++;
			}
			return index;
		}
		
		public int getOrDefault(long key1, long key2, int defaultValue) {
			int index = getIndex(key1, key2, false);
			if(index < 0)
				return defaultValue;
			return values[index];
		}
		
		public boolean put(long key1, long key2, int value) {
			int index = getIndex(key1, key2, true);
			
			if(index < size && keys1[index] == key1 && keys2[index] == key2) {
				// Matches an already existing item, so update it.
				values[index] = value;
				return true;
			}
			if(size == maxSize)
				return false;
			
			// We need to insert it.
			if(size == keys1.length) {
				// We are at capacity, so increase the size.
				keys1 = Arrays.copyOf(keys1, keys1.length * 2);
				keys2 = Arrays.copyOf(keys2, keys2.length * 2);
				values = Arrays.copyOf(values, values.length * 2);
			}
			
			// First move over all keys.
			for(int i = size; i > index; i--) {
				keys1[i] = keys1[i-1];
				keys2[i] = keys2[i-1];
				values[i] = values[i-1];
			}
			keys1[index] = key1;
			keys2[index] = key2;
			values[index] = value;
			size++;
			return true;
		}
		
		public void split(LeafNode otherNode) {
			int origSize = size;
			size = size / 2;
			otherNode.size = origSize - size;
			if(otherNode.size > otherNode.keys1.length) {
				otherNode.keys1 = new long[otherNode.size];
				otherNode.keys2 = new long[otherNode.size];
				otherNode.values = new int[otherNode.size];
			}
			for(int i = 0; i < otherNode.size; ++i) {
				otherNode.keys1[i] = keys1[i + size];
				otherNode.keys2[i] = keys2[i + size];
				otherNode.values[i] = values[i + size];
			}
		}
		
	}
	
	private LeafNode[] leafNodes;
	private long[] keys1;
	private long[] keys2;
	private int numNodes;
	private int maxSize;
	private int initialCapacity;
	
	public IndexCacheDoubleLong() {
		maxSize = 128;
		initialCapacity = 32;
		leafNodes = new LeafNode[16];
		leafNodes[0] = new LeafNode(initialCapacity, maxSize);
		keys1 = new long[16];
		keys1[0] = 0;
		keys2 = new long[16];
		keys2[0] = 0;
		numNodes = 1;
	}
	
	private int getIndex(long key1, long key2) {
		int left = 0;
		int right = numNodes - 1;
		int middle = 0;
		while(left <= right) {
			middle = (left + right) >>> 1;
			if(keys1[middle] < key1 || (keys1[middle] == key1 && keys2[middle] < key2))
				left = middle + 1;
			else if(keys1[middle] > key1 || (keys1[middle] == key1 && keys2[middle] > key2))
				right = middle - 1;
			else
				break;
		}
		if((keys1[middle] > key1 || (keys1[middle] == key1 && keys2[middle] > key2)) && middle > 0)
			middle -= 1;
		return middle;
	}
	
	public int getOrDefault(long key1, long key2, int defaultValue) {
		int index = getIndex(key1, key2);
		return leafNodes[index].getOrDefault(key1, key2, defaultValue);
	}
	
	public void put(long key1, long key2, int value) {
		int index = getIndex(key1, key2);
		boolean success = leafNodes[index].put(key1, key2, value);
		if(!success) {
			// Leaf node has hit max size, so split it.
			if(numNodes == leafNodes.length) {
				// Hit max capacity, so increase that.
				leafNodes = Arrays.copyOf(leafNodes, leafNodes.length * 2);
				keys1 = Arrays.copyOf(keys1, keys1.length * 2);
				keys2 = Arrays.copyOf(keys2, keys2.length * 2);
				maxSize = (maxSize * 3) / 2;
				for(int i = 0; i < numNodes; ++i)
					leafNodes[i].maxSize = maxSize;
			}
			
			// Move over the elements to make space.
			for(int i = numNodes-1; i > index; --i) {
				leafNodes[i+1] = leafNodes[i];
				keys1[i+1] = keys1[i];
				keys2[i+1] = keys2[i];
			}
			leafNodes[index+1] = new LeafNode(maxSize, maxSize);
			leafNodes[index].split(leafNodes[index+1]);
			keys1[index] = leafNodes[index].keys1[0];
			keys2[index] = leafNodes[index].keys2[0];
			keys1[index+1] = leafNodes[index+1].keys1[0];
			keys2[index+1] = leafNodes[index+1].keys2[0];
			numNodes++;
		}else {
			keys1[index] = leafNodes[index].keys1[0];
			keys2[index] = leafNodes[index].keys2[0];
		}
	}
	
	public void clear() {
		maxSize = 128;
		initialCapacity = 32;
		leafNodes = new LeafNode[16];
		leafNodes[0] = new LeafNode(initialCapacity, maxSize);
		keys1 = new long[16];
		keys1[0] = 0;
		keys2 = new long[16];
		keys2[0] = 0;
		numNodes = 1;
	}
	
}
