package nl.bramstout.mcworldexporter.translation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class BiomeTranslation {
	
	private Map<String, String> translationMap;
	private String sourceName;
	
	public BiomeTranslation(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = new HashMap<String, String>();
	}
	
	public void load() {
		Map<String, String> translationMap = new HashMap<String, String>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_biomes.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonObject data = Json.read(translationFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String biomeName = entry.getKey();
					if(!biomeName.contains(":"))
						biomeName = "minecraft:" + biomeName;
					
					String javaBiomeName = entry.getValue().getAsString();
					if(!javaBiomeName.contains(":"))
						javaBiomeName = "minecraft:" + javaBiomeName;
					
					translationMap.put(biomeName, javaBiomeName);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		this.translationMap = translationMap;
	}
	
	/**
	 * Maps the block to Java Edition. It returns the new name
	 * and it will modify properties directly.
	 * 
	 * @param name
	 * @param properties
	 * @return
	 */
	public String map(String name) {
		return translationMap.getOrDefault(name, name);
	}
	
}
