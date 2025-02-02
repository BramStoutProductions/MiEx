package nl.bramstout.mcworldexporter.resourcepack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.molang.AnimationInfo;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.molang.MolangValue;

public class AnimationController extends Animation{
	
	public static class AnimationState{
		
		private String name;
		private List<Entry<String, MolangScript>> animations;
		private Map<String, MolangScript> transitions;
		private float blendTransitionDuration;
		private List<AnimationEvent> onEntryEvents;
		private List<AnimationEvent> onExitEvents;
		private List<AnimationVariable> variables;
		
		public AnimationState(String name, float blendTransitionDuration) {
			this.name = name;
			this.blendTransitionDuration = blendTransitionDuration;
			this.animations = new ArrayList<Entry<String, MolangScript>>();
			this.transitions = new HashMap<String, MolangScript>();
			this.onEntryEvents = new ArrayList<AnimationEvent>();
			this.onExitEvents = new ArrayList<AnimationEvent>();
			this.variables = new ArrayList<AnimationVariable>();
		}
		
		/**
		 * Evaluates the animation state and returns the name of the new state
		 * to transition to, or null if it shouldn't transition.
		 * @param time
		 * @param context
		 * @return The name of the new state to transition to, or null
		 */
		public String eval(Map<String, AnimationControllerState> states, Map<String, Animation> animations, float time, 
							float deltaTime, float globalTime, float weight, MolangContext context, Entity entity) {
			AnimationInfo animInfo = context.pushAnimationInfo();
			animInfo.animTime = time;
			animInfo.deltaTime = deltaTime;
			animInfo.globalTime = globalTime;
			animInfo.allAnimationsFinished = true;
			animInfo.anyAnimationFinished = false;
			for(AnimationControllerState state : states.values()) {
				if(state.isAnimationController)
					continue;
				if(state.finishedAnimation)
					animInfo.anyAnimationFinished = true;
				else
					animInfo.allAnimationsFinished = false;
			}
			
			// Set up animation variables
			for(AnimationVariable variable : variables) {
				context.getVariableDict().getField(context, variable.getName()).set(variable.eval(context));
			}
			// Apply animations
			for(Entry<String, MolangScript> entry : this.animations) {
				float weight2 = entry.getValue().eval(context).asNumber(context) * weight;
				if(weight2 <= 0f)
					continue;
				Animation animation = animations.getOrDefault(entry.getKey(), null);
				if(animation == null)
					continue; // No animation with the name available.
				AnimationControllerState state = states.getOrDefault(entry.getKey(), null);
				if(state == null) {
					state = new AnimationControllerState();
					states.put(entry.getKey(), state);
				}
				animation.eval(state, animations, time, deltaTime, globalTime, weight2, context, entity);
			}
			// Check if we need to transition
			for(Entry<String, MolangScript> entry : transitions.entrySet()) {
				if(entry.getValue().eval(context).asBoolean(context)) {
					context.popAnimationInfo();
					return entry.getKey(); // Transition to this
				}
			}
			context.popAnimationInfo();
			return null;
		}
		
		public void addAnimation(String animation, MolangScript weight) {
			animations.add(new Pair<String, MolangScript>(animation, weight));
		}
		
		public void addTransition(String stateName, MolangScript expression) {
			transitions.put(stateName, expression);
		}
		
		public String getName() {
			return name;
		}
		
		public float getBlendTransitionDuration() {
			return blendTransitionDuration;
		}
		
		public List<AnimationEvent> getOnEntryEvents(){
			return onEntryEvents;
		}
		
		public List<AnimationEvent> getOnExitEvents(){
			return onExitEvents;
		}
		
		public List<AnimationVariable> getVariables(){
			return variables;
		}
		
	}
	
	public static abstract class AnimationVariable{
		
		private String name;
		
		protected AnimationVariable(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public abstract MolangValue eval(MolangContext context);
		
	}
	
	public static class AnimationVariableRemap extends AnimationVariable{
		
		public MolangScript inputExpression;
		public List<Entry<Float, Float>> remapCurve;
		
		public AnimationVariableRemap(String name) {
			super(name);
			remapCurve = new ArrayList<Entry<Float, Float>>();
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(remapCurve.isEmpty())
				return new MolangValue(0f);
			
			float inputVal = inputExpression.eval(context).asNumber(context);
			
			// Get the index of the key to the right of the inputVal.
			int endIndex = -1;
			for(endIndex = 0; endIndex < remapCurve.size(); ++endIndex) {
				if(remapCurve.get(endIndex).getKey().floatValue() >= inputVal) {
					break;
				}
			}
			
			int startIndex = endIndex - 1;
			startIndex = Math.min(Math.max(startIndex, 0), remapCurve.size() - 1);
			endIndex = Math.min(Math.max(endIndex, 0), remapCurve.size() - 1);
			
			if(startIndex == endIndex)
				return new MolangValue(remapCurve.get(startIndex).getValue().floatValue());
			
			Entry<Float, Float> startKey = remapCurve.get(startIndex);
			Entry<Float, Float> endKey = remapCurve.get(endIndex);
			
			float t = (inputVal - startKey.getKey().floatValue()) / (endKey.getKey().floatValue() - startKey.getKey().floatValue());
			return new MolangValue(startKey.getValue().floatValue() * (1f - t) + endKey.getValue().floatValue() * t);
		}
		
	}
	
