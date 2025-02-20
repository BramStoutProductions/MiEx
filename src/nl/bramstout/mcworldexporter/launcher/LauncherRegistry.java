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
		launchers.clear();
		
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
		
		for(String saveDirStr : FileUtil.getAdditionalSaveDirs()) {
			LauncherSavesDirectory launcher = new LauncherSavesDirectory(saveDirStr);
			if(launcher.isValid())
				launchers.add(launcher);
		}
	}
	
	public static List<Launcher> getLaunchers(){
		return launchers;
	}
	
}
