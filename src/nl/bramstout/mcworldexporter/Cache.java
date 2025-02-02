package nl.bramstout.mcworldexporter;

import java.util.Arrays;

import nl.bramstout.mcworldexporter.parallel.ReadWriteMutex;

public class Cache<T> {

	private static class Node<T>{
		
		private static final int maxSize = 256;
		private static final int maxDepth = 24;
		
		private T[] values;
		private long[] hashes;
		private int hashesSize;
		private Node<T> leftNode;
		private Node<T> rightNode;
		private long splitHash;
		private int depth;
		
		@SuppressWarnings("unchecked")
		public Node(int initialCapacity, int depth) {
			values = (T[]) new Object[initialCapacity];
			hashes = new long[initialCapacity];
			hashesSize = 0;
			leftNode = null;
			rightNode = null;
			splitHash = 0;
			this.depth = depth;
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
		
		public T getOrDefault(long hash, T defaultValue) {
			if(leftNode != null) {
				if(hash < splitHash)
					return leftNode.getOrDefault(hash, defaultValue);
				else
					return rightNode.getOrDefault(hash, defaultValue);
			}
			// This is a leaf node, so do a binary search through hashes
			int index = getIndex(hash);
			if(index < 0)
				return defaultValue;
			
			return values[index];
		}
		
		public void put(long hash, T value) {
			if(leftNode == null && hashesSize >= maxSize && depth < maxDepth) {
				split();
			}
			if(leftNode != null) {
				if(hash < splitHash)
					leftNode.put(hash, value);
				else
					rightNode.put(hash, value);
				return;
			}
			
			int insertIndex = getInsertIndex(hash);
			if(insertIndex < hashes.length) {
				if(hashes[insertIndex] == hash) {
					// Add it to the existing hash
					values[insertIndex] = value;
					return;
				}
			}
			// We need to insert it
			if(hashesSize == hashes.length) {
				// We're already at capacity, to increase our lists
				values = Arrays.copyOf(values, values.length * 2);
				hashes = Arrays.copyOf(hashes, hashes.length * 2);
			}
			
			// First move all values insertIndex and after up.
			for(int i = hashesSize - 1; i >= insertIndex; --i) {
				values[i + 1] = values[i];
				hashes[i + 1] = hashes[i];
			}
			
			// Now we insert out new values
			hashes[insertIndex] = hash;
			values[insertIndex] = value;
			hashesSize++;
		}
		
		private void split() {
			int middleIndex = hashesSize >>> 1;
			leftNode = new Node<T>(middleIndex, depth + 1);
			rightNode = new Node<T>(hashesSize - middleIndex, depth + 1);
			splitHash = hashes[middleIndex];
			for(int i = 0; i < middleIndex; ++i) {
				leftNode.hashes[i] = hashes[i];
				leftNode.values[i] = values[i];
				leftNode.hashesSize = middleIndex;
			}
			for(int i = middleIndex; i < hashesSize; ++i) {
				rightNode.hashes[i - middleIndex] = hashes[i];
				rightNode.values[i - middleIndex] = values[i];
				rightNode.hashesSize = hashesSize - middleIndex;
			}
			hashes = null;
			values = null;
			hashesSize = 0;
		}
		
	}
	
	private Node<T> rootNode;
	private ReadWriteMutex mutex;
	
	public Cache() {
		rootNode = new Node<T>(64, 0);
		mutex = new ReadWriteMutex();
	}
	
	public T getOrDefault(long key, T defaultValue) {
		mutex.acquireRead();
		T val = rootNode.getOrDefault(key, defaultValue);
		mutex.releaseRead();
		return val;
	}
	
	public void put(long key, T value) {
		mutex.acquireWrite();
		rootNode.put(key, value);
		mutex.releaseWrite();
	}
	
}
