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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
	
	private static Map<String, Class<? extends Command>> registry = new HashMap<String, Class<? extends Command>>();
	
	static {
		registerCommand("applyExportSettings", CommandApplyExportSettings.class);
		registerCommand("createAtlases", CommandCreateAtlases.class);
		registerCommand("downloadExampleResourcePacks", CommandDownloadExampleResourcePacks.class);
		registerCommand("executeFile", CommandExecuteFile.class);
		registerCommand("export", CommandExport.class);
		registerCommand("extractModResourcePack", CommandExtractModResourcePack.class);
		registerCommand("getActiveResourcePacks", CommandGetActiveResourcePacks.class);
		registerCommand("getBakedBlockstate", CommandGetBakedBlockState.class);
		registerCommand("getBiome", CommandGetBiome.class);
		registerCommand("getBlock", CommandGetBlock.class);
		registerCommand("getChunk", CommandGetChunk.class);
		registerCommand("getEntityModel", CommandGetEntityModel.class);
		registerCommand("getEnvironmentSettings", CommandGetEnvironmentSettings.class);
		registerCommand("getExampleResourcePacks", CommandGetExampleResourcePacks.class);
		registerCommand("getExportSettings", CommandGetExportSettings.class);
		registerCommand("getHytaleVersions", CommandGetHytaleVersions.class);
		registerCommand("getItemModel", CommandGetItemModel.class);
		registerCommand("getMinecraftVersions", CommandGetMinecraftVersions.class);
		registerCommand("getModel", CommandGetModel.class);
		registerCommand("getResourcePacks", CommandGetResourcePacks.class);
		registerCommand("getWorld", CommandGetWorld.class);
		registerCommand("getWorlds", CommandGetWorlds.class);
		registerCommand("loadDimension", CommandLoadDimension.class);
		registerCommand("loadWorld", CommandLoadWorld.class);
		registerCommand("pbrGenerator", CommandPbrGenerator.class);
		registerCommand("quit", CommandQuit.class);
		registerCommand("reloadResourcePacks", CommandReloadResourcePacks.class);
		registerCommand("resolveTags", CommandResolveTags.class);
		registerCommand("setEnvironmentSettings", CommandSetEnvironmentSettings.class);
		registerCommand("setActiveResourcePacks", CommandSetActiveResourcePacks.class);
		registerCommand("updateBaseResourcePack", CommandUpdateBaseResourcePack.class);
		registerCommand("updateBaseResourcePackHytale", CommandUpdateBaseResourcePackHytale.class);
		registerCommand("updateBuiltInFiles", CommandUpdateBuiltInFiles.class);
	}
	
	public static void registerCommand(String commandType, Class<? extends Command> command) {
		registry.put(commandType, command);
	}
	
	public static Command createCommand(String commandType) {
		Class<? extends Command> commandClass = registry.getOrDefault(commandType, null);
		if(commandClass == null)
			return null;
		
		try {
			Constructor<? extends Command> commandCtor = commandClass.getConstructor();
			return commandCtor.newInstance();
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
