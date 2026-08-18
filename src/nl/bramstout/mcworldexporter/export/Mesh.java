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

package nl.bramstout.mcworldexporter.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.Occlusion;

public class Mesh {
	
	private String name;
	private MeshPurpose purpose;
	private String texture;
	private String matTexture;
	private boolean animatedTexture;
	private String shadingMode;
	private String extraData;
	private FloatArray vertices;
	private FloatArray uvs;
	private FloatArray cornerUVs;
	private FloatArray normals;
	private VarIntArray faceIndices;
	private VarIntArray uvIndices;
	private VarIntArray cornerUVIndices;
	private VarIntArray normalIndices;
	private boolean doubleSided;
	private boolean hasColors;
	private boolean hasAO;
	//private FaceCache faceCache;
	private VertexColorSet colors;
	private VertexColorSet ao;
	private List<VertexColorSet> additionalColorSets;
	private Set<String> colorSetNames;
	private List<MeshSubset> subsets;
	private boolean hasProxySubsets;
	private boolean animatesTopology;
	private boolean animatesPoints;
	private boolean animatesUVs;
	private boolean animatesVertexColors;
	private Vector3f boundsMin;
	private Vector3f boundsMax;
	private byte blockLightEmission;
	
	public Mesh() {
		this("", MeshPurpose.UNDEFINED, "", "", false, false, ModelFace.SHADING_MODE_STANDARD, 6, 4);
	}
	
	public Mesh(String name, MeshPurpose purpose, String texture, String matTexture, boolean animatedTexture, 
				boolean doubleSided, String shadingMode, int largeCapacity, int smallCapacity) {
		this.name = name;
		this.purpose = purpose;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.shadingMode = shadingMode;
		this.extraData = "";
		this.vertices = new FloatArray(largeCapacity*3);
		this.uvs = new FloatArray(smallCapacity*2);
		if(Config.calculateCornerUVs)
			this.cornerUVs = new FloatArray(smallCapacity*2);
		else
			this.cornerUVs = null;
		this.normals = new FloatArray(smallCapacity*3);
		this.faceIndices = new VarIntArray(largeCapacity*4);
		this.uvIndices = new VarIntArray(largeCapacity*4);
		if(Config.calculateCornerUVs)
			this.cornerUVIndices = new VarIntArray(largeCapacity*4);
		else
			this.cornerUVIndices = null;
		this.normalIndices = new VarIntArray(largeCapacity);
		this.doubleSided = doubleSided;
		this.hasColors = false;
		this.hasAO = false;
		//this.faceCache = new FaceCache();
		this.colors = null;
		this.ao = null;
		this.additionalColorSets = null;
		this.colorSetNames = null;
		this.subsets = null;
		this.boundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.boundsMax = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		this.blockLightEmission = 0;
	}
	
	public void reset(String name, MeshPurpose purpose, String texture, String matTexture, 
						boolean animatedTexture, boolean doubleSided, String shadingMode) {
		this.name = name;
		this.purpose = purpose;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.shadingMode = shadingMode;
		this.extraData = "";
		this.vertices.clear();
		this.uvs.clear();
		if(this.cornerUVs != null)
			this.cornerUVs.clear();
		this.normals.clear();
		this.faceIndices.clear();
		this.uvIndices.clear();
		if(this.cornerUVIndices != null)
			this.cornerUVIndices.clear();
		this.normalIndices.clear();
		this.doubleSided = doubleSided;
		this.hasColors = false;
		this.hasAO = false;
		//this.faceCache.clear();
		if(this.colors != null)
			this.colors.clear();
		if(this.ao != null)
			this.ao.clear();
		if(this.additionalColorSets != null)
			this.additionalColorSets.clear();
		if(this.colorSetNames != null)
			this.colorSetNames.clear();
		if(this.subsets != null)
			this.subsets.clear();
		this.boundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.boundsMax = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		this.blockLightEmission = 0;
	}
	
	public void packVertices(IndexCacheFlat cache) {
		// Check if bounds are valid.
		if(this.boundsMax.x < this.boundsMin.x)
			return;
		
		float originX = this.boundsMin.x;
		float originY = this.boundsMin.y;
		float originZ = this.boundsMin.z;
		// 1048575 scales it to take up 20 bits.
		// 3x20 = 60 which makes it fit nicely in a single long.
		float scaleX = 1048575f / (this.boundsMax.x - this.boundsMin.x);
		float scaleY = 1048575f / (this.boundsMax.x - this.boundsMin.x);
		float scaleZ = 1048575f / (this.boundsMax.x - this.boundsMin.x);
		
		int numVertices = this.vertices.size() / 3;
		cache.reset(numVertices);
		int numIndices = this.faceIndices.size();
		int largestVertexI = 0;
		
		for(int i = 0; i < numIndices; ++i) {
			int vertexI = this.faceIndices.get(i);
			long keyX = (long) ((this.vertices.get(vertexI*3  ) - originX) * scaleX);
			long keyY = (long) ((this.vertices.get(vertexI*3+1) - originY) * scaleY);
			long keyZ = (long) ((this.vertices.get(vertexI*3+2) - originZ) * scaleZ);
			long key = ((keyX & 0x1FFFFFL) << 42) | ((keyY & 0x1FFFFFL) << 21) | (keyZ & 0x1FFFFFL);
			vertexI = cache.getOrInsert(key, vertexI);
			this.faceIndices.set(i, vertexI);
			largestVertexI = Math.max(largestVertexI, vertexI);
		}
		
		this.vertices.resizeFast(largestVertexI);
	}
	
	/*private long packVertexId(long x, long y, long z) {
		return  (((x >> 0)  & 7) << 61) | (((y >> 0)  & 7) << 58) | (((z >> 0)  & 7) << 55) | 
				(((x >> 3)  & 7) << 52) | (((y >> 3)  & 7) << 49) | (((z >> 3)  & 7) << 46) | 
				(((x >> 6)  & 7) << 43) | (((y >> 6)  & 7) << 40) | (((z >> 6)  & 7) << 37) | 
				(((x >> 9)  & 7) << 34) | (((y >> 9)  & 7) << 31) | (((z >> 9)  & 7) << 28) | 
				(((x >> 12) & 7) << 25) | (((y >> 12) & 7) << 22) | (((z >> 12) & 7) << 19) | 
				(((x >> 15) & 7) << 16) | (((y >> 15) & 7) << 13) | (((z >> 15) & 7) << 10) | 
				(((x >> 18) & 7) << 7)  | (((y >> 18) & 7) << 4)  | (((z >> 18) & 7) << 1);
	}*/
	
	/*private long calcVertexId1(float x, float y, float z) {
		// We compact the three floats into a single 64 bit integer
		return packVertexId(Float.floatToRawIntBits(x) >>> 14,
							Float.floatToRawIntBits(y) >>> 14,
							Float.floatToRawIntBits(z) >>> 14);
	}
	
	private long calcVertexId2(float x, float y, float z) {
		// We compact the three floats into a single 64 bit integer
		return packVertexId((Float.floatToRawIntBits(x) >> 6) & 0xFF,
							(Float.floatToRawIntBits(y) >> 6) & 0xFF,
							(Float.floatToRawIntBits(z) >> 6) & 0xFF);
	}*/
	
