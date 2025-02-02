package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;

public class AIComponentAgeable extends AIComponent{

	/**
	 * The amount of time in seconds before the entity grows up.
	 */
	public float growUpDuration;
	/**
	 * Event to run when the entity grows up.
	 */
	public EntityEvent growUpEvent;
	
	public AIComponentAgeable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("GrowUpDuration", growUpDuration));
		NbtTagInt ageTag = (NbtTagInt) entity.getProperties().get("Age");
		if(ageTag == null) {
			ageTag = NbtTagInt.newNonPooledInstance("Age", (int) (growUpDuration * -20f));
			entity.getProperties().addElement(ageTag);
		}
		ageTag.setData(ageTag.getData() + 1);
		if(ageTag.getData() == 0) {
			// Entity is now grown up
			growUpEvent.fireEvent(entity);
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("GrowUpDuration", 1200f));
	}

}
