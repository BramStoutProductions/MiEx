package nl.bramstout.mcworldexporter.parallel;

import java.util.concurrent.atomic.AtomicInteger;

import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;

public class Async {
	
	public static class AsyncGroup{
		
		private AtomicInteger taskCounter = new AtomicInteger(0);
		
		public Task runTask(Runnable runnable) {
			Task task = new Task(runnable);
			task.setTaskCounter(taskCounter);
			threadPool.submit(task);
			return task;
		}
		
		public boolean isDone() {
			return taskCounter.get() <= 0;
		}
		
		public void waitUntilDone() {
			int counter = 0;
			while(!isDone()) {
				counter++;
				if(counter < 20) {
					for(int i = 0; i < (counter*counter); ++i)
						Thread.yield();
				}else {
					try {
						Thread.sleep(5);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		
	}
	
	private static ThreadPool threadPool = new ThreadPool("AsyncPool", 1);
	
	public static Task runTask(Runnable runnable) {
		return threadPool.submit(runnable);
	}
	
}
