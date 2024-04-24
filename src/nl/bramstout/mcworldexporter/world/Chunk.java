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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.parallel.Queue;
import nl.bramstout.mcworldexporter.parallel.SpinLock;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public abstract class Chunk {

	private static Queue<Chunk> loadedChunks = new Queue<Chunk>();
	private static Queue<Chunk> loadedChunkImages = new Queue<Chunk>();
	private static final int MAX_LOADED_CHUNKS = 64*64;
	private static final int MAX_LOADED_CHUNK_IMAGES = 128*64;
	
	private static class LoadedChunksGC implements Runnable{
		
		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(1000 * 1);
					
					List<Chunk> putBackIn = new ArrayList<Chunk>();
					Chunk chunk = null;
					while((chunk = loadedChunks.pop()) != null) {
						if((System.currentTimeMillis() - chunk.lastAccess) > 1000) {
							chunk.unload();
						}else {
							putBackIn.add(chunk);
						}
					}
					int reverseCounter = putBackIn.size() - 1;
					for(int i = 0; i < putBackIn.size(); ++i) {
						// Unload the oldest chunks past our max loaded chunk count.
						if(reverseCounter > MAX_LOADED_CHUNKS)
							putBackIn.get(i).unload();
						else
							loadedChunks.push(putBackIn.get(i));
						reverseCounter--;
					}
					
					putBackIn = new ArrayList<Chunk>();
					chunk = null;
					while((chunk = loadedChunkImages.pop()) != null) {
						if((System.currentTimeMillis() - chunk.lastImageAccess) > 1000) {
							chunk.unloadImages();
						}else {
							putBackIn.add(chunk);
						}
					}
					reverseCounter = putBackIn.size() - 1;
					for(int i = 0; i < putBackIn.size(); ++i) {
						// Unload the oldest chunks past our max loaded chunk count.
						if(reverseCounter > MAX_LOADED_CHUNK_IMAGES)
							putBackIn.get(i).unload();
						else
							loadedChunkImages.push(putBackIn.get(i));
						reverseCounter--;
					}
					
					System.gc();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}
	
	static {
		Thread thread = new Thread(new LoadedChunksGC());
		thread.start();
	}

	protected int chunkX;
	protected int chunkZ;
	protected boolean loadError;
	protected long lastAccess;
	protected long lastImageAccess;
	/**
	 * A list of chunk sections. Each chunk section is a list of block ids from the
	 * BlockRegistry. Each chunk section stores 16*16*16 blocks in the YZX order.
	 */
	protected int[][] blocks;
	/**
	 * A list of biome sections. Each biome section is a list of biome ids from the
	 * BiomeRegistry. Each biome section stores 4*4*4 biome ids in the YZX order.
	 */
	protected short[][] biomes;
	/**
	 * The offset needed to be added to the chunk section Y to get the chunk section
	 * index in blocks.
	 */
	protected int chunkSectionOffset;
	/**
	 * A 16*16 array giving the Y coordinate of the top most block to show in the
	 * UI. The array is in ZX order.
	 */
	protected short[] heightMap;

	protected List<Entity> entities;

	protected BufferedImage chunkImg;
	protected BufferedImage chunkImgSmall;
	protected BufferedImage chunkImgSmaller;
	protected BufferedImage chunkImgSmaller2;
	protected BufferedImage chunkImgSmallest;
	protected Color chunkColor;
	private boolean isRendering;
	private boolean shouldRender;
	private boolean renderRequested;
	private boolean fullReRender;

	private int blockRegistryChangeCounter;
	
	private SpinLock loadLock;

	public Chunk(int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.loadError = false;
		this.blocks = null;
		this.biomes = null;
		this.heightMap = null;
		this.entities = new ArrayList<Entity>();
		this.chunkSectionOffset = 0;
		this.chunkImg = null;
		this.chunkImgSmall = null;
		this.chunkImgSmaller = null;
		this.chunkImgSmaller2 = null;
		this.chunkImgSmallest = null;
		this.chunkColor = null;
		this.isRendering = false;
		this.shouldRender = true;
		this.renderRequested = false;
		this.fullReRender = false;
		blockRegistryChangeCounter = BlockRegistry.getChangeCounter();
		this.lastAccess = System.currentTimeMillis();
		this.loadLock = new SpinLock();
	}

	public void load() throws Exception {
		if(this.loadError || this.blocks != null)
			return;
		
		loadLock.aqcuire();
		try {
			_load();
			this.lastAccess = System.currentTimeMillis();
			loadedChunks.push(this);
		} finally {
			loadLock.release();
		}
	}
	
	protected abstract void _load() throws Exception ;

	public abstract void unload();

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	public boolean hasLoadError() {
		return loadError;
	}

	public int getBlockId(int worldX, int worldY, int worldZ) {
		return getBlockIdLocal(worldX - chunkX * 16, worldY, worldZ - chunkZ * 16);
	}

	public int getBlockIdLocal(int x, int y, int z) {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			try {
				load();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (blocks == null)
			return 0;
		int sectionY = (y >> 4) - chunkSectionOffset;
		if (sectionY < 0 || sectionY >= blocks.length)
			return 0;
		if (blocks[sectionY] == null)
			return 0;
		return blocks[sectionY][((y - chunkSectionOffset * 16) & 15) * 16 * 16 + z * 16 + x];
	}

	public int getBiomeId(int worldX, int worldY, int worldZ) {
		return getBiomeIdLocal(worldX - chunkX * 16, worldY, worldZ - chunkZ * 16);
	}

	public int getBiomeIdLocal(int x, int y, int z) {
		this.lastAccess = System.currentTimeMillis();
		if (biomes == null)
			try {
				load();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (biomes == null)
			return 0;
		int sectionY = (y >> 4) - chunkSectionOffset;
		if (sectionY < 0 || sectionY >= blocks.length)
			return 0;
		if (biomes[sectionY] == null)
			return 0;
		x = x / 4;
		y = ((y - chunkSectionOffset * 16) & 15) / 4;
		z = z / 4;
		return biomes[sectionY][y * 4 * 4 + z * 4 + x];
	}

	public int getHeight(int worldX, int worldZ) {
		return getHeightLocal(worldX - chunkX * 16, worldZ - chunkZ * 16);
	}

	public int getHeightLocal(int x, int z) {
		if (heightMap == null)
			try {
				load();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (heightMap == null)
			return 0;
		return heightMap[z * 16 + x];
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public boolean getShouldRender() {
		return this.shouldRender;
	}

	public void setShouldRender(boolean render) {
		this.shouldRender = render;
		if (render)
			this.renderRequested = true;
	}
	
	public void setRenderRequested(boolean renderRequested) {
		this.renderRequested = renderRequested;
	}
	
	public boolean getRenderRequested() {
		return renderRequested;
	}
	
	public void setFullReRender(boolean fullRerender) {
		this.fullReRender = fullRerender;
		if(fullRerender)
			renderRequested = false;
	}

	public void renderChunkImage() {
		this.lastAccess = System.currentTimeMillis();
		if (this.renderRequested && this.fullReRender)
			calculateHeightmap();
		this.shouldRender = false;
		this.fullReRender = false;
		if (blocks == null || heightMap == null || isRendering
				|| blockRegistryChangeCounter != BlockRegistry.getChangeCounter()) {
			this.renderRequested = false;
			return;
		}
		
		this.lastImageAccess = System.currentTimeMillis();
		
		if(chunkImg == null) {
			loadedChunkImages.push(this);
		}
		
		isRendering = true;
		try {
			BufferedImage tmpChunkImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			BufferedImage tmpChunkImgSmall = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
			BufferedImage tmpChunkImgSmaller = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
			BufferedImage tmpChunkImgSmaller2 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
			BufferedImage tmpChunkImgSmallest = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			for (int z = 0; z < 16; ++z) {
				for (int x = 0; x < 16; ++x) {
					int height = getHeightLocal(x, z);
					int blockId = getBlockIdLocal(x, height, z);
					int colour = 0;
					// if (blockId != 0)
					// blockId = Color.HSBtoRGB(blockId * 256.6536f, ((float) (blockId & 15)) /
					// 32.0f,
					// Math.min(Math.max(getHeightLocal(x, z) / 500.0f + 0.15f, 0.1f), 1.0f));
					if (blockId != 0) {
						Block block = BlockRegistry.getBlock(blockId);
						int stateId = BlockStateRegistry.getIdForName(block.getName());
						BlockState state = BlockStateRegistry.getState(stateId);
						String defaultTexture = state.getDefaultTexture();
						if (defaultTexture != "") {
							colour = ResourcePack.getDefaultColour(defaultTexture);

							if (state.hasTint()) {
								int biomeId = getBiomeIdLocal(x, height, z);
								Biome biome = BiomeRegistry.getBiome(biomeId);
								nl.bramstout.mcworldexporter.Color tint = biome.getBiomeColor(state);
								Color color = new Color(colour);
								color = new Color((int) (color.getRed() * tint.getR()),
										(int) (color.getGreen() * tint.getG()), (int) (color.getBlue() * tint.getB()));
								colour = color.getRGB();
							}
						}
					}
					tmpChunkImg.setRGB(x, z, colour | 255 << 24);
				}
			}
			for (int z = 0; z < 8; ++z) {
				for (int x = 0; x < 8; ++x) {
					tmpChunkImgSmall.setRGB(x, z, tmpChunkImg.getRGB(x * 2, z * 2));
				}
			}
			for (int z = 0; z < 4; ++z) {
				for (int x = 0; x < 4; ++x) {
					tmpChunkImgSmaller.setRGB(x, z, tmpChunkImg.getRGB(x * 4, z * 4));
				}
			}
			for (int z = 0; z < 2; ++z) {
				for (int x = 0; x < 2; ++x) {
					tmpChunkImgSmaller2.setRGB(x, z, tmpChunkImg.getRGB(x * 8, z * 8));
				}
			}
			tmpChunkImgSmallest.setRGB(0, 0, tmpChunkImgSmaller2.getRGB(1, 1));
			chunkImg = tmpChunkImg;
			chunkImgSmall = tmpChunkImgSmall;
			chunkImgSmaller = tmpChunkImgSmaller;
			chunkImgSmaller2 = tmpChunkImgSmaller2;
			chunkImgSmallest = tmpChunkImgSmallest;
			chunkColor = new Color(chunkImgSmallest.getRGB(0, 0));
		} catch (Exception ex) {
		}
		isRendering = false;
		this.renderRequested = false;
	}

	public BufferedImage getChunkImageForZoomLevel(int zoomLevel) {
		this.lastImageAccess = System.currentTimeMillis();
		if(zoomLevel >= 4)
			return chunkImg;
		else if(zoomLevel == 3) {
			return chunkImgSmall;
		}else if(zoomLevel == 2) {
			return chunkImgSmaller;
		}else if(zoomLevel == 1) {
			return chunkImgSmaller2;
		}else {
			return chunkImgSmallest;
		}
	}
	
	public BufferedImage getChunkImage() {
		if (chunkImg == null) {
			renderChunkImage();
		}
		return chunkImg;
	}

	public BufferedImage getChunkImageSmall() {
		if (chunkImgSmall == null)
			getChunkImage();
		return chunkImgSmall;
	}

	public BufferedImage getChunkImageSmaller() {
		if (chunkImgSmaller == null)
			getChunkImage();
		return chunkImgSmaller;
	}
	
	public BufferedImage getChunkImageSmaller2() {
		if (chunkImgSmaller2 == null)
			getChunkImage();
		return chunkImgSmaller2;
	}
	
	public BufferedImage getChunkImageSmallest() {
		if (chunkImgSmallest == null)
			getChunkImage();
		return chunkImgSmallest;
	}

	public Color getChunkColor() {
		if (chunkColor == null)
			getChunkImage();
		return chunkColor;
	}
	
	public void unloadImages() {
		this.chunkImg = null;
		this.chunkImgSmall = null;
		this.chunkImgSmaller = null;
		this.chunkImgSmaller2 = null;
	}

	protected void calculateHeightmap() {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			return;
		short[] tmpHeightMap = new short[16 * 16];
		Arrays.fill(tmpHeightMap, Short.MIN_VALUE);
		int i = 0;
		int j = 0;
		int x = 0;
		int y = 0;
		int z = 0;
		int minY = chunkSectionOffset * 16;
		int maxY = minY + blocks.length * 16;
		minY = Math.max(minY, MCWorldExporter.getApp().getExportBounds().getMinY());
		maxY = Math.min(maxY, MCWorldExporter.getApp().getExportBounds().getMaxY());
		int sectionIndex = ((maxY - 1) >> 4) - chunkSectionOffset;
		int sectionY = (maxY - 1) % 16;
		if (sectionY < 0)
			sectionY += 16;
		int[] section = null;
		boolean done = false;
		for (y = maxY - 1; y >= minY; --y) {
			section = blocks[sectionIndex];
			if (section == null) {
				sectionY -= 1;
				if (sectionY == -1) {
					sectionY = 15;
					sectionIndex -= 1;
				}
				continue;
			}
			i = sectionY * 16 * 16;
			j = 0;
			done = true;
			for (z = 0; z < 16; ++z) {
				for (x = 0; x < 16; ++x) {
					if (tmpHeightMap[j] == Short.MIN_VALUE) {
						done = false;
						if (section[i] != 0)
							tmpHeightMap[j] = (short) y;
					}
					++i;
					++j;
				}
			}
			if (done)
				break;
			sectionY -= 1;
			if (sectionY == -1) {
				sectionY = 15;
				sectionIndex -= 1;
			}
		}
		for (i = 0; i < 16 * 16; ++i) {
			if (tmpHeightMap[i] < minY)
				tmpHeightMap[i] = (short) minY;
		}
		heightMap = tmpHeightMap;
	}

}
