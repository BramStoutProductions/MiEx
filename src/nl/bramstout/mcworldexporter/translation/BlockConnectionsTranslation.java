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

package nl.bramstout.mcworldexporter.translation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockConnectionsTranslation {
	
	public static class BlockConnectionMatch{
		
		private List<String> matches;
		private List<Map<String, String>> checks;
		
		public BlockConnectionMatch(String str) {
			String checkString = "";
			if(str.contains("[")) {
				checkString = str.substring(str.indexOf('[')+1, str.length()-1);
				str = str.substring(0, str.indexOf('['));
			}
			if(str.startsWith("#"))
				matches = Tags.getNamesInTag(str);
			else {
				matches = new ArrayList<String>();
				if(!str.contains(":"))
					str = "minecraft:" + str;
				matches.add(str);
			}
			checks = new ArrayList<Map<String, String>>();
			for(String checkToken : checkString.split("\\|\\|")) {
				Map<String, String> check = new HashMap<String, String>();
				for(String checkToken2 : checkToken.split(",")) {
					if(checkToken2.contains("=")) {
						String[] tokens = checkToken2.split("=");
						check.put(tokens[0], tokens[1]);
					}
				}
				checks.add(check);
			}
		}
		
		public boolean matches(Block otherBlock, String thisBlockName, List<String> thisGroup,
								int x, int y, int z) {
			boolean match = matchesList(otherBlock, matches, thisBlockName, thisGroup, x, y, z);
			if(match) {
				match = useConnection(otherBlock.getProperties());
			}
			return match;
		}
		
		private boolean matchesList(Block block, List<String> matches, 
				String thisBlockName, List<String> thisGroup, int x, int y, int z) {
			for(String match : matches) {
				if(match.equals("minecraft:this_block")) {
					if(block.getName().equals(thisBlockName))
						return true;
				} else if(match.equals("minecraft:this_group")) {
					if(matchesList(block, thisGroup, thisBlockName, thisGroup, x, y, z))
						return true;
				}else if(match.equals("minecraft:$solid_block")){
					BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(block.getId(), x, y, z, false);
					return state.isSolidBlock();
				} else if(match.equals(block.getName())) {
					return true;
				}
			}
			return false;
		}
		
		public boolean useConnection(NbtTagCompound properties) {
			if(checks.isEmpty())
				return true;
			
			for(Map<String, String> check : checks) {
				boolean res = doCheck(properties, check);
				if(res)
					return true;
			}
			return false;
		}
		
		private boolean doCheck(NbtTagCompound properties, Map<String, String> check) {
			int numItems = properties.getSize();
			for(int i = 0; i < numItems; ++i) {
				NbtTag tag = properties.get(i);
				String value = check.get(tag.getName());
				if(value != null) {
					String propValue = tag.asString();
					if(propValue != null) {
						if(!value.equals(propValue)) {
							if(!((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))))
								return false;
						}
					}
				}
			}
			return true;
		}
		
	}
	
	public static class BlockConnectionPartCheck{
		
		private int dx;
		private int dy;
		private int dz;
		private List<BlockConnectionMatch> matches;
		private boolean invert;
		
		public BlockConnectionPartCheck(int dx, int dy, int dz, List<BlockConnectionMatch> matches, boolean invert) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.matches = matches;
			this.invert = invert;
		}
		
		public boolean matches(Block thisBlock, List<String> thisGroup, int x, int y, int z) {
			int otherBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			Block otherBlock = BlockRegistry.getBlock(otherBlockId);
			
			boolean match = matchesList(otherBlock, thisBlock.getName(), thisGroup, x + dx, y + dy, z + dz);
			if(invert)
				match = !match;
			
			return match;
		}
		
		private boolean matchesList(Block otherBlock, String thisBlockName, List<String> thisGroup,
									int x, int y, int z) {
			for(BlockConnectionMatch match : matches) {
				if(match.matches(otherBlock, thisBlockName, thisGroup, x, y, z))
					return true;
			}
			return false;
		}
		
	}
	
	public static class BlockConnectionPart{
		
		List<BlockConnectionPartCheck> checks;
		Map<String, String> properties;
		String newName;
		
		public BlockConnectionPart() {
			this.checks = new ArrayList<BlockConnectionPartCheck>();
			this.properties = new HashMap<String, String>();
			this.newName = null;
		}
		
		public boolean matches(Block thisBlock, List<String> thisGroup, int x, int y, int z) {
			for(BlockConnectionPartCheck check : checks) {
				if(!check.matches(thisBlock, thisGroup, x, y, z))
					return false;
			}
			return true;
		}
		
		public String map(NbtTagCompound properties) {
			for(Entry<String, String> value : this.properties.entrySet()) {
				NbtTagString valueTag = NbtTagString.newInstance(value.getKey(), value.getValue());
				properties.addElement(valueTag);
			}
			return newName;
		}
		
	}
	
	public static class BlockConnection{
		
		private List<Map<String, String>> checks;
		List<BlockConnectionPart> connections;
		
		public BlockConnection(String checkString) {
			checks = new ArrayList<Map<String, String>>();
			for(String checkToken : checkString.split("\\|\\|")) {
				Map<String, String> check = new HashMap<String, String>();
				for(String checkToken2 : checkToken.split(",")) {
					if(checkToken2.contains("=")) {
						String[] tokens = checkToken2.split("=");
						check.put(tokens[0], tokens[1]);
					}
				}
				checks.add(check);
			}
			connections = new ArrayList<BlockConnectionPart>();
		}
		
		public String map(NbtTagCompound properties, int x, int y, int z, Block thisBlock, List<String> thisGroup) {
			String newName = null;
			for(BlockConnectionPart part : connections) {
				if(part.matches(thisBlock, thisGroup, x, y, z)) {
					String newName2 = part.map(properties);
					if(newName2 != null)
						newName = newName2;
				}
			}
			return newName;
		}
		
		public boolean useConnection(NbtTagCompound properties) {
			if(checks.isEmpty())
				return true;
			
			for(Map<String, String> check : checks) {
				boolean res = doCheck(properties, check);
				if(res)
					return true;
			}
			return false;
		}
		
		private boolean doCheck(NbtTagCompound properties, Map<String, String> check) {
			int numItems = properties.getSize();
			for(int i = 0; i < numItems; ++i) {
				NbtTag tag = properties.get(i);
				String value = check.get(tag.getName());
				if(value != null) {
					String propValue = tag.asString();
					if(propValue != null) {
						if(!value.equals(propValue)) {
							if(!((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))))
								return false;
						}
					}
				}
			}
			return true;
		}

		
	}
	
	public static class BlockConnections{
		
		private List<String> thisGroup;
		List<BlockConnection> connections;
		int minDataVersion;
		int maxDataVersion;
		
		public BlockConnections(List<String> thisGroup) {
			this.thisGroup = thisGroup;
			this.connections = new ArrayList<BlockConnection>();
			this.minDataVersion = Integer.MIN_VALUE;
			this.maxDataVersion = Integer.MAX_VALUE;
		}
		
		public String map(String name, NbtTagCompound properties, int x, int y, int z) {
			int thisBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			Block thisBlock = BlockRegistry.getBlock(thisBlockId);
			
			String newName = null;
			for(BlockConnection connection : connections) {
				if(connection.useConnection(properties)) {
					String newName2 = connection.map(properties, x, y, z, thisBlock, thisGroup);
					if(newName2 != null)
						newName = newName2;
				}
			}
			return newName;
		}
		
	}
	
	private String sourceName;
	private Map<String, List<BlockConnections>> translationMap;
	
	public BlockConnectionsTranslation(String sourceName) {
		this.sourceName = sourceName;
		this.translationMap = null;
	}
	
	public void load() {
		if(translationMap == null)
			translationMap = new HashMap<String, List<BlockConnections>>();
		translationMap.clear();
		List<ResourcePack> resourcePacks = ResourcePacks.getActiveResourcePacks();
		for(int i = resourcePacks.size() - 1; i >= 0; --i) {
			File translationFile = new File(resourcePacks.get(i).getFolder(), 
					"translation/minecraft/" + sourceName + "/miex_block_connections.json");
			if(!translationFile.exists())
				continue;
			try {
				JsonArray data = Json.read(translationFile).getAsJsonArray();
				for(JsonElement mappings : data.asList()) {
					if(!mappings.isJsonObject())
						continue;
					
					int minDataVersion = Integer.MIN_VALUE;
					int maxDataVersion = Integer.MAX_VALUE;
					if(mappings.getAsJsonObject().has("minDataVersion"))
						minDataVersion = mappings.getAsJsonObject().get("minDataVersion").getAsInt();
					if(mappings.getAsJsonObject().has("maxDataVersion"))
						maxDataVersion = mappings.getAsJsonObject().get("maxDataVersion").getAsInt();
					if(mappings.getAsJsonObject().has("mappings")) {
						for(Entry<String, JsonElement> entry : mappings.getAsJsonObject().getAsJsonObject("mappings").entrySet()) {
							String blockNames = entry.getKey();
							
							// Get the list of blocks for this object.
							List<String> blockGroup = new ArrayList<String>();
							String[] blockNameTokens = blockNames.split(",");
							for(String blockName : blockNameTokens) {
								if(blockName.startsWith("#")) {
									List<String> blockNames2 = Tags.getNamesInTag(blockName);
									for(String blockName2 : blockNames2) {
										blockGroup.add(blockName2);
									}
								}else {
									if(!blockName.contains(":"))
										blockName = "minecraft:" + blockName;
									blockGroup.add(blockName);
								}
							}
							
							BlockConnections blockConnections = new BlockConnections(blockGroup);
							blockConnections.minDataVersion = minDataVersion;
							blockConnections.maxDataVersion = maxDataVersion;
							
							for(JsonElement connection : entry.getValue().getAsJsonArray().asList()) {
								JsonObject connectionObj = connection.getAsJsonObject();
								String checkStr = "";
								if(connectionObj.has("condition"))
									checkStr = connectionObj.get("condition").getAsString();
								BlockConnection blockConnection = new BlockConnection(checkStr);
								
								if(connectionObj.has("connections")) {
									for(Entry<String, JsonElement> part : connectionObj.get("connections").getAsJsonObject().entrySet()) {
										BlockConnectionPart blockPart = new BlockConnectionPart();
										String[] checksTokens = part.getKey().split("&");
										for(String check : checksTokens) {
											if(check.isEmpty())
												continue;
											int equalIndex = check.indexOf('=');
											String offsetStr = check.substring(0, equalIndex);
											String[] offsetTokens = offsetStr.split("_");
											int dx = Integer.parseInt(offsetTokens[0]);
											int dy = Integer.parseInt(offsetTokens[1]);
											int dz = Integer.parseInt(offsetTokens[2]);
											String matchStr = check.substring(equalIndex+1);
											boolean invert = false;
											if(matchStr.startsWith("!")) {
												invert = true;
												matchStr = matchStr.substring(1);
											}
											List<BlockConnectionMatch> matches = new ArrayList<BlockConnectionMatch>();
											String[] matchTokens = matchStr.split("\\|");
											for(String matchItem : matchTokens) {
												matches.add(new BlockConnectionMatch(matchItem));
											}
											blockPart.checks.add(new BlockConnectionPartCheck(dx, dy, dz, matches, invert));
										}
										
										JsonObject newBlockData = part.getValue().getAsJsonObject();
										
										if(newBlockData.has("name")) {
											blockPart.newName = newBlockData.get("name").getAsString();
											if(!blockPart.newName.contains(":"))
												blockPart.newName = "minecraft:" + blockPart.newName;
										}
										if(newBlockData.has("blockState")) {
											for(Entry<String, JsonElement> value : newBlockData.getAsJsonObject("blockState").entrySet()) {
												blockPart.properties.put(value.getKey(), value.getValue().getAsString());
											}
										}
										blockConnection.connections.add(blockPart);
									}
								}
								blockConnections.connections.add(blockConnection);
							}
							
							for(String blockName : blockGroup) {
								List<BlockConnections> maps = translationMap.getOrDefault(blockName, null);
								if(maps == null) {
									maps = new ArrayList<BlockConnections>();
									translationMap.put(blockName, maps);
								}
								
								maps.add(0, blockConnections);
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if(translationMap.isEmpty())
			translationMap = null;
	}
	
	public BlockConnections getBlockConnections(String blockName, int dataVersion) {
		if(translationMap == null)
			return null;
		List<BlockConnections> maps = translationMap.getOrDefault(blockName, null);
		if(maps == null)
			return null;
		for(BlockConnections map : maps)
			if(dataVersion >= map.minDataVersion && dataVersion <= map.maxDataVersion)
				return map;
		return null;
	}
	
}
