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

package nl.bramstout.mcworldexporter.export.processors;

import java.util.Arrays;
import java.util.List;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.atlas.Atlas.AtlasItem;
import nl.bramstout.mcworldexporter.export.Mesh;

/**
 * This optimiser tries to combine faces into larger faces.
 */
public class FaceOptimiser implements MeshProcessors.IMeshProcessor{

	private Mesh tempMesh1 = new Mesh();
	private int[][] facesPerVertex = null;
	
	@Override
	public void process(Mesh mesh, MeshProcessors manager) throws Exception {
		optimise(mesh);
		manager.processNext(mesh, this);
	}
	
	public void optimise(Mesh inMesh) {
		/*MCMeta mcmeta = ResourcePacks.getMCMeta(inMesh.getTexture());
		if(mcmeta != null) {
			// Don't optimise the faces of animated materials.
			if(mcmeta.isAnimate() || mcmeta.isInterpolate())
				return;
				//return inMesh;
		}*/
		if(inMesh.hasAnimatedTexture()) {
			// Don't optimise the faces of animated materials.
			return;
		}
		
		CombinedFace combinedFace = new CombinedFace();
		
		// Go bottom and top
		//Mesh outMesh = new Mesh(inMesh.getName(), inMesh.getTexture(), inMesh.isDoubleSided());
		tempMesh1.reset(inMesh.getName(), inMesh.getTexture(), inMesh.getMatTexture(), 
						inMesh.hasAnimatedTexture(), inMesh.isDoubleSided());
		tempMesh1.setExtraData(inMesh.getExtraData());
		
		boolean[] processedFaces = new boolean[inMesh.getFaceIndices().size()/4];
		getFacesPerVertex(inMesh);
		process(inMesh, tempMesh1, processedFaces, facesPerVertex, 0, combinedFace);
		
		// Go left and right
		//Mesh outMesh2 = new Mesh(inMesh.getName(), inMesh.getTexture(), inMesh.isDoubleSided());
		//outMesh2.setExtraData(inMesh.getExtraData());
		
		inMesh.reset(inMesh.getName(), inMesh.getTexture(), inMesh.getMatTexture(), 
						inMesh.hasAnimatedTexture(), inMesh.isDoubleSided());
		inMesh.setExtraData(tempMesh1.getExtraData());
		
		processedFaces = new boolean[tempMesh1.getFaceIndices().size()/4];
		getFacesPerVertex(tempMesh1);
		process(tempMesh1, inMesh, processedFaces, facesPerVertex, 1, combinedFace);
		
		//return outMesh2;
	}
	
	private void getFacesPerVertex(Mesh mesh){
		int facesPerVertexSize = mesh.getVertices().size() / 3;
		if(facesPerVertex == null || facesPerVertex.length < facesPerVertexSize)
			facesPerVertex = new int[facesPerVertexSize][];
		else {
			for(int i = 0; i < facesPerVertexSize; ++i) {
				if(facesPerVertex[i] == null) {
					facesPerVertex[i] = new int[5];
				}
				facesPerVertex[i][0] = 0;
			}
		}
		
		for(int faceIndex = 0; faceIndex < mesh.getFaceIndices().size()/4; ++faceIndex) {
			for(int i = 0; i < 4; ++i) {
				int vertexId = mesh.getFaceIndices().get(faceIndex * 4 + i);
				
				if(facesPerVertex[vertexId] == null) {
					facesPerVertex[vertexId] = new int[5];
					facesPerVertex[vertexId][0] = 0;
				}
				int arrayLength = facesPerVertex[vertexId][0];
				if((arrayLength+1) >= facesPerVertex[vertexId].length) {
					// We hit the limit of the array, so increase the size.
					facesPerVertex[vertexId] = Arrays.copyOf(facesPerVertex[vertexId], arrayLength*2+1);
				}
				arrayLength += 1;
				facesPerVertex[vertexId][arrayLength] = faceIndex;
				facesPerVertex[vertexId][0] = arrayLength;
			}
		}
	}
	
	private static class CombinedFace{
		
