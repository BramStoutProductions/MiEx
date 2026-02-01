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

package nl.bramstout.mcworldexporter.math;

import java.util.Arrays;

public class Matrix {
	
	public float[] data;
	
	public Matrix() {
		data = new float[] {
				1f, 0f, 0f, 0f,
				0f, 1f, 0f, 0f,
				0f, 0f, 1f, 0f,
				0f, 0f, 0f, 1f
		};
	}
	
	public Matrix(float[] data) {
		this.data = data;
	}
	
	public Matrix(Matrix other) {
		this.data = Arrays.copyOf(other.data, 16);
	}
	
	/**
	 * Multiplies this matrix with another matrix.
	 * This matrix is on the left side.
	 * @param other Matrix on the right side.
	 * @return
	 */
	public Matrix mult(Matrix other) {
		Matrix res = new Matrix();
		
		for(int i = 0; i < 4; ++i) {
			for(int j = 0; j < 4; ++j) {
				res.data[j*4 + i] = 0f;
				for(int k = 0; k < 4; ++k) {
					res.data[j*4 + i] += data[j*4 + k] * other.data[k*4 + i];
				}
			}
		}
		
		return res;
	}
	
	public Vector3f transformPoint(Vector3f point) {
		Vector3f res = new Vector3f();
		res.x = point.x * data[0*4 + 0] + point.y * data[0*4 + 1] + point.z * data[0*4 + 2] + data[0*4 + 3];
		res.y = point.x * data[1*4 + 0] + point.y * data[1*4 + 1] + point.z * data[1*4 + 2] + data[1*4 + 3];
		res.z = point.x * data[2*4 + 0] + point.y * data[2*4 + 1] + point.z * data[2*4 + 2] + data[2*4 + 3];
		return res;
	}
	
	public Vector3f transformDirection(Vector3f point) {
		Vector3f res = new Vector3f();
		res.x = point.x * data[0*4 + 0] + point.y * data[0*4 + 1] + point.z * data[0*4 + 2];
		res.y = point.x * data[1*4 + 0] + point.y * data[1*4 + 1] + point.z * data[1*4 + 2];
		res.z = point.x * data[2*4 + 0] + point.y * data[2*4 + 1] + point.z * data[2*4 + 2];
		return res;
	}
	
	public static Matrix translate(float x, float y, float z) {
		return new Matrix(new float[] {
				1f, 0f, 0f, x,
				0f, 1f, 0f, y,
				0f, 0f, 1f, z,
				0f, 0f, 0f, 1f
		});
	}
	
	public static Matrix translate(Vector3f translation) {
		return translate(translation.x, translation.y, translation.z);
	}
	
	public static Matrix scale(float x, float y, float z) {
		return new Matrix(new float[] {
				x, 0f, 0f, 0f,
				0f, y, 0f, 0f,
				0f, 0f, z, 0f,
				0f, 0f, 0f, 1f
		});
	}
	
	public static Matrix scale(Vector3f scaling) {
		return scale(scaling.x, scaling.y, scaling.z);
	}
	
	public static Matrix quaternion(float i, float j, float k, float r) {
		float s = 1f / (float)Math.sqrt(i * i + j * j + k * k + r * r);
		return new Matrix(new float[] {
			1f - 2f * s * (j * j + k * k), 		 2f * s * (i * j - k * r), 		 2f * s * (i * k + j * r), 0f,
				 2f * s * (i * j + k * r), 	1f - 2f * s * (i * i + k * k),		 2f * s * (j * k - i * r), 0f,
				 2f * s * (i * k - j * r),		 2f * s * (j * k + i * r),	1f - 2f * s * (i * i + j * j), 0f,
			0f, 0f, 0f, 1f
		});
	}
	
	public static Matrix axisAngle(float x, float y, float z, float a) {
		float cosR = (float) Math.cos(a);
		float sinR = (float) Math.sin(a);
		float RcosR = 1f - cosR;
		return new Matrix(new float[] {
			x * x * RcosR + cosR,		x * y * RcosR - z * sinR,		x * z * RcosR + y * sinR, 0f,
			x * y * RcosR + z * sinR,	y * y * RcosR + cosR,			y * z * RcosR - z * sinR, 0f,
			x * z * RcosR - y * sinR, 	y * z * RcosR + z * sinR,		z * z * RcosR + cosR, 0f,
			0f, 0f, 0f, 1f
		});
	}
	
	public static Matrix rotateX(float angle) {
		float cosR = (float) Math.cos(Math.toRadians(angle));
		float sinR = (float) Math.sin(Math.toRadians(angle));
		return new Matrix(new float[] {
				1f, 0f,    0f,   0f,
				0f, cosR,  sinR, 0f,
				0f, -sinR, cosR, 0f,
				0f, 0f,    0f,   1f
		});
	}
	
	public static Matrix rotateY(float angle) {
		float cosR = (float) Math.cos(Math.toRadians(angle));
		float sinR = (float) Math.sin(Math.toRadians(angle));
		return new Matrix(new float[] {
				cosR, 0f, -sinR, 0f,
				0f,   1f, 0f,    0f,
				sinR, 0f, cosR,  0f,
				0f,   0f, 0f,    1f
		});
	}
	
	public static Matrix rotateZ(float angle) {
		float cosR = (float) Math.cos(Math.toRadians(angle));
		float sinR = (float) Math.sin(Math.toRadians(angle));
		return new Matrix(new float[] {
				cosR, -sinR, 0f, 0f,
				sinR, cosR,  0f, 0f,
				0f,   0f,    1f, 0f,
				0f,   0f,    0f, 1f
		});
	}
	
	public static Matrix rotate(float x, float y, float z) {
		return rotateZ(z).mult(rotateY(y).mult(rotateX(x)));
	}
	
	public static Matrix rotate(float x, float y, float z, Vector3f pivot) {
		return translate(pivot).mult(rotate(x, y, z).mult(translate(pivot.multiply(-1f))));
	}
	
	public static Matrix rotate(Vector3f rotation) {
		return rotate(rotation.x, rotation.y, rotation.z);
	}
	
	public static Matrix rotate(Vector3f rotation, Vector3f pivot) {
		return translate(pivot).mult(rotate(rotation).mult(translate(pivot.multiply(-1f))));
	}
	
}
