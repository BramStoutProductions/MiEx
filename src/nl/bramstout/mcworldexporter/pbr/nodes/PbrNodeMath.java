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

public class PbrNodeMath extends PbrNode{

	public PbrAttributeEnum operator = new PbrAttributeEnum(this, false, false, "add", 
											"add", "subtract", "multiply", "divide", "pow",
											"expn", "exp2", "exp10", "logn", "log2", "log10",
											"min", "max", "normalise", "dot", "cross",
											"floor", "ceil", "fract", "modulo");
	public PbrAttributeImage a = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeImage b = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest",
											"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat",
											"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeMath(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == operator || attr == a || attr == b || attr == interpolation || attr == boundary)
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
			
			int operator = this.operator.getIndexValue(context);
			
			PbrImage imgA = a.getImageValue(context);
			PbrImage imgB = b.getImageValue(context);
			
			int maxWidth = Math.max(imgA.getWidth(), imgB.getWidth());
			int maxHeight = Math.max(imgA.getHeight(), imgB.getHeight());
			PbrImage outImg = null;
			if(maxWidth <= 1 && maxHeight <= 1)
				outImg = new PbrImageConstant(new RGBA());
			else
				outImg = new PbrImageRaster(maxWidth, maxHeight);
			
			RGBA cA = new RGBA();
			RGBA cB = new RGBA();
			RGBA cOut = new RGBA();
			for(float j = 0.5f; j < maxHeight; j += 1f) {
				for(float i = 0.5f; i < maxWidth; i += 1f) {
					imgA.sample((i / ((float) maxWidth)) * ((float) imgA.getWidth()), 
							(j / ((float) maxHeight)) * ((float) imgA.getHeight()), boundary, interpolation, cA);
					imgB.sample((i / ((float) maxWidth)) * ((float) imgB.getWidth()), 
							(j / ((float) maxHeight)) * ((float) imgB.getHeight()), boundary, interpolation, cB);
					
					doMath(cA, cB, cOut, operator);
					outImg.write((int) i, (int) j, Boundary.EMPTY, cOut);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	private void doMath(RGBA a, RGBA b, RGBA out, int operator) {
		switch(operator) {
		case 0: // add
			out.set(a).add(b);
			break;
		case 1: // subtract
			out.set(a).sub(b);
			break;
		case 2: // multiply
			out.set(a).mult(b);
			break;
		case 3: // divide
			out.set(a).div(b);
			break;
		case 4: // pow
			out.set(a).pow(b);
			break;
		case 5: // expn
			out.set((float) Math.E).pow(a);
			break;
		case 6: // exp2
			out.set(2f).pow(a);
			break;
		case 7: // exp10
			out.set(10f).pow(a);
			break;
		case 8: // logn
			out.r = (float) Math.log(a.r);
			out.g = (float) Math.log(a.g);
			out.b = (float) Math.log(a.b);
			out.a = (float) Math.log(a.a);
			break;
		case 9: // log2
			out.r = (float) Math.log(a.r) / (float) Math.log(2f);
			out.g = (float) Math.log(a.g) / (float) Math.log(2f);
			out.b = (float) Math.log(a.b) / (float) Math.log(2f);
			out.a = (float) Math.log(a.a) / (float) Math.log(2f);
			break;
		case 10: // log10
			out.r = (float) Math.log10(a.r);
			out.g = (float) Math.log10(a.g);
			out.b = (float) Math.log10(a.b);
			out.a = (float) Math.log10(a.a);
			break;
		case 11: // min
			out.r = Math.min(a.r, b.r);
			out.g = Math.min(a.g, b.g);
			out.b = Math.min(a.b, b.b);
			out.a = Math.min(a.a, b.a);
			break;
		case 12: // min
			out.r = Math.max(a.r, b.r);
			out.g = Math.max(a.g, b.g);
			out.b = Math.max(a.b, b.b);
			out.a = Math.max(a.a, b.a);
			break;
		case 13: // normalise
			float length = (float) Math.sqrt(a.r * a.r + a.g * a.g + a.b * a.b);
			if(length > 0.00000001f) {
				out.r = a.r / length;
				out.g = a.g / length;
				out.b = a.b / length;
			}else {
				out.r = 0f;
				out.g = 0f;
				out.b = 0f;
			}
			out.a = a.a;
			break;
		case 14: // dot
			out.r = a.r * b.r + a.g * b.g + a.b * b.b;
			out.g = out.r;
			out.b = out.b;
			out.a = a.a;
			break;
		case 15: // cross
			out.r = a.g * b.b - a.b * b.g;
			out.r = a.b * b.r - a.r * b.b;
			out.r = a.r * b.g - a.g * b.r;
			out.a = a.a;
			break;
		case 16: // floor
			out.r = (float) Math.floor(a.r);
			out.g = (float) Math.floor(a.g);
			out.b = (float) Math.floor(a.b);
			out.a = (float) Math.floor(a.a);
		case 17: // ceil
			out.r = (float) Math.ceil(a.r);
			out.g = (float) Math.ceil(a.g);
			out.b = (float) Math.ceil(a.b);
			out.a = (float) Math.ceil(a.a);
		case 18: // fract
			out.r = a.r - ((float) Math.floor(a.r));
			out.g = a.g - ((float) Math.floor(a.g));
			out.b = a.b - ((float) Math.floor(a.b));
			out.a = a.a - ((float) Math.floor(a.a));
		case 19: // modulo
			out.set(a).div(b);
			out.r = out.r - ((float) Math.floor(out.r));
			out.g = out.g - ((float) Math.floor(out.g));
			out.b = out.b - ((float) Math.floor(out.b));
			out.a = out.a - ((float) Math.floor(out.a));
			out.mult(b);
		default:
			break;
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeMath(getName(), graph);
	}

}
