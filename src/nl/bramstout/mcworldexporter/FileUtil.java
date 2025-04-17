/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.bramstout.mcworldexporter.image.ImageReader;

public class FileUtil {
	
	protected static String homeDir = null;
	public static String getHomeDir() {
		if(homeDir != null)
			return homeDir;
		homeDir = "./";
		
		String envPath = Environment.getEnv("MIEX_HOME_DIR");
		if(envPath != null)
			homeDir = envPath;
		
		return homeDir;
	}
	
	protected static String[] additionalSaveDirs = null;
	public static String[] getAdditionalSaveDirs() {
		if(additionalSaveDirs != null)
			return additionalSaveDirs;
		
		additionalSaveDirs = new String[] {};
		
		String pathsStr = Environment.getEnv("MIEX_ADDITIONAL_SAVE_DIRS");
		if(pathsStr != null) {
			String[] paths = pathsStr.split(";");
			for(String str : paths) {
				additionalSaveDirs = Arrays.copyOf(additionalSaveDirs, additionalSaveDirs.length + 1);
				additionalSaveDirs[additionalSaveDirs.length-1] = str;
			}
		}
		
		return additionalSaveDirs;
	}
	
	protected static String resourcePackDir = null;
	public static String getResourcePackDir() {
		if(resourcePackDir != null)
			return resourcePackDir;
		resourcePackDir = "./resources/";
		
		String envPath = Environment.getEnv("MIEX_RESOURCEPACK_DIR");
		if(envPath != null)
			resourcePackDir = envPath + "/";
		
		return resourcePackDir;
	}
	
