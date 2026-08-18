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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel3D;
import nl.bramstout.mcworldexporter.export.LargeDataOutputStream;

public class USDWriter {
	
	private File outFile;
	private File usdaFile;
	private LargeDataOutputStream fw;
	private int indent;
	private boolean wroteChildren;
	private Process usdCatProcess;
	
	public USDWriter(File file) throws IOException {
		this.outFile = file;
		this.usdaFile = new File(file.getPath() + "a");
		if(FileUtil.hasUSDCat())
			//fw = new BufferedWriter(new FileWriter(usdaFile, Charset.forName("UTF-8")));
			fw = new LargeDataOutputStream(usdaFile);
		else
			//fw = new BufferedWriter(new FileWriter(outFile, Charset.forName("UTF-8")));
			fw = new LargeDataOutputStream(outFile);
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
		fw.write(getIndent());
		fw.write(")\n");
	}
	
	public void writeMetaData(String name, String value) throws IOException {
		fw.write(getIndent());  fw.write(name);  fw.write(" = ");  fw.write(value);  fw.write("\n");
	}
	
	public void writeMetaDataString(String name, String value) throws IOException {
		fw.write(getIndent());  fw.write(name);  fw.write(" = \"");  fw.write(value);  fw.write("\"\n");
	}
	
	public void writeMetaDataInt(String name, int value) throws IOException {
		fw.write(getIndent());  fw.write(name);  fw.write(" = ");  fw.write(Integer.toString(value));  fw.write("\n");
	}
	
	public void writeMetaDataFloat(String name, float value) throws IOException {
		fw.write(getIndent());  fw.write(name);  fw.write(" = ");  fw.write(Float.toString(value));  fw.write("\n");
	}
	
	public void writeMetaDataBoolean(String name, boolean value) throws IOException {
		fw.write(getIndent());  fw.write(name);  fw.write(" = ");  fw.write((value ? "true" : "false"));  fw.write("\n");
	}
	
	public void writeMetaDataStringArray(String name, String[] value) throws IOException{
		fw.write(getIndent());  fw.write(name);
		fw.write(" = [");
		for(int i = 0; i < value.length - 1; ++i) {
			fw.write("\"");  fw.write(value[i]);  fw.write("\",");
		}
		if(value.length > 0) {
			fw.write("\"");  fw.write(value[value.length - 1]);  fw.write("\"");
		}
		fw.write("]\n");
	}
	
	public void writeMetaData(String name) throws IOException{
		fw.write(getIndent());  fw.write(name);
	}
	
	public void beginDict() throws IOException{
		fw.write(" = {\n");
		indent++;
	}
	
	public void endDict() throws IOException{
		indent--;
		fw.write("\n");  fw.write(getIndent());  fw.write("}\n");
	}
	
	public void writePayload(String path, boolean append) throws IOException {
		fw.write(getIndent());  fw.write((append ? "append " : ""));  fw.write("payload = @");  fw.write(path);  fw.write("@\n");
	}
	
	public void writeReference(String path) throws IOException {
		if(path.startsWith("@")) {
			fw.write(getIndent() +"references = ");  fw.write(path);  fw.write("\n");
		}else {
			fw.write(getIndent() +"references = @");  fw.write(path);  fw.write("@\n");
		}
	}
	
	public void writeReferences(List<String> paths) throws IOException {
		fw.write(getIndent() +"references = [\n");
		indent++;
		for(int i = 0; i < paths.size() - 1; ++i) {
			if(paths.get(i).startsWith("@")) {
				fw.write(getIndent());  fw.write(paths.get(i));  fw.write(",\n");
			}else {
				fw.write(getIndent());  fw.write("@");  fw.write(paths.get(i));  fw.write("@,\n");
			}
		}
		if(!paths.isEmpty()) {
			if(paths.get(paths.size()-1).startsWith("@")) {
				fw.write(getIndent());  fw.write(paths.get(paths.size()-1));  fw.write("\n");
			}else {
				fw.write(getIndent());  fw.write("@");  fw.write(paths.get(paths.size()-1));  fw.write("@\n");
			}
		}
		indent--;
		fw.write(getIndent());  fw.write("]\n");
	}
	
