package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;

public class AIComponentCollisionBox extends AIComponent{

	public float width;
	public float height;
	
	public AIComponentCollisionBox(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getAI().collisionBoxWidth = width;
		entity.getAI().collisionBoxHeight = height;
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
	}
	
}
