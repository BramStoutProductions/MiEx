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
import java.util.Arrays;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.Occlusion;

public class Mesh {
	
	private String name;
	private String texture;
	private String matTexture;
	private boolean animatedTexture;
	private String extraData;
	private FloatArray vertices;
	private FloatArray us;
	private FloatArray vs;
	private FloatArray cornerUVs;
	private FloatArray colors;
	private FloatArray normals;
	private FloatArray ao;
	private IntArray faceIndices;
	private IntArray faceCounts;
	private IntArray uvIndices;
	private IntArray cornerUVIndices;
	private IntArray colorIndices;
	private IntArray normalIndices;
	private IntArray aoIndices;
	private FloatArray faceCenters;
	private boolean doubleSided;
	private boolean hasColors;
	private IndexCache vertexCache;
	private FaceCache faceCache;
	//private IndexCache normalCache;
	
	public Mesh() {
		this("", "", "", false, false, 6, 4);
	}
	
	public Mesh(String name, String texture, String matTexture, boolean animatedTexture, 
				boolean doubleSided, int largeCapacity, int smallCapacity) {
		this.name = name;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.extraData = "";
		this.vertices = new FloatArray(largeCapacity*3);
		this.us = new FloatArray(smallCapacity);
		this.vs = new FloatArray(smallCapacity);
		this.cornerUVs = new FloatArray(smallCapacity*2);
		this.colors = null;
		this.normals = new FloatArray(smallCapacity*3);
		this.ao = new FloatArray(smallCapacity);
		this.faceIndices = new IntArray(largeCapacity*4);
		this.faceCounts = new IntArray(largeCapacity);
		this.uvIndices = new IntArray(largeCapacity*4);
		this.cornerUVIndices = new IntArray(largeCapacity*4);
		this.colorIndices = null;
		this.normalIndices = new IntArray(largeCapacity*4);
		this.aoIndices = new IntArray(largeCapacity*4);
		this.faceCenters = new FloatArray(largeCapacity*4);
		this.doubleSided = doubleSided;
		this.vertexCache = new IndexCache();
		this.hasColors = false;
		this.faceCache = new FaceCache();
		//this.normalCache = new IndexCache();
	}
	
	public void reset(String name, String texture, String matTexture, boolean animatedTexture, boolean doubleSided) {
		this.name = name;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.extraData = "";
		this.vertices.clear();
		this.us.clear();
		this.vs.clear();
		this.cornerUVs.clear();
		if(this.colors != null)
			this.colors.clear();
		this.normals.clear();
		this.ao.clear();
		this.faceIndices.clear();
		this.faceCounts.clear();
		this.uvIndices.clear();
		this.cornerUVIndices.clear();
		if(this.colorIndices != null)
			this.colorIndices.clear();
		this.normalIndices.clear();
		this.aoIndices.clear();
		this.faceCenters.clear();
		this.doubleSided = doubleSided;
		this.vertexCache.clear();
		this.hasColors = false;
		this.faceCache.clear();
	}
	
