package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class EntitySpawnerConditionBiomeFilter extends EntitySpawnerCondition{

	public EntityFilter filter;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		if(filter == null)
			return false;
		// Dummy entity object since EntityFilter needs it
		Entity entity = new Entity(spawner.getEntityType(), NbtTagCompound.newNonPooledInstance(""), null);
		entity.setX(((float) x) + 0.5f);
		entity.setY(y);
		entity.setZ(((float) z) + 0.5f);
		return filter.testFilter(entity);
	}

}
