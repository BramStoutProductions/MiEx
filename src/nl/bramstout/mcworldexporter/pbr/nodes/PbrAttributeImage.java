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
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrAttributeImage extends PbrAttribute{

	private PbrImage value;
	private PbrImageConstant defaultValue;
	
	protected PbrAttributeImage(PbrNode node, boolean isOutput, boolean isContextSensitive, PbrImageConstant defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	@Override
	public Object getValue(PbrContext context) {
		return getImageValue(context);
	}
	
	public PbrImage getImageValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			PbrImage val = (PbrImage) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val;
		}
		return value;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	public PbrImage getDefaultImageValue() {
		return defaultValue;
	}

	@Override
	public void setValue(Object value, PbrContext context) {
		PbrImage newValue = null;
		if(value instanceof Number) {		
			float fv = ((Number) value).floatValue();
			newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
		}else if(value instanceof Boolean) {
			float fv = ((Boolean) value).booleanValue() ? 1f : 0f;
			newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
		}else if(value instanceof String) {
			try {
				float fv = Float.parseFloat((String) value);
				newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			newValue = (PbrImage) value;
		}else if(value instanceof RGBA) {
			newValue = new PbrImageConstant((RGBA) value);
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(newValue != null) {
			if(context == null)
				this.value = newValue;
			else
				context.valueCache.put(this, newValue);
		}
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeImage) other).value;
	}

}
