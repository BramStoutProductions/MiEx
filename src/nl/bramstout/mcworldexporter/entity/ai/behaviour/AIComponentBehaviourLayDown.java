package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourLayDown extends AIComponent{

	/**
	 * Defines the 1/interval chance to choose this goal per tick.
	 */
	public int interval;
	/**
	 * Defines the 1/stopInterval chance to stop this goal per tick.
	 */
	public int stopInterval;
	
	private boolean isLayingDown;
	
	public AIComponentBehaviourLayDown(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		isLayingDown = false; 
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(isLayingDown) {
			if(EntityUtil.randomChance(entity, stopInterval, deltaTime)) {
				isLayingDown = false;
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsLayingDown", (byte) 0));
				return false;
			}
			return true;
		}
		if(EntityUtil.randomChance(entity, interval, deltaTime)) {
			isLayingDown = true;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsLayingDown", (byte) 1));
			return true;
		}
		return false;
	}

	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		isLayingDown = false;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsLayingDown", (byte) 0));
	}

}
