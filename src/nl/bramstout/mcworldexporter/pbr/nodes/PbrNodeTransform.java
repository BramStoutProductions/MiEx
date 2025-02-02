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

public class PbrNodeTransform extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeFloat translateX = new PbrAttributeFloat(this, false, false, 0f);
	public PbrAttributeFloat translateY = new PbrAttributeFloat(this, false, false, 0f);
	public PbrAttributeFloat rotate = new PbrAttributeFloat(this, false, false, 0f);
	public PbrAttributeFloat scaleX = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeFloat scaleY = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeFloat pivotX = new PbrAttributeFloat(this, false, false, 0.5f);
	public PbrAttributeFloat pivotY = new PbrAttributeFloat(this, false, false, 0.5f);
	public PbrAttributeEnum coordinateSpace = new PbrAttributeEnum(this, false, false, "0-1", 
															"pixels", "0-1");
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest", 
															"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat", 
															"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeTransform(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == translateX || attr == translateY || attr == rotate || 
				attr == scaleX || attr == scaleY || attr == pivotX || attr == pivotY || 
				attr == coordinateSpace || attr == interpolation || attr == boundary)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage input = this.input.getImageValue(context);
			float translateX = this.translateX.getFloatValue(context);
			float translateY = this.translateY.getFloatValue(context);
			float rotate = this.rotate.getFloatValue(context);
			float scaleX = this.scaleX.getFloatValue(context);
			float scaleY = this.scaleY.getFloatValue(context);
			float pivotX = this.pivotX.getFloatValue(context);
			float pivotY = this.pivotY.getFloatValue(context);
			int coordinateSpace = this.coordinateSpace.getIndexValue(context);
			
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
			
			if(coordinateSpace == 1) {
				// Convert 0-1 to pixels
				translateX *= (float) input.getWidth();
				translateY *= (float) input.getHeight();
				pivotX *= (float) input.getWidth();
				pivotY *= (float) input.getHeight();
			}
			float sinR = (float) Math.sin(Math.toRadians(rotate));
			float cosR = (float) Math.cos(Math.toRadians(rotate));
			
			PbrImage outImg = new PbrImageRaster(input.getWidth(), input.getHeight());
			
			RGBA rgba = new RGBA();
			float ii = 0f;
			float jj = 0f;
			float ii2 = 0f;
			float jj2 = 0f;
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					ii = ((float) i) - pivotX;
					jj = ((float) j) - pivotY;
					
					ii /= scaleX;
					jj /= scaleY;
					
					ii2 = ii * cosR - jj * sinR;
					jj2 = ii * sinR + jj * cosR;
					
					ii = ii2 + pivotX;
					jj = jj2 + pivotY;
					
					ii -= translateX;
					jj -= translateY;
					
					input.sample(ii2, jj2, boundary, interpolation, rgba);
					outImg.write(i, j, boundary, rgba);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeTransform(getName(), graph);
	}

}
