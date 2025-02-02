package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerPermuterPermuteType extends EntitySpawnerPermuter{

	public static class Permutation{
		
		/**
		 * The weight of this permutation.
		 */
		public int weight;
		/**
		 * The type of the entity to spawn.
		 * If this value is null, then it spawns
		 * the normal entity type.
		 */
		public String entityType;
		
		public Permutation() {
			weight = 1;
			entityType = null;
		}
		
		public Permutation(int weight, String entityType) {
			this.weight = weight;
			this.entityType = entityType;
		}
		
	}
	
	public List<Permutation> permutations;
	
	public EntitySpawnerPermuterPermuteType() {
		this.permutations = new ArrayList<Permutation>();
	}
	
	@Override
	public void permute(EntitySpawner spawner, List<SpawnEntity> entities, Random random) {
		int totalWeight = 0;
		for(Permutation permutation : permutations)
			totalWeight += permutation.weight;
		for(SpawnEntity entity : entities) {
			int sample = random.nextInt(totalWeight);
			
			for(Permutation permutation : permutations) {
				sample -= permutation.weight;
				if(sample < 0) {
					if(permutation.entityType != null)
						entity.name = permutation.entityType;
					break;
				}
			}
		}
	}
	
}
