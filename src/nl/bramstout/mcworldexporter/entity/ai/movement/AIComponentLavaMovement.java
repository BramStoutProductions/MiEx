package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentLavaMovement extends AIComponent{

	/**
	 * Movement speed in blocks per tick when moving over a lava block
	 */
	public float speed;
	
	public AIComponentLavaMovement(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("LavaMovementSpeed", speed));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("LavaMovementSpeed", 0f));
	}

}
