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
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Very simple map that uses a String as the key.
 * Internally, it converts each key into a token (an integer id)
 * and uses that as the actual key.
 * It assumes that only a few keys will exist, so
 * it just stores everything in flat arrays.
 */
public class TokenMap<Value> implements Iterable<Entry<String, Value>>{

	private int[] keys;
	private Object[] values;
	private int size;
	
	public TokenMap() {
		this(4);
	}
	
	public TokenMap(int initialCapacity) {
		keys = new int[initialCapacity];
		values = new Object[initialCapacity];
		size = 0;
	}
	
	public void clear() {
		size = 0;
	}
	
	public boolean containsKey(int keyId) {
		for(int i = 0; i < size; ++i) {
			if(keys[i] == keyId)
				return true;
		}
		return false;
	}
	
	public boolean containsKey(String key) {
		return containsKey(_getKeyId(key));
	}
	
	public void put(int keyId, Value value) {
		for(int i = 0; i < size; ++i) {
			if(keys[i] == keyId) {
				values[i] = value;
				return;
			}
		}
		// Need to add it to the list.
		if(size == keys.length) {
			// Extend capacity.
			keys = Arrays.copyOf(keys, keys.length * 2);
			values = Arrays.copyOf(values, values.length * 2);
		}
		keys[size] = keyId;
		values[size] = value;
		size++;
	}
	
	public void put(String key, Value value) {
		put(_getKeyId(key), value);
	}
	
	public Value get(int keyId) {
		return getOrDefault(keyId, null);
	}
	
	public Value get(String key) {
		return get(_getKeyId(key));
	}
	
	@SuppressWarnings("unchecked")
	public Value getOrDefault(int keyId, Value defaultValue) {
		for(int i = 0; i < size; ++i) {
			if(keys[i] == keyId)
				return (Value) values[i];
		}
		return defaultValue;
	}
	
	public Value getOrDefault(String key, Value defaultValue) {
		return getOrDefault(_getKeyId(key), defaultValue);
	}
	
	public int getKeyId(int index) {
		return keys[index];
	}
	
	public String getKey(int index) {
		return _getKey(keys[index]);
	}
	
	@SuppressWarnings("unchecked")
	public Value getValue(int index) {
		return (Value) values[index];
	}
	
	public int size() {
		return size;
	}
	
	
	private static String[] KEY_REGISTRY = new String[8];
	private static int KEY_REGISTRY_SIZE = 0;
	
	public static int _getKeyId(String key) {
		// Quick search without mutex.
		// Hits most of the times and it's much faster
		// to not need to acquire the mutex.
		for(int i = 0; i < KEY_REGISTRY_SIZE; ++i) {
			if(KEY_REGISTRY[i].equals(key)) {
				return i;
			}
		}
		synchronized(KEY_REGISTRY) {
			// Search again with mutex.
			for(int i = 0; i < KEY_REGISTRY_SIZE; ++i) {
				if(KEY_REGISTRY[i].equals(key)) {
					return i;
				}
			}
			// Not in the registry, so add it.
			if(KEY_REGISTRY_SIZE == KEY_REGISTRY.length) {
				// Expand capacity
				KEY_REGISTRY = Arrays.copyOf(KEY_REGISTRY, KEY_REGISTRY.length * 2);
			}
			KEY_REGISTRY[KEY_REGISTRY_SIZE] = key;
			KEY_REGISTRY_SIZE++;
			return KEY_REGISTRY_SIZE - 1;
		}
	}
	
	public static String _getKey(int keyId) {
		if(keyId < 0 || keyId >= KEY_REGISTRY_SIZE)
			return "";
		return KEY_REGISTRY[keyId];
	}
	
	private static class TokenMapEntry<T> implements Entry<String, T>{

		private String key;
		private T value;
		
		public TokenMapEntry(String key, T value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public T setValue(T value) {
			return value;
		}
		
	}
	
	private static class TokenMapIterator<T> implements Iterator<Entry<String, T>>{

		private TokenMap<T> map;
		private int index;
		
		public TokenMapIterator(TokenMap<T> map) {
			this.map = map;
			this.index = 0;
		}
		
		@Override
		public boolean hasNext() {
			return index < map.size;
		}

		@Override
		public Entry<String, T> next() {
			Entry<String, T> value = new TokenMapEntry<T>(map.getKey(index), map.getValue(index));
			index++;
			return value;
		}
		
	}

	@Override
	public Iterator<Entry<String, Value>> iterator() {
		return new TokenMapIterator<Value>(this);
	}
	
}
