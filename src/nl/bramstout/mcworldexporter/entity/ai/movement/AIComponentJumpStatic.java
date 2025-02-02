package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentJumpStatic extends AIComponent{

	public float jumpPower;
	
	public AIComponentJumpStatic(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("CanJump", (byte) 1));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("JumpPower", jumpPower));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("CanJump", (byte) 0));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("JumpPower", 0.42f));
	}

}
