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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.export.IndexCache;

public class Occlusion {

	private boolean[] occlusionData;
	private Direction[] directionData;
	private byte[] cornerData;
	private IndexCache edgeIndexCache;
	private int[][] edgeToFaces;
	private int edgeToFacesSize;
	private long[] faceCenters;
	
	public Occlusion() {
		occlusionData = new boolean[32];
		directionData = new Direction[32];
		cornerData = new byte[32];
		edgeIndexCache = new IndexCache();
		edgeToFaces = new int[32][];
		edgeToFacesSize = 0;
		faceCenters = new long[32*3];
	}
	
	private int calcEdgeOffset(ModelFace face) {
		// Tangents get calculated once, using the main UV set.
		// Since the corner atlas is generally a normal map,
		// it needs to use the same tangents. UVs can be rotated,
		// which causes the tangents to differ from normal.
		// We need to correct for this. In other words, the direction
		// that UVs from vertex 0 to vertex 1 go towards, needs
		// to be the same for the main UV set and the corner UV set.
		// 
		// By default, vertex 0 to vertex 1 is seen as the bottom edge
		// and goes to the right in UV space. If this doesn't match
		// with our UVs, then we need to adjust the edge index.
		// We can do this by calculating the offset to apply to the edge index.
		float u0 = face.getUVs()[0];
		float v0 = face.getUVs()[1];
		float u1 = face.getUVs()[2];
		float v1 = face.getUVs()[3];
		float du = u1 - u0;
		float dv = v1 - v0;
		// We assume that the vector (du, dv) can point in one of four
		// directions: to the right, up, to the left, and down.
		// We check which one it is and that determines the offset.
		int edgeOffset = 0;
		if(Math.abs(du) >= Math.abs(dv)) {
			// du is the more significant value,
			// so it's either to the right or to the left.
			// To the right is an offset of 0.
			// To the left is an offset of 2.
			// If it points to the left, then we did a 180 degree
			// rotation, so it's an offset of 2.
			edgeOffset = du > 0.0f ? 0 : 2;
		}else {
			// dv is the more significant value,
			// so it's either up or down
			// Up is an offset of 1.
			// Down is an offset of 3.
			// Up means a 90 degree rotation.
			// Down means a 270 degree rotation.
			edgeOffset = dv > 0.0f ? 1 : 3;
		}
		return edgeOffset;
	}
	
