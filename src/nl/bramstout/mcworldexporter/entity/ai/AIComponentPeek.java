package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentPeek extends AIComponent{

	/**
	 * Event to initiate when the entity starts peeking.
	 */
	public EntityEvent onOpen;
	/**
	 * Event to initiate when the entity is done peeking.
	 */
	public EntityEvent onClose;
	/**
	 * Event to initiate when the entity's target entity starts peeking.
	 */
	public EntityEvent onTargetOpen;
	
	private boolean prevPeekState;
	private Entity prevTarget;
	private boolean prevTargetPeekState;
	
	public AIComponentPeek(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		prevPeekState = false;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		boolean peekState = false;
		NbtTagByte isPeekingTag = (NbtTagByte) entity.getProperties().get("IsPeeking");
		if(isPeekingTag != null)
			peekState = isPeekingTag.getData() > 0;
		
		if(peekState != prevPeekState) {
			prevPeekState = peekState;
			if(peekState)
				onOpen.fireEvent(entity);
			else
				onClose.fireEvent(entity);
		}
		
		if(entity.getAI().target != null) {
			if(entity.getAI().target instanceof EntityTargetEntity) {
				Entity target = ((EntityTargetEntity) entity.getAI().target).getEntity();
				if(prevTarget != target) {
					prevTarget = target;
					prevTargetPeekState = false;
				}
				
				peekState = false;
				isPeekingTag = (NbtTagByte) target.getProperties().get("IsPeeking");
				if(isPeekingTag != null)
					peekState = isPeekingTag.getData() > 0;
				
				if(peekState != prevTargetPeekState) {
					prevTargetPeekState = peekState;
					if(peekState)
						onTargetOpen.fireEvent(entity);
				}
			}else {
				prevTarget = null;
			}
		}else {
			prevTarget = null;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		boolean peekState = false;
		NbtTagByte isPeekingTag = (NbtTagByte) entity.getProperties().get("IsPeeking");
		if(isPeekingTag != null)
			peekState = isPeekingTag.getData() > 0;
		prevPeekState = peekState;
	}

}
