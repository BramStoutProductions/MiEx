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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeLUT extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeString lutPath = new PbrAttributeString(this, false, false, "");
	public PbrAttributeBoolean inverse = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeLUT(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == lutPath || attr == inverse)
			output.notifyChange(context);;
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage outImg = input.getImageValue(context).copy();
			String lutPath = this.lutPath.getStringValue(context);
			boolean inverse = this.inverse.getBooleanValue(context);
			
			File lutFile = new File(lutPath);
			if(!lutFile.exists()) {
				lutFile = context.getFile(lutPath, "luts", ".cube", "pbr");
			}
			if(lutFile == null || !lutFile.exists()) {
				lutFile = context.getFile(lutPath, "luts", ".sp1d", "pbr");
			}
			if(lutFile == null ||!lutFile.exists()) {
				lutFile = context.getFile(lutPath, "luts", ".sp3d", "pbr");
			}
			if(lutFile == null ||!lutFile.exists())
				throw new RuntimeException("Could not find lut file: " + lutPath);
			
			LUT lut = getLut(lutFile, inverse);
			RGBA rgba = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					lut.lookup(rgba);
					outImg.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	private static Map<String, LUT> LUT_CACHE = new HashMap<String, LUT>();
	private static Map<String, LUT> INVERTED_LUT_CACHE = new HashMap<String, LUT>();
	
	public static LUT getLut(File file, boolean inverted) {
		if(inverted) {
			synchronized(INVERTED_LUT_CACHE) {
				LUT lut = INVERTED_LUT_CACHE.getOrDefault(file.getAbsolutePath(), null);
				if(lut != null)
					return lut;
				
				lut = getLut(file, false);
				lut = lut.invert();
				INVERTED_LUT_CACHE.put(file.getAbsolutePath(), lut);
				return lut;
			}
		}else {
			synchronized(LUT_CACHE) {
				LUT lut = LUT_CACHE.getOrDefault(file.getAbsolutePath(), null);
				if(lut != null)
					return lut;
				
				LUTReader reader = null;
				for(LUTReader reader2 : LUT_READERS) {
					if(reader2.supportsFile(file)) {
						reader = reader2;
						break;
					}
				}
				if(reader == null)
					throw new RuntimeException("LUT file format not supported: " + file.getPath());
				
				lut = reader.read(file);
				
				LUT_CACHE.put(file.getAbsolutePath(), lut);
				
				return lut;
			}
		}
	}
	
	private static abstract class LUT{
		
		protected String path;
		protected boolean inverted;
		
		public LUT(String path, boolean inverted) {
			this.path = path;
			this.inverted = inverted;
		}
		
		public abstract void lookup(RGBA rgba);
		
		public abstract LUT invert();
		
	}
	
	private static class LUT1D extends LUT{
		
		private float[] table;
		private float min;
		private float max;
		
		public LUT1D(String path, boolean inverted, float[] table, float min, float max) {
			super(path, inverted);
			this.table = table;
			this.min = min;
			this.max = max;
		}
		
		public void lookup(RGBA rgba) {
			rgba.r = lookup(rgba.r);
			rgba.g = lookup(rgba.g);
			rgba.b = lookup(rgba.b);
		}
		
		private float lookup(float val) {
			val -= min;
			val /= (max - min);
			val = Math.min(Math.max(val, 0f), 1f);
			val *= (float) (table.length - 1);
			int index0 = (int) Math.floor(val);
			int index1 = Math.min(index0 + 1, table.length-1);
			float t = val - ((float) index0);
			
			float v0 = table[index0];
			float v1 = table[index1];
			return v0 * (1f - t) + v1 * t;
		}
		
		@Override
		public LUT invert() {
			float[] newTable = new float[table.length];
			float newMin = table[0];
			float newMax = table[table.length-1];
			for(int i = 0; i < table.length; ++i) {
				float val = min + (max - min) * (((float) i) / ((float) (table.length - 1)));
				val = lookup(val);
				newTable[i] = val;
			}
			return new LUT1D(path, !inverted, newTable, newMin, newMax);
		}
		
	}
	
	private static class LUT3D extends LUT{
		
		private RGBA[][][] table;
		private float minR;
		private float minG;
		private float minB;
		private float maxR;
		private float maxG;
		private float maxB;
		
		public LUT3D(String path, boolean inverted, RGBA[][][] table, 
				float minR, float minG, float minB, float maxR, float maxG, float maxB) {
			super(path, inverted);
			this.table = table;
			this.minR = minR;
			this.minG = minG;
			this.minB = minB;
			this.maxR = maxR;
			this.maxG = maxG;
			this.maxB = maxB;
		}
		
		public void lookup(RGBA rgba) {
			float r = remap(rgba.r, minR, maxR, table.length);
			float g = remap(rgba.g, minG, maxG, table[0].length);
			float b = remap(rgba.b, minB, maxB, table[0][0].length);
			
			int ri0 = (int) Math.floor(r);
			int ri1 = Math.min(ri0 + 1, table.length-1);
			float rt = r - ((float) ri0);
			
			int gi0 = (int) Math.floor(g);
			int gi1 = Math.min(gi0 + 1, table[0].length-1);
			float gt = g - ((float) gi0);
			
			int bi0 = (int) Math.floor(b);
			int bi1 = Math.min(bi0 + 1, table[0][0].length-1);
			float bt = b - ((float) bi0);
			
			RGBA v000 = table[ri0][gi0][bi0];
			RGBA v001 = table[ri0][gi0][bi1];
			RGBA v010 = table[ri0][gi1][bi0];
			RGBA v011 = table[ri0][gi1][bi1];
			
			RGBA v100 = table[ri1][gi0][bi0];
			RGBA v101 = table[ri1][gi0][bi1];
			RGBA v110 = table[ri1][gi1][bi0];
			RGBA v111 = table[ri1][gi1][bi1];
			
			RGBA a000 = new RGBA(v000).mix(v001, bt, 0f);
			RGBA a010 = new RGBA(v010).mix(v011, bt, 0f);
			RGBA a100 = new RGBA(v100).mix(v101, bt, 0f);
			RGBA a110 = new RGBA(v110).mix(v111, bt, 0f);
			a000.mix(a010, gt, 0f);
			a100.mix(a110, gt, 0f);
			a000.mix(a100, rt, 0f);
			rgba.set(a000);
		}
		
		public float remap(float v, float min, float max, int length) {
			v -= min;
			v /= (max - min);
			v = Math.min(Math.max(v, 0f), 1f);
			v *= (float) (length - 1);
			return v;
		}
		
		@Override
		public LUT invert() {
			int rSize = table.length;
			int gSize = table[0].length;
			int bSize = table[0][0].length;
			RGBA[][][] newTable = new RGBA[rSize][gSize][bSize];
			RGBA min = new RGBA(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
			RGBA max = new RGBA(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
			for(int i = 0; i < rSize; ++i) {
				for(int j = 0; j < gSize; ++j) {
					for(int k = 0; k < bSize; ++k) {
						min.min(table[i][j][k]);
						max.max(table[i][j][k]);
					}
				}
			}
			
			RGBA rgba = new RGBA();
			for(int i = 0; i < rSize; ++i) {
				for(int j = 0; j < gSize; ++j) {
					for(int k = 0; k < bSize; ++k) {
						rgba.r = min.r + (max.r - min.r) * (((float) i) / ((float) rSize));
						rgba.g = min.g + (max.g - min.g) * (((float) j) / ((float) gSize));
						rgba.b = min.b + (max.b - min.b) * (((float) k) / ((float) bSize));
						rgba.a = 1f;
						lookup(rgba);
						newTable[i][j][k] = new RGBA(rgba);
					}
				}
			}
			return new LUT3D(path, !inverted, newTable, min.r, min.g, min.b, max.r, max.g, max.b);
		}
		
	}
	
	private static LUTReader[] LUT_READERS = new LUTReader[] {
			new LUTReaderCube(),
			new LUTReaderSPI1D(),
			new LUTReaderSPI3D()
	}; 
	
	private static abstract class LUTReader{
		
		public abstract LUT read(File file);
		
		public abstract boolean supportsFile(File file);
		
	}
	
	private static class LUTReaderSPI1D extends LUTReader{
		
		@Override
		public LUT read(File file) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				float min = 0f;
				float max = 0f;
				int size = 0;
				
				int i = 0;
				float[] table = null;
				while((line = reader.readLine()) != null) {
					line = line.trim().toLowerCase();
					if(line.startsWith("from")) {
						String[] tokens = line.split(" ");
						int j = 0;
						for(String str : tokens) {
							if(str.isEmpty())
								continue;
							char firstChar = str.charAt(0);
							if(!Character.isDigit(firstChar) && firstChar != '-')
								continue;
							float v = Float.parseFloat(str);
							if(j == 0)
								min = v;
							else if(j == 1)
								max = v;
							j++;
						}
					}else if(line.startsWith("length")) {
						String[] tokens = line.split(" ");
						for(String str : tokens) {
							if(str.isEmpty())
								continue;
							char firstChar = str.charAt(0);
							if(!Character.isDigit(firstChar))
								continue;
							size = Integer.parseInt(str);
							break;
						}
						table = new float[size];
					}else if(!line.isEmpty()) {
						char firstChar = line.charAt(0);
						if(!Character.isDigit(firstChar) && firstChar != '-')
							continue;
						
						float val = Float.parseFloat(line);
						table[i] = val;
						i++;
						if(i == size)
							break;
					}
				}
				reader.close();
				
				return new LUT1D(file.getAbsolutePath(), false, table, min, max);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
		
		@Override
		public boolean supportsFile(File file) {
			return file.getName().toLowerCase().endsWith(".spi1d");
		}
		
	}
	
	private static class LUTReaderSPI3D extends LUTReader{
		
		@Override
		public LUT read(File file) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				
				int size = 0;
				float min = 0f;
				float max = 1f;
				
				int i = 0;
				int lineCounter = 0;
				
				RGBA[][][] table = null;
				
				while((line = reader.readLine()) != null) {
					line = line.trim();
					if(lineCounter == 0) {
						if(line.equals("3 3"))
							lineCounter = 1;
					} else if(lineCounter == 1) {
						if(line.isEmpty())
							continue;
						if(!Character.isDigit(line.charAt(0)))
							continue;
						String[] tokens = line.split(" ");
						size = Integer.parseInt(tokens[0]);
						lineCounter = 2;
						table = new RGBA[size][size][size];
					}else if(lineCounter == 2) {
						if(line.isEmpty())
							continue;
						if(!Character.isDigit(line.charAt(0)))
							continue;
						String[] tokens = line.split(" ");
						int j = 0;
						int ri = 0;
						int gi = 0;
						int bi = 0;
						RGBA rgba = new RGBA();
						for(String str : tokens) {
							if(j < 3) {
								int val = Integer.parseInt(str);
								if(j == 0)
									ri = val;
								else if(j == 1)
									gi = val;
								else if(j == 2)
									bi = val;
								j++;
							}else {
								float val = Float.parseFloat(str);
								if(j == 3)
									rgba.r = val;
								else if(j == 4)
									rgba.g = val;
								else if(j == 5)
									rgba.b = val;
								j++;
							}
						}
						
						table[ri][gi][bi] = rgba;
						i++;
						if(i == size*size*size)
							break;
					}
				}
				reader.close();
				
				return new LUT3D(file.getAbsolutePath(), false, table, min, min, min, max, max, max);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
		
		@Override
		public boolean supportsFile(File file) {
			return file.getName().toLowerCase().endsWith(".spi3d");
		}
		
	}
	
	private static class LUTReaderCube extends LUTReader{
		
		public LUT read(File file) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				
				boolean is3D = false;
				int size = 0;
				float min = 0f;
				float max = 1f;
				
				int i = 0;
				
				Object table = null;
				while((line = reader.readLine()) != null) {
					if(line.startsWith("LUT_1D_SIZE")) {
						is3D = false;
						size = Integer.parseInt(line.substring(12).trim());
						table = new float[size];
					}if(line.startsWith("LUT_3D_SIZE")) {
						is3D = true;
						size = Integer.parseInt(line.substring(12).trim());
						table = new RGBA[size][size][size];
					}else if(line.startsWith("LUT_1D_INPUT_RANGE") || line.startsWith("LUT_3D_INPUT_RANGE")) {
						String[] tokens = line.split(" ");
						boolean hasMin = false;
						for(String str : tokens) {
							if(str.startsWith("LUT"))
								continue;
							if(str.isEmpty())
								continue;
							if(!hasMin) {
								min = Float.parseFloat(str);
								hasMin = true;
							}else {
								max = Float.parseFloat(str);
								break;
							}
						}
					}else {
						if(line.trim().isEmpty())
							continue;
						char firstChar = line.charAt(0);
						if(!Character.isDigit(firstChar) && firstChar != '-')
							continue;
						String[] tokens = line.split(" ");
						
						if(is3D) {
							float v = 0f;
							float r = 0f;
							float g = 0f;
							float b = 0f;
							int j = 0;
							for(String str : tokens) {
								firstChar = str.charAt(0);
								if(!Character.isDigit(firstChar) && firstChar != '-')
									continue;
								v = Float.parseFloat(str);
								if(j == 0)
									r = v;
								else if(j == 1)
									g = v;
								else if(j == 2)
									b = v;
								j++;
							}
							int ri = i % size;
							int gbi = i / size;
							int gi = gbi % size;
							int bi = gbi / size;
							((RGBA[][][])table)[ri][gi][bi] = new RGBA(r, g, b, 1f);
							i++;
							if(i == size * size * size)
								break;
						}else {
							float val = Float.parseFloat(tokens[0]);
							((float[]) table)[i] = val;
							i++;
							if(i == size)
								break;
						}
					}
				}
				reader.close();
				
				if(is3D) {
					return new LUT3D(file.getAbsolutePath(), false, (RGBA[][][]) table, min, min, min, max, max, max);
				}else {
					return new LUT1D(file.getAbsolutePath(), false, (float[]) table, min, max);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
		
		public boolean supportsFile(File file) {
			return file.getName().toLowerCase().endsWith(".cube");
		}
		
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeLUT(getName(), graph);
	}

}
