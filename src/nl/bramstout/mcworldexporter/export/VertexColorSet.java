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

public class VertexColorSet {
	
	public static class VertexColorFace{
		
		public String name;
		public int componentCount;
		public float r0;
		public float g0;
		public float b0;
		public float a0;
		public float r1;
		public float g1;
		public float b1;
		public float a1;
		public float r2;
		public float g2;
		public float b2;
		public float a2;
		public float r3;
		public float g3;
		public float b3;
		public float a3;
		
		public VertexColorFace() {
			this.name = "";
		}
		
		public VertexColorFace(String name, int componentCount,
				float r0, float g0, float b0, float a0,
				float r1, float g1, float b1, float a1,
				float r2, float g2, float b2, float a2,
				float r3, float g3, float b3, float a3) {
			this.name = name;
			this.componentCount = componentCount;
			this.r0 = r0;
			this.g0 = g0;
			this.b0 = b0;
			this.a0 = a0;
			this.r1 = r1;
			this.g1 = g1;
			this.b1 = b1;
			this.a1 = a1;
			this.r2 = r2;
			this.g2 = g2;
			this.b2 = b2;
			this.a2 = a2;
			this.r3 = r3;
			this.g3 = g3;
			this.b3 = b3;
			this.a3 = a3;
		}
		
		public VertexColorFace(String name, int componentCount,
				float r0, float g0, float b0,
				float r1, float g1, float b1,
				float r2, float g2, float b2,
				float r3, float g3, float b3) {
			this(name, componentCount,
					r0, g0, b0, 1.0f,
					r1, g1, b1, 1.0f,
					r2, g2, b2, 1.0f,
					r3, g3, b3, 1.0f);
		}
		
		public VertexColorFace(String name, int componentCount,
				float r0, float g0,
				float r1, float g1,
				float r2, float g2,
				float r3, float g3) {
			this(name, componentCount,
					r0, g0, 0.0f,
					r1, g1, 0.0f,
					r2, g2, 0.0f,
					r3, g3, 0.0f);
		}
		
		public VertexColorFace(String name, int componentCount,
				float r0, float r1, float r2, float r3) {
			this(name, componentCount,
					r0, 0.0f, r1, 0.0f, r2, 0.0f, r3, 0.0f);
		}
		
	}
	
	private String name;
	private int componentCount;
	private FloatArray values;
	private IntArray indices;
	private IndexCache cache;
	
	public VertexColorSet(String name, int componentCount, int capacity) {
		this.name = name;
		this.componentCount = componentCount;
		if(this.componentCount < 1 || this.componentCount > 4)
			throw new RuntimeException("Invalid component count");
		values = new FloatArray(capacity * componentCount);
		indices = new IntArray(capacity);
		cache = new IndexCache();
	}
	
	public VertexColorSet(LargeDataInputStream dis) throws IOException{
		cache = new IndexCache();
		read(dis);
	}
	
	public FloatArray getValues() {
		return values;
	}
	
	public IntArray getIndices() {
		return indices;
	}
	
	public FloatArray getFlatValues() {
		FloatArray flatValues = new FloatArray(indices.size() * componentCount);
		for(int i = 0; i < indices.size(); ++i) {
			int index = indices.get(i);
			for(int j = 0; j < componentCount; ++j) {
				flatValues.set(i * componentCount + j, values.get(index * componentCount + j));
			}
		}
		return flatValues;
	}
	
	public void clear() {
		values.clear();
		indices.clear();
		cache.clear();
	}
	
	public void expandComponentCount(int newComponentCount) {
		throw new RuntimeException("expandComponentCount not yet supported");
		// TODO: Implement
	}
	
	private float _vertexColor[] = new float[4];
	public void addFace(VertexColorFace face) {
		if(getComponentCount() < face.componentCount) {
			expandComponentCount(face.componentCount);
		}
		
		_vertexColor[0] = face.r0;
		_vertexColor[1] = face.g0;
		_vertexColor[2] = face.b0;
		_vertexColor[3] = face.a0;
		int vertexColorIndex = addValue(_vertexColor);
		addIndex(vertexColorIndex);
		
		_vertexColor[0] = face.r1;
		_vertexColor[1] = face.g1;
		_vertexColor[2] = face.b1;
		_vertexColor[3] = face.a1;
		vertexColorIndex = addValue(_vertexColor);
		addIndex(vertexColorIndex);
		
		_vertexColor[0] = face.r2;
		_vertexColor[1] = face.g2;
		_vertexColor[2] = face.b2;
		_vertexColor[3] = face.a2;
		vertexColorIndex = addValue(_vertexColor);
		addIndex(vertexColorIndex);
		
		_vertexColor[0] = face.r3;
		_vertexColor[1] = face.g3;
		_vertexColor[2] = face.b3;
		_vertexColor[3] = face.a3;
		vertexColorIndex = addValue(_vertexColor);
		addIndex(vertexColorIndex);
	}
	
