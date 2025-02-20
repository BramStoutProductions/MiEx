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

import java.util.Arrays;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class ModelFace {

	private float points[];
	private float uvs[];
	private float vertexColors[];
	private long occludes;
	private long occludedBy;
	private String texture;
	private Direction direction;
	private boolean doubleSided;
	private int tintIndex;

	public ModelFace(ModelFace other) {
		points = Arrays.copyOf(other.points, other.points.length);
		uvs = Arrays.copyOf(other.uvs, other.uvs.length);
		vertexColors = null;
		if(other.vertexColors != null)
			vertexColors = Arrays.copyOf(other.vertexColors, other.vertexColors.length);
		occludes = other.occludes;
		occludedBy = other.occludedBy;
		texture = other.texture;
		direction = other.direction;
		doubleSided = other.doubleSided;
		tintIndex = other.tintIndex;
	}

	public ModelFace(float[] points, float[] uvs, String texture, int tintIndex, Direction direction, boolean doubleSided) {
		if(points.length != (4 * 3))
			throw new RuntimeException("Incorrect number of points");
		if(uvs.length != (4 * 2))
			throw new RuntimeException("Incorrect number of uvs");
		this.points = points;
		this.uvs = uvs;
		vertexColors = null;
		occludes = 0;
		occludedBy = 0;
		this.direction = direction;
		this.doubleSided = doubleSided;
		this.texture = texture;
		this.tintIndex = tintIndex;
		
		float[] minMaxPoints2 = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateOcclusion(minMaxPoints2);
	}
	
	public ModelFace(float[] minMaxPoints, Direction direction, JsonObject faceData, boolean doubleSided) {
		points = new float[4 * 3];
		uvs = new float[4 * 2];
		vertexColors = null;
		occludes = 0;
		occludedBy = 0;
		this.direction = direction;
		this.doubleSided = doubleSided;

		texture = "";
		if (faceData != null && faceData.has("texture"))
			texture = faceData.get("texture").getAsString();
		
		tintIndex = -1;
		if(faceData != null && faceData.has("tintindex"))
			tintIndex = faceData.get("tintindex").getAsInt();

		float minU = 0.0f;
		float minV = 0.0f;
		float maxU = 0.0f;
		float maxV = 0.0f;

		switch (direction) {
		case DOWN:
			points[0] = minMaxPoints[0];
			points[1] = minMaxPoints[1];
			points[2] = minMaxPoints[2];

			points[3] = minMaxPoints[3];
			points[4] = minMaxPoints[1];
			points[5] = minMaxPoints[2];

			points[6] = minMaxPoints[3];
			points[7] = minMaxPoints[1];
			points[8] = minMaxPoints[5];

			points[9] = minMaxPoints[0];
			points[10] = minMaxPoints[1];
			points[11] = minMaxPoints[5];

			minU = minMaxPoints[0];
			minV = minMaxPoints[2];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[5];
			break;
		case UP:
			points[0] = minMaxPoints[0];
			points[1] = minMaxPoints[4];
			points[2] = minMaxPoints[5];

			points[3] = minMaxPoints[3];
			points[4] = minMaxPoints[4];
			points[5] = minMaxPoints[5];

			points[6] = minMaxPoints[3];
			points[7] = minMaxPoints[4];
			points[8] = minMaxPoints[2];

			points[9] = minMaxPoints[0];
			points[10] = minMaxPoints[4];
			points[11] = minMaxPoints[2];

			minU = minMaxPoints[0];
			minV = minMaxPoints[2];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[5];
			break;
		case NORTH:
			points[0] = minMaxPoints[3];
			points[1] = minMaxPoints[1];
			points[2] = minMaxPoints[2];

			points[3] = minMaxPoints[0];
			points[4] = minMaxPoints[1];
			points[5] = minMaxPoints[2];

			points[6] = minMaxPoints[0];
			points[7] = minMaxPoints[4];
			points[8] = minMaxPoints[2];

			points[9] = minMaxPoints[3];
			points[10] = minMaxPoints[4];
			points[11] = minMaxPoints[2];

			minU = minMaxPoints[0];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[4];
			break;
		case SOUTH:
			points[0] = minMaxPoints[0];
			points[1] = minMaxPoints[1];
			points[2] = minMaxPoints[5];

			points[3] = minMaxPoints[3];
			points[4] = minMaxPoints[1];
			points[5] = minMaxPoints[5];

			points[6] = minMaxPoints[3];
			points[7] = minMaxPoints[4];
			points[8] = minMaxPoints[5];

			points[9] = minMaxPoints[0];
			points[10] = minMaxPoints[4];
			points[11] = minMaxPoints[5];

			minU = minMaxPoints[0];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[3];
			maxV = minMaxPoints[4];
			break;
		case WEST:
			points[0] = minMaxPoints[0];
			points[1] = minMaxPoints[1];
			points[2] = minMaxPoints[2];

			points[3] = minMaxPoints[0];
			points[4] = minMaxPoints[1];
			points[5] = minMaxPoints[5];

			points[6] = minMaxPoints[0];
			points[7] = minMaxPoints[4];
			points[8] = minMaxPoints[5];

			points[9] = minMaxPoints[0];
			points[10] = minMaxPoints[4];
			points[11] = minMaxPoints[2];

			minU = minMaxPoints[2];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[5];
			maxV = minMaxPoints[4];
			break;
		case EAST:
			points[0] = minMaxPoints[3];
			points[1] = minMaxPoints[1];
			points[2] = minMaxPoints[5];

			points[3] = minMaxPoints[3];
			points[4] = minMaxPoints[1];
			points[5] = minMaxPoints[2];

			points[6] = minMaxPoints[3];
			points[7] = minMaxPoints[4];
			points[8] = minMaxPoints[2];

			points[9] = minMaxPoints[3];
			points[10] = minMaxPoints[4];
			points[11] = minMaxPoints[5];

			minU = minMaxPoints[2];
			minV = minMaxPoints[1];
			maxU = minMaxPoints[5];
			maxV = minMaxPoints[4];
			break;
		}

		if (faceData != null && faceData.has("uv")) {
			minU = faceData.get("uv").getAsJsonArray().get(0).getAsFloat();
			minV = 16.0f - faceData.get("uv").getAsJsonArray().get(3).getAsFloat();
			maxU = faceData.get("uv").getAsJsonArray().get(2).getAsFloat();
			maxV = 16.0f - faceData.get("uv").getAsJsonArray().get(1).getAsFloat();
		}

		uvs[0] = minU;
		uvs[1] = minV;

		uvs[2] = maxU;
		uvs[3] = minV;

		uvs[4] = maxU;
		uvs[5] = maxV;
		
		uvs[6] = minU;
		uvs[7] = maxV;

		if (faceData != null && faceData.has("rotation")) {
			if(faceData.has("rotationMiEx") && faceData.get("rotationMiEx").getAsBoolean()) {
				float[] oldUVs = Arrays.copyOf(uvs, uvs.length);
				float rotation = faceData.get("rotation").getAsFloat();
				float cosR = (float) Math.cos(Math.toRadians(rotation));
				float sinR = (float) Math.sin(Math.toRadians(rotation));
				float pivotX = (oldUVs[0] + oldUVs[2] + oldUVs[4] + oldUVs[6]) / 4.0f;
				float pivotY = (oldUVs[1] + oldUVs[3] + oldUVs[5] + oldUVs[7]) / 4.0f;
				for(int i = 0; i < 8; i += 2) {
					uvs[i] = (oldUVs[i] - pivotX) * cosR + (oldUVs[i+1] - pivotY) * -sinR + pivotX;
					uvs[i+1] = (oldUVs[i] - pivotX) * sinR + (oldUVs[i+1] - pivotY) * cosR + pivotY;
				}
			}else {
				float[] oldUVs = Arrays.copyOf(uvs, uvs.length);
				float rotation = faceData.get("rotation").getAsFloat();
				while(rotation > 45f) {
					for(int i = 0; i < 8; ++i)
						uvs[i] = oldUVs[(i + 2) >= 8 ? (i + 2 - 8) : (i + 2)];
					oldUVs = Arrays.copyOf(uvs, uvs.length);
					rotation -= 90f;
				}
			}
		}

		float[] minMaxPoints2 = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateOcclusion(minMaxPoints2);
	}
	
	private void calculateDirection() {
		float x1 = points[1*3+0] - points[0*3+0];
		float y1 = points[1*3+1] - points[0*3+1];
		float z1 = points[1*3+2] - points[0*3+2];
		float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
		x1 /= length;
		y1 /= length;
		z1 /= length;
		
		float x2 = points[3*3+0] - points[0*3+0];
		float y2 = points[3*3+1] - points[0*3+1];
		float z2 = points[3*3+2] - points[0*3+2];
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
	
	private void calculateOcclusion(float[] minMaxPoints) {
		occludes = 0;
		occludedBy = 0;
		switch (direction) {
		case DOWN:
			if (Math.abs(minMaxPoints[1]) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[0], minMaxPoints[2], minMaxPoints[3],
						minMaxPoints[5]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[0], minMaxPoints[2], minMaxPoints[3],
						minMaxPoints[5]) << (direction.id * 4);
			}
			break;
		case UP:
			if (Math.abs(minMaxPoints[4] - 16.0f) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[0], minMaxPoints[2], minMaxPoints[3],
						minMaxPoints[5]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[0], minMaxPoints[2], minMaxPoints[3],
						minMaxPoints[5]) << (direction.id * 4);
			}
			break;
		case NORTH:
			if (Math.abs(minMaxPoints[2]) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[0], minMaxPoints[1], minMaxPoints[3],
						minMaxPoints[4]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[0], minMaxPoints[1], minMaxPoints[3],
						minMaxPoints[4]) << (direction.id * 4);
			}
			break;
		case SOUTH:
			if (Math.abs(minMaxPoints[5] - 16.0f) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[0], minMaxPoints[1], minMaxPoints[3],
						minMaxPoints[4]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[0], minMaxPoints[1], minMaxPoints[3],
						minMaxPoints[4]) << (direction.id * 4);
			}
			break;
		case WEST:
			if (Math.abs(minMaxPoints[0]) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[2], minMaxPoints[1], minMaxPoints[5],
						minMaxPoints[4]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[2], minMaxPoints[1], minMaxPoints[5],
						minMaxPoints[4]) << (direction.id * 4);
			}
			break;
		case EAST:
			if (Math.abs(minMaxPoints[3] - 16.0f) < 0.01f) {
				occludes = getSideOccludes(minMaxPoints[2], minMaxPoints[1], minMaxPoints[5],
						minMaxPoints[4]) << (direction.id * 4);
				occludedBy = getSideOccludedBy(minMaxPoints[2], minMaxPoints[1], minMaxPoints[5],
						minMaxPoints[4]) << (direction.id * 4);
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
		Vector3f v0 = new Vector3f(points[0], points[1], points[2]);
		Vector3f v1 = new Vector3f(points[3], points[4], points[5]);
		Vector3f v2 = new Vector3f(points[6], points[7], points[8]);
		Vector3f v3 = new Vector3f(points[9], points[10], points[11]);
		
		v0 = matrix.transformPoint(v0);
		v1 = matrix.transformPoint(v1);
		v2 = matrix.transformPoint(v2);
		v3 = matrix.transformPoint(v3);
		
		points[0] = v0.x; points[1] = v0.y; points[2] = v0.z;
		points[3] = v1.x; points[4] = v1.y; points[5] = v1.z;
		points[6] = v2.x; points[7] = v2.y; points[8] = v2.z;
		points[9] = v3.x; points[10] = v3.y; points[11] = v3.z;
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}

	public void rotate(JsonObject rotateData) {
		if (rotateData == null)
			return;
		occludes = 0;
		occludedBy = 0;

		float originX = rotateData.get("origin").getAsJsonArray().get(0).getAsFloat();
		float originY = rotateData.get("origin").getAsJsonArray().get(1).getAsFloat();
		float originZ = rotateData.get("origin").getAsJsonArray().get(2).getAsFloat();

		String axis = rotateData.get("axis").getAsString();

		float angle = -rotateData.get("angle").getAsFloat();
		if(axis.equals("z"))
			angle = -angle;

		boolean rescale = false;
		if (rotateData.has("rescale"))
			rescale = rotateData.get("rescale").getAsBoolean();

		float cosR = (float) Math.cos(Math.toRadians(angle));
		float sinR = (float) Math.sin(Math.toRadians(angle));
		float scaling = 1.0f;
		if (rescale)
			scaling = 1.0f / Math.max(cosR, sinR);

		float[] oldPoints = Arrays.copyOf(points, points.length);

		if (axis.equals("x")) {
			for (int i = 0; i < 12; i += 3) {
				points[i + 2] = ((oldPoints[i + 2] - originZ) * cosR - (oldPoints[i + 1] - originY) * sinR) * scaling
						+ originZ;
				points[i + 1] = ((oldPoints[i + 2] - originZ) * sinR + (oldPoints[i + 1] - originY) * cosR) * scaling
						+ originY;
			}
		} else if (axis.equals("y")) {
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = ((oldPoints[i + 0] - originX) * cosR - (oldPoints[i + 2] - originZ) * sinR) * scaling
						+ originX;
				points[i + 2] = ((oldPoints[i + 0] - originX) * sinR + (oldPoints[i + 2] - originZ) * cosR) * scaling
						+ originZ;
			}
		} else if (axis.equals("z")) {
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = ((oldPoints[i + 0] - originX) * cosR - (oldPoints[i + 1] - originY) * sinR) * scaling
						+ originX;
				points[i + 1] = ((oldPoints[i + 0] - originX) * sinR + (oldPoints[i + 1] - originY) * cosR) * scaling
						+ originY;
			}
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}

	public void rotate(float rotateX, float rotateY, boolean uvLock) {
		// X Rotation
		float cosR = (float) Math.cos(Math.toRadians(rotateX));
		float sinR = (float) Math.sin(Math.toRadians(rotateX));

		if (rotateX != 0.0) {

			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 2] = ((oldPoints[i + 2] - 8.0f) * cosR - (oldPoints[i + 1] - 8.0f) * sinR) + 8.0f;
				points[i + 1] = ((oldPoints[i + 2] - 8.0f) * sinR + (oldPoints[i + 1] - 8.0f) * cosR) + 8.0f;
			}

			// UV lock X rotation
			if (uvLock) {
				if (direction == Direction.WEST || direction == Direction.EAST) {
					float[] oldUVs = Arrays.copyOf(uvs, uvs.length);
					float rotation = -rotateX;
					if (direction == Direction.EAST)
						rotation = rotateX;
					float uvPivotU = 8.0f;
					float uvPivotV = 8.0f;
					cosR = (float) Math.cos(Math.toRadians(rotation));
					sinR = (float) Math.sin(Math.toRadians(rotation));
					for (int i = 0; i < 8; i += 2) {
						uvs[i] = ((oldUVs[i] - uvPivotU) * cosR - (oldUVs[i + 1] - uvPivotV) * sinR) + uvPivotU;
						uvs[i + 1] = ((oldUVs[i] - uvPivotU) * sinR + (oldUVs[i + 1] - uvPivotV) * cosR) + uvPivotV;
					}
				}
			}

			// Update direction X rotation
			float dirRotateX = rotateX;
			if (dirRotateX < 0.0f)
				dirRotateX += 360.0f;
			while (dirRotateX > 45.0f) {
				dirRotateX -= 90.0f;
				switch (direction) {
				case DOWN:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.UP;
					break;
				case UP:
					direction = Direction.NORTH;
					// Rotate UVs 180 degrees
					if(uvLock)
						for (int i = 0; i < uvs.length; ++i)
							uvs[i] = 16.0f - uvs[i];
					break;
				case NORTH:
					direction = Direction.DOWN;
					// Rotate UVs 180 degrees
					if(uvLock)
						for (int i = 0; i < uvs.length; ++i)
							uvs[i] = 16.0f - uvs[i];
					break;
				default:
					break;
				}
			}

		}

		if (rotateY != 0.0) {
			// Rotate Y
			cosR = (float) Math.cos(Math.toRadians(rotateY));
			sinR = (float) Math.sin(Math.toRadians(rotateY));
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = ((oldPoints[i + 0] - 8.0f) * cosR - (oldPoints[i + 2] - 8.0f) * sinR) + 8.0f;
				points[i + 2] = ((oldPoints[i + 0] - 8.0f) * sinR + (oldPoints[i + 2] - 8.0f) * cosR) + 8.0f;
			}

			// UV Lock Rotate Y
			if (uvLock) {
				if (direction == Direction.DOWN || direction == Direction.UP) {
					float[] oldUVs = Arrays.copyOf(uvs, uvs.length);
					float rotation = rotateY;
					if (direction == Direction.UP)
						rotation = -rotateY;
					float uvPivotU = 8.0f;
					float uvPivotV = 8.0f;
					cosR = (float) Math.cos(Math.toRadians(rotation));
					sinR = (float) Math.sin(Math.toRadians(rotation));
					for (int i = 0; i < 8; i += 2) {
						uvs[i] = ((oldUVs[i] - uvPivotU) * cosR - (oldUVs[i + 1] - uvPivotV) * sinR) + uvPivotU;
						uvs[i + 1] = ((oldUVs[i] - uvPivotU) * sinR + (oldUVs[i + 1] - uvPivotV) * cosR) + uvPivotV;
					}
				}
			}

			// Update direction Y rotation
			float dirRotateY = rotateY;
			if (dirRotateY < 0.0f)
				dirRotateY += 360.0f;
			while (dirRotateY > 45.0f) {
				dirRotateY -= 90.0f;
				switch (direction) {
				case NORTH:
					direction = Direction.EAST;
					break;
				case EAST:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.WEST;
					break;
				case WEST:
					direction = Direction.NORTH;
					break;
				default:
					break;
				}
			}
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}
	
	public void rotate(float rotateX, float rotateY, float rotateZ) {
		// X Rotation
		float cosR = (float) Math.cos(Math.toRadians(rotateX));
		float sinR = (float) Math.sin(Math.toRadians(rotateX));

		if (rotateX != 0.0) {

			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 2] = ((oldPoints[i + 2] - 8.0f) * cosR - (oldPoints[i + 1] - 8.0f) * sinR) + 8.0f;
				points[i + 1] = ((oldPoints[i + 2] - 8.0f) * sinR + (oldPoints[i + 1] - 8.0f) * cosR) + 8.0f;
			}

			// Update direction X rotation
			float dirRotateX = rotateX;
			if (dirRotateX < 0.0f)
				dirRotateX += 360.0f;
			while (dirRotateX > 45.0f) {
				dirRotateX -= 90.0f;
				switch (direction) {
				case DOWN:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.UP;
					// Rotate UVs 180 degrees
					for (int i = 0; i < uvs.length; ++i)
						uvs[i] = 16.0f - uvs[i];
					break;
				case UP:
					direction = Direction.NORTH;
					break;
				case NORTH:
					direction = Direction.DOWN;
					// Rotate UVs 180 degrees
					for (int i = 0; i < uvs.length; ++i)
						uvs[i] = 16.0f - uvs[i];
					break;
				default:
					break;
				}
			}

		}

		if (rotateY != 0.0) {
			// Rotate Y
			cosR = (float) Math.cos(Math.toRadians(rotateY));
			sinR = (float) Math.sin(Math.toRadians(rotateY));
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = ((oldPoints[i + 0] - 8.0f) * cosR - (oldPoints[i + 2] - 8.0f) * sinR) + 8.0f;
				points[i + 2] = ((oldPoints[i + 0] - 8.0f) * sinR + (oldPoints[i + 2] - 8.0f) * cosR) + 8.0f;
			}

			// Update direction Y rotation
			float dirRotateY = rotateY;
			if (dirRotateY < 0.0f)
				dirRotateY += 360.0f;
			while (dirRotateY > 45.0f) {
				dirRotateY -= 90.0f;
				switch (direction) {
				case NORTH:
					direction = Direction.EAST;
					break;
				case EAST:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.WEST;
					break;
				case WEST:
					direction = Direction.NORTH;
					break;
				default:
					break;
				}
			}
		}
		
		if (rotateZ != 0.0) {
			// Rotate Z
			cosR = (float) Math.cos(Math.toRadians(rotateZ));
			sinR = (float) Math.sin(Math.toRadians(rotateZ));
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = ((oldPoints[i + 0] - 8.0f) * cosR - (oldPoints[i + 1] - 8.0f) * sinR) + 8.0f;
				points[i + 1] = ((oldPoints[i + 0] - 8.0f) * sinR + (oldPoints[i + 1] - 8.0f) * cosR) + 8.0f;
			}

			// Update direction Z rotation
			float dirRotateZ = rotateZ;
			if (dirRotateZ < 0.0f)
				dirRotateZ += 360.0f;
			while (dirRotateZ > 45.0f) {
				dirRotateZ -= 90.0f;
				switch (direction) {
				case UP:
					direction = Direction.WEST;
					break;
				case WEST:
					direction = Direction.DOWN;
					break;
				case DOWN:
					direction = Direction.EAST;
					break;
				case EAST:
					direction = Direction.UP;
					break;
				default:
					break;
				}
			}
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}
	
	
	public void rotate(float rotateX, float rotateY, float rotateZ, float pivotX, float pivotY, float pivotZ) {
		// X Rotation
		float cosR = (float) Math.cos(Math.toRadians(rotateX));
		float sinR = (float) Math.sin(Math.toRadians(rotateX));
		for(int i = 0; i < 12; i += 3) {
			points[i + 0] -= pivotX;
			points[i + 1] -= pivotY;
			points[i + 2] -= pivotZ;
		}
		
		if (rotateX != 0.0f) {
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 2] = oldPoints[i + 2] * cosR - oldPoints[i + 1] * sinR;
				points[i + 1] = oldPoints[i + 2] * sinR + oldPoints[i + 1] * cosR;
			}

			// Update direction X rotation
			float dirRotateX = rotateX;
			if (dirRotateX < 0.0f)
				dirRotateX += 360.0f;
			while (dirRotateX > 45.0f) {
				dirRotateX -= 90.0f;
				switch (direction) {
				case DOWN:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.UP;
				case UP:
					direction = Direction.NORTH;
					break;
				case NORTH:
					direction = Direction.DOWN;
				default:
					break;
				}
			}

		}

		if (rotateY != 0.0f) {
			// Rotate Y
			cosR = (float) Math.cos(Math.toRadians(rotateY));
			sinR = (float) Math.sin(Math.toRadians(rotateY));
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = oldPoints[i + 0] * cosR - oldPoints[i + 2] * sinR;
				points[i + 2] = oldPoints[i + 0] * sinR + oldPoints[i + 2] * cosR;
			}

			// Update direction Y rotation
			float dirRotateY = rotateY;
			if (dirRotateY < 0.0f)
				dirRotateY += 360.0f;
			while (dirRotateY > 45.0f) {
				dirRotateY -= 90.0f;
				switch (direction) {
				case NORTH:
					direction = Direction.EAST;
					break;
				case EAST:
					direction = Direction.SOUTH;
					break;
				case SOUTH:
					direction = Direction.WEST;
					break;
				case WEST:
					direction = Direction.NORTH;
					break;
				default:
					break;
				}
			}
		}
		
		if (rotateZ != 0.0f) {
			// Rotate Z
			cosR = (float) Math.cos(Math.toRadians(rotateZ));
			sinR = (float) Math.sin(Math.toRadians(rotateZ));
			float[] oldPoints = Arrays.copyOf(points, points.length);
			for (int i = 0; i < 12; i += 3) {
				points[i + 0] = oldPoints[i + 0] * cosR - oldPoints[i + 1] * sinR;
				points[i + 1] = oldPoints[i + 0] * sinR + oldPoints[i + 1] * cosR;
			}

			// Update direction Z rotation
			float dirRotateZ = rotateZ;
			if (dirRotateZ < 0.0f)
				dirRotateZ += 360.0f;
			while (dirRotateZ > 45.0f) {
				dirRotateZ -= 90.0f;
				switch (direction) {
				case UP:
					direction = Direction.WEST;
					break;
				case WEST:
					direction = Direction.DOWN;
					break;
				case DOWN:
					direction = Direction.EAST;
					break;
				case EAST:
					direction = Direction.UP;
					break;
				default:
					break;
				}
			}
		}
		for(int i = 0; i < 12; i += 3) {
			points[i + 0] += pivotX;
			points[i + 1] += pivotY;
			points[i + 2] += pivotZ;
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}
	
	
	public void flip(boolean x, boolean y, boolean z) {
		float scaleX = x ? -1f : 1f;
		float scaleY = y ? -1f : 1f;
		float scaleZ = z ? -1f : 1f;
		
		for(int i = 0; i < 4; ++i) {
			points[i*3+0] = (points[i*3+0] - 8f) * scaleX + 8f;
			points[i*3+1] = (points[i*3+1] - 8f) * scaleY + 8f;
			points[i*3+2] = (points[i*3+2] - 8f) * scaleZ + 8f;
		}
		
		if(x) {
			if(direction == Direction.EAST || direction == Direction.WEST)
				direction = direction.getOpposite();
		}
		if(y) {
			if(direction == Direction.UP || direction == Direction.DOWN)
				direction = direction.getOpposite();
		}
		if(z) {
			if(direction == Direction.NORTH || direction == Direction.SOUTH)
				direction = direction.getOpposite();
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}
	
	public void mirror(boolean x, boolean y, boolean z, float pivotX, float pivotY, float pivotZ) {
		float scaleX = x ? -1f : 1f;
		float scaleY = y ? -1f : 1f;
		float scaleZ = z ? -1f : 1f;
		
		for(int i = 0; i < 4; ++i) {
			points[i*3+0] = (points[i*3+0] - pivotX) * scaleX + pivotX;
			points[i*3+1] = (points[i*3+1] - pivotY) * scaleY + pivotY;
			points[i*3+2] = (points[i*3+2] - pivotZ) * scaleZ + pivotZ;
		}
		
		if(x) {
			if(direction == Direction.EAST || direction == Direction.WEST)
				direction = direction.getOpposite();
		}
		if(y) {
			if(direction == Direction.UP || direction == Direction.DOWN)
				direction = direction.getOpposite();
		}
		if(z) {
			if(direction == Direction.NORTH || direction == Direction.SOUTH)
				direction = direction.getOpposite();
		}
		
		float[] minMaxPoints = {
				Math.min(points[0*3+0], points[2*3+0]),
				Math.min(points[0*3+1], points[2*3+1]),
				Math.min(points[0*3+2], points[2*3+2]),
				Math.max(points[0*3+0], points[2*3+0]),
				Math.max(points[0*3+1], points[2*3+1]),
				Math.max(points[0*3+2], points[2*3+2]),
		};
		calculateDirection();
		calculateOcclusion(minMaxPoints);
	}
	
	public void setTexture(String texture) {
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

	public String getTexture() {
		return texture;
	}

	public boolean isValid() {
		return !texture.isEmpty();
	}

	public float[] getPoints() {
		return points;
	}

	public float[] getUVs() {
		return uvs;
	}
	
	public float[] getVertexColors() {
		return vertexColors;
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
	
	public void translate(float x, float y, float z) {
		for(int i = 0; i < points.length; i += 3) {
			points[i] += x;
			points[i+1] += y;
			points[i+2] += z;
		}
	}

	public void scale(float scale) {
		for(int i = 0; i < points.length; ++i) {
			points[i] = (points[i] - 8.0f) * scale + 8.0f;
		}
	}
	
	public void scale(float scaleX, float scaleY, float scaleZ) {
		for(int i = 0; i < points.length; i += 3) {
			points[i] = (points[i] - 8f) * scaleX + 8f;
			points[i+1] = (points[i+1] - 8f) * scaleY + 8f;
			points[i+2] = (points[i+2] - 8f) * scaleZ + 8f;
		}
	}
	
	public void scale(float scaleX, float scaleY, float scaleZ, float pivotX, float pivotY, float pivotZ) {
		for(int i = 0; i < points.length; i += 3) {
			points[i] = (points[i] - pivotX) * scaleX + pivotX;
			points[i+1] = (points[i+1] - pivotY) * scaleY + pivotY;
			points[i+2] = (points[i+2] - pivotZ) * scaleZ + pivotZ;
		}
	}
	
	public void setFaceColour(float r, float g, float b) {
		this.vertexColors = new float[4 * 3];
		for(int i = 0; i < this.vertexColors.length; i += 3) {
			this.vertexColors[i] = r;
			this.vertexColors[i+1] = g;
			this.vertexColors[i+2] = b;
		}
	}
	
	public void setVertexColors(float[] vertexColors) {
		this.vertexColors = vertexColors;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public void calculateNormal(float[] out) {
		float x1 = points[1*3+0] - points[0*3+0];
		float y1 = points[1*3+1] - points[0*3+1];
		float z1 = points[1*3+2] - points[0*3+2];
		
		float x2 = points[3*3+0] - points[0*3+0];
		float y2 = points[3*3+1] - points[0*3+1];
		float z2 = points[3*3+2] - points[0*3+2];
		
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

}