	protected static String resourcePackUSDPrefix = null;
	public static String getResourcePackUSDPrefix() {
		if(resourcePackUSDPrefix != null)
			return resourcePackUSDPrefix;
		try {
			resourcePackUSDPrefix = new File(getResourcePackDir()).getCanonicalPath().replace('\\', '/');
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String envPath = Environment.getEnv("MIEX_RESOURCEPACK_USD_PREFIX");
		if(envPath != null)
			resourcePackUSDPrefix = envPath;
		
		if(!resourcePackUSDPrefix.endsWith("/"))
			resourcePackUSDPrefix = resourcePackUSDPrefix + "/";
		
		return resourcePackUSDPrefix;
	}
	
	protected static String resourcePackMTLXPrefix = null;
	public static String getResourcePackMTLXPrefix() {
		if(resourcePackMTLXPrefix != null)
			return resourcePackMTLXPrefix;
		try {
			resourcePackMTLXPrefix = new File(getResourcePackDir()).getCanonicalPath().replace('\\', '/');
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String envPath = Environment.getEnv("MIEX_RESOURCEPACK_MTLX_PREFIX");
		if(envPath != null)
			resourcePackMTLXPrefix = envPath;
		
		if(!resourcePackMTLXPrefix.endsWith("/"))
			resourcePackMTLXPrefix = resourcePackMTLXPrefix + "/";
		
		return resourcePackMTLXPrefix;
	}
	
	protected static String resourcePackJSONPrefix = null;
	public static String getResourcePackJSONPrefix() {
		if(resourcePackJSONPrefix != null)
			return resourcePackJSONPrefix;
		try {
			resourcePackJSONPrefix = new File(getResourcePackDir()).getCanonicalPath().replace('\\', '/');
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String envPath = Environment.getEnv("MIEX_RESOURCEPACK_JSON_PREFIX");
		if(envPath != null)
			resourcePackJSONPrefix = envPath;
		
		if(!resourcePackJSONPrefix.endsWith("/"))
			resourcePackJSONPrefix = resourcePackJSONPrefix + "/";
		
		return resourcePackJSONPrefix;
	}
	
	protected static String usdCatExe = null;
	public static String getUSDCatExe() {
		if(usdCatExe != null)
			return usdCatExe;
		String exe = Environment.getEnv("MIEX_USDCAT_EXE");
		if(exe == null)
			exe = "./usdcat/usdcat.exe";
		if(!new File(exe).exists())
			return null;
		usdCatExe = exe;
		return exe;
	}
	
	public static boolean hasUSDCat() {
		return getUSDCatExe() != null;
	}
	
	protected static String logFile = null;
	public static String getLogFile() {
		if(logFile != null)
			return logFile;
		String log = Environment.getEnv("MIEX_LOG_FILE");
		if(log == null)
			log = "./log.txt";
		logFile = log;
		return log;
	}
	
	
	private static Map<File, Boolean> hasAlphaCache = new HashMap<File, Boolean>();
	
	public static boolean hasAlpha(File file) {
		Boolean cachedValue = hasAlphaCache.getOrDefault(file, null);
		if(cachedValue == null) {
			synchronized(hasAlphaCache) {
				cachedValue = hasAlphaCache.getOrDefault(file, null);
				if(cachedValue == null) {
					cachedValue = Boolean.valueOf(calcHasAlpha(file));
					hasAlphaCache.put(file, cachedValue);
				}
			}
		}
		return cachedValue.booleanValue();
	}
	
	private static boolean calcHasAlpha(File file) {
		try {
			BufferedImage tex = ImageReader.readImage(file);
			if(tex != null) {
				for(int i = 0; i < tex.getWidth(); ++i) {
					for(int j = 0; j < tex.getHeight(); ++j) {
						Color color = new Color(tex.getRGB(i, j), true);
						if(color.getAlpha() < 255) {
							return true;
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	private static Map<File, Boolean> hasCutoutCache = new HashMap<File, Boolean>();
	
	public static boolean hasCutout(File file) {
		Boolean cachedValue = hasCutoutCache.getOrDefault(file, null);
		if(cachedValue == null) {
			synchronized(hasCutoutCache) {
				cachedValue = hasCutoutCache.getOrDefault(file, null);
				if(cachedValue == null) {
					cachedValue = Boolean.valueOf(calcHasCutout(file));
					hasCutoutCache.put(file, cachedValue);
				}
			}
		}
		return cachedValue.booleanValue();
	}
	
	private static boolean calcHasCutout(File file) {
		boolean hasAlpha = false;
		try {
			BufferedImage tex = ImageReader.readImage(file);
			if(tex != null) {
				for(int i = 0; i < tex.getWidth(); ++i) {
					for(int j = 0; j < tex.getHeight(); ++j) {
						Color color = new Color(tex.getRGB(i, j), true);
						if(color.getAlpha() > 0 && color.getAlpha() < 255) {
							return false;
						}
						if(color.getAlpha() < 255)
							hasAlpha = true;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return hasAlpha;
	}
	
	public static boolean isWindows() {
		return System.getProperty("os.name", "none").toLowerCase().contains("win");
	}
	
	public static boolean isMacOs() {
		return System.getProperty("os.name", "none").toLowerCase().contains("mac") ||
				System.getProperty("os.name", "none").toLowerCase().contains("darwin");
	}
	
	public static boolean isLinux() {
		return System.getProperty("os.name", "none").toLowerCase().contains("linux");
	}
	
	public static String getMinecraftRootDir() {
		String envPath = Environment.getEnv("MIEX_MINECRAFT_ROOT_DIR");
		if(envPath != null)
			return envPath;
		
		if(isWindows())
			return Environment.getEnv("APPDATA") + "/.minecraft";
		else if(isMacOs())
			return "~/Library/Application Support/minecraft";
		else if (isLinux())
			return "~/.config/.minecraft";

		// Return a placeholder value
		// to avoid erroring out on 
		// niche systems
		return "NOT FOUND";
	}
	
	public static String getMinecraftBedrockRootDir() {
		if(isWindows())
			return Environment.getEnv("LOCALAPPDATA") + "/Packages/Microsoft.MinecraftUWP_8wekyb3d8bbwe/LocalState/games/com.mojang";
		// Return a placeholder value
		// to avoid erroring out on 
		// niche systems
		return "NOT FOUND";
	}
	
	protected static String multiMCRootDir = null;
	public static String getMultiMCRootDir() {
		if(multiMCRootDir != null)
			return multiMCRootDir;
		multiMCRootDir = "";
		
		String envPath = Environment.getEnv("MIEX_MULTIMC_ROOT_DIR");
		if(envPath != null)
			multiMCRootDir = envPath + "/";
		
		return multiMCRootDir;
	}
	
	private static String getTechnicRootDir2() {
		if(isWindows())
			return Environment.getEnv("APPDATA") + "/.technic/";
		else if(isMacOs())
			return "~/Library/Application Support/technic/";
		else if (isLinux())
			return "~/.config/.technic/";

		// Return a placeholder value
		// to avoid erroring out on 
		// niche systems
		return "NOT FOUND";
	}
	
	protected static String technicRootDir = null;
	public static String getTechnicRootDir() {
		if(technicRootDir != null)
			return technicRootDir;
		technicRootDir = getTechnicRootDir2();
		
		String envPath = Environment.getEnv("MIEX_TECHNIC_ROOT_DIR");
		if(envPath != null)
			technicRootDir = envPath + "/";
		
		return technicRootDir;
	}
	
	private static String getModrinthRootDir2() {
		if(isWindows())
			return Environment.getEnv("APPDATA") + "/com.modrinth.theseus/";
		else if(isMacOs())
			return "~/Library/Application Support/com.modrinth.theseus/";
		else if (isLinux())
			return "~/.config/com.modrinth.theseus/";

		// Return a placeholder value
		// to avoid erroring out on 
		// niche systems
		return "NOT FOUND";
	}
	
	private static String getModrinthRootDir3() {
		// Modrinth changes the name of the root directory.
		if(isWindows())
			return Environment.getEnv("APPDATA") + "/ModrinthApp/";
		else if(isMacOs())
			return "~/Library/Application Support/ModrinthApp/";
		else if (isLinux())
			return "~/.config/ModrinthApp/";

		// Return a placeholder value
		// to avoid erroring out on 
		// niche systems
		return "NOT FOUND";
	}
	
	protected static String modrinthRootDir = null;
	public static String getModrinthRootDir() {
		if(modrinthRootDir != null)
			return modrinthRootDir;
		modrinthRootDir = getModrinthRootDir3();
		if(!(new File(modrinthRootDir.substring(0, modrinthRootDir.length()-1)).exists()))
			modrinthRootDir = getModrinthRootDir2();
		
		String envPath = Environment.getEnv("MIEX_MODRINTH_ROOT_DIR");
		if(envPath != null)
			modrinthRootDir = envPath + "/";
		
		return modrinthRootDir;
	}
	
	public static File findJarFile(File versionsFolder, String versionName) {
		File versionFolder = new File(versionsFolder, versionName);
		File versionJar = new File(versionFolder, versionName + ".jar");
		if(versionJar.exists())
			return versionJar;
		// It could also be located somewhere else, so let's try a few things.
		versionJar = new File(versionFolder, "/bin/minecraft.jar");
		if(versionJar.exists())
			return versionJar;
		
		// Iterate through all folders to find a valid jar file
		return findValidJarFileInFolder(versionFolder);
	}
	
	private static File findValidJarFileInFolder(File folder) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				File jarFile = findValidJarFileInFolder(f);
				if(jarFile != null)
					return jarFile;
			}else if(f.isFile()) {
				if(f.getName().endsWith(".jar")) {
					if(isValidJarFile(f))
						return f;
				}
			}
		}
		return null;
	}
	
	private static boolean isValidJarFile(File jarFile){
		ZipInputStream zipIn = null;
		try {
			zipIn = new ZipInputStream(new FileInputStream(jarFile));
		    ZipEntry entry = null;
		    
		    while ((entry = zipIn.getNextEntry()) != null) {
		    	String entryName = entry.getName();
		    	if(entryName.equals("version.json")) {
		    		zipIn.close();
		    		return true;
		    	}
		    }
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		try {
		if(zipIn != null)
			zipIn.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	    return false;
	}

}
