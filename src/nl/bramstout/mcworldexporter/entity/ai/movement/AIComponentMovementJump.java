package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityMovementUtil;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class AIComponentMovementJump extends AIComponent{

	/**
	 * Start of the range of possible delays after landing
	 * before the next jump.
	 */
	public float jumpDelayStart;
	/**
	 * End of the range of possible delays after landing
	 * before the next jump.
	 */
	public float jumpDelayEnd;
	/**
	 * The maximum number in degrees the entity can turn per tick.
	 */
	public float maxTurn;
	
	private float nextJump;
	
	public AIComponentMovementJump(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
		nextJump = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(entity.getAI().path == null) {
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true; // No need to move if there is no target.
		}
		
		Vector3f target = EntityMovementUtil.getNextPathTarget(entity, posX, posY, posZ);
		if(target == null) {
			// No valid path, so set it to null.
			entity.getAI().path = null;
			entity.getAI().target = null;
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			return true;
		}
		
		boolean canJump = time >= nextJump;
		
		boolean jumped = EntityMovementUtil.jump(entity, time, deltaTime, target, posX, posY, posZ, maxTurn, canJump);
		
		if(jumped)
			nextJump = entity.getRandom().nextFloat() * (jumpDelayEnd - jumpDelayStart) + jumpDelayStart + time;
		
		EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
