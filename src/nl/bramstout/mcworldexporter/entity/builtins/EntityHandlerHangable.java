package nl.bramstout.mcworldexporter.entity.builtins;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;

public abstract class EntityHandlerHangable extends EntityHandler{

	@Override
	public void setup(Entity entity) {
		NbtTag posTag = entity.getProperties().get("Pos");
		if(posTag != null && posTag instanceof NbtTagList) {
			NbtTagList posList = (NbtTagList) posTag;
			NbtTag el0 = posList.get(0);
			if(el0 instanceof NbtTagFloat) {
				entity.setX(((NbtTagFloat) posList.get(0)).getData());
				entity.setY(((NbtTagFloat) posList.get(1)).getData());
				entity.setZ(((NbtTagFloat) posList.get(2)).getData());
			}else if(el0 instanceof NbtTagDouble) {
				entity.setX((float) ((NbtTagDouble) posList.get(0)).getData());
				entity.setY((float) ((NbtTagDouble) posList.get(1)).getData());
				entity.setZ((float) ((NbtTagDouble) posList.get(2)).getData());
			}
		}
		if(entity.getProperties().get("TileX") != null) {
			float blockX = ((NbtTagInt) entity.getProperties().get("TileX")).getData();
			float blockY = ((NbtTagInt) entity.getProperties().get("TileY")).getData();
			float blockZ = ((NbtTagInt) entity.getProperties().get("TileZ")).getData();
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
		if(entity.getProperties().get("x") != null) {
			float blockX = ((NbtTagInt) entity.getProperties().get("x")).getData();
			float blockY = ((NbtTagInt) entity.getProperties().get("y")).getData();
			float blockZ = ((NbtTagInt) entity.getProperties().get("z")).getData();
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
	}

	@Override
	public EntityAIHandler getAIHandler(Entity entity) {
		return null;
	}

}
