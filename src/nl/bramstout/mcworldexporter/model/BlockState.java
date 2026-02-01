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

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation;
import nl.bramstout.mcworldexporter.translation.BlockConnectionsTranslation.BlockConnections;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockState {

	protected String name;
	protected int id;
	protected int dataVersion;
	protected boolean transparentOcclusion;
	protected boolean leavesOcclusion;
	protected boolean detailedOcclusion;
	protected boolean individualBlocks;
	protected boolean caveBlock;
	protected boolean randomOffset;
	protected boolean randomYOffset;
	protected boolean doubleSided;
	protected boolean randomAnimationXZOffset;
	protected boolean randomAnimationYOffset;
	protected boolean lodNoUVScale;
	protected boolean lodNoScale;
	protected int lodPriority;
	protected Tint tint;
	protected boolean _needsConnectionInfo;
	protected BlockConnections blockConnections;
	protected BlockStateHandler handler;
	
	public BlockState(String name, int dataVersion, BlockStateHandler handler) {
		this.handler = handler;
		this.name = name;
		this.dataVersion = dataVersion;
		this.id = BlockStateRegistry.getNextId();
		this.transparentOcclusion = Config.transparentOcclusion.contains(name);
		this.leavesOcclusion = Config.leavesOcclusion.contains(name);
		this.detailedOcclusion = Config.detailedOcclusion.contains(name);
		this.individualBlocks = Config.individualBlocks.contains(name);
		this.caveBlock = Config.caveBlocks.contains(name);
		this.randomOffset = Config.randomOffset.contains(name);
		this.randomYOffset = Config.randomYOffset.contains(name);
		doubleSided = Config.doubleSided.contains(name) || Config.forceDoubleSidedOnEverything;
		randomAnimationXZOffset = Config.randomAnimationXZOffset.contains(name);
		randomAnimationYOffset = Config.randomAnimationYOffset.contains(name);
		lodNoUVScale = Config.lodNoUVScale.contains(name);
		lodNoScale = Config.lodNoScale.contains(name);
		lodPriority = Config.lodPriority.getOrDefault(name, 1);
		tint = Tints.getTint(name);
		BlockConnectionsTranslation blockConnectionsTranslation = MCWorldExporter.getApp()
												.getWorld().getBlockConnectionsTranslation();
		if(blockConnectionsTranslation != null)
			blockConnections = blockConnectionsTranslation.getBlockConnections(name, dataVersion);
		
		_needsConnectionInfo = false;
		if(blockConnections != null)
			_needsConnectionInfo = true;
		
		if(handler != null)
			if(handler.needsConnectionInfo())
				_needsConnectionInfo = true;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isTransparentOcclusion() {
		return transparentOcclusion;
	}
	
	public boolean isLeavesOcclusion() {
		return leavesOcclusion;
	}
	
	public boolean isDetailedOcclusion() {
		return detailedOcclusion;
	}
	
	public boolean isIndividualBlocks() {
		return individualBlocks;
	}
	
	public boolean isCaveBlock() {
		return caveBlock;
	}
	
	public boolean hasRandomOffset() {
		return randomOffset;
	}
	
	public boolean hasRandomYOffset() {
		return randomYOffset;
	}
	
	public boolean isDoubleSided() {
		return doubleSided;
	}
	
	public int getLodPriority() {
		return lodPriority;
	}
	
	public boolean hasRandomAnimationXZOffset() {
		return randomAnimationXZOffset;
	}
	
	public boolean hasRandomAnimationYOffset() {
		return randomAnimationYOffset;
	}
	
	public boolean isLodNoUVScale() {
		return lodNoUVScale;
	}
	
	public boolean isLodNoScale() {
		return lodNoScale;
	}
	
	public boolean needsConnectionInfo() {
		return _needsConnectionInfo;
	}
	
	public int getDataVersion() {
		return dataVersion;
	}
	
	public Tint getTint() {
		return tint;
	}
	
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, boolean runBlockConnections) {
		if(blockConnections != null && runBlockConnections) {
			properties = (NbtTagCompound) properties.copy();
			String newName = blockConnections.map(name, properties, x, y, z);
			if(newName != null && !newName.equals(name)) {
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
				properties.free();
				return BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, runBlockConnections);
			}
		}
		
		BakedBlockState res = handler.getBakedBlockState(properties, x, y, z, this);
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return res;
	}
	
	public boolean hasLiquid(NbtTagCompound properties) {
		NbtTagString waterloggedTag = (NbtTagString) properties.get("waterlogged");
		
		if(waterloggedTag == null) {
			if(Config.waterlogged.contains(name))
				return true;
			return false;
		}
		return waterloggedTag.getData().equalsIgnoreCase("true");
	}
	
	public String getLiquidName(NbtTagCompound properties) {
		NbtTagString waterblockTag = (NbtTagString) properties.get("waterblock");
		
		if(waterblockTag == null)
			return null;
		
		return waterblockTag.asString();
	}
	
	public String getDefaultTexture() {
		return handler.getDefaultTexture();
	}

	public boolean hasTint() {
		return tint != null;
	}
	
	public BlockStateHandler getHandler() {
		return handler;
	}
	
}
