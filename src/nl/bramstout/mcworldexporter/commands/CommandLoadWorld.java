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

package nl.bramstout.mcworldexporter.commands;

import java.io.File;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;
import nl.bramstout.mcworldexporter.launcher.MinecraftSave;
import nl.bramstout.mcworldexporter.ui.Popups;
import nl.bramstout.mcworldexporter.ui.ResourcePackSourcesExtractorDialog;

public class CommandLoadWorld extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		String world = "";
		if(command.has("world"))
			world = command.get("world").getAsString();
		
		Integer loadRps = Popups.NO_OPTION;
		if(command.has("loadWorldResourcePacks"))
			loadRps = command.get("loadWorldResourcePacks").getAsBoolean() ? Popups.YES_OPTION : Popups.NO_OPTION;
		Popups.setInputDialogSelection("Load Resource Packs?", loadRps);
		
		String rpName = null;
		if(command.has("worldResourcePackName"))
			rpName = command.get("worldResourcePackName").getAsString();
		ResourcePackSourcesExtractorDialog.FORCE_RP_NAME = rpName;
		
		File worldFolder = new File(world);
		if(!(worldFolder.exists())) {
			boolean foundFile = false;
			for(Launcher launcher : LauncherRegistry.getLaunchers()) {
				for(MinecraftSave save : launcher.getSaves()) {
					if(save.getLabel().equals(world)) {
						worldFolder = save.getWorldFolder();
						foundFile = true;
						break;
					}
				}
				if(foundFile)
					break;
			}
		}
		
		MCWorldExporter.getApp().setWorld(worldFolder, worldFolder.getName(), null);
		
		return null;
	}

}
