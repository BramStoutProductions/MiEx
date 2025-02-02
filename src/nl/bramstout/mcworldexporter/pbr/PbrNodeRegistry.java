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

package nl.bramstout.mcworldexporter.pbr;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.pbr.nodes.PbrNode;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeAlphaMode;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeBlend;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeBlendNormals;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeBlur;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeComposite;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeCondition;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeGamutMap;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeLUT;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeMath;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeMatrix;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeNormalFromBump;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeNormaliseColor;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeRead;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeRemap;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeResize;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeShuffle;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeTransform;
import nl.bramstout.mcworldexporter.pbr.nodes.PbrNodeWrite;

public class PbrNodeRegistry {
	
	private static Map<String, Class<? extends PbrNode>> typeRegistry = new HashMap<String, Class<? extends PbrNode>>();
	private static Map<String, Constructor<? extends PbrNode>> constructors = new HashMap<String, Constructor<? extends PbrNode>>();
	
	static {
		typeRegistry.put("AlphaMode", PbrNodeAlphaMode.class);
		typeRegistry.put("Blend", PbrNodeBlend.class);
		typeRegistry.put("BlendNormals", PbrNodeBlendNormals.class);
		typeRegistry.put("Blur", PbrNodeBlur.class);
		typeRegistry.put("Composite", PbrNodeComposite.class);
		typeRegistry.put("Condition", PbrNodeCondition.class);
		typeRegistry.put("GamutMap", PbrNodeGamutMap.class);
		typeRegistry.put("LUT", PbrNodeLUT.class);
		typeRegistry.put("Math", PbrNodeMath.class);
		typeRegistry.put("Matrix", PbrNodeMatrix.class);
		typeRegistry.put("NormalFromBump", PbrNodeNormalFromBump.class);
		typeRegistry.put("NormaliseColor", PbrNodeNormaliseColor.class);
		typeRegistry.put("Read", PbrNodeRead.class);
		typeRegistry.put("Remap", PbrNodeRemap.class);
		typeRegistry.put("Resize", PbrNodeResize.class);
		typeRegistry.put("Shuffle", PbrNodeShuffle.class);
		typeRegistry.put("Transform", PbrNodeTransform.class);
		typeRegistry.put("Write", PbrNodeWrite.class);
		
		for(Entry<String, Class<? extends PbrNode>> entry : typeRegistry.entrySet()) {
			try {
				constructors.put(entry.getKey(), entry.getValue().getConstructor(String.class, PbrNodeGraph.class));
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PbrNode createNode(String type, String name, PbrNodeGraph graph) {
		Constructor<? extends PbrNode> constructor = constructors.getOrDefault(type, null);
		if(constructor == null)
			throw new RuntimeException("No node of type exists: " + type);
		try {
			return constructor.newInstance(name, graph);
		} catch (Exception ex) {
			throw new RuntimeException("Could not create new instance of node of type: " + type, ex);
		}
	}
	
}
