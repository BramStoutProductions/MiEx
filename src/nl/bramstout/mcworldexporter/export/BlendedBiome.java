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
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.TokenMap;
import nl.bramstout.mcworldexporter.resourcepack.Biome;

public class BlendedBiome {
	
	public static class WeightedColor{
		public Color color000;
		public float weight000;
		public Color color001;
		public float weight001;
		public Color color010;
		public float weight010;
		public Color color011;
		public float weight011;
		public Color color100;
		public float weight100;
		public Color color101;
		public float weight101;
		public Color color110;
		public float weight110;
		public Color color111;
		public float weight111;
		
		public WeightedColor() {
			this.color000 = new Color(0f, 0f, 0f, 0f);
			this.weight000 = 0f;
			this.color001 = new Color(0f, 0f, 0f, 0f);
			this.weight001 = 0f;
			this.color010 = new Color(0f, 0f, 0f, 0f);
			this.weight010 = 0f;
			this.color011 = new Color(0f, 0f, 0f, 0f);
			this.weight011 = 0f;
			this.color100 = new Color(0f, 0f, 0f, 0f);
			this.weight100 = 0f;
			this.color101 = new Color(0f, 0f, 0f, 0f);
			this.weight101 = 0f;
			this.color110 = new Color(0f, 0f, 0f, 0f);
			this.weight110 = 0f;
			this.color111 = new Color(0f, 0f, 0f, 0f);
			this.weight111 = 0f;
		}
		
		public WeightedColor(Color color) {
			this.color000 = new Color(color);
			this.weight000 = 1f;
			this.color001 = new Color(color);
			this.weight001 = 1f;
			this.color010 = new Color(color);
			this.weight010 = 1f;
			this.color011 = new Color(color);
			this.weight011 = 1f;
			this.color100 = new Color(color);
			this.weight100 = 1f;
			this.color101 = new Color(color);
			this.weight101 = 1f;
			this.color110 = new Color(color);
			this.weight110 = 1f;
			this.color111 = new Color(color);
			this.weight111 = 1f;
		}
		
		public WeightedColor(WeightedColor other) {
			this.color000 = new Color(other.color000);
			this.weight000 = other.weight000;
			this.color001 = new Color(other.color001);
			this.weight001 = other.weight001;
			this.color010 = new Color(other.color010);
			this.weight010 = other.weight010;
			this.color011 = new Color(other.color011);
			this.weight011 = other.weight011;
			this.color100 = new Color(other.color100);
			this.weight100 = other.weight100;
			this.color101 = new Color(other.color101);
			this.weight101 = other.weight101;
			this.color110 = new Color(other.color110);
			this.weight110 = other.weight110;
			this.color111 = new Color(other.color111);
			this.weight111 = other.weight111;
		}
		
		public void clear() {
			this.color000.set(0f, 0f, 0f, 0f);
			this.weight000 = 0f;
			this.color001.set(0f, 0f, 0f, 0f);
			this.weight001 = 0f;
			this.color010.set(0f, 0f, 0f, 0f);
			this.weight010 = 0f;
			this.color011.set(0f, 0f, 0f, 0f);
			this.weight011 = 0f;
			this.color100.set(0f, 0f, 0f, 0f);
			this.weight100 = 0f;
			this.color101.set(0f, 0f, 0f, 0f);
			this.weight101 = 0f;
			this.color110.set(0f, 0f, 0f, 0f);
			this.weight110 = 0f;
			this.color111.set(0f, 0f, 0f, 0f);
			this.weight111 = 0f;
		}
		
		public void normalise(){
			if(weight000 > 0.0f)
				color000.mult(1.0f / weight000);
			else
				color000.set(1f, 1f, 1f);
			
			if(weight001 > 0.0f)
				color001.mult(1.0f / weight001);
			else
				color001.set(1f, 1f, 1f);
			
			if(weight010 > 0.0f)
				color010.mult(1.0f / weight010);
			else
				color010.set(1f, 1f, 1f);
			
			if(weight011 > 0.0f)
				color011.mult(1.0f / weight011);
			else
				color011.set(1f, 1f, 1f);
			
			if(weight100 > 0.0f)
				color100.mult(1.0f / weight100);
			else
				color100.set(1f, 1f, 1f);
			
			if(weight101 > 0.0f)
				color101.mult(1.0f / weight101);
			else
				color101.set(1f, 1f, 1f);
			
			if(weight110 > 0.0f)
				color110.mult(1.0f / weight110);
			else
				color110.set(1f, 1f, 1f);
			
			if(weight111 > 0.0f)
				color111.mult(1.0f / weight111);
			else
				color111.set(1f, 1f, 1f);
		}
		
