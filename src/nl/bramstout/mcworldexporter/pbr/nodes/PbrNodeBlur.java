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

public class PbrNodeBlur extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeFloat radiusX = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeFloat radiusY = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeEnum radiusMode = new PbrAttributeEnum(this, false, false, "pixels", 
															"pixels", "0-1", "everything");
	public PbrAttributeEnum radiusShape = new PbrAttributeEnum(this, false, false, "round",
															"round", "square");
	public PbrAttributeEnum kernel = new PbrAttributeEnum(this, false, false, "box", 
															"box", "gaussian", "min", "max", "median", "standardDeviation");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat",
															"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeBlur(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == radiusX || attr == radiusY || attr == radiusMode || 
				attr == radiusShape || attr == kernel || attr == boundary)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage inImg = input.getImageValue(context);
			PbrImage outImg = inImg.copy();
			if(outImg instanceof PbrImageConstant) {
				output.setValue(outImg, context);
				return;
			}
			
			int kernel = this.kernel.getIndexValue(context);
			Boundary boundary = Boundary.REPEAT;
			int boundaryIndex = this.boundary.getIndexValue(context);
			if(boundaryIndex == 0)
				boundary = Boundary.EMPTY;
			else if(boundaryIndex == 1)
				boundary = Boundary.CLIP;
			else if(boundaryIndex == 2)
				boundary = Boundary.REPEAT;
			
			int radiusMode = this.radiusMode.getIndexValue(context);
			int radiusShape = this.radiusShape.getIndexValue(context);
			float radiusX = this.radiusX.getFloatValue(context);
			float radiusY = this.radiusY.getFloatValue(context);
			if(radiusMode == 1) {
				radiusX *= (float) outImg.getWidth();
				radiusY *= (float) outImg.getWidth();
			}
			
			int radiusXI = (int) Math.ceil(radiusX);
			int radiusYI = (int) Math.ceil(radiusY);
			int minXOffset = -radiusXI;
			int minYOffset = -radiusYI;
			int maxXOffset = radiusXI + 1;
			int maxYOffset = radiusYI + 1;
			
			int minX = 0;
			int minY = 0;
			int maxX = 0;
			int maxY = 0;
			RGBA rgba = new RGBA();
			
			if(radiusMode == 2) {
				int i = outImg.getWidth() / 2;
				int j = outImg.getHeight() / 2;
				maxX = outImg.getWidth();
				maxY = outImg.getHeight();
				radiusShape = 2;
				switch(kernel) {
				case 0:
					boxBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				case 1:
					gaussianBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				case 2:
					minBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				case 3:
					maxBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				case 4:
					medianBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				case 5:
					standardDeviationBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
					break;
				default:
					break;
				}
				outImg = new PbrImageConstant(rgba);
			}else {
				for(int j = 0; j < outImg.getHeight(); ++j) {
					for(int i = 0; i < outImg.getWidth(); ++i){
						minX = i + minXOffset;
						minY = j + minYOffset;
						maxX = i + maxXOffset;
						maxY = j + maxYOffset;
						
						switch(kernel) {
						case 0:
							boxBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						case 1:
							gaussianBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						case 2:
							minBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						case 3:
							maxBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						case 4:
							medianBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						case 5:
							standardDeviationBlur(inImg, rgba, i, j, minX, minY, maxX, maxY, radiusX, radiusY, radiusShape, boundary);
							break;
						default:
							break;
						}
						
						outImg.write(i, j, boundary, rgba);
					}
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	private void boxBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY, 
						float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		rgba.set(0f);
		RGBA rgba2 = new RGBA();
		float totalWeight = 0f;
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					rgba2.premultiply();
					rgba.add(rgba2);
					totalWeight += 1f;
				}
			}
		}
		rgba.div(totalWeight);
		rgba.unpremultiply();
	}
	
	private void gaussianBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY, 
						float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		rgba.set(0f);
		RGBA rgba2 = new RGBA();
		float totalWeight = 0f;
		float weight = 0f;
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					weight = (float) Math.exp(-2f * Math.E * (radius * radius));
					img.sample(ii, jj, boundary, rgba2);
					rgba2.premultiply();
					rgba2.mult(weight);
					rgba.add(rgba2);
					totalWeight += weight;
				}
			}
		}
		rgba.div(totalWeight);
		rgba.unpremultiply();
	}
	
	private void minBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY, 
						float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		rgba.set(Float.MAX_VALUE);
		RGBA rgba2 = new RGBA();
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					if(rgba2.a >= 0.000001f) {
						rgba.r = Math.min(rgba.r, rgba2.r);
						rgba.g = Math.min(rgba.g, rgba2.g);
						rgba.b = Math.min(rgba.b, rgba2.b);
					}
					rgba.a = Math.min(rgba.a, rgba2.a);
				}
			}
		}
	}
	
	private void maxBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY, 
						float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		rgba.set(Float.MIN_VALUE);
		RGBA rgba2 = new RGBA();
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					if(rgba2.a >= 0.000001f) {
						rgba.r = Math.max(rgba.r, rgba2.r);
						rgba.g = Math.max(rgba.g, rgba2.g);
						rgba.b = Math.max(rgba.b, rgba2.b);
					}
					rgba.a = Math.max(rgba.a, rgba2.a);
				}
			}
		}
	}
	
	private void medianBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY,
						float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		RGBA rgba2 = new RGBA();
		float[] rList = new float[(maxX - minX) * (maxY - minY)];
		float[] gList = new float[(maxX - minX) * (maxY - minY)];
		float[] bList = new float[(maxX - minX) * (maxY - minY)];
		float[] aList = new float[(maxX - minX) * (maxY - minY)];
		int listIndexRGB = 0;
		int listIndexA = 0;
		
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					if(rgba2.a >= 0.000001f) {
						rList[listIndexRGB] = rgba2.r;
						gList[listIndexRGB] = rgba2.g;
						bList[listIndexRGB] = rgba2.b;
						listIndexRGB++;
					}
					aList[listIndexA] = rgba2.a;
					listIndexA++;
				}
			}
		}
		rgba.set(0f);
		if(listIndexRGB > 0) {
			Arrays.sort(rList, 0, listIndexRGB);
			Arrays.sort(gList, 0, listIndexRGB);
			Arrays.sort(bList, 0, listIndexRGB);
			rgba.r = rList[listIndexRGB/2];
			rgba.g = gList[listIndexRGB/2];
			rgba.b = bList[listIndexRGB/2];
		}
		if(listIndexA > 0) {
			Arrays.sort(aList, 0, listIndexA);
			rgba.a = aList[listIndexA/2];
		}
	}
	
	private void standardDeviationBlur(PbrImage img, RGBA rgba, int i, int j, int minX, int minY, int maxX, int maxY,
							float radiusX, float radiusY, int radiusShape, Boundary boundary) {
		RGBA average = new RGBA();
		RGBA rgba2 = new RGBA();
		float totalWeightRGB = 0f;
		float totalWeightA = 0f;
		float radius = 0f;
		int oi = 0;
		int oj = 0;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					if(rgba2.a <= 0.00001f) {
						rgba2.r = 0f;
						rgba2.g = 0f;
						rgba2.b = 0f;
					}else {
						totalWeightRGB += 1f;
					}
					average.add(rgba2);
					totalWeightA += 1f;
				}
			}
		}
		average.div(totalWeightRGB, totalWeightA);
		
		totalWeightRGB = 0f;
		totalWeightA = 0f;
		rgba.set(0f);
		float alpha = 0f;
		for(int jj = minY; jj < maxY; ++jj) {
			oj = jj - j;
			for(int ii = minX; ii < maxX; ++ii) {
				oi = ii - i;
				radius = calcRadius(oi, oj, radiusX, radiusY, radiusShape);
				if(radius <= 1f) {
					img.sample(ii, jj, boundary, rgba2);
					alpha = rgba2.a;
					rgba2.sub(average).pow(2f);
					if(alpha <= 0.00001f) {
						rgba2.r = 0f;
						rgba2.g = 0f;
						rgba2.b = 0f;
					}else {
						totalWeightRGB += 1f;
					}
					rgba.add(rgba2);
					totalWeightA += 1f;
				}
			}
		}
		rgba.div(Math.max(totalWeightRGB-1f, 1f), Math.max(totalWeightA-1f, 1f));
		rgba.pow(0.5f);
	}
	
	private float calcRadius(int oi, int oj, float radiusX, float radiusY, int radiusShape) {
		if(radiusShape == 2)
			return 0f;
		if(radiusShape == 1) {
			return Math.max(
					Math.abs(((float) (oi)) / Math.max(radiusX, 0.1f)), 
					Math.abs(((float) (oj)) / Math.max(radiusY, 0.1f)));
		}
		return (float) Math.sqrt( (( (float) (oi * oi)) / Math.max(radiusX, 0.1f)) + 
				(( (float) (oj * oj)) / Math.max(radiusY, 0.1f)) );
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeBlur(getName(), graph);
	}

}
