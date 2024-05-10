package nl.bramstout.mcworldexporter.export.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Util;
import nl.bramstout.mcworldexporter.export.usd.USDConverter;
import nl.bramstout.mcworldexporter.materials.MaterialWriter;
import nl.bramstout.mcworldexporter.materials.Materials;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialNetwork;
import nl.bramstout.mcworldexporter.materials.Materials.MaterialTemplate;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingAttribute;
import nl.bramstout.mcworldexporter.materials.Materials.ShadingNode;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;

public class JsonMaterialWriter extends MaterialWriter {

	private JsonObject root;
	private boolean _hasWrittenAnything;

	public JsonMaterialWriter(File outputFile) {
		super(outputFile);
		_hasWrittenAnything = false;
	}

	@Override
	public void open() throws IOException {
		root = new JsonObject();
	}

	@Override
	public void close() throws IOException {
		if (!hasWrittenAnything())
			return;

		FileWriter writer = null;
		try {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			String jsonString = gson.toJson(root);
			writer = new FileWriter(outputFile);
			writer.write(jsonString);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (writer != null) {
			try {
				writer.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public boolean hasWrittenAnything() {
		return this._hasWrittenAnything;
	}

	@Override
	public String getUSDAssetPath() {
		return null; // Return null because we don't want a json file to be referenced into a USD file.
	}

	@Override
	public void writeSharedNodes(String parentPrim) throws IOException {
		if (Materials.sharedNodes.nodes.isEmpty())
			return;
		JsonObject sharedNodesObj = new JsonObject();

		writeMaterialNetwork(Materials.sharedNodes, sharedNodesObj, "", parentPrim + "/sharedNodes",
				parentPrim + "/sharedNodes");

		root.add("sharedNodes", sharedNodesObj);
	}

	@Override
	public void writeMaterial(MaterialTemplate material, String texture, boolean hasBiomeColor, String parentPrim,
			String sharedPrims) throws IOException {
		JsonObject matObj = new JsonObject();

		String matName = "MAT_"
				+ Util.makeSafeName(texture)
				+ (hasBiomeColor ? "_BIOME" : "");

		JsonObject terminalsObj = new JsonObject();

		for (Entry<String, String> conn : material.shadingGroup.entrySet()) {
			String connPath = "";
			if (conn.getValue().startsWith("shared/")) {
				connPath = sharedPrims + "/" + conn.getValue().substring(6);
			} else {
				String[] tokens = conn.getValue().split("\\.");
				connPath = parentPrim + "/" + matName + "/" + tokens[0] + "_" + Util.makeSafeName(texture);
				for (int i = 1; i < tokens.length; ++i)
					connPath += "." + tokens[i];
			}
			String terminal = conn.getKey();
			if(terminal.startsWith("json:")) {
				terminal = terminal.substring("json:".length());
				terminalsObj.addProperty(terminal, connPath);
			}
		}
		matObj.add("terminals", terminalsObj);

		JsonObject networkObj = new JsonObject();
		for (MaterialNetwork network : material.networks)
			writeMaterialNetwork(network, networkObj, texture, parentPrim + "/" + matName, sharedPrims);
		matObj.add("network", networkObj);

		root.add(matName, matObj);
	}

	private void writeMaterialNetwork(MaterialNetwork network, JsonObject networkObj, String texture, 
										String parentPrim, String sharedPrims)
			throws IOException {
		for (ShadingNode node : network.nodes) {
			try {
				writeShadingNode(node, networkObj, texture, parentPrim, sharedPrims);
			} catch (Exception ex) {
				System.out.println("Could not write node " + node.name + "for texture " + texture);
				throw ex;
			}
		}
	}
	
	private void writeShadingNode(ShadingNode node, JsonObject networkObj, String texture, String parentPrim, String sharedPrims)
			throws IOException {
		// If it's not a JSON node, then don't write it down.
		if (!node.type.startsWith("JSON:"))
			return;
		_hasWrittenAnything = true;
		
		String nodeTypeString = node.type.substring("JSON:".length());
		
		JsonObject shaderObj = new JsonObject();
		shaderObj.addProperty("type", nodeTypeString);
		
		JsonObject attrsObj = new JsonObject();
		for (ShadingAttribute attr : node.attributes) {
			writeShadingAttribute(attr, attrsObj, texture, parentPrim, sharedPrims);
		}
		shaderObj.add("attributes", attrsObj);
		
		networkObj.add(node.name + "_" + Util.makeSafeName(texture), shaderObj);
	}
	
	private void writeShadingAttribute(ShadingAttribute attr, JsonObject attrsObj, String texture, 
										String parentPrim, String sharedPrims) throws IOException {
		JsonObject attrObj = new JsonObject();
		attrObj.addProperty("type", attr.type);
		if (attr.expression != null) {
			writeExpressionValue(texture, attrObj, attr.expression, attr.type);
		} else if (attr.connection != null) {
			String connPath = "";
			if (attr.connection.startsWith("shared/")) {
				connPath = sharedPrims + "/" + attr.connection.substring(6);
			} else {
				String[] tokens = attr.connection.split("\\.");
				connPath = parentPrim + "/" + tokens[0] + "_" + Util.makeSafeName(texture);
				for (int i = 1; i < tokens.length; ++i)
					connPath += "." + tokens[i];
			}
			attrObj.addProperty("connection", connPath);
		} else if (attr.value != null) {
			if (attr.value instanceof Boolean)
				attrObj.addProperty("value", (Boolean) attr.value);
			else if (attr.value instanceof String) {
				if (attr.type.equals("asset")) {
					String strValue = (String) attr.value;
					strValue = strValue.replace("@texture@", texture);
					strValue = getAssetPathForTexture(strValue);
					attrObj.addProperty("value", strValue);
				} else {
					attrObj.addProperty("value", (String) attr.value);
				}
			} else if (attr.value instanceof Float)
				attrObj.addProperty("value", (Number) attr.value);
			else if (attr.value instanceof ArrayList<?>) {
				JsonArray arrayObj = new JsonArray();
				ArrayList<?> list = (ArrayList<?>) attr.value;
				for(Object val : list) {
					if(val instanceof Float)
						arrayObj.add((Number) val);
					else if(val instanceof String)
						arrayObj.add((String) val);
				}
				attrObj.add("value", arrayObj);
			}
		}
		attrsObj.add(attr.name, attrObj);
	}
	
	private void writeExpressionValue(String texture, JsonObject attrObj, String expression, String attrType) throws IOException{
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
				
		MCMeta animData = new MCMeta(texture);
		
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
			
			writeKeyframes(attrObj, timeCodes, values, isFloat2 ? 2 : 1);
		}else if(expression.equalsIgnoreCase("frameScale")) {
			float scale = (float) animData.getFrameCount();
			
			if(args.getOrDefault("powerof2", "false").equals("true"))
				scale /= animData.getPowerOfTwoScaleCompensation();
			
			if(args.getOrDefault("inverse", "false").equals("true"))
				scale = 1.0f / scale;
			
			if(!isFloat2)
				attrObj.addProperty("value", scale);
			else {
				JsonArray valueObj = new JsonArray();
				valueObj.add(1.0f);
				valueObj.add(scale);
				attrObj.add("value", valueObj);
			}
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
			
			writeKeyframes(attrObj, timeCodes, values, isFloat2 ? 2 : 1);
		}
	}
	
	private void writeKeyframes(JsonObject attrObj, List<Float> timeCodes, List<Float> values, int numComponents) {
		JsonArray keyframesObj = new JsonArray();
		int valuesI = 0;
		for(int i = 0; i < Math.min(timeCodes.size(), values.size()); ++i) {
			keyframesObj.add(timeCodes.get(i));
			if(numComponents == 1) {
				keyframesObj.add(values.get(valuesI));
			}else {
				JsonArray valueObj = new JsonArray();
				for(int j = 0; j < numComponents; ++j) {
					valueObj.add(values.get(valuesI + j));
				}
				keyframesObj.add(valueObj);
			}
			valuesI += numComponents;
		}
		attrObj.add("keyframes", keyframesObj);
	}
	
	private String getAssetPathForTexture(String texture) {
		try {
			File file = Materials.getTextureFile(texture, USDConverter.currentOutputDir.getCanonicalPath());
			if(!file.exists())
				return texture;
			String fullPath = file.getCanonicalPath().replace('\\', '/');
			String resourcePathDir = new File(FileUtil.getResourcePackDir()).getCanonicalPath().replace('\\', '/');
			if(!resourcePathDir.endsWith("/"))
				resourcePathDir = resourcePathDir + "/";
			
			String outputDir = USDConverter.currentOutputDir.getCanonicalPath().replace('\\', '/');
			if(!outputDir.endsWith("/"))
				outputDir = outputDir + "/";
			
			if(fullPath.startsWith(resourcePathDir)) {
				String relativePath = fullPath.substring(resourcePathDir.length());
				return FileUtil.getResourcePackJSONPrefix() + relativePath;
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
