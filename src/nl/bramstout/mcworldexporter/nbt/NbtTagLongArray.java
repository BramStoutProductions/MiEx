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

public class NbtTagLongArray extends NbtTag{

	public static final byte ID = 12;
	
	public static NbtTagLongArray newInstance(String name) {
		return (NbtTagLongArray) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagLongArray newInstance(String name, long[] data) {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagLongArray newNonPooledInstance(String name) {
		return (NbtTagLongArray) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagLongArray newNonPooledInstance(String name, long[] data) {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private long[] data;
	
	@Override
	protected void _free() {
		data = null;
	}
	
	@Override
	public byte getId() {
		return ID;
	}
	
	public long[] getData() {
		return data;
	}
	
	public void setData(long[] data) {
		this.data = data;
	}
	
	@Override
	protected void read(DataInput dis) throws Exception {
		int size = dis.readInt();
		data = new long[size];
		for(int i = 0; i < size; ++i)
			data[i] = dis.readLong();
	}

	@Override
	public NbtTag copy() {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newTag(ID, name);
		tag.data = Arrays.copyOf(data, data.length);
		return tag;
	}
	
	@Override
	public String asString() {
		return "";
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
