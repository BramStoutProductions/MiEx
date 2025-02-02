package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;

public class AIComponentScaleByAge extends AIComponent{
	
	/**
	 * Initial scale of the newborn entity.
	 */
	public float startScale;
	/**
	 * Ending scale of the entity when it's fully grown.
	 */
	public float endScale;
	
	public AIComponentScaleByAge(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float age = 0f;
		float babyAge = -1200 * 20;
		float adultAge = 0f;
		
		NbtTagInt ageTag = (NbtTagInt) entity.getProperties().get("Age");
		if(ageTag != null)
			age = (float) ageTag.getData();
		NbtTagFloat growUpDurationTag = (NbtTagFloat) entity.getProperties().get("GrowUpDuration");
		if(growUpDurationTag != null)
			babyAge = growUpDurationTag.getData() * -20f;
		
		float scale = startScale;
		if(babyAge < adultAge) {
			float t = (age - babyAge) / (adultAge - babyAge);
			scale = startScale * (1f - t) + endScale * t;
		}
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("Scale", scale));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("Scale", 1f));
	}

}
