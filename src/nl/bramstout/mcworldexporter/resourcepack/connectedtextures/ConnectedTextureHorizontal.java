package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureHorizontal extends ConnectedTexture{

	public ConnectedTextureHorizontal(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction left = getLeft(up, face);
		Direction right = left.getOpposite();
		
		boolean leftConnected = connects(face, x, y, z, left.x, left.y, left.z);
		boolean rightConnected = connects(face, x, y, z, right.x, right.y, right.z);
		
		int tile = 3;
		
		if(leftConnected && rightConnected)
			tile = 1;
		else if(leftConnected)
			tile = 2;
		else if(rightConnected)
			tile = 0;
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		return tiles.get(tile);
	}
	
}