		public float getTotalWeight() {
			return weight000 + weight001 + weight010 + weight011 +
					weight100 + weight101 + weight110 + weight111;
		}
		
		public void set(int index, float r, float g, float b) {
			switch(index) {
			case 0:
				color000.set(r, g, b);
				weight000 = 1f;
				break;
			case 1:
				color100.set(r, g, b);
				weight100 = 1f;
				break;
			case 2:
				color001.set(r, g, b);
				weight001 = 1f;
				break;
			case 3:
				color101.set(r, g, b);
				weight101 = 1f;
				break;
			case 4:
				color010.set(r, g, b);
				weight010 = 1f;
				break;
			case 5:
				color110.set(r, g, b);
				weight110 = 1f;
				break;
			case 6:
				color011.set(r, g, b);
				weight011 = 1f;
				break;
			case 7:
				color111.set(r, g, b);
				weight111 = 1f;
				break;
			}
		}
		
		public Color get(int index) {
			switch(index) {
			case 0:
				if(weight000 == 0f)
					return null;
				return color000;
			case 1:
				if(weight100 == 0f)
					return null;
				return color100;
			case 2:
				if(weight001 == 0f)
					return null;
				return color001;
			case 3:
				if(weight101 == 0f)
					return null;
				return color101;
			case 4:
				if(weight010 == 0f)
					return null;
				return color010;
			case 5:
				if(weight110 == 0f)
					return null;
				return color010;
			case 6:
				if(weight011 == 0f)
					return null;
				return color011;
			case 7:
				if(weight111 == 0f)
					return null;
				return color111;
			}
			return null;
		}
		
		public void addWeighted(Color color, float weight0, float weight1, float weight2, float weight3, 
										float weight4, float weight5, float weight6, float weight7) {
			color000.addWeighted(color, weight0);
			weight000 += weight0;
			color100.addWeighted(color, weight1);
			weight100 += weight1;
			color001.addWeighted(color, weight2);
			weight001 += weight2;
			color101.addWeighted(color, weight3);
			weight101 += weight3;
			color010.addWeighted(color, weight4);
			weight010 += weight4;
			color110.addWeighted(color, weight5);
			weight110 += weight5;
			color011.addWeighted(color, weight6);
			weight011 += weight6;
			color111.addWeighted(color, weight7);
			weight111 += weight7;
		}
		
		public void write(LargeDataOutputStream dos) throws IOException{
			dos.writeFloat(color000.getR());
			dos.writeFloat(color000.getG());
			dos.writeFloat(color000.getB());
			dos.writeFloat(color000.getA());
			dos.writeFloat(weight000);
			
			dos.writeFloat(color001.getR());
			dos.writeFloat(color001.getG());
			dos.writeFloat(color001.getB());
			dos.writeFloat(color001.getA());
			dos.writeFloat(weight001);
			
			dos.writeFloat(color010.getR());
			dos.writeFloat(color010.getG());
			dos.writeFloat(color010.getB());
			dos.writeFloat(color010.getA());
			dos.writeFloat(weight010);
			
			dos.writeFloat(color011.getR());
			dos.writeFloat(color011.getG());
			dos.writeFloat(color011.getB());
			dos.writeFloat(color011.getA());
			dos.writeFloat(weight011);
			
			dos.writeFloat(color100.getR());
			dos.writeFloat(color100.getG());
			dos.writeFloat(color100.getB());
			dos.writeFloat(color100.getA());
			dos.writeFloat(weight100);
			
			dos.writeFloat(color101.getR());
			dos.writeFloat(color101.getG());
			dos.writeFloat(color101.getB());
			dos.writeFloat(color101.getA());
			dos.writeFloat(weight101);
			
			dos.writeFloat(color110.getR());
			dos.writeFloat(color110.getG());
			dos.writeFloat(color110.getB());
			dos.writeFloat(color110.getA());
			dos.writeFloat(weight110);
			
			dos.writeFloat(color111.getR());
			dos.writeFloat(color111.getG());
			dos.writeFloat(color111.getB());
			dos.writeFloat(color111.getA());
			dos.writeFloat(weight111);
		}
		
