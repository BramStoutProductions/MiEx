package nl.bramstout.mcworldexporter;

import java.util.Arrays;

public class LongKeyMap<Value> {

	private long[] keysAndValues;
	private Object[] values;
	private int capacity;
	private int size;
	
	public LongKeyMap() {
		this(4);
	}
	
	public LongKeyMap(int initialCapacity) {
		keysAndValues = new long[initialCapacity*2];
		values = new Object[initialCapacity];
		capacity = initialCapacity;
		size = 0;
	}
	
	public LongKeyMap(LongKeyMap<Value> other) {
		this(other.capacity);
		size = other.size;
		for(int i = 0; i < size; ++i) {
			keysAndValues[i] = other.keysAndValues[i];
			keysAndValues[i + capacity] = other.keysAndValues[i + other.capacity];
			values[i] = other.values[i];
		}
	}
	
	public void clear() {
		size = 0;
	}
	
	public boolean containsKey(long key) {
		for(int i = 0; i < size; ++i) {
			if(keysAndValues[i] == key)
				return true;
		}
		return false;
	}
	
	public void put(long key, long longValue, Value objValue) {
		for(int i = 0; i < size; ++i) {
			if(keysAndValues[i] == key) {
				keysAndValues[i + capacity] = longValue;
				values[i] = objValue;
				return;
			}
		}
		// Need to add it to the list.
		if(size == capacity) {
			// Extend capacity.
			long[] newKeysAndValues = new long[capacity * 2 * 2];
			for(int i = 0; i < size; ++i) {
				newKeysAndValues[i] = keysAndValues[i];
				newKeysAndValues[i + (capacity * 2)] = keysAndValues[i + capacity];
			}
			keysAndValues = newKeysAndValues;
			values = Arrays.copyOf(values, capacity * 2);
			capacity = capacity * 2;
		}
		keysAndValues[size] = key;
		keysAndValues[size + capacity] = longValue;
		values[size] = objValue;
		size++;
	}
	
	@SuppressWarnings("unchecked")
	public void putAll(LongKeyMap<Value> other) {
		for(int i = 0; i < other.size; ++i)
			put(other.keysAndValues[i], 
					other.keysAndValues[i + other.capacity], 
					(Value) other.values[i]);
	}
	
	public Value getObj(long key) {
		return getObjOrDefault(key, null);
	}
	
	@SuppressWarnings("unchecked")
	public Value getObjOrDefault(long key, Value defaultValue) {
		for(int i = 0; i < size; ++i) {
			if(keysAndValues[i] == key)
				return (Value) values[i];
		}
		return defaultValue;
	}
	
	public long getLong(long key) {
		return getLongOrDefault(key, 0);
	}
	
	public long getLongOrDefault(long key, long defaultValue) {
		for(int i = 0; i < size; ++i) {
			if(keysAndValues[i] == key)
				return keysAndValues[i + capacity];
		}
		return defaultValue;
	}
	
	public long getKey(int index) {
		return keysAndValues[index];
	}
	
	@SuppressWarnings("unchecked")
	public Value getObjValue(int index) {
		return (Value) values[index];
	}
	
	public long getLongValue(int index) {
		return keysAndValues[index + capacity];
	}
	
	public int size() {
		return size;
	}
	
	public int getIndex(long key) {
		for(int i = 0; i < size; ++i)
			if(keysAndValues[i] == key)
				return i;
		return -1;
	}
	
}
