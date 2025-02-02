package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

public class EntitySpawnerConditionHeightFilter extends EntitySpawnerCondition{

	/**
	 * The minimum Y level at which the entity spawns.
	 */
	public int min;
	/**
	 * The maximum Y level at which the entity spawns.
	 */
	public int max;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		return y >= min && y <= max;
	}
	
}
