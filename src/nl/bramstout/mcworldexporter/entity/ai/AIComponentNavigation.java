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

import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil.CollisionResult;
import nl.bramstout.mcworldexporter.entity.ai.pathfinding.PathFinder;
import nl.bramstout.mcworldexporter.entity.ai.pathfinding.PathFinderHook;
import nl.bramstout.mcworldexporter.entity.ai.pathfinding.PathNode;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;

public class AIComponentNavigation extends AIComponent implements PathFinderHook{

	/**
	 * Tells the pathfinder to avoid blocks that cause damage when finding a path.
	 */
	public boolean avoidDamageBlocks;
	/**
	 * Tells the pathfinder to avoid portals (like nether portals) when finding a path.
	 */
	public boolean avoidPortals;
	/**
	 * Whether or not the pathfinder should avoid tiles that are exposed to the sun
	 * when creating paths.
	 */
	public boolean avoidSun;
	/**
	 * Tells the pathfinder to avoid water when creating a path.
	 */
	public boolean avoidWater;
	/**
	 * Tells the pathfinder which blocks to avoid when creating a path.
	 */
	public List<String> blocksToAvoid;
	/**
	 * Tells the pathfinder whether or not it can jump out of water (like a dolphin)
	 */
	public boolean canBreach;
	/**
	 * Tells the pathfinder that it can path through a closed door and break it.
	 */
	public boolean canBreakDoors;
	/**
	 * Tells the pathfinder whether or not it can jump up blocks.
	 */
	public boolean canJump;
	/**
	 * Tells the pathfinder that it can path through a closed door assuming the AI will open the door.
	 */
	public boolean canOpenDoors;
	/**
	 * Tells the pathfinder that it can path through a closed iron door assuming the AI will open the door.
	 */
	public boolean canOpenIronDoors;
	/**
	 * Whether a path can be created through a door.
	 */
	public boolean canPassDoors;
	/**
	 * Tells the pathfinder that it can start pathing when in the air.
	 */
	public boolean canPathFromAir;
	/**
	 * Tells the pathfinder whether or not it can travel on the surface of the lava.
	 */
	public boolean canPathOverLava;
	/**
	 * Tells the pathfinder whether or not it can travel over the surface of the water.
	 */
	public boolean canPathOverWater;
	/**
	 * Tells the pathfinder whether or not it will be pulled down by gravity while in water.
	 */
	public boolean canSink;
	/**
	 * Tells the pathfinder whether or not it can path anywhere through water and
	 * plays swimming animation along that path.
	 */
	public boolean canSwim;
	/**
	 * Tells the pathfinder whether or not it can walk on the ground outside water.
	 */
	public boolean canWalk;
	/**
	 * Tells the pathfinder whether or not it can travel in lava like walking on ground.
	 */
	public boolean canWalkInLava;
	/**
	 * Tells the pathfinder whether or not it can walk on the ground under water.
	 */
	public boolean isAmphibious;
	
	private PathFinder pathFinder;
	private EntityTarget currentTarget;
	private float nextRePath;
	private List<String> woodenDoors;
	private Entity currentEntity;
	private AIComponentPreferredPath preferredPathComponent;
	
	public AIComponentNavigation(String name) {
		super(name, PriorityGroup.NAVIGATION, 0, 0);
		pathFinder = new PathFinder(this);
		currentTarget = null;
		nextRePath = -1f;
		woodenDoors = Tags.getNamesInTag("minecraft:blocks/doors");
		currentEntity = null;
		preferredPathComponent = null;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime, boolean forceEnable) {
		currentEntity = entity;
		for(AIComponentGroup grp : entity.getAI().getActiveComponentGroups()) {
			for(AIComponent comp : grp.getComponents()) {
				if(comp instanceof AIComponentPreferredPath)
					preferredPathComponent = (AIComponentPreferredPath) comp;
			}
		}
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		float targetX = 0f;
		float targetY = 0f;
		float targetZ = 0f;
		if(entity.getAI().target != null) {
			targetX = entity.getAI().target.getPosX(time);
			targetY = entity.getAI().target.getPosY(time);
			targetZ = entity.getAI().target.getPosZ(time);
		}
		
		if(currentTarget == entity.getAI().target && (entity.getAI().target == null || entity.getAI().target.move) && time < nextRePath)
			return true; // No need to repath
		currentTarget = entity.getAI().target;
		
		nextRePath = time + 1.5f; // Repath in one and a half seconds.
		
		if(entity.getAI().target == null || !entity.getAI().target.move) {
			// No target, so we don't need a path
			entity.getAI().path = null;
			return true;
		}
		
		PathNode startNode = new PathNode((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ), 0f, 0f, null);
		PathNode endNode = new PathNode((int) Math.floor(targetX), (int) Math.floor(targetY), (int) Math.floor(targetZ), 0f, 0f, null);
		entity.getAI().path = pathFinder.pathFind(startNode, endNode);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		currentTarget = null;
		nextRePath = -1f;
	}

