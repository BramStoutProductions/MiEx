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
 * "adjustHSV" node takes in a FLOAT3 colour, converts it to HSV
 * and then add the XYZ values from adjustment to the HSV values,
 * afterwards converting it back to RGB and outputting that.
 */
public class ModifierNodeAdjustHSV extends ModifierNode{

	public Attribute color;
	public Attribute adjustment;
	
	public ModifierNodeAdjustHSV(String name) {
		super(name);
		this.color = new Attribute(this, new Value(1f));
		this.adjustment = new Attribute(this, new Value(0f));
	}

	@Override
	public Value evaluate(ModifierContext context) {
		Value valueColor = context.getValue(color);
		Value valueAdjustment = context.getValue(adjustment);
		
		Value hsv = RGBtoHSV(valueColor);
		hsv = hsv.add(valueAdjustment);
		// Make sure to clamp S and V so that we don't
		// get any weird colours. H is looped around,
		// so no need to clamp that.
		hsv = new Value(hsv.getR(), 
				Math.min(Math.max(hsv.getG(), 0f), 1f),
				Math.min(Math.max(hsv.getB(), 0f), 1f));
		return HSVtoRGB(hsv);
	}
	
	private float fmod(float x, float y) {
		x /= y;
		x -= Math.floor(x);
		x *= y;
		return x;
	}
	
	private Value RGBtoHSV(Value rgb) {
		float M = Math.max(Math.max(rgb.getR(), rgb.getG()), rgb.getB());
		if(M < 0.000001f)
			return new Value(0f, 0f, M);
		float m = Math.min(Math.min(rgb.getR(), rgb.getG()), rgb.getB());
		float C = M - m;
		float H = 0f;
		if(C == 0) {}
		else if(M == rgb.getR())
			H = fmod((rgb.getG() - rgb.getB()) / C, 6f);
		else if(M == rgb.getG())
			H = (rgb.getB() - rgb.getR()) / C + 2f;
		else if(M == rgb.getB())
			H = (rgb.getR() - rgb.getG()) / C + 4f;
		H *= 60f;
		float S = C / M;
		return new Value(H, S, M);
	}
	
	private Value HSVtoRGB(Value hsv) {
		float H = fmod(hsv.getR(), 360f);
		H /= 60f;
		float C = hsv.getB() * hsv.getG();
		float X = C * (1f - Math.abs(fmod(H, 2f) - 1f));
		float R = 0f;
		float G = 0f;
		float B = 0f;
		if(H < 0f) {}
		else if(H < 1f) {
			R = C;
			G = X;
		}else if(H < 2f) {
			R = X;
			G = C;
		}else if(H < 3f) {
			G = C;
			B = X;
		}else if(H < 4f) {
			G = X;
			B = C;
		}else if(H < 5f) {
			R = X;
			B = C;
		}else if(H < 6f) {
			R = C;
			B = X;
		}
		
		float m = hsv.getB() - C;
		R += m;
		G += m;
		B += m;
		return new Value(R, G, B);
	}

}
