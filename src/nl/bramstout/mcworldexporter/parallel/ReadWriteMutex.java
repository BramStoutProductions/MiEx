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

import java.util.concurrent.atomic.AtomicInteger;

public class ReadWriteMutex {
	
	private AtomicInteger lock;
	
	public ReadWriteMutex() {
		lock = new AtomicInteger(0);
	}
	
	public void acquireRead() {
		while(true) {
			int val = lock.incrementAndGet();
			if(val > 0)
				return;
			// The value is negative, meaning that there is
			// a write lock on it, so reset the value.
			// It could be that the write lock got released
			// just now, so we use a compare and set.
			lock.compareAndSet(val, -100000000);
		}
	}
	
	public void acquireWrite() {
		while(true) {
			int val = lock.get();
			if(val != 0) { // Write or read lock already acquired
				Thread.yield();
				continue;
			}
			int newVal = -100000000;
			if(!lock.weakCompareAndSetVolatile(val, newVal)) { // Another thread changed it first
				//Thread.yield();
				continue;
			}
			break;
		}
	}
	
	public void releaseRead() {
		lock.decrementAndGet();
	}
	
	public void releaseWrite() {
		lock.set(0);
	}
	
}
