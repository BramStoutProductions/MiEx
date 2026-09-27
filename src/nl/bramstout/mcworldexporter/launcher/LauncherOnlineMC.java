package nl.bramstout.mcworldexporter.launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.parallel.Async.AsyncGroup;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.world.World;

public class LauncherOnlineMC extends Launcher{

	@Override
	public String getName() {
		return "MC";
	}
	
	public static class MinecraftVersionOnline extends MinecraftVersion{

		private URL url;
		
		public MinecraftVersionOnline(String label, URL jarFile, Date releaseTime) {
			super(label, jarFile, releaseTime);
			this.url = jarFile;
		}
		
		@Override
		public URL getJarFile() {
			try {
				JsonElement rootData = Json.read(url, false);
				if(rootData.isJsonObject() && rootData.getAsJsonObject().has("downloads")) {
					JsonObject downloadsData = rootData.getAsJsonObject().getAsJsonObject("downloads");
					if(downloadsData.has("client")) {
						JsonObject clientData = downloadsData.getAsJsonObject("client");
						if(clientData.has("url")) {
							return new URI(clientData.get("url").getAsString()).toURL();
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			try {
				return new URI("").toURL();
			} catch (MalformedURLException | URISyntaxException e) {
				e.printStackTrace();
			}
			return null;
		}
		
	}
	
	private List<MinecraftVersion> versions = null;

	@Override
	public List<MinecraftVersion> getVersions() {
		// Minecraft versions can be downloaded straight from Mojang's servers.
		// This launcher will do that so that MiEx can work without the need
		// of having Minecraft installed.
		// All versions are stored in https://piston-meta.mojang.com/mc/game/version_manifest.json
		if(versions != null)
			return versions;
		
		versions = new ArrayList<MinecraftVersion>();
		if(MCWorldExporter.offlineMode)
			return versions;
		
		try {
			JsonElement rootTree = Json.read(new URI("https://piston-meta.mojang.com/mc/game/version_manifest.json").toURL(), false);
			if(rootTree.isJsonObject() && rootTree.getAsJsonObject().has("versions")) {
				JsonArray versionsData = rootTree.getAsJsonObject().get("versions").getAsJsonArray();
				boolean hasSnapshot = false;
				for(JsonElement versionData : versionsData) {
					if(!versionData.isJsonObject() || 
							!versionData.getAsJsonObject().has("id") || 
							!versionData.getAsJsonObject().has("url"))
						continue;
					// Only show the latest snapshot. Otherwise we're going to end up with a lot of snapshots.
					if(versionData.getAsJsonObject().has("type") && versionData.getAsJsonObject().get("type").getAsString().equals("snapshot")) {
						if(hasSnapshot)
							continue;
						hasSnapshot = true;
					}
					versions.add(new MinecraftVersionOnline(
							versionData.getAsJsonObject().get("id").getAsString(), 
							new URI(versionData.getAsJsonObject().get("url").getAsString()).toURL(), 
							Util.parseDateTime(versionData.getAsJsonObject().get("releaseTime").getAsString())));
				}
			}
		}catch(Exception ex) {
		}
		
		return versions;
	}

	@Override
	public List<MinecraftSave> getSaves() {
		return null;
	}

	@Override
	public List<ResourcePackSource> getResourcePackSourcesForWorld(World world) {
		return null;
	}

	@Override
	public void getAllResourcePackSources(ResourcePackSourceCollector collector, AsyncGroup asyncGroup) {
	}

	@Override
	public boolean ownsWorld(File worldFolder) {
		return false;
	}

	@Override
	public boolean hasVersions() {
		return true;
	}

	@Override
	public boolean hasSaves() {
		return false;
	}

	@Override
	public boolean hasResourcePacks() {
		return false;
	}

}
