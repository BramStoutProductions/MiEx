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
	
	private static class Node{
		
		private static final int maxSize = 256;
		private static final int maxDepth = 24;
		
		private int[] indices;
		private long[] hashes;
		private int hashesSize;
		private Node node0;
		private Node node1;
		private Node node2;
		private Node node3;
		private long splitHash01;
		private long splitHash12;
		private long splitHash23;
		private int depth;
		
		public Node(int initialCapacity, int depth) {
			indices = new int[initialCapacity];
			hashes = new long[initialCapacity];
			hashesSize = 0;
			node0 = null;
			node1 = null;
			node2 = null;
			node3 = null;
			splitHash01 = 0;
			splitHash12 = 0;
			splitHash23 = 0;
			this.depth = depth;
		}
		
		public void clear() {
			if(indices == null)
				indices = new int[64];
			if(hashes == null)
				hashes = new long[64];
			hashesSize = 0;
			node0 = null;
			node1 = null;
			node2 = null;
			node3 = null;
			splitHash01 = 0;
			splitHash12 = 0;
			splitHash23 = 0;
		}
		
		private int getIndex(long hash) {
			int left = 0;
			int right = hashesSize - 1;
			int middle = 0;
			int index = -1;
			while(left <= right) {
				middle = (left + right) >>> 1;
				if(hashes[middle] < hash)
					left = middle + 1;
				else if(hashes[middle] > hash)
					right = middle - 1;
				else {
					index = middle;
					break;
				}
			}
			return index;
		}
		
		private int getInsertIndex(long hash) {
			int left = 0;
			int right = hashesSize - 1;
			int middle = 0;
			while(left <= right) {
				middle = (left + right) >>> 1;
				if(hashes[middle] < hash)
					left = middle + 1;
				else if(hashes[middle] > hash)
					right = middle - 1;
				else {
					break;
				}
			}
			if(hashesSize > 0 && hash > hashes[middle])
				middle++;
			return middle;
		}
		
		public int getOrDefault(long hash, int defaultValue) {
			if(node0 != null) {
				if(hash < splitHash01)
					return node0.getOrDefault(hash, defaultValue);
				else if(hash < splitHash12)
					return node1.getOrDefault(hash, defaultValue);
				else if(hash < splitHash23)
					return node2.getOrDefault(hash, defaultValue);
				else
					return node3.getOrDefault(hash, defaultValue);
			}
			// This is a leaf node, so do a binary search through hashes
			int index = getIndex(hash);
			if(index < 0)
				return defaultValue;
			
			return indices[index];
		}
		
		public void put(long hash, int index) {
			if(node0 == null && hashesSize >= maxSize && depth < maxDepth) {
				split();
			}
			if(node0 != null) {
				if(hash < splitHash01)
					node0.put(hash, index);
				else if(hash < splitHash12)
					node1.put(hash, index);
				else if(hash < splitHash23)
					node2.put(hash, index);
				else
					node3.put(hash, index);
				return;
			}
			
			int insertIndex = getInsertIndex(hash);
			if(insertIndex < hashesSize) {
				if(hashes[insertIndex] == hash) {
					// Add it to the existing hash
					indices[insertIndex] = index;
					return;
				}
			}
			// We need to insert it
			if(hashesSize == hashes.length) {
				// We're already at capacity, so increase our lists
				indices = Arrays.copyOf(indices, indices.length * 2);
				hashes = Arrays.copyOf(hashes, hashes.length * 2);
			}
			
			// First move all values insertIndex and after up.
			for(int i = hashesSize - 1; i >= insertIndex; --i) {
				indices[i + 1] = indices[i];
				hashes[i + 1] = hashes[i];
			}
			
			// Now we insert out new values
			hashes[insertIndex] = hash;
			indices[insertIndex] = index;
			hashesSize++;
		}
		
		private void split() {
			int middleIndex = hashesSize >>> 1;
			int middleLeftIndex = hashesSize >>> 2;
			int middleRightIndex = middleIndex + middleLeftIndex;
			node0 = new Node(middleLeftIndex, depth + 1);
			node1 = new Node(middleIndex - middleLeftIndex, depth + 1);
			node2 = new Node(middleRightIndex - middleIndex, depth + 1);
			node3 = new Node(hashesSize - middleRightIndex, depth + 1);
			splitHash01 = hashes[middleLeftIndex];
			splitHash12 = hashes[middleIndex];
			splitHash23 = hashes[middleRightIndex];
			for(int i = 0; i < middleLeftIndex; ++i) {
				node0.hashes[i] = hashes[i];
				node0.indices[i] = indices[i];
				node0.hashesSize = middleLeftIndex;
			}
			for(int i = middleLeftIndex; i < middleIndex; ++i) {
				node1.hashes[i - middleLeftIndex] = hashes[i];
				node1.indices[i - middleLeftIndex] = indices[i];
				node1.hashesSize = middleIndex - middleLeftIndex;
			}
			for(int i = middleIndex; i < middleRightIndex; ++i) {
				node2.hashes[i - middleIndex] = hashes[i];
				node2.indices[i - middleIndex] = indices[i];
				node2.hashesSize = middleRightIndex - middleIndex;
			}
			for(int i = middleRightIndex; i < hashesSize; ++i) {
				node3.hashes[i - middleRightIndex] = hashes[i];
				node3.indices[i - middleRightIndex] = indices[i];
				node3.hashesSize = hashesSize - middleRightIndex;
			}
			hashes = null;
			indices = null;
			hashesSize = 0;
		}
		
	}
	
	private Node rootNode;
	
	public IndexCache() {
		rootNode = new Node(64, 0);
	}
	
	public int getOrDefault(long key, int defaultValue) {
		return rootNode.getOrDefault(key, defaultValue);
	}
	
	public void put(long key, int value) {
		rootNode.put(key, value);
	}
	
	public void clear() {
		rootNode.clear();
	}
	
}
