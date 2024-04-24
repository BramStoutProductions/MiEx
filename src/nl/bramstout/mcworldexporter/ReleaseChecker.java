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
	
	public static final String CURRENT_VERSION = "v1.2.0";
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
