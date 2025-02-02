package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRandomSitting extends AIComponent{

	/**
	 * Time in seconds the entity has to wait before using the goal again.
	 */
	public float cooldown;
	/**
	 * The minimum amount of time in seconds before the entity can stand back up.
	 */
	public float minSitTime;
	/**
	 * This is the chance that the entity will start this goal, from 0 to 1.
	 */
	public float startChance;
	/**
	 * This is the chance that the entity will stop this goal, from 0 to 1.
	 */
	public float stopChance;
	
	private boolean isSitting;
	private float stopTime;
	private float nextTry;
	
	public AIComponentBehaviourRandomSitting(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isSitting = false;
		stopTime = 0f;
		nextTry = 0f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isSitting) {
			if(time > stopTime) {
				if(EntityUtil.randomChance(entity, stopChance, deltaTime)) {
					isSitting = false;
					entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
					return false;
				}
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
			return true;
		}
		
		if(time < nextTry) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, startChance, deltaTime)) {
			nextTry = time + cooldown;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
			return false;
		}
		
		isSitting = true;
		stopTime = time + minSitTime;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isSitting)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Sitting", ((byte) 0)));
		isSitting = false;
		nextTry = 0f;
		stopTime = 0f;
	}

}
