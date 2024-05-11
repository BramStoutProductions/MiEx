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
import nl.bramstout.mcworldexporter.model.ModelFace;

public class Mesh {
	
	private String name;
	private String texture;
	private String extraData;
	private FloatArray vertices;
	private FloatArray us;
	private FloatArray vs;
	private FloatArray colors;
	private FloatArray normals;
	private IntArray edges;
	private IntArray edgeIndices;
	private IntArray uvIndices;
	private IntArray colorIndices;
	private IntArray normalIndices;
	private FloatArray faceCenters;
	private int maxEdgeIndex;
	private boolean doubleSided;
	private IndexCache vertexCache;
	private IndexCache edgeCache;
	private IndexCache normalCache;
	
	public Mesh() {
		this("", "", false);
	}
	
	public Mesh(String name, String texture, boolean doubleSided) {
		this.name = name;
		this.texture = texture;
		this.extraData = "";
		this.vertices = new FloatArray();
		this.us = new FloatArray();
		this.vs = new FloatArray();
		this.colors = null;
		this.normals = new FloatArray();
		this.edges = new IntArray();
		this.edgeIndices = new IntArray();
		this.uvIndices = new IntArray();
		this.colorIndices = null;
		this.normalIndices = new IntArray();
		this.faceCenters = new FloatArray();
		this.maxEdgeIndex = -1;
		this.doubleSided = doubleSided;
		this.vertexCache = new IndexCache();
		this.edgeCache = new IndexCache();
		this.normalCache = new IndexCache();
	}
	
	public Mesh(String name, String texture, boolean doubleSided, String extraData, float[] vertices,
				float[] uvs, float[] colors, float[] normals, int[] edges,
				int[] edgeIndices, int[] uvIndices, int[] colorIndices, int[] normalIndices) {
		this.name = name;
		this.texture = texture;
		this.extraData = extraData;
		this.vertices = new FloatArray(vertices);
		this.us = new FloatArray(uvs.length / 2);
		this.vs = new FloatArray(uvs.length / 2);
		for(int i = 0; i < uvs.length / 2; ++i) {
			this.us.set(i, uvs[i*2]);
			this.vs.set(i, uvs[i*2+1]);
		}
		this.colors = null;
		if(colors != null)
			this.colors = new FloatArray(colors);
		this.normals = new FloatArray(normals);
		this.edges = new IntArray(edges);
		this.edgeIndices = new IntArray(edgeIndices);
		this.uvIndices = new IntArray(uvIndices);
		this.colorIndices = null;
		if(colorIndices != null)
			this.colorIndices = new IntArray(colorIndices);
		this.normalIndices = new IntArray(normalIndices);
		this.faceCenters = new FloatArray();
		this.maxEdgeIndex = -1;
		this.doubleSided = doubleSided;
		this.vertexCache = new IndexCache();
		this.edgeCache = new IndexCache();
		this.normalCache = new IndexCache();
		
		for(int i = 0; i < this.vertices.size(); i += 3) {
			this.vertexCache.put(calcVertexId(this.vertices.get(i), this.vertices.get(i+1), this.vertices.get(i+2)), i/3);
		}
		for(int i = 0; i < this.edges.size(); i += 3) {
			this.edgeCache.put(((long) this.edges.get(i)) << 32 | ((long) this.edges.get(i+1)), i/3);
		}
		for(int i = 0; i < this.normals.size(); i += 3) {
			this.normalCache.put(calcVertexId(this.normals.get(i), this.normals.get(i+1), this.normals.get(i+2)), i/3);
		}
	}
	
	private long calcVertexId(float x, float y, float z) {
		// We compact the three floats into a single 64 bit integer
		return ((long) (Float.floatToRawIntBits(x) >>> 11)) << 42 |
				((long) (Float.floatToRawIntBits(y) >>> 11)) << 21 |
				((long) (Float.floatToRawIntBits(z) >>> 11));
	}
	
