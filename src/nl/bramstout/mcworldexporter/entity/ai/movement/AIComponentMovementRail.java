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

package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;

public class AIComponentMovementRail extends AIComponent{

	/**
	 * Maximum speed in blocks per tick that this entity will move at when on the rail.
	 */
	public float maxSpeed;
	
	public AIComponentMovementRail(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		/*if(entity.getAI().path == null) {
			EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);
			lastTargetNodeIndex = -1;
			return true; // No need to move if there is no target.
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		int closestNodeIndex = entity.getAI().path.getClosestNode(posX, posY, posZ);
		int targetNodeIndex = Math.min(closestNodeIndex + 1, entity.getAI().path.getSize() - 1);
		if(targetNodeIndex < 0) {
			// Invalid index, so we just don't move.
			EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);
			lastTargetNodeIndex = -1;
			return true;
		}
		if(lastTargetNodeIndex != -1) {
			// If we've been on the same target node index for a while,
			// then we're either stuck or at the end goal.
			if((time - lastTargetNodeIndexChange) > 1f) {
				entity.getAI().path = null;
				entity.getAI().target = null;
			}
		}
		if(lastTargetNodeIndex != targetNodeIndex) {
			lastTargetNodeIndex = targetNodeIndex;
			lastTargetNodeIndexChange = time;
		}
		PathNode target = entity.getAI().path.getNode(targetNodeIndex);
		
		EntityMovementUtil.rials(entity, time, deltaTime, target, posX, posY, posZ);
		
		EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);*/
		// TODO: Implement
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
