package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextureRandom.Symmetry;

public class OptifineLoader extends ConnectedTexturesLoader{

	@Override
	public void load() {
		List<String> resourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
		resourcePacks.add("base_resource_pack");
		
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File assetsFolder = new File(FileUtil.getResourcePackDir(), resourcePacks.get(i) + "/assets");
			if(!assetsFolder.exists() || !assetsFolder.isDirectory())
				continue;
			
			for(File namespace : assetsFolder.listFiles())
				processNamespace(namespace, resourcePacks.get(i));
		}
	}
	
	private void processNamespace(File namespaceFolder, String resourcePackName) {
		File optifineCTMFolder = new File(namespaceFolder, "optifine/ctm");
		if(!optifineCTMFolder.exists() || !optifineCTMFolder.isDirectory())
			return;
		
		processFolder(optifineCTMFolder, "optifine;" + namespaceFolder.getName(), "ctm/", resourcePackName);
	}
	
	private void processFolder(File folder, String namespace, String parent, String resourcePackName) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				processFolder(f, namespace, parent + f.getName() + "/", resourcePackName);
			}else if(f.isFile() && f.getName().endsWith(".properties")){
				processFile(f, folder, namespace, parent, resourcePackName);
			}
		}
	}
	
	private void processFile(File f, File folder, String namespace, String parent, String resourcePackName) {
		String matchTiles = null;
		String matchBlocks = null;
		if(f.getName().startsWith("block")) {
			matchBlocks = f.getName().split("\\.")[0].substring("block_".length());
		}else {
			matchTiles = f.getName().split("\\.")[0];
		}
		
		
		String method = null;
		String tiles = null;
		String weight = null;
		String connect = null;
		String faces = null;
		String biomes = null;
		String heights = null;
		Map<String, String> compactReplacements = new HashMap<String, String>();
		String innerSeams = null;
		String weights = null;
		String symmetry = null;
		String linked = null;
		String width = null;
		String height = null;
		String connectTiles = null;
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(f));
			
			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				String[] tokens = line.split("=");
				if(tokens.length != 2)
					continue;
				String key = tokens[0].trim();
				String value = tokens[1].trim();
				if(key.equalsIgnoreCase("method"))
					method = value;
				else if(key.equalsIgnoreCase("tiles"))
					tiles = value;
				else if(key.equalsIgnoreCase("matchTiles"))
					matchTiles = value;
				else if(key.equalsIgnoreCase("matchBlocks"))
					matchBlocks = value;
				else if(key.equalsIgnoreCase("weight"))
					weight = value;
				else if(key.equalsIgnoreCase("connect"))
					connect = value;
				else if(key.equalsIgnoreCase("faces"))
					faces = value;
				else if(key.equalsIgnoreCase("biomes"))
					biomes = value;
				else if(key.equalsIgnoreCase("heights"))
					heights = value;
				else if(key.startsWith("ctm."))
					compactReplacements.put(key.substring("ctm.".length()), value);
				else if(key.equalsIgnoreCase("innerSeams"))
					innerSeams = value;
				else if(key.equalsIgnoreCase("weights"))
					weights = value;
				else if(key.equalsIgnoreCase("symmetry"))
					symmetry = value;
				else if(key.equalsIgnoreCase("linked"))
					linked = value;
				else if(key.equalsIgnoreCase("width"))
					width = value;
				else if(key.equalsIgnoreCase("height"))
					height = value;
				else if(key.equalsIgnoreCase("connectTiles"))
					connectTiles = value;
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		try {
			if(reader != null)
				reader.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		
		int priority = 0;
		if(weight != null) {
			try {
				priority = Integer.parseInt(weight);
			}catch(Exception ex) {}
		}
		
		ConnectedTexture connectedTexture = null;
		if(method.equalsIgnoreCase("ctm")) {
			connectedTexture = new ConnectedTextureFull(parent + f.getName(), priority);
			if(innerSeams != null) {
				if(innerSeams.equalsIgnoreCase("true"))
					((ConnectedTextureFull)connectedTexture).setInnerSeams(true);
			}
		}else if(method.equalsIgnoreCase("ctm_compact")) {
			connectedTexture = new ConnectedTextureFull(parent + f.getName(), priority);
			if(innerSeams != null) {
				if(innerSeams.equalsIgnoreCase("true"))
					((ConnectedTextureFull)connectedTexture).setInnerSeams(true);
			}
		}else if(method.equalsIgnoreCase("horizontal")) {
			connectedTexture = new ConnectedTextureHorizontal(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("vertical")) {
			connectedTexture = new ConnectedTextureVertical(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("horizontal+vertical")) {
			// TODO: Implement
		}else if(method.equalsIgnoreCase("vertical+horizontal")) {
			// TODO: Implement
		}else if(method.equalsIgnoreCase("top")) {
			connectedTexture = new ConnectedTextureTop(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("random")) {
			connectedTexture = new ConnectedTextureRandom(parent + f.getName(), priority);
			if(symmetry != null) {
				if(symmetry.equalsIgnoreCase("none"))
					((ConnectedTextureRandom)connectedTexture).setSymmetry(Symmetry.NONE);
				else if(symmetry.equalsIgnoreCase("opposite"))
					((ConnectedTextureRandom)connectedTexture).setSymmetry(Symmetry.OPPOSITE);
				else if(symmetry.equalsIgnoreCase("all"))
					((ConnectedTextureRandom)connectedTexture).setSymmetry(Symmetry.ALL);
			}
			if(linked != null) {
				if(linked.equalsIgnoreCase("true"))
					((ConnectedTextureRandom)connectedTexture).setLinked(true);
			}
		}else if(method.equalsIgnoreCase("repeat")) {
			connectedTexture = new ConnectedTextureRepeat(parent + f.getName(), priority);
			if(width != null) {
				try {
					Integer widthVal = Integer.parseInt(width);
					((ConnectedTextureRepeat)connectedTexture).setWidth(widthVal.intValue());
				}catch(Exception ex) {}
			}
			if(height != null) {
				try {
					Integer heightVal = Integer.parseInt(height);
					((ConnectedTextureRepeat)connectedTexture).setHeight(heightVal.intValue());
				}catch(Exception ex) {}
			}
		}else if(method.equalsIgnoreCase("fixed")) {
			connectedTexture = new ConnectedTextureFixed(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("overlay")) {
			connectedTexture = new ConnectedTextureOverlay(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("overlay_ctm")) {
			connectedTexture = new ConnectedTextureFull(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("overlay_random")) {
			connectedTexture = new ConnectedTextureRandom(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("overlay_repeat")) {
			connectedTexture = new ConnectedTextureRepeat(parent + f.getName(), priority);
		}else if(method.equalsIgnoreCase("overlay_fixed")) {
			connectedTexture = new ConnectedTextureFixed(parent + f.getName(), priority);
		}
		
		if(connectedTexture == null)
			return;
		
		if(tiles != null) {
			String[] tilesTokens = tiles.split("[ ,]");
			for(String token : tilesTokens) {
				if(token.contains("-")) {
					// It's a range of values
					String[] rangeTokens = token.split("-");
					if(rangeTokens.length != 2)
						continue;
					try {
						Integer startI = Integer.parseInt(rangeTokens[0]);
						Integer endI = Integer.parseInt(rangeTokens[1]);
						for(int i = startI.intValue(); i <= endI.intValue(); ++i) {
							connectedTexture.getTiles().add(namespace + ":" + parent + String.valueOf(i));
						}
					}catch(Exception ex) {}
				}else if(token.equalsIgnoreCase("<skip>") || token.equalsIgnoreCase("<default>")){
					connectedTexture.getTiles().add(null); // null means default texture
				}else if(token.contains("/")){
					// It's a full path
					String[] pathTokens = token.split("\\/");
					String type = pathTokens[0];
					String path = token.substring(type.length() + 1);
					String resourceName = type + ";" + namespace.split(";")[1] + ":" + path;
					connectedTexture.getTiles().add(resourceName);
				}else {
					// It's a file in this folder
					String name = token;
					if(name.endsWith(".png"))
						name = name.substring(0, name.length() - 4);
					connectedTexture.getTiles().add(namespace + ":" + parent + name);
				}
			}
		}
		
		if(method.equalsIgnoreCase("ctm_compact")) {
			// We need to convert the compact tiles into full tiles.
			List<String> fullTiles = new ArrayList<String>();
			try {
				CtmUtils.createFullTilesFromCompact(connectedTexture.getTiles(), 
						namespace + ":" + parent + f.getName().split("\\.")[0] + "_", 
						fullTiles, resourcePackName);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			// Handle the compact replacements
			for(Entry<String, String> compactReplacement : compactReplacements.entrySet()) {
				try {
					Integer tileIndex = Integer.valueOf(compactReplacement.getKey());
					if(tileIndex < 0 || tileIndex >= fullTiles.size())
						continue;
					Integer replaceTileIndex = Integer.valueOf(compactReplacement.getValue());
					if(replaceTileIndex < 0 || replaceTileIndex >= connectedTexture.getTiles().size())
						continue;
					
					// Update the tile
					fullTiles.set(tileIndex.intValue(), connectedTexture.getTiles().get(replaceTileIndex.intValue()));
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			// Update the tile list to the full list
			connectedTexture.getTiles().clear();
			connectedTexture.getTiles().addAll(fullTiles);
		}
		
		ConnectLogic connectLogic = null;
		if(matchBlocks != null)
			connectLogic = new ConnectLogic.ConnectLogicSameBlock();
		else if(matchTiles != null)
			connectLogic = new ConnectLogic.ConnectLogicSameTile();
		if(connect != null) {
			if(connect.equalsIgnoreCase("block"))
				connectLogic = new ConnectLogic.ConnectLogicSameBlock();
			else if(connect.equalsIgnoreCase("tile"))
				connectLogic = new ConnectLogic.ConnectLogicSameTile();
			else if(connect.equalsIgnoreCase("state"))
				connectLogic = new ConnectLogic.ConnectLogicSameState();
		}
		if(connectTiles != null) {
			String[] tilesTokens = connectTiles.split("[ ,]");
			connectLogic = new ConnectLogic.ConnectLogicTextures();
			for(String tile : tilesTokens) {
				if(!tile.contains(":"))
					tile = "minecraft:" + tile;
				((ConnectLogic.ConnectLogicTextures)connectLogic).textures.add(tile);
			}
		}
		connectedTexture.setConnectLogic(connectLogic);
		
		if(faces != null) {
			String[] facesTokens = faces.split("[ ,]");
			for(String face : facesTokens) {
				if(face.equalsIgnoreCase("all")) {
					for(Direction dir : Direction.CACHED_VALUES)
						connectedTexture.getFacesToConnect().add(dir);
				}else if(face.equalsIgnoreCase("sides")) {
					connectedTexture.getFacesToConnect().add(Direction.NORTH);
					connectedTexture.getFacesToConnect().add(Direction.SOUTH);
					connectedTexture.getFacesToConnect().add(Direction.EAST);
					connectedTexture.getFacesToConnect().add(Direction.WEST);
				}else {
					try {
						connectedTexture.getFacesToConnect().add(Direction.getDirection(face));
					}catch(Exception ex) {}
				}
			}
		}else {
			for(Direction dir : Direction.CACHED_VALUES)
				connectedTexture.getFacesToConnect().add(dir);
		}
		
		if(biomes != null) {
			// Currently not supported
		}
		
		if(heights != null) {
			// Currently not supported
		}
		
		if(method.equalsIgnoreCase("random")) {
			if(weights != null) {
				float totalWeight = 0;
				String[] weightTokens = weights.split("[ ,]");
				for(String weightStr : weightTokens) {
					try {
						Float weightVal = Float.parseFloat(weightStr);
						((ConnectedTextureRandom)connectedTexture).getWeights().add(weightVal);
						totalWeight += weightVal.floatValue();
					}catch(Exception ex) {
						((ConnectedTextureRandom)connectedTexture).getWeights().add(1f);
						totalWeight += 1f;
					}
				}
				((ConnectedTextureRandom)connectedTexture).setTotalWeight(totalWeight);
			}else {
				for(int i = 0; i < connectedTexture.getTiles().size(); ++i)
					((ConnectedTextureRandom)connectedTexture).getWeights().add(1f);
				((ConnectedTextureRandom)connectedTexture).setTotalWeight(connectedTexture.getTiles().size());
			}
		}
		
		if(matchTiles != null) {
			String[] tileTokens = matchTiles.split("[ ,]");
			for(String tile : tileTokens) {
				if(!tile.contains(":"))
					tile = "minecraft:block/" + tile;
				ConnectedTextures.registerConnectedTextureByTile(tile, connectedTexture);
			}
		}
		if(matchBlocks != null) {
			String[] blocksTokens = matchBlocks.split("[ ,]");
			for(String block : blocksTokens) {
				String[] blockTokens = block.split(":");
				String blockNamespace = "minecraft";
				String blockName = blockTokens[0];
				if(blockTokens.length > 1) {
					if(!blockTokens[1].contains("=")){
						// If the next token doesn't contain an =,
						// then it's the block name and not a property
						blockNamespace = blockTokens[0];
						blockName = blockTokens[1];
					}
					// TODO: Add in support for specifying block properties here.
				}
				ConnectedTextures.registerConnectedTextureByBlock(blockNamespace + ":" + blockName, connectedTexture);
			}
		}
	}

}
