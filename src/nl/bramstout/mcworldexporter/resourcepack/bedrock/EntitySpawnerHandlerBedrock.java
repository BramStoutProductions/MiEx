package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilterMolang;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionBiomeFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionBrightnessFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionDensityLimit;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionHeightFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnLava;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsOnBlockFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsOnBlockPreventedFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsOnSurface;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsUnderground;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerConditionSpawnsUnderwater;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerPermuterPermuteType;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerPermuterSpawnEvent;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerSpawnerDefault;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerSpawnerDelayFilter;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawnerSpawnerHerd;
import nl.bramstout.mcworldexporter.molang.MolangParser;

public class EntitySpawnerHandlerBedrock {

	public static List<EntitySpawner> parseSpawnRules(JsonObject data){
		List<EntitySpawner> spawners = new ArrayList<EntitySpawner>();
		
		String identifier = null;
		String populationGroup = "";
		if(data.has("description")) {
			JsonObject descriptionObj = data.getAsJsonObject("description");
			if(descriptionObj.has("identifier"))
				identifier = descriptionObj.get("identifier").getAsString();
			
			if(descriptionObj.has("population_control"))
				populationGroup = descriptionObj.get("population_control").getAsString();
		}
		if(identifier == null)
			return spawners;
		
		if(data.has("conditions")) {
			JsonElement conditions = data.get("conditions");
			if(conditions.isJsonObject()) {
				EntitySpawner spawner = parseSpawner(conditions.getAsJsonObject(), identifier, populationGroup);
				if(spawner != null)
					spawners.add(spawner);
			}else if(conditions.isJsonArray()) {
				for(JsonElement condition : conditions.getAsJsonArray().asList()) {
					EntitySpawner spawner = parseSpawner(condition.getAsJsonObject(), identifier, populationGroup);
					if(spawner != null)
						spawners.add(spawner);
				}
			}
		}
		
		return spawners;
	}
	
	public static EntitySpawner parseSpawner(JsonObject data, String identifier, String populationGroup) {
		int weight = 100;
		if(data.has("minecraft:weight")) {
			JsonObject weightObj = data.getAsJsonObject("minecraft:weight");
			if(weightObj.has("default"))
				weight = weightObj.get("default").getAsInt();
		}
		EntitySpawner spawner = new EntitySpawner(identifier, populationGroup, weight);
		
		if(data.has("minecraft:biome_filter")) {
			EntitySpawnerConditionBiomeFilter component = new EntitySpawnerConditionBiomeFilter();
			component.filter = getFilter(data, "minecraft:biome_filter");
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:brightness_filter")) {
			JsonObject obj = data.getAsJsonObject("minecraft:brightness_filter");
			EntitySpawnerConditionBrightnessFilter component = new EntitySpawnerConditionBrightnessFilter();
			component.minLightLevel = getInt(obj, "min", 0);
			component.maxLightLevel = getInt(obj, "max", 15);
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:delay_filter")) {
			JsonObject obj = data.getAsJsonObject("minecraft:delay_filter");
			EntitySpawnerSpawnerDelayFilter component = new EntitySpawnerSpawnerDelayFilter();
			component.identifier = getString(obj, "identifier", null);
			component.spawnChance = getInt(obj, "spawn_chance", 100);
			spawner.getSpawners().add(component);
		}
		if(data.has("minecraft:density_limit")) {
			JsonObject obj = data.getAsJsonObject("minecraft:density_limit");
			EntitySpawnerConditionDensityLimit component = new EntitySpawnerConditionDensityLimit();
			component.surface = getInt(obj, "surface", -1);
			component.underground = getInt(obj, "underground", -1);
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:height_filter")) {
			JsonObject obj = data.getAsJsonObject("minecraft:height_filter");
			EntitySpawnerConditionHeightFilter component = new EntitySpawnerConditionHeightFilter();
			component.min = getInt(obj, "min", Integer.MIN_VALUE);
			component.max = getInt(obj, "max", Integer.MAX_VALUE);
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:herd")) {
			JsonElement el = data.get("minecraft:herd");
			if(el.isJsonArray()) {
				for(JsonElement el2 : el.getAsJsonArray().asList()) {
					JsonObject obj = el2.getAsJsonObject();
					EntitySpawnerSpawnerHerd component = new EntitySpawnerSpawnerHerd();
					component.minSize = getInt(obj, "min_size", 1);
					component.maxSize = getInt(obj, "max_size", 1);
					component.event = getString(obj, "event", "");
					component.eventSkipCount = getInt(obj, "event_skip_count", 0);
					spawner.getSpawners().add(component);
				}
			}else if(el.isJsonObject()) {
				JsonObject obj = el.getAsJsonObject();
				EntitySpawnerSpawnerHerd component = new EntitySpawnerSpawnerHerd();
				component.minSize = getInt(obj, "min_size", 1);
				component.maxSize = getInt(obj, "max_size", 1);
				component.event = getString(obj, "event", "");
				component.eventSkipCount = getInt(obj, "event_skip_count", 0);
				spawner.getSpawners().add(component);
			}
		}
		if(data.has("minecraft:permute_type")) {
			JsonArray array = data.getAsJsonArray("minecraft:permute_type");
			EntitySpawnerPermuterPermuteType component = new EntitySpawnerPermuterPermuteType();
			for(JsonElement el : array.asList()) {
				JsonObject obj = el.getAsJsonObject();
				EntitySpawnerPermuterPermuteType.Permutation permutation = new EntitySpawnerPermuterPermuteType.Permutation();
				permutation.weight = getInt(obj, "weight", 100);
				permutation.entityType = getString(obj, "entity_type", null);
				component.permutations.add(permutation);
			}
			spawner.getPermuters().add(component);
		}
		if(data.has("minecraft:spawn_event")) {
			JsonObject obj = data.getAsJsonObject("minecraft:spawn_event");
			EntitySpawnerPermuterSpawnEvent component = new EntitySpawnerPermuterSpawnEvent();
			component.event = getString(obj, "event", "");
			spawner.getPermuters().add(component);
		}
		if(data.has("minecraft:spawns_lava")) {
			EntitySpawnerConditionSpawnLava component = new EntitySpawnerConditionSpawnLava();
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:spawns_on_block_filter")) {
			EntitySpawnerConditionSpawnsOnBlockFilter component = new EntitySpawnerConditionSpawnsOnBlockFilter();
			component.blocks = getNameList(data, "minecraft:spawns_on_block_filter");
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:spawns_on_block_prevented_filter")) {
			EntitySpawnerConditionSpawnsOnBlockPreventedFilter component = new EntitySpawnerConditionSpawnsOnBlockPreventedFilter();
			component.blocks = getNameList(data, "minecraft:spawns_on_block_prevented_filter");
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:spawns_on_surface")) {
			EntitySpawnerConditionSpawnsOnSurface component = new EntitySpawnerConditionSpawnsOnSurface();
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:spawns_underground")) {
			EntitySpawnerConditionSpawnsUnderground component = new EntitySpawnerConditionSpawnsUnderground();
			spawner.getConditions().add(component);
		}
		if(data.has("minecraft:spawns_underwater")) {
			EntitySpawnerConditionSpawnsUnderwater component = new EntitySpawnerConditionSpawnsUnderwater();
			spawner.getConditions().add(component);
			spawner.setSpawnInBlock("minecraft:water");
		}
		
		if(spawner.getSpawners().isEmpty())
			spawner.getSpawners().add(new EntitySpawnerSpawnerDefault());
		
		return spawner;
	}
	
