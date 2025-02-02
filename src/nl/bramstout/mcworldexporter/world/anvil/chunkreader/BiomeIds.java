package nl.bramstout.mcworldexporter.world.anvil.chunkreader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.translation.TranslationRegistry;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.World;

public class BiomeIds {
	
	private static class Mapping{
		public int minDataVersion;
		public int maxDataVersion;
		public Map<Integer, String> translationMap = new HashMap<Integer, String>();
	}
	
	private static List<Mapping> mappings = new ArrayList<Mapping>();
	
	public static void load() {
		List<Mapping> mappings = new ArrayList<Mapping>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/java/biome_ids.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement entry : data.asList()) {
					if(!entry.isJsonObject())
						continue;
					
					int minDataVersion = entry.getAsJsonObject().get("minDataVersion").getAsInt();
					int maxDataVersion = entry.getAsJsonObject().get("maxDataVersion").getAsInt();
					
					Mapping mapping = null;
					for(Mapping mapping2 : mappings) {
						if(mapping2.minDataVersion == minDataVersion && mapping2.maxDataVersion == maxDataVersion) {
							mapping = mapping2;
							break;
						}
					}
					if(mapping == null) {
						mapping = new Mapping();
						mapping.minDataVersion = minDataVersion;
						mapping.maxDataVersion = maxDataVersion;
						mappings.add(0, mapping);
					}
					
					JsonArray mappingArray = entry.getAsJsonObject().getAsJsonArray("mapping");
					for(JsonElement el : mappingArray.asList()) {
						int biomeId = el.getAsJsonObject().get("id").getAsInt();
						
						String biomeName = el.getAsJsonObject().get("name").getAsString();
						if(!biomeName.contains(":"))
							biomeName = "minecraft:" + biomeName;
						
						mapping.translationMap.put(biomeId, biomeName);
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BiomeIds.mappings = mappings;
	}
	
	public static String getNameForId(int id, int dataVersion) {
		Integer idInstance = Integer.valueOf(id);
		for(int i = 0; i < mappings.size(); ++i) {
			Mapping mapping = mappings.get(i);
			if(dataVersion < mapping.minDataVersion || dataVersion > mapping.maxDataVersion)
				continue;
			String name = mapping.translationMap.getOrDefault(idInstance, null);
			if(name != null)
				return name;
		}
		World.handleError(new Exception("No mapping for biome id " + id + " for data version " + dataVersion));
		return "plains";
	}
	
	public static int getRuntimeIdForId(int id, int dataVersion) {
		String name = getNameForId(id, dataVersion);
		name = TranslationRegistry.BIOME_JAVA.map(name);
		return BiomeRegistry.getIdForName(name);
	}
	
}
