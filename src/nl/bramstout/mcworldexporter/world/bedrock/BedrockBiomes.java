package nl.bramstout.mcworldexporter.world.bedrock;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.World;

public class BedrockBiomes {
	
	private static Map<Integer, String> translationMap = new HashMap<Integer, String>();
	
	public static void load() {
		Map<Integer, String> translationMap = new HashMap<Integer, String>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/bedrock/bedrock_biomes.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement entry : data.asList()) {
					int biomeId = entry.getAsJsonObject().get("id").getAsInt();
					
					String biomeName = entry.getAsJsonObject().get("name").getAsString();
					if(!biomeName.contains(":"))
						biomeName = "minecraft:" + biomeName;
					
					translationMap.put(biomeId, biomeName);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		BedrockBiomes.translationMap = translationMap;
	}
	
	public static String getName(int id) {
		String name = translationMap.getOrDefault(id, null);
		if(name == null) {
			World.handleError(new RuntimeException("No biome name for Bedrock id " + id));
			return "minecraft:plains";
		}
		return name;
	}
	
}
