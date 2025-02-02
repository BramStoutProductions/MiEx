package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourFindCover extends AIComponent{

	/**
	 * Time in seconds the mob has to wait before using the goal again.
	 */
	public float cooldownTime;
	
	private int cooldownTicks;
	private boolean isFindingCover;
	
	public AIComponentBehaviourFindCover(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		cooldownTicks = 0;
		isFindingCover = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(EntityUtil.isUnderCover(entity, posX, posY, posZ)) {
			isFindingCover = false;
			return false;
		}
		
		// Entity isn't under cover, so let's find a place that is.

		if(isFindingCover) {
			// Entity is already finding cover, so let's check if we need to find a new target
			if(entity.getAI().target != null) {
				if(EntityUtil.isUnderCover(entity, entity.getAI().target.getPosX(time), entity.getAI().target.getPosY(time), 
											entity.getAI().target.getPosZ(time))) {
					// Target is still under cover, so we can keep using it.
					return true;
				}
			}
		}
		
		EntityTargetBlock target = EntityUtil.FindTargetUnderCover(16, 4, entity, posX, posY, posZ);
		if(target == null)
			return false;
		
		isFindingCover = true;
		entity.getAI().target = target;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isFindingCover = false;
	}

}
