package nl.bramstout.mcworldexporter.entity.builtins;

import java.util.HashMap;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.PaintingVariant;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.World;

public class EntityHandlerPainting extends EntityHandlerHangable{

	
	@Override
	public void setup(Entity entity) {
		super.setup(entity);
	}
	
	@Override
	public Model getModel(Entity entity) {
		String motive = "";
		PaintingVariant variant = null;
		NbtTag motiveTag = entity.getProperties().get("Motive");
		if(motiveTag != null) {
			motive = ((NbtTagString) motiveTag).getData();
			if(!motive.contains(":"))
				motive = "minecraft:" + motive;
			variant = getVariant(motive);
		}else {
			motiveTag = entity.getProperties().get("variant");
			if(motiveTag != null) {
				if(motiveTag instanceof NbtTagString) {
					motive = ((NbtTagString) motiveTag).getData();
					if(!motive.contains(":"))
						motive = "minecraft:" + motive;
					variant = getVariant(motive);
				}else if(motiveTag instanceof NbtTagCompound) {
					motive = null;
					int width = 1;
					int height = 1;
					NbtTagCompound dataTag = (NbtTagCompound) motiveTag;
					
					NbtTagString assetIdTag = (NbtTagString) dataTag.get("asset_id");
					if(assetIdTag != null)
						motive = assetIdTag.getData();
					
					NbtTagInt widthTag = (NbtTagInt) dataTag.get("width");
					if(widthTag != null)
						width = widthTag.getData();
					
					NbtTagInt heightTag = (NbtTagInt) dataTag.get("height");
					if(heightTag != null)
						height = heightTag.getData();
					
					if(motive != null) {
						variant = new PaintingVariant(motive, motive, width, height);
					}else {
						motive = "";
					}
				}
			}
		}
		
		
		Direction facing = Direction.NORTH;
		NbtTag facingTag = entity.getProperties().get("Facing");
		if(facingTag == null)
			facingTag = entity.getProperties().get("facing");
		if(facingTag == null)
			facingTag = entity.getProperties().get("Dir");
		if(facingTag == null)
			facingTag = entity.getProperties().get("Direction");
		byte facingByte = 0;
		if(facingTag != null)
			facingByte = ((NbtTagByte) facingTag).getData();
		if(facingByte == 4)
			facing = Direction.DOWN;
		else if(facingByte == 5)
			facing = Direction.UP;
		else if(facingByte == 2)
			facing = Direction.NORTH;
		else if(facingByte == 0)
			facing = Direction.SOUTH;
		else if(facingByte == 1)
			facing = Direction.WEST;
		else if(facingByte == 3)
			facing = Direction.EAST;
		
		if(variant == null) {
			World.handleError(new RuntimeException("No painting variant found for " + motive));
			return new Model("painting", null, false);
		}
		
		Model model = new Model("painting", null, false);
		model.addTexture("#back", "minecraft:painting/back");
		model.addTexture("#front", variant.getAssetPath());
		
		float sizeX = variant.getWidth();
		float sizeY = variant.getHeight();
		float sizeZ = 1f;
		float offsetX = getOffset((int) sizeX);
		float offsetY = getOffset((int) sizeY);
		float offsetZ = 0f;
		sizeX *= 16f;
		sizeY *= 16f;
		offsetX *= 16f;
		offsetY *= 16f;
		
		float[] minMaxPoints = new float[] {offsetX, offsetY, offsetZ, offsetX + sizeX, offsetY + sizeY, offsetZ + sizeZ};
		
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.SOUTH, "#front");

		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.NORTH, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 1.0f }, Direction.UP, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 1.0f }, Direction.DOWN, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 1.0f, 16.0f }, Direction.EAST, "#back");
		model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 1.0f, 16.0f }, Direction.WEST, "#back");
		
		model.rotate(facing.rotX, facing.rotY, false);
		model.translate(-8f, 0f, -8f);
		
		model.addRootBone();
		
		return model;
	}
	
	private int getOffset(int width) {
		return -Math.max((width - 1) / 2, 0);
	}
	
	private static class Size{
		public float x;
		public float y;
		
		public Size(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private PaintingVariant getVariant(String motif) {
		PaintingVariant variant = ResourcePacks.getPaintingVariant(motif);
		if(variant != null)
			return variant;
		Size size = paintingSizes.getOrDefault(motif, null);
		if(size == null)
			return null;
		return new PaintingVariant(motif, motif, (int) size.x, (int) size.y);
	}
	
	private static HashMap<String, Size> paintingSizes = new HashMap<String, Size>();
	static {
		paintingSizes.put("minecraft:alban", new Size(1,1));
		paintingSizes.put("minecraft:aztec", new Size(1,1));
		paintingSizes.put("minecraft:aztec2", new Size(1,1));
		paintingSizes.put("minecraft:bomb", new Size(1,1));
		paintingSizes.put("minecraft:kebab", new Size(1,1));
		paintingSizes.put("minecraft:plant", new Size(1,1));
		paintingSizes.put("minecraft:wasteland", new Size(1,1));
		paintingSizes.put("minecraft:courbet", new Size(2,1));
		paintingSizes.put("minecraft:pool", new Size(2,1));
		paintingSizes.put("minecraft:sea", new Size(2,1));
		paintingSizes.put("minecraft:creebet", new Size(2,1));
		paintingSizes.put("minecraft:sunset", new Size(2,1));
		paintingSizes.put("minecraft:graham", new Size(1,2));
		paintingSizes.put("minecraft:wanderer", new Size(1,2));
		paintingSizes.put("minecraft:bust", new Size(2,2));
		paintingSizes.put("minecraft:match", new Size(2,2));
		paintingSizes.put("minecraft:skull_and_roses", new Size(2,2));
		paintingSizes.put("minecraft:stage", new Size(2,2));
		paintingSizes.put("minecraft:void", new Size(2,2));
		paintingSizes.put("minecraft:wither", new Size(2,2));
		paintingSizes.put("minecraft:fighters", new Size(4,2));
		paintingSizes.put("minecraft:donkey_kong", new Size(4,3));
		paintingSizes.put("minecraft:skeleton", new Size(4,3));
		paintingSizes.put("minecraft:burning_skull", new Size(4,4));
		paintingSizes.put("minecraft:pigscene", new Size(4,4));
		paintingSizes.put("minecraft:pointer", new Size(4,4));
	}

}
