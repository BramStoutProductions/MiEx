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


public class SingleThreadedMemoryPool<T extends Poolable> {

	private static class Page<T extends Poolable>{
		
		private static final int PAGE_SIZE = 1024 * 16;
		
		public T[] data = null;
		public long[] occupancy = new long[PAGE_SIZE / 64];
		public Constructor<T> typeConstructor;
		public Page<T> nextPage = null;
		public int pageIndex = 0;
		public int numAllocations = 0;
		
		@SuppressWarnings("unchecked")
		public Page(Class<T> type) {
			try {
				this.typeConstructor = type.getConstructor();
			}catch(Exception ex) {
				throw new RuntimeException("No default constructor", ex);
			}
			data = (T[]) Array.newInstance(type, PAGE_SIZE);
		}
		
		public T alloc() {
			// If we have as many items allocated as we can fit in this page,
			// no need to through everything to find a free spot. There isn't one.
			if(numAllocations >= PAGE_SIZE)
				return null;
			int index = getFreeIndex();
			if(index < 0)
				return null;
			
			numAllocations++;
			
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
			long occupancyVal = occupancy[occupancyIndex];
			long newOccupancyVal = occupancyVal & ~(0b1L << bitIndex);
			occupancy[occupancyIndex] = newOccupancyVal;
			
			numAllocations--;
		}
		
		private int getFreeIndex() {
			int startIndex = numAllocations;
			long occupancyVal = 0;
			long newOccupancyVal = 0;
			for(int index = 0; index < occupancy.length; ++index) {
				int i = (startIndex + index) % occupancy.length;
				occupancyVal = occupancy[i];
				if(occupancyVal == 0xFFFFFFFFFFFFFFFFL)
					continue;
				
				// This group of 64 bits has at least one set to 0,
				// so try to find it and set it to 1
				for(int j = 0; j < 64; ++j) {
					if(((occupancyVal >>> j) & 0b1L) == 0) {
						// This bit is 0, so set it to 1 to claim this spot
						newOccupancyVal = occupancyVal | (0b1L << j);
						occupancy[i] = newOccupancyVal;
						return i * 64 + j;
					}
				}
			}
				
			return -1; // Could not allocate in this page
		}
		
	}
	
	private Class<T> type;
	private Page<T> page;
	
	public SingleThreadedMemoryPool(Class<T> type) {
		this.type = type;
		this.page = new Page<T>(type);
	}
	
	public T alloc() {
		Page<T> currentPage = page;
		while(true) {
			T val = currentPage.alloc();
			if(val == null) {
				Page<T> nextPage = currentPage.nextPage;
				if(nextPage == null) {
					nextPage = new Page<T>(type);
					nextPage.pageIndex = currentPage.pageIndex + 1;
					currentPage.nextPage = nextPage;
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
			currentPage = currentPage.nextPage;
			if(currentPage == null)
				break;
			pageIndex--;
		}
		if(currentPage != null)
			currentPage.free(val);
	}
	
}
