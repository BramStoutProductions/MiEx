package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentEntitySensor extends AIComponent{

	public static class SubSensor{
		public float cooldown;
		public EntityFilter filter;
		public String eventName;
		public int maxEntities;
		public int minEntities;
		public float horizontalRange;
		public float verticalRange;
		public boolean requireAllEntitiesToPassFilter;
		public float nextTick;
		
		public SubSensor copy() {
			SubSensor subSensor = new SubSensor();
			subSensor.cooldown = cooldown;
			subSensor.filter = filter;
			subSensor.eventName = eventName;
			subSensor.maxEntities = maxEntities;
			subSensor.minEntities = minEntities;
			subSensor.horizontalRange = horizontalRange;
			subSensor.verticalRange = verticalRange;
			subSensor.requireAllEntitiesToPassFilter = requireAllEntitiesToPassFilter;
			return subSensor;
		}
	}
	public List<SubSensor> subSensors;
	
	public AIComponentEntitySensor(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		subSensors = new ArrayList<SubSensor>();
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		for(SubSensor sensor : subSensors) {
			if(time >= sensor.nextTick) {
				if(sensor.nextTick >= 0f) {
					int entitiesPassed = 0;
					int entitiesChecked = 0;
					for(List<Entity> entities : MCWorldExporter.getApp().getWorld().getEntitiesInRange(
												(int) posX, (int) posZ, (int) sensor.horizontalRange)) {
						for(Entity entity2 : entities) {
							float posX2 = entity2.getX();
							float posY2 = entity2.getY();
							float posZ2 = entity2.getZ();
							if(entity2.getAnimation() != null) {
								posX2 = entity2.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
								posY2 = entity2.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
								posZ2 = entity2.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
							}
							if(Math.abs(posY - posY2) > sensor.verticalRange)
								continue;
							if(((posX2 - posX) * (posX2 - posX) + (posZ2 - posZ) * (posZ2 * posZ)) > 
													(sensor.horizontalRange * sensor.horizontalRange))
								continue;
							entitiesChecked++;
							if(sensor.filter.testFilter(entity2))
								entitiesPassed++;
						}
					}
					boolean passed = entitiesPassed == entitiesChecked;
					if(!sensor.requireAllEntitiesToPassFilter) {
						if(entitiesPassed < sensor.minEntities)
							passed = false;
						else {
							if(sensor.maxEntities > 0)
								passed = entitiesPassed <= sensor.maxEntities;
							else
								passed = true;
						}
					}
					if(passed)
						entity.getAI().fireEvent(sensor.eventName);
				}
				sensor.nextTick = time + sensor.cooldown;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
