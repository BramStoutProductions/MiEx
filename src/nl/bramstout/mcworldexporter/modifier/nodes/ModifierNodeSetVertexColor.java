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

import java.util.Arrays;

import nl.bramstout.mcworldexporter.export.VertexColorSet.VertexColorFace;
import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

/**
 * "setVertexColor" node sets the vertex colour for
 * the specified colorset identifier.
 */
public class ModifierNodeSetVertexColor extends ModifierNode{

	public Attribute colorset;
	public Attribute color;
	
	public ModifierNodeSetVertexColor(String name) {
		super(name);
		this.colorset = new Attribute(this, new Value("Cd"));
		this.color = new Attribute(this, new Value(1f, 1f, 1f));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value valueColormap = context.getValue(colorset);
		String colormapName = valueColormap.getString();
		Value valueColor = context.getValue(color);
		int componentCount = 1;
		if(valueColor.isFloat2())
			componentCount = 2;
		else if(valueColor.isFloat3())
			componentCount = 3;
		else if(valueColor.isFloat4())
			componentCount = 4;
		float r = valueColor.getR();
		float g = valueColor.getG();
		float b = valueColor.getB();
		float a = valueColor.getA();
		
		if(context.vertexColors != null) {
			for(VertexColorFace face : context.vertexColors) {
				if(face.name.equals(colormapName)) {
					face.componentCount = componentCount;
					face.r0 = face.r1 = face.r2 = face.r3 = r;
					face.g0 = face.g1 = face.g2 = face.g3 = g;
					face.b0 = face.b1 = face.b2 = face.b3 = b;
					face.a0 = face.a1 = face.a2 = face.a3 = a;
					return valueColor;
				}
			}
		}
		// No vertex colour previously defined, so let's make one
		if(context.vertexColors == null)
			context.vertexColors = new VertexColorFace[1];
		else
			context.vertexColors = Arrays.copyOf(context.vertexColors, context.vertexColors.length+1);
		
		VertexColorFace face = new VertexColorFace();
		context.vertexColors[context.vertexColors.length-1] = face;
		face.name = colormapName;
		face.componentCount = componentCount;
		face.r0 = face.r1 = face.r2 = face.r3 = r;
		face.g0 = face.g1 = face.g2 = face.g3 = g;
		face.b0 = face.b1 = face.b2 = face.b3 = b;
		face.a0 = face.a1 = face.a2 = face.a3 = a;
		return valueColor;
	}

}
