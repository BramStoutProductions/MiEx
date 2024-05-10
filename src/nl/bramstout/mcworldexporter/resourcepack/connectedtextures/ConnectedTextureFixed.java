package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureFixed extends ConnectedTexture{

	public ConnectedTextureFixed(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		if(tiles.size() <= 0)
			return null;
		return tiles.get(0);
	}
}
