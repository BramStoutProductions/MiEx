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

package nl.bramstout.mcworldexporter.modifier.nodes;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

/**
 * "getBiomeColor" node outputs the biome colour for
 * the specified colormap identifier.
 * It outputs null if no colormap exists of that name.
 */
public class ModifierNodeGetBiomeColor extends ModifierNode{

	public Attribute colormap;
	
	public ModifierNodeGetBiomeColor(String name) {
		super(name);
		this.colormap = new Attribute(this, new Value("minecraft:grass"));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value valueColormap = context.getValue(colormap);
		String colormapName = valueColormap.getString();
		if(colormapName.indexOf(':') == -1)
			colormapName = "minecraft:" + colormapName;
		
		Color color = context.biome[0].getColor(colormapName);
		if(color != null)
			return new Value(color.getR(), color.getG(), color.getB());
		return new Value();
	}

}
