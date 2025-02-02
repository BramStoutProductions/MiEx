package nl.bramstout.mcworldexporter;

public class Random extends java.util.Random{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Random() {
		super();
	}
	
	public Random(long seed) {
		super(seed);
	}
	
	public int nextInt(int min, int max) {
		int range = max - min;
		int rand = nextInt(range);
		return rand + min;
	}
	
	public float nextFloat(float min, float max) {
		float range = max - min;
		return nextFloat() * range + min;
	}
	
	public long nextUnsignedLong() {
		return ((long)(next(31)) << 32) | (((long) next(16)) << 16) | ((long) next(16));
	}
	
	public long nextLong(long bound) {
		long r = nextUnsignedLong();
		long m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = r & m;
        else {
            for (long u = r;
                 u - (r = u % bound) + m < 0;
                 u = nextUnsignedLong())
                ;
        }
        return r;
	}
	
	public long nextLong(long min, long max) {
		long range = max - min;
		long rand = nextLong(range);
		return rand + min;
	}

}
