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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.launcher.HytaleVersion;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherHytale;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;

public class CommandGetHytaleVersions extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		JsonObject res = new JsonObject();
		
		JsonArray array = new JsonArray();
		for(Launcher launcher : LauncherRegistry.getLaunchers()) {
			if(launcher instanceof LauncherHytale) {
				for(HytaleVersion version : ((LauncherHytale)launcher).getHytaleVersions()) {
					JsonObject versionObj = new JsonObject();
					versionObj.addProperty("name", version.getLabel());
					versionObj.addProperty("assetsFile", version.getAssetsFile().getPath());
					versionObj.addProperty("launcher", launcher.getName());
					array.add(versionObj);
				}
			}
		}
		res.add("versions", array);
		
		return res;
	}

}
