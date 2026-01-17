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

package nl.bramstout.mcworldexporter.world.bedrock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import nl.bramstout.mcworldexporter.export.IndexCache;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class WorldBedrock extends World{

	private AtomicInteger regionId;
	private IndexCache regionIdCache;
	private Object regionMutex;
	private DB worldDB;
	private Object dbMutex;
	
	public WorldBedrock(File worldDir) {
		super(worldDir);
		this.worldVersion = 0;
		regionId = new AtomicInteger(0);
		regionIdCache = new IndexCache();
		regionMutex = new Object();
		dbMutex = new Object();
		blockConnectionsTranslation = new BlockConnectionsTranslation("bedrock");
		blockConnectionsTranslation.load();
		
		Options options = new Options();
		options.createIfMissing(false);
		options.readOnly(true);
		DBFactory factory = new Iq80DBFactory();
		worldDB = null;
		try {
			worldDB = factory.open(new File(worldDir, "db"), options);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			regionIdCache.clear();
			regionId.set(0);
		}
		synchronized(dbMutex) {
			if(worldDB != null) {
				try {
					worldDB.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			worldDB = null;
		}
	}
	
	@Override
	protected void _unpause() {
		loadWorldSettings();
		findDimensions();
		findRegions();
	}
	
	public static boolean supportsWorld(File worldDir) {
		File dbDir = new File(worldDir, "db");
		return dbDir.exists() && dbDir.isDirectory();
	}

	@Override
	protected void loadWorldSettings() {
		players.clear();
		DB db = getWorldDB();
		if(db == null)
			return;
		
		byte[] localPlayerData = db.get(BedrockUtils.bytes("~local_player"));
		if(localPlayerData != null) {
			try {
				ByteArrayLEDataInputStream dis = new ByteArrayLEDataInputStream(localPlayerData);
				NbtTag tag = NbtTag.readFromStream(dis);
				if(tag instanceof NbtTagCompound)
					players.add(new PlayerBedrock("Local Player", (NbtTagCompound) tag));
				tag.free();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		DBIterator it = db.iterator();
		it.seek(BedrockUtils.bytes("player"));
		while(it.hasNext()) {
			Entry<byte[], byte[]> entry = it.next();
			try {
				if(entry.getKey().length > 6 && 
						entry.getKey()[0] == 'p' && 
						entry.getKey()[1] == 'l' && 
						entry.getKey()[2] == 'a' && 
						entry.getKey()[3] == 'y' && 
						entry.getKey()[4] == 'e' && 
						entry.getKey()[5] == 'r') {
					// A remote player
					ByteArrayLEDataInputStream dis = new ByteArrayLEDataInputStream(entry.getValue());
					NbtTag tag = NbtTag.readFromStream(dis);
					if(!(tag instanceof NbtTagCompound))
						continue;
					String playerUUID = new String(entry.getKey());
					if(playerUUID.startsWith("player_server_"))
						playerUUID = playerUUID.substring(14);
					if(playerUUID.startsWith("player_"))
						playerUUID = playerUUID.substring(7);
					NbtTag posTag = ((NbtTagCompound) tag).get("Pos");
					if(posTag == null)
						continue;
					players.add(new PlayerBedrock(playerUUID, (NbtTagCompound) tag));
				}else {
					break;
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	protected void findDimensions() {
		dimensions.clear();
		dimensions.add("overworld");
		dimensions.add("the_nether");
		dimensions.add("the_end");
	}
	
	public int getCurrentDimensionId() {
		if(currentDimension.equals("overworld"))
			return 0;
		else if(currentDimension.equals("the_nether"))
			return 1;
		else if(currentDimension.equals("the_end"))
			return 2;
		return -1;
	}

	@Override
	protected void findRegions() {
		synchronized(regionMutex) {
			regions = new Region[16];
		}
	}
	
	/*@Override
	protected void loadEntities() {
		if(worldDB == null)
			return;
		entities.clear();
		DBIterator iter = worldDB.iterator();
		byte[] prefix = "actorprefix".getBytes();
		iter.seek(prefix);
		while(iter.hasNext()) {
			Entry<byte[], byte[]> entry = iter.next();
			if(entry.getKey().length <= prefix.length)
				continue;
			boolean hasPrefix = true;
			for(int i = 0; i < prefix.length; ++i) {
				if(entry.getKey()[i] != prefix[i]) {
					hasPrefix = false;
					break;
				}
			}
			if(!hasPrefix)
				continue;
			
			ByteArrayDataInputStream dis = new ByteArrayDataInputStream(entry.getValue());
			try {
				NbtTag dataTag = NbtTag.readFromStream(dis);
				if(!(dataTag instanceof NbtTagCompound))
					continue;
				
				NbtTagCompound data = (NbtTagCompound) dataTag;
				
				NbtTagString identifierTag = (NbtTagString) data.get("identifier");
				if(identifierTag == null)
					continue;
				
				Entity entity = EntityRegistry.getEntity(identifierTag.value, data);
				if(entity == null)
					continue;
				
				entities.add(entity);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}*/
	
	@Override
	public Region getRegion(int chunkX, int chunkZ) {
		if(paused)
			return null;
		if (regions == null)
			return null;

		chunkX >>= 5;
		chunkZ >>= 5;
		
		int id = regionIdCache.getOrDefault((Integer.toUnsignedLong(chunkX) << 32) | Integer.toUnsignedLong(chunkZ), -1);
		if(id < 0 || id >= regions.length) {
			synchronized(regionMutex) {
				id = regionIdCache.getOrDefault((Integer.toUnsignedLong(chunkX) << 32) | Integer.toUnsignedLong(chunkZ), -1);
				if(id >= 0 && id < regions.length)
					return regions[id];
				
				id = regionId.getAndIncrement();
				regionIdCache.put((Integer.toUnsignedLong(chunkX) << 32) | Integer.toUnsignedLong(chunkZ), id);
				if(id >= regions.length)
					regions = Arrays.copyOf(regions, regions.length + 16);
				regions[id] = new RegionBedrock(this, new File(worldDir, "db"), chunkX, chunkZ, getCurrentDimensionId());
				return regions[id];
			}
		}
		return regions[id];
	}
	
	public DB getWorldDB() {
		if(paused)
			return null;
		if(worldDB == null) {
			synchronized(dbMutex) {
				if(worldDB != null)
					return worldDB;
				Options options = new Options();
				options.createIfMissing(false);
				options.readOnly(true);
				DBFactory factory = new Iq80DBFactory();
				try {
					worldDB = factory.open(new File(worldDir, "db"), options);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return worldDB;
	}

	@Override
	protected void _unload() {
		if(worldDB != null) {
			try {
				worldDB.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			worldDB = null;
		}
	}

}
