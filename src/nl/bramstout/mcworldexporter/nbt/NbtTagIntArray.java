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

package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;
import java.util.Arrays;

public class NbtTagIntArray extends NbtTag{

	public static final byte ID = 11;
	
	public static NbtTagIntArray newInstance(String name) {
		return (NbtTagIntArray) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagIntArray newInstance(String name, int[] data) {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagIntArray newNonPooledInstance(String name) {
		return (NbtTagIntArray) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagIntArray newNonPooledInstance(String name, int[] data) {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private int[] data;
	
	@Override
	protected void _free() {
		data = null;
	}
	
	@Override
	public byte getId() {
		return ID;
	}
	
	public int[] getData() {
		return data;
	}
	
	public void setData(int[] data) {
		this.data = data;
	}
	
	@Override
	protected void read(DataInput dis) throws Exception {
		int size = dis.readInt();
		data = new int[size];
		for(int i = 0; i < size; ++i)
			data[i] = dis.readInt();
	}

	@Override
	public NbtTag copy() {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newTag(ID, name);
		tag.data = Arrays.copyOf(data, data.length);
		return tag;
	}
	
	@Override
	public String asString() {
		return "";
	}
	
	@Override
	public byte asByte() {
		return 0;
	}
	
	@Override
	public short asShort() {
		return 0;
	}
	
	@Override
	public int asInt() {
		return 0;
	}
	
	@Override
	public long asLong() {
		return 0;
	}
	
	@Override
	public float asFloat() {
		return 0;
	}
	
	@Override
	public double asDouble() {
		return 0;
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Arrays.hashCode(data);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof NbtTagByteArray) {
			if(((NbtTagByteArray) obj).getData().length != data.length)
				return false;
			for(int i = 0; i < data.length; ++i)
				if(((NbtTagByteArray) obj).getData()[i] != data[i])
					return false;
			return true;
		}
		if(obj instanceof NbtTagIntArray) {
			if(((NbtTagIntArray) obj).getData().length != data.length)
				return false;
			for(int i = 0; i < data.length; ++i)
				if(((NbtTagIntArray) obj).getData()[i] != data[i])
					return false;
			return true;
		}
		if(obj instanceof NbtTagLongArray) {
			if(((NbtTagLongArray) obj).getData().length != data.length)
				return false;
			for(int i = 0; i < data.length; ++i)
				if(((NbtTagLongArray) obj).getData()[i] != data[i])
					return false;
			return true;
		}
		return false;
	}
	
}