	public Mesh(String name, String texture, String matTexture, boolean animatedTexture, boolean doubleSided, String extraData, 
				float[] vertices, float[] uvs, float[] cornerUVs, float[] colors, float[] normals, float[] ao, int[] faceIndices,
				int[] faceCounts, int[] uvIndices, int[] cornerUVIndices, int[] colorIndices, int[] normalIndices,
				int[] aoIndices) {
		this.name = name;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.extraData = extraData;
		this.vertices = new FloatArray(vertices);
		this.us = new FloatArray(uvs.length / 2);
		this.vs = new FloatArray(uvs.length / 2);
		for(int i = 0; i < uvs.length / 2; ++i) {
			this.us.set(i, uvs[i*2]);
			this.vs.set(i, uvs[i*2+1]);
		}
		this.cornerUVs = new FloatArray(cornerUVs);
		this.colors = null;
		if(colors != null)
			this.colors = new FloatArray(colors);
		this.normals = new FloatArray(normals);
		this.ao = new FloatArray(ao);
		this.faceIndices = new IntArray(faceIndices);
		this.faceCounts = new IntArray(faceCounts);
		this.uvIndices = new IntArray(uvIndices);
		this.cornerUVIndices = new IntArray(cornerUVIndices);
		this.colorIndices = null;
		if(colorIndices != null)
			this.colorIndices = new IntArray(colorIndices);
		this.normalIndices = new IntArray(normalIndices);
		this.aoIndices = new IntArray(aoIndices);
		this.faceCenters = new FloatArray();
		this.doubleSided = doubleSided;
		this.vertexCache = new IndexCache();
		this.hasColors = this.colors != null;
		this.faceCache = new FaceCache();
		//this.normalCache = new IndexCache();
		
		for(int i = 0; i < this.vertices.size(); i += 3) {
			this.vertexCache.put(calcVertexId(this.vertices.get(i), this.vertices.get(i+1), this.vertices.get(i+2)), i/3);
		}
		//for(int i = 0; i < this.normals.size(); i += 3) {
		//	this.normalCache.put(calcVertexId(this.normals.get(i)*16f, this.normals.get(i+1)*16f, this.normals.get(i+2)*16f), i/3);
		//}
		for(int i = 0; i < this.faceIndices.size(); i += 4) {
			int v0 = this.faceIndices.get(i);
			int v1 = this.faceIndices.get(i + 1);
			int v2 = this.faceIndices.get(i + 2);
			int v3 = this.faceIndices.get(i + 3);
			faceCache.register(v0, v1, v2, v3);
			this.faceCenters.add((this.vertices.get(v0*3) + this.vertices.get(v2*3)) / 2f);
			this.faceCenters.add((this.vertices.get(v0*3+1) + this.vertices.get(v2*3+1)) / 2f);
			this.faceCenters.add((this.vertices.get(v0*3+2) + this.vertices.get(v2*3+2)) / 2f);
			this.faceCenters.add(1f);
		}
	}
	
	private long packVertexId(long x, long y, long z) {
		return  (((x >> 0)  & 7) << 61) | (((y >> 0)  & 7) << 58) | (((z >> 0)  & 7) << 55) | 
				(((x >> 3)  & 7) << 52) | (((y >> 3)  & 7) << 49) | (((z >> 3)  & 7) << 46) | 
				(((x >> 6)  & 7) << 43) | (((y >> 6)  & 7) << 40) | (((z >> 6)  & 7) << 37) | 
				(((x >> 9)  & 7) << 34) | (((y >> 9)  & 7) << 31) | (((z >> 9)  & 7) << 28) | 
				(((x >> 12) & 7) << 25) | (((y >> 12) & 7) << 22) | (((z >> 12) & 7) << 19) | 
				(((x >> 15) & 7) << 16) | (((y >> 15) & 7) << 13) | (((z >> 15) & 7) << 10) | 
				(((x >> 18) & 7) << 7)  | (((y >> 18) & 7) << 4)  | (((z >> 18) & 7) << 1);
	}
	
	private long calcVertexId(float x, float y, float z) {
		// We compact the three floats into a single 64 bit integer
		return packVertexId(Float.floatToRawIntBits(x) >>> 11,
							Float.floatToRawIntBits(y) >>> 11,
							Float.floatToRawIntBits(z) >>> 11);
	}
	
