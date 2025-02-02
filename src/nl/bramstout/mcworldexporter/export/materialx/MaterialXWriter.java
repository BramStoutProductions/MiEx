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

package nl.bramstout.mcworldexporter.export.materialx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MaterialXWriter {
	
	private File outFile;
	private BufferedWriter fw;
	private int indent;
	private boolean wroteChildren;
	
	public MaterialXWriter(File file) throws IOException {
		this.outFile = file;
		fw = new BufferedWriter(new FileWriter(outFile));
		indent = 0;
		wroteChildren = false;
		
		fw.write("<?xml version=\"1.0\"?>\n");
	}
	
	public void close(boolean delete) throws IOException{
		fw.close();
		fw = null;
		
		if(delete)
			outFile.delete();
	}
	
	private static String indentString = "    ";
	private String getIndent() {
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < indent; ++i)
			buffer.append(indentString);
		return buffer.toString();
	}
	
	public void beginNode(String type) throws IOException{
		fw.write(getIndent() + "<" + type);
		wroteChildren = false;
	}
	
	public void writeAttribute(String name, String value) throws IOException{
		fw.write(" " + name + "=\"" + value + "\"");
	}
	
	public void writeAttributeArray(String name, List<?> values) throws IOException{
		fw.write(" " + name + "=\"");
		if(values.size() > 0) {
			for(int i = 0; i < values.size()-1; ++i) {
				fw.write(String.valueOf(values.get(i)));
				fw.write(", ");
			}
			fw.write(String.valueOf(values.get(values.size()-1)));
		}
		fw.write("\"");
	}
	
	public void beginChildren() throws IOException{
		fw.write(">\n");
		indent++;
	}
	
	public void endChildren() throws IOException{
		indent--;
		wroteChildren = true;
	}
	
	public void endNode(String name) throws IOException{
		if(wroteChildren) {
			fw.write(getIndent() + "</" + name + ">\n");
		}else {
			fw.write("/>\n");
		}
	}
	
}
