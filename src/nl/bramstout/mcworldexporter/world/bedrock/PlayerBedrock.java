package nl.bramstout.mcworldexporter.world.bedrock;

import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.world.Player;

public class PlayerBedrock extends Player{

	public PlayerBedrock(String uuid, NbtTagCompound data) {
		super(uuid, data, 0, 0, 0, getDimension(data), false);
		
		NbtTag posTag = data.get("Pos");
		if(posTag != null && posTag instanceof NbtTagList) {
			if(((NbtTagList) posTag).getSize() >= 3) {
				NbtTagFloat xTag = (NbtTagFloat) ((NbtTagList)posTag).get(0);
				NbtTagFloat yTag = (NbtTagFloat) ((NbtTagList)posTag).get(1);
				NbtTagFloat zTag = (NbtTagFloat) ((NbtTagList)posTag).get(2);
				this.x = xTag.getData();
				this.y = yTag.getData();
				this.z = zTag.getData();
			}
		}
	}
	
	private static String getDimension(NbtTagCompound data) {
		NbtTag dimensionIdTag = data.get("DimensionId");
		if(dimensionIdTag == null)
			return "overworld";
		if(dimensionIdTag instanceof NbtTagInt) {
			int id = ((NbtTagInt) dimensionIdTag).getData();
			if(id == 0)
				return "overworld";
			else if(id == 1)
				return "the_nether";
			else if(id == 2)
				return "the_end";
		}
		return "overworld";
	}

}
