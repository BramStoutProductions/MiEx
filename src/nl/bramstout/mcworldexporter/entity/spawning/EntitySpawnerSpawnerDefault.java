package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerSpawnerDefault extends EntitySpawnerSpawner{

	@Override
	public List<SpawnEntity> spawn(EntitySpawner spawner, int x, int y, int z, Random random) {
		List<SpawnEntity> entities = new ArrayList<SpawnEntity>();
		
		entities.add(new SpawnEntity(spawner.getEntityType(), x, y, z));
		
		return entities;
	}

}
