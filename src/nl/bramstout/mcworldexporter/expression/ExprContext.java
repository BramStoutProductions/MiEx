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

package nl.bramstout.mcworldexporter.expression;

import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueThisBlock;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInGenerator;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class ExprContext {

	public String name;
	public NbtTagCompound properties;
	public boolean isLocationDependent;
	public int x;
	public int y;
	public int z;
	public float fx;
	public float fy;
	public float fz;
	public float time;
	public Model model;
	public ExprValue thisBlock;
	public Map<String, ExprValue> variables;
	public ExprValue globals;
	public Map<String, ExprValue> builtins;
	public Map<String, BuiltInGenerator> localGenerators;
	public Map<String, ExprValue> localFunctions;
	public ExprValue returnValue;
	
	public ExprContext(String name, NbtTagCompound properties, boolean isLocationDependent, int x, int y, int z, 
					float fx, float fy, float fz, float time, Model model, 
					ExprValue globals, Map<String, ExprValue> builtins, Map<String, BuiltInGenerator> localGenerators,
					Map<String, ExprValue> localFunctions) {
		this.name = name;
		this.properties = properties;
		this.isLocationDependent = isLocationDependent;
		this.x = x;
		this.y = y;
		this.z = z;
		this.fx = fx;
		this.fy = fy;
		this.fz = fz;
		this.time = time;
		this.model = model;
		this.thisBlock = new ExprValue(new ExprValueThisBlock(this));
		this.variables = new HashMap<String, ExprValue>();
		this.globals = globals;
		this.builtins = builtins;
		this.localGenerators = localGenerators;
		this.localFunctions = localFunctions;
		this.returnValue = null;
	}
	
	public ExprContext(ExprContext other) {
		this.name = other.name;
		this.properties = other.properties;
		this.isLocationDependent = other.isLocationDependent;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.fx = other.fx;
		this.fy = other.fy;
		this.fz = other.fz;
		this.time = other.time;
		this.model = other.model;
		this.thisBlock = new ExprValue(new ExprValueThisBlock(this));
		this.variables = new HashMap<String, ExprValue>();
		this.globals = other.globals;
		this.builtins = other.builtins;
		this.localGenerators = other.localGenerators;
		this.localFunctions = other.localFunctions;
		this.returnValue = null;
	}
	
}