		public void read(LargeDataInputStream dis) throws IOException{
			color000.setR(dis.readFloat());
			color000.setG(dis.readFloat());
			color000.setB(dis.readFloat());
			color000.setA(dis.readFloat());
			weight000 = dis.readFloat();
			
			color001.setR(dis.readFloat());
			color001.setG(dis.readFloat());
			color001.setB(dis.readFloat());
			color001.setA(dis.readFloat());
			weight001 = dis.readFloat();
			
			color010.setR(dis.readFloat());
			color010.setG(dis.readFloat());
			color010.setB(dis.readFloat());
			color010.setA(dis.readFloat());
			weight010 = dis.readFloat();
			
			color011.setR(dis.readFloat());
			color011.setG(dis.readFloat());
			color011.setB(dis.readFloat());
			color011.setA(dis.readFloat());
			weight011 = dis.readFloat();
			
			color100.setR(dis.readFloat());
			color100.setG(dis.readFloat());
			color100.setB(dis.readFloat());
			color100.setA(dis.readFloat());
			weight100 = dis.readFloat();
			
			color101.setR(dis.readFloat());
			color101.setG(dis.readFloat());
			color101.setB(dis.readFloat());
			color101.setA(dis.readFloat());
			weight101 = dis.readFloat();
			
			color110.setR(dis.readFloat());
			color110.setG(dis.readFloat());
			color110.setB(dis.readFloat());
			color110.setA(dis.readFloat());
			weight110 = dis.readFloat();
			
			color111.setR(dis.readFloat());
			color111.setG(dis.readFloat());
			color111.setB(dis.readFloat());
			color111.setA(dis.readFloat());
			weight111 = dis.readFloat();
		}
	}
	
	private boolean empty;
	private TokenMap<WeightedColor> colors;
	
	public BlendedBiome() {
		this.empty = true;
		this.colors = new TokenMap<WeightedColor>();
	}
	
	public BlendedBiome(BlendedBiome other) {
		this.empty = other.empty;
		this.colors = new TokenMap<WeightedColor>();
		for(int i = 0; i < other.colors.size(); ++i) {
			this.colors.put(other.colors.getKey(i), new WeightedColor(other.colors.getValue(i)));
		}
	}
	
	public boolean isEmpty() {
		return empty;
	}
	
	public void clear() {
		if(this.empty)
			return;
		this.empty = true;
		for(int i = 0; i < colors.size(); ++i) {
			colors.getValue(i).clear();
		}
	}
	
	public void normalise() {
		for(int i = 0; i < colors.size(); ++i) {
			colors.getValue(i).normalise();
		}
	}
	
	public WeightedColor getColor(String name) {
		WeightedColor color = colors.getOrDefault(name, null);
		if(color == null)
			return null;
		if(color.getTotalWeight() == 0.0f)
			return null;
		return color;
	}
	
	public void setColor(String name, int index, float r, float g, float b) {
		this.empty = false;
		WeightedColor color = colors.getOrDefault(name, null);
		if(color == null) {
			color = new WeightedColor();
			colors.put(name, color);
		}
		color.set(index, r, g, b);
	}
	
	public void addBiome(Biome biome, float weight0, float weight1, float weight2, float weight3, 
										float weight4, float weight5, float weight6, float weight7) {
		this.empty = false;
		for(int i = 0; i < biome.getColors().size(); ++i) {
			if(biome.getColors().getValue(i) == null)
				continue;
			WeightedColor color = colors.getOrDefault(biome.getColors().getKeyId(i), null);
			if(color == null) {
				color = new WeightedColor();
				colors.put(biome.getColors().getKeyId(i), color);
			}
			color.addWeighted(biome.getColors().getValue(i), weight0, weight1, weight2, weight3, weight4, weight5, weight6, weight7);
		}
	}
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeBoolean(empty);
		dos.writeInt(colors.size());
		for(Entry<String, WeightedColor> entry : colors) {
			dos.writeUTF(entry.getKey());
			entry.getValue().write(dos);
		}
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		empty = dis.readBoolean();
		int numColors = dis.readInt();
		colors.clear();
		for(int i = 0; i < numColors; ++i) {
			String name = dis.readUTF();
			WeightedColor weightedColor = new WeightedColor();
			weightedColor.read(dis);
			colors.put(name, weightedColor);
		}
	}
	
}