	public void getFace(int faceIndex, VertexColorFace face) {
		face.name = name;
		face.componentCount = componentCount;
		int index0 = getIndex(faceIndex * 4);
		int index1 = getIndex(faceIndex * 4 + 1);
		int index2 = getIndex(faceIndex * 4 + 2);
		int index3 = getIndex(faceIndex * 4 + 3);
		switch(componentCount) {
		case 1:
			face.r0 = getR(index0);
			face.g0 = 0f;
			face.b0 = 0f;
			face.a0 = 1f;
			face.r1 = getR(index1);
			face.g1 = 0f;
			face.b1 = 0f;
			face.a1 = 1f;
			face.r2 = getR(index2);
			face.g2 = 0f;
			face.b2 = 0f;
			face.a2 = 1f;
			face.r3 = getR(index3);
			face.g3 = 0f;
			face.b3 = 0f;
			face.a3 = 1f;
			break;
		case 2:
			face.r0 = getR(index0);
			face.g0 = getG(index0);
			face.b0 = 0f;
			face.a0 = 1f;
			face.r1 = getR(index1);
			face.g1 = getG(index1);
			face.b1 = 0f;
			face.a1 = 1f;
			face.r2 = getR(index2);
			face.g2 = getG(index2);
			face.b2 = 0f;
			face.a2 = 1f;
			face.r3 = getR(index3);
			face.g3 = getG(index3);
			face.b3 = 0f;
			face.a3 = 1f;
			break;
		case 3:
			face.r0 = getR(index0);
			face.g0 = getG(index0);
			face.b0 = getB(index0);
			face.a0 = 1f;
			face.r1 = getR(index1);
			face.g1 = getG(index1);
			face.b1 = getB(index1);
			face.a1 = 1f;
			face.r2 = getR(index2);
			face.g2 = getG(index2);
			face.b2 = getB(index2);
			face.a2 = 1f;
			face.r3 = getR(index3);
			face.g3 = getG(index3);
			face.b3 = getB(index3);
			face.a3 = 1f;
			break;
		case 4:
			face.r0 = getR(index0);
			face.g0 = getG(index0);
			face.b0 = getB(index0);
			face.a0 = getA(index0);
			face.r1 = getR(index1);
			face.g1 = getG(index1);
			face.b1 = getB(index1);
			face.a1 = getA(index1);
			face.r2 = getR(index2);
			face.g2 = getG(index2);
			face.b2 = getB(index2);
			face.a2 = getA(index2);
			face.r3 = getR(index3);
			face.g3 = getG(index3);
			face.b3 = getB(index3);
			face.a3 = getA(index3);
			break;
		}
	}
	
	public int addValue(float[] values) {
		if(values.length < this.componentCount)
			throw new RuntimeException("Invalid component count");
		switch(this.componentCount) {
		case 1:
			return addValue(values[0]);
		case 2:
			return addValue(values[0], values[1]);
		case 3:
			return addValue(values[0], values[1], values[2]);
		case 4:
			return addValue(values[0], values[1], values[2], values[3]);
		default:
			throw new RuntimeException("Invalid component count");
		}
	}
	
	public int addValue(float r) {
		if(this.componentCount != 1)
			throw new RuntimeException("Invalid component count");
		int index = -1;
		float[] values = this.values.getData();
		for(int i = this.values.size() - 1; i >= 0; i--) {
			if(Math.abs(values[i] - r) < 0.0001f) {
				index = i;
				break;
			}
		}
		if(index == -1) {
			index = this.values.size();
			this.values.add(r);
		}
		return index;
	}
	
	public int addValue(float r, float g) {
		if(this.componentCount != 2)
			throw new RuntimeException("Invalid component count");
		int index = -1;
		float[] values = this.values.getData();
		for(int i = this.values.size() - 2; i >= 0; i-=2) {
			if(Math.abs(values[i] - r) < 0.0001f && 
					Math.abs(values[i+1] - g) < 0.0001f) {
				index = i/2;
				break;
			}
		}
		if(index == -1) {
			index = this.values.size()/2;
			this.values.add(r);
			this.values.add(g);
		}
		return index;
	}
	
