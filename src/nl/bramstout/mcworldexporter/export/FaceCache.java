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

public class FaceCache {
	
	private static class Page{
		
		private static final int SUB_PAGES_BITS = 6;
		private static final int SUB_PAGES = 1 << SUB_PAGES_BITS;
		private static final int MAX_KEYS = 1024;
		private static final int MAX_DEPTH = 8;
		
		private Page[] subPages;
		private long[] key1s;
		private long[] key2s;
		private int numKeys;
		private int depth;
		
		public Page(int depth) {
			subPages = null;
			key1s = new long[16];
			key2s = new long[16];
			numKeys = 0;
			this.depth = depth;
		}
		
		public void clear() {
			subPages = null;
			if(key1s == null)
				key1s = new long[16];
			if(key2s == null)
				key2s = new long[16];
			numKeys = 0;
		}
		
		private int getIndex(long key1) {
			int left = 0;
			int right = numKeys - 1;
			int middle = 0;
			while(left <= right) {
				middle = (left + right) >>> 1;
				if(key1s[middle] < key1)
					left = middle + 1;
				else if(key1s[middle] > key1)
					right = middle - 1;
				else {
					break;
				}
			}
			return middle;
		}
		
		public boolean register(long key1, long key2) {
			if(subPages != null) {
				int index = (int) (key1 & (SUB_PAGES - 1));
				if(subPages[index] == null) {
					subPages[index] = new Page(depth + 1);
				}
				return subPages[index].register(key1 >>> SUB_PAGES_BITS, key2);
			}
			
			// Get the index where key1 should be.
			int index = getIndex(key1);
			// We could have multiple key1s but with different key2s.
			// so we loop backwards and forwards to look through all
			// key1s that match our key1 to check all key2s to find a match.
			int index2 = index;
			if(numKeys > 0) {
				while(index2 >= 0 && key1s[index2] == key1) {
					if(key2s[index2] == key2) {
						// We got a match, so return true.
						return true;
					}
					// No match yet, so check the index before
					index2--;
				}
			}
			int minIndex = Math.max(index2, 0);
			index2 = index + 1;
			while(index2 < numKeys && key1s[index2] == key1) {
				if(key2s[index2] == key2) {
					// We got a match so return true.
					return true;
				}
				index2++;
			}
			int maxIndex = Math.min(index2, numKeys - 1);
			
			
			// We didn't match anything, so insert it.
			
			if(numKeys == MAX_KEYS && depth < MAX_DEPTH) {
				// We've reached the maximum number of keys, so
				// split it up into pages.
				subPages = new Page[SUB_PAGES];
				for(int i = 0; i < numKeys; ++i) {
					// Because we've initialised subPages,
					// it'll now start putting the keys into
					// the sub pages.
					register(key1s[i], key2s[i]);
				}
				key1s = null;
				key2s = null;
				numKeys = 0;
				// Don't forget to also redo the current key
				return register(key1, key2);
			}
			
			// Let's insert it into out list.
			if(numKeys >= key1s.length) {
				// We first need to extend our lists.
				key1s = Arrays.copyOf(key1s, key1s.length * 2);
				key2s = Arrays.copyOf(key2s, key2s.length * 2);
			}
			
			// First make sure that we get the right insert index.
			if(numKeys > 0) {
				if(key1 < key1s[index]) {
					// The key is lower than the index, so we need to
					// insert to the left of it.
					index = minIndex;
					if(key1 > key1s[index]) {
						// The key is higher than minIndex, so we need
						// to insert just to the right of it.
						index = index + 1;
					}
				}else if(key1 > key1s[index]) {
					// The key is higher than the index, so we need to
					// insert to the right of it.
					index = maxIndex;
					if(key1 > key1s[index]) {
						// The key is still higher than the index, so
						// we need to insert just to the right of it.
						index = index + 1;
					}
				}
			}
			
			// The index is now set properly, so let's move all values
			// over to make room
			for(int i = numKeys; i > index; i--) {
				key1s[i] = key1s[i-1];
				key2s[i] = key2s[i-1];
			}
			// Now that we've made room, we can add it to the list.
			key1s[index] = key1;
			key2s[index] = key2;
			
			// Return that we needed to insert it.
			return false;
		}
	}
	
	private Page page;
	public FaceCache() {
		this.page = new Page(0);
	}
	
	public void clear() {
		page.clear();
	}
	
	/**
	 * Checks if a face with these four vertex indices
	 * already exists. If so, it returns true. If not,
	 * it registers the face and returns false.
	 * 
	 * This allows you to make sure that there aren't
	 * faces who end up being the exact same.
	 * @param v0
	 * @param v1
	 * @param v2
	 * @param v3
	 * @return
	 */
	public boolean register(int v0, int v1, int v2, int v3) {
		// Since order doesn't matter, we are going to sort
		// the four values.
		int minV = Math.min(Math.min(Math.min(v0, v1), v2), v3);
		int tmpV = v0;
		if(minV == v1) {
			v0 = v1;
			v1 = tmpV;
		}else if(minV == v2) {
			v0 = v2;
			v2 = tmpV;
		}else if(minV == v3) {
			v0 = v3;
			v3 = tmpV;
		}
		minV = Math.min(Math.min(v1, v2), v3);
		tmpV = v1;
		if(minV == v2) {
			v1 = v2;
			v2 = tmpV;
		}else if(minV == v3) {
			v1 = v3;
			v3 = tmpV;
		}
		minV = Math.min(v2, v3);
		tmpV = v2;
		if(minV == v3) {
			v2 = v3;
			v3 = tmpV;
		}
		
		// Combine the indices into two 64 bit integers
		long lv0 = (long) v0;
		long lv1 = (long) v1;
		long lv2 = (long) v2;
		long lv3 = (long) v3;
		
		long key1 = (spaceOutBits(lv0) << 1) | spaceOutBits(lv1);
		long key2 = (spaceOutBits(lv2) << 1) | spaceOutBits(lv3);
		
		return page.register(key1, key2);
	}
	
	private long spaceOutBits(long v) {
		// From https://stackoverflow.com/a/58980803 
		v = v & 0xFFFFFFFFL;
		v = (v | (v << 16)) & 0x0000FFFF0000FFFFL;
        v = (v | (v <<  8)) & 0x00FF00FF00FF00FFL;
        v = (v | (v <<  4)) & 0x0F0F0F0F0F0F0F0FL;
        v = (v | (v <<  2)) & 0x3333333333333333L;
        v = (v | (v <<  1)) & 0x5555555555555555L;
        return v;
	}
	
}
