package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil.CollisionResult;
import nl.bramstout.mcworldexporter.entity.ai.pathfinding.PathNode;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;

public class EntityMovementUtil {
	
	public static void simulatePhysics(Entity entity, float time, float deltaTime, float posX, float posY, float posZ) {
		float friction = 0.35f;
		NbtTagFloat movementFrictionModifierTag = (NbtTagFloat) entity.getProperties().get("MovementFrictionModifier");
		if(movementFrictionModifierTag != null) {
			friction *= movementFrictionModifierTag.getData();
		}
		friction = friction / (friction + 1f);
		
		if(getOnGround(entity)) {
			// Apply friction with ground
			entity.getAI().vx *= 1.0f - friction;
			entity.getAI().vy *= 1.0f - friction;
			entity.getAI().vz *= 1.0f - friction;
		}
		
		entity.getAI().vx = entity.getAI().vx * (1f - friction) + entity.getAI().newVx * friction;
		entity.getAI().vy = entity.getAI().vy * (1f - friction) + entity.getAI().newVy * friction;
		entity.getAI().vz = entity.getAI().vz * (1f - friction) + entity.getAI().newVz * friction;
		entity.getAI().vy += entity.getAI().jumpVy; // Jumping
		entity.getAI().newVx = 0f;
		entity.getAI().newVy = 0f;
		entity.getAI().newVz = 0f;
		entity.getAI().jumpVy = 0f;
		
		boolean hasGravity = false;
		NbtTagByte hasGravityTag = (NbtTagByte) entity.getProperties().get("HasGravity");
		if(hasGravityTag != null)
			hasGravity = hasGravityTag.getData() > 0;
			
		boolean isSwimming = false;
		NbtTagByte isSwimmingTag = (NbtTagByte) entity.getProperties().get("IsSwimming");
		if(isSwimmingTag != null)
			isSwimming = isSwimmingTag.getData() > 0;
		
		if(hasGravity && !isSwimming)
			entity.getAI().vy -= 30f * deltaTime; // Gravity

		float dx = entity.getAI().vx * deltaTime;
		float dy = entity.getAI().vy * deltaTime;
		float dz = entity.getAI().vz * deltaTime;
		
		boolean hasCollision = false;
		NbtTagByte hasCollisionTag = (NbtTagByte) entity.getProperties().get("HasCollision");
		if(hasCollisionTag != null)
			hasCollision = hasCollisionTag.getData() > 0;
		
		if(hasCollision) {
			solveCollisions(entity, time, deltaTime, posX, posY, posZ, dx, dy, dz);
		}else {
			entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(time, posX + dx));
			entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(time, posY + dy));
			entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(time, posZ + dz));
			entity.getAI().distanceMoved += Math.sqrt(dx * dx + dz * dz);
			float movementSpeed = getMovementSpeed(entity);
			entity.getAI().normalisedDistanceMoved += Math.sqrt(dx * dx + dy * dy) / (movementSpeed * deltaTime);
			
			entity.getProperties().addElement(NbtTagList.newNonPooledInstance("Motion", new NbtTag[] {
												NbtTagFloat.newNonPooledInstance("dX", (dx / deltaTime) / 20f),
												NbtTagFloat.newNonPooledInstance("dY", (dy / deltaTime) / 20f),
												NbtTagFloat.newNonPooledInstance("dZ", (dz / deltaTime) / 20f)}));
		}
		
		// Set a keyframe for the yaw and pitch
		float yaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(time, yaw));
		float pitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		entity.getAnimation().getAnimPitch().addKeyframe(new Keyframe(time, pitch));
	}
	
	public static void solveCollisions(Entity entity, float time, float deltaTime, float posX, float posY, float posZ,
			float dx, float dy, float dz) {
		// The distance to step through
		float maxDist = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
		// The distance of one step, with the largest step being one eight of a block.
		float stepDist = Math.min(maxDist, 1f / 8f);
		int numSteps = (int) Math.ceil(maxDist / stepDist);
		stepDist = 1f / ((float) numSteps);
		float posX2 = posX;
		float posY2 = posY;
		float posZ2 = posZ;
		float dx2 = dx;
		float dy2 = dy;
		float dz2 = dz;
		boolean onGround = false;
		CollisionResult res = new CollisionResult();
		// Iterate over the steps.
		// We start at step 1 instead of step 0,
		// since step 0 is the current location of the entity,
		// which should already be not colliding with anything.
		for(int i = 1; i <= numSteps; ++i) {
			// Update the position to the new position.
			posX2 += dx2 * stepDist;
			posY2 += dy2 * stepDist;
			posZ2 += dz2 * stepDist;
			// Get the current collision
			// It's possible that we are colliding with multiple colliders
			// at the same time, so we run out collision detection eight times
			// to make sure that we handle all collisions.
			for(int j = 0; j < 8; ++j) {
				EntityUtil.getClosestCollision(entity, posX2, posY2, posZ2, res);
				if(res.t > 0f) {
					// There was a collision, so move the entity away from the collider
					posX2 += res.nx * res.t;
					posY2 += res.ny * res.t;
					posZ2 += res.nz * res.t;
					// Also, we want to update the delta value to tell it that we shouldn't
					// continue moving in this direction.
					if(res.nx > 0f || res.nx < 0f)
						dx2 = 0f;
					else if(res.ny > 0f || res.ny < 0f)
						dy2 = 0f;
					else if(res.nz > 0f || res.nz < 0f)
						dz2 = 0f;
					
					// If we need to move upwards, that means that we are standing
					// on something and so set onGround to true.
					if(res.ny > 0f)
						onGround = true;
				}else {
					// No collision, so we can break from this inner loop.
					break;
				}
			}
			
			// Now that we, in theory, aren't colliding with anything anymore
			// we can continue to the next step.
			
			// It could be that the deltas end up at zero and so we wouldn't be
			// moving anymore. In that case, we might as well stop here for
			// performance reasons.
			if(Math.abs(dx2) < 0.0001f && Math.abs(dy2) < 0.0001f && Math.abs(dz2) < 0.0001f)
				break;
		}
		
		// We have moved the entity and handled all collisions.
		// Let's recalculate the final deltas
		dx2 = posX2 - posX;
		dy2 = posY2 - posY;
		dz2 = posZ2 - posZ;
		
		entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(time, posX2));
		entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(time, posY2));
		entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(time, posZ2));
		entity.getAI().distanceMoved += Math.sqrt(dx2 * dx2 + dz2 * dz2);
		float movementSpeed = getMovementSpeed(entity);
		entity.getAI().normalisedDistanceMoved += Math.sqrt(dx2 * dx2 + dz2 * dz2) / (movementSpeed * deltaTime);
		
		entity.getProperties().addElement(NbtTagList.newNonPooledInstance("Motion", new NbtTag[] {
													NbtTagFloat.newNonPooledInstance("dX", (dx2 / deltaTime) / 20f),
													NbtTagFloat.newNonPooledInstance("dY", (dy2 / deltaTime) / 20f),
													NbtTagFloat.newNonPooledInstance("dZ", (dz2 / deltaTime) / 20f)}));
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("OnGround", onGround ? ((byte) 1) : ((byte) 0)));
		entity.getAI().vx = dx2 / deltaTime;
		entity.getAI().vy = dy2 / deltaTime;
		entity.getAI().vz = dz2 / deltaTime;
	}
	
	private static float handleYaw(Entity entity, float time, float deltaTime, float dx, float dz, float maxTurn) {
		float length = (float) Math.sqrt(dx * dx + dz * dz);
		float dirX = dx / length;
		float dirZ = dz / length;
		float angle = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));
		
		float yaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float deltaYaw = angle - yaw;
		// Make deltaYaw be in the range of [-180, 180]
		deltaYaw += 180f;
		deltaYaw /= 360f;
		deltaYaw -= Math.floor(deltaYaw);
		deltaYaw *= 360f;
		deltaYaw -= 180f;
		maxTurn = maxTurn * 20f * deltaTime; // Update max turn value to new delta time
		deltaYaw = Math.min(Math.max(deltaYaw, -maxTurn), maxTurn);
		
		float newYaw = yaw + deltaYaw;
		entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(time, newYaw));
		return newYaw;
	}
	
	private static float handlePitch(Entity entity, float time, float deltaTime, float dx, float dy, float dz, float maxTurn) {
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float dirY = dy / length;
		float angle = (float) Math.toDegrees(Math.asin(dirY));
		
		float pitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		float deltaPitch = angle - pitch;
		maxTurn = maxTurn * 20f * deltaTime; // Update max turn value to new delta time
		deltaPitch = Math.min(Math.max(deltaPitch, -maxTurn), maxTurn);
		
		float newPitch = pitch + deltaPitch;
		entity.getAnimation().getAnimPitch().addKeyframe(new Keyframe(time, newPitch));
		return newPitch;
	}
	
	private static float getMovementSpeed(Entity entity) {
		float movementSpeed = 0.1f;
		float scale = 1f;
		NbtTagFloat tag = (NbtTagFloat) entity.getProperties().get("Scale");
		if(tag != null)
			scale = tag.getData();
		NbtTagFloat movementSpeedTag = (NbtTagFloat) entity.getProperties().get("MovementSpeed");
		if(movementSpeedTag != null)
			movementSpeed = movementSpeedTag.getData();
		movementSpeed *= 20.0f; // Speed per second
		return movementSpeed * scale;
	}
	
	private static boolean shouldJump(Entity entity, float dy) {
		float autoStepHeight = 0.5625f;
		NbtTagFloat autoStepHeightTag = (NbtTagFloat) entity.getProperties().get("AutoStepHeight");
		if(autoStepHeightTag != null)
			autoStepHeight = autoStepHeightTag.getData();
		
		boolean shouldJump = dy > autoStepHeight;
		
		boolean canJump = false;
		NbtTagByte canJumpTag = (NbtTagByte) entity.getProperties().get("CanJump");
		if(canJumpTag != null)
			canJump = canJumpTag.getData() > 0;
		
		return shouldJump && canJump;
	}
	
	private static float getJumpPower(Entity entity) {
		float jumpPower = 0.42f;
		NbtTagFloat jumpPowerTag = (NbtTagFloat) entity.getProperties().get("JumpPower");
		if(jumpPowerTag != null)
			jumpPower = jumpPowerTag.getData();
		return jumpPower;
	}
	
	public static boolean getOnGround(Entity entity) {
		boolean isOnGround = false;
		NbtTagByte onGroundTag = (NbtTagByte) entity.getProperties().get("OnGround");
		if(onGroundTag != null)
			isOnGround = onGroundTag.getData() > 0;
		return isOnGround;
	}
	
	public static void walk(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, float maxTurn) {
		float dx = target.x - posX;
		float dy = target.y - posY;
		float dz = target.z - posZ;
		
		float newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
		
		float movementSpeed = getMovementSpeed(entity);
		
		float vx = (float) -Math.sin(Math.toRadians(newYaw));
		float vz = (float) Math.cos(Math.toRadians(newYaw));
		vx *= movementSpeed;
		vz *= movementSpeed;
		
		// Check if it should jump
		float jumpPower = getJumpPower(entity);
		boolean jump = shouldJump(entity, dy);
		boolean isOnGround = getOnGround(entity);
		
		// Update the velocity
		entity.getAI().newVx = vx;
		entity.getAI().newVz = vz;
		if(jump && isOnGround) {
			entity.getAI().jumpVy = jumpPower * 20f;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsJumping", (byte) 1));
		}else {
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsJumping", (byte) 0));
		}
	}
	
	public static void swim(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, float maxTurn) {
		float dx = target.x - posX;
		float dy = target.y - posY;
		float dz = target.z - posZ;
		
		float newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
		float newPitch = handlePitch(entity, time, deltaTime, dx, dy, dz, maxTurn);
		
		float movementSpeed = getMovementSpeed(entity);
		
		float vx = (float) -Math.sin(Math.toRadians(newYaw));
		float vy = (float) Math.sin(Math.toRadians(newPitch));
		float vz = (float) Math.cos(Math.toRadians(newYaw));
		vx *= movementSpeed;
		vy *= movementSpeed;
		vz *= movementSpeed;
		
		// Update the velocity
		entity.getAI().newVx = vx;
		entity.getAI().newVy = vy;
		entity.getAI().newVz = vz;
	}
	
	public static void fly(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, float maxTurn) {
		float dx = target.x - posX;
		float dy = target.y - posY;
		float dz = target.z - posZ;
		
		float newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
		float newPitch = handlePitch(entity, time, deltaTime, dx, dy, dz, maxTurn);
		
		float movementSpeed = getMovementSpeed(entity);
		
		float vx = (float) -Math.sin(Math.toRadians(newYaw));
		float vy = (float) Math.sin(Math.toRadians(newPitch));
		float vz = (float) Math.cos(Math.toRadians(newYaw));
		vx *= movementSpeed;
		vy *= movementSpeed;
		vz *= movementSpeed;
		
		if(dy > 0f) {
			// We need to fly, so compensate for gravity if we have it.
			boolean hasGravity = false;
			NbtTagByte hasGravityTag = (NbtTagByte) entity.getProperties().get("HasGravity");
			if(hasGravityTag != null)
				hasGravity = hasGravityTag.getData() > 0;
			
			if(hasGravity) {
				entity.getAI().jumpVy = 9.81f * deltaTime; // Cancels out gravity
			}
		}
		
		// Update the velocity
		entity.getAI().newVx = vx;
		entity.getAI().newVy = vy;
		entity.getAI().newVz = vz;
	}
	
	public static void glide(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, float maxTurn) {
		float newYaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		float newPitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		if(target != null) {
			float dx = target.x - posX;
			float dy = target.y - posY;
			float dz = target.z - posZ;
			
			newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
			newPitch = handlePitch(entity, time, deltaTime, dx, dy, dz, maxTurn);
		}
		
		float movementSpeed = getMovementSpeed(entity);
		
		float vx = (float) -Math.sin(Math.toRadians(newYaw));
		float vy = (float) Math.sin(Math.toRadians(newPitch));
		float vz = (float) Math.cos(Math.toRadians(newYaw));
		vx *= movementSpeed;
		vy *= movementSpeed;
		vz *= movementSpeed;
		
		// We need to glide, so compensate for gravity if we have it.
		boolean hasGravity = false;
		NbtTagByte hasGravityTag = (NbtTagByte) entity.getProperties().get("HasGravity");
		if(hasGravityTag != null)
			hasGravity = hasGravityTag.getData() > 0;
		
		if(hasGravity) {
			entity.getAI().jumpVy = 9.81f * deltaTime * 0.9f; // Partially cancels out gravity
		}
		
		// Update the velocity
		entity.getAI().newVx = vx;
		entity.getAI().newVy = vy;
		entity.getAI().newVz = vz;
	}
	
	public static void hover(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, float maxTurn) {
		if(target != null) {
			float dx = target.x - posX;
			float dy = target.y - posY;
			float dz = target.z - posZ;
			
			float newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
			float newPitch = handlePitch(entity, time, deltaTime, dx, dy, dz, maxTurn);
			
			float movementSpeed = getMovementSpeed(entity);
			
			float vx = (float) -Math.sin(Math.toRadians(newYaw));
			float vy = (float) Math.sin(Math.toRadians(newPitch));
			float vz = (float) Math.cos(Math.toRadians(newYaw));
			vx *= movementSpeed;
			vy *= movementSpeed;
			vz *= movementSpeed;
			
			// Update the velocity
			entity.getAI().newVx = vx;
			entity.getAI().newVy = vy;
			entity.getAI().newVz = vz;
		}
		
		// We need to hover, so compensate for gravity if we have it.
		boolean hasGravity = false;
		NbtTagByte hasGravityTag = (NbtTagByte) entity.getProperties().get("HasGravity");
		if(hasGravityTag != null)
			hasGravity = hasGravityTag.getData() > 0;
		
		if(hasGravity) {
			entity.getAI().jumpVy = 9.81f * deltaTime; // Cancels out gravity
		}
	}
	
	public static boolean jump(Entity entity, float time, float deltaTime, Vector3f target, float posX, float posY, float posZ, 
								float maxTurn, boolean canJump) {
		float dx = target.x - posX;
		//float dy = ((float) target.getY()) - posY;
		float dz = target.z - posZ;
		
		float newYaw = handleYaw(entity, time, deltaTime, dx, dz, maxTurn);
		
		float movementSpeed = getMovementSpeed(entity);
		
		float vx = (float) -Math.sin(Math.toRadians(newYaw));
		float vz = (float) Math.cos(Math.toRadians(newYaw));
		vx *= movementSpeed;
		vz *= movementSpeed;
		
		// Check if it should jump
		float jumpPower = getJumpPower(entity);
		
		boolean jump = false;
		NbtTagByte canJumpTag = (NbtTagByte) entity.getProperties().get("CanJump");
		if(canJumpTag != null)
			jump = canJumpTag.getData() > 0;
		
		boolean isOnGround = getOnGround(entity);
		
		// Update the velocity
		if(canJump && jump && isOnGround) {
			entity.getAI().newVx = vx;
			entity.getAI().newVz = vz;
			entity.getAI().jumpVy = jumpPower * 20f;
			entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsJumping", (byte) 1));
			return true;
		}
		entity.getProperties().addElement(NbtTagByte.newNonPooledInstance("IsJumping", (byte) 0));
		return false;
	}
	
	public static Vector3f getNextPathTarget(Entity entity, float posX, float posY, float posZ) {
		int closestNodeIndex = entity.getAI().path.getClosestNode(posX, posY, posZ);
		int targetNodeIndex = Math.min(closestNodeIndex + 1, entity.getAI().path.getSize() - 1);
		if(targetNodeIndex < 0) {
			// Invalid index, so we just don't move.
			return null;
		}
		
		// Average out the next 3 nodes to smooth out the path
		Vector3f targetPos = new Vector3f();
		float totalWeight = 0f;
		for(int i = targetNodeIndex; i < Math.min(targetNodeIndex + 3, entity.getAI().path.getSize()); ++i) {
			PathNode target = entity.getAI().path.getNode(i);
			targetPos.x += target.getX() + 0.5f;
			targetPos.y += target.getY();
			targetPos.z += target.getZ() + 0.5f;
			totalWeight += 1f;
		}
		
		targetPos = targetPos.divide(totalWeight);
		
		Vector3f distance = targetPos.subtract(new Vector3f(posX, posY, posZ));
		if(distance.length() < 0.5f)
			return null; // Reached the end.
		
		return targetPos;
	}

}
