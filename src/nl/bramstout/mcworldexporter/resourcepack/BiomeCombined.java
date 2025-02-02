package nl.bramstout.mcworldexporter.resourcepack;

import java.util.ArrayList;
import java.util.List;

public class BiomeCombined extends Biome{

	private List<Biome> subBiomes;
	
	public BiomeCombined(String name, int id) {
		super(name, id);
		subBiomes = new ArrayList<Biome>();
	}
	
	public List<Biome> getSubBiomes(){
		return subBiomes;
	}

	@Override
	public void calculateTints() {
		foliageColour = null;
		grassColour = null;
		waterColour = null;
		
		for(Biome biome : subBiomes) {
			if(biome.foliageColour != null && foliageColour == null)
				foliageColour = biome.foliageColour;
			if(biome.grassColour != null && grassColour == null)
				grassColour = biome.grassColour;
			if(biome.waterColour != null && waterColour == null)
				waterColour = biome.waterColour;
		}
	}

}
