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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.ExportBounds.ExcludeRegion;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel3D;
import nl.bramstout.mcworldexporter.export.AnimatedBlock;
import nl.bramstout.mcworldexporter.export.AnimatedBlock.AnimatedBlockId;
import nl.bramstout.mcworldexporter.export.Converter;
import nl.bramstout.mcworldexporter.export.ExportData;
import nl.bramstout.mcworldexporter.export.Exporter;
import nl.bramstout.mcworldexporter.export.FloatArray;
import nl.bramstout.mcworldexporter.export.IndividualBlockId;
import nl.bramstout.mcworldexporter.export.LargeDataInputStream;
import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.export.MeshGroup;
import nl.bramstout.mcworldexporter.export.MeshPurpose;
import nl.bramstout.mcworldexporter.export.MeshSubset;
import nl.bramstout.mcworldexporter.export.VertexColorSet;
import nl.bramstout.mcworldexporter.export.json.JsonMaterialWriter;
import nl.bramstout.mcworldexporter.export.materialx.MaterialXMaterialWriter;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialNetwork;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialTemplate;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingAttribute;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingNode;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;
import nl.bramstout.mcworldexporter.resourcepack.BannerTextureCreator;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class USDConverter extends Converter{

	private static enum Kind{
		ASSEMBLY("assembly"), GROUP("group"), COMPONENT("component"), SUBCOMPONENT("subcomponent");
		
		public String strValue;
		
		Kind(String strValue){
			this.strValue = strValue;
		}
	}
	
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
	
	private static class Texture{
		String texture;
		Materials.MaterialTemplate materialTemplate;
		boolean hasBiomeColor;
		
		public Texture(String texture, String matTexture, boolean hasBiomeColor, boolean isDoubleSided, 
						Set<String> colorSets, Map<MatKey, Materials.MaterialTemplate> templates) {
			this.texture = texture;
			MatKey matKey = new MatKey(matTexture, hasBiomeColor);
			this.materialTemplate = templates.getOrDefault(matKey, null);
			if(this.materialTemplate == null) {
				this.materialTemplate = Materials.getMaterial(matTexture, hasBiomeColor, isDoubleSided, colorSets,
										Exporter.currentExportFile.getParentFile().getAbsolutePath());
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
	
	private static class IndividualBlockInfo{
		String path;
		
		public IndividualBlockInfo(String path) {
			this.path = path;
		}
	}
	
	private static ThreadPool threadPool = new ThreadPool("USD_Converter", 2048);
	
	private LargeDataInputStream dis;
	private File inputFile;
	private File outputFile;
	private File chunksFolder;
	private File materialsFile;
	private Map<IndividualBlockId, IndividualBlockInfo> individualBlocksRegistry;
	
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
	public void init() throws Exception {
		dis = new LargeDataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
	}
	
	@Override
	public boolean deleteMiExFiles() {
		return true;
	}

	@Override
	public void convert() throws Exception {
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Converting to USD");
		int version = dis.readInt();
		if(version != 2)
			throw new IOException("Unsupport input file version");
		
		long individualBlocksOffset = dis.readLong();
		ExportData exportData = ExportData.fromStream(dis);
		
		USDWriter rootWriter = new USDWriter(outputFile);
		rootWriter.beginMetaData();
		rootWriter.writeMetaDataString("defaultPrim", "world");
		rootWriter.writeMetaDataFloat("metersPerUnit", Config.usdMetersPerUnit);
		rootWriter.writeMetaDataString("upAxis", "Y");
		rootWriter.writeMetaDataFloat("timeCodesPerSecond", Config.animationFrameRate);
		rootWriter.writeMetaDataInt("startTimeCode", (int) Config.animationStartFrame);
		rootWriter.writeMetaDataInt("endTimeCode", (int) Config.animationEndFrame);
		
		writeExportData(exportData, rootWriter);
		
		rootWriter.beginDef("Xform", "world");
		rootWriter.beginMetaData();
		rootWriter.writeVariantSets("MiEx_LOD");
		rootWriter.writeMetaDataString("kind", Kind.ASSEMBLY.strValue);
		rootWriter.endMetaData();
		rootWriter.beginChildren();
		
		Map<String, Texture> usedTextures = new HashMap<String, Texture>();
		
		writeEntities(rootWriter, usedTextures);
		
		String[] chunkFilenames = readChunkNames();
		
		writeIndividualBlocks(rootWriter, usedTextures, individualBlocksOffset);
		
		// Done with this input file, so we can close it.
		dis.close();
		
		List<ConvertChunkTask> bgChunks = new ArrayList<ConvertChunkTask>();
		List<ConvertChunkTask> fgChunks = new ArrayList<ConvertChunkTask>();
		
		writeChunks(rootWriter, usedTextures, chunkFilenames, bgChunks, fgChunks);
		
		writeMaterials(rootWriter, usedTextures);
		
		writeChunkPayloads(rootWriter, bgChunks, fgChunks);
		
		rootWriter.endChildren();
		rootWriter.endDef();
		rootWriter.close(false);
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.95f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Cleaning up");
		
		USDWriter.finalCleanup();
	}
	
	@SuppressWarnings("unchecked")
	private void writeExportData(ExportData exportData, USDWriter writer) throws IOException {
		writer.writeMetaData("customLayerData");
		writer.beginDict();
		writer.writeAttributeName("dictionary", "MiEx", false);
		writer.beginDict();
		
		for(Field field : ExportData.class.getDeclaredFields()) {
			try {
				if(Modifier.isStatic(field.getModifiers()))
					continue;
				if(!Modifier.isPublic(field.getModifiers()))
					continue;
				Class<?> type = field.getType();
				if(type.equals(String.class)) {
					writer.writeAttributeName("string", field.getName(), false);
					writer.writeAttributeValueString(((String) field.get(exportData)).replace('\\', '/'));
				}else if(type.equals(int.class)) {
					writer.writeAttributeName("int", field.getName(), false);
					writer.writeAttributeValueInt(field.getInt(exportData));
				}else if(type.equals(boolean.class)) {
					writer.writeAttributeName("int", field.getName(), false);
					writer.writeAttributeValueInt(field.getBoolean(exportData) ? 1 : 0);
				}else if(type.equals(List.class)) {
					ParameterizedType subTypes = (ParameterizedType) field.getGenericType();
					Type[] types = subTypes.getActualTypeArguments();
					Class<?> subType = null;
					if(types[0] instanceof ParameterizedType)
						subType = (Class<?>)((ParameterizedType) types[0]).getRawType();
					else
						subType = (Class<?>) types[0];
					if(subType.equals(String.class)) {
						writer.writeAttributeName("string[]", field.getName(), false);
						writer.writeAttributeValueStringArray((List<String>)field.get(exportData));
					}else if(subType.equals(Pair.class)) {
						writer.writeAttributeName("int[]", field.getName(), false);
						List<Pair<Integer, Integer>> data = (List<Pair<Integer, Integer>>) field.get(exportData);
						int[] intData = new int[data.size()*2];
						int i = 0;
						for(Pair<Integer, Integer> chunk : data) {
							intData[i] = chunk.getKey().intValue();
							intData[i+1] = chunk.getValue().intValue();
							i += 2;
						}
						writer.writeAttributeValueIntArray(intData);
					}else if(subType.equals(ResourcePack.class)) {
						writer.writeAttributeName("string[]", "resourcePacks", false);
						List<ResourcePack> data = (List<ResourcePack>) field.get(exportData);
						List<String> stringData = new ArrayList<String>();
						for(ResourcePack pack : data)
							stringData.add(pack.getUUID());
						writer.writeAttributeValueStringArray(stringData);
					}else if(subType.equals(ExportBounds.class)){
						List<ExportBounds> data = (List<ExportBounds>) field.get(exportData);
						for(ExportBounds exportBounds : data) {
							String ebNamespace = "region_" + exportBounds.getSafeName() + "_";
							for(Field ebField : ExportBounds.class.getFields()) {
								Class<?> ebType = ebField.getType();
								if(ebType.equals(String.class)) {
									writer.writeAttributeName("string", ebNamespace + ebField.getName(), false);
									writer.writeAttributeValueString(((String) ebField.get(exportBounds)).replace('\\', '/'));
								}else if(ebType.equals(int.class)) {
									writer.writeAttributeName("int", ebNamespace + ebField.getName(), false);
									writer.writeAttributeValueInt(ebField.getInt(exportBounds));
								}else if(ebType.equals(boolean.class)) {
									writer.writeAttributeName("int", ebNamespace + ebField.getName(), false);
									writer.writeAttributeValueInt(ebField.getBoolean(exportBounds) ? 1 : 0);
								}else if(ebType.equals(List.class)) {
									ParameterizedType ebSubTypes = (ParameterizedType) ebField.getGenericType();
									Type[] ebTypes = ebSubTypes.getActualTypeArguments();
									Class<?> ebSubType = null;
									if(ebTypes[0] instanceof ParameterizedType)
										ebSubType = (Class<?>)((ParameterizedType) ebTypes[0]).getRawType();
									else
										ebSubType = (Class<?>) ebTypes[0];
									if(ebSubType.equals(String.class)) {
										writer.writeAttributeName("string[]", ebNamespace + ebField.getName(), false);
										writer.writeAttributeValueStringArray((List<String>)ebField.get(exportBounds));
									}else if(ebSubType.equals(Pair.class)) {
										writer.writeAttributeName("int[]", ebNamespace + ebField.getName(), false);
										List<Pair<Integer, Integer>> ebData = (List<Pair<Integer, Integer>>) ebField.get(exportBounds);
										int[] intData = new int[ebData.size()*2];
										int i = 0;
										for(Pair<Integer, Integer> chunk : ebData) {
											intData[i] = chunk.getKey().intValue();
											intData[i+1] = chunk.getValue().intValue();
											i += 2;
										}
										writer.writeAttributeValueIntArray(intData);
									}else if(ebSubType.equals(ExcludeRegion.class)) {
										writer.writeAttributeName("int[]", ebNamespace + ebField.getName(), false);
										List<ExcludeRegion> ebData = (List<ExcludeRegion>) ebField.get(exportBounds);
										int[] intData = new int[ebData.size()*6];
										int i = 0;
										for(ExcludeRegion excludeRegion : ebData) {
											intData[i] = excludeRegion.minX;
											intData[i+1] = excludeRegion.minY;
											intData[i+2] = excludeRegion.minZ;
											intData[i+3] = excludeRegion.maxX;
											intData[i+4] = excludeRegion.maxY;
											intData[i+5] = excludeRegion.maxZ;
											i += 6;
										}
										writer.writeAttributeValueIntArray(intData);
									}
								}
							}
						}
					}
				}
			}catch(Exception ex) {
				throw new RuntimeException("Could not write ExportData", ex);
			}
		}
		
		writer.endDict();
		writer.endDict();
		
		writer.endMetaData();
	}
	
	private void writeEntities(USDWriter writer, Map<String, Texture> usedTextures) throws IOException {
		String entitiesFilename = dis.readUTF();
		File entitiesFile = new File(inputFile.getParentFile(), entitiesFilename);
		Map<String, Texture> usedTexturesEntities = new HashMap<String, Texture>();
		
		convertEntities(new LargeDataInputStream(new BufferedInputStream(new FileInputStream(entitiesFile))),
						new File(chunksFolder, "entities.usd"), usedTexturesEntities);
		writer.beginDef("Xform", "entities");
		writer.beginMetaData();
		writer.writeMetaDataString("kind", Kind.GROUP.strValue);
		writer.writePayload("./" + chunksFolder.getName() + "/entities.usd", false);
		writer.endMetaData();
		writer.beginChildren();
		writer.beginOver("materials");
		writer.beginChildren();
		for(Entry<String, Texture> entry : usedTexturesEntities.entrySet()) {
			usedTextures.put(entry.getKey(), entry.getValue());
			
			writer.writeAttributeName("rel", entry.getKey(), false);
			writer.writeAttributeValue("</world/materials/" + entry.getKey() + ">");
		}
		writer.endChildren();
		writer.endOver();
		writer.endChildren();
		writer.endDef();
	}
	
	private void convertEntities(LargeDataInputStream dis, File usdFile, Map<String, Texture> usedTextures) throws IOException {
		USDWriter writer = new USDWriter(usdFile);
		Map<MatKey, Materials.MaterialTemplate> templates = new HashMap<MatKey, Materials.MaterialTemplate>();
		
		writer.beginMetaData();
		writer.writeMetaDataString("defaultPrim", "entities");
		writer.endMetaData();
		
		writer.beginDef("Xform", "entities");
		writer.beginMetaData();
		writer.writeMetaDataString("kind", Kind.GROUP.strValue);
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
			writer.writeMetaDataString("kind", Kind.COMPONENT.strValue);
			writer.endMetaData();
			writer.beginChildren();
			
			int numMeshes = dis.readInt();
			for(int meshId = 0; meshId < numMeshes; ++meshId)
				writeSingleMesh(dis, writer, writer, usedTextures, templates, null, "/entities/materials.");
			
			writer.endChildren();
			writer.endClass();
		}
		writer.endChildren();
		writer.endDef();
		
		writer.beginDef("Scope", "materials");
		writer.beginChildren();
		for(Entry<String, Texture> entry : usedTextures.entrySet()) {
			writer.writeAttributeName("rel", entry.getKey(), false);
		}
		writer.endChildren();
		writer.endDef();
		
		int numEntityInstanceTypes = dis.readInt();
		for(int i = 0; i < numEntityInstanceTypes; ++i) {
			String prototypeName = dis.readUTF();
			int numEntityInstances = dis.readInt();
			if(numEntityInstances <= 0)
				continue;
			
			writer.beginDef("Xform", prototypeName);
			writer.beginMetaData();
			writer.writeMetaDataString("kind", Kind.COMPONENT.strValue);
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
	
	private void writeIndividualBlocks(USDWriter writer, Map<String, Texture> usedTextures, long individualBlocksOffset) throws IOException {
		Map<MatKey, Materials.MaterialTemplate> templates = new HashMap<MatKey, Materials.MaterialTemplate>();
		
		dis.skipBytes(individualBlocksOffset - dis.getPosition());
		int numBaseMeshes = dis.readInt();
		if(numBaseMeshes > 0) {
			writer.beginDef("Scope", "individualBlocksBaseMeshes");
			writer.beginChildren();
			
			for(int i = 0; i < numBaseMeshes; ++i) {
				int baseMeshId = dis.readInt();
				int baseMeshX = dis.readInt();
				int baseMeshY = dis.readInt();
				int baseMeshZ = dis.readInt();
				String blockName = dis.readUTF();
				String primName = ("_class_" + Util.makeSafeName(blockName) + baseMeshId + "_" + 
												baseMeshX + "_" + baseMeshY + "_" + baseMeshZ).replace('-', 'N');
				
				if(Config.usePointInstancersForIndividualBlocks)
					writer.beginDef("Xform", primName);
				else
					writer.beginClass("Xform", primName);
				writer.beginMetaData();
				writer.writeMetaDataString("kind", Kind.COMPONENT.strValue);
				writer.endMetaData();
				writer.beginChildren();
				
				int numMeshes = dis.readInt();
				for(int meshId = 0; meshId < numMeshes; ++meshId)
					writeSingleMesh(dis, writer, writer, usedTextures, templates, null, "/world/materials/");
				
				writer.endChildren();
				if(Config.usePointInstancersForIndividualBlocks)
					writer.endDef();
				else
					writer.endClass();
				
				
				individualBlocksRegistry.put(new IndividualBlockId(baseMeshId, baseMeshX, baseMeshY, baseMeshZ, 0), 
												new IndividualBlockInfo(primName));
			}
			writer.endChildren();
			writer.endDef();
		}
	}
	
	private String[] readChunkNames() throws IOException {
		int numChunks = dis.readInt();
		String[] chunkFilenames = new String[numChunks];
		for(int i = 0; i < numChunks; ++i)
			chunkFilenames[i] = dis.readUTF();
		return chunkFilenames;
	}
	
	private void writeChunks(USDWriter writer, Map<String, Texture> usedTextures, String[] chunkFilenames,
						List<ConvertChunkTask> bgChunks, List<ConvertChunkTask> fgChunks) throws IOException {
		int numChunks = chunkFilenames.length;
		
		MCWorldExporter.getApp().getUI().getProgressBar().setNumChunks(numChunks);;
		
		List<Task> futures = new ArrayList<Task>();
		for(int chunkId = 0; chunkId < numChunks; ++chunkId) {
			futures.add(threadPool.submit(new ConvertChunkTask(new File(inputFile.getParentFile(), chunkFilenames[chunkId]), chunksFolder)));
		}
		
		for(Task future : futures) {
			try {
				future.waitUntilTaskIsDone();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(((ConvertChunkTask) future.runnable).isFG)
				fgChunks.add((ConvertChunkTask) future.runnable);
			else
				bgChunks.add((ConvertChunkTask) future.runnable);
			usedTextures.putAll(((ConvertChunkTask) future.runnable).usedTextures);
		}
		Comparator<ConvertChunkTask> chunkInfoComparator = new Comparator<ConvertChunkTask>() {

			@Override
			public int compare(ConvertChunkTask o1, ConvertChunkTask o2) {
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
		bgChunks.sort(chunkInfoComparator);
		fgChunks.sort(chunkInfoComparator);
	}
	
	private static class ConvertChunkTask implements Runnable {

		private File inputFile;
		private File chunksFolder;
		public boolean isFG;
		public String name;
		public Map<String, Texture> usedTextures;
		public Map<IndividualBlockId, List<Float>> instancers;
		public Map<MatKey, MaterialTemplate> templates;
		public List<AnimatedBlock> animatedBlocks;
		public Map<AnimatedBlockId, Map<String, Mesh>> animatedBlockBaseMeshes;
		public Map<AnimatedBlockId, Map<String, String>> animatedBlockMats;
		
		public ConvertChunkTask(File inputFile, File chunksFolder) {
			this.inputFile = inputFile;
			this.chunksFolder = chunksFolder;
			this.isFG = true;
			this.name = "";
			this.usedTextures = new HashMap<String, Texture>();
			this.instancers = new HashMap<IndividualBlockId, List<Float>>();
			this.templates = new HashMap<MatKey, MaterialTemplate>();
			this.animatedBlocks = new ArrayList<AnimatedBlock>();
			this.animatedBlockBaseMeshes = new HashMap<AnimatedBlockId, Map<String, Mesh>>();
			this.animatedBlockMats = new HashMap<AnimatedBlockId, Map<String, String>>();
		}
		
		@Override
		public void run() {
			LargeDataInputStream dis = null;
			try {
				dis = new LargeDataInputStream(new BufferedInputStream(new FileInputStream(inputFile)));
				
				String chunkName = dis.readUTF();
				this.name = chunkName;
				
				boolean isFG = dis.readByte() > 0;
				this.isFG = isFG;
				
				int numAnimatedBlocks = dis.readInt();
				for(int i = 0; i < numAnimatedBlocks; ++i) {
					AnimatedBlock animatedBlock = new AnimatedBlock(null, null, null, null);
					animatedBlock.read(dis);
					animatedBlocks.add(animatedBlock);
				}
				
				USDWriter chunkWriter = new USDWriter(new File(chunksFolder, chunkName + ".usd"));
				USDWriter chunkRenderWriter = new USDWriter(new File(chunksFolder, chunkName + "_render.usd"));
				chunkWriter.beginMetaData();
				chunkWriter.writeMetaDataString("defaultPrim", "chunk");
				chunkWriter.endMetaData();
				
				chunkWriter.beginDef("Xform", "chunk");
				chunkWriter.beginMetaData();
				chunkWriter.writeMetaDataString("kind", Kind.COMPONENT.strValue);
				chunkWriter.endMetaData();
				chunkWriter.beginChildren();
				
				chunkRenderWriter.beginMetaData();
				chunkRenderWriter.writeMetaDataString("defaultPrim", "chunk");
				chunkRenderWriter.endMetaData();
				
				chunkRenderWriter.beginDef("Xform", "chunk");
				chunkRenderWriter.beginMetaData();
				chunkRenderWriter.writeMetaDataString("kind", Kind.COMPONENT.strValue);
				chunkRenderWriter.endMetaData();
				chunkRenderWriter.beginChildren();
				
				
				writeMeshes(dis, chunkWriter, chunkRenderWriter, 
									usedTextures, templates, null, "/chunk/materials.");
				
				int numIndividualBlocks = dis.readInt();
				for(int individualBlockId = 0; individualBlockId < numIndividualBlocks; ++individualBlockId) {
					readIndividualBlock(dis);
				}
				
				// Write animation file first, since it populates animatedBlockBaseMeshes
				writeAnimationFile();
				
				if(Config.usePointInstancersForAnimatedBlocks && Config.useSinglePointInstancer) {
					writeAnimatedBlockReferences(animatedBlocks, chunkWriter, "/chunk/materials.");
					writeAnimatedBlockReferences(animatedBlocks, chunkRenderWriter, "/chunk/materials.");
				}else {
					for(AnimatedBlock animatedBlock : animatedBlocks) {
						writeAnimatedBlockReferences(animatedBlock, chunkWriter, "/chunk/materials.");
						writeAnimatedBlockReferences(animatedBlock, chunkRenderWriter, "/chunk/materials.");
					}
				}
				
				writeMaterialSlots(chunkWriter);
				writeMaterialSlots(chunkRenderWriter);
				
				chunkWriter.endChildren();
				chunkWriter.endDef();
				chunkWriter.close(false);
				
				chunkRenderWriter.endChildren();
				chunkRenderWriter.endDef();
				chunkRenderWriter.close(false);
				
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
		
		private void readIndividualBlock(LargeDataInputStream dis) throws IOException{
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
			
			instancers.put(new IndividualBlockId(blockId, blockX, blockY, blockZ, 0), points);
		}
		
		private void writeAnimatedBlockReferences(AnimatedBlock animatedBlock, USDWriter chunkWriter, String materialsPrim) throws IOException {
			String blockName = animatedBlock.getName() + "_" + 
							Integer.toHexString(animatedBlock.getId().blockId) + "_" + 
							Integer.toHexString(animatedBlock.getId().x) + "_" + 
							Integer.toHexString(animatedBlock.getId().y) + "_" + 
							Integer.toHexString(animatedBlock.getId().z) + "_";
			blockName = Util.makeSafeName(blockName);
			int numBlocks = animatedBlock.getPositions().size()/3;
			
			FloatArray times = new FloatArray();
			
			Map<String, Mesh> baseMeshes = animatedBlockBaseMeshes.get(animatedBlock.getId());
			if(baseMeshes == null)
				return;
			Map<String, String> mats = animatedBlockMats.get(animatedBlock.getId());
			
			
			if(Config.usePointInstancersForAnimatedBlocks) {
				// First we want to get a list of all unique time offsets.
				Set<Integer> timeOffsets = new HashSet<Integer>();
				List<String> prototypes = new ArrayList<String>();
				List<Integer> indices = new ArrayList<Integer>();
				List<Float> positions = new ArrayList<Float>();
				float timeOffsetMergeRadius = Config.animatedBlocksPointInstancerTimeOffsetMergeRadius;
				
				for(int blockIndex = 0; blockIndex < numBlocks; ++blockIndex) {
					float timeOffset = animatedBlock.getTimeOffsets().get(blockIndex);
					float timeOffsetInFrames = timeOffset * Config.animationFrameRate;
					// If we are doing random time offsets, then every block would most likely
					// have their own time offset and we wouldn't get much benefit from
					// using point instancers. So, we want to collapse them.
					int timeOffsetId = (int) Math.floor(timeOffsetInFrames / timeOffsetMergeRadius);
					int prototypeIndex = 0;
					String prototypePath = "Prototypes/" + blockName + "_" + Integer.toString(timeOffsetId);
					if(!timeOffsets.contains(Integer.valueOf(timeOffsetId))) {
						timeOffsets.add(Integer.valueOf(timeOffsetId));
						
						prototypeIndex = prototypes.size();
						prototypes.add(prototypePath);
					}else {
						prototypeIndex = prototypes.indexOf(prototypePath);
					}
					indices.add(Integer.valueOf(prototypeIndex));
					positions.add(animatedBlock.getPositions().get(blockIndex*3));
					positions.add(animatedBlock.getPositions().get(blockIndex*3+1));
					positions.add(animatedBlock.getPositions().get(blockIndex*3+2));
				}
				
				chunkWriter.beginDef("PointInstancer", blockName);
				chunkWriter.beginMetaData();
				chunkWriter.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
				chunkWriter.endMetaData();
				chunkWriter.beginChildren();
				
				chunkWriter.writeAttributeName("rel", "prototypes", false);
				chunkWriter.writeAttributeValuePrimPathArray(prototypes);
				
				chunkWriter.writeAttributeName("point3f[]", "positions", false);
				chunkWriter.writeAttributeValuePoint3fArray(positions, positions.size());
				
				chunkWriter.writeAttributeName("int[]", "protoIndices", false);
				chunkWriter.writeAttributeValueIntArray(indices);
				
				chunkWriter.beginOver("Prototypes");
				chunkWriter.beginChildren();
				for(Integer timeOffsetId : timeOffsets) {
					float timeOffset = (timeOffsetId.intValue() * timeOffsetMergeRadius) / Config.animationFrameRate;
					setupTimes(times, animatedBlock, timeOffset);
					
					writeAnimatedBlockRef(chunkWriter, animatedBlock, blockName, timeOffsetId.intValue(), baseMeshes, mats, 
							materialsPrim, times, false);
				}
				chunkWriter.endChildren();
				chunkWriter.endOver();
				
				chunkWriter.endChildren();
				chunkWriter.endDef();
			}else {
				for(int blockIndex = 0; blockIndex < numBlocks; ++blockIndex) {
					float timeOffset = animatedBlock.getTimeOffsets().get(blockIndex);
					setupTimes(times, animatedBlock, timeOffset);
					
					writeAnimatedBlockRef(chunkWriter, animatedBlock, blockName, blockIndex, baseMeshes, mats, 
							materialsPrim, times, true);
				}
			}
		}
		
		private void writeAnimatedBlockReferences(List<AnimatedBlock> animatedBlocks, USDWriter chunkWriter, String materialsPrim) throws IOException {
			// First we want to get a list of all unique time offsets.
			List<Set<Integer>> timeOffsetsList = new ArrayList<Set<Integer>>();
			List<String> prototypes = new ArrayList<String>();
			List<Integer> indices = new ArrayList<Integer>();
			List<Float> positions = new ArrayList<Float>();
			float timeOffsetMergeRadius = Config.animatedBlocksPointInstancerTimeOffsetMergeRadius;
			
			for(AnimatedBlock animatedBlock : animatedBlocks) {
				String blockName = animatedBlock.getName() + "_" + 
						Integer.toHexString(animatedBlock.getId().blockId) + "_" + 
						Integer.toHexString(animatedBlock.getId().x) + "_" + 
						Integer.toHexString(animatedBlock.getId().y) + "_" + 
						Integer.toHexString(animatedBlock.getId().z) + "_";
				blockName = Util.makeSafeName(blockName);
				int numBlocks = animatedBlock.getPositions().size()/3;
				Set<Integer> timeOffsets = new HashSet<Integer>();
				timeOffsetsList.add(timeOffsets);
				
				Map<String, Mesh> baseMeshes = animatedBlockBaseMeshes.get(animatedBlock.getId());
				if(baseMeshes == null)
					continue;
				
				for(int blockIndex = 0; blockIndex < numBlocks; ++blockIndex) {
					float timeOffset = animatedBlock.getTimeOffsets().get(blockIndex);
					float timeOffsetInFrames = timeOffset * Config.animationFrameRate;
					// If we are doing random time offsets, then every block would most likely
					// have their own time offset and we wouldn't get much benefit from
					// using point instancers. So, we want to collapse them.
					int timeOffsetId = (int) Math.floor(timeOffsetInFrames / timeOffsetMergeRadius);
					int prototypeIndex = 0;
					String prototypePath = "Prototypes/" + blockName + "_" + Integer.toString(timeOffsetId);
					if(!timeOffsets.contains(Integer.valueOf(timeOffsetId))) {
						timeOffsets.add(Integer.valueOf(timeOffsetId));
						
						prototypeIndex = prototypes.size();
						prototypes.add(prototypePath);
					}else {
						prototypeIndex = prototypes.indexOf(prototypePath);
					}
					indices.add(Integer.valueOf(prototypeIndex));
					positions.add(animatedBlock.getPositions().get(blockIndex*3));
					positions.add(animatedBlock.getPositions().get(blockIndex*3+1));
					positions.add(animatedBlock.getPositions().get(blockIndex*3+2));
				}
			}
			
			chunkWriter.beginDef("PointInstancer", "animated_blocks");
			chunkWriter.beginMetaData();
			chunkWriter.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
			chunkWriter.endMetaData();
			chunkWriter.beginChildren();
			
			chunkWriter.writeAttributeName("rel", "prototypes", false);
			chunkWriter.writeAttributeValuePrimPathArray(prototypes);
			
			chunkWriter.writeAttributeName("point3f[]", "positions", false);
			chunkWriter.writeAttributeValuePoint3fArray(positions, positions.size());
			
			chunkWriter.writeAttributeName("int[]", "protoIndices", false);
			chunkWriter.writeAttributeValueIntArray(indices);
			
			chunkWriter.beginOver("Prototypes");
			chunkWriter.beginChildren();
			FloatArray times = new FloatArray();
			for(int i = 0; i < timeOffsetsList.size(); ++i) {
				AnimatedBlock animatedBlock = animatedBlocks.get(i);
				String blockName = animatedBlock.getName() + "_" + 
						Integer.toHexString(animatedBlock.getId().blockId) + "_" + 
						Integer.toHexString(animatedBlock.getId().x) + "_" + 
						Integer.toHexString(animatedBlock.getId().y) + "_" + 
						Integer.toHexString(animatedBlock.getId().z) + "_";
				blockName = Util.makeSafeName(blockName);
				
				Map<String, Mesh> baseMeshes = animatedBlockBaseMeshes.get(animatedBlock.getId());
				if(baseMeshes == null)
					continue;
				Map<String, String> mats = animatedBlockMats.get(animatedBlock.getId());
				
				for(Integer timeOffsetId : timeOffsetsList.get(i)) {
					float timeOffset = (timeOffsetId.intValue() * timeOffsetMergeRadius) / Config.animationFrameRate;
					setupTimes(times, animatedBlock, timeOffset);
					
					writeAnimatedBlockRef(chunkWriter, animatedBlock, blockName, timeOffsetId.intValue(), baseMeshes, mats, 
							materialsPrim, times, false);
				}
			}
			chunkWriter.endChildren();
			chunkWriter.endOver();
			
			chunkWriter.endChildren();
			chunkWriter.endDef();
		}
		
		private void setupTimes(FloatArray times, AnimatedBlock animatedBlock, float timeOffset) {
			float fps = Config.animationFrameRate;
			float stageDuration = (Config.animationEndFrame - Config.animationStartFrame) / fps;
			
			int numLoops = (int) ((stageDuration / animatedBlock.getDuration())+1);
			times.clear();
			for(int loop = 0; loop < numLoops; ++loop) {
				float time = (((float) loop) * animatedBlock.getDuration() - timeOffset) * fps + Config.animationStartFrame;
				time = (float) (Math.floor(time * 100.0) / 100.0);
				times.add(time);
				times.add(0f);
				time = (((float) (loop+1)) * animatedBlock.getDuration() - timeOffset) * fps + Config.animationStartFrame;
				time = (float) (Math.floor(time * 100.0) / 100.0);
				times.add(time);
				times.add(animatedBlock.getDuration() * fps);
			}
		}
		
		private void writeAnimatedBlockRef(USDWriter chunkWriter, AnimatedBlock animatedBlock, String blockName, int blockIndex, 
											Map<String, Mesh> baseMeshes, Map<String, String> mats, String materialsPrim,
											FloatArray times, boolean writeTranslate) throws IOException{
			chunkWriter.beginDef(baseMeshes.size() <= 1 ? "Mesh" : "Xform", blockName + "_" + Integer.toString(blockIndex));
			chunkWriter.beginMetaData();
			
			chunkWriter.writeMetaData("references", "@./" + name + "_anim.topology.usd@</" + blockName + ">");
			
			chunkWriter.writeMetaData("clips");
			chunkWriter.beginDict();
			
			chunkWriter.writeAttributeName("dictionary", "default", false);
			chunkWriter.beginDict();
			
			chunkWriter.writeAttributeName("asset[]", "assetPaths", false);
			chunkWriter.writeAttributeValue("[@./" + name + "_anim.usd@]");
			
			chunkWriter.writeAttributeName("asset", "manifestAssetPath", false);
			chunkWriter.writeAttributeValue("@./" + name + "_anim.manifest.usd@");
			
			chunkWriter.writeAttributeName("double2[]", "active", false);
			chunkWriter.writeAttributeValue("[(0,0)]");
			
			chunkWriter.writeAttributeName("string", "primPath", false);
			chunkWriter.writeAttributeValueString("/" + blockName);
			
			chunkWriter.writeAttributeName("double2[]", "times", false);
			chunkWriter.writeAttributeValuePoint2fArray(times.getData(), times.size());
			
			chunkWriter.endDict();
			
			chunkWriter.endDict();
			
			chunkWriter.writeMetaData("clipSets", "[\"default\"]");
			
			chunkWriter.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
			if(baseMeshes.size() <= 1) {
				chunkWriter.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
			}
			
			chunkWriter.endMetaData();
			
			chunkWriter.beginChildren();
			
			if(writeTranslate) {
				chunkWriter.writeAttributeName("double3", "xformOp:translate", false);
				chunkWriter.writeAttributeValuePoint3f(animatedBlock.getPositions().get(blockIndex*3),
														animatedBlock.getPositions().get(blockIndex*3+1),
														animatedBlock.getPositions().get(blockIndex*3+2));
				chunkWriter.writeAttributeName("token[]", "xformOpOrder", true);
				chunkWriter.writeAttributeValueStringArray(new String[] { "xformOp:translate" });
			}
			
			if(baseMeshes.size() <= 1) {
				for(Entry<String, Mesh> entry : baseMeshes.entrySet()) {
					chunkWriter.writeAttributeName("rel", "material:binding", false);
					chunkWriter.writeAttributeValue("<" + materialsPrim + mats.get(entry.getKey()) + ">");
				}
			}else {
				for(Entry<String, Mesh> entry : baseMeshes.entrySet()) {
					chunkWriter.beginDef("Mesh", Util.makeSafeName(entry.getKey()));
					chunkWriter.beginMetaData();
					chunkWriter.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
					chunkWriter.endMetaData();
					chunkWriter.beginChildren();
					chunkWriter.writeAttributeName("rel", "material:binding", false);
					chunkWriter.writeAttributeValue("<" + materialsPrim + mats.get(entry.getKey()) + ">");
					chunkWriter.endChildren();
					chunkWriter.endDef();
				}
			}
			
			chunkWriter.endChildren();
			
			chunkWriter.endDef();
		}
		
		private void writeMaterialSlots(USDWriter chunkWriter) throws IOException {
			chunkWriter.beginDef("Scope", "materials");
			chunkWriter.beginChildren();
			for(Entry<String, Texture> entry : usedTextures.entrySet()) {
				chunkWriter.writeAttributeName("rel", entry.getKey(), false);
			}
			chunkWriter.endChildren();
			chunkWriter.endDef();
		}
		
		private void writeAnimationFile() throws IOException{
			if(animatedBlocks.isEmpty())
				return;
			USDWriter writer = new USDWriter(new File(chunksFolder, name + "_anim.usd"));
			USDWriter manifestWriter = new USDWriter(new File(chunksFolder, name + "_anim.manifest.usd"));
			USDWriter topologyWriter = new USDWriter(new File(chunksFolder, name + "_anim.topology.usd"));
			
			for(AnimatedBlock animatedBlock : animatedBlocks) {
				writeAnimatedBlock(animatedBlock, writer, manifestWriter, topologyWriter);
				
				String blockName = animatedBlock.getName() + "_" + 
						Integer.toHexString(animatedBlock.getId().blockId) + "_" + 
						Integer.toHexString(animatedBlock.getId().x) + "_" + 
						Integer.toHexString(animatedBlock.getId().y) + "_" + 
						Integer.toHexString(animatedBlock.getId().z) + "_";
				blockName = Util.makeSafeName(blockName);
				Map<String, Mesh> baseMeshes = animatedBlockBaseMeshes.get(animatedBlock.getId());
				if(baseMeshes != null) {
					topologyWriter.beginDef(baseMeshes.size() <= 1 ? "Mesh" : "Xform", blockName);
					topologyWriter.beginChildren();
					if(baseMeshes.size() <= 1) {
						for(Entry<String, Mesh> entry : baseMeshes.entrySet()) {
							writeAnimatedBaseMesh(animatedBlock, entry.getKey(), entry.getValue(), topologyWriter);
						}
					}else {
						for(Entry<String, Mesh> entry : baseMeshes.entrySet()) {
							topologyWriter.beginDef("Mesh", Util.makeSafeName(entry.getKey()));
							topologyWriter.beginMetaData();
							topologyWriter.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
							topologyWriter.endMetaData();
							topologyWriter.beginChildren();
							writeAnimatedBaseMesh(animatedBlock, entry.getKey(), entry.getValue(), topologyWriter);
							topologyWriter.endChildren();
							topologyWriter.endDef();
						}
					}
					
					topologyWriter.endChildren();
					topologyWriter.endDef();
				}
			}
			
			writer.close(false);
			manifestWriter.close(false);
			topologyWriter.close(false);
		}
		
		private void writeAnimatedBlock(AnimatedBlock animatedBlock, USDWriter writer, USDWriter manifestWriter, 
										USDWriter topologyWriter) throws IOException{
			String blockName = animatedBlock.getName() + "_" + 
					Integer.toHexString(animatedBlock.getId().blockId) + "_" + 
					Integer.toHexString(animatedBlock.getId().x) + "_" + 
					Integer.toHexString(animatedBlock.getId().y) + "_" + 
					Integer.toHexString(animatedBlock.getId().z) + "_";
			blockName = Util.makeSafeName(blockName);
			
			float fps = Config.animationFrameRate;
			int numFrames = (int) Math.floor(animatedBlock.getDuration() * fps) + 1;
			Map<String, List<Mesh>> meshes = new HashMap<String, List<Mesh>>();
			Map<String, Mesh> meshesMap = new HashMap<String, Mesh>();
			for(int i = 0; i < numFrames; ++i) {
				meshesMap.clear();
				animatedBlock.getMeshes(((float) i) / fps, meshesMap);
				Set<String> missingMeshes = new HashSet<String>(meshes.keySet());
				for(Entry<String, Mesh> mesh : meshesMap.entrySet()) {
					List<Mesh> frameMeshes = meshes.getOrDefault(mesh.getKey(), null);
					if(frameMeshes == null) {
						frameMeshes = new ArrayList<Mesh>();
						// Newly added, if we're not on frame 0,
						// then this got added mid-animation,
						// and we need to add in some null meshes.
						for(int j = 0; j < i; ++j) {
							frameMeshes.add(null);
						}
						meshes.put(mesh.getKey(), frameMeshes);
					}
					frameMeshes.add(mesh.getValue());
					missingMeshes.remove(mesh.getKey());
				}
				for(String mesh : missingMeshes) {
					List<Mesh> mesh2 = meshes.getOrDefault(mesh, null);
					if(mesh2 != null)
						mesh2.add(null);
				}
			}
			
			if(meshes.size() > 1) {
				writer.beginDef("Xform", blockName);
				writer.beginChildren();
				
				manifestWriter.beginDef("Xform", blockName);
				manifestWriter.beginChildren();
				
				for(Entry<String, List<Mesh>> mesh : meshes.entrySet()) {
					writeAnimatedMesh(animatedBlock, mesh.getKey(), mesh.getValue(), writer, manifestWriter, false);
				}
				
				writer.endChildren();
				manifestWriter.endChildren();
			}else {
				for(Entry<String, List<Mesh>> mesh : meshes.entrySet()) {
					writeAnimatedMesh(animatedBlock, blockName, mesh.getValue(), writer, manifestWriter, true);
				}
			}
		}
		
		private void writeAnimatedBaseMesh(AnimatedBlock block, String meshName, Mesh mesh, 
											USDWriter writer) throws IOException{
			Texture textureObj = new Texture(mesh.getTexture(), mesh.getMatTexture(), mesh.hasColors(), mesh.isDoubleSided(), 
											mesh.getColorSetNames(), templates);
			String matName = MaterialWriter.getMaterialName(textureObj.texture, textureObj.materialTemplate, textureObj.hasBiomeColor);
			usedTextures.put(matName, textureObj);
			
			Map<String, String> mats = animatedBlockMats.getOrDefault(block.getId(), null);
			if(mats == null) {
				mats = new HashMap<String, String>();
				animatedBlockMats.put(block.getId(), mats);
			}
			mats.put(meshName, matName);
			
			if(textureObj.materialTemplate != null) {
				if(textureObj.materialTemplate.networks.size() > 0) {
					MaterialNetwork network = textureObj.materialTemplate.networks.get(0);
					for(ShadingNode node : network.nodes) {
						if(node.type != null && node.type.equals("MESH:Attributes")) {
							// Found such a node, make sure to add its attributes to this.
							for(ShadingAttribute attr : node.attributes) {
								if(attr.value == null)
									continue;
								writer.writeAttributeName(attr.type, attr.name, false);
								writer.writeAttributeValue(String.valueOf(attr.value));
							}
						}
					}
				}
			}
			
			writer.writeAttributeName("bool", "doubleSided", true);
			writer.writeAttributeValueBoolean(mesh.isDoubleSided());
			
			// Renderman doesn't respect the doubleSided attribute
			writer.writeAttributeName("int", "primvars:ri:attributes:Ri:Sides", false);
			writer.writeAttributeValueInt(mesh.isDoubleSided() ? 2 : 1);
			
			writer.writeAttributeName("token", "subdivisionScheme", true);
			writer.writeAttributeValueString("none");
			
			writer.writeAttributeName("point3f[]", "points", false);
			if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints())
				writer.writeAttributeValue("[]");
			else 
				writer.writeAttributeValuePoint3fArray(mesh.getVertices().getData(), mesh.getVertices().size());
			
			writer.writeAttributeName("int[]", "faceVertexIndices", false);
			if(mesh.isAnimatesTopology())
				writer.writeAttributeValue("[]");
			else 
				writer.writeAttributeValueIntArray(mesh.getFaceIndices().getData(), mesh.getFaceIndices().size());
			
			writer.writeAttributeName("int[]", "faceVertexCounts", false);
			if(mesh.isAnimatesTopology())
				writer.writeAttributeValue("[]");
			else 
				writer.writeAttributeValueIntArray(mesh.getFaceCounts().getData(), mesh.getFaceCounts().size());
			
			
			if(Config.useIndexedUVs) {
				writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs())
					writer.writeAttributeValue("[]");
				else 
					writer.writeAttributeValuePoint2fArray(mesh.getUs().getData(), mesh.getVs().getData(), mesh.getUs().size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:st:indices", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs())
					writer.writeAttributeValue("[]");
				else 
					writer.writeAttributeValueIntArray(mesh.getUvIndices().getData(), mesh.getUvIndices().size());
			}else {
				writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs()) {
					writer.writeAttributeValue("[]");
				}else {
					FloatArray flatUs = new FloatArray();
					FloatArray flatVs = new FloatArray();
					mesh.getFlatUVs(flatUs, flatVs);
					writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size());
				}
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
			}
			
			
			if(Config.calculateCornerUVs) {
				if(Config.useIndexedUVs) {
					writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
					if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs())
						writer.writeAttributeValue("[]");
					else 
						writer.writeAttributeValuePoint2fArray(mesh.getCornerUVs().getData(), mesh.getCornerUVs().size());
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
					
					writer.writeAttributeName("int[]", "primvars:uvCornerST:indices", false);
					if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs())
						writer.writeAttributeValue("[]");
					else 
						writer.writeAttributeValueIntArray(mesh.getCornerUVIndices().getData(), mesh.getCornerUVIndices().size());
					writer.endTimeSamples();
				}else {
					writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
					if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs()) {
						writer.writeAttributeValue("[]");
					} else {
						FloatArray flatUs = new FloatArray();
						FloatArray flatVs = new FloatArray();
						mesh.getFlatCornerUVs(flatUs, flatVs);
						writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size());
					}
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
				}
			}
			
			if(Config.useIndexedNormals) {
				writer.writeAttributeName("normal3f[]", "primvars:normals", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints())
					writer.writeAttributeValue("[]");
				else 
					writer.writeAttributeValuePoint3fArray(mesh.getNormals().getData(), mesh.getNormals().size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:normals:indices", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints())
					writer.writeAttributeValue("[]");
				else 
					writer.writeAttributeValueIntArray(mesh.getNormalIndices().getData(), mesh.getNormalIndices().size());
			}else {
				writer.writeAttributeName("normal3f[]", "primvars:normals", false);
				if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints()) {
					writer.writeAttributeValue("[]");
				}else {
					FloatArray flatNormals = new FloatArray();
					mesh.getFlatNormals(flatNormals);
					writer.writeAttributeValuePoint3fArray(flatNormals.getData(), flatNormals.size());
				}
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
			}
			
			// If we're exporting vertex colours as display colour but exporting
			// display colour is turned off, then don't export out vertex colours.
			if(mesh.hasColors() && !(Config.exportVertexColorAsDisplayColor && !Config.exportDisplayColor)) {
				if(Config.useIndexedVertexColors) {
					if(Config.exportVertexColorAsDisplayColor) {
						writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
					}else {
						writer.writeAttributeName("color3f[]", "primvars:Cd", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors())
						writer.writeAttributeValue("[]");
					else
						writer.writeAttributeValuePoint3fArray(mesh.getColors().getValues().getData(), mesh.getColors().getValues().size());
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
					
					if(Config.exportVertexColorAsDisplayColor) {
						writer.writeAttributeName("int[]", "primvars:displayColor:indices", false);
					}else {
						writer.writeAttributeName("int[]", "primvars:Cd:indices", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors())
						writer.writeAttributeValue("[]");
					else
						writer.writeAttributeValueIntArray(mesh.getColors().getIndices().getData(), mesh.getColors().getIndices().size());
				}else {
					if(Config.exportVertexColorAsDisplayColor) {
						writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
					}else {
						writer.writeAttributeName("color3f[]", "primvars:Cd", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors()) {
						writer.writeAttributeValue("[]");
					} else {
						FloatArray flatColors = mesh.getColors().getFlatValues();
						writer.writeAttributeValuePoint3fArray(flatColors.getData(), flatColors.size());
					}
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
				}
			}
			if(Config.exportDisplayColor && !Config.exportVertexColorAsDisplayColor) {
				// If we were exporting vertex colours as display colours,
				// then the if statement above handles exporting out display colours,
				// so we'd need to not run this bit of code.
				Color blockColor = new Color(ResourcePacks.getDefaultColour(mesh.getTexture()));
				if(mesh.hasColors()) {
					// Add in biome colours
					blockColor.mult(new Color(mesh.getColors().getR(0), mesh.getColors().getG(0), mesh.getColors().getB(0)));
				}
				writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
				writer.writeAttributeValuePoint3fArray(new float[] {blockColor.getR(), blockColor.getG(), blockColor.getB()});
			}
			
			
			if(mesh.hasAO()) {
				if(Config.useIndexedVertexColors) {
					if(Config.exportAmbientOcclusionAsDisplayOpacity) {
						writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
					}else {
						writer.writeAttributeName("float[]", "primvars:CdAO", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints())
						writer.writeAttributeValue("[]");
					else 
						writer.writeAttributeValueFloatArray(mesh.getAO().getValues().getData(), mesh.getAO().getValues().size());
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
					
					if(Config.exportAmbientOcclusionAsDisplayOpacity) {
						writer.writeAttributeName("int[]", "primvars:displayOpacity:indices", false);
					}else {
						writer.writeAttributeName("int[]", "primvars:CdAO:indices", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints())
						writer.writeAttributeValue("[]");
					else 
						writer.writeAttributeValueIntArray(mesh.getAO().getIndices().getData(), mesh.getAO().getIndices().size());
				}else {
					if(Config.exportAmbientOcclusionAsDisplayOpacity) {
						writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
					}else {
						writer.writeAttributeName("float[]", "primvars:CdAO", false);
					}
					if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints()) {
						writer.writeAttributeValue("[]");
					}else {
						FloatArray flatValues = mesh.getAO().getFlatValues();
						writer.writeAttributeValueFloatArray(flatValues.getData(), flatValues.size());
					}
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
				}
			}
			
			if(mesh.getAdditionalColorSets() != null) {
				for(VertexColorSet colorSet : mesh.getAdditionalColorSets()) {
					String typeName = "float[]";
					if(colorSet.getComponentCount() == 2)
						typeName = "float2[]";
					else if(colorSet.getComponentCount() == 3)
						typeName = "color3f[]";
					else if(colorSet.getComponentCount() == 4)
						typeName = "color4f[]";
					if(Config.useIndexedVertexColors) {
						writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
						if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors())
							writer.writeAttributeValue("[]");
						else 
							writer.writeAttributeValuePointNfArray(colorSet.getValues().getData(), colorSet.getValues().size(), colorSet.getComponentCount());
						writer.beginMetaData();
						writer.writeMetaData("interpolation", "\"faceVarying\"");
						writer.endMetaData();
						
						writer.writeAttributeName("int[]", "primvars:" + colorSet.getName() + ":indices", false);
						if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors())
							writer.writeAttributeValue("[]");
						else 
							writer.writeAttributeValueIntArray(colorSet.getIndices().getData(), colorSet.getIndices().size());
					}else {
						writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
						if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors()) {
							writer.writeAttributeValue("[]");
						}else {
							FloatArray flatValues = colorSet.getFlatValues();
							writer.writeAttributeValuePointNfArray(flatValues.getData(), flatValues.size(), colorSet.getComponentCount());
						}
						writer.beginMetaData();
						writer.writeMetaData("interpolation", "\"faceVarying\"");
						writer.endMetaData();
					}
				}
			}
		}
		
		private void writeAnimatedMesh(AnimatedBlock block, String meshName, List<Mesh> meshes, 
										USDWriter writer, USDWriter manifestWriter, boolean singleMesh) throws IOException{
			Mesh mesh = meshes.get(0);
			if(mesh == null) {
				for(Mesh mesh2 : meshes) {
					if(mesh2 != null) {
						mesh = mesh2;
						break;
					}
				}
			}
			if(mesh.getTexture().startsWith("banner:")) {
				String bannerTexName = mesh.getTexture().replace(':', '/');
				try {
					String texture = BannerTextureCreator.createBannerTexture(mesh.getExtraData(), bannerTexName);
					mesh.setTexture(texture, false);
					mesh.setMatTexture(texture);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			Map<String, Mesh> baseMeshes = animatedBlockBaseMeshes.getOrDefault(block.getId(), null);
			if(baseMeshes == null) {
				baseMeshes = new HashMap<String, Mesh>();
				animatedBlockBaseMeshes.put(block.getId(), baseMeshes);
			}
			baseMeshes.put(meshName, mesh);
			
			if(!singleMesh && !(mesh.isAnimatesTopology() || mesh.isAnimatesPoints() || mesh.isAnimatesUVs() || mesh.isAnimatesVertexColors()))
				// If this mesh doesn't contain any animation, then no need to animate it.
				return;
			
			writer.beginDef("Mesh", Util.makeSafeName(meshName));
			writer.beginChildren();
			
			manifestWriter.beginDef("Mesh", Util.makeSafeName(meshName));
			manifestWriter.beginChildren();
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints()) {
				writer.writeAttributeName("point3f[]", "points", false);
				manifestWriter.writeAttributeName("point3f[]", "points", false);
				writer.beginTimeSamples();
				for(int i = 0; i < meshes.size(); ++i) {
					Mesh mesh2 = meshes.get(i);
					writer.writeTimeSampleTime((float) i);
					if(mesh2 != null)
						writer.writeAttributeValuePoint3fArray(mesh2.getVertices().getData(), mesh2.getVertices().size(), true);
					else
						writer.writeAttributeValue("[]", true);
				}
				writer.endTimeSamples();
			}
			
			if(mesh.isAnimatesTopology()) {
				writer.writeAttributeName("int[]", "faceVertexIndices", false);
				manifestWriter.writeAttributeName("int[]", "faceVertexIndices", false);
				writer.beginTimeSamples();
				for(int i = 0; i < meshes.size(); ++i) {
					Mesh mesh2 = meshes.get(i);
					writer.writeTimeSampleTime((float) i);
					if(mesh2 != null)
						writer.writeAttributeValueIntArray(mesh2.getFaceIndices().getData(), mesh2.getFaceIndices().size(), true);
					else
						writer.writeAttributeValue("[]", true);
				}
				writer.endTimeSamples();
				
				writer.writeAttributeName("int[]", "faceVertexCounts", false);
				manifestWriter.writeAttributeName("int[]", "faceVertexCounts", false);
				writer.beginTimeSamples();
				for(int i = 0; i < meshes.size(); ++i) {
					Mesh mesh2 = meshes.get(i);
					writer.writeTimeSampleTime((float) i);
					if(mesh2 != null)
						writer.writeAttributeValueIntArray(mesh2.getFaceCounts().getData(), mesh2.getFaceCounts().size(), true);
					else
						writer.writeAttributeValue("[]", true);
				}
				writer.endTimeSamples();
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs()) {
				if(Config.useIndexedUVs) {
					writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
					manifestWriter.writeAttributeName("texCoord2f[]", "primvars:st", false);
					writer.beginTimeSamples();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null)
							writer.writeAttributeValuePoint2fArray(mesh2.getUs().getData(), mesh2.getVs().getData(), mesh2.getUs().size(), true);
						else
							writer.writeAttributeValue("[]", true);
					}
					writer.endTimeSamples();
					
					writer.writeAttributeName("int[]", "primvars:st:indices", false);
					manifestWriter.writeAttributeName("int[]", "primvars:st:indices", false);
					writer.beginTimeSamples();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null)
							writer.writeAttributeValueIntArray(mesh2.getUvIndices().getData(), mesh2.getUvIndices().size(), true);
						else
							writer.writeAttributeValue("[]", true);
					}
					writer.endTimeSamples();
				}else {
					writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
					manifestWriter.writeAttributeName("texCoord2f[]", "primvars:st", false);
					writer.beginTimeSamples();
					FloatArray flatUs = new FloatArray();
					FloatArray flatVs = new FloatArray();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null) {
							flatUs.clear();
							flatVs.clear();
							mesh2.getFlatUVs(flatUs, flatVs);
							writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size(), true);
						}else {
							writer.writeAttributeValue("[]", true);
						}
					}
					writer.endTimeSamples();
				}
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesUVs()) {
				if(Config.calculateCornerUVs) {
					if(Config.useIndexedUVs) {
						writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
						manifestWriter.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValuePoint2fArray(mesh2.getCornerUVs().getData(), mesh2.getCornerUVs().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
						
						writer.writeAttributeName("int[]", "primvars:uvCornerST:indices", false);
						manifestWriter.writeAttributeName("int[]", "primvars:uvCornerST:indices", false);
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValueIntArray(mesh2.getCornerUVIndices().getData(), mesh2.getCornerUVIndices().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}else {
						FloatArray flatUs = new FloatArray();
						FloatArray flatVs = new FloatArray();
						
						writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
						manifestWriter.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null) {
								flatUs.clear();
								flatVs.clear();
								mesh2.getFlatCornerUVs(flatUs, flatVs);
								writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size(), true);
							}else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}
				}
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints()) {
				if(Config.useIndexedNormals) {
					writer.writeAttributeName("normal3f[]", "primvars:normals", false);
					manifestWriter.writeAttributeName("normal3f[]", "primvars:normals", false);
					writer.beginTimeSamples();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null)
							writer.writeAttributeValuePoint3fArray(mesh2.getNormals().getData(), mesh2.getNormals().size(), true);
						else
							writer.writeAttributeValue("[]", true);
					}
					writer.endTimeSamples();
					
					writer.writeAttributeName("int[]", "primvars:normals:indices", false);
					manifestWriter.writeAttributeName("int[]", "primvars:normals:indices", false);
					writer.beginTimeSamples();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null)
							writer.writeAttributeValueIntArray(mesh2.getNormalIndices().getData(), mesh2.getNormalIndices().size(), true);
						else
							writer.writeAttributeValue("[]", true);
					}
					writer.endTimeSamples();
				}else {
					FloatArray flatNormals = new FloatArray();
					
					writer.writeAttributeName("normal3f[]", "primvars:normals", false);
					manifestWriter.writeAttributeName("normal3f[]", "primvars:normals", false);
					writer.beginTimeSamples();
					for(int i = 0; i < meshes.size(); ++i) {
						Mesh mesh2 = meshes.get(i);
						writer.writeTimeSampleTime((float) i);
						if(mesh2 != null) {
							flatNormals.clear();
							mesh2.getFlatNormals(flatNormals);
							writer.writeAttributeValuePoint3fArray(flatNormals.getData(), flatNormals.size(), true);
						}else
							writer.writeAttributeValue("[]", true);
					}
					writer.endTimeSamples();
				}
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors()) {
				// If we're exporting vertex colours as display colour but exporting
				// display colour is turned off, then don't export out vertex colours.
				if(mesh.hasColors() && !(Config.exportVertexColorAsDisplayColor && !Config.exportDisplayColor)) {
					if(Config.useIndexedVertexColors) {
						if(Config.exportVertexColorAsDisplayColor) {
							writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
							manifestWriter.writeAttributeName("color3f[]", "primvars:displayColor", false);
						}else {
							writer.writeAttributeName("color3f[]", "primvars:Cd", false);
							manifestWriter.writeAttributeName("color3f[]", "primvars:Cd", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValuePoint3fArray(mesh2.getColors().getValues().getData(), mesh2.getColors().getValues().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
						
						if(Config.exportVertexColorAsDisplayColor) {
							writer.writeAttributeName("int[]", "primvars:displayColor:indices", false);
							manifestWriter.writeAttributeName("int[]", "primvars:displayColor:indices", false);
						}else {
							writer.writeAttributeName("int[]", "primvars:Cd:indices", false);
							manifestWriter.writeAttributeName("int[]", "primvars:Cd:indices", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValueIntArray(mesh2.getColors().getIndices().getData(), mesh2.getColors().getIndices().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}else {
						if(Config.exportVertexColorAsDisplayColor) {
							writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
							manifestWriter.writeAttributeName("color3f[]", "primvars:displayColor", false);
						}else {
							writer.writeAttributeName("color3f[]", "primvars:Cd", false);
							manifestWriter.writeAttributeName("color3f[]", "primvars:Cd", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null) {
								FloatArray flatColors = mesh2.getColors().getFlatValues();
								writer.writeAttributeValuePoint3fArray(flatColors.getData(), flatColors.size(), true);
							}else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}
				}
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesPoints()) {
				if(mesh.hasAO()) {
					if(Config.useIndexedVertexColors) {
						if(Config.exportAmbientOcclusionAsDisplayOpacity) {
							writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
							manifestWriter.writeAttributeName("float[]", "primvars:displayOpacity", false);
						}else {
							writer.writeAttributeName("float[]", "primvars:CdAO", false);
							manifestWriter.writeAttributeName("float[]", "primvars:CdAO", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValueFloatArray(mesh2.getAO().getValues().getData(), mesh2.getAO().getValues().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
						
						if(Config.exportAmbientOcclusionAsDisplayOpacity) {
							writer.writeAttributeName("int[]", "primvars:displayOpacity:indices", false);
							manifestWriter.writeAttributeName("int[]", "primvars:displayOpacity:indices", false);
						}else {
							writer.writeAttributeName("int[]", "primvars:CdAO:indices", false);
							manifestWriter.writeAttributeName("int[]", "primvars:CdAO:indices", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null)
								writer.writeAttributeValueIntArray(mesh2.getAO().getIndices().getData(), mesh2.getAO().getIndices().size(), true);
							else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}else {
						if(Config.exportAmbientOcclusionAsDisplayOpacity) {
							writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
							manifestWriter.writeAttributeName("float[]", "primvars:displayOpacity", false);
						}else {
							writer.writeAttributeName("float[]", "primvars:CdAO", false);
							manifestWriter.writeAttributeName("float[]", "primvars:CdAO", false);
						}
						writer.beginTimeSamples();
						for(int i = 0; i < meshes.size(); ++i) {
							Mesh mesh2 = meshes.get(i);
							writer.writeTimeSampleTime((float) i);
							if(mesh2 != null) {
								FloatArray flatValues = mesh2.getAO().getFlatValues();
								writer.writeAttributeValueFloatArray(flatValues.getData(), flatValues.size(), true);
							}else
								writer.writeAttributeValue("[]", true);
						}
						writer.endTimeSamples();
					}
				}
			}
			
			if(mesh.isAnimatesTopology() || mesh.isAnimatesVertexColors()) {
				if(mesh.getAdditionalColorSets() != null) {
					for(VertexColorSet colorSet : mesh.getAdditionalColorSets()) {
						String typeName = "float[]";
						if(colorSet.getComponentCount() == 2)
							typeName = "float2[]";
						else if(colorSet.getComponentCount() == 3)
							typeName = "color3f[]";
						else if(colorSet.getComponentCount() == 4)
							typeName = "color4f[]";
						if(Config.useIndexedVertexColors) {
							writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
							manifestWriter.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
							writer.beginTimeSamples();
							for(int i = 0; i < meshes.size(); ++i) {
								Mesh mesh2 = meshes.get(i);
								VertexColorSet colorSet2 = null;
								if(mesh2 != null)
									colorSet2 = mesh2.getAdditionalColorSet(colorSet.getName());
								writer.writeTimeSampleTime((float) i);
								if(colorSet2 != null)
									writer.writeAttributeValuePointNfArray(colorSet2.getValues().getData(), colorSet2.getValues().size(), colorSet2.getComponentCount(), true);
								else
									writer.writeAttributeValue("[]", true);
							}
							writer.endTimeSamples();
							
							writer.writeAttributeName("int[]", "primvars:" + colorSet.getName() + ":indices", false);
							manifestWriter.writeAttributeName("int[]", "primvars:" + colorSet.getName() + ":indices", false);
							writer.beginTimeSamples();
							for(int i = 0; i < meshes.size(); ++i) {
								Mesh mesh2 = meshes.get(i);
								VertexColorSet colorSet2 = null;
								if(mesh2 != null)
									colorSet2 = mesh2.getAdditionalColorSet(colorSet.getName());
								writer.writeTimeSampleTime((float) i);
								if(colorSet2 != null)
									writer.writeAttributeValueIntArray(colorSet2.getIndices().getData(), colorSet2.getIndices().size());
								else
									writer.writeAttributeValue("[]", true);
							}
							writer.endTimeSamples();
						}else {
							writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
							manifestWriter.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
							writer.beginTimeSamples();
							for(int i = 0; i < meshes.size(); ++i) {
								Mesh mesh2 = meshes.get(i);
								VertexColorSet colorSet2 = null;
								if(mesh2 != null)
									colorSet2 = mesh2.getAdditionalColorSet(colorSet.getName());
								writer.writeTimeSampleTime((float) i);
								if(colorSet2 != null) {
									FloatArray flatValues = colorSet.getFlatValues();
									writer.writeAttributeValuePointNfArray(flatValues.getData(), flatValues.size(), colorSet.getComponentCount(), true);
								}else
									writer.writeAttributeValue("[]", true);
							}
							writer.endTimeSamples();
						}
					}
				}
			}
			
			writer.endChildren();
			writer.endDef();
			manifestWriter.endChildren();
			manifestWriter.endDef();
		}
		
	}
	
	private static void writeMeshes(LargeDataInputStream dis, USDWriter proxyWriter, 
									USDWriter renderWriter, Map<String, Texture> usedTextures, 
									Map<MatKey, MaterialTemplate> templates, Kind kind, 
									String materialsPrim) throws IOException {
		while(true) {
			boolean hasNext = writeSingleMesh(dis, proxyWriter, renderWriter, usedTextures, templates, kind, materialsPrim);
			if(!hasNext)
				break;
		}
	}
	
	private static boolean writeSingleMesh(LargeDataInputStream dis, USDWriter proxyWriter, 
				USDWriter renderWriter, Map<String, Texture> usedTextures, 
				Map<MatKey, MaterialTemplate> templates, Kind kind, 
				String materialsPrim) throws IOException {
		Mesh mesh = readMesh(dis);
		if(mesh == null)
			return false;
		
		if(mesh instanceof MeshGroup) {
			// Write out the group to the proxy file.
			writeGroup((MeshGroup) mesh, proxyWriter, MeshPurpose.PROXY, usedTextures, templates, kind, materialsPrim);
			
			if(renderWriter != proxyWriter) {
				// We're using a separate render writer, so we want to write it out
				// to the render writer as well.
				writeGroup((MeshGroup) mesh, renderWriter, MeshPurpose.RENDER, usedTextures, templates, kind, materialsPrim);
			}
		}else {
			// It's a normal mesh.
			// Write out the mesh to the render file.
			writeMesh(mesh, proxyWriter, MeshPurpose.PROXY, usedTextures, templates, kind, materialsPrim);
			
			if(renderWriter != proxyWriter) {
				// We're using a separate render writer, so we want to write it out
				// to the render writer as well.
				writeMesh(mesh, renderWriter, MeshPurpose.RENDER, usedTextures, templates, kind, materialsPrim);
			}
		}
		return true;
	}
	
	
	
	private static void writeMesh(Mesh mesh, USDWriter writer, MeshPurpose purpose,
									Map<String, Texture> usedTextures, Map<MatKey, MaterialTemplate> templates,
									Kind kind, String materialsPrim) throws IOException{
		mesh.validateSubsets();
		String meshName = mesh.getName();
		if(meshName == "")
			meshName = "mesh";
		meshName = Util.makeSafeName(meshName);
		
		if(purpose == MeshPurpose.RENDER && mesh.getPurpose() == MeshPurpose.PROXY) {
			// We are doing the render purpose, but this mesh is a proxy mesh.
			// So, let's tag it as proxy.
			writer.beginOver(meshName);
			writer.beginChildren();
			writer.writeAttributeName("token", "purpose", true);
			writer.writeAttributeValueString("proxy");
			writer.endChildren();
			writer.endOver();
			return;
		}
		if(mesh.getPurpose() != purpose && mesh.getPurpose() != MeshPurpose.UNDEFINED) {
			return;
		}
		
		if(purpose == MeshPurpose.RENDER && mesh.getPurpose() == MeshPurpose.UNDEFINED) {
			// We are doing the render purpose and this mesh is undefined, which means
			// that it's already specified in the proxy file.
			// So we only need to write something if this mesh contains a subset with
			// render purpose.
			if(mesh.hasPurpose(MeshPurpose.RENDER)) {
				writer.beginOver(meshName);
				writer.beginChildren();
				writeMeshSubsets(writer, mesh, purpose, usedTextures, materialsPrim, templates);
				writer.endChildren();
				writer.endOver();
			}
			return;
		}
		
		if(mesh.getTexture().startsWith("banner:")) {
			String bannerTexName = mesh.getTexture().replace(':', '/');
			try {
				String texture = BannerTextureCreator.createBannerTexture(mesh.getExtraData(), bannerTexName);
				mesh.setTexture(texture, false);
				mesh.setMatTexture(texture);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		Texture textureObj = new Texture(mesh.getTexture(), mesh.getMatTexture(), mesh.hasColors(), mesh.isDoubleSided(), 
										mesh.getColorSetNames(), templates);
		String matName = MaterialWriter.getMaterialName(textureObj.texture, textureObj.materialTemplate, textureObj.hasBiomeColor);
		usedTextures.put(matName, textureObj);
		
		writer.beginDef("Mesh", meshName);
		writer.beginMetaData();
		writer.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
		if(kind != null)
			writer.writeMetaDataString("kind", kind.strValue);
		writer.endMetaData();
		writer.beginChildren();
		
		if(mesh.getPurpose() == MeshPurpose.RENDER) {
			writer.writeAttributeName("token", "purpose", true);
			writer.writeAttributeValueString("render");
		}
		
		// Check if the materialTemplate has a MESH:Attributes node,
		// if so, then that contains attributes to set on this.
		if(textureObj.materialTemplate != null) {
			if(textureObj.materialTemplate.networks.size() > 0) {
				MaterialNetwork network = textureObj.materialTemplate.networks.get(0);
				for(ShadingNode node : network.nodes) {
					if(node.type != null && node.type.equals("MESH:Attributes")) {
						// Found such a node, make sure to add its attributes to this.
						for(ShadingAttribute attr : node.attributes) {
							if(attr.value == null)
								continue;
							writer.writeAttributeName(attr.type, attr.name, false);
							writer.writeAttributeValue(String.valueOf(attr.value));
						}
					}
				}
			}
		}
		
		writer.writeAttributeName("bool", "doubleSided", true);
		writer.writeAttributeValueBoolean(mesh.isDoubleSided());
		
		// Renderman doesn't respect the doubleSided attribute
		writer.writeAttributeName("int", "primvars:ri:attributes:Ri:Sides", false);
		writer.writeAttributeValueInt(mesh.isDoubleSided() ? 2 : 1);
		
		writer.writeAttributeName("token", "subdivisionScheme", true);
		writer.writeAttributeValueString("none");
		
		writer.writeAttributeName("point3f[]", "points", false);
		writer.writeAttributeValuePoint3fArray(mesh.getVertices().getData(), mesh.getVertices().size());
		
		writer.writeAttributeName("int[]", "faceVertexIndices", false);
		writer.writeAttributeValueIntArray(mesh.getFaceIndices().getData(), mesh.getFaceIndices().size());
		
		writer.writeAttributeName("int[]", "faceVertexCounts", false);
		writer.writeAttributeValueIntArray(mesh.getFaceCounts().getData(), mesh.getFaceCounts().size());
		
		if(Config.useIndexedUVs) {
			writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
			writer.writeAttributeValuePoint2fArray(mesh.getUs().getData(), mesh.getVs().getData(), mesh.getUs().size());
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
			
			writer.writeAttributeName("int[]", "primvars:st:indices", false);
			writer.writeAttributeValueIntArray(mesh.getUvIndices().getData(), mesh.getUvIndices().size());
		}else {
			FloatArray flatUs = new FloatArray();
			FloatArray flatVs = new FloatArray();
			mesh.getFlatUVs(flatUs, flatVs);
			writer.writeAttributeName("texCoord2f[]", "primvars:st", false);
			writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size());
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
		}
		
		if(Config.calculateCornerUVs) {
			if(Config.useIndexedUVs) {
				writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
				writer.writeAttributeValuePoint2fArray(mesh.getCornerUVs().getData(), mesh.getCornerUVs().size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				writer.writeAttributeName("int[]", "primvars:uvCornerST:indices", false);
				writer.writeAttributeValueIntArray(mesh.getCornerUVIndices().getData(), mesh.getCornerUVIndices().size());
			}else {
				FloatArray flatUs = new FloatArray();
				FloatArray flatVs = new FloatArray();
				mesh.getFlatCornerUVs(flatUs, flatVs);
				
				writer.writeAttributeName("texCoord2f[]", "primvars:uvCornerST", false);
				writer.writeAttributeValuePoint2fArray(flatUs.getData(), flatVs.getData(), flatUs.size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
			}
		}
		
		if(Config.useIndexedNormals) {
			writer.writeAttributeName("normal3f[]", "primvars:normals", false);
			writer.writeAttributeValuePoint3fArray(mesh.getNormals().getData(), mesh.getNormals().size());
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
			
			writer.writeAttributeName("int[]", "primvars:normals:indices", false);
			writer.writeAttributeValueIntArray(mesh.getNormalIndices().getData(), mesh.getNormalIndices().size());
		}else {
			FloatArray flatNormals = new FloatArray();
			mesh.getFlatNormals(flatNormals);
			
			writer.writeAttributeName("normal3f[]", "primvars:normals", false);
			writer.writeAttributeValuePoint3fArray(flatNormals.getData(), flatNormals.size());
			writer.beginMetaData();
			writer.writeMetaData("interpolation", "\"faceVarying\"");
			writer.endMetaData();
		}
		
		// If we're exporting vertex colours as display colour but exporting
		// display colour is turned off, then don't export out vertex colours.
		if(mesh.hasColors() && !(Config.exportVertexColorAsDisplayColor && !Config.exportDisplayColor)) {
			if(Config.useIndexedVertexColors) {
				if(Config.exportVertexColorAsDisplayColor)
					writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
				else
					writer.writeAttributeName("color3f[]", "primvars:Cd", false);
				writer.writeAttributeValuePoint3fArray(mesh.getColors().getValues().getData(), mesh.getColors().getValues().size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				if(Config.exportVertexColorAsDisplayColor)
					writer.writeAttributeName("int[]", "primvars:displayColor:indices", false);
				else
					writer.writeAttributeName("int[]", "primvars:Cd:indices", false);
				writer.writeAttributeValueIntArray(mesh.getColors().getIndices().getData(), mesh.getColors().getIndices().size());
			}else {
				FloatArray flatColors = mesh.getColors().getFlatValues();
				
				if(Config.exportVertexColorAsDisplayColor)
					writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
				else
					writer.writeAttributeName("color3f[]", "primvars:Cd", false);
				writer.writeAttributeValuePoint3fArray(flatColors.getData(), flatColors.size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
			}
		}
		if(Config.exportDisplayColor && !Config.exportVertexColorAsDisplayColor) {
			// If we were exporting vertex colours as display colours,
			// then the if statement above handles exporting out display colours,
			// so we'd need to not run this bit of code.
			Color blockColor = new Color(ResourcePacks.getDefaultColour(mesh.getTexture()));
			if(mesh.hasColors()) {
				// Add in biome colours
				blockColor.mult(new Color(mesh.getColors().getR(0), mesh.getColors().getG(0), mesh.getColors().getB(0)));
			}
			writer.writeAttributeName("color3f[]", "primvars:displayColor", false);
			writer.writeAttributeValuePoint3fArray(new float[] {blockColor.getR(), blockColor.getG(), blockColor.getB()});
		}
		
		if(mesh.hasAO()) {
			if(Config.useIndexedVertexColors) {
				if(Config.exportAmbientOcclusionAsDisplayOpacity)
					writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
				else
					writer.writeAttributeName("float[]", "primvars:CdAO", false);
				writer.writeAttributeValueFloatArray(mesh.getAO().getValues().getData(), mesh.getAO().getValues().size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
				
				if(Config.exportAmbientOcclusionAsDisplayOpacity)
					writer.writeAttributeName("int[]", "primvars:displayOpacity:indices", false);
				else
					writer.writeAttributeName("int[]", "primvars:CdAO:indices", false);
				writer.writeAttributeValueIntArray(mesh.getAO().getIndices().getData(), mesh.getAO().getIndices().size());
			}else {
				FloatArray flatValues = mesh.getAO().getFlatValues();
				
				if(Config.exportAmbientOcclusionAsDisplayOpacity)
					writer.writeAttributeName("float[]", "primvars:displayOpacity", false);
				else
					writer.writeAttributeName("float[]", "primvars:CdAO", false);
				writer.writeAttributeValueFloatArray(flatValues.getData(), flatValues.size());
				writer.beginMetaData();
				writer.writeMetaData("interpolation", "\"faceVarying\"");
				writer.endMetaData();
			}
		}
		
		if(mesh.getAdditionalColorSets() != null) {
			for(VertexColorSet colorSet : mesh.getAdditionalColorSets()) {
				String typeName = "float[]";
				if(colorSet.getComponentCount() == 2)
					typeName = "float2[]";
				else if(colorSet.getComponentCount() == 3)
					typeName = "color3f[]";
				else if(colorSet.getComponentCount() == 4)
					typeName = "color4f[]";
				if(Config.useIndexedVertexColors) {
					writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
					writer.writeAttributeValuePointNfArray(colorSet.getValues().getData(), colorSet.getValues().size(), colorSet.getComponentCount());
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
					
					writer.writeAttributeName("int[]", "primvars:" + colorSet.getName() + ":indices", false);
					writer.writeAttributeValueIntArray(colorSet.getIndices().getData(), colorSet.getIndices().size());
				}else {
					FloatArray flatValues = colorSet.getFlatValues();
					
					writer.writeAttributeName(typeName, "primvars:" + colorSet.getName(), false);
					writer.writeAttributeValuePointNfArray(flatValues.getData(), flatValues.size(), colorSet.getComponentCount());
					writer.beginMetaData();
					writer.writeMetaData("interpolation", "\"faceVarying\"");
					writer.endMetaData();
				}
			}
		}
		
		if(mesh.getNumSubsets() > 0) {
			writer.writeAttributeName("token", "subsetFamily:materialBind:familyType", true);
			writer.writeAttributeValueString("partition");
		}
		
		writer.writeAttributeName("rel", "material:binding", false);
		writer.writeAttributeValue("<" + materialsPrim + matName + ">");
		
		writeMeshSubsets(writer, mesh, purpose, usedTextures, materialsPrim, templates);
		
		writer.endChildren();
		writer.endDef();
	}
	
	private static void writeMeshSubsets(USDWriter writer, Mesh mesh, MeshPurpose purpose, Map<String, Texture> usedTextures, 
											String materialsPrim, Map<MatKey, MaterialTemplate> templates) throws IOException {
		if(mesh.getSubsets() != null) {
			for(MeshSubset subset : mesh.getSubsets()) {
				String subsetName = subset.getName();
				if(subsetName == "")
					subsetName = "subset";
				subsetName = Util.makeSafeName(subsetName);
				
				if(purpose == MeshPurpose.RENDER && subset.getPurpose() == MeshPurpose.PROXY) {
					// We are writing out the render purpose, but this subset should only be
					// available when in proxy mode, so we need to deactive it.
					writer.beginOver(subsetName);
					writer.beginMetaData();
					writer.writeMetaDataBoolean("active", false);
					writer.endMetaData();
					writer.endOver();
					continue;
				}
				if(subset.getPurpose() != purpose && subset.getPurpose() != MeshPurpose.UNDEFINED) {
					continue;
				}
				
				writer.beginDef("GeomSubset", subsetName);
				if(subset.getMatTexture() != null) {
					writer.beginMetaData();
					writer.writeMetaDataStringArray("apiSchemas", new String[] { "MaterialBindingAPI" });
					writer.writeMetaData("customData", "{\ndictionary Maya = {\nbool generated = 1\n}\n}");
					writer.endMetaData();
				}
				writer.beginChildren();
				writer.writeAttributeName("token", "elementType", true);
				writer.writeAttributeValueString("face");
				if(subset.getMatTexture() != null) {
					writer.writeAttributeName("token", "familyName", true);
					writer.writeAttributeValueString("materialBind");
				}
				writer.writeAttributeName("int[]", "indices", false);
				writer.writeAttributeValueIntArray(subset.getFaceIndices().getData(), subset.getFaceIndices().size());
				if(subset.getMatTexture() != null) {
					Texture textureObj2 = new Texture(subset.getTexture(), subset.getMatTexture(), mesh.hasColors(), 
							mesh.isDoubleSided(), mesh.getColorSetNames(), templates);
					String matName = MaterialWriter.getMaterialName(textureObj2.texture, textureObj2.materialTemplate, textureObj2.hasBiomeColor);
					usedTextures.put(matName, textureObj2);
					writer.writeAttributeName("rel", "material:binding", false);
					writer.writeAttributeValue("<" + materialsPrim + matName + ">");
				}
				writer.endChildren();
				writer.endOver();
			}
		}
	}
	
	private static void writeGroup(MeshGroup group, USDWriter writer, MeshPurpose purpose,
									Map<String, Texture> usedTextures, Map<MatKey, MaterialTemplate> templates,
									Kind kind, String materialsPrim) throws IOException {
		String groupName = group.getName();
		if(groupName == "")
			groupName = "group";
		groupName = Util.makeSafeName(groupName);
		
		if(purpose == MeshPurpose.RENDER && group.getPurpose() == MeshPurpose.PROXY) {
			// We are doing the render purpose, but this group is a proxy group.
			// So, let's tag it as proxy.
			writer.beginOver(groupName);
			writer.beginChildren();
			writer.writeAttributeName("token", "purpose", true);
			writer.writeAttributeValueString("proxy");
			writer.endChildren();
			writer.endOver();
			return;
		}
		if(group.getPurpose() != purpose && group.getPurpose() != MeshPurpose.UNDEFINED) {
			return;
		}
		
		if(purpose == MeshPurpose.RENDER && group.getPurpose() == MeshPurpose.UNDEFINED) {
			// We are doing the render purpose, so we are only interested in meshes or
			// groups whose purpose is render. If this group has an undefined purpose,
			// then we want check if any of its children has a render purpose.
			// Otherwise, there's no need to write anything out here.
			if(!group.hasPurpose(purpose))
				return;
		}
		
		if(purpose == MeshPurpose.RENDER && group.getPurpose() == MeshPurpose.UNDEFINED) {
			// We are doing the render purpose and this group is of undefined purpose,
			// which means that it's already defined in the proxy file, so we just need
			// to put in an override prim.
			writer.beginOver(groupName);
		}else {
			writer.beginDef("Xform", groupName);
			if(kind != null) {
				writer.beginMetaData();
				writer.writeMetaDataString("kind", kind.strValue);
				writer.endMetaData();
			}
		}
		writer.beginChildren();
		
		if(group.getPurpose() == MeshPurpose.RENDER) {
			// Make sure to tag this group properly.
			writer.writeAttributeName("token", "purpose", true);
			writer.writeAttributeValueString("render");
		}
		
		String extraData = group.getExtraData();
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
		
		Kind childKind = null;
		if(kind != null)
			childKind = Kind.SUBCOMPONENT;
		for(Mesh child : group.getChildren()) {
			if(child instanceof MeshGroup) {
				writeGroup((MeshGroup) child, writer, purpose, usedTextures, templates, childKind, materialsPrim);
			}else {
				writeMesh(child, writer, purpose, usedTextures, templates, childKind, materialsPrim);
			}
		}
		
		writer.endChildren();
		if(purpose == MeshPurpose.RENDER && group.getPurpose() == MeshPurpose.UNDEFINED) {
			writer.endOver();
		}else {
			writer.endDef();
		}
	}
	
	private static Mesh readMesh(LargeDataInputStream dis) throws IOException {
		byte meshType = dis.readByte();
		if(meshType == 0) {
			return null;
		}else if(meshType == 1) {
			return new Mesh(dis);
		}else if(meshType == 2) {
			return new MeshGroup(dis);
		}
		return null;
	}
	
	
	private void writeMaterials(USDWriter writer, Map<String, Texture> usedTextures) throws IOException {
		Materials.reload();
		
		MaterialWriter[] materialWriters = new MaterialWriter[] { 
				new USDMaterialWriter(materialsFile), 
				new MaterialXMaterialWriter(new File(materialsFile.getPath().replace(".usd", ".mtlx"))),
				new JsonMaterialWriter(new File(materialsFile.getPath().replace(".usd", ".json")))};
		
		for(MaterialWriter materialWriter : materialWriters)
			materialWriter.open();
		
		for(MaterialWriter materialWriter : materialWriters)
			materialWriter.writeSharedNodes("/materials");
		
		for(Entry<String, Texture> entry : usedTextures.entrySet()) {
			Materials.MaterialTemplate material = entry.getValue().materialTemplate;
			if(material != null)
				for(MaterialWriter materialWriter : materialWriters)
					materialWriter.writeMaterial(entry.getKey(), material, entry.getValue().texture, entry.getValue().hasBiomeColor, 
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
		
		writer.beginDef("Scope", "materials");
		writer.beginMetaData();
		writer.writeReferences(materialReferences);
		writer.endMetaData();
		writer.endDef();
	}
	
	
	private void writeChunkPayloads(USDWriter writer, List<ConvertChunkTask> bgChunks, List<ConvertChunkTask> fgChunks) throws IOException {
		if(fgChunks.size() > 0) {
			writer.beginDef("Xform", "foreground");
			writer.beginMetaData();
			writer.writeMetaDataString("kind", Kind.GROUP.strValue);
			writer.endMetaData();
			writer.beginChildren();
			for(ConvertChunkTask chunk : fgChunks)
				writeChunkPayload(writer, chunk);
			writer.endChildren();
			writer.endDef();
		}
		if(bgChunks.size() > 0) {
			writer.beginDef("Xform", "background");
			writer.beginMetaData();
			writer.writeMetaDataString("kind", Kind.GROUP.strValue);
			writer.endMetaData();
			writer.beginChildren();
			for(ConvertChunkTask chunk : bgChunks)
				writeChunkPayload(writer, chunk);
			writer.endChildren();
			writer.endDef();
		}
		
		writer.beginVariantSet("MiEx_LOD");
		writer.beginVariant("proxy");
		writer.endVariant();
		writer.beginVariant("render");
		if(fgChunks.size() > 0) {
			writer.beginOver("foreground");
			writer.beginChildren();
			for(ConvertChunkTask chunk : fgChunks)
				writeChunkRenderPayload(writer, chunk);
			writer.endChildren();
			writer.endOver();
		}
		if(bgChunks.size() > 0) {
			writer.beginOver("background");
			writer.beginChildren();
			for(ConvertChunkTask chunk : bgChunks)
				writeChunkRenderPayload(writer, chunk);
			writer.endChildren();
			writer.endOver();
		}
		writer.endVariant();
		writer.endVariantSet();
	}
	
	private void writeChunkPayload(USDWriter writer, ConvertChunkTask chunk) throws IOException {
		writer.beginDef("Xform", chunk.name);
		writer.beginMetaData();
		writer.writeMetaDataString("kind", Kind.COMPONENT.strValue);
		writer.writePayload("./" + chunksFolder.getName() + "/" + chunk.name + ".usd", false);
		writer.endMetaData();
		writer.beginChildren();
		writer.writeAttributeName("bool", "isFG", true);
		writer.writeAttributeValueBoolean(chunk.isFG);
		
		writer.beginOver("materials");
		writer.beginChildren();
		for(Entry<String, Texture> entry : chunk.usedTextures.entrySet()) {
			writer.writeAttributeName("rel", entry.getKey(), false);
			writer.writeAttributeValue("</world/materials/" + entry.getKey() + ">");
		}
		writer.endChildren();
		writer.endOver();
		
		if(Config.usePointInstancersForIndividualBlocks && Config.useSinglePointInstancer) {
			List<String> prototypes = new ArrayList<String>();
			List<Integer> indices = new ArrayList<Integer>();
			List<Float> positions = new ArrayList<Float>();
			for(Entry<IndividualBlockId, List<Float>> instancer : chunk.instancers.entrySet()) {
				IndividualBlockInfo baseInfo = individualBlocksRegistry.get(instancer.getKey());
				int prototypeIndex = prototypes.size();
				prototypes.add("/world/individualBlocksBaseMeshes/" + baseInfo.path);
				
				for(int i = 0; i < instancer.getValue().size()/3; ++i) {
					indices.add(Integer.valueOf(prototypeIndex));
					positions.add(instancer.getValue().get(i*3+0));
					positions.add(instancer.getValue().get(i*3+1));
					positions.add(instancer.getValue().get(i*3+2));
				}
			}
			
			writer.beginDef("PointInstancer", "individual_blocks");
			writer.beginMetaData();
			writer.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
			writer.endMetaData();
			writer.beginChildren();
			
			writer.writeAttributeName("rel", "prototypes", false);
			writer.writeAttributeValuePrimPathArray(prototypes);
			
			writer.writeAttributeName("point3f[]", "positions", false);
			writer.writeAttributeValuePoint3fArray(positions, positions.size());
			
			writer.writeAttributeName("int[]", "protoIndices", false);
			writer.writeAttributeValueIntArray(indices);
			
			writer.endChildren();
			writer.endDef();
		}else {
			for(Entry<IndividualBlockId, List<Float>> instancer : chunk.instancers.entrySet()) {
				IndividualBlockInfo baseInfo = individualBlocksRegistry.get(instancer.getKey());
				
				if(Config.usePointInstancersForIndividualBlocks) {
					writer.beginDef("PointInstancer", baseInfo.path.replace("_class_", ""));
					writer.beginMetaData();
					writer.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
					writer.endMetaData();
					writer.beginChildren();
					
					writer.writeAttributeName("rel", "prototypes", false);
					writer.writeAttributeValuePrimPathArray(new String[] {"/world/individualBlocksBaseMeshes/" + baseInfo.path});
					
					writer.writeAttributeName("point3f[]", "positions", false);
					writer.writeAttributeValuePoint3fArray(instancer.getValue(), instancer.getValue().size());
					
					writer.writeAttributeName("int[]", "protoIndices", false);
					int[] indices = new int[instancer.getValue().size()/3];
					writer.writeAttributeValueIntArray(indices);
					
					writer.endChildren();
					writer.endDef();
				}else {
					for(int i = 0; i < instancer.getValue().size()/3; ++i) {
						writer.beginDef("Xform", baseInfo.path.replace("_class_", "") + "_" + i);
						writer.beginMetaData();
						writer.writeInherit("/world/individualBlocksBaseMeshes/" + baseInfo.path);
						writer.writeMetaDataBoolean("instanceable", true);
						writer.writeMetaDataString("kind", Kind.SUBCOMPONENT.strValue);
						writer.endMetaData();
						writer.beginChildren();
						
						writer.writeAttributeName("double3", "xformOp:translate", false);
						writer.writeAttributeValuePoint3f(instancer.getValue().get(i*3),
															instancer.getValue().get(i*3+1),
															instancer.getValue().get(i*3+2));
						writer.writeAttributeName("token[]", "xformOpOrder", true);
						writer.writeAttributeValueStringArray(new String[] { "xformOp:translate" });
						
						writer.endChildren();
						writer.endDef();
					}
				}
			}
		}
		
		writer.endChildren();
		writer.endDef();
	}
	
	private void writeChunkRenderPayload(USDWriter writer, ConvertChunkTask chunk) throws IOException {
		writer.beginOver(chunk.name);
		writer.beginMetaData();
		writer.writePayload("./" + chunksFolder.getName() + "/" + chunk.name + "_render.usd", true);
		writer.endMetaData();
		writer.endOver();
	}
	
	
	

	@Override
	public ExportData getExportData(File file) {
		if(!file.exists() || !file.getName().endsWith(".usd"))
			return null;
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String firstLine = in.readLine();
			if(firstLine == null || !firstLine.startsWith("#usda 1.0")) {
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
				if(firstLine == null || !firstLine.startsWith("#usda 1.0")) {
					in.close();
					return null;
				}
			}
			
			ExportData data = new ExportData();
			ExportBounds mainExportBounds = null;
			
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
				if(name.startsWith("region_")) {
					try {
						int fieldSep = name.lastIndexOf('_');
						String regionName = name.substring(7, fieldSep);
						String fieldName = name.substring(fieldSep+1);
						
						ExportBounds exportBounds = null;
						for(ExportBounds exportBounds2 : data.exportRegions) {
							if(exportBounds2.getSafeName().equals(regionName)) {
								exportBounds = exportBounds2;
								break;
							}
						}
						if(exportBounds == null) {
							exportBounds = new ExportBounds("Region 1");
							exportBounds.safeName = regionName;
							data.exportRegions.add(exportBounds);
						}
						
						Field field = ExportBounds.class.getField(fieldName);
						if(type.equals("int")) {
							if(field.getType() == Boolean.TYPE)
								field.set(exportBounds, Integer.parseInt(value) > 0 ? true : false);
							else
								field.set(exportBounds, Integer.parseInt(value));
						}else if(type.equals("string")) {
							field.set(exportBounds, value.substring(1, value.length()-1));
						}else if(type.equals("string[]")) {
							String[] values = value.substring(1, value.length()-1).split(",");
							List<String> valuesList = new ArrayList<String>();
							for(String val : values) {
								val = val.trim();
								if(val.length() > 2)
									valuesList.add(val.substring(1, val.length()-1));
							}
							field.set(exportBounds, valuesList);
						}else if(type.equals("int[]")) {
							if(fieldName.equals("disabledChunks")) {
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
								field.set(exportBounds, valuesList);
							}else if(fieldName.equals("excludeRegions")) {
								String[] values = value.substring(1, value.length()-1).split(",");
								List<ExcludeRegion> valuesList = new ArrayList<ExcludeRegion>();
								for(int i = 0; i < values.length/6; ++i) {
									String minXStr = values[i*6+0].trim();
									String minYStr = values[i*6+1].trim();
									String minZStr = values[i*6+2].trim();
									String maxXStr = values[i*6+3].trim();
									String maxYStr = values[i*6+4].trim();
									String maxZStr = values[i*6+5].trim();
									int minX = minXStr.isEmpty() ? 0 : Integer.parseInt(minXStr);
									int minY = minYStr.isEmpty() ? 0 : Integer.parseInt(minYStr);
									int minZ = minZStr.isEmpty() ? 0 : Integer.parseInt(minZStr);
									int maxX = maxXStr.isEmpty() ? 0 : Integer.parseInt(maxXStr);
									int maxY = maxYStr.isEmpty() ? 0 : Integer.parseInt(maxYStr);
									int maxZ = maxZStr.isEmpty() ? 0 : Integer.parseInt(maxZStr);
									valuesList.add(new ExcludeRegion(minX, minY, minZ, maxX, maxY, maxZ));
								}
								field.set(exportBounds, valuesList);
							}else {
								String[] values = value.substring(1, value.length()-1).split(",");
								List<Integer> valuesList = new ArrayList<Integer>();
								for(String val : values) {
									val = val.trim();
									if(val.isEmpty())
										continue;
									int intVal = Integer.parseInt(val);
									valuesList.add(intVal);
								}
								field.set(exportBounds, valuesList);
							}
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}else {
					try {
						Object dataObj = data;
						// We now support multiple export regions,
						// so how we write out the export regions has changed.
						// It could be that we're opening up a file that
						// was exported out before this update and so
						// would still define it the old way. We need to handle that.
						Field field = null;
						if(name.equals("exportMinX")) {
							name = "minX";
						}else if(name.equals("exportMinY")) {
							name = "minY";
						}else if(name.equals("exportMinZ")) {
							name = "minZ";
						}else if(name.equals("exportMaxX")) {
							name = "maxX";
						}else if(name.equals("exportMaxY")) {
							name = "maxY";
						}else if(name.equals("exportMaxZ")) {
							name = "maxZ";
						}else if(name.equals("exportOffsetY")) {
							name = "offsetY";
						}else if(name.equals("hasLOD")) {
							name = "enableLOD";
						}else if(name.equals("lodCenterX")) {
							name = "lodCenterX";
						}else if(name.equals("lodCenterZ")) {
							name = "lodCenterZ";
						}else if(name.equals("lodWidth")) {
							name = "lodWidth";
						}else if(name.equals("lodDepth")) {
							name = "lodDepth";
						}else if(name.equals("lodYDetail")) {
							name = "lodYDetail";
						}else if(name.equals("fgChunks")) {
							name = "fgChunks";
						}else if(name.equals("disabledChunks")) {
							name = "disabledChunks";
						}else if(name.equals("onlyIndividualBlocks")){
							name = "onlyIndividualBlocks";
						}else {
							field = ExportData.class.getField(name);
						}
						if(field == null) {
							// This is data for the export bounds.
							if(mainExportBounds == null) {
								mainExportBounds = new ExportBounds("Region 1");
								data.exportRegions.add(mainExportBounds);
							}
							dataObj = mainExportBounds;
							field = ExportBounds.class.getField(name);
						}
						if(type.equals("int")) {
							if(field.getType() == Boolean.TYPE)
								field.set(dataObj, Integer.parseInt(value) > 0 ? true : false);
							else
								field.set(dataObj, Integer.parseInt(value));
						}else if(type.equals("string")) {
							field.set(dataObj, value.substring(1, value.length()-1));
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
								field.set(dataObj, valuesList);
							}else {
								String[] values = value.substring(1, value.length()-1).split(",");
								List<String> valuesList = new ArrayList<String>();
								for(String val : values) {
									val = val.trim();
									if(val.length() > 2)
										valuesList.add(val.substring(1, val.length()-1));
								}
								field.set(dataObj, valuesList);
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
								field.set(dataObj, valuesList);
							}else {
								String[] values = value.substring(1, value.length()-1).split(",");
								List<Integer> valuesList = new ArrayList<Integer>();
								for(String val : values) {
									val = val.trim();
									if(val.isEmpty())
										continue;
									int intVal = Integer.parseInt(val);
									valuesList.add(intVal);
								}
								field.set(dataObj, valuesList);
							}
						}
					}catch(Exception ex) {
						ex.printStackTrace();
					}
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
