package nl.bramstout.mcworldexporter.resourcepack;

import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public abstract class BlockStateHandler {
	
	public abstract BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, BlockState state);
	
	public abstract String getDefaultTexture();
	
	public abstract boolean needsConnectionInfo();
	
}
