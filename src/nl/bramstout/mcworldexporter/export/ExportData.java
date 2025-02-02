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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ExportData {
	
	public String world;
	public String dimension;
	public int chunkSize;
	public int exportMinX;
	public int exportMinY;
	public int exportMinZ;
	public int exportMaxX;
	public int exportMaxY;
	public int exportMaxZ;
	public boolean hasLOD;
	public int lodCenterX;
	public int lodCenterZ;
	public int lodWidth;
	public int lodDepth;
	public int lodYDetail;
	public List<String> fgChunks;
	public List<Pair<Integer, Integer>> disabledChunks;
	public List<ResourcePack> resourcePacks;
	public boolean runOptimiser;
	public boolean removeCaves;
	public boolean fillInCaves;
	public boolean onlyIndividualBlocks;
	
	public int entityStartFrame;
	public int entityEndFrame;
	public int entityFPS;
	public int entityRandomSeed;
	public int entitySpawnDensity;
	public int entitySunLightLevel;
	public List<String> entitySpawnRules;
	public List<String> entityExport;
	public List<String> entitySimulate;
	
	public ExportData() {
		world = "";
		dimension = "";
		fgChunks = new ArrayList<String>();
		disabledChunks = new ArrayList<Pair<Integer, Integer>>();
		resourcePacks = new ArrayList<ResourcePack>();
		entitySpawnRules = new ArrayList<String>();
		entityExport = new ArrayList<String>();
		entitySimulate = new ArrayList<String>();
	}
	
	public static ExportData fromApp() {
		ExportData data = new ExportData();
		
		data.world = MCWorldExporter.getApp().getWorld().getWorldDir().getPath();
		data.dimension = MCWorldExporter.getApp().getWorld().getCurrentDimensions();
		data.chunkSize = Config.chunkSize;
		data.exportMinX = MCWorldExporter.getApp().getExportBounds().getMinX();
		data.exportMinY = MCWorldExporter.getApp().getExportBounds().getMinY();
		data.exportMinZ = MCWorldExporter.getApp().getExportBounds().getMinZ();
		data.exportMaxX = MCWorldExporter.getApp().getExportBounds().getMaxX();
		data.exportMaxY = MCWorldExporter.getApp().getExportBounds().getMaxY();
		data.exportMaxZ = MCWorldExporter.getApp().getExportBounds().getMaxZ();
		data.hasLOD = MCWorldExporter.getApp().getExportBounds().hasLod();
		data.lodCenterX = MCWorldExporter.getApp().getExportBounds().getLodCenterX();
		data.lodCenterZ = MCWorldExporter.getApp().getExportBounds().getLodCenterZ();
		data.lodWidth = MCWorldExporter.getApp().getExportBounds().getLodWidth();
		data.lodDepth = MCWorldExporter.getApp().getExportBounds().getLodDepth();
		data.lodYDetail = MCWorldExporter.getApp().getExportBounds().getLodYDetail();
		data.fgChunks = new ArrayList<String>(MCWorldExporter.getApp().getFGChunks());
		data.disabledChunks.clear();
		for(Pair<Integer, Integer> chunk : MCWorldExporter.getApp().getExportBounds().getDisabledChunks())
			data.disabledChunks.add(new Pair<Integer, Integer>(chunk.getKey(), chunk.getValue()));
		data.resourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
		data.runOptimiser = Config.runOptimiser;
		data.removeCaves = Config.removeCaves;
		data.fillInCaves = Config.fillInCaves;
		data.onlyIndividualBlocks = Config.onlyIndividualBlocks;
		
		data.entityStartFrame = MCWorldExporter.getApp().getUI().getEntityDialog().getStartFrame();
		data.entityEndFrame = MCWorldExporter.getApp().getUI().getEntityDialog().getEndFrame();
		data.entityFPS = MCWorldExporter.getApp().getUI().getEntityDialog().getFPS();
		data.entityRandomSeed = MCWorldExporter.getApp().getUI().getEntityDialog().getRandomSeed();
		data.entitySpawnDensity = MCWorldExporter.getApp().getUI().getEntityDialog().getSpawnDensityInput();
		data.entitySunLightLevel = MCWorldExporter.getApp().getUI().getEntityDialog().getSunLightLevel();
		data.entitySpawnRules = MCWorldExporter.getApp().getUI().getEntityDialog().getSpawnRules().getSelection();
		data.entityExport = MCWorldExporter.getApp().getUI().getEntityDialog().getExportEntities().getSelection();
		data.entitySimulate = MCWorldExporter.getApp().getUI().getEntityDialog().getSimulateEntities().getSelection();
		
		return data;
	}
	
	public static ExportData fromStream(LargeDataInputStream dis) throws IOException{
		ExportData data = new ExportData();
		
		int dataVersion = dis.readInt();
		if(dataVersion <= 0)
			return data; // Invalid data version
		
		data.world = dis.readUTF();
		data.dimension = dis.readUTF();
		data.chunkSize = dis.readInt();
		data.exportMinX = dis.readInt();
		data.exportMinY = dis.readInt();
		data.exportMinZ = dis.readInt();
		data.exportMaxX = dis.readInt();
		data.exportMaxY = dis.readInt();
		data.exportMaxZ = dis.readInt();
		data.hasLOD = dis.readBoolean();
		data.lodCenterX = dis.readInt();
		data.lodCenterZ = dis.readInt();
		data.lodWidth = dis.readInt();
		data.lodDepth = dis.readInt();
		data.lodYDetail = dis.readInt();
		int numFGChunks = dis.readInt();
		data.fgChunks = new ArrayList<String>();
		for(int i = 0; i < numFGChunks; ++i)
			data.fgChunks.add(dis.readUTF());
		int numDisabledChunks = dis.readInt();
		data.disabledChunks = new ArrayList<Pair<Integer, Integer>>();
		for(int i = 0; i < numDisabledChunks; ++i) {
			int chunkX = dis.readInt();
			int chunkZ = dis.readInt();
			data.disabledChunks.add(new Pair<Integer, Integer>(chunkX, chunkZ));
		}
		int numResourcePacks = dis.readInt();
		data.resourcePacks = new ArrayList<ResourcePack>();
		for(int i = 0; i < numResourcePacks; ++i) {
			String uuid = dis.readUTF();
			ResourcePack pack = ResourcePacks.getResourcePack(uuid);
			if(pack != null)
				data.resourcePacks.add(pack);
		}
		data.runOptimiser = dis.readBoolean();
		data.removeCaves = dis.readBoolean();
		data.fillInCaves = dis.readBoolean();
		data.onlyIndividualBlocks = dis.readBoolean();
		
		data.entityStartFrame = dis.readInt();
		data.entityEndFrame = dis.readInt();
		data.entityFPS = dis.readInt();
		data.entityRandomSeed = dis.readInt();
		data.entitySpawnDensity = dis.readInt();
		data.entitySunLightLevel = dis.readInt();
		int numEntitySpawnRules = dis.readInt();
		for(int i = 0; i < numEntitySpawnRules; ++i)
			data.entitySpawnRules.add(dis.readUTF());
		int numEntityExport = dis.readInt();
		for(int i = 0; i < numEntityExport; ++i)
			data.entityExport.add(dis.readUTF());
		int numEntitySimulate = dis.readInt();
		for(int i = 0; i < numEntitySimulate; ++i)
			data.entitySimulate.add(dis.readUTF());
		
		return data;
	}
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeInt(1); // Data version, in case additional data gets added later on.
		dos.writeUTF(world);
		dos.writeUTF(dimension);
		dos.writeInt(chunkSize);
		dos.writeInt(exportMinX);
		dos.writeInt(exportMinY);
		dos.writeInt(exportMinZ);
		dos.writeInt(exportMaxX);
		dos.writeInt(exportMaxY);
		dos.writeInt(exportMaxZ);
		dos.writeBoolean(hasLOD);
		dos.writeInt(lodCenterX);
		dos.writeInt(lodCenterZ);
		dos.writeInt(lodWidth);
		dos.writeInt(lodDepth);
		dos.writeInt(lodYDetail);
		dos.writeInt(fgChunks.size());
		for(String val : fgChunks)
			dos.writeUTF(val);
		dos.writeInt(disabledChunks.size());
		for(Pair<Integer, Integer> chunk : disabledChunks) {
			dos.writeInt(chunk.getKey().intValue());
			dos.writeInt(chunk.getValue().intValue());
		}
		dos.writeInt(resourcePacks.size());
		for(ResourcePack val : resourcePacks)
			dos.writeUTF(val.getUUID());
		dos.writeBoolean(runOptimiser);
		dos.writeBoolean(removeCaves);
		dos.writeBoolean(fillInCaves);
		dos.writeBoolean(onlyIndividualBlocks);
		
		dos.writeInt(entityStartFrame);
		dos.writeInt(entityEndFrame);
		dos.writeInt(entityFPS);
		dos.writeInt(entityRandomSeed);
		dos.writeInt(entitySpawnDensity);
		dos.writeInt(entitySunLightLevel);
		dos.writeInt(entitySpawnRules.size());
		for(String str : entitySpawnRules)
			dos.writeUTF(str);
		dos.writeInt(entityExport.size());
		for(String str : entityExport)
			dos.writeUTF(str);
		dos.writeInt(entitySimulate.size());
		for(String str : entitySimulate)
			dos.writeUTF(str);
	}
	
	public void apply() {
		if(!(new File(world)).exists())
			return;
		//ResourcePack.setActiveResourcePacks(resourcePacks);
		MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
		List<String> resourcePackUUIDS = new ArrayList<String>();
		for(ResourcePack pack : resourcePacks)
			resourcePackUUIDS.add(pack.getUUID());
		MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(resourcePackUUIDS);
		
		BackgroundThread.waitUntilDoneWithBackgroundTasks();
		
		MCWorldExporter.getApp().setWorld(new File(world));
		MCWorldExporter.getApp().getWorld().loadDimension(dimension);
		
		Config.chunkSize = chunkSize;
		Config.runOptimiser = runOptimiser;
		Config.removeCaves = removeCaves;
		Config.fillInCaves = fillInCaves;
		Config.onlyIndividualBlocks = onlyIndividualBlocks;
		MCWorldExporter.getApp().getUI().update();
		
		MCWorldExporter.getApp().getExportBounds().set(exportMinX, exportMinY, exportMinZ, exportMaxX, exportMaxY, exportMaxZ);
		MCWorldExporter.getApp().getExportBounds().disableLod();
		if(hasLOD) {
			MCWorldExporter.getApp().getExportBounds().enableLod();
			MCWorldExporter.getApp().getExportBounds().setLodCenterX(lodCenterX);
			MCWorldExporter.getApp().getExportBounds().setLodCenterZ(lodCenterZ);
			MCWorldExporter.getApp().getExportBounds().setLodWidth(lodWidth);
			MCWorldExporter.getApp().getExportBounds().setLodDepth(lodDepth);
			MCWorldExporter.getApp().getExportBounds().setLodYDetail(lodYDetail);
		}
		MCWorldExporter.getApp().setFGChunks(fgChunks);
		MCWorldExporter.getApp().getExportBounds().setDisabledChunks(disabledChunks);
		
		MCWorldExporter.getApp().getUI().getViewer().teleport((exportMinX + exportMaxX)/2, (exportMinZ + exportMaxZ) / 2);
		
		BackgroundThread.waitUntilDoneWithBackgroundTasks();
		
		MCWorldExporter.getApp().getUI().getEntityDialog().setStartFrame(entityStartFrame);
		MCWorldExporter.getApp().getUI().getEntityDialog().setEndFrame(entityEndFrame);
		MCWorldExporter.getApp().getUI().getEntityDialog().setFPS(entityFPS);
		MCWorldExporter.getApp().getUI().getEntityDialog().setRandomSeed(entityRandomSeed);
		MCWorldExporter.getApp().getUI().getEntityDialog().setSpawnDensity(entitySpawnDensity);
		MCWorldExporter.getApp().getUI().getEntityDialog().setSunLightLevel(entitySunLightLevel);
		MCWorldExporter.getApp().getUI().getEntityDialog().noDefaultSelection = true;
		MCWorldExporter.getApp().getUI().getEntityDialog().load();
		MCWorldExporter.getApp().getUI().getEntityDialog().getSpawnRules().setSelection(entitySpawnRules);
		MCWorldExporter.getApp().getUI().getEntityDialog().getExportEntities().setSelection(entityExport);
		MCWorldExporter.getApp().getUI().getEntityDialog().getSimulateEntities().setSelection(entitySimulate);
	}
	
}
