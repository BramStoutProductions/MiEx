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

package nl.bramstout.mcworldexporter.launcher;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;

public class ResourcePackSourceCollector {
	
	public static interface CollectorCallback{
		
		public void addSources(List<ResourcePackSource> sources);
		
	}
	
	private List<ResourcePackSource> sources;
	private AtomicBoolean callbackRequested;
	private AtomicBoolean pause;
	private Timer timer;
	private AtomicInteger totalSources;
	private AtomicInteger processedSources;
	
	public ResourcePackSourceCollector(CollectorCallback callback) {
		sources = new ArrayList<ResourcePackSource>();
		callbackRequested = new AtomicBoolean(false);
		pause = new AtomicBoolean(false);
		totalSources = new AtomicInteger(0);
		processedSources = new AtomicInteger(0);
		timer = new Timer(50, (ActionEvent e)->{
			if(pause.get()) {
				if(callbackRequested.get()) {
					timer.restart();
				}
				return;
			}
			
			boolean stopTimer = false;
			List<ResourcePackSource> oldSources = null;
			synchronized(sources) {
				oldSources = sources;
				sources = new ArrayList<ResourcePackSource>();
				if(oldSources.size() > 50) {
					// Max 50 items per invocation
					sources.addAll(oldSources.subList(50, oldSources.size()));
					oldSources = oldSources.subList(0, 50);
				}else {
					// No more items, so stop the timer.
					stopTimer = true;
					callbackRequested.set(false);
				}
			}
			callback.addSources(oldSources);
			processedSources.addAndGet(oldSources.size());
			if(!stopTimer)
				timer.restart();
		});
		timer.setRepeats(false);
	}
	
	public void addSource(ResourcePackSource source) {
		synchronized(sources) {
			totalSources.addAndGet(1);
			sources.add(source);
			runCallback();
		}
	}
	
	public void pause() {
		pause.set(true);
	}
	
	public void unpause() {
		pause.set(false);
		if(!sources.isEmpty()) {
			runCallback();
		}
	}
	
	private void runCallback() {
		if(!callbackRequested.get() && !pause.get()) {
			callbackRequested.set(true);
			SwingUtilities.invokeLater(()->{
				timer.restart();
			});
		}
	}
	
	public void resetProgress() {
		synchronized(sources) {
			totalSources.set(sources.size());
			processedSources.set(0);
		}
	}
	
	public float getProgress() {
		return (float) (((double) processedSources.get()) / ((double) totalSources.get()));
	}
	
	public boolean isProcessing() {
		return callbackRequested.get() || !sources.isEmpty() || totalSources.get() != processedSources.get();
	}

}
