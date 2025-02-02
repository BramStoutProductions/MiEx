package nl.bramstout.mcworldexporter.parallel;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class BackgroundThread {
	
	private static LinkedList<Runnable> tasks = new LinkedList<Runnable>();
	private static AtomicInteger isBusy = new AtomicInteger();
	
	private static Thread backgroundThread = new Thread(new Runnable() {

		@Override
		public void run() {
			int counter = 0;
			while(true) {
				Thread.yield();
				Runnable task = null;
				synchronized(tasks) {
					if(!tasks.isEmpty())
						task = tasks.removeFirst();
					isBusy.set(1);
				}
				if (task == null) {
					isBusy.set(0);
					Thread.yield();
					counter++;
					if ((counter & 15) == 15) {
						try {
							Thread.sleep(1);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					if (counter >= 100) {
						counter = 0;
						try {
							Thread.sleep(50);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					continue;
				}
				try {
					task.run();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}, "Background Thread");
	
	static {
		backgroundThread.setDaemon(true);
		backgroundThread.start();
	}
	
	public static void runInBackground(Runnable task) {
		synchronized(tasks) {
			tasks.push(task);
		}
	}
	
	public static void waitUntilDoneWithBackgroundTasks() {
		int counter = 0;
		while(true) {
			boolean isEmpty = true;
			synchronized(tasks) {
				isEmpty = tasks.isEmpty();
			}
			if(isEmpty && isBusy.get() == 0)
				return;
			
			Thread.yield();
			counter++;
			if ((counter & 15) == 15) {
				try {
					Thread.sleep(1);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (counter >= 100) {
				counter = 0;
				try {
					Thread.sleep(50);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
}
