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

package nl.bramstout.mcworldexporter.export.optimiser;

import java.util.Arrays;
import java.util.List;

import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.atlas.Atlas.AtlasItem;
import nl.bramstout.mcworldexporter.export.Mesh;

/**
 * This optimiser tries to combine faces into larger faces.
 */
public class FaceOptimiser {

	public static Mesh optimise(Mesh inMesh) {
		// Go bottom and top
		Mesh outMesh = new Mesh(inMesh.getName(), inMesh.getTexture(), inMesh.isDoubleSided());
		outMesh.setExtraData(inMesh.getExtraData());
		
		boolean[] processedFaces = new boolean[inMesh.getEdgeIndices().size()/4];
		int[][] facesPerVertex = getFacesPerVertex(inMesh);
		process(inMesh, outMesh, processedFaces, facesPerVertex, 0);
		
		// Go left and right
		Mesh outMesh2 = new Mesh(inMesh.getName(), inMesh.getTexture(), inMesh.isDoubleSided());
		outMesh2.setExtraData(inMesh.getExtraData());
		
		processedFaces = new boolean[outMesh.getEdgeIndices().size()/4];
		facesPerVertex = getFacesPerVertex(outMesh);
		process(outMesh, outMesh2, processedFaces, facesPerVertex, 1);
		
		return outMesh2;
	}
	
