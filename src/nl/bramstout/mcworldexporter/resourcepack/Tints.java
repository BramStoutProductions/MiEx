package nl.bramstout.mcworldexporter.resourcepack;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class Tints {
	
	public static class Tint{
		
		private Color baseTint;
		private Map<List<Map<String, String>>, Color> stateTints;
		
		public Tint(JsonObject data) {
			baseTint = new Color(1f,1f,1f);
			stateTints = null;
			if(data.has("tint")) {
				JsonElement tintData = data.get("tint");
				baseTint = getColorFromElement(tintData);
			}
			if(data.has("states")) {
				stateTints = new HashMap<List<Map<String, String>>, Color>();
				JsonObject statesData = data.get("states").getAsJsonObject();
				for(Entry<String, JsonElement> entry : statesData.asMap().entrySet()) {
					List<Map<String, String>> checks = new ArrayList<Map<String, String>>();
					for(String checkToken : entry.getKey().split("\\|\\|")) {
						Map<String, String> check = new HashMap<String, String>();
						for(String checkToken2 : checkToken.split(",")) {
							if(checkToken2.contains("=")) {
								String[] tokens = checkToken2.split("=");
								check.put(tokens[0], tokens[1]);
							}
						}
						checks.add(check);
					}
					stateTints.put(checks, getColorFromElement(entry.getValue()));
				}
			}
		}
		
		private Color getColorFromElement(JsonElement tintData) {
			if(tintData.isJsonPrimitive()) {
				JsonPrimitive tintPrim = tintData.getAsJsonPrimitive();
				if(tintPrim.isString()) {
					String tintString = tintPrim.getAsString();
					if(tintString.startsWith("#"))
						tintString = tintString.substring(1);
					try {
						int rgb = Integer.parseUnsignedInt(tintString, 16);
						return new Color(rgb);
					}catch(Exception ex) {}
				}else if(tintPrim.isNumber()) {
					return new Color(tintPrim.getAsInt());
				}
			}
			return new Color(1f,1f,1f);
		}
		
		public Color getTint(NbtTagCompound properties) {
			if(stateTints == null || properties == null)
				return baseTint;
			
			for(Entry<List<Map<String, String>>, Color> state : stateTints.entrySet()) {
				if(useTint(properties, state.getKey())) {
					return state.getValue();
				}
			}
			return baseTint;
		}
		
		private boolean useTint(NbtTagCompound properties, List<Map<String, String>> checks) {
			if(checks.isEmpty())
				return true;
			
			for(Map<String, String> check : checks) {
				boolean res = doCheck(properties, check);
				if(res)
					return true;
			}
			return false;
		}
		
		private boolean doCheck(NbtTagCompound properties, Map<String, String> check) {
			int numItems = properties.getSize();
			for(int i = 0; i < numItems; ++i) {
				NbtTag tag = properties.get(i);
				String value = check.get(tag.getName());
				if(value != null) {
					String propValue = tag.asString();
					if(propValue != null) {
						if(!value.equals(propValue)) {
							if(!((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))))
								return false;
						}
					}
				}
			}
			return true;
		}
		
	}
	
	private static Map<String, Tint> tintRegistry = new HashMap<String, Tint>();
	private static BufferedImage grassColorMap = null;
	private static BufferedImage foliageColorMap = null;
	private static Object mutex = new Object();
	
	public static void load() {
		tintRegistry.clear();
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File tintsFile = new File(resourcePacks.get(i).getFolder(), "miex_block_tints.json");
			if(!tintsFile.exists())
				continue;
			try {
				JsonObject data = Json.read(tintsFile).getAsJsonObject();
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					String blockName = entry.getKey();
					if(blockName.startsWith("#")) {
						List<String> blockNames = Tags.getNamesInTag(blockName);
						for(String blockName2 : blockNames) {
							tintRegistry.put(blockName2, new Tint(entry.getValue().getAsJsonObject()));
						}
					}else {
						if(!blockName.contains(":"))
							blockName = "minecraft:" + blockName;
						tintRegistry.put(blockName, new Tint(entry.getValue().getAsJsonObject()));
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		synchronized(mutex) {
			grassColorMap = null;
			foliageColorMap = null;
		}
	}
	
	public static Tint getTint(String name) {
		return tintRegistry.getOrDefault(name, null);
	}
	
	public static BufferedImage getGrassColorMap() {
		if (grassColorMap != null)
			return grassColorMap;
		synchronized(mutex) {
			if (grassColorMap != null)
				return grassColorMap;
			try {
				File mapFile = ResourcePacks.getTexture("minecraft:colormap/grass");
				if(mapFile != null && mapFile.exists())
					grassColorMap = ImageReader.readImage(mapFile);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return grassColorMap;
		}
	}
	
	public static BufferedImage getFoliageColorMap() {
		if (foliageColorMap != null)
			return foliageColorMap;
		synchronized(mutex) {
			if (foliageColorMap != null)
				return foliageColorMap;
			try {
				File mapFile = ResourcePacks.getTexture("minecraft:colormap/foliage");
				if(mapFile != null && mapFile.exists())
					foliageColorMap = ImageReader.readImage(mapFile);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return foliageColorMap;
		}
	}
	
}
