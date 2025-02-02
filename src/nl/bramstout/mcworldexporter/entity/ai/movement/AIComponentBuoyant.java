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
