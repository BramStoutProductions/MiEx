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

public class PbrNodeResize extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeFloat scaleWidth = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeFloat scaleHeight = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest", 
												"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat", 
												"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeResize(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == scaleWidth || attr == scaleHeight || attr == interpolation || attr == boundary)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage input = this.input.getImageValue(context);
			float scaleWidth = this.scaleWidth.getFloatValue(context);
			float scaleHeight = this.scaleHeight.getFloatValue(context);
			int newWidth = (int) Math.max(((float) input.getWidth()) * scaleWidth, 1f);
			int newHeight = (int) Math.max(((float) input.getHeight()) * scaleHeight, 1f);
			
			PbrImage outImg = null;
			if(newWidth == 1 && newHeight == 1) {
				outImg = new PbrImageConstant(new RGBA());
			}else {
				outImg = new PbrImageRaster(newWidth, newHeight);
			}
			
			Boundary boundary = Boundary.REPEAT;
			int boundaryIndex = this.boundary.getIndexValue(context);
			if (boundaryIndex == 0)
				boundary = Boundary.EMPTY;
			else if (boundaryIndex == 1)
				boundary = Boundary.CLIP;
			else if (boundaryIndex == 2)
				boundary = Boundary.REPEAT;

			Interpolation interpolation = Interpolation.NEAREST;
			int interpolationIndex = this.interpolation.getIndexValue(context);
			if (interpolationIndex == 0)
				interpolation = Interpolation.NEAREST;
			else if (interpolationIndex == 1)
				interpolation = Interpolation.LINEAR;
			else if (interpolationIndex == 2)
				interpolation = Interpolation.CUBIC;
			
			RGBA rgba = new RGBA();
			for(int j = 0; j < newHeight; ++j) {
				for(int i = 0; i < newWidth; ++i) {
					input.sample(((float) i) / ((float) newWidth) * ((float) input.getWidth()), 
							((float) j) / ((float) newHeight) * ((float) input.getHeight()), 
							boundary, interpolation, rgba);
					outImg.write(i, j, boundary, rgba);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeResize(getName(), graph);
	}

}
