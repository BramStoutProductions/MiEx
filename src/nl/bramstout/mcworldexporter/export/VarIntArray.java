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

public class VarIntArray {

	private static final int INIT_SIZE = 64;
	
	private static enum Type{
		BYTE, SHORT, INT
	}

	private byte[] byteData;
	private short[] shortData;
	private int[] intData;
	private int size;
	private Type type;
	
	public VarIntArray() {
		this(INIT_SIZE);
		this.size = 0;
	}
	
	public VarIntArray(int capacity) {
		type = Type.BYTE;
		byteData = new byte[capacity];
		shortData = null;
		intData = null;
		this.size = 0;
	}
	
	public VarIntArray(byte[] data) {
		this.type = Type.BYTE;
		this.byteData = data;
		this.shortData = null;
		this.intData = null;
		this.size = data.length;
	}
	
	public VarIntArray(short[] data) {
		this.type = Type.SHORT;
		this.byteData = null;
		this.shortData = data;
		this.intData = null;
		this.size = data.length;
	}
	
	public VarIntArray(int[] data) {
		this.type = Type.INT;
		this.byteData = null;
		this.shortData = null;
		this.intData = data;
		this.size = data.length;
	}
	
	public VarIntArray(VarIntArray other) {
		this.type = other.type;
		this.byteData = null;
		this.shortData = null;
		this.intData = null;
		if(other.byteData != null)
			this.byteData = Arrays.copyOf(other.byteData, other.byteData.length);
		if(other.shortData != null)
			this.shortData = Arrays.copyOf(other.shortData, other.shortData.length);
		if(other.intData != null)
			this.intData = Arrays.copyOf(other.intData, other.intData.length);
		this.size = other.size;
	}
	
	public void resize(int size) {
		switch(type) {
		case BYTE:
			byteData = Arrays.copyOf(byteData, size);
			break;
		case SHORT:
			shortData = Arrays.copyOf(shortData, size);
			break;
		case INT:
			intData = Arrays.copyOf(intData, size);
			break;
		}
		this.size = size;
	}
	
	private void switchType(Type newType) {
		if(newType == type)
			return;
		if(newType == Type.BYTE) {
			if(type == Type.SHORT) {
				byteData = new byte[shortData.length];
				for(int i = 0; i < size; ++i)
					byteData[i] = (byte) shortData[i];
				shortData = null;
			}
			else if(type == Type.INT) {
				byteData = new byte[intData.length];
				for(int i = 0; i < size; ++i)
					byteData[i] = (byte) intData[i];
				intData = null;
			}
		}else if(newType == Type.SHORT) {
			if(type == Type.BYTE) {
				shortData = new short[byteData.length];
				for(int i = 0; i < size; ++i)
					shortData[i] = byteData[i];
				byteData = null;
			}
			else if(type == Type.INT) {
				shortData = new short[intData.length];
				for(int i = 0; i < size; ++i)
					shortData[i] = (short) intData[i];
				intData = null;
			}
		}else if(newType == Type.INT) {
			if(type == Type.BYTE) {
				intData = new int[byteData.length];
				for(int i = 0; i < size; ++i)
					intData[i] = byteData[i];
				byteData = null;
			}
			else if(type == Type.SHORT) {
				intData = new int[shortData.length];
				for(int i = 0; i < size; ++i)
					intData[i] = shortData[i];
				shortData = null;
			}
		}
		type = newType;
	}
	
	public void set(int index, int value) {
		int capacity = capacity();
		if(index >= capacity) {
			setCapacity(Math.max(capacity * 2, index + 1));
		}
		if(type == Type.BYTE || type == Type.SHORT) {
			if(value > ((int) Short.MAX_VALUE) || value < ((int) Short.MIN_VALUE))
				switchType(Type.INT);
		}
		if(type == Type.BYTE) {
			if(value > ((int) Byte.MAX_VALUE) || value < ((int) Byte.MIN_VALUE))
				switchType(Type.SHORT);
		}
		switch(type) {
		case BYTE:
			this.byteData[index] = (byte) value;
			break;
		case SHORT:
			this.shortData[index] = (short) value;
			break;
		case INT:
			this.intData[index] = value;
			break;
		}
		this.size = index >= this.size ? (index + 1) : this.size;
	}
	
	public void reserve(int capacity) {
		if(capacity >= capacity)
			return;
		setCapacity(capacity);
	}
	
	public void add(int value) {
		set(this.size, value);
	}
	
	public int get(int index) {
		switch(type) {
		case BYTE:
			return this.byteData[index];
		case SHORT:
			return this.shortData[index];
		case INT:
			return this.intData[index];
		}
		return 0;
	}
	
	public Object getData() {
		switch(type) {
		case BYTE:
			return this.byteData;
		case SHORT:
			return this.shortData;
		case INT:
			return this.intData;
		}
		return null;
	}
	
	public byte[] getByteData() {
		return this.byteData;
	}
	
	public short[] getShortData() {
		return this.shortData;
	}
	
	public int[] getIntData() {
		return this.intData;
	}
	
	public int size() {
		return this.size;
	}
	
	public int capacity() {
		switch(type) {
		case BYTE:
			return byteData.length;
		case SHORT:
			return shortData.length;
		case INT:
			return intData.length;
		}
		return 0;
	}
	
	public void setCapacity(int capacity) {
		switch(type) {
		case BYTE:
			byteData = Arrays.copyOf(byteData, capacity);
			break;
		case SHORT:
			shortData = Arrays.copyOf(shortData, capacity);
			break;
		case INT:
			intData = Arrays.copyOf(intData, capacity);
			break;
		}
	}
	
	public void clear() {
		this.size = 0;
	}
	
	public int getMemoryUsage() {
		switch(type) {
		case BYTE:
			return byteData.length;
		case SHORT:
			return shortData.length * 2;
		case INT:
			return intData.length * 4;
		}
		return 0;
	}
	
}
