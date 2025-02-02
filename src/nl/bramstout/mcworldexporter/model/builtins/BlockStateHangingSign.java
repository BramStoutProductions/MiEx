package nl.bramstout.mcworldexporter.model.builtins;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateHangingSign extends BlockState{

	public BlockStateHangingSign(String name, int dataVersion) {
		super(name, dataVersion, null);	
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/signs/hanging/oak";
	}
	
	@Override
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
		
		List<List<Model>> models = new ArrayList<List<Model>>();
		
		List<Model> list = new ArrayList<Model>();
		Model model = new Model("hanging_sign", null, true);
		list.add(model);
		models.add(list);
		
		boolean isWall = name.contains("wall");
		float rotY = 0f;
		if (isWall) {
			String val = properties.get("facing").asString();
			if (val == null)
				val = "north";
			if (val.equals("north")) {
				rotY = 180f;
			} else if (val.equals("east")) {
				rotY = 270f;
			} else if (val.equals("south")) {
				rotY = 0f;
			} else if (val.equals("west")) {
				rotY = 90f;
			}
		} else {
			String val = properties.get("rotation").asString();
			if (val == null)
				val = "0";
			int ival = 0;
			try {
				ival = Integer.parseInt(val);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			rotY = (((float) ival) / 16f) * 360f;
		}
		
		boolean attached = false;
		NbtTag attachedTag = properties.get("attached");
		if(attachedTag != null)
			attached = attachedTag.asString().equalsIgnoreCase("true");
		
		model.addTexture("#texture", "minecraft:entity/signs/hanging/" +
							name.replace("minecraft:", "").replace("_wall_", "_").replace("_hanging_sign", ""));

		// sign part
		float minX = 1;
		float minY = 0;
		float minZ = 7f;
		float maxX = 15;
		float maxY = 10;
		float maxZ = 9f;

		model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 6f, 8f, 12f }, "#texture");
		
		float textOffsetX = (minX + maxX) / 2f;
		float textOffsetY = (minY + maxY) / 2f + 0.133f;
		float textOffsetZ = maxZ + 0.25f;
		float textOffsetZBack = minZ - 0.25f;
		
		// Chains
		if(attached) {
			minX = 2;
			minY = 10;
			minZ = 8f;
			maxX = 14;
			maxY = 16;
			maxZ = 8f;
			model.addFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 3.5f, 3f, 6.5f, 6f }, 
							Direction.SOUTH, "#texture");
		}else {
			minX = 2;
			minY = 10;
			minZ = 8f;
			maxX = 5;
			maxY = 16;
			maxZ = 8f;
			ModelFace face = model.addFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 3f, 0.75f, 6f }, 
							Direction.SOUTH, "#texture");
			face.rotate(0, 45f, 0f, (minX + maxX)/2f, minY, minZ);
			
			face = model.addFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 1.5f, 3f, 2.25f, 6f }, 
							Direction.SOUTH, "#texture");
			face.rotate(0, -45f, 0f, (minX + maxX)/2f, minY, minZ);
			
			minX = 11;
			maxX = 14;
			face = model.addFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 3f, 0.75f, 6f }, 
							Direction.SOUTH, "#texture");
			face.rotate(0, 45f, 0f, (minX + maxX)/2f, minY, minZ);
			
			face = model.addFace(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 1.5f, 3f, 2.25f, 6f }, 
							Direction.SOUTH, "#texture");
			face.rotate(0, -45f, 0f, (minX + maxX)/2f, minY, minZ);
		}

		// Post part
		if (isWall) {
			minX = 0;
			minY = 14f;
			minZ = 6f;
			maxX = 16;
			maxY = 16f;
			maxZ = 10f;
			
			model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 0f, 10f, 3f }, "#texture");
		}
		
		// Text
		float lineDistance = (2f);
		BlockStateSign.handleText(properties, textOffsetX, textOffsetY, textOffsetZ, textOffsetZBack, 1.75f, lineDistance, model);
		
		model.rotate(0, rotY, false);
		
		BakedBlockState bakedState = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, false, false, false, true, 0, null,
				needsConnectionInfo());
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}

}
