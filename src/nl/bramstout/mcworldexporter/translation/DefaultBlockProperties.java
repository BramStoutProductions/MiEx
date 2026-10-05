package nl.bramstout.mcworldexporter.translation;

import java.io.File;
import java.util.ArrayList;
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

	private Map<String, List<String>> generalPropertiesMap;
	private Map<String, NbtTagCompound> translationMap;
	private String sourceName;
	
	public DefaultBlockProperties(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = new HashMap<String, NbtTagCompound>();
		this.generalPropertiesMap = new HashMap<String, List<String>>();
	}
	
	public void load() {
		Map<String, NbtTagCompound> translationMap = new HashMap<String, NbtTagCompound>();
		Map<String, List<String>> generalPropertiesMap = new HashMap<String, List<String>>();
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_default_block_properties.json");
			if(translationFile.exists()) {
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
			translationFile = new File(resourcePacks.get(i).getFolder(), "translation/minecraft/" + sourceName + "/miex_general_block_properties.json");
			if(translationFile.exists()) {
				try {
					JsonObject data = Json.read(translationFile).getAsJsonObject();
					for(Entry<String, JsonElement> entry : data.entrySet()) {
						String propertyName = entry.getKey();
						List<String> propertiesMap = generalPropertiesMap.getOrDefault(propertyName, null);
						if(propertiesMap == null) {
							propertiesMap = new ArrayList<String>();
							generalPropertiesMap.put(propertyName, propertiesMap);
						}
						
						JsonElement properties = entry.getValue();
						if(properties.isJsonArray()) {
							for(JsonElement el : properties.getAsJsonArray()) {
								if(el.isJsonPrimitive()) {
									if(!propertiesMap.contains(el.getAsString()))
										propertiesMap.add(el.getAsString());
								}
							}
						}else if(properties.isJsonPrimitive()) {
							if(!propertiesMap.contains(properties.getAsString()))
								propertiesMap.add(properties.getAsString());
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		this.translationMap = translationMap;
		this.generalPropertiesMap = generalPropertiesMap;
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
	
	public String getGeneralDefaultProperty(String property, List<String> options) {
		List<String> defaults = generalPropertiesMap.getOrDefault(property, null);
		if(defaults == null) {
			if(options.size() > 0)
				return options.get(0);
			return "";
		}
		for(String option : options) {
			if(defaults.contains(option))
				return option;
		}
		if(options.size() > 0)
			return options.get(0);
		return "";
	}
	
}
