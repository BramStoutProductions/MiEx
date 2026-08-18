package nl.bramstout.mcworldexporter.export;

import java.util.Arrays;

public class IndexCacheFlat {
	
	private long[] keys;
	private int[] values;
	private int size;
	
	public IndexCacheFlat(int capacity) {
		keys = new long[capacity];
		values = new int[capacity];
		size = 0;
	}
	
	public void reset(int capacity) {
		if(capacity > keys.length) {
			keys = new long[capacity];
			values = new int[capacity];
		}
		size = 0;
	}
	
	public void clear() {
		size = 0;
	}
	
	private int getIndex(long key) {
		int left = 0;
		int right = size - 1;
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
		if(keys[middle] < key && size > 0)
			middle += 1;
		return middle;
	}
	
	/**
	 * If a value for the key exists, return that.
	 * Otherwise, insert the given value and return that.
	 */
	public int getOrInsert(long key, int value) {
		int index = getIndex(key);
		if(index < size && keys[index] == key)
			// We have a hit.
			return values[index];
		// We have a miss, so add it.
		if(index >= keys.length) {
			// Increase capacity.
			keys = Arrays.copyOf(keys, keys.length * 2);
			values = Arrays.copyOf(values, values.length * 2);
		}
		// First make space.
		if(index <= size) {
			System.arraycopy(keys, index, keys, index + 1, size - index);
			System.arraycopy(values, index, values, index + 1, size - index);
		}
		keys[index] = key;
		values[index] = value;
		size++;
		return value;
	}
	
	public int getOrDefault(long key, int defaultValue) {
		int index = getIndex(key);
		if(index < size && keys[index] == key)
			// We have a hit.
			return values[index];
		return defaultValue;
	}
	
	public void put(long key, int value) {
		int index = getIndex(key);
		if(index < size && keys[index] == key) {
			// We have a hit.
			values[index] = value;
			return;
		}
		// We have a miss, so add it.
		if(size >= keys.length) {
			// Increase capacity.
			keys = Arrays.copyOf(keys, keys.length * 2);
			values = Arrays.copyOf(values, values.length * 2);
		}
		// First make space.
		if(index < size) {
			System.arraycopy(keys, index, keys, index + 1, size - index);
			System.arraycopy(values, index, values, index + 1, size - index);
		}
		keys[index] = key;
		values[index] = value;
		size++;
	}
	
}
