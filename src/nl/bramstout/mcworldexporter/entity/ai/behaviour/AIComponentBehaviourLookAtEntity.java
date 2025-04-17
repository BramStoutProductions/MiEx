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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourLookAtEntity extends AIComponent{

	/**
	 * The angle in degrees that the mob can see in the X-axis
	 */
	public float angleOfViewHorizontal;
	/**
	 * The angle in degrees that the mob can see in the Y axis.
	 */
	public float angleOfViewVertical;
	/**
	 * The filter that the entity must match for this entity to look at it.
	 */
	public EntityFilter filter;
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
	private Entity lookTarget;
	
	public AIComponentBehaviourLookAtEntity(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		stopLooking = 0f;
		lookTarget = null;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(lookTarget != null) {
			if(time >= stopLooking) {
				lookTarget = null;
				entity.getAI().target = null;
				return false;
			}
			return true;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime) && !forceEnable) {
			return false; // Not going to look at another entity this tick
		}
		
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange((int) posX, (int) posZ, (int) lookDistance)) {
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
				if(distanceSquared <= (lookDistance * lookDistance)) {
					if(filter.testFilter(entity2)) {
						lookTarget = entity2;
						entity.getAI().target = new EntityTargetEntity(lookTarget);
						entity.getAI().target.move = false;
						entity.getAI().target.minLookYaw = -angleOfViewHorizontal / 2f;
						entity.getAI().target.minLookPitch = -angleOfViewVertical / 2f;
						entity.getAI().target.maxLookYaw = angleOfViewHorizontal / 2f;
						entity.getAI().target.maxLookPitch = angleOfViewVertical / 2f;
						stopLooking = entity.getRandom().nextFloat() * (maxLookTime - minLookTime) + minLookTime + time;
						return true;
					}
				}
			}
		}
		
		// Couldn't find an entity to look at.
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		lookTarget = null;
	}

}
