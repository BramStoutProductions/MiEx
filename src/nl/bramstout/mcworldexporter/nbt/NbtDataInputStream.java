package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

public class NbtDataInputStream implements DataInput{

	private InputStream in;
	
	public NbtDataInputStream(InputStream in) {
		this.in = in;
	}
	
	public void close() throws IOException{
		in.close();
	}
	
	@Override
	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		while(len > 0) {
			int read = in.read(b, off, len);
			off += read;
			len -= read;
		}
	}

	@Override
	public int skipBytes(int n) throws IOException {
		while(n > 0) {
			n -= in.skip(n);
		}
		return n;
	}

	@Override
	public boolean readBoolean() throws IOException {
		int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
	}

	@Override
	public byte readByte() throws IOException {
		int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);
	}

	@Override
	public int readUnsignedByte() throws IOException {
		int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
	}

	@Override
	public short readShort() throws IOException {
		int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch1 << 8) + (ch2 << 0));
	}

	@Override
	public int readUnsignedShort() throws IOException {
		int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
	}

	@Override
	public char readChar() throws IOException {
		int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch1 << 8) + (ch2 << 0));
	}

	@Override
	public int readInt() throws IOException {
		int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	private byte readBuffer[] = new byte[8];

	@Override
	public long readLong() throws IOException {
		readFully(readBuffer, 0, 8);
        return (((long)readBuffer[0] << 56) +
                ((long)(readBuffer[1] & 255) << 48) +
                ((long)(readBuffer[2] & 255) << 40) +
                ((long)(readBuffer[3] & 255) << 32) +
                ((long)(readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) <<  8) +
                ((readBuffer[7] & 255) <<  0));
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
	
	private byte[] bytearr = new byte[80];
	private char[] chararr = new char[80];
	
	private static StringSet internedStrings = new StringSet();
	private StringSet localInternedStrings = new StringSet();
	

	@Override
	public String readUTF() throws IOException {
		int utflen = readUnsignedShort();
        if (bytearr.length < utflen){
            bytearr = new byte[utflen*2];
            chararr = new char[utflen*2];
        }

        int c, char2, char3;
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
            switch (c >> 4) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                /* 0xxxxxxx*/
                count++;
                chararr[chararr_count++]=(char)c;
                break;
            case 12:
            case 13:
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
                break;
            case 14:
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
                break;
            default:
                /* 10xx xxxx,  1111 xxxx */
                throw new UTFDataFormatException(
                    "malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        String str = localInternedStrings.getOrNull(chararr, chararr_count);
        if(str != null)
        	return str;
        // It's not in our local interned strings, so check it in the global one.
        synchronized (internedStrings) {
        	str = internedStrings.getOrNull(chararr, chararr_count);
        	if(str == null) {
        		str = new String(chararr, 0, chararr_count);
        		internedStrings.put(str);
        	}
        }
        localInternedStrings.put(str);
        return str;
	}

}
