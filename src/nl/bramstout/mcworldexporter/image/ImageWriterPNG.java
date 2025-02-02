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

package nl.bramstout.mcworldexporter.image;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class ImageWriterPNG extends ImageWriter{

	@Override
	public void write(File file, PbrImage img) {
		BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		RGBA rgba = new RGBA();
		for(int j = 0; j < img.getHeight(); ++j) {
			for(int i = 0; i < img.getWidth(); ++i) {
				img.sample(i, j, Boundary.EMPTY, rgba);
				int rI = (int) Math.max(Math.min(rgba.r * 255.0f + 0.5f, 255f), 0f);
				int gI = (int) Math.max(Math.min(rgba.g * 255.0f + 0.5f, 255f), 0f);
				int bI = (int) Math.max(Math.min(rgba.b * 255.0f + 0.5f, 255f), 0f);
				int aI = (int) Math.max(Math.min(rgba.a * 255.0f + 0.5f, 255f), 0f);
				int rgb = aI << 24 | rI << 16 | gI << 8 | bI;
				img2.setRGB(i, j, rgb);
			}
		}
		try {
			ImageIO.write(img2, "PNG", file);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public boolean supportsImage(File file) {
		return file.getName().toLowerCase().endsWith(".png");
	}

}