	public void addPoint(float x, float y, float z, float u, float v, float cornerU, float cornerV, 
						float r, float g, float b, float ao, int[] out) {
		int vertexIndex = -1;
		//long hash1 = calcVertexId1(x, y, z);
		//long hash2 = calcVertexId2(x, y, z);
		//vertexIndex = this.vertexCache.getOrDefault(hash1, hash2, -1);
		
		int uvIndex = -1;
		float[] uvData = uvs.getData();
		int uvsSize = uvs.size();
		for(int i = 0; i < uvsSize; i+=2) {
			if(Math.abs(uvData[i] - u) < 0.00001f && Math.abs(uvData[i + 1] - v) < 0.00001f) {
				uvIndex = i/2;
				break;
			}
		}
		
		int cornerUVIndex = -1;
		if(cornerUVs != null) {
			float[] cornerUVData = cornerUVs.getData();
			int cornerUVsSize = cornerUVs.size();
			for(int i = 0; i < cornerUVsSize; i += 2) {
				if(Math.abs(cornerUVData[i] - cornerU) < 0.00001f && Math.abs(cornerUVData[i + 1] - cornerV) < 0.00001f) {
					cornerUVIndex = i/2;
					break;
				}
			}
		}
		
		if(vertexIndex == -1) {
			vertexIndex = vertices.size() / 3;
			vertices.add(x);
			vertices.add(y);
			vertices.add(z);
			this.boundsMin.x = Math.min(this.boundsMin.x, x);
			this.boundsMin.y = Math.min(this.boundsMin.y, y);
			this.boundsMin.z = Math.min(this.boundsMin.z, z);
			this.boundsMax.x = Math.min(this.boundsMax.x, x);
			this.boundsMax.y = Math.min(this.boundsMax.y, y);
			this.boundsMax.z = Math.min(this.boundsMax.z, z);
			//this.vertexCache.put(hash1, hash2, vertexIndex);
		}
		
		if(uvIndex == -1) {
			uvIndex = uvs.size()/2;
			uvs.add(u);
			uvs.add(v);
		}
		
		if(cornerUVs != null) {
			if(cornerUVIndex == -1) {
				cornerUVIndex = cornerUVs.size() / 2;
				cornerUVs.add(cornerU);
				cornerUVs.add(cornerV);
			}
		}
		
		int colorIndex = -2;
		if(hasColors)
			colorIndex = colors.addValue(r, g, b);
		int aoIndex = -2;
		if(hasAO)
			aoIndex = this.ao.addValue(ao);
		
		out[0] = vertexIndex;
		out[1] = uvIndex;
		out[2] = colorIndex;
		out[3] = aoIndex;
		out[4] = cornerUVIndex;
	}
	
	/*private void forceAddPoint(float x, float y, float z, int[] out) {
		int vertexIndex = vertices.size() / 3;
		vertices.add(x);
		vertices.add(y);
		vertices.add(z);
		out[0] = vertexIndex;
	}*/
	
	public void addFaceVertex(int[] v0) {
		faceIndices.add(v0[0]);
		
		uvIndices.add(v0[1]);
		
		if(v0[2] >= 0)
			this.colors.addIndex(v0[2]);

		if(v0[3] >= 0)
			this.ao.addIndex(v0[3]);
		
		if(cornerUVIndices != null)
			cornerUVIndices.add(v0[4]);
	}
	
	public int addNormal(float x, float y, float z) {
		int normalIndex = -1;
		
		float[] normalsData = normals.getData();
		int normalsSize = normals.size();
		for(int i = 0; i < normalsSize; i += 3) {
			if(Math.abs(normalsData[i] - x) < 0.00001f && 
					Math.abs(normalsData[i + 1] - y) < 0.00001f &&
					Math.abs(normalsData[i + 2] - z) < 0.00001f) {
				normalIndex = i/3;
				break;
			}
		}
		
		if(normalIndex == -1) {
			normalIndex = normals.size() / 3;
			normals.add(x);
			normals.add(y);
			normals.add(z);
		}
		
		return normalIndex;
	}
	
	private Color[] tint1 = new Color[1];
	
	public void addFace(ModelFace face, float bx, float by, float bz, Atlas.AtlasItem atlas, 
						Color tint, int cornerData, VertexColorSet.VertexColorFace[] vertexColors) {
		tint1[0] = tint;
		addFace(face, bx, by, bz, 0f, 0f, 0f, 0f, 1.0f, 1.0f, 1.0f, 1.0f, atlas, tint == null ? null : tint1, null, cornerData, vertexColors, null);
	}
	
	private float lerp(float t, float v0, float v1) {
		return v0 + (v1 - v0) * t;
	}
	
	private float lerp3d(float tx, float ty, float tz, 
			float v000, float v100, float v001, float v101,
			float v010, float v110, float v011, float v111) {
		float v00 = lerp(tz, v000, v001);
		float v01 = lerp(tz, v010, v011);
		float v10 = lerp(tz, v100, v101);
		float v11 = lerp(tz, v110, v111);
		float v0 = lerp(ty, v00, v01);
		float v1 = lerp(ty, v10, v11);
		return lerp(tx, v0, v1);
	}
	
	private void setTint(float[] points, float[] colors, Color[] tint, int index) {
		if(tint.length == 1) {
			colors[index*3] *= tint[0].getR();
			colors[index*3+1] *= tint[0].getG();
			colors[index*3+2] *= tint[0].getB();
		}else {
			float tx = Math.min(Math.max(points[index*3]/16f, 0f), 1f);
			float ty = Math.min(Math.max(points[index*3+1]/16f, 0f), 1f);
			float tz = Math.min(Math.max(points[index*3+2]/16f, 0f), 1f);
			colors[index*3] *= lerp3d(tx, ty, tz,
					tint[0].getR(), tint[1].getR(), tint[2].getR(), tint[3].getR(),
					tint[4].getR(), tint[5].getR(), tint[6].getR(), tint[7].getR());
			colors[index*3+1] *= lerp3d(tx, ty, tz,
					tint[0].getG(), tint[1].getG(), tint[2].getG(), tint[3].getG(),
					tint[4].getG(), tint[5].getG(), tint[6].getG(), tint[7].getG());
			colors[index*3+2] *= lerp3d(tx, ty, tz,
					tint[0].getB(), tint[1].getB(), tint[2].getB(), tint[3].getB(),
					tint[4].getB(), tint[5].getB(), tint[6].getB(), tint[7].getB());
		}
	}
	
