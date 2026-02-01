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
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.world.World;

public class LauncherModrinth extends Launcher{
	
	private File rootFile;
	
	public LauncherModrinth(File rootFile) {
		this.rootFile = rootFile;
	}

	@Override
	public String getName() {
		return "Modrinth";
	}
	
	@Override
	public List<MinecraftVersion> getVersions() {
		List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
		File versionsFolder = new File(rootFile, "meta/versions");
		if(versionsFolder.exists() && versionsFolder.isDirectory()) {
			for(File f : versionsFolder.listFiles()) {
				if(f.isDirectory()) {
					File jarFile = FileUtil.findJarFile(versionsFolder, f.getName());
					if(jarFile != null)
						versions.add(new MinecraftVersion("Modrinth/" + f.getName(), jarFile));
				}
			}
		}
		return versions;
	}

	@Override
	public List<MinecraftSave> getSaves() {
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		File instacesFolder = new File(rootFile, "profiles");
		if(instacesFolder.exists() && instacesFolder.isDirectory()) {
			for(File f : instacesFolder.listFiles()) {
				File savesFolder = new File(f, "saves");
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
	
	@Override
	public List<ResourcePackSource> getResourcePackSourcesForWorld(World world) {
		List<ResourcePackSource> sources = new ArrayList<ResourcePackSource>();
		
		File instanceFolder = getInstanceFolderForWorld(world);
		if(instanceFolder != null) {
			File modsFolder = new File(instanceFolder, "mods");
			if(modsFolder.exists()) {
				ResourcePackSource source = new ResourcePackSource("Modrinth " + instanceFolder.getName() + " Mods");
				findSources(modsFolder, source);
				sources.add(source);
			}
		}
		
		return sources;
	}
	
	private File getInstanceFolderForWorld(World world) {
		File instacesFolder = new File(rootFile, "profiles");
		if(instacesFolder.exists() && instacesFolder.isDirectory()) {
			for(File f : instacesFolder.listFiles()) {
				if(world.getWorldDir().getAbsolutePath().startsWith(f.getAbsolutePath())) {
					return f;
				}
			}
		}
		return null;
	}
	
	private void findSources(File file, ResourcePackSource source) {
		if(file.isDirectory()) {
			for(File f : file.listFiles()) {
				findSources(f, source);
			}
		}else if(file.isFile()) {
			if(file.getName().endsWith(".jar")) {
				source.addSource(ResourcePackSource.getHash(file), file);
			}
		}
	}
	
	@Override
	public boolean ownsWorld(File worldFolder) {
		return worldFolder.getAbsolutePath().startsWith(rootFile.getAbsolutePath());
	}
	
}
