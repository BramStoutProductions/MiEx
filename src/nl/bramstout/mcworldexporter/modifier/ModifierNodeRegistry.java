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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.modifier.ModifierNode.Attribute;
import nl.bramstout.mcworldexporter.modifier.ModifierNode.Value;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeAdd;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeAdjustHSV;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeAnd;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeCast;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeClamp;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeDivide;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeEquals;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeExp;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeExp10;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBiomeColor;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBiomeName;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockName;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockProperty;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockX;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockXYZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockY;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetBlockZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetFaceCenter;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetFaceDirection;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetFaceNormal;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetFaceTintIndex;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetTime;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetVertexColor;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetVertexPosition;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetVertexTint;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetVertexUVs;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetW;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetX;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetXY;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetXYZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetXZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetY;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetYX;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetYZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetZ;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetZW;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetZX;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGetZY;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGreaterThan;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeGreaterThanOrEquals;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeIf;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeLerp;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeLessThan;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeLessThanOrEquals;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeLog;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeLog10;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMakeFloat2;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMakeFloat3;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMakeFloat4;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMatch;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMax;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMin;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeMultiply;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNoiseFloat;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNoiseFloat3;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNormalise;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNot;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeNotEquals;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeOr;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodePow;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeRemap;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetBiomeColor;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetFaceNormal;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetFaceTintIndex;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexColor;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexPosition;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexTint;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexUVs;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSubtract;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSwitch;

public class ModifierNodeRegistry {
	
	private static Map<String, Class<? extends ModifierNode>> registry = new HashMap<String, Class<? extends ModifierNode>>();
	
