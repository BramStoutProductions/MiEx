package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentFollowRange extends AIComponent{

	public float radius;
	public float maxDistance;
	
	public AIComponentFollowRange(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FollowRangeRadius", radius));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FollowRangeMaxDistance", maxDistance));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FollowRangeRadius", 0f));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FollowRangeMaxDistance", 0f));
	}

}
