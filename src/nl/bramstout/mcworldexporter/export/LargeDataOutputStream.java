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

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

/*
 *
 * This is the same as DataOutputStream, but
 * the private field "written" is turned into a long,
 * so that it supports writing more than 2GB,
 * and it's little endian.
 *
 */

public class LargeDataOutputStream extends FilterOutputStream implements DataOutput {
	
    /**
     * The number of bytes written to the data output stream so far.
     */
    protected long written;

    /**
     * bytearr is initialized on demand by writeUTF
     */
    private byte[] bytearr = null;

    public LargeDataOutputStream(OutputStream out) {
        super(out);
    }
    
    private void incCount(int value) {
        written += value;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * b to the underlying output stream.
     */
    public synchronized void write(int b) throws IOException {
        out.write(b);
        incCount(1);
    }

    /**
     * Writes len bytes from the specified byte array
     * starting at offset off to the underlying output stream.
     */
    public synchronized void write(byte b[], int off, int len) throws IOException{
        out.write(b, off, len);
        incCount(len);
    }

    /**
     * Flushes this data output stream. This forces any buffered output
     * bytes to be written out to the stream.
     */
    public void flush() throws IOException {
        out.flush();
    }

    public final void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
        incCount(1);
    }

    public final void writeByte(int v) throws IOException {
        out.write(v);
        incCount(1);
    }

    public final void writeShort(int v) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        incCount(2);
    }
    
    public final void writeChar(int v) throws IOException {
        out.write((v >>> 0) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        incCount(2);
    }

    public final void writeInt(int v) throws IOException {
        out.write((v >>>  0) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
        incCount(4);
    }

    public final void writeLong(long v) throws IOException {
    	out.write((int) ((v >>>  0) & 0xFF));
    	out.write((int) ((v >>>  8) & 0xFF));
    	out.write((int) ((v >>> 16) & 0xFF));
    	out.write((int) ((v >>> 24) & 0xFF));
    	out.write((int) ((v >>> 32) & 0xFF));
    	out.write((int) ((v >>> 40) & 0xFF));
    	out.write((int) ((v >>> 48) & 0xFF));
    	out.write((int) ((v >>> 56) & 0xFF));
        incCount(8);
    }
    
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }
    
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            out.write((byte)s.charAt(i));
        }
        incCount(len);
    }

    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            int v = s.charAt(i);
            out.write((v >>> 0) & 0xFF);
            out.write((v >>> 8) & 0xFF);
        }
        incCount(len * 2);
    }

    public final void writeUTF(String str) throws IOException {
    	int strlen = str.length();
        int utflen = 0;
        int c, count = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535)
            throw new UTFDataFormatException(
                "encoded string too long: " + utflen + " bytes");

        if(bytearr == null || (bytearr.length < (utflen+2)))
            bytearr = new byte[(utflen*2) + 2];

        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);

        int i=0;
        for (i=0; i<strlen; i++) {
           c = str.charAt(i);
           if (!((c >= 0x0001) && (c <= 0x007F))) break;
           bytearr[count++] = (byte) c;
        }

        for (;i < strlen; i++){
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytearr[count++] = (byte) c;

            } else if (c > 0x07FF) {
            	bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
            	bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
            	bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            } else {
            	bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
            	bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
            }
        }
        out.write(bytearr, 0, utflen+2);
        incCount(utflen+2);
    }

    public final long size() {
        return written;
    }
}

