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

package nl.bramstout.mcworldexporter.modifier.nodes;

import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

public class ModifierNodeSetFaceNormal extends ModifierNode{
	
	public Attribute normal;
	public Attribute strength;
	
	public ModifierNodeSetFaceNormal(String name) {
		super(name);
		this.normal = new Attribute(this, new Value(0f, 1f, 0f));
		this.strength = new Attribute(this, new Value(1f));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value normal = context.getValue(this.normal);
		Value strength = context.getValue(this.strength);
		
		float t = strength.getX();
		
		context.faceNormalX = normal.getX() * t + context.faceNormalX * (1f - t);
		context.faceNormalY = normal.getY() * t + context.faceNormalY * (1f - t);
		context.faceNormalZ = normal.getZ() * t + context.faceNormalZ * (1f - t);
		double length = Math.sqrt(context.faceNormalX * context.faceNormalX + 
								context.faceNormalY * context.faceNormalY + 
								context.faceNormalZ * context.faceNormalZ);
		if(length < 0.000001) {
			context.faceNormalX = 0f;
			context.faceNormalY = 1f;
			context.faceNormalZ = 0f;
		}else {
			context.faceNormalX /= length;
			context.faceNormalY /= length;
			context.faceNormalZ /= length;
		}
		
		return new Value(context.faceNormalX, context.faceNormalY, context.faceNormalZ);
	}
	
}
