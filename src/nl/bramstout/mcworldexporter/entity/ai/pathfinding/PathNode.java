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

package nl.bramstout.mcworldexporter.entity.ai.pathfinding;

public class PathNode {
	
	private int x;
	private int y;
	private int z;
	private float cost;
	private float totalCost;
	private PathNode prevNode;
	
	public PathNode() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.cost = 0f;
		this.totalCost = 0f;
		this.prevNode = null;
	}
	
	public PathNode(int x, int y, int z, float cost, float totalCost, PathNode prevNode) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.totalCost = totalCost;
		this.prevNode = prevNode;
	}
	
	public PathNode(PathNode other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.cost = other.cost;
		this.totalCost = other.totalCost;
		this.prevNode = other.prevNode;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	public float getCost() {
		return cost;
	}
	
	public float getTotalCost() {
		return totalCost;
	}
	
	public PathNode getPrevNode() {
		return prevNode;
	}
	
	public void setCost(float cost, float totalCost) {
		this.cost = cost;
		this.totalCost = totalCost;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PathNode)
			return x == ((PathNode)obj).x && y == ((PathNode)obj).y && z == ((PathNode)obj).z;
		return false;
	}

}