	private static int[][] getFacesPerVertex(Mesh mesh){
		int[][] facesPerVertex = new int[mesh.getVertices().size()/3][];
		
		for(int faceIndex = 0; faceIndex < mesh.getEdgeIndices().size()/4; ++faceIndex) {
			for(int i = 0; i < 4; ++i) {
				int edgeIndex = mesh.getEdgeIndices().get(faceIndex * 4 + i);
				int vertexId = mesh.getEdges().get(edgeIndex * 3);
				
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
		
		return facesPerVertex;
	}
	
	private static class CombinedFace{
		
		float[] vertices;
		float[] us;
		float[] vs;
		float[] normals;
		float[] colors;
		
		public CombinedFace(Mesh mesh, int face) {
			vertices = new float[3*4];
			us = new float[4];
			vs = new float[4];
			normals = new float[3];
			colors = null;
			
			for(int edgeId = 0; edgeId < 4; ++edgeId) {
				int edgeIndex = mesh.getEdgeIndices().get(face*4 + edgeId);
				int vertexIndex = mesh.getEdges().get(edgeIndex*3);
				vertices[edgeId*3] = mesh.getVertices().get(vertexIndex*3);
				vertices[edgeId*3+1] = mesh.getVertices().get(vertexIndex*3+1);
				vertices[edgeId*3+2] = mesh.getVertices().get(vertexIndex*3+2);
				
				int uvIndex = mesh.getUvIndices().get(face * 4 + edgeId);
				us[edgeId] = mesh.getUs().get(uvIndex);
				vs[edgeId] = mesh.getVs().get(uvIndex);
			}
			int normalIndex = mesh.getNormalIndices().get(face*4);
			normals[0] = mesh.getNormals().get(normalIndex*3);
			normals[1] = mesh.getNormals().get(normalIndex*3+1);
			normals[2] = mesh.getNormals().get(normalIndex*3+2);
			
			if(mesh.getColors() != null) {
				colors = new float[3];
				int colorIndex = mesh.getColorIndices().get(face*4);
				colors[0] = mesh.getColors().get(colorIndex*3);
				colors[1] = mesh.getColors().get(colorIndex*3+1);
				colors[2] = mesh.getColors().get(colorIndex*3+2);
			}
		}
		
		public void extend(int edgeId, Mesh mesh, int includeFace, int includeEdge, 
							float offsetU, float offsetV, boolean reverse, AtlasItem currentAtlasItem) {
			int includeEdgeIndex = mesh.getEdgeIndices().get(includeFace*4 + includeEdge);
			int vertexIndex1 = mesh.getEdges().get(includeEdgeIndex*3);
			int vertexIndex2 = mesh.getEdges().get(includeEdgeIndex*3+1);
			
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
	
	private static void process(Mesh inMesh, Mesh outMesh, boolean[] processedFaces, int[][] facesPerVertex, int edgeId) {
		// Get the atlas items if this mesh uses an atlas.
		// If this mesh doesn't use an atlas, then Atlas.getItems()
		// will return null.
		List<AtlasItem> atlas = Atlas.getItems(inMesh.getTexture());
		
		for(int faceIndex = 0; faceIndex < inMesh.getEdgeIndices().size()/4; ++faceIndex) {
			if(processedFaces[faceIndex])
				continue; // We have already processed this face, so skip.
			
			CombinedFace combinedFace = new CombinedFace(inMesh, faceIndex);
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
				outMesh.addFace(combinedFace.vertices, combinedFace.us, combinedFace.vs, combinedFace.normals, combinedFace.colors);
		}
		// Add in any faces that we weren't able to combine
		for(int faceIndex = 0; faceIndex < inMesh.getEdgeIndices().size()/4; ++faceIndex) {
			if(processedFaces[faceIndex])
				continue;
			outMesh.addFaceFromMesh(inMesh, faceIndex);
		}
	}
	
	private static void processFace(Mesh inMesh, boolean[] processedFaces, int[][] facesPerVertex, int faceIndex, 
										int edgeId, CombinedFace combinedFace,
										List<AtlasItem> atlas, AtlasItem currentAtlasItem) {
		int origEdgeId = edgeId;
		float totalOffsetU = 0f;
		float totalOffsetV = 0f;
		// Basically, we keep checking face by face if we can find a neighbouring face that we
		// can include into our larger face. If so, then we try again but that neighbouring face
		// becomes the source face to check with.
		for(int tmpCounter = 0; tmpCounter < inMesh.getEdgeIndices().size()/4; ++tmpCounter) {
			// Get the vertex ids
			int edgeIndex = inMesh.getEdgeIndices().get(faceIndex * 4 + edgeId);
			int vertexId1 = inMesh.getEdges().get(edgeIndex * 3);
			int vertexId2 = inMesh.getEdges().get(edgeIndex * 3 + 1);
			int normalId = inMesh.getNormalIndices().get(faceIndex*4);
			int colorId = 0;
			if(inMesh.getColorIndices() != null)
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
			
			boolean addedFace = false;
			
			int[] facesToCheck = facesPerVertex[vertexId1];
			int facesToCheckSize = facesToCheck[0];
			// Now try to find another face that also shares those vertices
			for(int faceIndex2Index = 0; faceIndex2Index < facesToCheckSize; ++faceIndex2Index) {
				int faceIndex2 = facesToCheck[faceIndex2Index+1];
				if(processedFaces[faceIndex2] || faceIndex2 == faceIndex)
					continue; // Already processed, so skip
				
				int normalId2 = inMesh.getNormalIndices().get(faceIndex2*4);
				if(normalId2 != normalId)
					continue; // Normals don't match
				
				int colorId2 = 0;
				if(inMesh.getColorIndices() != null)
					colorId2 = inMesh.getColorIndices().get(faceIndex2*4);
				if(colorId2 != colorId)
					continue; // Colours don't match.
				
				int edgeId2;
				int vertexId2_1;
				int vertexId2_2;
				boolean match = false;
				boolean reverse = false;
				
				for(edgeId2 = 0; edgeId2 < 4; ++edgeId2) {
					int edgeIndex2 = inMesh.getEdgeIndices().get(faceIndex2 * 4 + edgeId2);
					vertexId2_1 = inMesh.getEdges().get(edgeIndex2 * 3);
					vertexId2_2 = inMesh.getEdges().get(edgeIndex2 * 3 + 1);
					
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
				float duDir = Math.abs(-uDir2 - uDir);
				float dvDir = Math.abs(-vDir2 - vDir);
				if(duDir > 0.001f || dvDir > 0.001f)
					continue; // Uv's don't go in the same direction (it's mirrored)
				
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
	
	private static boolean UVsMatch(float u, float v, float u2, float v2) {
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
