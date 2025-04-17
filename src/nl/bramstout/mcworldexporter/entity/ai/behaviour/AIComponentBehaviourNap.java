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

public class AIComponentBehaviourNap extends AIComponent{

	/**
	 * The minimum time in seconds the mob has to wait before
	 * using the goal again.
	 */
	public float minCooldown;
	/**
	 * The maximum time in seconds the mob has to wait before
	 * using the goal again.
	 */
	public float maxCooldown;
	/**
	 * The block distance in x and z that will be checked for
	 * mobs that this mob detects.
	 */
	public float mobDetectionDistance;
	/**
	 * The block distance in y that will be checked for mobs
	 * that this mob detects.
	 */
	public float mobDetectionHeight;
	/**
	 * Filter that has to be true for this mob to be able to nap.
	 */
	public EntityFilter canNapFilter;
	/**
	 * Filter that has to be true for an entity to not wake this
	 * mob up.
	 */
	public EntityFilter wakeMobExceptionFilter;
	
	private boolean isNapping;
	private float nextSleep;
	
	public AIComponentBehaviourNap(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isNapping = false;
		nextSleep = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isNapping) {
			if(shouldWakeUp(entity, time)) {
				isNapping = false;
				return false;
			}
			return true;
		}
		
		if(time >= nextSleep) {
			if(nextSleep == -1f) {
				nextSleep = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
				return false;
			}
			nextSleep = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
			
			if(!canNapFilter.testFilter(entity))
				return false;
			if(shouldWakeUp(entity, time))
				return false;
			
			isNapping = true;
			return true;
		}
		return false;
	}
	
	private boolean shouldWakeUp(Entity entity, float time) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange(
									(int) posX, (int) posZ, (int) mobDetectionDistance)) {
			for(Entity entity2 : entities) {
				float posX2 = entity2.getX();
				float posY2 = entity2.getY();
				float posZ2 = entity2.getZ();
				if(entity2.getAnimation() != null) {
					posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
					posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
					posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
				}
				if(Math.abs(posX2 - posX) > mobDetectionDistance || Math.abs(posZ2 - posZ) > mobDetectionDistance ||
						Math.abs(posY2 - posY) > mobDetectionHeight)
					continue;
				if(wakeMobExceptionFilter.testFilter(entity2))
					continue;
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isNapping = false;
		nextSleep = -1f;
	}

}
