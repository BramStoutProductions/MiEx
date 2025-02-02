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

import java.util.Random;

import nl.bramstout.mcworldexporter.MCWorldExporter;

public class EntitySpawnerConditionBrightnessFilter extends EntitySpawnerCondition{

	/**
	 * The minimum light level value that allows the entity to spawn.
	 */
	public int minLightLevel;
	/**
	 * The maximum light level value that allows the entity to spawn.
	 */
	public int maxLightLevel;
	
	@Override
	public boolean test(EntitySpawner spawner, int x, int y, int z, int sunLightLevel, Random random) {
		// Let's calculate sun light.
		int lightValue = calculateSunLight(x, y, z);
		lightValue = (int) ((((float) lightValue) * ((float) sunLightLevel) / 15f));
		return lightValue >= minLightLevel && lightValue <= maxLightLevel;
	}
	
	private int calculateSunLight(int x, int y, int z) {
		int surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(x, z);
		if(surfaceHeight <= y) {
			// This sample is at the surface.
			return 15;
		}
		int radius = 16;
		int maxLightValue = 0;
		for(int sampleZ = z - radius; sampleZ <= z + radius; ++z) {
			for(int sampleX = x - radius; x <= x + radius; ++x) {
				int distance = Math.abs(z - sampleZ) + Math.abs(x - sampleX);
				int falloff = 15 - distance;
				if(falloff < 0)
					continue;
				surfaceHeight = MCWorldExporter.getApp().getWorld().getHeight(sampleX, sampleZ);
				if(surfaceHeight <= y) {
					// This sample is at the surface.
					maxLightValue = Math.max(maxLightValue, falloff);
					if(maxLightValue >= 15)
						return maxLightValue;
				}
			}
		}
		return maxLightValue;
	}
	
}
