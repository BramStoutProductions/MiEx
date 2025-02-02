package nl.bramstout.mcworldexporter.export;

public class IndividualBlockId {
	
	private int blockId;
	private int x;
	private int y;
	private int z;
	
	public IndividualBlockId(int blockId, int x, int y, int z) {
		this.blockId = blockId;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getBlockId() {
		return blockId;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	@Override
	public int hashCode() {
		return ((blockId * 31 + x) * 31 + y) * 31 + z;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof IndividualBlockId))
			return false;
		return ((IndividualBlockId) obj).blockId == blockId && 
				((IndividualBlockId) obj).x == x && 
				((IndividualBlockId) obj).y == y && 
				((IndividualBlockId) obj).z == z;
	}
	
}
