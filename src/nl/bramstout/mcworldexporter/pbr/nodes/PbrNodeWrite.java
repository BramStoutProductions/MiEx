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

import java.io.File;

import nl.bramstout.mcworldexporter.image.ImageWriter;
import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeWrite extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeString imageName = new PbrAttributeString(this, false, false, "@texture@_pbr");
	public PbrAttributeBoolean applyGamma = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeBoolean isTemporary = new PbrAttributeBoolean(this, false, false, false);
	
	public PbrNodeWrite(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {		
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == null) {
			PbrImage img = input.getImageValue(context);
			String imageName = this.imageName.getStringValue(context);
			if(imageName.equals("@texture@") && context.saveToResourcePack == null)
				throw new RuntimeException("Cannot overwrite current texture");
			File file = context.getTexture(imageName, true, true);
			file.getParentFile().mkdirs();
			boolean applyGamma = this.applyGamma.getBooleanValue(context);
			if(applyGamma) {
				img = img.copy();
				RGBA rgba = new RGBA();
				for(int j = 0; j < img.getHeight(); ++j) {
					for(int i = 0; i < img.getWidth(); ++i) {
						img.sample(i, j, Boundary.EMPTY, rgba);
						rgba.pow(1f/2.2f, 1f);
						img.write(i, j, Boundary.EMPTY, rgba);
					}
				}
			}
			ImageWriter.writeImage(file, img);
			
			boolean isTemporary = this.isTemporary.getBooleanValue(context);
			if(isTemporary)
				context.temporaryFiles.add(file);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeWrite(getName(), graph);
	}

}
