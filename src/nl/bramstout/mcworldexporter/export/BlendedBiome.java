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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.resourcepack.Biome;

public class BlendedBiome {
	
	private static class WeightedColor{
		public Color color;
		public float weight;
		
		public WeightedColor() {
			this.color = new Color(0f, 0f, 0f, 0f);
			this.weight = 0f;
		}
		
		public WeightedColor(WeightedColor other) {
			this.color = new Color(other.color);
			this.weight = other.weight;
		}
	}
	
	private boolean empty;
	private Map<String, WeightedColor> colors;
	
	public BlendedBiome() {
		this.empty = true;
		this.colors = new HashMap<String, WeightedColor>();
	}
	
	public BlendedBiome(BlendedBiome other) {
		this.empty = other.empty;
		this.colors = new HashMap<String, WeightedColor>();
		for(Entry<String, WeightedColor> entry : other.colors.entrySet()) {
			this.colors.put(entry.getKey(), new WeightedColor(entry.getValue()));
		}
	}
	
	public boolean isEmpty() {
		return empty;
	}
	
	public void clear() {
		if(this.empty)
			return;
		this.empty = true;
		for(Entry<String, WeightedColor> entry : colors.entrySet()) {
			entry.getValue().color.set(0f, 0f, 0f, 0f);
			entry.getValue().weight = 0f;
		}
	}
	
	public void normalise() {
		for(Entry<String, WeightedColor> entry : colors.entrySet()) {
			if(entry.getValue().weight > 0.0f) {
				float invWeight = 1.0f / entry.getValue().weight;
				entry.getValue().color.mult(invWeight);
			}else {
				entry.getValue().color.set(1f, 1f, 1f);
			}
		}
	}
	
	public Color getColor(String name) {
		WeightedColor color = colors.getOrDefault(name, null);
		if(color == null)
			return null;
		if(color.weight == 0.0f)
			return null;
		return color.color;
	}
	
	public void setColor(String name, float r, float g, float b) {
		this.empty = false;
		WeightedColor weightedColor = new WeightedColor();
		weightedColor.color.set(r, g, b, 1f);
		weightedColor.weight = 1f;
		this.colors.put(name, weightedColor);
	}
	
	public void addBiome(Biome biome, float weight) {
		this.empty = false;
		for(Entry<String, Color> entry : biome.getColors()) {
			if(entry.getValue() == null)
				continue;
			WeightedColor color = colors.getOrDefault(entry.getKey(), null);
			if(color == null) {
				color = new WeightedColor();
				colors.put(entry.getKey(), color);
			}
			color.color.addWeighted(entry.getValue(), weight);
			color.weight += weight;
		}
	}
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeBoolean(empty);
		dos.writeInt(colors.size());
		for(Entry<String, WeightedColor> entry : colors.entrySet()) {
			dos.writeUTF(entry.getKey());
			dos.writeFloat(entry.getValue().color.getR());
			dos.writeFloat(entry.getValue().color.getG());
			dos.writeFloat(entry.getValue().color.getB());
			dos.writeFloat(entry.getValue().color.getA());
			dos.writeFloat(entry.getValue().weight);
		}
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		empty = dis.readBoolean();
		int numColors = dis.readInt();
		colors.clear();
		for(int i = 0; i < numColors; ++i) {
			String name = dis.readUTF();
			float r = dis.readFloat();
			float g = dis.readFloat();
			float b = dis.readFloat();
			float a = dis.readFloat();
			float weight = dis.readFloat();
			WeightedColor weightedColor = new WeightedColor();
			weightedColor.color.set(r, g, b, a);
			weightedColor.weight = weight;
			colors.put(name, weightedColor);
		}
	}
	
}
