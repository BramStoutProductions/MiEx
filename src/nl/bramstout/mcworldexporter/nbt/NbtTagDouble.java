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

public class NbtTagDouble extends NbtTag{

	public static final byte ID = 6;
	
	public static NbtTagDouble newInstance(String name) {
		return (NbtTagDouble) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagDouble newInstance(String name, double data) {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagDouble newNonPooledInstance(String name) {
		return (NbtTagDouble) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagDouble newNonPooledInstance(String name, double data) {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private double data;
	
	public double getData() {
		return data;
	}
	
	public void setData(double data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readDouble();
	}
	
	@Override
	public String asString() {
		return Double.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Double.hashCode(data);
		return result;
	}

	
}
