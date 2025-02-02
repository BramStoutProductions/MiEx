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
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeRemap extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA inMin = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 0f));
	public PbrAttributeRGBA inMax = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeRGBA outMin = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 0f));
	public PbrAttributeRGBA outMax = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeRGBA gamma = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeBoolean clamp = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeRemap(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == inMin || attr == inMax || 
				attr == outMin || attr == outMax || attr == gamma ||
				attr == clamp) {
			output.notifyChange(context);
		}
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage outImg = input.getImageValue(context).copy();
			
			RGBA inMin = this.inMin.getRGBAValue(context);
			RGBA inMax = this.inMax.getRGBAValue(context);
			RGBA outMin = this.outMin.getRGBAValue(context);
			RGBA outMax = this.outMax.getRGBAValue(context);
			RGBA gamma = this.gamma.getRGBAValue(context);
			boolean clamp = this.clamp.getBooleanValue(context);
			
			RGBA rgba = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					
					remap(rgba, inMin, inMax, outMin, outMax, gamma, clamp);
					
					outImg.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	private void remap(RGBA rgba, RGBA inMin, RGBA inMax, RGBA outMin, RGBA outMax, RGBA gamma, boolean clamp) {
		RGBA inScale = new RGBA(inMax).sub(inMin);
		RGBA outScale = new RGBA(outMax).sub(outMin);
		
		rgba.sub(inMin).div(inScale);
		if(clamp) {
			rgba.r = Math.min(Math.max(rgba.r, 0f), 1f);
			rgba.g = Math.min(Math.max(rgba.g, 0f), 1f);
			rgba.b = Math.min(Math.max(rgba.b, 0f), 1f);
			rgba.a = Math.min(Math.max(rgba.a, 0f), 1f);
		}
		rgba.pow(gamma).mult(outScale).add(outMin);
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeRemap(getName(), graph);
	}

}
