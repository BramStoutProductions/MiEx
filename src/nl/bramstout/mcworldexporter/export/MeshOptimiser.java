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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MeshOptimiser {
	
	private static class BVHNode{
		
		public float minX, minY, minZ, maxX, maxY, maxZ = 0f;
		public BVHNode left = null;
		public BVHNode right = null;
		public List<Integer> prims = new ArrayList<Integer>();
		public int depth = 0;
		
		public void calculateBoundingBox(Mesh mesh) {
			if(prims.size() >= 1) {
				float size = mesh.getFaceCenters().get(prims.get(0) * 4 + 3) * 16f;
				minX = mesh.getFaceCenters().get(prims.get(0) * 4 + 0);
				minY = mesh.getFaceCenters().get(prims.get(0) * 4 + 1);
				minZ = mesh.getFaceCenters().get(prims.get(0) * 4 + 2);
				maxX = minX + size;
				maxY = minY + size;
				maxZ = minZ + size;
				
				float x, y, z = 0f;
				for(int i = 1; i < prims.size(); ++i) {
					x = mesh.getFaceCenters().get(prims.get(i) * 4 + 0);
					y = mesh.getFaceCenters().get(prims.get(i) * 4 + 1);
					z = mesh.getFaceCenters().get(prims.get(i) * 4 + 2);
					size = mesh.getFaceCenters().get(prims.get(i) * 4 + 3) * 16f;
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
			if(prims.size() <= 1)
				return false;
			
			float volume = getVolume();
			
			if(volume > 16f*16f*16f)
				return true;
			
			return false;
		}
		
		public void split(Mesh mesh) {
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
			for(int i = 0; i < prims.size(); ++i) {
				x = mesh.getFaceCenters().get(prims.get(i) * 4 + 0);
				y = mesh.getFaceCenters().get(prims.get(i) * 4 + 1);
				z = mesh.getFaceCenters().get(prims.get(i) * 4 + 2);
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
			spreadX = (float) Math.sqrt(spreadX / prims.size()) / dx;
			spreadY = (float) Math.sqrt(spreadY / prims.size()) / dy;
			spreadZ = (float) Math.sqrt(spreadZ / prims.size()) / dz;
			biasX = Math.abs(biasX / prims.size() / dx);
			biasY = Math.abs(biasY / prims.size() / dy);
			biasZ = Math.abs(biasZ / prims.size() / dz);
			spreadX += biasX;
			spreadY += biasY;
			spreadZ += biasZ;
			spreadX *= Math.pow(dx / maxD, 0.25f);
			spreadY *= Math.pow(dy / maxD, 0.25f);
			spreadZ *= Math.pow(dz / maxD, 0.25f);
			averageX /= prims.size();
			averageY /= prims.size();
			averageZ /= prims.size();
			
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
			
			left = new BVHNode();
			right = new BVHNode();
			left.depth = depth + 1;
			right.depth = depth + 1;
			((ArrayList<Integer>)left.prims).ensureCapacity(prims.size()/2);
			((ArrayList<Integer>)right.prims).ensureCapacity(prims.size()/2);
			
			float pos = 0;
			for(int i = 0; i < prims.size(); ++i) {
				pos = mesh.getFaceCenters().get(prims.get(i) * 4 + componentOffset);
				if(pos < threshold)
					left.prims.add(prims.get(i));
				else
					right.prims.add(prims.get(i));
			}
			if(left.prims.isEmpty() || right.prims.isEmpty()) {
				left = null;
				right = null;
				return;
			}
			
			prims = null;
			
			left.calculateBoundingBox(mesh);
			right.calculateBoundingBox(mesh);
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
	}
	
	private static class BVH{
		
		public BVHNode root;
		
	}
	
	private static BVH buildBVH(Mesh mesh) {
		BVH bvh = new BVH();
		
		Stack<BVHNode> stack = new Stack<BVHNode>();
		
		BVHNode currentNode = new BVHNode();
		for(int i = 0; i < mesh.getFaceCenters().size() / 4; ++i) {
			currentNode.prims.add(i);
		}
		currentNode.calculateBoundingBox(mesh);
		stack.add(currentNode);
		bvh.root = currentNode;
		
		while(!stack.empty()) {
			currentNode = stack.pop();
			if(currentNode.shouldSplit()) {
				currentNode.split(mesh);
				if(currentNode.left != null) {
					stack.add(currentNode.left);
					stack.add(currentNode.right);
				}
			}
		}
		
		return bvh;
	}
	
	private static void addPrimsToMesh(Mesh origMesh, Mesh outMesh, BVHNode node) {
		Stack<BVHNode> stack = new Stack<BVHNode>();
		stack.add(node);
		
		while(!stack.empty()) {
			node = stack.pop();
			if(node.left != null) {
				stack.add(node.left);
				stack.add(node.right);
			}else {
				for(int i = 0; i < node.prims.size(); ++i) {
					outMesh.addFaceFromMesh(origMesh, node.prims.get(i));
				}
			}
		}
	}
	
	private static Mesh newMesh(Mesh origMesh, BVHNode node, int meshCounter) {
		Mesh mesh = new Mesh(origMesh.getName() + meshCounter, origMesh.getTexture(), origMesh.isDoubleSided());
		mesh.setExtraData(origMesh.getExtraData());
		addPrimsToMesh(origMesh, mesh, node);
		return mesh;
	}
	
	public static Mesh optimiseMesh(Mesh mesh, float fullnessThreshold) {
		if(fullnessThreshold <= 0f)
			return mesh;
		fullnessThreshold = Math.min(fullnessThreshold, 0.99f);
		BVH bvh = buildBVH(mesh);
		MeshGroup outMesh = new MeshGroup(mesh.getName());
		int meshCounter = 0;
		
		Stack<BVHNode> stack = new Stack<BVHNode>();
		stack.add(bvh.root);
		
		BVHNode currentNode = null;
		while(!stack.empty()) {
			currentNode = stack.pop();
			if(currentNode.getFullness() >= fullnessThreshold) {
				outMesh.addMesh(newMesh(mesh, currentNode, ++meshCounter));
			}else {
				stack.add(currentNode.left);
				stack.add(currentNode.right);
			}
		}
		if(outMesh.getNumChildren() <= 1)
			return mesh;
		return outMesh;
	}

}