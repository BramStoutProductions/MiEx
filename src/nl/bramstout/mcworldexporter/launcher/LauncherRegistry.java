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

package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.FileUtil;

public class LauncherRegistry {
	
	private static List<Launcher> launchers = new ArrayList<Launcher>();
	
	public static void initLaunchers() {
		System.out.println("Searching for launchers.");
		launchers.clear();
		
		File javaEditionRootDir = new File(FileUtil.getMinecraftRootDir());
		if(javaEditionRootDir.exists() && javaEditionRootDir.isDirectory()) {
			System.out.println("Found Minecraft Java Edition launcher at " + javaEditionRootDir.getPath());
			launchers.add(new LauncherJavaEdition(javaEditionRootDir));
		}
		
		List<String> bedrockEditionRootDirs = FileUtil.getMinecraftBedrockRootDir();
		if(!bedrockEditionRootDirs.isEmpty()) {
			System.out.println("Found Minecraft Bedrock Edition launcher at:");
			for(String str : bedrockEditionRootDirs)
				System.out.println("    " + str);
			launchers.add(new LauncherBedrockEdition(bedrockEditionRootDirs));
		}
		
		File multiMCRootDir = new File(FileUtil.getMultiMCRootDir());
		if(multiMCRootDir.exists() && multiMCRootDir.isDirectory()) {
			System.out.println("Found Multi MC launcher at " + multiMCRootDir.getPath());
			launchers.add(new LauncherMultiMC(multiMCRootDir));
		}
		
		File technicRootDir = new File(FileUtil.getTechnicRootDir());
		if(technicRootDir.exists() && technicRootDir.isDirectory()) {
			System.out.println("Found Technic launcher at " + technicRootDir.getPath());
			launchers.add(new LauncherTechnic(technicRootDir));
		}
		
		File modrinthRootDir = new File(FileUtil.getModrinthRootDir());
		if(modrinthRootDir.exists() && modrinthRootDir.isDirectory()) {
			System.out.println("Found Modrinth launcher at " + modrinthRootDir.getPath());
			launchers.add(new LauncherModrinth(modrinthRootDir));
		}
		
		File hytaleRootDir = new File(FileUtil.getHytaleRootDir());
		if(hytaleRootDir.exists() && hytaleRootDir.isDirectory()) {
			System.out.println("Found Hytale launcher at " + hytaleRootDir.getPath());
			launchers.add(new LauncherHytale(hytaleRootDir));
		}
		
		for(String saveDirStr : FileUtil.getAdditionalSaveDirs()) {
			LauncherSavesDirectory launcher = new LauncherSavesDirectory(saveDirStr);
			if(launcher.isValid()) {
				System.out.println("Found " + launcher.getName() + " saves launcher at " + launcher.getFolder().getPath());
				launchers.add(launcher);
			}
		}
	}
	
	public static List<Launcher> getLaunchers(){
		return launchers;
	}
	
	public static Launcher getLauncherForWorld(File worldFolder) {
		for(Launcher launcher : launchers)
			if(launcher.ownsWorld(worldFolder))
				return launcher;
		return null;
	}
	
}
