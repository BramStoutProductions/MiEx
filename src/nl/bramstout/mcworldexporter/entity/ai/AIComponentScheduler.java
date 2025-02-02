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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentScheduler extends AIComponent{

	/**
	 * The minimum delay in seconds between schedule ticks.
	 */
	public float minDelay;
	/**
	 * The maximum delay in seconds between schedule ticks.
	 */
	public float maxDelay;
	/**
	 * List of events with their filters to trigger on schedule ticks.
	 */
	public List<EntityEvent> scheduledEvents;
	
	private float nextTick;
	
	public AIComponentScheduler(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		scheduledEvents = new ArrayList<EntityEvent>();
		nextTick = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(time >= nextTick) {
			if(nextTick != -1f) {
				for(EntityEvent event : scheduledEvents)
					event.fireEvent(entity);
			}
			nextTick = entity.getRandom().nextFloat() * (maxDelay - minDelay) + minDelay + time;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextTick = -1f;
	}

}
