package nl.bramstout.mcworldexporter.entity.ai.property;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentSittable extends AIComponent{

	/**
	 * Event to run when the entity enters the sit state.
	 */
	public EntityEvent sitEvent;
	/**
	 * Event to run when the entity leaves the sit state.
	 */
	public EntityEvent standEvent;
	
	private byte prevSitting;
	
	public AIComponentSittable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		prevSitting = 0;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		byte sitting = 0;
		NbtTagByte sittingTag = (NbtTagByte) entity.getProperties().get("Sitting");
		if(sittingTag != null) {
			sitting = sittingTag.getData();
		}
		
		if(sitting != prevSitting) {
			if(sitting <= 0) {
				standEvent.fireEvent(entity);
			}else {
				sitEvent.fireEvent(entity);
			}
			prevSitting = sitting;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		byte sitting = 0;
		NbtTagByte sittingTag = (NbtTagByte) entity.getProperties().get("Sitting");
		if(sittingTag != null) {
			sitting = sittingTag.getData();
		}
		
		prevSitting = sitting;
	}

}
