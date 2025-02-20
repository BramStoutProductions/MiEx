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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.model.Occlusion;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.parallel.ThreadPool.Task;
import nl.bramstout.mcworldexporter.resourcepack.BannerTextureCreator;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class Exporter {
	
	private static ThreadPool threadPool = new ThreadPool("Exporter", 1024);
	private static Object mutex = new Object();
	private static Set<IndividualBlockId> individualBlockIds = new HashSet<IndividualBlockId>();
	public static int NUM_CHUNKS = 0;
	public static File currentExportFile = null;
	public static File chunksFolder = null;
	private static AtomicBoolean isExporting = new AtomicBoolean();

	public static boolean isExporting() {
		return isExporting.get();
	}
	
	public static void export(File usdFile) throws Exception{
		isExporting.set(true);
		try {
			_export(usdFile);
		}catch(Exception e) {
			isExporting.set(false);
			throw e;
		}
		isExporting.set(false);
	}
	
	private static void _export(File usdFile) throws Exception {
		if(MCWorldExporter.getApp().getWorld() == null) {
			throw new RuntimeException("No valid world loaded.");
		}
		
		String extensionTokens[] = usdFile.getName().split("\\.");
		String extension = extensionTokens[extensionTokens.length-1];
		
		currentExportFile = usdFile;
		chunksFolder = new File(usdFile.getParentFile(), usdFile.getName().replace("." + extension, "_chunks"));
		BackgroundThread.waitUntilDoneWithBackgroundTasks();
		
		MaterialWriter.clearCounters();
		
		BannerTextureCreator.load();
		
		File file = new File(usdFile.getPath().replace("." + extension, ".miex"));
		
		Converter converter = Converter.getConverter(extension.toLowerCase(), file, usdFile);
		
		List<Task> futures = new ArrayList<Task>();
		int chunkSize = Config.chunkSize;
		
		// Set the amount of threads based on how much memory each thread may use.
		// The amount of memory that each thread may use is scaled by the chunk size.
		// The value coming from the config assumes a chunkSize of 16.
		//float memoryScalingFactor = (float) Math.sqrt(((float) (chunkSize * chunkSize)) / (16f * 16f));
		float memoryScalingFactor = ((float) chunkSize) / 16f;
		threadPool.setNumThreads(Math.max((int) (((float) Config.memoryPerThread) * memoryScalingFactor), 64));
		
		int chunkStartX = MCWorldExporter.getApp().getExportBounds().getMinX() >> 4;
		int chunkStartZ = MCWorldExporter.getApp().getExportBounds().getMinZ() >> 4;
		int chunkEndX = MCWorldExporter.getApp().getExportBounds().getMaxX() >> 4;
		int chunkEndZ = MCWorldExporter.getApp().getExportBounds().getMaxZ() >> 4;
		
		int centerX = MCWorldExporter.getApp().getExportBounds().getCenterX();
		int centerZ = MCWorldExporter.getApp().getExportBounds().getCenterZ();
		if(MCWorldExporter.getApp().getExportBounds().hasLod()) {
			centerX = MCWorldExporter.getApp().getExportBounds().getLodCenterX();
			centerZ = MCWorldExporter.getApp().getExportBounds().getLodCenterZ();
		}
		MCWorldExporter.getApp().getExportBounds().setOffsetX(centerX);
		//MCWorldExporter.getApp().getExportBounds().setOffsetY(MCWorldExporter.getApp().getWorld().getHeight(centerX, centerZ) + 1);
		MCWorldExporter.getApp().getExportBounds().setOffsetZ(centerZ);
		
		LargeDataOutputStream dos = new LargeDataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		dos.writeInt(2); // Version
		dos.writeLong(0); // Offset for individual blocks
		
		// Export settings
		ExportData exportData = ExportData.fromApp();
		exportData.write(dos);
		

		// Export entities
		EntityExporter entityExporter = new EntityExporter(MCWorldExporter.getApp().getExportBounds(), MCWorldExporter.getApp().getWorld());
		entityExporter.generateEntityInstances();
		String entityFilename = file.getName().replace(".miex", "_entities.miex");
		File entityFile = new File(file.getParentFile(), entityFilename);
		LargeDataOutputStream entityDos = new LargeDataOutputStream(new BufferedOutputStream(new FileOutputStream(entityFile)));
		entityExporter.writeEntities(entityDos);
		entityDos.close();
		dos.writeUTF(entityFilename);
		
		// Export chunks
		int numChunksX = (chunkEndX - chunkStartX + 1 + chunkSize - 1) / chunkSize;
		int numChunksZ = (chunkEndZ - chunkStartZ + 1 + chunkSize - 1) / chunkSize;
		int numChunks = 0;
		for(int i = 0; i < numChunksX; ++i) {
			for(int j = 0; j < numChunksZ; ++j) {
				if(MCWorldExporter.getApp().getExportBounds().isChunkEnabled(i, j))
					numChunks++;
			}
		}
		NUM_CHUNKS = numChunks;
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Exporting");
		MCWorldExporter.getApp().getUI().getProgressBar().setNumChunks(numChunks);
		
		individualBlockIds = new HashSet<IndividualBlockId>();
		
		List<File> chunkFiles = new ArrayList<File>();
		
		dos.writeInt(numChunks);
		
		int j = 0;
		for(int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ += chunkSize) {
			int i = 0;
			for(int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX += chunkSize) {
				if(MCWorldExporter.getApp().getExportBounds().isChunkEnabled(i, j)) {
					String chunkName = "chunk_" + (i + 1) + "_" + (j + 1);
					String chunkFilename = file.getName().replace(".miex", "_" + chunkName + ".miex");
					File chunkFile = new File(file.getParentFile(), chunkFilename);
					LargeDataOutputStream chunkDos = new LargeDataOutputStream(new BufferedOutputStream(new FileOutputStream(chunkFile)));
					futures.add(threadPool.submit(new ExportChunkTask(new ChunkExporter(MCWorldExporter.getApp().getExportBounds(), 
							MCWorldExporter.getApp().getWorld(), chunkX, chunkZ, chunkSize, chunkName), chunkDos)));
					
					dos.writeUTF(chunkFilename);
					chunkFiles.add(chunkFile);
				}
				++i;
			}
			++j;
		}
		
		for(Task future : futures) {
			try {
				future.waitUntilTaskIsDone();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		int defaultBiomeId = MCWorldExporter.getApp().getWorld().getBiomeId(
													centerX, 
													MCWorldExporter.getApp().getWorld().getHeight(centerX, centerZ), 
													centerZ);
		Biome defaultBiome = BiomeRegistry.getBiome(defaultBiomeId);
		BlendedBiome defaultBlendedBiome = new BlendedBiome();
		defaultBlendedBiome.addBiome(defaultBiome, 1.0f);
		
		long individualBlocksOffset = dos.size();
		dos.writeInt(individualBlockIds.size());
		List<Model> models = new ArrayList<Model>();
		Occlusion occlusionHandler = new Occlusion();
		List<ModelFace> emptyFaceList = new ArrayList<ModelFace>();
		for(IndividualBlockId blockId : individualBlockIds) {
			dos.writeInt(blockId.getBlockId());
			dos.writeInt(blockId.getX());
			dos.writeInt(blockId.getY());
			dos.writeInt(blockId.getZ());
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId.getBlockId(), 
											blockId.getX(), blockId.getY(), blockId.getZ());
			dos.writeUTF(state.getName());
			Map<String, Mesh> meshes = new HashMap<String, Mesh>();
			models.clear();
			state.getDefaultModels(models);
			
			occlusionHandler.calculateCornerDataForModel(models, state, 0, emptyFaceList);
			
			Color tint = defaultBlendedBiome.getBiomeColor(state);
			
			int faceIndex = 0;
			for(Model model : models) {
				for(ModelFace face : model.getFaces()) {
					String texture = model.getTexture(face.getTexture());
					if(Config.bannedMaterials.contains(texture)) {
						faceIndex++;
						continue;
					}
					Color faceTint = tint;
					if((face.getTintIndex() < 0 && !Config.forceBiomeColor.contains(texture)) || 
							Config.forceNoBiomeColor.contains(state.getName()))
						faceTint = null;
					
					String matTexture = texture;
					Atlas.AtlasItem atlas = Atlas.getAtlasItem(texture);
					if(atlas != null)
						texture = atlas.atlas;
					
					int cornerData = occlusionHandler.getCornerIndexForFace(face, faceIndex);
					
					Mesh mesh = meshes.get(matTexture);
					if(mesh == null) {
						mesh = new Mesh(matTexture, texture, matTexture, false, model.isDoubleSided(), 32, 8);
						mesh.addFace(face, -0.5f, -0.5f, -0.5f, atlas, faceTint, cornerData);
						meshes.put(matTexture, mesh);
					}else {
						mesh.addFace(face, -0.5f, -0.5f, -0.5f, atlas, faceTint, cornerData);
					}
					mesh.setExtraData(model.getExtraData());
					
					faceIndex++;
				}
			}
			dos.writeInt(meshes.size());
			for(Entry<String, Mesh> mesh : meshes.entrySet()) {
				mesh.getValue().write(dos);
			}
		}
		
		dos.close();
		
		// Write out any data that we couldn't write earlier.
		// Like the byte offset for where the instancer models are
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.seek(4);
		raf.write((int) ((individualBlocksOffset >>>  0) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>>  8) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 16) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 24) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 32) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 40) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 48) & 0xFF));
		raf.write((int) ((individualBlocksOffset >>> 56) & 0xFF));
		raf.close();
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.1f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Converting");
		
		converter.init();
		converter.convert();
		
		if(converter.deleteMiExFiles()) {
			// Delete the .miex file, since we don't need it anymore
			file.delete();
			entityFile.delete();
			for(File chunkFile : chunkFiles)
				chunkFile.delete();
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		String message = "World exported successfully.";
		if(BlockStateRegistry.missingBlockStates.size() > 0) {
			message = "World exported, but some blocks may be missing due to missing blockstates or models. Check the log for more information.";
			for(String blockName : BlockStateRegistry.missingBlockStates) {
				System.out.println("Missing blockstate: " + blockName);
			}
		}
		
		if(ModelRegistry.missingModels.size() > 0) {
			message = "World exported, but some blocks may be missing due to missing blockstates or models. Check the log for more information.";
			for(String blockName : ModelRegistry.missingModels) {
				System.out.println("Missing model: " + blockName);
			}
		}
		System.out.println("Exported:" + usdFile.getPath());
		
		// Make sure to unload the entities.
		// The entity exporter modifies the entities during simulation.
		// In case we want to export again, we need to reload the entities
		// to get rid of the modified data.
		MCWorldExporter.getApp().getWorld().unloadEntities();
		JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), message, "Done", JOptionPane.PLAIN_MESSAGE);
	}
	
	private static class ExportChunkTask implements Runnable {

		private ChunkExporter chunk;
		private LargeDataOutputStream dos;

		public ExportChunkTask(ChunkExporter chunk, LargeDataOutputStream dos) {
			this.chunk = chunk;
			this.dos = dos;
		}

		@Override
		public void run() {
			try {
				chunk.generateMeshes();
				//NbtTag.freeMemoryFromPools();
				//if(Config.runOptimiser)
				//	chunk.optimiseMeshes();
				//chunk.writeMeshes(dos);
				chunk.optimiseAndWriteMeshes(dos);
				dos.close();
				synchronized(mutex) {
					individualBlockIds.addAll(chunk.getIndividualBlockIds());
				}
				chunk.cleanUp();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
}
