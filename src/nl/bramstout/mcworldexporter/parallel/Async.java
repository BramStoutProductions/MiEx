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
