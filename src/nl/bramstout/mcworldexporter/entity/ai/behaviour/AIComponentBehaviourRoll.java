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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRoll extends AIComponent{

	/**
	 * The probability that the mob will use the goal.
	 */
	public float probability;
	
	private boolean isRolling;
	
	public AIComponentBehaviourRoll(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isRolling) {
			if(entity.getAI().target == null) {
				isRolling = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= 0.5f) {
				isRolling = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
				return false;
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 1)));
			return true;
		}
		
		if(!EntityUtil.standingOnSolidBlock(entity, posX, posY, posZ)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime) && !forceEnable) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
			return false;
		}
		
		// Find the forward direction
		float yaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		Matrix rotMatrix = Matrix.rotateY(yaw);
		Vector3f forward = rotMatrix.transformDirection(new Vector3f(0f, 0f, 1f));
		float rollDistance = 2f;
		forward = forward.multiply(rollDistance);
		
		entity.getAI().target = new EntityTargetPosition(posX + forward.x, posY, posZ + forward.z);
		
		isRolling = true;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 1)));
		
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isRolling)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
		isRolling = false;
	}

}
