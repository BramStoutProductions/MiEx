package nl.bramstout.mcworldexporter.entity.ai.pathfinding;

public interface PathFinderHook {

	/**
	 * Returns the cost of traversing this specific block.
	 * If this block should not be visited, returns
	 * a negative number.
	 * @param x X coordinate of block
	 * @param y Y coordinate of block
	 * @param z Z coordinate of block
	 * @return The cost of the block
	 */
	public float getCost(int x, int y, int z, int dx, int dy, int dz);
	
	/**
	 * Returns the additional cost to help guide the pathfinding.
	 * @param x X coordinate of the current block
	 * @param y Y coordinate of the current block
	 * @param z Z coordinate of the current block
	 * @param goalX X coordinate of the target block
	 * @param goalY Y coordinate of the target block
	 * @param goalZ Z coordinate of the target block
	 * @return The additional cost
	 */
	public float getAdditionalCost(int x, int y, int z, int goalX, int goalY, int goalZ);
	
}
