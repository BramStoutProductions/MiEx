package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class EntitySpawnerConditionSpawnLava extends EntitySpawnerCondition{

	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		int blockIdBelow = MCWorldExporter.getApp().getWorld().getBlockId(x, y - 1, z);
		Block block = BlockRegistry.getBlock(blockIdBelow);
		return block.getName().equals("minecraft:lava");
	}
	
}
