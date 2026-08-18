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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.math.Matrix;

public class ModelFace {
	
	public static class FaceData{
		
		public String texture = null;
		public int tintindex = -1;
		public boolean hasUV = false;
		public float uv0 = 0;
		public float uv1 = 0;
		public float uv2 = 0;
		public float uv3 = 0;
		public boolean hasRotation = false;
		public float rotation = 0;
		public boolean rotationMiEx = false;
		
		public FaceData() {}
		
		public FaceData(JsonObject data) {
			if(data == null)
				return;
			if(data.has("texture"))
				texture = data.get("texture").getAsString();
			if(data.has("tintindex"))
				tintindex = data.get("tintindex").getAsInt();
			if(data.has("uv")) {
				JsonArray uvs = data.getAsJsonArray("uv");
				if(uvs.size() >= 4) {
					uv0 = uvs.get(0).getAsFloat();
					uv1 = uvs.get(1).getAsFloat();
					uv2 = uvs.get(2).getAsFloat();
					uv3 = uvs.get(3).getAsFloat();
					hasUV = true;
				}
			}
			if(data.has("rotation")) {
				rotation = data.get("rotation").getAsFloat();
				hasRotation = true;
				if(data.has("rotationMiEx"))
					rotationMiEx = data.get("rotationMiEx").getAsBoolean();
			}
		}
		
	}
	
	public static final String SHADING_MODE_STANDARD = "standard".intern();
	public static final String SHADING_MODE_FLAT = "flat".intern();
	public static final String SHADING_MODE_FULLBRIGHT = "fullbright".intern();
	public static final String SHADING_MODE_REFLECTIVE = "reflective".intern();

	public float point0X;
	public float point0Y;
	public float point0Z;
	public float point1X;
	public float point1Y;
	public float point1Z;
	public float point2X;
	public float point2Y;
	public float point2Z;
	public float point3X;
	public float point3Y;
	public float point3Z;
	public float uvs0U;
	public float uvs0V;
	public float uvs1U;
	public float uvs1V;
	public float uvs2U;
	public float uvs2V;
	public float uvs3U;
	public float uvs3V;
	public boolean hasVertexColor;
	public float vertexColorR;
	public float vertexColorG;
	public float vertexColorB;
	private long occludes;
	private long occludedBy;
	private long texture;
	private Direction direction;
	private boolean doubleSided;
	private int tintIndex;
	private String shadingMode;

	public ModelFace(ModelFace other) {
		//points = Arrays.copyOf(other.points, other.points.length);
		//uvs = Arrays.copyOf(other.uvs, other.uvs.length);
		point0X = other.point0X;
		point0Y = other.point0Y;
		point0Z = other.point0Z;
		point1X = other.point1X;
		point1Y = other.point1Y;
		point1Z = other.point1Z;
		point2X = other.point2X;
		point2Y = other.point2Y;
		point2Z = other.point2Z;
		point3X = other.point3X;
		point3Y = other.point3Y;
		point3Z = other.point3Z;
		uvs0U = other.uvs0U;
		uvs0V = other.uvs0V;
		uvs1U = other.uvs1U;
		uvs1V = other.uvs1V;
		uvs2U = other.uvs2U;
		uvs2V = other.uvs2V;
		uvs3U = other.uvs3U;
		uvs3V = other.uvs3V;
		hasVertexColor = other.hasVertexColor;
		vertexColorR = other.vertexColorR;
		vertexColorG = other.vertexColorG;
		vertexColorB = other.vertexColorB;
		occludes = other.occludes;
		occludedBy = other.occludedBy;
		texture = other.texture;
		direction = other.direction;
		doubleSided = other.doubleSided;
		tintIndex = other.tintIndex;
		shadingMode = other.shadingMode;	}

