package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourCroak extends AIComponent{

	/**
	 * The minimum time in seconds that a croak takes.
	 */
	public float minDuration;
	/**
	 * The maximum time in seconds that a croak takes.
	 */
	public float maxDuration;
	/**
	 * The minimum time between croaks.
	 */
	public float minInterval;
	/**
	 * The maximum time between croaks.
	 */
	public float maxInterval;
	/**
	 * Filter that has to be satisfied for this entity to croak.
	 */
	public EntityFilter filter;
	
	private boolean isCroaking;
	private float nextCroak;
	private float endCroak;
	
	public AIComponentBehaviourCroak(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isCroaking = false;
		nextCroak = -1f;
		endCroak = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isCroaking) {
			if(time >= endCroak) {
				isCroaking = false;
				nextCroak = entity.getRandom().nextFloat() * (maxInterval - minInterval) + minInterval + time;
				endCroak = -1f;
			}
		}else {
			if(time >= nextCroak) {
				if(nextCroak == -1f) {
					nextCroak = entity.getRandom().nextFloat() * (maxInterval - minInterval) + minInterval + time;
				}else {
					isCroaking = true;
					nextCroak = -1f;
					endCroak = entity.getRandom().nextFloat() * (maxDuration - minDuration) + minDuration + time;
				}
			}
		}
		
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsCroaking", isCroaking ? ((byte) 1) : ((byte) 0)));
		return isCroaking;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isCroaking = false;
		nextCroak = -1f;
		endCroak = -1f;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsCroaking", ((byte) 0)));
	}

}
