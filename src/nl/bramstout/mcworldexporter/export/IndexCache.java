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

public class IndexCache {
	
	private static class Node{
		
		long startKey;
		long keyLength;
		long keyLength2;
		Node child00;
		Node child01;
		Node child10;
		Node child11;
		long key1;
		int value1;
		long key2;
		int value2;
		long key3;
		int value3;
		long key4;
		int value4;
		long key5;
		int value5;
		long key6;
		int value6;
		long key7;
		int value7;
		long key8;
		int value8;
		
		public Node() {
			startKey = 0;
			keyLength = 1l << 63;
			keyLength2 = 1l << 62;
			child00 = null;
			child01 = null;
			child10 = null;
			child11 = null;
			key1 = -1;
			value1 = -1;
			key2 = -1;
			value2 = -1;
			key3 = -1;
			value3 = -1;
			key4 = -1;
			value4 = -1;
			key5 = -1;
			value5 = -1;
			key6 = -1;
			value6 = -1;
			key7 = -1;
			value7 = -1;
			key8 = -1;
			value8 = -1;
		}
		
		public int getOrDefault(long key, int defaultValue) {
			if(child00 != null || child01 != null || child10 != null || child11 != null) {
				// This is an intermediate node.
				// Check if it's in the left or right child.
				if(((key - startKey) & keyLength) == 0) {
					if(((key - startKey) & keyLength2) == 0)
						return child00.getOrDefault(key, defaultValue);
					else
						return child01.getOrDefault(key, defaultValue);
				}else {
					if(((key - startKey) & keyLength2) == 0)
						return child10.getOrDefault(key, defaultValue);
					else
						return child11.getOrDefault(key, defaultValue);
				}
			}else{
				// This is a leaf node, so check the keys for a hit.
				if(key == key1)
					return value1;
				else if(key == key2)
					return value2;
				else if(key == key3)
					return value3;
				else if(key == key4)
					return value4;
				else if(key == key5)
					return value5;
				else if(key == key6)
					return value6;
				else if(key == key7)
					return value7;
				else if(key == key8)
					return value8;
				else
					return defaultValue;
			}
		}
		
		public void put(long key, int value) {
			if(child00 != null || child01 != null || child10 != null || child11 != null) {
				// This is an intermediate node.
				// Check if it's in the left or right child.
				if(((key - startKey) & keyLength) == 0) {
					if(((key - startKey) & keyLength2) == 0)
						child00.put(key, value);
					else
						child01.put(key, value);
				}else {
					if(((key - startKey) & keyLength2) == 0)
						child10.put(key, value);
					else
						child11.put(key, value);
				}
			}else {
				// This is a leaf node.
				// If they key matches or it's empty,
				// put it in that slot.
				if(key == key1 || key1 == -1) {
					key1 = key;
					value1 = value;
				}else if(key == key2 || key2 == -1) {
					key2 = key;
					value2 = value;
				}else if(key == key3 || key3 == -1) {
					key3 = key;
					value3 = value;
				}else if(key == key4 || key4 == -1) {
					key4 = key;
					value4 = value;
				}else if(key == key5 || key5 == -1) {
					key5 = key;
					value5 = value;
				}else if(key == key6 || key6 == -1) {
					key6 = key;
					value6 = value;
				}else if(key == key7 || key7 == -1) {
					key7 = key;
					value7 = value;
				}else if(key == key8 || key8 == -1) {
					key8 = key;
					value8 = value;
				}else {
					// We've reached the end
					// which means that we need to
					// split this node up.
					long childKeyLength = keyLength >>> 2;
					long childKeyLength2 = keyLength2 >>> 2;
					child00 = new Node();
					child01 = new Node();
					child10 = new Node();
					child11 = new Node();
					child00.startKey = startKey;
					child00.keyLength = childKeyLength;
					child00.keyLength2 = childKeyLength2;
					child01.startKey = startKey + keyLength2;
					child01.keyLength = childKeyLength;
					child01.keyLength2 = childKeyLength2;
					child10.startKey = startKey + keyLength;
					child10.keyLength = childKeyLength;
					child10.keyLength2 = childKeyLength2;
					child11.startKey = startKey + keyLength + keyLength2;
					child11.keyLength = childKeyLength;
					child11.keyLength2 = childKeyLength2;
					
					put(key1, value1);
					put(key2, value2);
					put(key3, value3);
					put(key4, value4);
					put(key5, value5);
					put(key6, value6);
					put(key7, value7);
					put(key8, value8);
				}
			}
		}
	}
	
	private Node rootNode;
	
	public IndexCache() {
		rootNode = new Node();
	}
	
	public int getOrDefault(long key, int defaultValue) {
		return rootNode.getOrDefault(key, defaultValue);
	}
	
	public void put(long key, int value) {
		rootNode.put(key, value);
	}
	
}