	public void addPoint(float x, float y, float z, float u, float v, float cornerU, float cornerV, 
						float r, float g, float b, float ao, int[] out) {
		int vertexIndex = -1;
		long hash = calcVertexId(x, y, z);
		vertexIndex = this.vertexCache.getOrDefault(hash, -1);
		
		int uvIndex = -1;
		float[] uData = us.getData();
		float[] vData = vs.getData();
		int uvsSize = us.size();
		for(int i = 0; i < uvsSize; ++i) {
			if(uData[i] == u && vData[i] == v) {
				uvIndex = i;
				break;
			}
		}
		
		int cornerUVIndex = -1;
		float[] cornerUVData = cornerUVs.getData();
		int cornerUVsSize = cornerUVs.size();
		for(int i = 0; i < cornerUVsSize; i += 2) {
			if(Math.abs(cornerUVData[i] - cornerU) < 0.00001f && Math.abs(cornerUVData[i + 1] - cornerV) < 0.00001f) {
				cornerUVIndex = i/2;
				break;
			}
		}
		
		int colorIndex = -2;
		if(hasColors) {
			colorIndex = -1;
			float[] colorData = colors.getData();
			int colorsSize = colors.size();
			for(int i = colorsSize - 3; i >= 0; i -= 3) {
				if(Math.abs(colorData[i] - r) < 0.00001f && 
						Math.abs(colorData[i + 1] - g) < 0.00001f && 
						Math.abs(colorData[i + 2] - b) < 0.00001f) {
					colorIndex = i / 3;
					break;
				}
			}
		}
		
		int aoIndex = -1;
		float[] aoData = this.ao.getData();
		int aoSize = this.ao.size();
		for(int i = 0; i < aoSize; ++i) {
			if(Math.abs(aoData[i] - ao) < 0.005f) {
				aoIndex = i;
				break;
			}
		}
		
		if(vertexIndex == -1) {
			vertexIndex = vertices.size() / 3;
			vertices.add(x);
			vertices.add(y);
			vertices.add(z);
			this.vertexCache.put(hash, vertexIndex);
		}
		
		if(uvIndex == -1) {
			uvIndex = us.size();
			us.add(u);
			vs.add(v);
		}
		
		if(cornerUVIndex == -1) {
			cornerUVIndex = cornerUVs.size() / 2;
			cornerUVs.add(cornerU);
			cornerUVs.add(cornerV);
		}
		
		if(colorIndex == -1) {
			colorIndex = colors.size() / 3;
			colors.add(r);
			colors.add(g);
			colors.add(b);
		}
		
		if(aoIndex == -1) {
			aoIndex = this.ao.size();
			this.ao.add(ao);
		}
		
		out[0] = vertexIndex;
		out[1] = uvIndex;
		out[2] = colorIndex;
		out[3] = aoIndex;
		out[4] = cornerUVIndex;
	}
	
	private void forceAddPoint(float x, float y, float z, int[] out) {
		int vertexIndex = vertices.size() / 3;
		vertices.add(x);
		vertices.add(y);
		vertices.add(z);
		out[0] = vertexIndex;
	}
	
	public void addFaceVertex(int[] v0) {
		faceIndices.add(v0[0]);
		
		uvIndices.add(v0[1]);
		
		if(v0[2] >= 0)
			colorIndices.add(v0[2]);
		
		aoIndices.add(v0[3]);
		
		cornerUVIndices.add(v0[4]);
	}
	
	public int addNormal(float x, float y, float z) {
		int normalIndex = -1;
		//long hash = calcVertexId(x*16f, y*16f, z*16f);
		//normalIndex = this.normalCache.getOrDefault(hash, -1);
		
		if(normalIndex == -1) {
			normalIndex = normals.size() / 3;
			normals.add(x);
			normals.add(y);
			normals.add(z);
			//this.normalCache.put(hash, normalIndex);
		}
		
		return normalIndex;
	}
	
	private static final float[] blankColors = new float[] {
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f
	};
	
	public void addFace(ModelFace face, float bx, float by, float bz, Atlas.AtlasItem atlas, Color tint, int cornerData) {
		addFace(face, bx, by, bz, 0f, 0f, 0f, 0f, 1.0f, 1.0f, 1.0f, 1.0f, atlas, tint, 0, cornerData);
	}
	
