package nl.bramstout.mcworldexporter.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LauncherSaveDir extends Launcher{

	private String name;
	private File path;
	
	public LauncherSaveDir(String name, File path) {
		this.name = name;
		this.path = path;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		return new ArrayList<MinecraftVersion>();
	}

	@Override
	public List<MinecraftSave> getSaves() {
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		if(path.exists() && path.isDirectory()) {
			for(File f : path.listFiles()) {
				if(f.isDirectory()) {
					File levelName = new File(f, "levelname.txt");
					if(levelName.exists()) {
						// Bedrock Edition
						BufferedReader reader = null;
						try {
							reader = new BufferedReader(new FileReader(levelName));
							String name = reader.readLine();
							saves.add(new MinecraftSave(name, f, new File(f, "world_icon.jpeg"), this));
							reader.close();
						}catch(Exception ex) {
							ex.printStackTrace();
						}
						try {
							if(reader != null)
								reader.close();
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}else {
						// Jave Edition
						saves.add(new MinecraftSave(f.getName(), f, new File(f, "icon.png"), this));
					}
				}
			}
		}
		return saves;
	}

}