	public void addPoint(float x, float y, float z, float u, float v, float r, float g, float b, int[] out) {
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
		
		int colorIndex = -2;
		if(colors != null) {
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
		
		if(colorIndex == -1) {
			colorIndex = colors.size() / 3;
			colors.add(r);
			colors.add(g);
			colors.add(b);
		}
		
		out[0] = vertexIndex;
		out[1] = uvIndex;
		out[2] = colorIndex;
	}
	
	public void addEdge(int[] v0, int[] v1) {
		int edgeIndex = -1;
		long edgeKey = ((long) v0[0]) << 32 | ((long) v1[0]);
		if(v0[0] <= maxEdgeIndex && v1[0] <= maxEdgeIndex) {
			edgeIndex = edgeCache.getOrDefault(edgeKey, -1);
		}
		
		if(edgeIndex == -1) {
			edgeIndex = edges.size() / 3;
			edges.add(v0[0]);
			edges.add(v1[0]);
			edges.add(0);
			maxEdgeIndex = Math.max(Math.max(v0[0], v1[0]), maxEdgeIndex);
			edgeCache.put(edgeKey, edgeIndex);
		}
		uvIndices.add(v0[1]);
		if(v0[2] >= 0)
			colorIndices.add(v0[2]);
		
		edgeIndices.add(edgeIndex);
	}
	
	public int addNormal(float x, float y, float z) {
		int normalIndex = -1;
		long hash = calcVertexId(x, y, z);
		normalIndex = this.normalCache.getOrDefault(hash, -1);
		
		if(normalIndex == -1) {
			normalIndex = normals.size() / 3;
			normals.add(x);
			normals.add(y);
			normals.add(z);
			this.normalCache.put(hash, normalIndex);
		}
		
		return normalIndex;
	}
	
	private static final float[] blankColors = new float[] {
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f
	};
	
	public void addFace(ModelFace face, float bx, float by, float bz, Atlas.AtlasItem atlas, Color tint) {
		addFace(face, bx, by, bz, 0f, 0f, 0f, atlas, tint);
	}
	
	public void addFace(ModelFace face, float bx, float by, float bz, float additionalX, float additionalY, float additionalZ, Atlas.AtlasItem atlas, Color tint) {
		addFace(face, bx, by, bz, additionalX, additionalY, additionalZ, 0f, 1.0f, 1.0f, 1.0f, 1.0f, atlas, tint);
	}
	
	private float[] normalData = new float[3];
	private int[] v0Data = new int[3];
	private int[] v1Data = new int[3];
	private int[] v2Data = new int[3];
	private int[] v3Data = new int[3];
	public void addFace(ModelFace face, float bx, float by, float bz, float additionalX, float additionalY, float additionalZ,
			float uvOffsetY, float scale, float yScale, float uvScale, float yuvScale, Atlas.AtlasItem atlas, Color tint) {
		float ox = bx * 16.0f + additionalX;
		float oy = by * 16.0f + additionalY;
		float oz = bz * 16.0f + additionalZ;
		float[] points = face.getPoints();
		float[] uvs = face.getUVs();
		float[] colors = face.getVertexColors();
		if(colors != null || tint != null){
			if(this.colors == null) {
				this.colors = new FloatArray();
				this.colorIndices = new IntArray();
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.edgeIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.edgeIndices.size(); ++i)
						this.colorIndices.add(0);
				}
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
			for(int i = 2; i < uvs.length; i += 2) {
				uvs[i] = (uvs[i] - pivotU) * uvScale + pivotU;
				uvs[i + 1] = (uvs[i + 1] - pivotV) * yuvScale + pivotV;
			}
		}
		addPoint((points[0] - 8f) * scale + 8f + ox, (points[1] - 8f) * yScale + 8f + oy, (points[2] - 8f) * scale + 8f + oz, 
				uvs[0] / 16.0f, uvs[1] / 16.0f + uvOffsetY, 
				colors[0], colors[1], colors[2], v0Data);
		addPoint((points[3] - 8f) * scale + 8f + ox, (points[4] - 8f) * yScale + 8f + oy, (points[5] - 8f) * scale + 8f + oz, 
				uvs[2] / 16.0f, uvs[3] / 16.0f + uvOffsetY, 
				colors[3], colors[4], colors[5], v1Data);
		addPoint((points[6] - 8f) * scale + 8f + ox, (points[7] - 8f) * yScale + 8f + oy, (points[8] - 8f) * scale + 8f + oz, 
				uvs[4] / 16.0f, uvs[5] / 16.0f + uvOffsetY, 
				colors[6], colors[7], colors[8], v2Data);
		addPoint((points[9] - 8f) * scale + 8f + ox, (points[10] - 8f) * yScale + 8f + oy, (points[11] - 8f) * scale + 8f + oz, 
				uvs[6] / 16.0f, uvs[7] / 16.0f + uvOffsetY, 
				colors[9], colors[10], colors[11], v3Data);
		addEdge(v0Data, v1Data);
		addEdge(v1Data, v2Data);
		addEdge(v2Data, v3Data);
		addEdge(v3Data, v0Data);
		
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
	
	public void getVertex(int faceIndex, int vertexIndex, float[] out) {
		int edgeId = edgeIndices.get(faceIndex * 4 + vertexIndex);
		int vertexId = edges.get(edgeId * 3);
		out[0] = vertices.get(vertexId * 3 + 0);
		out[1] = vertices.get(vertexId * 3 + 1);
		out[2] = vertices.get(vertexId * 3 + 2);
	}
	
	public void getUV(int faceIndex, int vertexIndex, float[] out) {
		int uvId = uvIndices.get(faceIndex * 4 + vertexIndex);
		out[0] = us.get(uvId);
		out[1] = vs.get(uvId);
	}
	
	public void getColor(int faceIndex, int vertexIndex, float[] out) {
		if(colorIndices == null || colors == null) {
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
	
	private float[] _v0 = new float[3];
	private float[] _v1 = new float[3];
	private float[] _v2 = new float[3];
	private float[] _v3 = new float[3];
	private float[] _uv0 = new float[2];
	private float[] _uv1 = new float[2];
	private float[] _uv2 = new float[2];
	private float[] _uv3 = new float[2];
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
		mesh.getColor(index, 0, _color0);
		mesh.getColor(index, 1, _color1);
		mesh.getColor(index, 2, _color2);
		mesh.getColor(index, 3, _color3);
		mesh.getNormal(index, 0, _normal);
		
		if(mesh.colors != null) {
			if(this.colors == null) {
				this.colors = new FloatArray();
				this.colorIndices = new IntArray();
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.edgeIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.edgeIndices.size(); ++i)
						this.colorIndices.add(0);
				}
			}
		}
		
		addPoint(_v0[0], _v0[1], _v0[2], _uv0[0], _uv0[1], _color0[0], _color0[1], _color0[2], v0Data);
		addPoint(_v1[0], _v1[1], _v1[2], _uv1[0], _uv1[1], _color1[0], _color1[1], _color1[2], v1Data);
		addPoint(_v2[0], _v2[1], _v2[2], _uv2[0], _uv2[1], _color2[0], _color2[1], _color2[2], v2Data);
		addPoint(_v3[0], _v3[1], _v3[2], _uv3[0], _uv3[1], _color3[0], _color3[1], _color3[2], v3Data);
		
		addEdge(v0Data, v1Data);
		addEdge(v1Data, v2Data);
		addEdge(v2Data, v3Data);
		addEdge(v3Data, v0Data);
		
		int normalIndex = addNormal(_normal[0], _normal[1], _normal[2]);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
	}
	
