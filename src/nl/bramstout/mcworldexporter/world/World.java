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
import java.util.List;

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

	public World(File worldDir) {
		dimensions = new ArrayList<String>();
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
		for(Region region : regions) {
			if(region == null)
				continue;
			region.unload();
		}
		_unload();
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
			e.printStackTrace();
		}
		return 0;
	}

	public int getBiomeId(int blockX, int blockY, int blockZ) {
		try {
			Chunk chunk = getChunkFromBlockPosition(blockX, blockZ);
			if (chunk != null)
				return chunk.getBiomeId(blockX, blockY, blockZ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getHeight(int blockX, int blockZ) {
		try {
			Chunk chunk = getChunkFromBlockPosition(blockX, blockZ);
			if (chunk != null)
				return chunk.getHeight(blockX, blockZ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Integer.MIN_VALUE;
	}

	public void setWorldDir(File worldDir) {
		this.worldDir = worldDir;
		this.dimensions.clear();
		this.currentDimension = "";
		findDimensions();
		String newDimension = "";
		if (!dimensions.isEmpty())
			newDimension = dimensions.get(0);

		loadWorldSettings();

		loadDimension(newDimension);
	}

	public void loadDimension(String dimension) {
		if (!this.dimensions.contains(dimension))
			return;
		if (this.currentDimension.equals(dimension))
			return;
		this.currentDimension = dimension;

		BlockRegistry.clearBlockRegistry();

		findRegions();

		MCWorldExporter.getApp().getUI().setTitle(this.worldDir.getName() + " - " + dimension);
		MCWorldExporter.getApp().getUI().update();
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

}
