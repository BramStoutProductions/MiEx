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

public class LauncherBedrockEdition extends Launcher{
	
	private List<String> rootFolders;
	
	public LauncherBedrockEdition(List<String> rootFolders) {
		this.rootFolders = rootFolders;
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
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();

		for(String rootFolder : rootFolders) {
			File savesFolder = new File(rootFolder, "minecraftWorlds");
			if(!savesFolder.exists() || !savesFolder.isDirectory())
				continue;
			
			
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
		}
		
		return saves;
	}
	
	@Override
	public List<ResourcePackSource> getResourcePackSourcesForWorld(World world) {
		return new ArrayList<ResourcePackSource>();
	}
	
	@Override
	public boolean ownsWorld(File worldFolder) {
		for(String rootFolder : rootFolders) {
			if(worldFolder.getAbsolutePath().startsWith(new File(rootFolder).getAbsolutePath()))
				return true;
		}
		return false;
	}
	
	@Override
	public List<ResourcePackSource> getAllResourcePackSources() {
		List<ResourcePackSource> sources = new ArrayList<ResourcePackSource>();

		for(String rootFolder : rootFolders) {
			File resourcePacksFolder = new File(rootFolder, "resource_packs");
			if(resourcePacksFolder.exists() && resourcePacksFolder.isDirectory()) {
				for(File f : resourcePacksFolder.listFiles()) {
					if(f.isDirectory() && new File(f, "manifest.json").exists()) {
						ResourcePackSource source = new ResourcePackSource("resource_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}else if(f.isFile() && f.getName().endsWith(".zip")) {
						ResourcePackSource source = new ResourcePackSource("resource_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}
				}
			}
			File behaviorPacksFolder = new File(rootFolder, "behavior_packs");
			if(behaviorPacksFolder.exists() && behaviorPacksFolder.isDirectory()) {
				for(File f : behaviorPacksFolder.listFiles()) {
					if(f.isDirectory() && new File(f, "manifest.json").exists()) {
						ResourcePackSource source = new ResourcePackSource("behavior_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}else if(f.isFile() && f.getName().endsWith(".zip")) {
						ResourcePackSource source = new ResourcePackSource("behavior_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}
				}
			}
			File developmentResourcePacksFolder = new File(rootFolder, "development_resource_packs");
			if(developmentResourcePacksFolder.exists() && developmentResourcePacksFolder.isDirectory()) {
				for(File f : developmentResourcePacksFolder.listFiles()) {
					if(f.isDirectory() && new File(f, "manifest.json").exists()) {
						ResourcePackSource source = new ResourcePackSource("development_resource_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}else if(f.isFile() && f.getName().endsWith(".zip")) {
						ResourcePackSource source = new ResourcePackSource("development_resource_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}
				}
			}
			File developmentBehaviorPacksFolder = new File(rootFolder, "development_behavior_packs");
			if(developmentBehaviorPacksFolder.exists() && developmentBehaviorPacksFolder.isDirectory()) {
				for(File f : developmentBehaviorPacksFolder.listFiles()) {
					if(f.isDirectory() && new File(f, "manifest.json").exists()) {
						ResourcePackSource source = new ResourcePackSource("development_behavior_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}else if(f.isFile() && f.getName().endsWith(".zip")) {
						ResourcePackSource source = new ResourcePackSource("development_behavior_packs/" + f.getName(), this);
						source.addSource(ResourcePackSource.getHash(f), f);
						sources.add(source);
					}
				}
			}
		}
		
		return sources;
	}

}
