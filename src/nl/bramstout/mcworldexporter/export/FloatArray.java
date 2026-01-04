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

public class FloatArray {
	
	private static final int INIT_SIZE = 32;
	
	private float[] data;
	private int size;
	
	public FloatArray() {
		this(INIT_SIZE);
		this.size = 0;
	}
	
	public FloatArray(int capacity) {
		data = new float[capacity];
		this.size = 0;
	}
	
	public FloatArray(float[] data) {
		this.data = data;
		this.size = data.length;
	}
	
	public void resize(int size) {
		data = Arrays.copyOf(data, size);
		this.size = size;
	}
	
	public void set(int index, float value) {
		if(index >= data.length) {
			this.data = Arrays.copyOf(this.data, Math.max(this.data.length * 2, index + 1));
		} 
		this.data[index] = value;
		this.size = index >= this.size ? (index + 1) : this.size;
	}
	
	public void add(float value) {
		set(this.size, value);
	}
	
	public float get(int index) {
		return this.data[index];
	}
	
	public float[] getData() {
		return this.data;
	}
	
	public int size() {
		return this.size;
	}

	public void add(FloatArray value) {
		for(int i = 0; i < value.size; ++i)
			add(value.get(i));
	}
	
	public void clear() {
		this.size = 0;
	}

}
