package nl.bramstout.mcworldexporter.launcher;

import java.io.File;

public class MinecraftVersion {
	
	private String label;
	private File jarFile;
	
	public MinecraftVersion(String label, File jarFile) {
		this.label = label;
		this.jarFile = jarFile;
	}
	
	public String getLabel() {
		return label;
	}
	
	public File getJarFile() {
		return jarFile;
	}
	
}
