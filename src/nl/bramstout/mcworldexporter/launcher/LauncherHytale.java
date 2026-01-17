package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LauncherHytale extends Launcher{
	
	private File rootFolder;
	
	public LauncherHytale(File rootFolder) {
		this.rootFolder = rootFolder;
	}

	@Override
	public String getName() {
		return "Hytale";
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		return new ArrayList<MinecraftVersion>();
	}

	@Override
	public List<MinecraftSave> getSaves() {
		File savesFolder = new File(rootFolder, "UserData/Saves");
		if(!savesFolder.exists() || !savesFolder.isDirectory())
			return new ArrayList<MinecraftSave>();
		
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		for(File f : savesFolder.listFiles()) {
			if(f.isDirectory())
				saves.add(new MinecraftSave(f.getName(), f, new File(f, "preview.png"), this));
		}
		return saves;
	}

}