	private float[] pointsData = new float[12];
	private float[] uvsData = new float[8];
	private float[] colorsData = new float[12];
	private float[] normalData = new float[3];
	private float[] cornerUVData = new float[8];
	private int[] v0Data = new int[5];
	private int[] v1Data = new int[5];
	private int[] v2Data = new int[5];
	private int[] v3Data = new int[5];
	public void addFace(ModelFace face, float bx, float by, float bz, float additionalX, float additionalY, float additionalZ,
			float uvOffsetY, float scale, float yScale, float uvScale, float yuvScale, Atlas.AtlasItem atlas, Color[] tint,
			AmbientOcclusion ambientOcclusion, int cornerData, VertexColorSet.VertexColorFace[] vertexColors,
			float[] normalData) {
		float ox = bx * 16.0f + additionalX;
		float oy = by * 16.0f + additionalY;
		float oz = bz * 16.0f + additionalZ;
		face.getPoints(pointsData);
		face.getUVs(uvsData);
		colorsData[0] = 1f; colorsData[1] = 1f; colorsData[2] = 1f;
		colorsData[3] = 1f; colorsData[4] = 1f; colorsData[5] = 1f;
		colorsData[6] = 1f; colorsData[7] = 1f; colorsData[8] = 1f;
		colorsData[9] = 1f; colorsData[10] = 1f; colorsData[11] = 1f;
		if(face.hasVertexColor || tint != null){
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new VertexColorSet("Cd", 3, this.faceIndices.size() + 3);
					registerColorSetName(this.colors.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.colors.addValue(1.0f, 1.0f, 1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colors.addIndex(whiteIndex);
				}
				hasColors = true;
			}
		}
		if(face.hasVertexColor) {
			colorsData[0] = face.vertexColorR; 
			colorsData[1] = face.vertexColorG; 
			colorsData[2] = face.vertexColorB;
			colorsData[3] = face.vertexColorR; 
			colorsData[4] = face.vertexColorG; 
			colorsData[5] = face.vertexColorB;
			colorsData[6] = face.vertexColorR; 
			colorsData[7] = face.vertexColorG; 
			colorsData[8] = face.vertexColorB;
			colorsData[9] = face.vertexColorR; 
			colorsData[10] = face.vertexColorG; 
			colorsData[11] = face.vertexColorB;
		}
		if(tint != null) {
			setTint(pointsData, colorsData, tint, 0);
			setTint(pointsData, colorsData, tint, 1);
			setTint(pointsData, colorsData, tint, 2);
			setTint(pointsData, colorsData, tint, 3);
		}
		if(atlas != null) {
			for(int i = 0; i < uvsData.length; i += 2) {
				uvsData[i] = (uvsData[i] + atlas.x * 16.0f) / atlas.width;
				uvsData[i+1] = (uvsData[i+1] + (atlas.height - atlas.y - ((float) atlas.padding)) * 16.0f) / atlas.height;
			}
		}
		// Scale the UVs
		if(uvScale != 1.0f || yuvScale != 1.0f) {
			float pivotU = Math.min(uvsData[0], uvsData[4]);
			float pivotV = Math.min(uvsData[1], uvsData[5]);
			for(int i = 0; i < uvsData.length; i += 2) {
				uvsData[i] = (uvsData[i] - pivotU) * uvScale + pivotU;
				uvsData[i + 1] = (uvsData[i + 1] - pivotV) * yuvScale + pivotV;
			}
		}
		
		float ao0 = 1.0f;
		float ao1 = 1.0f;
		float ao2 = 1.0f;
		float ao3 = 1.0f;
		if(Config.calculateAmbientOcclusion && ambientOcclusion != null) {
			ao0 = getAOForPoint(pointsData[0], pointsData[1], pointsData[2], ambientOcclusion, face.getDirection());
			ao1 = getAOForPoint(pointsData[3], pointsData[4], pointsData[5], ambientOcclusion, face.getDirection());
			ao2 = getAOForPoint(pointsData[6], pointsData[7], pointsData[8], ambientOcclusion, face.getDirection());
			ao3 = getAOForPoint(pointsData[9], pointsData[10], pointsData[11], ambientOcclusion, face.getDirection());
			if(!hasAO) {
				if(this.ao == null) {
					this.ao = new VertexColorSet("CdAO", 1, this.faceIndices.size() + 3);
					registerColorSetName(this.ao.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.ao.addValue(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.ao.addIndex(whiteIndex);
				}
				hasAO = true;
			}
		}
		
		Occlusion.getCornerUVsForIndex(cornerData, cornerUVData);
		
		addPoint(
				(pointsData[0] - 8f) * scale + 8f + ox, 
				(pointsData[1] - 8f) * yScale + 8f + oy, 
				(pointsData[2] - 8f) * scale + 8f + oz, 
				uvsData[0] / 16.0f, 
				uvsData[1] / 16.0f + uvOffsetY, 
				cornerUVData[0], 
				cornerUVData[1],
				colorsData[0], 
				colorsData[1], 
				colorsData[2], 
				ao0, 
				v0Data);
		addPoint(
				(pointsData[3] - 8f) * scale + 8f + ox, 
				(pointsData[4] - 8f) * yScale + 8f + oy, 
				(pointsData[5] - 8f) * scale + 8f + oz, 
				uvsData[2] / 16.0f, 
				uvsData[3] / 16.0f + uvOffsetY, 
				cornerUVData[2], 
				cornerUVData[3], 
				colorsData[3], 
				colorsData[4], 
				colorsData[5], 
				ao1, 
				v1Data);
		addPoint(
				(pointsData[6] - 8f) * scale + 8f + ox, 
				(pointsData[7] - 8f) * yScale + 8f + oy, 
				(pointsData[8] - 8f) * scale + 8f + oz, 
				uvsData[4] / 16.0f, 
				uvsData[5] / 16.0f + uvOffsetY, 
				cornerUVData[4], 
				cornerUVData[5],
				colorsData[6], 
				colorsData[7], 
				colorsData[8], 
				ao2, 
				v2Data);
		addPoint(
				(pointsData[9] - 8f) * scale + 8f + ox, 
				(pointsData[10] - 8f) * yScale + 8f + oy, 
				(pointsData[11] - 8f) * scale + 8f + oz, 
				uvsData[6] / 16.0f, 
				uvsData[7] / 16.0f + uvOffsetY, 
				cornerUVData[6], 
				cornerUVData[7],
				colorsData[9], 
				colorsData[10], 
				colorsData[11], 
				ao3, 
				v3Data);
		
		if(v0Data[0] == v1Data[0] || v0Data[0] == v2Data[0] || v0Data[0] == v3Data[0] ||
				v1Data[0] == v2Data[0] || v1Data[0] == v3Data[0] || v2Data[0] == v3Data[0]) {
			//throw new RuntimeException("Face contains duplicate vertex");
			return;
		}
		if(v0Data[1] == v1Data[1] || v0Data[1] == v2Data[1] || v0Data[1] == v3Data[1] ||
				v1Data[1] == v2Data[1] || v1Data[1] == v3Data[1] || v2Data[1] == v3Data[1]) {
			//throw new RuntimeException("Face contains duplicate UV vertex");
			return;
		}
		
		/*boolean faceAlreadyExists = faceCache.register(v0Data[0], v1Data[0], v2Data[0], v3Data[0]);
		if(faceAlreadyExists) {
			// Faces that share all edges/vertices can cause issues,
			// so we want to duplicate the vertices then.
			forceAddPoint(
					(pointsData[0] - 8f) * scale + 8f + ox, 
					(pointsData[1] - 8f) * yScale + 8f + oy, 
					(pointsData[2] - 8f) * scale + 8f + oz, 
					v0Data);
			forceAddPoint(
					(pointsData[3] - 8f) * scale + 8f + ox, 
					(pointsData[4] - 8f) * yScale + 8f + oy, 
					(pointsData[5] - 8f) * scale + 8f + oz, 
					v1Data);
			forceAddPoint(
					(pointsData[6] - 8f) * scale + 8f + ox, 
					(pointsData[7] - 8f) * yScale + 8f + oy, 
					(pointsData[8] - 8f) * scale + 8f + oz, 
					v2Data);
			forceAddPoint(
					(pointsData[9] - 8f) * scale + 8f + ox, 
					(pointsData[10] - 8f) * yScale + 8f + oy, 
					(pointsData[11] - 8f) * scale + 8f + oz, 
					v3Data);
		}*/
		
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		
		if(normalData == null) {
			normalData = this.normalData;
			face.calculateNormal(normalData);
		}
		int normalIndex = addNormal(normalData[0], normalData[1], normalData[2]);
		normalIndices.add(normalIndex);
		
		if(face.isDoubleSided())
			doubleSided = true;
		
		if(vertexColors != null) {
			for(VertexColorSet.VertexColorFace vertexColorFace : vertexColors) {
				VertexColorSet thisColorSet = getAdditionalColorSet(vertexColorFace.name);
				if(thisColorSet == null) {
					thisColorSet = new VertexColorSet(vertexColorFace.name, vertexColorFace.componentCount, faceIndices.size());
					if(getAdditionalColorSets() == null)
						this.additionalColorSets = new ArrayList<VertexColorSet>();
					getAdditionalColorSets().add(thisColorSet);
					registerColorSetName(thisColorSet.getName());
					
					_vertexColor[0] = 1.0f;
					_vertexColor[1] = 1.0f;
					_vertexColor[2] = 1.0f;
					_vertexColor[3] = 1.0f;
					int whiteIndex = thisColorSet.addValue(_vertexColor);
					for(int i = 0; i < (faceIndices.size()-4); ++i) {
						thisColorSet.addIndex(whiteIndex);
					}
				}
				
				thisColorSet.addFace(vertexColorFace);
			}
		}
		if(getAdditionalColorSets() != null) {
			// Make sure to also add indices for any color sets
			// that are in this mesh but not the parent mesh.
			for(VertexColorSet thisColorSet : getAdditionalColorSets()) {
				boolean isHandled = false;
				if(vertexColors != null) {
					for(VertexColorSet.VertexColorFace vertexColorFace : vertexColors) {
						if(vertexColorFace.name.equals(thisColorSet.getName())) {
							isHandled = true;
							break;
						}
					}
				}
				if(isHandled)
					continue;
				
				_vertexColor[0] = 1.0f;
				_vertexColor[1] = 1.0f;
				_vertexColor[2] = 1.0f;
				_vertexColor[3] = 1.0f;
				int whiteIndex = thisColorSet.addValue(_vertexColor);
				for(int i = 0; i < 4; ++i) {
					thisColorSet.addIndex(whiteIndex);
				}
			}
		}
	}
	
	private float getAOForPoint(float x, float y, float z, AmbientOcclusion ao, Direction dir) {
		x = Math.min(Math.max(x/16f, 0f), 1f);
		y = Math.min(Math.max(y/16f, 0f), 1f);
		z = Math.min(Math.max(z/16f, 0f), 1f);
		float aof = ao.getAmbientOcclusionForPoint(x, y, z, dir);
		
		return (float) Math.floor(aof * 100f + 0.5f) / 100f;
	}
	
	public void getVertex(int faceIndex, int vertexIndex, float[] out) {
		int vertexId = faceIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = vertices.get(vertexId * 3 + 0);
		out[1] = vertices.get(vertexId * 3 + 1);
		out[2] = vertices.get(vertexId * 3 + 2);
	}
	
	public void getUV(int faceIndex, int vertexIndex, float[] out) {
		int uvId = uvIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = uvs.get(uvId*2);
		out[1] = uvs.get(uvId*2+1);
	}
	
	public void getCornerUV(int faceIndex, int vertexIndex, float[] out) {
		if(cornerUVs != null) {
			int cornerUVId = cornerUVIndices.get(faceIndex * 4 + vertexIndex);
			out[0] = cornerUVs.get(cornerUVId * 2);
			out[1] = cornerUVs.get(cornerUVId * 2 + 1);
		}
	}
	
	public void getColor(int faceIndex, int vertexIndex, float[] out) {
		if(!hasColors) {
			out[0] = 1.0f;
			out[1] = 1.0f;
			out[2] = 1.0f;
			return;
		}
		int colorId = colors.getIndex(faceIndex * 4 + vertexIndex);
		out[0] = colors.getR(colorId);
		out[1] = colors.getG(colorId);
		out[2] = colors.getB(colorId);
	}
	
	public void getNormal(int faceIndex, int vertexIndex, float[] out) {
		int normalId = normalIndices.get(faceIndex);
		out[0] = normals.get(normalId * 3 + 0);
		out[1] = normals.get(normalId * 3 + 1);
		out[2] = normals.get(normalId * 3 + 2);
	}
	
	public float getAO(int faceIndex, int vertexIndex) {
		if(!hasAO)
			return 1.0f;
		int aoId = ao.getIndex(faceIndex * 4 + vertexIndex);
		return ao.getR(aoId);
	}
	
	private float[] _v0 = new float[3];
	private float[] _v1 = new float[3];
	private float[] _v2 = new float[3];
	private float[] _v3 = new float[3];
	private float[] _uv0 = new float[2];
	private float[] _uv1 = new float[2];
	private float[] _uv2 = new float[2];
	private float[] _uv3 = new float[2];
	private float[] _corneruv0 = new float[2];
	private float[] _corneruv1 = new float[2];
	private float[] _corneruv2 = new float[2];
	private float[] _corneruv3 = new float[2];
	private float[] _color0 = new float[3];
	private float[] _color1 = new float[3];
	private float[] _color2 = new float[3];
	private float[] _color3 = new float[3];
	private float[] _normal = new float[3];
	private float[] _vertexColor = new float[4];
	private float _ao0;
	private float _ao1;
	private float _ao2;
	private float _ao3;
	
	private void addFaceFromMesh_getData(Mesh mesh, int index) {
		mesh.getVertex(index, 0, _v0);
		mesh.getVertex(index, 1, _v1);
		mesh.getVertex(index, 2, _v2);
		mesh.getVertex(index, 3, _v3);
		mesh.getUV(index, 0, _uv0);
		mesh.getUV(index, 1, _uv1);
		mesh.getUV(index, 2, _uv2);
		mesh.getUV(index, 3, _uv3);
		if(Config.calculateCornerUVs) {
			mesh.getCornerUV(index, 0, _corneruv0);
			mesh.getCornerUV(index, 1, _corneruv1);
			mesh.getCornerUV(index, 2, _corneruv2);
			mesh.getCornerUV(index, 3, _corneruv3);
		}
		mesh.getColor(index, 0, _color0);
		mesh.getColor(index, 1, _color1);
		mesh.getColor(index, 2, _color2);
		mesh.getColor(index, 3, _color3);
		mesh.getNormal(index, 0, _normal);
		_ao0 = mesh.getAO(index, 0);
		_ao1 = mesh.getAO(index, 1);
		_ao2 = mesh.getAO(index, 2);
		_ao3 = mesh.getAO(index, 3);
	}
	
	private void addFaceFromMesh_setupColorAndAO(Mesh mesh) {
		if(mesh.hasColors) {
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new VertexColorSet("Cd", 3, this.faceIndices.size() + 3);
					registerColorSetName(this.colors.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.colors.addValue(1.0f, 1.0f, 1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colors.addIndex(whiteIndex);
				}
				hasColors = true;
			}
		}
		if(mesh.hasAO) {
			if(!hasAO) {
				if(this.ao == null) {
					this.ao = new VertexColorSet("CdAO", 1, this.faceIndices.size() + 3);
					registerColorSetName(this.ao.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.ao.addValue(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.ao.addIndex(whiteIndex);
				}
				hasAO = true;
			}
		}
	}
	
	private void addFaceFromMesh_addPoints() {
		addPoint(_v0[0], _v0[1], _v0[2], _uv0[0], _uv0[1], _corneruv0[0], _corneruv0[1], 
				_color0[0], _color0[1], _color0[2], _ao0, v0Data);
		addPoint(_v1[0], _v1[1], _v1[2], _uv1[0], _uv1[1], _corneruv1[0], _corneruv1[1],
					_color1[0], _color1[1], _color1[2], _ao1, v1Data);
		addPoint(_v2[0], _v2[1], _v2[2], _uv2[0], _uv2[1], _corneruv2[0], _corneruv2[1],
					_color2[0], _color2[1], _color2[2], _ao2, v2Data);
		addPoint(_v3[0], _v3[1], _v3[2], _uv3[0], _uv3[1], _corneruv3[0], _corneruv3[1],
					_color3[0], _color3[1], _color3[2], _ao3, v3Data);
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		
		int normalIndex = addNormal(_normal[0], _normal[1], _normal[2]);
		normalIndices.add(normalIndex);
	}
	
	private void addFaceFromMesh_setupAdditionalColorSets(Mesh mesh, int index) {
		if(mesh.getAdditionalColorSets() != null) {
			for(VertexColorSet colorSet : mesh.getAdditionalColorSets()) {
				VertexColorSet thisColorSet = getAdditionalColorSet(colorSet.getName());
				if(thisColorSet == null) {
					thisColorSet = new VertexColorSet(colorSet.getName(), colorSet.getComponentCount(), faceIndices.size());
					if(getAdditionalColorSets() == null)
						this.additionalColorSets = new ArrayList<VertexColorSet>();
					getAdditionalColorSets().add(thisColorSet);
					registerColorSetName(thisColorSet.getName());
					
					_vertexColor[0] = 1.0f;
					_vertexColor[1] = 1.0f;
					_vertexColor[2] = 1.0f;
					_vertexColor[3] = 1.0f;
					int whiteIndex = thisColorSet.addValue(_vertexColor);
					for(int i = 0; i < (faceIndices.size()-4); ++i) {
						thisColorSet.addIndex(whiteIndex);
					}
				}
				if(thisColorSet.getComponentCount() < colorSet.getComponentCount()) {
					thisColorSet.expandComponentCount(colorSet.getComponentCount());
				}
				
				for(int vertexId = 0; vertexId < 4; ++vertexId) {
					int vcIndex = colorSet.getIndex(index * 4 + vertexId);
					colorSet.get(vcIndex, _vertexColor);
					int vcIndex2 = thisColorSet.addValue(_vertexColor);
					thisColorSet.addIndex(vcIndex2);
				}
			}
		}
		if(getAdditionalColorSets() != null) {
			// Make sure to also add indices for any color sets
			// that are in this mesh but not the parent mesh.
			for(VertexColorSet thisColorSet : getAdditionalColorSets()) {
				if(mesh.getAdditionalColorSet(thisColorSet.getName()) != null)
					continue;
				_vertexColor[0] = 1.0f;
				_vertexColor[1] = 1.0f;
				_vertexColor[2] = 1.0f;
				_vertexColor[3] = 1.0f;
				int whiteIndex = thisColorSet.addValue(_vertexColor);
				for(int i = 0; i < 4; ++i) {
					thisColorSet.addIndex(whiteIndex);
				}
			}
		}
	}
	
	private void addFaceFromMesh_setupSubsets(Mesh mesh, int index, MeshSubset faceSubset, boolean useSubsets) {
		if(faceSubset != null || useSubsets) {
			String subsetName = "section_0";
			String texture = mesh.getTexture();
			String matTexture = mesh.getMatTexture();
			boolean isAnimated = mesh.hasAnimatedTexture();
			boolean isUnique = false;
			MeshPurpose purpose = MeshPurpose.UNDEFINED;
			long uniqueId = 0;
			int initialCapacity = 8;
			if(faceSubset != null) {
				subsetName = faceSubset.getName();
				isUnique = faceSubset.isUnique();
				purpose = faceSubset.getPurpose();
				uniqueId = faceSubset.getUniqueId();
				initialCapacity = faceSubset.getFaceIndices().size();
				if(faceSubset.getTexture() != null && faceSubset.getMatTexture() != null) {
					texture = faceSubset.getTexture();
					matTexture = faceSubset.getMatTexture();
					isAnimated = faceSubset.isAnimatedTexture();
				}
			}
			if(purpose == MeshPurpose.RENDER && this.purpose == MeshPurpose.PROXY)
				purpose = MeshPurpose.UNDEFINED;
			
			if(texture != null && texture.equals(getTexture()))
				texture = null;
			if(matTexture != null && matTexture.equals(getMatTexture()))
				matTexture = null;
			
			// This face was part of a subset,
			// so we need to find an appropriate subset.
			MeshSubset subset = null;
			if(subsets != null) {
				for(int i = 0; i < subsets.size(); ++i) {
					MeshSubset subset2 = subsets.get(i);
					if(subset2.equals(texture, matTexture, isAnimated, isUnique, uniqueId, purpose)) {
						subset = subset2;
						break;
					}
				}
			}
			if(subset == null) {
				subset = new MeshSubset(subsetName, texture, matTexture, 
						isAnimated, purpose, isUnique, uniqueId, initialCapacity);
				addSubset(subset);
			}
			// Add the index of this face to the subset.
			subset.getFaceIndices().add((faceIndices.size()/4)-1);
			
			if(purpose == MeshPurpose.RENDER && this.purpose != MeshPurpose.RENDER && (matTexture != null || hasProxySubsets)) {
				// The subset is just for the render purpose, but we do need to use subsets for this face
				// and we are missing the subset for the proxy purpose. So add that in.
				purpose = MeshPurpose.PROXY;
				isUnique = false;
				uniqueId = 0;
				
				if(!hasProxySubsets && faceIndices.size() > 0) {
					// We don't yet have any proxy subsets, so we need to make one
					// for the existing faces.
					subset = new MeshSubset("section_0", null, null, false, purpose, isUnique, uniqueId, faceIndices.size()/4);
					addSubset(subset);
					for(int i = 0; i < faceIndices.size()/4; ++i)
						subset.getFaceIndices().add(i);
				}
				
				subset = null;
				if(subsets != null) {
					for(int i = 0; i < subsets.size(); ++i) {
						MeshSubset subset2 = subsets.get(i);
						if(subset2.equals(texture, matTexture, isAnimated, isUnique, uniqueId, purpose)) {
							subset = subset2;
							break;
						}
					}
				}
				if(subset == null) {
					subset = new MeshSubset(subsetName, texture, matTexture, isAnimated, 
							purpose, isUnique, uniqueId, faceSubset.getFaceIndices().size());
					addSubset(subset);
				}
				// Add the index of this face to the subset.
				subset.getFaceIndices().add((faceIndices.size()/4)-1);
			}
		}
	}
	
	public void addFaceFromMesh(Mesh mesh, int index, MeshSubset faceSubset, boolean useSubsets) {
		addFaceFromMesh_getData(mesh, index);
		addFaceFromMesh_setupColorAndAO(mesh);
		addFaceFromMesh_addPoints();
		addFaceFromMesh_setupAdditionalColorSets(mesh, index);
		addFaceFromMesh_setupSubsets(mesh, index, faceSubset, useSubsets);
	}
	
	public void addFace(float[] vertices, float[] us, float[] vs, float[] cornerUVs, 
					float[] normal, float[] color, float[] ao, VertexColorSet.VertexColorFace[] vertexColors) {
		if(color != null) {
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new VertexColorSet("Cd", 3, this.faceIndices.size() + 3);
					registerColorSetName(this.colors.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.colors.addValue(1.0f, 1.0f, 1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colors.addIndex(whiteIndex);
				}
				hasColors = true;
			}
		}
		if(ao != null) {
			if(!hasAO) {
				if(this.ao == null) {
					this.ao = new VertexColorSet("CdAO", 1, this.faceIndices.size() + 3);
					registerColorSetName(this.ao.getName());
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					int whiteIndex = this.ao.addValue(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.ao.addIndex(whiteIndex);
				}
				hasAO = true;
			}
		}
		float cr = 1f;
		float cg = 1f;
		float cb = 1f;
		float ao1 = 1f;
		float ao2 = 1f;
		float ao3 = 1f;
		float ao4 = 1f;
		if(color != null) {
			cr = color[0];
			cg = color[1];
			cb = color[2];
		}
		if(ao != null) {
			ao1 = ao[0];
			ao2 = ao[1];
			ao3 = ao[2];
			ao4 = ao[3];
		}
		
		addPoint(vertices[0], vertices[1], vertices[2],   us[0], vs[0], cornerUVs[0], cornerUVs[1], 
					cr, cg, cb, ao1, v0Data);
		addPoint(vertices[3], vertices[4], vertices[5],   us[1], vs[1], cornerUVs[2], cornerUVs[3],
					cr, cg, cb, ao2, v1Data);
		addPoint(vertices[6], vertices[7], vertices[8],   us[2], vs[2], cornerUVs[4], cornerUVs[5],
					cr, cg, cb, ao3, v2Data);
		addPoint(vertices[9], vertices[10], vertices[11], us[3], vs[3], cornerUVs[6], cornerUVs[7],
					cr, cg, cb, ao4, v3Data);
		
		if(v0Data[0] == v1Data[0] || v0Data[0] == v2Data[0] || v0Data[0] == v3Data[0] ||
				v1Data[0] == v2Data[0] || v1Data[0] == v3Data[0] || v2Data[0] == v3Data[0]) {
			//throw new RuntimeException("Face contains duplicate vertex");
			//return;
		}
		if(v0Data[1] == v1Data[1] || v0Data[1] == v2Data[1] || v0Data[1] == v3Data[1] ||
				v1Data[1] == v2Data[1] || v1Data[1] == v3Data[1] || v2Data[1] == v3Data[1]) {
			//throw new RuntimeException("Face contains duplicate UV vertex");
			//return;
		}
		
		/*boolean faceAlreadyExists = faceCache.register(v0Data[0], v1Data[0], v2Data[0], v3Data[0]);
		if(faceAlreadyExists) {
			// Faces that share all edges/vertices can cause issues,
			// so we want to duplicate the vertices then.
			forceAddPoint(vertices[0], vertices[1], vertices[2], v0Data);
			forceAddPoint(vertices[3], vertices[4], vertices[5], v1Data);
			forceAddPoint(vertices[6], vertices[7], vertices[8], v2Data);
			forceAddPoint(vertices[9], vertices[10], vertices[11], v3Data);
		}*/
		
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		
		int normalIndex = addNormal(normal[0], normal[1], normal[2]);
		normalIndices.add(normalIndex);
		
		if(vertexColors != null) {
			for(VertexColorSet.VertexColorFace vertexColorFace : vertexColors) {
				VertexColorSet thisColorSet = getAdditionalColorSet(vertexColorFace.name);
				if(thisColorSet == null) {
					thisColorSet = new VertexColorSet(vertexColorFace.name, vertexColorFace.componentCount, faceIndices.size());
					if(getAdditionalColorSets() == null)
						this.additionalColorSets = new ArrayList<VertexColorSet>();
					getAdditionalColorSets().add(thisColorSet);
					registerColorSetName(thisColorSet.getName());
					
					_vertexColor[0] = 1.0f;
					_vertexColor[1] = 1.0f;
					_vertexColor[2] = 1.0f;
					_vertexColor[3] = 1.0f;
					int whiteIndex = thisColorSet.addValue(_vertexColor);
					for(int i = 0; i < (faceIndices.size()-4); ++i) {
						thisColorSet.addIndex(whiteIndex);
					}
				}
				
				thisColorSet.addFace(vertexColorFace);
			}
		}
		if(getAdditionalColorSets() != null) {
			// Make sure to also add indices for any color sets
			// that are in this mesh but not the parent mesh.
			for(VertexColorSet thisColorSet : getAdditionalColorSets()) {
				boolean isHandled = false;
				if(vertexColors != null) {
					for(VertexColorSet.VertexColorFace vertexColorFace : vertexColors) {
						if(vertexColorFace.name.equals(thisColorSet.getName())) {
							isHandled = true;
							break;
						}
					}
				}
				if(isHandled)
					continue;
				
				_vertexColor[0] = 1.0f;
				_vertexColor[1] = 1.0f;
				_vertexColor[2] = 1.0f;
				_vertexColor[3] = 1.0f;
				int whiteIndex = thisColorSet.addValue(_vertexColor);
				for(int i = 0; i < 4; ++i) {
					thisColorSet.addIndex(whiteIndex);
				}
			}
		}
	}
	
	public void appendMesh(Mesh mesh, boolean useSubsets) {
		int[] subsetIds = mesh.generateSubsetIds();
		for(int i = 0; i < (mesh.faceIndices.size()/4); ++i) {
			MeshSubset subset = null;
			if(subsetIds != null) {
				int subsetId = subsetIds[i];
				if(subsetId >= 0)
					subset = mesh.getSubset(subsetId);
			}
			addFaceFromMesh(mesh, i, subset, getNumSubsets() > 0 || mesh.getNumSubsets() > 0 || useSubsets);
		}
	}
	
	public int[] generateSubsetIds() {
		int[] subsetIds = null;
		if(getNumSubsets() > 0) {
			// Mesh has subsets, so go through the list
			// and keep track of which subset each face is a part of.
			subsetIds = new int[getFaceIndices().size()/4];
			Arrays.fill(subsetIds, -1);
			int subsetId = 0;
			for(MeshSubset subset : getSubsets()) {
				for(int i = 0; i < subset.getFaceIndices().size(); ++i) {
					subsetIds[subset.getFaceIndices().get(i)] = subsetId;
				}
				subsetId++;
			}
		}
		return subsetIds;
	}
	
	public String getShadingMode() {
		return shadingMode;
	}
	
	public void setShadingMode(String shadingMode) {
		this.shadingMode = shadingMode;
	}
	
	public byte getBlockLightEmission() {
		return blockLightEmission;
	}
	
	public void setBlockLightEmission(byte blockLightEmission) {
		this.blockLightEmission = blockLightEmission;
	}
	
	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	public String getExtraData() {
		return extraData;
	}
	
	public FloatArray getVertices() {
		return vertices;
	}

	public FloatArray getUVs() {
		return uvs;
	}
	
	public FloatArray getCornerUVs() {
		return cornerUVs;
	}
	
	public boolean hasColors() {
		return hasColors;
	}
	
	public boolean hasAO() {
		return hasAO;
	}
	
	public VertexColorSet getColors() {
		return colors;
	}
	
	public FloatArray getNormals() {
		return normals;
	}
	
	public VertexColorSet getAO() {
		return ao;
	}

	public VarIntArray getFaceIndices() {
		return faceIndices;
	}

	public VarIntArray getUvIndices() {
		return uvIndices;
	}
	
	public VarIntArray getCornerUVIndices() {
		return cornerUVIndices;
	}
	
	public VarIntArray getNormalIndices() {
		return normalIndices;
	}
	
	public List<VertexColorSet> getAdditionalColorSets(){
		return this.additionalColorSets;
	}
	
	public VertexColorSet getAdditionalColorSet(String name) {
		if(this.additionalColorSets == null)
			return null;
		for(VertexColorSet colorSet : this.additionalColorSets)
			if(colorSet.getName().equals(name))
				return colorSet;
		return null;
	}
	
	public List<MeshSubset> getSubsets(){
		return subsets;
	}
	
	public int getNumSubsets() {
		if(subsets == null)
			return 0;
		return subsets.size();
	}
	
	public MeshSubset getSubset(int index) {
		if(subsets == null)
			return null;
		if(index < 0 || index >= subsets.size())
			return null;
		return subsets.get(index);
	}
	
	public boolean hasSubset(String name) {
		for(int i = 0; i < subsets.size(); ++i) {
			if(subsets.get(i).getName().equals(name))
				return true;
		}
		return false;
	}
	
	public void addSubset(MeshSubset subset) {
		if(subsets == null)
			subsets = new ArrayList<MeshSubset>();
		
		// Make sure that the name is unique
		String origName = subset.getName();
		for(int i = 1; i < 1000000; ++i) {
			boolean nameCollision = hasSubset(subset.getName());
			if(!nameCollision)
				break;
			subset.setName(origName + "_" + i);
		}
		subsets.add(subset);
		if(subset.getPurpose() == MeshPurpose.PROXY)
			hasProxySubsets = true;
	}
	
	public void setSubsets(ArrayList<MeshSubset> subsets) {
		this.subsets = subsets;
		hasProxySubsets = false;
		for(MeshSubset subset : subsets) {
			if(subset.getPurpose() == MeshPurpose.PROXY) {
				hasProxySubsets = true;
				break;
			}
		}
	}
	
	public void getFlatUVs(FloatArray flatUs, FloatArray flatVs) {
		flatUs.resize(uvIndices.size());
		flatVs.resize(uvIndices.size());
		
		for(int i = 0; i < uvIndices.size(); ++i) {
			flatUs.set(i, uvs.get(uvIndices.get(i)*2));
			flatVs.set(i, uvs.get(uvIndices.get(i)*2+1));
		}
	}
	
	public void getFlatCornerUVs(FloatArray flatUs, FloatArray flatVs) {
		if(cornerUVs != null) {
			flatUs.resize(cornerUVIndices.size());
			flatVs.resize(cornerUVIndices.size());
			
			for(int i = 0; i < cornerUVIndices.size(); ++i) {
				flatUs.set(i, cornerUVs.get(cornerUVIndices.get(i) * 2));
				flatVs.set(i, cornerUVs.get(cornerUVIndices.get(i) * 2 + 1));
			}
		}
	}
	
	public void getFlatNormals(FloatArray flatNormals) {
		flatNormals.resize(normalIndices.size()*3*4);
		
		for(int i = 0; i < normalIndices.size(); ++i) {
			flatNormals.set(i * 3 * 4    , normals.get(normalIndices.get(i) * 3));
			flatNormals.set(i * 3 * 4 + 1, normals.get(normalIndices.get(i) * 3 + 1));
			flatNormals.set(i * 3 * 4 + 2, normals.get(normalIndices.get(i) * 3 + 2));
			
			flatNormals.set(i * 3 * 4     + 3, normals.get(normalIndices.get(i) * 3));
			flatNormals.set(i * 3 * 4 + 1 + 3, normals.get(normalIndices.get(i) * 3 + 1));
			flatNormals.set(i * 3 * 4 + 2 + 3, normals.get(normalIndices.get(i) * 3 + 2));
			
			flatNormals.set(i * 3 * 4     + 6, normals.get(normalIndices.get(i) * 3));
			flatNormals.set(i * 3 * 4 + 1 + 6, normals.get(normalIndices.get(i) * 3 + 1));
			flatNormals.set(i * 3 * 4 + 2 + 6, normals.get(normalIndices.get(i) * 3 + 2));
			
			flatNormals.set(i * 3 * 4     + 9, normals.get(normalIndices.get(i) * 3));
			flatNormals.set(i * 3 * 4 + 1 + 9, normals.get(normalIndices.get(i) * 3 + 1));
			flatNormals.set(i * 3 * 4 + 2 + 9, normals.get(normalIndices.get(i) * 3 + 2));
		}
	}

	public boolean isAnimatesTopology() {
		return animatesTopology;
	}

	public void setAnimatesTopology(boolean animatesTopology) {
		this.animatesTopology = animatesTopology;
	}

	public boolean isAnimatesPoints() {
		return animatesPoints;
	}

	public void setAnimatesPoints(boolean animatesPoints) {
		this.animatesPoints = animatesPoints;
	}

	public boolean isAnimatesUVs() {
		return animatesUVs;
	}

	public void setAnimatesUVs(boolean animatesUVs) {
		this.animatesUVs = animatesUVs;
	}

	public boolean isAnimatesVertexColors() {
		return animatesVertexColors;
	}

	public void setAnimatesVertexColors(boolean animatesVertexColors) {
		this.animatesVertexColors = animatesVertexColors;
	}

	public void write(LargeDataOutputStream dos, boolean chunked) throws IOException {
		dos.writeByte(1); // Mesh type : Mesh
		dos.writeUTF(name);
		dos.writeInt(doubleSided ? 1 : 0);
		dos.writeInt(purpose.id);
		dos.writeUTF(texture);
		dos.writeUTF(matTexture);
		dos.writeInt(animatedTexture ? 1 : 0);
		dos.writeUTF(shadingMode);
		dos.writeByte(blockLightEmission);
		dos.writeUTF(extraData);
		dos.writeInt(vertices.size() / 3); // num vertices
		dos.writeInt(uvs.size()/2); // num UVs
		if(Config.calculateCornerUVs)
			dos.writeInt(cornerUVs.size()/2); // num corner UVs
		else
			dos.writeInt(0);
		dos.writeInt(normals.size() / 3); // num normals
		dos.writeInt(faceIndices.size() / 4); // num faces
		
		// Pretty much all of the code assumes that a block is 16 units.
		// In order to not break any of that, we do the scaling here.
		float worldScale = Config.blockSizeInUnits / 16.0f;
		float worldOffsetXZ = Config.blockCenteredXZOnOrigin ? 0f : (Config.blockSizeInUnits * 0.5f);
		
		// vertex data
		int i = 0;
		for(i = 0; i < vertices.size(); i += 3) {
			dos.writeFloat(vertices.get(i) * worldScale + worldOffsetXZ);
			dos.writeFloat(vertices.get(i+1) * worldScale);
			dos.writeFloat(vertices.get(i+2) * worldScale + worldOffsetXZ);
		}
		// uv data
		for(i = 0; i < uvs.size(); ++i)
			dos.writeFloat(uvs.get(i));
		// corner uv data
		if(Config.calculateCornerUVs)
			for(i = 0; i < cornerUVs.size(); ++i)
				dos.writeFloat(cornerUVs.get(i));
		// normal data
		for(i = 0; i < normals.size(); ++i)
			dos.writeFloat(normals.get(i));
		// face index data
		for(i = 0; i < faceIndices.size(); ++i)
			dos.writeInt(faceIndices.get(i));
		// uv index data
		for(i = 0; i < uvIndices.size(); ++i)
			dos.writeInt(uvIndices.get(i));
		// corner uv index data
		if(Config.calculateCornerUVs)
			for(i = 0; i < cornerUVIndices.size(); ++i)
				dos.writeInt(cornerUVIndices.get(i));
		// normal index data
		for(i = 0; i < normalIndices.size(); ++i)
			dos.writeInt(normalIndices.get(i));
		// Write vertex color sets
		int numColorSets = 0;
		if(hasColors)
			numColorSets += 1;
		if(hasAO)
			numColorSets += 1;
		if(additionalColorSets != null)
			numColorSets += additionalColorSets.size();
		dos.writeInt(numColorSets);
		if(hasColors)
			colors.write(dos);
		if(hasAO)
			ao.write(dos);
		if(additionalColorSets != null)
			for(VertexColorSet colorSet : additionalColorSets)
				colorSet.write(dos);
		
		// Write mesh subsets
		if(subsets == null) {
			dos.writeInt(0);
		}else {
			dos.writeInt(subsets.size());
			for(MeshSubset subset : subsets)
				subset.write(dos);
		}
	}
	
	public Mesh(LargeDataInputStream dis) throws IOException{
		this.name = dis.readUTF();
		this.doubleSided = dis.readInt() > 0;
		this.purpose = MeshPurpose.fromId(dis.readInt());
		this.texture = dis.readUTF();
		this.matTexture = dis.readUTF();
		this.animatedTexture = dis.readInt() > 0;
		this.shadingMode = dis.readUTF();
		this.blockLightEmission = dis.readByte();
		this.extraData = dis.readUTF();
		this.boundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		this.boundsMax = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		int numVertices = dis.readInt();
		int numUVs = dis.readInt();
		int numCornerUVs = dis.readInt();
		int numNormals = dis.readInt();
		int numFaces = dis.readInt();
		
		float[] vertices = new float[numVertices*3];
		for(int i = 0; i < numVertices; ++i) {
			vertices[i*3] = dis.readFloat();
			vertices[i*3+1] = dis.readFloat();
			vertices[i*3+2] = dis.readFloat();
			this.boundsMin.x = Math.min(this.boundsMin.x, vertices[i*3]);
			this.boundsMin.y = Math.min(this.boundsMin.y, vertices[i*3+1]);
			this.boundsMin.z = Math.min(this.boundsMin.z, vertices[i*3+2]);
			this.boundsMax.x = Math.min(this.boundsMax.x, vertices[i*3]);
			this.boundsMax.y = Math.min(this.boundsMax.y, vertices[i*3+1]);
			this.boundsMax.z = Math.min(this.boundsMax.z, vertices[i*3+2]);
		}
		this.vertices = new FloatArray(vertices);
		
		float[] uvs = new float[numUVs*2];
		for(int i = 0; i < uvs.length; ++i)
			uvs[i] = dis.readFloat();
		this.uvs = new FloatArray(uvs);
		
		if(numCornerUVs > 0) {
			float[] cornerUVs = new float[numCornerUVs*2];
			for(int i = 0; i < cornerUVs.length; ++i)
				cornerUVs[i] = dis.readFloat();
			this.cornerUVs = new FloatArray(cornerUVs);
		}else {
			this.cornerUVs = new FloatArray(2);
		}
		
		float[] normals = new float[numNormals*3];
		for(int i = 0; i < normals.length; ++i)
			normals[i] = dis.readFloat();
		this.normals = new FloatArray(normals);
		
		int[] faceIndices = new int[numFaces * 4];
		for(int i = 0; i < faceIndices.length; ++i)
			faceIndices[i] = dis.readInt();
		this.faceIndices = new VarIntArray(faceIndices);
		
		int[] uvIndices = new int[numFaces * 4];
		for(int i = 0; i < uvIndices.length; ++i)
			uvIndices[i] = dis.readInt();
		this.uvIndices = new VarIntArray(uvIndices);
		
		if(numCornerUVs > 0) {
			int[] cornerUVIndices = new int[numFaces * 4];
			for(int i = 0; i < cornerUVIndices.length; ++i)
				cornerUVIndices[i] = dis.readInt();
			this.cornerUVIndices = new VarIntArray(cornerUVIndices);
		}else {
			this.cornerUVIndices = new VarIntArray(2);
		}
		
		int[] normalIndices = new int[numFaces];
		for(int i = 0; i < normalIndices.length; ++i)
			normalIndices[i] = dis.readInt();
		this.normalIndices = new VarIntArray(normalIndices);
		
		int numColorSets = dis.readInt();
		for(int i = 0; i < numColorSets; ++i) {
			VertexColorSet colorSet = new VertexColorSet(dis);
			if(colorSet.getName().equals("Cd")) {
				this.colors = colorSet;
				this.hasColors = true;
				registerColorSetName(this.colors.getName());
			}else if(colorSet.getName().equals("CdAO")) {
				this.ao = colorSet;
				this.hasAO = true;
				registerColorSetName(this.ao.getName());
			}else {
				if(this.additionalColorSets == null)
					this.additionalColorSets = new ArrayList<VertexColorSet>();
				this.additionalColorSets.add(colorSet);
				registerColorSetName(colorSet.getName());
			}
		}
		
		//this.vertexCache = new IndexCacheDoubleLong();
		//this.faceCache = new FaceCache();
		
		int numSubsets = dis.readInt();
		if(numSubsets > 0) {
			this.subsets = new ArrayList<MeshSubset>();
			for(int i = 0; i < numSubsets; ++i) {
				MeshSubset subset = new MeshSubset(dis);
				this.subsets.add(subset);
				if(subset.getPurpose() == MeshPurpose.PROXY)
					hasProxySubsets = true;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public MeshPurpose getPurpose() {
		return purpose;
	}
	
	public void setPurpose(MeshPurpose purpose) {
		this.purpose = purpose;
	}

	public boolean hasPurpose(MeshPurpose purpose) {
		if(this.purpose == purpose)
			return true;
		if(subsets != null) {
			for(MeshSubset subset : subsets)
				if(subset.getPurpose() == purpose)
					return true;
		}
		return false;
	}
	
	public String getTexture() {
		return texture;
	}
	
	public void setTexture(String texture, boolean animatedTexture) {
		this.texture = texture;
		this.animatedTexture = animatedTexture;
	}
	
	public String getMatTexture() {
		return matTexture;
	}
	
	public void setMatTexture(String texture) {
		this.matTexture = texture;
	}
	
	public boolean hasAnimatedTexture() {
		return animatedTexture;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}
	
	public Set<String> getColorSetNames(){
		return colorSetNames;
	}
	
	protected void registerColorSetName(String name) {
		if(colorSetNames == null)
			colorSetNames = new HashSet<String>();
		colorSetNames.add(name);
	}
	
	public void validateSubsets() {
		// If this mesh isn't using any subsets, no need to do anything.
		if(getNumSubsets() == 0)
			return;
		
		// Let's make sure that every face is in exactly one subset.
		int[] subsetIdsProxy = new int[faceIndices.size()/4];
		Arrays.fill(subsetIdsProxy, -1);
		int[] subsetIdsRender = new int[faceIndices.size()/4];
		Arrays.fill(subsetIdsRender, -1);
		boolean hasProxySubsets = false;
		boolean hasRenderSubsets = false;
		int numSubsets = getNumSubsets();
		for(int i = 0; i < numSubsets; ++i) {
			MeshSubset subset = getSubset(i);
			int numFaces = subset.getFaceIndices().size();
			for(int j = 0; j < numFaces; ++j) {
				int faceIndex = subset.getFaceIndices().get(j);
				if(subset.getPurpose() == MeshPurpose.PROXY || subset.getPurpose() == MeshPurpose.UNDEFINED) {
					if(subsetIdsProxy[faceIndex] != -1) {
						System.out.println("Face assigned to multiple subsets: " + faceIndex);
					}else {
						subsetIdsProxy[faceIndex] = i;
					}
					hasProxySubsets = true;
				}
				if(subset.getPurpose() == MeshPurpose.RENDER || subset.getPurpose() == MeshPurpose.UNDEFINED) {
					if(subsetIdsRender[faceIndex] != -1) {
						System.out.println("Face assigned to multiple subsets: " + faceIndex);
					}else {
						subsetIdsRender[faceIndex] = i;
					}
					hasRenderSubsets = true;
				}
			}
			subset.getFaceIndices().clear();
		}
		MeshSubset leftOverSubset = null;
		MeshSubset leftOverSubsetProxy = null;
		MeshSubset leftOverSubsetRender = null;
		for(int i = 0; i < subsetIdsProxy.length; ++i) {
			int subsetIdProxy = subsetIdsProxy[i];
			int subsetIdRender = subsetIdsRender[i];
			if((subsetIdProxy == -1 && hasProxySubsets) || (subsetIdRender == -1 && hasRenderSubsets)) {
				System.out.println("Face not assigned to subset: " + i);
				if((subsetIdProxy == -1 && hasProxySubsets) && (subsetIdRender == -1 && hasRenderSubsets)) {
					if(leftOverSubset == null) {
						leftOverSubset = new MeshSubset("subset00", texture, matTexture, animatedTexture, MeshPurpose.UNDEFINED, false, 0, 1);
						addSubset(leftOverSubset);
					}
					leftOverSubset.getFaceIndices().add(i);
				}else if(subsetIdProxy == -1 && hasProxySubsets) {
					if(leftOverSubsetProxy == null) {
						leftOverSubsetProxy = new MeshSubset("subset00_proxy", texture, matTexture, animatedTexture, MeshPurpose.PROXY, false, 0, 1);
						addSubset(leftOverSubsetProxy);
					}
					leftOverSubsetProxy.getFaceIndices().add(i);
					
					if(hasRenderSubsets)
						getSubset(subsetIdRender).getFaceIndices().add(i);
				}else if(subsetIdRender == -1 && hasRenderSubsets) {
					if(leftOverSubsetRender == null) {
						leftOverSubsetRender = new MeshSubset("subset00_render", texture, matTexture, animatedTexture, MeshPurpose.RENDER, false, 0, 1);
						addSubset(leftOverSubsetRender);
					}
					leftOverSubsetRender.getFaceIndices().add(i);

					if(hasProxySubsets)
						getSubset(subsetIdProxy).getFaceIndices().add(i);
				}else {
					throw new RuntimeException("Unreachable code");
				}
			}else {
				if(hasProxySubsets)
					getSubset(subsetIdProxy).getFaceIndices().add(i);
				if(hasRenderSubsets)
					getSubset(subsetIdRender).getFaceIndices().add(i);
			}
		}
	}
	
	public int estimateMemoryUsage() {
		int memory = 0;
		memory += vertices.getMemoryUsage();
		memory += uvs.getMemoryUsage();
		if(cornerUVs != null)
			memory += cornerUVs.getMemoryUsage();
		memory += normals.getMemoryUsage();
		memory += faceIndices.getMemoryUsage();
		memory += uvIndices.getMemoryUsage();
		if(cornerUVIndices != null)
			memory += cornerUVIndices.getMemoryUsage();
		memory += normalIndices.getMemoryUsage();
		if(colors != null)
			memory += colors.getMemoryUsage();
		if(ao != null)
			memory += ao.getMemoryUsage();
		if(additionalColorSets != null)
			for(VertexColorSet vcs : additionalColorSets)
				memory += vcs.getMemoryUsage();
		return memory;
	}

}