	public void writeInherit(String path) throws IOException {
		fw.write(getIndent() +"inherits = <");  fw.write(path);  fw.write(">\n");
	}
	
	public void writeVariantSets(String name) throws IOException{
		fw.write(getIndent());  fw.write("append variantSets = \"");  fw.write(name);  fw.write("\"\n");
	}
	
	public void beginDef(String type, String name) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write("def ");  fw.write(type);  fw.write(" \"");  fw.write(name);  fw.write("\"");
		wroteChildren = false;
	}
	
	public void endDef() throws IOException {
		if(!wroteChildren) {
			fw.write("\n");  fw.write(getIndent());  fw.write("{\n");  fw.write(getIndent());  fw.write("}\n");
		}
	}
	
	public void beginOver(String name) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write("over \"");  fw.write(name);  fw.write("\"");
		wroteChildren = false;
	}
	
	public void endOver() throws IOException{
		if(!wroteChildren) {
			fw.write("\n");  fw.write(getIndent());  fw.write("{\n");  fw.write(getIndent());  fw.write("}\n");
		}
	}
	
	public void beginClass(String type, String name) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write("class ");  fw.write(type);  fw.write(" \"");  fw.write(name);  fw.write("\"");
		wroteChildren = false;
	}
	
	public void endClass() throws IOException {
		if(!wroteChildren) {
			fw.write("\n");  fw.write(getIndent());  fw.write("{\n");  fw.write(getIndent());  fw.write("}\n");
		}
	}
	
	public void beginChildren() throws IOException{
		wroteChildren = true;
		fw.write("\n");  fw.write(getIndent());  fw.write("{");
		indent++;
	}
	
	public void endChildren() throws IOException{
		indent--;
		fw.write("\n");  fw.write(getIndent());  fw.write("}\n");
		wroteChildren = true;
	}
	
	public void beginVariantSet(String name) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write("variantSet \"");  fw.write(name);  fw.write("\" = {");
		indent++;
	}
	
	public void endVariantSet() throws IOException{
		indent--;
		fw.write("\n");  fw.write(getIndent());  fw.write("}\n");
	}
	
	public void beginVariant(String name) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write("\"");  fw.write(name);  fw.write("\" {");
		indent++;
	}
	
	public void endVariant() throws IOException{
		indent--;
		fw.write("\n");  fw.write(getIndent());  fw.write("}\n");
	}
	
	public void writeAttributeName(String type, String name, boolean isUniform) throws IOException{
		fw.write("\n");  fw.write(getIndent());  fw.write((isUniform ? "uniform " : ""));  fw.write(type);  fw.write(" ");  fw.write(name);
	}
	
	public void writeAttributeConnection(String primPath) throws IOException{
		fw.write(".connect = <");  fw.write(primPath);  fw.write(">");
	}
	
	public void writeAttributeValue(String value) throws IOException{
		writeAttributeValue(value, false);
	}
	
	public void writeAttributeValue(String value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " " : " = "));  fw.write(value);
	}
	
	public void writeAttributeValueString(String value) throws IOException{
		writeAttributeValueString(value, false);
	}
	
	public void writeAttributeValueString(String value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " \"" : " = \""));  fw.write(value.replace("\"", "\\\"").replace("\n", "\\n"));  fw.write("\"");
	}
	
	public void writeAttributeValuePrimPath(String value) throws IOException{
		writeAttributeValueString(value, false);
	}
	
	public void writeAttributeValuePrimPath(String value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " <" : " = <"));  fw.write(value.replace("\"", "\\\"").replace("\n", "\\n"));  fw.write(">");
	}
	
	public void writeAttributeValuePath(String value) throws IOException{
		writeAttributeValuePath(value, false);
	}
	
	public void writeAttributeValuePath(String value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " @" : " = @"));  fw.write(value.replace("\"", "\\\"").replace("\n", "\\n"));  fw.write("@");
	}
	
	public void writeAttributeValueInt(int value) throws IOException{
		writeAttributeValueInt(value, false);
	}
	
	public void writeAttributeValueInt(int value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " " : " = "));  fw.write(Integer.toString(value));
	}
	
	public void writeAttributeValueFloat(float value) throws IOException{
		writeAttributeValueFloat(value, false);
	}
	
	public void writeAttributeValueFloat(float value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " " : " = "));  fw.write(Float.toString(value));
	}
	
	public void writeAttributeValueBoolean(boolean value) throws IOException{
		writeAttributeValueBoolean(value, false);
	}
	
	public void writeAttributeValueBoolean(boolean value, boolean noEqual) throws IOException{
		fw.write((noEqual ? " " : " = "));  fw.write((value ? "true" : "false"));
	}
	
	public void writeAttributeValuePoint3f(float x, float y, float z) throws IOException{
		writeAttributeValuePoint3f(x, y, z, false);
	}
	
	public void writeAttributeValuePoint3f(float x, float y, float z, boolean noEqual) throws IOException{
		fw.write((noEqual ? " (" : " = ("));  fw.write(Float.toString(x));  fw.write(",");  fw.write(Float.toString(y));  fw.write(",");  fw.write(Float.toString(z));  fw.write(")");
	}
	
	public void writeAttributeValueAnimation(AnimationChannel value, float timeScale) throws IOException{
		fw.write(" = {");
		for(int i = 0; i < value.getKeyframes().size() - 1; ++i) {
			fw.write(Float.toString(value.getKeyframes().get(i).time * timeScale));  fw.write(":");  fw.write(Float.toString(value.getKeyframes().get(i).value));  fw.write(",");
		}
		if(value.getKeyframes().size() > 0) {
			fw.write(Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).time * timeScale));  fw.write(":");  fw.write(
					Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).value));
		}
		fw.write("}");
	}
	
	public void writeAttributeValueAnimation3D(AnimationChannel3D value, float timeScale, float scaleX, float scaleY, float scaleZ) throws IOException{
		fw.write(" = {");
		for(int i = 0; i < value.getKeyframes().size() - 1; ++i) {
			fw.write(Float.toString(value.getKeyframes().get(i).time * timeScale));  fw.write(": (");  fw.write(Float.toString(value.getKeyframes().get(i).valueX * scaleX));  fw.write(",");  fw.write(
					Float.toString(value.getKeyframes().get(i).valueY * scaleY));  fw.write(",");  fw.write(Float.toString(value.getKeyframes().get(i).valueZ * scaleZ));  fw.write("),");
		}
		if(value.getKeyframes().size() > 0) {
			fw.write(Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).time * timeScale));  fw.write(": (");  fw.write(
					Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).valueX * scaleX));  fw.write(",");  fw.write(
					Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).valueY * scaleY));  fw.write(",");  fw.write(
					Float.toString(value.getKeyframes().get(value.getKeyframes().size()-1).valueZ * scaleZ));  fw.write(")");
		}
		fw.write("}");
	}
	
	public void writeAttributeValueStringArray(String[] value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.length - 1; ++i) {
			fw.write("\"");  fw.write(value[i]);  fw.write("\",");
		}
		if(value.length > 0) {
			fw.write("\"");  fw.write(value[value.length - 1]);  fw.write("\"");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueStringArray(List<String> value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.size() - 1; ++i) {
			fw.write("\"");  fw.write(value.get(i));  fw.write("\",");
		}
		if(value.size() > 0) {
			fw.write("\"");  fw.write(value.get(value.size() - 1));  fw.write("\"");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePrimPathArray(String[] value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.length - 1; ++i) {
			fw.write("<");  fw.write(value[i]);  fw.write(">,");
		}
		if(value.length > 0) {
			fw.write("<");  fw.write(value[value.length - 1]);  fw.write(">");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePrimPathArray(List<String> value) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < value.size() - 1; ++i) {
			fw.write("<");  fw.write(value.get(i));  fw.write(">,");
		}
		if(value.size() > 0) {
			fw.write("<");  fw.write(value.get(value.size() - 1));  fw.write(">");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArray(int[] value) throws IOException{
		writeAttributeValueIntArray(value, value.length);
	}
	
	public void writeAttributeValueIntArray(int[] value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(int[] value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			fw.write(Integer.toString(value[i]));
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArray(byte[] value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(byte[] value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			fw.write(Integer.toString(value[i]));
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArray(short[] value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(short[] value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			fw.write(Integer.toString(value[i]));
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArray(Object value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(Object value, int count, boolean noEqual) throws IOException{
		if(value instanceof byte[])
			writeAttributeValueIntArray((byte[]) value, count, noEqual);
		if(value instanceof short[])
			writeAttributeValueIntArray((short[]) value, count, noEqual);
		if(value instanceof int[])
			writeAttributeValueIntArray((int[]) value, count, noEqual);
	}
	
	public void writeAttributeValueIntArray(int value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(int value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		String valueStr = Integer.toString(value);
		for(int i = 0; i <= num; ++i) {
			fw.write(valueStr);
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArrayRepeat(int[] value, int count, int repeat) throws IOException{
		writeAttributeValueIntArrayRepeat(value, count, false, repeat);
	}
	
	public void writeAttributeValueIntArrayRepeat(int[] value, int count, boolean noEqual, int repeat) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			for(int j = 1; j <= repeat; ++j) {
				fw.write(Integer.toString(value[i]));
				if(j != repeat)
					fw.write(",");
			}
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArrayRepeat(byte[] value, int count, int repeat) throws IOException{
		writeAttributeValueIntArrayRepeat(value, count, false, repeat);
	}
	
	public void writeAttributeValueIntArrayRepeat(byte[] value, int count, boolean noEqual, int repeat) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			for(int j = 1; j <= repeat; ++j) {
				fw.write(Integer.toString(value[i]));
				if(j != repeat)
					fw.write(",");
			}
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArrayRepeat(short[] value, int count, int repeat) throws IOException{
		writeAttributeValueIntArrayRepeat(value, count, false, repeat);
	}
	
	public void writeAttributeValueIntArrayRepeat(short[] value, int count, boolean noEqual, int repeat) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			for(int j = 1; j <= repeat; ++j) {
				fw.write(Integer.toString(value[i]));
				if(i != num && j != repeat)
					fw.write(",");
			}
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueIntArrayRepeat(Object value, int count, int repeat) throws IOException{
		writeAttributeValueIntArrayRepeat(value, count, false, repeat);
	}
	
	public void writeAttributeValueIntArrayRepeat(Object value, int count, boolean noEqual, int repeat) throws IOException{
		if(value instanceof byte[])
			writeAttributeValueIntArrayRepeat((byte[]) value, count, noEqual, repeat);
		if(value instanceof short[])
			writeAttributeValueIntArrayRepeat((short[]) value, count, noEqual, repeat);
		if(value instanceof int[])
			writeAttributeValueIntArrayRepeat((int[]) value, count, noEqual, repeat);
	}

	public void writeAttributeValueIntArray(List<Integer> value) throws IOException{
		writeAttributeValueIntArray(value, value.size());
	}
	
	public void writeAttributeValueIntArray(List<Integer> value, int count) throws IOException{
		writeAttributeValueIntArray(value, count, false);
	}
	
	public void writeAttributeValueIntArray(List<Integer> value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = count - 1;
		for(int i = 0; i <= num; ++i) {
			fw.write(Integer.toString(value.get(i).intValue()));
			if(i != num)
				fw.write(",");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePointNfArray(float[] value, int componentCount) throws IOException{
		writeAttributeValuePointNfArray(value, value.length, componentCount);
	}
	
	public void writeAttributeValuePointNfArray(float[] value, int size, int componentCount) throws IOException{
		writeAttributeValuePointNfArray(value, size, componentCount, false);
	}
	
	public void writeAttributeValuePointNfArray(float[] value, int size, int componentCount, boolean noEqual) throws IOException{
		if(componentCount <= 1) {
			writeAttributeValueFloatArray(value, size, noEqual);
			return;
		}
		fw.write(noEqual ? " [" : " = [");
		int num = size - componentCount;
		for(int i = 0; i <= num; i += componentCount) {
			fw.write("(");
			for(int j = 0; j < componentCount; ++j) {
				if(j > 0)
					fw.write(",");
				fw.write(Float.toString(value[i+j]));
			}
			if(i == num)
				fw.write(")");
			else
				fw.write("),");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePointNfArray(List<Float> value, int componentCount) throws IOException{
		writeAttributeValuePointNfArray(value, value.size(), componentCount);
	}
	
	public void writeAttributeValuePointNfArray(List<Float> value, int size, int componentCount) throws IOException{
		writeAttributeValuePointNfArray(value, size, componentCount, false);
	}
	
	public void writeAttributeValuePointNfArray(List<Float> value, int size, int componentCount, boolean noEqual) throws IOException{
		if(componentCount <= 1) {
			writeAttributeValueFloatArray(value, size, noEqual);
			return;
		}
		fw.write(noEqual ? " [" : " = [");
		int num = size - componentCount;
		for(int i = 0; i <= num; i += componentCount) {
			fw.write("(");
			for(int j = 0; j < componentCount; ++j) {
				if(j > 0)
					fw.write(",");
				fw.write(Float.toString(value.get(i+j)));
			}
			if(i == num)
				fw.write(")");
			else
				fw.write("),");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueFloatArray(float[] value) throws IOException{
		writeAttributeValueFloatArray(value, value.length);
	}
	
	public void writeAttributeValueFloatArray(float[] value, int count) throws IOException{
		writeAttributeValueFloatArray(value, count, false);
	}
	
	public void writeAttributeValueFloatArray(float[] value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		for(int i = 0; i < count - 1; ++i) {
			fw.write(Float.toString(value[i]));  fw.write(",");
		}
		if(count > 0) {
			fw.write(Float.toString(value[count - 1]));  fw.write("");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueFloatArray(List<Float> value) throws IOException{
		writeAttributeValueFloatArray(value, value.size());
	}
	
	public void writeAttributeValueFloatArray(List<Float> value, int count) throws IOException{
		writeAttributeValueFloatArray(value, count, false);
	}
	
	public void writeAttributeValueFloatArray(List<Float> value, int count, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		for(int i = 0; i < count - 1; ++i) {
			fw.write(Float.toString(value.get(i)));  fw.write(",");
		}
		if(count > 0) {
			fw.write(Float.toString(value.get(count - 1)));  fw.write("");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueFloatCompound(float[] value) throws IOException{
		writeAttributeValueFloatCompound(value, false);
	}
	
	public void writeAttributeValueFloatCompound(float[] value, boolean noEqual) throws IOException{
		fw.write(noEqual ? " (" : " = (");
		for(int i = 0; i < value.length - 1; ++i) {
			fw.write(Float.toString(value[i]));  fw.write(",");
		}
		if(value.length > 0) {
			fw.write(Float.toString(value[value.length - 1]));  fw.write("");
		}
		fw.write(")");
	}
	
	public void writeAttributeValuePoint3fArray(float[] value) throws IOException{
		writeAttributeValuePoint3fArray(value, value.length);
	}
	
	public void writeAttributeValuePoint3fArray(float[] value, int size) throws IOException{
		writeAttributeValuePoint3fArray(value, size, false);
	}
	
	public void writeAttributeValuePoint3fArray(float[] value, int size, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
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
	
	public void writeAttributeValuePoint3fArray(List<Float> value) throws IOException{
		writeAttributeValuePoint3fArray(value, value.size());
	}
	
	public void writeAttributeValuePoint3fArray(List<Float> value, int size) throws IOException{
		writeAttributeValuePoint3fArray(value, size, false);
	}
	
	public void writeAttributeValuePoint3fArray(List<Float> value, int size, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		int num = size - 3;
		for(int i = 0; i <= num; i += 3) {
			fw.write("(");
			fw.write(Float.toString(value.get(i)));
			fw.write(",");
			fw.write(Float.toString(value.get(i+1)));
			fw.write(",");
			fw.write(Float.toString(value.get(i+2)));
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
		writeAttributeValuePoint2fArray(value, size, false);
	}
	
	public void writeAttributeValuePoint2fArray(float[] value, int size, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		for(int i = 0; i < size - 2; i += 2) {
			fw.write("(");  fw.write(Float.toString(value[i]));  fw.write(",");  fw.write(Float.toString(value[i+1]));  fw.write("),");
		}
		if(size > 1) {
			fw.write("(");  fw.write(Float.toString(value[size - 2]));  fw.write(",");  fw.write(Float.toString(value[size - 1]));  fw.write(")");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePoint2fArray(float[] valueX, float[] valueY, int size) throws IOException{
		fw.write(" = [");
		for(int i = 0; i < size - 1; i++) {
			fw.write("(");  fw.write(Float.toString(valueX[i]));  fw.write(",");  fw.write(Float.toString(valueY[i]));  fw.write("),");
		}
		if(size > 0) {
			fw.write("(");  fw.write(Float.toString(valueX[size - 1]));  fw.write(",");  fw.write(Float.toString(valueY[size - 1]));  fw.write(")");
		}
		fw.write("]");
	}
	
	public void writeAttributeValuePoint2fArray(float[] valueX, float[] valueY, int size, boolean noEqual) throws IOException{
		fw.write(noEqual ? " [" : " = [");
		for(int i = 0; i < size - 1; i++) {
			fw.write("(");  fw.write(Float.toString(valueX[i]));  fw.write(",");  fw.write(Float.toString(valueY[i]));  fw.write("),");
		}
		if(size > 0) {
			fw.write("(");  fw.write(Float.toString(valueX[size - 1]));  fw.write(",");  fw.write(Float.toString(valueY[size - 1]));  fw.write(")");
		}
		fw.write("]");
	}
	
	public void writeAttributeValueTimeSamplesFloat(List<Float> timeCodes, List<Float> values) throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		for(int i = 0; i < Math.min(timeCodes.size(), values.size()); ++i) {
			fw.write(getIndent());  fw.write(Float.toString(timeCodes.get(i).floatValue()));  fw.write(": ");  fw.write(Float.toString(values.get(i).floatValue()));  fw.write(",\n");
		}
		indent--;
		fw.write("}");
	}
	
	public void writeAttributeValueTimeSamplesFloatCompound(List<Float> timeCodes, List<Float> values, int compoundLength) throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		for(int i = 0; i < Math.min(timeCodes.size(), values.size()/compoundLength); ++i) {
			fw.write(getIndent());  fw.write(Float.toString(timeCodes.get(i).floatValue()));  fw.write(": (");
			for(int j = 0; j < compoundLength - 1; ++j) {
				fw.write(Float.toString(values.get(i*compoundLength + j).floatValue()));  fw.write(",");
			}
			if(compoundLength > 0)
				fw.write(Float.toString(values.get(i*compoundLength + compoundLength - 1).floatValue()));
			fw.write("),\n");
		}
		indent--;
		fw.write("}");
	}
	
	public void writeAttributeValueTimeSamplesFloatCompound(float[] timeCodes, float[] values, int compoundLength) throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		for(int i = 0; i < Math.min(timeCodes.length, values.length/compoundLength); ++i) {
			fw.write(getIndent());  fw.write(Float.toString(timeCodes[i]));  fw.write(": (");
			for(int j = 0; j < compoundLength - 1; ++j) {
				fw.write(Float.toString(values[i*compoundLength + j]));  fw.write(",");
			}
			if(compoundLength > 0)
				fw.write(Float.toString(values[i*compoundLength + compoundLength - 1]));
			fw.write("),\n");
		}
		indent--;
		fw.write("}");
	}
	
	private boolean hasWrittenTimeSamples = false;
	public void beginTimeSamples() throws IOException{
		fw.write(".timeSamples = {\n");
		indent++;
		hasWrittenTimeSamples = false;
	}
	
	public void endTimeSamples() throws IOException{
		indent--;
		fw.write("}");
	}
	
	public void writeTimeSampleTime(float timeCode) throws IOException{
		if(hasWrittenTimeSamples)
			fw.write(",");
		fw.write("\n");  fw.write(getIndent());  fw.write(Float.toString(timeCode));  fw.write(": ");
		hasWrittenTimeSamples = true;
	}
	
}