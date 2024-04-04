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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {

	private List<Thread> threads;
	private Queue<Task> queue;

	public ThreadPool() {
		threads = new ArrayList<Thread>();
		queue = new Queue<Task>();

		for (int i = 0; i < Runtime.getRuntime().availableProcessors() - 2; i++) {
			Thread thread = new Thread(new Worker(this));
			thread.setName("Threadpool-" + i);
			thread.start();
			this.threads.add(thread);
		}
	}

	public Task submit(Runnable runnable) {
		Task task = new Task(runnable);
		queue.push(task);
		return task;
	}

	public static class Task {
		public Runnable runnable;
		private AtomicInteger future;

		public Task(Runnable runnable) {
			this.runnable = runnable;
			this.future = new AtomicInteger();
		}

		public void done() {
			this.future.addAndGet(1);
		}

		public void waitUntilTaskIsDone() {
			while(this.future.get() == 0) {
				try {
					Thread.sleep(5);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	private static class Worker implements Runnable {

		private ThreadPool pool;

		public Worker(ThreadPool pool) {
			this.pool = pool;
		}

		@Override
		public void run() {
			int counter = 0;
			while (true) {
				Thread.yield();
				Task task = pool.queue.pop();
				if (task == null) {
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
					task.runnable.run();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				task.done();
			}
		}

	}

}
