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

import java.util.Arrays;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeNormaliseColor extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA outMin = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 0f));
	public PbrAttributeRGBA outMax = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeEnum outMidMode = new PbrAttributeEnum(this, false, false, "none", 
														"none", "average", "median");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeNormaliseColor(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == outMin || attr == outMax || attr == outMidMode)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage outImg = input.getImageValue(context).copy();
			
			RGBA outMin = this.outMin.getRGBAValue(context);
			RGBA outMax = this.outMax.getRGBAValue(context);
			int midMode = outMidMode.getIndexValue(context);
			
			RGBA rgba = new RGBA();
			RGBA rgbaMin = new RGBA();
			RGBA rgbaMax = new RGBA();
			RGBA rgbaMid = new RGBA();
			
			if(midMode == 2) {
				float[] rList = new float[outImg.getWidth() * outImg.getHeight()];
				float[] gList = new float[outImg.getWidth() * outImg.getHeight()];
				float[] bList = new float[outImg.getWidth() * outImg.getHeight()];
				float[] aList = new float[outImg.getWidth() * outImg.getHeight()];
				
				int index = 0;
				for(int j = 0; j < outImg.getHeight(); ++j) {
					for(int i = 0; i < outImg.getWidth(); ++i) {
						index = j * outImg.getWidth() + i;
						outImg.sample(i, j, Boundary.EMPTY, rgba);
						rList[index] = rgba.r;
						gList[index] = rgba.g;
						bList[index] = rgba.b;
						aList[index] = rgba.a;
					}
				}
				Arrays.sort(rList);
				Arrays.sort(gList);
				Arrays.sort(bList);
				Arrays.sort(aList);
				
				rgbaMin.r = rList[0];
				rgbaMin.g = gList[0];
				rgbaMin.b = bList[0];
				rgbaMin.a = aList[0];
				rgbaMax.r = rList[rList.length-1];
				rgbaMax.g = gList[gList.length-1];
				rgbaMax.b = bList[bList.length-1];
				rgbaMax.a = aList[aList.length-1];
				rgbaMid.r = rList[rList.length/2];
				rgbaMid.g = gList[gList.length/2];
				rgbaMid.b = bList[bList.length/2];
				rgbaMid.a = aList[aList.length/2];
			}else {
				float rgbCount = 0f;
				float aCount = 0f;
				for(int j = 0; j < outImg.getHeight(); ++j) {
					for(int i = 0; i < outImg.getWidth(); ++i) {
						outImg.sample(i, j, Boundary.EMPTY, rgba);
						rgbaMin.r = Math.min(rgbaMin.r, rgba.r);
						rgbaMin.g = Math.min(rgbaMin.g, rgba.g);
						rgbaMin.b = Math.min(rgbaMin.b, rgba.b);
						rgbaMin.a = Math.min(rgbaMin.a, rgba.a);
						rgbaMax.r = Math.max(rgbaMax.r, rgba.r);
						rgbaMax.g = Math.max(rgbaMax.g, rgba.g);
						rgbaMax.b = Math.max(rgbaMax.b, rgba.b);
						rgbaMax.a = Math.max(rgbaMax.a, rgba.a);
						
						if(rgba.a > 0f) {
							rgbaMid.r += rgba.r;
							rgbaMid.g += rgba.g;
							rgbaMid.b += rgba.b;
							rgbCount += 1f;
						}
						rgbaMid.a += rgba.a;
						aCount += 1f;
					}
				}
				rgbaMid.div(rgbCount, aCount);
			}
			RGBA scale = new RGBA(rgbaMax).sub(rgbaMin);
			RGBA outScale = new RGBA(outMax).sub(outMin);
			RGBA gamma = new RGBA(rgbaMid).sub(rgbaMin).div(scale);
			gamma.r = (float) (Math.log(0.5) / Math.log(gamma.r));
			gamma.g = (float) (Math.log(0.5) / Math.log(gamma.g));
			gamma.b = (float) (Math.log(0.5) / Math.log(gamma.b));
			gamma.a = (float) (Math.log(0.5) / Math.log(gamma.a));
			gamma.r = Math.min(Math.max(gamma.r, 0.1f), 10f);
			gamma.g = Math.min(Math.max(gamma.g, 0.1f), 10f);
			gamma.b = Math.min(Math.max(gamma.b, 0.1f), 10f);
			gamma.a = Math.min(Math.max(gamma.a, 0.1f), 10f);
			
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					
					remap(rgba, rgbaMin, scale, gamma, outMin, outScale, midMode > 0);
					
					outImg.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	private void remap(RGBA rgba, RGBA min, RGBA scale, RGBA gamma, RGBA outMin, RGBA outScale, boolean applyGamma) {
		rgba.sub(min).div(scale);
		if(applyGamma) {
			rgba.pow(gamma);
		}
		rgba.mult(outScale);
		rgba.add(outMin);
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeNormaliseColor(getName(), graph);
	}

}
