package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentGroundOffset extends AIComponent{

	public float offset;
	
	public AIComponentGroundOffset(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("GroundOffset", offset));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("GroundOffset", 0f));
	}

}
