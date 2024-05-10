package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;

public abstract class ConnectedTexture {
	
	private String name;
	private int priority;
	protected List<String> tiles;
	private Set<Direction> facesToConnect;
	private ConnectLogic connectLogic;
	
	public ConnectedTexture(String name, int priority) {
		this.name = name;
		this.priority = priority;
		this.tiles = new ArrayList<String>();
		this.facesToConnect = new HashSet<Direction>();
		this.connectLogic = null;
	}
	
	public abstract String getTexture(int x, int y, int z, ModelFace face);
	
	protected boolean connects(ModelFace face, int x, int y, int z, int dx, int dy, int dz) {
		if(!facesToConnect.contains(face.getDirection()))
			return false;
		if(connectLogic == null)
			return false;
		return connectLogic.connects(face, x, y, z, dx, dy, dz);
	}
	
	protected int calcConnectionBits(int x, int y, int z, ModelFace face, Direction up) {
		Direction left = getLeft(up, face);
		Direction down = up.getOpposite();
		Direction right = left.getOpposite();
		
		int res = 0;
		
		if(connects(face, x, y, z, up.x + left.x, up.y + left.y, up.z + left.z))
			res |= 1 << 7;
		
		if(connects(face, x, y, z, up.x, up.y, up.z))
			res |= 1 << 6;
		
		if(connects(face, x, y, z, up.x + right.x, up.y + right.y, up.z + right.z))
			res |= 1 << 5;
		
		if(connects(face, x, y, z, right.x, right.y, right.z))
			res |= 1 << 4;
		
		if(connects(face, x, y, z, down.x + right.x, down.y + right.y, down.z + right.z))
			res |= 1 << 3;
		
		if(connects(face, x, y, z, down.x, down.y, down.z))
			res |= 1 << 2;
		
		if(connects(face, x, y, z, down.x + left.x, down.y + left.y, down.z + left.z))
			res |= 1 << 1;
		
		if(connects(face, x, y, z, left.x, left.y, left.z))
			res |= 1;
		
		return res;
	}
	
	protected Direction getUp(ModelFace face) {
		float v0 = face.getUVs()[1];
		float vRight = face.getUVs()[3];
		float vUp = face.getUVs()[7];
		
		float dvRight = vRight - v0;
		float dvUp = vUp - v0;
		
		float x0 = face.getPoints()[0];
		float y0 = face.getPoints()[1];
		float z0 = face.getPoints()[2];
		float x1 = face.getPoints()[3];
		float y1 = face.getPoints()[4];
		float z1 = face.getPoints()[5];
		boolean mirrored = v0 > vRight;
		if(Math.abs(dvUp) > Math.abs(dvRight)) {
			// Vertex 0 and 3 form a vertical line
			// rather than vertex 0 and 1.
			x1 = face.getPoints()[9];
			y1 = face.getPoints()[10];
			z1 = face.getPoints()[11];
			mirrored = v0 > vUp;
		}
		
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		float dLength = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(dLength > 0.00001f) {
			dx /= dLength;
			dy /= dLength;
			dz /= dLength;
		}
		
		// Now we need to find a direction that matches it the most.
		Direction dir = null;
		float minDist = 100000000000f;
		for(Direction testDir : Direction.CACHED_VALUES) {
			float testDx = (((float) testDir.x) - dx);
			float testDy = (((float) testDir.y) - dy);
			float testDz = (((float) testDir.z) - dz);
			float testDist = testDx * testDx + testDy * testDy + testDz * testDz;
			if(testDist < minDist) {
				dir = testDir;
				minDist = testDist;
			}
		}
		if(dir == null)
			dir = Direction.UP;
		if(mirrored)
			dir = dir.getOpposite();
		return dir;
	}
	
	protected Direction getLeft(Direction up, ModelFace face) {
		float u0 = face.getUVs()[0];
		float uRight = face.getUVs()[2];
		float uUp = face.getUVs()[6];
		
		float duRight = uRight - u0;
		float duUp = uUp - u0;
		
		boolean mirrored = u0 > uRight;
		if(Math.abs(duUp) > Math.abs(duRight)) {
			// Vertex 0 and 3 form a horizontal line
			// rather than vertex 0 and 1.
			mirrored = u0 > uUp;
		}
		
		Direction res = Direction.UP;
		switch(face.getDirection()) {
		case NORTH:
			switch(up) {
			case UP:
				res = Direction.EAST;
				break;
			case DOWN:
				res = Direction.WEST;
				break;
			case EAST:
				res = Direction.DOWN;
				break;
			case WEST:
				res = Direction.UP;
				break;
			default:
				break;
			}
			break;
		case SOUTH:
			switch(up) {
			case UP:
				res = Direction.WEST;
				break;
			case DOWN:
				res = Direction.EAST;
				break;
			case EAST:
				res = Direction.UP;
				break;
			case WEST:
				res = Direction.DOWN;
				break;
			default:
				break;
			}
			break;
		case EAST:
			switch(up) {
			case UP:
				res = Direction.SOUTH;
				break;
			case DOWN:
				res = Direction.NORTH;
				break;
			case NORTH:
				res = Direction.UP;
				break;
			case SOUTH:
				res = Direction.DOWN;
				break;
			default:
				break;
			}
			break;
		case WEST:
			switch(up) {
			case UP:
				res = Direction.NORTH;
				break;
			case DOWN:
				res = Direction.SOUTH;
				break;
			case NORTH:
				res = Direction.DOWN;
				break;
			case SOUTH:
				res = Direction.UP;
				break;
			default:
				break;
			}
			break;
		case UP:
			switch(up) {
			case NORTH:
				res = Direction.WEST;
				break;
			case SOUTH:
				res = Direction.EAST;
				break;
			case EAST:
				res = Direction.NORTH;
				break;
			case WEST:
				res = Direction.SOUTH;
				break;
			default:
				break;
			}
			break;
		case DOWN:
			switch(up) {
			case NORTH:
				res = Direction.EAST;
				break;
			case SOUTH:
				res = Direction.WEST;
				break;
			case EAST:
				res = Direction.SOUTH;
				break;
			case WEST:
				res = Direction.NORTH;
				break;
			default:
				break;
			}
			break;
		}
		
		if(mirrored)
			res = res.getOpposite();
		return res;
	}
	
	public String getName() {
		return name;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public List<String> getTiles(){
		return tiles;
	}
	
	public Set<Direction> getFacesToConnect(){
		return facesToConnect;
	}
	
	public ConnectLogic getConnectLogic() {
		return connectLogic;
	}
	
	public void setConnectLogic(ConnectLogic logic) {
		this.connectLogic = logic;
	}
	
}
