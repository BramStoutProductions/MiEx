package nl.bramstout.mcworldexporter.lighting;

import java.util.Arrays;
import java.util.List;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.VertexColorSet;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.world.Chunk;

public class BlockLightingCache {
	
	private static class BlockLightingData{
		
		byte skyLight;
		byte blockLight1;
		byte blockLight2;
		byte blockLight3;
		byte blockLight4;
		byte blockLight5;
		byte blockLight6;
		byte blockLight7;
		short blockLightColor1;
		short blockLightColor2;
		short blockLightColor3;
		short blockLightColor4;
		short blockLightColor5;
		short blockLightColor6;
		short blockLightColor7;
		byte attenuationX;
		byte attenuationY;
		byte attenuationZ;
		
		public boolean isSolid() {
			return attenuationX == 127 && attenuationY == 127 && attenuationZ == 127;
		}
		
		public boolean isSolid(int dx, int dy, int dz) {
			if(dx != 0 && attenuationX == 127)
				return true;
			if(dy != 0 && attenuationY == 127)
				return true;
			if(dz != 0 && attenuationZ == 127)
				return true;
			return false;
		}
		
		public void combine(BlockLightingData other, Direction dir) {
			byte attenuation = 0;
			switch(dir) {
			case UP:
			case DOWN:
				attenuation = attenuationY;
				break;
			case NORTH:
			case SOUTH:
				attenuation = attenuationZ;
				break;
			case EAST:
			case WEST:
				attenuation = attenuationX;
				break;
			}
			
			skyLight = (byte) Math.max(skyLight, other.skyLight - attenuation);
			addBlockLight(other.blockLight1 - attenuation, other.blockLightColor1);
			addBlockLight(other.blockLight2 - attenuation, other.blockLightColor2);
			addBlockLight(other.blockLight3 - attenuation, other.blockLightColor3);
			addBlockLight(other.blockLight4 - attenuation, other.blockLightColor4);
			addBlockLight(other.blockLight5 - attenuation, other.blockLightColor5);
			addBlockLight(other.blockLight6 - attenuation, other.blockLightColor6);
			addBlockLight(other.blockLight7 - attenuation, other.blockLightColor7);
		}
		
		private void addBlockLight(int lightLevel, short lightColor) {
			if(lightLevel <= 0)
				return;
			// Find the index that matches the light colour
			if(blockLightColor1 == lightColor) {
				blockLight1 = (byte) Math.max(blockLight1, lightLevel);
			}else if(blockLightColor2 == lightColor) {
				blockLight2 = (byte) Math.max(blockLight2, lightLevel);
			}else if(blockLightColor3 == lightColor) {
				blockLight3 = (byte) Math.max(blockLight3, lightLevel);
			}else if(blockLightColor4 == lightColor) {
				blockLight4 = (byte) Math.max(blockLight4, lightLevel);
			}else if(blockLightColor5 == lightColor) {
				blockLight5 = (byte) Math.max(blockLight5, lightLevel);
			}else if(blockLightColor6 == lightColor) {
				blockLight6 = (byte) Math.max(blockLight6, lightLevel);
			}else if(blockLightColor7 == lightColor) {
				blockLight7 = (byte) Math.max(blockLight7, lightLevel);
			}else {
				// Doesn't match any, so add it as a new slot.
				if(blockLight1 == 0) {
					blockLight1 = (byte) lightLevel;
					blockLightColor1 = lightColor;
				}else if(blockLight2 == 0) {
					blockLight2 = (byte) lightLevel;
					blockLightColor2 = lightColor;
				}else if(blockLight3 == 0) {
					blockLight3 = (byte) lightLevel;
					blockLightColor3 = lightColor;
				}else if(blockLight4 == 0) {
					blockLight4 = (byte) lightLevel;
					blockLightColor4 = lightColor;
				}else if(blockLight5 == 0) {
					blockLight5 = (byte) lightLevel;
					blockLightColor5 = lightColor;
				}else if(blockLight6 == 0) {
					blockLight6 = (byte) lightLevel;
					blockLightColor6 = lightColor;
				}else if(blockLight7 == 0) {
					blockLight7 = (byte) lightLevel;
					blockLightColor7 = lightColor;
				}else {
					// Looks like no free slots available, so let's pick the one
					// with the lowest light level. We want to keep the brightest ones.
					byte minLightLevel = (byte) Math.min(blockLight1, Math.min(blockLight2, 
									Math.min(blockLight3, Math.min(blockLight4, 
									Math.min(blockLight5, Math.min(blockLight6, blockLight7))))));
					if(lightLevel > minLightLevel) {
						if(blockLight1 == minLightLevel) {
							blockLight1 = (byte) lightLevel;
							blockLightColor1 = lightColor;
						}else if(blockLight2 == minLightLevel) {
							blockLight2 = (byte) lightLevel;
							blockLightColor2 = lightColor;
						}else if(blockLight3 == minLightLevel) {
							blockLight3 = (byte) lightLevel;
							blockLightColor3 = lightColor;
						}else if(blockLight4 == minLightLevel) {
							blockLight4 = (byte) lightLevel;
							blockLightColor4 = lightColor;
						}else if(blockLight5 == minLightLevel) {
							blockLight5 = (byte) lightLevel;
							blockLightColor5 = lightColor;
						}else if(blockLight6 == minLightLevel) {
							blockLight6 = (byte) lightLevel;
							blockLightColor6 = lightColor;
						}else if(blockLight7 == minLightLevel) {
							blockLight7 = (byte) lightLevel;
							blockLightColor7 = lightColor;
						}
					}
				}
			}
		}
		
	}
	
