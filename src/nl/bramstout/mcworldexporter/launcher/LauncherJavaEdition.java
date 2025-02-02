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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;

public class LauncherJavaEdition extends Launcher{
	
	private File rootFolder;
	
	public LauncherJavaEdition(File rootFolder) {
		this.rootFolder = rootFolder;
	}

	@Override
	public String getName() {
		return "Java Edition";
	}

	@Override
	public List<MinecraftVersion> getVersions() {
		File versionsFolder = new File(rootFolder, "versions");
		if(!versionsFolder.exists() || !versionsFolder.isDirectory())
			return new ArrayList<MinecraftVersion>();
		
		File versionManifest = new File(versionsFolder, "version_manifest_v2.json");
		if(versionManifest.exists()) {
			List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
			try {
				JsonObject data = Json.read(versionManifest).getAsJsonObject();
				for(JsonElement e : data.get("versions").getAsJsonArray().asList()) {
					String name = e.getAsJsonObject().get("id").getAsString();
					File versionFolder = new File(versionsFolder, name);
					if(!versionFolder.exists() || !versionsFolder.isDirectory())
						continue;
					File versionJar = new File(versionFolder, name + ".jar");
					if(!versionJar.exists())
						continue;
					versions.add(new MinecraftVersion("MC/" + name, versionJar));
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return versions;
		}else {
			List<MinecraftVersion> versions = new ArrayList<MinecraftVersion>();
			for(File f : versionsFolder.listFiles()) {
				if(f.isDirectory()) {
					File versionJar = new File(versionsFolder, f.getName() + "/" + f.getName() + ".jar");
					if(versionJar.exists())
						versions.add(new MinecraftVersion("MC/" + f.getName(), versionJar));
				}
			}
			return versions;
		}
	}

	@Override
	public List<MinecraftSave> getSaves() {
		File savesFolder = new File(rootFolder, "saves");
		if(!savesFolder.exists() || !savesFolder.isDirectory())
			return new ArrayList<MinecraftSave>();
		
		List<MinecraftSave> saves = new ArrayList<MinecraftSave>();
		for(File f : savesFolder.listFiles()) {
			if(f.isDirectory())
				saves.add(new MinecraftSave(f.getName(), f, new File(f, "icon.png"), this));
		}
		return saves;
	}
}
