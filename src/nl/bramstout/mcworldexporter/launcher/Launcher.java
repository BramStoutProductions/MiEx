package nl.bramstout.mcworldexporter.launcher;

import java.util.List;

public abstract class Launcher {
	
	public abstract String getName();
	
	public abstract List<MinecraftVersion> getVersions();
	
	public abstract List<MinecraftSave> getSaves();
	
}