	private static class BlockLightingChunk{
		
		int chunkX;
		int chunkZ;
		int minY;
		int maxY;
		byte[] lightLevels;
		short[] lightColors;
		byte[] attenuations;
		int maxYFound;
		
		public BlockLightingChunk(Chunk chunk, int minY, int maxY, byte skyLightLevel) {
			this.chunkX = chunk.getChunkX();
			this.chunkZ = chunk.getChunkZ();
			this.minY = minY;
			this.maxY = maxY;
			this.maxYFound = Integer.MIN_VALUE;
			lightLevels = new byte[16 * 16 * (maxY - minY) * 8];
			lightColors = new short[16 * 16 * (maxY - minY) * 7];
			attenuations = new byte[16 * 16 * (maxY - minY) * 3];
			if(chunk.hasLoadError())
				return;
			
			// Setting up the initial values
			for(int y = minY; y < maxY; ++y) {
				for(int z = 0; z < 16; ++z) {
					for(int x = 0; x < 16; ++x) {
						int index = (y - minY) * 16 * 16 + z * 16 + x;
						int indexLightLevels = index * 8;
						int indexLightColors = index * 7;
						int indexAttenuations = index * 3;
						int wx = x + this.chunkX * 16;
						int wy = y;
						int wz = z + this.chunkZ * 16;
						
						byte blockLightLevel = 0;
						short blockLightColor = 0;
						byte attenuationX = 0;
						byte attenuationY = 0;
						byte attenuationZ = 0;
						
						for(int layer = 0; layer < chunk.getLayerCount(); ++layer) {
							int blockId = chunk.getBlockIdLocal(x, y, z, layer);
							BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, wx, wy, wz, layer);
							
							if(state.getEmissiveLightLevel() > blockLightLevel) {
								blockLightLevel = state.getEmissiveLightLevel();
								blockLightColor = state.getEmissiveLightColor();
							}
							attenuationX = (byte) Math.max(attenuationX, state.getLightAttenuationX());
							attenuationY = (byte) Math.max(attenuationY, state.getLightAttenuationY());
							attenuationZ = (byte) Math.max(attenuationZ, state.getLightAttenuationZ());
						}
						
						if(y > chunk.getHeightLocal(x, z)) {
							lightLevels[indexLightLevels] = skyLightLevel;
						}
						lightLevels[indexLightLevels + 1] = blockLightLevel;
						lightColors[indexLightColors] = blockLightColor;
						attenuations[indexAttenuations] = attenuationX;
						attenuations[indexAttenuations + 1] = attenuationY;
						attenuations[indexAttenuations + 2] = attenuationZ;
					}
				}
			}
			// Propagate skylight for blocks with no attenuation
			// like glass for example.
			for(int z = 0; z < 16; ++z) {
				for(int x = 0; x < 16; ++x) {
					int yTop = chunk.getHeightLocal(x, z);
					maxYFound = Math.max(maxYFound, yTop);
					for(int y = yTop; y >= minY; --y) {
						boolean attenuates = false;
						for(int layer = 0; layer < chunk.getLayerCount(); ++layer) {
							int blockId = chunk.getBlockIdLocal(x, y, z, layer);
							BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, layer);
							
							if(state.getLightAttenuationY() > 0) {
								attenuates = true;
								break;
							}
						}
						if(attenuates)
							break;
						
						// Block doesn't attenuate to allow skylight to move down.
						if(y < maxY) {
							int index = (y - minY) * 16 * 16 + z * 16 + x;
							int indexLightLevels = index * 8;
							lightLevels[indexLightLevels] = skyLightLevel;
						}
					}
				}
			}
		}
		
		public void get(int x, int y, int z, BlockLightingData out) {
			x -= chunkX * 16;
			z -= chunkZ * 16;
			getLocal(x, y, z, out);
		}
		
		public void getLocal(int x, int y, int z, BlockLightingData out) {
			if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < minY || y >= maxY) {
				out.skyLight = 0;
				out.blockLight1 = 0;
				out.blockLight2 = 0;
				out.blockLight3 = 0;
				out.blockLight4 = 0;
				out.blockLight5 = 0;
				out.blockLight6 = 0;
				out.blockLight7 = 0;
				out.blockLightColor1 = 0;
				out.blockLightColor2 = 0;
				out.blockLightColor3 = 0;
				out.blockLightColor4 = 0;
				out.blockLightColor5 = 0;
				out.blockLightColor6 = 0;
				out.blockLightColor7 = 0;
				out.attenuationX = 0;
				out.attenuationY = 0;
				out.attenuationZ = 0;
				return;
			}
			y -= minY;
			int index = y * 16 * 16 + z * 16 + x;
			int indexLightLevels = index * 8;
			int indexLightColors = index * 7;
			int indexAttenuations = index * 3;
			out.skyLight = lightLevels[indexLightLevels];
			out.blockLight1 = lightLevels[indexLightLevels + 1];
			out.blockLight2 = lightLevels[indexLightLevels + 2];
			out.blockLight3 = lightLevels[indexLightLevels + 3];
			out.blockLight4 = lightLevels[indexLightLevels + 4];
			out.blockLight5 = lightLevels[indexLightLevels + 5];
			out.blockLight6 = lightLevels[indexLightLevels + 6];
			out.blockLight7 = lightLevels[indexLightLevels + 7];
			out.blockLightColor1 = lightColors[indexLightColors];
			out.blockLightColor2 = lightColors[indexLightColors + 1];
			out.blockLightColor3 = lightColors[indexLightColors + 2];
			out.blockLightColor4 = lightColors[indexLightColors + 3];
			out.blockLightColor5 = lightColors[indexLightColors + 4];
			out.blockLightColor6 = lightColors[indexLightColors + 5];
			out.blockLightColor7 = lightColors[indexLightColors + 6];
			out.attenuationX = attenuations[indexAttenuations];
			out.attenuationY = attenuations[indexAttenuations + 1];
			out.attenuationZ = attenuations[indexAttenuations + 2];
		}
		
		public void setLocal(int x, int y, int z, BlockLightingData in) {
			if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < minY || y >= maxY)
				return;
			y -= minY;
			int index = y * 16 * 16 + z * 16 + x;
			int indexLightLevels = index * 8;
			int indexLightColors = index * 7;
			
			lightLevels[indexLightLevels] = in.skyLight;
			lightLevels[indexLightLevels + 1] = in.blockLight1;
			lightLevels[indexLightLevels + 2] = in.blockLight2;
			lightLevels[indexLightLevels + 3] = in.blockLight3;
			lightLevels[indexLightLevels + 4] = in.blockLight4;
			lightLevels[indexLightLevels + 5] = in.blockLight5;
			lightLevels[indexLightLevels + 6] = in.blockLight6;
			lightLevels[indexLightLevels + 7] = in.blockLight7;
			lightColors[indexLightColors] = in.blockLightColor1;
			lightColors[indexLightColors + 1] = in.blockLightColor2;
			lightColors[indexLightColors + 2] = in.blockLightColor3;
			lightColors[indexLightColors + 3] = in.blockLightColor4;
			lightColors[indexLightColors + 4] = in.blockLightColor5;
			lightColors[indexLightColors + 5] = in.blockLightColor6;
			lightColors[indexLightColors + 6] = in.blockLightColor7;
		}
		
	}
	
	private int minChunkX;
	private int minChunkZ;
	private int maxChunkX;
	private int maxChunkZ;
	private int minY;
	private int maxY;
	private int maxYFound;
	private BlockLightingChunk[] chunks;
	private byte maxLightLevel;
	
	public BlockLightingCache(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, int minY, int maxY) {
		this.minChunkX = minChunkX;
		this.minChunkZ = minChunkZ;
		this.maxChunkX = maxChunkX;
		this.maxChunkZ = maxChunkZ;
		this.minY = minY;
		this.maxY = maxY;
		this.maxYFound = Integer.MIN_VALUE;
		this.chunks = new BlockLightingChunk[(maxChunkX - minChunkX) * (maxChunkZ - minChunkZ)];
		byte skyLight = Lighting.getLightMap((short) 0).getMaxLightLevel();
		this.maxLightLevel = Lighting.getMaxLightLevel();
		for(int z = minChunkZ; z < maxChunkZ; ++z) {
			for(int x = minChunkX; x < maxChunkX; ++x) {
				try {
					Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(x, z);
					if(chunk == null || chunk.hasLoadError())
						continue;
					int index = (z - minChunkZ) * (maxChunkZ - minChunkZ) + (x - minChunkX);
					this.chunks[index] = new BlockLightingChunk(chunk, minY, maxY, skyLight);
					this.maxYFound = Math.max(maxYFound, this.chunks[index].maxYFound);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		this.maxYFound += this.maxLightLevel;
	}
	
	private void get(int x, int y, int z, BlockLightingData out) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		if(chunkX < minChunkX || chunkX >= maxChunkX || chunkZ < minChunkZ || chunkZ >= maxChunkZ)
			return;
		chunkX -= minChunkX;
		chunkZ -= minChunkZ;
		BlockLightingChunk chunk = chunks[chunkZ * (maxChunkZ - minChunkZ) + chunkX];
		if(chunk == null)
			return;
		chunk.get(x, y, z, out);
	}
	
	public void calculateLighting() {
		for(byte i = 0; i < maxLightLevel; ++i) {
			floodFillIteration();
		}
	}
	
	private void floodFillIteration() {
		BlockLightingData currentLighting = new BlockLightingData();
		BlockLightingData neighbourLighting = new BlockLightingData();
		
		for(int chunkZ = 0; chunkZ < (maxChunkZ - minChunkZ); ++chunkZ) {
			for(int chunkX = 0; chunkX < (maxChunkX - minChunkX); ++chunkX) {
				BlockLightingChunk chunk = chunks[chunkZ * (maxChunkZ - minChunkZ) + chunkX];
				if(chunk == null)
					continue;
				
				for(int y = minY; y < maxYFound; ++y) {
					for(int z = 0; z < 16; ++z) {
						for(int x = 0; x < 16; ++x) {
							chunk.getLocal(x, y, z, currentLighting);
							
							for(Direction dir : Direction.CACHED_VALUES) {
								int sampleX = x + dir.x;
								int sampleY = y + dir.y;
								int sampleZ = z + dir.z;
								if(sampleY < minY || sampleY >= maxY)
									continue;
								
								BlockLightingChunk sampleChunk = chunk;
								if(sampleX < 0) {
									if(chunkX == 0)
										continue;
									
									sampleX += 16;
									sampleChunk = chunks[chunkZ * (maxChunkZ - minChunkZ) + (chunkX - 1)];
								}else if(sampleX >= 16) {
									if(chunkX == (maxChunkX-minChunkX-1))
										continue;
									
									sampleX -= 16;
									sampleChunk = chunks[chunkZ * (maxChunkZ - minChunkZ) + (chunkX + 1)];
								}
								
								if(sampleZ < 0) {
									if(chunkZ == 0)
										continue;
									
									sampleZ += 16;
									sampleChunk = chunks[(chunkZ - 1) * (maxChunkZ - minChunkZ) + chunkX];
								}else if(sampleZ >= 16) {
									if(chunkZ == (maxChunkZ-minChunkZ-1))
										continue;
									
									sampleZ -= 16;
									sampleChunk = chunks[(chunkZ + 1) * (maxChunkZ - minChunkZ) + chunkX];
								}
								if(sampleChunk == null)
									continue;
								
								sampleChunk.getLocal(sampleX, sampleY, sampleZ, neighbourLighting);
								
								currentLighting.combine(neighbourLighting, dir);
							}
							
							chunk.setLocal(x, y, z, currentLighting);
						}
					}
				}
			}
		}
	}
	
	private static class LightingData{
		
		public List<String> names;
		public Color[] colors;
		
		public LightingData() {
			this.names = Lighting.getColorSetNames();
			this.colors = new Color[this.names.size()];
			for(int i = 0; i < this.colors.length; ++i)
				this.colors[i] = new Color(0f, 0f, 0f);
		}
		
		public void combine(LightingData d0, LightingData d1, LightingData d2, LightingData d3) {
			for(int i = 0; i < colors.length; ++i) {
				float weight = 0f;
				if(d0 != null) {
					this.colors[i].add(d0.colors[i]);
					weight += 1f;
				}
				if(d1 != null) {
					this.colors[i].add(d1.colors[i]);
					weight += 1f;
				}
				if(d2 != null) {
					this.colors[i].add(d2.colors[i]);
					weight += 1f;
				}
				if(d3 != null) {
					this.colors[i].add(d3.colors[i]);
					weight += 1f;
				}
				if(weight > 0f)
					this.colors[i].mult(1.0f / weight);
			}
		}
		
		public LightingData lerp(LightingData other, float t) {
			LightingData res = new LightingData();
			for(int i = 0; i < colors.length; ++i) {
				res.colors[i] = colors[i].lerp(other.colors[i], t);
			}
			return res;
		}
		
	}
	
	public VertexColorSet.VertexColorFace[] getLightingForFace(ModelFace face, int x, int y, int z, 
									VertexColorSet.VertexColorFace[] vertexColors){
		Direction dir = face.getDirection();
		
		BlockLightingData d000 = new BlockLightingData();
		BlockLightingData d001 = new BlockLightingData();
		BlockLightingData d002 = new BlockLightingData();
		BlockLightingData d010 = new BlockLightingData();
		BlockLightingData d011 = new BlockLightingData();
		BlockLightingData d012 = new BlockLightingData();
		BlockLightingData d020 = new BlockLightingData();
		BlockLightingData d021 = new BlockLightingData();
		BlockLightingData d022 = new BlockLightingData();
		
		BlockLightingData d100 = new BlockLightingData();
		BlockLightingData d101 = new BlockLightingData();
		BlockLightingData d102 = new BlockLightingData();
		BlockLightingData d110 = new BlockLightingData();
		BlockLightingData d111 = new BlockLightingData();
		BlockLightingData d112 = new BlockLightingData();
		BlockLightingData d120 = new BlockLightingData();
		BlockLightingData d121 = new BlockLightingData();
		BlockLightingData d122 = new BlockLightingData();
		
		get(x - dir.rightX - dir.upX, y - dir.rightY - dir.upY, z - dir.rightZ - dir.upZ, d000);
		get(x              - dir.upX, y              - dir.upY, z              - dir.upZ, d001);
		get(x + dir.rightX - dir.upX, y + dir.rightY - dir.upY, z + dir.rightZ - dir.upZ, d002);
		
		get(x - dir.rightX          , y - dir.rightY          , z - dir.rightZ          , d010);
		get(x                       , y                       , z                       , d011);
		get(x + dir.rightX          , y + dir.rightY          , z + dir.rightZ          , d012);

		get(x - dir.rightX + dir.upX, y - dir.rightY + dir.upY, z - dir.rightZ + dir.upZ, d020);
		get(x              + dir.upX, y              + dir.upY, z              + dir.upZ, d021);
		get(x + dir.rightX + dir.upX, y + dir.rightY + dir.upY, z + dir.rightZ + dir.upZ, d022);
		
		get(x - dir.rightX - dir.upX + dir.x, y - dir.rightY - dir.upY + dir.y, z - dir.rightZ - dir.upZ + dir.z, d100);
		get(x              - dir.upX + dir.x, y              - dir.upY + dir.y, z              - dir.upZ + dir.z, d101);
		get(x + dir.rightX - dir.upX + dir.x, y + dir.rightY - dir.upY + dir.y, z + dir.rightZ - dir.upZ + dir.z, d102);
		
		get(x - dir.rightX           + dir.x, y - dir.rightY           + dir.y, z - dir.rightZ           + dir.z, d110);
		get(x                        + dir.x, y                        + dir.y, z                        + dir.z, d111);
		get(x + dir.rightX           + dir.x, y + dir.rightY           + dir.y, z + dir.rightZ           + dir.z, d112);

		get(x - dir.rightX + dir.upX + dir.x, y - dir.rightY + dir.upY + dir.y, z - dir.rightZ + dir.upZ + dir.z, d120);
		get(x              + dir.upX + dir.x, y              + dir.upY + dir.y, z              + dir.upZ + dir.z, d121);
		get(x + dir.rightX + dir.upX + dir.x, y + dir.rightY + dir.upY + dir.y, z + dir.rightZ + dir.upZ + dir.z, d122);
		
		// If two adjacent sides are solid, then they occlude the corner,
		// so we don't want to include the corner then.
		if((d001.isSolid() && d010.isSolid()) || d000.isSolid(-dir.rightX - dir.upX, -dir.rightY - dir.upY, -dir.rightZ - dir.upZ))
			d000 = null;
		if((d001.isSolid() && d012.isSolid()) || d002.isSolid( dir.rightX - dir.upX,  dir.rightY - dir.upY,  dir.rightZ - dir.upZ))
			d002 = null;
		if((d021.isSolid() && d010.isSolid()) || d020.isSolid(-dir.rightX + dir.upX, -dir.rightY + dir.upY, -dir.rightZ + dir.upZ))
			d020 = null;
		if((d021.isSolid() && d012.isSolid()) || d022.isSolid( dir.rightX + dir.upX,  dir.rightY + dir.upY,  dir.rightZ + dir.upZ))
			d022 = null;
		
		if((d101.isSolid() && d110.isSolid()) || d100.isSolid(-dir.rightX - dir.upX, -dir.rightY - dir.upY, -dir.rightZ - dir.upZ))
			d100 = null;
		if((d101.isSolid() && d112.isSolid()) || d102.isSolid( dir.rightX - dir.upX,  dir.rightY - dir.upY,  dir.rightZ - dir.upZ))
			d102 = null;
		if((d121.isSolid() && d110.isSolid()) || d120.isSolid(-dir.rightX + dir.upX, -dir.rightY + dir.upY, -dir.rightZ + dir.upZ))
			d120 = null;
		if((d121.isSolid() && d112.isSolid()) || d122.isSolid( dir.rightX + dir.upX,  dir.rightY + dir.upY,  dir.rightZ + dir.upZ))
			d122 = null;
		
		// Check if the sides are solid, if so their lighting
		// would be 0, which we don't want to affect our lighting.
		// Otherwise, it would create a sort of ambient occlusion effect,
		// but we have a separate system for that.
		if(d001.isSolid(-dir.upX, -dir.upY, -dir.upZ))
			d001 = null;
		if(d021.isSolid(dir.upX, dir.upY, dir.upZ))
			d021 = null;
		if(d010.isSolid(-dir.rightX, -dir.rightY, -dir.rightZ))
			d010 = null;
		if(d012.isSolid(dir.rightX, dir.rightY, dir.rightZ))
			d012 = null;
		if(d101.isSolid(-dir.upX, -dir.upY, -dir.upZ))
			d101 = null;
		if(d121.isSolid(dir.upX, dir.upY, dir.upZ))
			d121 = null;
		if(d110.isSolid(-dir.rightX, -dir.rightY, -dir.rightZ))
			d110 = null;
		if(d112.isSolid(dir.rightX, dir.rightY, dir.rightZ))
			d112 = null;
		
		LightingData l000 = getLighting(d000);
		LightingData l001 = getLighting(d001);
		LightingData l002 = getLighting(d002);
		LightingData l010 = getLighting(d010);
		LightingData l011 = getLighting(d011);
		LightingData l012 = getLighting(d012);
		LightingData l020 = getLighting(d020);
		LightingData l021 = getLighting(d021);
		LightingData l022 = getLighting(d022);

		LightingData l100 = getLighting(d100);
		LightingData l101 = getLighting(d101);
		LightingData l102 = getLighting(d102);
		LightingData l110 = getLighting(d110);
		LightingData l111 = getLighting(d111);
		LightingData l112 = getLighting(d112);
		LightingData l120 = getLighting(d120);
		LightingData l121 = getLighting(d121);
		LightingData l122 = getLighting(d122);
		
		LightingData lc000 = combine(l000, l001, l010, l011);
		LightingData lc001 = combine(l001, l002, l011, l012);
		LightingData lc010 = combine(l010, l011, l020, l021);
		LightingData lc011 = combine(l011, l012, l021, l022);
		
		LightingData lc100 = combine(l100, l101, l110, l111);
		LightingData lc101 = combine(l101, l102, l111, l112);
		LightingData lc110 = combine(l110, l111, l120, l121);
		LightingData lc111 = combine(l111, l112, l121, l122);
		
		// If the current block is solid in the direction that we
		// are facing, then it's light value will most likely be 0.
		// To then still interpolate, would mean that things like
		// slabs and stairs are seen as if they are in shadow,
		// even though they shouldn't be.
		boolean forceSide = d011.isSolid(dir.x, dir.y, dir.z);
		// If there is a solid block right above, that would cause
		// a shadow, so in that case, let's not do the force side.
		if(d111 == null || d111.isSolid(dir.x, dir.y, dir.z))
			forceSide = false;
		
		LightingData vert0 = interpolate(lc000, lc001, lc010, lc011, lc100, lc101, lc110, lc111, dir, 
				face.getPoints()[0], face.getPoints()[1], face.getPoints()[2], forceSide);
		LightingData vert1 = interpolate(lc000, lc001, lc010, lc011, lc100, lc101, lc110, lc111, dir, 
				face.getPoints()[3], face.getPoints()[4], face.getPoints()[5], forceSide);
		LightingData vert2 = interpolate(lc000, lc001, lc010, lc011, lc100, lc101, lc110, lc111, dir, 
				face.getPoints()[6], face.getPoints()[7], face.getPoints()[8], forceSide);
		LightingData vert3 = interpolate(lc000, lc001, lc010, lc011, lc100, lc101, lc110, lc111, dir, 
				face.getPoints()[9], face.getPoints()[10], face.getPoints()[11], forceSide);
		
		if(vert0.colors.length > 0 && vertexColors == null) {
			vertexColors = new VertexColorSet.VertexColorFace[0];
		}
		for(int i = 0; i < vert0.colors.length; ++i) {
			String name = vert0.names.get(i);
			int colorSetI = -1;
			for(int j = 0; j < vertexColors.length; ++j) {
				// Name strings are interned so we can do the ==
				// to check if they are the same, which is faster than equals()
				if(vertexColors[j].name == name) {
					colorSetI = j;
					break;
				}
			}
			if(colorSetI == -1) {
				vertexColors = Arrays.copyOf(vertexColors, vertexColors.length + 1);
				colorSetI = vertexColors.length - 1;
			}
			vertexColors[colorSetI] = new VertexColorSet.VertexColorFace(name, 3);
			
			vertexColors[colorSetI].r0 = vert0.colors[i].getR();
			vertexColors[colorSetI].g0 = vert0.colors[i].getG();
			vertexColors[colorSetI].b0 = vert0.colors[i].getB();

			vertexColors[colorSetI].r1 = vert1.colors[i].getR();
			vertexColors[colorSetI].g1 = vert1.colors[i].getG();
			vertexColors[colorSetI].b1 = vert1.colors[i].getB();

			vertexColors[colorSetI].r2 = vert2.colors[i].getR();
			vertexColors[colorSetI].g2 = vert2.colors[i].getG();
			vertexColors[colorSetI].b2 = vert2.colors[i].getB();

			vertexColors[colorSetI].r3 = vert3.colors[i].getR();
			vertexColors[colorSetI].g3 = vert3.colors[i].getG();
			vertexColors[colorSetI].b3 = vert3.colors[i].getB();
		}
		
		return vertexColors;
	}
	
	private LightingData getLighting(BlockLightingData data) {
		if(data == null)
			return null;
		LightingData res = new LightingData();
		float[] totalLightValue = new float[res.colors.length];
		float[] maxLightValue = new float[res.colors.length];
		
		// Skylight always has a lightColor of 0
		addLightingData(data.skyLight, (short) 0, res, totalLightValue, maxLightValue);
		
		if(data.blockLight1 > 0)
			addLightingData(data.blockLight1, data.blockLightColor1, res, totalLightValue, maxLightValue);
		if(data.blockLight2 > 0)
			addLightingData(data.blockLight2, data.blockLightColor2, res, totalLightValue, maxLightValue);
		if(data.blockLight3 > 0)
			addLightingData(data.blockLight3, data.blockLightColor3, res, totalLightValue, maxLightValue);
		if(data.blockLight4 > 0)
			addLightingData(data.blockLight4, data.blockLightColor4, res, totalLightValue, maxLightValue);
		if(data.blockLight5 > 0)
			addLightingData(data.blockLight5, data.blockLightColor5, res, totalLightValue, maxLightValue);
		if(data.blockLight6 > 0)
			addLightingData(data.blockLight6, data.blockLightColor6, res, totalLightValue, maxLightValue);
		if(data.blockLight7 > 0)
			addLightingData(data.blockLight7, data.blockLightColor7, res, totalLightValue, maxLightValue);
		
		if(!Config.blockLightingAdditive) {
			for(int i = 0; i < res.colors.length; ++i) {
				if(totalLightValue[i] > 0f) {
					res.colors[i].mult(maxLightValue[i] / totalLightValue[i]);
				}
			}
		}
		
		return res;
	}
	
	private void addLightingData(byte lightLevel, short lightColor, LightingData data, float[] totalLightValue, float[] maxLightValue) {
		LightMap lightMap = Lighting.getLightMap(lightColor);
		if(lightMap == null)
			return;
		int index = -1;
		for(int i = 0; i < data.names.size(); ++i) {
			// The names and color set names are interned,
			// so we can use == instead of equals()
			if(data.names.get(i) == lightMap.getColorSet()) {
				index = i;
				break;
			}
		}
		if(index == -1)
			// Shouldn't ever happen, but check for it just in case.
			return;
		if(lightMap.getMaxLightLevel() <= 0)
			return;
		
		// Clamp lightLevel to 1+, so that it always has some weight.
		// This is mainly for skyLight which even at light level 0,
		// we might still want some ambient light.
		float lightLevelF = ((float) Math.max(lightLevel, 1)) / ((float) lightMap.getMaxLightLevel());
		lightLevelF = (float) Math.pow(lightLevelF, lightMap.getLightGamma());
		if(Config.blockLightingAdditive)
			lightLevelF = 1f;
		
		data.colors[index].addWeighted(lightMap.getColor(lightLevel), 1f);
		totalLightValue[index] += lightLevelF;
		maxLightValue[index] = Math.max(maxLightValue[index], lightLevelF);
	}
	
	private LightingData combine(LightingData l00, LightingData l01, LightingData l10, LightingData l11) {
		LightingData res = new LightingData();
		res.combine(l00, l01, l10, l11);
		return res;
	}
	
	private LightingData interpolate(LightingData l000, LightingData l001, LightingData l010, LightingData l011, 
									LightingData l100, LightingData l101, LightingData l110, LightingData l111, 
									Direction dir, float x, float y, float z, boolean forceSide) {
		float i = dir.rightX != 0 ? x : (dir.rightY != 0 ? y : z);
		float j = dir.upX != 0 ? x : (dir.upY != 0 ? y : z);
		float k = dir.x != 0 ? x : (dir.y != 0 ? y : z);
		if(dir.rightX < 0 || dir.rightY < 0 || dir.rightZ < 0)
			i = 16f - i;
		if(dir.upX < 0 || dir.upY < 0 || dir.upZ < 0)
			j = 16f - j;
		if(dir.x < 0 || dir.y < 0 || dir.z < 0)
			k = 16f - k;
		i = Math.min(Math.max(i, 0f), 16f) / 16f;
		j = Math.min(Math.max(j, 0f), 16f) / 16f;
		k = Math.min(Math.max(k, 0f), 16f) / 16f;
		
		LightingData l00 = l000.lerp(l001, i);
		LightingData l01 = l010.lerp(l011, i);
		LightingData l10 = l100.lerp(l101, i);
		LightingData l11 = l110.lerp(l111, i);
		
		LightingData l0 = l00.lerp(l01, j);
		LightingData l1 = l10.lerp(l11, j);
		
		if(forceSide)
			return l1;
		
		return l0.lerp(l1, k);
	}

}
