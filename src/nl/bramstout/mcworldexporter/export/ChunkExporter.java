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

package nl.bramstout.mcworldexporter.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.export.processors.FaceOptimiser;
import nl.bramstout.mcworldexporter.export.processors.MeshProcessors;
import nl.bramstout.mcworldexporter.export.processors.MeshProcessors.MeshMergerMode;
import nl.bramstout.mcworldexporter.export.processors.RaytracingOptimiser;
import nl.bramstout.mcworldexporter.export.processors.WriteProcessor;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.Occlusion;
import nl.bramstout.mcworldexporter.model.Subdivider;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTexture;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class ChunkExporter {
	
	private ExportBounds bounds;
	private World world;
	private int chunkX;
	private int chunkZ;
	private int chunkSize;
	private int worldOffsetX;
	private int worldOffsetY;
	private int worldOffsetZ;
	private Map<String, Mesh> meshes;
	private Map<IndividualBlockId, FloatArray> individualBlocks;
	private String name;
	//private LODCache lodCache;
	private CaveCache caveCache;
	private Reference<char[]> charBuffer;

	
	public ChunkExporter(ExportBounds bounds, World world, int chunkX, int chunkZ, int chunkSize, String name) {
		this.bounds = bounds;
		this.world = world;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.chunkSize = chunkSize;
		this.worldOffsetX = bounds.getOffsetX();
		this.worldOffsetY = bounds.getOffsetY();
		this.worldOffsetZ = bounds.getOffsetZ();
		this.meshes = new HashMap<String, Mesh>();
		this.individualBlocks = new HashMap<IndividualBlockId, FloatArray>();
		this.name = name;
		//this.lodCache = new LODCache(chunkX, chunkZ, chunkSize, bounds.getMinY(), bounds.getMaxY() - bounds.getMinY());
		this.caveCache = null;
		if(Config.fillInCaves)
			this.caveCache = new CaveCache(chunkX, chunkZ, chunkSize, bounds.getMinY(), bounds.getMaxY() - bounds.getMinY());
		this.charBuffer = new Reference<char[]>();
	}
	
	public void generateMeshes() {
		for(int z = chunkZ; z < (chunkZ + chunkSize); ++z) {
			for(int x = chunkX; x < (chunkX + chunkSize); ++x) {
				final int fx = x;
				final int fz = z;
				try {
					generateChunkMeshes(fx, fz);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				MCWorldExporter.getApp().getUI().getProgressBar().finishedMesh(chunkSize * chunkSize);
			}
		}
		
		for(String bannedMaterial : Config.bannedMaterials) {
			if(meshes.containsKey(bannedMaterial))
				meshes.remove(bannedMaterial);
			bannedMaterial = bannedMaterial + "_BIOME";
			if(meshes.containsKey(bannedMaterial))
				meshes.remove(bannedMaterial);
		}
	}
	
	private Chunk[] prefetchedChunks = new Chunk[3*3];
	private int prefetchedChunkWorldX = -1;
	private int prefetchedChunkWorldZ = -1;
	
	private void generateChunkMeshes(int x, int z) throws Exception {
		prefetchChunks(x, z);
		
		Chunk chunk = getPrefetchedChunk(x, z);
		if(chunk == null)
			return;
		
		int stoneBlockId = BlockRegistry.getIdForName("minecraft:stone", null, chunk.getDataVersion(), charBuffer);
		
		int minX = Math.max(0, bounds.getMinX() - x * 16);
		int maxX = Math.min(16, (bounds.getMaxX() + 1) - x * 16);
		int minY = bounds.getMinY();
		int maxY = bounds.getMaxY();
		int minZ = Math.max(0, bounds.getMinZ() - z * 16);
		int maxZ = Math.min(16, (bounds.getMaxZ() + 1) - z * 16);
		
		int lodSize = getLodSize(x, z);
		int lodYSize = getLodYSize(x, z);
		int lodLevel = Integer.numberOfTrailingZeros(lodSize);
		int lodYLevel = Integer.numberOfTrailingZeros(lodYSize);
		
		// Make the bounds align to the lodSize
		minX = (((minX + x * 16) >> lodLevel) << lodLevel) - x * 16;
		minY = (minY >> lodYLevel) << lodYLevel;
		minZ = (((minZ + z * 16) >> lodLevel) << lodLevel) - z * 16;
		maxX = (((maxX + x * 16) >> lodLevel) << lodLevel) - x * 16;
		maxY = (maxY >> lodYLevel) << lodYLevel;
		maxZ = (((maxZ + z * 16) >> lodLevel) << lodLevel) - z * 16;
		
		int bx = 0;
		int by = 0;
		int bz = 0;
		int wx = 0;
		int wz = 0;
		int chunkWorldX = x * 16;
		int chunkWorldZ = z * 16;
		int blockId[] = new int[4];
		float offsetX = 0f;
		float offsetY = 0f;
		float offsetZ = 0f;
		float uvOffsetY = 0f;
		BlendedBiome biome = new BlendedBiome();
		BakedBlockState state = null;
		BakedBlockState liquidState = null;
		List<Model> models = new ArrayList<Model>();
		List<ModelFace> detailedOcclusionFaces = new ArrayList<ModelFace>();
		boolean placeStone = false;
		long occlusion = 0;
		long ambientOcclusion = 0;
		Occlusion occlusionHandler = new Occlusion();
		
		for(by = minY; by < maxY; by += lodYSize) {
			for(bz = minZ; bz < maxZ; bz += lodSize) {
				wz = bz + chunkWorldZ;
				for(bx = minX; bx < maxX; bx += lodSize) {
					wx = bx + chunkWorldX;
					
					getLODBlockId(chunk, bx, by, bz, lodSize, lodYSize, blockId);
					if(blockId[0] < 0)
						continue;
					state = BlockStateRegistry.getBakedStateForBlock(blockId[0], blockId[1], blockId[2], blockId[3]);
					
					if(state.isAir() || state.hasLiquid()) {
						placeStone = false;
						if(Config.fillInCaves) {
							// We need to fill in the holes generated
							// by the cave removal algorithm.
							// Holes can occur in air or liquids.
							
							// We do this by checking if this air block
							// would be considered in a cave. If so,
							// we check the surrounding blocks to see
							// if one wouldn't be in a cave. If so,
							// we put in a stone block.
							
							if(isInCaveCached(wx, by, wz)) {
								if(by == minY)
									placeStone = true;
								else {
									for(Direction dir : Direction.CACHED_VALUES) {
										if(!isInCaveCached(wx + dir.x, by + dir.y, wz + dir.z)) {
											placeStone = true;
											break;
										}
									}
								}
							}
							if(placeStone)
								state = BlockStateRegistry.getBakedStateForBlock(
										stoneBlockId, blockId[1], blockId[2], blockId[3]);
						}
						if(!placeStone && state.isAir())
							continue; // No need to do anything with air
					}
					
					occlusion = getOcclusion(chunk, bx, by, bz, state, detailedOcclusionFaces, lodSize, lodYSize);
					if(occlusion == Long.MAX_VALUE)
						continue; // Block is in cave
					
					ambientOcclusion = getAmbientOcclusion(chunk, bx, by, bz, lodSize, lodYSize);
					
					liquidState = state.getLiquidState();
					
					offsetX = 0f;
					offsetY = 0f;
					offsetZ = 0f;
					if(state.hasRandomOffset()) {
						offsetX = (Noise.get(wx, 0, wz) - 0.5f) * 5f;
						offsetZ = (Noise.get(wx, 1, wz) - 0.5f) * 5f;
					} 
					if(state.hasRandomYOffset()){
						offsetY = Noise.get(wx, 2, wz) * -3.0f;
					}
					
					uvOffsetY = 0f;
					if(state.isRandomAnimationXZOffset() && state.isRandomAnimationYOffset()) 
						uvOffsetY = (float) Math.floor(Noise.get(wx, by, wz) * 32.0f);
					else if(state.isRandomAnimationXZOffset())
						uvOffsetY = (float) Math.floor(Noise.get(wx, 0, wz) * 32.0f);
					else if(state.isRandomAnimationYOffset())
						uvOffsetY = (float) Math.floor(Noise.get(0, by, 0) * 32.0f);
					
					
					if(state.isGrassColormap() || state.isFoliageColormap() || state.isWaterColormap() || 
							liquidState != null || state.getTint() != null)
						getBlendedBiome(wx, by, wz, biome);
					
					if(state.isIndividualBlocks() || Config.onlyIndividualBlocks) {
						handleIndividualBlock(blockId, wx, by, wz, offsetX, offsetY, offsetZ, state.isNeedsConnectionInfo());
					}else {
						handleBlock(models, state, blockId, chunk.getDataVersion(), occlusion, ambientOcclusion, detailedOcclusionFaces, 
									wx, by, wz, offsetX, offsetY, offsetZ, uvOffsetY, biome, lodSize, lodYSize, occlusionHandler);
					}
					
					
					if(liquidState != null) {
						handleLiquidState(models, state, liquidState, blockId, chunk.getDataVersion(), occlusion, ambientOcclusion, 
									wx, by, wz, biome, lodSize, lodYSize, occlusionHandler);
					}
				}
			}
		}
		
		//handleEntities(chunk);
	}
	
	private void handleIndividualBlock(int[] blockId, int wx, int by, int wz, float offsetX, float offsetY, float offsetZ,
										boolean needsConnectionInfo) {
		IndividualBlockId id = new IndividualBlockId(blockId[0], 
										needsConnectionInfo ? blockId[1] : 0,
										needsConnectionInfo ? blockId[2] : 0,
										needsConnectionInfo ? blockId[3] : 0);
		FloatArray array = individualBlocks.get(id);
		if(array == null) {
			array = new FloatArray();
			array.add(wx*16 + offsetX - worldOffsetX * 16);
			array.add(by*16 + offsetY - worldOffsetY * 16 + 8.0f);
			array.add(wz*16 + offsetZ - worldOffsetZ * 16);
			individualBlocks.put(id, array);
		}else {
			array.add(wx*16 + offsetX - worldOffsetX * 16);
			array.add(by*16 + offsetY - worldOffsetY * 16 + 8.0f);
			array.add(wz*16 + offsetZ - worldOffsetZ * 16);
		}
	}
	
	private void handleBlock(List<Model> models, BakedBlockState state, int[] blockId, int dataVersion, long occlusion, 
							long ambientOcclusion, List<ModelFace> detailedOcclusionFaces, int wx, int by, int wz, 
							float offsetX, float offsetY, float offsetZ, float uvOffsetY,
							BlendedBiome biome, int lodSize, int lodYSize, Occlusion occlusionHandler) {
		models.clear();
		state.getModels(blockId[1], blockId[2], blockId[3], models);
		
		if(Config.subdivideModelsForCorners) {
			/**
			 * If we want to calculate corner UVs for something like edge highlights,
			 * then you can end up with situations like a slab next to a full block,
			 * where the slab's side will be occluded and no corner added in, but
			 * the full block's side won't be occluded (since it's only partially
			 * occluded) and thus would get a corner added. That corner looks correct
			 * for half of the full block's, but not for the half where the slab is.
			 * There shouldn't be a corner where the slab is touching the full blocks.
			 * 
			 * So, we need to subdivide those faces to solve this.
			 */
			Subdivider.subdivideModelForOcclusion(models, occlusion);
		}
		
		occlusionHandler.calculateCornerDataForModel(models, state, occlusion, detailedOcclusionFaces);
		
		Model model;
		ModelFace face;
		int faceIndex = 0;
		for(int i = 0; i < models.size(); ++i) {
			model = models.get(i);
			for(int j = 0; j < model.getFaces().size(); ++j) {
				if(occlusionHandler.isFaceOccluded(faceIndex)) {
					faceIndex++;
					continue;
				}
				
				face = model.getFaces().get(j);
				
				int cornerData = occlusionHandler.getCornerIndexForFace(face, faceIndex);
				
				addFace(meshes, state.getName(), blockId[0], dataVersion, face, model.getTexture(face.getTexture()), wx, by, wz, 
						offsetX, offsetY, offsetZ, uvOffsetY, model.getExtraData(), biome.getBiomeColor(state), model.isDoubleSided(), 
						lodSize, lodYSize, state.isLodNoUVScale(), false, ambientOcclusion, cornerData);
				
				faceIndex++;
			}
		}
	}
	
	private List<ModelFace> emptyFaceList = new ArrayList<ModelFace>();
	
	private void handleLiquidState(List<Model> models, BakedBlockState state, BakedBlockState liquidState, int[] blockId,
									int dataVersion, long occlusion, long ambientOcclusion, int wx, int by, int wz, 
									BlendedBiome biome, int lodSize, int lodYSize, Occlusion occlusionHandler) {
		models.clear();
		liquidState.getModels(blockId[1], blockId[2], blockId[3], models);
		
		occlusionHandler.calculateCornerDataForModel(models, state, occlusion, emptyFaceList);
		
		Model model;
		ModelFace face;
		int faceIndex = 0;
		for(int i = 0; i < models.size(); ++i) {
			model = models.get(i);
			for(int j = 0; j < model.getFaces().size(); ++j) {
				if(occlusionHandler.isFaceOccluded(faceIndex)) {
					faceIndex++;
					continue;
				}
				
				face = model.getFaces().get(j);
				
				int cornerData = occlusionHandler.getCornerIndexForFace(face, faceIndex);
				
				addFace(meshes, "minecraft:water", blockId[0], dataVersion, face, model.getTexture(face.getTexture()), wx, by, wz, 
						0f, 0f, 0f, 0f, model.getExtraData(), biome.getWaterColour(), model.isDoubleSided(), lodSize, lodYSize, 
						state.isLodNoUVScale(), false, ambientOcclusion, cornerData);
				
				faceIndex++;
			}
		}
	}
	
	private void prefetchChunks(int chunkX, int chunkZ) {
		int i = 0;
		for(int z = chunkZ - 1; z <= chunkZ + 1; ++z) {
			for(int x = chunkX - 1; x <= chunkX + 1; ++x) {
				try {
					prefetchedChunks[i] = world.getChunk(x, z);
				}catch(Exception ex) {
					ex.printStackTrace();
					prefetchedChunks[i] = null;
				}
				i++;
			}
		}
		prefetchedChunkWorldX = (chunkX - 1) * 16;
		prefetchedChunkWorldZ = (chunkZ - 1) * 16;
	}
	
	private Chunk getPrefetchedChunk(int chunkX, int chunkZ) {
		int x = chunkX - (prefetchedChunkWorldX >> 4);
		int z = chunkZ - (prefetchedChunkWorldZ >> 4);
		if(x < 0 || x > 2 || z < 0 || z > 2) {
			try {
				return world.getChunk(chunkX, chunkZ);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
		return prefetchedChunks[z * 3 + x];
	}
	
	private Chunk getPrefetchedChunkForBlockPos(int blockX, int blockZ) {
		blockX >>= 4;
		blockZ >>= 4;
		return getPrefetchedChunk(blockX, blockZ);
	}
	
	private int getLodSize(int chunkX, int chunkZ) {
		if(!bounds.hasLod())
			return 1;
		int lodSizeX = (int) Math.ceil(((double) Math.abs(bounds.getLodCenterX() - (chunkX * 16 + 8))) / 
				((double) bounds.getLodWidth()/2));
		int lodSizeZ = (int) Math.ceil(((double) Math.abs(bounds.getLodCenterZ() - (chunkZ * 16 + 8))) / 
						((double) bounds.getLodDepth()/2));
		int lodSize = Math.max(Math.min(Math.max(lodSizeX, lodSizeZ), 16), 1);
		// Make lodSize go up by powers of two.
		return Integer.highestOneBit(lodSize);
	}
	
	private int getLodYSize(int chunkX, int chunkZ) {
		if(!bounds.hasLod())
			return 1;
		int lodSizeX = (int) Math.ceil(((double) Math.abs(bounds.getLodCenterX() - (chunkX * 16 + 8))) / 
				((double) bounds.getLodWidth()/2));
		int lodSizeZ = (int) Math.ceil(((double) Math.abs(bounds.getLodCenterZ() - (chunkZ * 16 + 8))) / 
						((double) bounds.getLodDepth()/2));
		int lodSize = Math.max(lodSizeX, lodSizeZ);
		int lodYSize = Math.min(Math.max(lodSize / bounds.getLodYDetail(), 1), 16);
		// Make lodSize go up by powers of two.
		return Integer.highestOneBit(lodYSize);
	}
	
	private int lodSampleBlockId(Chunk chunk, int cx, int cy, int cz) {
		if(cx < 0 || cx >= 16 || cz < 0 || cz >= 16)
			return lodSampleBlockId(cx + chunk.getChunkX() * 16, cy, cz + chunk.getChunkZ() * 16);
		return chunk.getBlockIdLocal(cx, cy, cz);
	}
	
	private int lodSampleBlockId(int wx, int wy, int wz) {
		Chunk chunk = getPrefetchedChunkForBlockPos(wx, wz);
		if (chunk != null)
			return lodSampleBlockId(chunk, wx - chunk.getChunkX() * 16, wy, wz - chunk.getChunkZ() * 16);
		return -1;
	}
	
	/** 
	 * Stores the blockId and then the number of blocks for that Id.
	 * and then the x, y, z coordinate of the block.
	 */
	private int lod_blockIds[] = new int[16*16*16*5];
	
	private void getLODBlockId(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize, int[] out) {
		if(lodSize <= 1) {
			out[0] = lodSampleBlockId(chunk, cx, cy, cz);
			out[1] = chunk.getChunkX() * 16 + cx;
			out[2] = cy;
			out[3] = chunk.getChunkZ() * 16 + cz;
			return;
		}
		
		// First check the cache, otherwise calculate it.
		//if(lodCache.get(chunk, cx, cy, cz, lodSize, lodYSize, out))
		//	return;
		
		
		int numBlocks = lodSize*lodSize*lodYSize;
		int count = numBlocks * 5;
		for(int i = 0; i < count; i += 5) {
			lod_blockIds[i] = -2;
			lod_blockIds[i+1] = -1;
		}
		int blockId = 0;
		BakedBlockState state = null;
		boolean allowed = true;
		// At higher level of details, we sample the world with a lower resolution.
		// This reduces the accuracy, but significantly speeds things up.
		int stepXZ = Math.max(lodSize / 4, 1);
		int stepY = Math.max(lodYSize / 4, 1);
		int chunkX = chunk.getChunkX() * 16;
		int chunkZ = chunk.getChunkZ() * 16;
		int blockPriority = 0;
		
		// Gather all block ids.
		for(int y = cy; y < cy + lodYSize; y += stepY) {
			for(int z = cz; z < cz + lodSize; z += stepXZ) {
				for(int x = cx; x < cx + lodSize; x += stepXZ) {
					blockId = lodSampleBlockId(chunk, x, y, z);
					if(blockId < 0) {
						out[0] = -1;
						out[1] = 0;
						out[2] = 0;
						out[3] = 0;
						return;
					}
					allowed = true;
					state = BlockStateRegistry.getBakedStateForBlock(blockId, x + chunkX, y, z + chunkZ);
					if(state.isAir())
						allowed = false;
					blockPriority = state.getLodPriority();
					if(blockPriority <= 0)
						allowed = false;
					
					if(allowed) {
						for(int i = 0; i < numBlocks*5; i += 5) {
							if(lod_blockIds[i] == blockId) {
								lod_blockIds[i+1] += blockPriority;
								if(y > lod_blockIds[i+3]) {
									// Store the upper most block.
									// This is mainly for things like liquids.
									// Otherwise it might think that there is
									// another water block above it and so
									// not show the upper face.
									// This shows up as the surface of water just
									// disappearing.
									lod_blockIds[i+2] = x + chunkX;
									lod_blockIds[i+3] = y;
									lod_blockIds[i+4] = z + chunkZ;
								}
								break;
							}else if(lod_blockIds[i] == -2) {
								lod_blockIds[i] = blockId;
								lod_blockIds[i+1] = blockPriority;
								lod_blockIds[i+2] = x + chunkX;
								lod_blockIds[i+3] = y;
								lod_blockIds[i+4] = z + chunkZ;
								break;
							}
						}
					}
				}
			}
		}
		
		// Find the most common one
		// Return value 0 in case we found nothing.
		int mostCommonBlockId = 0;
		int mostCommonAmount = 0;
		int mostCommonX = 0;
		int mostCommonY = 0;
		int mostCommonZ = 0;
		for(int i = 0; i < numBlocks * 5; i += 5) {
			if(lod_blockIds[i+1] > mostCommonAmount) {
				mostCommonBlockId = lod_blockIds[i];
				mostCommonAmount = lod_blockIds[i+1];
				mostCommonX = lod_blockIds[i+2];
				mostCommonY = lod_blockIds[i+3];
				mostCommonZ = lod_blockIds[i+4];
			}
		}
		out[0] = mostCommonBlockId;
		out[1] = mostCommonX;
		out[2] = mostCommonY;
		out[3] = mostCommonZ;
		
		// Update the LOD cache
		//lodCache.set(chunk, cx, cy, cz, lodSize, lodYSize, mostCommonBlockId, mostCommonX, mostCommonY, mostCommonZ);
	}
	
	private void getLODBlockIdOcclusion(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize, Direction direction, int[] out) {
		if(lodSize <= 1) {
			out[0] = lodSampleBlockId(chunk, cx, cy, cz);
			out[1] = chunk.getChunkX() * 16 + cx;
			out[2] = cy;
			out[3] = chunk.getChunkZ() * 16 + cz;
			return;
		}
		
		if(cx < 0 || cx >= 16 || cz < 0 || cz >= 16) {
			getLODBlockIdOcclusion(cx + chunk.getChunkX() * 16, cy, cz + chunk.getChunkZ() * 16, lodSize, lodYSize, direction, out);
			return;
		}
		
		int sampleLodSize = getLodSize(chunk.getChunkX(), chunk.getChunkZ());
		int sampleLodYSize = getLodYSize(chunk.getChunkX(), chunk.getChunkZ());
		int sampleLodLevel = Integer.numberOfTrailingZeros(sampleLodSize);
		int sampleLodYLevel = Integer.numberOfTrailingZeros(sampleLodYSize);
		if(sampleLodSize >= lodSize) {
			getLODBlockId(chunk, 
					(cx >> sampleLodLevel) << sampleLodLevel, 
					(cy >> sampleLodYLevel) << sampleLodYLevel, 
					(cz >> sampleLodLevel) << sampleLodLevel, 
					sampleLodSize, sampleLodYSize, out);
		}else {
			out[0] = -2;
			int lodXSize = lodSize;
			int lodZSize = lodSize;
			int startX = cx;
			int startY = cy;
			int startZ = cz;
			if(direction == Direction.DOWN)
				startY += lodYSize / 2;
			if(direction == Direction.NORTH)
				startZ += lodZSize / 2;
			if(direction == Direction.WEST)
				startX += lodXSize / 2;
			if(direction == Direction.UP || direction == Direction.DOWN)
				lodYSize = Math.max(lodYSize/2, 1);
			if(direction == Direction.NORTH || direction == Direction.SOUTH)
				lodZSize = Math.max(lodZSize/2, 1);
			if(direction == Direction.EAST || direction == Direction.WEST)
				lodXSize = Math.max(lodXSize/2, 1);
			BakedBlockState state = null;
			for(int y = startY; y < cy + lodYSize; y += sampleLodYSize) {
				for(int z = startZ; z < cz + lodZSize; z += sampleLodSize) {
					for(int x = startX; x < cx + lodXSize; x += sampleLodSize) {
						getLODBlockId(chunk, x, y, z, sampleLodSize, sampleLodYSize, out);
						if(out[0] <= 0) {
							// Early out for performance.
							//out[0] = 0;
							out[1] = 0;
							out[2] = 0;
							out[3] = 0;
							return;
						}
						state = BlockStateRegistry.getBakedStateForBlock(out[0], out[1], out[2], out[3]);
						if(state.isTransparentOcclusion() || state.isLeavesOcclusion() || state.isAir()) {
							return;
						}
						long occludes = state.getOccludes();
						occludes >>= direction.getOpposite().id * 4;
						occludes &= 0b1111;
						if(occludes != 0b1111) {
							// This block doesn't fully occlude this side,
							// therefore it shouldn't occlude the block.
							out[0] = 0;
							out[1] = 0;
							out[2] = 0;
							out[3] = 0;
							return;
						}
					}
				}
			}
			if(out[0] == -2) {
				out[0] = -1;
				out[1] = 0;
				out[2] = 0;
				out[3] = 0;
			}
		}
	}
	
	private void getLODBlockIdOcclusion(int wx, int wy, int wz, int lodSize, int lodYSize, Direction direction, int[] out) {
		Chunk chunk = getPrefetchedChunkForBlockPos(wx, wz);
		if (chunk != null && !chunk.hasLoadError()) {
			getLODBlockIdOcclusion(chunk, wx - chunk.getChunkX() * 16, wy, wz - chunk.getChunkZ() * 16, 
									lodSize, lodYSize, direction, out);
			return;
		}
		out[0] = -1;
		out[1] = 0;
		out[2] = 0;
		out[3] = 0;
	}
	
	private void getBlendedBiome(int wx, int wy, int wz, BlendedBiome res) {
		res.clear();
		// Make sure that the radius is a multiple of four
		int radius = (Config.biomeBlendRadius / 4) * 4;
		int x;
		int y;
		int z;
		int biomeId;
		int xMin = wx - radius;
		int yMin = wy - radius;
		int zMin = wz - radius;
		int xMax = wx + radius;
		int yMax = wy + radius;
		int zMax = wz + radius;
		// How much are we covering on the edges
		int coverXMin = 4 - (xMin & 3);
		int coverYMin = 4 - (yMin & 3);
		int coverZMin = 4 - (zMin & 3);
		int coverXMax = 1 + (xMax & 3);
		int coverYMax = 1 + (yMax & 3);
		int coverZMax = 1 + (zMax & 3);
		int coverX;
		int coverY;
		int coverZ;
		Chunk chunk;
		
		coverY = coverYMin;
		for(y = yMin; y <= yMax; y += 4) {
			coverZ = coverZMin;
			for(z = zMin; z <= zMax; z += 4) {
				coverX = coverXMin;
				for(x = xMin; x <= xMax; x += 4) {
					chunk = getPrefetchedChunkForBlockPos(x, z);
					if(chunk != null) {
						biomeId = chunk.getBiomeIdLocal(x & 15, y, z & 15);
						res.addBiome(BiomeRegistry.getBiome(biomeId), coverX * coverY * coverZ);
					}
					
					coverX = x == xMax ? coverXMax : 4;
				}
				coverZ = z == zMax ? coverZMax : 4;
			}
			coverY = y == yMax ? coverYMax : 4;
		}
		res.normalise();
	}
	
	private static class AtlasKey{
		
		public String atlasTexture;
		public Materials.MaterialTemplate materialTemplate;
		
		public AtlasKey(Atlas.AtlasItem item, String originalTexture, boolean hasBiomeColor, Set<String> colorSets) {
			atlasTexture = item.atlas;
			materialTemplate = Materials.getMaterial(originalTexture, hasBiomeColor, colorSets, "");
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(atlasTexture, materialTemplate);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof AtlasKey))
				return false;
			return ((AtlasKey) obj).atlasTexture.equals(atlasTexture) && 
					(((AtlasKey)obj).materialTemplate == materialTemplate ||
					(((AtlasKey)obj).materialTemplate != null &&
					((AtlasKey) obj).materialTemplate.equals(materialTemplate)));
		}
		
	}
	
	private Map<String, String> atlasMappings = new HashMap<String, String>();
	private Map<AtlasKey, String> atlasMappings2 = new HashMap<AtlasKey, String>();
	private Map<String, Integer> atlasMeshCounters = new HashMap<String, Integer>();
	
	private String getMeshName(Atlas.AtlasItem item, String originalTexture, boolean hasBiomeColor) {
		String meshName = atlasMappings.getOrDefault(originalTexture, null);
		if(meshName != null)
			return meshName;
		
		AtlasKey key = new AtlasKey(item, originalTexture, hasBiomeColor, null);
		meshName = atlasMappings2.getOrDefault(key, null);
		if(meshName != null) {
			atlasMappings.put(originalTexture, meshName);
			return meshName;
		}
		
		Integer counter = atlasMeshCounters.getOrDefault(item.atlas, null);
		if(counter == null) {
			counter = Integer.valueOf(0);
		}
		atlasMeshCounters.put(item.atlas, counter + 1);
		
		meshName = item.atlas + "_" + counter.toString() + "_";
		if(hasBiomeColor)
			meshName += "BIOME";
		
		atlasMappings.put(originalTexture, meshName);
		atlasMappings2.put(key, meshName);
		return meshName;
	}
	
	private void addFace(Map<String, Mesh> meshes, String blockName, int blockId, int dataVersion, ModelFace face, String texture, 
			float bx, float by, float bz, float ox, float oy, float oz, float uvOffsetY,
			String extraData, Color tint, boolean doubleSided, int lodSize, int lodYSize,
			boolean lodNoUVScale, boolean noConnectedTextures, long ambientOcclusion, int cornerData) {
		if(texture == null || texture.equals(""))
			return;
		
		// Connected textures
		if(!noConnectedTextures) {
			Block block = BlockRegistry.getBlock(blockId);
			Entry<ConnectedTexture, List<ConnectedTexture>> connectedTextures = 
										ConnectedTextures.getConnectedTexture(blockName, block.getProperties(), texture);
			if(connectedTextures != null) {
				ConnectedTexture connectedTexture = connectedTextures.getKey();
				if(connectedTexture != null) {
					if(connectedTexture.getFacesToConnect().contains(face.getDirection())) {
						String newTexture = connectedTexture.getTexture((int) bx, (int) by, (int) bz, face);
						if(newTexture != null)
							texture = newTexture;
						if(newTexture == ConnectedTexture.DELETE_FACE)
							return;
					}
				}
				List<ConnectedTexture> overlayTextures = connectedTextures.getValue();
				if(overlayTextures != null) {
					float faceOffset = 0.0125f;
					for(ConnectedTexture overlayTexture : overlayTextures) {
						if(overlayTexture.getFacesToConnect().contains(face.getDirection())) {
							String newTexture = overlayTexture.getTexture((int) bx, (int) by, (int) bz, face);
							if(newTexture != null && newTexture != ConnectedTexture.DELETE_FACE) {
								ModelFace overlayFace = new ModelFace(face);
								overlayFace.translate(((float) face.getDirection().x) * faceOffset, 
														((float) face.getDirection().y) * faceOffset, 
														((float) face.getDirection().z) * faceOffset);
								faceOffset += 0.0125f;
								Color overlayTint = null;
								if(overlayTexture.getTintIndex() != null) {
									overlayFace.setTintIndex(overlayTexture.getTintIndex().intValue());
									if(overlayTexture.getTintIndex().intValue() < 0)
										overlayTint = null;
									else {
										if(overlayTexture.getTintBlock() != null) {
											// It's the biome colour from some other block,
											// so figure that out.
											
											// Get the biome
											BlendedBiome biome = new BlendedBiome();
											getBlendedBiome((int) bx, (int) by, (int) bz, biome);
											
											// Get a block state for the tintBlock name
											int blockId2 = BlockRegistry.getIdForName(overlayTexture.getTintBlock(), null, 
																					dataVersion, charBuffer);
											BakedBlockState blockState = BlockStateRegistry.getBakedStateForBlock(blockId2, 
																						(int) bx, (int) by, (int) bz);
											overlayTint = biome.getBiomeColor(blockState);
										}else {
											overlayTint = tint;
										}
									}
								}
								
								addFace(meshes, blockName, blockId, dataVersion, overlayFace, newTexture, bx, by, bz, ox, oy, oz, 
										uvOffsetY, extraData, overlayTint, doubleSided, lodSize, lodYSize, 
										lodNoUVScale, true, ambientOcclusion, cornerData);
							}
						}
					}
				}
			}
		}
		
		String matTexture = texture;
		String meshName = texture;
		if(tint != null) {
			// If the face doesn't have a tintIndex, get rid of the tint.
			// This is also how Minecraft does it.
			// But don't do it, if we want to force the biome colour anyways.
			if((face.getTintIndex() < 0 && !Config.forceBiomeColor.contains(texture)) || 
					Config.forceNoBiomeColor.contains(blockName))
				tint = null;
			else
				meshName = meshName + "_BIOME";
		}
		float lodSizeF = ((float) (lodSize-1)) / 2.0f;
		float lodYSizeF = ((float) (lodYSize-1)) / 2.0f;
		float lodScale = (float) lodSize;
		float lodYScale = (float) lodYSize;
		float lodUVScale = lodNoUVScale ? 1.0f : lodScale;
		float lodYUVScale = lodNoUVScale ? 1.0f : lodYScale;
		Atlas.AtlasItem atlas = Atlas.getAtlasItem(texture);
		if(atlas != null) {
			meshName = getMeshName(atlas, texture, tint != null);
			texture = atlas.atlas;
			// When using an atlas, we can't just scale up the UVs.
			lodUVScale = Math.min(lodUVScale, (float) atlas.padding);
			lodYUVScale = Math.min(lodYUVScale, (float) atlas.padding);
		}
		// Scale the Y uv's on the top and bottom faces like normal.
		if(face.getDirection() == Direction.UP || face.getDirection() == Direction.DOWN)
			lodYUVScale = lodUVScale;
		
		// TODO: Run code for additionalColorSets
		VertexColorSet.VertexColorFace[] vertexColors = null;
		
		Mesh mesh = meshes.getOrDefault(meshName, null);
		if(mesh == null) {
			boolean animatedTexture = false;
			MCMeta mcmeta = ResourcePacks.getMCMeta(texture);
			if(mcmeta != null)
				animatedTexture = mcmeta.isAnimate() || mcmeta.isInterpolate();
			
			mesh = new Mesh(meshName, MeshPurpose.UNDEFINED, texture, matTexture, animatedTexture, doubleSided, 1024, 8);
			mesh.setExtraData(extraData);
			meshes.put(meshName, mesh);
		}
		
		mesh.addFace(face, 
				bx - worldOffsetX - 0.5f + lodSizeF, by - worldOffsetY + lodYSizeF, bz - worldOffsetZ - 0.5f + lodSizeF, 
				ox, oy, oz, uvOffsetY, lodScale, lodYScale, lodUVScale, lodYUVScale, atlas, 
				tint, ambientOcclusion, cornerData, vertexColors);
	}
	
	private int[] OCCLUSION_BLOCK_ID = new int[4];
	private List<Model> OCCLUSION_MODELS = new ArrayList<Model>();
	
	private long getAmbientOcclusionContributionFromCorner(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize, int corner) {
		BakedBlockState state = null;
		long occlusion = 0;
		getLODBlockId(chunk, cx, cy, cz, lodSize, lodYSize, OCCLUSION_BLOCK_ID);
		if(OCCLUSION_BLOCK_ID[0] < 0)
			occlusion = 0;
		else {
			state = BlockStateRegistry.getBakedStateForBlock(OCCLUSION_BLOCK_ID[0], OCCLUSION_BLOCK_ID[1],
					OCCLUSION_BLOCK_ID[2], OCCLUSION_BLOCK_ID[3]);
			if(state.isTransparentOcclusion()) {
				occlusion = 0;
			}else {
				occlusion = state.getOccludes();
			}
		}
		
		// The occludes bit set contains six sets of four bits,
		// one for each direction of a block. Each direction,
		// is divided up into four sections with one bit for each
		// section. The order is BL, BR, TL, TR going from the least
		// significant bit to most significant bit. B means bottom,
		// which is the lower value on the secondary axis. T means
		// top and is the opposite of bottom. L means left, which is
		// the lower value on the primary axis. R means right and
		// is the opposite of left.
		
		long contribution = 0; 
		if(corner == 0) {
			// -X -Y -Z
			// NORTH, WEST, DOWN
			contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0001) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0001) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0001) != 0 ? 1 : 0;
		}else if(corner == 1) {
			// +X -Y -Z
			// NORTH, EAST, DOWN
			contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0010) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0001) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0010) != 0 ? 1 : 0;
		}else if(corner == 2) {
			// -X -Y +Z
			// SOUTH, WEST, DOWN
			contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0001) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0010) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b0100) != 0 ? 1 : 0;
		}else if(corner == 3) {
			// +X -Y +Z
			// SOUTH, EAST, DOWN
			contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0010) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0010) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.DOWN.id  * 4)) & 0b1000) != 0 ? 1 : 0;
		}else if(corner == 4) {
			// -X +Y -Z
			// NORTH, WEST, UP
			contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b0100) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b0100) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0001) != 0 ? 1 : 0;
		}else if(corner == 5) {
			// +X +Y -Z
			// NORTH, EAST, UP
			contribution += ((occlusion >>> (Direction.NORTH.id * 4)) & 0b1000) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b0100) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0010) != 0 ? 1 : 0;
		}else if(corner == 6) {
			// -X +Y +Z
			// SOUTH, WEST, UP
			contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b0100) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.WEST.id  * 4)) & 0b1000) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b0100) != 0 ? 1 : 0;
		}else if(corner == 7) {
			// +X +Y +Z
			// SOUTH, EAST, UP
			contribution += ((occlusion >>> (Direction.SOUTH.id * 4)) & 0b1000) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.EAST.id  * 4)) & 0b1000) != 0 ? 1 : 0;
			contribution += ((occlusion >>> (Direction.UP.id    * 4)) & 0b1000) != 0 ? 1 : 0;
		}
		return contribution > 1 ? 1 : 0;
	}
	
	private long getAmbientOcclusionForSide(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize, Direction dir) {
		long ao00 = 0;
		long ao01 = 0;
		long ao10 = 0;
		long ao11 = 0;
		
		if(dir == Direction.NORTH) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz - lodSize, lodSize, lodYSize, 2);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz - lodSize, lodSize, lodYSize, 6);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz - lodSize, lodSize, lodYSize, 3);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 7);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz - lodSize, lodSize, lodYSize, 3);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz - lodSize, lodSize, lodYSize, 7);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz - lodSize, lodSize, lodYSize, 2);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 6);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz - lodSize, lodSize, lodYSize, 6);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz - lodSize, lodSize, lodYSize, 2);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz - lodSize, lodSize, lodYSize, 7);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 3);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz - lodSize, lodSize, lodYSize, 7);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz - lodSize, lodSize, lodYSize, 3);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz - lodSize, lodSize, lodYSize, 6);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 2);
		}else if(dir == Direction.SOUTH) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz + lodSize, lodSize, lodYSize, 0);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz + lodSize, lodSize, lodYSize, 4);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz + lodSize, lodSize, lodYSize, 1);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 5);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz + lodSize, lodSize, lodYSize, 1);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz + lodSize, lodSize, lodYSize, 5);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz + lodSize, lodSize, lodYSize, 0);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 4);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz + lodSize, lodSize, lodYSize, 4);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz + lodSize, lodSize, lodYSize, 0);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz + lodSize, lodSize, lodYSize, 5);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 1);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy           , cz + lodSize, lodSize, lodYSize, 5);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz + lodSize, lodSize, lodYSize, 1);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz + lodSize, lodSize, lodYSize, 4);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 0);
		}else if(dir == Direction.EAST) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz          , lodSize, lodYSize, 0);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 4);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz - lodSize, lodSize, lodYSize, 2);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 6);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz          , lodSize, lodYSize, 2);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 6);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz + lodSize, lodSize, lodYSize, 0);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 4);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz          , lodSize, lodYSize, 4);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 0);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz - lodSize, lodSize, lodYSize, 6);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 2);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz          , lodSize, lodYSize, 6);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 2);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy           , cz + lodSize, lodSize, lodYSize, 4);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 0);
		}else if(dir == Direction.WEST) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz          , lodSize, lodYSize, 1);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 5);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz - lodSize, lodSize, lodYSize, 3);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 7);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz          , lodSize, lodYSize, 3);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 7);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz + lodSize, lodSize, lodYSize, 1);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 5);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz          , lodSize, lodYSize, 5);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 1);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz - lodSize, lodSize, lodYSize, 7);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 3);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz          , lodSize, lodYSize, 7);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 3);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy           , cz + lodSize, lodSize, lodYSize, 5);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 1);
		}else if(dir == Direction.UP) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz          , lodSize, lodYSize, 0);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 1);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz - lodSize, lodSize, lodYSize, 2);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 3);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz          , lodSize, lodYSize, 1);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 0);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz - lodSize, lodSize, lodYSize, 3);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz - lodSize, lodSize, lodYSize, 2);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz          , lodSize, lodYSize, 2);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 3);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz + lodSize, lodSize, lodYSize, 0);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 1);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz          , lodSize, lodYSize, 3);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz          , lodSize, lodYSize, 2);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy + lodYSize, cz + lodSize, lodSize, lodYSize, 1);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy + lodYSize, cz + lodSize, lodSize, lodYSize, 0);
		}else if(dir == Direction.DOWN) {
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz          , lodSize, lodYSize, 4);
			
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 5);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz - lodSize, lodSize, lodYSize, 6);
			ao00 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 7);
			
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz          , lodSize, lodYSize, 5);
			
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 4);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz - lodSize, lodSize, lodYSize, 7);
			ao01 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz - lodSize, lodSize, lodYSize, 6);
			
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz          , lodSize, lodYSize, 6);
			
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 7);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz + lodSize, lodSize, lodYSize, 4);
			ao10 += getAmbientOcclusionContributionFromCorner(chunk, cx - lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 5);
			
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz          , lodSize, lodYSize, 7);
			
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz          , lodSize, lodYSize, 6);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx          , cy - lodYSize, cz + lodSize, lodSize, lodYSize, 5);
			ao11 += getAmbientOcclusionContributionFromCorner(chunk, cx + lodSize, cy - lodYSize, cz + lodSize, lodSize, lodYSize, 4);
		}
		ao00 = Math.min(ao00, 3);
		ao01 = Math.min(ao01, 3);
		ao10 = Math.min(ao10, 3);
		ao11 = Math.min(ao11, 3);
		
		long ao = ao00 | (ao01 << 2) | (ao10 << 4) | (ao11 << 6);
		return ao;
	}
	
	private long getAmbientOcclusion(Chunk chunk, int cx, int cy, int cz, int lodSize, int lodYSize) {
		if(!Config.calculateAmbientOcclusion)
			return 0;
		long ao = 0;
		for(Direction dir : Direction.CACHED_VALUES) {
			ao |= getAmbientOcclusionForSide(chunk, cx, cy, cz, lodSize, lodYSize, dir) << (dir.id * 10);
		}
		return ao;
	}
	
	private long getOcclusionFromDirection(int wx, int wy, int wz, Direction dir, BakedBlockState currentState,
											List<ModelFace> detailedOcclusionFaces, int lodSize, int lodYSize) {
		Chunk chunk = getPrefetchedChunkForBlockPos(wx, wz);
		if (chunk != null && !chunk.hasLoadError()) {
			return getOcclusionFromDirection(chunk, wx - chunk.getChunkX() * 16, wy, wz - chunk.getChunkZ() * 16, 
									dir, currentState, detailedOcclusionFaces, lodSize, lodYSize);
		}
		// No chunk, so return that it's occluded. This gets rid of the side of the world.
		return 0b1111L;
	}
	
	private long getOcclusionFromDirection(Chunk chunk, int bx, int by, int bz, Direction dir, BakedBlockState currentState,
										List<ModelFace> detailedOcclusionFaces, int lodSize, int lodYSize) {
		if(bx < 0 || bx >= 16 || bz < 0 || bz >= 16) {
			return getOcclusionFromDirection(bx + chunk.getChunkX() * 16, by, bz + chunk.getChunkZ() * 16, dir, 
					currentState, detailedOcclusionFaces, lodSize, lodYSize);
		}
		
		int sampleLodSize = getLodSize(chunk.getChunkX(), chunk.getChunkZ());
		int sampleLodYSize = getLodYSize(chunk.getChunkX(), chunk.getChunkZ());
		int sampleLodLevel = Integer.numberOfTrailingZeros(sampleLodSize);
		int sampleLodYLevel = Integer.numberOfTrailingZeros(sampleLodYSize);
		int lodLevel = Integer.numberOfTrailingZeros(lodSize);
		int lodYLevel = Integer.numberOfTrailingZeros(lodYSize);
		
		if(lodSize == sampleLodSize) {
			getLODBlockId(chunk, 
					(bx >> sampleLodLevel) << sampleLodLevel, 
					(by >> sampleLodYLevel) << sampleLodYLevel, 
					(bz >> sampleLodLevel) << sampleLodLevel, 
					sampleLodSize, sampleLodYSize, OCCLUSION_BLOCK_ID);
			
			if(OCCLUSION_BLOCK_ID[0] < 0) {
				// If the block id is less than 0, that means
				// that there was no chunk, so let's say that it does
				// occlude. This gets rid of the side of the world.
				return 0b1111L;
			}
			
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(OCCLUSION_BLOCK_ID[0], OCCLUSION_BLOCK_ID[1],
					OCCLUSION_BLOCK_ID[2], OCCLUSION_BLOCK_ID[3]);

			// Transparent blocks don't occlude non-transparent blocks
			if(state.isTransparentOcclusion() && !currentState.isTransparentOcclusion())
				return 0;
			// Leaves blocks don't occlude non-leaves and non-transparent blocks
			if(state.isLeavesOcclusion() && !currentState.isLeavesOcclusion() && !currentState.isTransparentOcclusion())
				return 0;
			// If both sides are leaves blocks, then only one side kept.
			if(state.isLeavesOcclusion() && currentState.isLeavesOcclusion()) {
				// Safe out that we have another leaf here.
				// Used by the corner algorithm.
				if(!state.isDoubleSided())
					return 0b1L << 32;
				else if(dir == Direction.DOWN || dir == Direction.SOUTH || dir == Direction.WEST)
					return 0b1L << 32;
			}
			
			if(state.isDetailedOcclusion() && currentState.isDetailedOcclusion()) {
				OCCLUSION_MODELS.clear();
				state.getModels(OCCLUSION_BLOCK_ID[1], OCCLUSION_BLOCK_ID[2], OCCLUSION_BLOCK_ID[3], OCCLUSION_MODELS);
				Model model;
				ModelFace face;
				for(int i = 0; i < OCCLUSION_MODELS.size(); ++i) {
					model = OCCLUSION_MODELS.get(i);
					for(int j = 0; j < model.getFaces().size(); ++j) {
						face = model.getFaces().get(j);
						if(face.getOccludedBy() == 0)
							continue;
						if(face.getDirection() == dir || face.getDirection() == dir.getOpposite()) {
							ModelFace faceTmp = new ModelFace(face);
							faceTmp.translate(dir.x * 16.0f, dir.y * 16.0f, dir.z * 16.0f);
							detailedOcclusionFaces.add(faceTmp);
						}
					}
				}
			}
			
			long occludes = state.getOccludes();
			occludes >>= dir.getOpposite().id * 4;
			occludes &= 0b1111L;
			
			if(by < bounds.getMinY())
				occludes = 0b1111L;
			return occludes;
		}else if(lodSize < sampleLodSize) {
			// The occluding block is larger than the current block,
			// so we need to resize the occlusion.
			getLODBlockId(chunk, 
					(bx >> sampleLodLevel) << sampleLodLevel, 
					(by >> sampleLodYLevel) << sampleLodYLevel, 
					(bz >> sampleLodLevel) << sampleLodLevel, 
					sampleLodSize, sampleLodYSize, OCCLUSION_BLOCK_ID);
			if(OCCLUSION_BLOCK_ID[0] < 0) {
				// If the block id is less than 0, that means
				// that there was no chunk, so let's say that it does
				// occlude. This gets rid of the side of the world.
				return 0b1111;
			}
			
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(OCCLUSION_BLOCK_ID[0], OCCLUSION_BLOCK_ID[1],
					OCCLUSION_BLOCK_ID[2], OCCLUSION_BLOCK_ID[3]);

			// Transparent blocks don't occlude non-transparent blocks
			if(state.isTransparentOcclusion() && !currentState.isTransparentOcclusion())
				return 0;
			// Leaves blocks don't occlude non-leaves and non-transparent blocks
			if(state.isLeavesOcclusion() && !currentState.isLeavesOcclusion() && !currentState.isTransparentOcclusion())
				return 0;
			// If both sides are leaves blocks, then only one side kept.
			if(state.isLeavesOcclusion() && currentState.isLeavesOcclusion()) {
				// Safe out that we have another leaf here.
				// Used by the corner algorithm.
				if(!state.isDoubleSided())
					return 0b1L << 32;
				else if(dir == Direction.DOWN || dir == Direction.SOUTH || dir == Direction.WEST)
					return 0b1L << 32;
			}
			
			long occludes = state.getOccludes();
			occludes >>= dir.getOpposite().id * 4;
			occludes &= 0b1111L;
			
			if(by < bounds.getMinY())
				occludes = 0b1111L;
			
			// Now we have our occlusion value, but we need to pick one of the four
			// corners and have that be the occlusion for the entire face.
			if(occludes == 0b1111)
				return occludes; // Fast path
			
			int localX = bx - ((bx >> sampleLodLevel) << sampleLodLevel); 
			int localY = by - ((by >> sampleLodYLevel) << sampleLodYLevel);
			int localZ = bz - ((bz >> sampleLodLevel) << sampleLodLevel);
			int u = 0;
			int v = 0;
			if(dir == Direction.NORTH || dir == Direction.SOUTH) {
				u = localX;
				v = localY;
			}else if(dir == Direction.EAST || dir == Direction.WEST) {
				u = localZ;
				v = localY;
			}else if(dir == Direction.UP || dir == Direction.DOWN) {
				u = localX;
				v = localY;
			}
			
			// Divide by half the lod size so that we get the corner
			u /= (sampleLodSize / 2);
			v /= (sampleLodSize / 2);
			int cornerIndex = (v & 0b1) * 2 + (u & 0b1);
			
			// Check if that corner occludes this block
			if(((occludes >> cornerIndex) & 0b1) != 0)
				return 0b1111;
			return 0b0000;
		}else {
			// The sample blocks are smaller than the current blocks, so we need to
			// sample multiple blocks in order to construct an occlusion value.
			
			int sx = (bx >> lodLevel) << lodLevel;
			int sy = (by >> lodYLevel) << lodYLevel;
			int sz = (bz >> lodLevel) << lodLevel;
			if(dir == Direction.DOWN)
				sy += lodYSize / 2;
			if(dir == Direction.NORTH)
				sz += lodSize / 2;
			if(dir == Direction.WEST)
				sx += lodSize / 2;
			
			long occludes = 0;
			for(int v = 0; v < 2; ++v) {
				for(int u = 0; u < 2; ++u) {
					int cornerIndex = v * 2 + u;
					int x = sx;
					int y = sy;
					int z = sz;
					if(dir == Direction.NORTH || dir == Direction.SOUTH) {
						x += u * (lodSize / 2);
						y += v * (lodYSize / 2);
					}else if(dir == Direction.EAST || dir == Direction.WEST) {
						z += u * (lodSize / 2);
						y += v * (lodYSize / 2);
					}else if(dir == Direction.UP || dir == Direction.DOWN) {
						x += u * (lodSize / 2);
						z += v * (lodSize / 2);
					}
					
					getLODBlockId(chunk, 
							(x >> sampleLodLevel) << sampleLodLevel, 
							(y >> sampleLodYLevel) << sampleLodYLevel, 
							(z >> sampleLodLevel) << sampleLodLevel, 
							sampleLodSize, sampleLodYSize, OCCLUSION_BLOCK_ID);
					
					if(OCCLUSION_BLOCK_ID[0] < 0) {
						// If the block id is less than 0, that means
						// that there was no chunk, so let's say that it does
						// occlude. This gets rid of the side of the world.
						occludes |= 0b1 << cornerIndex;
						continue;
					}
					
					BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(OCCLUSION_BLOCK_ID[0], OCCLUSION_BLOCK_ID[1],
							OCCLUSION_BLOCK_ID[2], OCCLUSION_BLOCK_ID[3]);

					// Transparent blocks don't occlude non-transparent blocks
					if(state.isTransparentOcclusion() && !currentState.isTransparentOcclusion())
						continue;
					// Leaves blocks don't occlude non-leaves and non-transparent blocks
					if(state.isLeavesOcclusion() && !currentState.isLeavesOcclusion() && !currentState.isTransparentOcclusion())
						continue;
					// If both sides are leaves blocks, then only one side kept.
					if(state.isLeavesOcclusion() && currentState.isLeavesOcclusion()) {
						// Safe out that we have another leaf here.
						// Used by the corner algorithm.
						occludes |= 0b1L << 32;
						if(!state.isDoubleSided())
							continue;
						else if(dir == Direction.DOWN || dir == Direction.SOUTH || dir == Direction.WEST)
							continue;
					}
					
					long occludesTmp = state.getOccludes();
					occludesTmp >>= dir.getOpposite().id * 4;
					occludesTmp &= 0b1111L;
					
					if(by < bounds.getMinY())
						occludesTmp = 0b1111L;
					
					if(occludesTmp == 0b1111L)
						occludes |= 0b1L << cornerIndex; // Only set this corner to occluded if it occludes the entire face.
				}
			}
			
			return occludes;
		}
	}
	
	private long getOcclusion(Chunk chunk, int cx, int cy, int cz, BakedBlockState currentState, 
								List<ModelFace> detailedOcclusionFaces, int lodSize, int lodYSize) {
		long occlusion = 0;
		
		if(currentState.isDetailedOcclusion())
			detailedOcclusionFaces.clear();
		
		long occludes = 0;
		for(Direction dir : Direction.CACHED_VALUES) {
			int bx = cx + dir.x * lodSize;
			int by = cy + dir.y * lodYSize;
			int bz = cz + dir.z * lodSize;
			
			occludes = getOcclusionFromDirection(chunk, bx, by, bz, dir, currentState, detailedOcclusionFaces, lodSize, lodYSize);
			if((occludes & (0b1L << 32)) != 0)
				// Safe out that we have another leaf here.
				// Used by the corner algorithm.
				occlusion |= 0b1 << (dir.id + 32);
			
			occludes &= 0b1111;
			occludes <<= dir.id * 4;
			occlusion |= occludes;
		}
		
		if(Config.removeCaves) {
			if(currentState.isCaveBlock() || currentState.hasLiquid()) {
				int wx = chunk.getChunkX() * 16 + cx;
				int wy = cy;
				int wz = chunk.getChunkZ() * 16 + cz;
				if(occlusion == 0b111111111111111111111111L)
					occlusion = Long.MAX_VALUE;
				// Offset the position from which we test if it's in a cave to the top of
				// the LoD block.
				else if(isInCaveCached(wx + (lodSize-1)/2, wy + lodYSize - 1, wz + (lodSize-1)/2)) {
					if(Config.fillInCaves) {
						// If we need to fill in the caves, then
						// we need to check the neighbours and see if
						// any of them aren't in a cave, if so we don't
						// occlude it.
						boolean dontOcclude = false;
						for(Direction dir : Direction.CACHED_VALUES) {
							if(!isInCaveCached(wx + (lodSize-1)/2 + dir.x * lodSize, 
												wy + lodYSize - 1 + dir.y * lodYSize, 
												wz + (lodSize-1)/2 + dir.z * lodSize)) {
								dontOcclude = true;
								break;
							}
						}
						if(!dontOcclude)
							occlusion = Long.MAX_VALUE;
					}else {
						occlusion = Long.MAX_VALUE;
					}
				}
			}
		}
		
		return occlusion;
	}
	
	public boolean isInCaveCached(int wx, int wy, int wz) {
		// Do the isInCave check on half resolution.
		wx &= ~1;
		wy &= ~1;
		wz &= ~1;
		if(this.caveCache != null) {
			byte cached = this.caveCache.get(wx, wy, wz);
			if(cached >= 1)
				return cached > 1;
		}
		boolean res = isInCave(wx, wy, wz);
		if(this.caveCache != null)
			this.caveCache.set(wx, wy, wz, (byte) (res ? 2 : 1));
		return res;
	}
	
	public int sampleHeight(int wx, int wz) {
		Chunk chunk = getPrefetchedChunkForBlockPos(wx, wz);
		if(chunk == null || chunk.hasLoadError())
			return Integer.MAX_VALUE;
		return chunk.getHeight(wx, wz);
	}
	
	public boolean isInCave(int wx, int wy, int wz) {
		// Non-existing chunks are not seen as in caves
		Chunk chunk = getPrefetchedChunkForBlockPos(wx, wz);
		if(chunk == null || chunk.hasLoadError())
			return false;
		
		// First check if the current block is near the surface. If so, it's not in a cave.
		final int surfaceDepth = Config.removeCavesSearchEnergy;
		if((sampleHeight(wx, wz) - surfaceDepth) < wy)
			return false;
		for(int z = wz - Config.removeCavesSurfaceRadius; z <= wz + Config.removeCavesSurfaceRadius; ++z) {
			for(int x = wx - Config.removeCavesSurfaceRadius; x <= wx + Config.removeCavesSurfaceRadius; ++x) {
				if((sampleHeight(x, z) - surfaceDepth) < wy)
					return false;
			}
		}
		
		// Now look for any non-cave blocks. If any non-cave blocks are found, we're not in a cave.
		// We keep going up to find these blocks. If there is air or liquid, then we keep going some more.
		
		// yEnergy is how much we can keep going up. If there's air, we only subtract one.
		// If there's liquid, we subtract nothing. Otherwise we subtract 5.
		// Once yEnergy reaches 0 we stop and we say that we are in a cave.
		int yEnergy = surfaceDepth * 3;
		final int searchRadius = Config.removeCavesSearchRadius;
		final int allowedNonCaveBlocks = Config.removeCavesSearchRadius + 1;
		int numNonCaveBlocks = 0;
		int y = wy;
		int z = wz;
		int x = wx;
		int minZ = wz - searchRadius;
		int maxZ = wz + searchRadius;
		int minX = wx - searchRadius;
		int maxX = wx + searchRadius;
		BakedBlockState state = null;
		int blockId;
		boolean foundLiquid = false;
		boolean foundAir = false;
		
		// Since we skip every other block (y += 2), this can cause
		// some artefacts where one block ends up in a cave, but the 
		// block above it not, and the block above that is in a cave
		// again, etc.
		// We take wy and make sure it's a multiple of two to get
		// rid of this artefacting. We also do this for wx and wz
		// later.
		// Basically, we divide by two and then multiply again.
		// We use bit shifting because it keeps -1 at -1 and doesn't
		// make it 0, which allows it to still work with negative values
		// which can happen in Minecraft.
		wy = (wy >> 1) << 1;
		
		// For performance, first check straight up
		for(y = wy; yEnergy > 0; y += 2) {
			foundLiquid = false;
			foundAir = false;
			blockId = lodSampleBlockId(x, y, z);
			if(blockId < 0) {
				yEnergy -= Config.removeCavesCaveBlockCost;
				continue;
			}
			state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z);
			if(sampleHeight(x, z) < y) {
				// Surface reached, so we aren't in a cave
				return false;
			} else if(state.isAir()) {
				foundAir = true;
			} else if(state.hasLiquid()) {
				foundLiquid = true;
			} else if(state.isCaveBlock()) {
			} else {
				// Non-cave block that also isn't air or liquid
				numNonCaveBlocks++;
				if(numNonCaveBlocks > allowedNonCaveBlocks) {
					return false;
				}
			}
			if(!foundLiquid && foundAir)
				yEnergy -= Config.removeCavesAirCost;
			else if(!foundLiquid)
				yEnergy -= Config.removeCavesCaveBlockCost;
				
		}
		
		minX = (minX >> 1) << 1;
		minZ = (minZ >> 1) << 1;
		maxX = (maxX >> 1) << 1;
		maxZ = (maxZ >> 1) << 1;
		
		yEnergy = surfaceDepth * 3;
		for(y = wy; yEnergy > 0; y += 2) {
			foundLiquid = false;
			foundAir = false;
			for(z = minZ; z <= maxZ; z += 2) {
				for(x = minX; x <= maxX; x += 2) {
					blockId = lodSampleBlockId(x, y, z);
					if(blockId < 0) {
						continue;
					}
					state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z);
					if(sampleHeight(x, z) < y) {
						// Surface reached, so we aren't in a cave
						return false;
					} else if(state.isAir()) {
						foundAir = true;
					} else if(state.hasLiquid()) {
						foundLiquid = true;
					} else if(state.isCaveBlock()) {
					} else {
						// Non-cave block that also isn't air or liquid
						numNonCaveBlocks++;
						if(numNonCaveBlocks > allowedNonCaveBlocks) {
							return false;
						}
					}
				}
			}
			if(!foundLiquid && foundAir)
				yEnergy -= Config.removeCavesAirCost;
			else if(!foundLiquid)
				yEnergy -= Config.removeCavesCaveBlockCost;
				
		}
		
		return true;
	}
	
	public void writeMeshes(LargeDataOutputStream dos) throws IOException {
		dos.writeUTF(name);
		dos.writeByte((MCWorldExporter.getApp().getFGChunks().contains(name) || MCWorldExporter.getApp().getFGChunks().isEmpty()) ? 
				1 : 0); // Is foreground chunk
		dos.writeInt(meshes.size());
		for(Entry<String, Mesh> mesh : meshes.entrySet()) {
			mesh.getValue().write(dos);
		}
		
		// Pretty much all of the code assumes that a block is 16 units.
		// In order to not break any of that, we do the scaling here.
		float worldScale = Config.blockSizeInUnits / 16.0f;
				
		dos.writeInt(individualBlocks.size());
		for(Entry<IndividualBlockId, FloatArray> blocks : individualBlocks.entrySet()) {
			dos.writeInt(blocks.getKey().getBlockId());
			dos.writeInt(blocks.getKey().getX());
			dos.writeInt(blocks.getKey().getY());
			dos.writeInt(blocks.getKey().getZ());
			dos.writeInt(blocks.getValue().size()/3);
			for(int i = 0; i < blocks.getValue().size(); ++i)
				dos.writeFloat(blocks.getValue().get(i) * worldScale);
		}
	}
	
	public Set<IndividualBlockId> getIndividualBlockIds(){
		return individualBlocks.keySet();
	}
	
	public void cleanUp() {
		world = null;
		meshes = null;
		individualBlocks = null;
		//lodCache = null;
		caveCache = null;
		
		System.gc();
	}
	
	public void optimiseMeshes() {
		float threshold = (MCWorldExporter.getApp().getFGChunks().contains(name) || MCWorldExporter.getApp().getFGChunks().isEmpty()) ? 
				Config.fgFullnessThreshold : Config.bgFullnessThreshold;
		
		RaytracingOptimiser raytracingOptimiser = new RaytracingOptimiser(0.0f, null);
		FaceOptimiser faceOptimiser = new FaceOptimiser();
		
		final Map<String, Mesh> optimisedMeshes = new HashMap<String, Mesh>();
		for(Entry<String, Mesh> mesh : meshes.entrySet()) {
			final Mesh inMesh = mesh.getValue();
			final String key = mesh.getKey();
			
			Mesh newMesh = inMesh;
			
			if(Config.runRaytracingOptimiser)
				newMesh = raytracingOptimiser.optimiseMesh(inMesh, threshold);
			
			if(Config.runFaceOptimiser) {
				if(newMesh instanceof MeshGroup) {
					MeshGroup newMeshGroup = (MeshGroup) newMesh;
					for(int i = 0; i < newMeshGroup.getNumChildren(); ++i) {
						//Mesh faceOptimsed = FaceOptimiser.optimise(newMeshGroup.getChildren().get(i));
						//newMeshGroup.getChildren().set(i, faceOptimsed);
						faceOptimiser.optimise(newMeshGroup.getChildren().get(i));
					}
				}else {
					//newMesh = faceOptimiser.optimise(newMesh);
					faceOptimiser.optimise(newMesh);
				}
			}
			
			synchronized(optimisedMeshes) {
				optimisedMeshes.put(key, newMesh);
			}
			MCWorldExporter.getApp().getUI().getProgressBar().finishedOptimising(meshes.size());
		}
		meshes = optimisedMeshes;
	}
	
	public void optimiseAndWriteMeshes(LargeDataOutputStream dos) throws Exception {
		float threshold = (MCWorldExporter.getApp().getFGChunks().contains(name) || MCWorldExporter.getApp().getFGChunks().isEmpty()) ? 
				Config.fgFullnessThreshold : Config.bgFullnessThreshold;
		
		MeshProcessors processors = new MeshProcessors("mesh_chunk_" + chunkX + "_" + chunkZ);
		if(Config.runOptimiser) {
			if(Config.runRaytracingOptimiser) {
				RaytracingOptimiser raytracingOptimiser = new RaytracingOptimiser(threshold, dos);
				processors.addProcessor(raytracingOptimiser);
			}
			if(Config.runFaceOptimiser) {
				FaceOptimiser faceOptimiser = new FaceOptimiser();
				processors.addProcessor(faceOptimiser);
			}
		}
		
		WriteProcessor writeProcessor = new WriteProcessor(dos);
		processors.addProcessor(writeProcessor);
		
		int meshMergerId = -1;
		if(Config.useGeometerySubsets)
			meshMergerId = processors.beginMeshMerger(MeshMergerMode.MERGE);
		
		dos.writeUTF(name);
		dos.writeByte((MCWorldExporter.getApp().getFGChunks().contains(name) || MCWorldExporter.getApp().getFGChunks().isEmpty()) ? 
				1 : 0); // Is foreground chunk
		//dos.writeInt(meshes.size());
		//dos.writeInt(meshes.size());
		for(Entry<String, Mesh> mesh : meshes.entrySet()) {
			processors.process(mesh.getValue());

			MCWorldExporter.getApp().getUI().getProgressBar().finishedOptimising(meshes.size() + 2);
		}
		if(meshMergerId >= 0)
			processors.endMeshMerger(meshMergerId);
		

		MCWorldExporter.getApp().getUI().getProgressBar().finishedOptimising(meshes.size() + 2);
		dos.writeByte(0); // Array of meshes end with a 0
		
		// Pretty much all of the code assumes that a block is 16 units.
		// In order to not break any of that, we do the scaling here.
		float worldScale = Config.blockSizeInUnits / 16.0f;
				
		dos.writeInt(individualBlocks.size());
		for(Entry<IndividualBlockId, FloatArray> blocks : individualBlocks.entrySet()) {
			dos.writeInt(blocks.getKey().getBlockId());
			dos.writeInt(blocks.getKey().getX());
			dos.writeInt(blocks.getKey().getY());
			dos.writeInt(blocks.getKey().getZ());
			dos.writeInt(blocks.getValue().size()/3);
			for(int i = 0; i < blocks.getValue().size(); ++i)
				dos.writeFloat(blocks.getValue().get(i) * worldScale);
		}

		MCWorldExporter.getApp().getUI().getProgressBar().finishedOptimising(meshes.size() + 2);
	}

}
