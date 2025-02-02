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

package nl.bramstout.mcworldexporter.export.materialx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.export.usd.USDConverter;
import nl.bramstout.mcworldexporter.export.usd.USDWriter;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialNetwork;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialTemplate;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingAttribute;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingNode;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.java.MCMetaJavaEdition;
import nl.bramstout.mcworldexporter.world.World;

public class MaterialXMaterialWriter extends MaterialWriter {

	private USDWriter usdWriter;
	private File usdOutputFile;
	private MaterialXWriter writer;
	private boolean _hasWrittenAnything;

	public MaterialXMaterialWriter(File mtlxFile) {
		super(mtlxFile);
		usdOutputFile = new File(mtlxFile.getPath().replace(".mtlx", "_mtlx.usd"));
		_hasWrittenAnything = false;
	}

	@Override
	public String getUSDAssetPath() {
		return "./" + usdOutputFile.getName();
	}

	@Override
	public boolean hasWrittenAnything() {
		return _hasWrittenAnything;
	}

	@Override
	public void open() throws IOException {
		writer = new MaterialXWriter(outputFile);
		writer.beginNode("materialx");
		writer.writeAttribute("version", "1.38");
		writer.beginChildren();
		
		usdWriter = new USDWriter(usdOutputFile);
		usdWriter.beginMetaData();
		usdWriter.writeMetaDataString("defaultPrim", "materials");
		usdWriter.endMetaData();
		usdWriter.beginDef("Scope", "materials");
		usdWriter.beginChildren();
	}

	@Override
	public void close() throws IOException {
		// If we haven't written anything, then we can just as well delete the file.
		// It probably would be nicer if we'd never have to write the file at all,
		// but then we'd have to go through all of the materials first to figure
		// out which file types are used. That seems a bit excessive.
		// So while not idea, this is fine for now.
		// We do it by telling in the close() func to delete it.
		
		writer.endChildren();
		writer.endNode("materialx");
		writer.close(!hasWrittenAnything());
		
		usdWriter.endChildren();
		usdWriter.endDef();
		usdWriter.close(!hasWrittenAnything());
	}

	@Override
	public void writeSharedNodes(String parentPrim) throws IOException {
		if (Materials.sharedNodes.nodes.isEmpty())
			return;
		writer.beginNode("nodegraph");
		writer.writeAttribute("name", "sharedNodes");
		writer.beginChildren();
		writeMaterialNetwork(Materials.sharedNodes, "", "", parentPrim + "/sharedNodes", parentPrim + "/sharedNodes", 
								"", new HashSet<String>());
		writer.endChildren();
		writer.endNode("nodegraph");
	}

