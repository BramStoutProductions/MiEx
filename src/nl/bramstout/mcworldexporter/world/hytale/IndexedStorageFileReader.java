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
		
		//ByteBuffer buffer = ByteBuffer.allocate(this.segmentSize);
		//this.fileChannel.read(buffer, position);
		MappedByteBuffer buffer = this.fileChannel.map(MapMode.READ_ONLY, position, this.segmentSize);
		
		int decompressedSize = buffer.getInt(0);
		int compressedSize = buffer.getInt(4);
		int fullSize = compressedSize + 8;
		if(fullSize > this.segmentSize) {
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
