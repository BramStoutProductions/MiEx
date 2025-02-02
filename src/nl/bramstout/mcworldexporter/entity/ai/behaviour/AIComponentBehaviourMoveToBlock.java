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
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourMoveToBlock extends AIComponent{

	public static enum SelectionMethod{
		NEAREST, RANDOM
	}
	
	/**
	 * Distance in blocks within the mob considers it has reached the goal.
	 * This is the wiggle room to stop the AI from bouncing back and forth
	 * trying to reach a specific spot. 
	 */
	public double goalRadius;
	/**
	 * Event to run on block reached.
	 */
	public EntityEvent onReach;
	/**
	 * Event to run on completing a stay of stayDuration at the block.
	 */
	public EntityEvent onStayCompleted;
	/**
	 * The height in blocks that the mob will look for the block.
	 */
	public int searchHeight;
	/**
	 * The distance in blocks that the mob will look for the block.
	 */
	public int searchRange;
	/**
	 * Chance to start the behaviour (applied after each tickInterval).
	 */
	public float startChance;
	/**
	 * Number of ticks needed to complete a stay at the block.
	 */
	public float stayDuration;
	/**
	 * Block types to move to.
	 */
	public List<String> targetBlocks;
	/**
	 * X offset to add to the selected block's center position.
	 */
	public float targetOffsetX;
	/**
	 * Y offset to add to the selected block's center position.
	 */
	public float targetOffsetY;
	/**
	 * Z offset to add to the selected block's center position.
	 */
	public float targetOffsetZ;
	/**
	 * Method used to search for the block.
	 */
	public SelectionMethod targetSelectionMethod;
	/**
	 * Interval in ticks to try to run this behaviour.
	 */
	public int tickInterval;
	
	private int tickCounter;
	private boolean isMoving;
	private boolean isStaying;
	private float endStay;
	
	public AIComponentBehaviourMoveToBlock(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		targetBlocks = new ArrayList<String>();
		
		tickCounter = 0;
		isMoving = false;
		isStaying = false;
		endStay = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
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
				// Reached the goal
				entity.getAI().target = null; // Tell it to stay still
				isMoving = false;
				isStaying = true;
				endStay = time + stayDuration / 20f;
				onReach.fireEvent(entity);
			}
			return true;
		}
		if(isStaying) {
			if(time >= endStay) {
				onStayCompleted.fireEvent(entity);;
				isStaying = false;
				tickCounter = tickInterval;
				return false;
			}
			return true;
		}
		
		if(tickCounter > 0) {
			tickCounter--;
			return false;
		}
		tickCounter = tickInterval;
		
		if(!EntityUtil.randomChance(entity, startChance, deltaTime))
			return false;
		
		// Time to search for a block to move to.
		// We have two options, either randomly select blocks
		// or get the nearest
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		
		int selectX = Integer.MIN_VALUE;
		int selectY = Integer.MIN_VALUE;
		int selectZ = Integer.MIN_VALUE;
		
		if(targetSelectionMethod == SelectionMethod.NEAREST) {
			int closestDistance = Integer.MAX_VALUE;
			for(int sampleY = -searchHeight; sampleY <= searchHeight; ++sampleY) {
				for(int sampleZ = -searchRange; sampleZ <= searchRange; ++sampleZ) {
					for(int sampleX = -searchRange; sampleX <= searchRange; ++sampleX) {
						int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX + blockX, 
																	sampleY + blockY, sampleZ + blockZ);
						Block block = BlockRegistry.getBlock(blockId);
						if(targetBlocks.contains(block.getName())) {
							int distance = sampleX * sampleX + sampleY * sampleY + sampleZ * sampleZ;
							if(distance < closestDistance) {
								selectX = sampleX + blockX;
								selectY = sampleY + blockY;
								selectZ = sampleZ + blockZ;
							}
						}
					}
				}
			}
		}else if(targetSelectionMethod == SelectionMethod.RANDOM){
			int numAttempts = searchRange * searchRange * searchHeight + 1;
			for(int attempt = 0; attempt < numAttempts; ++attempt) {
				int sampleX = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockX;
				int sampleY = entity.getRandom().nextInt(-searchHeight, searchHeight + 1) + blockY;
				int sampleZ = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockZ;
				
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY, sampleZ);
				Block block = BlockRegistry.getBlock(blockId);
				if(targetBlocks.contains(block.getName())) {
					// We've found a block
					selectX = sampleX;
					selectY = sampleY;
					selectZ = sampleZ;
					break;
				}
			}
		}
		
		if(selectX == Integer.MIN_VALUE)
			return false; // We didn't find a block to target
		
		isMoving = true;
		isStaying = false;
		entity.getAI().target = new EntityTargetPosition(((float) selectX) + 0.5f + targetOffsetX,
														((float) selectY) + 0.5f + targetOffsetY,
														((float) selectZ) + 0.5f + targetOffsetZ);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isMoving = false;
		isStaying = false;
		if(tickCounter > 0)
			tickCounter--;
	}

}
