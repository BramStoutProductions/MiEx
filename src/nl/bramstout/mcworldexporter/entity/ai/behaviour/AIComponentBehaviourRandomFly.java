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
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourRandomFly extends AIComponent{

	/**
	 * The search radius on the XZ axis.
	 */
	public int searchDistanceXZ;
	/**
	 * The search radius on the Y axis.
	 */
	public int searchDistanceY;
	/**
	 * Y offset added to the select random point to wander to.
	 */
	public float yOffset;
	
	private boolean isFloating;
	
	public AIComponentBehaviourRandomFly(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFloating = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(isFloating) {
			if(entity.getAI().target == null) {
				isFloating = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
				return false;
			}
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
			return true;
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		EntityTargetBlock target = EntityUtil.FindTarget(searchDistanceXZ, searchDistanceY, entity, posX, posY, posZ);
		if(target == null)
			return false;
		entity.getAI().target = new EntityTargetBlock((int) target.getPosX(time), (int) (target.getPosY(time) + yOffset), 
													(int) target.getPosZ(time));
		isFloating = true;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isFloating)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
		isFloating = false;
	}

}
