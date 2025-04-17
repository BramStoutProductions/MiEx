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

import java.util.List;

public class Subdivider {
	
	public static void subdivideModelForOcclusion(List<Model> models, long occlusion) {
		// First we need to check the occlusion data to see 
		// if we even need to do any subdivisions.
		byte[] needsSubdivides = new byte[] { 0, 0, 0, 0, 0, 0 };
		boolean hasNeedsSubdivide = false;
		for(Direction dir : Direction.CACHED_VALUES) {
			needsSubdivides[dir.id] = needsSubdivide(occlusion >> (dir.id * 4));
			if(needsSubdivides[dir.id] != 0)
				hasNeedsSubdivide = true;
		}
				
		if(!hasNeedsSubdivide) {
			// None of the directions have occlusion that requires subdivisions, so we
			// can just end it here.
			return;
		}
		
		// If a face needs the subdivision, the subdivision needs to be a full loop,
		// so we now need to propagate the needsSubdivide values to make sure this happens.
		propagateNeedsSubdivides(needsSubdivides);
		
		// Now we go through all models and then all faces of every model,
		// and if a face requires to be subdivided, then copy the model and
		// subdivide that face in the copy of the model.
		for(int i = 0; i < models.size(); ++i) {
			Model model = models.get(i);
			
			boolean hasMadeCopy = false;
			
			int numFaces = model.getFaces().size();
			
			for(int j = 0; j < numFaces; ++j) {
				ModelFace face = model.getFaces().get(j);
				ModelFace face2Hor = null;
				
				byte needsSubdivide = needsSubdivides[face.getDirection().id];
				if(needsSubdivide == 0)
					continue;
				
				//long occludedBy = face.getOccludedBy() >> (face.getDirection().id * 4);
				
				if((needsSubdivide & 0b1) != 0) {
					//if(faceNeedsSubdivideHorizontal(occludedBy)) {
						// Subdivide the face horizontally.
						if(!hasMadeCopy) {
							model = new Model(model);
							hasMadeCopy = true;
							
							// Also update face to point to the copy.
							face = model.getFaces().get(j);
						}
						
						face2Hor = subdivideFaceHorizontal(face);
						if(face2Hor != null)
							model.getFaces().add(face2Hor);
					//}
				}
				
				if((needsSubdivide & 0b10) != 0) {
					//if(faceNeedsSubdivideVertical(occludedBy)) {
						// Subdivide the face vertically.
						if(!hasMadeCopy) {
							model = new Model(model);
							hasMadeCopy = true;
							
							// Also update face to point to the copy.
							face = model.getFaces().get(j);
						}
						
						ModelFace face2Vert = subdivideFaceVertical(face);
						if(face2Vert != null)
							model.getFaces().add(face2Vert);
						
						if(face2Hor != null) {
							// We had previously subdivided the face horizontally,
							// so we also need to vertically subdivide the second face.
							face2Vert = subdivideFaceVertical(face2Hor);
							if(face2Vert != null)
								model.getFaces().add(face2Vert);
						}
					//}
				}
			}
			
			if(hasMadeCopy)
				models.set(i, model);
		}
	}
	
	private static byte needsSubdivide(long dirOcclusion) {
		return (byte) ((needsSubdivideHorizontal(dirOcclusion) ? 1 : 0) | 
					(needsSubdivideVertical(dirOcclusion) ? 2 : 0));
	}
	
	private static boolean needsSubdivideHorizontal(long dirOcclusion) {
		// We need to horizontally subdivide if the top and bottom occlusion don't match.
		return (dirOcclusion & 0b11) != ((dirOcclusion >> 2) & 0b11);
	}
	
	private static boolean needsSubdivideVertical(long dirOcclusion) {
		// We need to vertically subdivide if the left and right occlusion don't match.
		return (dirOcclusion & 0b1) != ((dirOcclusion >> 1) & 0b1) || 
				((dirOcclusion >> 2) & 0b1) != ((dirOcclusion >> 3) & 0b1);
	}
	
