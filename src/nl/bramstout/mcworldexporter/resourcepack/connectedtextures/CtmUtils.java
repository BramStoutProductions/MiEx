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

package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.iq80.leveldb.shaded.guava.io.Files;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.java.MCMetaJavaEdition;

public class CtmUtils {
	
	/*
	 * Tile connection data
	 * index : 00000000 (top left, top, top right, right, bottom right, bottom, bottom left, left)
	 * 0:  00000000
	 * 1:  00010000
	 * 2:  00010001
	 * 3:  00000001
	 * 4:  00010100
	 * 5:  00000101
	 * 6:  01010100
	 * 7:  00010101
	 * 8:  01110101
	 * 9:  01011101
	 * 10: 11010111
	 * 11: 11110101
	 * 12: 00000100
	 * 13: 00011100
	 * 14: 00011111
	 * 15: 00000111
	 * 16: 01010000
	 * 17: 01000001
	 * 18: 01010001
	 * 19: 01000101
	 * 20: 11010101
	 * 21: 01010111
	 * 22: 01011111
	 * 23: 01111101
	 * 24: 01000100
	 * 25: 01111100
	 * 26: 11111111
	 * 27: 11000111
	 * 28: 01011100
	 * 29: 00010111
	 * 30: 01110100
	 * 31: 00011101
	 * 32: 11110111
	 * 33: 11111101
	 * 34: 01110111
	 * 35: 11011101
	 * 36: 01000000
	 * 37: 01110000
	 * 38: 11110001
	 * 39: 11000001
	 * 40: 01110001
	 * 41: 11000101
	 * 42: 11010001
	 * 43: 01000111
	 * 44: 11011111
	 * 45: 01111111
	 * 46: 01010101
	 */
	
	public static int[] TileToConnectionData = new int[] {
			0b00000000, 0b00010000, 0b00010001, 0b00000001, 0b00010100, 0b00000101,
			0b01010100, 0b00010101, 0b01110101, 0b01011101, 0b11010111, 0b11110101,
			0b00000100, 0b00011100, 0b00011111, 0b00000111, 0b01010000, 0b01000001,
			0b01010001, 0b01000101, 0b11010101, 0b01010111, 0b01011111, 0b01111101,
			0b01000100, 0b01111100, 0b11111111, 0b11000111, 0b01011100, 0b00010111,
			0b01110100, 0b00011101, 0b11110111, 0b11111101, 0b01110111, 0b11011101,
			0b01000000, 0b01110000, 0b11110001, 0b11000001, 0b01110001, 0b11000101,
			0b11010001, 0b01000111, 0b11011111, 0b01111111, 0b01010101
	};
	public static int[] ConnectionDataToTile = new int[256];
	static {
		Arrays.fill(ConnectionDataToTile, -1);
		ConnectionDataToTile[0b00000000] = 0;
		ConnectionDataToTile[0b00010000] = 1;
		ConnectionDataToTile[0b00010001] = 2;
		ConnectionDataToTile[0b00000001] = 3;
		ConnectionDataToTile[0b00010100] = 4;
		ConnectionDataToTile[0b00000101] = 5;
		ConnectionDataToTile[0b01010100] = 6;
		ConnectionDataToTile[0b00010101] = 7;
		ConnectionDataToTile[0b01110101] = 8;
		ConnectionDataToTile[0b01011101] = 9;
		ConnectionDataToTile[0b11010111] = 10;
		ConnectionDataToTile[0b11110101] = 11;
		ConnectionDataToTile[0b00000100] = 12;
		ConnectionDataToTile[0b00011100] = 13;
		ConnectionDataToTile[0b00011111] = 14;
		ConnectionDataToTile[0b00000111] = 15;
		ConnectionDataToTile[0b01010000] = 16;
		ConnectionDataToTile[0b01000001] = 17;
		ConnectionDataToTile[0b01010001] = 18;
		ConnectionDataToTile[0b01000101] = 19;
		ConnectionDataToTile[0b11010101] = 20;
		ConnectionDataToTile[0b01010111] = 21;
		ConnectionDataToTile[0b01011111] = 22;
		ConnectionDataToTile[0b01111101] = 23;
		ConnectionDataToTile[0b01000100] = 24;
		ConnectionDataToTile[0b01111100] = 25;
		ConnectionDataToTile[0b11111111] = 26;
		ConnectionDataToTile[0b11000111] = 27;
		ConnectionDataToTile[0b01011100] = 28;
		ConnectionDataToTile[0b00010111] = 29;
		ConnectionDataToTile[0b01110100] = 30;
		ConnectionDataToTile[0b00011101] = 31;
		ConnectionDataToTile[0b11110111] = 32;
		ConnectionDataToTile[0b11111101] = 33;
		ConnectionDataToTile[0b01110111] = 34;
		ConnectionDataToTile[0b11011101] = 35;
		ConnectionDataToTile[0b01000000] = 36;
		ConnectionDataToTile[0b01110000] = 37;
		ConnectionDataToTile[0b11110001] = 38;
		ConnectionDataToTile[0b11000001] = 39;
		ConnectionDataToTile[0b01110001] = 40;
		ConnectionDataToTile[0b11000101] = 41;
		ConnectionDataToTile[0b11010001] = 42;
		ConnectionDataToTile[0b01000111] = 43;
		ConnectionDataToTile[0b11011111] = 44;
		ConnectionDataToTile[0b01111111] = 45;
		ConnectionDataToTile[0b01010101] = 46;
		
		for(int i = 0; i < ConnectionDataToTile.length; ++i) {
			if(ConnectionDataToTile[i] >= 0)
				continue;
			// We don't have connection info for this,
			// so let's fill in missing data.
			// If the corners are connected but not the sides
			// then we want to use the texture as if the corners also
			// aren't connected.
			
			int tileIndex = i;
			// Loop over each corner
			for(int cornerBit = 1; cornerBit < 8; cornerBit += 2) {
				// Check the side bits on both sides
				int leftSideBit = cornerBit - 1;
				if(leftSideBit < 0)
					leftSideBit += 8;
				int rightSideBit = cornerBit + 1;
				if(rightSideBit >= 8)
					rightSideBit -= 8;
				
				int leftSide = tileIndex & (1 << leftSideBit);
				int rightSide = tileIndex & (1 << rightSideBit);
				
				// If either sides are not connected, then set this corner
				// to be not connected as well.
				if(leftSide == 0 || rightSide == 0)
					tileIndex &= ~(1 << cornerBit);
			}
			
			// Copy the file to use from the tileIndex
			ConnectionDataToTile[i] = ConnectionDataToTile[tileIndex];
		}
		
		for(int i = 0; i < ConnectionDataToTile.length; ++i)
			if(ConnectionDataToTile[i] < 0)
				ConnectionDataToTile[i] = 0;
	}
	
