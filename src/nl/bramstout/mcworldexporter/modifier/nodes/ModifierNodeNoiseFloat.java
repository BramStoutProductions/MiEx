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

import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

/**
 * "noiseFloat" returns a smoothly varying random float.
 * If position is null, then it uses the block position.
 */
public class ModifierNodeNoiseFloat extends ModifierNode{

	public Attribute position;
	public Attribute scale;
	public Attribute minValue;
	public Attribute maxValue;
	public Attribute octaves;
	public Attribute octaveAmplitude;
	public Attribute octaveScale;
	public Attribute offset;
	
	public ModifierNodeNoiseFloat(String name) {
		super(name);
		this.position = new Attribute(this, new Value());
		this.scale = new Attribute(this, new Value(32f));
		this.minValue = new Attribute(this, new Value(0f));
		this.maxValue = new Attribute(this, new Value(1f));
		this.octaves = new Attribute(this, new Value(4));
		this.octaveAmplitude = new Attribute(this, new Value(0.7f));
		this.octaveScale = new Attribute(this, new Value(0.707f));
		this.offset = new Attribute(this, new Value(0f));
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
		
		float noise = evalFractalNoise(x, y, z, valueOctaves.getInt(), 
				valueOctaveAmplitude.getX(), valueOctaveScale.getX());
		
		noise = noise * (valueMaxValue.getR() - valueMinValue.getR()) + valueMinValue.getR();
		
		return new Value(noise);
	}
	
	private static float lerp(float a, float b, float t) {
		return a * (1f - t) + b * t;
	}
	
	private static float smoothstep(float x) {
		return x * x * (3.0f - 2.0f * x);
	}
	
	public static float evalNoise(float x, float y, float z) {
		int ix = (int) Math.floor(x);
		int iy = (int) Math.floor(y);
		int iz = (int) Math.floor(z);
		float fx = x - ((float) ix);
		float fy = y - ((float) iy);
		float fz = z - ((float) iz);
		fx = smoothstep(fx);
		fy = smoothstep(fy);
		fz = smoothstep(fz);
		
		float noise000 = Noise.getLarge(ix, iy, iz);
		float noise010 = Noise.getLarge(ix, iy+1, iz);
		float noise100 = Noise.getLarge(ix+1, iy, iz);
		float noise110 = Noise.getLarge(ix+1, iy+1, iz);
		float noise001 = Noise.getLarge(ix, iy, iz+1);
		float noise011 = Noise.getLarge(ix, iy+1, iz+1);
		float noise101 = Noise.getLarge(ix+1, iy, iz+1);
		float noise111 = Noise.getLarge(ix+1, iy+1, iz+1);
		
		noise000 = lerp(noise000, noise001, fz);
		noise010 = lerp(noise010, noise011, fz);
		noise100 = lerp(noise100, noise101, fz);
		noise110 = lerp(noise110, noise111, fz);
		
		noise000 = lerp(noise000, noise010, fy);
		noise100 = lerp(noise100, noise110, fy);
		
		noise000 = lerp(noise000, noise100, fx);
		return noise000;
	}
	
	public static float evalFractalNoise(float x, float y, float z, int octaves, 
											float octavesAmplitude, float octavesScale) {
		float res = 0f;
		float totalWeight = 0f;
		for(int i = 0; i < octaves; ++i) {
			float weight = (float) Math.pow(octavesAmplitude, i);
			float scale = (float) Math.pow(octavesScale, -i);
			res += evalNoise(x * scale, y * scale, z * scale) * weight;
			totalWeight += weight;
		}
		return totalWeight != 0f ? (res / totalWeight) : 0f;
	}

}
