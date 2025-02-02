package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;

public class AIComponentVariableMaxAutoStep extends AIComponent{

	/**
	 * The maximum auto step height when on any other block.
	 */
	public float baseHeight;
	/**
	 * The maximum auto step height when on a block that prevents jumping.
	 */
	public float jumpPreventedHeight;
	
	public AIComponentVariableMaxAutoStep(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("AutoStepHeight", baseHeight));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("AutoStepJumpPreventedHeight", jumpPreventedHeight));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("AutoStepHeight", 0.5625f));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("AutoStepJumpPreventedHeight", 0.5625f));
	}

}
