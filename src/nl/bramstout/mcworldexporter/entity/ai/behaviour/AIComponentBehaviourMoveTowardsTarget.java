package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;

public class AIComponentBehaviourMoveTowardsTarget extends AIComponent{

	/**
	 * Defines the radius in blocks that the mob tries to be from the target.
	 * A value of 0 means it tries to occupy the same block as the target.
	 */
	public float withinRadius;
	
	public AIComponentBehaviourMoveTowardsTarget(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(entity.getAI().target == null)
			return false;
		if(!(entity.getAI().target instanceof EntityTargetEntity))
			return false;
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		float targetX = entity.getAI().target.getPosX(time);
		float targetY = entity.getAI().target.getPosY(time);
		float targetZ = entity.getAI().target.getPosZ(time);
		float distanceSquared = (targetX - posX) * (targetX - posX) + 
								(targetY - posY) * (targetY - posY) + 
								(targetZ - posZ) * (targetZ - posZ);
		if(distanceSquared <= Math.max(1.0f, withinRadius * withinRadius)) {
			entity.getAI().target = null;
			return false;
		}
		
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