	public ModelFace(float[] points, float[] uvs, String texture, int tintIndex, Direction direction, boolean doubleSided, String shadingMode) {
		if(points.length != (4 * 3))
			throw new RuntimeException("Incorrect number of points");
		if(uvs.length != (4 * 2))
			throw new RuntimeException("Incorrect number of uvs");
		//this.points = points;
		//this.uvs = uvs;
		point0X = points[0];
		point0Y = points[1];
		point0Z = points[2];
		point1X = points[3];
		point1Y = points[4];
		point1Z = points[5];
		point2X = points[6];
		point2Y = points[7];
		point2Z = points[8];
		point3X = points[9];
		point3Y = points[10];
		point3Z = points[11];
		uvs0U = uvs[0];
		uvs0V = uvs[1];
		uvs1U = uvs[2];
		uvs1V = uvs[3];
		uvs2U = uvs[4];
		uvs2V = uvs[5];
		uvs3U = uvs[6];
		uvs3V = uvs[7];
		hasVertexColor = false;
		occludes = 0;
		occludedBy = 0;
		this.direction = direction;
		this.doubleSided = doubleSided;
		this.texture = TextureRegistry.getIdFromTexture(texture, 0);
		this.tintIndex = tintIndex;
		this.shadingMode = shadingMode;
		
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	public ModelFace(float[] minMaxPoints, Direction direction, FaceData faceData, boolean doubleSided, String shadingMode) {
		//points = new float[4 * 3];
		//uvs = new float[4 * 2];
		hasVertexColor = false;
		occludes = 0;
		occludedBy = 0;
		this.direction = direction;
		this.doubleSided = doubleSided;
		this.shadingMode = shadingMode;

		texture = 0L;
		if (faceData != null && faceData.texture != null)
			texture = TextureRegistry.getIdFromTexture(faceData.texture, 0);
		
		tintIndex = -1;
		if(faceData != null)
			tintIndex = faceData.tintindex;

		float minU = 0.0f;
		float minV = 0.0f;
		float maxU = 0.0f;
		float maxV = 0.0f;

		switch (direction) {
		case DOWN:
			point0X = minMaxPoints[0];
			point0Y = minMaxPoints[1];
			point0Z = minMaxPoints[2];

			point1X = minMaxPoints[3];
			point1Y = minMaxPoints[1];
			point1Z = minMaxPoints[2];

			point2X = minMaxPoints[3];
			point2Y = minMaxPoints[1];
			point2Z = minMaxPoints[5];

			point3X = minMaxPoints[0];
			point3Y = minMaxPoints[1];
			point3Z = minMaxPoints[5];

			minU = minMaxPoints[0];
			minV = minMaxPoints[2];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[5];
			break;
		case UP:
			point0X = minMaxPoints[0];
			point0Y = minMaxPoints[4];
			point0Z = minMaxPoints[5];

			point1X = minMaxPoints[3];
			point1Y = minMaxPoints[4];
			point1Z = minMaxPoints[5];

			point2X = minMaxPoints[3];
			point2Y = minMaxPoints[4];
			point2Z = minMaxPoints[2];

			point3X = minMaxPoints[0];
			point3Y = minMaxPoints[4];
			point3Z = minMaxPoints[2];

			minU = minMaxPoints[0];
			minV = minMaxPoints[2];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[5];
			break;
		case NORTH:
			point0X = minMaxPoints[3];
			point0Y = minMaxPoints[1];
			point0Z = minMaxPoints[2];

			point1X = minMaxPoints[0];
			point1Y = minMaxPoints[1];
			point1Z = minMaxPoints[2];

			point2X = minMaxPoints[0];
			point2Y = minMaxPoints[4];
			point2Z = minMaxPoints[2];

			point3X = minMaxPoints[3];
			point3Y = minMaxPoints[4];
			point3Z = minMaxPoints[2];

			minU = minMaxPoints[0];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[4];
			break;
		case SOUTH:
			point0X = minMaxPoints[0];
			point0Y = minMaxPoints[1];
			point0Z = minMaxPoints[5];

			point1X = minMaxPoints[3];
			point1Y = minMaxPoints[1];
			point1Z = minMaxPoints[5];

			point2X = minMaxPoints[3];
			point2Y = minMaxPoints[4];
			point2Z = minMaxPoints[5];

			point3X = minMaxPoints[0];
			point3Y = minMaxPoints[4];
			point3Z = minMaxPoints[5];

			minU = minMaxPoints[0];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[4];
			break;
		case WEST:
			point0X = minMaxPoints[0];
			point0Y = minMaxPoints[1];
			point0Z = minMaxPoints[2];

			point1X = minMaxPoints[0];
			point1Y = minMaxPoints[1];
			point1Z = minMaxPoints[5];

			point2X = minMaxPoints[0];
			point2Y = minMaxPoints[4];
			point2Z = minMaxPoints[5];

			point3X = minMaxPoints[0];
			point3Y = minMaxPoints[4];
			point3Z = minMaxPoints[2];

			minU = minMaxPoints[2];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[5];
			maxV = minMaxPoints[4];
			break;
		case EAST:
			point0X = minMaxPoints[3];
			point0Y = minMaxPoints[1];
			point0Z = minMaxPoints[5];

			point1X = minMaxPoints[3];
			point1Y = minMaxPoints[1];
			point1Z = minMaxPoints[2];

			point2X = minMaxPoints[3];
			point2Y = minMaxPoints[4];
			point2Z = minMaxPoints[2];

			point3X = minMaxPoints[3];
			point3Y = minMaxPoints[4];
			point3Z = minMaxPoints[5];

			minU = minMaxPoints[2];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[5];
			maxV = minMaxPoints[4];
			break;
		}

		if (faceData != null && faceData.hasUV) {
			minU = faceData.uv0;
			minV = 16.0f - faceData.uv3;
			maxU = faceData.uv2;
			maxV = 16.0f - faceData.uv1;
		}

		uvs0U = minU;
		uvs0V = minV;

		uvs1U = maxU;
		uvs1V = minV;

		uvs2U = maxU;
		uvs2V = maxV;
		
		uvs3U = minU;
		uvs3V = maxV;

		if (faceData != null && faceData.hasRotation) {
			if(faceData.rotationMiEx) {
				float ouvs0U = uvs0U;
				float ouvs0V = uvs0V;
				float ouvs1U = uvs1U;
				float ouvs1V = uvs1V;
				float ouvs2U = uvs2U;
				float ouvs2V = uvs2V;
				float ouvs3U = uvs3U;
				float ouvs3V = uvs3V;
				
				float rotation = faceData.rotation;
				float cosR = (float) Math.cos(Math.toRadians(rotation));
				float sinR = (float) Math.sin(Math.toRadians(rotation));
				float pivotX = (ouvs0U + ouvs1U + ouvs2U + ouvs3U) / 4.0f;
				float pivotY = (ouvs0V + ouvs1V + ouvs2V + ouvs3V) / 4.0f;
				
				uvs0U = (ouvs0U - pivotX) * cosR + (ouvs0V - pivotY) * -sinR + pivotX;
				uvs0V = (ouvs0U - pivotX) * sinR + (ouvs0V - pivotY) * cosR + pivotY;

				uvs1U = (ouvs1U - pivotX) * cosR + (ouvs1V - pivotY) * -sinR + pivotX;
				uvs1V = (ouvs1U - pivotX) * sinR + (ouvs1V - pivotY) * cosR + pivotY;

				uvs2U = (ouvs2U - pivotX) * cosR + (ouvs2V - pivotY) * -sinR + pivotX;
				uvs2V = (ouvs2U - pivotX) * sinR + (ouvs2V - pivotY) * cosR + pivotY;

				uvs3U = (ouvs3U - pivotX) * cosR + (ouvs3V - pivotY) * -sinR + pivotX;
				uvs3V = (ouvs3U - pivotX) * sinR + (ouvs3V - pivotY) * cosR + pivotY;
			}else {
				float rotation = faceData.rotation;
				while(rotation > 45f) {
					float ouvs0U = uvs0U;
					float ouvs0V = uvs0V;
					float ouvs1U = uvs1U;
					float ouvs1V = uvs1V;
					float ouvs2U = uvs2U;
					float ouvs2V = uvs2V;
					float ouvs3U = uvs3U;
					float ouvs3V = uvs3V;
					
					uvs0U = ouvs1U;
					uvs0V = ouvs1V;
					
					uvs1U = ouvs2U;
					uvs1V = ouvs2V;
					
					uvs2U = ouvs3U;
					uvs2V = ouvs3V;
					
					uvs3U = ouvs0U;
					uvs3V = ouvs0V;
					
					rotation -= 90f;
				}
			}
		}

		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	private void calculateDirection() {
		float x1 = point1X - point0X;
		float y1 = point1Y - point0Y;
		float z1 = point1Z - point0Z;
		float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
		x1 /= length;
		y1 /= length;
		z1 /= length;
		
		float x2 = point3X - point0X;
		float y2 = point3Y - point0Y;
		float z2 = point3Z - point0Z;
		length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
		x2 /= length;
		y2 /= length;
		z2 /= length;
		
		float nx = y1 * z2 - z1 * y2;
		float ny = z1 * x2 - x1 * z2;
		float nz = x1 * y2 - y1 * x2;
		
		float anx = Math.abs(nx);
		float any = Math.abs(ny);
		float anz = Math.abs(nz);
		
		if(anx >= any && anx >= anz) {
			direction = nx >= 0 ? Direction.EAST : Direction.WEST;
		}else if(any >= anz) {
			direction = ny >= 0 ? Direction.UP : Direction.DOWN;
		}else {
			direction = nz >= 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}
	
	public void calculateOcclusion(float minX, float minY, float minZ,
									float maxX, float maxY, float maxZ) {
		occludes = 0;
		occludedBy = 0;
		if((maxX - minX) > 0.01f && 
				(maxY - minY) > 0.01f &&
				(maxZ - minZ) > 0.01f) {
			// If the face isn't perfectly aligned to an axis,
			// then all three axis of the bounding box will be
			// non-zero. In that case, this face also wouldn't
			// be able to occlude other faces or be occluded.
			// So we'll just end it here.
			return;
		}
		
		switch (direction) {
		case DOWN:
			if (minY > -0.02f && minY < 0.001f) {
				occludes = getSideOccludes(minX, minZ, maxX, maxZ) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minX, minZ, maxX, maxZ) << (direction.id * 4);
			}
			break;
		case UP:
			if (maxY > 15.999f && maxY < 16.02f) {
				occludes = getSideOccludes(minX, minZ, maxX, maxZ) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minX, minZ, maxX, maxZ) << (direction.id * 4);
			}
			break;
		case NORTH:
			if (minZ > -0.02f && minZ < 0.001f) {
				occludes = getSideOccludes(minX, minY, maxX, maxY) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minX, minY, maxX,
						maxY) << (direction.id * 4);
			}
			break;
		case SOUTH:
			if (maxZ > 15.999f && maxZ < 16.02f) {
				occludes = getSideOccludes(minX, minY, maxX,
						maxY) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minX, minY, maxX,
						maxY) << (direction.id * 4);
			}
			break;
		case WEST:
			if (minX > -0.02f && minX < 0.001f) {
				occludes = getSideOccludes(minZ, minY, maxZ,
						maxY) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minZ, minY, maxZ,
						maxY) << (direction.id * 4);
			}
			break;
		case EAST:
			if (maxX > 15.999f && maxX < 16.02f) {
				occludes = getSideOccludes(minZ, minY, maxZ,
						maxY) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minZ, minY, maxZ,
						maxY) << (direction.id * 4);
			}
			break;
		}
	}

	private long getSideOccludes(float minX, float minY, float maxX, float maxY) {
		long res = 0;
		// Bottom left
		if (minX <= 0.01f && maxX >= 7.99f && minY <= 0.01f && maxY >= 7.99f) {
			res |= 1;
		}
		// Bottom right
		if (minX <= 8.01f && maxX >= 15.99f && minY <= 0.01f && maxY >= 7.99f) {
			res |= 1 << 1;
		}
		// Top left
		if (minX <= 0.01f && maxX >= 7.99f && minY <= 8.01f && maxY >= 15.99f) {
			res |= 1 << 2;
		}
		// Top right
		if (minX <= 8.01f && maxX >= 15.99f && minY <= 8.01f && maxY >= 15.99f) {
			res |= 1 << 3;
		}
		return res;
	}

	private long getSideOccludedBy(float minX, float minY, float maxX, float maxY) {
		long res = 0;
		// Bottom left
		if (minX < 7.99f && minY < 7.99f) {
			res |= 1;
		}
		// Bottom right
		if (maxX > 8.01f && minY < 7.99f) {
			res |= 1 << 1;
		}
		// Top left
		if (minX < 7.99f && maxY > 8.01f) {
			res |= 1 << 2;
		}
		// Top right
		if (maxX > 8.01f && maxY > 8.01f) {
			res |= 1 << 3;
		}
		return res;
	}
	
	public void transform(Matrix matrix) {
		matrix.transformFace(this);
		
		calculateDirection();
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	public void rotateUVs(float rotation) {
		while(rotation > 45f) {
			float ouvs0U = uvs0U;
			float ouvs0V = uvs0V;
			float ouvs1U = uvs1U;
			float ouvs1V = uvs1V;
			float ouvs2U = uvs2U;
			float ouvs2V = uvs2V;
			float ouvs3U = uvs3U;
			float ouvs3V = uvs3V;
			
			uvs0U = ouvs1U;
			uvs0V = ouvs1V;
			
			uvs1U = ouvs2U;
			uvs1V = ouvs2V;
			
			uvs2U = ouvs3U;
			uvs2V = ouvs3V;
			
			uvs3U = ouvs0U;
			uvs3V = ouvs0V;
			
			rotation -= 90f;
		}
	}
	
	public void rotateUVsAffine(float rotation, float pivotX, float pivotY) {
		float ouvs0U = uvs0U;
		float ouvs0V = uvs0V;
		float ouvs1U = uvs1U;
		float ouvs1V = uvs1V;
		float ouvs2U = uvs2U;
		float ouvs2V = uvs2V;
		float ouvs3U = uvs3U;
		float ouvs3V = uvs3V;
		
		float cosR = (float) Math.cos(Math.toRadians(rotation));
		float sinR = (float) Math.sin(Math.toRadians(rotation));
		
		uvs0U = (ouvs0U - pivotX) * cosR + (ouvs0V - pivotY) * -sinR + pivotX;
		uvs0V = (ouvs0U - pivotX) * sinR + (ouvs0V - pivotY) * cosR + pivotY;

		uvs1U = (ouvs1U - pivotX) * cosR + (ouvs1V - pivotY) * -sinR + pivotX;
		uvs1V = (ouvs1U - pivotX) * sinR + (ouvs1V - pivotY) * cosR + pivotY;

		uvs2U = (ouvs2U - pivotX) * cosR + (ouvs2V - pivotY) * -sinR + pivotX;
		uvs2V = (ouvs2U - pivotX) * sinR + (ouvs2V - pivotY) * cosR + pivotY;

		uvs3U = (ouvs3U - pivotX) * cosR + (ouvs3V - pivotY) * -sinR + pivotX;
		uvs3V = (ouvs3U - pivotX) * sinR + (ouvs3V - pivotY) * cosR + pivotY;
	}

	public void rotate(JsonObject rotateData) {
		if (rotateData == null)
			return;
		occludes = 0;
		occludedBy = 0;

		float originX = rotateData.get("origin").getAsJsonArray().get(0).getAsFloat();
		float originY = rotateData.get("origin").getAsJsonArray().get(1).getAsFloat();
		float originZ = rotateData.get("origin").getAsJsonArray().get(2).getAsFloat();
		boolean rescale = false;
		if (rotateData.has("rescale"))
			rescale = rotateData.get("rescale").getAsBoolean();
		
		if(rotateData.has("axis") && rotateData.has("angle")) {
			String axis = rotateData.get("axis").getAsString();
	
			float angle = -rotateData.get("angle").getAsFloat();
			if(axis.equals("z"))
				angle = -angle;
	
	
			rotateImpl(angle, rescale, axis, originX, originY, originZ);
		}else if(rotateData.has("x") || rotateData.has("y") || rotateData.has("z")) {
			float angleX = rotateData.get("x").getAsFloat();
			float angleY = rotateData.get("y").getAsFloat();
			float angleZ = -rotateData.get("z").getAsFloat();
			
			if(angleX != 0f)
				rotateImpl(angleX, rescale, "x", originX, originY, originZ);
			if(angleY != 0f)
				rotateImpl(angleY, rescale, "y", originX, originY, originZ);
			if(angleZ != 0f)
				rotateImpl(angleZ, rescale, "z", originX, originY, originZ);
		}
		
		calculateDirection();
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	private void rotateImpl(float angle, boolean rescale, String axis, float originX, float originY, float originZ) {
		float cosR = (float) Math.cos(Math.toRadians(angle));
		float sinR = (float) Math.sin(Math.toRadians(angle));
		float scaling = 1.0f;
		if (rescale)
			scaling = 1.0f / Math.max(cosR, sinR);

		float opoint0X = point0X;
		float opoint0Y = point0Y;
		float opoint0Z = point0Z;
		float opoint1X = point1X;
		float opoint1Y = point1Y;
		float opoint1Z = point1Z;
		float opoint2X = point2X;
		float opoint2Y = point2Y;
		float opoint2Z = point2Z;
		float opoint3X = point3X;
		float opoint3Y = point3Y;
		float opoint3Z = point3Z;

		if (axis.equals("x")) {
			point0Z = ((opoint0Z - originZ) * cosR - (opoint0Y - originY) * sinR) * scaling
					+ originZ;
			point0Y = ((opoint0Z - originZ) * sinR + (opoint0Y - originY) * cosR) * scaling
					+ originY;
			
			point1Z = ((opoint1Z - originZ) * cosR - (opoint1Y - originY) * sinR) * scaling
					+ originZ;
			point1Y = ((opoint1Z - originZ) * sinR + (opoint1Y - originY) * cosR) * scaling
					+ originY;
			
			point2Z = ((opoint2Z - originZ) * cosR - (opoint2Y - originY) * sinR) * scaling
					+ originZ;
			point2Y = ((opoint2Z - originZ) * sinR + (opoint2Y - originY) * cosR) * scaling
					+ originY;
			
			point3Z = ((opoint3Z - originZ) * cosR - (opoint3Y - originY) * sinR) * scaling
					+ originZ;
			point3Y = ((opoint3Z - originZ) * sinR + (opoint3Y - originY) * cosR) * scaling
					+ originY;
		} else if (axis.equals("y")) {
			point0X = ((opoint0X - originX) * cosR - (opoint0Z - originZ) * sinR) * scaling
					+ originX;
			point0Z = ((opoint0X - originX) * sinR + (opoint0Z - originZ) * cosR) * scaling
					+ originZ;
			
			point1X = ((opoint1X - originX) * cosR - (opoint1Z - originZ) * sinR) * scaling
					+ originX;
			point1Z = ((opoint1X - originX) * sinR + (opoint1Z - originZ) * cosR) * scaling
					+ originZ;
			
			point2X = ((opoint2X - originX) * cosR - (opoint2Z - originZ) * sinR) * scaling
					+ originX;
			point2Z = ((opoint2X - originX) * sinR + (opoint2Z - originZ) * cosR) * scaling
					+ originZ;
			
			point3X = ((opoint3X - originX) * cosR - (opoint3Z - originZ) * sinR) * scaling
					+ originX;
			point3Z = ((opoint3X - originX) * sinR + (opoint3Z - originZ) * cosR) * scaling
					+ originZ;
		} else if (axis.equals("z")) {
			point0X = ((opoint0X - originX) * cosR - (opoint0Y - originY) * sinR) * scaling
					+ originX;
			point0Y = ((opoint0X - originX) * sinR + (opoint0Y - originY) * cosR) * scaling
					+ originY;
			
			point1X = ((opoint1X - originX) * cosR - (opoint1Y - originY) * sinR) * scaling
					+ originX;
			point1Y = ((opoint1X - originX) * sinR + (opoint1Y - originY) * cosR) * scaling
					+ originY;
			
			point2X = ((opoint2X - originX) * cosR - (opoint2Y - originY) * sinR) * scaling
					+ originX;
			point2Y = ((opoint2X - originX) * sinR + (opoint2Y - originY) * cosR) * scaling
					+ originY;
			
			point3X = ((opoint3X - originX) * cosR - (opoint3Y - originY) * sinR) * scaling
					+ originX;
			point3Y = ((opoint3X - originX) * sinR + (opoint3Y - originY) * cosR) * scaling
					+ originY;
		}
	}
	
	public void rotate(float rotateX, float rotateY, boolean uvLock) {
		rotate(rotateX, rotateY, 0f, uvLock);
	}

	public void rotate(float rotateX, float rotateY, float rotateZ, boolean uvLock) {
		rotate(rotateX, rotateY, rotateZ, 8f, 8f, 8f, uvLock);
	}
	
	public void rotate(float rotateX, float rotateY, float rotateZ) {
		rotate(rotateX, rotateY, rotateZ, 8f, 8f, 8f);
	}
	
	public void rotate(float rotateX, float rotateY, float rotateZ,
						float pivotX, float pivotY, float pivotZ) {
		rotate(rotateX, rotateY, rotateZ, pivotX, pivotY, pivotZ, false);
	}
	
	public void rotate(float rotateX, float rotateY, float rotateZ, 
						float pivotX, float pivotY, float pivotZ, boolean uvLock) {
		// X Rotation
		float cosR = (float) Math.cos(Math.toRadians(rotateX));
		float sinR = (float) Math.sin(Math.toRadians(rotateX));
		point0X -= pivotX;
		point0Y -= pivotY;
		point0Z -= pivotZ;
		point1X -= pivotX;
		point1Y -= pivotY;
		point1Z -= pivotZ;
		point2X -= pivotX;
		point2Y -= pivotY;
		point2Z -= pivotZ;
		point3X -= pivotX;
		point3Y -= pivotY;
		point3Z -= pivotZ;
		
		float opoint0X = point0X;
		float opoint0Y = point0Y;
		float opoint0Z = point0Z;
		float opoint1X = point1X;
		float opoint1Y = point1Y;
		float opoint1Z = point1Z;
		float opoint2X = point2X;
		float opoint2Y = point2Y;
		float opoint2Z = point2Z;
		float opoint3X = point3X;
		float opoint3Y = point3Y;
		float opoint3Z = point3Z;
		
		if (rotateX != 0.0f) {
			point0Z = opoint0Z * cosR - opoint0Y * sinR;
			point0Y = opoint0Z * sinR + opoint0Y * cosR;
			
			point1Z = opoint1Z * cosR - opoint1Y * sinR;
			point1Y = opoint1Z * sinR + opoint1Y * cosR;
			
			point2Z = opoint2Z * cosR - opoint2Y * sinR;
			point2Y = opoint2Z * sinR + opoint2Y * cosR;
			
			point3Z = opoint3Z * cosR - opoint3Y * sinR;
			point3Y = opoint3Z * sinR + opoint3Y * cosR;

			// UV lock X rotation
			if (uvLock) {
				if (direction == Direction.WEST || direction == Direction.EAST) {
					float ouvs0U = uvs0U;
					float ouvs0V = uvs0V;
					float ouvs1U = uvs1U;
					float ouvs1V = uvs1V;
					float ouvs2U = uvs2U;
					float ouvs2V = uvs2V;
					float ouvs3U = uvs3U;
					float ouvs3V = uvs3V;
					float rotation = rotateX;
					if (direction == Direction.EAST)
						rotation = -rotateX;
					float uvPivotU = 8.0f;
					float uvPivotV = 8.0f;
					cosR = (float) Math.cos(Math.toRadians(rotation));
					sinR = (float) Math.sin(Math.toRadians(rotation));

					uvs0U = ((ouvs0U - uvPivotU) * cosR - (ouvs0V - uvPivotV) * sinR) + uvPivotU;
					uvs0V = ((ouvs0U - uvPivotU) * sinR + (ouvs0V - uvPivotV) * cosR) + uvPivotV;

					uvs1U = ((ouvs1U - uvPivotU) * cosR - (ouvs1V - uvPivotV) * sinR) + uvPivotU;
					uvs1V = ((ouvs1U - uvPivotU) * sinR + (ouvs1V - uvPivotV) * cosR) + uvPivotV;
					
					uvs2U = ((ouvs2U - uvPivotU) * cosR - (ouvs2V - uvPivotV) * sinR) + uvPivotU;
					uvs2V = ((ouvs2U - uvPivotU) * sinR + (ouvs2V - uvPivotV) * cosR) + uvPivotV;
					
					uvs3U = ((ouvs3U - uvPivotU) * cosR - (ouvs3V - uvPivotV) * sinR) + uvPivotU;
					uvs3V = ((ouvs3U - uvPivotU) * sinR + (ouvs3V - uvPivotV) * cosR) + uvPivotV;
				}
			}
		}

		if (rotateY != 0.0f) {
			// Rotate Y
			cosR = (float) Math.cos(Math.toRadians(rotateY));
			sinR = (float) Math.sin(Math.toRadians(rotateY));
			opoint0X = point0X;
			opoint0Y = point0Y;
			opoint0Z = point0Z;
			opoint1X = point1X;
			opoint1Y = point1Y;
			opoint1Z = point1Z;
			opoint2X = point2X;
			opoint2Y = point2Y;
			opoint2Z = point2Z;
			opoint3X = point3X;
			opoint3Y = point3Y;
			opoint3Z = point3Z;
			
			point0X = opoint0X * cosR - opoint0Z * sinR;
			point0Z = opoint0X * sinR + opoint0Z * cosR;

			point1X = opoint1X * cosR - opoint1Z * sinR;
			point1Z = opoint1X * sinR + opoint1Z * cosR;

			point2X = opoint2X * cosR - opoint2Z * sinR;
			point2Z = opoint2X * sinR + opoint2Z * cosR;

			point3X = opoint3X * cosR - opoint3Z * sinR;
			point3Z = opoint3X * sinR + opoint3Z * cosR;
			
			// UV Lock Rotate Y
			if (uvLock) {
				if (direction == Direction.DOWN || direction == Direction.UP) {
					float ouvs0U = uvs0U;
					float ouvs0V = uvs0V;
					float ouvs1U = uvs1U;
					float ouvs1V = uvs1V;
					float ouvs2U = uvs2U;
					float ouvs2V = uvs2V;
					float ouvs3U = uvs3U;
					float ouvs3V = uvs3V;
					float rotation = rotateY;
					if (direction == Direction.UP)
						rotation = -rotateY;
					float uvPivotU = 8.0f;
					float uvPivotV = 8.0f;
					cosR = (float) Math.cos(Math.toRadians(rotation));
					sinR = (float) Math.sin(Math.toRadians(rotation));
					uvs0U = ((ouvs0U - uvPivotU) * cosR - (ouvs0V - uvPivotV) * sinR) + uvPivotU;
					uvs0V = ((ouvs0U - uvPivotU) * sinR + (ouvs0V - uvPivotV) * cosR) + uvPivotV;

					uvs1U = ((ouvs1U - uvPivotU) * cosR - (ouvs1V - uvPivotV) * sinR) + uvPivotU;
					uvs1V = ((ouvs1U - uvPivotU) * sinR + (ouvs1V - uvPivotV) * cosR) + uvPivotV;
					
					uvs2U = ((ouvs2U - uvPivotU) * cosR - (ouvs2V - uvPivotV) * sinR) + uvPivotU;
					uvs2V = ((ouvs2U - uvPivotU) * sinR + (ouvs2V - uvPivotV) * cosR) + uvPivotV;
					
					uvs3U = ((ouvs3U - uvPivotU) * cosR - (ouvs3V - uvPivotV) * sinR) + uvPivotU;
					uvs3V = ((ouvs3U - uvPivotU) * sinR + (ouvs3V - uvPivotV) * cosR) + uvPivotV;
				}
			}
		}
		
		if (rotateZ != 0.0f) {
			// Rotate Z
			cosR = (float) Math.cos(Math.toRadians(rotateZ));
			sinR = (float) Math.sin(Math.toRadians(rotateZ));
			opoint0X = point0X;
			opoint0Y = point0Y;
			opoint0Z = point0Z;
			opoint1X = point1X;
			opoint1Y = point1Y;
			opoint1Z = point1Z;
			opoint2X = point2X;
			opoint2Y = point2Y;
			opoint2Z = point2Z;
			opoint3X = point3X;
			opoint3Y = point3Y;
			opoint3Z = point3Z;
			
			point0X = opoint0X * cosR - opoint0Y * sinR;
			point0Y = opoint0X * sinR + opoint0Y * cosR;

			point1X = opoint1X * cosR - opoint1Y * sinR;
			point1Y = opoint1X * sinR + opoint1Y * cosR;

			point2X = opoint2X * cosR - opoint2Y * sinR;
			point2Y = opoint2X * sinR + opoint2Y * cosR;

			point3X = opoint3X * cosR - opoint3Y * sinR;
			point3Y = opoint3X * sinR + opoint3Y * cosR;
			
			// UV lock Z rotation
			if (uvLock) {
				if (direction == Direction.NORTH || direction == Direction.SOUTH) {
					float ouvs0U = uvs0U;
					float ouvs0V = uvs0V;
					float ouvs1U = uvs1U;
					float ouvs1V = uvs1V;
					float ouvs2U = uvs2U;
					float ouvs2V = uvs2V;
					float ouvs3U = uvs3U;
					float ouvs3V = uvs3V;
					float rotation = rotateX;
					if (direction == Direction.SOUTH)
						rotation = -rotateX;
					float uvPivotU = 8.0f;
					float uvPivotV = 8.0f;
					cosR = (float) Math.cos(Math.toRadians(rotation));
					sinR = (float) Math.sin(Math.toRadians(rotation));
					uvs0U = ((ouvs0U - uvPivotU) * cosR - (ouvs0V - uvPivotV) * sinR) + uvPivotU;
					uvs0V = ((ouvs0U - uvPivotU) * sinR + (ouvs0V - uvPivotV) * cosR) + uvPivotV;

					uvs1U = ((ouvs1U - uvPivotU) * cosR - (ouvs1V - uvPivotV) * sinR) + uvPivotU;
					uvs1V = ((ouvs1U - uvPivotU) * sinR + (ouvs1V - uvPivotV) * cosR) + uvPivotV;
					
					uvs2U = ((ouvs2U - uvPivotU) * cosR - (ouvs2V - uvPivotV) * sinR) + uvPivotU;
					uvs2V = ((ouvs2U - uvPivotU) * sinR + (ouvs2V - uvPivotV) * cosR) + uvPivotV;
					
					uvs3U = ((ouvs3U - uvPivotU) * cosR - (ouvs3V - uvPivotV) * sinR) + uvPivotU;
					uvs3V = ((ouvs3U - uvPivotU) * sinR + (ouvs3V - uvPivotV) * cosR) + uvPivotV;
				}
			}
		}
		point0X += pivotX;
		point0Y += pivotY;
		point0Z += pivotZ;
		point1X += pivotX;
		point1Y += pivotY;
		point1Z += pivotZ;
		point2X += pivotX;
		point2Y += pivotY;
		point2Z += pivotZ;
		point3X += pivotX;
		point3Y += pivotY;
		point3Z += pivotZ;
		
		calculateDirection();
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	
	public void flip(boolean x, boolean y, boolean z) {
		float scaleX = x ? -1f : 1f;
		float scaleY = y ? -1f : 1f;
		float scaleZ = z ? -1f : 1f;
		
		scale(scaleX, scaleY, scaleZ, 8f, 8f, 8f);
	}
	
	public void mirror(boolean x, boolean y, boolean z, float pivotX, float pivotY, float pivotZ) {
		float scaleX = x ? -1f : 1f;
		float scaleY = y ? -1f : 1f;
		float scaleZ = z ? -1f : 1f;
		
		scale(scaleX, scaleY, scaleZ, pivotX, pivotY, pivotZ);
	}
	
	public void reverseDirection() {
		float opoint1X = point1X;
		float opoint1Y = point1Y;
		float opoint1Z = point1Z;
		float opoint3X = point3X;
		float opoint3Y = point3Y;
		float opoint3Z = point3Z;
		float ouvs1U = uvs1U;
		float ouvs1V = uvs1V;
		float ouvs3U = uvs3U;
		float ouvs3V = uvs3V;
		
		point1X = opoint3X;
		point1Y = opoint3Y;
		point1Z = opoint3Z;
		point3X = opoint1X;
		point3Y = opoint1Y;
		point3Z = opoint1Z;
		
		uvs1U = ouvs3U;
		uvs1V = ouvs3V;
		uvs3U = ouvs1U;
		uvs3V = ouvs1V;
		
		calculateDirection();
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	public void setTexture(String texture) {
		setTexture(TextureRegistry.getIdFromTexture(texture, 0));
	}
	
	public void setTexture(Long texture) {
		this.texture = texture;
	}
	
	public long getOccludes() {
		return occludes;
	}

	public long getOccludedBy() {
		return occludedBy;
	}

	public boolean isOccluded(long occlusion) {
		return occludedBy == 0 ? false : ((occlusion & occludedBy) == occludedBy);
	}

	public long getTexture() {
		return texture;
	}
	
	public String getTextureString() {
		return TextureRegistry.getTextureFromId(texture);
	}

	public boolean isValid() {
		return texture != 0;
	}
	
	public void getPoints(float[] out) {
		out[0] = point0X;
		out[1] = point0Y;
		out[2] = point0Z;
		out[3] = point1X;
		out[4] = point1Y;
		out[5] = point1Z;
		out[6] = point2X;
		out[7] = point2Y;
		out[8] = point2Z;
		out[9] = point3X;
		out[10] = point3Y;
		out[11] = point3Z;
	}
	
	public void getUVs(float[] out) {
		out[0] = uvs0U;
		out[1] = uvs0V;
		out[2] = uvs1U;
		out[3] = uvs1V;
		out[4] = uvs2U;
		out[5] = uvs2V;
		out[6] = uvs3U;
		out[7] = uvs3V;
	}

	public void noOcclusion() {
		this.occludedBy = 0;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public int getTintIndex() {
		return tintIndex;
	}
	
	public String getShadingMode() {
		return shadingMode;
	}
	
	public void setShadingMode(String shadingMode) {
		this.shadingMode = shadingMode;
	}
	
	public void translate(float x, float y, float z) {
		point0X += x;
		point0Y += y;
		point0Z += z;
		point1X += x;
		point1Y += y;
		point1Z += z;
		point2X += x;
		point2Y += y;
		point2Z += z;
		point3X += x;
		point3Y += y;
		point3Z += z;
	}

	public void scale(float scale) {
		scale(scale, scale, scale, 8f, 8f, 8f);
	}
	
	public void scale(float scaleX, float scaleY, float scaleZ) {
		scale(scaleX, scaleY, scaleZ, 8f, 8f, 8f);
	}
	
	public void scale(float scaleX, float scaleY, float scaleZ, float pivotX, float pivotY, float pivotZ) {
		point0X = (point0X - pivotX) * scaleX + pivotX;
		point0Y = (point0Y - pivotY) * scaleY + pivotY;
		point0Z = (point0Z - pivotZ) * scaleZ + pivotZ;
		
		point1X = (point1X - pivotX) * scaleX + pivotX;
		point1Y = (point1Y - pivotY) * scaleY + pivotY;
		point1Z = (point1Z - pivotZ) * scaleZ + pivotZ;
		
		point2X = (point2X - pivotX) * scaleX + pivotX;
		point2Y = (point2Y - pivotY) * scaleY + pivotY;
		point2Z = (point2Z - pivotZ) * scaleZ + pivotZ;
		
		point3X = (point3X - pivotX) * scaleX + pivotX;
		point3Y = (point3Y - pivotY) * scaleY + pivotY;
		point3Z = (point3Z - pivotZ) * scaleZ + pivotZ;
	}
	
	public void setFaceColour(float r, float g, float b) {
		this.hasVertexColor = true;
		this.vertexColorR = r;
		this.vertexColorG = g;
		this.vertexColorB = b;
	}
	
	public void setFaceColour(float r, float g, float b, boolean combine) {
		if(combine) {
			if(!this.hasVertexColor) {
				this.hasVertexColor = true;
				this.vertexColorR = 1f;
				this.vertexColorG = 1f;
				this.vertexColorB = 1f;
			}
			this.vertexColorR *= r;
			this.vertexColorG *= g;
			this.vertexColorB *= b;
		}else {
			this.hasVertexColor = true;
			this.vertexColorR = r;
			this.vertexColorG = g;
			this.vertexColorB = b;
		}
	}
	
	public void setFaceColour(Color colour) {
		setFaceColour(colour.getR(), colour.getG(), colour.getB());
	}
	
	public void setFaceColour(Color colour, boolean combine) {
		setFaceColour(colour.getR(), colour.getG(), colour.getB(), combine);
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}
	
	public void calculateNormal(float[] out) {
		float x1 = point1X - point0X;
		float y1 = point1Y - point0Y;
		float z1 = point1Z - point0Z;
		
		float x2 = point3X - point0X;
		float y2 = point3Y - point0Y;
		float z2 = point3Z - point0Z;
		
		out[0] = y1 * z2 - z1 * y2;
		out[1] = z1 * x2 - x1 * z2;
		out[2] = x1 * y2 - y1 * x2;
		float length = (float) (1.0 / Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2]));
		out[0] *= length;
		out[1] *= length;
		out[2] *= length;
		
		// Round the values to get rid of error and get nice numbers.
		out[0] = ((float) Math.round(out[0] * 1000000.0f)) / 1000000.0f;
		out[1] = ((float) Math.round(out[1] * 1000000.0f)) / 1000000.0f;
		out[2] = ((float) Math.round(out[2] * 1000000.0f)) / 1000000.0f;
	}

	public void setTintIndex(int tintIndex) {
		this.tintIndex = tintIndex;
	}
	
	public void calculateOcclusion() {
		calculateDirection();
		calculateOcclusion(Math.min(point0X, point2X),
				Math.min(point0Y, point2Y),
				Math.min(point0Z, point2Z),
				Math.max(point0X, point2X),
				Math.max(point0Y, point2Y),
				Math.max(point0Z, point2Z));
	}
	
	public void allowOcclusionIfFullyOccluded() {
		// If this face isn't being occluded by anything,
		// then make it so that it can be occluded if it is
		// occluded from all sides.
		// Occlusion is specified as four bits per side of a block.
		// Each bit is a corner of that side. Four bits times six sides,
		// means twenty four bits that need to be set, a.k.a. 0xFFFFFF
		if(this.occludedBy == 0)
			this.occludedBy = 0xFFFFFF;
	}

	public float getCenterX() {
		return (point0X + point1X + point2X + point3X) / 4f;
	}
	
	public float getCenterY() {
		return (point0Y + point1Y + point2Y + point3Y) / 4f;
	}
	
	public float getCenterZ() {
		return (point0Z + point1Z + point2Z + point3Z) / 4f;
	}
	
	/**
	 * Checks if the two faces are on top of each other.
	 * @param other
	 * @return
	 */
	public boolean isOnTop(ModelFace other) {
		if(other.direction != direction)
			return false;
		if(Math.floor(point0X * 1000.0 + 0.5) != Math.floor(other.point0X * 1000.0 + 0.5))
			return false;
		if(Math.floor(point0Y * 1000.0 + 0.5) != Math.floor(other.point0Y * 1000.0 + 0.5))
			return false;
		if(Math.floor(point0Z * 1000.0 + 0.5) != Math.floor(other.point0Z * 1000.0 + 0.5))
			return false;
		if(Math.floor(point1X * 1000.0 + 0.5) != Math.floor(other.point1X * 1000.0 + 0.5))
			return false;
		if(Math.floor(point1Y * 1000.0 + 0.5) != Math.floor(other.point1Y * 1000.0 + 0.5))
			return false;
		if(Math.floor(point1Z * 1000.0 + 0.5) != Math.floor(other.point1Z * 1000.0 + 0.5))
			return false;
		if(Math.floor(point2X * 1000.0 + 0.5) != Math.floor(other.point2X * 1000.0 + 0.5))
			return false;
		if(Math.floor(point2Y * 1000.0 + 0.5) != Math.floor(other.point2Y * 1000.0 + 0.5))
			return false;
		if(Math.floor(point2Z * 1000.0 + 0.5) != Math.floor(other.point2Z * 1000.0 + 0.5))
			return false;
		if(Math.floor(point3X * 1000.0 + 0.5) != Math.floor(other.point3X * 1000.0 + 0.5))
			return false;
		if(Math.floor(point3Y * 1000.0 + 0.5) != Math.floor(other.point3Y * 1000.0 + 0.5))
			return false;
		if(Math.floor(point3Z * 1000.0 + 0.5) != Math.floor(other.point3Z * 1000.0 + 0.5))
			return false;
		return true;
	}

	public JsonObject toJson() {
		JsonObject res = new JsonObject();
		
		JsonArray pointsArray = new JsonArray();
		//for(float point : points) {
		//	pointsArray.add(point);
		//}
		pointsArray.add(point0X);
		pointsArray.add(point0Y);
		pointsArray.add(point0Z);
		pointsArray.add(point1X);
		pointsArray.add(point1Y);
		pointsArray.add(point1Z);
		pointsArray.add(point2X);
		pointsArray.add(point2Y);
		pointsArray.add(point2Z);
		pointsArray.add(point3X);
		pointsArray.add(point3Y);
		pointsArray.add(point3Z);
		res.add("points", pointsArray);
		JsonArray uvsArray = new JsonArray();
		//for(float uv : uvs) {
		//	uvsArray.add(uv);
		//}
		uvsArray.add(uvs0U);
		uvsArray.add(uvs0V);
		uvsArray.add(uvs1U);
		uvsArray.add(uvs1V);
		uvsArray.add(uvs2U);
		uvsArray.add(uvs2V);
		uvsArray.add(uvs3U);
		uvsArray.add(uvs3V);
		res.add("uvs", uvsArray);
		if(hasVertexColor) {
			JsonArray vertexColorsArray = new JsonArray();
			vertexColorsArray.add(vertexColorR);
			vertexColorsArray.add(vertexColorG);
			vertexColorsArray.add(vertexColorB);
			res.add("vertexColor", vertexColorsArray);
		}
		res.addProperty("occludes", occludes);
		res.addProperty("occludedBy", occludedBy);
		res.addProperty("texture", texture);
		res.addProperty("direction", direction.toString());
		res.addProperty("doubleSided", doubleSided);
		res.addProperty("tintIndex", tintIndex);
		res.addProperty("shadingMode", shadingMode);
		
		return res;
	}
	
	public float getPoint0X() {
		return point0X;
	}

	public float getPoint0Y() {
		return point0Y;
	}

	public float getPoint0Z() {
		return point0Z;
	}

	public float getPoint1X() {
		return point1X;
	}

	public float getPoint1Y() {
		return point1Y;
	}

	public float getPoint1Z() {
		return point1Z;
	}

	public float getPoint2X() {
		return point2X;
	}

	public float getPoint2Y() {
		return point2Y;
	}

	public float getPoint2Z() {
		return point2Z;
	}

	public float getPoint3X() {
		return point3X;
	}

	public float getPoint3Y() {
		return point3Y;
	}

	public float getPoint3Z() {
		return point3Z;
	}

	public float getUvs0U() {
		return uvs0U;
	}

	public float getUvs0V() {
		return uvs0V;
	}

	public float getUvs1U() {
		return uvs1U;
	}

	public float getUvs1V() {
		return uvs1V;
	}

	public float getUvs2U() {
		return uvs2U;
	}

	public float getUvs2V() {
		return uvs2V;
	}

	public float getUvs3U() {
		return uvs3U;
	}

	public float getUvs3V() {
		return uvs3V;
	}
	
	public void setPoints(float[] points) {
		point0X = points[0];
		point0Y = points[1];
		point0Z = points[2];
		point1X = points[3];
		point1Y = points[4];
		point1Z = points[5];
		point2X = points[6];
		point2Y = points[7];
		point2Z = points[8];
		point3X = points[9];
		point3Y = points[10];
		point3Z = points[11];
	}
	
	public void setUVs(float[] uvs) {
		uvs0U = uvs[0];
		uvs0V = uvs[1];
		uvs1U = uvs[2];
		uvs1V = uvs[3];
		uvs2U = uvs[4];
		uvs2V = uvs[5];
		uvs3U = uvs[6];
		uvs3V = uvs[7];
	}
	
	/**
	 * If this face sits perfectly on the unit cube,
	 * then move it inwards slightly.
	 */
	public void moveTransparentFace(boolean inwards, boolean useDirection) {
		float amt = inwards ? 0.01f : -0.01f;
		switch(direction) {
		case DOWN:
		case UP:
			if(Math.abs(point0Y-0f) < 0.001f && Math.abs(point1Y-0f) < 0.001f && 
					Math.abs(point2Y-0f) < 0.001f && Math.abs(point3Y-0f) < 0.001f) {
				if(useDirection && direction == Direction.UP)
					amt = -amt;
				point0Y += amt;
				point1Y += amt;
				point2Y += amt;
				point3Y += amt;
				
			}
			if(Math.abs(point0Y-16f) < 0.001f && Math.abs(point1Y-16f) < 0.001f && 
					Math.abs(point2Y-16f) < 0.001f && Math.abs(point3Y-16f) < 0.001f) {
				if(useDirection && direction == Direction.DOWN)
					amt = -amt;
				point0Y -= amt;
				point1Y -= amt;
				point2Y -= amt;
				point3Y -= amt;
			}
			break;
		case NORTH:
		case SOUTH:
			if(Math.abs(point0Z-0f) < 0.001f && Math.abs(point1Z-0f) < 0.001f && 
					Math.abs(point2Z-0f) < 0.001f && Math.abs(point3Z-0f) < 0.001f) {
				if(useDirection && direction == Direction.SOUTH)
					amt = -amt;
				point0Z += amt;
				point1Z += amt;
				point2Z += amt;
				point3Z += amt;
			}
			if(Math.abs(point0Z-16f) < 0.001f && Math.abs(point1Z-16f) < 0.001f && 
					Math.abs(point2Z-16f) < 0.001f && Math.abs(point3Z-16f) < 0.001f) {
				if(useDirection && direction == Direction.NORTH)
					amt = -amt;
				point0Z -= amt;
				point1Z -= amt;
				point2Z -= amt;
				point3Z -= amt;
			}
			break;
		case EAST:
		case WEST:
			if(Math.abs(point0X-0f) < 0.001f && Math.abs(point1X-0f) < 0.001f && 
					Math.abs(point2X-0f) < 0.001f && Math.abs(point3X-0f) < 0.001f) {
				if(useDirection && direction == Direction.EAST)
					amt = -amt;
				point0X += amt;
				point1X += amt;
				point2X += amt;
				point3X += amt;
			}
			if(Math.abs(point0X-16f) < 0.001f && Math.abs(point1X-16f) < 0.001f && 
					Math.abs(point2X-16f) < 0.001f && Math.abs(point3X-16f) < 0.001f) {
				if(useDirection && direction == Direction.WEST)
					amt = -amt;
				point0X -= amt;
				point1X -= amt;
				point2X -= amt;
				point3X -= amt;
			}
			break;
		}
	}
	
	/**
	 * If this face sits perfectly on the unit cub
	 */
	public boolean shouldMoveTransparentFace() {
		switch(direction) {
		case DOWN:
		case UP:
			if(Math.abs(point0Y-0f) < 0.001f && Math.abs(point1Y-0f) < 0.001f && 
					Math.abs(point2Y-0f) < 0.001f && Math.abs(point3Y-0f) < 0.001f) {
				return true;
				
			}
			if(Math.abs(point0Y-16f) < 0.001f && Math.abs(point1Y-16f) < 0.001f && 
					Math.abs(point2Y-16f) < 0.001f && Math.abs(point3Y-16f) < 0.001f) {
				return true;
			}
			break;
		case NORTH:
		case SOUTH:
			if(Math.abs(point0Z-0f) < 0.001f && Math.abs(point1Z-0f) < 0.001f && 
					Math.abs(point2Z-0f) < 0.001f && Math.abs(point3Z-0f) < 0.001f) {
				return true;
			}
			if(Math.abs(point0Z-16f) < 0.001f && Math.abs(point1Z-16f) < 0.001f && 
					Math.abs(point2Z-16f) < 0.001f && Math.abs(point3Z-16f) < 0.001f) {
				return true;
			}
			break;
		case EAST:
		case WEST:
			if(Math.abs(point0X-0f) < 0.001f && Math.abs(point1X-0f) < 0.001f && 
					Math.abs(point2X-0f) < 0.001f && Math.abs(point3X-0f) < 0.001f) {
				return true;
			}
			if(Math.abs(point0X-16f) < 0.001f && Math.abs(point1X-16f) < 0.001f && 
					Math.abs(point2X-16f) < 0.001f && Math.abs(point3X-16f) < 0.001f) {
				return true;
			}
			break;
		}
		return false;
	}

}
