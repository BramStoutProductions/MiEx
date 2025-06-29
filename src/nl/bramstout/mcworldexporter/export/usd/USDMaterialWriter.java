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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.iq80.leveldb.shaded.guava.io.Files;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.export.Exporter;
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

public class USDMaterialWriter extends MaterialWriter{
	
	private USDWriter writer;
	private boolean _hasWrittenAnything;
	
	public USDMaterialWriter(File outputFile) {
		super(outputFile);
		_hasWrittenAnything = false;
	}
	
	@Override
	public String getUSDAssetPath() {
		return "./" + outputFile.getName();
	}
	
	@Override
	public void open() throws IOException {
		writer = new USDWriter(outputFile);
		writer.beginMetaData();
		writer.writeMetaDataString("defaultPrim", "materials");
		writer.endMetaData();
		writer.beginDef("Scope", "materials");
		writer.beginChildren();
	}
	
	@Override
	public void close() throws IOException {
		writer.endChildren();
		writer.endDef();
		writer.close(!_hasWrittenAnything);
	}
	
	@Override
	public boolean hasWrittenAnything() {
		return this._hasWrittenAnything;
	}
	
	@Override
	public void writeSharedNodes(String parentPrim) throws IOException {
		if(Materials.sharedNodes.nodes.isEmpty())
			return;
		writer.beginDef("NodeGraph", "sharedNodes");
		writer.beginChildren();
		writeMaterialNetwork(Materials.sharedNodes, "", parentPrim + "/sharedNodes", parentPrim + "/sharedNodes");
		writer.endChildren();
		writer.endDef();
	}
	
	@Override
	public void writeMaterial(MaterialTemplate material, String texture, boolean hasBiomeColor,
										String parentPrim, String sharedPrims) throws IOException{
		String matName = getMaterialName(texture, material, hasBiomeColor);
		writer.beginDef("Material", matName);
		writer.beginChildren();
		for(Entry<String, String> conn : material.shadingGroup.entrySet()) {
			if(conn.getKey().startsWith("json:"))
				continue; // Ignore JSON specific terminals
			String connPath = "";
			if(conn.getValue().startsWith("shared/")) {
				connPath = sharedPrims + "/" + conn.getValue().substring(7);
			}else {
				String[] tokens = conn.getValue().split("\\.");
				connPath = parentPrim + "/" + matName + "/" + tokens[0] + "_" + Util.makeSafeName(texture);
				for(int i = 1; i < tokens.length; ++i)
					connPath += "." + tokens[i];
			}
			writer.writeAttributeName("token", "outputs:" + conn.getKey(), false);
			writer.writeAttributeConnection(connPath);
		}
		for(MaterialNetwork network : material.networks)
			writeMaterialNetwork(network, texture, parentPrim + "/" + matName, sharedPrims);
		writer.endChildren();
		writer.endDef();
	}
	
	private void writeMaterialNetwork(MaterialNetwork network, String texture, 
											String parentPrim, String sharedPrims) throws IOException {
		for(ShadingNode node : network.nodes) {
			try {
				writeShadingNode(node, texture, parentPrim, sharedPrims);
			}catch(Exception ex) {
				System.out.println("Could not write node " + node.name + "for texture " + texture);
				throw ex;
			}
		}
	}
	
	private void writeShadingNode(ShadingNode node, String texture, 
										String parentPrim, String sharedPrims) throws IOException {
		// If it's not a USD node, then don't write it down.
		if(node.type == null)
			return;
		if(node.type.contains(":") && !node.type.startsWith("USD:"))
			return;
		_hasWrittenAnything = true;
		writer.beginDef("Shader", node.name + "_" + Util.makeSafeName(texture));
		writer.beginChildren();
		writer.writeAttributeName("token", "info:id", true);
		if(node.type.startsWith("USD:"))
			writer.writeAttributeValueString(node.type.substring("USD:".length()));
		else
			writer.writeAttributeValueString(node.type);
		for(ShadingAttribute attr : node.attributes) {
			writeShadingAttribute(attr, texture, parentPrim, sharedPrims);
		}
		writer.endChildren();
		writer.endDef();
	}
	
