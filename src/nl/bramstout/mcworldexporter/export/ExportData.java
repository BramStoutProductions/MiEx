package nl.bramstout.mcworldexporter.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

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
	public List<String> resourcePacks;
	public boolean runOptimiser;
	public boolean removeCaves;
	public boolean fillInCaves;
	public boolean onlyIndividualBlocks;
	
	public ExportData() {
		world = "";
		dimension = "";
		fgChunks = new ArrayList<String>();
		resourcePacks = new ArrayList<String>();
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
		data.resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		data.runOptimiser = Config.runOptimiser;
		data.removeCaves = Config.removeCaves;
		data.fillInCaves = Config.fillInCaves;
		data.onlyIndividualBlocks = Config.onlyIndividualBlocks;
		
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
		int numResourcePacks = dis.readInt();
		data.resourcePacks = new ArrayList<String>();
		for(int i = 0; i < numResourcePacks; ++i)
			data.resourcePacks.add(dis.readUTF());
		data.runOptimiser = dis.readBoolean();
		data.removeCaves = dis.readBoolean();
		data.fillInCaves = dis.readBoolean();
		data.onlyIndividualBlocks = dis.readBoolean();
		
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
		dos.writeInt(resourcePacks.size());
		for(String val : resourcePacks)
			dos.writeUTF(val);
		dos.writeBoolean(runOptimiser);
		dos.writeBoolean(removeCaves);
		dos.writeBoolean(fillInCaves);
		dos.writeBoolean(onlyIndividualBlocks);
	}
	
	public void apply() {
		if(!(new File(world)).exists())
			return;
		MCWorldExporter.getApp().setWorld(new File(world));
		MCWorldExporter.getApp().getWorld().loadDimension(dimension);
		//ResourcePack.setActiveResourcePacks(resourcePacks);
		MCWorldExporter.getApp().getUI().getResourcePackManager().reset();
		for(String defaultPack : ResourcePack.getActiveResourcePacks())
			MCWorldExporter.getApp().getUI().getResourcePackManager().disableResourcePack(defaultPack);;
		for(int i = resourcePacks.size() - 1; i >= 0; --i)
			MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(resourcePacks.get(i));
		
		Config.chunkSize = chunkSize;
		Config.runOptimiser = runOptimiser;
		Config.removeCaves = removeCaves;
		Config.fillInCaves = fillInCaves;
		Config.onlyIndividualBlocks = onlyIndividualBlocks;
		MCWorldExporter.getApp().getUI().update();
		
		MCWorldExporter.getApp().getExportBounds().set(exportMinX, exportMinY, exportMinZ, exportMaxX, exportMaxY, exportMaxZ);
		MCWorldExporter.getApp().getExportBounds().disableLod();
		if(hasLOD) {
			MCWorldExporter.getApp().getExportBounds().setLodCenterX(lodCenterX);
			MCWorldExporter.getApp().getExportBounds().setLodCenterZ(lodCenterZ);
			MCWorldExporter.getApp().getExportBounds().setLodWidth(lodWidth);
			MCWorldExporter.getApp().getExportBounds().setLodDepth(lodDepth);
			MCWorldExporter.getApp().getExportBounds().setLodYDetail(lodYDetail);
		}
		MCWorldExporter.getApp().setFGChunks(fgChunks);
		
		MCWorldExporter.getApp().getUI().getViewer().teleport((exportMinX + exportMaxX)/2, (exportMinZ + exportMaxZ) / 2);
	}
	
}
