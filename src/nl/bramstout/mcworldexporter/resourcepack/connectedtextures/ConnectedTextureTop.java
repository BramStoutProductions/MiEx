package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureTop extends ConnectedTexture{

	public ConnectedTextureTop(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		
		if(tiles.size() <= 0)
			return null;
		
		if(connects(face, x, y, z, up.x, up.y, up.z))
			return tiles.get(0);
		
		return null;
	}

}
