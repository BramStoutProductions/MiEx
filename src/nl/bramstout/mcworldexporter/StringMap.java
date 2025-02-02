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

package nl.bramstout.mcworldexporter;

import java.util.Arrays;

import nl.bramstout.mcworldexporter.parallel.ReadWriteMutex;


public class StringMap<T> {

	private static class StringMapNode<T>{
		
		private static final int maxSize = 256;
		
		private String[][] keys;
		private Object[][] values;
		private int[] hashes;
		private int hashesSize;
		private StringMapNode<T> leftNode;
		private StringMapNode<T> rightNode;
		private int splitHash;
		
		public StringMapNode(int initialCapacity) {
			keys = new String[initialCapacity][];
			values = new Object[initialCapacity][];
			hashes = new int[initialCapacity];
			hashesSize = 0;
			leftNode = null;
			rightNode = null;
			splitHash = 0;
		}
		
		public static int hash(char[] data, int length) {
			int result = 7;
			for(int i = 0; i < length; ++i) {
				result = 31 * result + data[i];
			}
			return result;
		}
		
		public static int hash(String str) {
			int result = 7;
			int length = str.length();
			for(int i = 0; i < length; ++i) {
				result = 31 * result + str.charAt(i);
			}
			return result;
		}
		
		private int getIndex(int hash) {
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
		
		private int getInsertIndex(int hash) {
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
		
		@SuppressWarnings("unchecked")
		public T getOrNull(int hash, char[] data, int length) {
			if(leftNode != null) {
				if(hash < splitHash)
					return leftNode.getOrNull(hash, data, length);
				else
					return rightNode.getOrNull(hash, data, length);
			}
			// This is a leaf node, so do a binary search through hashes
			int index = getIndex(hash);
			if(index < 0)
				return null;
			
			// Hash matches, now see if the string matches
			String[] strings2 = keys[index];
			Object[] values2 = values[index];
			int i = 0;
			boolean match = true;
			String str = null;
			for(int index2 = 0; index2 < strings2.length; ++index2) {
				str = strings2[index2];
				if(str.length() != length)
					continue;
				
				match = true;
				for(i = 0; i < length; ++i) {
					if(str.charAt(i) != data[i]) {
						match = false;
						break;
					}
				}
				if(match)
					return (T) values2[index2];
			}
			return null;
		}
		
		public void put(int hash, String str, T value) {
			if(leftNode == null && hashesSize >= maxSize) {
				split();
			}
			if(leftNode != null) {
				if(hash < splitHash)
					leftNode.put(hash, str, value);
				else
					rightNode.put(hash, str, value);
				return;
			}
			
			int insertIndex = getInsertIndex(hash);
			if(insertIndex < hashes.length) {
				if(hashes[insertIndex] == hash) {
					// Add it to the existing hash
					String[] strings2 = keys[insertIndex];
					Object[] values2 = values[insertIndex];
					String[] newStrings2 = Arrays.copyOf(strings2, strings2.length + 1);
					Object[] newValues2 = Arrays.copyOf(values2, values2.length + 1);
					newStrings2[newStrings2.length - 1] = str;
					newValues2[newValues2.length - 1] = value;
					return;
				}
			}
			// We need to insert it
			if(hashesSize == hashes.length) {
				// We're already at capacity, to increase our lists
				keys = Arrays.copyOf(keys, keys.length * 2);
				values = Arrays.copyOf(values, values.length * 2);
				hashes = Arrays.copyOf(hashes, hashes.length * 2);
			}
			
			// First move all values insertIndex and after up.
			for(int i = hashesSize - 1; i >= insertIndex; --i) {
				keys[i + 1] = keys[i];
				values[i + 1] = values[i];
				hashes[i + 1] = hashes[i];
			}
			
			// Now we insert out new values
			hashes[insertIndex] = hash;
			keys[insertIndex] = new String[] { str };
			values[insertIndex] = new Object[] { value };
			hashesSize++;
		}
		
		private void split() {
			int middleIndex = hashesSize >>> 1;
			leftNode = new StringMapNode<T>(middleIndex);
			rightNode = new StringMapNode<T>(hashesSize - middleIndex);
			splitHash = hashes[middleIndex];
			for(int i = 0; i < middleIndex; ++i) {
				leftNode.hashes[i] = hashes[i];
				leftNode.keys[i] = keys[i];
				leftNode.values[i] = values[i];
				leftNode.hashesSize = middleIndex;
			}
			for(int i = middleIndex; i < hashesSize; ++i) {
				rightNode.hashes[i - middleIndex] = hashes[i];
				rightNode.keys[i - middleIndex] = keys[i];
				rightNode.values[i - middleIndex] = values[i];
				rightNode.hashesSize = hashesSize - middleIndex;
			}
			hashes = null;
			keys = null;
			values = null;
			hashesSize = 0;
		}
		
	}
	
	private StringMapNode<T> rootNode;
	private ReadWriteMutex mutex;
	
	public StringMap() {
		rootNode = new StringMapNode<T>(16);
		mutex = new ReadWriteMutex();
	}
	
	public void clear() {
		rootNode = new StringMapNode<T>(16);
	}
	
	public T getOrNull(char[] data, int length) {
		int hash = StringMapNode.hash(data, length);
		mutex.acquireRead();
		T val = rootNode.getOrNull(hash, data, length);
		mutex.releaseRead();
		return val;
	}
	
	public void put(String key, T value) {
		int hash = StringMapNode.hash(key);
		mutex.acquireWrite();
		rootNode.put(hash, key, value);
		mutex.releaseWrite();
	}
	
}
