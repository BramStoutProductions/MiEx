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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.export.Converter;
import nl.bramstout.mcworldexporter.export.ExportData;
import nl.bramstout.mcworldexporter.export.LargeDataInputStream;
import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.export.json.JsonMaterialWriter;
import nl.bramstout.mcworldexporter.export.materialx.MaterialXMaterialWriter;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.resourcepack.BannerTextureCreator;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class USDConverter extends Converter{

	private static class ChunkInfo{
		String name = "";
		boolean isFG = false;
		Map<Integer, List<Float>> instancers = new HashMap<Integer, List<Float>>();
		Set<Texture> usedTextures = new HashSet<Texture>();
	}
	
	private static class IndividualBlockInfo{
		String path;
		
		public IndividualBlockInfo(String path) {
			this.path = path;
		}
	}
	private static class Texture{
		String texture;
		boolean hasBiomeColor;
		
		public Texture(String texture, boolean hasBiomeColor) {
			this.texture = texture;
			this.hasBiomeColor = hasBiomeColor;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Texture)
				return ((Texture)obj).texture.equals(texture) && ((Texture)obj).hasBiomeColor == hasBiomeColor;
			return false;
		}
		
		@Override
		public int hashCode() {
			return texture.hashCode();
		}
	}
	
	private LargeDataInputStream dis;
	private File inputFile;
	private File outputFile;
	private File chunksFolder;
	private File materialsFile;
	private Map<Integer, IndividualBlockInfo> individualBlocksRegistry;
	private Set<Texture> usedTextures = new HashSet<Texture>();
	private List<ChunkInfo> chunkInfosFG = new ArrayList<ChunkInfo>();
	private List<ChunkInfo> chunkInfosBG = new ArrayList<ChunkInfo>();
	private Object mutex = new Object();
	private static ExecutorService threadPool = Executors.newWorkStealingPool();
	public static File currentOutputDir = null;
	
	public USDConverter(File inputFile, File outputFile) throws IOException {
		if(inputFile == null || outputFile == null)
			return;
		
		this.inputFile = inputFile;
		dis = new LargeDataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
		this.outputFile = outputFile;
		chunksFolder = new File(outputFile.getParentFile(), outputFile.getName().replace(".usd", "_chunks"));
		if(chunksFolder.exists())
			deleteDir(chunksFolder);
		chunksFolder.mkdirs();
		materialsFile = new File(outputFile.getPath().replace(".usd", "_materials.usd"));
		individualBlocksRegistry = new HashMap<Integer, IndividualBlockInfo>();
		currentOutputDir = outputFile.getParentFile();
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
		
		int numChunks = dis.readInt();
		String[] chunkFilenames = new String[numChunks];
		for(int i = 0; i < numChunks; ++i)
			chunkFilenames[i] = dis.readUTF();
		
		dis.skipBytes(invididualBlocksOffset - dis.getPosition());
		int numBaseMeshes = dis.readInt();
		if(numBaseMeshes > 0) {
			rootWriter.beginDef("Scope", "individualBlocksBaseMeshes");
			rootWriter.beginChildren();
			
			for(int i = 0; i < numBaseMeshes; ++i) {
				int baseMeshId = dis.readInt();
				String blockName = dis.readUTF();
				String primName = "_class_" + Util.makeSafeName(blockName) + baseMeshId;
				
				rootWriter.beginClass("Xform", primName);
				rootWriter.beginMetaData();
				rootWriter.writeMetaDataString("kind", "subcomponent");
				rootWriter.endMetaData();
				rootWriter.beginChildren();
				
				Set<Texture> usedTextures = new HashSet<Texture>();
				
				int numMeshes = dis.readInt();
				for(int meshId = 0; meshId < numMeshes; ++meshId)
					readMesh(dis, rootWriter, rootWriter, null, true, "/world/materials/", usedTextures);
				
				rootWriter.endChildren();
				rootWriter.endClass();
				
				
				this.usedTextures.addAll(usedTextures);
				individualBlocksRegistry.put(baseMeshId, new IndividualBlockInfo(primName));
			}
			rootWriter.endChildren();
			rootWriter.endDef();
		}
		
		// We're done reading from this input stream.
		// Each ConvertChunkTask will create their own input stream.
		dis.close();
		
		MCWorldExporter.getApp().getUI().getProgressBar().setNumChunks(numChunks);;

		List<Future<?>> futures = new ArrayList<Future<?>>();
		for(int chunkId = 0; chunkId < numChunks; ++chunkId) {
			futures.add(threadPool.submit(new ConvertChunkTask(new File(inputFile.getParentFile(), chunkFilenames[chunkId]), this)));
		}
		for(Future<?> future : futures) {
			try {
				future.get();
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
			Materials.MaterialTemplate material = Materials.getMaterial(texture.texture, texture.hasBiomeColor, currentOutputDir.getCanonicalPath());
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
		
		rootWriter.writeAttributeName("string[]", "resourcePacks", false);
		rootWriter.writeAttributeValueStringArray(data.resourcePacks);
		
		rootWriter.writeAttributeName("int", "runOptimiser", false);
		rootWriter.writeAttributeValueInt(data.runOptimiser ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "removeCaves", false);
		rootWriter.writeAttributeValueInt(data.removeCaves ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "fillInCaves", false);
		rootWriter.writeAttributeValueInt(data.fillInCaves ? 1 : 0);
		
		rootWriter.writeAttributeName("int", "onlyIndividualBlocks", false);
		rootWriter.writeAttributeValueInt(data.onlyIndividualBlocks ? 1 : 0);
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
				rootWriter.writeAttributeName("rel", "MAT_" + Util.makeSafeName(tex.texture) + 
												(tex.hasBiomeColor ? "_BIOME" : ""), false);
				rootWriter.writeAttributeValue("</world/materials/" + "MAT_" + Util.makeSafeName(tex.texture) + 
												(tex.hasBiomeColor ? "_BIOME" : "") + ">");
			}
			rootWriter.endChildren();
			rootWriter.endOver();
			
			for(Entry<Integer, List<Float>> instancer : chunkInfo.instancers.entrySet()) {
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
				int numMeshes = dis.readInt();
				
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
				
				
				for(int meshId = 0; meshId < numMeshes; ++meshId) {
					converter.readMesh(dis, chunkWriter, chunkRenderWriter, null, false, "/chunk/materials.", chunkInfo.usedTextures);
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
				chunkWriter.writeAttributeName("rel", "MAT_" + Util.makeSafeName(tex.texture) + 
												(tex.hasBiomeColor ? "_BIOME" : ""), false);
			}
			chunkWriter.endChildren();
			chunkWriter.endDef();
		}
	}
	
	private void readMesh(LargeDataInputStream dis, USDWriter writer, USDWriter renderWriter, Mesh proxyMesh, boolean noProxy, 
							String materialsPrim, Set<Texture> usedTextures) throws IOException{
		byte meshType = dis.readByte();
		if(meshType == 1) { // Mesh
			
			String meshName = dis.readUTF();
			if(meshName.equals(""))
				meshName = "mesh";
			else
				meshName = Util.makeSafeName(meshName);
			boolean doubleSided = dis.readInt() > 0;
			String texture = dis.readUTF();
			String extraData = dis.readUTF();
			int numVertices = dis.readInt();
			int numUVs = dis.readInt();
			int numNormals = dis.readInt();
			int numEdges = dis.readInt();
			int numFaces = dis.readInt();
			int numColors = dis.readInt();
			
			// Handle banners
			if(texture.startsWith("banner:")) {
				String bannerTexName = texture.substring("banner:".length());
				File textureFolder = new File(chunksFolder, "banners");
				try {
					BannerTextureCreator.createBannerTexture(extraData, textureFolder, bannerTexName);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				texture = "./" + chunksFolder.getName() + "/banners/" + bannerTexName;
			}
			
			usedTextures.add(new Texture(texture, numColors > 0));
			
			float[] vertexData = new float[numVertices * 3];
			float[] uvData = new float[numUVs * 2];
			float[] normalData = new float[numNormals * 3];
			float[] colorData = numColors == 0 ? null : new float[numColors * 3];
			int[] edgeData = new int[numEdges * 3];
			int[] edgeIndexData = new int[numFaces * 4];
			int[] uvIndexData = new int[numFaces * 4];
			int[] normalIndexData = new int[numFaces * 4];
			int[] colorIndexData = numColors == 0 ? null : new int[numFaces * 4];
			
			for(int i = 0; i < numVertices * 3; ++i)
				vertexData[i] = dis.readFloat();
			
			for(int i = 0; i < numUVs; ++i)
				uvData[i*2] = dis.readFloat();
			for(int i = 0; i < numUVs; ++i)
				uvData[i*2+1] = dis.readFloat();
			
			for(int i = 0; i < numNormals * 3; ++i)
				normalData[i] = dis.readFloat();
			
			if(numColors > 0)
				for(int i = 0; i < numColors * 3; ++i)
					colorData[i] = dis.readFloat();
			
			for(int i = 0; i < numEdges * 3; ++i)
				edgeData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				edgeIndexData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				uvIndexData[i] = dis.readInt();
			
			for(int i = 0; i < numFaces * 4; ++i)
				normalIndexData[i] = dis.readInt();
			
			if(numColors > 0)
				for(int i = 0; i < numFaces * 4; ++i)
					colorIndexData[i] = dis.readInt();
			
			int[] vertexIndices = new int[numFaces * 4];
			for(int i = 0; i < numFaces * 4; ++i)
				vertexIndices[i] = edgeData[edgeIndexData[i] * 3];
			
			int[] vertexIndicesCounts = new int[numFaces];
			Arrays.fill(vertexIndicesCounts, 4);
			
			writeMesh(writer, meshName, materialsPrim, texture, extraData, null, doubleSided, vertexData, vertexIndices, 
						vertexIndicesCounts, uvData, uvIndexData, normalData, normalIndexData, 
						numColors, colorData, colorIndexData, proxyMesh == null ? "component" : "subcomponent");
			
			
			if(proxyMesh != null) {
				proxyMesh.setTexture(texture);
				proxyMesh.setExtraData(extraData);
				proxyMesh.setDoubleSided(doubleSided);
				Mesh subMesh = new Mesh(meshName, texture, doubleSided, extraData, vertexData, uvData, colorData, normalData, 
										edgeData, edgeIndexData, uvIndexData, colorIndexData, normalIndexData);
				proxyMesh.appendMesh(subMesh);
			}
			
		}else if(meshType == 2) { // Group
			
			String groupName = dis.readUTF();
			if(groupName.equals(""))
				groupName = "mesh";
			else
				groupName = Util.makeSafeName(groupName);
			int numChildren = dis.readInt();
			
			
			renderWriter.beginDef("Xform", groupName);
			renderWriter.beginMetaData();
			renderWriter.writeMetaDataString("kind", "component");
			renderWriter.endMetaData();
			renderWriter.beginChildren();
			
			Mesh proxyMesh2 = proxyMesh;
			if(!noProxy) {
				if(proxyMesh == null) {
					proxyMesh2 = new Mesh();
					proxyMesh2.setName(groupName + "_proxy");
				}
				
				renderWriter.writeAttributeName("token", "purpose", true);
				renderWriter.writeAttributeValueString("render");
			}
			
			for(int childId = 0; childId < numChildren; ++childId) {
				readMesh(dis, renderWriter, renderWriter, 
						proxyMesh2, noProxy, materialsPrim, usedTextures);
			}
			
			renderWriter.endChildren();
			renderWriter.endDef();
			
			if(!noProxy) {
				writeMesh(writer, proxyMesh2, materialsPrim, null, "component");
				
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
			
		}else {
			throw new IOException("Invalid mesh type");
		}
	}
	
	private void writeMesh(USDWriter writer, Mesh mesh, String materialsPrim, String purpose, String kind) throws IOException {
		int numUVs = mesh.getUs().size();
		float[] uvData = new float[numUVs * 2];
		for(int i = 0; i < numUVs; ++i) {
			uvData[i*2] = mesh.getUs().get(i);
			uvData[i*2+1] = mesh.getVs().get(i);
		}
		
		int numColors = 0;
		if(mesh.getColors() != null)
			numColors = mesh.getColors().size() / 3;
		float[] colorData = numColors == 0 ? null : Arrays.copyOf(mesh.getColors().getData(), mesh.getColors().size());
		int[] colorIndexData = numColors == 0 ? null : Arrays.copyOf(mesh.getColorIndices().getData(), mesh.getColorIndices().size());
		
		int numFaces = mesh.getEdgeIndices().size() / 4;
		int[] vertexIndices = new int[numFaces * 4];
		for(int i = 0; i < numFaces * 4; ++i)
			vertexIndices[i] = mesh.getEdges().get(mesh.getEdgeIndices().get(i) * 3);
		
		int[] vertexIndicesCounts = new int[numFaces];
		Arrays.fill(vertexIndicesCounts, 4);
		
		writeMesh(writer, mesh.getName(), materialsPrim, mesh.getTexture(), mesh.getExtraData(), purpose, mesh.isDoubleSided(), 
					Arrays.copyOf(mesh.getVertices().getData(), mesh.getVertices().size()), 
					vertexIndices, vertexIndicesCounts, uvData, 
					Arrays.copyOf(mesh.getUvIndices().getData(), mesh.getUvIndices().size()), 
					Arrays.copyOf(mesh.getNormals().getData(), mesh.getNormals().size()), 
					Arrays.copyOf(mesh.getNormalIndices().getData(), mesh.getNormalIndices().size()), 
					numColors, colorData, colorIndexData, kind);
	}
	
	private void writeMesh(USDWriter writer, String meshName, String materialsPrim, String texture, String extraData, String purpose,
							boolean doubleSided, float[] vertexData, int[] vertexIndices,
							int[] vertexIndicesCounts, float[] uvData, int[] uvIndexData,
							float[] normalData, int[] normalIndexData, int numColors,
							float[] colorData, int[] colorIndexData, String kind) throws IOException {
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
		writer.writeAttributeValueIntArray(vertexIndices);
		
		writer.writeAttributeName("int[]", "faceVertexCounts", false);
		writer.writeAttributeValueIntArray(vertexIndicesCounts);
		
		writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
		writer.writeAttributeValuePoint2fArray(uvData);
		writer.beginMetaData();
		writer.writeMetaData("interpolation", "\"faceVarying\"");
		writer.endMetaData();
		
		writer.writeAttributeName("int[]", "primvars:st:indices", false);
		writer.writeAttributeValueIntArray(uvIndexData);
		
		writer.writeAttributeName("normal3f[]", "primvars:normals", false);
		writer.writeAttributeValuePoint3fArray(normalData);
		writer.beginMetaData();
		writer.writeMetaData("interpolation", "\"faceVarying\"");
		writer.endMetaData();
		
		writer.writeAttributeName("int[]", "primvars:normals:indices", false);
		writer.writeAttributeValueIntArray(normalIndexData);
		
		if(numColors > 0) {
			writer.writeAttributeName("color3f[]", "primvars:Cd", false);
			writer.writeAttributeValuePoint3fArray(colorData);
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
			
			writer.writeAttributeName("int[]", "primvars:Cd:indices", false);
			writer.writeAttributeValueIntArray(colorIndexData);
		}
		
		Color blockColor = new Color(ResourcePack.getDefaultColour(texture));
		if(numColors > 0) {
			// Add in biome colours
			blockColor.mult(new Color(colorData[0], colorData[1], colorData[2]));
		}
		writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
		writer.writeAttributeValuePoint3fArray(new float[] {blockColor.getR(), blockColor.getG(), blockColor.getB()});
		
		
		writer.writeAttributeName("rel", "material:binding", false);
		writer.writeAttributeValue("<" + materialsPrim + "MAT_" + Util.makeSafeName(texture) + 
				(numColors > 0 ? "_BIOME" : "")+ ">");
		
		writer.endChildren();
		writer.endDef();
	}
	
	private void readIndividualBlock(LargeDataInputStream dis, Map<Integer, List<Float>> instancers) throws IOException{
		int blockId = dis.readInt();
		int numInstances = dis.readInt();
		
		List<Float> points = new ArrayList<Float>(numInstances*3);
		
		for(int instanceId = 0; instanceId < numInstances; ++instanceId) {
			points.add(dis.readFloat());
			points.add(dis.readFloat());
			points.add(dis.readFloat());
		}
		
		instancers.put(blockId, points);
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
				
				String[] tokens = line.split("=");
				String[] nameTokens = tokens[0].trim().split("\\s");
				String name = nameTokens[nameTokens.length-1];
				String type = nameTokens[nameTokens.length-2];
				String value = tokens[1].trim();
				
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
						String[] values = value.substring(1, value.length()-1).split(",");
						List<String> valuesList = new ArrayList<String>();
						for(String val : values) {
							val = val.trim();
							if(val.length() > 2)
								valuesList.add(val.substring(1, val.length()-1));
						}
						field.set(data, valuesList);
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
	
}
