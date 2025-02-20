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

package nl.bramstout.mcworldexporter.resourcepack;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.world.Block;

public abstract class Biome {

	protected String name;
	protected int id;
	protected Color foliageColour;
	protected Color grassColour;
	protected Color waterColour;
	
	public Biome(String name, int id) {
		this.name = name;
		this.id = id;
	}
	
	public abstract void calculateTints();
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public Color getFoliageColour() {
		return foliageColour;
	}
	
	public Color getGrassColour() {
		return grassColour;
	}
	
	public Color getWaterColour() {
		return waterColour;
	}
	
	public void setFoliageColour(Color color) {
		this.foliageColour = color;
	}
	
	public void setGrassColour(Color color) {
		this.grassColour = color;
	}
	
	public void setWaterColour(Color color) {
		this.waterColour = color;
	}
	
	public Color getBiomeColor(BlockState state, Block block) {
		Tint tint = Tints.getTint(block.getName());
		if(tint != null)
			return tint.getTint(block.getProperties());
		if(state.isGrassColormap())
			return getGrassColour();
		else if(state.isFoliageColormap())
			return getFoliageColour();
		else if(state.isWaterColormap())
			return getWaterColour();
		return new Color();
	}
	
}
