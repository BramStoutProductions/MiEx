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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;

public class AIComponentBehaviourFollowMob extends AIComponent{

	/**
	 * The distance to search for mobs of the same type.
	 */
	public float searchRange;
	/**
	 * The distance from the mob it's following at which it'll stop.
	 */
	public float stopDistance;
	
	private boolean isFollowing;
	
	public AIComponentBehaviourFollowMob(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFollowing = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isFollowing) {
			if(entity.getAI().target == null) {
				isFollowing = false;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			
			float distanceSquared = (targetX - posX) * (targetX - posY) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= (stopDistance * stopDistance)) {
				entity.getAI().target = null;
				isFollowing = false;
				return false;
			}
			return true;
		}
		
		List<Entity> fellowEntities = new ArrayList<Entity>();
		float avgX = 0f;
		float avgY = 0f;
		float avgZ = 0f;
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange((int) posX, (int) posZ, (int) searchRange)) {
			for(Entity entity2 : entities) {
				if(entity2 == entity)
					continue;
				if(entity2.getId().equals(entity.getId())) {
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
					if(distanceSquared <= (searchRange * searchRange)) {
						fellowEntities.add(entity2);
						avgX += posX2;
						avgY += posY2;
						avgZ += posZ2;
					}
				}
			}
		}
		if(fellowEntities.isEmpty()) {
			// No entity to follow
			return false;
		}
		
		avgX /= (float) fellowEntities.size();
		avgY /= (float) fellowEntities.size();
		avgZ /= (float) fellowEntities.size();
		float closestDistance = Float.MAX_VALUE;
		Entity closestEntity = null;
		// Let's find the entity that's the closest to this average
		for(Entity entity2 : fellowEntities) {
			float posX2 = entity2.getX();
			float posY2 = entity2.getY();
			float posZ2 = entity2.getZ();
			if(entity2.getAnimation() != null) {
				posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			}
			
			float distanceSquared = (posX2 - avgX) * (posX2 - avgX) + 
									(posY2 - avgY) * (posY2 - avgY) + 
									(posZ2 - avgZ) * (posZ2 - avgZ);
			if(distanceSquared < closestDistance) {
				closestDistance = distanceSquared;
				closestEntity = entity2;
			}
		}
		if(closestEntity == null)
			return false;
		
		// We've got a mob to follow
		entity.getAI().target = new EntityTargetEntity(closestEntity);
		isFollowing = true;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isFollowing = true;
	}

}
