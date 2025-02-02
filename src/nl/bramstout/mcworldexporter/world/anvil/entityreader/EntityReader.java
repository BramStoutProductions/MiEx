package nl.bramstout.mcworldexporter.world.anvil.entityreader;

import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.Chunk;

public abstract class EntityReader {

	public abstract void readEntities(Chunk chunk, NbtTagCompound rootTag);
	
	public abstract boolean supportDataVersion(int dataVersion);
	
	private static EntityReader[] readers = new EntityReader[] {
			new EntityReader_0169_UP()
	};
	
	public static EntityReader getEntityReader(int dataVersion) {
		for(EntityReader reader : readers)
			if(reader.supportDataVersion(dataVersion))
				return reader;
		return null;
	}
	
}