	@Override
	public float getCost(int x, int y, int z, int dx, int dy, int dz) {
		float cost = 1f;
		try {
			Chunk chunk = MCWorldExporter.getApp().getWorld().getChunkFromBlockPosition(x, z);
			float maxFall = 1f;
			for(int layer = 0; layer < chunk.getLayerCount(); ++layer) {
				int blockId = chunk.getBlockId(x, y, z, layer);
				int blockIdBelow = chunk.getBlockId(x, y - 1, z, layer);
				int blockIdAbove = chunk.getBlockId(x, y + 1, z, layer);
				Block block = BlockRegistry.getBlock(blockId);
				Block blockBelow = BlockRegistry.getBlock(blockIdBelow);
				Block blockAbove = BlockRegistry.getBlock(blockIdAbove);
				if(preferredPathComponent != null) {
					cost = preferredPathComponent.defaultBlockCost;
					if(y > 0 && !block.isLiquid()) {
						cost += preferredPathComponent.jumpCost;
					}
					Float blockCost = preferredPathComponent.preferredPathBlocks.getOrDefault(blockBelow.getName(), cost);
					cost = blockCost.floatValue();
					maxFall = preferredPathComponent.maxFallBlocks;
				}
				
				if(avoidSun) {
					int height = MCWorldExporter.getApp().getWorld().getHeight(x, z);
					if(height < y)
						return -1f; // In the sun
				}
				if(avoidWater) {
					if(block.isLiquid() || blockBelow.isLiquid())
						return -1f; // In water.
				}
				if(blocksToAvoid.contains(block.getName()) || blocksToAvoid.contains(blockBelow.getName()))
					return -1f; // Avoid these blocks.
				if(!canBreach) {
					if(blockBelow.isLiquid() && !block.isLiquid())
						return -1f; // Can't be in the block just above water.
					if(block.isLiquid() && !blockAbove.isLiquid() && blockBelow.isLiquid())
						return -1f; // Can't be in the top block of water, unless the water only one block deep.
				}
				if(canBreakDoors || canOpenDoors || canPassDoors) {
					if(Tags.isInList(block.getName(), woodenDoors)) {
						return 1f;
					}
				}
				if(canOpenIronDoors) {
					if(block.getName().equals("minecraft:iron_door")) {
						return 1f;
					}
				}
				if(!canJump) {
					if(y > 0 && !block.isLiquid()) {
						float autoStepHeight = 0.5625f;
						NbtTag autoStepHeightTag = currentEntity.getProperties().get("AutoStepHeight");
						if(autoStepHeightTag != null)
							autoStepHeight = autoStepHeightTag.asFloat();
						
						if(y > autoStepHeight)
							return -1f;
					}
				}
				if(!canPathOverLava) {
					if(blockBelow.getName().equals("minecraft:lava") && !block.getName().equals("minecraft:lava"))
						return -1f;
				}
				if(!canPathOverWater) {
					if(blockBelow.getName().equals("minecraft:water") && !block.getName().equals("minecraft:water"))
						return -1f;
				}
				if(canSink) {
					if(block.getName().equals("minecraft:water") && blockBelow.getName().equals("minecraft:water")) {
						if(y < 0)
							cost *= 0.5f; // Prefer going down.
					}
				}
				if(!canSwim) {
					if(block.isLiquid() && blockAbove.isLiquid())
						return -1f; // Only allow the top block of water.
				}
				if(!canWalk) {
					// Must be in liquid
					if(!block.isLiquid())
						return -1f;
				}
				if(canWalkInLava) {
					if(block.getName().equals("minecraft:lava") && !blockBelow.getName().equals("minecraft:lava"))
						cost *= 0.5f; // Prefer walking on the ground.
				}
				if(isAmphibious) {
					if(block.getName().equals("minecraft:water") && !blockBelow.getName().equals("minecraft:water"))
						cost *= 0.5f; // Prefer walking on the ground.
				}
				if(block.isLiquid() || blockBelow.isLiquid()) {
					cost *= 0.85f; // Prefer not swimming since it's slower.
				}
				if(EntityUtil.isCollidingWithWorld(currentEntity, ((float) x) + 0.5f, (float) y, ((float) z) + 0.5f, new CollisionResult()))
					return -1f; // Entity can't fit here.
				if(!block.isLiquid() && !blockBelow.isLiquid()) {
					if(!EntityUtil.standingOnSolidBlock(currentEntity, ((float) x) + 0.5f, (float) y, ((float) z) + 0.5f)) {
						// Entity must be on solid block, but it may fall down, so check that first.
						for(float sampleY = (float) y; sampleY >= (((float) y) - maxFall); sampleY -= 1f) {
							if(EntityUtil.standingOnSolidBlock(currentEntity, ((float) x) + 0.5f, sampleY, ((float) z) + 0.5f))
								return cost; // There's a block there to catch the entity, so it's fine to take this path.
						}
						return -1f; // Entity must be on solid block.
					}
				}
			}
		}catch(Exception ex) {}
		
		return cost;
	}

	@Override
	public float getAdditionalCost(int x, int y, int z, int goalX, int goalY, int goalZ) {
		return (float) Math.sqrt((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) + (goalZ - z) * (goalZ - z));
	}

}
