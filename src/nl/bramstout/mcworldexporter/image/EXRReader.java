package nl.bramstout.mcworldexporter.image;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EXRReader {

	private InputStream is;
	private long bytesRead;
	
	public EXRReader(InputStream is) throws IOException {
		this.is = is;
		int val1 = readInt();
		if(val1 != 20000630)
			throw new RuntimeException("Unsupported EXR");
		int val2 = readInt();
		if(val2 != 2)
			throw new RuntimeException("Unsupported EXR");
	}
	
	public long getBytesRead() {
		return bytesRead;
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
	
	public List<Channel> readChannelsAttribute() throws IOException{	
		List<Channel> channels = new ArrayList<Channel>();
		while(true) {
			String channelName = readNullTerminatedString();
			if(channelName.isEmpty())
				break;
			int channelTypeId = readInt();
			DataType channelType = DataType.UINT;
			switch(channelTypeId) {
			case 0:
				channelType = DataType.UINT;
				break;
			case 1:
				channelType = DataType.HALF;
				break;
			case 2:
				channelType = DataType.FLOAT;
				break;
			}
			@SuppressWarnings("unused")
			char pLinear = readChar();
			@SuppressWarnings("unused")
			char char1 = readChar();
			@SuppressWarnings("unused")
			char char2 = readChar();
			@SuppressWarnings("unused")
			char char3 = readChar();
			@SuppressWarnings("unused")
			int xSampling = readInt();
			@SuppressWarnings("unused")
			int ySampling = readInt();
			channels.add(new Channel(channelName, channelType));
		}
		return channels;
	}
	
	public String readAttributeName() throws IOException{
		return readNullTerminatedString();
	}
	
	public String readAttributeType() throws IOException{
		return readNullTerminatedString();
	}
	
	public int readAttributeSize() throws IOException{
		return readInt();
	}
	
	/*public void writeAttributeStart(String name, String type, int size) throws IOException{
		writeNullTerminatedString(name);
		writeNullTerminatedString(type);
		writeInt(size);
	}*/
	
	public int[] readBox2i() throws IOException{
		return new int[] {	readInt(),
							readInt(),
							readInt(),
							readInt()};
	}
	
	public float[] writeBoxif() throws IOException{
		return new float[] {	readFloat(),
								readFloat(),
								readFloat(),
								readFloat()};
	}
	
	public int readCompression() throws IOException{
		return readChar();
	}
	
	public LineOrder readLineOrder() throws IOException{
		char lineOrder = readChar();
		switch(lineOrder) {
		case 0:
			return LineOrder.INCREASING_Y;
		case 1:
			return LineOrder.DECREASING_Y;
		case 2:
			return LineOrder.RANDOM_Y;
		default:
			return LineOrder.INCREASING_Y;
		}
	}
	
	
	
	
	public String readNullTerminatedString() throws IOException{
		char[] buffer = new char[16];
		int length = 0;
		while(true) {
			char c = readChar();
			if(c == 0)
				break;
			if(length == buffer.length)
				buffer = Arrays.copyOf(buffer, length * 2);
			buffer[length] = c;
			length++;
		}
		if(length == 0)
			return "";
		return new String(buffer, 0, length);
	}
	
	public char readChar() throws IOException {
		char val = (char) (is.read() & 0xFF);
		bytesRead += 1;
		return val;
	}
	
	public short readShort() throws IOException {
		int val1 = is.read() & 0xFF;
		int val2 = is.read() & 0xFF;
		bytesRead += 2;
		return (short) (val1 | (val2 << 8));
	}
	
	public int readInt() throws IOException {
		int val1 = is.read() & 0xFF;
		int val2 = is.read() & 0xFF;
		int val3 = is.read() & 0xFF;
		int val4 = is.read() & 0xFF;
		bytesRead += 4;
		return (int) (val1 | (val2 << 8) | (val3 << 16) | (val4 << 24));
	}
	
	public long readLong() throws IOException {
		long val1 = is.read() & 0xFF;
		long val2 = is.read() & 0xFF;
		long val3 = is.read() & 0xFF;
		long val4 = is.read() & 0xFF;
		long val5 = is.read() & 0xFF;
		long val6 = is.read() & 0xFF;
		long val7 = is.read() & 0xFF;
		long val8 = is.read() & 0xFF;
		bytesRead += 8;
		return (long) (val1 | (val2 << 8) | (val3 << 16) | (val4 << 24) | 
						(val5 << 32) | (val6 << 40) | (val7 << 48) | (val8 << 56));
	}
	
	public float readFloat() throws IOException{
		return Float.intBitsToFloat(readInt());
	}
	
	public double readDouble() throws IOException{
		return Double.longBitsToDouble(readLong());
	}
	
	public float readHalf() throws IOException{
		int val1 = is.read() & 0xFF;
		int val2 = is.read() & 0xFF;
		int halfBits = val1 | (val2 << 8);
		bytesRead += 2;
		return halfToFloat(halfBits);
	}
	
	private float halfToFloat(int h) {
		// Conversion code adapted from https://github.com/AcademySoftwareFoundation/Imath/blob/main/src/Imath/half.h#L285
		//
		// SPDX-License-Identifier: BSD-3-Clause
		// Copyright Contributors to the OpenEXR Project.
		//

		//
		// Primary original authors:
		//		     Florian Kainz <kainz@ilm.com>
		//		     Rod Bogart <rgb@ilm.com>
		//
		
		
	    int v;
	    // this code would be clearer, although it does appear to be faster
	    // (1.06 vs 1.08 ns/call) to avoid the constants and just do 4
	    // shifts.
	    //
	    int hexpmant = ((int) (h) << 17) >>> 4;
	    v = ((int) (h >>> 15)) << 31;

	    // the likely really does help if most of your numbers are "normal" half numbers
	    if ((hexpmant >= 0x00800000))
	    {
	        v |= hexpmant;
	        // either we are a normal number, in which case add in the bias difference
	        // otherwise make sure all exponent bits are set
	        if ((hexpmant < 0x0f800000))
	            v += 0x38000000;
	        else
	            v |= 0x7f800000;
	    }
	    else if (hexpmant != 0)
	    {
	        // exponent is 0 because we're denormal, don't have to extract
	        // the mantissa, can just use as is
	        //
	        //
	        // other compilers may provide count-leading-zeros primitives,
	        // but we need the community to inform us of the variants
	        int lc;
	        lc = 0;
	        while (0 == ((hexpmant << lc) & 0x80000000))
	            ++lc;
	
	        lc -= 8;
	        // so nominally we want to remove that extra bit we shifted
	        // up, but we are going to add that bit back in, then subtract
	        // from it with the 0x38800000 - (lc << 23)....
	        //
	        // by combining, this allows us to skip the & operation (and
	        // remove a constant)
	        //
	        // hexpmant &= ~0x00800000;
	        v |= 0x38800000;
	        // lc is now x, where the desired exponent is then
	        // -14 - lc
	        // + 127 -> new exponent
	        v |= (hexpmant << lc);
	        v -= (lc << 23);
	    }
	    return Float.intBitsToFloat(v);
	}
	
}