	public void calculateCornerDataForModel(List<Model> models, BakedBlockState state, long occlusion, 
											List<ModelFace> detailedOcclusionFaces) {
		edgeIndexCache.clear();
		for(int i = 0; i < edgeToFacesSize; ++i) {
			if(edgeToFaces[i] != null)
				edgeToFaces[i][0] = 0;
		}
		edgeToFacesSize = 0;
		
		Model model = null;
		ModelFace face = null;
		int faceIndex = 0;
		boolean hasUnoccludedFace = false;
		
		// First discover all shared edges
		for(int i = 0; i < models.size(); ++i) {
			model = models.get(i);
			
			if(occlusionData.length < (faceIndex + model.getFaces().size())) {
				occlusionData = Arrays.copyOf(occlusionData, faceIndex + model.getFaces().size());
				directionData = Arrays.copyOf(directionData, faceIndex + model.getFaces().size());
			}
			
			for(int j = 0; j < model.getFaces().size(); ++j) {
				face = model.getFaces().get(j);
				
				// Calculate whether the face is occluded
				occlusionData[faceIndex] = false;
				directionData[faceIndex] = face.getDirection();
				if(face.getOccludedBy() != 0 && (face.getOccludedBy() & occlusion) == face.getOccludedBy())
					occlusionData[faceIndex] = true;
				else if(state.isDetailedOcclusion() && face.getOccludedBy() != 0) {
					boolean occluded = false;
					for(ModelFace face2 : detailedOcclusionFaces) {
						if(getDetailedOcclusion(face, face2)) {
							occluded = true;
							break;
						}
					}
					occlusionData[faceIndex] = occluded;
				}
				if(occlusionData[faceIndex] == false)
					hasUnoccludedFace = true;
				
				if(Config.calculateCornerUVs) {
					float centerX = 0f;
					float centerY = 0f;
					float centerZ = 0f;
					for(int edgeIndex = 0; edgeIndex < 4; ++edgeIndex) {
						float x0 = face.getPoints()[edgeIndex * 3];
						float y0 = face.getPoints()[edgeIndex * 3 + 1];
						float z0 = face.getPoints()[edgeIndex * 3 + 2];
						centerX += x0;
						centerY += y0;
						centerZ += z0;
						
						float x1 = face.getPoints()[((edgeIndex + 1) % 4) * 3];
						float y1 = face.getPoints()[((edgeIndex + 1) % 4) * 3 + 1];
						float z1 = face.getPoints()[((edgeIndex + 1) % 4) * 3 + 2];
						
						long edgeId1 = positionToIndex(x0, y0, z0);
						long edgeId2 = positionToIndex(x1, y1, z1);
						// Order of the two vertices doesn't matter, so we just quickly
						// sort the two vertices.
						long edgeId = edgeId1 < edgeId2 ? ((edgeId1 << 32) | edgeId2) : ((edgeId2 << 32) | edgeId1);
						
						int edgeToFaceIndex = edgeIndexCache.getOrDefault(edgeId, -1);
						if(edgeToFaceIndex == -1) {
							edgeToFaceIndex = edgeToFacesSize;
							edgeToFacesSize++;
							edgeIndexCache.put(edgeId, edgeToFaceIndex);
							while(edgeToFaces.length <= edgeToFaceIndex) {
								edgeToFaces = Arrays.copyOf(edgeToFaces, edgeToFaces.length * 2);
							}
						}
						int[] facesArray = edgeToFaces[edgeToFaceIndex];
						if(facesArray == null) {
							facesArray = new int[5];
							facesArray[0] = 0;
							edgeToFaces[edgeToFaceIndex] = facesArray;
						}
						// Index 0 contains the size of the array.
						facesArray[0] += 1;
						if(facesArray.length <= facesArray[0]) {
							facesArray = Arrays.copyOf(facesArray, facesArray.length * 2);
							edgeToFaces[edgeToFaceIndex] = facesArray;
						}
						// Store the index of the face.
						facesArray[facesArray[0]] = faceIndex;
					}
					centerX *= 0.25f;
					centerY *= 0.25f;
					centerZ *= 0.25f;
					if(faceCenters.length <= (faceIndex * 3 + 2)) {
						faceCenters = Arrays.copyOf(faceCenters, Math.max(faceCenters.length * 2, (faceIndex + 1) * 3));
					}
					faceCenters[faceIndex*3] = (long) (centerX * 10f);
					faceCenters[faceIndex*3 + 1] = (long) (centerY * 10f);
					faceCenters[faceIndex*3 + 2] = (long) (centerZ * 10f);
				}
				
				faceIndex++;
			}
		}
		
		// No need to do a bunch of calculations
		// if the face is occluded anyways.
		if(!hasUnoccludedFace || !Config.calculateCornerUVs)
			return;
		
		
		if(cornerData.length < faceIndex) {
			cornerData = Arrays.copyOf(cornerData, faceIndex);
		}
		
		faceIndex = 0;
		// Now calculate corner data
		for(int i = 0; i < models.size(); ++i) {
			model = models.get(i);
			
			for(int j = 0; j < model.getFaces().size(); ++j) {
				if(occlusionData[faceIndex]) {
					// Face is occluded, so no need to calculate corner data.
					faceIndex++;
					continue;
				}
				
				face = model.getFaces().get(j);
				
				int edgeOffset = calcEdgeOffset(face);
				
				byte connectionData = 0;
				
				Direction faceDir = directionData[faceIndex];
				Direction oppositeFaceDir = faceDir.getOpposite();
				
				long centerX = faceCenters[faceIndex*3];
				long centerY = faceCenters[faceIndex*3 + 1];
				long centerZ = faceCenters[faceIndex*3 + 2];
				
				for(int edgeIndex = 0; edgeIndex < 4; ++edgeIndex) {
					float x0 = face.getPoints()[edgeIndex * 3];
					float y0 = face.getPoints()[edgeIndex * 3 + 1];
					float z0 = face.getPoints()[edgeIndex * 3 + 2];
					
					float x1 = face.getPoints()[((edgeIndex + 1) % 4) * 3];
					float y1 = face.getPoints()[((edgeIndex + 1) % 4) * 3 + 1];
					float z1 = face.getPoints()[((edgeIndex + 1) % 4) * 3 + 2];
					
					long edgeId1 = positionToIndex(x0, y0, z0);
					long edgeId2 = positionToIndex(x1, y1, z1);
					// Order of the two vertices doesn't matter, so we just quickly
					// sort the two vertices.
					long edgeId = edgeId1 < edgeId2 ? ((edgeId1 << 32) | edgeId2) : ((edgeId2 << 32) | edgeId1);
					
					int edgeToFaceIndex = edgeIndexCache.getOrDefault(edgeId, -1);
					if(edgeToFaceIndex < 0)
						continue;
					
					int[] facesArray = edgeToFaces[edgeToFaceIndex];
					
					boolean isConnected = false;
					boolean acceptableFaceFound = false;
					
					for(int k = 0; k < facesArray[0]; ++k) {
						int sampleFaceIndex = facesArray[k + 1];
						if(sampleFaceIndex == faceIndex)
							continue;
						
						if(directionData[sampleFaceIndex].id == faceDir.id || 
								directionData[sampleFaceIndex].id == oppositeFaceDir.id) {
							// If it's connected to a different face with the same or opposite normal
							// then we don't want to put in an edge.
							// Otherwise, we ignore the face.
							long sampleCenterX = faceCenters[sampleFaceIndex * 3];
							long sampleCenterY = faceCenters[sampleFaceIndex * 3 + 1];
							long sampleCenterZ = faceCenters[sampleFaceIndex * 3 + 2];
							if(sampleCenterX == centerX && sampleCenterY == centerY && sampleCenterZ == centerZ)
								continue;
							
							// It's not connected to a duplicate of itself,
							// but instead to another face with the same normals.
							// So we don't want to put in an edge.
							acceptableFaceFound = false;
							break;
						}
						// If we are dealing with leaves and the sample face is against
						// another leaves block but not being occluded, then it can create
						// an corner even though we don't want to.
						// occlusion has a bit for when this is the case, so read it.
						if((occlusion >>> (directionData[sampleFaceIndex].id + 32) & 0b1) == 1) {
							// We don't want to put an edge here.
							acceptableFaceFound = false;
							break;
						}
						
						acceptableFaceFound = true;
						// If one of the connected faces isn't occluded,
						// then this face is connected. Only if all of the
						// connected faces are occluded, is this face not connected.
						if(occlusionData[sampleFaceIndex] == false)
							isConnected = true;
					}
					
					if(isConnected && acceptableFaceFound)
						connectionData |= 1 << ((edgeIndex + edgeOffset) % 4);
				}
				
				cornerData[faceIndex] = connectionData;
				
				faceIndex++;
			}
		}
	}
	
