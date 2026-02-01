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

import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.world.World;

public class LauncherHytale extends Launcher{
	
	private File rootFolder;
	
	public LauncherHytale(File rootFolder) {
		this.rootFolder = rootFolder;
	}

	@Override
	public String getName() {
		return "Hytale";
	}
	
	public List<HytaleVersion> getHytaleVersions(){
		List<HytaleVersion> versions = new ArrayList<HytaleVersion>();
		
		File installFolder = new File(rootFolder, "install");
		for(File versionFolder : installFolder.listFiles()) {
			if(!versionFolder.isDirectory())
				continue;
			File assetsFolder = new File(versionFolder, "package/game/latest/Assets.zip");
			if(assetsFolder.exists()) {
				versions.add(new HytaleVersion(versionFolder.getName(), assetsFolder));
			}
		}
		
		return versions;
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		return new ArrayList<MinecraftVersion>();
	}

	@Override
	public List<MinecraftSave> getSaves() {
		File savesFolder = new File(rootFolder, "UserData/Saves");
		if(!savesFolder.exists() || !savesFolder.isDirectory())
			return new ArrayList<MinecraftSave>();
		
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		for(File f : savesFolder.listFiles()) {
			if(f.isDirectory())
				saves.add(new MinecraftSave(f.getName(), f, new File(f, "preview.png"), this));
		}
		return saves;
	}
	
	@Override
	public List<ResourcePackSource> getResourcePackSourcesForWorld(World world) {
		return new ArrayList<ResourcePackSource>();
	}
	
	@Override
	public boolean ownsWorld(File worldFolder) {
		return worldFolder.getAbsolutePath().startsWith(rootFolder.getAbsolutePath());
	}

}
