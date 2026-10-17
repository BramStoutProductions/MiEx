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

public class TargaWriter {
	
	private OutputStream os;
	private long bytesWritten;
	
	public TargaWriter(OutputStream os) throws IOException {
		this.os = os;
	}
	
	public long getBytesWritten() {
		return bytesWritten;
	}
	
	public void write(byte[] data) throws IOException{
		os.write(data);
	}
	
	
	public void writeNullTerminatedString(String str) throws IOException{
		for(int i = 0; i < str.length(); ++i)
			writeChar(str.charAt(i));
		writeChar((char) 0);
	}
	
	public void writeByte(int val) throws IOException {
		os.write(val & 0xFF);
		bytesWritten += 1;
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

}