	private boolean getDetailedOcclusion(ModelFace faceA, ModelFace faceB) {
		if(faceA.getDirection() != faceB.getDirection() && faceA.getDirection() != faceB.getDirection().getOpposite())
			return false;
		float[] minMaxA = getMinMaxPoints(faceA);
		float[] minMaxB = getMinMaxPoints(faceB);
		switch(faceA.getDirection()) {
		case UP:
		case DOWN:
			if(faceA.getPoints()[1] != faceB.getPoints()[1])
				return false;
			return faceOccludedByOtherFace(minMaxA[0], minMaxA[2], minMaxA[3], minMaxA[5], 
											minMaxB[0], minMaxB[2], minMaxB[3], minMaxB[5]);
		case NORTH:
		case SOUTH:
			if(faceA.getPoints()[2] != faceB.getPoints()[2])
				return false;
			return faceOccludedByOtherFace(minMaxA[0], minMaxA[1], minMaxA[3], minMaxA[4], 
											minMaxB[0], minMaxB[1], minMaxB[3], minMaxB[4]);
		case EAST:
		case WEST:
			if(faceA.getPoints()[0] != faceB.getPoints()[0])
				return false;
			return faceOccludedByOtherFace(minMaxA[2], minMaxA[1], minMaxA[5], minMaxA[4], 
											minMaxB[2], minMaxB[1], minMaxB[5], minMaxB[4]);
		}
		return false;
	}
	
