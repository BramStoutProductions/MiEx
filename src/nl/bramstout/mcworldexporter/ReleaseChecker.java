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

package nl.bramstout.mcworldexporter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class ReleaseChecker {
	
	public static final String CURRENT_VERSION = "v1.5.1";
	public static String LATEST_VERSION = CURRENT_VERSION;
	public static String LATEST_VERSION_URL = "https://github.com/BramStoutProductions/MiEx/releases";
	
	public static void checkRelease(){
		HttpURLConnection connection = null;
		InputStream stream = null;
		try {
			URL url = new URL("https://api.github.com/repos/BramStoutProductions/MiEx/releases");
			connection = (HttpURLConnection) url.openConnection();
			stream = connection.getInputStream();
			
			JsonArray releases = JsonParser.parseReader(new JsonReader(new BufferedReader(new InputStreamReader(stream)))).getAsJsonArray();
			for(JsonElement el : releases.asList()) {
				JsonObject release = el.getAsJsonObject();
				if(release.has("tag_name")) {
					LATEST_VERSION = release.get("tag_name").getAsString();
					
					if(release.has("html_url")) {
						LATEST_VERSION_URL = release.get("html_url").getAsString();
					}
					break;
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
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
	
	public static boolean hasNewRelease() {
		return !LATEST_VERSION.equalsIgnoreCase(CURRENT_VERSION);
	}
	
}
