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

package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.resourcepack.connectedtextures.ConnectedTextures.MatchBlock;
import nl.bramstout.mcworldexporter.resourcepack.hytale.BlockStateHandlerHytale;
import nl.bramstout.mcworldexporter.resourcepack.hytale.BlockStateVariant;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public abstract class ConnectLogic {
	
	public abstract boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz);
	
	public static class ConnectLogicSameBlock extends ConnectLogic{
		
		@Override
		public boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
			int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			int otherId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			Block thisBlock = BlockRegistry.getBlock(thisId);
			Block otherBlock = BlockRegistry.getBlock(otherId);
			return thisBlock.getName().equals(otherBlock.getName());
		}
		
	}
	
	public static class ConnectLogicSameState extends ConnectLogic{
		
		@Override
		public boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
			int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			int otherId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			return thisId == otherId;
		}
		
	}
	
	public static class ConnectLogicSameTile extends ConnectLogic{
		
		@Override
		public boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
			int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			int otherId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			BakedBlockState thisState = BlockStateRegistry.getBakedStateForBlock(thisId, x, y, z);
			BakedBlockState otherState = BlockStateRegistry.getBakedStateForBlock(otherId, x + dx, y + dy, z + dz);
			String thisTex = null;
			String otherTex = null;
			List<Model> models = new ArrayList<Model>();
			thisState.getDefaultModels(models);
			for(Model model : models) {
				for(ModelFace face2 : model.getFaces()) {
					if(face2.getDirection() == face.getDirection()) {
						thisTex = model.getTexture(face2.getTexture());
						break;
					}
				}
				if(thisTex != null)
					break;
			}
			if(thisTex == null)
				return false;
			models.clear();
			otherState.getDefaultModels(models);
			for(Model model : models) {
				for(ModelFace face2 : model.getFaces()) {
					if(face2.getDirection() == face.getDirection()) {
						otherTex = model.getTexture(face2.getTexture());
						break;
					}
				}
				if(otherTex != null)
					break;
			}
			if(otherTex == null)
				return false;
			return thisTex.equals(otherTex);
		}
		
	}
	
	public static class ConnectLogicBlockNames extends ConnectLogic{
		
		public List<MatchBlock> blocks = new ArrayList<MatchBlock>();
		public boolean ignoreSameBlock = false;
		public boolean hytaleSpecificLogic = false;
		
		@Override
		public boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
			int otherId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			Block otherBlock = BlockRegistry.getBlock(otherId);
			
			if(ignoreSameBlock) {
				int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
				Block thisBlock = BlockRegistry.getBlock(thisId);
				if(thisBlock.getName().equals(otherBlock.getName()))
					return false;
			}
			if(hytaleSpecificLogic) {
				int thisId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
				Block thisBlock = BlockRegistry.getBlock(thisId);
				
				int thisBlockStateId = BlockStateRegistry.getIdForName(thisBlock.getName(), thisBlock.getDataVersion());
				int otherBlockStateId = BlockStateRegistry.getIdForName(otherBlock.getName(), otherBlock.getDataVersion());
				BlockState thisBlockState = BlockStateRegistry.getState(thisBlockStateId);
				BlockState otherBlockState = BlockStateRegistry.getState(otherBlockStateId);
				
				if(thisBlockState.getHandler() instanceof BlockStateHandlerHytale && 
						otherBlockState.getHandler() instanceof BlockStateHandlerHytale) {
					// It can be that both blocks have transition textures set up for each other.
					// If that is the case, then we should only allow one of the two.
					// Each block contains a list of block groups to provide transition textures for.
					// If the group of thisBlock is higher up or equal on the list of otherBlock,
					// then we allow the connection. Otherwise, we don't.
					BlockStateHandlerHytale thisBlockState2 = (BlockStateHandlerHytale) thisBlockState.getHandler();
					BlockStateHandlerHytale otherBlockState2 = (BlockStateHandlerHytale) otherBlockState.getHandler();
					BlockStateVariant thisBlockVariant = thisBlockState2.getVariants().getOrDefault("", null);
					BlockStateVariant otherBlockVariant = otherBlockState2.getVariants().getOrDefault("", null);
					
					if(thisBlockVariant.getTransitionTexture() != null && otherBlockVariant.getTransitionTexture() != null && 
							thisBlockVariant.getTransitionToGroups() != null && otherBlockVariant.getTransitionToGroups() != null) {
						// Both of them have transition textures.
						int thisIndex = -1;
						for(int i = 0; i < thisBlockVariant.getTransitionToGroups().length; ++i) {
							if(thisBlockVariant.getTransitionToGroups()[i].equals(otherBlockState2.getGroup())) {
								thisIndex = i;
								break;
							}
						}
						int otherIndex = -1;
						for(int i = 0; i < otherBlockVariant.getTransitionToGroups().length; ++i) {
							if(otherBlockVariant.getTransitionToGroups()[i].equals(thisBlockState2.getGroup())) {
								otherIndex = i;
								break;
							}
						}
						if(thisIndex != -1 && otherIndex != -1) {
							// Both blocks have each other's groups in their lists,
							// so we have clashing transition textures.
							// So now only make sure that one of them shows up.
							if(otherIndex > thisIndex)
								return false;
						}
					}
				}
			}
			
			for(MatchBlock block : blocks) {
				if(otherBlock.getName().equals(block.name) && block.state.meetsConstraint(otherBlock.getProperties())) {
					return true;
				}
			}
			
			return false;
		}
		
	}
	
	public static class ConnectLogicTextures extends ConnectLogic{
		
		public List<String> textures = new ArrayList<String>();
		
		@Override
		public boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
			int otherId = MCWorldExporter.getApp().getWorld().getBlockId(x + dx, y + dy, z + dz);
			BakedBlockState otherState = BlockStateRegistry.getBakedStateForBlock(otherId, x + dx, y + dy, z + dz);
			String otherTex = null;
			List<Model> models = new ArrayList<Model>();
			otherState.getDefaultModels(models);
			for(Model model : models) {
				for(ModelFace face2 : model.getFaces()) {
					if(face2.getDirection() == face.getDirection()) {
						otherTex = model.getTexture(face2.getTexture());
						break;
					}
				}
				if(otherTex != null)
					break;
			}
			if(otherTex == null)
				return false;
			
			return textures.contains(otherTex);
		}
		
	}
	
}
