package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentColor2 extends AIComponent{

	public int color;
	
	public AIComponentColor2(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	public int getColor() {
		return color;
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Color2", (byte) color));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("Color2", (byte) 0));	
	}

}
