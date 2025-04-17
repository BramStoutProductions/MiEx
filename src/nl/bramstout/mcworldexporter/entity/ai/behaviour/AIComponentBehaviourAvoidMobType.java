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
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourAvoidMobType extends AIComponent{

	public static class Avoidee{
		
		/**
		 * Filter that the entity must comply with.
		 */
		public EntityFilter filter;
		/**
		 * If true, there doesn't have to be a direct line-of-sight.
		 */
		public boolean ignoreVisibility;
		/**
		 * The maximum distance that an entity must be within.
		 */
		public float maxDist;
		/**
		 * How many blocks away from its avoid target
		 * the entity must be for it to stop fleeing.
		 */
		public float maxFlee;
		/**
		 * How many blocks within range of its avoid target
		 * the entity must be for it to begin sprinting away
		 * from the avoid target.
		 */
		public float sprintDistance;
		/**
		 * The event to run when initiating an escape.
		 */
		public EntityEvent onEscape;
		
		public Avoidee copy() {
			Avoidee avoidee = new Avoidee();
			avoidee.filter = filter;
			avoidee.ignoreVisibility = ignoreVisibility;
			avoidee.maxDist = maxDist;
			avoidee.maxFlee = maxFlee;
			avoidee.sprintDistance = sprintDistance;
			avoidee.onEscape = onEscape;
			return avoidee;
		}
		
	}
	
	/**
	 * The target block used to avoid the entity should be within
	 * this distance on the XZ axis.
	 */
	public int avoidTargetXZ;
	/**
	 * The target block used to avoid the entity should be within
	 * this distance on the Y axis.
	 */
	public int avoidTargetY;
	/**
	 * List of entities to avoid.
	 */
	public List<Avoidee> avoidees;
	
	private boolean isEscaping;
	private float maxFlee;
	private Entity avoidTarget;
	private int fleeTicks;
	
	public AIComponentBehaviourAvoidMobType(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		avoidees = new ArrayList<Avoidee>();
		isEscaping = false;
		maxFlee = 0f;
		avoidTarget = null;
		fleeTicks = 40;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isEscaping) {
			float targetX = avoidTarget.getX();
			float targetY = avoidTarget.getY();
			float targetZ = avoidTarget.getZ();
			if(avoidTarget.getAnimation() != null) {
				targetX = avoidTarget.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				targetY = avoidTarget.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				targetZ = avoidTarget.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			}
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) +
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared > (maxFlee * maxFlee)) {
				// We are done fleeing.
				isEscaping = false;
				avoidTarget = null;
				fleeTicks = 40;
				return false;
			}
			
			// Still fleeing.
			// Select a new target every so often.
			fleeTicks--;
			if(fleeTicks <= 0) {
				// Select a new target
				EntityTargetBlock target = EntityUtil.FindTarget(avoidTargetXZ, avoidTargetY, entity, posX, posY, posZ, 
																targetX, targetY, targetZ);
				if(target != null)
					entity.getAI().target = target;
			}
			return true;
		}
		
		int searchRadius = 0;
		for(Avoidee avoidee : avoidees)
			searchRadius = Math.max(searchRadius, (int) avoidee.maxDist);
		
		// We're currently not escaping anything, so search around to check if
		// there is anything that we should escape.
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange((int) posX, (int) posZ, searchRadius)) {
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
				for(Avoidee avoidee : avoidees) {
					if(distanceSquared > (avoidee.maxDist * avoidee.maxDist))
						continue;
					if(!avoidee.filter.testFilter(entity2))
						continue;
					if(!avoidee.ignoreVisibility) {
						// Check for a line of sight;
						float dx = posX2 - posX;
						float dy = posY2 - posY;
						float dz = posZ2 - posZ;
						float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
						dx /= length;
						dy /= length;
						dz /= length;
						float maxComponent = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
						float tStep = 1.0f / maxComponent;
						boolean directLineOfSight = true;
						for(float t = 0.0f; t < length; t += tStep) {
							float sampleX = posX + dx * t;
							float sampleY = posY + dy * t + 1.5f; // Add 1.5 to the Y to get the rough eyeline.
							float sampleZ = posZ + dz * t;
							int blockX = (int) Math.floor(sampleX);
							int blockY = (int) Math.floor(sampleY);
							int blockZ = (int) Math.floor(sampleZ);
							int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
							if(blockId != 0) {
								directLineOfSight = false;
								break;
							}
						}
						if(!directLineOfSight)
							continue;
					}
					// We've found an entity that we should avoid.
					avoidee.onEscape.fireEvent(entity);
					isEscaping = true;
					maxFlee = avoidee.maxFlee;
					avoidTarget = entity2;
					fleeTicks = 40;
					EntityTargetBlock target = EntityUtil.FindTarget(avoidTargetXZ, avoidTargetY, entity, posX, posY, posZ, posX2, posY2, posZ2);
					if(target != null)
						entity.getAI().target = target;
					
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isEscaping = false;
		avoidTarget = null;
		fleeTicks = 40;
	}

}
