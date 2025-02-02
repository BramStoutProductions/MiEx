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
