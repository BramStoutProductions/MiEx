package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBehaviourRandomSwim extends AIComponent{

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
	/**
	 * If true, the entity will avoid surface water blocks by swimming below them.
	 */
	public boolean avoidSurface;
	
	private boolean isSwimming;
	
	public AIComponentBehaviourRandomSwim(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isSwimming = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isSwimming) {
			if(entity.getAI().target == null) {
				isSwimming = false;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= 1f) {
				isSwimming = false;
				return false;
			}
			
			return true;
		}
		
		if(!EntityUtil.randomChance(entity, interval, deltaTime)) {
			return false;
		}
		
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockX;
			int sampleY = entity.getRandom().nextInt(-searchHeight, searchHeight + 1) + blockY;
			int sampleZ = entity.getRandom().nextInt(-searchRange, searchRange + 1) + blockZ;
			
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY, sampleZ);
			if(avoidSurface) {
				// Make sure that this isn't a surface water block.
				int blockIdAbove = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, sampleY + 1, sampleZ);
				if(blockIdAbove != 0)
					continue;
			}
			Block block = BlockRegistry.getBlock(blockId);
			if(!block.hasLiquid())
				continue;
			
			// We've found a block that's at the surface.
			isSwimming = true;
			entity.getAI().target = new EntityTargetPosition(((float) sampleX) + 0.5f, ((float) sampleY) + 0.5f, ((float) sampleZ) + 0.5f);
			return true;
		}
		
		// We didn't find a spot to swim to.
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isSwimming = false;
	}
	
}
