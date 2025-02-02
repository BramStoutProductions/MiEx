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

package nl.bramstout.mcworldexporter.pbr;

import java.awt.image.BufferedImage;

public class PbrImageRaster extends PbrImage{

	private int width;
	private int height;
	private float[] data;
	
	public PbrImageRaster(PbrImage other) {
		this(other.getWidth(), other.getHeight());
		RGBA rgba = new RGBA();
		int index = 0;
		for(int j = 0; j < height; ++j) {
			for(int i = 0; i < width; ++i) {
				other.sample(i, j, Boundary.EMPTY, rgba);
				index = (j * width + i) * 4;
				data[index] = rgba.r;
				data[index + 1] = rgba.g;
				data[index + 2] = rgba.b;
				data[index + 3] = rgba.a;
			}
		}
	}
	
	public PbrImageRaster(int width, int height) {
		this.width = width;
		this.height = height;
		this.data = new float[width * height * 4];
	}
	
	public PbrImageRaster(BufferedImage img, boolean linearise) {
		this(img.getWidth(), img.getHeight());
		int rgba = 0;
		int r = 0;
		int g = 0;
		int b = 0;
		int a = 0;
		float rf = 0f;
		float gf = 0f;
		float bf = 0f;
		float af = 0f;
		int index = 0;
		boolean alpha = img.getColorModel().hasAlpha();
		for(int j = 0; j < height; ++j) {
			for(int i = 0; i < width; ++i) {
				rgba = img.getRGB(i, j);
				a = alpha ? (rgba >> 24) & 0xFF : 255;
				r = (rgba >> 16) & 0xFF;
				g = (rgba >> 8) & 0xFF;
				b = rgba & 0xFF;
				af = ((float) a) / 255.0f;
				rf = ((float) r) / 255.0f;
				gf = ((float) g) / 255.0f;
				bf = ((float) b) / 255.0f;
				if(linearise) {
					rf = (float) Math.pow(rf, 2.2f);
					gf = (float) Math.pow(gf, 2.2f);
					bf = (float) Math.pow(bf, 2.2f);
				}
				index = (j * width + i) * 4;
				data[index] = rf;
				data[index + 1] = gf;
				data[index + 2] = bf;
				data[index + 3] = af;
			}
		}
	}
	
	public float[] getData() {
		return data;
	}
	
	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void sample(int x, int y, Boundary boundaryMode, RGBA out) {
		if(width <= 0 || height <= 0) {
			out.r = 0f;
			out.g = 0f;
			out.b = 0f;
			out.a = 0f;
			return;
		}
		switch(boundaryMode) {
		case EMPTY:
			if(x < 0 || y < 0 || x >= width || y >= height) {
				out.r = 0f;
				out.g = 0f;
				out.b = 0f;
				out.a = 0f;
				return;
			}
		case CLIP:
			if(x >= width)
				x = width - 1;
			if(x < 0)
				x = 0;
			if(y >= height)
				y = height - 1;
			if(y < 0)
				y = 0;
		case REPEAT:
			x = x % width;
			if(x < 0)
				x += width;
			y = y % height;
			if(y < 0)
				y += height;
		}
		int index = (y * width + x) * 4;
		out.r = data[index];
		out.g = data[index + 1];
		out.b = data[index + 2];
		out.a = data[index + 3];
	}

	@Override
	public void write(int x, int y, Boundary boundaryMode, RGBA value) {
		if(width <= 0 || height <= 0)
			return;
		switch(boundaryMode) {
		case EMPTY:
			if(x < 0 || y < 0 || x >= width || y >= height)
				return;
		case CLIP:
			if(x >= width)
				x = width - 1;
			if(x < 0)
				x = 0;
			if(y >= height)
				y = height - 1;
			if(y < 0)
				y = 0;
		case REPEAT:
			x = x % width;
			if(x < 0)
				x += width;
			y = y % height;
			if(y < 0)
				y += height;
		}
		int index = (y * width + x) * 4;
		data[index] = value.r;
		data[index + 1] = value.g;
		data[index + 2] = value.b;
		data[index + 3] = value.a;
	}

	@Override
	public PbrImage copy() {
		return new PbrImageRaster(this);
	}

}
