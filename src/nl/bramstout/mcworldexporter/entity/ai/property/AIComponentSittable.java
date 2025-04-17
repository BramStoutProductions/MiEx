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

package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.nbt.NbtTag;

public class AIComponentSittable extends AIComponent{

	/**
	 * Event to run when the entity enters the sit state.
	 */
	public EntityEvent sitEvent;
	/**
	 * Event to run when the entity leaves the sit state.
	 */
	public EntityEvent standEvent;
	
	private byte prevSitting;
	
	public AIComponentSittable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		prevSitting = 0;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		byte sitting = 0;
		NbtTag sittingTag = entity.getProperties().get("Sitting");
		if(sittingTag != null) {
			sitting = sittingTag.asByte();
		}
		
		if(sitting != prevSitting) {
			if(sitting <= 0) {
				standEvent.fireEvent(entity);
			}else {
				sitEvent.fireEvent(entity);
			}
			prevSitting = sitting;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		byte sitting = 0;
		NbtTag sittingTag = entity.getProperties().get("Sitting");
		if(sittingTag != null) {
			sitting = sittingTag.asByte();
		}
		
		prevSitting = sitting;
	}

}
