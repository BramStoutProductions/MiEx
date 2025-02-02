package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class EntitySpawnerConditionSpawnsUnderwater extends EntitySpawnerCondition{

	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
		Block block = BlockRegistry.getBlock(blockId);
		return block.getName().equals("minecraft:water");
	}
	
}
