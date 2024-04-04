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
	
	float r;
	float g;
	float b;
	
	public Color() {
		r = 1.0f;
		g = 1.0f;
		b = 1.0f;
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
		r = rF * 0.6131f + gF * 0.3395f + bF * 0.0474f;
        g = rF * 0.0702f + gF * 0.9164f + bF * 0.0134f;
        b = rF * 0.0206f + gF * 0.1096f + bF * 0.8698f;
	}
	
	public Color(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public void set(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
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
	
	public void add(Color other) {
		r += other.r;
		g += other.g;
		b += other.b;
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
	}
	
	public void mult(float v) {
		r *= v;
		g *= v;
		b *= v;
	}
	
}
