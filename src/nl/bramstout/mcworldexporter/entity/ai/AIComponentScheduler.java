package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentScheduler extends AIComponent{

	/**
	 * The minimum delay in seconds between schedule ticks.
	 */
	public float minDelay;
	/**
	 * The maximum delay in seconds between schedule ticks.
	 */
	public float maxDelay;
	/**
	 * List of events with their filters to trigger on schedule ticks.
	 */
	public List<EntityEvent> scheduledEvents;
	
	private float nextTick;
	
	public AIComponentScheduler(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		scheduledEvents = new ArrayList<EntityEvent>();
		nextTick = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(time >= nextTick) {
			if(nextTick != -1f) {
				for(EntityEvent event : scheduledEvents)
					event.fireEvent(entity);
			}
			nextTick = entity.getRandom().nextFloat() * (maxDelay - minDelay) + minDelay + time;
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextTick = -1f;
	}

}
