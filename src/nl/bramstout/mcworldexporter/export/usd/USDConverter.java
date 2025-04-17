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

package nl.bramstout.mcworldexporter.export.usd;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel3D;
import nl.bramstout.mcworldexporter.export.Converter;
import nl.bramstout.mcworldexporter.export.ExportData;
import nl.bramstout.mcworldexporter.export.Exporter;
import nl.bramstout.mcworldexporter.export.IndividualBlockId;
import nl.bramstout.mcworldexporter.export.LargeDataInputStream;
import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.export.json.JsonMaterialWriter;
import nl.bramstout.mcworldexporter.export.materialx.MaterialXMaterialWriter;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;
import nl.bramstout.mcworldexporter.resourcepack.BannerTextureCreator;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class USDConverter extends Converter{

	private static class MatKey{
		
		private String matTexture;
		private boolean hasBiomeColor;
		
		public MatKey(String matTexture, boolean hasBiomeColor) {
			this.matTexture = matTexture;
			this.hasBiomeColor = hasBiomeColor;
		}
		
		@Override
		public int hashCode() {
			return matTexture.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof MatKey))
				return false;
			return ((MatKey) obj).matTexture.equals(matTexture) && ((MatKey) obj).hasBiomeColor == hasBiomeColor;
		}
		
	}
	
	private static class ChunkInfo{
		String name = "";
		boolean isFG = false;
		Map<IndividualBlockId, List<Float>> instancers = new HashMap<IndividualBlockId, List<Float>>();
		Set<Texture> usedTextures = new HashSet<Texture>();
		Map<MatKey, Materials.MaterialTemplate> templates = new HashMap<MatKey, Materials.MaterialTemplate>();
	}
	
	private static class IndividualBlockInfo{
		String path;
		
		public IndividualBlockInfo(String path) {
			this.path = path;
		}
	}
	private static class Texture{
		String texture;
		Materials.MaterialTemplate materialTemplate;
		boolean hasBiomeColor;
		
		public Texture(String texture, String matTexture, boolean hasBiomeColor, Map<MatKey, Materials.MaterialTemplate> templates) {
			this.texture = texture;
			MatKey matKey = new MatKey(matTexture, hasBiomeColor);
			this.materialTemplate = templates.getOrDefault(matKey, null);
			if(this.materialTemplate == null) {
				this.materialTemplate = Materials.getMaterial(matTexture, hasBiomeColor, currentOutputDir.getAbsolutePath());
				templates.put(matKey, materialTemplate);
			}
			this.hasBiomeColor = hasBiomeColor;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Texture) {
				if(((Texture) obj).materialTemplate == null) {
					return ((Texture)obj).texture.equals(texture) && 
							((Texture)obj).materialTemplate == materialTemplate && 
							((Texture)obj).hasBiomeColor == hasBiomeColor;
				}
				return ((Texture)obj).texture.equals(texture) && 
						((Texture)obj).materialTemplate.equals(materialTemplate) && 
						((Texture)obj).hasBiomeColor == hasBiomeColor;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(texture, materialTemplate);
		}
	}
	
	private LargeDataInputStream dis;
	private File inputFile;
	private File outputFile;
	private File chunksFolder;
	private File materialsFile;
	private Map<IndividualBlockId, IndividualBlockInfo> individualBlocksRegistry;
	private Set<Texture> usedTextures = new HashSet<Texture>();
	private Set<Texture> entityUsedTextures = new HashSet<Texture>();
	private List<ChunkInfo> chunkInfosFG = new ArrayList<ChunkInfo>();
	private List<ChunkInfo> chunkInfosBG = new ArrayList<ChunkInfo>();
	private Object mutex = new Object();
	private static ThreadPool threadPool = new ThreadPool("USD_Converter", 2048);
	public static File currentOutputDir = null;
	
	public USDConverter(File inputFile, File outputFile) throws IOException {
		if(inputFile == null || outputFile == null)
			return;
		
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		chunksFolder = Exporter.chunksFolder;
		if(chunksFolder.exists())
			deleteDir(chunksFolder);
		chunksFolder.mkdirs();
		materialsFile = new File(outputFile.getPath().replace(".usd", "_materials.usd"));
		individualBlocksRegistry = new HashMap<IndividualBlockId, IndividualBlockInfo>();
		currentOutputDir = outputFile.getParentFile();
	}
	
	@Override
	public void init() throws Exception{
		dis = new LargeDataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
	}
	
	@Override
	public boolean deleteMiExFiles() {
		return true;
	}
	
	private void deleteDir(File dir) {
		for(String f : dir.list()) {
			File file = new File(dir, f);
			if(file.isDirectory())
				deleteDir(file);
			else
				file.delete();
		}
		dir.delete();
	}
	
	@Override
	public void convert() throws Exception{
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Converting to USD");
		int version = dis.readInt();
		if(version != 2)
			throw new IOException("Unsupport input file version");
		
		long invididualBlocksOffset = dis.readLong();
		ExportData exportData = ExportData.fromStream(dis);
		
		
		USDWriter rootWriter = new USDWriter(outputFile);
		rootWriter.beginMetaData();
		rootWriter.writeMetaDataString("defaultPrim", "world");
		rootWriter.writeMetaDataFloat("metersPerUnit", 1.0f);
		rootWriter.writeMetaDataString("upAxis", "Y");
		
		// Export settings
		rootWriter.writeMetaData("customLayerData");
		rootWriter.beginDict();
		rootWriter.writeAttributeName("dictionary", "MiEx", false);
		rootWriter.beginDict();
		
		writeExportData(rootWriter, exportData);
		
		rootWriter.endDict();
		rootWriter.endDict();
		
		rootWriter.endMetaData();
		rootWriter.beginDef("Xform", "world");
		rootWriter.beginMetaData();
		rootWriter.writeVariantSets("MiEx_LOD");
		rootWriter.writeMetaDataString("kind", "assembly");
		rootWriter.endMetaData();
		rootWriter.beginChildren();
		
		// Entities
		String entitiesFilename = dis.readUTF();
		File entitiesFile = new File(inputFile.getParentFile(), entitiesFilename);
		convertEntities(new LargeDataInputStream(new BufferedInputStream(new FileInputStream(entitiesFile))),
						new File(chunksFolder, "entities.usd"));
		rootWriter.beginDef("Xform", "entities");
		rootWriter.beginMetaData();
		rootWriter.writeMetaDataString("kind", "group");
		rootWriter.writePayload("./" + chunksFolder.getName() + "/entities.usd", false);
		rootWriter.endMetaData();
		rootWriter.beginChildren();
		rootWriter.beginOver("materials");
		rootWriter.beginChildren();
		for(Texture tex : entityUsedTextures) {
			rootWriter.writeAttributeName("rel", 
					MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor), false);
			rootWriter.writeAttributeValue("</world/materials/" + 
					MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor) + ">");
		}
		rootWriter.endChildren();
		rootWriter.endOver();
		rootWriter.endChildren();
		rootWriter.endDef();
		
		// Chunks
		int numChunks = dis.readInt();
		String[] chunkFilenames = new String[numChunks];
		for(int i = 0; i < numChunks; ++i)
			chunkFilenames[i] = dis.readUTF();
		
		Map<MatKey, Materials.MaterialTemplate> templates = new HashMap<MatKey, Materials.MaterialTemplate>();
		dis.skipBytes(invididualBlocksOffset - dis.getPosition());
		int numBaseMeshes = dis.readInt();
		if(numBaseMeshes > 0) {
			rootWriter.beginDef("Scope", "individualBlocksBaseMeshes");
			rootWriter.beginChildren();
			
			for(int i = 0; i < numBaseMeshes; ++i) {
				int baseMeshId = dis.readInt();
				int baseMeshX = dis.readInt();
				int baseMeshY = dis.readInt();
				int baseMeshZ = dis.readInt();
				String blockName = dis.readUTF();
				String primName = ("_class_" + Util.makeSafeName(blockName) + baseMeshId + "_" + 
												baseMeshX + "_" + baseMeshY + "_" + baseMeshZ).replace('-', 'N');
				
				rootWriter.beginClass("Xform", primName);
				rootWriter.beginMetaData();
				rootWriter.writeMetaDataString("kind", "subcomponent");
				rootWriter.endMetaData();
				rootWriter.beginChildren();
				
				Set<Texture> usedTextures = new HashSet<Texture>();
				
				int numMeshes = dis.readInt();
				for(int meshId = 0; meshId < numMeshes; ++meshId)
					readMesh(dis, rootWriter, rootWriter, null, true, "/world/materials/", usedTextures, templates);
				
				rootWriter.endChildren();
				rootWriter.endClass();
				
				
				this.usedTextures.addAll(usedTextures);
				individualBlocksRegistry.put(new IndividualBlockId(baseMeshId, baseMeshX, baseMeshY, baseMeshZ), 
												new IndividualBlockInfo(primName));
			}
			rootWriter.endChildren();
			rootWriter.endDef();
		}
		
		// We're done reading from this input stream.
		// Each ConvertChunkTask will create their own input stream.
		dis.close();
		
		MCWorldExporter.getApp().getUI().getProgressBar().setNumChunks(numChunks);;

		List<Task> futures = new ArrayList<Task>();
		for(int chunkId = 0; chunkId < numChunks; ++chunkId) {
			futures.add(threadPool.submit(new ConvertChunkTask(new File(inputFile.getParentFile(), chunkFilenames[chunkId]), this)));
		}
		for(Task future : futures) {
			try {
				future.waitUntilTaskIsDone();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		ChunkInfo[] chunkInfosFG = new ChunkInfo[this.chunkInfosFG.size()];
		this.chunkInfosFG.toArray(chunkInfosFG);
		ChunkInfo[] chunkInfosBG = new ChunkInfo[this.chunkInfosBG.size()];
		this.chunkInfosBG.toArray(chunkInfosBG);
		Comparator<ChunkInfo> chunkInfoComparator = new Comparator<ChunkInfo>() {

			@Override
			public int compare(ChunkInfo o1, ChunkInfo o2) {
				String s1 = o1.name;
				String s2 = o2.name;
				for(int i = 0; i < Math.min(s1.length(), s2.length()); ++i) {
					int diff = s1.codePointAt(i) - s2.codePointAt(i);
					if(diff == 0)
						continue;
					return diff;
				}
				return 0;
			}
			
		};
		Arrays.sort(chunkInfosFG, chunkInfoComparator);
		Arrays.sort(chunkInfosBG, chunkInfoComparator);
		
		Materials.reload();
		
		MaterialWriter[] materialWriters = new MaterialWriter[] { 
				new USDMaterialWriter(materialsFile), 
				new MaterialXMaterialWriter(new File(materialsFile.getPath().replace(".usd", ".mtlx"))),
				new JsonMaterialWriter(new File(materialsFile.getPath().replace(".usd", ".json")))};
		
		for(MaterialWriter materialWriter : materialWriters)
			materialWriter.open();
		
		for(MaterialWriter materialWriter : materialWriters)
			materialWriter.writeSharedNodes("/materials");
		
		for(Texture texture : usedTextures) {
			Materials.MaterialTemplate material = texture.materialTemplate;
			if(material != null)
				for(MaterialWriter materialWriter : materialWriters)
					materialWriter.writeMaterial(material, texture.texture, texture.hasBiomeColor, 
													"/materials", "/materials/sharedNodes");
		}
		
		for(MaterialWriter materialWriter : materialWriters) {
			try {
				materialWriter.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		List<String> materialReferences = new ArrayList<String>();
		for(MaterialWriter materialWriter : materialWriters) {
			if(materialWriter.hasWrittenAnything()) {
				String refPath = materialWriter.getUSDAssetPath();
				if(refPath != null)
					materialReferences.add(refPath);
			}
		}
		
		
		rootWriter.beginDef("Scope", "materials");
		rootWriter.beginMetaData();
		rootWriter.writeReferences(materialReferences);
		rootWriter.endMetaData();
		rootWriter.endDef();
		
		if(chunkInfosFG.length > 0) {
			rootWriter.beginDef("Xform", "foreground");
			rootWriter.beginChildren();
			writeChunks(rootWriter, chunkInfosFG);
			rootWriter.endChildren();
			rootWriter.endDef();
		}
		if(chunkInfosBG.length > 0) {
			rootWriter.beginDef("Xform", "background");
			rootWriter.beginChildren();
			writeChunks(rootWriter, chunkInfosBG);
			rootWriter.endChildren();
			rootWriter.endDef();
		}
		
		rootWriter.beginVariantSet("MiEx_LOD");
		rootWriter.beginVariant("proxy");
		rootWriter.endVariant();
		rootWriter.beginVariant("render");
		if(chunkInfosFG.length > 0) {
			rootWriter.beginOver("foreground");
			rootWriter.beginChildren();
			writeChunkRenderVariants(rootWriter, chunkInfosFG);
			rootWriter.endChildren();
			rootWriter.endOver();
		}
		if(chunkInfosBG.length > 0) {
			rootWriter.beginOver("background");
			rootWriter.beginChildren();
			writeChunkRenderVariants(rootWriter, chunkInfosBG);
			rootWriter.endChildren();
			rootWriter.endOver();
		}
		rootWriter.endVariant();
		rootWriter.endVariantSet();
		
		rootWriter.endChildren();
		rootWriter.endDef();
		rootWriter.close(false);
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.95f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Cleaning up");
		
		USDWriter.finalCleanup();
	}
	
	private void writeExportData(USDWriter rootWriter, ExportData data) throws IOException{
		rootWriter.writeAttributeName("string", "world", false);
		rootWriter.writeAttributeValueString(data.world.replace('\\', '/'));
		
		rootWriter.writeAttributeName("string", "dimension", false);
		rootWriter.writeAttributeValueString(data.dimension);
		
		rootWriter.writeAttributeName("int", "chunkSize", false);
		rootWriter.writeAttributeValueInt(data.chunkSize);
		
		rootWriter.writeAttributeName("int", "exportMinX", false);
		rootWriter.writeAttributeValueInt(data.exportMinX);
		rootWriter.writeAttributeName("int", "exportMinY", false);
		rootWriter.writeAttributeValueInt(data.exportMinY);
		rootWriter.writeAttributeName("int", "exportMinZ", false);
		rootWriter.writeAttributeValueInt(data.exportMinZ);
		
		rootWriter.writeAttributeName("int", "exportMaxX", false);
		rootWriter.writeAttributeValueInt(data.exportMaxX);
		rootWriter.writeAttributeName("int", "exportMaxY", false);
		rootWriter.writeAttributeValueInt(data.exportMaxY);
		rootWriter.writeAttributeName("int", "exportMaxZ", false);
		rootWriter.writeAttributeValueInt(data.exportMaxZ);
		
		rootWriter.writeAttributeName("int", "hasLOD", false);
		rootWriter.writeAttributeValueInt(data.hasLOD ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "lodCenterX", false);
		rootWriter.writeAttributeValueInt(data.lodCenterX);
		rootWriter.writeAttributeName("int", "lodCenterZ", false);
		rootWriter.writeAttributeValueInt(data.lodCenterZ);
		rootWriter.writeAttributeName("int", "lodWidth", false);
		rootWriter.writeAttributeValueInt(data.lodWidth);
		rootWriter.writeAttributeName("int", "lodDepth", false);
		rootWriter.writeAttributeValueInt(data.lodDepth);
		rootWriter.writeAttributeName("int", "lodYDetail", false);
		rootWriter.writeAttributeValueInt(data.lodYDetail);
		
		rootWriter.writeAttributeName("string[]", "fgChunks", false);
		rootWriter.writeAttributeValueStringArray(data.fgChunks);
		
		rootWriter.writeAttributeName("int[]", "disabledChunks", false);
		int[] disabledChunksData = new int[data.disabledChunks.size()*2];
		int i = 0;
		for(Pair<Integer, Integer> chunk : data.disabledChunks) {
			disabledChunksData[i] = chunk.getKey().intValue();
			disabledChunksData[i+1] = chunk.getValue().intValue();
			i += 2;
		}
		rootWriter.writeAttributeValueIntArray(disabledChunksData);
		
		rootWriter.writeAttributeName("string[]", "resourcePacks", false);
		List<String> resourcePackNames = new ArrayList<String>();
		for(ResourcePack pack : data.resourcePacks)
			resourcePackNames.add(pack.getUUID());
		rootWriter.writeAttributeValueStringArray(resourcePackNames);
		
		rootWriter.writeAttributeName("int", "runOptimiser", false);
		rootWriter.writeAttributeValueInt(data.runOptimiser ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "removeCaves", false);
		rootWriter.writeAttributeValueInt(data.removeCaves ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "fillInCaves", false);
		rootWriter.writeAttributeValueInt(data.fillInCaves ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "onlyIndividualBlocks", false);
		rootWriter.writeAttributeValueInt(data.onlyIndividualBlocks ? 1 : 0);
		
		
		rootWriter.writeAttributeName("int", "entityStartFrame", false);
		rootWriter.writeAttributeValueInt(data.entityStartFrame);
		
		rootWriter.writeAttributeName("int", "entityEndFrame", false);
		rootWriter.writeAttributeValueInt(data.entityEndFrame);
		
		rootWriter.writeAttributeName("int", "entityFPS", false);
		rootWriter.writeAttributeValueInt(data.entityFPS);
		
		rootWriter.writeAttributeName("int", "entityRandomSeed", false);
		rootWriter.writeAttributeValueInt(data.entityRandomSeed);
		
		rootWriter.writeAttributeName("int", "entitySpawnDensity", false);
		rootWriter.writeAttributeValueInt(data.entitySpawnDensity);
		
		rootWriter.writeAttributeName("int", "entitySunLightLevel", false);
		rootWriter.writeAttributeValueInt(data.entitySunLightLevel);
		
		rootWriter.writeAttributeName("string[]", "entitySpawnRules", false);
		rootWriter.writeAttributeValueStringArray(data.entitySpawnRules);
		
		rootWriter.writeAttributeName("string[]", "entityExport", false);
		rootWriter.writeAttributeValueStringArray(data.entityExport);
		
		rootWriter.writeAttributeName("string[]", "entitySimulate", false);
		rootWriter.writeAttributeValueStringArray(data.entitySimulate);
	}
	
	private void writeChunks(USDWriter rootWriter, ChunkInfo[] chunkInfos) throws IOException{
		for(ChunkInfo chunkInfo : chunkInfos) {
			rootWriter.beginDef("Xform", chunkInfo.name);
			rootWriter.beginMetaData();
			rootWriter.writeMetaDataString("kind", "group");
			rootWriter.writePayload("./" + chunksFolder.getName() + "/" + chunkInfo.name + ".usd", false);
			rootWriter.endMetaData();
			rootWriter.beginChildren();
			rootWriter.writeAttributeName("bool", "isFG", true);
			rootWriter.writeAttributeValueBoolean(chunkInfo.isFG);
			
			rootWriter.beginOver("materials");
			rootWriter.beginChildren();
			for(Texture tex : chunkInfo.usedTextures) {
				rootWriter.writeAttributeName("rel", 
						MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor), false);
				rootWriter.writeAttributeValue("</world/materials/" + 
						MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor) + ">");
			}
			rootWriter.endChildren();
			rootWriter.endOver();
			
			for(Entry<IndividualBlockId, List<Float>> instancer : chunkInfo.instancers.entrySet()) {
				IndividualBlockInfo baseInfo = individualBlocksRegistry.get(instancer.getKey());
				
				for(int i = 0; i < instancer.getValue().size()/3; ++i) {
					rootWriter.beginDef("Xform", baseInfo.path.replace("_class_", "") + "_" + i);
					rootWriter.beginMetaData();
					rootWriter.writeInherit("/world/individualBlocksBaseMeshes/" + baseInfo.path);
					rootWriter.writeMetaDataBoolean("instanceable", true);
					rootWriter.endMetaData();
					rootWriter.beginChildren();
					
					rootWriter.writeAttributeName("double3", "xformOp:translate", false);
					rootWriter.writeAttributeValuePoint3f(instancer.getValue().get(i*3),
														instancer.getValue().get(i*3+1),
														instancer.getValue().get(i*3+2));
					rootWriter.writeAttributeName("token[]", "xformOpOrder", true);
					rootWriter.writeAttributeValueStringArray(new String[] { "xformOp:translate" });
					
					rootWriter.endChildren();
					rootWriter.endDef();
				}
			}
			
			rootWriter.endChildren();
			rootWriter.endDef();
		}
	}
	
	private void writeChunkRenderVariants(USDWriter rootWriter, ChunkInfo[] chunkInfos) throws IOException{
		for(ChunkInfo chunkInfo : chunkInfos) {
			rootWriter.beginOver(chunkInfo.name);
			rootWriter.beginMetaData();
			rootWriter.writePayload("./" + chunksFolder.getName() + "/" + chunkInfo.name + "_render.usd", true);
			rootWriter.endMetaData();
			rootWriter.endOver();
		}
	}
	
	private static class ConvertChunkTask implements Runnable {
		
		private File inputFile;
		private USDConverter converter;
		
		public ConvertChunkTask(File file, USDConverter converter) throws IOException {
			this.inputFile = file;
			this.converter = converter;
		}
	
		@Override
		public void run() {
			LargeDataInputStream dis = null;
			try {
				dis = new LargeDataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
				ChunkInfo chunkInfo = new ChunkInfo();
				
				String chunkName = dis.readUTF();
				chunkInfo.name = chunkName;
				
				boolean isFG = dis.readByte() > 0;
				chunkInfo.isFG = isFG;
				//int numMeshes = dis.readInt();
				
				USDWriter chunkWriter = new USDWriter(new File(converter.chunksFolder, chunkName + ".usd"));
				USDWriter chunkRenderWriter = new USDWriter(new File(converter.chunksFolder, chunkName + "_render.usd"));
				chunkWriter.beginMetaData();
				chunkWriter.writeMetaDataString("defaultPrim", "chunk");
				chunkWriter.endMetaData();
				
				chunkWriter.beginDef("Xform", "chunk");
				chunkWriter.beginMetaData();
				chunkWriter.writeMetaDataString("kind", "group");
				chunkWriter.endMetaData();
				chunkWriter.beginChildren();
				
				chunkRenderWriter.beginMetaData();
				chunkRenderWriter.writeMetaDataString("defaultPrim", "chunk");
				chunkRenderWriter.endMetaData();
				
				chunkRenderWriter.beginDef("Xform", "chunk");
				chunkRenderWriter.beginMetaData();
				chunkRenderWriter.writeMetaDataString("kind", "group");
				chunkRenderWriter.endMetaData();
				chunkRenderWriter.beginChildren();
				
				
				//for(int meshId = 0; meshId < numMeshes; ++meshId) {
				while(true) {
					boolean hasNext = converter.readMesh(dis, chunkWriter, chunkRenderWriter, 
									null, false, "/chunk/materials.", chunkInfo.usedTextures, chunkInfo.templates);
					if(!hasNext)
						break;
				}
				
				int numIndividualBlocks = dis.readInt();
				for(int individualBlockId = 0; individualBlockId < numIndividualBlocks; ++individualBlockId) {
					converter.readIndividualBlock(dis, chunkInfo.instancers);
				}
				
				writeMaterialSlots(chunkWriter, chunkInfo);
				writeMaterialSlots(chunkRenderWriter, chunkInfo);
				
				chunkWriter.endChildren();
				chunkWriter.endDef();
				chunkWriter.close(false);
				
				chunkRenderWriter.endChildren();
				chunkRenderWriter.endDef();
				chunkRenderWriter.close(false);
				
				synchronized(converter.mutex) {
					if(isFG)
						converter.chunkInfosFG.add(chunkInfo);
					else
						converter.chunkInfosBG.add(chunkInfo);
					converter.usedTextures.addAll(chunkInfo.usedTextures);
				}
				MCWorldExporter.getApp().getUI().getProgressBar().finishedMesh(1);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			try {
				if(dis != null)
					dis.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		private void writeMaterialSlots(USDWriter chunkWriter, ChunkInfo chunkInfo) throws IOException {
			chunkWriter.beginDef("Scope", "materials");
			chunkWriter.beginChildren();
			for(Texture tex : chunkInfo.usedTextures) {
				chunkWriter.writeAttributeName("rel", 
						MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor), false);
			}
			chunkWriter.endChildren();
			chunkWriter.endDef();
		}
	}
	
	private boolean readMesh(LargeDataInputStream dis, USDWriter writer, USDWriter renderWriter, Mesh proxyMesh, boolean noProxy, 
							String materialsPrim, Set<Texture> usedTextures, Map<MatKey, Materials.MaterialTemplate> templates) 
									throws IOException{
		byte meshType = dis.readByte();
		if(meshType == 0) { // End of list
			return false;
		} else if(meshType == 1) { // Mesh
			
			String meshName = dis.readUTF();
			if(meshName.equals(""))
				meshName = "mesh";
			else
				meshName = Util.makeSafeName(meshName);
			boolean doubleSided = dis.readInt() > 0;
			String texture = dis.readUTF();
			String matTexture = dis.readUTF();
			String extraData = dis.readUTF();
			int numVertices = dis.readInt();
			int numUVs = dis.readInt();
			int numCornerUVs = dis.readInt();
			int numNormals = dis.readInt();
			int numAO = dis.readInt();
			int numFaces = dis.readInt();
			int numColors = dis.readInt();
			
			// Handle banners
			if(texture.startsWith("banner:")) {
				String bannerTexName = texture.replace(':', '/');
				try {
					texture = BannerTextureCreator.createBannerTexture(extraData, bannerTexName);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			Texture textureObj = new Texture(texture, matTexture, numColors > 0, templates);
			
			usedTextures.add(textureObj);
			
			float[] vertexData = new float[numVertices * 3];
			float[] uvData = new float[numUVs * 2];
			float[] cornerUVData = new float[numCornerUVs * 2];
			float[] normalData = new float[numNormals * 3];
			float[] colorData = numColors == 0 ? null : new float[numColors * 3];
			float[] aoData = new float[numAO];
			int[] faceIndexData = new int[numFaces * 4];
			int[] uvIndexData = new int[numFaces * 4];
			int[] cornerUVIndexData = new int[numCornerUVs == 0 ? 0 : (numFaces * 4)];
			int[] normalIndexData = new int[numFaces * 4];
			int[] aoIndexData = new int[numFaces * 4];
			int[] colorIndexData = numColors == 0 ? null : new int[numFaces * 4];
			
			for(int i = 0; i < numVertices * 3; ++i)
				vertexData[i] = dis.readFloat();
			
			for(int i = 0; i < numUVs; ++i)
				uvData[i*2] = dis.readFloat();
			for(int i = 0; i < numUVs; ++i)
				uvData[i*2+1] = dis.readFloat();
			
			for(int i = 0; i < numCornerUVs * 2; ++i)
				cornerUVData[i] = dis.readFloat();
			
			for(int i = 0; i < numNormals * 3; ++i)
				normalData[i] = dis.readFloat();
			
			for(int i = 0; i < numAO; ++i)
				aoData[i] = dis.readFloat();
			
			if(numColors > 0)
				for(int i = 0; i < numColors * 3; ++i)
					colorData[i] = dis.readFloat();
			
			for(int i = 0; i < numFaces * 4; ++i)
				faceIndexData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				uvIndexData[i] = dis.readInt();
			
			if(numCornerUVs > 0)
				for(int i = 0; i < numFaces * 4; ++i)
					cornerUVIndexData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				normalIndexData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				aoIndexData[i] = dis.readInt();
			
			if(numColors > 0)
				for(int i = 0; i < numFaces * 4; ++i)
					colorIndexData[i] = dis.readInt();
			
			int[] vertexIndicesCounts = new int[numFaces];
			Arrays.fill(vertexIndicesCounts, 4);
			
			writeMesh(writer, meshName, materialsPrim, textureObj, extraData, null, doubleSided, vertexData, faceIndexData, 
						vertexIndicesCounts, uvData, uvIndexData, cornerUVData, cornerUVIndexData, normalData, normalIndexData, 
						aoData, aoIndexData, numColors, colorData, colorIndexData, proxyMesh == null ? "component" : "subcomponent");
			
			
			if(proxyMesh != null) {
				proxyMesh.setTexture(texture, false);
				proxyMesh.setMatTexture(matTexture);
				proxyMesh.setExtraData(extraData);
				proxyMesh.setDoubleSided(doubleSided);
				Mesh subMesh = new Mesh(meshName, texture, matTexture, false, doubleSided, extraData, vertexData, uvData, cornerUVData, 
						colorData, normalData, aoData, faceIndexData, vertexIndicesCounts, uvIndexData, cornerUVIndexData, colorIndexData, 
						normalIndexData, aoIndexData);
				proxyMesh.appendMesh(subMesh);
			}
			
			return true;
		}else if(meshType == 2) { // Group
			
			String groupName = dis.readUTF();
			if(groupName.equals(""))
				groupName = "mesh";
			else
				groupName = Util.makeSafeName(groupName);
			
			String extraData = dis.readUTF();
			
			//int numChildren = dis.readInt();
			
			
			renderWriter.beginDef("Xform", groupName);
			renderWriter.beginMetaData();
			renderWriter.writeMetaDataString("kind", "component");
			renderWriter.endMetaData();
			renderWriter.beginChildren();
			
			if(extraData != null && extraData.length() > 0) {
				try {
					JsonObject data = JsonParser.parseString(extraData).getAsJsonObject();
					
					List<String> xformOp = new ArrayList<String>();
					if(data.has("translate")) {
						JsonArray translateArray = data.getAsJsonArray("translate");
						writer.writeAttributeName("double3", "xformOp:translate", false);
						writer.writeAttributeValuePoint3f(translateArray.get(0).getAsFloat(), 
															translateArray.get(1).getAsFloat(),
															translateArray.get(2).getAsFloat());
						xformOp.add("xformOp:translate");
					}
					if(data.has("pivot")) {
						JsonArray translateArray = data.getAsJsonArray("pivot");
						writer.writeAttributeName("double3", "xformOp:translate:pivot", false);
						writer.writeAttributeValuePoint3f(translateArray.get(0).getAsFloat(), 
															translateArray.get(1).getAsFloat(),
															translateArray.get(2).getAsFloat());
						xformOp.add("xformOp:translate:pivot");
					}
					if(data.has("rotate")) {
						JsonArray rotateArray = data.getAsJsonArray("rotate");
						writer.writeAttributeName("double3", "xformOp:rotateXYZ", false);
						writer.writeAttributeValuePoint3f(rotateArray.get(0).getAsFloat(), 
															rotateArray.get(1).getAsFloat(),
															rotateArray.get(2).getAsFloat());
						xformOp.add("xformOp:rotateXYZ");
					}
					if(data.has("scale")) {
						JsonArray scaleArray = data.getAsJsonArray("scale");
						writer.writeAttributeName("double3", "xformOp:scale", false);
						writer.writeAttributeValuePoint3f(scaleArray.get(0).getAsFloat(), 
															scaleArray.get(1).getAsFloat(),
															scaleArray.get(2).getAsFloat());
						xformOp.add("xformOp:scale");
					}
					if(data.has("pivot")) {
						JsonArray translateArray = data.getAsJsonArray("pivot");
						writer.writeAttributeName("double3", "xformOp:translate:pivotInverse", false);
						writer.writeAttributeValuePoint3f(-translateArray.get(0).getAsFloat(), 
															-translateArray.get(1).getAsFloat(),
															-translateArray.get(2).getAsFloat());
						xformOp.add("xformOp:translate:pivotInverse");
					}
					writer.writeAttributeName("token[]", "xformOpOrder", true);
					writer.writeAttributeValueStringArray(xformOp);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			Mesh proxyMesh2 = proxyMesh;
			if(!noProxy) {
				if(proxyMesh == null) {
					proxyMesh2 = new Mesh(groupName + "_proxy", "", "", false, false, 1024, 16);
				}
				
				renderWriter.writeAttributeName("token", "purpose", true);
				renderWriter.writeAttributeValueString("render");
			}
			
			//for(int childId = 0; childId < numChildren; ++childId) {
			while(true) {
				boolean hasNext = readMesh(dis, renderWriter, renderWriter, 
						proxyMesh2, noProxy, materialsPrim, usedTextures, templates);
				if(!hasNext)
					break;
			}
			
			renderWriter.endChildren();
			renderWriter.endDef();
			
			if(!noProxy) {
				writeMesh(writer, proxyMesh2, materialsPrim, null, "component", templates);
				
				// If we do load in the render version of the chunk, then
				// that will have the render purpose. This means that the
				// proxy geometry should get the proxy purpose so that only
				// one of the two meshes will show up in either the viewport
				// or final render.
				// We only set the proxy geometry's purpose if we also have
				// the render meshes, because otherwise when we aren't
				// loading in the render meshes and try to final render the set
				// then it wouldn't show up because the final renderer only
				// renders meshes with no purpose or the render purpose, and
				// not the proxy purpose.
				renderWriter.beginOver(proxyMesh2.getName());
				renderWriter.beginChildren();
				renderWriter.writeAttributeName("token", "purpose", true);
				renderWriter.writeAttributeValueString("proxy");
				renderWriter.endChildren();
				renderWriter.endOver();
			}
			
			return true;
		}else {
			throw new IOException("Invalid mesh type");
		}
	}
	
	private void writeMesh(USDWriter writer, Mesh mesh, String materialsPrim, String purpose, String kind,
							Map<MatKey, Materials.MaterialTemplate> templates) throws IOException {
		int numUVs = mesh.getUs().size();
		float[] uvData = new float[numUVs * 2];
		for(int i = 0; i < numUVs; ++i) {
			uvData[i*2] = mesh.getUs().get(i);
			uvData[i*2+1] = mesh.getVs().get(i);
		}
		
		int numColors = 0;
		if(mesh.hasColors())
			numColors = mesh.getColors().size() / 3;
		float[] colorData = numColors == 0 ? null : Arrays.copyOf(mesh.getColors().getData(), mesh.getColors().size());
		int[] colorIndexData = numColors == 0 ? null : Arrays.copyOf(mesh.getColorIndices().getData(), mesh.getColorIndices().size());
		
		int numFaces = mesh.getFaceIndices().size() / 4;
		
		int[] vertexIndicesCounts = new int[numFaces];
		Arrays.fill(vertexIndicesCounts, 4);
		
		Texture textureObj = new Texture(mesh.getTexture(), mesh.getMatTexture(), numColors > 0, templates);
		
		writeMesh(writer, mesh.getName(), materialsPrim, textureObj, mesh.getExtraData(), purpose, 
					mesh.isDoubleSided(), Arrays.copyOf(mesh.getVertices().getData(), mesh.getVertices().size()), 
					Arrays.copyOf(mesh.getFaceIndices().getData(), mesh.getFaceIndices().size()), 
					vertexIndicesCounts, uvData, 
					Arrays.copyOf(mesh.getUvIndices().getData(), mesh.getUvIndices().size()),
					Arrays.copyOf(mesh.getCornerUVs().getData(), mesh.getCornerUVs().size()),
					Arrays.copyOf(mesh.getCornerUVIndices().getData(), mesh.getCornerUVIndices().size()),
					Arrays.copyOf(mesh.getNormals().getData(), mesh.getNormals().size()), 
					Arrays.copyOf(mesh.getNormalIndices().getData(), mesh.getNormalIndices().size()), 
					Arrays.copyOf(mesh.getAO().getData(), mesh.getAO().size()),
					Arrays.copyOf(mesh.getAOIndices().getData(), mesh.getAOIndices().size()),
					numColors, colorData, colorIndexData, kind);
	}
	
	private void writeMesh(USDWriter writer, String meshName, String materialsPrim, Texture texture, 
							String extraData, String purpose, boolean doubleSided, float[] vertexData, int[] faceIndices,
							int[] faceIndicesCounts, float[] uvData, int[] uvIndexData,
							float[] cornerUVData, int[] cornerUVIndexData,
							float[] normalData, int[] normalIndexData, float[] aoData, int[] aoIndexData, 
							int numColors, float[] colorData, int[] colorIndexData, String kind) throws IOException {
		writer.beginDef("Mesh", meshName);
		writer.beginMetaData();
		writer.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
		writer.writeMetaDataString("kind", kind);
		writer.endMetaData();
		writer.beginChildren();
		
		if(purpose != null && !purpose.isEmpty()) {
			writer.writeAttributeName("token", "purpose", true);
			writer.writeAttributeValueString(purpose);
		}
		
		writer.writeAttributeName("bool", "doubleSided", true);
		writer.writeAttributeValueBoolean(doubleSided);
		
		// Renderman doesn't respect the doubleSided attribute
		writer.writeAttributeName("int", "primvars:ri:attributes:Ri:Sides", false);
		writer.writeAttributeValueInt(doubleSided ? 2 : 1);
		
		// Blender doesn't respect the doubSided attribute, so
		// add the data in here so that it can be recovered
		// by the import script.
		writer.writeAttributeName("custom bool", "userProperties:doubleSided", false);
		writer.writeAttributeValueInt(doubleSided ? 1 : 0);
		
		writer.writeAttributeName("token", "subdivisionScheme", true);
		writer.writeAttributeValueString("none");
		
		writer.writeAttributeName("point3f[]", "points", false);
		writer.writeAttributeValuePoint3fArray(vertexData);
		
		writer.writeAttributeName("int[]", "faceVertexIndices", false);
		writer.writeAttributeValueIntArray(faceIndices);
		
		writer.writeAttributeName("int[]", "faceVertexCounts", false);
		writer.writeAttributeValueIntArray(faceIndicesCounts);
		
		writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
		writer.writeAttributeValuePoint2fArray(uvData);
		writer.beginMetaData();
		writer.writeMetaData("interpolation", "\"faceVarying\"");
		writer.endMetaData();
		
		writer.writeAttributeName("int[]", "primvars:st:indices", false);
		writer.writeAttributeValueIntArray(uvIndexData);
		
		if(Config.calculateCornerUVs) {
			writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
			writer.writeAttributeValuePoint2fArray(cornerUVData);
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
			
			writer.writeAttributeName("int[]", "primvars:uvCornerST:indices", false);
			writer.writeAttributeValueIntArray(cornerUVIndexData);
		}
		
		writer.writeAttributeName("normal3f[]", "primvars:normals", false);
		writer.writeAttributeValuePoint3fArray(normalData);
		writer.beginMetaData();
		writer.writeMetaData("interpolation", "\"faceVarying\"");
		writer.endMetaData();
		
		writer.writeAttributeName("int[]", "primvars:normals:indices", false);
		writer.writeAttributeValueIntArray(normalIndexData);
		
		if(Config.exportVertexColorAsDisplayColor) {
			if(numColors > 0 && Config.exportDisplayColor) {
				writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
				writer.writeAttributeValuePoint3fArray(colorData);
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:displayColor:indices", false);
				writer.writeAttributeValueIntArray(colorIndexData);
			}
		}else {
			if(numColors > 0) {
				writer.writeAttributeName("color3f[]", "primvars:Cd", false);
				writer.writeAttributeValuePoint3fArray(colorData);
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:Cd:indices", false);
				writer.writeAttributeValueIntArray(colorIndexData);
			}
			
			if(Config.exportDisplayColor) {
				Color blockColor = new Color(ResourcePacks.getDefaultColour(texture.texture));
				if(numColors > 0) {
					// Add in biome colours
					blockColor.mult(new Color(colorData[0], colorData[1], colorData[2]));
				}
				writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
				writer.writeAttributeValuePoint3fArray(new float[] {blockColor.getR(), blockColor.getG(), blockColor.getB()});
			}
		}
		
		if(Config.calculateAmbientOcclusion) {
			if(Config.exportAmbientOcclusionAsDisplayOpacity) {
				writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
				writer.writeAttributeValueFloatArray(aoData);
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:displayOpacity:indices", false);
				writer.writeAttributeValueIntArray(aoIndexData);
			}else {
				writer.writeAttributeName("float[]", "primvars:ao", false);
				writer.writeAttributeValueFloatArray(aoData);
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:ao:indices", false);
				writer.writeAttributeValueIntArray(aoIndexData);
			}
		}
		
		
		writer.writeAttributeName("rel", "material:binding", false);
		writer.writeAttributeValue("<" + materialsPrim + 
				MaterialWriter.getMaterialName(texture.texture, texture.materialTemplate, texture.hasBiomeColor) + ">");
		
		writer.endChildren();
		writer.endDef();
	}
	
	private void readIndividualBlock(LargeDataInputStream dis, Map<IndividualBlockId, List<Float>> instancers) throws IOException{
		int blockId = dis.readInt();
		int blockX = dis.readInt();
		int blockY = dis.readInt();
		int blockZ = dis.readInt();
		int numInstances = dis.readInt();
		
		List<Float> points = new ArrayList<Float>(numInstances*3);
		
		for(int instanceId = 0; instanceId < numInstances; ++instanceId) {
			points.add(dis.readFloat());
			points.add(dis.readFloat());
			points.add(dis.readFloat());
		}
		
		instancers.put(new IndividualBlockId(blockId, blockX, blockY, blockZ), points);
	}
	
	@Override
	public ExportData getExportData(File file) {
		if(!file.exists() || !file.getName().endsWith(".usd"))
			return null;
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String firstLine = in.readLine();
			if(!firstLine.startsWith("#usda 1.0")) {
				// It's not a usda file, so we first have to convert it into
				// usda.
				in.close();
				
				if(!FileUtil.hasUSDCat())
					return null;
				
				File outFile = File.createTempFile("miex_", ".usd");
				outFile.deleteOnExit();
				// Convert from Crate to ASCII
				String usdCatExe = FileUtil.getUSDCatExe();
				ProcessBuilder builder = new ProcessBuilder(new File(usdCatExe).getCanonicalPath(), 
															file.getCanonicalPath(), 
															"--out", outFile.getCanonicalPath(), "--usdFormat", "usda");
				builder.directory(new File(usdCatExe).getParentFile());
				Process usdCatProcess = builder.start();
				int returnCode = usdCatProcess.waitFor();
				if(returnCode != 0)
					return null;
				
				// It's been converted, so read it
				in = new BufferedReader(new FileReader(outFile));
				firstLine = in.readLine();
				if(!firstLine.startsWith("#usda 1.0")) {
					in.close();
					return null;
				}
			}
			
			ExportData data = new ExportData();
			
			// Go through each line to find the start of the MiEx dict
			boolean foundDict = false;
			while(true) {
				String line = in.readLine();
				if(line == null)
					break;
				
				if(line.trim().equals(")"))
					break; // We've reached the end of the metadata part. 
				
				if(line.contains("dictionary MiEx")) {
					foundDict = true;
					continue;
				}
				if(!foundDict)
					continue;
				
				if(line.contains("}")) {
					// We've reached the end of the dict.
					break;
				}
				
				// Now it's reading in the data
				if(!line.contains("="))
					continue; // Not a valid line
				
				int equalIndex = line.indexOf('=');
				String[] nameTokens = line.substring(0, equalIndex).trim().split("\\s");
				String name = nameTokens[nameTokens.length-1];
				String type = nameTokens[nameTokens.length-2];
				String value = line.substring(equalIndex+1).trim();
				
				try {
					Field field = ExportData.class.getField(name);
					if(type.equals("int")) {
						if(field.getType() == Boolean.TYPE)
							field.set(data, Integer.parseInt(value) > 0 ? true : false);
						else
							field.set(data, Integer.parseInt(value));
					}else if(type.equals("string")) {
						field.set(data, value.substring(1, value.length()-1));
					}else if(type.equals("string[]")) {
						if(name.equals("resourcePacks")) {
							String[] values = value.substring(1, value.length()-1).split(",");
							List<ResourcePack> valuesList = new ArrayList<ResourcePack>();
							for(String val : values) {
								val = val.trim();
								if(val.length() > 2) {
									ResourcePack pack = ResourcePacks.getResourcePack(val.substring(1, val.length()-1));
									if(pack != null)
										valuesList.add(pack);
								}
							}
							field.set(data, valuesList);
						}else {
							String[] values = value.substring(1, value.length()-1).split(",");
							List<String> valuesList = new ArrayList<String>();
							for(String val : values) {
								val = val.trim();
								if(val.length() > 2)
									valuesList.add(val.substring(1, val.length()-1));
							}
							field.set(data, valuesList);
						}
					}else if(type.equals("int[]")) {
						if(name.equals("disabledChunks")) {
							String[] values = value.substring(1, value.length()-1).split(",");
							List<Pair<Integer, Integer>> valuesList = new ArrayList<Pair<Integer, Integer>>();
							int prevVal = Integer.MIN_VALUE;
							for(String val : values) {
								val = val.trim();
								if(val.isEmpty())
									continue;
								int intVal = Integer.parseInt(val);
								if(prevVal == Integer.MIN_VALUE)
									prevVal = intVal;
								else {
									valuesList.add(new Pair<Integer, Integer>(prevVal, intVal));
									prevVal = Integer.MIN_VALUE;
								}
							}
							field.set(data, valuesList);
						}else {
							String[] values = value.substring(1, value.length()-1).split(",");
							List<Integer> valuesList = new ArrayList<Integer>();
							for(String val : values) {
								val = val.trim();
								int intVal = Integer.parseInt(val);
								valuesList.add(intVal);
							}
							field.set(data, valuesList);
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			in.close();
			return data;
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(in != null) {
			try {
				in.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		return null;
	}
	
	private void convertEntities(LargeDataInputStream dis, File usdFile) throws IOException {
		USDWriter writer = new USDWriter(usdFile);
		Map<MatKey, Materials.MaterialTemplate> templates = new HashMap<MatKey, Materials.MaterialTemplate>();
		
		writer.beginMetaData();
		writer.writeMetaDataString("defaultPrim", "entities");
		writer.endMetaData();
		
		writer.beginDef("Xform", "entities");
		writer.beginMetaData();
		writer.writeMetaDataString("kind", "group");
		writer.endMetaData();
		writer.beginChildren();
		
		writer.beginDef("Scope", "entityPrototypes");
		writer.beginChildren();
		
		int numPrototypes = dis.readInt();
		for(int i = 0; i < numPrototypes; ++i) {
			String entityName = dis.readUTF();
			String primName = "_class_" + entityName;
			
			writer.beginClass("Xform", primName);
			writer.beginMetaData();
			writer.writeMetaDataString("kind", "subcomponent");
			writer.endMetaData();
			writer.beginChildren();
			
			int numMeshes = dis.readInt();
			for(int meshId = 0; meshId < numMeshes; ++meshId)
				readMesh(dis, writer, writer, null, true, "/entities/materials.", entityUsedTextures, templates);
			
			writer.endChildren();
			writer.endClass();
		}
		writer.endChildren();
		writer.endDef();
		
		writer.beginDef("Scope", "materials");
		writer.beginChildren();
		for(Texture tex : entityUsedTextures) {
			writer.writeAttributeName("rel", 
					MaterialWriter.getMaterialName(tex.texture, tex.materialTemplate, tex.hasBiomeColor), false);
		}
		writer.endChildren();
		writer.endDef();
		
		this.usedTextures.addAll(entityUsedTextures);
		
		int numEntityInstanceTypes = dis.readInt();
		for(int i = 0; i < numEntityInstanceTypes; ++i) {
			String prototypeName = dis.readUTF();
			int numEntityInstances = dis.readInt();
			if(numEntityInstances <= 0)
				continue;
			
			writer.beginDef("Xform", prototypeName);
			writer.beginMetaData();
			writer.writeMetaDataString("kind", "component");
			writer.endMetaData();
			writer.beginChildren();
			
			String classPrimName = "/entities/entityPrototypes/_class_" + prototypeName;
			for(int j = 0; j < numEntityInstances; ++j) {
				AnimationChannel3D posAnim = new AnimationChannel3D(null, null, null);
				posAnim.read(dis);
				AnimationChannel3D rotAnim = new AnimationChannel3D(null, null, null);
				rotAnim.read(dis);
				AnimationChannel3D scaleAnim = new AnimationChannel3D(null, null, null);
				scaleAnim.read(dis);
				
				int numBones = dis.readInt();
				
				writer.beginDef("Xform", prototypeName + "_" + j);
				writer.beginMetaData();
				writer.writeInherit(classPrimName);
				if(numBones == 0)
					writer.writeMetaDataBoolean("instanceable", true);
				writer.endMetaData();
				writer.beginChildren();
				
				if(posAnim.getKeyframes().size() == 1) {
					float posX = posAnim.getKeyframes().get(0).valueX;
					float posY = posAnim.getKeyframes().get(0).valueY;
					float posZ = posAnim.getKeyframes().get(0).valueZ;
					writer.writeAttributeName("double3", "xformOp:translate", false);
					writer.writeAttributeValuePoint3f(posX, posY, posZ);
				}else {
					writer.writeAttributeName("double3", "xformOp:translate.timeSamples", false);
					writer.writeAttributeValueAnimation3D(posAnim, 24f, 1f, 1f, 1f);
				}
				if(rotAnim.getKeyframes().size() == 1) {
					float rotX = rotAnim.getKeyframes().get(0).valueX;
					float rotY = rotAnim.getKeyframes().get(0).valueY;
					float rotZ = rotAnim.getKeyframes().get(0).valueZ;
					writer.writeAttributeName("double3", "xformOp:rotateXYZ", false);
					writer.writeAttributeValuePoint3f(-rotX, -rotY, rotZ);
				}else {
					writer.writeAttributeName("double3", "xformOp:rotateXYZ.timeSamples", false);
					writer.writeAttributeValueAnimation3D(rotAnim, 24f, -1f, -1f, 1f);
				}
				if(scaleAnim.getKeyframes().size() == 1) {
					float scaleX = scaleAnim.getKeyframes().get(0).valueX;
					float scaleY = scaleAnim.getKeyframes().get(0).valueY;
					float scaleZ = scaleAnim.getKeyframes().get(0).valueZ;
					writer.writeAttributeName("double3", "xformOp:scale", false);
					writer.writeAttributeValuePoint3f(scaleX, scaleY, scaleZ);
				}else {
					writer.writeAttributeName("double3", "xformOp:scale.timeSamples", false);
					writer.writeAttributeValueAnimation3D(scaleAnim, 24f, 1f, 1f, 1f);
				}
				writer.writeAttributeName("token[]", "xformOpOrder", true);
				writer.writeAttributeValueStringArray(new String[] { "xformOp:translate", "xformOp:rotateXYZ", "xformOp:scale" });
				
				for(int k = 0; k < numBones; ++k) {
					convertEntityBone(writer, dis);
				}
				
				writer.endChildren();
				writer.endDef();
			}
			
			writer.endChildren();
			writer.endDef();
		}
		
		writer.endChildren();
		writer.endDef();
		writer.close(false);
	}
	
	private void convertEntityBone(USDWriter writer, LargeDataInputStream dis) throws IOException{
		String boneName = dis.readUTF();
		boolean hasAnimationData = dis.readInt() == 1;
		AnimationChannel3D posAnim = null;
		AnimationChannel3D rotAnim = null;
		AnimationChannel3D scaleAnim = null;
		if(hasAnimationData) {
			posAnim = new AnimationChannel3D(null, null, null);
			posAnim.read(dis);
			rotAnim = new AnimationChannel3D(null, null, null);
			rotAnim.read(dis);
			scaleAnim = new AnimationChannel3D(null, null, null);
			scaleAnim.read(dis);
		}
		boolean visibility = dis.readBoolean();
		int numChildren = dis.readInt();
		
		writer.beginOver(boneName);
		writer.beginChildren();
		
		if(hasAnimationData) {
			if(posAnim.getKeyframes().size() == 1) {
				float posX = posAnim.getKeyframes().get(0).valueX;
				float posY = posAnim.getKeyframes().get(0).valueY;
				float posZ = posAnim.getKeyframes().get(0).valueZ;
				writer.writeAttributeName("double3", "xformOp:translate", false);
				writer.writeAttributeValuePoint3f(posX, posY, -posZ);
			}else {
				writer.writeAttributeName("double3", "xformOp:translate.timeSamples", false);
				writer.writeAttributeValueAnimation3D(posAnim, 24f, 1f, 1f, -1f);
			}
			
			if(rotAnim.getKeyframes().size() == 1) {
				float rotX = rotAnim.getKeyframes().get(0).valueX;
				float rotY = rotAnim.getKeyframes().get(0).valueY;
				float rotZ = rotAnim.getKeyframes().get(0).valueZ;
				writer.writeAttributeName("double3", "xformOp:rotateXYZ", false);
				writer.writeAttributeValuePoint3f(rotX, -rotY, -rotZ);
			}else {
				writer.writeAttributeName("double3", "xformOp:rotateXYZ.timeSamples", false);
				writer.writeAttributeValueAnimation3D(rotAnim, 24f, 1f, -1f, -1f);
			}
			if(scaleAnim.getKeyframes().size() == 1) {
				float scaleX = scaleAnim.getKeyframes().get(0).valueX;
				float scaleY = scaleAnim.getKeyframes().get(0).valueY;
				float scaleZ = scaleAnim.getKeyframes().get(0).valueZ;
				writer.writeAttributeName("double3", "xformOp:scale", false);
				writer.writeAttributeValuePoint3f(scaleX, scaleY, scaleZ);
			}else {
				writer.writeAttributeName("double3", "xformOp:scale.timeSamples", false);
				writer.writeAttributeValueAnimation3D(scaleAnim, 24f, 1f, 1f, 1f);
			}
			
			writer.writeAttributeName("token[]", "xformOpOrder", true);
			writer.writeAttributeValueStringArray(new String[] { "xformOp:translate", "xformOp:rotateXYZ", "xformOp:scale"});
		}
		
		if(visibility == false) {
			writer.writeAttributeName("token", "visibility", false);
			writer.writeAttributeValueString("invisible");
		}
		
		for(int i = 0; i < numChildren; ++i) {
			convertEntityBone(writer, dis);
		}
		
		writer.endChildren();
		writer.endOver();
	}
	
}
