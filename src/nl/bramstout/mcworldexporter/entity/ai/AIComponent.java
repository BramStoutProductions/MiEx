package nl.bramstout.mcworldexporter.entity.ai;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;

public abstract class AIComponent {

	public static enum PriorityGroup{
		NONE, BEHAVIOUR, MOVEMENT, NAVIGATION
	}
	
	private String name;
	private PriorityGroup priorityGroup;
	private int priority;
	private int order;
	
	public AIComponent(String name, PriorityGroup priorityGroup, int priority, int order) {
		this.name = name;
		this.priorityGroup = priorityGroup;
		this.priority = priority;
		this.order = order;
	}
	
	public String getName() {
		return name;
	}
	
	public PriorityGroup getPriorityGroup() {
		return priorityGroup;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public int getOrder() {
		return order;
	}

	public abstract boolean tick(Entity entity, float time, float deltaTime);
	
	public abstract void disabledTick(Entity entity, float time, float deltaTime);
	
	public AIComponent copy() {
		try {
			Class<? extends AIComponent> clazz = this.getClass();
			Constructor<?> constructor = clazz.getConstructors()[0];
			Object[] constructorArgs = new Object[constructor.getParameterCount()];
			int i = 0;
			for(Parameter parameter : constructor.getParameters()) {
				if(parameter.getType().isAssignableFrom(String.class)) {
					constructorArgs[i] = name;
				}else if(parameter.getType().isAssignableFrom(int.class)) {
					constructorArgs[i] = priority;
				}else {
					constructorArgs[i] = null;
				}
				
				i++;
			}
			Object instance = constructor.newInstance(constructorArgs);
			
			for(Field field : clazz.getFields()) {
				if(field.getType().isAssignableFrom(List.class)) {
					@SuppressWarnings("unchecked")
					List<Object> instanceList = (List<Object>) field.get(instance);
					@SuppressWarnings("unchecked")
					List<Object> thisList = (List<Object>) field.get(this);
					
					Class<?> listType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
					Method copyMethod = null;
					for(Method method : listType.getMethods()) {
						if(method.getName().equals("copy") && method.getParameterCount() == 0) {
							copyMethod = method;
							break;
						}
					}
					if(copyMethod != null) {
						for(i = 0; i < thisList.size(); ++i) {
							instanceList.add(copyMethod.invoke(thisList.get(i)));
						}
						continue;
					}
				}
				field.set(instance, field.get(this));
			}
			
			return (AIComponent) instance;
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
}
