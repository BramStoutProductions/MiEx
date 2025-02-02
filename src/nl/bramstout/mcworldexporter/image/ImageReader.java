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

import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;

public abstract class ImageReader {
	
	public abstract BufferedImage read(File file);
	
	public abstract boolean supportsImage(File file);
	
	public PbrImage readPbr(File file, boolean linearise) {
		BufferedImage img = read(file);
		if(img == null)
			return null;
		return new PbrImageRaster(img, linearise);
	}
	
	private static ImageReader[] readers = new ImageReader[] {
			new ImageReaderPNG(),
			new ImageReaderTarga(),
			new ImageReaderJPG(),
			new ImageReaderEXR()
	};
	
	/**
	 * Reads in the image pointed to by file.
	 * If it can't read the image, it returns null.
	 * @param file The image file to read.
	 * @return A BufferedImage with the image data or null.
	 */
	public static BufferedImage readImage(File file) {
		for(ImageReader reader : readers)
			if(reader.supportsImage(file))
				return reader.read(file);
		return null;
	}
	
	/**
	 * Reads in the image pointed to by file.
	 * If it can't read the image, it returns null.
	 * @param file The image file to read.
	 * @return A BufferedImage with the image data or null.
	 */
	public static PbrImage readPbrImage(File file, boolean linearise) {
		for(ImageReader reader : readers)
			if(reader.supportsImage(file))
				return reader.readPbr(file, linearise);
		return null;
	}
	
}
