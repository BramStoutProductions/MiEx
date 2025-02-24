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

public class NbtTagInt extends NbtTag{

	public static final byte ID = 3;
	
	public static NbtTagInt newInstance(String name) {
		return (NbtTagInt) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagInt newInstance(String name, int data) {
		NbtTagInt tag = (NbtTagInt) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagInt newNonPooledInstance(String name) {
		return (NbtTagInt) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagInt newNonPooledInstance(String name, int data) {
		NbtTagInt tag = (NbtTagInt) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private int data;
	
	public int getData() {
		return data;
	}
	
	public void setData(int data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagInt tag = (NbtTagInt) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readInt();
	}
	
	@Override
	public String asString() {
		return Integer.toString(data);
	}
	
	@Override
	public byte asByte() {
		return (byte) data;
	}
	
	@Override
	public short asShort() {
		return (short) data;
	}
	
	@Override
	public int asInt() {
		return (int) data;
	}
	
	@Override
	public long asLong() {
		return (long) data;
	}
	
	@Override
	public float asFloat() {
		return (float) data;
	}
	
	@Override
	public double asDouble() {
		return (double) data;
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof NbtTagByte)
			return ((NbtTagByte) obj).getData() == data;
		if(obj instanceof NbtTagShort)
			return ((NbtTagShort) obj).getData() == data;
		if(obj instanceof NbtTagInt)
			return ((NbtTagInt) obj).getData() == data;
		if(obj instanceof NbtTagLong)
			return ((NbtTagLong) obj).getData() == data;
		if(obj instanceof NbtTagFloat)
			return ((NbtTagFloat) obj).getData() == data;
		if(obj instanceof NbtTagDouble)
			return ((NbtTagDouble) obj).getData() == data;
		return false;
	}
	
}
