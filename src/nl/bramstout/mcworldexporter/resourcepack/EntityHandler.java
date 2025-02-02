package nl.bramstout.mcworldexporter.resourcepack;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.model.Model;

public abstract class EntityHandler {

	public abstract Model getModel(Entity entity);
	
	public abstract void setup(Entity entity);
	
	public abstract EntityAIHandler getAIHandler(Entity entity);
	
}
