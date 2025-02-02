package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourRiseToLiquidLevel extends AIComponent{

	/**
	 * Vertical offset from the liquid
	 */
	public float liquidYOffset;
	/**
	 * Displacement for how much the entity will move up in the vertical axis.
	 */
	public float riseDelta;
	/**
	 * Displacement for how much the entity will move down in the vertical axis.
	 */
	public float sinkDelta;
	
	public AIComponentBehaviourRiseToLiquidLevel(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ)) {
			// Set a new target a bit further up
			entity.getAI().target = new EntityTargetPosition(posX, posY + liquidYOffset, posZ);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
