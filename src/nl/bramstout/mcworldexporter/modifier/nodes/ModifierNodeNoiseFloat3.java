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

import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

/**
 * "noiseFloat3" returns a smoothly varying random float3.
 * If position is null, then it uses the block position.
 */
public class ModifierNodeNoiseFloat3 extends ModifierNode{

	public Attribute position;
	public Attribute scale;
	public Attribute minValue;
	public Attribute maxValue;
	public Attribute octaves;
	public Attribute octaveAmplitude;
	public Attribute octaveScale;
	public Attribute offset;
	public Attribute evolution;
	public Attribute evolutionPeriod;
	public Attribute evolutionOctaveScale;
	
	public ModifierNodeNoiseFloat3(String name) {
		super(name);
		this.position = new Attribute(this, new Value());
		this.scale = new Attribute(this, new Value(32f));
		this.minValue = new Attribute(this, new Value(0f));
		this.maxValue = new Attribute(this, new Value(1f));
		this.octaves = new Attribute(this, new Value(4));
		this.octaveAmplitude = new Attribute(this, new Value(0.7f));
		this.octaveScale = new Attribute(this, new Value(0.707f));
		this.offset = new Attribute(this, new Value(0f));
		this.evolution = new Attribute(this, new Value(0f));
		this.evolutionPeriod = new Attribute(this, new Value(0f));
		this.evolutionOctaveScale = new Attribute(this, new Value(1.5f));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value valuePosition = context.getValue(position);
		float x = context.blockX;
		float y = context.blockY;
		float z = context.blockZ;
		if(!valuePosition.isNull()) {
			x = valuePosition.getX();
			y = valuePosition.getY();
			z = valuePosition.getZ();
		}
		Value valueOffset = context.getValue(offset);
		x += valueOffset.getX();
		y += valueOffset.getY();
		z += valueOffset.getZ();
		Value valueScale = context.getValue(scale);
		x /= valueScale.getX();
		y /= valueScale.getY();
		z /= valueScale.getZ();
		Value valueOctaves = context.getValue(octaves);
		Value valueOctaveAmplitude = context.getValue(octaveAmplitude);
		Value valueOctaveScale = context.getValue(octaveScale);
		Value valueMinValue = context.getValue(minValue);
		Value valueMaxValue = context.getValue(maxValue);
		Value valueEvolution = context.getValue(evolution);
		Value valueEvolutionPeriod = context.getValue(evolutionPeriod);
		Value valueEvolutionOctaveScale = context.getValue(evolutionOctaveScale);
		
		float noiseR = ModifierNodeNoiseFloat.evalFractalNoise(x, y, z, valueOctaves.getInt(), 
				valueOctaveAmplitude.getX(), valueOctaveScale.getX(), 
				valueEvolution.getX(), valueEvolutionPeriod.getX(), valueEvolutionOctaveScale.getX());
		float noiseG = ModifierNodeNoiseFloat.evalFractalNoise(x + 160, y, z, valueOctaves.getInt(), 
				valueOctaveAmplitude.getX(), valueOctaveScale.getX(), 
				valueEvolution.getX(), valueEvolutionPeriod.getX(), valueEvolutionOctaveScale.getX());
		float noiseB = ModifierNodeNoiseFloat.evalFractalNoise(x, y + 400, z, valueOctaves.getInt(), 
				valueOctaveAmplitude.getX(), valueOctaveScale.getX(), 
				valueEvolution.getX(), valueEvolutionPeriod.getX(), valueEvolutionOctaveScale.getX());
		
		noiseR = noiseR * (valueMaxValue.getR() - valueMinValue.getR()) + valueMinValue.getR();
		noiseG = noiseG * (valueMaxValue.getG() - valueMinValue.getG()) + valueMinValue.getG();
		noiseB = noiseB * (valueMaxValue.getB() - valueMinValue.getB()) + valueMinValue.getB();
		
		return new Value(noiseR, noiseG, noiseB);
	}
	
}
