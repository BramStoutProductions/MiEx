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
