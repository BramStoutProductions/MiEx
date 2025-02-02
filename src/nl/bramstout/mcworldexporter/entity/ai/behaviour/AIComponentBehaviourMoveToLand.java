package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil.CollisionResult;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourMoveToLand extends AIComponent{

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
	
	private boolean isMoving;
	
	public AIComponentBehaviourMoveToLand(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isMoving = false;
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
				isMoving = false;
				return false;
			}
			
			return true;
		}
		
		if(!EntityUtil.isInLiquid(entity, posX, posY, posZ))
			return false;
		
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		// Entity is in liquid, so try to find a spot for it to move to.
		CollisionResult res = new CollisionResult();
		if(searchCount <= 0) {
			// Check every block in range
			for(int sampleY = -searchHeight; sampleY <= searchHeight; ++sampleY) {
				for(int sampleZ = -searchRange; sampleZ <= searchRange; ++sampleZ) {
					for(int sampleX = -searchRange; sampleX <= searchRange; ++sampleX) {
						int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX + blockX, 
																	sampleY + blockY - 1, sampleZ + blockZ);
						Block block = BlockRegistry.getBlock(blockId);
						if(block.hasLiquid())
							continue;
						if(EntityUtil.isCollidingWithWorld(entity, ((float) (sampleX + blockX)) + 0.5f, (float) (sampleY + blockY), 
																	((float) (sampleZ + blockZ)) + 0.5f, res))
							continue;
						// We've found a block on land to move to.
						
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
				
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY - 1, sampleZ);
				Block block = BlockRegistry.getBlock(blockId);
				if(block.hasLiquid())
					continue;
				if(EntityUtil.isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, (float) sampleY, ((float) sampleZ) + 0.5f, res))
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
