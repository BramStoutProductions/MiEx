package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRandomLookAroundAndSit extends AIComponent{

	/**
	 * The probability of randomly looking around.
	 */
	public float probability;
	/**
	 * The cooldown in seconds before the goal can be used again.
	 */
	public float cooldown;
	/**
	 * The minimum amount of random looks before stopping.
	 */
	public int minLookCount;
	/**
	 * The maximum amount of random looks before stopping.
	 */
	public int maxLookCount;
	/**
	 * The minimum amount of time in ticks to look in a single direction.
	 */
	public int minLookTime;
	/**
	 * The maximum amount of time in ticks to look in a single direction.
	 */
	public int maxLookTime;
	/**
	 * The left most angle the entity can look at.
	 */
	public float minAngleOfViewHorizontal;
	/**
	 * The right most angle the entity can look at.
	 */
	public float maxAngleOfViewHorizontal;
	
	private boolean isLooking;
	private float nextTry;
	private int lookTimer;
	private int looksLeft;
	
	public AIComponentBehaviourRandomLookAroundAndSit(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isLooking = false;
		nextTry = -1f;
		lookTimer = 0;
		looksLeft = 0;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isLooking) {
			if(looksLeft <= 0) {
				isLooking = false;
				nextTry = time + cooldown;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
				entity.getAI().target = null;
				return false;
			}
			lookTimer--;
			if(lookTimer <= 0) {
				looksLeft--;
				
				float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
				float lookAngle = entity.getRandom().nextFloat() * (maxAngleOfViewHorizontal - minAngleOfViewHorizontal) + minAngleOfViewHorizontal;
				lookAngle += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
				float vx = (float) -Math.sin(Math.toRadians(lookAngle));
				float vz = (float) Math.cos(Math.toRadians(lookAngle));
				entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY, posZ + vz * 16f);
				entity.getAI().target.look = true;
				entity.getAI().target.move = false;
				entity.getAI().target.minLookPitch = 0f;
				entity.getAI().target.minLookYaw = -minAngleOfViewHorizontal;
				entity.getAI().target.maxLookPitch = 0f;
				entity.getAI().target.maxLookYaw = maxAngleOfViewHorizontal;
				lookTimer = entity.getRandom().nextInt(minLookTime, maxLookTime);
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
			return true;
		}
		
		if(time < nextTry) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			nextTry = time + cooldown;
			return false;
		}
		
		// We're going to sit and look around
		isLooking = true;
		entity.getAI().target = null;
		looksLeft = entity.getRandom().nextInt(minLookCount, maxLookCount);
		lookTimer = entity.getRandom().nextInt(minLookTime, maxLookTime);
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		float lookAngle = entity.getRandom().nextFloat() * (maxAngleOfViewHorizontal - minAngleOfViewHorizontal) + minAngleOfViewHorizontal;
		lookAngle += entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float vx = (float) -Math.sin(Math.toRadians(lookAngle));
		float vz = (float) Math.cos(Math.toRadians(lookAngle));
		entity.getAI().target = new EntityTargetPosition(posX + vx * 16f, posY, posZ + vz * 16f);
		entity.getAI().target.look = true;
		entity.getAI().target.move = false;
		entity.getAI().target.minLookPitch = 0f;
		entity.getAI().target.minLookYaw = -minAngleOfViewHorizontal;
		entity.getAI().target.maxLookPitch = 0f;
		entity.getAI().target.maxLookYaw = maxAngleOfViewHorizontal;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isLooking)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
		isLooking = false;
		nextTry = -1f;
	}

}
