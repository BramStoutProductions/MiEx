package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public class ConnectedTextureFull extends ConnectedTexture{

	private boolean innerSeams;
	
	public ConnectedTextureFull(String name, int priority) {
		super(name, priority);
		innerSeams = false;
	}

	@Override
	public String getTexture(int x, int y, int z, ModelFace face) {
		Direction up = getUp(face);
		
		int connectionBits = calcConnectionBits(x, y, z, face, up);
		int tile = CtmUtils.ConnectionDataToTile[connectionBits];
		
		// TODO: Implement innerSeams
		
		if(tile < 0 || tile >= tiles.size())
			return null;
		return tiles.get(tile);
	}
	
	public boolean getInnerSeams() {
		return innerSeams;
	}
	
	public void setInnerSeams(boolean innerSeams) {
		this.innerSeams = innerSeams;
	}

}
