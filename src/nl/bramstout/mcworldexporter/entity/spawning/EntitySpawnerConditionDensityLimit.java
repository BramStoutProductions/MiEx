package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.List;
import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class EntitySpawnerConditionDensityLimit extends EntitySpawnerCondition{

	/**
	 * The maximum number of entities of this type spawnable on the surface.
	 * A value of -1 means no limit.
	 */
	public int surface;
	/**
	 * The maximum number of entities of this type spawnable underground.
	 * A value of -1 means no limit.
	 */
	public int underground;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		int surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(x, z);
		boolean isOnSurface = surfaceHeight <= y;
		
		int spawnLimit = isOnSurface ? surface : underground;
		
		if(spawnLimit < 0)
			return true; // No limit
		
		int entityCounter = 0;
		
		// Now we need to count how many entities of this type are around here.
		// Normally it's around the player, but we don't have a player,
		// so we do within x radius around this entity in terms of chunks.
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		int chunkRadius = 3;
		for(int cz = chunkZ - chunkRadius; cz <= chunkZ + chunkRadius; ++cz) {
			for(int cx = chunkX - chunkRadius; cx <= chunkX + chunkRadius; ++cx) {
				try {
					Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(cx, cz);
					if(chunk == null)
						continue;
					List<Entity> entities = chunk.getEntities();
					if(entities == null)
						continue;
					
					for(Entity entity : entities) {
						if(entity.getId().equals(spawner.getEntityType())) {
							entityCounter++;
							if(entityCounter >= spawnLimit)
								return false;
						}
					}
				}catch(Exception ex) {
					World.handleError(ex);
				}
			}
		}
		
		return true;
	}

}
