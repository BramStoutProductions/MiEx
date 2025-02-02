package nl.bramstout.mcworldexporter.entity.ai.property;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.molang.MolangScript;

public class AIComponentRideable extends AIComponent{

	public static class Seat{
		
		/**
		 * Angle in degrees that a rider is allowed to rotate
		 * while riding this entity.
		 */
		public float lockRiderRotation;
		/**
		 * Defines the maximum number of riders that can be riding
		 * this entiity for this seat to be valid.
		 */
		public int maxRiderCount;
		/**
		 * Defines the minimum number of riders that need to be riding
		 * this entity before this seat can be used.
		 */
		public int minRiderCount;
		/**
		 * Position X of this seat relative to this entity's position.
		 */
		public float posX;
		/**
		 * Position Y of this seat relative to this entity's position.
		 */
		public float posY;
		/**
		 * Position Z of this seat relative to this entity's position.
		 */
		public float posZ;
		/**
		 * Offset to rotate riders by.
		 */
		public MolangScript rotateRiderBy;
		
		public Seat copy() {
			Seat seat = new Seat();
			seat.lockRiderRotation = lockRiderRotation;
			seat.maxRiderCount = maxRiderCount;
			seat.minRiderCount = minRiderCount;
			seat.posX = posX;
			seat.posY = posY;
			seat.posZ = posZ;
			seat.rotateRiderBy = rotateRiderBy;
			return seat;
		}
		
	}
	
	/**
	 * List of entities that can ride this entity.
	 */
	public List<String> familyTypes;
	/**
	 * The maximum width a mob can be to be a passenger.
	 * A value of 0 ignores this paramter.
	 */
	public float passengerMaxWidth;
	/**
	 * If true, the entity will pull in entities that
	 * are in the correct familyTypes into any
	 * available seats.
	 */
	public boolean pullInEntities;
	/**
	 * The list of positions and number of riders for
	 * each position for entities riding this entity.
	 */
	public List<Seat> seats;
	
	public AIComponentRideable(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		familyTypes = new ArrayList<String>();
		seats = new ArrayList<Seat>();
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {}

}
