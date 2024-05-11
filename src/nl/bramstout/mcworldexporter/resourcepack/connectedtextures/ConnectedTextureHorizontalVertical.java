package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureHorizontalVertical extends ConnectedTexture{

	public ConnectedTextureHorizontalVertical(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction left = getLeft(up, face);
		Direction right = left.getOpposite();
		Direction down = up.getOpposite();
		
		int tile = getTile(face, x, y, z, left, right);
		
		if(tile == 3) {
			// Check if we can connect vertically.
			int tileUp = getTile(face, x + up.x, y + up.y, z + up.z, left, right);
			int tileDown = getTile(face, x + down.x, y + down.y, z + down.z, left, right);
			
			boolean upConnected = tileUp == 3;
			boolean downConnected = tileDown == 3;
			
			if(upConnected && downConnected)
				tile = 5;
			else if(upConnected)
				tile = 4;
			else if(downConnected)
				tile = 6;
		}
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		return tiles.get(tile);
	}
	
	private int getTile(ModelFace face, int x, int y, int z, Direction left, Direction right) {
		boolean leftConnected = connects(face, x, y, z, left.x, left.y, left.z);
		boolean rightConnected = connects(face, x, y, z, right.x, right.y, right.z);
		
		int tile = 3;
		
		if(leftConnected && rightConnected)
			tile = 1;
		else if(leftConnected)
			tile = 2;
		else if(rightConnected)
			tile = 0;
		
		return tile;
	}
	
}
