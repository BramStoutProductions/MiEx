package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.world.Block;

public abstract class BlockConstraints {

	public abstract boolean test(Block block, int x, int y, int z, Biome biome);
	
	public static class BlockConstraintBiome extends BlockConstraints{
		
		public List<String> biomes;
		public boolean invert;
		
		public BlockConstraintBiome() {
			biomes = new ArrayList<String>();
			invert = false;
		}
		
		@Override
		public boolean test(Block block, int x, int y, int z, Biome biome) {
			boolean hits = biomes.contains(biome.getName());
			if(invert)
				return !hits;
			return hits;
		}
		
	}
	
	public static class HeightRange{
		public int min;
		public int max;
		
		public HeightRange(int min, int max) {
			this.min = min;
			this.max = max;
		}
		
		public boolean test(int y){
			return y >= min && y <= max;
		}
	}
	
	public static class BlockConstraintHeight extends BlockConstraints{
		
		public List<HeightRange> ranges;
		
		public BlockConstraintHeight() {
			ranges = new ArrayList<HeightRange>();
		}
		
		@Override
		public boolean test(Block block, int x, int y, int z, Biome biome) {
			for(HeightRange range : ranges) {
				if(range.test(y))
					return true;
			}
			return false;
		}
		
	}
	
}
