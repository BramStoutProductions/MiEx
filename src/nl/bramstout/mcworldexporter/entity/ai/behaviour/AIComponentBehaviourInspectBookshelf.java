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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil.CollisionResult;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourInspectBookshelf extends AIComponent{

	/**
	 * The distance in blocks within the mob considers it has reached the goal.
	 * This is the wiggle room to stop the AI from bouncing back and forth trying
	 * to reach a specific spot.
	 */
	public float goalRadius;
	/**
	 * The number of blocks each tick that the mob will check within its search
	 * range and height for a valid block to move to. A value of 0 will have
	 * the mob check every block within range in one tick.
	 */
	public int searchCount;
	/**
	 * The height that the mob will search for bookshelves.
	 */
	public int searchHeight;
	/**
	 * The distance in blocks the mob will look for bookshelves to inspect.
	 */
	public int searchRange;
	
	private boolean isInspecting;
	
	public AIComponentBehaviourInspectBookshelf(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isInspecting = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isInspecting) {
			if(entity.getAI().target == null) {
				isInspecting = false;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= (goalRadius * goalRadius)) {
				// Reached the bookshelf
				entity.getAI().target = null;
				isInspecting = false;
				return false;
			}
			// Still getting there.
			return true;
		}
		
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		
		int bookshelfX = Integer.MIN_VALUE;
		int bookshelfY = Integer.MIN_VALUE;
		int bookshelfZ = Integer.MIN_VALUE;
		
		// Let's find a bookshelf. There are two possible strategies.
		// Randomly selecting blocks or just checking everything.
		if(searchCount <= 0) {
			// Check everything.
			for(int sampleY = -searchHeight; sampleY <= searchHeight; ++sampleY) {
				for(int sampleZ = -searchRange; sampleZ <= searchRange; ++sampleZ) {
					for(int sampleX = -searchRange; sampleX <= searchRange; ++sampleX) {
						int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX + blockX, sampleY + blockY, sampleZ + blockZ);
						Block block = BlockRegistry.getBlock(blockId);
						if(block.getName().equals("minecraft:bookshelf") || block.getName().equals("minecraft:chiseled_bookshelf")) {
							bookshelfX = sampleX + blockX;
							bookshelfY = sampleY + blockY;
							bookshelfZ = sampleZ + blockZ;
							break;
						}
					}
					if(bookshelfX != Integer.MIN_VALUE)
						break;
				}
				if(bookshelfX != Integer.MIN_VALUE)
					break;
			}
		}else {
			// Randomly select blocks.
			for(int attempt = 0; attempt < searchCount; ++attempt) {
				int sampleX = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockX;
				int sampleY = entity.getRandom().nextInt(-searchHeight, searchHeight + 1) + blockY;
				int sampleZ = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockZ;
				
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY, sampleZ);
				Block block = BlockRegistry.getBlock(blockId);
				if(block.getName().equals("minecraft:bookshelf") || block.getName().equals("minecraft:chiseled_bookshelf")) {
					bookshelfX = sampleX;
					bookshelfY = sampleY;
					bookshelfZ = sampleZ;
					break;
				}
			}
		}
		
		if(bookshelfX == Integer.MIN_VALUE)
			return false; // Didn't find a bookshelf.
		
		CollisionResult res = new CollisionResult();
		// We've found a bookshelf, now we need to a spot for the mob to move to
		// We can check the four different directions
		for(Direction dir : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			int sampleX = bookshelfX + dir.x;
			int sampleZ = bookshelfZ + dir.z;
			for(int sampleY = bookshelfY; sampleY >= bookshelfY - 3; --sampleY) {
				if(EntityUtil.standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, ((float) sampleY), ((float) sampleZ) + 0.5f)) {
					if(EntityUtil.isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, ((float) sampleY), ((float) sampleZ) + 0.5f, res)) {
						// We've found a spot.
						isInspecting = true;
						entity.getAI().target = new EntityTargetBlock(sampleX, sampleY, sampleZ);
						return true;
					}
				}
			}
		}
		// We didn't find a good spot.
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isInspecting = false;
	}

}
