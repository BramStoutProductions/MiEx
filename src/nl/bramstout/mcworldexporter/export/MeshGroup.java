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
import java.util.List;

import nl.bramstout.mcworldexporter.model.ModelFace;

public class MeshGroup extends Mesh{

	private List<Mesh> children;
	private List<String> childNames;
	
	public MeshGroup(String name, MeshPurpose purpose) {
		super(name, purpose, name, name, false, false, ModelFace.SHADING_MODE_STANDARD, 0, 0);
		children = new ArrayList<Mesh>();
		childNames = new ArrayList<String>();
	}
	
	public MeshGroup(LargeDataInputStream dis, boolean chunked) throws IOException{
		super();
		children = new ArrayList<Mesh>();
		childNames = new ArrayList<String>();
		read(dis, chunked);
	}
	
	public void appendMesh(Mesh mesh, boolean useSubsets) {
		if(mesh instanceof MeshGroup) {
			for(String child : ((MeshGroup) mesh).childNames) {
				if(!childNames.contains(child))
					childNames.add(child);
			}
		}
	}
	
	public void addMesh(Mesh mesh) {
		children.add(mesh);
	}
	
	public List<Mesh> getChildren(){
		return children;
	}
	
	public int getNumChildren() {
		return children.size();
	}
	
	public List<String> getChildNames(){
		return childNames;
	}
	
	@Override
	public boolean hasPurpose(MeshPurpose purpose) {
		if(getPurpose() == purpose)
			return true;
		for(Mesh child : children)
			if(child.hasPurpose(purpose))
				return true;
		return false;
	}
	
	@Override
	public void write(LargeDataOutputStream dos, boolean chunked) throws IOException {
		dos.writeByte(2); // Mesh type : Group
		dos.writeUTF(getName());
		dos.writeInt(getPurpose().id);
		if(getExtraData() != null)
			dos.writeUTF(getExtraData());
		else
			dos.writeUTF("");
		
		if(chunked) {			
			dos.writeInt(getNumChildren());
			for(Mesh child : children) {
				dos.writeUTF(child.getName());
			}
		}else {
			for(Mesh child : children)
				child.write(dos, false);
			dos.writeByte(0);
		}
	}
	
	public void read(LargeDataInputStream dis, boolean chunked) throws IOException{
		setName(dis.readUTF());
		setPurpose(MeshPurpose.fromId(dis.readInt()));
		String extraData = dis.readUTF();
		if(extraData != "")
			setExtraData(extraData);
		
		if(chunked) {
			childNames.clear();
			int numChildren = dis.readInt();
			for(int i = 0; i < numChildren; i++) {
				childNames.add(dis.readUTF());
			}
		}else {
			while(true) {
				byte childType = dis.readByte();
				if(childType == 0) {
					break;
				}else if(childType == 1) {
					Mesh child = new Mesh(dis);
					children.add(child);
				}else if(childType == 2) {
					Mesh child = new MeshGroup(dis, false);
					children.add(child);
				}
			}
		}
	}
	
}
