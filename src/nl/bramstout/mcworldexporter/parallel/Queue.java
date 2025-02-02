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

package nl.bramstout.mcworldexporter.parallel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Queue<T> {

	private static class Block<T>{
		private T[] data;
		private long startAddress;
		public Block<T> nextBlock;
		
		@SuppressWarnings("unchecked")
		public Block(long startAddress) {
			data = (T[]) new Object[1024];
			nextBlock = null;
			this.startAddress = startAddress;
		}
		
		public T get(long index) {
			long localIndex = index - startAddress;
			if(localIndex >= 1024) {
				if(nextBlock == null)
					return null;
				return nextBlock.get(index);
			}
			return data[(int) localIndex];
		}
		
		public void set(long index, T value) {
			long localIndex = index - startAddress;
			if(localIndex >= 1024) {
				nextBlock.set(index, value);
			}else {
				if(localIndex == 0)
					nextBlock = new Block<T>(startAddress + 1024);
				data[(int) localIndex] = value;
			}
		}
	}
	
	private AtomicLong readIndex;
	private AtomicLong writeIndex;
	private AtomicReference<Block<T>> readBlock;
	private AtomicReference<Block<T>> writeBlock;
	
	public Queue() {
		this.readIndex = new AtomicLong(0);
		this.writeIndex = new AtomicLong(0);
		this.readBlock = new AtomicReference<Block<T>>(new Block<T>(0));
		this.writeBlock = new AtomicReference<Block<T>>(this.readBlock.get());
	}
	
	public void push(T value) {
		// Get the current index for writing.
		long index = writeIndex.getAndIncrement();
		// Get the block
		Block<T> block = writeBlock.get();
		// Set the value
		try {
			block.set(index, value);
		}catch(Exception ex) {}
		// If we've reached the next block, then update
		// the writeBlock value to the next block.
		// If we are going really fast, nextBlock might still be
		// null, so just do a quick spin lock until it's no longer
		// null
		while(block.nextBlock == null) {
			Thread.yield();
		}
		if(index - block.startAddress == 1152)
			writeBlock.set(block.nextBlock);
	}
	
	public T pop() {
		// Get the block
		Block<T> block = readBlock.get();
		
		long _writeIndex = writeIndex.get();
		// Get the current index for reading.
		long index = readIndex.getAndIncrement();
		// If we are at the end of the queue, return null;
		if(index >= _writeIndex) {
			readIndex.decrementAndGet();
			return null;
		}
		// Get the value
		T value = block.get(index);
		// If we've reached the next block, then update
		// the readBlock value to the next block.
		if(index - block.startAddress == 1152)
			readBlock.set(block.nextBlock);
		return value;
	}
	
	
	
}
