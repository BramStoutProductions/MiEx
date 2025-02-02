package nl.bramstout.mcworldexporter.resourcepack.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.entity.ai.EntityFilterMolang;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionBiomeFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionBrightnessFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionHeightFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsUnderwater;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerSpawnerHerd;
import nl.bramstout.mcworldexporter.molang.MolangParser;

public class EntitySpawnerJavaEdition {

	public static List<EntitySpawner> parseSpawnRules(JsonObject data, String biomeId){
		List<EntitySpawner> spawners = new ArrayList<EntitySpawner>();
		
		for(Entry<String, JsonElement> entry : data.entrySet()) {
			for(JsonElement el : entry.getValue().getAsJsonArray().asList()) {
				JsonObject obj = el.getAsJsonObject();
				String entityType = null;
				if(obj.has("type"))
					entityType = obj.get("type").getAsString();
				if(entityType == null)
					continue;
				
				int weight = 50;
				if(obj.has("weight"))
					weight = obj.get("weight").getAsInt();
				
				EntitySpawner spawner = new EntitySpawner(entityType, entry.getKey(), weight);
				
				EntitySpawnerConditionBiomeFilter filter = new EntitySpawnerConditionBiomeFilter();
				filter.filter = new EntityFilterMolang(MolangParser.parse("query.has_biome('" + biomeId + "')"));
				spawner.getConditions().add(filter);
				
				if(entry.getKey().equalsIgnoreCase("monster")) {
					EntitySpawnerConditionBrightnessFilter filter2 = new EntitySpawnerConditionBrightnessFilter();
					filter2.maxLightLevel = 7;
					spawner.getConditions().add(filter2);
				}else if(entry.getKey().equalsIgnoreCase("ambient")) {
					EntitySpawnerConditionBrightnessFilter filter3 = new EntitySpawnerConditionBrightnessFilter();
					filter3.maxLightLevel = 4;
					spawner.getConditions().add(filter3);
					EntitySpawnerConditionHeightFilter filter4 = new EntitySpawnerConditionHeightFilter();
					filter4.max = 62;
					spawner.getConditions().add(filter4);
				}else if(entry.getKey().equalsIgnoreCase("water_creature")) {
					EntitySpawnerConditionSpawnsUnderwater filter2 = new EntitySpawnerConditionSpawnsUnderwater();
					spawner.getConditions().add(filter2);
				}else if(entry.getKey().equalsIgnoreCase("underground_water_creature")) {
					EntitySpawnerConditionSpawnsUnderwater filter2 = new EntitySpawnerConditionSpawnsUnderwater();
					spawner.getConditions().add(filter2);
				}else if(entry.getKey().equalsIgnoreCase("water_ambient")) {
					EntitySpawnerConditionSpawnsUnderwater filter2 = new EntitySpawnerConditionSpawnsUnderwater();
					spawner.getConditions().add(filter2);
				}
				
				int minCount = 1;
				int maxCount = 1;
				if(obj.has("minCount"))
					minCount = obj.get("minCount").getAsInt();
				if(obj.has("maxCount"))
					maxCount = obj.get("maxCount").getAsInt();
				
				if(minCount > 1 || maxCount > 1) {
					EntitySpawnerSpawnerHerd herd = new EntitySpawnerSpawnerHerd();
					herd.minSize = minCount;
					herd.maxSize = maxCount;
					spawner.getSpawners().add(herd);
				}
				
				spawners.add(spawner);
			}
		}
		
		return spawners;
	}
	
}
