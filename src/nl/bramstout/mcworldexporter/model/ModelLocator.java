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
