package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourSwimIdle extends AIComponent{

	/**
	 * Amount of time in seconds to stay idle
	 */
	public float idleTime;
	/**
	 * Percent chance this entity will go idle. 1.0 = 100%
	 */
	public float successRate;
	
	private boolean isIdling;
	private float stopIdling;
	
	public AIComponentBehaviourSwimIdle(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isIdling = false;
		stopIdling = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isIdling) {
			if(time >= stopIdling) {
				isIdling = false;
				return false;
			}
			return true;
		}
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(!EntityUtil.isInLiquid(entity, posX, posY, posZ))
			return false;
		
		if(EntityUtil.randomChance(entity, successRate, deltaTime)) {
			isIdling = true;
			stopIdling = time + idleTime;
			return true;
		}
		return false;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isIdling = false;
	}

}
