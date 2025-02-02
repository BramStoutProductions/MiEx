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

public class AIComponentBehaviourLookAtTarget extends AIComponent{

	/**
	 * The angle in degrees that the mob can see in the X-axis
	 */
	public float angleOfViewHorizontal;
	/**
	 * The angle in degrees that the mob can see in the Y axis.
	 */
	public float angleOfViewVertical;
	/**
	 * The maximum distance for the entity to look at it.
	 */
	public float lookDistance;
	/**
	 * The minimum amount of time this entity will look at another entity.
	 */
	public float minLookTime;
	/**
	 * The maximum amount of time this entity will look at another entity.
	 */
	public float maxLookTime;
	/**
	 * The probability per tick that this entity will look at another entity.
	 * A value of 1.0 is 100%
	 */
	public float probability;
	
	private float stopLooking;
	private boolean isLooking;
	
	public AIComponentBehaviourLookAtTarget(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		stopLooking = 0f;
		isLooking = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isLooking) {
			if(entity.getAI().target == null) {
				isLooking = false;
				return false;
			}
			if(time >= stopLooking) {
				isLooking = false;
				entity.getAI().target.move = true;
				return false;
			}
			return true;
		}
		
		if(Math.random() > probability) {
			return false; // Not going to look at another entity this tick
		}
		
		if(entity.getAI().target == null) {
			return false; // No target to look at.
		}
		
		isLooking = true;
		entity.getAI().target.move = false;
		entity.getAI().target.look = true;
		entity.getAI().target.minLookYaw = -angleOfViewHorizontal / 2f;
		entity.getAI().target.minLookPitch = -angleOfViewVertical / 2f;
		entity.getAI().target.maxLookYaw = angleOfViewHorizontal / 2f;
		entity.getAI().target.maxLookPitch = angleOfViewVertical / 2f;
		stopLooking = ((float) Math.random()) * (maxLookTime - minLookTime) + minLookTime + time;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isLooking = false;
	}
	
}
