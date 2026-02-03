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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.bramstout.mcworldexporter.Json;
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
		List<ResourcePackSource> sources = new ArrayList<ResourcePackSource>();
		
		List<String> mods = new ArrayList<String>();
		File configFile = new File(world.getWorldDir(), "config.json");
		if(configFile.exists()) {
			JsonElement el = Json.read(configFile);
			if(el != null && el.isJsonObject()) {
				JsonObject configData = el.getAsJsonObject();
				if(configData.has("Mods")) {
					for(Entry<String, JsonElement> entry : configData.getAsJsonObject("Mods").entrySet()) {
						if(entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("Enabled")) {
							if(entry.getValue().getAsJsonObject().get("Enabled").getAsBoolean()) {
								mods.add(entry.getKey());
							}
						}
					}
				}
			}
		}
		
		if(!mods.isEmpty()) {
			List<HytaleMod> installedMods = getMods();
			
			for(String modName : mods) {
				for(HytaleMod mod : installedMods) {
					if(mod.name.equals(modName)) {
						ResourcePackSource source = new ResourcePackSource(mod.name);
						source.addSource(mod.uuid, mod.file);
						sources.add(source);
						
						break;
					}
				}
			}
		}
		
		return sources;
	}
	
	public static class HytaleMod{
		String name;
		String uuid;
		File file;
		
		public HytaleMod(String name, String uuid, File file) {
			this.name = name;
			this.uuid = uuid;
			this.file = file;
		}
	}
	
	public List<HytaleMod> getMods(){
		List<HytaleMod> mods = new ArrayList<HytaleMod>();
		
		File modsFolder = new File(rootFolder, "UserData/Mods");
		if(modsFolder.exists() && modsFolder.isDirectory()) {
			for(File mod : modsFolder.listFiles()) {
				try {
					if(mod.isDirectory()) {
						File manifestFile = new File(mod, "manifest.json");
						JsonObject manifestData = Json.read(manifestFile).getAsJsonObject();
						
						String name = mod.getName();
						if(manifestData.has("Name"))
							name = manifestData.get("Name").getAsString();
						if(manifestData.has("Group")) {
							String group = manifestData.get("Group").getAsString();
							if(!group.isEmpty())
								name = group + ":" + name;
						}
						
						String uuid = ResourcePackSource.getHash(mod);
						
						mods.add(new HytaleMod(name, uuid, mod));
					}else if(mod.isFile() && mod.getName().endsWith(".zip")) {
						ZipInputStream zipIn = new ZipInputStream(new FileInputStream(mod));
						try {
						    ZipEntry entry = null;
						    
						    while ((entry = zipIn.getNextEntry()) != null) {
						    	String entryName = entry.getName();
						    	if(!entryName.equals("manifest.json"))
						    		continue;
						    	
						    	JsonObject manifestData = JsonParser.parseReader(new InputStreamReader(zipIn)).getAsJsonObject();
						    	
						    	String name = mod.getName();
								if(manifestData.has("Name"))
									name = manifestData.get("Name").getAsString();
								if(manifestData.has("Group")) {
									String group = manifestData.get("Group").getAsString();
									if(!group.isEmpty())
										name = group + ":" + name;
								}
								
								String uuid = ResourcePackSource.getHash(mod);
								
								mods.add(new HytaleMod(name, uuid, mod));
						    	
						        zipIn.closeEntry();
						    }
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					    zipIn.close();
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		return mods;
	}
	
	@Override
	public boolean ownsWorld(File worldFolder) {
		return worldFolder.getAbsolutePath().startsWith(rootFolder.getAbsolutePath());
	}

}
