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
	
	public static Matrix rotate(Vector3f rotation) {
		return rotate(rotation.x, rotation.y, rotation.z);
	}
	
}
