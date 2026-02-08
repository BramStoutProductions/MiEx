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

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.LayeredBlock;

public class AIComponentBehaviourRandomBreach extends AIComponent{

	/**
	 * Time in seconds the mob has to wait before using the goal again.
	 */
	public float cooldownTime;
	/**
	 * A random value to determine when to randomly move somewhere.
	 * This has a 1/interval chance to choose this goal.
	 */
	public int interval;
	/**
	 * Distance in blocks in xz axis that the mob will look for a new spot
	 * to move to. Must be at least 1
	 */
	public int searchRange;
	/**
	 * Distance in blocks in y axis that the mob will look for a new spot
	 * to move to. Must be at least 1
	 */
	public int searchHeight;
	
	private float nextTry;
	private boolean isBreaching;
	
	public AIComponentBehaviourRandomBreach(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		nextTry = -1f;
		isBreaching = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isBreaching) {
			if(entity.getAI().target == null) {
				isBreaching = false;
				nextTry = time + cooldownTime;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= 1f) {
				isBreaching = false;
				nextTry = time + cooldownTime;
				return false;
			}
			
			return true;
		}
		
		if(time < nextTry)
			return false;
		
		if(!EntityUtil.randomChance(entity, interval, deltaTime) && !forceEnable) {
			nextTry = time + cooldownTime;
			return false;
		}
		
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockX;
			int sampleY = entity.getRandom().nextInt(-searchHeight, searchHeight + 1) + blockY;
			int sampleZ = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockZ;
			
			int blockIdAbove = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY + 1, sampleZ, 0);
			if(blockIdAbove != 0)
				continue;
			LayeredBlock blocks = new LayeredBlock();
			MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY, sampleZ, blocks);
			boolean isLiquid = false;
			for(int layer = 0; layer < blocks.getLayerCount(); ++layer) {
				Block block = BlockRegistry.getBlock(blocks.getBlock(layer));
				if(block.isLiquid()) {
					isLiquid = true;
					break;
				}
			}
			if(!isLiquid)
				continue;
			
			// We've found a block that's at the surface.
			isBreaching = true;
			entity.getAI().target = new EntityTargetPosition(((float) sampleX) + 0.5f, ((float) sampleY) + 0.8f, ((float) sampleZ) + 0.5f);
			return true;
		}
		
		// We didn't find a spot to breach to.
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextTry = -1f;
		isBreaching = false;
	}

}
