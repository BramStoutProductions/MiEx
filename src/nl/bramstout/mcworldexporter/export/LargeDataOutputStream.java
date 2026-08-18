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
import java.io.File;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

/*
 *
 * This is the same as DataOutputStream, but
 * the private field "written" is turned into a long,
 * so that it supports writing more than 2GB,
 * and it's little endian.
 *
 */

public class LargeDataOutputStream implements DataOutput {
	
    /**
     * The number of bytes written to the data output stream so far.
     */
    protected long written;

    /**
     * bytearr is initialized on demand by writeUTF
     */
    private byte[] bytearr = null;
    
    private byte[] buffer = new byte[1 * 1024 * 1024];
    private int bufferWritten = 0;
    private FileChannel channel;

    public LargeDataOutputStream(File file) throws IOException {
        channel = FileChannel.open(file.toPath(), 
        		StandardOpenOption.CREATE, 
        		StandardOpenOption.WRITE, 
        		StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void close() throws IOException{
    	flush();
    	channel.close();
    }
    
    private void flush(byte[] buffer, int offset, int length) throws IOException {
    	ByteBuffer bufferWrapper = ByteBuffer.wrap(buffer, offset, length);
    	channel.write(bufferWrapper);
    }
    
    /**
     * Flushes this data output stream. This forces any buffered output
     * bytes to be written out to the stream.
     */
    public void flush() throws IOException {
    	if(bufferWritten > 0) {
	        flush(buffer, 0, bufferWritten);
	        bufferWritten = 0;
    	}
    }
    
    private void ensureBufferSpace(int bytes) throws IOException {
    	int bufferCapacity = buffer.length - bufferWritten;
    	if(bytes > bufferCapacity)
    		flush();
    }
    
    private void writeToBuffer(int b) {
    	buffer[bufferWritten] = (byte) (b & 0xFF);
    	bufferWritten++;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * b to the underlying output stream.
     */
    public void write(int b) throws IOException {
    	ensureBufferSpace(1);
        writeToBuffer(b);
        written += 1;
    }

    /**
     * Writes len bytes from the specified byte array
     * starting at offset off to the underlying output stream.
     */
    public void write(byte b[], int off, int len) throws IOException{
        //ensureBufferSpace()
    	//out.write(b, off, len);
    	if(len > buffer.length) {
    		// Write directly to file.
    		// First flush whatever is in the buffer.
    		flush();
    		// Then write the byte array.
    		flush(b, off, len);
    		written += len;
    	}else {
    		// Write to buffer.
    		ensureBufferSpace(len);
    		System.arraycopy(b, off, buffer, bufferWritten, len);
    		bufferWritten += len;
    		written += len;
    	}
    }

    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public final void writeByte(int v) throws IOException {
        write(v);
    }

    public final void writeShort(int v) throws IOException {
    	ensureBufferSpace(2);
        writeToBuffer((v >>> 0) & 0xFF);
        writeToBuffer((v >>> 8) & 0xFF);
        written += 2;
    }
    
    public final void writeChar(int v) throws IOException {
    	ensureBufferSpace(2);
        writeToBuffer((v >>> 0) & 0xFF);
        writeToBuffer((v >>> 8) & 0xFF);
        written += 2;
    }

    public final void writeInt(int v) throws IOException {
    	ensureBufferSpace(4);
        writeToBuffer((v >>>  0) & 0xFF);
        writeToBuffer((v >>>  8) & 0xFF);
        writeToBuffer((v >>> 16) & 0xFF);
        writeToBuffer((v >>> 24) & 0xFF);
        written += 4;
    }

    public final void writeLong(long v) throws IOException {
    	ensureBufferSpace(8);
    	writeToBuffer((int) ((v >>>  0) & 0xFF));
    	writeToBuffer((int) ((v >>>  8) & 0xFF));
    	writeToBuffer((int) ((v >>> 16) & 0xFF));
    	writeToBuffer((int) ((v >>> 24) & 0xFF));
    	writeToBuffer((int) ((v >>> 32) & 0xFF));
    	writeToBuffer((int) ((v >>> 40) & 0xFF));
    	writeToBuffer((int) ((v >>> 48) & 0xFF));
    	writeToBuffer((int) ((v >>> 56) & 0xFF));
        written += 8;
    }
    
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }
    
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }
    
    public final void writeUTF(String str) throws IOException {
    	writeUTF(str, true);
    }

    private final void writeUTF(String str, boolean includeLength) throws IOException {
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

        if(includeLength) {
	        bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
	        bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        }
        
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
        
        write(bytearr, 0, count);
    }

    public final long size() {
        return written;
    }

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public void writeChars(String s) throws IOException {
		throw new RuntimeException("Not Implemented");
	}
	
	public void write(String s) throws IOException{
		// We start writing the string assuming
		// that it's ASCII.
		// If we encounter a non-ascii character,
		// then we backtrack and switch over to a
		// UTF-8 version of this function.
		int numChars = s.length();
		ensureBufferSpace(numChars);
		int origBufferWritten = bufferWritten;
		for(int i = 0; i < numChars; ++i) {
			int val = ((int) s.charAt(i)) & 0xFFFF;
			if(val >= 0x7F) {
				// Non-ascii
				bufferWritten = origBufferWritten;
				writeToUTF8(s);
				return;
			}
			
			writeToBuffer(val);
		}
		written += numChars;
	}
	
	private void writeToUTF8(String s) throws IOException{
		byte[] bytes = getBytesFromString(s);
		write(bytes, 0, bytes.length);
	}
	
	private static byte[] getBytesFromString(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}
	
}

