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

public class AIComponentBehaviourRandomLookAround extends AIComponent{

	/**
	 * The probability of randomly looking around.
	 */
	public float probability;
	/**
	 * The minimum amount of time in ticks to look in a single direction.
	 */
	public float minLookTime;
	/**
	 * The maximum amount of time in ticks to look in a single direction.
	 */
	public float maxLookTime;
	/**
	 * The angle in degrees that an entity can see in the Y-axis.
	 */
	public float angleOfViewHorizontal;
	/**
	 * The angle in degrees that an entity can see in the X-axis.
	 */
	public float angleOfViewVertical;
	
	private boolean isLooking;
	private float stopLooking;
	
	public AIComponentBehaviourRandomLookAround(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isLooking = false;
		stopLooking = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isLooking) {
			if(time >= stopLooking) {
				isLooking = false;
				entity.getAI().target = null;
				return false;
			}
			return true;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime) && !forceEnable) {
			return false;
		}
		
		// We're going to sit and look around
		isLooking = true;
		stopLooking = entity.getRandom().nextFloat() * (maxLookTime - minLookTime) + minLookTime + time;
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		float lookAngleX = entity.getRandom().nextFloat() * angleOfViewVertical - angleOfViewVertical / 2f;
		float lookAngleY = entity.getRandom().nextFloat() * angleOfViewHorizontal - angleOfViewHorizontal / 2f;
		lookAngleX += entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		lookAngleY += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float vx = (float) -Math.sin(Math.toRadians(lookAngleY));
		float vy = (float) Math.sin(Math.toRadians(lookAngleX));
		float vz = (float) Math.cos(Math.toRadians(lookAngleY));
		entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY + vy * 16f, posZ + vz * 16f);
		entity.getAI().target.look = true;
		entity.getAI().target.move = false;
		entity.getAI().target.minLookPitch = -angleOfViewVertical / 2f;
		entity.getAI().target.minLookYaw = -angleOfViewHorizontal / 2f;
		entity.getAI().target.maxLookPitch = angleOfViewVertical / 2f;
		entity.getAI().target.maxLookYaw = angleOfViewHorizontal / 2f;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isLooking = false;
	}

}
