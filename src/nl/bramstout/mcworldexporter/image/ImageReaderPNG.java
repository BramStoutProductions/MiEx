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

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageReaderPNG extends ImageReader{

	@Override
	public BufferedImage read(File file) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(file);
			if(img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
				// A grayscale image is set as linear, while all other images
				// are set as sRGB, but it needs to be interpreted as-is and not get
				// converted to sRGB. So, we convert it to a normal RGB image.
				BufferedImage img2 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
				if(img.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE) {
					byte[] pixelData = new byte[4];
					for(int j = 0; j < img2.getHeight(); ++j) {
						for(int i = 0; i < img2.getWidth(); ++i) {
							img.getRaster().getDataElements(i, j, pixelData);
							int val = ((int) pixelData[0]) & 0xFF;
							int alpha = 0xFF;
							if(img.getColorModel().hasAlpha()) {
								alpha = ((int) pixelData[1]) & 0xFF;
							}
							int rgb = alpha << 24 | val << 16 | val << 8 | val;
							img2.setRGB(i, j, rgb);
						}
					}
				}else if(img.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_SHORT) {
					short[] pixelData = new short[4];
					for(int j = 0; j < img2.getHeight(); ++j) {
						for(int i = 0; i < img2.getWidth(); ++i) {
							img.getRaster().getDataElements(i, j, pixelData);
							int val = (((int) pixelData[0]) >> 8) & 0xFF;
							int alpha = 0xFF;
							if(img.getColorModel().hasAlpha()) {
								alpha = ((int) pixelData[1]) & 0xFF;
							}
							int rgb = alpha << 24 | val << 16 | val << 8 | val;
							img2.setRGB(i, j, rgb);
						}
					}
				}
				img = img2;
			}
		}catch(Exception ex) {
			System.out.println(file.getPath());
			ex.printStackTrace();
		}
		return img;
	}

	@Override
	public boolean supportsImage(File file) {
		return file.getName().toLowerCase().endsWith(".png");
	}

}
