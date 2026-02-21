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
import nl.bramstout.mcworldexporter.export.BlendedBiome;
import nl.bramstout.mcworldexporter.export.BlendedBiome.WeightedColor;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.parallel.Queue;
import nl.bramstout.mcworldexporter.parallel.SpinLock;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintValue;

public abstract class Chunk {

	protected static Queue<Chunk> loadedChunks = new Queue<Chunk>();
	private static Queue<Chunk> loadedChunkImages = new Queue<Chunk>();
	private static final int MAX_LOADED_CHUNKS = 64*64;
	private static final int MAX_LOADED_CHUNK_IMAGES = 512*512;
	
	private static class LoadedChunksGC implements Runnable{
		
		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(1000 * 1);
					
					List<Chunk> putBackIn = new ArrayList<Chunk>();
					Chunk chunk = null;
					while((chunk = loadedChunks.pop()) != null) {
						if((System.currentTimeMillis() - chunk.lastAccess) > 1000 && !chunk.isLoading) {
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
						if((System.currentTimeMillis() - chunk.lastImageAccess) > 10000) {
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
		thread.setName("Chunk_unloader");
		thread.start();
	}

	protected int dataVersion;
	protected int chunkX;
	protected int chunkZ;
	protected boolean loadError;
	protected boolean isLoading;
	protected long lastAccess;
	protected long lastImageAccess;
	/**
	 * A list of layers of chunk sections. Each layer is a list of chunk sections. 
	 * Each chunk section is a list of block ids from the
	 * BlockRegistry. Each chunk section stores 16*16*16 blocks in the YZX order.
	 */
	protected int[][][] blocks;
	/**
	 * A list of biome sections. Each biome section is a list of biome ids from the
	 * BiomeRegistry. Each biome section stores 4*4*4 biome ids in the YZX order.
	 */
	protected int[][] biomes;
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
	protected short heightMapMaxVal;
	
	protected Region region;

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
	
	protected SpinLock loadLock;

	public Chunk(Region region, int chunkX, int chunkZ) {
		this.region = region;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.dataVersion = Integer.MAX_VALUE;
		this.loadError = false;
		this.isLoading = false;
		this.blocks = null;
		this.biomes = null;
		this.heightMap = null;
		this.heightMapMaxVal = 320;
		this.entities = null;
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
		if(this.loadError || this.blocks != null || this.region.getWorld().isPaused())
			return;
		
		loadLock.aqcuire();
		isLoading = true;
		try {
			_load();
			this.lastAccess = System.currentTimeMillis();
			loadedChunks.push(this);
		} finally {
			isLoading = false;
			loadLock.release();
		}
	}
	
	public void loadEntities() throws Exception{
		if(this.loadError || this.entities != null)
			return;
		
		loadLock.aqcuire();
		try {
			if(this.entities != null)
				return;
			_loadEntities();
		} finally {
			loadLock.release();
		}
	}
	
	protected abstract void _load() throws Exception;
	
	protected abstract void _loadEntities() throws Exception;

	public abstract void unload();
	
	public abstract void addBiomeTints(BlendedBiome biome, int x, int y, int z, int index);
	
	public void unloadEntities() {
		loadLock.aqcuire();
		try {
			this.entities = null;
		} finally {
			loadLock.release();
		}
	}
	
	public int[][][] _getBlocks() {
		return blocks;
	}
	
	public int[][] _getBiomes(){
		return biomes;
	}
	
	public int _getChunkSectionOffset() {
		return chunkSectionOffset;
	}
	
	public void _setBlocks(int[][][] blocks) {
		this.blocks = blocks;
	}
	
	public void _setBiomes(int[][] biomes) {
		this.biomes = biomes;
	}
	
	public void _setChunkSectionOffset(int chunkSectionOffset) {
		this.chunkSectionOffset = chunkSectionOffset;
	}
	
	public List<Entity> _getEntities() {
		return entities;
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	public int getDataVersion() {
		return dataVersion;
	}
	
	public boolean hasLoadError() {
		return loadError && !isLoading;
	}
	
	public boolean isLoaded() {
		return !loadError && this.blocks != null;
	}
	
	public Region getRegion() {
		return region;
	}
	
	public int getLayerCount() {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			try {
				load();
			} catch (Exception e) {
				World.handleError(e);
			}
		if(blocks == null)
			return 0;
		return blocks.length;
	}

	public int getBlockId(int worldX, int worldY, int worldZ, int layer) {
		return getBlockIdLocal(worldX - chunkX * 16, worldY, worldZ - chunkZ * 16, layer);
	}
	

	public int getBlockIdLocal(int x, int y, int z, int layer) {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			try {
				load();
			} catch (Exception e) {
				World.handleError(e);
			}
		if (blocks == null)
			return -1; // Couldn't load, so chunk doesn't exist.
		if(layer < 0 || layer >= blocks.length)
			return 0;
		int[][] layerBlocks = blocks[layer];
		int sectionY = (y >> 4) - chunkSectionOffset;
		if (sectionY < 0 || sectionY >= layerBlocks.length)
			return 0;
		if (layerBlocks[sectionY] == null)
			return 0;
		return layerBlocks[sectionY][((y - chunkSectionOffset * 16) & 15) * 16 * 16 + z * 16 + x];
	}

	public void getBlockId(int worldX, int worldY, int worldZ, LayeredBlock blocks) {
		getBlockIdLocal(worldX - chunkX * 16, worldY, worldZ - chunkZ * 16, blocks);
	}
	
	public void getBlockIdLocal(int x, int y, int z, LayeredBlock outBlocks) {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			try {
				load();
			} catch (Exception e) {
				World.handleError(e);
			}
		if (blocks == null) {
			outBlocks.setLayerCount(0);
			return;
		}
		outBlocks.setLayerCount(blocks.length);
		for(int layer = 0; layer < blocks.length; ++layer) {
			outBlocks.setBlock(layer, 0);
			if(layer < 0 || layer >= blocks.length)
				continue;
			int[][] layerBlocks = blocks[layer];
			int sectionY = (y >> 4) - chunkSectionOffset;
			if (sectionY < 0 || sectionY >= layerBlocks.length)
				continue;
			if (layerBlocks[sectionY] == null)
				continue;
			int blockId = layerBlocks[sectionY][((y - chunkSectionOffset * 16) & 15) * 16 * 16 + z * 16 + x];
			outBlocks.setBlock(layer, blockId);
		}
	}
	
	public int getBiomeId(int worldX, int worldY, int worldZ) {
		return getBiomeIdLocal(worldX - chunkX * 16, worldY, worldZ - chunkZ * 16);
	}

	public int getBiomeIdLocal(int x, int y, int z) {
		this.lastAccess = System.currentTimeMillis();
		if (biomes == null || blocks == null)
			try {
				load();
			} catch (Exception e) {
				World.handleError(e);
			}
		if (biomes == null || blocks == null)
			return -1;
		int sectionY = (y >> 4) - chunkSectionOffset;
		if (sectionY < 0 || sectionY >= biomes.length)
			return -1;
		if (biomes[sectionY] == null)
			return -1;
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
				World.handleError(e);
			}
		if (heightMap == null)
			return 0;
		// In very rare cases heightMap can end up being null.
		// This is due to race conditions, but for performance
		// reasons I don't want to use a mutex
		try {
			return heightMap[z * 16 + x];
		}catch(Exception ex) {}
		return 0;
	}
	
	public int getHeightLocalNoLoad(int x, int z) {
		if (heightMap == null)
			return 0;
		// In very rare cases heightMap can end up being null.
		// This is due to race conditions, but for performance
		// reasons I don't want to use a mutex
		try {
			return heightMap[z * 16 + x];
		}catch(Exception ex) {}
		return 0;
	}

	public List<Entity> getEntities() {
		this.lastAccess = System.currentTimeMillis();
		if (entities == null)
			try {
				loadEntities();
			} catch (Exception e) {
				World.handleError(e);
			}
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
	
	private int getColourForBlock(Block block, BlockState state, int x, int y, int z, BlendedBiome blendedBiome) {
		int colour = 0;
		blendedBiome.clear();
		String defaultTexture = state.getDefaultTexture();
		if (defaultTexture != "") {
			colour = ResourcePacks.getDefaultColour(defaultTexture);

			if (state.hasTint()) {
				int biomeId = getBiomeIdLocal(x, y, z);
				Biome biome = BiomeRegistry.getBiome(biomeId);
				blendedBiome.addBiome(biome, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f);
				blendedBiome.normalise();
				addBiomeTints(blendedBiome, x, y, z, 0);
				Tint blockTint = state.getTint();
				if(blockTint != null) {
					TintLayers tintLayers = blockTint.getTint(block.getProperties());
					if(tintLayers != null) {
						TintValue tintVal = tintLayers.getGenericTint();
						if(tintVal != null) {
							WeightedColor weightedColor = tintVal.getColor(blendedBiome);
							if(weightedColor != null) {
								nl.bramstout.mcworldexporter.Color tint = weightedColor.get(0);
								if(tint != null) {
									int r = (colour >>> 16) & 0xFF;
									int g = (colour >>> 8) & 0xFF;
									int b = (colour) & 0xFF;
									r = (int) (((float) r) * tint.getR());
									g = (int) (((float) g) * tint.getG());
									b = (int) (((float) b) * tint.getB());
									colour = (r << 16) | (g << 8) | b;
								}
							}
						}
					}
				}
			}
		}
		return colour;
	}

	public void renderChunkImage() {
		if(region.world.isPaused())
			return;
		this.lastAccess = System.currentTimeMillis();
		if (this.renderRequested && this.fullReRender) {
			try {
				load();
			} catch (Exception e) {
				World.handleError(e);
			}
			calculateHeightmap();
		}
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
			BlendedBiome blendedBiome = new BlendedBiome();
			int layerCount = getLayerCount();
			for (int z = 0; z < 16; ++z) {
				for (int x = 0; x < 16; ++x) {
					int height = getHeightLocal(x, z);
					int finalColorR = 0;
					int finalColorG = 0;
					int finalColorB = 0;
					int finalColorWeight = 0;
					for(int layer = 0; layer < layerCount; ++layer) {
						int colour = 0;
						int blockId = getBlockIdLocal(x, height, z, layer);
						if(MCWorldExporter.getApp().getActiveExportBounds().getMaxY() < 320) {
							// Cave mode. If the block at maxY equals blockId, then keep moving
							// down until we find an air block and then pick the next non-air block.
							if((height+1) >= heightMapMaxVal) {
								// Do cave mode
								blockId = 0;
								boolean foundAir = false;
								for(int sampleY = height; sampleY >= MCWorldExporter.getApp().getActiveExportBounds().getMinY(); --sampleY) {
									int sampleBlockId = getBlockIdLocal(x, sampleY, z, layer);
									Block block = BlockRegistry.getBlock(sampleBlockId);
									if(sampleBlockId == 0) {
										foundAir = true;
									}else {
										if(foundAir || block.isLiquid()) {
											// Found our block to show
											height = sampleY;
											blockId = sampleBlockId;
											break;
										}
									}
								}
							}
						}
						if (blockId > 0) {
							Block block = BlockRegistry.getBlock(blockId);
							int stateId = BlockStateRegistry.getIdForName(block.getName(), block.getDataVersion());
							BlockState state = BlockStateRegistry.getState(stateId);
							colour = getColourForBlock(block, state, x, height, z, blendedBiome);
							
							if(block.isLiquid()) {
								// Let's make water transparent. We keep going down until we find
								// a block that's not water. We get that block's colour
								// and blend it with the water colour.
								for(int sampleY = height - 1; sampleY >= MCWorldExporter.getApp().getActiveExportBounds().getMinY(); --sampleY) {
									boolean foundSolidBlock = false;
									for(int sampleLayer = 0; sampleLayer < layerCount; ++sampleLayer) {
										blockId = getBlockIdLocal(x, sampleY, z, sampleLayer);
										block = BlockRegistry.getBlock(blockId);
										stateId = BlockStateRegistry.getIdForName(block.getName(), block.getDataVersion());
										state = BlockStateRegistry.getState(stateId);
										if(blockId == 0 || block.isLiquid())
											continue;
										
										// We have found a non-water block!
										int bgColour = getColourForBlock(block, state, x, sampleY, z, blendedBiome);
										if(bgColour == 0)
											bgColour = colour;
										Color fgColor = new Color(colour);
										Color bgColor = new Color(bgColour);
										Color resColor = new Color((fgColor.getRed() * 2 + bgColor.getRed()) / 3,
																	(fgColor.getGreen() * 2 + bgColor.getGreen()) / 3,
																	(fgColor.getBlue() * 2 + bgColor.getBlue()) / 3);
										colour = resColor.getRGB();
										foundSolidBlock = true;
										break;
									}
									if(foundSolidBlock)
										break;
								}
							}
							Color fgColor = new Color(colour);
							finalColorR += fgColor.getRed();
							finalColorG += fgColor.getGreen();
							finalColorB += fgColor.getBlue();
							finalColorWeight += 1;
						}
					}
					if(finalColorWeight == 0)
						finalColorWeight = 1;
					Color finalColor = new Color(finalColorR / finalColorWeight, finalColorG / finalColorWeight, finalColorB / finalColorWeight);
					tmpChunkImg.setRGB(x, z, finalColor.getRGB() | (255 << 24));
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
			//ex.printStackTrace();
			World.handleError(ex);
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
		this.chunkImgSmallest = null;
		this.heightMap = null;
	}

	protected void calculateHeightmap() {
		this.lastAccess = System.currentTimeMillis();
		if (blocks == null)
			return;
		if(blocks.length == 0)
			return;
		short[] tmpHeightMap = new short[16 * 16];
		Arrays.fill(tmpHeightMap, Short.MIN_VALUE);
		int i = 0;
		int j = 0;
		int x = 0;
		int y = 0;
		int z = 0;
		int minY = chunkSectionOffset * 16;
		int maxY = minY + blocks[0].length * 16;
		minY = Math.max(minY, MCWorldExporter.getApp().getActiveExportBounds().getMinY());
		maxY = Math.min(maxY, MCWorldExporter.getApp().getActiveExportBounds().getMaxY());
		heightMapMaxVal = (short) maxY;
		int layerCount = blocks.length;
		for(int layer = 0; layer < layerCount; ++layer) {
			int sectionIndex = ((maxY - 1) >> 4) - chunkSectionOffset;
			int sectionY = (maxY - 1) % 16;
			if (sectionY < 0)
				sectionY += 16;
			int[] section = null;
			boolean done = false;
			for (y = maxY - 1; y >= minY; --y) {
				section = blocks[layer][sectionIndex];
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
		}
		for (i = 0; i < 16 * 16; ++i) {
			if (tmpHeightMap[i] < minY)
				tmpHeightMap[i] = (short) minY;
		}
		heightMap = tmpHeightMap;
	}
	
	private int renderCounter = 0;
	
	public int getRenderCounter() {
		return renderCounter;
	}
	
	public void setRenderCounter(int renderCounter) {
		this.renderCounter = renderCounter;
	}

}
