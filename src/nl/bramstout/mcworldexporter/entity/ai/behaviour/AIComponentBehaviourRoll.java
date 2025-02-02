package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetPosition;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourRoll extends AIComponent{

	/**
	 * The probability that the mob will use the goal.
	 */
	public float probability;
	
	private boolean isRolling;
	
	public AIComponentBehaviourRoll(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		if(isRolling) {
			if(entity.getAI().target == null) {
				isRolling = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
				return false;
			}
			float targetX = entity.getAI().target.getPosX(time);
			float targetY = entity.getAI().target.getPosY(time);
			float targetZ = entity.getAI().target.getPosZ(time);
			float distanceSquared = (targetX - posX) * (targetX - posX) + 
									(targetY - posY) * (targetY - posY) + 
									(targetZ - posZ) * (targetZ - posZ);
			if(distanceSquared <= 0.5f) {
				isRolling = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
				return false;
			}
			
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 1)));
			return true;
		}
		
		if(!EntityUtil.standingOnSolidBlock(entity, posX, posY, posZ)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
			return false;
		}
		
		if(!EntityUtil.randomChance(entity, probability, deltaTime)) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
			return false;
		}
		
		// Find the forward direction
		float yaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		Matrix rotMatrix = Matrix.rotateY(yaw);
		Vector3f forward = rotMatrix.transformDirection(new Vector3f(0f, 0f, 1f));
		float rollDistance = 2f;
		forward = forward.multiply(rollDistance);
		
		entity.getAI().target = new EntityTargetPosition(posX + forward.x, posY, posZ + forward.z);
		
		isRolling = true;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 1)));
		
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isRolling)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsRolling", ((byte) 0)));
		isRolling = false;
	}

}
