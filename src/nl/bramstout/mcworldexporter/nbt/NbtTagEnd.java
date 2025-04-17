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

public class NbtTagEnd extends NbtTag{

	public static final byte ID = 0;
	
	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		return NbtTag.newTag(ID, name);
	}

	@Override
	public byte getId() {
		return ID;
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
	public boolean asBoolean() {
		return false;
	}

	@Override
	protected void read(DataInput dis) throws Exception {}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof NbtTagEnd;
	}

}
