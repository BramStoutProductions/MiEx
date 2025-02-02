package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityMovementUtil;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class AIComponentMovementGlide extends AIComponent{

	/**
	 * Initial speed of the entity when it starts gliding.
	 */
	public float startSpeed;
	/**
	 * Speed that the entity adjusts to when it has to turn quickly.
	 */
	public float speedWhenTurning;
	/**
	 * The maximum number in degrees the entity can turn per tick
	 */
	public float maxTurn;
	
	public AIComponentMovementGlide(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		if(entity.getAI().path == null) {
			EntityMovementUtil.glide(entity, time, deltaTime, null, posX, posY, posZ, maxTurn);
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true; // No need to move if there is no target.
		}
		Vector3f target = EntityMovementUtil.getNextPathTarget(entity, posX, posY, posZ);
		if(target == null) {
			// No valid path, so set it to null.
			entity.getAI().path = null;
			entity.getAI().target = null;
			EntityMovementUtil.glide(entity, time, deltaTime, null, posX, posY, posZ, maxTurn);
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true;
		}
		
		EntityMovementUtil.glide(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		
		EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
