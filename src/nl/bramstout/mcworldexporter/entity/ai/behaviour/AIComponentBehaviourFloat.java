package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourFloat extends AIComponent{

	public AIComponentBehaviourFloat(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ)) {
			entity.getAI().target = null;
			return true;
		}
		return false;
	}

	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
