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

package nl.bramstout.mcworldexporter.entity.ai;

import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentTimer extends AIComponent{

	/**
	 * If true, the timer will restart every time after it initiates.
	 */
	public boolean looping;
	/**
	 * If true, the amount of time on the timer will be random between
	 * the min and max values specified in time.
	 */
	public boolean randomInterval;
	/**
	 * A list of times in seconds that can be picked from
	 * before initiating the event. If this list isn't null,
	 * then it'll ignore minTime and maxTime.
	 */
	public List<Float> randomTimeChoices;
	/**
	 * A list of weights for the randomTimeChoices.
	 */
	public List<Float> randomTimeChoicesWeights;
	/**
	 * The minimum random time used to initiate this timer.
	 */
	public float minTime;
	/**
	 * The maximum random time used to initiate this timer
	 */
	public float maxTime;
	/**
	 * The event to trigger when the timer is done.
	 */
	public EntityEvent timerDoneEvent;
	
	private float nextTimerTick;
	
	public AIComponentTimer(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		nextTimerTick = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(time >= nextTimerTick) {
			if(nextTimerTick != -1f) {
				timerDoneEvent.fireEvent(entity);
				
				if(!looping) {
					// Disable timer
					nextTimerTick = Float.MAX_VALUE;
					return true;
				}
			}
			
			// Select a new nextTimerTick
			if(randomTimeChoices != null && randomTimeChoicesWeights != null) {
				float totalWeight = 0f;
				for(Float f : randomTimeChoicesWeights)
					totalWeight += f.floatValue();
				float weight = entity.getRandom().nextFloat() * totalWeight;
				int i = 0;
				for(; i < Math.min(randomTimeChoices.size(), randomTimeChoicesWeights.size()); ++i) {
					if(weight <= 0f)
						break;
					weight -= randomTimeChoicesWeights.get(i).floatValue();
				}
				float delay = randomTimeChoices.get(Math.max(i, 0)).floatValue();
				nextTimerTick = time + delay;
			}else {
				float delay = minTime;
				if(randomInterval)
					delay = entity.getRandom().nextFloat() * (maxTime - minTime) + minTime;
				nextTimerTick = time + delay;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextTimerTick = -1f;
	}

}