	private static int getInt(JsonObject data, String name, int defaultValue) {
		if(data.has(name))
			return data.get(name).getAsInt();
		return defaultValue;
	}
	
	private static String getString(JsonObject data, String name, String defaultValue) {
		if(data.has(name))
			return data.get(name).getAsString();
		return defaultValue;
	}
	
	private static EntityFilter getFilter(JsonObject data, String name) {
		if(data.has(name)) {
			return new EntityFilterMolang(MolangParser.parse(parseFilterPart(data.get(name))));
		}
		return new EntityFilterMolang(MolangParser.parse("true"));
	}
	
	private static String parseFilterPart(JsonElement element) {
		if(element.isJsonArray()) {
			String code = "";
			for(JsonElement el : element.getAsJsonArray().asList()) {
				if(!code.isEmpty())
					code += " && ";
				code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
			}
			return code;
		} else if(element.isJsonObject()) {
			JsonObject data = element.getAsJsonObject();
		
			if(data.has("all_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("all_of")) {
					if(!code.isEmpty())
						code += " && ";
					code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("any_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("any_of")) {
					if(!code.isEmpty())
						code += " || ";
					code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("none_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("none_of")) {
					if(!code.isEmpty())
						code += " && ";
					code += "!(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("filters")) {
				return parseFilterPart(data.get("filters"));
			}else {
				// TODO: Support subject
				
				String query = "false";
				String operator = "==";
				@SuppressWarnings("unused")
				String subject = "self";
				String value = "true";
				String domain = null;
				
				String testStr = data.get("test").getAsString();
				@SuppressWarnings("unused")
				String subjectStr = "self";
				if(data.has("subject"))
					subjectStr = data.get("subject").getAsString();
				String operatorStr = "==";
				if(data.has("operator"))
					operatorStr = data.get("operator").getAsString();
				if(data.has("domain"))
					domain = data.get("domain").getAsString();
				if(data.has("value")) {
					JsonPrimitive prim = data.getAsJsonPrimitive("value");
					if(prim.isBoolean())
						value = prim.getAsBoolean() ? "true" : "false";
					else if(prim.isNumber())
						value = Float.toString(prim.getAsFloat());
					else if(prim.isString())
						value = "'" + prim.getAsString() + "'";
				}
				
				if(operatorStr.equalsIgnoreCase("equals"))
					operator = "==";
				else if(operatorStr.equalsIgnoreCase("not"))
					operator = "!=";
				else if(operatorStr.equals("!="))
					operator = "!=";
				else if(operatorStr.equals("<"))
					operator = "<";
				else if(operatorStr.equals("<="))
					operator = "<=";
				else if(operatorStr.equals("<>"))
					operator = "!=";
				else if(operatorStr.equals("="))
					operator = "==";
				else if(operatorStr.equals("=="))
					operator = "==";
				else if(operatorStr.equals(">"))
					operator = ">";
				else if(operatorStr.equals(">="))
					operator = ">=";
				
				if(testStr.equals("actor_health")) 
					query = "query.health";
				else if(testStr.equals("bool_property"))
					query = "query.property";
				else if(testStr.equals("clock_time"))
					query = "query.time_of_day";
				else if(testStr.equals("distance_to_nearest_player"))
					query = "query.distance_from_camera";
				else if(testStr.equals("enum_property"))
					query = "query.property";
				else if(testStr.equals("float_property"))
					query = "query.property";
				else if(testStr.equals("has_biome_tag")) {
					query = "query.has_biome_tag";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("has_component")) {
					query = "query.has_component";
					domain = value;
					value = "true";
				}else if(testStr.equals("has_property")) {
					query = "query.has_property";
					domain = value;
					value = "true";
				}else if(testStr.equals("has_tag")) {
					query = "query.any_tag";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("has_target"))
					query = "query.has_target";
				else if(testStr.equals("hourly_clock_time"))
					query = "(query.time_of_day * 24000)";
				else if(testStr.equals("int_property"))
					query = "query.property";
				else if(testStr.equals("is_baby"))
					query = "query.is_baby";
				else if(testStr.equals("has_equipment")) {
					query = "query.is_item_name_any";
					if(domain.equals("any"))
						domain = "'slot.any'";
					else if(domain.equals("armor"))
						domain = "'slot.armor'";
					else if(domain.equals("feet"))
						domain = "'slot.armor.feet'";
					else if(domain.equals("hand"))
						domain = "'slot.weapon_mainhand'";
					else if(domain.equals("head"))
						domain = "'slot.armor.head'";
					else if(domain.equals("inventory"))
						domain = "'slot.inventory'";
					else if(domain.equals("leg"))
						domain = "'slot.armor.legs'";
					else if(domain.equals("torso"))
						domain = "'slot.armor.chest'";
					domain += ", -1, " + value;
					value = "true";
				}else if(testStr.equals("has_component")) {
					query = "query.has_component";
					domain = value;
					value = "true";
				}else if(testStr.equals("in_block")) {
					query = "query.get_block_name";
					if(value.equals("true")) {
						if(operator.equals("==")) {
							operator = "!=";
						}else {
							operator = "==";
						}
						value = "\"minecraft:air\"";
					}else if(value.equals("false")) {
						value = "\"minecraft:air\"";
					}
				}else if(testStr.equals("is_biome")) {
					query = "query.has_biome";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("is_block")) {
					query = "query.get_block_name";
				}else if(testStr.equals("is_daytime")) {
					query = "math.mod(query.time_of_day - 0.25, 1)";
					operator = "<=";
					value = "0.5";
				}else if(testStr.equals("is_family")) {
					query = "query.has_any_family";
					domain = value;
					value = "true";
				}else if(testStr.equals("is_mark_variant")) {
					query = "query.mark_variant";
				}else if(testStr.equals("is_moving")) {
					query = "query.is_moving";
				}else if(testStr.equals("is_skin_id")) {
					query = "query.skin_id";
				}else if(testStr.equals("is_variant")) {
					query = "query.variant";
				}else if(testStr.equals("moon_intensity")) {
					query = "query.moon_brightness";
				}else if(testStr.equals("moon_phase")) {
					query = "query.moon_phase";
				}else if(testStr.equals("on_fire")) {
					query = "query.is_on_fire";
				}else if(testStr.equals("on_ground")) {
					query = "query.is_on_ground";
				}else if(testStr.equals("random_chance")) {
					query = "math.random_integer";
					domain = "0, " + value;
					value = "0";
				}else if(testStr.equals("in_contact_with_water"))
					query = "query.is_in_contact_with_water";
				else if(testStr.equals("in_lava"))
					query = "query.is_in_lava";
				else if(testStr.equals("in_water_or_rain"))
					query = "query.is_in_water_or_rain";
				else if(testStr.equals("in_water"))
					query = "query.is_in_water";
				
				String code = query;
				if(domain != null && !query.equals("false"))
					code += "(" + domain + ")";
				code += " " + operator + " ";
				code += value;
				return code;
			}
		}
		return "";
	}
	
	private static List<String> getNameList(JsonObject data, String name){
		if(data.has(name)) {
			List<String> res = new ArrayList<String>();
			JsonElement array = data.get(name);
			if(array.isJsonArray()) {
				for(JsonElement el : array.getAsJsonArray().asList())
					if(el.isJsonPrimitive()) {
						String value = el.getAsString();
						if(!value.contains(":"))
							value = "minecraft:" + value;
						res.add(value);
					}
			}else if(array.isJsonPrimitive()) {
				String value = array.getAsString();
				if(!value.contains(":"))
					value = "minecraft:" + value;
				res.add(value);
			}
			return res;
		}
		return new ArrayList<String>();
	}
	
}
