package nl.bramstout.mcworldexporter.math;

public class Vector3f {
	
	public float x;
	public float y;
	public float z;
	
	public Vector3f() {
		this(0f, 0f, 0f);
	}
	
	public Vector3f(float v) {
		this(v, v, v);
	}
	
	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3f(Vector3f other) {
		this(other.x, other.y, other.z);
	}
	
	public void set(float v) {
		set(v, v, v);
	}
	
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3f add(Vector3f other) {
		return new Vector3f(x + other.x, y + other.y, z + other.z);
	}
	
	public Vector3f subtract(Vector3f other) {
		return new Vector3f(x - other.x, y - other.y, z - other.z);
	}
	
	public Vector3f multiply(Vector3f other) {
		return new Vector3f(x * other.x, y * other.y, z * other.z);
	}
	
	public Vector3f divide(Vector3f other) {
		return new Vector3f(x / other.x, y / other.y, z / other.z);
	}
	
	public Vector3f add(float v) {
		return new Vector3f(x + v, y + v, z + v);
	}
	
	public Vector3f subtract(float v) {
		return new Vector3f(x - v, y - v, z - v);
	}
	
	public Vector3f multiply(float v) {
		return new Vector3f(x * v, y * v, z * v);
	}
	
	public Vector3f divide(float v) {
		return new Vector3f(x / v, y / v, z / v);
	}
	
	public float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

}
