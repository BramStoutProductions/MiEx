package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.List;
import java.util.Random;

import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerPermuterSpawnEvent extends EntitySpawnerPermuter{

	/**
	 * The event to trigger on the entities when entities get spawned.
	 */
	public String event;
	
	@Override
	public void permute(EntitySpawner spawner, List<SpawnEntity> entities, Random random) {
		for(SpawnEntity entity : entities) {
			entity.events.add(event);
		}
	}
	
}
