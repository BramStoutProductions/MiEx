package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.FileUtil;

public class LauncherMultiMC extends Launcher{
	
	private File rootFile;
	
	public LauncherMultiMC(File rootFile) {
		this.rootFile = rootFile;
	}

	@Override
	public String getName() {
		return "MultiMC";
	}
	
	@Override
	public List<MinecraftVersion> getVersions() {
		List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
		File multimcVersionsFolder = new File(rootFile, "libraries/com/mojang/minecraft");
		if(multimcVersionsFolder.exists() && multimcVersionsFolder.isDirectory()) {
			for(File f : multimcVersionsFolder.listFiles()) {
				if(f.isDirectory()) {
					File jarFile = FileUtil.findJarFile(multimcVersionsFolder, f.getName());
					if(jarFile != null)
						versions.add(new MinecraftVersion("MultiMC/" + f.getName(), jarFile));
				}
			}
		}
		return versions;
	}

	@Override
	public List<MinecraftSave> getSaves() {
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		File instacesFolder = new File(rootFile, "instances");
		if(instacesFolder.exists() && instacesFolder.isDirectory()) {
			for(File f : instacesFolder.listFiles()) {
				File savesFolder = new File(f, "minecraft/saves");
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
