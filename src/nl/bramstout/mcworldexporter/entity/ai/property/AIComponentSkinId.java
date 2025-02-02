package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;

public class AIComponentSkinId extends AIComponent{

	public int skinId;
	
	public AIComponentSkinId(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagInt.newNonPooledInstance("SkinID", skinId));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagInt.newNonPooledInstance("SkinID", 0));		
	}

}
