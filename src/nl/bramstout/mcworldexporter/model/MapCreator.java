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

package nl.bramstout.mcworldexporter.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import org.iq80.leveldb.DB;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.nbt.NbtDataInputStream;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.bedrock.BedrockUtils;
import nl.bramstout.mcworldexporter.world.bedrock.ByteArrayLEDataInputStream;
import nl.bramstout.mcworldexporter.world.bedrock.WorldBedrock;

public class MapCreator {
	
	public static Model createMapModel(long mapId, boolean isBedrock) {
		Model model = new Model("map", null, false);

		RGB[] colorIds = new RGB[128 * 128];

		if(isBedrock) {
			readBedrockMapData(mapId, colorIds);
		}else {
			readJavaMapData(mapId, colorIds);
		}
		
		model.addTexture("#VertexColor", "vertexcolor:vertexcolor");

		for (int i = 0; i < 128; ++i) {
			for (int j = 0; j < 128; ++j) {
				float x = ((float) i) / 8f;
				float y = ((float) (127-j)) / 8f;
				float xMax = x + (1f / 8f);
				float yMax = y + (1f / 8f);

				RGB color = colorIds[i + j * 128];
				if (color == null)
					continue;

				model.addFace(new float[]{ x, y, 8.05f, xMax, yMax, 8.05f }, new float[] { x, y, xMax, yMax }, 
						Direction.SOUTH, "#VertexColor");
				
				model.faces.get(model.faces.size() - 1).setFaceColour(color.x, color.y, color.z);
			}
		}

		return model;
	}
	
	private static void readJavaMapData(long mapId, RGB[] colorIds) {
		try {
			File dataDir = new File(MCWorldExporter.getApp().getWorld().getWorldDir(), "data");
			File mapFile = new File(dataDir, "map_" + mapId + ".dat");

			if (!mapFile.exists()) {
				mapFile = new File(dataDir, "minecraft/maps/" + mapId + ".dat");
				if(!mapFile.exists())
					return;
			}

			GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(mapFile)));

			NbtDataInputStream dis = new NbtDataInputStream(is);
			NbtTagCompound root = (NbtTagCompound) NbtTag.readFromStream(dis);
			NbtTagCompound dataTag = (NbtTagCompound) root.get("data");
			if (dataTag == null) {
				root.free();
				return;
			}
			NbtTagByteArray colors = (NbtTagByteArray) dataTag.get("colors");
			if (colors == null) {
				root.free();
				return;
			}

			for (int i = 0; i < 128 * 128; ++i) {
				colorIds[i] = getRGBFromId(((int) colors.getData()[i]) & 0xFF);
			}
			