	@Override
	public void writeMaterial(MaterialTemplate material, String texture, boolean hasBiomeColor, String parentPrim,
			String sharedPrims) throws IOException {
		// MaterialX requires that when you connect to a node,
		// that that node is already defined.
		// In the material templates, you can define nodes in whatever
		// order that you want. This means that we need to do some
		// processing to change the order.
		Set<String> writtenNodes = new HashSet<String>();
		
		String suffix = "_" + Util.makeSafeName(texture) + (hasBiomeColor ? "_BIOME" : "");
		
		String matName = "MAT" + suffix;
		
		String surfaceShaderNodeName = material.shadingGroup.getOrDefault("mtlx:surface", "");
		ShadingNode surfaceShaderNode = null;
		for(MaterialNetwork network : material.networks) {
			for(ShadingNode node : network.nodes) {
				if(node.name.equals(surfaceShaderNodeName)) {
					surfaceShaderNode = node;
					break;
				}
			}
			if(surfaceShaderNode != null)
				break;
		}
		if(surfaceShaderNode == null)
			return;
		
		writer.beginNode("nodegraph");
		writer.writeAttribute("name", "NG_" + matName);
		writer.beginChildren();
		for(int i = 0; i < 1000; ++i) { // For loop instead of while loop to prevent infinite looping
			boolean skippedNodes = false;
			for(MaterialNetwork network : material.networks) {
				boolean skippedNode = writeMaterialNetwork(network, texture, suffix, parentPrim, sharedPrims, 
															surfaceShaderNodeName, writtenNodes);
				if(skippedNode)
					skippedNodes = true;
			}
			if(!skippedNodes)
				break;
		}
		
		// Write the outputs to go into the surface shader.
		for(ShadingAttribute attr : surfaceShaderNode.attributes) {
			if(attr.connection != null) {
				ShadingAttribute copy = new ShadingAttribute(attr);
				copy.name = "OUT_" + attr.connection.split("\\.")[0].split("\\[")[0] + suffix;
				writeShadingAttribute(copy, texture, suffix, parentPrim, sharedPrims, true, null);
			}
		}
		writer.endChildren();
		writer.endNode("nodegraph");
		
		// Write the surface shader
		writeShadingNode(surfaceShaderNode, texture, suffix, parentPrim, sharedPrims, "NG_" + matName);
		
		writer.beginNode("surfacematerial");
		writer.writeAttribute("name", matName);
		writer.writeAttribute("type", "material");
		writer.beginChildren();
		writer.beginNode("input");
		writer.writeAttribute("name", "surfaceshader");
		writer.writeAttribute("type", "surfaceshader");
		writer.writeAttribute("nodename", "NODE_" + matName);
		writer.endNode("input");
		writer.endChildren();
		writer.endNode("surfacematerial");
		
		usdWriter.beginDef("Material", matName);
		usdWriter.beginMetaData();
		usdWriter.writeReference("@./" + outputFile.getName() + "@</MaterialX/Materials/" + matName + ">");
		usdWriter.endMetaData();
		usdWriter.endDef();
	}

	private boolean writeMaterialNetwork(MaterialNetwork network, String texture, String suffix, String parentPrim, 
			String sharedPrims, String surfaceShaderNode, Set<String> writtenNodes) throws IOException {
		boolean skippedANode = false;
		List<String> referencedNodes = new ArrayList<String>();
		for (ShadingNode node : network.nodes) {
			if(writtenNodes.contains(node.name))
				continue; // We already handled it.
			if(node.name.equals(surfaceShaderNode))
				continue; // We place this somewhere else.
			
			referencedNodes.clear();
			node.getReferencedNodes(referencedNodes);
			boolean missingNode = false;
			for(String refNode : referencedNodes) {
				if(!writtenNodes.contains(refNode)) {
					missingNode = true;
					break;
				}
			}
			if(missingNode) {
				skippedANode = true;
				continue;
			}
			
			try {
				writeShadingNode(node, texture, suffix, parentPrim, sharedPrims, null);
			} catch (Exception ex) {
				System.out.println("Could not write node " + node.name + "for texture " + texture);
				throw ex;
			}
			writtenNodes.add(node.name);
		}
		return skippedANode;
	}

	private void writeShadingNode(ShadingNode node, String texture, String suffix, String parentPrim, String sharedPrims,
								String nodeGraph) throws IOException {
		// If it's not a MTLX node, then don't write it down.
		if (!node.type.startsWith("MTLX:"))
			return;
		
		String nodeTypeString = node.type.substring("MTLX:".length());

		String[] tokens = nodeTypeString.split("\\.");
		if (tokens.length != 2)
			return; // Invalid type name
		String nodeType = tokens[0];
		String typeAttr = tokens[1];

		writer.beginNode(nodeType);
		writer.writeAttribute("name", "NODE_" + node.name + suffix);
		writer.writeAttribute("type", typeAttr);

		writer.beginChildren();
		for (ShadingAttribute attr : node.attributes) {
			writeShadingAttribute(attr, texture, suffix, parentPrim, sharedPrims, false, nodeGraph);
		}
		writer.endChildren();
		writer.endNode(nodeType);

		_hasWrittenAnything = true;
	}

