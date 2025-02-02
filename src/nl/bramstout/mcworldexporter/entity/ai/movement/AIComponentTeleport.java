package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetBlock;
import nl.bramstout.mcworldexporter.entity.ai.EntityUtil;

public class AIComponentTeleport extends AIComponent{

	/**
	 * Modifies the chance that the entity will teleport if the entity
	 * is in darkness.
	 */
	public float darkTeleportChance;
	/**
	 * Modifies the chance that the entity will teleport if the entity
	 * is in daylight.
	 */
	public float lightTeleportChance;
	/**
	 * Maximum amount of time in seconds between random teleports.
	 */
	public float maxRandomTeleportTime;
	/**
	 * Minimum amount of time in seconds between random teleports.
	 */
	public float minRandomTeleportTime;
	/**
	 * Width of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeWidth;
	/**
	 * Height of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeHeight;
	/**
	 * Depth of the cube that the entity will teleport within.
	 */
	public float randomTeleportCubeDepth;
	/**
	 * If true, the entity will teleport randomly.
	 */
	public boolean randomTeleports;
	/**
	 * Maximum distance the entity will teleport when chasing a target.
	 */
	public float targetDistance;
	/**
	 * The chance that the entity will teleport, per tick when chasing a target, 
	 * between 0.0 and 1.0. 1.0 means 100%
	 */
	public float targetTeleportChance;
	
	private float nextRandomTeleport;
	
	public AIComponentTeleport(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		nextRandomTeleport = -1f;
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(randomTeleports) {
			if(time >= nextRandomTeleport) {
				if(nextRandomTeleport != -1f) {
					float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
					float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
					float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
					EntityTargetBlock target = EntityUtil.FindTarget((int) (randomTeleportCubeWidth/2f), (int) (randomTeleportCubeHeight/2f), 
												entity, posX, posY, posZ);
					if(target != null) {
						entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(time, target.getPosX(time)));
						entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(time, target.getPosY(time)));
						entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(time, target.getPosZ(time)));
					}
				}
				nextRandomTeleport = entity.getRandom().nextFloat() * (maxRandomTeleportTime - minRandomTeleportTime) + 
										minRandomTeleportTime + time;
			}
		}
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		nextRandomTeleport = -1f;
	}

}
