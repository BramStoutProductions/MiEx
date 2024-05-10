package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureVertical extends ConnectedTexture{

	public ConnectedTextureVertical(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction down = up.getOpposite();
		
		boolean upConnected = connects(face, x, y, z, up.x, up.y, up.z);
		boolean downConnected = connects(face, x, y, z, down.x, down.y, down.z);
		
		int tile = 3;
		
		if(upConnected && downConnected)
			tile = 1;
		else if(upConnected)
			tile = 0;
		else if(downConnected)
			tile = 2;
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		
		return tiles.get(tile);
	}
	
}
