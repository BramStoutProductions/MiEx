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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;

import io.airlift.compress.v3.zstd.ZstdJavaDecompressor;

public class IndexedStorageFileReader {
	
	private static byte[] MAGIC_NUMBER = "HytaleIndexedStorage".getBytes();
	
	private FileChannel fileChannel;
	private long fileSize;
	private int version;
	private int segmentSize;
	private long[] blobOffsets;
	
	public IndexedStorageFileReader(File file) throws IOException {
		this.fileChannel = null;
		this.version = 0;
		this.blobOffsets = new long[0];
		
		if(!file.exists())
			return;
		
		this.fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
		this.fileSize = fileChannel.size();
		ByteBuffer headerBuffer = ByteBuffer.allocate(32);
		this.fileChannel.read(headerBuffer, 0);
		
		for(int i = 0; i < MAGIC_NUMBER.length; ++i) {
			if(headerBuffer.get(i) != MAGIC_NUMBER[i]) {
				this.fileChannel.close();
				throw new IOException("Invalid file format");
			}
		}
		
		this.version = headerBuffer.getInt(20);
		int blobCount = headerBuffer.getInt(24);
		this.segmentSize = headerBuffer.getInt(28);
		
		if(this.version != 1) {
			this.fileChannel.close();
			throw new IOException("Invalid version");
		}
		
		headerBuffer = ByteBuffer.allocate(blobCount * 4);
		this.fileChannel.read(headerBuffer, 32);
		
		long segmentOffset = 32 + (blobCount * 4);
		this.blobOffsets = new long[blobCount];
		for(int i = 0; i < blobCount; ++i) {
			this.blobOffsets[i] = headerBuffer.getInt(i * 4);
			if(this.blobOffsets[i] != 0)
				this.blobOffsets[i] = (this.blobOffsets[i] - 1) * this.segmentSize + segmentOffset;
		}
	}
	
	public void close() throws IOException {
		this.fileChannel.close();
	}
	
	public boolean isOpen() {
		return this.fileChannel.isOpen();
	}
	
	public byte[] getBlob(int index) throws IOException {
		if(index < 0 || index >= this.blobOffsets.length)
			return null;
		long position = this.blobOffsets[index];
		if(position <= 0)
			return null;
		
		if((position + this.segmentSize) >= this.fileSize) {
			//throw new IOException("EOF");
			return null;
		}
		
		//ByteBuffer buffer = ByteBuffer.allocate(this.segmentSize);
		//this.fileChannel.read(buffer, position);
		MappedByteBuffer buffer = this.fileChannel.map(MapMode.READ_ONLY, position, this.segmentSize);
		
		int decompressedSize = buffer.getInt(0);
		int compressedSize = buffer.getInt(4);
		int fullSize = compressedSize + 8;
		if(fullSize > this.segmentSize) {
			if((position + fullSize) >= this.fileSize) {
				//throw new IOException("EOF");
				return null;
			}
			
			//buffer = ByteBuffer.allocate(fullSize);
			//this.fileChannel.read(buffer, position);
			buffer = this.fileChannel.map(MapMode.READ_ONLY, position, fullSize);
		}
		buffer.position(8);
		
		byte[] byteBuffer = new byte[compressedSize];
		buffer.get(byteBuffer, 0, compressedSize);
		
		byte[] outBuffer = new byte[decompressedSize];
		ZstdJavaDecompressor decompressor = new ZstdJavaDecompressor();
		decompressor.decompress(byteBuffer, 0, compressedSize, outBuffer, 0, decompressedSize);
		
		return outBuffer;
	}
	
	public boolean hasBlob(int index) {
		if(index < 0 || index >= this.blobOffsets.length)
			return false;
		return this.blobOffsets[index] > 0;
	}
	
}
