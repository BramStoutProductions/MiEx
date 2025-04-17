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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourMoveToLiquid extends AIComponent{

	/**
	 * Distance in blocks within the mob considers it has reached the goal.
	 * This is the wiggle room to stop the AI from bouncing back and
	 * forth trying to reach a specific spot.
	 */
	public float goalRadius;
	/**
	 * The number of blocks each tick that the mob will check within
	 * its search range and height for a valid block to move to.
	 * A value of 0 will have the mob check every block within range
	 * in one tick.
	 */
	public int searchCount;
	/**
	 * Height in blocks the mob will look for land to move towards.
	 */
	public int searchHeight;
	/**
	 * The distance in blocks it will look for land to move towards.
	 */
	public int searchRange;
	/**
	 * A list of block names of the liquid that it can move to.
	 */
	public List<String> liquidTypes;
	
	private boolean isMoving;
	
	public AIComponentBehaviourMoveToLiquid(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		liquidTypes = new ArrayList<String>();
		isMoving = false;
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isMoving) {
			if(entity.getAI().target == null) {
				isMoving = false;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= (goalRadius * goalRadius)) {
				isMoving = false;
				return false;
			}
			
			return true;
		}
		
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ))
			return false;
		
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		// Entity is in liquid, so try to find a spot for it to move to.
		if(searchCount <= 0) {
			// Check every block in range
			for(int sampleY = -searchHeight; sampleY <= searchHeight; ++sampleY) {
				for(int sampleZ = -searchRange; sampleZ <= searchRange; ++sampleZ) {
					for(int sampleX = -searchRange; sampleX <= searchRange; ++sampleX) {
						int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX + blockX, 
																	sampleY + blockY, sampleZ + blockZ);
						int blockIdAbove = MCWorldExporter.getApp().getWorld().getBlockId(sampleX + blockX, 
																	sampleY + blockY + 1, sampleZ + blockZ);
						if(blockIdAbove != 0)
							continue;
						Block block = BlockRegistry.getBlock(blockId);
						if(!block.hasLiquid())
							continue;
						if(!liquidTypes.isEmpty())
							if(!liquidTypes.contains(block.getName()))
								continue;
						
						// We've found a block in lava to move to.
						entity.getAI().target = new EntityTargetBlock(sampleX + blockX, sampleY + blockY, sampleZ + blockZ);
						isMoving = true;
						return true;
					}
				}
			}
		}else {
			// Randomly check blocks.
			for(int i = 0; i < searchCount; ++i) {
				int sampleX = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockX;
				int sampleY = entity.getRandom().nextInt(-searchHeight, searchHeight + 1) + blockY;
				int sampleZ = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockZ;
				
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY, sampleZ);
				int blockIdAbove = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY + 1, sampleZ);
				if(blockIdAbove != 0)
					continue;
				Block block = BlockRegistry.getBlock(blockId);
				if(!block.hasLiquid())
					continue;
				if(!liquidTypes.isEmpty())
					if(!liquidTypes.contains(block.getName()))
						continue;
				
				// We've found a block on land to move to.
				entity.getAI().target = new EntityTargetBlock(sampleX, sampleY, sampleZ);
				isMoving = true;
				return true;
			}
		}
		
		// We didn't find a spot to move to.
		return false;
	}

	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isMoving = false;
	}
	
}
