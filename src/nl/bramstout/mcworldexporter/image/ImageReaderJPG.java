package nl.bramstout.mcworldexporter.image;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageReaderJPG extends ImageReader{

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
		return file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".jpeg");
	}

}
