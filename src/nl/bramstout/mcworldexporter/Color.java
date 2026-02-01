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

package nl.bramstout.mcworldexporter;

public class Color {
	
	public static ColorGamut GAMUT = ColorGamut.ACEScg;
	
	float r;
	float g;
	float b;
	float a;
	
	public Color() {
		r = 1.0f;
		g = 1.0f;
		b = 1.0f;
		a = 1.0f;
	}
	
	public Color(Color other) {
		r = other.r;
		g = other.g;
		b = other.b;
		a = other.a;
	}
	
	public Color(int RGB) {
		int rI = (RGB >> 16) & 0xFF;
		int gI = (RGB >> 8) & 0xFF;
		int bI = RGB & 0xFF;
		float rF = ((float) rI) / 255.0f;
		float gF = ((float) gI) / 255.0f;
		float bF = ((float) bI) / 255.0f;
		rF = (float) Math.pow(rF, 2.2f);
		gF = (float) Math.pow(gF, 2.2f);
		bF = (float) Math.pow(bF, 2.2f);
		r = rF * GAMUT.r0 + gF * GAMUT.r1 + bF * GAMUT.r2;
        g = rF * GAMUT.g0 + gF * GAMUT.g1 + bF * GAMUT.g2;
        b = rF * GAMUT.b0 + gF * GAMUT.b1 + bF * GAMUT.b2;
        a = 1.0f;
	}
	
	public Color(int RGB, boolean hasAlpha, boolean toRenderGamut) {
		int aI = (RGB >> 24) & 0xFF;
		int rI = (RGB >> 16) & 0xFF;
		int gI = (RGB >> 8) & 0xFF;
		int bI = RGB & 0xFF;
		float rF = ((float) rI) / 255.0f;
		float gF = ((float) gI) / 255.0f;
		float bF = ((float) bI) / 255.0f;
		rF = (float) Math.pow(rF, 2.2f);
		gF = (float) Math.pow(gF, 2.2f);
		bF = (float) Math.pow(bF, 2.2f);
		if(toRenderGamut) {
			r = rF * GAMUT.r0 + gF * GAMUT.r1 + bF * GAMUT.r2;
	        g = rF * GAMUT.g0 + gF * GAMUT.g1 + bF * GAMUT.g2;
	        b = rF * GAMUT.b0 + gF * GAMUT.b1 + bF * GAMUT.b2;
		}else {
			r = rF;
			g = gF;
			b = bF;
		}
        a = hasAlpha ? (((float) aI) / 255.0f) : 1.0f;
	}
	
	public Color(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 1.0f;
	}
	
	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public void setFromInt(int RGB) {
		setFromInt(RGB, false, true);
	}
	
	public void setFromInt(int RGB, boolean hasAlpha, boolean toRenderGamut) {
		int aI = (RGB >> 24) & 0xFF;
		int rI = (RGB >> 16) & 0xFF;
		int gI = (RGB >> 8) & 0xFF;
		int bI = RGB & 0xFF;
		float rF = ((float) rI) / 255.0f;
		float gF = ((float) gI) / 255.0f;
		float bF = ((float) bI) / 255.0f;
		rF = (float) Math.pow(rF, 2.2f);
		gF = (float) Math.pow(gF, 2.2f);
		bF = (float) Math.pow(bF, 2.2f);
		if(toRenderGamut) {
			r = rF * GAMUT.r0 + gF * GAMUT.r1 + bF * GAMUT.r2;
	        g = rF * GAMUT.g0 + gF * GAMUT.g1 + bF * GAMUT.g2;
	        b = rF * GAMUT.b0 + gF * GAMUT.b1 + bF * GAMUT.b2;
		}else {
			r = rF;
			g = gF;
			b = bF;
		}
        a = hasAlpha ? (((float) aI) / 255.0f) : 1.0f;
	}
	
	public void set(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public void set(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public void setR(float r) {
		this.r = r;
	}
	
	public void setG(float g) {
		this.g = g;
	}
	
	public void setB(float b) {
		this.b = b;
	}
	
	public void setA(float a) {
		this.a = a;
	}
	
	public float getR() {
		return r;
	}
	
	public float getG() {
		return g;
	}
	
	public float getB() {
		return b;
	}
	
	public float getA() {
		return a;
	}
	
	public int getRGB() {
		float rF = (float) Math.pow(r, 1.0f / 2.2f);
		float gF = (float) Math.pow(g, 1.0f / 2.2f);
		float bF = (float) Math.pow(b, 1.0f / 2.2f);
		
		int rI = (int) (rF * 255.0f);
		int gI = (int) (gF * 255.0f);
		int bI = (int) (bF * 255.0f);
		int aI = (int) (a * 255.0f);
		return aI << 24 | rI << 16 | gI << 8 | bI;
	}
	
	public void add(Color other) {
		r += other.r;
		g += other.g;
		b += other.b;
		a = 1.0f - ((1.0f - a) * (1.0f - other.a));
	}
	
	public void addWeighted(Color other, float weight) {
		r += other.r * weight;
		g += other.g * weight;
		b += other.b * weight;
	}
	
	public void mult(Color other) {
		r *= other.r;
		g *= other.g;
		b *= other.b;
		a *= other.a;
	}
	
	public void mult(float v) {
		r *= v;
		g *= v;
		b *= v;
		a *= v;
	}
	
	public void composite(Color other) {
		r = r * (1.0f - other.a) + other.r * other.a;
		g = g * (1.0f - other.a) + other.g * other.a;
		b = b * (1.0f - other.a) + other.b * other.a;
		a = 1.0f - ((1.0f - a) * (1.0f - other.a)); 
	}
	
}
