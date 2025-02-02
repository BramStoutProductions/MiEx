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

public abstract class EntityTarget {

	public boolean move = true;
	public boolean look = true;
	public float minLookYaw = -60f;
	public float maxLookYaw = 60f;
	public float minLookPitch = -45f;
	public float maxLookPitch = 45f;
	public float maxRotationDelta = 10f;
	
	public abstract float getPosX(float time);
	public abstract float getPosY(float time);
	public abstract float getPosZ(float time);
	
	public static class EntityTargetBlock extends EntityTarget{
		
		private int x;
		private int y;
		private int z;
		
		public EntityTargetBlock(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public float getPosX(float time) {
			return ((float) x) + 0.5f;
		}
		
		@Override
		public float getPosY(float time) {
			return y;
		}
		
		@Override
		public float getPosZ(float time) {
			return ((float) z) + 0.5f;
		}
		
	}
	
	public static class EntityTargetPosition extends EntityTarget{
		
		private float x;
		private float y;
		private float z;
		
		public EntityTargetPosition(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public float getPosX(float time) {
			return x;
		}
		
		@Override
		public float getPosY(float time) {
			return y;
		}
		
		@Override
		public float getPosZ(float time) {
			return z;
		}
		
	}
	
	public static class EntityTargetEntity extends EntityTarget{
		
		private Entity entity;
		
		public EntityTargetEntity(Entity entity) {
			this.entity = entity;
		}
		
		public Entity getEntity() {
			return entity;
		}
		
		@Override
		public float getPosX(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
			return entity.getX();
		}
		
		@Override
		public float getPosY(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
			return entity.getY();
		}
		
		@Override
		public float getPosZ(float time) {
			if(entity.getAnimation() != null)
				return entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			return entity.getZ();
		}
		
	}
	
}
