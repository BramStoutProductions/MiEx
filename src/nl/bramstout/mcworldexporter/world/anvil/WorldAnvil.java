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

package nl.bramstout.mcworldexporter.world.anvil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class WorldAnvil extends World{

	public WorldAnvil(File worldDir) {
		super(worldDir);
	}

	@Override
	protected void _unload() {		
	}
	
	@Override
	protected void loadWorldSettings() {
		
	}

	@Override
	protected void findDimensions() {
		if(new File(worldDir, "region").exists()) {
			dimensions.add("Overworld");
		}
		for(File f : worldDir.listFiles()) {
			if(new File(f, "region").exists()) {
				dimensions.add(f.getName());
			}
		}
	}

	@Override
	protected void findRegions() {
		File regionFolder = new File(new File(worldDir, currentDimension), "region");
		if(currentDimension == "Overworld")
			regionFolder = new File(worldDir, "region");
		
		if(!regionFolder.exists())
			return;
		
		regionMinX = Integer.MAX_VALUE;
		regionMinZ = Integer.MAX_VALUE;
		regionMaxX = Integer.MIN_VALUE;
		regionMaxZ = Integer.MIN_VALUE;
		
		File[] files = regionFolder.listFiles();
		
		List<Region> tmpRegions = new ArrayList<Region>(files.length);
		
		for(File f : files) {
			if(!f.getName().endsWith(".mca"))
				continue;
			String[] tokens = f.getName().split("\\.");
			if(tokens.length != 4)
				continue;
			int x = Integer.parseInt(tokens[1]);
			int z = Integer.parseInt(tokens[2]);
			
			regionMinX = Math.min(regionMinX, x);
			regionMinZ = Math.min(regionMinZ, z);
			regionMaxX = Math.max(regionMaxX, x);
			regionMaxZ = Math.max(regionMaxZ, z);
			
			tmpRegions.add(new RegionAnvil(f, x, z));
		}
		
		regionsStride = regionMaxX - regionMinX + 1;
		regions = new Region[(regionMaxZ - regionMinZ + 1) * regionsStride];
		
		for(Region r : tmpRegions) {
			int id = (r.getZCoordinate() - regionMinZ) * regionsStride + 
						(r.getXCoordinate() - regionMinX);
			regions[id] = r;
		}
	}

}
