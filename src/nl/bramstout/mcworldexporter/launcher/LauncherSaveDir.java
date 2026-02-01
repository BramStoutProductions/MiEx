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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.world.World;

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
	
	@Override
	public List<ResourcePackSource> getResourcePackSourcesForWorld(World world) {
		return new ArrayList<ResourcePackSource>();
	}
	
	@Override
	public boolean ownsWorld(File worldFolder) {
		return worldFolder.getAbsolutePath().startsWith(path.getAbsolutePath());
	}

}
