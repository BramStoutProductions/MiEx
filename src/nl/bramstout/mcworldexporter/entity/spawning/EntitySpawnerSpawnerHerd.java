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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerSpawnerHerd extends EntitySpawnerSpawner{

	/**
	 * The minimum amount of entities to spawn in this herd.
	 */
	public int minSize;
	/**
	 * The maximum amount of entities to spawn in this herd.
	 */
	public int maxSize;
	/**
	 * The event to be triggered on entities when spawning.
	 */
	public String event;
	/**
	 * The number of entities to skip before the event is triggered.
	 */
	public int eventSkipCount;
	
	@Override
	public List<SpawnEntity> spawn(EntitySpawner spawner, int x, int y, int z, Random random) {
		List<SpawnEntity> entities = new ArrayList<SpawnEntity>();
		
		int spawnRadius = maxSize;
		int spawnAmount = random.nextInt(minSize, maxSize + 1);
		int[] pos = new int[] {0, 0, 0};
		for(int i = 0; i < spawnAmount; ++i) {
			// Find a spot within spawnRadius.
			findSpot(x, y, z, spawnRadius, pos, random);
			SpawnEntity entity = new SpawnEntity(spawner.getEntityType(), pos[0], pos[1], pos[2]);
			if(i >= eventSkipCount)
				entity.events.add(event);
			entities.add(entity);
		}
		
		return entities;
	}
	
	private void findSpot(int x, int y, int z, int radius, int[] pos, Random random) {
		for(int attempt = 0; attempt < radius * radius; ++attempt) {
			int spawnX = random.nextInt(x - radius, x + radius + 1);
			int spawnZ = random.nextInt(z - radius, z + radius + 1);
			
			// Find a spot with an air block and non-air block below.
			int spawnY = y;
			int prevBlockId = MCWorldExporter.getApp().getWorld().getBlockId(spawnX, spawnY, spawnZ, 0);
			if(prevBlockId == 0) {
				// This is air, so move down to find a spot.
				for(spawnY = y - 1; spawnY >= y - radius; --spawnY) {
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(spawnX, spawnY, spawnZ, 0);
					if(prevBlockId == 0 && blockId != 0) {
						// We've found a spot, so return it.
						pos[0] = spawnX;
						pos[1] = spawnY + 1;
						pos[2] = spawnZ;
						return;
					}
					prevBlockId = blockId;
				}
			}else {
				// This is not air, so move up to find a spot.
				for(spawnY = y + 1; spawnY <= y + radius; ++spawnY) {
					int blockId = MCWorldExporter.getApp().getWorld().getBlockId(spawnX, spawnY, spawnZ, 0);
					if(prevBlockId != 0 && blockId == 0) {
						// We've found a spot, so return it.
						pos[0] = spawnX;
						pos[1] = spawnY;
						pos[2] = spawnZ;
						return;
					}
					prevBlockId = blockId;
				}
			}
		}
		// We couldn't find a spot, so return the center
		pos[0] = x;
		pos[1] = y;
		pos[2] = z;
	}
	
}
