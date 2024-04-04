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

package nl.bramstout.mcworldexporter.export;

import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

public class LargeDataInputStream extends FilterInputStream implements DataInput {
	
	long read = 0;

	public LargeDataInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		read += in.read(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		read += in.read(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		long skipped = in.skip(n);
		read += skipped;
		return (int) skipped;
	}
	
	public void skipBytes(long n) throws IOException{
		read += n;
		while (n > 0) {
            long ns = in.skip(n);
            if (ns == 0) {
            	if(in.read() == -1)
            		throw new EOFException();
            	ns = 1;
            }
            n -= ns;
        }
	}

	@Override
	public boolean readBoolean() throws IOException {
		read += 1;
		int data = in.read();
        if (data < 0)
            throw new EOFException();
		return data > 0;
	}

	@Override
	public byte readByte() throws IOException {
		read += 1;
		int data = in.read();
        if (data < 0)
            throw new EOFException();
		return (byte) data;
	}

	@Override
	public int readUnsignedByte() throws IOException {
		read += 1;
		int data = in.read();
        if (data < 0)
            throw new EOFException();
		return (byte) (data & 0xFF);
	}

	@Override
	public short readShort() throws IOException {
		read += 2;
		int data1 = in.read();
		int data2 = in.read();
        if (data1 < 0 || data2 < 0)
            throw new EOFException();
		return (short) (data1 & 0xFF | ((data2 & 0xFF) << 8));
	}

	@Override
	public int readUnsignedShort() throws IOException {
		read += 2;
		int data1 = in.read();
		int data2 = in.read();
        if (data1 < 0 || data2 < 0)
            throw new EOFException();
		return data1 & 0xFF | ((data2 & 0xFF) << 8);
	}

	@Override
	public char readChar() throws IOException {
		return (char) readUnsignedShort();
	}

	@Override
	public int readInt() throws IOException {
		read += 4;
		int data1 = in.read();
		int data2 = in.read();
		int data3 = in.read();
		int data4 = in.read();
        if (data1 < 0 || data2 < 0 || data3 < 0 || data4 < 0)
            throw new EOFException();
		return data1 & 0xFF | ((data2 & 0xFF) << 8) | ((data3 & 0xFF) << 16) | ((data4 & 0xFF) << 24);
	}

	@Override
	public long readLong() throws IOException {
		read += 8;
		long data1 = in.read();
		long data2 = in.read();
		long data3 = in.read();
		long data4 = in.read();
		long data5 = in.read();
		long data6 = in.read();
		long data7 = in.read();
		long data8 = in.read();
        if (data1 < 0 || data2 < 0 || data3 < 0 || data4 < 0 || data5 < 0 || data6 < 0 || data7 < 0 || data8 < 0)
            throw new EOFException();
		return data1 & 0xFF | ((data2 & 0xFF) << 8) | ((data3 & 0xFF) << 16) | ((data4 & 0xFF) << 24) |
				((data5 & 0xFF) << 32) | ((data6 & 0xFF) << 40) | ((data7 & 0xFF) << 48) | ((data8 & 0xFF) << 56);
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
	
	private byte bytearr[] = new byte[80];
    private char chararr[] = new char[80];

	@Override
	public String readUTF() throws IOException {
		int utflen = readUnsignedShort();
        if (bytearr.length < utflen){
            bytearr = new byte[utflen*2];
            chararr = new char[utflen*2];
        }

        int c, char2, char3;
        int c2;
        int count = 0;
        int chararr_count=0;

        readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127) break;
            count++;
            chararr[chararr_count++]=(char)c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            c2 = c >> 4;
            
        	if(c2 < 8) {
                /* 0xxxxxxx*/
                count++;
                chararr[chararr_count++]=(char)c;
            }else if(c2 == 12 || c2 == 13){
                /* 110x xxxx   10xx xxxx*/
                count += 2;
                if (count > utflen)
                    throw new UTFDataFormatException(
                        "malformed input: partial character at end");
                char2 = (int) bytearr[count-1];
                if ((char2 & 0xC0) != 0x80)
                    throw new UTFDataFormatException(
                        "malformed input around byte " + count);
                chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
                                                (char2 & 0x3F));
            }else if(c2 == 14) {
                /* 1110 xxxx  10xx xxxx  10xx xxxx */
                count += 3;
                if (count > utflen)
                    throw new UTFDataFormatException(
                        "malformed input: partial character at end");
                char2 = (int) bytearr[count-2];
                char3 = (int) bytearr[count-1];
                if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                    throw new UTFDataFormatException(
                        "malformed input around byte " + (count-1));
                chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
                                                ((char2 & 0x3F) << 6)  |
                                                ((char3 & 0x3F) << 0));
            }else {
                /* 10xx xxxx,  1111 xxxx */
                throw new UTFDataFormatException(
                    "malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
	}

	public long getPosition() {
		return read;
	}

}
