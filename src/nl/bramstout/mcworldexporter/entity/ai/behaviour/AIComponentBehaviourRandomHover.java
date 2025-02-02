package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil.CollisionResult;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRandomHover extends AIComponent{

	/**
	 * The search radius on the XZ axis.
	 */
	public int searchDistanceXZ;
	/**
	 * The search radius on the Y axis.
	 */
	public int searchDistanceY;
	/**
	 * Y offset added to the select random point to wander to.
	 */
	public float yOffset;
	/**
	 * The minimum height above the surface which the entity will try to maintain. 
	 */
	public float minHoverHeight;
	/**
	 * The maximum height above the surface which the entity will try to maintain.
	 */
	public float maxHoverHeight;
	/**
	 * A random value to dtermine when to randomly move somewhere.
	 * This has a 1/interval chance to choose this goal.
	 */
	public int interval;
	
	private boolean isFloating;
	
	public AIComponentBehaviourRandomHover(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFloating = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isFloating) {
			if(entity.getAI().target == null) {
				isFloating = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
				return false;
			}
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
			return true;
		}
		if(!EntityUtil.randomChance(entity, interval, deltaTime))
			return false;
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		CollisionResult res = new CollisionResult();
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchDistanceXZ, searchDistanceXZ+1) + (int) Math.floor(posX);
			int sampleY = entity.getRandom().nextInt(-searchDistanceY, searchDistanceY+1) + (int) Math.floor(posY);
			int sampleZ = entity.getRandom().nextInt(-searchDistanceXZ, searchDistanceXZ+1) + (int) Math.floor(posZ);
			if(EntityUtil.isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY, ((float) sampleZ) + 0.5f, res))
				continue;
			
			// Find the surface
			int surfaceY = sampleY;
			for(; surfaceY > sampleY - ((int) maxHoverHeight) - 2; --surfaceY) {
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(sampleX, surfaceY, sampleZ);
				if(blockId != 0)
					break;
			}
			float distanceFromGround = ((float) (sampleY - surfaceY)) - 0.5f;
			if(distanceFromGround < minHoverHeight || distanceFromGround > maxHoverHeight)
				continue;
			
			isFloating = true;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
			entity.getAI().target = new EntityTargetPosition(((float) sampleX) + 0.5f, ((float) sampleY) + 0.5f, ((float) sampleZ) + 0.5f);
			return true;
		}
		
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isFloating)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
		isFloating = false;
	}
	
}
