package nl.bramstout.mcworldexporter.launcher;

import java.io.File;

public class MinecraftSave {
	
	private String label;
	private File worldFolder;
	private File icon;
	private Launcher launcher;
	
	public MinecraftSave(String label, File worldFolder, File icon, Launcher launcher) {
		this.label = label;
		this.worldFolder = worldFolder;
		this.icon = icon;
		this.launcher = launcher;
	}
	
	public String getLabel() {
		return label;
	}
	
	public File getWorldFolder() {
		return worldFolder;
	}
	
	public File getIcon() {
		return icon;
	}
	
	public Launcher getLauncher() {
		return launcher;
	}
	
}
