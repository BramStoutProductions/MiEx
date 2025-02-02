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

package nl.bramstout.mcworldexporter.entity.ai.movement;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class AIComponentBuoyant extends AIComponent{

	public boolean applyGravity;
	public float baseBuoyancy;
	public float bigWaveProbability;
	public float bigWaveSpeed;
	public boolean simulateWaves;
	public List<String> liquidBlocks;
	
	public AIComponentBuoyant(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		liquidBlocks = new ArrayList<String>();
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("BuoyancyGravity", applyGravity ? ((byte) 1) : ((byte) 0)));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("Buoyancy", baseBuoyancy));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("BuoyancyBigWaveProbability", bigWaveProbability));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("BuoyancyBigWaveSpeed", bigWaveSpeed));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("BuoyancySimulateWaves", simulateWaves ? ((byte) 1) : ((byte) 0)));
		// Figure out whether to apply buoyancy
		int blockX = (int) Math.floor(entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value);
		int blockY = (int) Math.floor(entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value - 0.5f);
		int blockZ = (int) Math.floor(entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value);
		int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
		Block block = BlockRegistry.getBlock(blockId);
		String blockName = block.getName();
		if(block.isWaterlogged())
			blockName = "minecraft:water";
		boolean applyBuoyancy = false;
		if(liquidBlocks.contains(blockName)) {
			applyBuoyancy = true;
		}
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("ApplyBuoyancy", applyBuoyancy ? ((byte) 1) : ((byte) 0)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("ApplyBuoyancy", ((byte) 0)));
	}

}
