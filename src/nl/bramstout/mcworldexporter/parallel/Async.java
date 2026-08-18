package nl.bramstout.mcworldexporter.parallel;

import java.util.concurrent.atomic.AtomicInteger;

import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;

public class Async {
	
	public static class AsyncGroup{
		
		private AtomicInteger taskCounter = new AtomicInteger(0);
		private AtomicInteger numTasks = new AtomicInteger(0);
		
		public Task runTask(Runnable runnable) {
			Task task = new Task(runnable);
			task.setTaskCounter(taskCounter);
			int taskCounterVal = taskCounter.get();
			if(taskCounterVal > numTasks.get())
				numTasks.set(taskCounterVal);
			threadPool.submit(task);
			return task;
		}
		
		public float progress() {
			int taskCounterVal = taskCounter.get();
			int numTasksVal = numTasks.get();
			int tasksCompleted = numTasksVal - taskCounterVal;
			tasksCompleted = Math.min(Math.max(tasksCompleted, 0), numTasksVal);
			return (float) (((double) tasksCompleted) / ((double) numTasksVal));
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
