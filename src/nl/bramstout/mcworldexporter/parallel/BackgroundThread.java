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
