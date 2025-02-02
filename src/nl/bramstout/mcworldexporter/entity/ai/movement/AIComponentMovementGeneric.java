package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityMovementUtil;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentMovementGeneric extends AIComponent{

	/**
	 * The maximum number in degrees the entity can turn per tick.
	 */
	public float maxTurn;
	
	public AIComponentMovementGeneric(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
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
		
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 1));
			EntityMovementUtil.swim(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		}else {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 0));
			
			boolean isFlying = false;
			NbtTagByte isFlyingTag = (NbtTagByte) entity.getProperties().get("IsFlying");
			if(isFlyingTag != null)
				isFlying = isFlyingTag.getData() > 0;
			
			if(isFlying)
				EntityMovementUtil.fly(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
			else
				EntityMovementUtil.walk(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		}
		
		EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