			dis.close();
			root.free();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void readBedrockMapData(long mapId, RGB[] colorIds) {
		DB worldDB = null;
		if(MCWorldExporter.getApp().getWorld() instanceof WorldBedrock) {
			worldDB = ((WorldBedrock) MCWorldExporter.getApp().getWorld()).getWorldDB();
		}
		if(worldDB == null)
			return;
		
		byte[] key = BedrockUtils.bytes("map_" + (mapId | 0xC000000000000000L));
		byte[] data = worldDB.get(key);
		if(data == null)
			return;
		
		ByteArrayLEDataInputStream dis = new ByteArrayLEDataInputStream(data);
		
		try {
			NbtTagCompound root = (NbtTagCompound) NbtTag.readFromStream(dis);
			NbtTagByteArray colors = (NbtTagByteArray) root.get("colors");
			if (colors == null) {
				root.free();
				return;
			}
	
			for (int i = 0; i < 128 * 128; ++i) {
				int r = ((int) colors.getData()[i*4]) & 0xFF;
				int g = ((int) colors.getData()[i*4 + 1]) & 0xFF;
				int b = ((int) colors.getData()[i*4 + 2]) & 0xFF;
				int a = ((int) colors.getData()[i*4 + 3]) & 0xFF;
				
				if(a < 127) {
					colorIds[i] = null;
					continue;
				}
				
				RGB baseColor = new RGB(r, g, b);
				baseColor.x = (float) Math.pow(baseColor.x / 255f, 2.2f);
				baseColor.y = (float) Math.pow(baseColor.y / 255f, 2.2f);
				baseColor.z = (float) Math.pow(baseColor.z / 255f, 2.2f);
				
				baseColor.x = baseColor.x * Color.GAMUT.r0 + baseColor.y * Color.GAMUT.r1 + baseColor.z * Color.GAMUT.r2;
				baseColor.y = baseColor.x * Color.GAMUT.g0 + baseColor.y * Color.GAMUT.g1 + baseColor.z * Color.GAMUT.g2;
				baseColor.z = baseColor.x * Color.GAMUT.b0 + baseColor.y * Color.GAMUT.b1 + baseColor.z * Color.GAMUT.b2;
				colorIds[i] = baseColor;
			}
			
			root.free();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private static RGB getRGBFromId(int colorId) {
		int multiplier = colorId % 4;
		int baseColorIndex = colorId / 4;

		if(baseColorIndex < 0)
			return null;
		
		RGB baseColor = BASE_COLORS[baseColorIndex];
		if(baseColor == null)
			return null;
		baseColor = new RGB(baseColor);
		
		if (multiplier == 0)
			baseColor = baseColor.mult(new RGB(0.71f, 0.71f, 0.71f));
		else if (multiplier == 1)
			baseColor = baseColor.mult(new RGB(0.86f, 0.86f, 0.86f));
		else if (multiplier == 3)
			baseColor = baseColor.mult(new RGB(0.53f, 0.53f, 0.53f));

		baseColor.x = (float) Math.pow(baseColor.x / 255f, 2.2f);
		baseColor.y = (float) Math.pow(baseColor.y / 255f, 2.2f);
		baseColor.z = (float) Math.pow(baseColor.z / 255f, 2.2f);
		
		baseColor.x = baseColor.x * Color.GAMUT.r0 + baseColor.y * Color.GAMUT.r1 + baseColor.z * Color.GAMUT.r2;
		baseColor.y = baseColor.x * Color.GAMUT.g0 + baseColor.y * Color.GAMUT.g1 + baseColor.z * Color.GAMUT.g2;
		baseColor.z = baseColor.x * Color.GAMUT.b0 + baseColor.y * Color.GAMUT.b1 + baseColor.z * Color.GAMUT.b2;

		return baseColor;
	}

	private static final RGB BASE_COLORS[] = { null, new RGB(127, 178, 56), new RGB(247, 233, 163),
			new RGB(199, 199, 199), new RGB(255, 0, 0), new RGB(160, 160, 255), new RGB(167, 167, 167),
			new RGB(0, 124, 0), new RGB(255, 255, 255), new RGB(164, 168, 184), new RGB(151, 109, 77),
			new RGB(112, 112, 112), new RGB(64, 64, 255), new RGB(143, 119, 72), new RGB(255, 252, 245),
			new RGB(216, 127, 51), new RGB(178, 76, 216), new RGB(102, 153, 216), new RGB(229, 229, 51),
			new RGB(127, 204, 25), new RGB(242, 127, 165), new RGB(76, 76, 76), new RGB(153, 153, 153),
			new RGB(76, 127, 153), new RGB(127, 63, 178), new RGB(51, 76, 178), new RGB(102, 127, 51),
			new RGB(153, 51, 51), new RGB(25, 25, 25), new RGB(250, 238, 77), new RGB(92, 219, 213),
			new RGB(74, 128, 255), new RGB(0, 217, 58), new RGB(129, 86, 49), new RGB(112, 2, 0),
			new RGB(209, 177, 161), new RGB(159, 82, 36), new RGB(149, 87, 108), new RGB(112, 108, 138),
			new RGB(186, 133, 36), new RGB(103, 117, 53), new RGB(160, 77, 78), new RGB(57, 41, 35),
			new RGB(135, 107, 98), new RGB(87, 92, 92), new RGB(122, 73, 88), new RGB(76, 62, 92),
			new RGB(76, 50, 35), new RGB(76, 82, 42), new RGB(142, 60, 46), new RGB(37, 22, 16),
			new RGB(189, 48, 49), new RGB(148, 63, 97), new RGB(92, 25, 29), new RGB(22, 126, 134),
			new RGB(58, 142, 140), new RGB(86, 44, 62), new RGB(20, 180, 133), new RGB(100, 100, 100),
			new RGB(216, 175, 147), new RGB(127, 167, 150)};
	
	private static class RGB{
		float x;
		float y;
		float z;
		
		public RGB(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public RGB(RGB other) {
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
		}
		
		public RGB mult(RGB other) {
			return new RGB(x * other.x, y * other.y, z * other.z);
		}
	}

}
