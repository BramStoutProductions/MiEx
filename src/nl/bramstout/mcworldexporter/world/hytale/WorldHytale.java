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

package nl.bramstout.mcworldexporter.world.hytale;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.export.IndexCache;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation;
import nl.bramstout.mcworldexporter.world.Player;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class WorldHytale extends World{

	private Map<String, String> dimensionNameToFolder = new HashMap<String, String>();
	private Object regionMutex;
	
	public WorldHytale(File worldDir, String name, Launcher launcher) {
		super(worldDir, name, launcher);
		regionMutex = new Object();
		blockConnectionsTranslation = new BlockConnectionsTranslation("hytale");
		blockConnectionsTranslation.load();
	}
	
	public static boolean supportsWorld(File worldDir) {
		File worldsFolder = new File(worldDir, "universe/worlds");
		return worldsFolder.exists() && worldsFolder.isDirectory();
	}

	@Override
	protected void loadWorldSettings() {
		players.clear();
		
		File playersFolder = new File(worldDir, "universe/players");
		if(playersFolder.exists() && playersFolder.isDirectory()) {
			for(File playerFile : playersFolder.listFiles()) {
				if(!playerFile.getName().endsWith(".json"))
					continue;
				
				JsonObject data = Json.read(playerFile).getAsJsonObject();
				if(!data.has("Components"))
					continue;
				data = data.getAsJsonObject("Components");
				
				// Get rid of .json at the end
				String playerUuid = playerFile.getName().substring(0, playerFile.getName().length()-4);
				String playerName = playerUuid;
				String playerDimension = "";
				double playerX = 0f;
				double playerY = 0f;
				double playerZ = 0f;
				
				if(data.has("Nameplate")) {
					JsonObject nameplate = data.getAsJsonObject("Nameplate");
					if(nameplate.has("Text"))
						playerName = nameplate.get("Text").getAsString();
				}
				if(data.has("DisplayName")) {
					JsonObject displayName = data.getAsJsonObject("DisplayName");
					if(displayName.has("DisplayName"))
						displayName = displayName.getAsJsonObject("DisplayName");
					if(displayName.has("RawText"))
						playerName = displayName.get("RawText").getAsString();
				}
				
				if(data.has("Transform")) {
					JsonObject transform = data.getAsJsonObject("Transform");
					if(transform.has("Position")) {
						JsonObject position = transform.getAsJsonObject("Position");
						if(position.has("X"))
							playerX = position.get("X").getAsDouble();
						if(position.has("Y"))
							playerY = position.get("Y").getAsDouble();
						if(position.has("Z"))
							playerZ = position.get("Z").getAsDouble();
					}
				}
				
				if(data.has("Player")) {
					JsonObject player = data.getAsJsonObject("Player");
					if(player.has("PlayerData")) {
						JsonObject playerData = player.getAsJsonObject("PlayerData");
						
						if(playerData.has("World")) {
							playerDimension = playerData.get("World").getAsString();
						}
						
						if(playerData.has("PerWorldData")) {
							JsonObject perWorldData = playerData.getAsJsonObject("PerWorldData");
							if(perWorldData.has(playerDimension)) {
								JsonObject worldData = perWorldData.getAsJsonObject(playerDimension);
								
								if(worldData.has("LastPosition")) {
									JsonObject lastPosition = worldData.getAsJsonObject("LastPosition");
									if(lastPosition.has("X"))
										playerX = lastPosition.get("X").getAsDouble();
									if(lastPosition.has("Y"))
										playerY = lastPosition.get("Y").getAsDouble();
									if(lastPosition.has("Z"))
										playerZ = lastPosition.get("Z").getAsDouble();
								}
							}
						}
					}
				}
				
				if(playerDimension.isEmpty())
					continue;
				
				players.add(new Player(playerUuid, playerName, null, playerX, playerY, playerZ, playerDimension, paused));
			}
		}
	}

	@Override
	protected void findDimensions() {
		this.dimensions.clear();
		this.dimensionNameToFolder.clear();
		File worldsFolder = new File(this.worldDir, "universe/worlds");
		if(worldsFolder.exists() && worldsFolder.isDirectory()) {
			for(File f : worldsFolder.listFiles()) {
				try {
					if(f.isDirectory()) {
						File chunksFolder = new File(f, "chunks");
						if(!chunksFolder.exists() || !chunksFolder.isDirectory())
							continue;
						File configFile = new File(f, "config.json");
						if(!configFile.exists())
							continue;
						JsonObject configData = Json.read(configFile).getAsJsonObject();
						
						if(!configData.has("ChunkStorage"))
							continue;
						
						JsonObject chunkStorageData = configData.getAsJsonObject("ChunkStorage");
						if(!chunkStorageData.has("Type"))
							continue;
						String chunkStorageType = chunkStorageData.get("Type").getAsString();
						if(!chunkStorageType.equals("Hytale"))
							continue;
						
						String displayName = f.getName();
						if(configData.has("DisplayName"))
							displayName = configData.get("DisplayName").getAsString();
						String origDisplayName = displayName;
						int counter = 2;
						while(this.dimensionNameToFolder.containsKey(displayName)) {
							displayName = origDisplayName + "_" + Integer.toString(counter);
							counter++;
						}
						
						this.dimensions.add(displayName);
						this.dimensionNameToFolder.put(displayName, f.getName());
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void findRegions() {
		regions = null;
		regionsIndices = null;
		String worldFolderName = dimensionNameToFolder.getOrDefault(this.currentDimension, null);
		if(worldFolderName == null)
			return;
		File regionFolder = new File(worldDir, "universe/worlds/" + worldFolderName + "/chunks");
		if(!regionFolder.exists() || !regionFolder.isDirectory())
			return;
		
		regionMinX = Integer.MAX_VALUE;
		regionMinZ = Integer.MAX_VALUE;
		regionMaxX = Integer.MIN_VALUE;
		regionMaxZ = Integer.MIN_VALUE;
		
		File[] files = regionFolder.listFiles();
		if(files.length <= 0) {
			regions = null;
			regionsIndices = null;
			return;
		}
		
		List<Region> tmpRegions = new ArrayList<Region>(files.length);
		
		for(File f : files) {
			try {
				if(!f.getName().endsWith(".region.bin"))
					continue;
				String[] tokens = f.getName().split("\\.");
				if(tokens.length != 4)
					continue;
				int x = Integer.parseInt(tokens[0]);
				int z = Integer.parseInt(tokens[1]);
				
				regionMinX = Math.min(regionMinX, x);
				regionMinZ = Math.min(regionMinZ, z);
				regionMaxX = Math.max(regionMaxX, x);
				regionMaxZ = Math.max(regionMaxZ, z);
				
				tmpRegions.add(new RegionHytale(this, f, x, z));
			}catch(Exception ex) {}
		}
		
		if(tmpRegions.size() == 0)
			return;
		if((regionMaxX - regionMinX) > 500 || (regionMaxZ - regionMinZ) > 500) {
			regions = new Region[tmpRegions.size()];
			regionsIndices = new IndexCache();
			int i = 0;
			for(Region r : tmpRegions) {
				long lid = ((long) r.getXCoordinate()) << 32 | (((long) r.getZCoordinate()) & 0xFFFFFFFFL);
				regionsIndices.put(lid, i);
				regions[i] = r;
				i++;
			}
		}else{
			regionsStride = regionMaxX - regionMinX + 1;
			regions = new Region[(regionMaxZ - regionMinZ + 1) * regionsStride];
			
			for(Region r : tmpRegions) {
				int id = (r.getZCoordinate() - regionMinZ) * regionsStride + 
							(r.getXCoordinate() - regionMinX);
				regions[id] = r;
			}
		}
	}
	
	public Region getRegion(int chunkX, int chunkZ) {
		if(paused)
			return null;
		
		if (regions == null)
			return null;

		// Hytale chunks are 32x32 rather than MC's 16x16
		chunkX >>= 1;
		chunkZ >>= 1;
		
		chunkX >>= 5;
		chunkZ >>= 5;
		
		if(chunkX < regionMinX || chunkZ < regionMinZ || chunkX > regionMaxX || chunkZ > regionMaxZ)
			return null;
		
		int id = 0;
		if(regionsIndices != null) {
			long lid = ((long) chunkX) << 32 | (((long) chunkZ) & 0xFFFFFFFFL);
			id = regionsIndices.getOrDefault(lid, -1);
		}else {
			id = (chunkZ - regionMinZ) * regionsStride + (chunkX - regionMinX);
		}

		if(id < 0 || id >= regions.length)
			return null;
		return regions[id];
	}

	@Override
	protected void _unload() {
		regions = null;
	}

	@Override
	protected void _pause() {
		synchronized(regionMutex) {
			if(regions != null) {
				for(Region region : regions) {
					if(region == null)
						continue;
					try {
						region.pause();
					}catch(Exception ex) {
						handleError(ex);
					}
				}
			}
			regions = null;
			regionsIndices = null;
		}
	}

	@Override
	protected void _unpause() {
		loadWorldSettings();
		findDimensions();
		findRegions();
	}
	
	@Override
	public List<String> getRequiredResourcePacks() {
		return Arrays.asList("base_resource_pack_hytale");
	}
	
	@Override
	public List<ResourcePackSource> getDependentResourcePacks() {
		List<ResourcePackSource> sources = new ArrayList<ResourcePackSource>();
		
		File modsFolder = new File(getWorldDir(), "mods");
		if(modsFolder.exists()) {
			for(File modFile : modsFolder.listFiles()) {
				if(modFile.isDirectory() && new File(modFile, "manifest.json").exists()) {
					ResourcePackSource source = new ResourcePackSource(modFile.getName());
					source.addSource(ResourcePackSource.getHash(modFile), modFile);
					sources.add(source);
				}else if(modFile.isFile() && modFile.getName().endsWith(".zip")){
					ResourcePackSource source = new ResourcePackSource(modFile.getName());
					source.addSource(ResourcePackSource.getHash(modFile), modFile);
					sources.add(source);
				}
			}
		}
		
		if(launcher != null)
			sources.addAll(launcher.getResourcePackSourcesForWorld(this));
		
		return sources;
	}

}
