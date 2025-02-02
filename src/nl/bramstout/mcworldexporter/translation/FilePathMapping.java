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

public class FilePathMapping {

	private Map<String, String> translationMap;
	private Map<String, String> inverseTranslationMap;
	private String sourceName;
	
	public FilePathMapping(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = new HashMap<String, String>();
		this.inverseTranslationMap = new HashMap<String, String>();
	}
	
	public void load() {
		translationMap.clear();
		inverseTranslationMap.clear();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_file_path_mapping.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonObject data = Json.read(translationFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String resourceId = entry.getKey();
					
					String path = entry.getValue().getAsString();
					
					translationMap.put(resourceId, path);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		for(Entry<String, String> entry : translationMap.entrySet()) {
			if(inverseTranslationMap.containsKey(entry.getValue()))
				continue;
			inverseTranslationMap.put(entry.getValue(), entry.getKey());
		}
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
	
	public String unmap(String name) {
		return inverseTranslationMap.getOrDefault(name, name);
	}
	
}
