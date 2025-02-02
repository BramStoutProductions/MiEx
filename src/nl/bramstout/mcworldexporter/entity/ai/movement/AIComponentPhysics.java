package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;

public class AIComponentPhysics extends AIComponent{

	/**
	 * Whether or not the entity collides with things.
	 */
	public boolean hasCollision;
	/**
	 * Whether or not the entity is affected by gravity.
	 */
	public boolean hasGravity;
	/**
	 * Whether or not the entity should be pushed towards
	 * the nearest open area when stuck inside a block.
	 */
	public boolean pushTowardsClosestSpace;
	
	public AIComponentPhysics(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("HasCollision", hasCollision ? ((byte) 1) : ((byte) 0)));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("HasGravity", hasGravity ? ((byte) 1) : ((byte) 0)));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("PushTowardsClosestSpace", pushTowardsClosestSpace ? ((byte) 1) : ((byte) 0)));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("HasCollision", (byte) 0));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("HasGravity", (byte) 0));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("PushTowardsClosestSpace", (byte) 0));
	}

}
