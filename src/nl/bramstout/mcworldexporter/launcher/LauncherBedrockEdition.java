package nl.bramstout.mcworldexporter.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LauncherBedrockEdition extends Launcher{
	
	private File rootFolder;
	
	public LauncherBedrockEdition(File rootFolder) {
		this.rootFolder = rootFolder;
	}

	@Override
	public String getName() {
		return "Bedrock Edition";
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		return new ArrayList<MinecraftVersion>();
	}

	@Override
	public List<MinecraftSave> getSaves() {
		File savesFolder = new File(rootFolder, "minecraftWorlds");
		if(!savesFolder.exists() || !savesFolder.isDirectory())
			return new ArrayList<MinecraftSave>();
		
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		
		for(File f : savesFolder.listFiles()) {
			File levelName = new File(f, "levelname.txt");
			if(!levelName.exists())
				continue;
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
		}
		
		return saves;
	}

}
