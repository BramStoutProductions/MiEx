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
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRandomLookAroundAndSit extends AIComponent{

	/**
	 * The probability of randomly looking around.
	 */
	public float probability;
	/**
	 * The cooldown in seconds before the goal can be used again.
	 */
	public float cooldown;
	/**
	 * The minimum amount of random looks before stopping.
	 */
	public int minLookCount;
	/**
	 * The maximum amount of random looks before stopping.
	 */
	public int maxLookCount;
	/**
	 * The minimum amount of time in ticks to look in a single direction.
	 */
	public int minLookTime;
	/**
	 * The maximum amount of time in ticks to look in a single direction.
	 */
	public int maxLookTime;
	/**
	 * The left most angle the entity can look at.
	 */
	public float minAngleOfViewHorizontal;
	/**
	 * The right most angle the entity can look at.
	 */
	public float maxAngleOfViewHorizontal;
	
	private boolean isLooking;
	private float nextTry;
	private int lookTimer;
	private int looksLeft;
	
	public AIComponentBehaviourRandomLookAroundAndSit(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isLooking = false;
		nextTry = -1f;
		lookTimer = 0;
		looksLeft = 0;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isLooking) {
			if(looksLeft <= 0) {
				isLooking = false;
				nextTry = time + cooldown;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
				entity.getAI().target = null;
				return false;
			}
			lookTimer--;
			if(lookTimer <= 0) {
				looksLeft--;
				
				float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
				float lookAngle = entity.getRandom().nextFloat() * (maxAngleOfViewHorizontal - minAngleOfViewHorizontal) + minAngleOfViewHorizontal;
				lookAngle += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
				float vx = (float) -Math.sin(Math.toRadians(lookAngle));
				float vz = (float) Math.cos(Math.toRadians(lookAngle));
				entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY, posZ + vz * 16f);
				entity.getAI().target.look = true;
				entity.getAI().target.move = false;
				entity.getAI().target.minLookPitch = 0f;
				entity.getAI().target.minLookYaw = -minAngleOfViewHorizontal;
				entity.getAI().target.maxLookPitch = 0f;
				entity.getAI().target.maxLookYaw = maxAngleOfViewHorizontal;
				lookTimer = entity.getRandom().nextInt(minLookTime, maxLookTime);
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
			return true;
		}
		
		if(time < nextTry) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime) && !forceEnable) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			nextTry = time + cooldown;
			return false;
		}
		
		// We're going to sit and look around
		isLooking = true;
		entity.getAI().target = null;
		looksLeft = entity.getRandom().nextInt(minLookCount, maxLookCount);
		lookTimer = entity.getRandom().nextInt(minLookTime, maxLookTime);
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		float lookAngle = entity.getRandom().nextFloat() * (maxAngleOfViewHorizontal - minAngleOfViewHorizontal) + minAngleOfViewHorizontal;
		lookAngle += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float vx = (float) -Math.sin(Math.toRadians(lookAngle));
		float vz = (float) Math.cos(Math.toRadians(lookAngle));
		entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY, posZ + vz * 16f);
		entity.getAI().target.look = true;
		entity.getAI().target.move = false;
		entity.getAI().target.minLookPitch = 0f;
		entity.getAI().target.minLookYaw = -minAngleOfViewHorizontal;
		entity.getAI().target.maxLookPitch = 0f;
		entity.getAI().target.maxLookYaw = maxAngleOfViewHorizontal;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isLooking)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
		isLooking = false;
		nextTry = -1f;
	}

}
