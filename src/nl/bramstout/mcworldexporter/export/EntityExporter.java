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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.AnimationChannel3D;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BindPose;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BoneAnimation;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Locator;
import nl.bramstout.mcworldexporter.entity.EntityRegistry;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner.SpawnEntity;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelBone;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelLocator;
import nl.bramstout.mcworldexporter.molang.AnimationInfo;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class EntityExporter {

	private static class ModelKey{
		
		private String key;
		private int hash;
		
		public ModelKey(Model model) {
			key = "";
			if(model != null) {
				int faceHash = 0;
				for(ModelFace face : model.getFaces()) {
					for(float f : face.getPoints())
						faceHash = faceHash * 31 + Float.floatToRawIntBits(f);
					for(float f : face.getUVs())
						faceHash = faceHash * 31 + Float.floatToRawIntBits(f);
					if(face.getVertexColors() != null)
						for(float f : face.getVertexColors())
							faceHash = faceHash * 31 + Float.floatToRawIntBits(f);
					key += face.getTexture();
					key += face.getDirection().name();
				}
				key += Integer.toHexString(faceHash);
				for(ModelBone bone : model.getBones()) {
					key += bone.getName();
					if(bone.getParent() != null)
						key += bone.getParent().getName();
					key += Float.toString(bone.translation.x);
					key += Float.toString(bone.translation.y);
					key += Float.toString(bone.translation.z);
					key += Float.toString(bone.rotation.x);
					key += Float.toString(bone.rotation.y);
					key += Float.toString(bone.rotation.z);
				}
				for(Entry<String, String> entry : model.getTextures().entrySet()) {
					key += entry.getKey() + "=" + entry.getValue();
				}
			}
			hash = key.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof ModelKey)
				return key.equals(((ModelKey)obj).key);
			return false;
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
		
	}
	
	private static class EntityPrototype{
		
		private String name;
		private Model prototypeModel;
		
		public EntityPrototype(String name, Model prototypeModel) {
			this.name = name;
			this.prototypeModel = prototypeModel;
		}
		
		public String getName() {
			return name;
		}
		
		public MeshGroup getPrototypeMesh() {
			MeshGroup root = new MeshGroup(name);
			
			for(ModelBone bone : prototypeModel.getBones()) {
				if(bone.getParent() == null)
					root.addMesh(convertBone(bone));
			}
			
			return root;
		}
		
		private MeshGroup convertBone(ModelBone bone) {
			MeshGroup boneGroup = new MeshGroup(Util.makeSafeName(bone.getName()));
			
			if(bone.faceIds.size() > 0) {
				Map<String, Mesh> meshes = new HashMap<String, Mesh>();
				for(Integer faceId : bone.faceIds) {
					if(faceId.intValue() < 0 || faceId.intValue() >= prototypeModel.getFaces().size())
						continue;
					ModelFace face = prototypeModel.getFaces().get(faceId.intValue());
					
					String texture = prototypeModel.getTexture(face.getTexture());
					
					Atlas.AtlasItem atlas = Atlas.getAtlasItem(texture);
					if(atlas != null) {
						texture = atlas.atlas;
					}
					
					Mesh mesh = meshes.getOrDefault(texture, null);
					if(mesh == null) {
						mesh = new Mesh(boneGroup.getName() + "_" + Util.makeSafeName(texture), texture, texture, false, true, 32, 16);
						meshes.put(texture, mesh);
					}
					mesh.addFace(face, 0, 0, 0, atlas, null, 0);
				}
				for(Mesh mesh : meshes.values())
					boneGroup.addMesh(mesh);
			}
			
			// Find child bones and handle those
			for(ModelBone bone2 : prototypeModel.getBones()) {
				if(bone2.getParent() == bone) {
					boneGroup.addMesh(convertBone(bone2));
				}
			}
			
			return boneGroup;
		}
		
	}
	
	private static class EntityInstance{
		
		private ModelKey prototypeKey;
		private Model poseModel;
		private Entity entity;
		
		public EntityInstance(ModelKey prototypeKey, Model poseModel, Entity entity) {
			this.prototypeKey = prototypeKey;
			this.poseModel = poseModel;
			this.entity = entity;
		}
		
		@SuppressWarnings("unused")
		public ModelKey getPrototypeKey() {
			return prototypeKey;
		}
		
		public Model getPoseModel() {
			return poseModel;
		}
		
		public Entity getEntity() {
			return entity;
		}
		
		public boolean isDefaultPose() {
			if(entity.getAnimation() != null)
				return false;
			if(!entity.getAnimation().getBones().isEmpty())
				return false;
			for(ModelBone bone : poseModel.getBones()) {
				if(!bone.isIdentity())
					return false;
			}
			return true;
		}
		
	}
	
	private ExportBounds bounds;
	private World world;
	private Map<ModelKey, EntityPrototype> entityPrototypes;
	private Map<ModelKey, List<EntityInstance>> entityInstances;
	private static ExecutorService threadPool = Executors.newWorkStealingPool(ThreadPool.getNumThreads(1024));
	
	public EntityExporter(ExportBounds bounds, World world) {
		this.bounds = bounds;
		this.world = world;
		this.entityPrototypes = new HashMap<ModelKey, EntityPrototype>();
		this.entityInstances = new HashMap<ModelKey, List<EntityInstance>>();
	}
	
	public void generateEntityInstances() {
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Simulating entities");
		
		int startFrame = MCWorldExporter.getApp().getUI().getEntityDialog().getStartFrame();
		int endFrame = MCWorldExporter.getApp().getUI().getEntityDialog().getEndFrame();
		int fps = MCWorldExporter.getApp().getUI().getEntityDialog().getFPS();
		long globalSeed = MCWorldExporter.getApp().getUI().getEntityDialog().getRandomSeed();
		int spawnDensity = MCWorldExporter.getApp().getUI().getEntityDialog().getSpawnDensityInput();
		int sunLightLevel = MCWorldExporter.getApp().getUI().getEntityDialog().getSunLightLevel();
		if(fps <= 0)
			fps = 24;
		
		Set<String> enabledSpawnRules = new HashSet<String>(
				MCWorldExporter.getApp().getUI().getEntityDialog().getSpawnRules().getSelection());
		Set<String> enabledEntityExports = new HashSet<String>(
				MCWorldExporter.getApp().getUI().getEntityDialog().getExportEntities().getSelection());
		Set<String> enabledEntitySimulations = new HashSet<String>(
				MCWorldExporter.getApp().getUI().getEntityDialog().getSimulateEntities().getSelection());
		
		Random random = new Random(globalSeed);
		
		int boundsMinX = bounds.getMinX();
		int boundsMinY = bounds.getMinY();
		int boundsMinZ = bounds.getMinZ();
		int boundsMaxX = bounds.getMaxX();
		int boundsMaxY = bounds.getMaxY();
		int boundsMaxZ = bounds.getMaxZ();
		if(bounds.hasLod()) {
			boundsMinX = bounds.getLodMinX();
			boundsMinZ = bounds.getLodMinZ();
			boundsMaxX = bounds.getLodMaxX();
			boundsMaxZ = bounds.getLodMaxZ();
		}
		
		// Get all entities in the export bounds.
		List<Entity> entities = new ArrayList<Entity>();
		for(List<Entity> entities2 : world.getEntitiesInRegion(MCWorldExporter.getApp().getExportBounds())) {
			for(Entity entity : entities2) {
				if(entity.getX() < boundsMinX || entity.getX() > boundsMaxX ||
						entity.getY() < boundsMinY || entity.getY() > boundsMaxY ||
						entity.getZ() < boundsMinZ || entity.getZ() > boundsMaxZ)
					continue;
				if(!MCWorldExporter.getApp().getExportBounds().isBlockInEnabledChunk((int) entity.getX(), (int) entity.getZ()))
					continue;
				if(!enabledEntityExports.contains(entity.getId()))
					continue;
				entity.setGlobalRandomSeed(globalSeed);
				entities.add(entity);
			}
		}
		
		List<EntitySpawner> spawners = ResourcePacks.getEntitySpawners();
		
		if(!enabledSpawnRules.isEmpty()) {
			long numBlocksInBounds = (((long) boundsMaxX) - ((long) boundsMinX)) * 
									(((long) boundsMaxY) - ((long) boundsMinY)) * 
									(((long) boundsMaxZ) - ((long) boundsMinZ));
			// One spawn try per 32x32x32 blocks.
			long spawnTries = (numBlocksInBounds * spawnDensity) / (32*32*32) / 1000;
			long uniqueIdCounter = 0;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0);
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Spawning entities");
			for(long spawnTry = 0; spawnTry < spawnTries; ++spawnTry) {
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(((float) spawnTry) / ((float) spawnTries));
				// Select a random block.
				int blockX = random.nextInt(boundsMinX, boundsMaxX + 1);
				int blockY = random.nextInt(boundsMinY, boundsMaxY + 1);
				int blockZ = random.nextInt(boundsMinZ, boundsMaxZ + 1);
				
				if(!MCWorldExporter.getApp().getExportBounds().isBlockInEnabledChunk(blockX, blockZ))
					continue;
				int worldHeight = MCWorldExporter.getApp().getWorld().getHeight(blockX, blockZ);
				if((blockY - 32) > worldHeight)
					continue; // We are fully up in the air.
				
				// Make sure that the block can be spawned in.
				// So it's either air or water.
				int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
				if(blockId != 0) {
					Block block = BlockRegistry.getBlock(blockId);
					if(!block.getName().equals("minecraft:water"))
						continue;
				}
				
				// Now if the block is air, we need to find a spot with a non-air block below it.
				if(blockId == 0) {
					int minY = Math.max(boundsMinY, blockY - 32);
					// Clamp blockY to just above the height map.
					blockY = Math.min(blockY, worldHeight + 2);
					boolean found = false;
					for(; blockY >= minY; --blockY) {
						int blockIdBelow = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY - 1, blockZ);
						if(blockIdBelow != 0) {
							found = true;
							break;
						}
					}
					if(!found)
						continue;
				}
				
				// We have a block that we can try to spawn on.
				// Now we go through all EntitySpawners to find one to use.
				List<EntitySpawner> candidateSpawners = new ArrayList<EntitySpawner>();
				long totalWeight = 0;
				for(EntitySpawner spawner : spawners) {
					if(!enabledSpawnRules.contains(spawner.getEntityType()))
						continue;
					if(!enabledEntityExports.contains(spawner.getEntityType()))
						continue;
					if(spawner.test(blockX, blockY, blockZ, sunLightLevel, random)) {
						candidateSpawners.add(spawner);
						totalWeight += spawner.getWeight();
					}
				}
				if(candidateSpawners.isEmpty())
					continue;
				
				// Select one randomly
				long selection = random.nextLong(totalWeight);
				EntitySpawner selectedSpawner = null;
				for(EntitySpawner spawner : candidateSpawners) {
					selection -= spawner.getWeight();
					if(selection < 0) {
						selectedSpawner = spawner;
						break;
					}
				}
				
				// Spawn entities
				List<SpawnEntity> spawns = selectedSpawner.spawn(blockX, blockY, blockZ, random);
				for(SpawnEntity spawn : spawns) {
					int chunkX = spawn.x >> 4;
					int chunkZ = spawn.z >> 4;
					Chunk chunk = null;
					try {
						chunk = MCWorldExporter.getApp().getWorld().getChunk(chunkX, chunkZ);
					}catch(Exception ex) {
						World.handleError(ex);
					}
					if(chunk == null)
						continue;
					
					NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("");
					
					NbtTagList posTag = NbtTagList.newNonPooledInstance("Pos", new NbtTag[] {
							NbtTagFloat.newNonPooledInstance("X", ((float) spawn.x) + 0.5f),
							NbtTagFloat.newNonPooledInstance("Y", ((float) spawn.y)),
							NbtTagFloat.newNonPooledInstance("Z", ((float) spawn.z) + 0.5f)
					});
					properties.addElement(posTag);
					
					NbtTagList rotTag = NbtTagList.newNonPooledInstance("Rotation", new NbtTag[] {
							NbtTagFloat.newNonPooledInstance("Yaw", random.nextFloat() * 360f),
							NbtTagFloat.newNonPooledInstance("Pitch", 0f)
					});
					properties.addElement(rotTag);
					
					NbtTagLong uniqueIdTag = NbtTagLong.newNonPooledInstance("UniqueID", uniqueIdCounter);
					uniqueIdCounter += 100;
					properties.addElement(uniqueIdTag);
					
					NbtTagString identifierTag = NbtTagString.newNonPooledInstance("identifier", spawn.name);
					properties.addElement(identifierTag);
					
					Entity entity = EntityRegistry.getEntity(spawn.name, properties);
					if(entity != null) {
						entity.setGlobalRandomSeed(globalSeed);
						entities.add(entity);
						chunk.getEntities().add(entity);
						
						if(entity.getAI() != null) {
							for(String event : spawn.events) {
								if(event != null)
									entity.getAI().fireEvent(event);
							}
						}
					}
				}
			}
		}
		
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Simulating entities");
		
		
		int counter = 0;
		int numEntities = entities.size() + (endFrame - startFrame);
		
		// Get the models for the entities.
		for(Entity entity : entities) {	
			Model entityModel = entity.getModel();
			ModelKey modelKey = new ModelKey(entityModel);
			
			if(entityModel != null && !entityPrototypes.containsKey(modelKey)) {
				entityPrototypes.put(modelKey, new EntityPrototype(Util.makeSafeName(entity.getId()) + "_" + 
												Integer.toHexString(modelKey.hash), entityModel));
			}
			
			List<EntityInstance> entityList = entityInstances.getOrDefault(modelKey, null);
			if(entityList == null) {
				entityList = new ArrayList<EntityInstance>();
				entityInstances.put(modelKey, entityList);
			}
			entityList.add(new EntityInstance(modelKey, entityModel, entity));
			
			if(entity.getAI() != null) {
				float[] boundingBox = entityModel.getBoundingBox();
				float width = 0;
				float height = Math.max(boundingBox[4], 1f); // max Y
				width = Math.max(Math.abs(boundingBox[0]), Math.abs(boundingBox[3]));
				width = Math.max(Math.abs(boundingBox[2]), Math.abs(boundingBox[5]));
				width /= 16f;
				height /= 16f;
				entity.getAI().collisionBoxWidth = width;
				entity.getAI().collisionBoxHeight = height;
			}
			
			// Set up the animation
			float startTime = ((float) startFrame) / ((float) fps);
			entity.setupAnimation();
			entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(startTime, entity.getX()));
			entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(startTime, entity.getY()));
			entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(startTime, entity.getZ()));
			entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(startTime, entity.getYaw()));
			entity.getAnimation().getAnimPitch().addKeyframe(new Keyframe(startTime, entity.getPitch()));
			entity.getAnimation().getAnimHeadYaw().addKeyframe(new Keyframe(startTime, entity.getHeadYaw()));
			entity.getAnimation().getAnimHeadPitch().addKeyframe(new Keyframe(startTime, entity.getHeadPitch()));
			entity.getAnimation().getAnimScaleX().addKeyframe(new Keyframe(startTime, 1f));
			entity.getAnimation().getAnimScaleY().addKeyframe(new Keyframe(startTime, 1f));
			entity.getAnimation().getAnimScaleZ().addKeyframe(new Keyframe(startTime, 1f));
			// Set up the bind pose and locators
			if(entityModel != null) {
				for(ModelBone bone : entityModel.getBones()) {
					BindPose bindPose = new BindPose(bone.getName(), bone.getParent() == null ? "" : bone.getParent().getName());
					bindPose.posX = bone.translation.x;
					// Apparently there's an offset on the bone's Y translation.
					// No idea why. Later on when writing the resulting values we need
					// to add this offset back on.
					bindPose.posY = bone.translation.y - 24f;
					bindPose.posZ = bone.translation.z;
					bindPose.rotX = bone.rotation.x;
					bindPose.rotY = bone.rotation.y;
					bindPose.rotZ = bone.rotation.z;
					bindPose.scaleX = bone.scaling.x;
					bindPose.scaleY = bone.scaling.y;
					bindPose.scaleZ = bone.scaling.z;
					entity.getAnimation().getBindPoses().put(bone.getName(), bindPose);
				}
				for(ModelLocator locator : entityModel.getLocators()) {
					Locator locator2 = new Locator(locator.getName());
					locator2.offset = locator.offset;
					locator2.rotation = locator.rotation;
					locator2.ignoreInheritedScale = locator.ignoreInheritedScale;
					locator2.bone = locator.bone == null ? "" : locator.bone.getName();
					entity.getAnimation().getLocators().put(locator.getName(), locator2);
				}
			}
			MolangQuery query = new MolangQuery(entity.getId(), entity.getProperties(), entity.getX(), entity.getY(), entity.getZ());
			MolangContext context = new MolangContext(query, entity.getRandom());
			context.setVariableDict(entity.getVariables());
			for(MolangScript initScript : entity.getInitMolangScripts())
				initScript.eval(context);
			if(entity.getAI() != null)
				for(MolangScript initScript : entity.getAI().getInitMolangScripts())
					initScript.eval(context);
			counter++;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress((((float) counter) / ((float) numEntities)) * 0.8f);
		}
		
		// Simulate the entities and animate them
		for(int frame = startFrame; frame < endFrame; ++frame) {
			float time = ((float) frame) / ((float) fps);
			float deltaTime = 1f / ((float) fps);
			
			int numEntities2 = entities.size();
			int numTasks = ThreadPool.getNumThreads(1024) * 4;
			int numEntitiesPerTask = (numEntities2 + numTasks - 1) / numTasks;
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for(int i = 0; i < numTasks; ++i) {
				final int entityStartIndex = i * numEntitiesPerTask;
				final int entityEndIndex = Math.min((i + 1) * numEntitiesPerTask, numEntities2);
				futures.add(threadPool.submit(new Runnable() {

					@Override
					public void run() {
						for(int i = entityStartIndex; i < entityEndIndex; ++i) {
							Entity entity = entities.get(i);
							if(!enabledEntitySimulations.contains(entity.getId()))
								continue;
							
							if(entity.getAI() != null)
								entity.getAI().tick(time, deltaTime);
							
							if(entity.getAnimationController() != null) {
								float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
								float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
								float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
								MolangQuery query = new MolangQuery(entity.getId(), entity.getProperties(), posX, posY, posZ);
								MolangContext context = new MolangContext(query, entity.getRandom());
								context.setVariableDict(entity.getVariables());
								AnimationInfo animInfo = context.pushAnimationInfo();
								animInfo.animation = entity.getAnimation();
								animInfo.globalTime = time;
								animInfo.deltaTime = deltaTime;
								entity.applyBindPoseToAnimation(time);
								entity.getAnimationController().eval(entity.getAnimationControllerState(), entity.getAnimations(), 
										time, deltaTime, time, 1f, context, entity);
							}
						}
					}
				
				}));
			}
			for(Future<?> future : futures) {
				try {
					future.get();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			counter++;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress((((float) counter) / ((float) numEntities)) * 0.8f);
		}
	}
	
	public void writeEntities(LargeDataOutputStream dos) throws IOException{
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.85f);
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Writing entities");
		dos.writeInt(entityPrototypes.size());
		float numPrototypes = (float) entityPrototypes.size();
		float counter = 0f;
		for(Entry<ModelKey, EntityPrototype> prototype : entityPrototypes.entrySet()) {
			MeshGroup rootGroup = prototype.getValue().getPrototypeMesh();
			dos.writeUTF(rootGroup.getName());
			dos.writeInt(rootGroup.getNumChildren());
			for(Mesh childMesh : rootGroup.getChildren()) {
				childMesh.write(dos);
			}
			counter += 1f;
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.85f + (counter / numPrototypes) * 0.05f);
		}
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.9f);
		dos.writeInt(entityInstances.size());
		float numInstances = (float) entityInstances.size();
		counter = 0f;
		for(Entry<ModelKey, List<EntityInstance>> entry : entityInstances.entrySet()) {
			EntityPrototype prototype = entityPrototypes.getOrDefault(entry.getKey(), null);
			if(prototype == null) {
				dos.writeUTF("");
				dos.writeInt(0);
				continue;
			}
			dos.writeUTF(prototype.getName());
			dos.writeInt(entry.getValue().size());
			float numInstances2 = (float) entry.getValue().size();
			float counter2 = 0f;
			for(EntityInstance entity : entry.getValue()) {
				entity.getEntity().getAnimation().getAnimPos3D().write(dos, 
							-bounds.getOffsetX(), -bounds.getOffsetY(), -bounds.getOffsetZ(), 16f);
				entity.getEntity().getAnimation().getAnimRotation3D().write(dos, 0f, 0f, 0f, 1f);
				entity.getEntity().getAnimation().getAnimScale3D().write(dos, 0f, 0f, 0f, 1f);
				
				if(entity.isDefaultPose()) {
					dos.writeInt(0);
				}else {
					List<ModelBone> rootBones = new ArrayList<ModelBone>();
					for(ModelBone bone : entity.getPoseModel().getBones()) {
						if(bone.getParent() == null)
							rootBones.add(bone);
					}
					dos.writeInt(rootBones.size());
					for(ModelBone bone : rootBones)
						writeBone(bone, entity, dos);
				}
				counter2 += 1f;
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(
						0.9f + (((counter2 / numInstances2) + counter) / numInstances) * 0.1f);
			}
			counter += 1f;
		}
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(1f);
	}
	
	private void writeBone(ModelBone bone, EntityInstance entity, LargeDataOutputStream dos) throws IOException{
		dos.writeUTF(Util.makeSafeName(bone.getName()));
		BoneAnimation anim = null;
		if(entity.getEntity().getAnimation() != null)
			anim = entity.getEntity().getAnimation().getBones().getOrDefault(bone.getName(), null);
		
		if(anim == null) {
			BindPose bindPose = null;
			if(entity.getEntity().getAnimation() != null)
				bindPose = entity.getEntity().getAnimation().getBindPoses().getOrDefault(bone.getName(), null);
			if(bindPose != null) {
				dos.writeInt(1);
				new AnimationChannel3D(0f, bindPose.posX, bindPose.posY, bindPose.posZ).write(dos, 0f, 24f, 0f, 1f);
				new AnimationChannel3D(0f, bindPose.rotX, bindPose.rotY, bindPose.rotZ).write(dos, 0f, 0f, 0f, 1f);
				new AnimationChannel3D(0f, bindPose.scaleX, bindPose.scaleY, bindPose.scaleZ).write(dos, 0f, 0f, 0f, 1f);
				dos.writeBoolean(bone.visibility);
			}else {
				dos.writeInt(0);
				dos.writeBoolean(bone.visibility);
			}
		}else {
			dos.writeInt(1);
			anim.getAnimPos3D().write(dos, 0, 24f, 0f, 1f);
			anim.getAnimRotation3D().write(dos, 0f, 0f, 0f, 1f);
			anim.getAnimScale3D().write(dos, 0f, 0f, 0f, 1f);
			dos.writeBoolean(bone.visibility);
		}
		
		List<ModelBone> children = new ArrayList<ModelBone>();
		for(ModelBone bone2 : entity.getPoseModel().getBones()) {
			if(bone2.getParent() == bone)
				if(shouldWriteBone(bone2, entity))
					children.add(bone2);
		}
		
		dos.writeInt(children.size());
		for(ModelBone bone2 : children) {
			writeBone(bone2, entity, dos);
		}
	}
	
	private boolean shouldWriteBone(ModelBone bone, EntityInstance entity) {
		if(entity.getEntity().getAnimation() != null) {
			if(entity.getEntity().getAnimation().getBones().getOrDefault(bone.getName(), null) != null)
				return true;
			BindPose bindPose = entity.getEntity().getAnimation().getBindPoses().getOrDefault(bone.getName(), null);
			if(bindPose != null) {
				if(!bindPose.isIdentity())
					return true;
			}
		}
		for(ModelBone bone2 : entity.getPoseModel().getBones())
			if(bone2.getParent() == bone)
				if(shouldWriteBone(bone2, entity))
					return true;
		return false;
	}
	
}
