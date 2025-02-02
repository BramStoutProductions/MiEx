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

package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class ModelHandlerCross extends ModelHandler{

	@Override
	public void getGeometry(Model model) {
		float u0 = 0;
		float v0 = 0;
		float uvWidth = 16;
		float uvHeight = 16;
		float[] uvs = new float[] {
				u0, v0,
				u0 + uvWidth, v0 + uvHeight
		};
		
		// North
		float[] points = new float[] {
				-8f,  0f, -8f,
				 8f, 16f,  8f
		};
		model.addFace(points, uvs, Direction.NORTH, "#north");
		
		// South
		points = new float[] {
				-8f,  0f,  8f,
				 8f, 16f, -8f
		};
		model.addFace(points, uvs, Direction.SOUTH, "#south");
		
		if(!model.isDoubleSided()) {
			// East
			points = new float[] {
					-8f,  0f,  8f,
					 8f, 16f,  -8f
			};
			model.addFace(points, uvs, Direction.EAST, "#east");
			
			// West
			points = new float[] {
					-8f,  0f, -8f,
					 8f, 16f,  8f
			};
			model.addFace(points, uvs, Direction.WEST, "#west");
		}
	}
	
}
