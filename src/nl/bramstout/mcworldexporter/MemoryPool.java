/*
D * BSD 3-Clause License
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

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
		public AtomicIntegerArray freelist;
		public AtomicLong freelistRead;
		public AtomicLong freelistWrite;
		public int pageIndex = 0;
		public Constructor<T> typeConstructor;
		
		@SuppressWarnings("unchecked")
		public Page(Class<T> type, int pageIndex) {
			try {
				this.typeConstructor = type.getConstructor();
			}catch(Exception ex) {
				throw new RuntimeException("No default constructor", ex);
			}
			this.pageIndex = pageIndex;
			data = (T[]) Array.newInstance(type, PAGE_SIZE);
			freelist = new AtomicIntegerArray(PAGE_SIZE);
			for(int i = 0; i < PAGE_SIZE; ++i) {
				freelist.set(i, i);
			}
			freelistRead = new AtomicLong(0);
			freelistWrite = new AtomicLong(PAGE_SIZE);
		}
		
		public T alloc() {
			int index;
			
			while(true) {
				long freelistIndex = freelistRead.get();
				long freelistSize = freelistWrite.get();
				if(freelistIndex >= freelistSize)
					return null;
				if(freelistRead.compareAndSet(freelistIndex, freelistIndex + 1)) {
					index = freelist.getAndSet((int) (freelistIndex % PAGE_SIZE), -1);
					if(index >= 0)
						break;
				}
			}
			
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
			if(val._MEMORY_POOL_PAGE_INDEX != pageIndex)
				throw new RuntimeException("Provided instance's page index doesn't match.");
			if(data[val._MEMORY_POOL_PAGE_SUBINDEX] != val)
				throw new RuntimeException("Provided instance isn't allocated on this page.");
			
			int index = val._MEMORY_POOL_PAGE_SUBINDEX;
			
			val._MEMORY_POOL_PAGE_INDEX = -1;
			val._MEMORY_POOL_PAGE_SUBINDEX = -1;
			
			{
				
				long writeIndex = freelistWrite.getAndIncrement();
				freelist.set((int)(writeIndex % PAGE_SIZE), index);
				
			}
		}
		
		/**
		 * Goes through the data and deallocates instances not in use.
		 */
		public void freeMemory() {
			for(int i = 0; i < PAGE_SIZE; ++i) {
				int index = freelist.get(i);
				if(index >= 0) {
					data[i] = null;
				}
			}
		}
		
	}
	
	private Class<T> type;
	private Page<T>[] pages;
	
	@SuppressWarnings("unchecked")
	public MemoryPool(Class<T> type) {
		this.type = type;
		this.pages = new Page[1];
		this.pages[0] = new Page<T>(type, 0);
	}
	
	public T alloc() {
		Page<T>[] pages = this.pages;
		for(int i = 0; i < pages.length; ++i) {
			T val = pages[i].alloc();
			if(val != null)
				return val;
		}
		// No page has space, so allocate another page.
		synchronized(this) {
			pages = Arrays.copyOf(this.pages, this.pages.length + 1);
			pages[pages.length-1] = new Page<T>(type, pages.length-1);
			this.pages = pages;
		}
		T val = pages[pages.length-1].alloc();
		if(val != null)
			return val;
		
		// Still no valid allocation, so we call this function
		// to try again.
		return alloc();
	}
	
	public void free(T val) {
		int pageIndex = val._MEMORY_POOL_PAGE_INDEX;
		if(pageIndex < 0 || pageIndex >= pages.length || val._MEMORY_POOL_PAGE_SUBINDEX < 0)
			return;
		pages[pageIndex].free(val);
	}
	
	/**
	 * Goes through the data and deallocates instances not in use.
	 */
	public void freeMemory() {
		Page<T>[] pages = this.pages;
		for(int i = 0; i < pages.length; ++i) {
			pages[i].freeMemory();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void free(Object val) {
		free((T) val);
	}
	
}
