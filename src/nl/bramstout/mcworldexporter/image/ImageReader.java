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
