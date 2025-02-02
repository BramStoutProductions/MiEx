package nl.bramstout.mcworldexporter.entity;

import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class EntityRegistry {
	
	public static Entity getEntity(String id, NbtTagCompound properties) {
		if(!id.contains(":"))
			id = "minecraft:" + id;
		EntityHandler handler = ResourcePacks.getEntityHandler(id);
		if(handler == null)
			return null;
		return new Entity(id, properties, handler);
	}
	
}
