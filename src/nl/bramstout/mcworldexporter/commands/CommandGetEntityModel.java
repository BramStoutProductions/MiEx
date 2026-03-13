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

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class CommandGetEntityModel extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		JsonArray entityModels = new JsonArray();
		
		
		if(command.has("entities")) {
			for(JsonElement el : command.getAsJsonArray("entities")) {
				String id = "";
				NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("");
				
				if(el.isJsonObject()) {
					JsonObject elObj = el.getAsJsonObject();
					if(elObj.has("id"))
						id = elObj.get("id").getAsString();
					if(elObj.has("data"))
						properties = (NbtTagCompound) NbtTag.fromJsonValue(elObj.getAsJsonObject("data"));
				}else if(el.isJsonPrimitive()) {
					id = el.getAsString();
				}
				
				Entity entity = EntityRegistry.getEntity(id, properties);
				
				Model model = entity.getModel();
				
				JsonObject entityObj = new JsonObject();
				entityObj.addProperty("id", id);
				entityObj.addProperty("x", entity.getX());
				entityObj.addProperty("y", entity.getY());
				entityObj.addProperty("z", entity.getZ());
				entityObj.addProperty("pitch", entity.getPitch());
				entityObj.addProperty("yaw", entity.getYaw());
				entityObj.addProperty("headPitch", entity.getHeadPitch());
				entityObj.addProperty("headYaw", entity.getHeadYaw());
				entityObj.add("data", entity.getProperties().asJson());
				entityObj.add("model", model.toJson());
				entityModels.add(entityObj);
			}
		}
		
		
		JsonObject res = new JsonObject();
		res.add("entities", entityModels);
		return res;
	}

}