	private static final Map<String, String> USD_TYPE_TO_MTLX_TYPE = new HashMap<String, String>();
	static {
		USD_TYPE_TO_MTLX_TYPE.put("float2", "vector2");
		USD_TYPE_TO_MTLX_TYPE.put("float3", "vector3");
		USD_TYPE_TO_MTLX_TYPE.put("float4", "vector4");
		USD_TYPE_TO_MTLX_TYPE.put("color3f", "color3");
		USD_TYPE_TO_MTLX_TYPE.put("color4f", "color4");
		USD_TYPE_TO_MTLX_TYPE.put("asset", "filename");
	}
	
	private void writeShadingAttribute(ShadingAttribute attr, String texture, String suffix, String parentPrim, String sharedPrims,
										boolean isOutput, String nodeGraph)
			throws IOException {
		writer.beginNode(isOutput ? "output" : "input");
		
		String typeString = USD_TYPE_TO_MTLX_TYPE.getOrDefault(attr.type, attr.type);
		writer.writeAttribute("name", attr.name);
		writer.writeAttribute("type", typeString);
		
		if (attr.expression != null) {
			writeExpressionValue(texture, attr.expression, attr.type);
		} else if (attr.connection != null) {
			String connString = attr.connection;
			String channelString = null;
			if(connString.contains("[")) {
				String tokens[] = connString.split("\\[");
				connString = tokens[0];
				if(tokens.length > 1)
					channelString = tokens[1].substring(0, tokens[1].length()-1);
			}
			String tokens[] = connString.split("\\.");
			String nodeName = "NODE_" + tokens[0] + suffix;
			String outputName = null;
			if(tokens.length > 1)
				outputName = tokens[1];
			
			if(nodeGraph != null) {
				writer.writeAttribute("nodegraph", nodeGraph);
				outputName = "OUT_" + tokens[0] + suffix;
			}else {
				writer.writeAttribute("nodename", nodeName);
			}
			if(outputName != null)
				writer.writeAttribute("output", outputName);
			if(channelString != null)
				writer.writeAttribute("channels", channelString);
		} else if (attr.value != null) {
			if(attr.value instanceof ArrayList<?>) {
				StringBuilder sb = new StringBuilder();
				ArrayList<?> list = (ArrayList<?>) attr.value;
				if(list.size() > 0) {
					for(int i = 0; i < list.size() - 1; ++i) {
						sb.append(String.valueOf(list.get(i)) + ", ");
					}
					sb.append(String.valueOf(list.get(list.size()-1)));
				}
				writer.writeAttribute("value", sb.toString());
			}else {
				if(typeString.equals("filename")) {
					String strValue = String.valueOf(attr.value);
					strValue = strValue.replace("@texture@", texture);
					strValue = getAssetPathForTexture(strValue);
					writer.writeAttribute("value", strValue);
				}else {
					writer.writeAttribute("value", String.valueOf(attr.value));
				}
			}
		}
		
		writer.endNode(isOutput ? "output" : "input");
	}
	
