package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;

public class AIComponentMovementRail extends AIComponent{

	/**
	 * Maximum speed in blocks per tick that this entity will move at when on the rail.
	 */
	public float maxSpeed;
	
	public AIComponentMovementRail(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		/*if(entity.getAI().path == null) {
			EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);
			lastTargetNodeIndex = -1;
			return true; // No need to move if there is no target.
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		int closestNodeIndex = entity.getAI().path.getClosestNode(posX, posY, posZ);
		int targetNodeIndex = Math.min(closestNodeIndex + 1, entity.getAI().path.getSize() - 1);
		if(targetNodeIndex < 0) {
			// Invalid index, so we just don't move.
			EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);
			lastTargetNodeIndex = -1;
			return true;
		}
		if(lastTargetNodeIndex != -1) {
			// If we've been on the same target node index for a while,
			// then we're either stuck or at the end goal.
			if((time - lastTargetNodeIndexChange) > 1f) {
				entity.getAI().path = null;
				entity.getAI().target = null;
			}
		}
		if(lastTargetNodeIndex != targetNodeIndex) {
			lastTargetNodeIndex = targetNodeIndex;
			lastTargetNodeIndexChange = time;
		}
		PathNode target = entity.getAI().path.getNode(targetNodeIndex);
		
		EntityMovementUtil.rials(entity, time, deltaTime, target, posX, posY, posZ);
		
		EntityMovementUtil.simulateRailPhysics(entity, time, deltaTime);*/
		// TODO: Implement
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
