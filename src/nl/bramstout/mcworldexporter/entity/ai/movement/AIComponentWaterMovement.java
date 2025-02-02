package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentWaterMovement extends AIComponent{

	/**
	 * Drag factor to determine movement speed when in water.
	 */
	public float dragFactor;
	
	public AIComponentWaterMovement(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("WaterMovementDragFactor", dragFactor));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("WaterMovementDragFactor", 0.8f));
	}

}
