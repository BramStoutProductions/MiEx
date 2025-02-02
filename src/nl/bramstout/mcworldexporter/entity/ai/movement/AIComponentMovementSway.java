package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityMovementUtil;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentMovementSway extends AIComponent{

	/**
	 * The maximum number in degrees the entity can turn per tick.
	 */
	public float maxTurn;
	/**
	 * Strength of the sway movement
	 */
	public float swayAmplitude;
	/**
	 * Multiplier for the frequency of the sway movement
	 */
	public float swayFrequency;
	
	public AIComponentMovementSway(String name) {
		super(name, PriorityGroup.MOVEMENT, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		// Remove sway from last frame
		Vector3f sway = getSway(entity, time, deltaTime);
		posX -= sway.x;
		posY -= sway.y;
		posZ -= sway.z;
		
		if(entity.getAI().path == null) {
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			
			// Add sway
			sway = getSway(entity, time, 0f);
			entity.getAnimation().getAnimPosX().getClosestKeyframeAtTime(time).value += sway.x;
			entity.getAnimation().getAnimPosY().getClosestKeyframeAtTime(time).value += sway.y;
			entity.getAnimation().getAnimPosZ().getClosestKeyframeAtTime(time).value += sway.z;
			
			return true; // No need to move if there is no target.
		}
		
		Vector3f target = EntityMovementUtil.getNextPathTarget(entity, posX, posY, posZ);
		if(target == null) {
			// No valid path, so set it to null.
			entity.getAI().path = null;
			entity.getAI().target = null;
			EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
			
			// Add sway
			sway = getSway(entity, time, 0f);
			entity.getAnimation().getAnimPosX().getClosestKeyframeAtTime(time).value += sway.x;
			entity.getAnimation().getAnimPosY().getClosestKeyframeAtTime(time).value += sway.y;
			entity.getAnimation().getAnimPosZ().getClosestKeyframeAtTime(time).value += sway.z;
			
			return true; // No need to move if there is no target.
		}
		
		if(EntityUtil.isInLiquid(entity, posX, posY, posZ)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 1));
			EntityMovementUtil.swim(entity, time, deltaTime, target, posX, posY, posZ, maxTurn);
		}else {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsSwimming", (byte) 0));
		}
		
		EntityMovementUtil.simulatePhysics(entity, time, deltaTime, posX, posY, posZ);
		
		// Add sway
		sway = getSway(entity, time, 0f);
		entity.getAnimation().getAnimPosX().getClosestKeyframeAtTime(time).value += sway.x;
		entity.getAnimation().getAnimPosY().getClosestKeyframeAtTime(time).value += sway.y;
		entity.getAnimation().getAnimPosZ().getClosestKeyframeAtTime(time).value += sway.z;
		
		return true;
	}
	
	private Vector3f getSway(Entity entity, float time, float deltaTime) {
		float yaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float pitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		Matrix rotMatrix = Matrix.rotate(pitch, yaw, 0f);
		Vector3f sway = rotMatrix.transformDirection(new Vector3f((float) Math.sin((time - deltaTime) * Math.PI * swayFrequency) * 
																	swayAmplitude, 0f, 0f));
		return sway;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