	private void writeShadingAttribute(ShadingAttribute attr, String texture,
											String parentPrim, String sharedPrims) throws IOException{
		writer.writeAttributeName(attr.type, attr.name, false);
		if(attr.expression != null) {
			writeExpressionValue(texture, attr.expression, attr.type);
		}else if(attr.connection != null) {
			String connPath = "";
			if(attr.connection.startsWith("shared/")) {
				connPath = sharedPrims + "/" + attr.connection.substring(7);
			}else {
				String[] tokens = attr.connection.split("\\.");
				connPath = parentPrim + "/" + tokens[0] + "_" + Util.makeSafeName(texture);
				for(int i = 1; i < tokens.length; ++i)
					connPath += "." + tokens[i];
			}
			writer.writeAttributeConnection(connPath);
		}else if(attr.value != null) {
			if(attr.value instanceof Boolean)
				writer.writeAttributeValueBoolean((Boolean) attr.value);
			else if(attr.value instanceof String) {
				if(attr.type.equals("asset")) {
					String strValue = (String) attr.value;
					strValue = strValue.replace("@texture@", texture);
					strValue = getAssetPathForTexture(strValue);
					writer.writeAttributeValuePath(strValue);
				}else {
					writer.writeAttributeValueString((String) attr.value);
				}
			}else if(attr.value instanceof Float)
				writer.writeAttributeValueFloat((Float) attr.value);
			else if(attr.value instanceof ArrayList<?>) {
				ArrayList<?> list = (ArrayList<?>) attr.value;
				if(attr.type.contains("[]")) {
					if(list.isEmpty())
						writer.writeAttributeValueStringArray(new String[] {});
					else {
						if(list.get(0) instanceof String) {
							String[] array = new String[list.size()];
							list.toArray(array);
							writer.writeAttributeValueStringArray(array);
						}else if(list.get(0) instanceof Float) {
							float[] array = new float[list.size()];
							for(int i = 0; i < list.size(); ++i)
								array[i] = (Float) list.get(i);
							writer.writeAttributeValueFloatArray(array);
						}
					}
				}else {
					if(!list.isEmpty()) {
						if(list.get(0) instanceof Float) {
							float[] array = new float[list.size()];
							for(int i = 0; i < list.size(); ++i)
								array[i] = (Float) list.get(i);
							writer.writeAttributeValueFloatCompound(array);
						}
					}
				}
			}
		}
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
			List<Float> timeCodes = new ArrayList<Float>();
			List<Float> values = new ArrayList<Float>();
			Materials.getFrameIdSamples(timeCodes, values, startFrame, endFrame, 
								 		animData, frameTimeMultiplier, isFloat2, args);
			
			if(isFloat2) {
				writer.writeAttributeValueTimeSamplesFloatCompound(timeCodes, values, 2);
			}else {
				writer.writeAttributeValueTimeSamplesFloat(timeCodes, values);
			}
		}else if(expression.equalsIgnoreCase("frameScale")) {
			float scale = (float) animData.getFrameCount();
			
			if(args.getOrDefault("powerof2", "false").equals("true"))
				scale /= animData.getPowerOfTwoScaleCompensation();
			
			if(args.getOrDefault("inverse", "false").equals("true"))
				scale = 1.0f / scale;
			
			if(!isFloat2)
				writer.writeAttributeValueFloat(scale);
			else
				writer.writeAttributeValueFloatCompound(new float[] { 1.0f, scale });
		}else if(expression.equalsIgnoreCase("interpFactor")) {
			List<Float> timeCodes = new ArrayList<Float>();
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
				
				timeCodes.add(timeCode - 0.301f);
				if(isFloat2) {
					values.add(0.0f);
					values.add(1.0f);
				}else {
					values.add(1.0f);
				}
				
				timeCodes.add(timeCode - 0.3f);
				if(isFloat2) {
					values.add(0.0f);
					values.add(0.0f);
				}else {
					values.add(0.0f);
				}
				
				timeCode += frameTimeF;
				i++;
			}
			
			if(isFloat2) {
				writer.writeAttributeValueTimeSamplesFloatCompound(timeCodes, values, 2);
			}else {
				writer.writeAttributeValueTimeSamplesFloat(timeCodes, values);
			}
		}else if(expression.equalsIgnoreCase("biomeColor")) {
			Color color = Materials.getBiomeColor(args);
			if(color == null)
				color = new Color();
			float[] data = null;
			String attrTypeLower = attrType.toLowerCase();
			if(attrTypeLower.contains("2"))
				data = new float[] { color.getR(), color.getG() };
			else if(attrTypeLower.contains("3"))
				data = new float[] { color.getR(), color.getG(), color.getB() };
			else if(attrTypeLower.contains("4"))
				data = new float[] { color.getR(), color.getG(), color.getB(), color.getA() };
			else if(attrTypeLower.contains("RGBA"))
				data = new float[] { color.getR(), color.getG(), color.getB(), color.getA() };
			else if(attrTypeLower.contains("RGB"))
				data = new float[] { color.getR(), color.getG(), color.getB() };
			else if(attrTypeLower.contains("color"))
				data = new float[] { color.getR(), color.getG(), color.getB() };
			else
				data = new float[] { color.getR() };
			
			if(data.length == 1)
				writer.writeAttributeValueFloat(data[0]);
			else
				writer.writeAttributeValueFloatCompound(data);
		}else if(expression.equalsIgnoreCase("animData")) {
			String animDataStr = Materials.getAnimationData(animData, frameTimeMultiplier);
			writer.writeAttributeValueString(animDataStr);
		}
	}
	
	private String getAssetPathForTexture(String texture) {
		try {
			File file = Materials.getTextureFile(texture, Exporter.currentExportFile.getParentFile().getCanonicalPath());
			if(file == null || !file.exists()) {
				World.handleError(new RuntimeException("Missing texture " + texture));
				return texture;
			}
			
			String fullPath = file.getCanonicalPath().replace('\\', '/');
			String resourcePathDir = new File(FileUtil.getResourcePackDir()).getCanonicalPath().replace('\\', '/');
			if(!resourcePathDir.endsWith("/"))
				resourcePathDir = resourcePathDir + "/";
			
			String outputDir = Exporter.currentExportFile.getParentFile().getCanonicalPath().replace('\\', '/');
			if(!outputDir.endsWith("/"))
				outputDir = outputDir + "/";
			
			if(fullPath.startsWith(resourcePathDir)) {
				String relativePath = fullPath.substring(resourcePathDir.length());
				
				if(MCWorldExporter.portableExports) {
					File outFile = new File(Exporter.chunksFolder, "resources/" + relativePath);
					File outFolder = outFile.getParentFile();
					if(!outFolder.exists())
						outFolder.mkdirs();
					try {
						Files.copy(file, outFile);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					return "./" + Exporter.chunksFolder.getName() + "/resources/" + relativePath;
				}
				
				return FileUtil.getResourcePackUSDPrefix() + relativePath;
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
