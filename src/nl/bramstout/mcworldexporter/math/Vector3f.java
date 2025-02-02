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
