package nl.bramstout.mcworldexporter.world.anvil.chunkreader;

import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.world.Chunk;

public abstract class ChunkReader {

	public abstract void readChunk(Chunk chunk, NbtTagCompound rootTag, int dataVersion);
	
	public abstract boolean supportDataVersion(int dataVersion);
	
	private static ChunkReader[] readers = new ChunkReader[] {
			new ChunkReader_2844_UP(),
			new ChunkReader_2836_2843(),
			new ChunkReader_2529_2835(),
			new ChunkReader_2203_2528(),
			new ChunkReader_1466_2202(),
			new ChunkReader_1444_1465(),
			new ChunkReader_0169_1443(),
			new ChunkReader_0_0()
	};
	
	public static ChunkReader getChunkReader(int dataVersion) {
		for(ChunkReader reader : readers)
			if(reader.supportDataVersion(dataVersion))
				return reader;
		return null;
	}
	
}
