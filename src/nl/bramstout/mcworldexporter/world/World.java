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

import nl.bramstout.mcworldexporter.MCWorldExporter;

public abstract class World {

	protected File worldDir;
	protected List<String> dimensions;
	protected String currentDimension;
	// protected Map<Long, Region> regions;
	protected Region[] regions;
	protected int regionMinX;
	protected int regionMinZ;
	protected int regionMaxX;
	protected int regionMaxZ;
	protected int regionsStride;
	protected List<Player> players;

	public World(File worldDir) {
		dimensions = new ArrayList<String>();
		players = new ArrayList<Player>();
		currentDimension = "";
		// regions = new HashMap<Long, Region>();
		regions = null;
		setWorldDir(worldDir);
	}

	protected abstract void loadWorldSettings();

	protected abstract void findDimensions();

	protected abstract void findRegions();
	
	protected abstract void _unload();
	
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

	// protected abstract long getRegionIdFromChunkPosition(int chunkX, int chunkZ);

	public Region getRegion(int chunkX, int chunkZ) {
		// long id = getRegionIdFromChunkPosition(chunkX, chunkZ);
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
		Region region = getRegion(chunkX, chunkZ);
		if (region == null)
			return null;
		return region.getChunk(chunkX, chunkZ);
	}

	public Chunk getChunkFromBlockPosition(int blockX, int blockZ) throws Exception {
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
	}

	public void loadDimension(String dimension) {
		if (!this.dimensions.contains(dimension))
			return;
		if (this.currentDimension.equals(dimension))
			return;
		this.currentDimension = dimension;

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

		MCWorldExporter.getApp().getUI().setTitle(this.worldDir.getName() + " - " + dimension);
		MCWorldExporter.getApp().getUI().update();
		MCWorldExporter.getApp().getUI().fullReRender();
	}

	public void forceReRender() {
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
