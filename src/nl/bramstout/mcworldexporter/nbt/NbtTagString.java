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

public class NbtTagString extends NbtTag{

	public static final byte ID = 8;
	
	public static NbtTagString newInstance(String name) {
		return (NbtTagString) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagString newInstance(String name, String data) {
		NbtTagString tag = (NbtTagString) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagString newNonPooledInstance(String name) {
		return (NbtTagString) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagString newNonPooledInstance(String name, String data) {
		NbtTagString tag = (NbtTagString) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private String data;
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}

	@Override
	protected void _free() {
		data = null;
	}

	@Override
	public NbtTag copy() {
		NbtTagString tag = (NbtTagString) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readUTF();
	}
	
	@Override
	public String asString() {
		return data;
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof NbtTagString)
			return ((NbtTagString) obj).getData().equals(data);
		return false;
	}
	
}
