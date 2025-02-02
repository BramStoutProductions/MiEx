package nl.bramstout.mcworldexporter.image;

import java.io.File;

import nl.bramstout.mcworldexporter.pbr.PbrImage;

public abstract class ImageWriter {
	
	public abstract void write(File file, PbrImage img);
	
	public abstract boolean supportsImage(File file);
	
	private static ImageWriter[] writers = new ImageWriter[] {
			new ImageWriterPNG(),
			new ImageWriterEXR()
	};
	
	/**
	 * Reads in the image pointed to by file.
	 * If it can't read the image, it returns null.
	 * @param file The image file to read.
	 * @return A BufferedImage with the image data or null.
	 */
	public static void writeImage(File file, PbrImage img) {
		for(ImageWriter writer : writers) {
			if(writer.supportsImage(file)) {
				writer.write(file, img);
				return;
			}
		}
	}
	
}
