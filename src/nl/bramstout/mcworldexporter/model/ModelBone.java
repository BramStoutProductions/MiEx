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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class ModelBone {
	
	private String name;
	public Vector3f translation;
	public Vector3f rotation;
	public Vector3f scaling;
	public boolean visibility;
	public List<Integer> faceIds;
	private ModelBone parent;
	
	public ModelBone(String name) {
		this.name = name.toLowerCase();
		this.translation = new Vector3f();
		this.rotation = new Vector3f();
		this.scaling = new Vector3f(1f);
		this.visibility = true;
		this.faceIds = new ArrayList<Integer>();
		this.parent = null;
	}
	
	public ModelBone(ModelBone other) {
		this.name = other.name;
		this.translation = new Vector3f(other.translation);
		this.rotation = new Vector3f(other.rotation);
		this.scaling = new Vector3f(other.scaling);
		this.visibility = other.visibility;
		this.faceIds = new ArrayList<Integer>(other.faceIds);
		this.parent = other.parent;
	}
	
	public boolean isIdentity() {
		if(translation.x != 0f || translation.y != 0f || translation.z != 0f)
			return false;
		if(rotation.x != 0f || rotation.y != 0f || rotation.z != 0f)
			return false;
		if(scaling.x != 1f || scaling.y != 1f || scaling.z != 1f)
			return false;
		if(visibility == false)
			return false;
		return true;
	}
	
	public String getName() {
		return this.name;
	}
	
	public ModelBone getParent() {
		return parent;
	}
	
	public void setParent(ModelBone parent) {
		this.parent = parent;
	}
	
	public Vector3f getWorldSpacePivot() {
		if(parent == null)
			return translation;
		return parent.getWorldSpacePivot().add(translation);
	}
	
	public Matrix getLocalMatrix() {
		return Matrix.translate(translation).mult(
				Matrix.rotate(rotation).mult(
				Matrix.scale(scaling)));
	}
	
	public Matrix getMatrix() {
		if(parent == null)
			return getLocalMatrix();
		
		Matrix parentMatrix = parent.getMatrix();
		Matrix localMatrix = getLocalMatrix();
		
		return parentMatrix.mult(localMatrix);
	}
	
}