		float[] vertices;
		float[] us;
		float[] vs;
		float[] cornerUVs;
		float[] normals;
		float[] colors;
		boolean hasColors;
		float[] ao;
		
		public CombinedFace() {
			vertices = new float[3*4];
			us = new float[4];
			vs = new float[4];
			cornerUVs = new float[8];
			normals = new float[3];
			colors = new float[3];
			hasColors = false;
			ao = new float[4];
		}
		
		public void setup(Mesh mesh, int face) {
			for(int edgeId = 0; edgeId < 4; ++edgeId) {
				int vertexIndex = mesh.getFaceIndices().get(face*4 + edgeId);
				vertices[edgeId*3] = mesh.getVertices().get(vertexIndex*3);
				vertices[edgeId*3+1] = mesh.getVertices().get(vertexIndex*3+1);
				vertices[edgeId*3+2] = mesh.getVertices().get(vertexIndex*3+2);
				
				int uvIndex = mesh.getUvIndices().get(face * 4 + edgeId);
				us[edgeId] = mesh.getUs().get(uvIndex);
				vs[edgeId] = mesh.getVs().get(uvIndex);
				
				int cornerUVIndex = mesh.getCornerUVIndices().get(face * 4 + edgeId);
				cornerUVs[edgeId * 2] = mesh.getCornerUVs().get(cornerUVIndex * 2);
				cornerUVs[edgeId * 2 + 1] = mesh.getCornerUVs().get(cornerUVIndex * 2 + 1);
				
				int aoIndex = mesh.getAOIndices().get(face * 4 + edgeId);
				ao[edgeId] = mesh.getAO().get(aoIndex);
			}
			int normalIndex = mesh.getNormalIndices().get(face*4);
			normals[0] = mesh.getNormals().get(normalIndex*3);
			normals[1] = mesh.getNormals().get(normalIndex*3+1);
			normals[2] = mesh.getNormals().get(normalIndex*3+2);
			
			hasColors = false;
			if(mesh.hasColors()) {
				hasColors = true;
				int colorIndex = mesh.getColorIndices().get(face*4);
				colors[0] = mesh.getColors().get(colorIndex*3);
				colors[1] = mesh.getColors().get(colorIndex*3+1);
				colors[2] = mesh.getColors().get(colorIndex*3+2);
			}
		}
		
