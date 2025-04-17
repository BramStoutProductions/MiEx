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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.molang.MolangScript;

public class AIComponentRideable extends AIComponent{

	public static class Seat{
		
		/**
		 * Angle in degrees that a rider is allowed to rotate
		 * while riding this entity.
		 */
		public float lockRiderRotation;
		/**
		 * Defines the maximum number of riders that can be riding
		 * this entiity for this seat to be valid.
		 */
		public int maxRiderCount;
		/**
		 * Defines the minimum number of riders that need to be riding
		 * this entity before this seat can be used.
		 */
		public int minRiderCount;
		/**
		 * Position X of this seat relative to this entity's position.
		 */
		public float posX;
		/**
		 * Position Y of this seat relative to this entity's position.
		 */
		public float posY;
		/**
		 * Position Z of this seat relative to this entity's position.
		 */
		public float posZ;
		/**
		 * Offset to rotate riders by.
		 */
		public MolangScript rotateRiderBy;
		
		public Seat copy() {
			Seat seat = new Seat();
			seat.lockRiderRotation = lockRiderRotation;
			seat.maxRiderCount = maxRiderCount;
			seat.minRiderCount = minRiderCount;
			seat.posX = posX;
			seat.posY = posY;
			seat.posZ = posZ;
			seat.rotateRiderBy = rotateRiderBy;
			return seat;
		}
		
	}
	
	/**
	 * List of entities that can ride this entity.
	 */
	public List<String> familyTypes;
	/**
	 * The maximum width a mob can be to be a passenger.
	 * A value of 0 ignores this paramter.
	 */
	public float passengerMaxWidth;
	/**
	 * If true, the entity will pull in entities that
	 * are in the correct familyTypes into any
	 * available seats.
	 */
	public boolean pullInEntities;
	/**
	 * The list of positions and number of riders for
	 * each position for entities riding this entity.
	 */
	public List<Seat> seats;
	
	public AIComponentRideable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		familyTypes = new ArrayList<String>();
		seats = new ArrayList<Seat>();
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
