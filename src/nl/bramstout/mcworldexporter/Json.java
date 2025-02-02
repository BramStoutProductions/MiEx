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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class Json {
	
	public static JsonElement read(URL url, boolean printError) {
		HttpURLConnection connection = null;
		InputStream stream = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			stream = connection.getInputStream();
			
			JsonElement data = JsonParser.parseReader(new JsonReader(new BufferedReader(new InputStreamReader(stream))));
			try {
				stream.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			try {
				connection.disconnect();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return data;
		}catch(Exception ex) {
			if(printError)
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
		
		return null;
	}
	
	public static JsonElement read(File file) {
		JsonReader reader = null;
		try {
			reader = new JsonReader(new BufferedReader(new FileReader(file)));
			reader.setLenient(true);
			return JsonParser.parseReader(reader);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(reader != null) {
			try {
				reader.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	public static JsonElement readString(String data) {
		JsonReader reader = null;
		try {
			reader = new JsonReader(new StringReader(data));
			reader.setLenient(true);
			return JsonParser.parseReader(reader);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(reader != null) {
			try {
				reader.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	public static void writeJson(File file, JsonElement data) {
		FileWriter writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			String jsonString = gson.toJson(data);
			writer = new FileWriter(file);
			writer.write(jsonString);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(writer != null) {
			try {
				writer.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
