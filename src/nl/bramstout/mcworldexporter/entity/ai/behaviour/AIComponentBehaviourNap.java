package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;

public class AIComponentBehaviourNap extends AIComponent{

	/**
	 * The minimum time in seconds the mob has to wait before
	 * using the goal again.
	 */
	public float minCooldown;
	/**
	 * The maximum time in seconds the mob has to wait before
	 * using the goal again.
	 */
	public float maxCooldown;
	/**
	 * The block distance in x and z that will be checked for
	 * mobs that this mob detects.
	 */
	public float mobDetectionDistance;
	/**
	 * The block distance in y that will be checked for mobs
	 * that this mob detects.
	 */
	public float mobDetectionHeight;
	/**
	 * Filter that has to be true for this mob to be able to nap.
	 */
	public EntityFilter canNapFilter;
	/**
	 * Filter that has to be true for an entity to not wake this
	 * mob up.
	 */
	public EntityFilter wakeMobExceptionFilter;
	
	private boolean isNapping;
	private float nextSleep;
	
	public AIComponentBehaviourNap(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isNapping = false;
		nextSleep = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isNapping) {
			if(shouldWakeUp(entity, time)) {
				isNapping = false;
				return false;
			}
			return true;
		}
		
		if(time >= nextSleep) {
			if(nextSleep == -1f) {
				nextSleep = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
				return false;
			}
			nextSleep = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
			
			if(!canNapFilter.testFilter(entity))
				return false;
			if(shouldWakeUp(entity, time))
				return false;
			
			isNapping = true;
			return true;
		}
		return false;
	}
	
	private boolean shouldWakeUp(Entity entity, float time) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange(
									(int) posX, (int) posZ, (int) mobDetectionDistance)) {
			for(Entity entity2 : entities) {
				float posX2 = entity2.getX();
				float posY2 = entity2.getY();
				float posZ2 = entity2.getZ();
				if(entity2.getAnimation() != null) {
					posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
					posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
					posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
				}
				if(Math.abs(posX2 - posX) > mobDetectionDistance || Math.abs(posZ2 - posZ) > mobDetectionDistance ||
						Math.abs(posY2 - posY) > mobDetectionHeight)
					continue;
				if(wakeMobExceptionFilter.testFilter(entity2))
					continue;
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isNapping = false;
		nextSleep = -1f;
	}

}
