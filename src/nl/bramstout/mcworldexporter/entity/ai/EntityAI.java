package nl.bramstout.mcworldexporter.entity.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.EntityTarget.EntityTargetEntity;
import nl.bramstout.mcworldexporter.entity.ai.pathfinding.Path;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationControllerState;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;

public class EntityAI {

	private List<AIComponentGroup> componentGroups;
	private List<AIComponentGroup> activeComponentGroups;
	private List<EntityEvent> events;
	private Entity entity;
	private EntityAIHandler handler;
	private AnimationController animationController;
	private Map<String, Animation> animations;
	private AnimationControllerState animationControllerState;
	public float vx;
	public float vy;
	public float vz;
	public float newVx;
	public float newVy;
	public float newVz;
	public float jumpVy;
	public float distanceMoved;
	public float collisionBoxWidth;
	public float collisionBoxHeight;
	/**
	 * The distance moved by the entity, normalised
	 * to the movement speed attribute
	 */
	public float normalisedDistanceMoved;
	public EntityTarget target;
	public Path path;
	private List<MolangScript> initMolang;
	private MolangScript scaleXExpression;
	private MolangScript scaleYExpression;
	private MolangScript scaleZExpression;
	
	private AIComponent currentBehaviourComponent;
	
	public EntityAI(Entity entity, EntityAIHandler handler) {
		this.componentGroups = new ArrayList<AIComponentGroup>();
		this.activeComponentGroups = new ArrayList<AIComponentGroup>();
		this.events = new ArrayList<EntityEvent>();
		this.animationController = null;
		this.animations = new HashMap<String, Animation>();
		this.animationControllerState = new AnimationControllerState();
		this.entity = entity;
		this.handler = handler;
		this.target = null;
		this.path = null;
		this.initMolang = new ArrayList<MolangScript>();
		this.scaleXExpression = null;
		this.scaleYExpression = null;
		this.scaleZExpression = null;
		this.currentBehaviourComponent = null;
		this.distanceMoved = 0f;
		this.normalisedDistanceMoved = 0f;
		this.handler.setup(this);
	}
	
	public List<AIComponentGroup> getComponentGroups(){
		return componentGroups;
	}
	
	public List<AIComponentGroup> getActiveComponentGroups(){
		return activeComponentGroups;
	}
	
	public void disableComponentGroup(String name) {
		for(AIComponentGroup grp : activeComponentGroups) {
			if(grp.getName().equals(name)) {
				activeComponentGroups.remove(grp);
				return;
			}
		}
	}
	
	public void enableComponentGroup(String name) {
		for(AIComponentGroup grp : activeComponentGroups) {
			if(grp.getName().equals(name)) {
				// Already active
				return;
			}
		}
		for(AIComponentGroup grp : componentGroups) {
			if(grp.getName().equals(name)) {
				activeComponentGroups.add(grp);
				return;
			}
		}
	}
	
