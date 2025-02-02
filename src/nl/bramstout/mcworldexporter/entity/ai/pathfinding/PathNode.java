package nl.bramstout.mcworldexporter.entity.ai.pathfinding;

public class PathNode {
	
	private int x;
	private int y;
	private int z;
	private float cost;
	private float totalCost;
	private PathNode prevNode;
	
	public PathNode() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.cost = 0f;
		this.totalCost = 0f;
		this.prevNode = null;
	}
	
	public PathNode(int x, int y, int z, float cost, float totalCost, PathNode prevNode) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.cost = cost;
		this.totalCost = totalCost;
		this.prevNode = prevNode;
	}
	
	public PathNode(PathNode other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.cost = other.cost;
		this.totalCost = other.totalCost;
		this.prevNode = other.prevNode;
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
	
	public float getCost() {
		return cost;
	}
	
	public float getTotalCost() {
		return totalCost;
	}
	
	public PathNode getPrevNode() {
		return prevNode;
	}
	
	public void setCost(float cost, float totalCost) {
		this.cost = cost;
		this.totalCost = totalCost;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PathNode)
			return x == ((PathNode)obj).x && y == ((PathNode)obj).y && z == ((PathNode)obj).z;
		return false;
	}

}
