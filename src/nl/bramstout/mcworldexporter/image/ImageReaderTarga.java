package nl.bramstout.mcworldexporter.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import nl.bramstout.mcworldexporter.world.bedrock.ByteArrayDataInputStream;

public class ImageReaderTarga extends ImageReader{

	@Override
	public BufferedImage read(File file) {
		BufferedImage img = null;
		try {
			byte[] data = Files.readAllBytes(file.toPath());
			ByteArrayDataInputStream dis = new ByteArrayDataInputStream(data);
			
			byte[] footer = new byte[16];
			dis.seek(data.length - 18);
			dis.readFully(footer);
			byte[] footerSignature = "TRUEVISION-XFILE".getBytes();
			@SuppressWarnings("unused")
			boolean isExtended = Arrays.equals(footer, footerSignature);
			
			dis.seek(0);
			
			int idLength = dis.readUnsignedByte();
			int colorMapType = dis.readUnsignedByte();
			int imageType = dis.readUnsignedByte();
			if(imageType != 1 && imageType != 9)
				colorMapType = 0;
			
			int colorMapFirstEntryIndex = dis.readUnsignedShort();
			int colorMapLength = dis.readUnsignedShort();
			int colorMapEntrySize = dis.readUnsignedByte();
			int colorMapEntrySizeInBytes = (colorMapEntrySize + 7) / 8;
			if(colorMapType == 0) {
				colorMapLength = 0;
				colorMapEntrySize = 0;
				colorMapEntrySizeInBytes = 0;
			}
			
			@SuppressWarnings("unused")
			int xOrigin = dis.readShort();
			@SuppressWarnings("unused")
			int yOrigin = dis.readShort();
			
			int width = dis.readUnsignedShort();
			int height = dis.readUnsignedShort();
			int pixelDepth = dis.readUnsignedByte();
			int pixelDepthInBytes = (pixelDepth + 7) / 8;
			
			int imgDescriptor = dis.readUnsignedByte();
			@SuppressWarnings("unused")
			int numAlphaBits = imgDescriptor & 0b1111;
			boolean rightToLeft = (imgDescriptor & 0b01000) == 0;
			boolean topToBottom = (imgDescriptor & 0b10000) != 0;
			
			byte[] imageId = new byte[idLength];
			dis.readFully(imageId);
			
			byte[] colorMapData = new byte[colorMapLength * colorMapEntrySizeInBytes];
			dis.readFully(colorMapData);
			int[] colorMap = new int[colorMapLength];
			for(int i = 0; i < colorMap.length; ++i) {
				int byteIndex = i * colorMapEntrySizeInBytes;
				colorMap[i] = readColor(colorMapData, byteIndex, colorMapEntrySize);
			}
			
			
			byte[] pixelData = new byte[width * height * pixelDepthInBytes];
			dis.readFully(pixelData);
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			if(imageType == 1) {
				int i = 0;
				for(int y = 0; y < height; ++y) {
					for(int x = 0; x < width; ++x) {
						int index = -1;
						if(pixelDepth == 8) {
							index = Byte.toUnsignedInt(pixelData[i]) + colorMapFirstEntryIndex; 
						}else if(pixelDepth == 16) {
							index = Byte.toUnsignedInt(pixelData[i*2]) | 
									(Byte.toUnsignedInt(pixelData[i*2+1])) << 8 + 
									colorMapFirstEntryIndex;
						}
						i++;
						
						if(index < 0 || index >= colorMap.length)
							continue;
						
						img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
									colorMap[index]);
					}
				}
			} else if(imageType == 2) {
				int i = 0;
				for(int y = 0; y < height; ++y) {
					for(int x = 0; x < width; ++x) {
						int index = i * pixelDepthInBytes;
						i++;
						
						int color = readColor(pixelData, index, pixelDepth);
						
						img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
									color);
					}
				}
			}else if(imageType == 3) {
				int i = 0;
				for(int y = 0; y < height; ++y) {
					for(int x = 0; x < width; ++x) {
						int index = i;
						i++;
						
						int gray = Byte.toUnsignedInt(pixelData[index]) << (8 - pixelDepth);
						int color = gray | (gray << 8) | (gray << 16) | 0xFF000000;
						
						img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
									color);
					}
				}
			} else if(imageType == 9) {
				int pos = 0;
				for(int i = 0; i < width*height;) {
					int header = Byte.toUnsignedInt(pixelData[pos]);
					pos++;
					boolean isRLE = (header & 0b10000000) != 0;
					int numValues = (header & 0b01111111) + 1;
					if(isRLE) {
						int index = -1;
						if(pixelDepth == 8) {
							index = Byte.toUnsignedInt(pixelData[pos]) + colorMapFirstEntryIndex;
							pos++;
						}else if(pixelDepth == 16) {
							index = Byte.toUnsignedInt(pixelData[pos]) | 
									(Byte.toUnsignedInt(pixelData[pos+1])) << 8 + 
									colorMapFirstEntryIndex;
							pos += 2;
						}
						
						if(index < 0 || index >= colorMap.length)
							continue;
						
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
									colorMap[index]);
						}
					}else {
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							
							int index = -1;
							if(pixelDepth == 8) {
								index = Byte.toUnsignedInt(pixelData[pos]) + colorMapFirstEntryIndex;
								pos++;
							}else if(pixelDepth == 16) {
								index = Byte.toUnsignedInt(pixelData[pos]) | 
										(Byte.toUnsignedInt(pixelData[pos+1])) << 8 + 
										colorMapFirstEntryIndex;
								pos += 2;
							}
							
							if(index < 0 || index >= colorMap.length)
								continue;
							
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
									colorMap[index]);
						}
					}
					i += numValues;
				}
			} else if(imageType == 10) {
				int pos = 0;
				for(int i = 0; i < width*height;) {
					int header = Byte.toUnsignedInt(pixelData[pos]);
					pos++;
					boolean isRLE = (header & 0b10000000) != 0;
					int numValues = (header & 0b01111111) + 1;
					if(isRLE) {
						int color = readColor(pixelData, pos, pixelDepth);
						pos += pixelDepthInBytes;
						
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
											color);
						}
					}else {
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							
							int color = readColor(pixelData, pos, pixelDepth);
							pos += pixelDepthInBytes;
							
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
											color);
						}
					}
					i += numValues;
				}
			} else if(imageType == 11) {
				int pos = 0;
				for(int i = 0; i < width*height;) {
					int header = Byte.toUnsignedInt(pixelData[pos]);
					pos++;
					boolean isRLE = (header & 0b10000000) != 0;
					int numValues = (header & 0b01111111) + 1;
					if(isRLE) {
						int gray = Byte.toUnsignedInt(pixelData[pos]) << (8 - pixelDepth);
						int color = gray | (gray << 8) | (gray << 16) | 0xFF000000;
						pos += pixelDepthInBytes;
						
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
											color);
						}
					}else {
						for(int j = 0; j < numValues; ++j) {
							int pixelIndex = i + j;
							int x = pixelIndex % width;
							int y = pixelIndex / width;
							
							int gray = Byte.toUnsignedInt(pixelData[pos]) << (8 - pixelDepth);
							int color = gray | (gray << 8) | (gray << 16) | 0xFF000000;
							pos += pixelDepthInBytes;
							
							img.setRGB(rightToLeft ? (width - x - 1) : x, 
									topToBottom ? y : (height - y - 1), 
											color);
						}
					}
					i += numValues;
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return img;
	}
	
	private int readColor(byte[] data, int index, int bitDepth) {
		int colorBits = Math.min(bitDepth / 3, 8);
		int attributeBits = bitDepth - (colorBits * 3);
		
		int numBytes = (bitDepth + 7) / 8;
		int intVal = 0;
		for(int i = 0; i < numBytes; ++i) {
			intVal |= Byte.toUnsignedInt(data[index+i]) << (i * 8);
		}
		
		int colorBitMask = (1 << colorBits) - 1;
		int colorShift = 8 - colorBits; // Convert the bit depth to 8 bits
		int attributeBitMask = (1 << attributeBits) - 1;
		int attributeShift = 8 - attributeBits;
		
		int color = 0;
		color |= ((intVal & colorBitMask) << colorShift); // red
		color |= (((intVal >>> colorBits) & colorBitMask) << colorShift) << 8; // green
		color |= (((intVal >>> (colorBits * 2)) & colorBitMask) << colorShift) << 16; // blue
		color |= (((intVal >>> (colorBits * 3)) & attributeBitMask) << attributeShift) << 24; // attribute
		
		return color;
	}

	@Override
	public boolean supportsImage(File file) {
		return file.getName().toLowerCase().endsWith(".tga");
	}

}
