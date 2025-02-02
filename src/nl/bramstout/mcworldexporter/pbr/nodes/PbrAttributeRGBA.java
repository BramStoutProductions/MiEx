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

public class PbrAttributeRGBA extends PbrAttribute{

	private RGBA value;
	private RGBA defaultValue;
	
	public PbrAttributeRGBA(PbrNode node, boolean isOutput, boolean isContextSensitive, RGBA defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = new RGBA(defaultValue);
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return getRGBAValue(context);
	}
	
	public RGBA getRGBAValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			RGBA val = (RGBA) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val;
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	public RGBA getDefaultRGBAValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		float newR = 0f;
		float newG = 0f;
		float newB = 0f;
		float newA = 1f;
		if(value instanceof Number) {		
			float fv = ((Number) value).floatValue();
			newR = fv;
			newG = fv;
			newB = fv;
			newA = fv;
		}else if(value instanceof Boolean) {
			float fv = ((Boolean) value).booleanValue() ? 1f : 0f;
			newR = fv;
			newG = fv;
			newB = fv;
			newA = fv;
		}else if(value instanceof String) {
			try {
				float fv = Float.parseFloat((String) value);
				newR = fv;
				newG = fv;
				newB = fv;
				newA = fv;
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newR = rgba.r;
			newG = rgba.g;
			newB = rgba.b;
			newA = rgba.a;
		}else if(value instanceof RGBA) {
			newR = ((RGBA) value).r;
			newG = ((RGBA) value).g;
			newB = ((RGBA) value).b;
			newA = ((RGBA) value).a;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(context == null) {
			this.value.r = newR;
			this.value.g = newG;
			this.value.b = newB;
			this.value.a = newA;
		}else {
			context.valueCache.put(this, new RGBA(newR, newG, newB, newA));
		}
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value.r = ((PbrAttributeRGBA) other).value.r;
		this.value.g = ((PbrAttributeRGBA) other).value.g;
		this.value.b = ((PbrAttributeRGBA) other).value.b;
		this.value.a = ((PbrAttributeRGBA) other).value.a;
	}
	
}
