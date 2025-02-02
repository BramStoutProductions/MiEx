package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.List;
import java.util.Random;

import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public abstract class EntitySpawnerPermuter {

	/**
	 * Permutes the list of entity types to spawn.
	 * @param spawner The entity spawner instance.
	 * @param entities The list of entity types to permute.
	 */
	public abstract void permute(EntitySpawner spawner, List<SpawnEntity> entities, Random random);
	
}
