package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentPushable extends AIComponent{

	/**
	 * Whether the entity can be pushed by other entities.
	 */
	public boolean isPushable;
	/**
	 * Whether the entity can be pushed by pistons safely.
	 */
	public boolean isPushableByPiston;
	
	public AIComponentPushable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsPushable", (byte) 1));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsPushable", (byte) 0));
	}

}
