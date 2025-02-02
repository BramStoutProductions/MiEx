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

public class AIComponentBehaviourFloatWander extends AIComponent{

	/**
	 * The minimum amount of seconds that the entity will float wander for.
	 */
	public float minFloatDuration;
	/**
	 * The maximum amount of seconds that the entity will float wander for.
	 */
	public float maxFloatDuration;
	/**
	 * If true, the entity will select a new random point to wander to
	 * at the end of the float wander, rather than ending.
	 */
	public boolean randomSelect;
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
	
	private float endFloat;
	private boolean isFloating;
	
	public AIComponentBehaviourFloatWander(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		endFloat = -1f;
		isFloating = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isFloating) {
			if(time >= endFloat) {
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
		EntityTargetBlock target = EntityUtil.FindTargetInAir(searchDistanceXZ, searchDistanceY, entity, posX, posY, posZ);
		if(target == null) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
			return false;
		}
		entity.getAI().target = new EntityTargetBlock((int) target.getPosX(time), (int) (target.getPosY(time) + yOffset), 
													(int) target.getPosZ(time));
		isFloating = true;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
		endFloat = entity.getRandom().nextFloat() * (maxFloatDuration - minFloatDuration) + minFloatDuration + time;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isFloating)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
		isFloating = false;
	}

}
