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

package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourTimerFlag1 extends AIComponent{

	/**
	 * The minimum time in seconds before this behaviour can start again.
	 */
	public float minCooldown;
	/**
	 * The maximum time in seconds before this behaviour can start again.
	 */
	public float maxCooldown;
	/**
	 * The minimum time in seconds that this timer will run.
	 */
	public float minDuration;
	/**
	 * The maximum time in seconds that this timer will run.
	 */
	public float maxDuration;
	/**
	 * The event to run when starting this timer.
	 */
	public EntityEvent onStart;
	/**
	 * The event to run when stopping this timer either because it ran out
	 * or it got interrupted.
	 */
	public EntityEvent onEnd;
	
	private float endCooldown;
	private float endTimer;
	
	public AIComponentBehaviourTimerFlag1(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		endCooldown = -1f;
		endTimer = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(time < endCooldown) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag1", ((byte) 0)));
			return false;
		}
		if(endTimer == -1f) {
			// Let's start the timer.
			endTimer = entity.getRandom().nextFloat() * (maxDuration - minDuration) + minDuration + time;
			onStart.fireEvent(entity);
		}else {
			if(time >= endTimer) {
				// Timer ended.
				endTimer = -1f;
				endCooldown = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
				onEnd.fireEvent(entity);
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag1", ((byte) 0)));
				return false;
			}
			// Timer still going.
		}
		
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag1", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(endTimer != -1f) {
			onEnd.fireEvent(entity);
		}
		endCooldown = -1f;
		endTimer = -1f;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag1", ((byte) 0)));
	}

}
