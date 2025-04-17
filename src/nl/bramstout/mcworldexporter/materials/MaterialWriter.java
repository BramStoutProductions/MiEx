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

package nl.bramstout.mcworldexporter.materials;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialTemplate;

public abstract class MaterialWriter {
	
	protected File outputFile;
	
	public MaterialWriter(File outputFile) {
		this.outputFile = outputFile;
	}
	
	public File getOutputFile() {
		return outputFile;
	}
	
	private static class MatKey{
		
		private String texture;
		private Materials.MaterialTemplate template;
		
		public MatKey(String texture, Materials.MaterialTemplate template) {
			this.texture = texture;
			this.template = template;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(texture, template);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof MatKey))
				return false;
			if(((MatKey) obj).template == null)
				return ((MatKey) obj).texture.equals(texture) && ((MatKey) obj).template == template;
			return ((MatKey) obj).texture.equals(texture) && ((MatKey) obj).template.equals(template);
		}
		
	}
	
	private static Map<MatKey, String> mats = new HashMap<MatKey, String>();
	private static Map<String, Integer> matCounters = new HashMap<String, Integer>();
	
	public static void clearCounters() {
		mats.clear();
		matCounters.clear();
	}
	
	public static String getMaterialName(String texture, Materials.MaterialTemplate template, boolean hasBiomeColor) {
		MatKey matkey = new MatKey(texture, template);
		String matName = mats.getOrDefault(matkey, null);
		if(matName == null) {
			synchronized(mats) {
				matName = mats.getOrDefault(matkey, null);
				if(matName != null)
					return "MAT_" + matName;
				
				Integer counter = matCounters.getOrDefault(texture, null);
				
				if(counter == null) {
					matCounters.put(texture, 1);
					matName = Util.makeSafeName(texture) + (hasBiomeColor ? "_BIOME" : "");
				}else {
					matCounters.put(texture, counter+1);
					matName = Util.makeSafeName(texture) + "_" + counter.toString() + (hasBiomeColor ? "_BIOME" : "");
				}
				
				if(Config.maxMaterialNameLength > 0 && (matName.length()+4) >= Config.maxMaterialNameLength) {
					int prefix = 4;
					String hashStr = Integer.toHexString(matName.hashCode());
					int hashChars = Math.min(hashStr.length(), Config.maxMaterialNameLength / 2);
					int keepChars = Config.maxMaterialNameLength - prefix - hashChars;
					
					matName = matName.substring(matName.length()-keepChars) + hashStr.substring(0, hashChars);
				}
				
				mats.put(matkey, matName);
			}
		}
		
		return "MAT_" + matName;
	}
	
	public abstract void writeMaterial(MaterialTemplate material, String texture, boolean hasBiomeColor,
			String parentPrim, String sharedPrims) throws IOException;
	
	public abstract void writeSharedNodes(String parentPrim) throws IOException;
	
	public abstract void open() throws IOException;
	
	public abstract void close() throws IOException;
	
	public abstract boolean hasWrittenAnything();
	
	public abstract String getUSDAssetPath();
	
}
