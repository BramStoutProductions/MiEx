package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourRandomStroll extends AIComponent{

	/**
	 * A random value to determine when to randomly move somewhere.
	 * This has a 1/interval chance to choose this goal.
	 */
	public int interval;
	/**
	 * The search radius on the XZ axis.
	 */
	public int searchDistanceXZ;
	/**
	 * The search radius on the Y axis.
	 */
	public int searchDistanceY;
	
	private boolean isStrolling;
	
	public AIComponentBehaviourRandomStroll(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isStrolling = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isStrolling) {
			if(entity.getAI().target == null) {
				isStrolling = false;
				return false;
			}
			return true;
		}
		
		if(!EntityUtil.randomChance(entity, interval, deltaTime))
			return false;
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		EntityTargetBlock target = EntityUtil.FindTarget(searchDistanceXZ, searchDistanceY, entity, posX, posY, posZ);
		if(target == null)
			return false;
		entity.getAI().target = target;
		isStrolling = true;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isStrolling = false;
	}
	
}
