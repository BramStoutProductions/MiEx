package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.FileUtil;

public class LauncherRegistry {
	
	private static List<Launcher> launchers = new ArrayList<Launcher>();
	
	static {
		File javaEditionRootDir = new File(FileUtil.getMinecraftRootDir());
		if(javaEditionRootDir.exists() && javaEditionRootDir.isDirectory())
			launchers.add(new LauncherJavaEdition(javaEditionRootDir));
		
		File bedrockEditionRootDir = new File(FileUtil.getMinecraftBedrockRootDir());
		if(bedrockEditionRootDir.exists() && bedrockEditionRootDir.isDirectory())
			launchers.add(new LauncherBedrockEdition(bedrockEditionRootDir));
		
		File multiMCRootDir = new File(FileUtil.getMultiMCRootDir());
		if(multiMCRootDir.exists() && multiMCRootDir.isDirectory())
			launchers.add(new LauncherMultiMC(multiMCRootDir));
		
		File technicRootDir = new File(FileUtil.getTechnicRootDir());
		if(technicRootDir.exists() && technicRootDir.isDirectory())
			launchers.add(new LauncherTechnic(technicRootDir));
		
		File modrinthRootDir = new File(FileUtil.getModrinthRootDir());
		if(modrinthRootDir.exists() && modrinthRootDir.isDirectory())
			launchers.add(new LauncherModrinth(modrinthRootDir));
	}
	
	public static List<Launcher> getLaunchers(){
		return launchers;
	}
	
}
