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
 * "remap" node which takes the value of input and remaps it from the input range to the output range.
 */
public class ModifierNodeRemap extends ModifierNode{

	public Attribute input;
	public Attribute inMin;
	public Attribute inMax;
	public Attribute outMin;
	public Attribute outMax;
	public Attribute pow;
	public Attribute clamp;
	
	public ModifierNodeRemap(String name) {
		super(name);
		
		this.input = new Attribute(this, new Value(0f));
		this.inMin = new Attribute(this, new Value(0f));
		this.inMax = new Attribute(this, new Value(1f));
		this.outMin = new Attribute(this, new Value(0f));
		this.outMax = new Attribute(this, new Value(1f));
		this.pow = new Attribute(this, new Value(1f));
		this.clamp = new Attribute(this, new Value(true));
	}

	@Override
	public Value evaluate(ModifierContext context) {
		Value valueInput = context.getValue(input);
		Value valueInMin = context.getValue(inMin);
		Value valueInMax = context.getValue(inMax);
		Value valueOutMin = context.getValue(outMin);
		Value valueOutMax = context.getValue(outMax);
		Value valuePow = context.getValue(pow);
		Value valueClamp = context.getValue(clamp);
		
		valueInMax = valueInMax.subtract(valueInMin);
		valueOutMax = valueOutMax.subtract(valueOutMin);
		
		valueInput = valueInput.subtract(valueInMin).divide(valueInMax);
		if(valueClamp.getBool()) {
			valueInput = valueInput.min(new Value(1.0f)).max(new Value(0.0f));
		}
		valueInput = valueInput.pow(valuePow);
		valueInput = valueInput.multiply(valueOutMax).add(valueOutMin);
		
		return valueInput;
	}

}


