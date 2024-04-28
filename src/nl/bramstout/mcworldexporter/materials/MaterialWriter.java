package nl.bramstout.mcworldexporter.materials;

import java.io.File;
import java.io.IOException;

import nl.bramstout.mcworldexporter.materials.Materials.MaterialTemplate;

public abstract class MaterialWriter {
	
	protected File outputFile;
	
	public MaterialWriter(File outputFile) {
		this.outputFile = outputFile;
	}
	
	public File getOutputFile() {
		return outputFile;
	}
	
	public abstract void writeMaterial(MaterialTemplate material, String texture, boolean hasBiomeColor,
			String parentPrim, String sharedPrims) throws IOException;
	
	public abstract void writeSharedNodes(String parentPrim) throws IOException;
	
	public abstract void open() throws IOException;
	
	public abstract void close() throws IOException;
	
	public abstract boolean hasWrittenAnything();
	
	public abstract String getUSDAssetPath();
	
}
