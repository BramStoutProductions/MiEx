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

public class MeshSubset {
	
	private String name;
	private String texture;
	private String matTexture;
	private boolean animatedTexture;
	private MeshPurpose purpose;
	private IntArray faceIndices;
	private boolean unique;
	private long uniqueId;
	
	public MeshSubset(String name, String texture, String matTexture, 
						boolean animatedTexture, MeshPurpose purpose, boolean isUnique, long uniqueId) {
		this.name = name;
		this.texture = texture;
		this.matTexture = matTexture;
		this.animatedTexture = animatedTexture;
		this.purpose = purpose;
		this.faceIndices = new IntArray(1);
		this.unique = isUnique;
		this.uniqueId = uniqueId;
	}
	
	public MeshSubset(LargeDataInputStream dis) throws IOException{
		read(dis);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof MeshSubset))
			return false;
		MeshSubset other = (MeshSubset) obj;
		if(texture == null) {
			if(other.texture != null)
				return false;
		}else {
			if(!texture.equals(other.texture))
				return false;
		}
		if(matTexture == null) {
			if(other.matTexture != null)
				return false;
		}else {
			if(!matTexture.equals(other.matTexture))
				return false;
		}
		if(animatedTexture != other.animatedTexture)
			return false;
		if(unique != other.unique)
			return false;
		return true;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTexture() {
		return texture;
	}
	
	public String getMatTexture() {
		return matTexture;
	}
	
	public boolean isAnimatedTexture() {
		return animatedTexture;
	}
	
	public boolean isUnique() {
		return unique;
	}
	
	public MeshPurpose getPurpose() {
		return purpose;
	}
	
	public IntArray getFaceIndices() {
		return faceIndices;
	}
	
	public long getUniqueId() {
		return uniqueId;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setTexture(String texture) {
		this.texture = texture;
	}
	
	public void setMatTexture(String matTexture) {
		this.matTexture = matTexture;
	}
	
	public void setAnimatedTexture(boolean animatedTexture) {
		this.animatedTexture = animatedTexture;
	}
	
	public void setPurpose(MeshPurpose purpose) {
		this.purpose = purpose;
	}
	
	public void setIsUnique(boolean unique) {
		this.unique = unique;
	}
	
	public void setUniqueId(long id) {
		this.uniqueId = id;
	}
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeUTF(name);
		if(texture == null) {
			dos.writeBoolean(false);
		}else {
			dos.writeBoolean(true);
			dos.writeUTF(texture);
		}
		if(matTexture == null) {
			dos.writeBoolean(false);
		}else {
			dos.writeBoolean(true);
			dos.writeUTF(matTexture);
		}
		dos.writeBoolean(animatedTexture);
		dos.writeBoolean(unique);
		dos.writeInt(purpose.id);
		int numFaceIndices = faceIndices.size();
		dos.writeInt(numFaceIndices);
		for(int i = 0; i < numFaceIndices; ++i)
			dos.writeInt(faceIndices.get(i));
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		name = dis.readUTF();
		if(dis.readBoolean())
			texture = dis.readUTF();
		else
			texture = null;
		if(dis.readBoolean())
			matTexture = dis.readUTF();
		else
			matTexture = null;
		animatedTexture = dis.readBoolean();
		unique = dis.readBoolean();
		purpose = MeshPurpose.fromId(dis.readInt());
		int numFaceIndices = dis.readInt();
		if(faceIndices == null)
			faceIndices = new IntArray(numFaceIndices);
		else
			faceIndices.reserve(numFaceIndices);
		for(int i = 0; i < numFaceIndices; ++i)
			faceIndices.set(i, dis.readInt());
	}

}
