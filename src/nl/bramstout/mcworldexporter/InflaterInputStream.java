package nl.bramstout.mcworldexporter;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

public class InflaterInputStream extends FilterInputStream {
	
	private static final ThreadLocal<byte[]> BUFFERS = new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			return new byte[1024*1024*16];
		}
	};

	protected Inflater inf;
	protected byte[] buf;
	protected int len;
    private boolean closed = false;
    private boolean reachEOF = false;
    private byte[] singleByteBuf = new byte[1];
    
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }
    
    public InflaterInputStream(InputStream in) {
        super(in);
        if (in == null) {
            throw new NullPointerException();
        }
        this.inf = new Inflater();
        
        buf = BUFFERS.get();
    }
    
    public int read() throws IOException {
        ensureOpen();
        return read(singleByteBuf, 0, 1) == -1 ? -1 : Byte.toUnsignedInt(singleByteBuf[0]);
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        try {
            int n;
            do {
                if (inf.finished() || inf.needsDictionary()) {
                    reachEOF = true;
                    return -1;
                }
                if (inf.needsInput()/* && !inf.hasPendingOutput()*/) {
                    // Even if needsInput() is true, the native inflater may have some
                    // buffered data which couldn't fit in to the output buffer during the
                    // last call to inflate. Consume that buffered data first before calling
                    // fill() to avoid an EOF error if no more input is available and the
                    // next call to inflate will finish the inflation.
                    fill();
                }
            } while ((n = inf.inflate(b, off, len)) == 0);
            return n;
        } catch (DataFormatException e) {
            String s = e.getMessage();
            throw new ZipException(s != null ? s : "Invalid ZLIB data format");
        }
    }
    
    public int available() throws IOException {
        ensureOpen();
        if (reachEOF) {
            return 0;
        } else if (inf.finished()) {
            // the end of the compressed data stream has been reached
            reachEOF = true;
            return 0;
        } else {
            return 1;
        }
    }
    
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int max = (int)Math.min(n, Integer.MAX_VALUE);
        int total = 0;
        byte[] b = new byte[Math.min(max, 512)];
        while (total < max) {
            int len = max - total;
            if (len > b.length) {
                len = b.length;
            }
            len = read(b, 0, len);
            if (len == -1) {
                reachEOF = true;
                break;
            }
            total += len;
        }
        return total;
    }
    
    public void close() throws IOException {
        if (!closed) {
            inf.end();
            in.close();
            closed = true;
        }
    }
    
    protected void fill() throws IOException {
        ensureOpen();
        len = in.read(buf, 0, buf.length);
        if (len == -1) {
            throw new EOFException("Unexpected end of ZLIB input stream");
        }
        inf.setInput(buf, 0, len);
    }
    
    public boolean markSupported() {
        return false;
    }
    
    @Override
    public void mark(int readlimit) {
    }
    
    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
	
}
