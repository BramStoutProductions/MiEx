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

package nl.bramstout.mcworldexporter.export.usd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel3D;

public class USDWriter {
	
	private File outFile;
	private File usdaFile;
	private BufferedWriter fw;
	private int indent;
	private boolean wroteChildren;
	private Process usdCatProcess;
	
	public USDWriter(File file) throws IOException {
		this.outFile = file;
		this.usdaFile = new File(file.getPath() + "a");
		if(FileUtil.hasUSDCat())
			fw = new BufferedWriter(new FileWriter(usdaFile, Charset.forName("UTF-8")));
		else
			fw = new BufferedWriter(new FileWriter(outFile, Charset.forName("UTF-8")));
		indent = 0;
		wroteChildren = false;
		usdCatProcess = null;
		
		fw.write("#usda 1.0\n");
	}
	
	private static ConcurrentLinkedDeque<USDWriter> finalCleanupList = new ConcurrentLinkedDeque<USDWriter>();
	
	public static void finalCleanup() {
		Iterator<USDWriter> iter = finalCleanupList.iterator();
		while(iter.hasNext()) {
			USDWriter writer = iter.next();
			writer._finalCleanup();
		}
		finalCleanupList.clear();
	}
	
	private void _finalCleanup() {
		try {
			int returnCode = usdCatProcess.waitFor();
			String line = null;
			boolean hasError = false;
			if(returnCode != 0)
				hasError = true;
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(usdCatProcess.getInputStream()));
			while((line = reader.readLine()) != null) {
				hasError = true;
				System.out.println(line);
			}
			
			reader = new BufferedReader(new InputStreamReader(usdCatProcess.getErrorStream()));
			while((line = reader.readLine()) != null) {
				hasError = true;
				System.out.println(line);
			}
			
			if(!hasError)
				usdaFile.delete();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void close(boolean delete) throws IOException{
		fw.close();
		fw = null;
		
		if(delete) {
			if(FileUtil.hasUSDCat())
				usdaFile.delete();
			else
				outFile.delete();
			return;
		}
			
		
		if(FileUtil.hasUSDCat()) {
			String usdCatExe = FileUtil.getUSDCatExe();
			
			// Convert from ASCII to Crate
			ProcessBuilder builder = new ProcessBuilder(new File(usdCatExe).getCanonicalPath(), usdaFile.getCanonicalPath(), 
														"--out", outFile.getCanonicalPath(), "--usdFormat", "usdc");
			builder.directory(new File(usdCatExe).getParentFile());
			usdCatProcess = builder.start();
			finalCleanupList.add(this);
		}
	}
	
	
	private static String indentString = "    ";
	private String getIndent() {
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < indent; ++i)
			buffer.append(indentString);
		return buffer.toString();
	}
	
	public void beginMetaData() throws IOException {
		fw.write("(\n");
		indent++;
	}
	
	public void endMetaData() throws IOException{
		indent--;
		fw.write(getIndent() + ")\n");
	}
	
	public void writeMetaData(String name, String value) throws IOException {
		fw.write(getIndent() + name + " = " + value + "\n");
	}
	
	public void writeMetaDataString(String name, String value) throws IOException {
		fw.write(getIndent() + name + " = \"" + value + "\"\n");
	}
	
	public void writeMetaDataInt(String name, int value) throws IOException {
		fw.write(getIndent() + name + " = " + value + "\n");
	}
	
	public void writeMetaDataFloat(String name, float value) throws IOException {
		fw.write(getIndent() + name + " = " + value + "\n");
	}
	
	public void writeMetaDataBoolean(String name, boolean value) throws IOException {
		fw.write(getIndent() + name + " = " + (value ? "true" : "false") + "\n");
	}
	
	public void writeMetaDataStringArray(String name, String[] value) throws IOException{
		fw.write(getIndent() + name);
		fw.write(" = [");
		for(int i = 0; i < value.length - 1; ++i)
			fw.write("\"" + value[i] + "\",");
		if(value.length > 0)
			fw.write("\"" + value[value.length - 1] + "\"");
		fw.write("]\n");
	}
	
	public void writeMetaData(String name) throws IOException{
		fw.write(getIndent() + name);
	}
	
	public void beginDict() throws IOException{
		fw.write(" = {\n");
		indent++;
	}
	
	public void endDict() throws IOException{
		indent--;
		fw.write("\n" + getIndent() + "}\n");
	}
	
	public void writePayload(String path, boolean append) throws IOException {
		fw.write(getIndent() + (append ? "append " : "") + "payload = @" + path + "@\n");
	}
	
	public void writeReference(String path) throws IOException {
		if(path.startsWith("@"))
			fw.write(getIndent() +"references = " + path + "\n");
		else
			fw.write(getIndent() +"references = @" + path + "@\n");
	}
	
