package nl.bramstout.mcworldexporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Environment {
	
	public static enum VariableType{
		STRING, FILE, FOLDER, INTEGER, FLOAT, BOOLEAN,
		STRING_ARRAY, FILE_ARRAY, FOLDER_ARRAY
	}
	
	public static class EnvironmentVariable{
		
		private String name;
		private VariableType type;
		private String defaultValue;
		private String description;
		
		public EnvironmentVariable(String name, VariableType type, String defaultValue, String description) {
			this.name = name;
			this.type = type;
			this.defaultValue = defaultValue;
			this.description = description;
		}
		
		public String getName() {
			return name;
		}
		
		public VariableType getType() {
			return type;
		}
		
		public String getDefaultValue() {
			return defaultValue;
		}
		
		public String getDescription() {
			return description;
		}
		
	}
	
	public static EnvironmentVariable[] ENVIRONMENT_VARIABLES = new EnvironmentVariable[] {
			new EnvironmentVariable("MIEX_HOME_DIR", VariableType.FOLDER, "./",
					"The default directory opened up when launching a file browser."),
			new EnvironmentVariable("MIEX_LOG_FILE", VariableType.FILE, "./log.txt",
					"The path to the log file where it should write the log into."),
			new EnvironmentVariable("MIEX_USDCAT_EXE", VariableType.FILE, "./usdcat/usdcat.exe",
					"The path to the usdcat executable. If it exists, it will use this to convert the USD files to USDC (binary) files. " + 
					"USDC files are more efficient."),
			new EnvironmentVariable("MIEX_RESOURCEPACK_DIR", VariableType.FOLDER, "./resources",
					"The folder containing all of the resource packs."),
			new EnvironmentVariable("MIEX_RESOURCEPACK_JSON_PREFIX", VariableType.STRING, null,
					"The path prefix used when exporting materials to JSON. The internal paths are relative to the MIEX_RESOURCEPACK_DIR " + 
					"folder, and the path prefix is used to construct the final path. By default it is set to the absolute path of " + 
					"MIEX_RESOURCE_PACK_DIR"),
			new EnvironmentVariable("MIEX_RESOURCEPACK_USD_PREFIX", VariableType.STRING, null,
					"The path prefix used when exporting materials to USD. The internal paths are relative to the MIEX_RESOURCEPACK_DIR " + 
					"folder, and the path prefix is used to construct the final path. By default it is set to the absolute path of " + 
					"MIEX_RESOURCE_PACK_DIR"),
			new EnvironmentVariable("MIEX_ADDITIONAL_SAVE_DIRS", VariableType.FOLDER_ARRAY, null,
					"A semicolon separated list of directories containing world saves. Additionally, each entry can specify a name " + 
					"using the following syntax <name>|<directory_path>"),
			new EnvironmentVariable("MIEX_MODRINTH_ROOT_DIR", VariableType.FOLDER, null,
					"The path to the Modrinth root directory. This allows MiEx to find Minecraft versions and saves from the Modrinth launcher."),
			new EnvironmentVariable("MIEX_MULTIMC_ROOT_DIR", VariableType.FOLDER, null,
					"The path to a MultiMC (or MultiMC derivative like Prism) root directory. This allows MiEx to find Minecraft versions " + 
					"and saves from the MultiMC launcher."),
			new EnvironmentVariable("MIEX_TECHNIC_ROOT_DIR", VariableType.FOLDER, null,
					"The path to the Technic launcher root directory. This allows MiEx to find Minecraft versions and saves from the " + 
					"Technic launcher."),
			new EnvironmentVariable("MIEX_NUM_UI_THREADS", VariableType.INTEGER, "4",
					"The number of threads that MiEx should leave, at the minimum, for the user interface, other programs, and your OS."),
			new EnvironmentVariable("MIEX_PORTABLE_EXPORTS", VariableType.BOOLEAN, "0",
					"Whether MiEx should make exports with all files used inside of the export's chunks folder, making the export fully " + 
					"portable. By default MiEx puts in the paths to the files directly in the resource pack folders, which prevents lots " + 
					"of unnecessary duplicate files, but makes it difficult to share exports."),
			new EnvironmentVariable("MIEX_GITHUB_REPO", VariableType.STRING, "BramStoutProductions/MiEx",
					"The GitHub repository that MiEx should check for the latest versions of the built-in files. " + 
					"You can change this if, for example, you're working in a team and want to have your own set of built-in files."),
			new EnvironmentVariable("MIEX_OFFLINE_MODE", VariableType.BOOLEAN, "0",
					"Puts MiEx into offline mode where it won't check GitHub for new versions of MiEx or new built-in files.")
	};

	private static Map<String, String> values = new HashMap<String, String>();
	
	public static void loadFromEnvFile() {
		String envFilePath = System.getenv("MIEX_ENV_FILE");
		if(envFilePath == null)
			envFilePath = "./miex.env";
		
		File envFile = new File(envFilePath);
		if(envFile.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(envFile));
				
				String str = null;
				while((str = reader.readLine()) != null) {
					if(str.startsWith("#"))
						continue;
					int sepPos = str.indexOf('=');
					if(sepPos < 0)
						continue;
					if(sepPos == 0)
						continue;
					String envName = str.substring(0, sepPos);
					String envValue = str.substring(sepPos + 1);
					values.put(envName, envValue);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(reader != null) {
				try {
					reader.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public static void saveToEnvFile() {
		String envFilePath = System.getenv("MIEX_ENV_FILE");
		if(envFilePath == null)
			envFilePath = "./miex.env";
		
		File envFile = new File(envFilePath);
		if(!envFile.exists()) {
			File envFileFolder = envFile.getAbsoluteFile().getParentFile();
			if(!envFileFolder.exists())
				envFileFolder.mkdirs();
		}
		
		String envData = "";
		for(Entry<String, String> entry : values.entrySet()) {
			envData += entry.getKey() + "=" + entry.getValue() + "\n";
		}
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(envFile);
			writer.write(envData);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(writer != null) {
			try {
				writer.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void setEnv(String key, String value) {
		if(value == null || value.isEmpty())
			values.remove(key);
		else
			values.put(key, value);
	}
	
	public static void setEnv(Map<String, String> newValues) {
		if(newValues == null) {
			values.clear();
		}else {
			values = newValues;
		}
	}
	
	public static String getEnv(String key) {
		String val = values.getOrDefault(key, null);
		if(val != null)
			return val;
		return System.getenv(key);
	}
	
	public static boolean hasEditedInEnvFile(String key) {
		return values.containsKey(key);
	}
	
	public static boolean hasChanged(Map<String, String> newValues) {
		if(newValues.size() != values.size())
			return true;
		for(Entry<String, String> value : newValues.entrySet()) {
			String oldValue = values.getOrDefault(value.getKey(), null);
			if(oldValue == null) {
				if(value.getValue() == null)
					continue;
				if(value.getValue() != null)
					return true;
			}
			if(!oldValue.equals(value.getValue()))
				return true;
		}
		return false;
	}
	
}