	private void writeExpressionValue(String texture, String expression, String attrType) throws IOException{
		// Get rid of the '${' and '}'
		expression = expression.substring(2, expression.length()-1);
		// Split the expression from the arguments
		String[] exprTokens = expression.split("\\(");
		expression = exprTokens[0].trim();
		Map<String, String> args = new HashMap<String, String>();
		if(exprTokens.length > 1) {
			// The split got rid of the opening '(', so here we get rid of the closing ')'.
			String argsString = exprTokens[1].substring(0, exprTokens[1].length()-1);
			String[] argsList = argsString.split(",");
			for(String arg : argsList) {
				String[] argTokens = arg.split("=");
				if(argTokens.length == 2) {
					// Put this arg in the map
					// Also strip the white spaces and set it to lower case
					// to make it easier to match different ways of writing
					// names and values.
					args.put(argTokens[0].trim().toLowerCase(), argTokens[1].trim().toLowerCase());
				}
			}
		}
		
		// It's possible that we want the animation data of some other texture,
		// so let's figure that out.
		expression = expression.replace("@texture@", texture);
		if(expression.contains(".")) {
			String[] tokens = expression.split("\\.");
			if(tokens.length == 2) {
				texture = tokens[0];
				expression = tokens[1];
			}
		}
				
		MCMeta animData = ResourcePacks.getMCMeta(texture);
		if(animData == null)
			animData = new MCMetaJavaEdition(null, null);
		
		// Because time samples aren't being looped,
		// we need to specify time frames for a very large range,
		// to make sure that it works in pretty much every scene.
		float startFrame = 0.0f;
		float endFrame = 10000.0f;
		float frameTimeMultiplier = Config.animatedTexturesFrameTimeMultiplier;
		boolean isFloat2 = attrType.equals("float2") || attrType.equals("half2") || attrType.equals("double2");
		
		if(expression.equalsIgnoreCase("frameId")) {
			List<Float> values = new ArrayList<Float>();
			Materials.getFrameIdSamplesPerFrame(values, startFrame, endFrame, 
										animData, frameTimeMultiplier, isFloat2, args);
			
			writer.writeAttribute("valuerange", Integer.toString((int) startFrame) + ", " + Integer.toString((int) endFrame));
			writer.writeAttributeArray("valuecurve", values);
		}else if(expression.equalsIgnoreCase("frameScale")) {
			float scale = (float) animData.getFrameCount();
			
			if(args.getOrDefault("powerof2", "false").equals("true"))
				scale /= animData.getPowerOfTwoScaleCompensation();
			
			if(args.getOrDefault("inverse", "false").equals("true"))
				scale = 1.0f / scale;
			
			if(!isFloat2)
				writer.writeAttribute("value", Float.toString(scale));
			else
				writer.writeAttribute("value", "1.0, " + Float.toString(scale));
		}else if(expression.equalsIgnoreCase("interpFactor")) {
			List<Float> values = new ArrayList<Float>();
			int i = 0;
			for(float timeCode = startFrame; timeCode <= endFrame;) {
				int frameIndex = i % (animData.getFrames().length/2);
				int frameTime = animData.getFrames()[frameIndex*2 + 1];
				float frameTimeF = ((float) frameTime) * frameTimeMultiplier;
				
				// We are creating a sawtooth graph here,
				// where it smoothly goes up from 0.0 to 1.0,
				// and then immediately restarts at 0.0
				// At the time it restarts, the frameIds also
				// jump to the next frame. Because this
				// all happens at the same time, it looks like
				// it's just smoothly interpolates throughout
				// the frames rather than jumping.
				
				for(int j = 0; j < frameTimeF; ++j) {
					values.add(((float)j) / frameTimeF);
				}
				
				timeCode += frameTimeF;
				i++;
			}
			
			writer.writeAttribute("valuerange", Integer.toString((int) startFrame) + ", " + Integer.toString((int) endFrame));
			writer.writeAttributeArray("valuecurve", values);
		}
	}
	
	private String getAssetPathForTexture(String texture) {
		try {
			File file = Materials.getTextureFile(texture, USDConverter.currentOutputDir.getCanonicalPath());
			if(file == null || !file.exists()) {
				World.handleError(new RuntimeException("Missing texture " + texture));
				return texture;
			}
			String fullPath = file.getCanonicalPath().replace('\\', '/');
			String resourcePathDir = new File(FileUtil.getResourcePackDir()).getCanonicalPath().replace('\\', '/');
			if(!resourcePathDir.endsWith("/"))
				resourcePathDir = resourcePathDir + "/";
			
			String outputDir = USDConverter.currentOutputDir.getCanonicalPath().replace('\\', '/');
			if(!outputDir.endsWith("/"))
				outputDir = outputDir + "/";
			
			if(fullPath.startsWith(resourcePathDir)) {
				String relativePath = fullPath.substring(resourcePathDir.length());
				return FileUtil.getResourcePackMTLXPrefix() + relativePath;
			}
			if(fullPath.startsWith(outputDir)) {
				String relativePath = fullPath.substring(outputDir.length());
				return "./" + relativePath;
			}
			
			System.out.println("Texture isn't located in resource path dir or output dir");
			System.out.println(fullPath + " : " + resourcePathDir);
			return texture;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return texture;
	}

}
