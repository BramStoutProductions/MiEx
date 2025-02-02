package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentFlyingSpeed extends AIComponent{

	/**
	 * The speed in blocks per tick when flying.
	 */
	public float speed;
	
	public AIComponentFlyingSpeed(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FlyingSpeed", speed));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("FlyingSpeed", 0.4f));
	}

}
