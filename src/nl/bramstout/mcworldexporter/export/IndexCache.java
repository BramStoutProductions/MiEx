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
		Node child00;
		Node child01;
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
		long key9;
		int value9;
		long key10;
		int value10;
		long key11;
		int value11;
		long key12;
		int value12;
		long key13;
		int value13;
		long key14;
		int value14;
		long key15;
		int value15;
		long key16;
		int value16;
		boolean leafNode;
		
		public Node() {
			startKey = 0;
			keyLength = 1l << 63;
			child00 = null;
			child01 = null;
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
			key9 = -1;
			value9 = -1;
			key10 = -1;
			value10 = -1;
			key11 = -1;
			value11 = -1;
			key12 = -1;
			value12 = -1;
			key13 = -1;
			value13 = -1;
			key14 = -1;
			value14 = -1;
			key15 = -1;
			value15 = -1;
			key16 = -1;
			value16 = -1;
			leafNode = true;
		}
		
		public int getOrDefault(long key, int defaultValue) {
			if(!leafNode) {
				// This is an intermediate node.
				// Check if it's in the left or right child.
				if(((key - startKey) & keyLength) == 0) {
					if(child00 == null)
						return defaultValue;
					return child00.getOrDefault(key, defaultValue);
				}else {
					if(child01 == null)
						return defaultValue;
					return child01.getOrDefault(key, defaultValue);
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
				else if(key == key9)
					return value9;
				else if(key == key10)
					return value10;
				else if(key == key11)
					return value11;
				else if(key == key12)
					return value12;
				else if(key == key13)
					return value13;
				else if(key == key14)
					return value14;
				else if(key == key15)
					return value15;
				else if(key == key16)
					return value16;
				else
					return defaultValue;
			}
		}
		
		public void put(long key, int value) {
			if(!leafNode) {
				// This is an intermediate node.
				// Check if it's in the left or right child.
				if(((key - startKey) & keyLength) == 0) {
					if(child00 == null) {
						long childKeyLength = keyLength >>> 1;
						child00 = new Node();
						child00.startKey = startKey;
						child00.keyLength = childKeyLength;
					}
					child00.put(key, value);
				}else {
					if(child01 == null) {
						long childKeyLength = keyLength >>> 1;
						child01 = new Node();
						child01.startKey = startKey + keyLength;
						child01.keyLength = childKeyLength;
					}
					child01.put(key, value);
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
				}else if(key == key9 || key9 == -1) {
					key9 = key;
					value9 = value;
				}else if(key == key10 || key10 == -1) {
					key10 = key;
					value10 = value;
				}else if(key == key11 || key11 == -1) {
					key11 = key;
					value11 = value;
				}else if(key == key12 || key12 == -1) {
					key12 = key;
					value12 = value;
				}else if(key == key13 || key13 == -1) {
					key13 = key;
					value13 = value;
				}else if(key == key14 || key14 == -1) {
					key14 = key;
					value14 = value;
				}else if(key == key15 || key15 == -1) {
					key15 = key;
					value15 = value;
				}else if(key == key16 || key16 == -1) {
					key16 = key;
					value16 = value;
				}else {
					// We've reached the end
					// which means that we need to
					// split this node up.
					leafNode = false;
					
					put(key1, value1);
					put(key2, value2);
					put(key3, value3);
					put(key4, value4);
					put(key5, value5);
					put(key6, value6);
					put(key7, value7);
					put(key8, value8);
					put(key9, value9);
					put(key10, value10);
					put(key11, value11);
					put(key12, value12);
					put(key13, value13);
					put(key14, value14);
					put(key15, value15);
					put(key16, value16);
					put(key, value);
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
