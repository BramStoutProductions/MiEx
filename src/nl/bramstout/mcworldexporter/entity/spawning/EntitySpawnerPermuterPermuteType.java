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
import java.util.Random;

import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;

public class EntitySpawnerPermuterPermuteType extends EntitySpawnerPermuter{

	public static class Permutation{
		
		/**
		 * The weight of this permutation.
		 */
		public int weight;
		/**
		 * The type of the entity to spawn.
		 * If this value is null, then it spawns
		 * the normal entity type.
		 */
		public String entityType;
		
		public Permutation() {
			weight = 1;
			entityType = null;
		}
		
		public Permutation(int weight, String entityType) {
			this.weight = weight;
			this.entityType = entityType;
		}
		
	}
	
	public List<Permutation> permutations;
	
	public EntitySpawnerPermuterPermuteType() {
		this.permutations = new ArrayList<Permutation>();
	}
	
	@Override
	public void permute(EntitySpawner spawner, List<SpawnEntity> entities, Random random) {
		int totalWeight = 0;
		for(Permutation permutation : permutations)
			totalWeight += permutation.weight;
		for(SpawnEntity entity : entities) {
			int sample = random.nextInt(totalWeight);
			
			for(Permutation permutation : permutations) {
				sample -= permutation.weight;
				if(sample < 0) {
					if(permutation.entityType != null)
						entity.name = permutation.entityType;
					break;
				}
			}
		}
	}
	
}
