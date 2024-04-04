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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte_Array;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;

public class MapCreator {
	
	public static Model createMapModel(int mapId) {
		Model model = new Model("map", null, false);

		int[] colorIds = new int[128 * 128];

		try {
			File dataDir = new File(MCWorldExporter.getApp().getWorld().getWorldDir(), "data");
			File mapFile = new File(dataDir, "map_" + mapId + ".dat");

			if (!mapFile.exists())
				return null;

			GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(mapFile)));

			DataInputStream dis = new DataInputStream(is);
			TAG_Compound root = (TAG_Compound) NBT_Tag.make(dis);
			TAG_Compound dataTag = (TAG_Compound) root.getElement("data");
			if (dataTag == null)
				return null;
			TAG_Byte_Array colors = (TAG_Byte_Array) dataTag.getElement("colors");
			if (colors == null)
				return null;

			for (int i = 0; i < 128 * 128; ++i) {
				colorIds[i] = colors.data[i];
			}
			
			dis.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		model.addTexture("VertexColor", "vertexcolor:vertexcolor");

		for (int i = 0; i < 128; ++i) {
			for (int j = 0; j < 128; ++j) {
				float x = ((float) i) / 8f;
				float y = ((float) (127-j)) / 8f;
				float xMax = x + (1f / 8f);
				float yMax = y + (1f / 8f);

				RGB color = getRGBFromId(colorIds[i + j * 128]);
				if (color == null)
					continue;

				model.addFace(new float[]{ x, y, 8.05f, xMax, yMax, 8.05f }, new float[] { x, y, xMax, yMax }, 
						Direction.SOUTH, "#VertexColor");
				
				model.faces.get(model.faces.size() - 1).setFaceColour(color.x, color.y, color.z);
			}
		}

		return model;
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
		
		baseColor.x = baseColor.x * sRGB_TO_ACEScg[0][0] + baseColor.y * sRGB_TO_ACEScg[0][1] + baseColor.z * sRGB_TO_ACEScg[0][2];
		baseColor.y = baseColor.x * sRGB_TO_ACEScg[1][0] + baseColor.y * sRGB_TO_ACEScg[1][1] + baseColor.z * sRGB_TO_ACEScg[1][2];
		baseColor.z = baseColor.x * sRGB_TO_ACEScg[2][0] + baseColor.y * sRGB_TO_ACEScg[2][1] + baseColor.z * sRGB_TO_ACEScg[2][2];

		return baseColor;
	}

	private static final float sRGB_TO_ACEScg[][] = { { 0.61313242239054f, 0.33953801579967f, 0.04741669604827f },
			{ 0.07012438083392f, 0.91639401131357f, 0.01345152395824f },
			{ 0.02058765752818f, 0.10957457161068f, 0.86978540403533f } };

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
			new RGB(58, 142, 140), new RGB(86, 44, 62), new RGB(20, 180, 133) };
	
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