	public void writeReferences(List<String> paths) throws IOException {
		fw.write(getIndent() +"references = [\n");
		indent++;
		for(int i = 0; i < paths.size() - 1; ++i) {
			if(paths.get(i).startsWith("@"))
				fw.write(getIndent() + paths.get(i) + ",\n");
			else
				fw.write(getIndent() + "@" + paths.get(i) + "@,\n");
		}
		if(!paths.isEmpty()) {
			if(paths.get(paths.size()-1).startsWith("@"))
				fw.write(getIndent() + paths.get(paths.size()-1) + "\n");
			else
				fw.write(getIndent() + "@" + paths.get(paths.size()-1) + "@\n");
		}
		indent--;
		fw.write(getIndent() + "]\n");
	}
	
	public void writeInherit(String path) throws IOException {
		fw.write(getIndent() +"inherits = <" + path + ">\n");
	}
	
	public void writeVariantSets(String name) throws IOException{
		fw.write(getIndent() + "append variantSets = \"" + name + "\"\n");
	}
	
	public void beginDef(String type, String name) throws IOException{
		fw.write("\n" + getIndent() + "def " + type + " \"" + name + "\"");
		wroteChildren = false;
	}
	
	public void endDef() throws IOException {
		if(!wroteChildren)
			fw.write("\n" + getIndent() + "{\n" + getIndent() + "}\n");
	}
	
	public void beginOver(String name) throws IOException{
		fw.write("\n" + getIndent() + "over \"" + name + "\"");
		wroteChildren = false;
	}
	
	public void endOver() throws IOException{
		if(!wroteChildren)
			fw.write("\n" + getIndent() + "{\n" + getIndent() + "}\n");
	}
	
	public void beginClass(String type, String name) throws IOException{
		fw.write("\n" + getIndent() + "class " + type + " \"" + name + "\"");
		wroteChildren = false;
	}
	
	public void endClass() throws IOException {
		if(!wroteChildren)
			fw.write("\n" + getIndent() + "{\n" + getIndent() + "}\n");
	}
	
	public void beginChildren() throws IOException{
		wroteChildren = true;
		fw.write("\n" + getIndent() + "{");
		indent++;
	}
	
	public void endChildren() throws IOException{
		indent--;
		fw.write("\n" + getIndent() + "}\n");
		wroteChildren = true;
	}
	
	public void beginVariantSet(String name) throws IOException{
		fw.write("\n" + getIndent() + "variantSet \"" + name + "\" = {");
		indent++;
	}
	
	public void endVariantSet() throws IOException{
		indent--;
		fw.write("\n" + getIndent() + "}\n");
	}
	
	public void beginVariant(String name) throws IOException{
		fw.write("\n" + getIndent() + "\"" + name + "\" {");
		indent++;
	}
	
	public void endVariant() throws IOException{
		indent--;
		fw.write("\n" + getIndent() + "}\n");
	}
	
	public void writeAttributeName(String type, String name, boolean isUniform) throws IOException{
		fw.write("\n" + getIndent() + (isUniform ? "uniform " : "") + type + " " + name);
	}
	
	public void writeAttributeConnection(String primPath) throws IOException{
		fw.write(".connect = <" + primPath + ">");
	}
	
	public void writeAttributeValue(String value) throws IOException{
		fw.write(" = " + value);
	}
	
	public void writeAttributeValueString(String value) throws IOException{
		fw.write(" = \"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"");
	}
	
	public void writeAttributeValuePath(String value) throws IOException{
		fw.write(" = @" + value.replace("\"", "\\\"").replace("\n", "\\n") + "@");
	}
	
	public void writeAttributeValueInt(int value) throws IOException{
		fw.write(" = " + Integer.toString(value));
	}
	
	public void writeAttributeValueFloat(float value) throws IOException{
		fw.write(" = " + Float.toString(value));
	}
	
	public void writeAttributeValueBoolean(boolean value) throws IOException{
		fw.write(" = " + (value ? "true" : "false"));
	}
	
	public void writeAttributeValuePoint3f(float x, float y, float z) throws IOException{
		fw.write(" = (" + x + "," + y + "," + z + ")");
	}
	
	public void writeAttributeValueAnimation(AnimationChannel value, float timeScale) throws IOException{
		fw.write(" = {");
		for(int i = 0; i < value.getKeyframes().size() - 1; ++i)
			fw.write(value.getKeyframes().get(i).time * timeScale + ":" + value.getKeyframes().get(i).value + ",");
		if(value.getKeyframes().size() > 0)
			fw.write(value.getKeyframes().get(value.getKeyframes().size()-1).time * timeScale + ":" + 
						value.getKeyframes().get(value.getKeyframes().size()-1).value);
		fw.write("}");
	}
	
	public void writeAttributeValueAnimation3D(AnimationChannel3D value, float timeScale, float scaleX, float scaleY, float scaleZ) throws IOException{
		fw.write(" = {");
		for(int i = 0; i < value.getKeyframes().size() - 1; ++i)
			fw.write(value.getKeyframes().get(i).time * timeScale + ": (" + value.getKeyframes().get(i).valueX * scaleX + "," + 
						value.getKeyframes().get(i).valueY * scaleY + "," + value.getKeyframes().get(i).valueZ * scaleZ + "),");
		if(value.getKeyframes().size() > 0)
			fw.write(value.getKeyframes().get(value.getKeyframes().size()-1).time * timeScale + ": (" + 
						value.getKeyframes().get(value.getKeyframes().size()-1).valueX * scaleX + "," + 
						value.getKeyframes().get(value.getKeyframes().size()-1).valueY * scaleY + "," + 
						value.getKeyframes().get(value.getKeyframes().size()-1).valueZ * scaleZ + ")");
		fw.write("}");
	}
	
	public void writeAttributeValueStringArray(String[] value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.length - 1; ++i)
			fw.write("\"" + value[i] + "\",");
		if(value.length > 0)
			fw.write("\"" + value[value.length - 1] + "\"");
		fw.write("]");
	}
	