	private float[] normalData = new float[3];
	private float[] cornerUVData = new float[8];
	private int[] v0Data = new int[5];
	private int[] v1Data = new int[5];
	private int[] v2Data = new int[5];
	private int[] v3Data = new int[5];
	public void addFace(ModelFace face, float bx, float by, float bz, float additionalX, float additionalY, float additionalZ,
			float uvOffsetY, float scale, float yScale, float uvScale, float yuvScale, Atlas.AtlasItem atlas, Color tint,
			long ambientOcclusion, int cornerData) {
		float ox = bx * 16.0f + additionalX;
		float oy = by * 16.0f + additionalY;
		float oz = bz * 16.0f + additionalZ;
		float[] points = face.getPoints();
		float[] uvs = face.getUVs();
		float[] colors = face.getVertexColors();
		if(colors != null || tint != null){
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new FloatArray();
					this.colorIndices = new IntArray();
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colorIndices.add(0);
				}
				hasColors = true;
			}
		}
		if(colors == null) {
			colors = blankColors;
		}
		if(tint != null) {
			colors = colors.clone();
			colors[0] *= tint.getR();
			colors[1] *= tint.getG();
			colors[2] *= tint.getB();
			
			colors[3] *= tint.getR();
			colors[4] *= tint.getG();
			colors[5] *= tint.getB();
			
			colors[6] *= tint.getR();
			colors[7] *= tint.getG();
			colors[8] *= tint.getB();
			
			colors[9] *= tint.getR();
			colors[10] *= tint.getG();
			colors[11] *= tint.getB();
		}
		if(atlas != null) {
			uvs = Arrays.copyOf(uvs, uvs.length);
			for(int i = 0; i < uvs.length; i += 2) {
				uvs[i] = (uvs[i] + atlas.x * 16.0f) / atlas.width;
				uvs[i+1] = (uvs[i+1] + (atlas.height - atlas.y - ((float) atlas.padding)) * 16.0f) / atlas.height;
			}
		}
		// Scale the UVs
		if(uvScale != 1.0f || yuvScale != 1.0f) {
			uvs = Arrays.copyOf(uvs, uvs.length);
			float pivotU = Math.min(uvs[0], uvs[4]);
			float pivotV = Math.min(uvs[1], uvs[5]);
			for(int i = 0; i < uvs.length; i += 2) {
				uvs[i] = (uvs[i] - pivotU) * uvScale + pivotU;
				uvs[i + 1] = (uvs[i + 1] - pivotV) * yuvScale + pivotV;
			}
		}
		
		float ao0 = getAOForPoint(points[0], points[1], points[2], ambientOcclusion, face.getDirection());
		float ao1 = getAOForPoint(points[3], points[4], points[5], ambientOcclusion, face.getDirection());
		float ao2 = getAOForPoint(points[6], points[7], points[8], ambientOcclusion, face.getDirection());
		float ao3 = getAOForPoint(points[9], points[10], points[11], ambientOcclusion, face.getDirection());
		
		Occlusion.getCornerUVsForIndex(cornerData, cornerUVData);
		
		addPoint((points[0] - 8f) * scale + 8f + ox, (points[1] - 8f) * yScale + 8f + oy, (points[2] - 8f) * scale + 8f + oz, 
				uvs[0] / 16.0f, uvs[1] / 16.0f + uvOffsetY, cornerUVData[0], cornerUVData[1],
				colors[0], colors[1], colors[2], ao0, v0Data);
		addPoint((points[3] - 8f) * scale + 8f + ox, (points[4] - 8f) * yScale + 8f + oy, (points[5] - 8f) * scale + 8f + oz, 
				uvs[2] / 16.0f, uvs[3] / 16.0f + uvOffsetY, cornerUVData[2], cornerUVData[3], 
				colors[3], colors[4], colors[5], ao1, v1Data);
		addPoint((points[6] - 8f) * scale + 8f + ox, (points[7] - 8f) * yScale + 8f + oy, (points[8] - 8f) * scale + 8f + oz, 
				uvs[4] / 16.0f, uvs[5] / 16.0f + uvOffsetY, cornerUVData[4], cornerUVData[5],
				colors[6], colors[7], colors[8], ao2, v2Data);
		addPoint((points[9] - 8f) * scale + 8f + ox, (points[10] - 8f) * yScale + 8f + oy, (points[11] - 8f) * scale + 8f + oz, 
				uvs[6] / 16.0f, uvs[7] / 16.0f + uvOffsetY, cornerUVData[6], cornerUVData[7],
				colors[9], colors[10], colors[11], ao3, v3Data);
		
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
		
		boolean faceAlreadyExists = faceCache.register(v0Data[0], v1Data[0], v2Data[0], v3Data[0]);
		if(faceAlreadyExists) {
			// Faces that share all edges/vertices can cause issues,
			// so we want to duplicate the vertices then.
			forceAddPoint((points[0] - 8f) * scale + 8f + ox, (points[1] - 8f) * yScale + 8f + oy, 
							(points[2] - 8f) * scale + 8f + oz, v0Data);
			forceAddPoint((points[3] - 8f) * scale + 8f + ox, (points[4] - 8f) * yScale + 8f + oy, 
							(points[5] - 8f) * scale + 8f + oz, v1Data);
			forceAddPoint((points[6] - 8f) * scale + 8f + ox, (points[7] - 8f) * yScale + 8f + oy, 
							(points[8] - 8f) * scale + 8f + oz, v2Data);
			forceAddPoint((points[9] - 8f) * scale + 8f + ox, (points[10] - 8f) * yScale + 8f + oy, 
							(points[11] - 8f) * scale + 8f + oz, v3Data);
		}
		
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		faceCounts.add(4);
		
		face.calculateNormal(normalData);
		int normalIndex = addNormal(normalData[0], normalData[1], normalData[2]);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		
		faceCenters.add(bx * 16.0f);
		faceCenters.add(by * 16.0f);
		faceCenters.add(bz * 16.0f);
		faceCenters.add(scale);
		
		if(face.isDoubleSided())
			doubleSided = true;
	}
	
	private float getAOForPoint(float x, float y, float z, long ao, Direction dir) {
		long ao0 = 0;
		long ao1 = 0;
		float t0 = 0f;
		float t1 = 0f;
		float t2 = 0f;
		switch(dir) {
		case NORTH:
		case SOUTH:
			ao0 = (ao >>> (Direction.NORTH.id * 10)) & 0b1111111111L;
			ao1 = (ao >>> (Direction.SOUTH.id * 10)) & 0b1111111111L;
			t0 = x;
			t1 = y;
			t2 = z;
			break;
		case EAST:
		case WEST:
			ao0 = (ao >>> (Direction.WEST.id * 10)) & 0b1111111111L;
			ao1 = (ao >>> (Direction.EAST.id * 10)) & 0b1111111111L;
			t0 = z;
			t1 = y;
			t2 = x;
			break;
		case UP:
		case DOWN:
			ao0 = (ao >>> (Direction.DOWN.id * 10)) & 0b1111111111L;
			ao1 = (ao >>> (Direction.UP.id * 10)) & 0b1111111111L;
			t0 = x;
			t1 = z;
			t2 = y;
			break;
		default:
			break;
		}
		t0 = Math.min(Math.max(t0 / 16f, 0f), 1f);
		t1 = Math.min(Math.max(t1 / 16f, 0f), 1f);
		t2 = Math.min(Math.max(t2 / 16f, 0f), 1f);
		
		float ao000 = ((float) ((ao0      ) & 0b11)) / 3f;
		float ao010 = ((float) ((ao0 >>> 2) & 0b11)) / 3f;
		float ao100 = ((float) ((ao0 >>> 4) & 0b11)) / 3f;
		float ao110 = ((float) ((ao0 >>> 6) & 0b11)) / 3f;
		float ao001 = ((float) ((ao1      ) & 0b11)) / 3f;
		float ao011 = ((float) ((ao1 >>> 2) & 0b11)) / 3f;
		float ao101 = ((float) ((ao1 >>> 4) & 0b11)) / 3f;
		float ao111 = ((float) ((ao1 >>> 6) & 0b11)) / 3f;
		
		float ao00 = lerp(ao000, ao001, t2);
		float ao01 = lerp(ao010, ao011, t2);
		float ao10 = lerp(ao100, ao101, t2);
		float ao11 = lerp(ao110, ao111, t2);
		
		float ao0f = lerp(ao00, ao01, t0);
		float ao1f = lerp(ao10, ao11, t0);
		
		float aof = lerp(ao0f, ao1f, t1);
		
		aof = 1f - aof;
		
		return (float) Math.floor(aof * 100f + 0.5f) / 100f;
	}
	
	private float lerp(float a, float b, float t) {
		return a * (1f-t) + b * t;
	}
	
	public void getVertex(int faceIndex, int vertexIndex, float[] out) {
		int vertexId = faceIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = vertices.get(vertexId * 3 + 0);
		out[1] = vertices.get(vertexId * 3 + 1);
		out[2] = vertices.get(vertexId * 3 + 2);
	}
	
	public void getUV(int faceIndex, int vertexIndex, float[] out) {
		int uvId = uvIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = us.get(uvId);
		out[1] = vs.get(uvId);
	}
	
	public void getCornerUV(int faceIndex, int vertexIndex, float[] out) {
		int cornerUVId = cornerUVIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = cornerUVs.get(cornerUVId * 2);
		out[1] = cornerUVs.get(cornerUVId * 2 + 1);
	}
	
	public void getColor(int faceIndex, int vertexIndex, float[] out) {
		if(!hasColors) {
			out[0] = 1.0f;
			out[1] = 1.0f;
			out[2] = 1.0f;
			return;
		}
		int colorId = colorIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = colors.get(colorId * 3 + 0);
		out[1] = colors.get(colorId * 3 + 1);
		out[2] = colors.get(colorId * 3 + 2);
	}
	
	public void getNormal(int faceIndex, int vertexIndex, float[] out) {
		int normalId = normalIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = normals.get(normalId * 3 + 0);
		out[1] = normals.get(normalId * 3 + 1);
		out[2] = normals.get(normalId * 3 + 2);
	}
	
	public float getAO(int faceIndex, int vertexIndex) {
		int aoId = aoIndices.get(faceIndex * 4 + vertexIndex);
		return ao.get(aoId);
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
	public void addFaceFromMesh(Mesh mesh, int index) {
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
		float ao0 = mesh.getAO(index, 0);
		float ao1 = mesh.getAO(index, 1);
		float ao2 = mesh.getAO(index, 2);
		float ao3 = mesh.getAO(index, 3);
		
		if(mesh.hasColors) {
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new FloatArray();
					this.colorIndices = new IntArray();
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colorIndices.add(0);
				}
				hasColors = true;
			}
		}
		
		addPoint(_v0[0], _v0[1], _v0[2], _uv0[0], _uv0[1], _corneruv0[0], _corneruv0[1], 
					_color0[0], _color0[1], _color0[2], ao0, v0Data);
		addPoint(_v1[0], _v1[1], _v1[2], _uv1[0], _uv1[1], _corneruv1[0], _corneruv1[1],
					_color1[0], _color1[1], _color1[2], ao1, v1Data);
		addPoint(_v2[0], _v2[1], _v2[2], _uv2[0], _uv2[1], _corneruv2[0], _corneruv2[1],
					_color2[0], _color2[1], _color2[2], ao2, v2Data);
		addPoint(_v3[0], _v3[1], _v3[2], _uv3[0], _uv3[1], _corneruv3[0], _corneruv3[1],
					_color3[0], _color3[1], _color3[2], ao3, v3Data);
		
		if(v0Data[0] == v1Data[0] || v0Data[0] == v2Data[0] || v0Data[0] == v3Data[0] ||
				v1Data[0] == v2Data[0] || v1Data[0] == v3Data[0] || v2Data[0] == v3Data[0]) {
			throw new RuntimeException("Face contains duplicate vertex");
			//return;
		}
		if(v0Data[1] == v1Data[1] || v0Data[1] == v2Data[1] || v0Data[1] == v3Data[1] ||
				v1Data[1] == v2Data[1] || v1Data[1] == v3Data[1] || v2Data[1] == v3Data[1]) {
			throw new RuntimeException("Face contains duplicate UV vertex");
			//return;
		}
		
		boolean faceAlreadyExists = faceCache.register(v0Data[0], v1Data[0], v2Data[0], v3Data[0]);
		if(faceAlreadyExists) {
			// Faces that share all edges/vertices can cause issues,
			// so we want to duplicate the vertices then.
			forceAddPoint(_v0[0], _v0[1], _v0[2], v0Data);
			forceAddPoint(_v1[0], _v1[1], _v1[2], v1Data);
			forceAddPoint(_v2[0], _v2[1], _v2[2], v2Data);
			forceAddPoint(_v3[0], _v3[1], _v3[2], v3Data);
		}
		
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		faceCounts.add(4);
		
		int normalIndex = addNormal(_normal[0], _normal[1], _normal[2]);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		
		faceCenters.add(mesh.getFaceCenters().get(index * 4));
		faceCenters.add(mesh.getFaceCenters().get(index * 4 + 1));
		faceCenters.add(mesh.getFaceCenters().get(index * 4 + 2));
		faceCenters.add(mesh.getFaceCenters().get(index * 4 + 3));
	}
	
	public void addFace(float[] vertices, float[] us, float[] vs, float[] cornerUVs, float[] normal, float[] color, float[] ao) {
		if(color != null) {
			if(!hasColors) {
				if(this.colors == null) {
					this.colors = new FloatArray();
					this.colorIndices = new IntArray();
				}
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.faceIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.faceIndices.size(); ++i)
						this.colorIndices.add(0);
				}
				hasColors = true;
			}
		}
		
		if(color != null) {
			addPoint(vertices[0], vertices[1], vertices[2],   us[0], vs[0], cornerUVs[0], cornerUVs[1], 
						color[0], color[1], color[2], ao[0], v0Data);
			addPoint(vertices[3], vertices[4], vertices[5],   us[1], vs[1], cornerUVs[2], cornerUVs[3],
						color[0], color[1], color[2], ao[1], v1Data);
			addPoint(vertices[6], vertices[7], vertices[8],   us[2], vs[2], cornerUVs[4], cornerUVs[5],
						color[0], color[1], color[2], ao[2], v2Data);
			addPoint(vertices[9], vertices[10], vertices[11], us[3], vs[3], cornerUVs[6], cornerUVs[7],
						color[0], color[1], color[2], ao[3], v3Data);
		}else {
			addPoint(vertices[0], vertices[1], vertices[2],   us[0], vs[0], cornerUVs[0], cornerUVs[1],
						1f, 1f, 1f, ao[0], v0Data);
			addPoint(vertices[3], vertices[4], vertices[5],   us[1], vs[1], cornerUVs[2], cornerUVs[3],
						1f, 1f, 1f, ao[1], v1Data);
			addPoint(vertices[6], vertices[7], vertices[8],   us[2], vs[2], cornerUVs[4], cornerUVs[5],
						1f, 1f, 1f, ao[2], v2Data);
			addPoint(vertices[9], vertices[10], vertices[11], us[3], vs[3], cornerUVs[6], cornerUVs[7],
						1f, 1f, 1f, ao[3], v3Data);
		}
		
		if(v0Data[0] == v1Data[0] || v0Data[0] == v2Data[0] || v0Data[0] == v3Data[0] ||
				v1Data[0] == v2Data[0] || v1Data[0] == v3Data[0] || v2Data[0] == v3Data[0]) {
			throw new RuntimeException("Face contains duplicate vertex");
			//return;
		}
		if(v0Data[1] == v1Data[1] || v0Data[1] == v2Data[1] || v0Data[1] == v3Data[1] ||
				v1Data[1] == v2Data[1] || v1Data[1] == v3Data[1] || v2Data[1] == v3Data[1]) {
			throw new RuntimeException("Face contains duplicate UV vertex");
			//return;
		}
		
		boolean faceAlreadyExists = faceCache.register(v0Data[0], v1Data[0], v2Data[0], v3Data[0]);
		if(faceAlreadyExists) {
			// Faces that share all edges/vertices can cause issues,
			// so we want to duplicate the vertices then.
			forceAddPoint(vertices[0], vertices[1], vertices[2], v0Data);
			forceAddPoint(vertices[3], vertices[4], vertices[5], v1Data);
			forceAddPoint(vertices[6], vertices[7], vertices[8], v2Data);
			forceAddPoint(vertices[9], vertices[10], vertices[11], v3Data);
		}
		
		addFaceVertex(v0Data);
		addFaceVertex(v1Data);
		addFaceVertex(v2Data);
		addFaceVertex(v3Data);
		faceCounts.add(4);
		
		int normalIndex = addNormal(normal[0], normal[1], normal[2]);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		
		faceCenters.add((vertices[0] + vertices[6]) / 2f);
		faceCenters.add((vertices[1] + vertices[7]) / 2f);
		faceCenters.add((vertices[2] + vertices[8]) / 2f);
		faceCenters.add(1f);
	}
	
	public void appendMesh(Mesh mesh) {
		for(int i = 0; i < (mesh.faceIndices.size()/4); ++i) {
			addFaceFromMesh(mesh, i);
		}
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

	public FloatArray getUs() {
		return us;
	}

	public FloatArray getVs() {
		return vs;
	}
	
	public FloatArray getCornerUVs() {
		return cornerUVs;
	}
	
	public boolean hasColors() {
		return hasColors;
	}

	public FloatArray getColors() {
		return colors;
	}
	
	public FloatArray getNormals() {
		return normals;
	}
	
	public FloatArray getAO() {
		return ao;
	}

	public IntArray getFaceIndices() {
		return faceIndices;
	}

	public IntArray getFaceCounts() {
		return faceCounts;
	}

	public IntArray getUvIndices() {
		return uvIndices;
	}
	
	public IntArray getCornerUVIndices() {
		return cornerUVIndices;
	}

	public IntArray getColorIndices() {
		return colorIndices;
	}
	
	public IntArray getNormalIndices() {
		return normalIndices;
	}
	
	public IntArray getAOIndices() {
		return aoIndices;
	}

	public FloatArray getFaceCenters() {
		return faceCenters;
	}

	public void write(LargeDataOutputStream dos) throws IOException {
		dos.writeByte(1); // Mesh type : Mesh
		dos.writeUTF(name);
		dos.writeInt(doubleSided ? 1 : 0);
		dos.writeUTF(texture);
		dos.writeUTF(matTexture);
		dos.writeUTF(extraData);
		dos.writeInt(vertices.size() / 3); // num vertices
		dos.writeInt(us.size()); // num UVs
		if(Config.calculateCornerUVs)
			dos.writeInt(cornerUVs.size()/2); // nul corner UVs
		else
			dos.writeInt(0);
		dos.writeInt(normals.size() / 3); // num normals
		dos.writeInt(ao.size()); // num AO
		dos.writeInt(faceIndices.size() / 4); // num faces
		if(!hasColors) {
			dos.writeInt(0); // No vertex colours
		}else {
			dos.writeInt(colors.size() / 3); // Vertex colours
		}
		
		// Pretty much all of the code assumes that a block is 16 units.
		// In order to not break any of that, we do the scaling here.
		float worldScale = Config.blockSizeInUnits / 16.0f;
		
		// vertex data
		int i = 0;
		for(i = 0; i < vertices.size(); ++i)
			dos.writeFloat(vertices.get(i) * worldScale);
		// uv data
		for(i = 0; i < us.size(); ++i)
			dos.writeFloat(us.get(i));
		for(i = 0; i < vs.size(); ++i)
			dos.writeFloat(vs.get(i));
		// corner uv data
		if(Config.calculateCornerUVs)
			for(i = 0; i < cornerUVs.size(); ++i)
				dos.writeFloat(cornerUVs.get(i));
		// normal data
		for(i = 0; i < normals.size(); ++i)
			dos.writeFloat(normals.get(i));
		// AO data
		for(i = 0; i < ao.size(); ++i)
			dos.writeFloat(ao.get(i));
		// color data
		if(hasColors) {
			if(Config.vertexColorGamma != 1f)
				for(i = 0; i < colors.size(); ++i)
					dos.writeFloat((float) Math.pow(colors.get(i), Config.vertexColorGamma));
			else
				for(i = 0; i < colors.size(); ++i)
					dos.writeFloat(colors.get(i));
		}
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
		// AO index data
		for(i = 0; i < aoIndices.size(); ++i)
			dos.writeInt(aoIndices.get(i));
		if(hasColors) {
			for(i = 0; i < colorIndices.size(); ++i)
				dos.writeInt(colorIndices.get(i));
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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

}
