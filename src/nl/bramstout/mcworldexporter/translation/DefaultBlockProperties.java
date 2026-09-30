package nl.bramstout.mcworldexporter.translation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class DefaultBlockProperties {

	private Map<String, NbtTagCompound> translationMap;
	private String sourceName;
	
	public DefaultBlockProperties(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = new HashMap<String, NbtTagCompound>();
	}
	
	public void load() {
		Map<String, NbtTagCompound> translationMap = new HashMap<String, NbtTagCompound>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_default_block_properties.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonObject data = Json.read(translationFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String blockName = entry.getKey();
					if(!blockName.contains(":"))
						blockName = "minecraft:" + blockName;
					
					JsonObject properties = entry.getValue().getAsJsonObject();
					NbtTag propertiesTag = NbtTag.fromJsonValue("properties", properties);
					if(propertiesTag instanceof NbtTagCompound)
						translationMap.put(blockName, (NbtTagCompound) propertiesTag);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		this.translationMap = translationMap;
	}
	
	/**
	 * Adds the default properties to the properties compound.
	 * @param name The name of the block.
	 * @param properties The properties compound. May be null.
	 * @return The updated properties compound. May return null.
	 */
	public NbtTagCompound addDefaultProperties(String name, NbtTagCompound properties) {
		if(properties == null) {
			return translationMap.getOrDefault(name, null);
		}else {
			return properties;
		}
	}
	
}
