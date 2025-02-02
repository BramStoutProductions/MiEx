package nl.bramstout.mcworldexporter.image;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.image.EXRReader.Channel;
import nl.bramstout.mcworldexporter.image.EXRReader.DataType;
import nl.bramstout.mcworldexporter.image.EXRReader.LineOrder;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;

public class ImageReaderEXR extends ImageReader{

	@Override
	public BufferedImage read(File file) {
		PbrImage img = readPbr(file, false);
		if(img == null)
			return null;
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
		return img2;
	}
	
	@Override
	public PbrImage readPbr(File file, boolean linearise) {
		PbrImage img = null;
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			EXRReader reader = new EXRReader(bis);
			
			// Header attributes
			List<Channel> channels = new ArrayList<Channel>();
			int compression = 0;
			int[] dataWindow = null;
			int[] displayWindow = null;
			LineOrder lineOrder = LineOrder.DECREASING_Y;
			
			while(true) {
				String attributeName = reader.readAttributeName();
				if(attributeName.isEmpty())
					break;
				@SuppressWarnings("unused")
				String attributeType = reader.readAttributeType();
				int attributeSize = reader.readAttributeSize();
				
				if(attributeName.equals("channels")) {
					channels = reader.readChannelsAttribute();
				}else if(attributeName.equals("compression")) {
					compression = reader.readCompression();
				}else if(attributeName.equals("dataWindow")) {
					dataWindow = reader.readBox2i();
				}else if(attributeName.equals("displayWindow")) {
					displayWindow = reader.readBox2i();
				}else if(attributeName.equals("lineOrder")) {
					lineOrder = reader.readLineOrder();
				}else {
					for(int i = 0; i < attributeSize; ++i)
						reader.readChar();
				}
			}
			if(compression != 0 || dataWindow == null || displayWindow == null || 
					lineOrder != LineOrder.INCREASING_Y || channels.size() != 4)
				throw new RuntimeException("Unsupported EXR");
			if(!channels.get(0).name.equals("A") || !channels.get(1).name.equals("B") || 
					!channels.get(2).name.equals("G") || !channels.get(3).name.equals("R") ||
					channels.get(0).type != DataType.HALF || channels.get(1).type != DataType.HALF ||
					channels.get(2).type != DataType.HALF || channels.get(3).type != DataType.HALF)
				throw new RuntimeException("Unsupported EXR");
			if(dataWindow[0] != displayWindow[0] || dataWindow[1] != displayWindow[1] ||
					dataWindow[2] != displayWindow[2] || dataWindow[3] != displayWindow[3])
				throw new RuntimeException("Unsupported EXR");
			
			int imgWidth = dataWindow[2] - dataWindow[0] + 1;
			int imgHeight = dataWindow[3] - dataWindow[1] + 1;
			img = new PbrImageRaster(imgWidth, imgHeight);
			
			int numScanlines = img.getHeight();
			for(int i = 0; i < numScanlines; ++i) {
				reader.readLong();
			}
			
			RGBA rgba = new RGBA();
			for(int j = 0; j < numScanlines; ++j) {
				@SuppressWarnings("unused")
				int y = reader.readInt();
				int scanlineSize = reader.readInt();
				if(scanlineSize != (img.getWidth() * 2 * 4))
					throw new RuntimeException("Unsupported EXR");
				
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					rgba.a = reader.readHalf();
					img.write(i, j, Boundary.EMPTY, rgba);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					rgba.b = reader.readHalf();
					img.write(i, j, Boundary.EMPTY, rgba);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					rgba.g = reader.readHalf();
					img.write(i, j, Boundary.EMPTY, rgba);
				}
				for(int i = 0; i < img.getWidth(); ++i) {
					img.sample(i, j, Boundary.EMPTY, rgba);
					rgba.r = reader.readHalf();
					img.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(bis != null) {
			try {
				bis.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return img;
	}

	@Override
	public boolean supportsImage(File file) {
		return file.getName().toLowerCase().endsWith(".exr");
	}

}
