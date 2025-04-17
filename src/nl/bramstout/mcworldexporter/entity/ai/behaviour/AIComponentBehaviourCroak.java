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
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourCroak extends AIComponent{

	/**
	 * The minimum time in seconds that a croak takes.
	 */
	public float minDuration;
	/**
	 * The maximum time in seconds that a croak takes.
	 */
	public float maxDuration;
	/**
	 * The minimum time between croaks.
	 */
	public float minInterval;
	/**
	 * The maximum time between croaks.
	 */
	public float maxInterval;
	/**
	 * Filter that has to be satisfied for this entity to croak.
	 */
	public EntityFilter filter;
	
	private boolean isCroaking;
	private float nextCroak;
	private float endCroak;
	
	public AIComponentBehaviourCroak(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isCroaking = false;
		nextCroak = -1f;
		endCroak = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isCroaking) {
			if(time >= endCroak) {
				isCroaking = false;
				nextCroak = entity.getRandom().nextFloat() * (maxInterval - minInterval) + minInterval + time;
				endCroak = -1f;
			}
		}else {
			if(time >= nextCroak) {
				if(nextCroak == -1f) {
					nextCroak = entity.getRandom().nextFloat() * (maxInterval - minInterval) + minInterval + time;
				}else {
					isCroaking = true;
					nextCroak = -1f;
					endCroak = entity.getRandom().nextFloat() * (maxDuration - minDuration) + minDuration + time;
				}
			}
		}
		
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsCroaking", isCroaking ? ((byte) 1) : ((byte) 0)));
		return isCroaking;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isCroaking = false;
		nextCroak = -1f;
		endCroak = -1f;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsCroaking", ((byte) 0)));
	}

}
