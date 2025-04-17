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

package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;
import nl.bramstout.mcworldexporter.nbt.NbtTag;

public class AIComponentPeek extends AIComponent{

	/**
	 * Event to initiate when the entity starts peeking.
	 */
	public EntityEvent onOpen;
	/**
	 * Event to initiate when the entity is done peeking.
	 */
	public EntityEvent onClose;
	/**
	 * Event to initiate when the entity's target entity starts peeking.
	 */
	public EntityEvent onTargetOpen;
	
	private boolean prevPeekState;
	private Entity prevTarget;
	private boolean prevTargetPeekState;
	
	public AIComponentPeek(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		prevPeekState = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		boolean peekState = false;
		NbtTag isPeekingTag = entity.getProperties().get("IsPeeking");
		if(isPeekingTag != null)
			peekState = isPeekingTag.asByte() > 0;
		
		if(peekState != prevPeekState) {
			prevPeekState = peekState;
			if(peekState)
				onOpen.fireEvent(entity);
			else
				onClose.fireEvent(entity);
		}
		
		if(entity.getAI().target != null) {
			if(entity.getAI().target instanceof EntityTargetEntity) {
				Entity target = ((EntityTargetEntity) entity.getAI().target).getEntity();
				if(prevTarget != target) {
					prevTarget = target;
					prevTargetPeekState = false;
				}
				
				peekState = false;
				isPeekingTag = target.getProperties().get("IsPeeking");
				if(isPeekingTag != null)
					peekState = isPeekingTag.asByte() > 0;
				
				if(peekState != prevTargetPeekState) {
					prevTargetPeekState = peekState;
					if(peekState)
						onTargetOpen.fireEvent(entity);
				}
			}else {
				prevTarget = null;
			}
		}else {
			prevTarget = null;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		boolean peekState = false;
		NbtTag isPeekingTag = entity.getProperties().get("IsPeeking");
		if(isPeekingTag != null)
			peekState = isPeekingTag.asByte() > 0;
		prevPeekState = peekState;
	}

}