	public void addFace(float[] vertices, float[] us, float[] vs, float[] normal, float[] color) {
		if(color != null) {
			if(this.colors == null) {
				this.colors = new FloatArray();
				this.colorIndices = new IntArray();
				// If there is already data in here, then we need to fill in for every face so far.
				if(this.edgeIndices.size() > 0) {
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					this.colors.add(1.0f);
					for(int i = 0; i < this.edgeIndices.size(); ++i)
						this.colorIndices.add(0);
				}
			}
		}
		
		if(color != null) {
			addPoint(vertices[0], vertices[1], vertices[2],   us[0], vs[0], color[0], color[1], color[2], v0Data);
			addPoint(vertices[3], vertices[4], vertices[5],   us[1], vs[1], color[0], color[1], color[2], v1Data);
			addPoint(vertices[6], vertices[7], vertices[8],   us[2], vs[2], color[0], color[1], color[2], v2Data);
			addPoint(vertices[9], vertices[10], vertices[11], us[3], vs[3], color[0], color[1], color[2], v3Data);
		}else {
			addPoint(vertices[0], vertices[1], vertices[2],   us[0], vs[0], 1f, 1f, 1f, v0Data);
			addPoint(vertices[3], vertices[4], vertices[5],   us[1], vs[1], 1f, 1f, 1f, v1Data);
			addPoint(vertices[6], vertices[7], vertices[8],   us[2], vs[2], 1f, 1f, 1f, v2Data);
			addPoint(vertices[9], vertices[10], vertices[11], us[3], vs[3], 1f, 1f, 1f, v3Data);
		}
		
		addEdge(v0Data, v1Data);
		addEdge(v1Data, v2Data);
		addEdge(v2Data, v3Data);
		addEdge(v3Data, v0Data);
		
		int normalIndex = addNormal(normal[0], normal[1], normal[2]);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
		normalIndices.add(normalIndex);
	}
	
