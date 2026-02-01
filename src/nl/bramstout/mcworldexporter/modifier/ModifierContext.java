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

package nl.bramstout.mcworldexporter.modifier;

import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.export.BlendedBiome;
import nl.bramstout.mcworldexporter.export.VertexColorSet.VertexColorFace;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.modifier.ModifierNode.Attribute;
import nl.bramstout.mcworldexporter.modifier.ModifierNode.Value;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.world.Block;

public class ModifierContext {

	private Map<ModifierNode, Value> evalCache = new HashMap<ModifierNode, Value>();
	// [READ_WRITE, BLOCK/FACE] Biome colours
	public BlendedBiome[] biome;
	// [READ, BLOCK] Biome data
	public Biome biomeInstance;
	// [READ, BLOCK] Block data
	public Block block;
	// [READ, BLOCK] Block X position
	public int blockX;
	// [READ, BLOCK] Block Y position
	public int blockY;
	// [READ, BLOCK] Block Z position
	public int blockZ;
	// [READ_WRITE, BLOCK/FACE] Vertex colours
	public VertexColorFace[] vertexColors;
	// [READ, FACE] Face center X
	public float faceCenterX;
	// [READ, FACE] Face center Y
	public float faceCenterY;
	// [READ, FACE] Face center Z
	public float faceCenterZ;
	// [READ_WRITE, FACE] Face normal X
	public float faceNormalX;
	// [READ_WRITE, FACE] Face normal Y
	public float faceNormalY;
	// [READ_WRITE, FACE] Face normal Z
	public float faceNormalZ;
	// [READ, FACE] Face Tint Red
	public float faceTintR;
	// [READ, FACE] Face Tint Green
	public float faceTintG;
	// [READ, FACE] Face Tint Blue
	public float faceTintB;
	// [READ_WRITE, FACE] Face tint index
	public int faceTintIndex;
	// [READ, FACE] Face direction
	public Direction faceDirection;
	public Modifier currentModifier;
	
	public void clearEvalCache() {
		this.evalCache.clear();
	}
	
	public Value getValue(Attribute attr) {
		if(attr.isConnected()) {
			String nodeName = attr.getInput();
			ModifierNode node = currentModifier.getNode(nodeName);
			if(node == null)
				return new Value();
			Value val = evalCache.getOrDefault(node, null);
			if(val == null) {
				val = node.evaluate(this);
				evalCache.put(node, val);
			}
			return val;
		}else {
			return attr.getValue();
		}
	}
	
}
