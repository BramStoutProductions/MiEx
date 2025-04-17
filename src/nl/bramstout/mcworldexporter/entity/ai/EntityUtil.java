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

package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class EntityUtil {
	
	public static boolean randomChance(Entity entity, float probability, float deltaTime) {
		// Adjusts the probability to account for differing deltaTimes
		return entity.getRandom().nextFloat() <= Math.pow(probability, 0.05f / deltaTime);
	}
	
	public static boolean randomChance(Entity entity, int count, float deltaTime) {
		return randomChance(entity, 1f / ((float) count), deltaTime);
	}
	
	public static boolean isInLiquid(Entity entity, float posX, float posY, float posZ) {
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
		BakedBlockState blockState = BlockStateRegistry.getBakedStateForBlock(blockId, blockX, blockY, blockZ);
		return blockState.hasLiquid();
	}
	
	public static boolean standingOnSolidBlock(Entity entity, float posX, float posY, float posZ) {
		float minX = posX;
		float minY = posY - 0.01f;
		float minZ = posZ;
		float maxX = posX;
		float maxY = posY;
		float maxZ = posZ;
		if(entity.getAI() != null) {
			float scale = 1f;
			NbtTag tag = entity.getProperties().get("Scale");
			if(tag != null)
				scale = tag.asFloat();
			
			minX = posX - (entity.getAI().collisionBoxWidth * scale) / 2f;
			minZ = posZ - (entity.getAI().collisionBoxWidth * scale) / 2f;
			maxX = posX + (entity.getAI().collisionBoxWidth * scale) / 2f;
			maxY = posY + (entity.getAI().collisionBoxHeight * scale);
			maxZ = posZ + (entity.getAI().collisionBoxWidth * scale) / 2f;
		}
		int minBlockX = (int) Math.floor(minX);
		int minBlockY = (int) Math.floor(minY);
		int minBlockZ = (int) Math.floor(minZ);
		int maxBlockX = (int) Math.floor(maxX);
		int maxBlockY = (int) Math.floor(maxY);
		int maxBlockZ = (int) Math.floor(maxZ);
		
		List<Model> models = new ArrayList<Model>();
		for(int y = minBlockY; y <= maxBlockY; ++y) {
			for(int z = minBlockZ; z <= maxBlockZ; ++z) {
				for(int x = minBlockX; x <= maxBlockX; ++x) {
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
					if(blockId != 0) {
						// Get bounding box local to the block.
						float ebbMinX = minX - ((float) x);
						float ebbMinY = minY - ((float) y);
						float ebbMinZ = minZ - ((float) z);
						float ebbMaxX = maxX - ((float) x);
						float ebbMaxY = maxY - ((float) y);
						float ebbMaxZ = maxZ - ((float) z);
						float eCenterY = (ebbMinY + ebbMaxY) / 2f;
						
						Block block = BlockRegistry.getBlock(blockId);
						BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z);
						if(state.isAir() || block.isLiquid())
							continue;
						models.clear();
						// TODO: check if a custom collision box is specified.
						state.getModels(x, y, z, models);
						
						// Loop over all faces
						for(int i = 0; i < models.size(); ++i) {
							Model model = models.get(i);
							for(int j = 0; j < model.getFaces().size(); ++j) {
								ModelFace face = model.getFaces().get(j);
								// Get the face's bounding box
								float fbbMinX = Math.min(face.getPoints()[0], face.getPoints()[6]) / 16f;
								float fbbMinY = Math.min(face.getPoints()[1], face.getPoints()[7]) / 16f;
								float fbbMinZ = Math.min(face.getPoints()[2], face.getPoints()[8]) / 16f;
								float fbbMaxX = Math.max(face.getPoints()[0], face.getPoints()[6]) / 16f;
								float fbbMaxY = Math.max(face.getPoints()[1], face.getPoints()[7]) / 16f;
								float fbbMaxZ = Math.max(face.getPoints()[2], face.getPoints()[8]) / 16f;
								
								// Only faces aligned with primary axis can be a collider.
								// In other words, the size of the face's bounding box must
								// be zero on one axis
								boolean flatY = (fbbMaxY - fbbMinY) < 0.0000001f;
								if(!flatY)
									continue;
								
								// Now check if the two bounding boxes intersect
								if(fbbMaxX <= ebbMinX || fbbMinX >= ebbMaxX || 
										fbbMaxY <= ebbMinY || fbbMinY >= ebbMaxY ||
										fbbMaxZ <= ebbMinZ || fbbMinZ >= ebbMaxZ)
									continue; // No collide
								
								if(eCenterY >= fbbMinY) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public static class CollisionResult{
		
		/**
		 * The amount to move the entity so that it doesn't
		 * collide with the collider anymore.
		 * 
		 * A value of -1 means no collision.
		 */
		public float t = -1f;
		/**
		 * The x component of the normal of the collision.
		 * A.K.A. the direction to move in.
		 */
		public float nx = 0f;
		/**
		 * The y component of the normal of the collision.
		 * A.K.A. the direction to move in.
		 */
		public float ny = 0f;
		/**
		 * The z component of the normal of the collision.
		 * A.K.A. the direction to move in.
		 */
		public float nz = 0f;
		
	}
	
	public static void getClosestCollision(Entity entity, float posX, float posY, float posZ, CollisionResult out) {
		float minX = posX;
		float minY = posY;
		float minZ = posZ;
		float maxX = posX;
		float maxY = posY;
		float maxZ = posZ;
		if(entity.getAI() != null) {
			float scale = 1f;
			NbtTag tag = entity.getProperties().get("Scale");
			if(tag != null)
				scale = tag.asFloat();
			
			minX = posX - (entity.getAI().collisionBoxWidth * scale) / 2f;
			minZ = posZ - (entity.getAI().collisionBoxWidth * scale) / 2f;
			maxX = posX + (entity.getAI().collisionBoxWidth * scale) / 2f;
			maxY = posY + (entity.getAI().collisionBoxHeight * scale);
			maxZ = posZ + (entity.getAI().collisionBoxWidth * scale) / 2f;
		}
		int minBlockX = (int) Math.floor(minX);
		int minBlockY = (int) Math.floor(minY);
		int minBlockZ = (int) Math.floor(minZ);
		int maxBlockX = (int) Math.floor(maxX);
		int maxBlockY = (int) Math.floor(maxY);
		int maxBlockZ = (int) Math.floor(maxZ);
		
		float closestT = Float.MAX_VALUE;
		float closestNX = 0f;
		float closestNY = 0f;
		float closestNZ = 0f;
		
		List<Model> models = new ArrayList<Model>();
		for(int y = minBlockY; y <= maxBlockY; ++y) {
			for(int z = minBlockZ; z <= maxBlockZ; ++z) {
				for(int x = minBlockX; x <= maxBlockX; ++x) {
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
					if(blockId != 0) {
						// Get bounding box local to the block.
						float ebbMinX = minX - ((float) x);
						float ebbMinY = minY - ((float) y);
						float ebbMinZ = minZ - ((float) z);
						float ebbMaxX = maxX - ((float) x);
						float ebbMaxY = maxY - ((float) y);
						float ebbMaxZ = maxZ - ((float) z);
						float eCenterX = (ebbMinX + ebbMaxX) / 2f;
						float eCenterY = (ebbMinY + ebbMaxY) / 2f;
						float eCenterZ = (ebbMinZ + ebbMaxZ) / 2f;
						
						Block block = BlockRegistry.getBlock(blockId);
						BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z);
						if(state.isAir() || block.isLiquid())
							continue;
						models.clear();
						// TODO: check if a custom collision box is specified.
						state.getModels(x, y, z, models);
						
						// Loop over all faces
						for(int i = 0; i < models.size(); ++i) {
							Model model = models.get(i);
							for(int j = 0; j < model.getFaces().size(); ++j) {
								ModelFace face = model.getFaces().get(j);
								// Get the face's bounding box
								float fbbMinX = Math.min(face.getPoints()[0], face.getPoints()[6]) / 16f;
								float fbbMinY = Math.min(face.getPoints()[1], face.getPoints()[7]) / 16f;
								float fbbMinZ = Math.min(face.getPoints()[2], face.getPoints()[8]) / 16f;
								float fbbMaxX = Math.max(face.getPoints()[0], face.getPoints()[6]) / 16f;
								float fbbMaxY = Math.max(face.getPoints()[1], face.getPoints()[7]) / 16f;
								float fbbMaxZ = Math.max(face.getPoints()[2], face.getPoints()[8]) / 16f;
								
								// Only faces aligned with primary axis can be a collider.
								// In other words, the size of the face's bounding box must
								// be zero on one axis
								boolean flatX = (fbbMaxX - fbbMinX) < 0.0000001f;
								boolean flatY = (fbbMaxY - fbbMinY) < 0.0000001f;
								boolean flatZ = (fbbMaxZ - fbbMinZ) < 0.0000001f;
								if(!(flatX || flatY || flatZ))
									continue;
								
								// Now check if the two bounding boxes intersect
								if(fbbMaxX <= ebbMinX || fbbMinX >= ebbMaxX || 
										fbbMaxY <= ebbMinY || fbbMinY >= ebbMaxY ||
										fbbMaxZ <= ebbMinZ || fbbMinZ >= ebbMaxZ)
									continue; // No collide
								
								// There's a collision, so now figure out the direction
								// to move into
								float nx = 0f;
								float ny = 0f;
								float nz = 0f;
								if(flatX) {
									nx = eCenterX >= fbbMinX ? 1f : -1f;
									ny = 0f;
									nz = 0f;
								}else if(flatY) {
									nx = 0f;
									ny = eCenterY >= fbbMinY ? 1f : -1f;
									nz = 0f;
								}else { // flatZ must be true then
									nx = 0f;
									ny = 0f;
									nz = eCenterZ >= fbbMinZ ? 1f : -1f;
								}
								// Now figure out the amount to move
								float t = 0f;
								if(nx > 0f) {
									t = Math.abs(fbbMaxX - ebbMinX);
								}else if(nx < 0f) {
									t = Math.abs(fbbMinX - ebbMaxX);
								}else if(ny > 0f) {
									t = Math.abs(fbbMaxY - ebbMinY);
								}else if(ny < 0f) {
									t = Math.abs(fbbMinY - ebbMaxY);
								}else if(nz > 0f) {
									t = Math.abs(fbbMaxZ - ebbMinZ);
								}else if(nz < 0f) {
									t = Math.abs(fbbMinZ - ebbMaxZ);
								}
								
								// Check if movement is necessary
								// and it's the closest collision.
								if(t > 0f && t < closestT) {
									closestT = t;
									closestNX = nx;
									closestNY = ny;
									closestNZ = nz;
								}
							}
						}
					}
				}
			}
		}
		
		if(closestT == Float.MAX_VALUE || closestT <= 0f) {
			out.t = -1f;
		}else {
			out.t = closestT;
			out.nx = closestNX;
			out.ny = closestNY;
			out.nz = closestNZ;
		}
	}
	
	public static boolean isCollidingWithWorld(Entity entity, float posX, float posY, float posZ, CollisionResult res) {
		getClosestCollision(entity, posX, posY, posZ, res);
		return res.t > 0f;
	}
	
	public static boolean isInSunlight(Entity entity, float posX, float posY, float posZ) {
		int blockX = (int) Math.floor(posX);
		int blockZ = (int) Math.floor(posZ);

		int height = MCWorldExporter.getApp().getWorld().getHeight(blockX, blockZ);
		return posY >= height;
	}
	
	public static boolean isUnderCover(Entity entity, float posX, float posY, float posZ) {
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);

		for(int sampleY = blockY + 2; sampleY < blockY + 8; sampleY++) {
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, sampleY, blockZ);
			if(blockId != 0)
				return true;
		}
		return false;
	}
	
	public static EntityTargetBlock FindTarget(int searchRadiusXZ, int searchRadiusY,
												Entity entity, float posX, float posY, float posZ) {
		CollisionResult res = new CollisionResult();
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockX;
			int sampleZ = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockZ;
			
			// We've found a target. Now just find a place on the Y axis
			for(int sampleY = 0; sampleY >= -searchRadiusY; sampleY--) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res))
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
			}
			for(int sampleY = 1; sampleY <= searchRadiusY; sampleY++) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res))
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
			}
		}
		// Couldn't find a spot
		return null;
	}
	
	public static EntityTargetBlock FindTarget(int searchRadiusXZ, int searchRadiusY,
												Entity entity, float posX, float posY, float posZ, 
												float avoidX, float avoidY, float avoidZ) {
		CollisionResult res = new CollisionResult();
		float dx = avoidX - posX;
		float dz = avoidZ - posZ;
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockX;
			int sampleZ = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockZ;
			float sdx = ((float) sampleX) - posX;
			float sdz = ((float) sampleZ) - posZ;
			float dot = dx * sdx + dz * sdz;
			if(dot >= 0f)
				continue; // The sample is roughly in the same direction as our avoid target, so skip this sample
			
			// We've found a target. Now just find a place on the Y axis
			for(int sampleY = 0; sampleY >= -searchRadiusY; sampleY--) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res))
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
			}
			for(int sampleY = 1; sampleY <= searchRadiusY; sampleY++) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res))
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
			}
		}
		// Couldn't find a spot
		return null;
	}
	
	public static EntityTargetBlock FindTargetUnderCover(int searchRadiusXZ, int searchRadiusY,
												Entity entity, float posX, float posY, float posZ) {
		CollisionResult res = new CollisionResult();
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockX;
			int sampleZ = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockZ;
			
			// We've found a target. Now just find a place on the Y axis
			for(int sampleY = 0; sampleY >= -searchRadiusY; sampleY--) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res)) {
					if(!isUnderCover(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f))
						break;
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
				}
			}
			for(int sampleY = 1; sampleY <= searchRadiusY; sampleY++) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res)) {
					if(!isUnderCover(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f))
						break;
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
				}
			}
		}
			// Couldn't find a spot
		return null;
	}
	
	public static EntityTargetBlock FindTargetNotInSunlight(int searchRadiusXZ, int searchRadiusY,
					Entity entity, float posX, float posY, float posZ) {
		CollisionResult res = new CollisionResult();
		int blockX = (int) Math.floor(posX);
		int blockY = (int) Math.floor(posY);
		int blockZ = (int) Math.floor(posZ);
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockX;
			int sampleZ = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + blockZ;
			
			// We've found a target. Now just find a place on the Y axis
			for(int sampleY = 0; sampleY >= -searchRadiusY; sampleY--) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res)) {
					if(isInSunlight(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f))
						break;
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
				}
			}
			for(int sampleY = 1; sampleY <= searchRadiusY; sampleY++) {
				if(standingOnSolidBlock(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f) && 
						!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f, res)) {
					if(isInSunlight(entity, ((float) sampleX) + 0.5f, sampleY + blockY, ((float) sampleZ) + 0.5f))
						break;
					return new EntityTargetBlock(sampleX, sampleY + blockY, sampleZ);
				}
			}
		}
		// Couldn't find a spot
		return null;
	}
	
	public static EntityTargetBlock FindTargetInAir(int searchRadiusXZ, int searchRadiusY,
												Entity entity, float posX, float posY, float posZ) {
		CollisionResult res = new CollisionResult();
		for(int attempt = 0; attempt < 100; ++attempt) {
			int sampleX = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + (int) Math.floor(posX);
			int sampleY = entity.getRandom().nextInt(-searchRadiusY, searchRadiusY+1) + (int) Math.floor(posY);
			int sampleZ = entity.getRandom().nextInt(-searchRadiusXZ, searchRadiusXZ+1) + (int) Math.floor(posZ);
			if(!isCollidingWithWorld(entity, ((float) sampleX) + 0.5f, sampleY, ((float) sampleZ) + 0.5f, res))
				return new EntityTargetBlock(sampleX, sampleY, sampleZ);
		}
		// Couldn't find a spot
		return null;
	}
	
}
