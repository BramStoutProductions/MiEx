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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourSwimWander extends AIComponent{

	/**
	 * Percent chance to start wandering when not path finding. 1 = 100%
	 */
	public float interval;
	/**
	 * Amount of time in seconds to wander after wandering behaviour was successfully started.
	 */
	public float wanderTime;
	/**
	 * Distance to look ahead for obstacle avoidance, while wandering.
	 */
	public float lookAhead;
	
	private boolean isWandering;
	private float stopWandering;
	
	public AIComponentBehaviourSwimWander(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isWandering) {
			if(time >= stopWandering) {
				isWandering = false;
				entity.getAI().target = null;
				return false;
			}
			
			if(entity.getAI().target == null)
				entity.getAI().target = randomPosition(entity, posX, posY, posZ);
			else {
				float targetX = entity.getAI().target.getPosX(time);
				float targetY = entity.getAI().target.getPosY(time);
				float targetZ = entity.getAI().target.getPosZ(time);
				float distanceSquared = (targetX - posX) * (targetX - posX) + 
										(targetY - posY) * (targetY - posY) + 
										(targetZ - posZ) * (targetZ - posZ);
				if(distanceSquared < 0.5f)
					entity.getAI().target = randomPosition(entity, posX, posY, posZ);
			}
			return true;
		}
		
		if(!EntityUtil.isInLiquid(entity, posX, posY, posZ))
			return false;
		if(entity.getAI().target != null)
			return false;
		if(!EntityUtil.randomChance(entity, interval, deltaTime))
			return false;
		
		isWandering = true;
		stopWandering = time + wanderTime;
		entity.getAI().target = randomPosition(entity, posX, posY, posZ);
		return true;
	}
	
	private EntityTargetPosition randomPosition(Entity entity, float posX, float posY, float posZ) {
		for(int attempt = 0; attempt < 100; ++attempt) {
			float dx = entity.getRandom().nextFloat() * 2f - 1f;
			float dy = (entity.getRandom().nextFloat() * 2f - 1f) * 0.5f;
			float dz = entity.getRandom().nextFloat() * 2f - 1f;
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			dx /= length;
			dy /= length;
			dz /= length;
			length = entity.getRandom().nextFloat() * lookAhead;
			dx *= length;
			dy *= length;
			dz *= length;
			
			float targetX = posX + dx;
			float targetY = posY + dy;
			float targetZ = posZ + dz;
			if(EntityUtil.isInLiquid(entity, targetX, targetY, targetZ)) {
				return new EntityTargetPosition(targetX, targetY, targetZ);
			}
		}
		return null;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isWandering = false;
	}

}