	private float[] getMinMaxPoints(ModelFace face) {
		return new float[]{
				Math.min(face.getPoints()[0*3+0], face.getPoints()[2*3+0]),
				Math.min(face.getPoints()[0*3+1], face.getPoints()[2*3+1]),
				Math.min(face.getPoints()[0*3+2], face.getPoints()[2*3+2]),
				Math.max(face.getPoints()[0*3+0], face.getPoints()[2*3+0]),
				Math.max(face.getPoints()[0*3+1], face.getPoints()[2*3+1]),
				Math.max(face.getPoints()[0*3+2], face.getPoints()[2*3+2]),
		};
	}
	
	private boolean faceOccludedByOtherFace(float minXA, float minYA, float maxXA, float maxYA,
											float minXB, float minYB, float maxXB, float maxYB) {
		return minXA >= minXB && minYA >= minYB && maxXA <= maxXB && maxYA <= maxYB;
	}
	
	private long positionToIndex(float x, float y, float z) {
		x += 16f;
		y += 16f;
		z += 16f;
		x *= 4f;
		y *= 4f;
		z *= 4f;
		x += 0.5f;
		y += 0.5f;
		z += 0.5f;
		x = Math.min(Math.max(x, 0f), 255f);
		y = Math.min(Math.max(y, 0f), 255f);
		z = Math.min(Math.max(z, 0f), 255f);
		return (((long) x) << 16) | (((long) y) << 8) | ((long) z);
	}
	
	public int getCornerIndexForFace(ModelFace face, int faceIndex) {
		if(!Config.calculateCornerUVs)
			return 0;
		
		int cornerData = (int) this.cornerData[faceIndex];
		
		int edgeOffset = calcEdgeOffset(face);
		int v0 = edgeOffset * 3;
		int v1 = ((1 + edgeOffset) % 4) * 3;
		int v3 = ((3 + edgeOffset) % 4) * 3;
		
		float widthX = face.getPoints()[v0 + 0] - face.getPoints()[v1 + 0];
		float widthY = face.getPoints()[v0 + 1] - face.getPoints()[v1 + 1];
		float widthZ = face.getPoints()[v0 + 2] - face.getPoints()[v1 + 2];
		float width = (float) Math.sqrt(widthX * widthX + widthY * widthY + widthZ * widthZ);
		float heightX = face.getPoints()[v0 + 0] - face.getPoints()[v3 + 0];
		float heightY = face.getPoints()[v0 + 1] - face.getPoints()[v3 + 1];
		float heightZ = face.getPoints()[v0 + 2] - face.getPoints()[v3 + 2];
		float height = (float) Math.sqrt(heightX * heightX + heightY * heightY + heightZ * heightZ);
		
		int iwidth = floatSizeToInt(width);
		int iheight = floatSizeToInt(height);
		
		return edgeOffset << 24 | cornerData << 16 | iwidth << 8 | iheight;
	}
	
	private int floatSizeToInt(float val) {
		if(val < 1.5f)
			return 1;
		else if(val < 2.5f)
			return 2;
		else if(val < 3.5f)
			return 3;
		else if(val < 4.5f)
			return 4;
		else if(val < 5.5f)
			return 5;
		else if(val < 6.5f)
			return 6;
		else if(val < 7.5f)
			return 7;
		else if(val < 9.0f)
			return 8;
		else if(val < 11.0f)
			return 10;
		else if(val < 13.0f)
			return 12;
		else if(val < 15.0f)
			return 14;
		else
			return 16;
	}
	
