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

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.world.Player;
import nl.bramstout.mcworldexporter.world.World;

public class CommandGetWorld extends Command{

	@Override
	public JsonObject run(JsonObject command) {
		World world = MCWorldExporter.getApp().getWorld();
		
		JsonObject worldObj = new JsonObject();
		
		if(world != null) {
			worldObj.addProperty("path", world.getWorldDir().getPath());
			worldObj.addProperty("dimension", world.getCurrentDimensions());
			JsonArray dimensions = new JsonArray();
			for(String dimension : world.getDimensions()) {
				dimensions.add(dimension);
			}
			worldObj.add("dimensions", dimensions);
			worldObj.addProperty("version", world.getWorldVersion());
			JsonArray worldBounds = new JsonArray();
			worldBounds.add(world.getRegionMinX() * 16 * 32);
			worldBounds.add(world.getRegionMinZ() * 16 * 32);
			worldBounds.add(world.getRegionMaxX() * 16 * 32);
			worldBounds.add(world.getRegionMaxZ() * 16 * 32);
			worldObj.add("worldBounds", worldBounds);
			
			JsonArray players = new JsonArray();
			for(Player player : world.getPlayers()) {
				JsonObject playerObj = new JsonObject();
				playerObj.addProperty("uuid", player.getUuid());
				playerObj.addProperty("name", player.getName());
				playerObj.addProperty("dimension", player.getDimension());
				playerObj.addProperty("x", player.getX());
				playerObj.addProperty("y", player.getY());
				playerObj.addProperty("z", player.getZ());
				playerObj.add("data", player.getData().asJson());
				players.add(playerObj);
			}
			worldObj.add("players", players);
		}
		
		JsonObject res = new JsonObject();
		res.add("world", worldObj);
		return res;
	}

}
