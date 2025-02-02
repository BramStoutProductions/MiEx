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

package nl.bramstout.mcworldexporter.export;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.resourcepack.Biome;

public class BlendedBiome {
	
	private Color grassColor;
	private Color foliageColor;
	private Color waterColor;
	private float grassWeight;
	private float foliageWeight;
	private float waterWeight;
	
	public BlendedBiome() {
		this.grassColor = new Color(0);
		this.foliageColor = new Color(0);
		this.waterColor = new Color(0);
		this.grassWeight = 0.0f;
		this.foliageWeight = 0.0f;
		this.waterWeight = 0.0f;
	}
	
	public void clear() {
		grassColor.set(0f, 0f, 0f);
		foliageColor.set(0f, 0f, 0f);
		waterColor.set(0f, 0f, 0f);
		grassWeight = 0.0f;
		foliageWeight = 0.0f;
		waterWeight = 0.0f;
	}
	
	public void normalise() {
		if(grassWeight > 0.0f) {
			float invWeight = 1.0f / grassWeight;
			grassColor.mult(invWeight);
		}else {
			grassColor.set(1f, 1f, 1f);
		}
		if(foliageWeight > 0.0f) {
			float invWeight = 1.0f / foliageWeight;
			foliageColor.mult(invWeight);
		}else {
			foliageColor.set(1f, 1f, 1f);
		}
		if(waterWeight > 0.0f) {
			float invWeight = 1.0f / waterWeight;
			waterColor.mult(invWeight);
		}else {
			waterColor.set(1f, 1f, 1f);
		}
	}
	
	public Color getFoliageColour() {
		return foliageColor;
	}
	
	public Color getGrassColour() {
		return grassColor;
	}
	
	public Color getWaterColour() {
		return waterColor;
	}
	
	public void addBiome(Biome biome, float weight) {
		if(biome.getGrassColour() != null) {
			grassColor.addWeighted(biome.getGrassColour(), weight);
			grassWeight += weight;
		}
		if(biome.getFoliageColour() != null) {
			foliageColor.addWeighted(biome.getFoliageColour(), weight);
			foliageWeight += weight;
		}
		if(biome.getWaterColour() != null) {
			waterColor.addWeighted(biome.getWaterColour(), weight);
			waterWeight += weight;
		}
	}
	
	public Color getBiomeColor(BakedBlockState block) {
		if(block.getTint() != null)
			return block.getTint();
		if(block.isGrassColormap())
			return getGrassColour();
		else if(block.isFoliageColormap())
			return getFoliageColour();
		else if(block.isWaterColormap())
			return getWaterColour();
		return null;
	}
	
}
