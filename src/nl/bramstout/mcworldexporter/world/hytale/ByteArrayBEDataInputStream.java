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

package nl.bramstout.mcworldexporter.world.hytale;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ByteArrayBEDataInputStream implements DataInput{

	private byte[] data;
	private int pos;
	
	public ByteArrayBEDataInputStream(byte[] data) {
		this.data = data;
		this.pos = 0;
	}
	
	public void seek(int pos) throws IOException{
		if(pos < 0 || pos >= data.length)
			throw new EOFException();
		this.pos = pos;
	}
	
	public int read() throws IOException {
		if(pos >= data.length)
			throw new EOFException();
		int val = ((int) data[pos]) & 0xFF;
		pos += 1;
		return val;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		System.arraycopy(data, pos, b, off, Math.min(len, data.length-pos));
		pos += Math.min(len, data.length-pos);
	}
	
	public void readInts(int[] b, int off, int len) throws IOException{
		for(int i = 0; i < len; ++i)
			b[i+off] = readInt();
	}

	@Override
	public int skipBytes(int n) throws IOException {
		n = Math.min(n, data.length-pos);
		pos += n;
		return n;
	}

	@Override
	public boolean readBoolean() throws IOException {
		int val = read();
		return val > 0;
	}

	@Override
	public byte readByte() throws IOException {
		return (byte) read();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return read();
	}

	@Override
	public short readShort() throws IOException {
		int val2 = read();
		int val1 = read();
		return (short) (val1 | (val2 << 8));
	}

	@Override
	public int readUnsignedShort() throws IOException {
		int val2 = read();
		int val1 = read();
		return val1 | (val2 << 8);
	}

	@Override
	public char readChar() throws IOException {
		int val2 = read();
		int val1 = read();
		return (char) (val1 | (val2 << 8));
	}

	@Override
	public int readInt() throws IOException {
		int val4 = read();
		int val3 = read();
		int val2 = read();
		int val1 = read();
		return val1 | (val2 << 8) | (val3 << 16) | (val4 << 24);
	}

	@Override
	public long readLong() throws IOException {
		long val8 = read();
		long val7 = read();
		long val6 = read();
		long val5 = read();
		long val4 = read();
		long val3 = read();
		long val2 = read();
		long val1 = read();
		return val1 | (val2 << 8) | (val3 << 16) | (val4 << 24) | 
				(val5 << 32) | (val6 << 40) | (val7 << 48) | (val8 << 54);
	}

	@Override
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	@Override
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	@Override
	public String readLine() throws IOException {
		return null;
	}

	@Override
	public String readUTF() throws IOException {
		int currentPos = pos;
		try {
			return DataInputStream.readUTF(this);
		}catch(Exception ex) {
			// With Bedrock, it could store non UTF-8 strings here.
			// They can cause issues, so we need to handle them separately.
			// So, we are just going to read in the bytes like normal.
			// First we reset the position.
			pos = currentPos;
			int utflen = readUnsignedShort();
			char[] chars = new char[utflen];
			for(int i = 0; i < utflen; ++i)
				chars[i] = (char) read();
			return new String(chars);
		}
	}
	
	public String readHytaleUTF() throws IOException{
		int length = readShort();
		if(length < 0)
			throw new IOException("Invalid UTF length");
		byte[] bytes = new byte[length];
		readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public int available() {
		return data.length - pos;
	}

}
