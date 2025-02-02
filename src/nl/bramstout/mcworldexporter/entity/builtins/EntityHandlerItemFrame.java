package nl.bramstout.mcworldexporter.entity.builtins;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.MapCreator;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;

public class EntityHandlerItemFrame extends EntityHandlerHangable{
	
	@Override
	public void setup(Entity entity) {
		super.setup(entity);
	}
	
	@Override
	public Model getModel(Entity entity) {
		String itemName = null;
		float itemRotation = 0f;
		long mapId = -1;
		boolean isBedrock = false;
		Direction facing = Direction.NORTH;

		NbtTag itemTag = entity.getProperties().get("Item");
		if (itemTag != null) {
			NbtTagString itemNameTag = (NbtTagString) ((NbtTagCompound) itemTag).get("id");
			if(itemNameTag == null)
				itemNameTag = (NbtTagString) ((NbtTagCompound) itemTag).get("Name");
			if(itemNameTag != null)
				itemName = itemNameTag.getData();
			
			if(!itemName.contains(":"))
				itemName = "minecraft:" + itemName;
			
			if(itemName.equals("minecraft:filled_map")) {
				NbtTagCompound mapTag = (NbtTagCompound) ((NbtTagCompound) itemTag).get("tag");
				if(mapTag != null) {
					NbtTagInt mapTag2 = (NbtTagInt) mapTag.get("map");
					if(mapTag2 != null)
						mapId = mapTag2.getData();
					NbtTagLong mapTag3 = (NbtTagLong) mapTag.get("map_uuid");
					if(mapTag3 != null) {
						mapId = mapTag3.getData();
						isBedrock = true;
					}
				}
			}
		}

		NbtTag rotationTag = entity.getProperties().get("ItemRotation");
		if (rotationTag != null) {
			if(rotationTag instanceof NbtTagByte)
				itemRotation = ((NbtTagByte) rotationTag).getData() * 45f;
			else if(rotationTag instanceof NbtTagFloat)
				itemRotation = ((NbtTagFloat) rotationTag).getData();
			
			if(mapId >= 0 && rotationTag instanceof NbtTagByte)
				itemRotation *= 2f;
		}
		
		NbtTag facingTag = entity.getProperties().get("Facing");
		if(facingTag == null)
			facingTag = entity.getProperties().get("facing");
		if(facingTag == null)
			facingTag = entity.getProperties().get("facing_direction");
		byte facingByte = 0;
		if(facingTag instanceof NbtTagByte)
			facingByte = ((NbtTagByte) facingTag).getData();
		else if(facingTag instanceof NbtTagInt)
			facingByte = (byte) ((NbtTagInt) facingTag).getData();
		if(facingByte == 0)
			facing = Direction.DOWN;
		else if(facingByte == 1)
			facing = Direction.UP;
		else if(facingByte == 2)
			facing = Direction.NORTH;
		else if(facingByte == 3)
			facing = Direction.SOUTH;
		else if(facingByte == 4)
			facing = Direction.WEST;
		else if(facingByte == 5)
			facing = Direction.EAST;
		
		Model model = new Model("item_frame", null, false);
		model.addTexture("#back", "minecraft:block/birch_planks");
		model.addTexture("#front", "minecraft:block/" + (entity.getId().toLowerCase().contains("glow") ? "glow_item_frame" : "item_frame"));

		float minX = 2;
		float minY = 2;
		float minZ = 0;
		float maxX = 14;
		float maxY = 14;
		float maxZ = 1;
		if(mapId >= 0) {
			minX = 0;
			minY = 0;
			minZ = 0;
			maxX = 16;
			maxY = 16;
			maxZ = 1;
		}

		// Front Item Frame
		model.addFace(new float[] { minX + 1, minY + 1, minZ, maxX - 1, maxY - 1, maxZ - 0.5f },
						new float[] { minX + 1, minY + 1, maxX - 1, maxY - 1 }, Direction.SOUTH, "#front");

		// Back Item Frame
		model.addFace(new float[] { minX + 1, minY + 1, minZ, maxX - 1, maxY - 1, maxZ - 0.5f },
						new float[] { minX + 1, minY + 1, maxX - 1, maxY - 1 }, Direction.NORTH, "#front");

		// Left edge
		addBar(minX, minY, minZ, minX + 1, maxY, maxZ, model);
		// Right edge
		addBar(maxX - 1, minY, minZ, maxX, maxY, maxZ, model);
		// Top edge
		addBar(minX + 1, maxY - 1, minZ, maxX - 1, maxY, maxZ, model);
		// Bottom edge
		addBar(minX + 1, minY, minZ, maxX - 1, minY + 1, maxZ, model);

		model.rotate(facing.rotX, facing == Direction.UP ? (facing.rotY - 180f) : facing.rotY, false);
		model.translate(-8f, 0f, -8f);
		
		if (itemName != null) {
			int modelId = ModelRegistry.getIdForItemName(itemName);
			Model itemModel = null;
			if(mapId >= 0)
				itemModel = MapCreator.createMapModel(mapId, isBedrock);
			else 
				itemModel = ModelRegistry.getModel(modelId);
				
			if (itemModel != null) {
				itemModel = new Model(itemModel);

				itemModel.scale(0.5f);
				itemModel.applyItemFrameTransformation();
				
				if((itemModel.getExtraData() == null || !itemModel.getExtraData().equals("{\"item\":\"true\"}")) && mapId < 0)
					itemModel.rotate(0, 180, false);
				if(itemRotation != 0f)
					itemModel.rotate(0, 0, -itemRotation);
				itemModel.translate(0, 0, -7);
				
				itemModel.rotate(facing.rotX, facing == Direction.UP ? (facing.rotY - 180f) : facing.rotY, false);
				itemModel.translate(-8f, 0f, -8f);
				
				model.addModel(itemModel);

			}
		}
		
		model.addRootBone();
		
		return model;
	}
	
	private void addBar(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Model model) {
		float[] minMaxPoints = new float[] { minX, minY, minZ, maxX, maxY, maxZ };
		// Front
		model.addFace(minMaxPoints, new float[] { minX, minY, maxX, maxY }, Direction.SOUTH, "#back");

		// Back
		model.addFace(minMaxPoints, new float[] { minX, minY, maxX, maxY }, Direction.NORTH, "#back");
		
		// Top
		model.addFace(minMaxPoints, new float[] { minX, minZ, maxX, maxZ }, Direction.UP, "#back");
		
		// Bottom
		model.addFace(minMaxPoints, new float[] { minX, minZ, maxX, maxZ }, Direction.DOWN, "#back");
		
		// Left
		model.addFace(minMaxPoints, new float[] { minZ, minY, maxZ, maxY }, Direction.WEST, "#back");
		
		// Right
		model.addFace(minMaxPoints, new float[] { minZ, minY, maxZ, maxY }, Direction.EAST, "#back");
	}

}
