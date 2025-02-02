package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentEnvironmentScanner extends AIComponent{
	
	public static class SubSensor{
		public String eventName;
		public EntityFilter filter;
		public String target;
		
		public SubSensor copy() {
			SubSensor subSensor = new SubSensor();
			subSensor.eventName = eventName;
			subSensor.filter = filter;
			subSensor.target = target;
			return subSensor;
		}
	}
	
	public List<SubSensor> subSensors;
	
	public AIComponentEnvironmentScanner(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		subSensors = new ArrayList<SubSensor>();
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		for(SubSensor subSensor : subSensors) {
			if(subSensor.filter.testFilter(entity)) {
				// TODO: Implement target
				entity.getAI().fireEvent(subSensor.eventName);
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
