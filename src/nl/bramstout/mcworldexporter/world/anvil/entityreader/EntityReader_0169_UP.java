package nl.bramstout.mcworldexporter.world.anvil.entityreader;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.world.Chunk;

public class EntityReader_0169_UP extends EntityReader{

	@Override
	public void readEntities(Chunk chunk, NbtTagCompound rootTag) {
		NbtTagList entitiesTag = (NbtTagList) rootTag.get("Entities");
		if(entitiesTag == null) {
			NbtTagCompound levelTag = (NbtTagCompound) rootTag.get("Level");
			if(levelTag != null) {
				entitiesTag = (NbtTagList) levelTag.get("Entities");
			}
		}
		
		for(NbtTag tag : entitiesTag.getData()) {
			NbtTagCompound entityTag = (NbtTagCompound) tag;
			String name = ((NbtTagString)entityTag.get("id")).getData();
			Entity entity = EntityRegistry.getEntity(name, entityTag);
			if(entity != null)
				chunk._getEntities().add(entity);
		}
	}

	@Override
	public boolean supportDataVersion(int dataVersion) {
		return dataVersion >= 169;
	}

}
