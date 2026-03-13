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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Environment;
import nl.bramstout.mcworldexporter.Environment.EnvironmentVariable;

public class CommandGetEnvironmentSettings extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		boolean onlySetInEnvFile = false;
		if(command.has("onlySetInEnvFile"))
			onlySetInEnvFile = command.get("onlySetInEnvFile").getAsBoolean();
		boolean includeDefaults = false;
		if(command.has("includeDefaults"))
			includeDefaults = command.get("includeDefaults").getAsBoolean();
		
		JsonObject settings = new JsonObject();
		
		for(EnvironmentVariable envVar : Environment.ENVIRONMENT_VARIABLES) {
			try {
				if(onlySetInEnvFile && !Environment.hasEditedInEnvFile(envVar.getName()))
					continue;
				
				String value = Environment.getEnv(envVar.getName());
				if(value == null) {
					if(includeDefaults)
						value = envVar.getDefaultValue();
					else
						continue;
				}
				
				JsonElement jsonValue = null;
				switch(envVar.getType()) {
				case BOOLEAN:
					jsonValue = new JsonPrimitive(value.toLowerCase().startsWith("t") || value.toLowerCase().startsWith("1"));
					break;
				case FLOAT:
					jsonValue = new JsonPrimitive(Float.parseFloat(value));
					break;
				case INTEGER:
					jsonValue = new JsonPrimitive(Integer.parseInt(value));
					break;
				case STRING:
				case FILE:
				case FOLDER:
					jsonValue = new JsonPrimitive(value);
					break;
				case FILE_ARRAY:
				case FOLDER_ARRAY:
				case STRING_ARRAY:
					jsonValue = new JsonArray();
					for(String item : value.split(";")) {
						((JsonArray)jsonValue).add(item);
					}
					break;
				}
				
				settings.add(envVar.getName(), jsonValue);
			}catch(Exception ex) {}
		}
		
		JsonObject res = new JsonObject();
		res.add("settings", settings);
		return res;
	}

}
