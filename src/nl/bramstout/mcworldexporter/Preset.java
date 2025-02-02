package nl.bramstout.mcworldexporter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class Preset {
	
	
	private static List<Preset> presets;
	
	public static void loadPresets() {
		presets = new ArrayList<Preset>();
		
		File resourcePacksDir = new File(FileUtil.resourcePackDir);
		if(!resourcePacksDir.exists() || !resourcePacksDir.isDirectory())
			return;
		
		Map<String, Integer> nameCounter = new HashMap<String, Integer>();
		Integer zero = Integer.valueOf(0);
		
		for(ResourcePack resourcePack : ResourcePacks.getResourcePacks()) {
			File presetsFile = new File(resourcePack.getFolder(), "miex_presets.json");
			if(!presetsFile.exists())
				continue;
			
			try {
				JsonObject presetsData = Json.read(presetsFile).getAsJsonObject();
				
				for(Map.Entry<String, JsonElement> entry : presetsData.entrySet()) {
					if(!entry.getValue().isJsonObject())
						continue;
					
					String name = entry.getKey();
					String parent = resourcePack.getName();
					String parentUUID = resourcePack.getUUID();
					
					Preset preset = new Preset(name, parent, parentUUID, entry.getValue().getAsJsonObject());
					presets.add(preset);
					
					
					nameCounter.put(name, nameCounter.getOrDefault(name, zero).intValue() + 1);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// Go through all presets and if any name is there multiple times, then add the resource pack name in front of it.
		for(Preset preset : presets) {
			if(nameCounter.get(preset.getName()).intValue() > 1)
				preset.name = preset.getParent() + "/" + preset.getName();
		}
		
		// Sort the presets list
		presets.sort(new Comparator<Preset>() {

			@Override
			public int compare(Preset o1, Preset o2) {
				return o1.name.compareToIgnoreCase(o2.name);
			}
			
		});
	}
	
	public static List<Preset> getPresets(){
		if(presets == null)
			loadPresets();
		return presets;
	}
	
	public static Preset getPresetFromName(String name) {
		if(presets == null)
			loadPresets();
		for(Preset preset : presets)
			if(preset.name.equals(name))
				return preset;
		return null;
	}
	
	public static void addPreset(Preset preset) {
		// First get the resource pack that belongs to it, defaulting to the base resource pack.
		ResourcePack pack = ResourcePacks.getBaseResourcePack();
		for(ResourcePack pack2 : ResourcePacks.getResourcePacks()) {
			if(pack2.getName().equals(preset.getParent())) {
				pack = pack2;
				break;
			}
		}
		
		JsonObject presetsData = new JsonObject();
		File presetsFile = new File(pack.getFolder(), "miex_presets.json");
		if(presetsFile.exists()) {
			try {
				presetsData = Json.read(presetsFile).getAsJsonObject();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		String presetName = preset.getName();
		if(presetName.startsWith(preset.getParent() + "/")) {
			presetName = presetName.substring(preset.getParent().length() + 1);
		}
		
		presetsData.add(presetName, preset.toJson());
		
		// Write it out
		FileWriter writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			String jsonString = gson.toJson(presetsData);
			writer = new FileWriter(presetsFile);
			writer.write(jsonString);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(writer != null) {
			try {
				writer.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		// Reload presets
		loadPresets();
	}
	
	
	private String name;
	private String parent;
	private String parentUUID;
	private List<String> resourcePacks;
	private Boolean runOptimisers;
	private Boolean removeCaves;
	private Boolean fillInCaves;
	private Boolean onlyIndividualBlocks;
	private Integer chunkSize;
	
	public Preset(String name, String parent, String parentUUID) {
		this.name = name;
		this.parent = parent;
		this.parentUUID = parentUUID;
		resourcePacks = null;
		runOptimisers = null;
		removeCaves = null;
		fillInCaves = null;
		onlyIndividualBlocks = null;
		chunkSize = null;
	}
	
	public Preset(String name, String parent, String parentUUID, JsonObject data) {
		this(name, parent, parentUUID);
		if(data.has("resourcePacks")) {
			JsonArray rpArray = data.getAsJsonArray("resourcePacks");
			resourcePacks = new ArrayList<String>();
			for(JsonElement el : rpArray.asList()) {
				resourcePacks.add(el.getAsString());
			}
		}
		if(data.has("runOptimisers")) {
			runOptimisers = Boolean.valueOf(data.get("runOptimisers").getAsBoolean());
		}
		if(data.has("removeCaves")) {
			removeCaves = Boolean.valueOf(data.get("removeCaves").getAsBoolean());
		}
		if(data.has("fillInCaves")) {
			fillInCaves = Boolean.valueOf(data.get("fillInCaves").getAsBoolean());
		}
		if(data.has("onlyIndividualBlocks")) {
			onlyIndividualBlocks = Boolean.valueOf(data.get("onlyIndividualBlocks").getAsBoolean());
		}
		if(data.has("chunkSize")) {
			chunkSize = Integer.valueOf(data.get("chunkSize").getAsInt());
		}
	}
	
	public void apply() {		
		if(runOptimisers != null) {
			Config.runOptimiser = runOptimisers.booleanValue();
		}
		
		if(removeCaves != null) {
			Config.removeCaves = removeCaves.booleanValue();
		}
		
		if(fillInCaves != null) {
			Config.fillInCaves = fillInCaves.booleanValue();
		}
		
		if(onlyIndividualBlocks != null) {
			Config.onlyIndividualBlocks = onlyIndividualBlocks.booleanValue();
		}
		
		if(chunkSize != null) {
			Config.chunkSize = chunkSize.intValue();
		}
		MCWorldExporter.getApp().getUI().update();
		
		if(resourcePacks != null) {
			MCWorldExporter.getApp().getUI().getResourcePackManager().clear();
			MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(resourcePacks);
		}
	}
	
	public JsonObject toJson() {
		JsonObject data = new JsonObject();
		
		if(resourcePacks != null) {
			JsonArray rpArray = new JsonArray();
			for(String rp : resourcePacks)
				rpArray.add(rp);
			data.add("resourcePacks", rpArray);
		}
		
		if(runOptimisers != null) {
			data.addProperty("runOptimisers", runOptimisers);
		}
		
		if(removeCaves != null) {
			data.addProperty("removeCaves", removeCaves);
		}
		
		if(fillInCaves != null) {
			data.addProperty("fillInCaves", fillInCaves);
		}
		
		if(onlyIndividualBlocks != null) {
			data.addProperty("onlyIndividualBlocks", onlyIndividualBlocks);
		}
		
		if(chunkSize != null) {
			data.addProperty("chunkSize", chunkSize);
		}
		
		return data;
	}
	
	public void fromApp() {
		resourcePacks = new ArrayList<String>();
		for(ResourcePack pack : ResourcePacks.getActiveResourcePacks()) {
			if(pack == ResourcePacks.getBaseResourcePack())
				continue;
			resourcePacks.add(pack.getUUID());
		}
		runOptimisers = Boolean.valueOf(Config.runOptimiser);
		removeCaves = Boolean.valueOf(Config.removeCaves);
		fillInCaves = Boolean.valueOf(Config.fillInCaves);
		onlyIndividualBlocks = Boolean.valueOf(Config.onlyIndividualBlocks);
		chunkSize = Integer.valueOf(Config.chunkSize);
	}
	
	public String getName() {
		return name;
	}
	
	public String getParent() {
		return parent;
	}
	
	public String getParentUUID() {
		return parentUUID;
	}
	
}
