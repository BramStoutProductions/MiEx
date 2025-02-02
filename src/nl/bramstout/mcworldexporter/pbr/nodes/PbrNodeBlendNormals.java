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
import nl.bramstout.mcworldexporter.pbr.PbrImage.Interpolation;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeBlendNormals extends PbrNode{

	public PbrAttributeImage bottom = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA(0.5f, 0.5f, 1f, 1f)));
	public PbrAttributeImage top = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA(0.5f, 0.5f, 1f, 1f)));
	public PbrAttributeImage factor = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest",
															"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat",
															"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA(0.5f, 0.5f, 1f, 1f)));
	
	public PbrNodeBlendNormals(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == bottom || attr == top || attr == factor || attr == interpolation || attr == boundary)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			Boundary boundary = Boundary.REPEAT;
			int boundaryIndex = this.boundary.getIndexValue(context);
			if(boundaryIndex == 0)
				boundary = Boundary.EMPTY;
			else if(boundaryIndex == 1)
				boundary = Boundary.CLIP;
			else if(boundaryIndex == 2)
				boundary = Boundary.REPEAT;
			
			Interpolation interpolation = Interpolation.NEAREST;
			int interpolationIndex = this.interpolation.getIndexValue(context);
			if(interpolationIndex == 0)
				interpolation = Interpolation.NEAREST;
			else if(interpolationIndex == 1)
				interpolation = Interpolation.LINEAR;
			else if(interpolationIndex == 2)
				interpolation = Interpolation.CUBIC;
			
			PbrImage imgBottom = bottom.getImageValue(context);
			PbrImage imgTop = top.getImageValue(context);
			PbrImage imgFactor = factor.getImageValue(context);
			
			int maxWidth = Math.max(imgBottom.getWidth(), imgTop.getWidth());
			int maxHeight = Math.max(imgBottom.getHeight(), imgTop.getHeight());
			PbrImage outImg = null;
			if(maxWidth <= 1 && maxHeight <= 1)
				outImg = new PbrImageConstant(new RGBA());
			else
				outImg = new PbrImageRaster(maxWidth, maxHeight);
			
			RGBA cBottom = new RGBA();
			RGBA cTop = new RGBA();
			RGBA cFactor = new RGBA();
			float length = 0f;
			for(float j = 0.5f; j < maxHeight; j += 1f) {
				for(float i = 0.5f; i < maxWidth; i += 1f) {
					imgBottom.sample((i / ((float) maxWidth)) * ((float) imgBottom.getWidth()), 
							(j / ((float) maxHeight)) * ((float) imgBottom.getHeight()), boundary, interpolation, cBottom);
					imgTop.sample((i / ((float) maxWidth)) * ((float) imgTop.getWidth()), 
							(j / ((float) maxHeight)) * ((float) imgTop.getHeight()), boundary, interpolation, cTop);
					imgFactor.sample((i / ((float) maxWidth)) * ((float) imgFactor.getWidth()), 
							(j / ((float) maxHeight)) * ((float) imgFactor.getHeight()), boundary, interpolation, cFactor);
					
					cBottom.r = cBottom.r * 2f - 1f;
					cBottom.g = cBottom.g * 2f - 1f;
					cTop.r = cTop.r * 2f - 1f;
					cTop.g = cTop.g * 2f - 1f;
					cBottom.mult(1f - cFactor.r).add(cTop.mult(cFactor.r));
					length = (float) Math.sqrt(cBottom.r * cBottom.r + cBottom.g * cBottom.g + cBottom.b * cBottom.b);
					cBottom.div(length, 1f);
					cBottom.r = (cBottom.r + 1f) / 2f;
					cBottom.g = (cBottom.g + 1f) / 2f;
					cBottom.a = 1f;
					
					outImg.write((int) i, (int) j, Boundary.EMPTY, cBottom);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeBlendNormals(getName(), graph);
	}

}
