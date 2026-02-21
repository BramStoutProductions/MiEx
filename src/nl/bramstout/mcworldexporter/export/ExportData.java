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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.ExportBounds.ExcludeRegion;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ExportData {
	
	public String world;
	public String dimension;
	public int chunkSize;
	public List<ExportBounds> exportRegions;
	public List<ResourcePack> resourcePacks;
	public boolean runOptimiser;
	public boolean removeCaves;
	public boolean fillInCaves;
	public boolean exportBlockAnimations;
	
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
		exportRegions = new ArrayList<ExportBounds>();
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
		data.exportRegions = new ArrayList<ExportBounds>();
		for(ExportBounds bounds : MCWorldExporter.getApp().getExportBoundsList())
			data.exportRegions.add(bounds.copy());
		data.resourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
		data.runOptimiser = Config.runOptimiser;
		data.removeCaves = Config.removeCaves;
		data.fillInCaves = Config.fillInCaves;
		data.exportBlockAnimations = Config.exportBlockAnimations;
		
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
		data.exportRegions = new ArrayList<ExportBounds>();
		if(dataVersion >= 2) {
			int numExportRegions = dis.readInt();
			for(int i = 0; i < numExportRegions; ++i) {
				ExportBounds bounds = new ExportBounds("");
				bounds.read(dis);
				data.exportRegions.add(bounds);
			}
		}else {
			int exportMinX = dis.readInt();
			int exportMinY = dis.readInt();
			int exportMinZ = dis.readInt();
			int exportMaxX = dis.readInt();
			int exportMaxY = dis.readInt();
			int exportMaxZ = dis.readInt();
			int exportOffsetY = dis.readInt();
			boolean hasLOD = dis.readBoolean();
			int lodCenterX = dis.readInt();
			int lodCenterZ = dis.readInt();
			int lodWidth = dis.readInt();
			int lodDepth = dis.readInt();
			int lodYDetail = dis.readInt();
			int numFGChunks = dis.readInt();
			ArrayList<String> fgChunks = new ArrayList<String>();
			for(int i = 0; i < numFGChunks; ++i)
				fgChunks.add(dis.readUTF());
			int numDisabledChunks = dis.readInt();
			ArrayList<Pair<Integer, Integer>> disabledChunks = new ArrayList<Pair<Integer, Integer>>();
			for(int i = 0; i < numDisabledChunks; ++i) {
				int chunkX = dis.readInt();
				int chunkZ = dis.readInt();
				disabledChunks.add(new Pair<Integer, Integer>(chunkX, chunkZ));
			}
			
			data.exportRegions.add(new ExportBounds("Region 1", 
					exportMinX, exportMinY, exportMinZ, 
					exportMaxX, exportMaxY, exportMaxZ, 
					(exportMinX + exportMaxX)/2, exportOffsetY, (exportMinZ + exportMaxZ)/2, 
					lodCenterX, lodCenterZ, 
					lodWidth, lodDepth, lodYDetail, 0, hasLOD, 
					disabledChunks, fgChunks, data.chunkSize, new ArrayList<ExcludeRegion>(), false, true, false));
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
		data.exportBlockAnimations = dis.readBoolean();
		
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
		dos.writeInt(2); // Data version, in case additional data gets added later on.
		dos.writeUTF(world);
		dos.writeUTF(dimension);
		dos.writeInt(chunkSize);
		dos.writeInt(exportRegions.size());
		for(int i = 0; i < exportRegions.size(); ++i) {
			exportRegions.get(i).write(dos);
		}
		dos.writeInt(resourcePacks.size());
		for(ResourcePack val : resourcePacks)
			dos.writeUTF(val.getUUID());
		dos.writeBoolean(runOptimiser);
		dos.writeBoolean(removeCaves);
		dos.writeBoolean(fillInCaves);
		dos.writeBoolean(exportBlockAnimations);
		
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
	
	public void apply(boolean onlySettings) {
		if(!onlySettings && !(new File(world)).exists()) {
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Could not find world save specified in export.", "Error", JOptionPane.ERROR_MESSAGE);
				}
				
			});
			return;
		}
		if(!onlySettings) {
			MCWorldExporter.getApp().setWorld(null, null, null);
		}
		MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
		List<String> resourcePackUUIDS = new ArrayList<String>();
		for(ResourcePack pack : resourcePacks)
			resourcePackUUIDS.add(pack.getUUID());
		MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(resourcePackUUIDS);
		
		BackgroundThread.waitUntilDoneWithBackgroundTasks();
		
		if(!onlySettings) {
			MCWorldExporter.getApp().setWorld(new File(world), new File(world).getName(), null);
			MCWorldExporter.getApp().getWorld().loadDimension(dimension);
		}
		
		Config.chunkSize = chunkSize;
		Config.runOptimiser = runOptimiser;
		Config.removeCaves = removeCaves;
		Config.fillInCaves = fillInCaves;
		Config.exportBlockAnimations = exportBlockAnimations;
		MCWorldExporter.getApp().getUI().update();
		MCWorldExporter.getApp().setExportBounds(exportRegions);
		
		MCWorldExporter.getApp().getUI().getViewer().teleport(
				MCWorldExporter.getApp().getActiveExportBounds().getCenterX(), 
				MCWorldExporter.getApp().getActiveExportBounds().getCenterZ());
		
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
	
	public void print() {
		System.out.println("Exporting out using settings:");
		for(Field field : this.getClass().getFields()) {
			if(List.class.isAssignableFrom(field.getType())) {
				try {
					System.out.println("  " + field.getName() + ":");
					List<?> data = (List<?>) field.get(this);
					for(Object obj : data) {
						System.out.println("    " + obj.toString());
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}else{
				try {
					System.out.println("  " + field.getName() + ": " + field.get(this).toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println();
	}
	
}
