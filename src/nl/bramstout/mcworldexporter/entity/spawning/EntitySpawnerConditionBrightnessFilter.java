package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;

public class EntitySpawnerConditionBrightnessFilter extends EntitySpawnerCondition{

	/**
	 * The minimum light level value that allows the entity to spawn.
	 */
	public int minLightLevel;
	/**
	 * The maximum light level value that allows the entity to spawn.
	 */
	public int maxLightLevel;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		// Let's calculate sun light.
		int lightValue = calculateSunLight(x, y, z);
		lightValue = (int) ((((float) lightValue) * ((float) sunLightLevel) / 15f));
		return lightValue >= minLightLevel && lightValue <= maxLightLevel;
	}
	
	private int calculateSunLight(int x, int y, int z) {
		int surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(x, z);
		if(surfaceHeight <= y) {
			// This sample is at the surface.
			return 15;
		}
		int radius = 16;
		int maxLightValue = 0;
		for(int sampleZ = z - radius; sampleZ <= z + radius; ++z) {
			for(int sampleX = x - radius; x <= x + radius; ++x) {
				int distance = Math.abs(z - sampleZ) + Math.abs(x - sampleX);
				int falloff = 15 - distance;
				if(falloff < 0)
					continue;
				surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(sampleX, sampleZ);
				if(surfaceHeight <= y) {
					// This sample is at the surface.
					maxLightValue = Math.max(maxLightValue, falloff);
					if(maxLightValue >= 15)
						return maxLightValue;
				}
			}
		}
		return maxLightValue;
	}
	
}
