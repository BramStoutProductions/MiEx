package nl.bramstout.mcworldexporter.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LauncherSavesDirectory extends Launcher{
	
	private String name;
	private File savesFolder;
	
	public LauncherSavesDirectory(String savesFolderStr) {
		this.name = "Save Dir";
		int sepIndex = savesFolderStr.indexOf('|');
		if(sepIndex >= 0) {
			this.name = savesFolderStr.substring(0, sepIndex);
			savesFolderStr = savesFolderStr.substring(sepIndex+1);
		}
		this.savesFolder = new File(savesFolderStr);
	}
	
	public boolean isValid() {
		return this.savesFolder.exists() && this.savesFolder.isDirectory();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		return new ArrayList<MinecraftVersion>();
	}

	@Override
	public List<MinecraftSave> getSaves() {
		if(!savesFolder.exists() || !savesFolder.isDirectory())
			return new ArrayList<MinecraftSave>();
		
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		for(File f : savesFolder.listFiles()) {
			if(!f.isDirectory())
				continue;
			File levelName = new File(f, "levelname.txt");
			
			if(levelName.exists()) {
				// Bedrock save
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
				// Java save.
				saves.add(new MinecraftSave(f.getName(), f, new File(f, "icon.png"), this));
			}
		}
		
		return saves;
	}

}
