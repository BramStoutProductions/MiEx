package nl.bramstout.mcworldexporter.model;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class ModelLocator {

	private String name;
	public Vector3f offset;
	public Vector3f rotation;
	public boolean ignoreInheritedScale;
	public ModelBone bone;
	
	public ModelLocator(String name) {
		this.name = name.toLowerCase();
		offset = new Vector3f();
		rotation = new Vector3f();
		ignoreInheritedScale = false;
		bone = null;
	}
	
	public ModelLocator(ModelLocator other) {
		this.name = other.name;
		this.offset = other.offset;
		this.rotation = other.rotation;
		this.ignoreInheritedScale = other.ignoreInheritedScale;
		this.bone = other.bone;
	}
	
	public Matrix getLocalMatrix() {
		return Matrix.translate(offset).mult(Matrix.rotate(rotation));
	}
	
	public Matrix getMatrix() {
		Matrix localMatrix = getLocalMatrix();
		Matrix parentMatrix = new Matrix();
		if(bone != null)
			parentMatrix = bone.getMatrix();
		
		return parentMatrix.mult(localMatrix);
	}
	
	public String getName() {
		return name;
	}
	
}
