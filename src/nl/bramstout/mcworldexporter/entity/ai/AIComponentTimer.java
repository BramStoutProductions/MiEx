package nl.bramstout.mcworldexporter.entity.ai;

import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentTimer extends AIComponent{

	/**
	 * If true, the timer will restart every time after it initiates.
	 */
	public boolean looping;
	/**
	 * If true, the amount of time on the timer will be random between
	 * the min and max values specified in time.
	 */
	public boolean randomInterval;
	/**
	 * A list of times in seconds that can be picked from
	 * before initiating the event. If this list isn't null,
	 * then it'll ignore minTime and maxTime.
	 */
	public List<Float> randomTimeChoices;
	/**
	 * A list of weights for the randomTimeChoices.
	 */
	public List<Float> randomTimeChoicesWeights;
	/**
	 * The minimum random time used to initiate this timer.
	 */
	public float minTime;
	/**
	 * The maximum random time used to initiate this timer
	 */
	public float maxTime;
	/**
	 * The event to trigger when the timer is done.
	 */
	public EntityEvent timerDoneEvent;
	
	private float nextTimerTick;
	
	public AIComponentTimer(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		nextTimerTick = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(time >= nextTimerTick) {
			if(nextTimerTick != -1f) {
				timerDoneEvent.fireEvent(entity);
				
				if(!looping) {
					// Disable timer
					nextTimerTick = Float.MAX_VALUE;
					return true;
				}
			}
			
			// Select a new nextTimerTick
			if(randomTimeChoices != null && randomTimeChoicesWeights != null) {
				float totalWeight = 0f;
				for(Float f : randomTimeChoicesWeights)
					totalWeight += f.floatValue();
				float weight = entity.getRandom().nextFloat() * totalWeight;
				int i = 0;
				for(; i < Math.min(randomTimeChoices.size(), randomTimeChoicesWeights.size()); ++i) {
					if(weight <= 0f)
						break;
					weight -= randomTimeChoicesWeights.get(i).floatValue();
				}
				float delay = randomTimeChoices.get(Math.max(i, 0)).floatValue();
				nextTimerTick = time + delay;
			}else {
				float delay = minTime;
				if(randomInterval)
					delay = entity.getRandom().nextFloat() * (maxTime - minTime) + minTime;
				nextTimerTick = time + delay;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextTimerTick = -1f;
	}

}