	public int addValue(float r, float g, float b) {
		if(this.componentCount != 3)
			throw new RuntimeException("Invalid component count");
		
		long colorKey = ((long)(r * 4096f)) | (((long)(g * 4096f)) << 16) | (((long)(b * 4096f)) << 32);
		int index = cache.getOrDefault(colorKey, -1);
		if(index == -1) {
			index = this.values.size() / 3;
			cache.put(colorKey, index);
			this.values.add(r);
			this.values.add(g);
			this.values.add(b);
		}
		return index;
		
		/*int index = -1;
		float[] values = this.values.getData();
		for(int i = this.values.size() - 3; i >= 0; i-=3) {
			if(Math.abs(values[i] - r) < 0.0001f && 
					Math.abs(values[i+1] - g) < 0.0001f && 
					Math.abs(values[i+2] - b) < 0.0001f) {
				index = i/3;
				break;
			}
		}
		if(index == -1) {
			index = this.values.size()/3;
			this.values.add(r);
			this.values.add(g);
			this.values.add(b);
		}
		return index;*/
	}
	
	public int addValue(float r, float g, float b, float a) {
		if(this.componentCount != 4)
			throw new RuntimeException("Invalid component count");
		int index = -1;
		float[] values = this.values.getData();
		for(int i = this.values.size() - 4; i >= 0; i-=4) {
			if(Math.abs(values[i] - r) < 0.0001f && 
					Math.abs(values[i+1] - g) < 0.0001f && 
					Math.abs(values[i+2] - b) < 0.0001f && 
					Math.abs(values[i+3] - a) < 0.0001f) {
				index = i/4;
				break;
			}
		}
		if(index == -1) {
			index = this.values.size()/4;
			this.values.add(r);
			this.values.add(g);
			this.values.add(b);
			this.values.add(a);
		}
		return index;
	}
	
	public void addIndex(int index) {
		this.indices.add(index);
	}
	
	public int getIndex(int faceVertexId) {
		return this.indices.get(faceVertexId);
	}
	
	public float getR(int index) {
		if(index >= (this.values.size() / this.componentCount))
			return 1.0f;
		return this.values.get(index * this.componentCount);
	}
	
	public float getG(int index) {
		if(index >= (this.values.size() / this.componentCount))
			return 1.0f;
		return this.values.get(index * this.componentCount + 1);
	}

	public float getB(int index) {
		if(index >= (this.values.size() / this.componentCount))
			return 1.0f;
		return this.values.get(index * this.componentCount + 2);
	}
	
	public float getA(int index) {
		if(index >= (this.values.size() / this.componentCount))
			return 1.0f;
		return this.values.get(index * this.componentCount + 3);
	}
	
	public void get(int index, float[] out) {
		if(out.length < this.componentCount)
			throw new RuntimeException("Out array is too small");
		switch(this.componentCount) {
		case 1:
			out[0] = this.values.get(index * this.componentCount);
			return;
		case 2:
			out[0] = this.values.get(index * this.componentCount);
			out[1] = this.values.get(index * this.componentCount + 1);
			return;
		case 3:
			out[0] = this.values.get(index * this.componentCount);
			out[1] = this.values.get(index * this.componentCount + 1);
			out[2] = this.values.get(index * this.componentCount + 2);
			return;
		case 4:
			out[0] = this.values.get(index * this.componentCount);
			out[1] = this.values.get(index * this.componentCount + 1);
			out[2] = this.values.get(index * this.componentCount + 2);
			out[3] = this.values.get(index * this.componentCount + 3);
			return;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public int getComponentCount() {
		return componentCount;
	}
	
	public void write(LargeDataOutputStream dos) throws IOException {
		dos.writeUTF(name);
		dos.writeInt(componentCount);
		dos.writeInt(values.size() / componentCount);
		for(int i = 0; i < values.size(); ++i)
			dos.writeFloat(values.get(i));
		dos.writeInt(indices.size());
		for(int i = 0; i < indices.size(); ++i)
			dos.writeInt(indices.get(i));
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		this.name = dis.readUTF();
		this.componentCount = dis.readInt();
		int numValues = dis.readInt() * this.componentCount;
		float[] valuesTmp = new float[numValues];
		for(int i = 0; i < numValues; ++i)
			valuesTmp[i] = dis.readFloat();
		this.values = new FloatArray(valuesTmp);
		int numIndices = dis.readInt();
		int[] indicesTmp = new int[numIndices];
		for(int i = 0; i < numIndices; ++i)
			indicesTmp[i] = dis.readInt();
		this.indices = new IntArray(indicesTmp);
	}

}
