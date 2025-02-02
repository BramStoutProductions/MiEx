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

public class Random extends java.util.Random{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Random() {
		super();
	}
	
	public Random(long seed) {
		super(seed);
	}
	
	public int nextInt(int min, int max) {
		int range = max - min;
		int rand = nextInt(range);
		return rand + min;
	}
	
	public float nextFloat(float min, float max) {
		float range = max - min;
		return nextFloat() * range + min;
	}
	
	public long nextUnsignedLong() {
		return ((long)(next(31)) << 32) | (((long) next(16)) << 16) | ((long) next(16));
	}
	
	public long nextLong(long bound) {
		long r = nextUnsignedLong();
		long m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = r & m;
        else {
            for (long u = r;
                 u - (r = u % bound) + m < 0;
                 u = nextUnsignedLong())
                ;
        }
        return r;
	}
	
	public long nextLong(long min, long max) {
		long range = max - min;
		long rand = nextLong(range);
		return rand + min;
	}

}
