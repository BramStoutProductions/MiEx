package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerSpawnerDelayFilter extends EntitySpawnerSpawner{

	/**
	 * The entity type name of the entity that will spawn,
	 * next to the current entity.
	 */
	public String identifier;
	/**
	 * The chance of spawning this addition entity,
	 * with 0 meaning 0% chance and 100 meaning 100% chance.
	 */
	public int spawnChance;
	
	@Override
	public List<SpawnEntity> spawn(EntitySpawner spawner, int x, int y, int z, Random random) {
		List<SpawnEntity> entities = new ArrayList<SpawnEntity>();
		
		entities.add(new SpawnEntity(spawner.getEntityType(), x, y, z));
		
		if(random.nextInt(0, 100) < spawnChance) {
			entities.add(new SpawnEntity(identifier, x, y, z));
		}
		
		return entities;
	}
	
}