		public void extend(int edgeId, Mesh mesh, int includeFace, int includeEdge, 
							float offsetU, float offsetV, boolean reverse, AtlasItem currentAtlasItem) {
			int vertexIndex1 = mesh.getFaceIndices().get(includeFace*4 + includeEdge);
			int vertexIndex2 = mesh.getFaceIndices().get(includeFace*4 + ((includeEdge + 1) % 4));
			
			int vert1 = (edgeId+1) % 4;
			int vert2 = edgeId;
			if(reverse) {
				vert2 = vert1;
				vert1 = edgeId;
			}
			
			vertices[vert1*3] = mesh.getVertices().get(vertexIndex1*3);
			vertices[vert1*3+1] = mesh.getVertices().get(vertexIndex1*3+1);
			vertices[vert1*3+2] = mesh.getVertices().get(vertexIndex1*3+2);
			
			vertices[vert2*3] = mesh.getVertices().get(vertexIndex2*3);
			vertices[vert2*3+1] = mesh.getVertices().get(vertexIndex2*3+1);
			vertices[vert2*3+2] = mesh.getVertices().get(vertexIndex2*3+2);
			
			if((vertices[0] == vertices[3] && vertices[1] == vertices[4] && vertices[2] == vertices[5]) || 
					(vertices[0] == vertices[9] && vertices[1] == vertices[10] && vertices[2] == vertices[11])) {
				throw new RuntimeException();
			}
			
			int uvIndex1 = mesh.getUvIndices().get(includeFace*4 + includeEdge);
			int uvIndex2 = mesh.getUvIndices().get(includeFace*4 + ((includeEdge+1)%4));
			
			if(us[vert1] < us[vert2]) {
				// vert1 has the minU and vert2 has the maxU
				us[vert1] = Math.min(mesh.getUs().get(uvIndex1), mesh.getUs().get(uvIndex2)) + offsetU;
				us[vert2] = Math.max(mesh.getUs().get(uvIndex1), mesh.getUs().get(uvIndex2)) + offsetU;
			}else {
				// vert1 has the maxU and vert2 has the minU
				us[vert1] = Math.max(mesh.getUs().get(uvIndex1), mesh.getUs().get(uvIndex2)) + offsetU;
				us[vert2] = Math.min(mesh.getUs().get(uvIndex1), mesh.getUs().get(uvIndex2)) + offsetU;
			}
			
			if(vs[vert1] < vs[vert2]) {
				// vert1 has the minV and vert2 has the maxV
				vs[vert1] = Math.min(mesh.getVs().get(uvIndex1), mesh.getVs().get(uvIndex2)) + offsetV;
				vs[vert2] = Math.max(mesh.getVs().get(uvIndex1), mesh.getVs().get(uvIndex2)) + offsetV;
			}else {
				// vert1 has the maxV and vert2 has the minV
				vs[vert1] = Math.max(mesh.getVs().get(uvIndex1), mesh.getVs().get(uvIndex2)) + offsetV;
				vs[vert2] = Math.min(mesh.getVs().get(uvIndex1), mesh.getVs().get(uvIndex2)) + offsetV;
			}
			
			if(currentAtlasItem != null) {
				// We are using an atlas, and the UVs that we just read from the
				// mesh are still in atlas space, so transform it into local space.
				us[vert1] = currentAtlasItem.uToLocal(us[vert1] - offsetU) + offsetU;
				us[vert2] = currentAtlasItem.uToLocal(us[vert2] - offsetU) + offsetU;
				vs[vert1] = currentAtlasItem.vToLocal(vs[vert1] - offsetV) + offsetV;
				vs[vert2] = currentAtlasItem.vToLocal(vs[vert2] - offsetV) + offsetV;
			}
		}
		
	}
	
	private void process(Mesh inMesh, Mesh outMesh, boolean[] processedFaces, int[][] facesPerVertex, 
								int edgeId, CombinedFace combinedFace) {
		// Get the atlas items if this mesh uses an atlas.
		// If this mesh doesn't use an atlas, then Atlas.getItems()
		// will return null.
		List<AtlasItem> atlas = Atlas.getItems(inMesh.getTexture());
		
		for(int faceIndex = 0; faceIndex < inMesh.getFaceIndices().size()/4; ++faceIndex) {
			if(processedFaces[faceIndex])
				continue; // We have already processed this face, so skip.
			
			combinedFace.setup(inMesh, faceIndex);
			AtlasItem currentAtlasItem = null;
			if(atlas != null) {
				for(AtlasItem item : atlas) {
					if(item.isInItem((combinedFace.us[0] + combinedFace.us[2])/2f, (combinedFace.vs[0]+combinedFace.vs[2])/2f)) {
						currentAtlasItem = item;
						break;
					}
				}
				if(currentAtlasItem != null) {
					// Transform the UVs into local space from atlas space.
					for(int i = 0; i < 4; ++i) {
						combinedFace.us[i] = currentAtlasItem.uToLocal(combinedFace.us[i]);
						combinedFace.vs[i] = currentAtlasItem.vToLocal(combinedFace.vs[i]);
					}
				}
			}
			
			processFace(inMesh, processedFaces, facesPerVertex, faceIndex, edgeId, combinedFace, atlas, currentAtlasItem);
			
			// Also go the other direction.
			processFace(inMesh, processedFaces, facesPerVertex, faceIndex, (edgeId+2) % 4, combinedFace, atlas, currentAtlasItem);
			
			if(currentAtlasItem != null) {
				// Transform the UVs back into atlas space from local space,
				// if there was an atlas.
				for(int i = 0; i < 4; ++i) {
					combinedFace.us[i] = currentAtlasItem.uToAtlas(combinedFace.us[i]);
					combinedFace.vs[i] = currentAtlasItem.vToAtlas(combinedFace.vs[i]);
				}
			}
			
			if(processedFaces[faceIndex])
				outMesh.addFace(combinedFace.vertices, combinedFace.us, combinedFace.vs, combinedFace.cornerUVs, combinedFace.normals, 
						combinedFace.hasColors ? combinedFace.colors : null, combinedFace.ao);
		}
		// Add in any faces that we weren't able to combine
		for(int faceIndex = 0; faceIndex < inMesh.getFaceIndices().size()/4; ++faceIndex) {
			if(processedFaces[faceIndex])
				continue;
			outMesh.addFaceFromMesh(inMesh, faceIndex);
		}
	}
	
