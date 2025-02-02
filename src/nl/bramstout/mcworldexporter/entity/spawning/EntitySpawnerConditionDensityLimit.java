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

package nl.bramstout.mcworldexporter.entity.spawning;

import java.util.List;
import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class EntitySpawnerConditionDensityLimit extends EntitySpawnerCondition{

	/**
	 * The maximum number of entities of this type spawnable on the surface.
	 * A value of -1 means no limit.
	 */
	public int surface;
	/**
	 * The maximum number of entities of this type spawnable underground.
	 * A value of -1 means no limit.
	 */
	public int underground;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		int surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(x, z);
		boolean isOnSurface = surfaceHeight <= y;
		
		int spawnLimit = isOnSurface ? surface : underground;
		
		if(spawnLimit < 0)
			return true; // No limit
		
		int entityCounter = 0;
		
		// Now we need to count how many entities of this type are around here.
		// Normally it's around the player, but we don't have a player,
		// so we do within x radius around this entity in terms of chunks.
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		int chunkRadius = 3;
		for(int cz = chunkZ - chunkRadius; cz <= chunkZ + chunkRadius; ++cz) {
			for(int cx = chunkX - chunkRadius; cx <= chunkX + chunkRadius; ++cx) {
				try {
					Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(cx, cz);
					if(chunk == null)
						continue;
					List<Entity> entities = chunk.getEntities();
					if(entities == null)
						continue;
					
					for(Entity entity : entities) {
						if(entity.getId().equals(spawner.getEntityType())) {
							entityCounter++;
							if(entityCounter >= spawnLimit)
								return false;
						}
					}
				}catch(Exception ex) {
					World.handleError(ex);
				}
			}
		}
		
		return true;
	}

}
