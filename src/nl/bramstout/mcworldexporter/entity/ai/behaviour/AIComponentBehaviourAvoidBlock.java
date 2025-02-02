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
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourAvoidBlock extends AIComponent{

	/**
	 * Event to call when entity is running away from the block.
	 */
	public EntityEvent onEscape;
	/**
	 * The maximum distance to look for a block in the y axis.
	 */
	public int searchHeight;
	/**
	 * The maximum distance to look for a block in the xz axis.
	 */
	public int searchRange;
	/**
	 * List of blocks to avoid
	 */
	public List<String> blocks;
	/**
	 * Number of ticks before it checks again.
	 */
	public int tickInterval;
	
	private int tickCounter;
	private boolean prevReturn;
	
	public AIComponentBehaviourAvoidBlock(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		tickCounter = -1;
		prevReturn = false;
		blocks = new ArrayList<String>();
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(tickCounter == -1) {
			tickCounter = tickInterval;
			return prevReturn;
		}
		tickCounter--;
		if(tickCounter > 0)
			return prevReturn;
		
		// Time to do our check and reset tickCounter
		tickCounter = tickInterval;
		
		int centerX = (int) Math.floor(entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value);
		int centerY = (int) Math.floor(entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value);
		int centerZ = (int) Math.floor(entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value);
		
		int blockX = Integer.MIN_VALUE;
		@SuppressWarnings("unused")
		int blockY = Integer.MIN_VALUE;
		int blockZ = Integer.MIN_VALUE;
		int closestDistance = Integer.MAX_VALUE;
		
		for(int dy = -searchHeight; dy <= searchHeight; ++dy) {
			for(int dz = -searchRange; dz <= searchRange; ++dz) {
				for(int dx = -searchRange; dx <= searchRange; ++dx) {
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(centerX + dx, centerY + dy, centerZ + dz);
					Block block = BlockRegistry.getBlock(blockId);
					if(blocks.contains(block.getName())) {
						int distance = dx * dx + dy * dy + dz * dz;
						if(distance < closestDistance) {
							blockX = centerX + dx;
							blockY = centerY + dy;
							blockZ = centerZ + dz;
							closestDistance = distance;
						}
					}
				}
			}
		}
		
		if(closestDistance == Integer.MAX_VALUE) {
			prevReturn = false;
			return false; // We couldn't find a block to avoid, so we don't take over the AI.
		}
		
		// Now let's find a new location to run towards
		float dx = blockX - centerX;
		float dz = blockZ - centerZ;
		float length = (float) Math.sqrt(dx * dx + dz * dz);
		dx /= length;
		dz /= length;
		
		// Search for a spot 16 blocks in the opposite direction
		dx *= -16f;
		dz *= -16f;
		
		int searchX = centerX + ((int) dx);
		int searchY = centerY;
		int searchZ = centerZ + ((int) dz);
		
		// For now we'll just put that as the target, but really
		// we want to do a quick search for an actual spot.
		entity.getAI().target = new EntityTargetBlock(searchX, searchY, searchZ);
		onEscape.fireEvent(entity);
		prevReturn = true;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		tickCounter = -1;
		prevReturn = false;
	}

}