	private void processFace(Mesh inMesh, boolean[] processedFaces, int[][] facesPerVertex, int faceIndex, 
										int edgeId, CombinedFace combinedFace,
										List<AtlasItem> atlas, AtlasItem currentAtlasItem) {
		int origEdgeId = edgeId;
		float totalOffsetU = 0f;
		float totalOffsetV = 0f;
		// Basically, we keep checking face by face if we can find a neighbouring face that we
		// can include into our larger face. If so, then we try again but that neighbouring face
		// becomes the source face to check with.
		for(int tmpCounter = 0; tmpCounter < inMesh.getFaceIndices().size()/4; ++tmpCounter) {
			// Get the vertex ids
			int vertexId1 = inMesh.getFaceIndices().get(faceIndex * 4 + edgeId);
			int vertexId2 = inMesh.getFaceIndices().get(faceIndex * 4 + ((edgeId + 1) % 4));
			int vertexId3 = inMesh.getFaceIndices().get(faceIndex * 4 + ((edgeId + 2) % 4));
			
			float v1X = inMesh.getVertices().get(vertexId2 * 3);
			float v1Y = inMesh.getVertices().get(vertexId2 * 3 + 1);
			float v1Z = inMesh.getVertices().get(vertexId2 * 3 + 2);
			float v2X = inMesh.getVertices().get(vertexId3 * 3);
			float v2Y = inMesh.getVertices().get(vertexId3 * 3 + 1);
			float v2Z = inMesh.getVertices().get(vertexId3 * 3 + 2);
			float vdX = v2X - v1X;
			float vdY = v2Y - v1Y;
			float vdZ = v2Z - v1Z;
			float vdLength = (float) Math.sqrt(vdX * vdX + vdY * vdY + vdZ * vdZ);
			if(vdLength > 0.0000001f) {
				vdX /= vdLength;
				vdY /= vdLength;
				vdZ /= vdLength;
			}
			
			int normalId = inMesh.getNormalIndices().get(faceIndex*4);
			float normalX = inMesh.getNormals().get(normalId*3);
			float normalY = inMesh.getNormals().get(normalId*3+1);
			float normalZ = inMesh.getNormals().get(normalId*3+2);
			int colorId = 0;
			if(inMesh.hasColors())
				colorId = inMesh.getColorIndices().get(faceIndex*4);
			
			int uvIndex1 = inMesh.getUvIndices().get(faceIndex * 4 + edgeId);
			float u1 = inMesh.getUs().get(uvIndex1);
			float v1 = inMesh.getVs().get(uvIndex1);
			int uvIndex2 = inMesh.getUvIndices().get(faceIndex * 4 + ((edgeId+1)%4));
			float u2 = inMesh.getUs().get(uvIndex2);
			float v2 = inMesh.getVs().get(uvIndex2);
			int uvIndex3 = inMesh.getUvIndices().get(faceIndex * 4 + ((edgeId+3)%4));
			float u3 = inMesh.getUs().get(uvIndex3);
			float v3 = inMesh.getVs().get(uvIndex3);
			if(currentAtlasItem != null) {
				// If we are using an atlas, make sure it's all in local space.
				u1 = currentAtlasItem.uToLocal(u1);
				u2 = currentAtlasItem.uToLocal(u2);
				u3 = currentAtlasItem.uToLocal(u3);
				v1 = currentAtlasItem.vToLocal(v1);
				v2 = currentAtlasItem.vToLocal(v2);
				v3 = currentAtlasItem.vToLocal(v3);
			}
			float uDir = u3 - u1;
			float vDir = v3 - v1;
			float uLength = Math.abs(uDir);
			float vLength = Math.abs(vDir);
			
			int cornerUVIndex = inMesh.getCornerUVIndices().get(faceIndex * 4 + edgeId);
			
			int aoIndex0 = inMesh.getAOIndices().get(faceIndex * 4 + edgeId);
			float ao0 = inMesh.getAO().get(aoIndex0);
			int aoIndex1 = inMesh.getAOIndices().get(faceIndex * 4 + ((edgeId+1)%4));
			float ao1 = inMesh.getAO().get(aoIndex1);
			int aoIndex2 = inMesh.getAOIndices().get(faceIndex * 4 + ((edgeId+2)%4));
			float ao2 = inMesh.getAO().get(aoIndex2);
			int aoIndex3 = inMesh.getAOIndices().get(faceIndex * 4 + ((edgeId+3)%4));
			float ao3 = inMesh.getAO().get(aoIndex3);
			
			boolean addedFace = false;
			
			int[] facesToCheck = facesPerVertex[vertexId1];
			int facesToCheckSize = facesToCheck[0];
			// Now try to find another face that also shares those vertices
			for(int faceIndex2Index = 0; faceIndex2Index < facesToCheckSize; ++faceIndex2Index) {
				int faceIndex2 = facesToCheck[faceIndex2Index+1];
				if(processedFaces[faceIndex2] || faceIndex2 == faceIndex)
					continue; // Already processed, so skip
				
				int normalId2 = inMesh.getNormalIndices().get(faceIndex2*4);
				float normalX2 = inMesh.getNormals().get(normalId2*3);
				float normalY2 = inMesh.getNormals().get(normalId2*3+1);
				float normalZ2 = inMesh.getNormals().get(normalId2*3+2);
				//if(normalId2 != normalId)
				//	continue; // Normals don't match
				if(Math.abs(normalX - normalX2) > 0.01f || Math.abs(normalY - normalY2) > 0.01f ||
						Math.abs(normalZ - normalZ2) > 0.01f)
					continue; // Normals don't match
				
				int colorId2 = 0;
				if(inMesh.hasColors())
					colorId2 = inMesh.getColorIndices().get(faceIndex2*4);
				if(colorId2 != colorId)
					continue; // Colours don't match.
				
				int edgeId2 = 0;
				int vertexId2_1 = 0;
				int vertexId2_2 = 0;
				boolean match = false;
				boolean reverse = false;
				
				for(edgeId2 = 0; edgeId2 < 4; ++edgeId2) {
					vertexId2_1 = inMesh.getFaceIndices().get(faceIndex2 * 4 + edgeId2);
					vertexId2_2 = inMesh.getFaceIndices().get(faceIndex2 * 4 + ((edgeId2 + 1) % 4));
					
					if(vertexId2_1 == vertexId1 && vertexId2_2 == vertexId2) {
						match = true;
						break;
					}
					if(vertexId2_1 == vertexId2 && vertexId2_2 == vertexId1) {
						match = true;
						reverse = true;
						break;
					}
				}
				
				if(!match)
					continue;
				
				// The face needs to be going in the same direction.
				int vertexId2_3 = inMesh.getFaceIndices().get(faceIndex2 * 4 + ((edgeId2 + 2) % 4));
				int vertexId2_4 = inMesh.getFaceIndices().get(faceIndex2 * 4 + ((edgeId2 + 3) % 4));
				if(vertexId3 == vertexId2_3 || vertexId3 == vertexId2_4) {
					// This face shares at least 3 our of 4 vertices,
					// which would make this a duplicate face.
					continue;
				}
				
				float v2_1X = inMesh.getVertices().get(vertexId2_2 * 3);
				float v2_1Y = inMesh.getVertices().get(vertexId2_2 * 3 + 1);
				float v2_1Z = inMesh.getVertices().get(vertexId2_2 * 3 + 2);
				float v2_2X = inMesh.getVertices().get(vertexId2_3 * 3);
				float v2_2Y = inMesh.getVertices().get(vertexId2_3 * 3 + 1);
				float v2_2Z = inMesh.getVertices().get(vertexId2_3 * 3 + 2);
				float v2_dX = v2_2X - v2_1X;
				float v2_dY = v2_2Y - v2_1Y;
				float v2_dZ = v2_2Z - v2_1Z;
				float vdLength2 = (float) Math.sqrt(v2_dX * v2_dX + v2_dY * v2_dY + v2_dZ * v2_dZ);
				if(vdLength > 0.0000001f) {
					v2_dX /= vdLength2;
					v2_dY /= vdLength2;
					v2_dZ /= vdLength2;
				}
				
				//float dot = vdX * v2_dX + vdY * v2_dY + vdZ * v2_dZ;
				//if(dot < 0.5f)
				//	continue;
				
				// It needs to use the same corner data.
				if(Config.calculateCornerUVs) {
					boolean cornerUVMatch = false;
					for(int edgeId22 = 0; edgeId22 < 4; ++edgeId22) {
						int cornerUVIndex2 = inMesh.getCornerUVIndices().get(faceIndex2 * 4 + edgeId22);
						if(cornerUVIndex2 == cornerUVIndex) {
							cornerUVMatch = true;
							break;
						}
					}
					if(!cornerUVMatch)
						continue;
				}
				
				// We have a face that shares an edge, so now check if it's a proper match.
				// The UVs need to match in a repeating pattern.
				int uvIndex2_1 = inMesh.getUvIndices().get(faceIndex2*4 + edgeId2);
				float u2_1 = inMesh.getUs().get(uvIndex2_1);
				float v2_1 = inMesh.getVs().get(uvIndex2_1);
				int uvIndex2_2 = inMesh.getUvIndices().get(faceIndex2*4 + ((edgeId2+1)%4));
				float u2_2 = inMesh.getUs().get(uvIndex2_2);
				float v2_2 = inMesh.getVs().get(uvIndex2_2);
				int uvIndex2_3 = inMesh.getUvIndices().get(faceIndex2*4 + ((edgeId2+3)%4));
				float u2_3 = inMesh.getUs().get(uvIndex2_3);
				float v2_3 = inMesh.getVs().get(uvIndex2_3);
				if(currentAtlasItem != null) {
					// If we are using an atlas, make sure the UVs are in local space.
					u2_1 = currentAtlasItem.uToLocal(u2_1);
					u2_2 = currentAtlasItem.uToLocal(u2_2);
					u2_3 = currentAtlasItem.uToLocal(u2_3);
					v2_1 = currentAtlasItem.vToLocal(v2_1);
					v2_2 = currentAtlasItem.vToLocal(v2_2);
					v2_3 = currentAtlasItem.vToLocal(v2_3);
					
					// If the UVs are outside of this atlas item,
					// then we don't want to connect the faces
					float centerU = (u2_1 + u2_2 + u2_3) / 3f;
					float centerV = (v2_1 + v2_2 + v2_3) / 3f;
					if(centerU < 0f || centerU > ((float) currentAtlasItem.padding) ||
							centerV < 0f || centerV > ((float) currentAtlasItem.padding))
						continue;
				}
				
				if(!(UVsMatch(u1, v1, u2_1, v2_1) && UVsMatch(u2, v2, u2_2, v2_2)) &&
						!(UVsMatch(u1, v1, u2_2, v2_2) && UVsMatch(u2, v2, u2_1, v2_1))) {
					continue; // UVs don't match
				}
				
				float uDir2 = u2_3 - u2_1;
				float vDir2 = v2_3 - v2_1;
				float uLength2 = Math.abs(uDir2);
				float vLength2 = Math.abs(vDir2);
				float duDir = Math.abs(-uDir2 - uDir);
				float dvDir = Math.abs(-vDir2 - vDir);
				if(duDir > 0.001f || dvDir > 0.001f)
					continue; // Uv's don't go in the same direction (it's mirrored)
				float duLength = Math.abs(uLength2 - uLength);
				float dvLength = Math.abs(vLength2 - vLength);
				if(duLength > 0.001f || dvLength > 0.001f)
					continue; // Uv's aren't of the same size.
				
				// Let's figure out by what to offset the UVs
				float offsetU = Math.min(u1, u2) - Math.min(u2_1, u2_2);
				float offsetV = Math.min(v1, v2) - Math.min(v2_1, v2_2);
				totalOffsetU += offsetU;
				totalOffsetV += offsetV;
				
				if(currentAtlasItem != null) {
					// We are using an atlas, but the texture only repeats a certain amount,
					// so we need to make sure that we remain inside of the bounds.
					// So, if adding this face will cause us to go outside of the bounds,
					// then we need to skip it.
					float testU1 = u2_1 + totalOffsetU;
					float testV1 = v2_1 + totalOffsetV;
					float testU2 = u2_3 + totalOffsetU;
					float testV2 = v2_3 + totalOffsetV;
					
					float minU = Math.min(Math.min(combinedFace.us[0], combinedFace.us[2]), Math.min(testU1, testU2));
					float maxU = Math.max(Math.max(combinedFace.us[0], combinedFace.us[2]), Math.max(testU1, testU2));
					float minV = Math.min(Math.min(combinedFace.vs[0], combinedFace.vs[2]), Math.min(testV1, testV2));
					float maxV = Math.max(Math.max(combinedFace.vs[0], combinedFace.vs[2]), Math.max(testV1, testV2));
					float sizeU = maxU - minU;
					float sizeV = maxV - minV;
					
					// Check if we go outside of the bounds.
					if(sizeU > ((float) currentAtlasItem.padding) || sizeV > ((float) currentAtlasItem.padding))
						continue;
					
					minU = Math.min(minU, 0f);
					minV = Math.min(minV, 0f);
					
					// We are inside of the bounds still, but we need to move the UVs around
					// so that minU and minV are at (0,0), if they are otherwise negative
					// We also need to do that in the already stored UVs
					for(int i = 0; i < 4; ++i) {
						combinedFace.us[i] -= minU;
						combinedFace.vs[i] -= minV;
					}
					totalOffsetU -= minU;
					totalOffsetV -= minV;
				}
				
				// Make sure that AO matches.
				int aoIndex2_0 = inMesh.getAOIndices().get(faceIndex2 * 4 + ((edgeId2+2)%4));
				float ao2_0 = inMesh.getAO().get(aoIndex2_0);
				int aoIndex2_1 = inMesh.getAOIndices().get(faceIndex2 * 4 + ((edgeId2+3)%4));
				float ao2_1 = inMesh.getAO().get(aoIndex2_1);
				int aoIndex2_2 = inMesh.getAOIndices().get(faceIndex2 * 4 + ((edgeId2+0)%4));
				float ao2_2 = inMesh.getAO().get(aoIndex2_2);
				int aoIndex2_3 = inMesh.getAOIndices().get(faceIndex2 * 4 + ((edgeId2+1)%4));
				float ao2_3 = inMesh.getAO().get(aoIndex2_3);
				if(ao0 != ao2_0 || ao1 != ao2_1 || ao2 != ao2_2 || ao3 != ao2_3)
					continue;
				
				// Everything matches, so let's include this face.
				
				combinedFace.extend(origEdgeId, inMesh, faceIndex2, (edgeId2+2)%4, 
									totalOffsetU, totalOffsetV, reverse, currentAtlasItem);
				
				// Set the stuff up for the next iteration
				processedFaces[faceIndex] = true;
				faceIndex = faceIndex2;
				edgeId = (edgeId2+2)%4;
				processedFaces[faceIndex2] = true;
				addedFace = true;
				
				break;
			}
			
			if(!addedFace)
				break; // We didn't add another face, so stop here.
		}
	}
	
	private boolean UVsMatch(float u, float v, float u2, float v2) {
		// UVs repeat on the 0-1 space, so we wrap it around.
		u -= Math.floor(u);
		v -= Math.floor(v);
		u2 -= Math.floor(u2);
		v2 -= Math.floor(v2);
		
		// If they are really really close together,
		// we'll see it as close together
		float du = Math.abs(u2 - u);
		float dv = Math.abs(v2 - v);
		
		return du < 0.001f && dv < 0.001f; 
	}
	
}
