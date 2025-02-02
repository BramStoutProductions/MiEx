package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourFleeSun extends AIComponent{

	private boolean isFleeing;
	
	public AIComponentBehaviourFleeSun(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFleeing = false;
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(EntityUtil.isUnderCover(entity, posX, posY, posZ)) {
			isFleeing = false;
			return false;
		}
		
		// Entity isn't under cover, so let's find a place that is.

		if(isFleeing) {
			// Entity is already finding cover, so let's check if we need to find a new target
			if(entity.getAI().target != null) {
				if(!EntityUtil.isInSunlight(entity, entity.getAI().target.getPosX(time), entity.getAI().target.getPosY(time), 
											entity.getAI().target.getPosZ(time))) {
					// Target is still under cover, so we can keep using it.
					return true;
				}
			}
		}
		
		EntityTargetBlock target = EntityUtil.FindTargetNotInSunlight(16, 4, entity, posX, posY, posZ);
		if(target == null)
			return false;
		
		isFleeing = true;
		entity.getAI().target = target;
		return true;
	}

	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isFleeing = false;
	}

}
