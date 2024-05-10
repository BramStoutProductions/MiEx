package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureRepeat extends ConnectedTexture{
	
	private int width;
	private int height;
	
	public ConnectedTextureRepeat(String name, int priority) {
		super(name, priority);
		width = 1;
		height = 1;
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		Direction left = getLeft(up, face);
		
		int u = 0;
		int v = 0;
		if(left == Direction.EAST || left == Direction.WEST)
			u = x;
		if(left == Direction.UP || left == Direction.DOWN)
			u = y;
		if(left == Direction.NORTH || left == Direction.SOUTH)
			u = z;
		if(up == Direction.EAST || up == Direction.WEST)
			v = x;
		if(up == Direction.UP || up == Direction.DOWN)
			v = y;
		if(up == Direction.NORTH || up == Direction.SOUTH)
			v = z;
		
		u = u % width;
		v = v % height;
		if(u < 0)
			u += width;
		if(v < 0)
			v += height;
		
		int tileId = v * width + u;
		
		if(tileId < 0 || tileId > tiles.size())
			return null;
		
		return tiles.get(tileId);
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
}
