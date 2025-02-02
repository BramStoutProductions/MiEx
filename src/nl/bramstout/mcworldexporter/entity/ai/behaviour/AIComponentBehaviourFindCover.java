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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourFindCover extends AIComponent{

	/**
	 * Time in seconds the mob has to wait before using the goal again.
	 */
	public float cooldownTime;
	
	private int cooldownTicks;
	private boolean isFindingCover;
	
	public AIComponentBehaviourFindCover(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		cooldownTicks = 0;
		isFindingCover = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(EntityUtil.isUnderCover(entity, posX, posY, posZ)) {
			isFindingCover = false;
			return false;
		}
		
		// Entity isn't under cover, so let's find a place that is.

		if(isFindingCover) {
			// Entity is already finding cover, so let's check if we need to find a new target
			if(entity.getAI().target != null) {
				if(EntityUtil.isUnderCover(entity, entity.getAI().target.getPosX(time), entity.getAI().target.getPosY(time), 
											entity.getAI().target.getPosZ(time))) {
					// Target is still under cover, so we can keep using it.
					return true;
				}
			}
		}
		
		EntityTargetBlock target = EntityUtil.FindTargetUnderCover(16, 4, entity, posX, posY, posZ);
		if(target == null)
			return false;
		
		isFindingCover = true;
		entity.getAI().target = target;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isFindingCover = false;
	}

}
