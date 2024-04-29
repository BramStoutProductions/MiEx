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

package nl.bramstout.mcworldexporter.model;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public abstract class BlockStatePart {
	
	protected List<Model> models;
	
	protected BlockStatePart() {
		models = new ArrayList<Model>();
	}
	
	public List<Model> getModels(){
		return models;
	}

	public abstract boolean usePart(TAG_Compound properties, int x, int y, int z);
	
	public abstract boolean needsConnectionInfo();
	
	public void noOcclusion() {
		for(Model model : models) {
			for(ModelFace face : model.faces) {
				face.noOcclusion();
			}
		}
	}
	
	public String getDefaultTexture() {
		if(models.size() == 0)
			return "";
		return models.get(0).getDefaultTexture();
	}
	
	protected boolean testMiExConnection(String key, String value, int x, int y, int z) {
		// format: miex_connect_<x offset>_<y ofset>_<z ofset>
		// where the x, y, and z offsets are the block position
		// offsets from the current block to check.
		String[] tokens = key.split("_");
		if(tokens.length != 5)
			return false;
		try {
			int xOffset = Integer.parseInt(tokens[2]);
			int yOffset = Integer.parseInt(tokens[3]);
			int zOffset = Integer.parseInt(tokens[4]);
			
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x + xOffset, y + yOffset, z + zOffset);
			Block block = BlockRegistry.getBlock(blockId);
			
			boolean match = false;
			
			for(String valueItem : value.split("\\|")) {
				boolean invert = false;
				if(valueItem.startsWith("!")) {
					invert = true;
					valueItem = valueItem.substring(1);
				}
				
				if(valueItem.equalsIgnoreCase("this")) {
					// Check if the block is the same as the current block.
					int thisBlockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
					if(invert)
						match |= blockId != thisBlockId;
					else
						match |= blockId == thisBlockId;
				}else if(valueItem.startsWith("#")) {
					// Check if the sampled block is in the specified tag.
					List<String> blockNames = Tags.getNamesInTag(valueItem);
					if(invert)
						match |= !blockNames.contains(block.getName());
					else
						match |= blockNames.contains(block.getName());
				}else {
					// Check if the sampled block has the same name.
					if(!valueItem.contains(":"))
						valueItem = "minecraft:" + valueItem;
					if(invert)
						match |= !valueItem.equals(block.getName());
					else
						match |= valueItem.equals(block.getName());
				}
			}
			return match;
		}catch(Exception ex) {}
		return false;
	}
	
}
