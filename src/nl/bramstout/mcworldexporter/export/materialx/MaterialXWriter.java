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
	
	public void close() throws IOException{
		fw.close();
		fw = null;
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
