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

import nl.bramstout.mcworldexporter.export.VertexColorSet.VertexColorFace;
import nl.bramstout.mcworldexporter.modifier.ModifierContext;
import nl.bramstout.mcworldexporter.modifier.ModifierNode;

/**
 * "getVertexColor" node outputs the vertex colour for
 * the specified colorset identifier.
 * It outputs null if no colorset exists of that name.
 */
public class ModifierNodeGetVertexColor extends ModifierNode{

	public Attribute colorset;
	
	public ModifierNodeGetVertexColor(String name) {
		super(name);
		this.colorset = new Attribute(this, new Value("Cd"));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value valueColormap = context.getValue(colorset);
		String colormapName = valueColormap.getString();
		
		if(context.vertexColors != null) {
			for(VertexColorFace face : context.vertexColors) {
				if(face.name.equals(colormapName)) {
					if(face.componentCount == 1)
						return new Value(face.r0);
					else if(face.componentCount == 2)
						return new Value(face.r0, face.g0);
					else if(face.componentCount == 3)
						return new Value(face.r0, face.g0, face.b0);
					else if(face.componentCount == 4)
						return new Value(face.r0, face.g0, face.b0, face.a0);
				}
			}
		}
		return new Value();
	}

}
