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

public class PbrNodeMatrix extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA outR = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 0f, 0f, 0f));
	public PbrAttributeRGBA outG = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 1f, 0f, 0f));
	public PbrAttributeRGBA outB = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 1f, 0f));
	public PbrAttributeRGBA outA = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 1f));
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeMatrix(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == outR || attr == outG || attr == outB || attr == outA)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			RGBA outR = this.outR.getRGBAValue(context);
			RGBA outG = this.outG.getRGBAValue(context);
			RGBA outB = this.outB.getRGBAValue(context);
			RGBA outA = this.outA.getRGBAValue(context);
			
			PbrImage outImg = input.getImageValue(context).copy();
			
			RGBA rgba = new RGBA();
			RGBA rgba2 = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					rgba2.r = rgba.r * outR.r + rgba.g * outR.g + rgba.b * outR.b + rgba.a * outR.a;
					rgba2.g = rgba.r * outG.r + rgba.g * outG.g + rgba.b * outG.b + rgba.a * outG.a;
					rgba2.b = rgba.r * outB.r + rgba.g * outB.g + rgba.b * outB.b + rgba.a * outB.a;
					rgba2.a = rgba.r * outA.r + rgba.g * outA.g + rgba.b * outA.b + rgba.a * outA.a;
					outImg.write(i, j, Boundary.EMPTY, rgba2);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeMatrix(getName(), graph);
	}

}
