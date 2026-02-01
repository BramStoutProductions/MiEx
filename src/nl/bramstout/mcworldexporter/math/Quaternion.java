package nl.bramstout.mcworldexporter.math;

public class Quaternion {
	
	public float x;
	public float y;
	public float z;
	public float w;
	
	public Quaternion() {
		this(0f, 0f, 0f, 1f);
	}
	
	public Quaternion(float v) {
		this(v, v, v, 1f);
	}
	
	public Quaternion(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Quaternion(Quaternion other) {
		this(other.x, other.y, other.z, other.w);
	}
	
	public void set(float v) {
		set(v, v, v, 1f);
	}
	
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Matrix toMatrix() {
		return Matrix.quaternion(x, y, z, w);
	}

}
