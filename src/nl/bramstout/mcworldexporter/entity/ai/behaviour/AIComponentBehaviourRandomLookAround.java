package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourRandomLookAround extends AIComponent{

	/**
	 * The probability of randomly looking around.
	 */
	public float probability;
	/**
	 * The minimum amount of time in ticks to look in a single direction.
	 */
	public float minLookTime;
	/**
	 * The maximum amount of time in ticks to look in a single direction.
	 */
	public float maxLookTime;
	/**
	 * The angle in degrees that an entity can see in the Y-axis.
	 */
	public float angleOfViewHorizontal;
	/**
	 * The angle in degrees that an entity can see in the X-axis.
	 */
	public float angleOfViewVertical;
	
	private boolean isLooking;
	private float stopLooking;
	
	public AIComponentBehaviourRandomLookAround(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isLooking = false;
		stopLooking = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isLooking) {
			if(time >= stopLooking) {
				isLooking = false;
				entity.getAI().target = null;
				return false;
			}
			return true;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime)) {
			return false;
		}
		
		// We're going to sit and look around
		isLooking = true;
		stopLooking = entity.getRandom().nextFloat() * (maxLookTime - minLookTime) + minLookTime + time;
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		float lookAngleX = entity.getRandom().nextFloat() * angleOfViewVertical - angleOfViewVertical / 2f;
		float lookAngleY = entity.getRandom().nextFloat() * angleOfViewHorizontal - angleOfViewHorizontal / 2f;
		lookAngleX += entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		lookAngleY += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float vx = (float) -Math.sin(Math.toRadians(lookAngleY));
		float vy = (float) Math.sin(Math.toRadians(lookAngleX));
		float vz = (float) Math.cos(Math.toRadians(lookAngleY));
		entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY + vy * 16f, posZ + vz * 16f);
		entity.getAI().target.look = true;
		entity.getAI().target.move = false;
		entity.getAI().target.minLookPitch = -angleOfViewVertical / 2f;
		entity.getAI().target.minLookYaw = -angleOfViewHorizontal / 2f;
		entity.getAI().target.maxLookPitch = angleOfViewVertical / 2f;
		entity.getAI().target.maxLookYaw = angleOfViewHorizontal / 2f;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isLooking = false;
	}

}
