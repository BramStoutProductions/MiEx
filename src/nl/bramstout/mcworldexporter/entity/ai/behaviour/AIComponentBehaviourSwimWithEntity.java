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

import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTag;

public class AIComponentBehaviourSwimWithEntity extends AIComponent{

	/**
	 * The multiplier this entity's speed is modified by when matching
	 * another entity's direction.
	 */
	public float catchUpMultiplier;
	/**
	 * Distance from the entity being followed at which this entity
	 * will speed up to reach that entity.
	 */
	public float catchUpThreshold;
	/**
	 * Percent chance to stop following the current entity, if they're
	 * riding another entity or they're not swimming. 1.0 = 100%.
	 */
	public float chanceToStop;
	/**
	 * Filters which types of entities are valid to follow.
	 */
	public EntityFilter entityTypes;
	/**
	 * Distance from the entity being followed at which this entity
	 * will try to match that entity's direction.
	 */
	public float matchDirectionThreshold;
	/**
	 * Radius around this entity to search for another entity to follow.
	 */
	public float searchRange;
	/**
	 * The multiplier this entity's speed is modified by when trying to
	 * catch up to the entity being followed.
	 */
	public float speedMultiplier;
	/**
	 * Time in seconds between checks to determine if this entity
	 * should catch up to the entity being followed or match
	 * the direction of the entity being followed.
	 */
	public float stateCheckInterval;
	/**
	 * Distance from the entity being followed at which this entity
	 * will stop following that entity.
	 */
	public float stopDistance;
	/**
	 * Percent chance to start following another entity, if not already
	 * doing so. 1.0 = 100%.
	 */
	public float successRate;
	
	
	private Entity swimmingWith;
	private boolean isCatchingUp;
	private float nextStateCheck;
	
	public AIComponentBehaviourSwimWithEntity(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		swimmingWith = null;
		isCatchingUp = false;
		nextStateCheck = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(swimmingWith != null) {
			if(time < nextStateCheck)
				return true;
			nextStateCheck = time + stateCheckInterval;
			
			NbtTag isSwimmingTag = swimmingWith.getProperties().get("IsSwimming");
			if(isSwimmingTag == null || isSwimmingTag.asByte() <= 0) {
				if(EntityUtil.randomChance(entity, chanceToStop, deltaTime)) {
					swimmingWith = null;
					entity.getAI().target = null;
					return false;
				}
			}
			
			float posX2 = swimmingWith.getX();
			float posY2 = swimmingWith.getY();
			float posZ2 = swimmingWith.getZ();
			if(swimmingWith.getAnimation() != null) {
				posX2 = swimmingWith.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				posY2 = swimmingWith.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				posZ2 = swimmingWith.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			}
			
			float distanceSquared = (posX2 - posX) * (posX2 - posX) + 
					(posY2 - posY) * (posY2 - posY) + 
					(posZ2 - posZ) * (posZ2 - posZ);
			if(!isCatchingUp) {
				if(distanceSquared > (stopDistance * stopDistance)) {
					swimmingWith = null;
					return false;
				}
				if(distanceSquared >= (catchUpThreshold * catchUpThreshold))
					isCatchingUp = true;
			}
			
			if(distanceSquared <= (matchDirectionThreshold * matchDirectionThreshold)) {
				isCatchingUp = false;
				if(swimmingWith.getAI() != null) {
					float dx = swimmingWith.getAI().vx;
					float dy = swimmingWith.getAI().vy;
					float dz = swimmingWith.getAI().vz;
					float length = 1.0f + stateCheckInterval;
					dx *= length;
					dy *= length;
					dz *= length;
					entity.getAI().target = new EntityTargetPosition(posX + dx, posY + dy, posZ + dz);
				}
			}else {
				entity.getAI().target = new EntityTargetPosition(posX2, posY2, posZ2);
			}
			
			return true;
		}
		
		if(!EntityUtil.isInLiquid(entity, posX, posY, posZ))
			return false;
		
		if(!EntityUtil.randomChance(entity, successRate, deltaTime) && !forceEnable)
			return false;
		
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange((int) posX, (int) posZ, (int) searchRange)) {
			for(Entity entity2 : entities) {
				float posX2 = entity2.getX();
				float posY2 = entity2.getY();
				float posZ2 = entity2.getZ();
				if(entity2.getAnimation() != null) {
					posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
					posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
					posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
				}
				float distanceSquared = (posX2 - posX) * (posX2 - posX) + 
						(posY2 - posY) * (posY2 - posY) + 
						(posZ2 - posZ) * (posZ2 - posZ);
				if(distanceSquared > (searchRange * searchRange))
					continue;
				
				if(!entityTypes.testFilter(entity2))
					continue;
				
				NbtTag isSwimmingTag = entity2.getProperties().get("IsSwimming");
				if(isSwimmingTag == null || isSwimmingTag.asByte() <= 0) {
					continue;
				}
				
				swimmingWith = entity2;
				nextStateCheck = time + stateCheckInterval;
				isCatchingUp = distanceSquared > (catchUpThreshold * catchUpThreshold);
				
				if(distanceSquared <= (matchDirectionThreshold * matchDirectionThreshold)) {
					isCatchingUp = false;
					if(swimmingWith.getAI() != null) {
						float dx = swimmingWith.getAI().vx;
						float dy = swimmingWith.getAI().vy;
						float dz = swimmingWith.getAI().vz;
						float length = 1.0f + stateCheckInterval;
						dx *= length;
						dy *= length;
						dz *= length;
						entity.getAI().target = new EntityTargetPosition(posX + dx, posY + dy, posZ + dz);
					}
				}else {
					entity.getAI().target = new EntityTargetPosition(posX2, posY2, posZ2);
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		swimmingWith = null;
	}

}
