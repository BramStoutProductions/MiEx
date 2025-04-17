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

package nl.bramstout.mcworldexporter.resourcepack.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.export.FloatArray;
import nl.bramstout.mcworldexporter.export.IntArray;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class ModelHandlerObj extends ModelHandler{
	
	private FloatArray vertices;
	private FloatArray uvs;
	private IntArray vertIndices;
	private IntArray uvIndices;
	private List<String> textures;
	private IntArray textureIndices;
	private IntArray tintIndices;
	
	public ModelHandlerObj(File file) {
		vertices = new FloatArray();
		uvs = new FloatArray();
		vertIndices = new IntArray();
		uvIndices = new IntArray();
		textures = new ArrayList<String>();
		textureIndices = new IntArray();
		tintIndices = new IntArray();
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			
			int currentTintIndex = -1;
			
			String line = null;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("v ")) {
					String[] tokens = line.split(" ");
					float x = 0.0f;
					float y = 0.0f;
					float z = 0.0f;
					int j = 0;
					for(int i = 0; i < tokens.length; ++i) {
						if(tokens[i].length() <= 0)
							continue;
						if(Character.isDigit(tokens[i].codePointAt(0))) {
							float val = Float.parseFloat(tokens[i]);
							if(j == 0)
								x = val;
							else if(j == 1)
								y = val;
							else if(j == 2)
								z = val;
							j++;
							if(j > 2)
								break;
						}
					}
					vertices.add(x);
					vertices.add(y);
					vertices.add(z);
				}else if(line.startsWith("vt ")) {
					String[] tokens = line.split(" ");
					float u = 0.0f;
					float v = 0.0f;
					int j = 0;
					for(int i = 0; i < tokens.length; ++i) {
						if(tokens[i].length() <= 0)
							continue;
						if(Character.isDigit(tokens[i].codePointAt(0))) {
							float val = Float.parseFloat(tokens[i]);
							if(j == 0)
								u = val;
							else if(j == 1)
								v = val;
							j++;
							if(j > 1)
								break;
						}
					}
					uvs.add(u * 16f);
					uvs.add(v * 16f);
				}else if(line.startsWith("usemtl ")) {
					String texture = line.substring("usemtl ".length());
					if(!texture.isEmpty()) {
						currentTintIndex = -1;
						if(texture.startsWith("[")) {
							int endIndex = texture.indexOf(']');
							if(endIndex >= 0) {
								currentTintIndex = Integer.parseInt(texture.substring(1, endIndex));
								texture = texture.substring(endIndex+1);
							}
						}
						textures.add(texture);
					}
				}else if(line.startsWith("f ")) {
					if(textures.size() <= 0) {
						// No texture selected, so ignore this face.
						continue;
					}
					
					int vert0 = -1;
					int vert1 = -1;
					int vert2 = -1;
					int vert3 = -1;
					int uv0 = -1;
					int uv1 = -1;
					int uv2 = -1;
					int uv3 = -1;
					int texture = textures.size() - 1;
					
					String[] tokens = line.split(" ");
					int j = 0;
					for(int i = 0; i < tokens.length; ++i) {
						if(tokens[i].length() <= 0)
							continue;
						if(Character.isDigit(tokens[i].codePointAt(0))) {
							String[] subTokens = tokens[i].split("\\/");
							if(subTokens.length <= 1)
								// We need UVs which is the second number, so if there is no second number then skip
								continue;
							int vert = Integer.parseInt(subTokens[0]) - 1;
							int uv = Integer.parseInt(subTokens[1]) - 1;
							
							// Make sure that it's valid.
							if(vert < 0 || vert >= (vertices.size() / 3))
								continue;
							if(uv < 0 || uv >= (uvs.size() / 2))
								continue;
							
							if(j == 0) {
								vert0 = vert;
								uv0 = uv;
							}else if(j == 1) {
								vert1 = vert;
								uv1 = uv;
							}else if(j == 2) {
								vert2 = vert;
								uv2 = uv;
							}else if(j == 3) {
								vert3 = vert;
								uv3 = uv;
							}
							j++;
							if(j > 3)
								break;
						}
					}
					
					// Invalid
					if(vert0 == -1 || vert1 == -1 || vert2 == -1 || 
							uv0 == -1 || uv1 == -1 || uv2 == -1)
						continue;
					
					// If this face was a triangle, turn it into a quad.
					if(vert3 == -1) {
						float v0x = vertices.get(vert0 * 3);
						float v0y = vertices.get(vert0 * 3 + 1);
						float v0z = vertices.get(vert0 * 3 + 2);
						float v2x = vertices.get(vert2 * 3);
						float v2y = vertices.get(vert2 * 3 + 1);
						float v2z = vertices.get(vert2 * 3 + 2);
						
						float v3x = (v0x + v2x) / 2f;
						float v3y = (v0y + v2y) / 2f;
						float v3z = (v0z + v2z) / 2f;
						
						vert3 = vertices.size() / 3;
						vertices.add(v3x);
						vertices.add(v3y);
						vertices.add(v3z);
					}
					if(uv3 == -1) {
						float uv0u = uvs.get(uv0 * 2);
						float uv0v = uvs.get(uv0 * 2 + 1);
						float uv2u = uvs.get(uv2 * 2);
						float uv2v = uvs.get(uv2 * 2 + 1);
						
						float uv3u = (uv0u + uv2u) / 2f;
						float uv3v = (uv0v + uv2v) / 2f;
						
						uv3 = uvs.size() / 2;
						uvs.add(uv3u);
						uvs.add(uv3v);
					}
					
					// Invalid
					if(vert0 == -1 || vert1 == -1 || vert2 == -1 || vert3 == -1 || 
							uv0 == -1 || uv1 == -1 || uv2 == -1 || uv3 == -1)
						continue;
					
					// Add the face
					vertIndices.add(vert0);
					vertIndices.add(vert1);
					vertIndices.add(vert2);
					vertIndices.add(vert3);
					uvIndices.add(uv0);
					uvIndices.add(uv1);
					uvIndices.add(uv2);
					uvIndices.add(uv3);
					textureIndices.add(texture);
					tintIndices.add(currentTintIndex);
				}
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		try {
			if(reader != null)
				reader.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private Direction calculateDirection(float[] points) {
		float x1 = points[1*3+0] - points[0*3+0];
		float y1 = points[1*3+1] - points[0*3+1];
		float z1 = points[1*3+2] - points[0*3+2];
		float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
		x1 /= length;
		y1 /= length;
		z1 /= length;
		
		float x2 = points[3*3+0] - points[0*3+0];
		float y2 = points[3*3+1] - points[0*3+1];
		float z2 = points[3*3+2] - points[0*3+2];
		length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
		x2 /= length;
		y2 /= length;
		z2 /= length;
		
		float nx = y1 * z2 - z1 * y2;
		float ny = z1 * x2 - x1 * z2;
		float nz = x1 * y2 - y1 * x2;
		
		float anx = Math.abs(nx);
		float any = Math.abs(ny);
		float anz = Math.abs(nz);
		
		if(anx >= any && anx >= anz) {
			return nx >= 0 ? Direction.EAST : Direction.WEST;
		}else if(any >= anz) {
			return ny >= 0 ? Direction.UP : Direction.DOWN;
		}else {
			return nz >= 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}
	
	@Override
	public void getGeometry(Model model) {
		if(vertIndices.size() <= 0)
			return; // No faces, so do nothing
		
		// Add in the textures
		for(int i = 0; i < textures.size(); ++i)
			model.getTextures().put("#tex" + Integer.toString(i), textures.get(i));
		
		// Add in the faces
		model.getFaces().clear();
		int numFaces = vertIndices.size() / 4;
		for(int i = 0; i < numFaces; ++i) {
			int vert0 = vertIndices.get(i * 4) * 3;
			int vert1 = vertIndices.get(i * 4 + 1) * 3;
			int vert2 = vertIndices.get(i * 4 + 2) * 3;
			int vert3 = vertIndices.get(i * 4 + 3) * 3;
			int uv0 = uvIndices.get(i * 4) * 2;
			int uv1 = uvIndices.get(i * 4 + 1) * 2;
			int uv2 = uvIndices.get(i * 4 + 2) * 2;
			int uv3 = uvIndices.get(i * 4 + 3) * 2;
			
			float[] points = new float[] {
					vertices.get(vert0), vertices.get(vert0 + 1), vertices.get(vert0 + 2),
					vertices.get(vert1), vertices.get(vert1 + 1), vertices.get(vert1 + 2),
					vertices.get(vert2), vertices.get(vert2 + 1), vertices.get(vert2 + 2),
					vertices.get(vert3), vertices.get(vert3 + 1), vertices.get(vert3 + 2)
				};
			float[] uvs2 = new float[] {
					uvs.get(uv0), uvs.get(uv0 + 1),
					uvs.get(uv1), uvs.get(uv1 + 1),
					uvs.get(uv2), uvs.get(uv2 + 1),
					uvs.get(uv3), uvs.get(uv3 + 1)
				};
			Direction dir = calculateDirection(points);
			int tintIndex = tintIndices.get(i);
			ModelFace face = new ModelFace(points, uvs2, "#tex" + Integer.toString(textureIndices.get(i)), 
											tintIndex, dir, model.isDoubleSided());
			
			model.getFaces().add(face);
		}
	}

}