	public static class AnimationControllerState{
		public AnimationState currentState;
		public AnimationState prevState;
		public float stateStartTime;
		public float prevStateStartTime;
		
		public Map<String, Map<String, AnimationControllerState>> childStates;
		
		public float animTime;
		public float animStartTime;
		public float animLoopStart;
		public float animLoopDelay;
		public boolean finishedAnimation;
		public boolean isAnimationController;
		
		public AnimationControllerState() {
			currentState = null;
			prevState = null;
			stateStartTime = 0f;
			prevStateStartTime = 0f;
			childStates = new HashMap<String, Map<String, AnimationControllerState>>();
			
			animTime = 0f;
			animStartTime = -1f;
			animLoopStart = 0f;
			animLoopDelay = -1f;
			finishedAnimation = false;
			isAnimationController = false;
		}
		
		public void reset() {
			animTime = 0f;
			animStartTime = -1f;
			animLoopStart = 0f;
			animLoopDelay = -1f;
			finishedAnimation = false;
			isAnimationController = false;
			for(Map<String, AnimationControllerState> childState : childStates.values()) {
				for(AnimationControllerState state : childState.values()) {
					state.currentState = null;
					state.prevState = null;
					state.reset();
				}
			}
		}
	}
	
	protected List<AnimationState> states;
	protected String defaultState;
	protected List<MolangScript> preAnimationScripts;
	
	public AnimationController(String name) {
		super(name);
		this.defaultState = "default";
		this.states = new ArrayList<AnimationState>();
		this.preAnimationScripts = new ArrayList<MolangScript>();
	}
	
	public List<AnimationState> getStates(){
		return states;
	}
	
	public String getDefaultState() {
		return defaultState;
	}
	
	public List<MolangScript> getPreAnimationScripts(){
		return preAnimationScripts;
	}
	
	@Override
	public void eval(AnimationControllerState state, Map<String, Animation> animations, float time, float deltaTime, 
						float globalTime, float weight, MolangContext context, Entity entity) {
		state.isAnimationController = true;
		
		// Run the pre_animation scripts in case anything needs to be set up in it.
		for(MolangScript script : preAnimationScripts)
			script.eval(context);
		
		if(state.currentState == null) {
			// Initialise
			for(AnimationState state2 : states) {
				if(state2.getName().equalsIgnoreCase(defaultState)) {
					state.currentState = state2;
					state.stateStartTime = time;
					break;
				}
			}
			if(state.currentState == null) {
				return; // No default state
			}
			for(AnimationEvent event : state.currentState.getOnEntryEvents())
				event.eval(context, entity);
		}
		
		// Figure out the blending factor to transition between states.
		float t = 1f;
		if(state.prevState != null && state.prevState.getBlendTransitionDuration() > 0f) {
			t = Math.min((time - state.stateStartTime) / state.prevState.getBlendTransitionDuration(), 1f);
		}
		
		// If we are blending with the previous state, evaluate it
		if(state.prevState != null && t > 0f && t < 1f) {
			Map<String, AnimationControllerState> childStates = state.childStates.getOrDefault(state.prevState.getName(), null);
			
			state.prevState.eval(childStates, animations, time - state.prevStateStartTime, deltaTime, globalTime, 
									weight * (1f - t), context, entity);
		}
		
		// Get or set up the child states.
		Map<String, AnimationControllerState> childStates = state.childStates.getOrDefault(state.currentState.getName(), null);
		if(childStates == null) {
			childStates = new HashMap<String, AnimationControllerState>();
			state.childStates.put(state.currentState.getName(), childStates);
		}
		// Evaluate the current state.
		String newStateName = state.currentState.eval(childStates, animations, time - state.stateStartTime, deltaTime, globalTime, 
														weight * t, context, entity);
		
		if(newStateName != null) {
			AnimationState newState = null;
			for(AnimationState state2 : states) {
				if(state2.getName().equalsIgnoreCase(newStateName)) {
					newState = state2;
					break;
				}
			}
			if(newState != null) {
				state.prevState = state.currentState;
				state.prevStateStartTime = state.stateStartTime;
				state.currentState = newState;
				state.stateStartTime = time;
				state.reset();
				for(AnimationEvent event : state.prevState.getOnExitEvents())
					event.eval(context, entity);
				for(AnimationEvent event : state.currentState.getOnEntryEvents())
					event.eval(context, entity);
			}
		}
	}
	

}
