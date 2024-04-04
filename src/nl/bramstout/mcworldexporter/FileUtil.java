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
import java.io.IOException;

import javax.imageio.ImageIO;

public class FileUtil {
	
	protected static String homeDir = null;
	public static String getHomeDir() {
		if(homeDir != null)
			return homeDir;
		homeDir = "./";
		
		String envPath = System.getenv("MIEX_HOME_DIR");
		if(envPath != null)
			homeDir = envPath;
		
		return homeDir;
	}
	
	protected static String resourcePackDir = null;
	public static String getResourcePackDir() {
		if(resourcePackDir != null)
			return resourcePackDir;
		resourcePackDir = "./resources/";
		
		String envPath = System.getenv("MIEX_RESOURCEPACK_DIR");
		if(envPath != null)
			resourcePackDir = envPath + "/";
		
		return resourcePackDir;
	}
	
	protected static String resourcePackUSDPrefix = null;
	public static String getResourcePackUSDPrefix() {
		if(resourcePackUSDPrefix != null)
			return resourcePackUSDPrefix;
		try {
			resourcePackUSDPrefix = new File("./resources/").getCanonicalPath().replace('\\', '/');
			if(!resourcePackUSDPrefix.endsWith("/"))
				resourcePackUSDPrefix = resourcePackUSDPrefix + "/";
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String envPath = System.getenv("MIEX_RESOURCEPACK_USD_PREFIX");
		if(envPath != null)
			resourcePackUSDPrefix = envPath;
		
		return resourcePackUSDPrefix;
	}
	
	protected static String usdCatExe = null;
	public static String getUSDCatExe() {
		if(usdCatExe != null)
			return usdCatExe;
		String exe = System.getenv("MIEX_USDCAT_EXE");
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
	
	
	public static boolean hasAlpha(File file) {
		try {
			BufferedImage tex = ImageIO.read(file);
			for(int i = 0; i < tex.getWidth(); ++i) {
				for(int j = 0; j < tex.getHeight(); ++j) {
					Color color = new Color(tex.getRGB(i, j), true);
					if(color.getAlpha() < 255) {
						return true;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	public static String getMinecraftSavesDir() {
		return System.getenv("APPDATA") + "/.minecraft/saves";
	}

}
