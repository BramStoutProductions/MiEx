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
		
		int connectionBits = calcConnectionBits(x, y, z, face, up, false);
		int tile = CtmUtils.OverlayDataToTile[connectionBits];
		
		if(tile < 0 || tile >= tiles.size())
			return DELETE_FACE;
		
		String tileStr = tiles.get(tile);
		if(tileStr == null)
			return DELETE_FACE;
		return tileStr;
	}
	
	@Override
	public boolean isOverlay() {
		return true;
	}
	
}
