package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.List;

public class AIComponentGroup {
	
	private String name;
	private List<AIComponent> components;
	
	public AIComponentGroup(String name, List<AIComponent> components) {
		this.name = name;
		this.components = components;
	}
	
	public String getName() {
		return name;
	}
	
	public List<AIComponent> getComponents(){
		return components;
	}
	
	public AIComponentGroup copy() {
		List<AIComponent> components = new ArrayList<AIComponent>();
		for(AIComponent comp : this.components)
			components.add(comp.copy());
		return new AIComponentGroup(name, components);
	}

}
