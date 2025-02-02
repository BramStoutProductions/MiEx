package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;

public class EntitySpawnerConditionSpawnsUnderground extends EntitySpawnerCondition{

	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		int surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(x, z);
		return surfaceHeight > (y + 1);
	}
	
}
