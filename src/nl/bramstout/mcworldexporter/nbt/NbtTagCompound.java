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
import java.util.Collection;

public class NbtTagCompound extends NbtTag{
	
	public static final byte ID = 10;
	
	public static NbtTagCompound newInstance(String name) {
		return (NbtTagCompound) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagCompound newNonPooledInstance(String name) {
		return (NbtTagCompound) NbtTag.newNonPooledTag(ID, name);
	}
	
	private NbtTag[] data;
	private int dataSize;
	
	// There can be a lot of NbtTagCompound which all need
	// to then allocate an array list, but that can slow
	// things down. In most cases, there aren't that many
	// tags in a compound. So, to speed things up,
	// we store up to 8 tags directly. It's only when
	// we need to store more than 8 tags that we switch to
	// an array list.
	
	private NbtTag data0;
	private NbtTag data1;
	private NbtTag data2;
	private NbtTag data3;
	private NbtTag data4;
	private NbtTag data5;
	private NbtTag data6;
	private NbtTag data7;
	
	public NbtTag get(String name){	
		if(data != null) {
			for(int i = 0; i < dataSize; ++i) {
				if(data[i].getName().equals(name))
					return data[i];
			}
			return null;
		}
		if(data0 == null)
			return null;
		if(data0.name.equals(name))
			return data0;

		if(data1 == null)
			return null;
		if(data1.name.equals(name))
			return data1;
		
		if(data2 == null)
			return null;
		if(data2.name.equals(name))
			return data2;
		
		if(data3 == null)
			return null;
		if(data3.name.equals(name))
			return data3;
		
		if(data4 == null)
			return null;
		if(data4.name.equals(name))
			return data4;
		
		if(data5 == null)
			return null;
		if(data5.name.equals(name))
			return data5;
		
		if(data6 == null)
			return null;
		if(data6.name.equals(name))
			return data6;
		
		if(data7 == null)
			return null;
		if(data7.name.equals(name))
			return data7;
		
		return null;
	}
	
	protected void addUniqueElement(NbtTag tag) {
		if(tag == null)
			return;
		if(data != null) {
			if(dataSize == data.length)
				data = Arrays.copyOf(data, (data.length * 3) / 2);
			
			data[dataSize] = tag;
			dataSize++;
			return;
		}
		if(data0 == null) {
			data0 = tag;
			return;
		}
		
		if(data1 == null) {
			data1 = tag;
			return;
		}
		
		if(data2 == null) {
			data2 = tag;
			return;
		}
		
		if(data3 == null) {
			data3 = tag;
			return;
		}
		
		if(data4 == null) {
			data4 = tag;
			return;
		}
		
		if(data5 == null) {
			data5 = tag;
			return;
		}
		
		if(data6 == null) {
			data6 = tag;
			return;
		}
		
		if(data7 == null) {
			data7 = tag;
			return;
		}
		
		// Elements are full now, so switch over to an array list.
		data = new NbtTag[16];
		dataSize = 9;
		data[0] = data0;
		data[1] = data1;
		data[2] = data2;
		data[3] = data3;
		data[4] = data4;
		data[5] = data5;
		data[6] = data6;
		data[7] = data7;
		data[8] = tag;
		data0 = null;
		data1 = null;
		data2 = null;
		data3 = null;
		data4 = null;
		data5 = null;
		data6 = null;
		data7 = null;
	}
	
	public void addElement(NbtTag tag) {
		if(tag == null)
			return;
		// First check if it's already in there
		if(data != null) {
			for(int i = 0; i < dataSize; ++i) {
				if(data[i].getName().equals(tag.getName())) {
					data[i].free();
					data[i] = tag;
					return;
				}
			}
			if(dataSize == data.length)
				data = Arrays.copyOf(data, (data.length * 3) / 2);
			
			data[dataSize] = tag;
			dataSize++;
			return;
		}
		
		if(data0 == null) {
			data0 = tag;
			return;
		}
		if(data0.name.equals(tag.name)) {
			data0.free();
			data0 = tag;
			return;
		}
		
		if(data1 == null) {
			data1 = tag;
			return;
		}
		if(data1.name.equals(tag.name)) {
			data1.free();
			data1 = tag;
			return;
		}
		
		if(data2 == null) {
			data2 = tag;
			return;
		}
		if(data2.name.equals(tag.name)) {
			data2.free();
			data2 = tag;
			return;
		}
		
		if(data3 == null) {
			data3 = tag;
			return;
		}
		if(data3.name.equals(tag.name)) {
			data3.free();
			data3 = tag;
			return;
		}
		
		if(data4 == null) {
			data4 = tag;
			return;
		}
		if(data4.name.equals(tag.name)) {
			data4.free();
			data4 = tag;
			return;
		}
		
		if(data5 == null) {
			data5 = tag;
			return;
		}
		if(data5.name.equals(tag.name)) {
			data5.free();
			data5 = tag;
			return;
		}
		
		if(data6 == null) {
			data6 = tag;
			return;
		}
		if(data6.name.equals(tag.name)) {
			data6.free();
			data6 = tag;
			return;
		}
		
		if(data7 == null) {
			data7 = tag;
			return;
		}
		if(data7.name.equals(tag.name)) {
			data7.free();
			data7 = tag;
			return;
		}
		
		// Elements are full now, so switch over to an array list.
		data = new NbtTag[16];
		dataSize = 9;
		data[0] = data0;
		data[1] = data1;
		data[2] = data2;
		data[3] = data3;
		data[4] = data4;
		data[5] = data5;
		data[6] = data6;
		data[7] = data7;
		data[8] = tag;
		data0 = null;
		data1 = null;
		data2 = null;
		data3 = null;
		data4 = null;
		data5 = null;
		data6 = null;
		data7 = null;
	}
	
	public void addAllElements(Collection<NbtTag> tags) {
		for(NbtTag tag : tags)
			addElement(tag);
	}
	
	public void addAllElements(NbtTagCompound other) {
		int numItems = other.getSize();
		for(int i = 0; i < numItems; ++i) {
			addElement(other.get(i).copy());
		}
	}
	
	public int getSize() {
		if(data != null)
			return dataSize;
		if(data0 == null)
			return 0;
		if(data1 == null)
			return 1;
		if(data2 == null)
			return 2;
		if(data3 == null)
			return 3;
		if(data4 == null)
			return 4;
		if(data5 == null)
			return 5;
		if(data6 == null)
			return 6;
		if(data7 == null)
			return 7;
		return 8;
	}
	
	public NbtTag get(int index) {
		if(data != null) {
			if(index >= 0 && index < dataSize)
				return data[index];
			return null;
		}
		if(index == 0)
			return data0;
		else if(index == 1)
			return data1;
		else if(index == 2)
			return data2;
		else if(index == 3)
			return data3;
		else if(index == 4)
			return data4;
		else if(index == 5)
			return data5;
		else if(index == 6)
			return data6;
		else if(index == 7)
			return data7;
		return null;
	}


	@Override
	protected void _free() {
		int numItems = getSize();
		for(int i = 0; i < numItems; ++i) {
			NbtTag tag = get(i);
			if(tag != null)
				tag.free();
		}
		data = null;
		dataSize = 0;
		data0 = null;
		data1 = null;
		data2 = null;
		data3 = null;
		data4 = null;
		data5 = null;
		data6 = null;
		data7 = null;
	}

	@Override
	public NbtTag copy() {
		NbtTagCompound copy = (NbtTagCompound) NbtTag.newTag(ID, name);
		int numItems = getSize();
		for(int i = 0; i < numItems; ++i) {
			NbtTag tag = get(i);
			copy.addUniqueElement(tag.copy());
		}
		return copy;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		while(true){
			NbtTag tag = NbtTag.readFromStream(dis);
			if(tag.getId() == 0) {
				tag.free();
				return;
			}
			addUniqueElement(tag);
		}
	}
	
	@Override
	public String asString() {
		return "";
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		int numItems = getSize();
		for(int i = 0; i < numItems; ++i) {
			result = 31 * result + get(i).hashCode();
		}
		return result;
	}

}
