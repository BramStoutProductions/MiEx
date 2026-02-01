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
 * "cast" node which takes input and casts it to the type
 * specified by type.
 * type should be a string providing the name of the ValueType.
 */
public class ModifierNodeCast extends ModifierNode{

	public Attribute input;
	public Attribute type;
	
	public ModifierNodeCast(String name) {
		super(name);
		this.input = new Attribute(this, new Value());
		this.type = new Attribute(this, new Value("NULL"));
	}

	@Override
	public Value evaluate(ModifierContext context) {
		Value value = context.getValue(input);
		Value typeVal = context.getValue(type);
		String typeName = typeVal.getString();
		
		if(typeName.equalsIgnoreCase("null"))
			return value.castToType(ValueType.NULL);
		else if(typeName.equalsIgnoreCase("float"))
			return value.castToType(ValueType.FLOAT);
		else if(typeName.equalsIgnoreCase("float2"))
			return value.castToType(ValueType.FLOAT2);
		else if(typeName.equalsIgnoreCase("float3"))
			return value.castToType(ValueType.FLOAT3);
		else if(typeName.equalsIgnoreCase("float4"))
			return value.castToType(ValueType.FLOAT4);
		else if(typeName.equalsIgnoreCase("int"))
			return value.castToType(ValueType.INT);
		else if(typeName.equalsIgnoreCase("long"))
			return value.castToType(ValueType.LONG);
		else if(typeName.equalsIgnoreCase("bool"))
			return value.castToType(ValueType.BOOL);
		else if(typeName.equalsIgnoreCase("string"))
			return value.castToType(ValueType.STRING);
		else if(typeName.equalsIgnoreCase("array"))
			return value.castToType(ValueType.ARRAY);
		return new Value();
	}

}
