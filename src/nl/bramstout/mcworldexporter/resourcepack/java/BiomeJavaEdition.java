package nl.bramstout.mcworldexporter.resourcepack.java;

import java.awt.image.BufferedImage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.Tints;

public class BiomeJavaEdition extends Biome{
	
	private JsonObject data;
	
	public BiomeJavaEdition(String name, int id, JsonObject data) {
		super(name, id);
		this.data = data;
		calculateTints();
	}
	
	@Override
	public void calculateTints() {
		int foliageColourI = 0xffffff;
		int grassColourI = 0xffffff;
		int waterColourI = 0xffffff;
		
		if(data != null) {
			foliageColourI = 0;
			grassColourI = 0;
			JsonObject effectsObj = data.getAsJsonObject("effects");
			if(effectsObj != null) {
				JsonElement foliageColourObj = effectsObj.get("foliage_color");
				if(foliageColourObj != null)
					foliageColourI = foliageColourObj.getAsInt();
				
				JsonElement grassColourObj = effectsObj.get("grass_color");
				if(grassColourObj != null)
					grassColourI = grassColourObj.getAsInt();
				
				JsonElement waterColourObj = effectsObj.get("water_color");
				if(waterColourObj != null)
					waterColourI = waterColourObj.getAsInt();
			}
			
			float downfall = 0.5f;
			float temperature = 0.5f;
			
			JsonElement downfallObj = data.get("downfall");
			if(downfallObj != null)
				downfall = downfallObj.getAsFloat();
			
			JsonElement temperatureObj = data.get("temperature");
			if(temperatureObj != null)
				temperature = temperatureObj.getAsFloat();
			
			float tintX = Math.max(Math.min(temperature, 1.0f), 0.0f);
			float tintY = 1.0f - (Math.max(Math.min(downfall, 1.0f), 0.0f) * tintX);
			tintX = 1.0f - tintX;
			
			if(foliageColourI == 0) {
				BufferedImage foliageColorMap = Tints.getFoliageColorMap();
				if(foliageColorMap != null) {
					int tintXI = (int) (tintX * ((float) (foliageColorMap.getWidth()-1)));
					int tintYI = (int) (tintY * ((float) (foliageColorMap.getHeight()-1)));
					foliageColourI = foliageColorMap.getRGB(tintXI, tintYI);
				}
			}
			
			if(grassColourI == 0) {
				BufferedImage grassColorMap = Tints.getGrassColorMap();
				if(grassColorMap != null) {
					int tintXI = (int) (tintX * ((float) (grassColorMap.getWidth()-1)));
					int tintYI = (int) (tintY * ((float) (grassColorMap.getHeight()-1)));
					
					grassColourI = grassColorMap.getRGB(tintXI, tintYI);
				}
			}
		}
		
		foliageColour = new Color(foliageColourI);
		grassColour = new Color(grassColourI);
		waterColour = new Color(waterColourI);
	}

}
