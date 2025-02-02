package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentPushThrough extends AIComponent{

	/**
	 * The distance of the entity's push-through in blocks.
	 */
	public float distance;
	
	public AIComponentPushThrough(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("PushThroughDistance", distance));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("PushThroughDistance", 0f));
	}

}
