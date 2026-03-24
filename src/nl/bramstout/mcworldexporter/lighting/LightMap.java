package nl.bramstout.mcworldexporter.lighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;

public class LightMap {
	
	private short id;
	private String name;
	private String colorSet;
	private float lightGamma;
	private List<Color> colors;
	
	public LightMap(String name, short id, JsonObject data) {
		this.id = id;
		this.name = name;
		this.colorSet = "lighting".intern();
		this.lightGamma = 2.0f;
		this.colors = new ArrayList<Color>();
		colors.add(new Color(0f, 0f, 0f));
		if(data == null)
			return;
		
		if(data.has("colorSet"))
			colorSet = data.get("colorSet").getAsString().intern();
		
		for(Entry<String, JsonElement> entry : data.entrySet()) {
			try {
				byte lightLevel = Byte.parseByte(entry.getKey());
				// Ensure that colors is of the right size.
				for(int i = colors.size(); i <= lightLevel; ++i)
					colors.add(null);
				colors.set(lightLevel, parseColor(entry.getValue()));
			}catch(Exception ex) {}
		}
		// Force the colour for light value 0 to always be black.
		colors.set(0, new Color(0f, 0f, 0f));
		
		float gamma = 1f;
		if(data.has("gamma"))
			gamma = data.get("gamma").getAsFloat();
		if(data.has("lightGamma"))
			lightGamma = data.get("lightGamma").getAsFloat();
		
		// Now interpolate any missing colours
		int prevColorIndex = 0;
		int nextColorIndex = -1;
		float maxLightLevel = colors.size() - 1;
		for(int i = 0; i < colors.size(); ++i) {
			if(colors.get(i) != null) {
				// We have a colour, so no need to do anything.
				prevColorIndex = i;
				nextColorIndex = -1;
				continue;
			}
			if(nextColorIndex == -1) {
				// Find the next valid colour and cache it.
				for(int j = i+1; j < colors.size(); ++j) {
					if(colors.get(j) != null) {
						nextColorIndex = j;
						break;
					}
				}
			}
			// Now we can interpolate.
			Color prevColor = new Color(colors.get(prevColorIndex));
			Color nextColor = new Color(colors.get(nextColorIndex));
			float fi = (float) Math.pow(((float) i) / maxLightLevel, lightGamma);
			float fprevColorIndex = (float) Math.pow(((float) prevColorIndex) / maxLightLevel, lightGamma);
			float fnextColorIndex = (float) Math.pow(((float) nextColorIndex) / maxLightLevel, lightGamma);
			float t = (fi - fprevColorIndex) / (fnextColorIndex - fprevColorIndex);
			prevColor.pow(1.0f / gamma);
			nextColor.pow(1.0f / gamma);
			Color resColor = prevColor.lerp(nextColor, t);
			resColor.pow(gamma);
			
			colors.set(i, resColor);
		}
	}
	
	private Color parseColor(JsonElement el) {
		if(el.isJsonArray()) {
			float r = 1f;
			float g = 1f;
			float b = 1f;
			if(el.getAsJsonArray().size() >= 1) {
				r = g = b = el.getAsJsonArray().get(0).getAsFloat();
			}
			if(el.getAsJsonArray().size() >= 2) {
				g = b = el.getAsJsonArray().get(1).getAsFloat();
			}
			if(el.getAsJsonArray().size() >= 3) {
				b = el.getAsJsonArray().get(2).getAsFloat();
			}
			return new Color(r, g, b);
		}else if(el.isJsonPrimitive()) {
			if(el.getAsJsonPrimitive().isNumber()) {
				return new Color(el.getAsJsonPrimitive().getAsInt());
			}else if(el.getAsJsonPrimitive().isString()) {
				String val = el.getAsJsonPrimitive().getAsString();
				if(val.startsWith("#"))
					val = val.substring(1);
				try {
					return new Color(Integer.parseUnsignedInt(val, 16));
				}catch(Exception ex) {}
			}
		}else if(el.isJsonObject()) {
			Color color = new Color(1f, 1f, 1f);
			if(el.getAsJsonObject().has("color"))
				color = parseColor(el.getAsJsonObject().get("color"));
			if(el.getAsJsonObject().has("intensity"))
				color.mult(el.getAsJsonObject().get("intensity").getAsFloat());
		}
		return new Color();
	}
	
	public Color getColor(byte lightLevel) {
		lightLevel = (byte) Math.min(Math.max(lightLevel, 0), colors.size()-1);
		return colors.get(lightLevel);
	}
	
	public byte getMaxLightLevel() {
		return (byte) (colors.size()-1);
	}
	
	public short getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getColorSet() {
		return colorSet;
	}
	
	public float getLightGamma() {
		return lightGamma;
	}

}
