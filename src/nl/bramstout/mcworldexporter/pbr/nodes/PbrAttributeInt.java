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

package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeInt extends PbrAttribute{

	private int value;
	private int defaultValue;
	
	public PbrAttributeInt(PbrNode node, boolean isOutput, boolean isContextSensitive, int defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return Integer.valueOf(getIntValue(context));
	}
	
	public int getIntValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			Integer val = (Integer) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val.intValue();
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return Integer.valueOf(defaultValue);
	}
	
	public int getDefaultIntValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		int newValue = 0;
		if(value instanceof Number) {		
			newValue = ((Number) value).intValue();
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue() ? 1 : 0;
		}else if(value instanceof String) {
			try {
				newValue = Integer.parseInt((String) value);
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newValue = (int) rgba.r;
		}else if(value instanceof RGBA) {
			newValue = (int) ((RGBA) value).r;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(context == null)
			this.value = newValue;
		else
			context.valueCache.put(this, Integer.valueOf(newValue));
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeInt) other).value;
	}
	
}
