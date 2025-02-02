package nl.bramstout.mcworldexporter.entity.ai.movement;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;

public class AIComponentBodyRotationBlocked extends AIComponent{
	
	private float lockedYaw;
	private float lockedPitch;
	
	public AIComponentBodyRotationBlocked(String name) {
		super(name, PriorityGroup.NONE, 0, 4);
		lockedYaw = Float.NaN;
		lockedPitch = Float.NaN;
	}

	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		if(Float.isNaN(lockedYaw) || Float.isNaN(lockedPitch)) {
			lockedYaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
			lockedPitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
		}
		entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(time, lockedYaw));
		entity.getAnimation().getAnimPitch().addKeyframe(new Keyframe(time, lockedPitch));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		lockedYaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
		lockedPitch = entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
	}

}