	private static void propagateNeedsSubdivides(byte[] needsSubdivisions) {
		// There are three possible planes on which we can subdivide and if any of the faces
		// needs a subdivision on that plane, then we need to do that for all faces to make sure
		// the edges remain correct. So we first look through all data to find whether we need
		// any of these planes and then update the data based on that.
		
		boolean xPlane = false;
		boolean yPlane = false;
		boolean zPlane = false;
		
		// X plane:
		// up vertical, down vertical, north vertical, south vertical.
		xPlane = xPlane || (needsSubdivisions[Direction.UP.id] & 0b10) != 0;
		xPlane = xPlane || (needsSubdivisions[Direction.DOWN.id] & 0b10) != 0;
		xPlane = xPlane || (needsSubdivisions[Direction.NORTH.id] & 0b10) != 0;
		xPlane = xPlane || (needsSubdivisions[Direction.SOUTH.id] & 0b10) != 0;
		
		// Y plane:
		// north horizontal, south horizontal, east horizontal, west horizontal.
		yPlane = yPlane || (needsSubdivisions[Direction.NORTH.id] & 0b1) != 0;
		yPlane = yPlane || (needsSubdivisions[Direction.SOUTH.id] & 0b1) != 0;
		yPlane = yPlane || (needsSubdivisions[Direction.EAST.id] & 0b1) != 0;
		yPlane = yPlane || (needsSubdivisions[Direction.WEST.id] & 0b1) != 0;
		
		// up horizontal, down horizontal, east vertical, west vertical.
		zPlane = zPlane || (needsSubdivisions[Direction.UP.id] & 0b1) != 0;
		zPlane = zPlane || (needsSubdivisions[Direction.DOWN.id] & 0b1) != 0;
		zPlane = zPlane || (needsSubdivisions[Direction.EAST.id] & 0b10) != 0;
		zPlane = zPlane || (needsSubdivisions[Direction.WEST.id] & 0b10) != 0;
		
		// Z plane:
		
		// Now we update the needsSubdivisions based on these planes.
		needsSubdivisions[0] = 0;
		needsSubdivisions[1] = 0;
		needsSubdivisions[2] = 0;
		needsSubdivisions[3] = 0;
		needsSubdivisions[4] = 0;
		needsSubdivisions[5] = 0;
		
		if(xPlane) {
			needsSubdivisions[Direction.UP.id] |= 0b10;
			needsSubdivisions[Direction.DOWN.id] |= 0b10;
			needsSubdivisions[Direction.NORTH.id] |= 0b10;
			needsSubdivisions[Direction.SOUTH.id] |= 0b10;
		}
		
		if(yPlane) {
			needsSubdivisions[Direction.NORTH.id] |= 0b1;
			needsSubdivisions[Direction.SOUTH.id] |= 0b1;
			needsSubdivisions[Direction.EAST.id] |= 0b1;
			needsSubdivisions[Direction.WEST.id] |= 0b1;
		}
		
		if(zPlane) {
			needsSubdivisions[Direction.UP.id] |= 0b1;
			needsSubdivisions[Direction.DOWN.id] |= 0b1;
			needsSubdivisions[Direction.EAST.id] |= 0b10;
			needsSubdivisions[Direction.WEST.id] |= 0b10;
		}
	}
	
	/*private static boolean faceNeedsSubdivideHorizontal(long occludedBy) {
		// If the face spans the top and bottom, we need to add in a horizontal subdivison.
		return ((occludedBy & 0b1) != 0 && ((occludedBy >> 2) & 0b1) != 0) || 
				(((occludedBy >> 1) & 0b1) != 0 && ((occludedBy >> 3) & 0b1) != 0);
	}
	
	private static boolean faceNeedsSubdivideVertical(long occludedBy) {
		// If the face spans left and right, we need to add in a vertical subdivison.
		return ((occludedBy & 0b1) != 0 && ((occludedBy >> 1) & 0b1) != 0) || 
				(((occludedBy >> 2) & 0b1) != 0 && ((occludedBy >> 3) & 0b1) != 0);
	}*/
	
	/***
	 * Subdivides the given face by updating the given face and returning
	 * the extra face.
	 * @param faceA
	 * @return The second face created by the subdivision
	 */
	private static ModelFace subdivideFaceHorizontal(ModelFace faceA) {
		// 0: x plane, 1: y plane, 2: z plane.
		int planeId = 0;
		switch(faceA.getDirection()) {
		case UP:
			planeId = 2;
			break;
		case DOWN:
			planeId = 2;
			break;
		case NORTH:
			planeId = 1;
			break;
		case SOUTH:
			planeId = 1;
			break;
		case EAST:
			planeId = 1;
			break;
		case WEST:
			planeId = 1;
			break;
		}
		
		return subdivideFace(faceA, planeId);
	}
	
	/***
	 * Subdivides the given face by updating the given face and returning
	 * the extra face.
	 * @param faceA
	 * @return The second face created by the subdivision
	 */
	private static ModelFace subdivideFaceVertical(ModelFace faceA) {
		// 0: x plane, 1: y plane, 2: z plane.
		int planeId = 0;
		switch(faceA.getDirection()) {
		case UP:
			planeId = 0;
			break;
		case DOWN:
			planeId = 0;
			break;
		case NORTH:
			planeId = 0;
			break;
		case SOUTH:
			planeId = 0;
			break;
		case EAST:
			planeId = 2;
			break;
		case WEST:
			planeId = 2;
			break;
		}
		
		return subdivideFace(faceA, planeId);
	}
	
	private static ModelFace subdivideFace(ModelFace faceA, int planeId) {
		// First we need to find the edge pair to split on.
		// And the distance along the edge of the split.
		int edgeId = 0;
		float t = -1f;
		
		t = calcSplit(faceA.getPoints(), planeId, 0);
		if(t < 0f) {
			// No proper split, so check the other edge pair.
			t = calcSplit(faceA.getPoints(), planeId, 1);
			edgeId = 1;
		}
		
		if(t < 0f) {
			// Still no proper split, so we don't need to subdivide this face.
			return null;
		}
		
		// Now we make a copy of the face and then move the vertices of the faces
		// based on t
		ModelFace faceB = new ModelFace(faceA);
		
		moveFace(faceA, t, edgeId);
		moveFace(faceB, 1f - t, edgeId + 2);
		
		return faceB;
	}
	
