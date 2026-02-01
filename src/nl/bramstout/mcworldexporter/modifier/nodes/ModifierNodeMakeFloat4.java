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

/**
 * "makeFloat4" node which constructs a FLOAT4 value from x, y, z and w
 */
public class ModifierNodeMakeFloat4 extends ModifierNode{

	public Attribute x;
	public Attribute y;
	public Attribute z;
	public Attribute w;
	
	public ModifierNodeMakeFloat4(String name) {
		super(name);
		this.x = new Attribute(this, new Value(0f));
		this.y = new Attribute(this, new Value(0f));
		this.z = new Attribute(this, new Value(0f));
		this.w = new Attribute(this, new Value(0f));
	}
	
	@Override
	public Value evaluate(ModifierContext context) {
		Value valueX = context.getValue(x);
		Value valueY = context.getValue(y);
		Value valueZ = context.getValue(z);
		Value valueW = context.getValue(w);
		
		return new Value(valueX.getR(), valueY.getR(), valueZ.getR(), valueW.getR());
	}

}
