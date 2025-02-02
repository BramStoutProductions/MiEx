package nl.bramstout.mcworldexporter.nbt;

import java.util.Arrays;

public class StringSet {

	
	private static class StringSetNode{
		
		private static final int maxSize = 256;
		
		private String[][] strings;
		private int[] hashes;
		private int hashesSize;
		private StringSetNode leftNode;
		private StringSetNode rightNode;
		private int splitHash;
		
		public StringSetNode(int initialCapacity) {
			strings = new String[initialCapacity][];
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
		
		public String getOrNull(int hash, char[] data, int length) {
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
			String[] strings2 = strings[index];
			int i = 0;
			boolean match = true;
			for(String str : strings2) {
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
					return str;
			}
			return null;
		}
		
		public void put(int hash, String str) {
			if(leftNode == null && hashesSize >= maxSize) {
				split();
			}
			if(leftNode != null) {
				if(hash < splitHash)
					leftNode.put(hash, str);
				else
					rightNode.put(hash, str);
				return;
			}
			
			int insertIndex = getInsertIndex(hash);
			if(insertIndex < hashes.length) {
				if(hashes[insertIndex] == hash) {
					// Add it to the existing hash
					String[] strings2 = strings[insertIndex];
					String[] newStrings2 = Arrays.copyOf(strings2, strings2.length + 1);
					newStrings2[newStrings2.length - 1] = str;
					return;
				}
			}
			// We need to insert it
			if(hashesSize == hashes.length) {
				// We're already at capacity, to increase our lists
				strings = Arrays.copyOf(strings, strings.length * 2);
				hashes = Arrays.copyOf(hashes, hashes.length * 2);
			}
			
			// First move all values insertIndex and after up.
			for(int i = hashesSize - 1; i >= insertIndex; --i) {
				strings[i + 1] = strings[i];
				hashes[i + 1] = hashes[i];
			}
			
			// Now we insert out new values
			hashes[insertIndex] = hash;
			strings[insertIndex] = new String[] { str };
			hashesSize++;
		}
		
		private void split() {
			int middleIndex = hashesSize >>> 1;
			leftNode = new StringSetNode(middleIndex);
			rightNode = new StringSetNode(hashesSize - middleIndex);
			splitHash = hashes[middleIndex];
			for(int i = 0; i < middleIndex; ++i) {
				leftNode.hashes[i] = hashes[i];
				leftNode.strings[i] = strings[i];
				leftNode.hashesSize = middleIndex;
			}
			for(int i = middleIndex; i < hashesSize; ++i) {
				rightNode.hashes[i - middleIndex] = hashes[i];
				rightNode.strings[i - middleIndex] = strings[i];
				rightNode.hashesSize = hashesSize - middleIndex;
			}
			hashes = null;
			strings = null;
			hashesSize = 0;
		}
		
	}
	
	private StringSetNode rootNode;
	
	public StringSet() {
		rootNode = new StringSetNode(16);
	}
	
	public String getOrNull(char[] data, int length) {
		int hash = StringSetNode.hash(data, length);
		return rootNode.getOrNull(hash, data, length);
	}
	
	public void put(String str) {
		int hash = StringSetNode.hash(str);
		rootNode.put(hash, str);
	}
	
}
