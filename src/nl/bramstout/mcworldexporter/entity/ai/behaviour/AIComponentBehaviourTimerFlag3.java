package nl.bramstout.mcworldexporter.entity.ai.behaviour;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentBehaviourTimerFlag3 extends AIComponent{

	/**
	 * The minimum time in seconds before this behaviour can start again.
	 */
	public float minCooldown;
	/**
	 * The maximum time in seconds before this behaviour can start again.
	 */
	public float maxCooldown;
	/**
	 * The minimum time in seconds that this timer will run.
	 */
	public float minDuration;
	/**
	 * The maximum time in seconds that this timer will run.
	 */
	public float maxDuration;
	/**
	 * The event to run when starting this timer.
	 */
	public EntityEvent onStart;
	/**
	 * The event to run when stopping this timer either because it ran out
	 * or it got interrupted.
	 */
	public EntityEvent onEnd;
	
	private float endCooldown;
	private float endTimer;
	
	public AIComponentBehaviourTimerFlag3(String name, int priority) {
		super(name, PriorityGroup.BEHAVIOUR, priority, -1);
		endCooldown = -1f;
		endTimer = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(time < endCooldown) {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag3", ((byte) 0)));
			return false;
		}
		if(endTimer == -1f) {
			// Let's start the timer.
			endTimer = entity.getRandom().nextFloat() * (maxDuration - minDuration) + minDuration + time;
			onStart.fireEvent(entity);
		}else {
			if(time >= endTimer) {
				// Timer ended.
				endTimer = -1f;
				endCooldown = entity.getRandom().nextFloat() * (maxCooldown - minCooldown) + minCooldown + time;
				onEnd.fireEvent(entity);
				entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag3", ((byte) 0)));
				return false;
			}
			// Timer still going.
		}
		
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag3", ((byte) 1)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		if(endTimer != -1f) {
			onEnd.fireEvent(entity);
		}
		endCooldown = -1f;
		endTimer = -1f;
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("TimerFlag3", ((byte) 0)));
	}
	
}
