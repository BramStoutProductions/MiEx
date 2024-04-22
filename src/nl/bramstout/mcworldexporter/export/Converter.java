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

package nl.bramstout.mcworldexporter.export;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.export.usd.USDConverter;

public abstract class Converter {
	
	private static class NoConverter extends Converter{
		public NoConverter(File inputFile, File outputFile) {}
		
		@Override
		public void convert() throws Exception {}
		
		@Override
		public boolean deleteMiExFiles() {
			return false;
		}
	}
	
	private static Map<String, Class<? extends Converter>> converterRegistry = new HashMap<String, Class<? extends Converter>>();
	static {
		converterRegistry.put("usd", USDConverter.class);
		converterRegistry.put("miex", NoConverter.class);
	}
	
	public static String[] getExtensions() {
		String[] extensions = new String[converterRegistry.size()];
		int i = 0;
		for(String extension : converterRegistry.keySet())
			extensions[i++] = extension;
		return extensions;
	}
	
	public static Converter getConverter(String extension, File inputFile, File outputFile) {
		try {
			Class<? extends Converter> classType = converterRegistry.getOrDefault(extension, NoConverter.class);
			Constructor<? extends Converter> constructor = classType.getConstructor(File.class, File.class);
			return constructor.newInstance(inputFile, outputFile);
		}catch(Exception ex) {
			return new NoConverter(inputFile, outputFile);
		}
	}
	
	public abstract void convert() throws Exception;
	
	public abstract boolean deleteMiExFiles();
	
}
