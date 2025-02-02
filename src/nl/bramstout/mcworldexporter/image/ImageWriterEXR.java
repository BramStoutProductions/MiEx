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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import nl.bramstout.mcworldexporter.image.EXRWriter.Channel;
import nl.bramstout.mcworldexporter.image.EXRWriter.DataType;
import nl.bramstout.mcworldexporter.image.EXRWriter.LineOrder;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class ImageWriterEXR extends ImageWriter{

	@Override
	public void write(File file, PbrImage img) {
		try {
			file.createNewFile();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			EXRWriter writer = new EXRWriter(bos);
			// Header
			writer.writeChannelsAttribute(new Channel[] { 
					new Channel("A", DataType.HALF),
					new Channel("B", DataType.HALF),
					new Channel("G", DataType.HALF),
					new Channel("R", DataType.HALF)});
			writer.writeAttributeStart("compression", "compression", 1);
			writer.writeCompression(0);
			writer.writeAttributeStart("dataWindow", "box2i", 4*4);
			writer.writeBox2i(0, 0, img.getWidth()-1, img.getHeight()-1);
			writer.writeAttributeStart("displayWindow", "box2i", 4*4);
			writer.writeBox2i(0, 0, img.getWidth()-1, img.getHeight()-1);
			writer.writeAttributeStart("lineOrder", "lineOrder", 1);
			writer.writeLineOrder(LineOrder.INCREASING_Y);
			writer.writeAttributeStart("pixelAspectRatio", "float", 4);
			writer.writeFloat(1f);
			writer.writeAttributeStart("screenWindowCenter", "v2f", 8);
			writer.writeFloat(0f);
			writer.writeFloat(0f);
			writer.writeAttributeStart("screenWindowWidth", "float", 4);
			writer.writeFloat(1f);
			writer.writeChar((char) 0);
			
			// Scan line offset table
			int scanlineSize = img.getWidth() * 2 * 4 + 4 + 4;
			int numScanlines = img.getHeight();
			int scanlineOffsetTableSize = numScanlines * 8;
			long currentPosition = writer.getBytesWritten();
			currentPosition += scanlineOffsetTableSize;
			for(int i = 0; i < numScanlines; ++i) {
				writer.writeLong(currentPosition);
				currentPosition += scanlineSize;
			}
			
			// Data
			RGBA rgba = new RGBA();
			for(int j = 0; j < numScanlines; ++j) {
				writer.writeInt(j);
				writer.writeInt(img.getWidth() * 2 * 4);
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					writer.writeHalf(rgba.a);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					writer.writeHalf(rgba.b);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					writer.writeHalf(rgba.g);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					writer.writeHalf(rgba.r);
				}
			}
			
			bos.close();
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public boolean supportsImage(File file) {
		return file.getName().toLowerCase().endsWith(".exr");
	}

}
