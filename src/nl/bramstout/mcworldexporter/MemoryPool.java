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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple thread safe memory pool that allows us to re-use instances
 * of classes, in order to speed up allocation. It never deallocates
 * any of the instances though.
 * @param <T>
 */
public class MemoryPool<T extends Poolable> {
	
	private static class Page<T extends Poolable>{
		
		private static final int PAGE_SIZE = 1024 * 16;
		
		public T[] data = null;
		public AtomicLongArray occupancy = new AtomicLongArray(PAGE_SIZE / 64);
		public Constructor<T> typeConstructor;
		public AtomicReference<Page<T>> nextPage = new AtomicReference<Page<T>>();
		public int pageIndex = 0;
		public AtomicInteger numAllocations = new AtomicInteger();
		
		@SuppressWarnings("unchecked")
		public Page(Class<T> type) {
			try {
				this.typeConstructor = type.getConstructor();
			}catch(Exception ex) {
				throw new RuntimeException("No default constructor", ex);
			}
			data = (T[]) Array.newInstance(type, PAGE_SIZE);
		}
		
		/**
		 * Goes through the data and deallocates instances not in use.
		 */
		public void freeMemory() {
			int numAllocs = numAllocations.get();
			if(numAllocs > (PAGE_SIZE / 2))
				return; // If more than half of this page is used, skip this page.
			
			for(int i = 0; i < occupancy.length(); ++i) {
				long occ = occupancy.get(i);
				for(int j = 0; j < 64; ++j) {
					boolean occupied = (occ & (1l << j)) != 0;
					if(!occupied) {
						// If this spot isn't occupied, then just set it to null.
						// Because of Java's reference counting, if for some reason
						// the item within it still got used, it'll remain in memory
						// until that use is over.
						// The occupancy bitfield is used to prevent to items from
						// being put into the same spot, regardless of whether they
						// are stored in the data array. The data array is purely
						// to be able to re-use the instance if possible. Since we aren't
						// modifying occupancy, this function can be called at any moment
						// and won't cause any race conditions.
						data[i * 64 + j] = null;
					}
				}
			}
		}
		
		public T alloc() {
			int index = getFreeIndex();
			if(index < 0)
				return null;
			numAllocations.addAndGet(1); 
			
			T val = data[index];
			if(val == null) {
				try {
					val = typeConstructor.newInstance();
				}catch(Exception ex) {
					throw new RuntimeException("Could not create a new instance of type", ex);
				}
				data[index] = val;
			}
			val._MEMORY_POOL_PAGE_INDEX = pageIndex;
			val._MEMORY_POOL_PAGE_SUBINDEX = index;
			return val;
		}
		
		public void free(T val) {
			int occupancyIndex = val._MEMORY_POOL_PAGE_SUBINDEX / 64;
			int bitIndex = val._MEMORY_POOL_PAGE_SUBINDEX - (occupancyIndex * 64);
			// if(data[val._MEMORY_POOL_PAGE_SUBINDEX] != val)
			if(val._MEMORY_POOL_PAGE_INDEX != pageIndex)
				throw new RuntimeException("Provided instance's page index doesn't match.");
			if(data[val._MEMORY_POOL_PAGE_SUBINDEX] != val)
				throw new RuntimeException("Provided instance isn't allocated on this page.");
			val._MEMORY_POOL_PAGE_INDEX = -1;
			val._MEMORY_POOL_PAGE_SUBINDEX = -1;
			while(true) {
				long occupancyVal = occupancy.get(occupancyIndex);
				long newOccupancyVal = occupancyVal & ~(0b1L << bitIndex);
				if(occupancy.weakCompareAndSet(occupancyIndex, occupancyVal, newOccupancyVal))
					break;
			}
			numAllocations.addAndGet(-1);
		}
		
		private int getFreeIndex() {
			// Quick check to see if this memory page is already full
			int numAllocations = this.numAllocations.get();
			if(numAllocations >= PAGE_SIZE)
				return -1;
			// Since it loops through all occupancy longs to find a free spot,
			// we want to try spreading out the allocations over the page.
			// Since we keep track of numAllocations anyways, we can use this
			// as an easy way to somewhat spread out the allocations over the page.
			return getFreeIndex(numAllocations);
		}
		
		private int getFreeIndex(int startSearchIndex) {
			while(true) {
				boolean foundEmptySpot = false;
				long occupancyVal = 0;
				long newOccupancyVal = 0;
				for(int index = 0; index < occupancy.length(); ++index) {
					// Offset the index in the occupancy list by startSearchIndex
					// but loop back so that we still check everything.
					// This helps us spread out occupancy over the page more
					// evenly and should help reduce the amount of time it
					// takes to find a free spot.
					int i = (index + startSearchIndex) % occupancy.length();
					occupancyVal = occupancy.get(i);
					if(occupancyVal == 0xFFFFFFFFFFFFFFFFL)
						continue;
					foundEmptySpot = true;
					
					while(true) {
						boolean retry = false;
						// This group of 64 bits has at least one set to 0,
						// so try to find it and set it to 1
						for(int j = 0; j < 64; ++j) {
							if(((occupancyVal >>> j) & 0b1L) == 0) {
								// This bit is 0, so set it to 1 to claim this spot
								newOccupancyVal = occupancyVal | (0b1L << j);
								if(occupancy.weakCompareAndSet(i, occupancyVal, newOccupancyVal)) {
									return i * 64 + j; 
								}
								// Another thread got to us first and changed this occupancy value.
								// So try again.
								occupancyVal = occupancy.get(i);
								retry = true;
								break;
							}
						}
						if(!retry)
							break;
					}
				}
				
				if(!foundEmptySpot)
					return -1; // Could not allocate in this page
			}
		}
		
	}
	
	private Class<T> type;
	private Page<T> page;
	
	public MemoryPool(Class<T> type) {
		this.type = type;
		this.page = new Page<T>(type);
	}
	
	public T alloc() {
		Page<T> currentPage = page;
		while(true) {
			T val = currentPage.alloc();
			if(val == null) {
				Page<T> nextPage = currentPage.nextPage.get();
				Page<T> newInstance = null;
				while(nextPage == null) {
					if(newInstance == null) {
						newInstance = new Page<T>(type);
						newInstance.pageIndex = currentPage.pageIndex + 1;
					}
					nextPage = newInstance;
					if(!currentPage.nextPage.weakCompareAndSet(null, nextPage)) {
						// Another thread allocated the next page before us.
						nextPage = currentPage.nextPage.get();
					}
				}
				currentPage = nextPage;
			}else {
				return val;
			}
		}
	}
	
	public void free(T val) {
		int pageIndex = val._MEMORY_POOL_PAGE_INDEX;
		if(pageIndex < 0 || val._MEMORY_POOL_PAGE_SUBINDEX < 0)
			return;
		Page<T> currentPage = page;
		while(pageIndex > 0) {
			currentPage = currentPage.nextPage.get();
			if(currentPage == null)
				break;
			pageIndex--;
		}
		if(currentPage != null)
			currentPage.free(val);
	}
	
	@SuppressWarnings("unchecked")
	public void free(Object val) {
		free((T) val);
	}
	
	/**
	 * Goes through the data and deallocates instances not in use.
	 */
	public void freeMemory() {
		Page<T> currentPage = page;
		while(true) {
			currentPage.freeMemory();
			currentPage = currentPage.nextPage.get();
			if(currentPage == null)
				break;
		}
	}
	
}
