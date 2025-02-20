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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation;

public abstract class World {

	protected File worldDir;
	protected List<String> dimensions;
	protected String currentDimension;
	protected Region[] regions;
	protected int regionMinX;
	protected int regionMinZ;
	protected int regionMaxX;
	protected int regionMaxZ;
	protected int regionsStride;
	protected List<Player> players;
	protected BlockConnectionsTranslation blockConnectionsTranslation;
	protected int worldVersion;
	
	protected boolean paused;

	public World(File worldDir) {
		dimensions = new ArrayList<String>();
		players = new ArrayList<Player>();
		currentDimension = "";
		regions = null;
		blockConnectionsTranslation = null;
		worldVersion = 0;
		paused = false;
	}

	protected abstract void loadWorldSettings();

	protected abstract void findDimensions();

	protected abstract void findRegions();
	
	protected abstract void _unload();
	
	protected abstract void _pause();
	
	protected abstract void _unpause();
	
	public void reloadFromResourcepack() {
	}
	
	public void pauseLoading() {
		if(!paused) {
			paused = true;
			_pause();
		}
	}
	
	public void unpauseLoading() {
		if(paused) {
			paused = false;
			_unpause();
			forceReRender();
			MCWorldExporter.getApp().getUI().update();
			MCWorldExporter.getApp().getUI().fullReRender();
		}
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public void unload() {
		if(regions != null) {
			for(Region region : regions) {
				if(region == null)
					continue;
				try {
					region.unload();
				}catch(Exception ex) {
					handleError(ex);
				}
			}
		}
		if(players != null)
			players.clear();
		try {
			_unload();
		}catch(Exception ex) {
			handleError(ex);
		}
	}
	
	public void unloadEntities() {
		if(regions != null) {
			for(Region region : regions) {
				if(region == null)
					continue;
				try {
					region.unloadEntities();
				}catch(Exception ex) {
					handleError(ex);
				}
			}
		}
	}

	public Region getRegion(int chunkX, int chunkZ) {
		if(paused)
			return null;
		
		if (regions == null)
			return null;

		chunkX >>= 5;
		chunkZ >>= 5;
		
		if(chunkX < regionMinX || chunkZ < regionMinZ || chunkX > regionMaxX || chunkZ > regionMaxZ)
			return null;

		int id = (chunkZ - regionMinZ) * regionsStride + (chunkX - regionMinX);

		if(id < 0 || id >= regions.length)
			return null;
		return regions[id];
	}

	public Chunk getChunk(int chunkX, int chunkZ) throws Exception {
		if(paused)
			return null;
		Region region = getRegion(chunkX, chunkZ);
		if (region == null)
			return null;
		return region.getChunk(chunkX, chunkZ);
	}

	public Chunk getChunkFromBlockPosition(int blockX, int blockZ) throws Exception {
		if(paused)
			return null;
		
		if (blockX < 0)
			blockX -= 15;
		if (blockZ < 0)
			blockZ -= 15;
		blockX /= 16;
		blockZ /= 16;
		Region region = getRegion(blockX, blockZ);
		if (region == null)
			return null;
		return region.getChunk(blockX, blockZ);
	}

	public int getBlockId(int blockX, int blockY, int blockZ) {
		if(paused)
			return 0;
		
		try {
			Chunk chunk = getChunkFromBlockPosition(blockX, blockZ);
			if (chunk != null)
				return chunk.getBlockId(blockX, blockY, blockZ);
		} catch (Exception e) {
			handleError(e);
		}
		return 0;
	}

	public int getBiomeId(int blockX, int blockY, int blockZ) {
		if(paused)
			return 0;
		try {
			Chunk chunk = getChunkFromBlockPosition(blockX, blockZ);
			if (chunk != null)
				return chunk.getBiomeId(blockX, blockY, blockZ);
		} catch (Exception e) {
			handleError(e);
		}
		return 0;
	}

	public int getHeight(int blockX, int blockZ) {
		if(paused)
			return Integer.MIN_VALUE;
		try {
			Chunk chunk = getChunkFromBlockPosition(blockX, blockZ);
			if (chunk != null)
				return chunk.getHeight(blockX, blockZ);
		} catch (Exception e) {
			handleError(e);
		}
		return Integer.MIN_VALUE;
	}

	public void setWorldDir(File worldDir) {
		this.paused = false;
		this.worldDir = worldDir;
		this.dimensions.clear();
		this.currentDimension = "";
		handledErrors.clear();
		try {
			findDimensions();
			String newDimension = "";
			if (!dimensions.isEmpty())
				newDimension = dimensions.get(0);
	
			loadWorldSettings();
	
			loadDimension(newDimension);
		}catch(Exception ex) {
			handleError(ex);
		}
		MCWorldExporter.getApp().getUI().update();
	}

	public void loadDimension(String dimension) {
		if (!this.dimensions.contains(dimension))
			return;
		if (this.currentDimension.equals(dimension))
			return;
		MCWorldExporter.getApp().getUI().getEntityDialog().noDefaultSelection = false;
		this.currentDimension = dimension;
		this.paused = false;
		
		BlockRegistry.clearBlockRegistry();
		
		if(regions != null) {
			for(Region region : regions) {
				if(region == null)
					continue;
				try {
					region.unload();
				}catch(Exception ex) {
					handleError(ex);
				}
			}
		}

		findRegions();

		MCWorldExporter.getApp().getUI().reset();
		MCWorldExporter.getApp().getUI().setTitle(this.worldDir.getName() + " - " + dimension);
		MCWorldExporter.getApp().getUI().update();
		MCWorldExporter.getApp().getUI().fullReRender();
	}

	public void forceReRender() {
		if(paused)
			return;
		if (regions != null) {
			for (Region region : regions) {
				if(region != null)
					region.forceReRender();
			}
		}
	}

	public File getWorldDir() {
		return worldDir;
	}

	public List<String> getDimensions() {
		return dimensions;
	}

	public String getCurrentDimensions() {
		return currentDimension;
	}
	
	public List<Player> getPlayers(){
		return players;
	}
	
	public int getWorldVersion() {
		return worldVersion;
	}
	
	public int getRegionMinX() {
		return regionMinX;
	}
	
	public int getRegionMinZ() {
		return regionMinZ;
	}
	
	public int getRegionMaxX() {
		return regionMaxX;
	}
	
	public int getRegionMaxZ() {
		return regionMaxZ;
	}
	
	public BlockConnectionsTranslation getBlockConnectionsTranslation() {
		return blockConnectionsTranslation;
	}
	
	public List<List<Entity>> getEntitiesInRegion(int minX, int minZ, int maxX, int maxZ){
		int chunkMinX = minX >> 4;
		int chunkMinZ = minZ >> 4;
		int chunkMaxX = maxX >> 4;
		int chunkMaxZ = maxZ >> 4;
		List<List<Entity>> res = new ArrayList<List<Entity>>();
		if(paused)
			return res;
		for(int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; ++chunkZ) {
			for(int chunkX = chunkMinX; chunkX <= chunkMaxX; ++chunkX) {
				try {
					Chunk chunk = getChunk(chunkX, chunkZ);
					if(chunk == null)
						continue;
					List<Entity> entities = chunk.getEntities();
					if(entities == null || entities.isEmpty())
						continue;
					res.add(entities);
				}catch(Exception ex) {}
			}
		}
		return res;
	}
	
	public List<List<Entity>> getEntitiesInRange(int x, int z, int radius){
		return getEntitiesInRegion(x - radius, z - radius, x + radius, z + radius);
	}
	
	public List<List<Entity>> getEntitiesInRegion(ExportBounds bounds){
		return getEntitiesInRegion(bounds.getMinX(), bounds.getMinZ(), bounds.getMaxX(), bounds.getMaxZ());
	}
	
	private static Set<String> handledErrors = new HashSet<String>();
	
	public static void handleError(Exception ex) {
		// It's possible that we send in a whole bunch of the same
		// exception, but we don't want to spam the user.
		// So we make sure to only show it once.
		
		// We need some identifier for each exception.
		// In most cases .toString() will work just fine, but
		// in some cases you could have the same error, but with
		// some detail different. We don't want to then still
		// spam the user, so in that case, we can check the top
		// stack frame, which should be common among all of those
		// duplicate errors.
		String exceptionId = ex.toString();
		if(ex.getStackTrace().length > 0)
			exceptionId = ex.getStackTrace()[0].toString();
		
		synchronized(handledErrors) {
			if(handledErrors.contains(exceptionId))
				return;
			handledErrors.add(exceptionId);
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), ex.getLocalizedMessage() == null ? "An error occured loading the world." : ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
			
		});
		// Print is to out, because if we print it to err then we'd get another popup.
		ex.printStackTrace(System.out);
	}

}