	public void appendMesh(Mesh mesh) {
		for(int i = 0; i < (mesh.edgeIndices.size()/4); ++i) {
			addFaceFromMesh(mesh, i);
		}
		faceCenters.add(mesh.faceCenters);
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

	public FloatArray getColors() {
		return colors;
	}
	
	public FloatArray getNormals() {
		return normals;
	}

	public IntArray getEdges() {
		return edges;
	}

	public IntArray getEdgeIndices() {
		return edgeIndices;
	}

	public IntArray getUvIndices() {
		return uvIndices;
	}

	public IntArray getColorIndices() {
		return colorIndices;
	}
	
	public IntArray getNormalIndices() {
		return normalIndices;
	}

	public FloatArray getFaceCenters() {
		return faceCenters;
	}

	public void write(LargeDataOutputStream dos) throws IOException {
		dos.writeByte(1); // Mesh type : Mesh
		dos.writeUTF(name);
		dos.writeInt(doubleSided ? 1 : 0);
		dos.writeUTF(texture);
		dos.writeUTF(extraData);
		dos.writeInt(vertices.size() / 3); // num vertices
		dos.writeInt(us.size()); // num UVs
		dos.writeInt(normals.size() / 3); // num normals
		dos.writeInt(edges.size() / 3); // num edges
		dos.writeInt(edgeIndices.size() / 4); // num faces
		if(colors == null) {
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
		// normal data
		for(i = 0; i < normals.size(); ++i)
			dos.writeFloat(normals.get(i));
		// color data
		if(colors != null) {
			for(i = 0; i < colors.size(); ++i)
				dos.writeFloat(colors.get(i));
		}
		// edge data
		for(i = 0; i < edges.size(); ++i)
			dos.writeInt(edges.get(i));
		// edge index data
		for(i = 0; i < edgeIndices.size(); ++i)
			dos.writeInt(edgeIndices.get(i));
		// uv index data
		for(i = 0; i < uvIndices.size(); ++i)
			dos.writeInt(uvIndices.get(i));
		// normal index data
		for(i = 0; i < normalIndices.size(); ++i)
			dos.writeInt(normalIndices.get(i));
		if(colorIndices != null) {
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
	
	public void setTexture(String texture) {
		this.texture = texture;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}

}
