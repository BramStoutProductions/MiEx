/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