	public static void getCornerUVsForIndex(int cornerIndex, float[] outUVs) {
		int edgeOffset = 4 - ((cornerIndex >>> 24) & 0xFF);
		int connectionData = (cornerIndex >> 16) & 0xFF;
		int width = (cornerIndex >> 8) & 0xFF;
		int height = cornerIndex & 0xFF;
		
		if(width <= 0)
			width = 1;
		if(height <= 0)
			height = 1;
		
		int pageX = connectionData % 4;
		int pageY = connectionData / 4;
		
		int xStart = (SIZE_TO_PIXEL_OFFSET[width] + 1) + pageX * 128;
		int yStart = (SIZE_TO_PIXEL_OFFSET[height] + 1) + pageY * 128;
		
		int xEnd = xStart + width;
		int yEnd = yStart + height;
		
		float fxStart = ((float) xStart) / 512f;
		float fyStart = ((float) yStart) / 512f;
		float fxEnd = ((float) xEnd) / 512f;
		float fyEnd = ((float) yEnd) / 512f;
		fyStart = 1f - fyStart;
		fyEnd = 1f - fyEnd;
		
		outUVs[(0 + edgeOffset * 2) % 8] = fxStart;
		outUVs[(1 + edgeOffset * 2) % 8] = fyEnd;
		
		outUVs[(2 + edgeOffset * 2) % 8] = fxEnd;
		outUVs[(3 + edgeOffset * 2) % 8] = fyEnd;
		
		outUVs[(4 + edgeOffset * 2) % 8] = fxEnd;
		outUVs[(5 + edgeOffset * 2) % 8] = fyStart;
		
		outUVs[(6 + edgeOffset * 2) % 8] = fxStart;
		outUVs[(7 + edgeOffset * 2) % 8] = fyStart;
	}
	
	public boolean isFaceOccluded(int faceIndex) {
		return occlusionData[faceIndex];
	}
	
	private static int SIZE_TO_PIXEL_OFFSET[] = new int[17];
	static {
		int j = 0;
		for(int i = 1; i <= 17; i += (i < 8 ? 1 : 2)) {
			SIZE_TO_PIXEL_OFFSET[i] = j;
			j += i + 2; // + 2 to add one pixel of padding on either size
		}
	}
	
	public static BufferedImage generateEdgeNormalMapAtlas(int scale) {
		BufferedImage img = new BufferedImage(1024 * scale, 1024 * scale, BufferedImage.TYPE_INT_RGB);
		
		for(int connectionData = 0; connectionData < 16; connectionData++) {
			int pageX = connectionData % 4;
			int pageY = connectionData / 4;
			boolean topConnected = (connectionData & 0b0100) > 0;
			boolean bottomConnected = (connectionData & 0b0001) > 0;
			boolean leftConnected = (connectionData & 0b1000) > 0;
			boolean rightConnected = (connectionData & 0b0010) > 0;
			
			for(int width = 1; width <= 17; width += (width < 8 ? 1 : 2)) {
				for(int height = 1; height <= 17; height += (height < 8 ? 1 : 2)) {
					int xStart = (SIZE_TO_PIXEL_OFFSET[width] + pageX * 128) * 2 * scale;
					int yStart = (SIZE_TO_PIXEL_OFFSET[height] + pageY * 128) * 2 * scale;
					
					int fullWidth = (width + 2) * 2 * scale;
					int fullHeight = (height + 2) * 2 * scale;
					
					for(int y = 0; y < fullHeight; ++y) {
						for(int x = 0; x < fullWidth; ++x) {
							boolean topEdge = y < (2 * scale + 1);
							boolean bottomEdge = y >= (fullHeight - (2 * scale + 1));
							boolean leftEdge = x < (2 * scale + 1);
							boolean rightEdge = x >= (fullWidth - (2 * scale + 1));
							
							float nx = 0f;
							float ny = 0f;
							float nz = 1f;
							if(topEdge && topConnected)
								ny = 1f;
							if(bottomEdge && bottomConnected)
								ny = -1f;
							if(leftEdge && leftConnected)
								nx = -1f;
							if(rightEdge && rightConnected)
								nx = 1f;
							
							float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
							nx /= length;
							ny /= length;
							nz /= length;
							
							nx = (nx + 1f) / 2f;
							ny = (ny + 1f) / 2f;
							nz = (nz + 1f) / 2f;
							
							img.setRGB(xStart + x, yStart + y, new java.awt.Color(nx, ny, nz).getRGB());
						}
					}
				}
			}
		}
		
		return img;
	}
	
}
