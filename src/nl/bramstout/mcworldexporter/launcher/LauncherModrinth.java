package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.FileUtil;

public class LauncherModrinth extends Launcher{
	
	private File rootFile;
	
	public LauncherModrinth(File rootFile) {
		this.rootFile = rootFile;
	}

	@Override
	public String getName() {
		return "Modrinth";
	}
	
	@Override
	public List<MinecraftVersion> getVersions() {
		List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
		File versionsFolder = new File(rootFile, "meta/versions");
		if(versionsFolder.exists() && versionsFolder.isDirectory()) {
			for(File f : versionsFolder.listFiles()) {
				if(f.isDirectory()) {
					File jarFile = FileUtil.findJarFile(versionsFolder, f.getName());
					if(jarFile != null)
						versions.add(new MinecraftVersion("Modrinth/" + f.getName(), jarFile));
				}
			}
		}
		return versions;
	}

	@Override
	public List<MinecraftSave> getSaves() {
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		File instacesFolder = new File(rootFile, "profiles");
		if(instacesFolder.exists() && instacesFolder.isDirectory()) {
			for(File f : instacesFolder.listFiles()) {
				File savesFolder = new File(f, "saves");
				if(savesFolder.exists() && savesFolder.isDirectory()) {
					for(File save : savesFolder.listFiles()) {
						if(save.isDirectory()) {
							saves.add(new MinecraftSave(f.getName() + "/" + save.getName(), save, 
														new File(save, "icon.png"), this));
						}
					}
				}
			}
		}
		return saves;
	}
	
}
