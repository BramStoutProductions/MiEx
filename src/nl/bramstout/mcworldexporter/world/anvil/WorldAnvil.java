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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.IndexCache;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;

public class WorldAnvil extends World{

	public WorldAnvil(File worldDir) {
		super(worldDir);
		blockConnectionsTranslation = new BlockConnectionsTranslation("java");
		blockConnectionsTranslation.load();
		File levelDatFile = new File(worldDir, "level.dat");
		if(levelDatFile.exists()) {
			GZIPInputStream is = null;
			try {
				is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(levelDatFile)));
				DataInputStream dis = new DataInputStream(is);
				NbtTagCompound root = (NbtTagCompound) NbtTag.readFromStream(dis);
				NbtTagCompound tag = root;
				if(tag.get("Data") != null)
					tag = (NbtTagCompound) tag.get("Data");
				
				NbtTag dataVersionTag = tag.get("DataVersion");
				if(dataVersionTag != null)
					this.worldVersion = dataVersionTag.asInt();
				
				NbtTagCompound versionTag = (NbtTagCompound) tag.get("Version");
				if(versionTag != null) {
					NbtTag idTag = versionTag.get("Id");
					if(idTag != null)
						this.worldVersion = idTag.asInt();
				}
				root.free();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(is != null) {
				try {
					is.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected void _pause() {
		if(regions != null) {
			for(Region region : regions) {
				if(region == null)
					continue;
				try {
					region.pause();
				}catch(Exception ex) {
					handleError(ex);
				}
			}
		}
		regions = null;
		regionsIndices = null;
	}
	
	@Override
	protected void _unpause() {
		loadWorldSettings();
		findDimensions();
		findRegions();
	}
	
	public static boolean supportsWorld(File worldDir) {
		if(!new File(worldDir, "level.dat").exists())
			return false;
		List<String> dimensions = new ArrayList<String>();
		findDimensions(dimensions, worldDir);
		return dimensions.size() > 0;
	}

	@Override
	protected void _unload() {		
	}
	
	@Override
	protected void loadWorldSettings() {
		players.clear();
		File playerDataFolder = new File(worldDir, "playerdata");
		if(playerDataFolder.exists()) {
			for(File f : playerDataFolder.listFiles()) {
				if(!f.isFile())
					continue;
				if(!f.getName().endsWith(".dat"))
					continue;
				players.add(new PlayerAnvil(f.getName().replace(".dat", ""), f));
			}
		}
	}

	@Override
	protected void findDimensions() {
		findDimensions(dimensions, worldDir);
	}
	
	private static void findDimensions(List<String> dimensions, File worldDir) {
		dimensions.clear();
		if(new File(worldDir, "region").exists()) {
			dimensions.add("overworld");
		}
		for(File f : worldDir.listFiles()) {
			findDimensionsInFolder(dimensions, f, "");
		}
	}
	
	private static void findDimensionsInFolder(List<String> dimensions, File folder, String parent) {
		if(new File(folder, "region").exists()) {
			String dim = folder.getName();
			if(dim.equals("DIM-1"))
				dim = "the_nether";
			else if(dim.equals("DIM1"))
				dim = "the_end";
			dimensions.add(parent + dim);
		}else if(folder.isDirectory()){
			for(File f : folder.listFiles()) {
				findDimensionsInFolder(dimensions, f, parent + folder.getName() + "/");
			}
		}
	}

	@Override
	protected void findRegions() {
		File sessionLock = new File(worldDir, "session.lock");
		if(sessionLock.exists()) {
			try {
				// If the world is open in Minecraft, this will fail.
				FileOutputStream out = new FileOutputStream(sessionLock);
				out.write(0xE2);
				out.write(0x98);
				out.write(0x83);
				out.close();
			}catch(Exception ex) {
				// We couldn't acquire a session, so error out.
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "This world is open in another program.", "Locked world", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		File regionFolder = new File(new File(worldDir, currentDimension), "region");
		if(currentDimension.equals("overworld"))
			regionFolder = new File(worldDir, "region");
		if(currentDimension.equals("the_nether"))
			regionFolder = new File(worldDir, "DIM-1/region");
		if(currentDimension.equals("the_end"))
			regionFolder = new File(worldDir, "DIM1/region");
		
		if(!regionFolder.exists())
			return;
		
		regionMinX = Integer.MAX_VALUE;
		regionMinZ = Integer.MAX_VALUE;
		regionMaxX = Integer.MIN_VALUE;
		regionMaxZ = Integer.MIN_VALUE;
		
		File[] files = regionFolder.listFiles();
		if(files.length <= 0) {
			regions = null;
			regionsIndices = null;
			return;
		}
		
		List<Region> tmpRegions = new ArrayList<Region>(files.length);
		
		for(File f : files) {
			try {
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
				
				tmpRegions.add(new RegionAnvil(this, f, x, z));
			}catch(Exception ex) {}
		}
		
		if(tmpRegions.size() == 0)
			return;
		if((regionMaxX - regionMinX) > 500 || (regionMaxZ - regionMinZ) > 500) {
			regions = new Region[tmpRegions.size()];
			regionsIndices = new IndexCache();
			int i = 0;
			for(Region r : tmpRegions) {
				long lid = ((long) r.getXCoordinate()) << 32 | (((long) r.getZCoordinate()) & 0xFFFFFFFFL);
				regionsIndices.put(lid, i);
				regions[i] = r;
				i++;
			}
		}else{
			regionsStride = regionMaxX - regionMinX + 1;
			regions = new Region[(regionMaxZ - regionMinZ + 1) * regionsStride];
			
			for(Region r : tmpRegions) {
				int id = (r.getZCoordinate() - regionMinZ) * regionsStride + 
							(r.getXCoordinate() - regionMinX);
				regions[id] = r;
			}
		}
	}

}
