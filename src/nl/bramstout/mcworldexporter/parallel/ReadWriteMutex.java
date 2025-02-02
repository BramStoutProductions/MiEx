package nl.bramstout.mcworldexporter.parallel;

import java.util.concurrent.atomic.AtomicInteger;

public class ReadWriteMutex {
	
	private AtomicInteger lock;
	
	public ReadWriteMutex() {
		lock = new AtomicInteger(0);
	}
	
	public void acquireRead() {
		while(true) {
			int val = lock.get();
			if(val < 0) { // Write lock has been acquired
				Thread.yield();
				continue;
			}
			int newVal = val + 1;
			if(!lock.compareAndSet(val, newVal)) { // Another thread changed it first.
				continue;
			}
			break;
		}
	}
	
	public void acquireWrite() {
		while(true) {
			int val = lock.get();
			if(val != 0) { // Write or read lock already acquired
				Thread.yield();
				continue;
			}
			int newVal = -1;
			if(!lock.compareAndSet(val, newVal)) { // Another thread changed it first
				continue;
			}
			break;
		}
	}
	
	public void releaseRead() {
		lock.decrementAndGet();
	}
	
	public void releaseWrite() {
		lock.set(0);
	}
	
}