	private static void moveFace(ModelFace face, float t, int edgeId) {
		int i00 = ((edgeId    ) & 0b11) * 3;
		int i10 = ((edgeId + 3) & 0b11) * 3;
		int i01 = ((edgeId + 1) & 0b11) * 3;
		int i11 = ((edgeId + 2) & 0b11) * 3;
		int uvi00 = ((edgeId    ) & 0b11) * 2;
		int uvi10 = ((edgeId + 3) & 0b11) * 2;
		int uvi01 = ((edgeId + 1) & 0b11) * 2;
		int uvi11 = ((edgeId + 2) & 0b11) * 2;
		face.getPoints()[i01  ] = (face.getPoints()[i01  ] - face.getPoints()[i00  ]) * t + face.getPoints()[i00  ];
		face.getPoints()[i01+1] = (face.getPoints()[i01+1] - face.getPoints()[i00+1]) * t + face.getPoints()[i00+1];
		face.getPoints()[i01+2] = (face.getPoints()[i01+2] - face.getPoints()[i00+2]) * t + face.getPoints()[i00+2];
		
		face.getPoints()[i11  ] = (face.getPoints()[i11  ] - face.getPoints()[i10  ]) * t + face.getPoints()[i10  ];
		face.getPoints()[i11+1] = (face.getPoints()[i11+1] - face.getPoints()[i10+1]) * t + face.getPoints()[i10+1];
		face.getPoints()[i11+2] = (face.getPoints()[i11+2] - face.getPoints()[i10+2]) * t + face.getPoints()[i10+2];
		
		
		face.getUVs()[uvi01  ] = (face.getUVs()[uvi01  ] - face.getUVs()[uvi00  ]) * t + face.getUVs()[uvi00  ];
		face.getUVs()[uvi01+1] = (face.getUVs()[uvi01+1] - face.getUVs()[uvi00+1]) * t + face.getUVs()[uvi00+1];
		
		face.getUVs()[uvi11  ] = (face.getUVs()[uvi11  ] - face.getUVs()[uvi10  ]) * t + face.getUVs()[uvi10  ];
		face.getUVs()[uvi11+1] = (face.getUVs()[uvi11+1] - face.getUVs()[uvi10+1]) * t + face.getUVs()[uvi10+1];
		
		float[] minMaxPoints = {
				Math.min(face.getPoints()[0*3+0], face.getPoints()[2*3+0]),
				Math.min(face.getPoints()[0*3+1], face.getPoints()[2*3+1]),
				Math.min(face.getPoints()[0*3+2], face.getPoints()[2*3+2]),
				Math.max(face.getPoints()[0*3+0], face.getPoints()[2*3+0]),
				Math.max(face.getPoints()[0*3+1], face.getPoints()[2*3+1]),
				Math.max(face.getPoints()[0*3+2], face.getPoints()[2*3+2]),
		};
		face.calculateOcclusion(minMaxPoints);
	}
	
	private static float calcSplit(float[] points, int planeId, int edgeId) {
		if(planeId == 0) {
			// X plane, so check if the points are opposite sides of the x plane.
			float x0 = points[edgeId * 3];
			float x1 = points[(edgeId+1) * 3];
			boolean side0 = x0 >= 8f;
			boolean side1 = x1 >= 8f;
			
			if(side0 == side1)
				return -1f;
			
			// They aren't on the same side, so I can calculate the split factor.
			float d = Math.abs(x1 - x0);
			if(d <= 0.01f)
				return -1f;
			
			return Math.abs(8f - x0) / d;
		}
		if(planeId == 1) {
			// Y plane, so check if the points are opposite sides of the y plane.
			float y0 = points[edgeId * 3 + 1];
			float y1 = points[(edgeId+1) * 3 + 1];
			boolean side0 = y0 >= 8f;
			boolean side1 = y1 >= 8f;
			
			if(side0 == side1)
				return -1f;
			
			// They aren't on the same side, so I can calculate the split factor.
			float d = Math.abs(y1 - y0);
			if(d <= 0.01f)
				return -1f;
			
			return Math.abs(8f - y0) / d;
		}
		if(planeId == 2) {
			// Z plane, so check if the points are opposite sides of the z plane.
			float z0 = points[edgeId * 3 + 2];
			float z1 = points[(edgeId+1) * 3 + 2];
			boolean side0 = z0 >= 8f;
			boolean side1 = z1 >= 8f;
			
			if(side0 == side1)
				return -1f;
			
			// They aren't on the same side, so I can calculate the split factor.
			float d = Math.abs(z1 - z0);
			if(d <= 0.01f)
				return -1f;
			
			return Math.abs(8f - z0) / d;
		}
		return -1f;
	}
	
}
