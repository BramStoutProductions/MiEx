package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureVerticalHorizontal extends ConnectedTexture{
	
	public ConnectedTextureVerticalHorizontal(String name, int priority) {
		super(name, priority);
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction down = up.getOpposite();
		Direction left = getLeft(up, face);
		Direction right = left.getOpposite();
		
		int tile = getTile(face, x, y, z, up, down);
		
		if(tile == 3) {
			// Check if we can connect vertically.
			int tileLeft = getTile(face, x + left.x, y + left.y, z + left.z, up, down);
			int tileRight = getTile(face, x + right.x, y + right.y, z + right.z, up, down);
			
			boolean leftConnected = tileLeft == 3;
			boolean rightConnected = tileRight == 3;
			
			if(leftConnected && rightConnected)
				tile = 5;
			else if(leftConnected)
				tile = 6;
			else if(rightConnected)
				tile = 4;
		}
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		
		return tiles.get(tile);
	}
	
	private int getTile(ModelFace face, int x, int y, int z, Direction up, Direction down) {
		boolean upConnected = connects(face, x, y, z, up.x, up.y, up.z);
		boolean downConnected = connects(face, x, y, z, down.x, down.y, down.z);
		
		int tile = 3;
		
		if(upConnected && downConnected)
			tile = 1;
		else if(upConnected)
			tile = 0;
		else if(downConnected)
			tile = 2;
		
		return tile;
	}
	
}
