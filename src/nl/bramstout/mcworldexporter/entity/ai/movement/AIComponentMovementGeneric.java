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
import nl.bramstout.mcworldexporter.entity.ai.EntityMovementUtil;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentMovementGeneric extends AIComponent{

	/**
	 * The maximum number in degrees the entity can turn per tick.
	 */
	public float maxTurn;
	
	public AIComponentMovementGeneric(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(entity.getAI().path == null) {
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true; // No need to move if there is no target.
		}
		
		Vector3f target = EntityMovementUtil.getNextPathTarget(entity, posX, posY, posZ);
		if(target == null) {
			// No valid path, so set it to null.
			entity.getAI().path = null;
			entity.getAI().target = null;
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true;
		}
		
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 1));
			EntityMovementUtil.swim(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		}else {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 0));
			
			boolean isFlying = false;
			NbtTag isFlyingTag = entity.getProperties().get("IsFlying");
			if(isFlyingTag != null)
				isFlying = isFlyingTag.asByte() > 0;
			
			if(isFlying)
				EntityMovementUtil.fly(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
			else
				EntityMovementUtil.walk(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		}
		
		EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
