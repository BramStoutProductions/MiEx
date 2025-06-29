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

package nl.bramstout.mcworldexporter.export.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.Poolable;
import nl.bramstout.mcworldexporter.SingleThreadedMemoryPool;
import nl.bramstout.mcworldexporter.export.LargeDataOutputStream;
import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.export.MeshGroup;
import nl.bramstout.mcworldexporter.export.MeshPurpose;
import nl.bramstout.mcworldexporter.export.MeshSubset;
import nl.bramstout.mcworldexporter.export.processors.MeshProcessors.MeshMergerMode;
import nl.bramstout.mcworldexporter.export.processors.MeshProcessors.WriteCapturer;

/**
 * This optimiser splits meshes up into smaller meshes,
 * in order to speed up ray tracing.
 * 
 * A ray tracer doesn't test against every single polygon
 * in every single mesh. Instead, it uses an acceleration
 * structure to be able to ignore polygons that the ray
 * won't hit anyways. A good analogy for this would be
 * the bounding boxes of meshes. If a ray doesn't hit
 * the bouding box of a mesh, then it can't hit any of
 * its polygons and so the ray tracer doesn't have to
 * check those polygons.
 * 
 * This works well when the bounding box tightly wraps
 * the polygons, but any empty space in the bounding box
 * will decrease performance. Let's say that you have 
 * a village with doors. You might only have 20 or so
 * doors in the village, so not that many polygons.
 * But, when you look at the bounding box, it is really
 * big because it has to encompass all of the doors,
 * who are quite spread out. This create large pockets
 * of empty space. When a ray needs to be traced,
 * it's very likely to hit that big bounding box, and so
 * the ray tracer has to check the ray against that mesh,
 * even if the ray will never hit any of the doors.
 * 
 * This causes a lot of slow downs. This optimiser constructs
 * a BVH (the acceleration structure used in ray tracers)
 * in order to find these pockets of empty space and split
 * the mesh up into smaller meshes so that each of these
 * smaller meshes have much better bounding boxes.
 * This does increase the number of meshes which can also
 * slow things down, so it's really about finding the right
 * balance.
 */
public class RaytracingOptimiser implements MeshProcessors.IMeshProcessor{
	
	public static class BVHNode extends Poolable{
		
		public float minX, minY, minZ, maxX, maxY, maxZ = 0f;
		public BVHNode left = null;
		public BVHNode right = null;
		public int[] prims = null;
		public int primsOffset = 0;
		public int primsCount = 0;
		public int primsCapacity = 0;
		public int depth = 0;
		
		public BVHNode() {}
		
		public void setup() {
			minX = 0f;
			minY = 0f;
			minZ = 0f;
			maxX = 0f;
			maxY = 0f;
			maxZ = 0f;
			left = null;
			right = null;
			prims = null;
			primsOffset = 0;
			primsCount = 0;
			primsCapacity = 0;
			depth = 0;
		}
		