	public void writeAttributeValueStringArray(List<String> value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.size() - 1; ++i)
			fw.write("\"" + value.get(i) + "\",");
		if(value.size() > 0)
			fw.write("\"" + value.get(value.size() - 1) + "\"");
		fw.write("]");
	}
	
	public void writeAttributeValueIntArray(int[] value) throws IOException{
		writeAttributeValueIntArray(value, value.length);
	}
	
	public void writeAttributeValueIntArray(int[] value, int count) throws IOException{
		fw.write(" = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			fw.write(Integer.toString(value[i]));
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueFloatArray(float[] value) throws IOException{
		writeAttributeValueFloatArray(value, value.length);
	}
	
	public void writeAttributeValueFloatArray(float[] value, int count) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < count - 1; ++i)
			fw.write(value[i] + ",");
		if(count > 0)
			fw.write(value[count - 1] + "");
		fw.write("]");
	}
	
	public void writeAttributeValueFloatCompound(float[] value) throws IOException{
		fw.write(" = (");
		for(int i = 0; i < value.length - 1; ++i)
			fw.write(value[i] + ",");
		if(value.length > 0)
			fw.write(value[value.length - 1] + "");
		fw.write(")");
	}
	
	public void writeAttributeValuePoint3fArray(float[] value) throws IOException{
		writeAttributeValuePoint3fArray(value, value.length);
	}
	
	public void writeAttributeValuePoint3fArray(float[] value, int size) throws IOException{
		fw.write(" = [");
		int num = size - 3;
		for(int i = 0; i <= num; i += 3) {
			fw.write("(");
			fw.write(Float.toString(value[i]));
			fw.write(",");
			fw.write(Float.toString(value[i+1]));
			fw.write(",");
			fw.write(Float.toString(value[i+2]));
			if(i == num)
				fw.write(")");
			else
				fw.write("),");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePoint2fArray(float[] value) throws IOException{
		writeAttributeValuePoint2fArray(value, value.length);
	}
	
	public void writeAttributeValuePoint2fArray(float[] value, int size) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < size - 2; i += 2)
			fw.write("(" + value[i] + "," + value[i+1] + "),");
		if(size > 1)
			fw.write("(" + value[size - 2] + "," + value[size - 1] + ")");
		fw.write("]");
	}
	
	public void writeAttributeValuePoint2fArray(float[] valueX, float[] valueY, int size) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < size - 1; i++)
			fw.write("(" + valueX[i] + "," + valueY[i] + "),");
		if(size > 0)
			fw.write("(" + valueX[size - 1] + "," + valueY[size - 1] + ")");
		fw.write("]");
	}
	
	public void writeAttributeValueTimeSamplesFloat(List<Float> timeCodes, List<Float> values) throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		for(int i = 0; i < Math.min(timeCodes.size(), values.size()); ++i) {
			fw.write(getIndent() + timeCodes.get(i).floatValue() + ": " + values.get(i).floatValue() + ",\n");
		}
		indent--;
		fw.write("}");
	}
	
	public void writeAttributeValueTimeSamplesFloatCompound(List<Float> timeCodes, List<Float> values, int compoundLength) throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		for(int i = 0; i < Math.min(timeCodes.size(), values.size()/compoundLength); ++i) {
			fw.write(getIndent() + timeCodes.get(i).floatValue() + ": (");
			for(int j = 0; j < compoundLength - 1; ++j) {
				fw.write(values.get(i*compoundLength + j).floatValue() + ",");
			}
			if(compoundLength > 0)
				fw.write(values.get(i*compoundLength + compoundLength - 1).floatValue() + "");
			fw.write("),\n");
		}
		indent--;
		fw.write("}");
	}
	
}
