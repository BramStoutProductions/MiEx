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

package nl.bramstout.mcworldexporter.image;

import java.io.IOException;
import java.io.OutputStream;

public class EXRWriter {
	
	private OutputStream os;
	private long bytesWritten;
	
	public EXRWriter(OutputStream os) throws IOException {
		this.os = os;
		writeInt(20000630);
		writeInt(2);
	}
	
	public long getBytesWritten() {
		return bytesWritten;
	}
	
	public static enum DataType{
		HALF, FLOAT, UINT
	}
	
	public static enum LineOrder{
		INCREASING_Y, DECREASING_Y, RANDOM_Y
	}
	
	public static class Channel{
		public String name;
		public DataType type;
		
		public Channel(String name, DataType type) {
			this.name = name;
			this.type = type;
		}
		
		public int getSize() {
			return name.length() + 1 + 4 + 4 + 4 + 4;
		}
	}
	
	public void writeChannelsAttribute(Channel[] channels) throws IOException{
		int size = 0;
		for(Channel channel : channels)
			size += channel.getSize();
		writeAttributeStart("channels", "chlist", size);
		for(Channel channel : channels) {
			writeNullTerminatedString(channel.name);
			switch(channel.type) {
			case HALF:
				writeInt(1);
				break;
			case FLOAT:
				writeInt(2);
				break;
			case UINT:
				writeInt(0);
				break;
			}
			writeChar((char)0);
			writeChar((char)0);
			writeChar((char)0);
			writeChar((char)0);
			writeInt(1);
			writeInt(1);
		}
		writeChar((char) 0);
	}
	
	public void writeAttributeStart(String name, String type, int size) throws IOException{
		writeNullTerminatedString(name);
		writeNullTerminatedString(type);
		writeInt(size);
	}
	
	public void writeBox2i(int xMin, int yMin, int xMax, int yMax) throws IOException{
		writeInt(xMin);
		writeInt(yMin);
		writeInt(xMax);
		writeInt(yMax);
	}
	
	public void writeBoxif(float xMin, float yMin, float xMax, float yMax) throws IOException{
		writeFloat(xMin);
		writeFloat(yMin);
		writeFloat(xMax);
		writeFloat(yMax);
	}
	
	public void writeCompression(int compressionId) throws IOException{
		writeChar((char) compressionId);
	}
	
	public void writeLineOrder(LineOrder lineOrder) throws IOException{
		switch(lineOrder) {
		case INCREASING_Y:
			writeChar((char) 0);
			break;
		case DECREASING_Y:
			writeChar((char) 1);
			break;
		case RANDOM_Y:
			writeChar((char) 2);
			break;
		}
	}
	
	
	
	
	public void writeNullTerminatedString(String str) throws IOException{
		for(int i = 0; i < str.length(); ++i)
			writeChar(str.charAt(i));
		writeChar((char) 0);
	}
	
	public void writeChar(char val) throws IOException {
		os.write(val & 0xFF);
		bytesWritten += 1;
	}
	
	public void writeShort(short val) throws IOException {
		os.write(val & 0xFF);
		os.write((val >> 8) & 0xFF);
		bytesWritten += 2;
	}
	
	public void writeInt(int val) throws IOException {
		os.write(val & 0xFF);
		os.write((val >>> 8) & 0xFF);
		os.write((val >>> 16) & 0xFF);
		os.write((val >>> 24) & 0xFF);
		bytesWritten += 4;
	}
	
	public void writeLong(long val) throws IOException {
		os.write((int) (val & 0xFFL));
		os.write((int) ((val >>> 8) & 0xFFL));
		os.write((int) ((val >>> 16) & 0xFFL));
		os.write((int) ((val >>> 24) & 0xFFL));
		os.write((int) ((val >>> 32) & 0xFFL));
		os.write((int) ((val >>> 40) & 0xFFL));
		os.write((int) ((val >>> 48) & 0xFFL));
		os.write((int) ((val >>> 56) & 0xFFL));
		bytesWritten += 8;
	}
	
	public void writeFloat(float val) throws IOException{
		writeInt(Float.floatToRawIntBits(val));
	}
	
	public void writeDouble(double val) throws IOException{
		writeLong(Double.doubleToRawLongBits(val));
	}
	
	public void writeHalf(float val) throws IOException{
		int halfBits = floatToHalf(val);
		os.write(halfBits & 0xFF);
		os.write((halfBits >> 8) & 0xFF);
		bytesWritten += 2;
	}
	
	private int floatToHalf(float val) {
		// Conversion code adapted from https://github.com/AcademySoftwareFoundation/Imath/blob/main/src/Imath/half.h#L375
		//
		// SPDX-License-Identifier: BSD-3-Clause
		// Copyright Contributors to the OpenEXR Project.
		//

		//
		// Primary original authors:
		//		     Florian Kainz <kainz@ilm.com>
		//		     Rod Bogart <rgb@ilm.com>
		//
		
		int bits = Float.floatToRawIntBits(val);
		int ui = bits & ~0x80000000;
		int ret = (bits >>> 16) & 0x8000;
		
		if(ui >= 0x38800000) {
			if(ui >= 0x7f800000) {
				// inf or nan
				ret |= 0x7c00;
				if(ui == 0x7f800000) return ret;
				int m = (ui & 0x7fffff) >>> 13;
				return ret | m | (m == 0 ? 1 : 0);
			}
			if(ui > 0x477fefff) {
				// round to inf
				return ret | 0x7c00;
			}
			ui -= 0x38000000;
			ui = ((ui + 0x00000fff + ((ui >> 13) & 1)) >> 13);
			return ret | ui;
		}
		
		if (ui < 0x33000001) {
			// zero
			return ret;
		}
		
		int e     = (ui >> 23);
	    int shift = 0x7e - e;
	    int m     = 0x800000 | (ui & 0x7fffff);
	    int r     = m << (32 - shift);
	    ret |= (m >> shift);
	    if (r > 0x80000000 || (r == 0x80000000 && (ret & 0x1) != 0)) ++ret;
	    return ret;
	}

}
