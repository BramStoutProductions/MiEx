package nl.bramstout.mcworldexporter.entity.ai;

import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.entity.Entity;

public class AIComponentPreferredPath extends AIComponent{
	
	/**
	 * Cost for non-preferred blocks.
	 */
	public float defaultBlockCost;
	/**
	 * Added cost for jumping up a node.
	 */
	public float jumpCost;
	/**
	 * Distance entity can fall without taking damage.
	 */
	public float maxFallBlocks;
	/**
	 * A list of blocks with their associated cost.
	 */
	public Map<String, Float> preferredPathBlocks;
	
	public AIComponentPreferredPath(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		preferredPathBlocks = new HashMap<String, Float>();
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