	public List<EntityEvent> getEvents(){
		return events;
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public AnimationController getAnimationController() {
		return animationController;
	}
	
	public void setAnimationController(AnimationController animationController) {
		this.animationController = animationController;
	}
	
	public Map<String, Animation> getAnimations(){
		return animations;
	}
	
	public void tick(float time, float deltaTime) {
		List<AIComponent> disabledComponents = new ArrayList<AIComponent>();
		
		// First get all active components. Per type there can only be a single component active.
		// Also keep track of all disabled components.
		Map<Class<? extends AIComponent>, AIComponent> activeComponents = new HashMap<Class<? extends AIComponent>, AIComponent>();
		for(AIComponentGroup componentGroup : componentGroups) {
			if(activeComponentGroups.contains(componentGroup)) {
				for(AIComponent component : componentGroup.getComponents()) {
					AIComponent prevComponent = activeComponents.getOrDefault(component.getClass(), null);
					if(prevComponent != null)
						disabledComponents.add(prevComponent);
					
					activeComponents.put(component.getClass(), component);
				}
			}else{
				for(AIComponent component : componentGroup.getComponents()) {
					disabledComponents.add(component);
				}
			}
		}
		
		NbtTag[] enabledComponentNames = new NbtTag[activeComponents.size()];
		int i = 0;
		for(AIComponent component : activeComponents.values()) {
			enabledComponentNames[i] = NbtTagString.newNonPooledInstance("", component.getName());
			i++;
		}
		entity.getProperties().addElement(NbtTagList.newNonPooledInstance("ActiveComponents", enabledComponentNames));
		
		if(target != null && target instanceof EntityTargetEntity)
			entity.getProperties().addElement(NbtTagLong.newNonPooledInstance("TargetID", ((EntityTargetEntity) target).getEntity().getUniqueId()));
		else
			entity.getProperties().addElement(NbtTagLong.newNonPooledInstance("TargetID", 0));
		
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("DistanceMoved", distanceMoved));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("NormalisedDistanceMoved", normalisedDistanceMoved));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("CollisionBoxWidth", collisionBoxWidth));
		entity.getProperties().addElement(NbtTagFloat.newNonPooledInstance("CollisionBoxHeight", collisionBoxHeight));
		
		// Do the disabled tick for the disabled components
		for(AIComponent component : disabledComponents)
			component.disabledTick(entity, time, deltaTime);
		
		// Sort the kinds of components
		List<AIComponent> behaviourComponents = new ArrayList<AIComponent>();
		List<AIComponent> movementComponents = new ArrayList<AIComponent>();
		List<AIComponent> navigationComponents = new ArrayList<AIComponent>();
		List<AIComponent> normalComponents = new ArrayList<AIComponent>();
		for(Entry<Class<? extends AIComponent>, AIComponent> entry : activeComponents.entrySet()) {
			switch(entry.getValue().getPriorityGroup()) {
			case BEHAVIOUR:
				behaviourComponents.add(entry.getValue());
				break;
			case MOVEMENT:
				movementComponents.add(entry.getValue());
				break;
			case NAVIGATION:
				navigationComponents.add(entry.getValue());
				break;
			case NONE:
				normalComponents.add(entry.getValue());
				break;
			default:
				normalComponents.add(entry.getValue());
				break;
			}
		}
		
		// First handle normal components, then behaviour, then navigation, and lastly movement
		for(AIComponent component : normalComponents)
			component.tick(entity, time, deltaTime);
		
		if(!behaviourComponents.isEmpty())
			handleBehaviourComponents(behaviourComponents, time, deltaTime);
		
		// Just grab the first navigation component if we have one
		if(!navigationComponents.isEmpty())
			navigationComponents.get(0).tick(entity, time, deltaTime);
		
		// Just grab the first movement component if we have one
		if(!movementComponents.isEmpty())
			movementComponents.get(0).tick(entity, time, deltaTime);
		
		float posX = entity.getAnimation().getAnimPosX().getKeyframeAtTime(time).value;
		float posY = entity.getAnimation().getAnimPosY().getKeyframeAtTime(time).value;
		float posZ = entity.getAnimation().getAnimPosZ().getKeyframeAtTime(time).value;
		
		{
			// Calculate target rotation
			float currentYaw = entity.getAnimation().getAnimHeadYaw().getKeyframeAtTime(time).value;
			float currentPitch = entity.getAnimation().getAnimHeadPitch().getKeyframeAtTime(time).value;
			float targetYaw = 0f;
			float targetPitch = 0f;
			float maxDelta = 150f * deltaTime;
			
			if(entity.getAI().target != null && entity.getAI().target.look) {
				float targetX = entity.getAI().target.getPosX(time);
				float targetY = entity.getAI().target.getPosY(time);
				float targetZ = entity.getAI().target.getPosZ(time);
				
				float dx = targetX - posX;
				float dy = targetY - posY;
				float dz = targetZ - posZ;
				float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
				dx /= length;
				dy /= length;
				dz /= length;
				
				targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
				targetPitch = (float) Math.toDegrees(Math.asin(dy));
				targetYaw -= entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
				targetPitch -= entity.getAnimation().getAnimPitch().getKeyframeAtTime(time).value;
				
				maxDelta = entity.getAI().target.maxRotationDelta * 20f * deltaTime;
			}
			
			float deltaYaw = targetYaw - currentYaw;
			float deltaPitch = targetPitch - currentPitch;
			// Make deltaYaw be in the range of [-180, 180]
			deltaYaw += 180f;
			deltaYaw /= 360f;
			deltaYaw -= Math.floor(deltaYaw);
			deltaYaw *= 360f;
			deltaYaw -= 180f;
			// Make deltaPitch be in the range of [-180, 180]
			deltaPitch += 180f;
			deltaPitch /= 360f;
			deltaPitch -= Math.floor(deltaPitch);
			deltaPitch *= 360f;
			deltaPitch -= 180f;
			deltaYaw = Math.min(Math.max(deltaYaw, -maxDelta), maxDelta);
			deltaPitch = Math.min(Math.max(deltaPitch, -maxDelta), maxDelta);
			
			entity.getAnimation().getAnimHeadPitch().addKeyframe(new Keyframe(time, currentPitch + deltaPitch));
			entity.getAnimation().getAnimHeadYaw().addKeyframe(new Keyframe(time, currentYaw + deltaYaw));
			currentYaw = currentYaw + deltaYaw;
			
			if(entity.getAI().target != null && entity.getAI().target.look && !entity.getAI().target.move) {
				// If we aren't moving, but we are looking, then move the entity's yaw
				// towards our target, when the head is done pointing.
				// We can check if the head is done rotating towards the target,
				// then deltaYaw is small.
				if(Math.abs(deltaYaw) < 5f) {
					// Rotate the entity
					float entityYaw = entity.getAnimation().getAnimYaw().getKeyframeAtTime(time).value;
					float targetEntityYaw = targetYaw + entityYaw;
					deltaYaw = targetEntityYaw - entityYaw;
					// Make deltaYaw be in the range of [-180, 180]
					deltaYaw += 180f;
					deltaYaw /= 360f;
					deltaYaw -= Math.floor(deltaYaw);
					deltaYaw *= 360f;
					deltaYaw -= 180f;
					maxDelta = 90f * deltaTime;
					deltaYaw = Math.min(Math.max(deltaYaw, -maxDelta), maxDelta);
					entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(time, entityYaw + deltaYaw));
					// Correct the head yaw.
					entity.getAnimation().getAnimHeadYaw().addKeyframe(new Keyframe(time, currentYaw - deltaYaw));
				}
			}
		}
		
		MolangQuery query = new MolangQuery(entity.getId(), entity.getProperties(), posX, posY, posZ);
		MolangContext context = new MolangContext(query, entity.getRandom());
		context.setVariableDict(entity.getVariables());
		
		float scale = 1f;
		NbtTagFloat scaleTag = (NbtTagFloat) entity.getProperties().get("Scale");
		if(scaleTag != null)
			scale = scaleTag.getData();
		
		float scaleX = scale;
		float scaleY = scale;
		float scaleZ = scale;
		if(entity.getScaleXExpression() != null)
			scaleX *= entity.getScaleXExpression().eval(context).asNumber(context);
		if(entity.getScaleYExpression() != null)
			scaleY *= entity.getScaleYExpression().eval(context).asNumber(context);
		if(entity.getScaleZExpression() != null)
			scaleZ *= entity.getScaleZExpression().eval(context).asNumber(context);
		if(entity.getAI().getScaleXExpression() != null)
			scaleX *= entity.getAI().getScaleXExpression().eval(context).asNumber(context);
		if(entity.getAI().getScaleYExpression() != null)
			scaleY *= entity.getAI().getScaleYExpression().eval(context).asNumber(context);
		if(entity.getAI().getScaleZExpression() != null)
			scaleZ *= entity.getAI().getScaleZExpression().eval(context).asNumber(context);
		
		entity.getAnimation().getAnimScaleX().addKeyframe(new Keyframe(time, scaleX));
		entity.getAnimation().getAnimScaleY().addKeyframe(new Keyframe(time, scaleY));
		entity.getAnimation().getAnimScaleZ().addKeyframe(new Keyframe(time, scaleZ));
		
		if(animationController != null) {
			animationController.eval(animationControllerState, animations, time, deltaTime, time, 1f, context, entity);
		}
	}
	
	private void handleBehaviourComponents(List<AIComponent> components, float time, float deltaTime) {
		// First get the bounds of the priority
		int minPriority = Integer.MAX_VALUE;
		int maxPriority = Integer.MIN_VALUE;
		for(AIComponent component : components) {
			minPriority = Math.min(minPriority, component.getPriority());
			maxPriority = Math.max(maxPriority, component.getPriority());
		}
		
		boolean hasCurrentBehaviourComponent = false;
		
		// Now go from minPriority (which is actually the highest priority) to maxPriority
		// Behaviour components return a boolean specifying if they want to grab
		// focus and become the current behaviour. The first component that returns true,
		// will become the current behaviour component.
		for(int priority = minPriority; priority <= maxPriority; ++priority) {
			if(currentBehaviourComponent != null) {
				// If there is already a current behaviour component
				// and it matches priority, check it first.
				if(priority == currentBehaviourComponent.getPriority()) {
					if(hasCurrentBehaviourComponent) {
						// We already have a current behaviour, so give it the disabledTick
						currentBehaviourComponent.disabledTick(entity, time, deltaTime);
					}else {
						if(currentBehaviourComponent.tick(entity, time, deltaTime)) {
							// It still wants to be the current behaviour component.
							hasCurrentBehaviourComponent = true;
						}
					}
				}
			}
			
			// Loop through components with this priority
			for(AIComponent component : components) {
				if(component.getPriority() != priority || component == currentBehaviourComponent)
					continue;
				
				if(hasCurrentBehaviourComponent) {
					// We already have a current behaviour, so give it the disabledTick
					component.disabledTick(entity, time, deltaTime);
				}else {
					if(component.tick(entity, time, deltaTime)) {
						// This component should now become the current behaviour component
						currentBehaviourComponent = component;
						hasCurrentBehaviourComponent = true;
					}
				}
			}
		}
	}
	
	public void fireEvent(String eventName) {
		for(EntityEvent event : events) {
			if(event.getName().equals(eventName)) {
				event.fireEvent(entity);
			}
		}
	}
	
	public List<MolangScript> getInitMolangScripts(){
		return initMolang;
	}
	
	public void setInitMolangScripts(List<MolangScript> scripts) {
		this.initMolang = scripts;
	}
	
	public MolangScript getScaleXExpression() {
		return scaleXExpression;
	}
	
	public MolangScript getScaleYExpression() {
		return scaleYExpression;
	}
	
	public MolangScript getScaleZExpression() {
		return scaleZExpression;
	}
	
	public void setScaleXExpression(MolangScript expression) {
		this.scaleXExpression = expression;
	}
	
	public void setScaleYExpression(MolangScript expression) {
		this.scaleYExpression = expression;
	}
	
	public void setScaleZExpression(MolangScript expression) {
		this.scaleZExpression = expression;
	}
	
}
