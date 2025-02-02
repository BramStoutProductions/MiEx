package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentBehaviourRandomFly extends AIComponent{

	/**
	 * The search radius on the XZ axis.
	 */
	public int searchDistanceXZ;
	/**
	 * The search radius on the Y axis.
	 */
	public int searchDistanceY;
	/**
	 * Y offset added to the select random point to wander to.
	 */
	public float yOffset;
	
	private boolean isFloating;
	
	public AIComponentBehaviourRandomFly(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isFloating = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isFloating) {
			if(entity.getAI().target == null) {
				isFloating = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
				return false;
			}
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
			return true;
		}
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		EntityTargetBlock target = EntityUtil.FindTarget(searchDistanceXZ, searchDistanceY, entity, posX, posY, posZ);
		if(target == null)
			return false;
		entity.getAI().target = new EntityTargetBlock((int) target.getPosX(time), (int) (target.getPosY(time) + yOffset), 
													(int) target.getPosZ(time));
		isFloating = true;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(isFloating)
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsFlying", ((byte) 0)));
		isFloating = false;
	}

}