		public void calculateBoundingBox(Mesh mesh) {
			if(primsCount >= 1) {
				float size = mesh.getFaceCenters().get(prims[primsOffset] * 4 + 3) * 16f;
				minX = mesh.getFaceCenters().get(prims[primsOffset] * 4 + 0);
				minY = mesh.getFaceCenters().get(prims[primsOffset] * 4 + 1);
				minZ = mesh.getFaceCenters().get(prims[primsOffset] * 4 + 2);
				maxX = minX + size;
				maxY = minY + size;
				maxZ = minZ + size;
				
				float x, y, z = 0f;
				for(int i = 1; i < primsCount; ++i) {
					x = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 0);
					y = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 1);
					z = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 2);
					size = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 3) * 16f;
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					minZ = Math.min(minZ, z);
					maxX = Math.max(maxX, x + size);
					maxY = Math.max(maxY, y + size);
					maxZ = Math.max(maxZ, z + size);
				}
			}
		}
		
		public boolean shouldSplit() {
			if(primsCount <= 1)
				return false;
			
			float volume = getVolume();
			
			if(volume > 16f*16f*16f)
				return true;
			
			return false;
		}
		
		public void split(Mesh mesh, SingleThreadedMemoryPool<BVHNode> nodePool) {
			float dx = maxX - minX;
			float dy = maxY - minY;
			float dz = maxZ - minZ;
			float maxD = Math.max(Math.max(dx, dy), dz);
			float cx = (minX + maxX - 16f) * 0.5f;
			float cy = (minY + maxY - 16f) * 0.5f;
			float cz = (minZ + maxZ - 16f) * 0.5f;
			float spreadX = 0f;
			float spreadY = 0f;
			float spreadZ = 0f;
			float biasX = 0f;
			float biasY = 0f;
			float biasZ = 0f;
			float averageX = 0f;
			float averageY = 0f;
			float averageZ = 0f;
			float x, y, z = 0;
			for(int i = 0; i < primsCount; ++i) {
				x = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 0);
				y = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 1);
				z = mesh.getFaceCenters().get(prims[primsOffset + i] * 4 + 2);
				spreadX += (x - cx) * (x - cx);
				spreadY += (y - cy) * (y - cy);
				spreadZ += (z - cz) * (z - cz);
				biasX += x - cx;
				biasY += y - cy;
				biasZ += z - cz;
				averageX += x;
				averageY += y;
				averageZ += z;
			}
			spreadX = (float) Math.sqrt(spreadX / ((float) primsCount)) / dx;
			spreadY = (float) Math.sqrt(spreadY / ((float) primsCount)) / dy;
			spreadZ = (float) Math.sqrt(spreadZ / ((float) primsCount)) / dz;
			biasX = Math.abs(biasX / ((float) primsCount) / dx);
			biasY = Math.abs(biasY / ((float) primsCount) / dy);
			biasZ = Math.abs(biasZ / ((float) primsCount) / dz);
			spreadX += biasX;
			spreadY += biasY;
			spreadZ += biasZ;
			spreadX *= Math.pow(dx / maxD, 0.25f);
			spreadY *= Math.pow(dy / maxD, 0.25f);
			spreadZ *= Math.pow(dz / maxD, 0.25f);
			averageX /= ((float) primsCount);
			averageY /= ((float) primsCount);
			averageZ /= ((float) primsCount);
			
			int componentOffset = 0;
			if(spreadY > spreadX)
				componentOffset = 1;
			if(spreadZ > spreadX && spreadZ > spreadY)
				componentOffset = 2;
			
			float threshold = averageX;
			if(componentOffset == 1)
				threshold = averageY;
			if(componentOffset == 2)
				threshold = averageZ;
			
			//left = new BVHNode();
			//right = new BVHNode();
			left = nodePool.alloc();
			left.setup();
			right = nodePool.alloc();
			right.setup();
			left.depth = depth + 1;
			right.depth = depth + 1;
			left.prims = prims;
			right.prims = prims;
			left.primsOffset = primsOffset;
			right.primsOffset = primsOffset + (primsCapacity / 2);
			left.primsCapacity = primsCapacity / 2;
			right.primsCapacity = primsCapacity / 2;
			
			float pos = 0;
			int primIndex = 0;
			for(int i = 0; i < primsCount; ++i) {
				primIndex = prims[primsOffset + i];
				pos = mesh.getFaceCenters().get(primIndex * 4 + componentOffset);
				if(pos < threshold)
					left.addPrim(primIndex);
				else
					right.addPrim(primIndex);
			}
			if(left.primsCount == 0 || right.primsCount == 0) {
				left.free(nodePool);
				right.free(nodePool);
				left = null;
				right = null;
				return;
			}
			// The left and right children can have different prim counts
			// so we need to move the right prims over so that each
			// prim has double the capacity compared to its prims count.
			left.primsCapacity = left.primsCount * 2;
			int rightPrimsOffset = (left.primsOffset + left.primsCapacity) - right.primsOffset;
			if(rightPrimsOffset > 0) {
				// We need to move the numbers to the right
				for(int i = (right.primsOffset + right.primsCount) - 1; i >= right.primsOffset; i--) {
					right.prims[i + rightPrimsOffset] = right.prims[i];
				}
			}else if(rightPrimsOffset < 0) {
				// We need to move the numbers to the left
				for(int i = right.primsOffset; i < (right.primsOffset + right.primsCount); i++) {
					right.prims[i + rightPrimsOffset] = right.prims[i];
				}
			}
			right.primsOffset = left.primsOffset + left.primsCapacity;
			right.primsCapacity = primsCapacity - left.primsCapacity;
			
			primsCount = 0;
			
			left.calculateBoundingBox(mesh);
			right.calculateBoundingBox(mesh);
		}
		
		public void addPrim(int prim) {
			if(primsOffset + primsCount >= prims.length || 
					primsCount > primsCapacity) {
				throw new RuntimeException();
			}
			prims[primsOffset + primsCount] = prim;
			primsCount++;
		}
		
		public float getVolume() {
			float dx = maxX - minX;
			float dy = maxY - minY;
			float dz = maxZ - minZ;
			float volume = dx * dy * dz;
			return volume;
		}
		
		public float getFullness() {
			if(left == null)
				return 1f;
			float volume = getVolume();
			float leftVolume = left.getVolume();
			float rightVolume = right.getVolume();
			float leftFullness = left.getFullness();
			float rightFullness = right.getFullness();
			float fullness = (leftVolume * leftFullness + rightVolume * rightFullness) / volume;
			return fullness;
		}
		
		public void free(SingleThreadedMemoryPool<BVHNode> nodePool) {
			if(left != null)
				left.free(nodePool);
			if(right != null)
				right.free(nodePool);
			nodePool.free(this);
		}
	}
	
	private static class BVH{
		
		public BVHNode root;
		public int[] primsArray;
		
		public void free(SingleThreadedMemoryPool<BVHNode> nodePool) {
			root.free(nodePool);
		}
		
	}
	
	private SingleThreadedMemoryPool<BVHNode> nodePool = new SingleThreadedMemoryPool<BVHNode>(BVHNode.class);
	private int[] primsArray = null;
	private float fullnessThreshold = 0.0f;
	private LargeDataOutputStream dos;
	
	public RaytracingOptimiser(float fullnessThreshold, LargeDataOutputStream dos) {
		this.fullnessThreshold = fullnessThreshold;
		this.dos = dos;
	}
	
	private BVH buildBVH(Mesh mesh) {
		BVH bvh = new BVH();
		//bvh.primsArray = new int[Integer.highestOneBit((mesh.getFaceCenters().size() / 4) * 2 + 1) << 1];
		int primsArraySize = (mesh.getFaceCenters().size() / 4) * 2;
		if(primsArray == null || primsArray.length < primsArraySize)
			primsArray = new int[primsArraySize];
		bvh.primsArray = primsArray;
		
		Stack<BVHNode> stack = new Stack<BVHNode>();
		
		//BVHNode currentNode = new BVHNode();
		BVHNode currentNode = nodePool.alloc();
		currentNode.setup();
		currentNode.prims = bvh.primsArray;
		currentNode.primsOffset = 0;
		currentNode.primsCapacity = bvh.primsArray.length;
		for(int i = 0; i < mesh.getFaceCenters().size() / 4; ++i) {
			currentNode.addPrim(i);
		}
		currentNode.calculateBoundingBox(mesh);
		stack.add(currentNode);
		bvh.root = currentNode;
		
		while(!stack.empty()) {
			currentNode = stack.pop();
			if(currentNode.shouldSplit()) {
				currentNode.split(mesh, nodePool);
				if(currentNode.left != null) {
					stack.add(currentNode.left);
					stack.add(currentNode.right);
				}
			}
		}
		
		return bvh;
	}
	
	private Stack<BVHNode> stack = new Stack<BVHNode>();
	
	private void addPrimsToMesh(Mesh origMesh, Mesh outMesh, BVHNode node, int[] subsetIds) {
		stack.clear();
		stack.add(node);
		
		while(!stack.empty()) {
			node = stack.pop();
			if(node.left != null) {
				stack.add(node.left);
				stack.add(node.right);
			}else {
				for(int i = 0; i < node.primsCount; ++i) {
					int faceIndex = node.prims[node.primsOffset + i];
					MeshSubset subset = null;
					if(subsetIds != null) {
						int subsetId = subsetIds[faceIndex];
						if(subsetId >= 0)
							subset = origMesh.getSubset(subsetId);
					}
					outMesh.addFaceFromMesh(origMesh, faceIndex, subset, false);
				}
			}
		}
	}
	
	private void addPrimsToSubset(Mesh mesh, List<MeshSubset> subsets, int meshCounter, 
									int[] subsetIds, BVHNode node) {
		stack.clear();
		stack.add(node);
		
		while(!stack.empty()) {
			node = stack.pop();
			if(node.left != null) {
				stack.add(node.left);
				stack.add(node.right);
			}else {
				for(int i = 0; i < node.primsCount; ++i) {
					int faceIndex = node.prims[node.primsOffset + i];
					int subsetId = -1;
					if(subsetIds != null)
						subsetId = subsetIds[faceIndex];
					int actualSubsetId = subsetId < 0 ? 0 : subsetId;
					
					if(actualSubsetId >= subsets.size()) {
						for(int j = subsets.size(); j <= actualSubsetId; ++j)
							subsets.add(null);
					}
					
					MeshSubset subset = subsets.get(actualSubsetId);
					if(subset == null) {
						long uniqueId = Integer.toUnsignedLong(mesh.hashCode()) << 32 | Integer.toUnsignedLong(meshCounter);
						if(subsetId < 0) {
							// No subset to copy from.
							subset = new MeshSubset("section_" + meshCounter, null, null, false, MeshPurpose.RENDER, true, uniqueId);
							subsets.set(actualSubsetId, subset);
						}else {
							// Subset to copy from.
							MeshSubset origSubset = mesh.getSubset(subsetId);
							subset = new MeshSubset(origSubset.getName() + "_" + meshCounter,
													origSubset.getTexture(), origSubset.getMatTexture(),
													origSubset.isAnimatedTexture(), MeshPurpose.RENDER, true, uniqueId);
							subsets.set(actualSubsetId, subset);
						}
					}
					subset.getFaceIndices().add(faceIndex);
				}
			}
		}
	}
	
	private Mesh newMesh(Mesh origMesh, BVHNode node, int meshCounter, int[] subsetIds) {
		Mesh mesh = new Mesh(origMesh.getName() + meshCounter, MeshPurpose.RENDER, origMesh.getTexture(), 
							origMesh.getMatTexture(), origMesh.hasAnimatedTexture(), origMesh.isDoubleSided(), 128, 8);
		mesh.setExtraData(origMesh.getExtraData());
		addPrimsToMesh(origMesh, mesh, node, subsetIds);
		return mesh;
	}
	
	private List<MeshSubset> newSubsets = new ArrayList<MeshSubset>();
	
	private void newSubset(Mesh mesh, BVHNode node, int meshCounter, int[] subsetIds, List<MeshSubset> subsets) {
		newSubsets.clear();
		addPrimsToSubset(mesh, newSubsets, meshCounter, subsetIds, node);
		for(MeshSubset subset : newSubsets)
			if(subset != null)
				subsets.add(subset);
	}
	
	public Mesh optimiseMesh(Mesh mesh, float fullnessThreshold) {
		if(fullnessThreshold <= 0f)
			return mesh;
		int[] subsetIds = mesh.generateSubsetIds();
		
		fullnessThreshold = Math.min(fullnessThreshold, 0.99f);
		BVH bvh = buildBVH(mesh);
		MeshGroup outMesh = new MeshGroup(mesh.getName(), MeshPurpose.RENDER);
		int meshCounter = 0;
		
		Stack<BVHNode> stack = new Stack<BVHNode>();
		stack.add(bvh.root);
		
		BVHNode currentNode = null;
		while(!stack.empty()) {
			currentNode = stack.pop();
			if(currentNode.getFullness() >= fullnessThreshold) {
				outMesh.addMesh(newMesh(mesh, currentNode, ++meshCounter, subsetIds));
			}else {
				stack.add(currentNode.left);
				stack.add(currentNode.right);
			}
		}
		bvh.free(nodePool);
		if(outMesh.getNumChildren() <= 1)
			return mesh;
		return outMesh;
	}
	
	@Override
	public void process(Mesh mesh, MeshProcessors manager) throws Exception{
		if(fullnessThreshold <= 0f) {
			manager.processNext(mesh, this);
			return;
		}
		
		fullnessThreshold = Math.min(fullnessThreshold, 0.99f);
		BVH bvh = buildBVH(mesh);
		int meshCounter = 0;
		int[] subsetIds = mesh.generateSubsetIds();
		
		if(Config.raytracingOptimiserUseMeshSubsets) {
			// Write out a single mesh with geometry subsets.
			
			// The mesh could already make use of subsets,
			// so we want to then separate those subsets, if needed.
			ArrayList<MeshSubset> subsets = new ArrayList<MeshSubset>();
			
			Stack<BVHNode> stack = new Stack<BVHNode>();
			stack.add(bvh.root);
			
			BVHNode currentNode = null;
			while(!stack.empty()) {
				currentNode = stack.pop();
				if(currentNode.getFullness() >= fullnessThreshold) {
					newSubset(mesh, currentNode, ++meshCounter, subsetIds, subsets);
				}else {
					stack.add(currentNode.left);
					stack.add(currentNode.right);
				}
			}
			bvh.free(nodePool);
			mesh.setSubsets(subsets);
			
			manager.processNext(mesh, this);
		}else {
			// Write out a group with individual meshes.
			
			// We want to capture all meshes written out, so that we can combine
			// them into a proxy mesh and write it out.
			int writeCapturerId = manager.registerWriteCapturer();
			
			int meshMergerId = manager.beginMeshMerger(MeshMergerMode.DISABLED);
			
			dos.writeByte(2); // Mesh type : Group
			dos.writeUTF(mesh.getName() + "_Render");
			dos.writeInt(MeshPurpose.RENDER.id);
			dos.writeUTF(""); // No extra data to write out.
			//dos.writeInt(1);
			
			Stack<BVHNode> stack = new Stack<BVHNode>();
			stack.add(bvh.root);
			
			BVHNode currentNode = null;
			while(!stack.empty()) {
				currentNode = stack.pop();
				if(currentNode.getFullness() >= fullnessThreshold) {
					manager.processNext(newMesh(mesh, currentNode, ++meshCounter, subsetIds), this);
				}else {
					stack.add(currentNode.left);
					stack.add(currentNode.right);
				}
			}
			bvh.free(nodePool);
			
			dos.writeByte(0); // End array with empty type.
			
			manager.endMeshMerger(meshMergerId);
			
			
			// Now combine all meshes into a proxy mesh.
			WriteCapturer capturer = manager.getWriteCapturer(writeCapturerId);
			Mesh proxyMesh = new Mesh(mesh.getName(), MeshPurpose.PROXY, mesh.getTexture(), 
										mesh.getMatTexture(), mesh.hasAnimatedTexture(), mesh.isDoubleSided(),
										mesh.getVertices().size()/3, mesh.getUs().size());
			for(Mesh mesh2 : capturer.meshes) {
				mesh2.appendMesh(mesh2, false);
			}
			manager.getDefaultWriteProcessor().process(proxyMesh, manager);
			manager.unregisterWriteCapturer(writeCapturerId);
		}
	}

}