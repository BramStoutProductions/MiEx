package nl.bramstout.mcworldexporter.world.bedrock;

import java.io.File;

import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class RegionBedrock extends Region{
	
	private ChunkBedrock[] chunks = null;
	private Object mutex;
	private int dimensionId;

	public RegionBedrock(World world, File regionFile, int x, int z, int dimensionId) {
		super(world, regionFile, x, z);
		this.mutex = new Object();
		this.dimensionId = dimensionId;
	}
	
	@Override
	public void pause() {
		unloadEntities();
		unload();
	}

	@Override
	public void load() throws Exception {
		synchronized(mutex) {
			if(chunks != null)
				return;
			
			chunks = new ChunkBedrock[32*32];
			
			int i = 0;
			int regionXOffset = this.x * 32;
			int regionZOffset = this.z * 32;
			for(int z = 0; z < 32; ++z) {
				for(int x = 0; x < 32; ++x) {
					
					chunks[i] = new ChunkBedrock(this, x + regionXOffset, z + regionZOffset, dimensionId);
					++i;
				}
			}
		}
	}

	@Override
	public void unload() {
		if(chunks != null) {
			for(Chunk chunk : chunks) {
				if(chunk == null)
					continue;
				chunk.unload();
			}
		}
		chunks = null;
	}
	
	@Override
	public void unloadEntities() {
		if(chunks != null) {
			for(Chunk chunk : chunks) {
				if(chunk == null)
					continue;
				chunk.unloadEntities();
			}
		}
	}

	@Override
	public Chunk getChunk(int worldChunkX, int worldChunkZ) throws Exception {
		if(world.isPaused())
			return null;
		if(chunks == null)
			load();
		worldChunkX -= this.x * 32;
		worldChunkZ -= this.z * 32;
		
		return chunks[worldChunkX + worldChunkZ * 32];
	}

	@Override
	public void forceReRender() {
		if(chunks == null)
			return;
		for(Chunk chunk : chunks) {
			if(chunk == null)
				continue;
			chunk.setShouldRender(true);
			chunk.setFullReRender(true);
		}
	}

}
