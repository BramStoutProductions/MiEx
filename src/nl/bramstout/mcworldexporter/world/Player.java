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

package nl.bramstout.mcworldexporter.world;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.nbt.TAG_Compound;

public class Player {
	
	protected String uuid;
	protected String name;
	protected TAG_Compound data;
	protected double x;
	protected double y;
	protected double z;
	protected String dimension;
	
	public Player(String uuid, TAG_Compound data, double x, double y, double z, String dimension) {
		this.uuid = uuid;
		this.name = uuid;
		this.data = data;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		
		// Try to get the player name from the UUID
		HttpURLConnection connection = null;
		InputStream stream = null;
		try {
			URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			connection = (HttpURLConnection) url.openConnection();
			stream = connection.getInputStream();
			
			JsonObject jsonData = JsonParser.parseReader(new JsonReader(new BufferedReader(new InputStreamReader(stream)))).getAsJsonObject();
			if(jsonData.has("name"))
				this.name = jsonData.get("name").getAsString();
		}catch(Exception ex) {
		}
		try {
			if(stream != null)
				stream.close();
		}catch(Exception ex) {}
		try {
			if(connection != null)
				connection.disconnect();
		}catch(Exception ex) {}
	}

	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public TAG_Compound getData() {
		return data;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public String getDimension() {
		return dimension;
	}
	
}
