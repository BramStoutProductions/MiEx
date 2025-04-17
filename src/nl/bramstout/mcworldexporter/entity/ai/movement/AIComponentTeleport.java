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
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentTeleport extends AIComponent{

	/**
	 * Modifies the chance that the entity will teleport if the entity
	 * is in darkness.
	 */
	public float darkTeleportChance;
	/**
	 * Modifies the chance that the entity will teleport if the entity
	 * is in daylight.
	 */
	public float lightTeleportChance;
	/**
	 * Maximum amount of time in seconds between random teleports.
	 */
	public float maxRandomTeleportTime;
	/**
	 * Minimum amount of time in seconds between random teleports.
	 */
	public float minRandomTeleportTime;
	/**
	 * Width of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeWidth;
	/**
	 * Height of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeHeight;
	/**
	 * Depth of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeDepth;
	/**
	 * If true, the entity will teleport randomly.
	 */
	public boolean randomTeleports;
	/**
	 * Maximum distance the entity will teleport when chasing a target.
	 */
	public float targetDistance;
	/**
	 * The chance that the entity will teleport, per tick when chasing a target, 
	 * between 0.0 and 1.0. 1.0 means 100%
	 */
	public float targetTeleportChance;
	
	private float nextRandomTeleport;
	
	public AIComponentTeleport(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		nextRandomTeleport = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		if(randomTeleports) {
			if(time >= nextRandomTeleport) {
				if(nextRandomTeleport != -1f) {
					float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
					float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
					float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
					EntityTargetBlock target = EntityUtil.FindTarget((int) (randomTeleportCubeWidth/2f), (int) (randomTeleportCubeHeight/2f), 
												entity, posX, posY, posZ);
					if(target != null) {
						entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(time, target.getPosX(time)));
						entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(time, target.getPosY(time)));
						entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(time, target.getPosZ(time)));
					}
				}
				nextRandomTeleport = entity.getRandom().nextFloat() * (maxRandomTeleportTime - minRandomTeleportTime) + 
										minRandomTeleportTime + time;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextRandomTeleport = -1f;
	}

}
