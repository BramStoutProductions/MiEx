package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;

public class EntityEvent {

	public static abstract class EntityEventComponent{
		
		public EntityFilter filter;
		public String target;
		
		public EntityEventComponent() {
			this.filter = null;
		}
		
		protected abstract void _fireEvent(Entity entity);
		
		public void fireEvent(Entity entity) {
			if(filter == null)
				_fireEvent(entity);
			else
				if(filter.testFilter(entity))
					_fireEvent(entity);
		}
		
	}
	
	public static class EntityEventComponentSetProperty extends EntityEventComponent{
		
		public Map<String, MolangScript> properties;
		
		public EntityEventComponentSetProperty() {
			super();
			this.properties = new HashMap<String, MolangScript>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			float posX = entity.getX();
			float posY = entity.getY();
			float posZ = entity.getZ();
			if(entity.getAnimation() != null) {
				posX = entity.getAnimation().getAnimPosX().getLatestValue().value;
				posY = entity.getAnimation().getAnimPosY().getLatestValue().value;
				posZ = entity.getAnimation().getAnimPosZ().getLatestValue().value;
			}
			MolangQuery query = new MolangQuery(entity.getId(), entity.getProperties(), posX, posY, posZ);
			MolangContext context = new MolangContext(query, entity.getRandom());
			context.setVariableDict(entity.getVariables());
			for(Entry<String, MolangScript> entry : properties.entrySet()) {
				entity.getProperties().addElement(NbtTagString.newNonPooledInstance(entry.getKey(), entry.getValue().eval(context).asString(context)));
			}
		}
		
	}
	
	public static class EntityEventComponentAddGroup extends EntityEventComponent{
		
		public List<String> groups;
		
		public EntityEventComponentAddGroup() {
			super();
			this.groups = new ArrayList<String>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			for(String group : groups)
				entity.getAI().enableComponentGroup(group);
		}
		
	}
	
	public static class EntityEventComponentRemoveGroup extends EntityEventComponent{
		
		public List<String> groups;
		
		public EntityEventComponentRemoveGroup() {
			super();
			this.groups = new ArrayList<String>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			for(String group : groups)
				entity.getAI().disableComponentGroup(group);
		}
		
	}
	
	public static class EntityEventComponentTriggerEvent extends EntityEventComponent{
		
		public List<String> events;
		
		public EntityEventComponentTriggerEvent() {
			super();
			this.events = new ArrayList<String>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			for(EntityEvent event : entity.getAI().getEvents())
				if(events.contains(event.getName()))
					event.fireEvent(entity);
		}
		
	}
	
	public static class EntityEventComponentRandomize extends EntityEventComponent{
		
		public List<EntityEventComponent> components;
		public List<Float> weights;
		
		public EntityEventComponentRandomize() {
			super();
			this.components = new ArrayList<EntityEventComponent>();
			this.weights = new ArrayList<Float>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			float totalWeight = 0f;
			for(Float f : weights)
				totalWeight += f.floatValue();
			float rand = (float) (Math.random() * totalWeight);
			int i = 0;
			for(; i < weights.size(); ++i) {
				if(rand < weights.get(i).floatValue())
					break;
				rand -= weights.get(i).floatValue();
			}
			EntityEventComponent component = components.get(Math.min(i, components.size()-1));
			if(component != null)
				component.fireEvent(entity);
		}
		
	}
	
	public static class EntityEventComponentSequence extends EntityEventComponent{
		
		public List<EntityEventComponent> components;
		
		public EntityEventComponentSequence() {
			super();
			this.components = new ArrayList<EntityEventComponent>();
		}
		
		@Override
		protected void _fireEvent(Entity entity) {
			for(EntityEventComponent component : components)
				if(component != null)
					component.fireEvent(entity);
		}
		
	}
	
	private String name;
	private EntityEventComponent component;
	
	public EntityEvent(String name, EntityEventComponent component) {
		this.name = name;
		this.component = component;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public EntityEventComponent getComponent() {
		return component;
	}
	
	public void fireEvent(Entity entity) {
		if(component != null)
			component.fireEvent(entity);
	}
	
}
