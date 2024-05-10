package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureOverlay extends ConnectedTexture{

	public ConnectedTextureOverlay(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		
		int connectionBits = calcConnectionBits(x, y, z, face, up);
		int tile = CtmUtils.OverlayDataToTile[connectionBits];
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		
		return tiles.get(tile);
	}
	
}
