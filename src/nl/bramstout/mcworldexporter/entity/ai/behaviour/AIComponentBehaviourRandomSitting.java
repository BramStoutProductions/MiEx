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
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRandomSitting extends AIComponent{

	/**
	 * Time in seconds the entity has to wait before using the goal again.
	 */
	public float cooldown;
	/**
	 * The minimum amount of time in seconds before the entity can stand back up.
	 */
	public float minSitTime;
	/**
	 * This is the chance that the entity will start this goal, from 0 to 1.
	 */
	public float startChance;
	/**
	 * This is the chance that the entity will stop this goal, from 0 to 1.
	 */
	public float stopChance;
	
	private boolean isSitting;
	private float stopTime;
	private float nextTry;
	
	public AIComponentBehaviourRandomSitting(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isSitting = false;
		stopTime = 0f;
		nextTry = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isSitting) {
			if(time > stopTime) {
				if(EntityUtil.randomChance(entity, stopChance, deltaTime)) {
					isSitting = false;
					entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
					return false;
				}
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
			return true;
		}
		
		if(time < nextTry) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, startChance, deltaTime) && !forceEnable) {
			nextTry = time + cooldown;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		isSitting = true;
		stopTime = time + minSitTime;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isSitting)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
		isSitting = false;
		nextTry = 0f;
		stopTime = 0f;
	}

}
