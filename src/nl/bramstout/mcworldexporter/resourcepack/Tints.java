package nl.bramstout.mcworldexporter.resourcepack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Double;
import nl.bramstout.mcworldexporter.nbt.TAG_Float;
import nl.bramstout.mcworldexporter.nbt.TAG_Int;
import nl.bramstout.mcworldexporter.nbt.TAG_Long;
import nl.bramstout.mcworldexporter.nbt.TAG_Short;
import nl.bramstout.mcworldexporter.nbt.TAG_String;

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
		
		public Color getTint(TAG_Compound properties) {
			if(stateTints == null || properties == null)
				return baseTint;
			
			for(Entry<List<Map<String, String>>, Color> state : stateTints.entrySet()) {
				if(useTint(properties, state.getKey())) {
					return state.getValue();
				}
			}
			return baseTint;
		}
		
		private boolean useTint(TAG_Compound properties, List<Map<String, String>> checks) {
			if(checks.isEmpty())
				return true;
			
			for(Map<String, String> check : checks) {
				boolean res = doCheck(properties, check);
				if(res)
					return true;
			}
			return false;
		}
		
		private boolean doCheck(TAG_Compound properties, Map<String, String> check) {
			for(NBT_Tag tag : properties.elements) {
				String value = check.get(tag.getName());
				String propValue = null;
				if(value != null) {
					switch(tag.ID()) {
					case 1:
						// Byte
						propValue = Byte.toString(((TAG_Byte)tag).value);
						break;
					case 2:
						// Short
						propValue = Short.toString(((TAG_Short)tag).value);
						break;
					case 3:
						// Int
						propValue = Integer.toString(((TAG_Int)tag).value);
						break;
					case 4:
						// Long
						propValue = Long.toString(((TAG_Long)tag).value);
						break;
					case 5:
						// Float
						propValue = Float.toString(((TAG_Float)tag).value);
						break;
					case 6:
						// Double
						propValue = Double.toString(((TAG_Double)tag).value);
						break;
					case 8:
						// String
						propValue = ((TAG_String)tag).value;
						break;
					default:
						break;
					}
					
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
	
	public static void load() {
		tintRegistry.clear();
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File tintsFile = new File(FileUtil.getResourcePackDir(), resourcePacks.get(i) + "/miex_block_tints.json");
			if(!tintsFile.exists())
				continue;
			try {
				JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(tintsFile)))).getAsJsonObject();
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
	}
	
	public static Tint getTint(String name) {
		return tintRegistry.getOrDefault(name, null);
	}
	
}
