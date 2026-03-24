package nl.bramstout.mcworldexporter.lighting;

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
import nl.bramstout.mcworldexporter.resourcepack.Tags;

public class Lighting {
	
	private static class BlockLightDataEntry{
		
		private Map<String, String> state;
		private BlockLightValues values;
		
		public BlockLightDataEntry(JsonObject data) {
			state = new HashMap<String, String>();
			if(data.has("state")) {
				for(Entry<String, JsonElement> entry : data.getAsJsonObject("state").entrySet()) {
					state.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			values = new BlockLightValues(data);
		}
		
		public boolean matches(NbtTagCompound properties) {
			for(int i = 0; i < properties.getSize(); ++i) {
				NbtTag tag = properties.get(i);
				String expectedValue = state.getOrDefault(tag.getName(), null);
				if(expectedValue != null) {
					if(!expectedValue.equals(tag.asString())) {
						return false;
					}
				}
			}
			return true;
		}
		
		public BlockLightValues getValues() {
			return values;
		}
		
	}
	
	public static class BlockLightData{
		
		private BlockLightValues defaultValues;
		private List<BlockLightDataEntry> entries;
		
		public BlockLightData(JsonObject data) {
			defaultValues = null;
			entries = null;
			if(data.has("default")) {
				defaultValues = new BlockLightValues(data.getAsJsonObject("default"));
			}
			if(data.has("states")) {
				entries = new ArrayList<BlockLightDataEntry>();
				for(JsonElement el : data.getAsJsonArray("states")) {
					entries.add(new BlockLightDataEntry(el.getAsJsonObject()));
				}
			}
		}
		
		public BlockLightValues get(NbtTagCompound properties) {
			if(entries != null) {
				for(BlockLightDataEntry entry : entries) {
					if(entry.matches(properties))
						return entry.getValues();
				}
			}
			return defaultValues;
		}
		
		public byte getMaxLightLevel() {
			byte maxLightLevel = 0;
			if(defaultValues != null) {
				if(defaultValues.getEmissiveLightLevel() > 0) {
					LightMap lightMap = getLightMap(defaultValues.getEmissiveLightColor());
					if(lightMap != null) {
						maxLightLevel = lightMap.getMaxLightLevel();
					}
				}
			}
			if(entries != null) {
				for(BlockLightDataEntry entry : entries) {
					if(entry.getValues().getEmissiveLightLevel() > 0) {
						LightMap lightMap = getLightMap(entry.getValues().getEmissiveLightColor());
						if(lightMap != null) {
							maxLightLevel = (byte) Math.max(maxLightLevel, lightMap.getMaxLightLevel());
						}
					}
				}
			}
			return maxLightLevel;
		}
		
	}
	
	private static List<LightMap> lightMapRegistry = new ArrayList<LightMap>();
	private static Map<String, BlockLightData> blockDataRegistry = new HashMap<String, BlockLightData>();
	private static byte maxLightLevel = 0;
	private static List<String> colorSets = new ArrayList<String>();
	
	public static List<String> getColorSetNames(){
		return colorSets;
	}
	
	public static LightMap getLightMap(short id) {
		if(id < 0 || id >= lightMapRegistry.size())
			return null;
		return lightMapRegistry.get(id);
	}
	
	public static short getIdForName(String name) {
		for(int i = 0; i < lightMapRegistry.size(); ++i) {
			if(lightMapRegistry.get(i).getName().equals(name))
				return (short) i;
		}
		return -1;
	}
	
	public static BlockLightData getBlockLightData(String name) {
		return blockDataRegistry.getOrDefault(name, null);
	}
	
	public static BlockLightValues getBlockLightData(String name, NbtTagCompound properties) {
		BlockLightData data = blockDataRegistry.getOrDefault(name, null);
		if(data == null)
			return null;
		return data.get(properties);
	}
	
	public static byte getMaxLightLevel() {
		return maxLightLevel;
	}
	
	public static void load() {
		lightMapRegistry.clear();
		colorSets.clear();
		
		// Always make sure that skyLight has an id of 0.
		lightMapRegistry.add(new LightMap("skyLight", (short) 0, null));
		
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File lightingFile = new File(resourcePacks.get(i).getFolder(), "miex_lighting.json");
			if(!lightingFile.exists())
				continue;
			try {
				JsonObject data = Json.read(lightingFile).getAsJsonObject();
				if(data.has("lightMaps")) {
					for(Entry<String, JsonElement> entry : data.getAsJsonObject("lightMaps").entrySet()) {
						short id = getIdForName(entry.getKey());
						if(id == -1)
							id = (short) lightMapRegistry.size();
						for(int j = lightMapRegistry.size(); j <= id; ++j)
							lightMapRegistry.add(null);
						
						lightMapRegistry.set(id, new LightMap(entry.getKey(), id, entry.getValue().getAsJsonObject()));
					}
				}
				if(data.has("blocks")) {
					for(Entry<String, JsonElement> entry : data.getAsJsonObject("blocks").entrySet()) {
						BlockLightData blockLightData = new BlockLightData(entry.getValue().getAsJsonObject());
						String[] blockNames = entry.getKey().split(",");
						for(String blockName : blockNames) {
							if(blockName.startsWith("#")) {
								List<String> blockNames2 = Tags.getNamesInTag(blockName);
								for(String blockName2 : blockNames2) {
									blockDataRegistry.put(blockName2, blockLightData);
								}
							}else {
								if(!blockName.contains(":"))
									blockName = "minecraft:" + blockName;
								blockDataRegistry.put(blockName, blockLightData);
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		maxLightLevel = 0;
		for(BlockLightData data : blockDataRegistry.values()) {
			maxLightLevel = (byte) Math.max(maxLightLevel, data.getMaxLightLevel());
		}
		for(LightMap map : lightMapRegistry) {
			if(!colorSets.contains(map.getColorSet()))
				colorSets.add(map.getColorSet());
		}
	}

}
