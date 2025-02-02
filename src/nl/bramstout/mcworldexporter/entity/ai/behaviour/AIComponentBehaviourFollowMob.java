package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;

public class AIComponentBehaviourFollowMob extends AIComponent{

	/**
	 * The distance to search for mobs of the same type.
	 */
	public float searchRange;
	/**
	 * The distance from the mob it's following at which it'll stop.
	 */
	public float stopDistance;
	
	private boolean isFollowing;
	
	public AIComponentBehaviourFollowMob(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFollowing = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isFollowing) {
			if(entity.getAI().target == null) {
				isFollowing = false;
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			
			float distanceSquared = (targetX - posX) * (targetX - posY) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= (stopDistance * stopDistance)) {
				entity.getAI().target = null;
				isFollowing = false;
				return false;
			}
			return true;
		}
		
		List<Entity> fellowEntities = new ArrayList<Entity>();
		float avgX = 0f;
		float avgY = 0f;
		float avgZ = 0f;
		for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange((int) posX, (int) posZ, (int) searchRange)) {
			for(Entity entity2 : entities) {
				if(entity2 == entity)
					continue;
				if(entity2.getId().equals(entity.getId())) {
					float posX2 = entity2.getX();
					float posY2 = entity2.getY();
					float posZ2 = entity2.getZ();
					if(entity2.getAnimation() != null) {
						posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
						posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
						posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
					}
					
					float distanceSquared = (posX2 - posX) * (posX2 - posX) + 
											(posY2 - posY) * (posY2 - posY) + 
											(posZ2 - posZ) * (posZ2 - posZ);
					if(distanceSquared <= (searchRange * searchRange)) {
						fellowEntities.add(entity2);
						avgX += posX2;
						avgY += posY2;
						avgZ += posZ2;
					}
				}
			}
		}
		if(fellowEntities.isEmpty()) {
			// No entity to follow
			return false;
		}
		
		avgX /= (float) fellowEntities.size();
		avgY /= (float) fellowEntities.size();
		avgZ /= (float) fellowEntities.size();
		float closestDistance = Float.MAX_VALUE;
		Entity closestEntity = null;
		// Let's find the entity that's the closest to this average
		for(Entity entity2 : fellowEntities) {
			float posX2 = entity2.getX();
			float posY2 = entity2.getY();
			float posZ2 = entity2.getZ();
			if(entity2.getAnimation() != null) {
				posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
				posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
				posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
			}
			
			float distanceSquared = (posX2 - avgX) * (posX2 - avgX) + 
									(posY2 - avgY) * (posY2 - avgY) + 
									(posZ2 - avgZ) * (posZ2 - avgZ);
			if(distanceSquared < closestDistance) {
				closestDistance = distanceSquared;
				closestEntity = entity2;
			}
		}
		if(closestEntity == null)
			return false;
		
		// We've got a mob to follow
		entity.getAI().target = new EntityTargetEntity(closestEntity);
		isFollowing = true;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isFollowing = true;
	}

}