	static {
		registry.put("add", ModifierNodeAdd.class);
		registry.put("adjustHSV", ModifierNodeAdjustHSV.class);
		registry.put("and", ModifierNodeAnd.class);
		registry.put("cast", ModifierNodeCast.class);
		registry.put("clamp", ModifierNodeClamp.class);
		registry.put("divide", ModifierNodeDivide.class);
		registry.put("equals", ModifierNodeEquals.class);
		registry.put("exp", ModifierNodeExp.class);
		registry.put("exp10", ModifierNodeExp10.class);
		registry.put("getBiomeColor", ModifierNodeGetBiomeColor.class);
		registry.put("getBiomeName", ModifierNodeGetBiomeName.class);
		registry.put("getBlockName", ModifierNodeGetBlockName.class);
		registry.put("getBlockProperty", ModifierNodeGetBlockProperty.class);
		registry.put("getBlockX", ModifierNodeGetBlockX.class);
		registry.put("getBlockXYZ", ModifierNodeGetBlockXYZ.class);
		registry.put("getBlockY", ModifierNodeGetBlockY.class);
		registry.put("getBlockZ", ModifierNodeGetBlockZ.class);
		registry.put("getFaceCenter", ModifierNodeGetFaceCenter.class);
		registry.put("getFaceDirection", ModifierNodeGetFaceDirection.class);
		registry.put("getFaceNormal", ModifierNodeGetFaceNormal.class);
		registry.put("getFaceTintIndex", ModifierNodeGetFaceTintIndex.class);
		registry.put("getTime", ModifierNodeGetTime.class);
		registry.put("getVertexColor", ModifierNodeGetVertexColor.class);
		registry.put("getVertexPosition", ModifierNodeGetVertexPosition.class);
		registry.put("getVertexTint", ModifierNodeGetVertexTint.class);
		registry.put("getVertexUVs", ModifierNodeGetVertexUVs.class);
		registry.put("getW", ModifierNodeGetW.class);
		registry.put("getX", ModifierNodeGetX.class);
		registry.put("getXY", ModifierNodeGetXY.class);
		registry.put("getXYZ", ModifierNodeGetXYZ.class);
		registry.put("getXZ", ModifierNodeGetXZ.class);
		registry.put("getY", ModifierNodeGetY.class);
		registry.put("getYX", ModifierNodeGetYX.class);
		registry.put("getYZ", ModifierNodeGetYZ.class);
		registry.put("getZ", ModifierNodeGetZ.class);
		registry.put("getZW", ModifierNodeGetZW.class);
		registry.put("getZX", ModifierNodeGetZX.class);
		registry.put("getZY", ModifierNodeGetZY.class);
		registry.put("greaterThan", ModifierNodeGreaterThan.class);
		registry.put("greaterThanOrEquals", ModifierNodeGreaterThanOrEquals.class);
		registry.put("if", ModifierNodeIf.class);
		registry.put("lerp", ModifierNodeLerp.class);
		registry.put("lessThan", ModifierNodeLessThan.class);
		registry.put("lessThanOrEquals", ModifierNodeLessThanOrEquals.class);
		registry.put("log", ModifierNodeLog.class);
		registry.put("log10", ModifierNodeLog10.class);
		registry.put("makeFloat2", ModifierNodeMakeFloat2.class);
		registry.put("makeFloat3", ModifierNodeMakeFloat3.class);
		registry.put("makeFloat4", ModifierNodeMakeFloat4.class);
		registry.put("match", ModifierNodeMatch.class);
		registry.put("max", ModifierNodeMax.class);
		registry.put("min", ModifierNodeMin.class);
		registry.put("multiply", ModifierNodeMultiply.class);
		registry.put("noiseFloat", ModifierNodeNoiseFloat.class);
		registry.put("noiseFloat3", ModifierNodeNoiseFloat3.class);
		registry.put("normalise", ModifierNodeNormalise.class);
		registry.put("not", ModifierNodeNot.class);
		registry.put("notEquals", ModifierNodeNotEquals.class);
		registry.put("or", ModifierNodeOr.class);
		registry.put("pow", ModifierNodePow.class);
		registry.put("remap", ModifierNodeRemap.class);
		registry.put("setBiomeColor", ModifierNodeSetBiomeColor.class);
		registry.put("setFaceNormal", ModifierNodeSetFaceNormal.class);
		registry.put("setFaceTintIndex", ModifierNodeSetFaceTintIndex.class);
		registry.put("setVertexColor", ModifierNodeSetVertexColor.class);
		registry.put("setVertexPosition", ModifierNodeSetVertexPosition.class);
		registry.put("setVertexTint", ModifierNodeSetVertexTint.class);
		registry.put("setVertexUVs", ModifierNodeSetVertexUVs.class);
		registry.put("subtract", ModifierNodeSubtract.class);
		registry.put("switch", ModifierNodeSwitch.class);
	}
	
	public static ModifierNode createNode(String name, JsonObject data) {
		String type = "";
		if(data.has("type"))
			type = data.get("type").getAsString();
		Class<? extends ModifierNode> clazz = registry.getOrDefault(type, null);
		if(clazz == null)
			return null;
		try {
			Constructor<? extends ModifierNode> constructor = clazz.getConstructor(String.class);
			ModifierNode node = constructor.newInstance(name);
			
			for(Entry<String, JsonElement> entry : data.entrySet()) {
				if(entry.getKey().equals("type"))
					continue;
				
				Value value = null;
				String input = null;
				
				if(entry.getValue().isJsonPrimitive() || entry.getValue().isJsonArray()) {
					value = new Value(entry.getValue());
				}else if(entry.getValue().isJsonObject()) {
					JsonObject valObj = entry.getValue().getAsJsonObject();
					if(valObj.has("value")) {
						value = new Value(valObj.get("value"));
					}
					if(valObj.has("connect")) {
						input = valObj.get("connect").getAsString();
					}
					if(value == null && input == null)
						value = new Value(entry.getValue());
				}
				if(value == null)
					value = new Value();
				
				Attribute attr = node.getAttributeOrCreate(entry.getKey());
				if(attr == null)
					continue;
				
				attr.setValue(value);
				if(input != null)
					attr.connect(input);
			}
			
			return node;
		}catch(Exception ex) {}
		return null;
	}

}
