package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.List;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public abstract class EntitySpawnerSpawner {
	
	/**
	 * Spawn entities at the given location.
	 * @param spawner The entity spawner instance.
	 * @param x The X coordinate of the block to spawn the entity in.
	 * @param y The Y coordinate of the block to spawn the entity in.
	 * @param z The Z coordinate of the block to spawn the entity in.
	 * @return A list of entities to spawn.
	 */
	public abstract List<SpawnEntity> spawn(EntitySpawner spawner, int x, int y, int z, Random random);
	
}
