package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Random;

public class EntitySpawner {
	
	public static class SpawnEntity{
		
		/**
		 * The entity type name to spawn.
		 */
		public String name;
		/**
		 * The events to run on this entity when spawning.
		 */
		public List<String> events;
		/**
		 * The X coordinate of the block to spawn the entity in.
		 */
		public int x;
		/**
		 * The Y coordinate of the block to spawn the entity in.
		 */
		public int y;
		/**
		 * The Z coordinate of the block to spawn the entity in.
		 */
		public int z;
		
		public SpawnEntity(String name, int x, int y, int z) {
			this.name = name;
			this.events = new ArrayList<String>();
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
	}

	private String entityType;
	private String populationGroup;
	private int weight;
	private String spawnInBlock;
	private List<EntitySpawnerCondition> conditions;
	private List<EntitySpawnerSpawner> spawners;
	private List<EntitySpawnerPermuter> permuters;
	
	public EntitySpawner(String entityType, String populationGroup, int weight) {
		this.entityType = entityType;
		this.populationGroup = populationGroup;
		this.weight = weight;
		this.spawnInBlock = null;
		this.conditions = new ArrayList<EntitySpawnerCondition>();
		this.spawners = new ArrayList<EntitySpawnerSpawner>();
		this.permuters = new ArrayList<EntitySpawnerPermuter>();
	}
	
	/**
	 * Evaluates the conditions and returns whether this spawner
	 * can spawn at the given location.
	 * @param x The X coordinate of the block to spawn the entity in.
	 * @param y The Y coordinate of the block to spawn the entity in.
	 * @param z The Z coordinate of the block to spawn the entity in.
	 * @return
	 */
	public boolean test(int x, int y, int z, int sunLightLevel, Random random) {
		for(EntitySpawnerCondition condition : conditions)
			if(!condition.test(this, x, y, z, sunLightLevel, random))
				return false;
		return true;
	}
	
	/**
	 * Spawn entities at the given location.
	 * @param x The X coordinate of the block to spawn the entity in.
	 * @param y The Y coordinate of the block to spawn the entity in.
	 * @param z The Z coordinate of the block to spawn the entity in.
	 * @return A list of entities to spawn.
	 */
	public List<SpawnEntity> spawn(int x, int y, int z, Random random){
		List<SpawnEntity> entities = new ArrayList<SpawnEntity>();
		
		if(spawners.isEmpty())
			return entities;
		entities.addAll(spawners.get(random.nextInt(spawners.size())).spawn(this, x, y, z, random));
		
		for(EntitySpawnerPermuter permuter : permuters) {
			permuter.permute(this, entities, random);
		}
		
		return entities;
	}
	
	public String getEntityType() {
		return entityType;
	}
	
	public String getPopulationGroup() {
		return populationGroup;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public String getSpawnInBlock() {
		return spawnInBlock;
	}
	
	public void setSpawnInBlock(String spawnInBlock) {
		this.spawnInBlock = spawnInBlock;
	}
	
	public List<EntitySpawnerCondition> getConditions(){
		return conditions;
	}
	
	public List<EntitySpawnerSpawner> getSpawners(){
		return spawners;
	}
	
	public List<EntitySpawnerPermuter> getPermuters(){
		return permuters;
	}
	
}
