package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

public abstract class EntitySpawnerCondition {
	
	/**
	 * Tests if the entity can spawn at the given location.
	 * @param x The x coordinate of the block to spawn in.
	 * @param y The y coordinate of the block to spawn in.
	 * @param z The z coordinate of the block to spawn in.
	 * @return True if the entity can spawn here.
	 */
	public abstract boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random);

}