	/*
	 * Compact connection data
	 * 0: 00000000
	 * 1: 11111111
	 * 2: 01000100
	 * 3: 00010001
	 * 4: 01010101
	 */
	public static int[] CompactTileToConnectionData = new int[] {
		0b00000000,
		0b11111111,
		0b01000100,
		0b00010001,
		0b01010101	
	};
	
	private static int[] CompactCornerMasks = new int[] {
		0b11000001,
		0b01110000,
		0b00011100,
		0b00000111
	};
	
	private static class TileImage{
		
		public BufferedImage image;
		public int numFrames;
		public File mcMetaFile;
		
		public TileImage(BufferedImage image, File imgFile) {
			this.image = image;
			this.numFrames = 0;
			this.mcMetaFile = new File(imgFile.getPath() + ".mcmeta");
			JsonObject data = null;
			if(this.mcMetaFile.exists()) {
				try {
					data = Json.read(this.mcMetaFile).getAsJsonObject();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			MCMetaJavaEdition mcMeta = new MCMetaJavaEdition(imgFile, data);
			numFrames = mcMeta.getFrameCount();
		}
		
	}
	
	public static void createFullTilesFromCompact(List<String> compactTiles, String prefix, 
													List<String> fullTiles, ResourcePack resourcePack) {
		// If the full tiles already exist, then we don't have to create them again.
		File fullTileFile = ResourcePacks.getTexture(prefix + "0"); 
		if(fullTileFile != null && fullTileFile.exists()) {
			for(int fullTileIndex = 0; fullTileIndex < TileToConnectionData.length; ++fullTileIndex) {
				String fullTileName = prefix + Integer.toString(fullTileIndex);
				fullTiles.add(fullTileName);
			}
			return;
		}
		// Make sure we have at least 5 tiles
		if(compactTiles.size() < 5)
			return;
		
		TileImage[] images = new TileImage[5];
		try {
			for(int i = 0; i < images.length; ++i) {
				File imgFile = ResourcePacks.getTexture(compactTiles.get(i));
				if(imgFile == null || !imgFile.exists())
					return; // Texture didn't exist, so we can't make the full tiles anymore
				BufferedImage img = ImageReader.readImage(imgFile);
				if(img == null)
					return;
				images[i] = new TileImage(img, imgFile);
				if(images[i].image.getWidth() != images[0].image.getWidth() ||
						images[i].image.getHeight() != images[0].image.getHeight())
					return; // Resolution of the textures don't match. They need to match.
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		int width = images[0].image.getWidth();
		int height = images[0].image.getHeight() / images[0].numFrames;
		int numFrames = images[0].numFrames;
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		
		for(int fullTileIndex = 0; fullTileIndex < TileToConnectionData.length; ++fullTileIndex) {
			int fullTileData = TileToConnectionData[fullTileIndex];
			// Each compact tile can be split up into four corners.
			// All of the full tiles can be created from some combination
			// of these corners.
			
			BufferedImage fullTileImg = new BufferedImage(width, height * numFrames, BufferedImage.TYPE_INT_ARGB);
			
			for(int frame = 0; frame < numFrames; frame++) {
				// Place the corners in the full tile image.
				for(int cornerId = 0; cornerId < 4; ++cornerId) {
					int neededConnectionData = fullTileData & CompactCornerMasks[cornerId];
					int compactTileIndex = -1;
					for(int i = 0; i < CompactTileToConnectionData.length; ++i) {
						int sampleConnectionData = CompactTileToConnectionData[i] & CompactCornerMasks[cornerId];
						if(sampleConnectionData == neededConnectionData) {
							compactTileIndex = i;
							break;
						}
					}
					if(compactTileIndex < 0) {
						// We couldn't find the right corner
						throw new RuntimeException("No matching corner!");
					}
					
					// Let's draw the corner
					
					// Figure out the position of the corner
					int cornerX = cornerId % 2;
					int cornerY = cornerId / 2;
					if(cornerY == 1)
						cornerX = 1-cornerX;
					cornerX *= halfWidth;
					cornerY *= halfHeight;
					
					// Set the pixels
					for(int j = 0; j < halfHeight; ++j) {
						for(int i = 0; i < halfWidth; ++i) {
							fullTileImg.setRGB(cornerX + i, cornerY + j + height * frame, 
									images[compactTileIndex].image.getRGB(cornerX + i, cornerY + j + height * frame));
						}
					}
				}
			}
			
			String fullTileName = prefix + Integer.toString(fullTileIndex);
			fullTileFile = resourcePack.getResource(fullTileName, "textures", "assets", ".png");
			try {
				ImageIO.write(fullTileImg, "PNG", fullTileFile);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(images[0].mcMetaFile.exists()) {
				try {
					Files.copy(images[0].mcMetaFile, new File(fullTileFile.getPath() + ".mcmeta"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			fullTiles.add(fullTileName);
		}
	}
	
	
	/*
	 * Overlay connection data
	 * 0:  00001000
	 * 1:  00001110
	 * 2:  00000010
	 * 3:  00111110
	 * 4:  10001111
	 * 5:  10111111
	 * 6:  11101111
	 * 7:  00111000
	 * 8:  11111111
	 * 9:  10000011
	 * 10: 11111000
	 * 11: 11100011
	 * 12: 11111110
	 * 13: 11111011
	 * 14: 00100000
	 * 15: 11100000
	 * 16: 10000000
	 */
	public static int[] TileToOverlayData = new int[] {
			0b00001000, 0b00001110, 0b00000010, 0b00111110,
			0b10001111, 0b10111111, 0b11101111, 0b00111000,
			0b11111111, 0b10000011, 0b11111000, 0b11100011,
			0b11111110, 0b11111011, 0b00100000, 0b11100000,
			0b10000000
	};
	public static int[] OverlayDataToTile = new int[256];
	static {
		Arrays.fill(OverlayDataToTile, -2);
		OverlayDataToTile[0b00000000] = -1;
		OverlayDataToTile[0b00001000] = 0;
		OverlayDataToTile[0b00001110] = 1;
		OverlayDataToTile[0b00000010] = 2;
		OverlayDataToTile[0b00111110] = 3;
		OverlayDataToTile[0b10001111] = 4;
		OverlayDataToTile[0b10111111] = 5;
		OverlayDataToTile[0b11101111] = 6;
		OverlayDataToTile[0b00111000] = 7;
		OverlayDataToTile[0b11111111] = 8;
		OverlayDataToTile[0b10000011] = 9;
		OverlayDataToTile[0b11111000] = 10;
		OverlayDataToTile[0b11100011] = 11;
		OverlayDataToTile[0b11111110] = 12;
		OverlayDataToTile[0b11111011] = 13;
		OverlayDataToTile[0b00100000] = 14;
		OverlayDataToTile[0b11100000] = 15;
		OverlayDataToTile[0b10000000] = 16;
		
		// If on two opposite sides.
		// Optifine's CTM Overlay format doesn't have
		// a good tile for it, so just pick something.
		OverlayDataToTile[0b11101110] = 1;
		OverlayDataToTile[0b10111011] = 7;
		
		for(int i = 0; i < OverlayDataToTile.length; ++i) {
			if(OverlayDataToTile[i] >= -1)
				continue;
			// We don't have connection info for this,
			// so let's fill in missing data.
			// If the corners are connected but not the sides
			// then we want to use the texture as if the corners also
			// aren't connected.
			
			int tileIndex = i;
			// Loop over each corner
			for(int cornerBit = 1; cornerBit < 8; cornerBit += 2) {
				// Check the side bits on both sides
				int leftSideBit = cornerBit - 1;
				if(leftSideBit < 0)
					leftSideBit += 8;
				int rightSideBit = cornerBit + 1;
				if(rightSideBit >= 8)
					rightSideBit -= 8;
				
				int leftSide = tileIndex & (1 << leftSideBit);
				int rightSide = tileIndex & (1 << rightSideBit);
				
				// If either sides are connected, then set this corner
				// to be connected as well.
				if(leftSide > 0 || rightSide > 0)
					tileIndex |= 1 << cornerBit;
				// If both sides aren't connected, then set this corner
				// to not be connected as well.
				if(leftSide == 0 && rightSide == 0)
					tileIndex &= ~(1 << cornerBit);
			}
			
			// Copy the file to use from the tileIndex
			OverlayDataToTile[i] = OverlayDataToTile[tileIndex];
		}
	}
	
}
