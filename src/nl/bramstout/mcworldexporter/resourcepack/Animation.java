/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.resourcepack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BindPose;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.math.Interpolation;
import nl.bramstout.mcworldexporter.molang.AnimationInfo;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.molang.MolangValue;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationControllerState;

public class Animation {
	
	public static enum AnimationInterpolation{
		LINEAR, CATMULLROM
	}
	
	public static class AnimationKeyframe{
		
		private float time;
		private float preValue;
		private MolangScript preExpression;
		private float postValue;
		private MolangScript postExpression;
		private AnimationInterpolation interpolation;
		
		public AnimationKeyframe(float time, Object value, AnimationInterpolation interpolation) {
			this.time = time;
			this.interpolation = interpolation;
			this.preValue = 0f;
			this.postValue = 0f;
			this.preExpression = null;
			this.postExpression = null;
			if(value instanceof Number) {
				this.preValue = ((Number) value).floatValue();
				this.postValue = ((Number) value).floatValue();
			}else if(value instanceof MolangScript) {
				this.preExpression = (MolangScript) value;
				this.postExpression = (MolangScript) value;
			}
		}
		
		public AnimationKeyframe(float time, Object preValue, Object postValue, AnimationInterpolation interpolation) {
			this.time = time;
			this.interpolation = interpolation;
			this.preValue = 0f;
			this.postValue = 0f;
			this.preExpression = null;
			this.postExpression = null;
			if(preValue instanceof Number) {
				this.preValue = ((Number) preValue).floatValue();
			}else if(preValue instanceof MolangScript) {
				this.preExpression = (MolangScript) preValue;
			}
			
			if(postValue instanceof Number) {
				this.postValue = ((Number) postValue).floatValue();
			}else if(postValue instanceof MolangScript) {
				this.postExpression = (MolangScript) postValue;
			}
		}
		
		public float getTime() {
			return time;
		}
		
		public AnimationInterpolation getInterpolation() {
			return interpolation;
		}
		
		public float getPreValue(MolangContext context) {
			if(this.preExpression != null)
				return preExpression.eval(context).asNumber(context);
			return preValue;
		}
		
		public float getPostValue(MolangContext context) {
			if(this.postExpression != null)
				return postExpression.eval(context).asNumber(context);
			return postValue;
		}
		
	}
	
	public static class AnimationChannel{
		
		private List<AnimationKeyframe> keyframes;
		
		public AnimationChannel() {
			keyframes = new ArrayList<AnimationKeyframe>();
		}
		
		public void addKeyframe(AnimationKeyframe keyframe) {
			if(keyframes.isEmpty()) {
				keyframes.add(keyframe);
				return;
			}
			// Let's make sure that the keyframes are always in order
			// We assume that keyframes are added in order, so we
			// iterate in reverse to find the spot to insert it in.
			int i = keyframes.size() - 1;
			for(; i >= 0; --i) {
				// Find the keyframe that is just before the new keyframe.
				if(keyframes.get(i).time <= keyframe.time)
					break;
			}
			if(keyframes.get(Math.max(i, 0)).time == keyframe.time)
				// Override the keyframe
				keyframes.set(Math.max(i, 0), keyframe);
			else
				// Insert the new keyframe after the keyframe that we just found.
				keyframes.add(i + 1, keyframe);
		}
		
		public float eval(float time, MolangContext context) {
			if(keyframes.isEmpty())
				return 0f;
			
			int endKey = 0;
			for(; endKey < keyframes.size(); ++endKey) {
				if(keyframes.get(endKey).getTime() >= time)
					break;
			}
			// endKey now points to the closest keyframe after time
			int startKey = endKey - 1;
			startKey = Math.min(Math.max(startKey, 0), keyframes.size()-1);
			endKey = Math.min(Math.max(endKey, 0), keyframes.size()-1);
			AnimationKeyframe startKeyframe = keyframes.get(startKey);
			AnimationKeyframe endKeyframe = keyframes.get(endKey);
			if(startKeyframe.getTime() == endKeyframe.getTime()) {
				AnimationInfo animInfo = context.getAnimationInfo();
				animInfo.keyframeLerpTime = time - startKeyframe.getTime();
				return startKeyframe.getPostValue(context);
			}
			// Interpolate
			float t = (time - startKeyframe.getTime()) / (endKeyframe.getTime() - startKeyframe.getTime());
			AnimationInfo animInfo = context.getAnimationInfo();
			animInfo.keyframeLerpTime = t;
			float v0 = startKeyframe.getPostValue(context);
			float v1 = endKeyframe.getPreValue(context);
			AnimationInterpolation interpolation = startKeyframe.getInterpolation();
			if(interpolation == AnimationInterpolation.LINEAR)
				return Interpolation.linearInterpolation(v0, v1, t);
			else if(interpolation == AnimationInterpolation.CATMULLROM) {
				int beforeKey = Math.max(startKey - 1, 0);
				int afterKey = Math.min(endKey + 1, keyframes.size() - 1);
				AnimationKeyframe beforeKeyframe = keyframes.get(beforeKey);
				AnimationKeyframe afterKeyframe = keyframes.get(afterKey);
				// Add a slight offset in case we are at the start of end,
				// so that the interpolation doesn't get weird.
				float beforeKeyOffset = beforeKey == startKey ? -0.1f : 0f;
				float afterKeyOffset = afterKey == endKey ? 0.1f : 0f;
				
				return Interpolation.catmullRomInterpolation(
						beforeKeyframe.getPostValue(context), 
						v0, 
						v1, 
						afterKeyframe.getPreValue(context), 
						beforeKeyframe.getTime() + beforeKeyOffset,
						startKeyframe.getTime(), 
						endKeyframe.getTime(),
						afterKeyframe.getTime() + afterKeyOffset,
						time);
			}
			return v0;
		}
		
	}
	
	public static class BoneAnimation{
		
		private String name;
		public AnimationChannel posX;
		public AnimationChannel posY;
		public AnimationChannel posZ;
		public AnimationChannel rotX;
		public AnimationChannel rotY;
		public AnimationChannel rotZ;
		public AnimationChannel scaleX;
		public AnimationChannel scaleY;
		public AnimationChannel scaleZ;
		
		public BoneAnimation(String name) {
			this.name = name.toLowerCase();
			this.posX = null;
			this.posY = null;
			this.posZ = null;
			this.rotX = null;
			this.rotY = null;
			this.rotZ = null;
			this.scaleX = null;
			this.scaleY = null;
			this.scaleZ = null;
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	public static abstract class AnimationEvent{
		
		public float time;
		
		public abstract void eval(MolangContext context, Entity entity);
		
	}
	
	public static class AnimationEventTriggerEvent extends AnimationEvent{

		public String event;
		
		public AnimationEventTriggerEvent(float time, String event) {
			this.time = time;
			this.event = event;
		}
		
		@Override
		public void eval(MolangContext context, Entity entity) {
			if(entity.getAI() != null)
				entity.getAI().fireEvent(event);
		}
		
	}
	
	public static class AnimationEventMolang extends AnimationEvent{
		
		MolangScript code;
		
		public AnimationEventMolang(float time, MolangScript code) {
			this.time = time;
			this.code = code;
		}
		
		@Override
		public void eval(MolangContext context, Entity entity) {
			if(code != null)
				code.eval(context);
		}
		
	}
	
	protected String name;
	protected boolean loop;
	protected boolean holdOnLastFrame;
	protected MolangScript startDelay;
	protected MolangScript loopDelay;
	protected MolangScript blendWeight;
	protected MolangScript animTimeUpdate;
	protected float animationLength;
	protected boolean overridePreviousAnimation;
	protected List<BoneAnimation> bones;
	protected List<AnimationEvent> events;
	
	public Animation(String name) {
		this.name = name;
		loop = false;
		holdOnLastFrame = false;
		startDelay = MolangParser.parse("0.0");
		loopDelay = MolangParser.parse("0.0");
		blendWeight = MolangParser.parse("1.0");
		animTimeUpdate = MolangParser.parse("query.anim_time + query.delta_time");
		animationLength = 0f;
		overridePreviousAnimation = false;
		bones = new ArrayList<BoneAnimation>();
		events = new ArrayList<AnimationEvent>();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isLooping() {
		return loop;
	}
	
	public boolean isHoldingOnLastFrame() {
		return holdOnLastFrame;
	}
	
	public float getAnimationLength() {
		return animationLength;
	}
	
	public boolean isOverridingPreviousAnimation() {
		return overridePreviousAnimation;
	}
	
	public List<BoneAnimation> getBones(){
		return bones;
	}
	
	public List<AnimationEvent> getEvents(){
		return events;
	}
	
	public void setLooping(boolean loop) {
		this.loop = loop;
	}
	
	public void setStartDelay(MolangScript startDelay) {
		this.startDelay = startDelay;
	}
	
	public void setLoopDelay(MolangScript loopDelay) {
		this.loopDelay = loopDelay;
	}
	
	public void setBlendWeight(MolangScript blendWeight) {
		this.blendWeight = blendWeight;
	}
	
	public void setHoldOnLastFrame(boolean hold) {
		this.holdOnLastFrame = hold;
	}
	
	public void setAnimationLength(float animationLength) {
		this.animationLength = animationLength;
	}
	
	public void calcAnimationLength() {
		this.animationLength = 0f;
		for(BoneAnimation bone : bones) {
			calcAnimationLength(bone.posX);
			calcAnimationLength(bone.posY);
			calcAnimationLength(bone.posZ);
			calcAnimationLength(bone.rotX);
			calcAnimationLength(bone.rotY);
			calcAnimationLength(bone.rotZ);
			calcAnimationLength(bone.scaleX);
			calcAnimationLength(bone.scaleY);
			calcAnimationLength(bone.scaleZ);
		}
		for(AnimationEvent event : events)
			this.animationLength = Math.max(this.animationLength, event.time);
	}
	
	private void calcAnimationLength(AnimationChannel channel) {
		if(channel == null)
			return;
		if(channel.keyframes.isEmpty())
			return;
		this.animationLength = Math.max(this.animationLength, channel.keyframes.get(channel.keyframes.size() - 1).getTime());
	}
	
	public void setOverridePreviousAnimation(boolean override) {
		this.overridePreviousAnimation = override;
	}
	
	public void eval(AnimationControllerState state, Map<String, Animation> animations, float time, float deltaTime, 
						float globalTime, float weight, MolangContext context, Entity entity) {
		AnimationInfo animInfo = context.pushAnimationInfo();
		animInfo.animTime = state.animTime;
		
		if(state.animStartTime == -1f) {
			// Init
			state.animTime = time;
			animInfo.animTime = state.animTime;
			state.animStartTime = startDelay.eval(context).asNumber(context);
			state.animTime -= state.animStartTime;
			animInfo.animTime = state.animTime;
		}
		if(!loop && !holdOnLastFrame) {
			if(state.animTime > animationLength) {
				updateTime(state, context, animInfo);
				state.finishedAnimation = true;
				context.popAnimationInfo();
				return; // We don't hold and we've reached the end, so just don't apply this animation.
			}
		}
		
		// Evaluate events
		for(AnimationEvent event : events) {
			if(event.time >= state.animTime && event.time < (state.animTime + deltaTime)) {
				event.eval(context, entity);
			}
		}
		
		weight *= blendWeight.eval(context).asNumber(context);
		if(weight <= 0f) {
			updateTime(state, context, animInfo);
			context.popAnimationInfo();
			return; // No need to evaluate this is it doesn't do anything anyways.
		}
		float invWeight = 1f - weight;
		
		for(BoneAnimation bone : bones) {
			EntityAnimation.BoneAnimation entityAnim = entity.getAnimation().getBones().getOrDefault(bone.getName(), null);
			if(entityAnim == null) {
				entityAnim = new EntityAnimation.BoneAnimation(bone.getName());
				entity.getAnimation().getBones().put(bone.getName(), entityAnim);
			}
			BindPose bindPose = entity.getAnimation().getBindPoses().getOrDefault(bone.getName(), null);
			if(bindPose == null)
				bindPose = new BindPose("", "");
			
			if(bone.posX != null) {
				// Get the key at this global time.
				Keyframe key = entityAnim.getAnimTranslateX().getKeyframeAtExactTime(globalTime);
				// Default to the bind pose
				float value = bindPose.posX;
				if(key != null)
					// If there's already a key, use that value.
					value = key.value;
				float origValue = value;
				// Reset to bind pose if needed.
				if(overridePreviousAnimation)
					value = bindPose.posX;
				// Evaluate and add to the value.
				context.setGlobal("this", new MolangValue(value));
				value += bone.posX.eval(state.animTime, context);
				// Update the animation
				entityAnim.getAnimTranslateX().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.posY != null) {
				Keyframe key = entityAnim.getAnimTranslateY().getKeyframeAtExactTime(globalTime);
				float value = bindPose.posY;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.posY;
				context.setGlobal("this", new MolangValue(value));
				value += bone.posY.eval(state.animTime, context);
				entityAnim.getAnimTranslateY().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.posZ != null) {
				Keyframe key = entityAnim.getAnimTranslateZ().getKeyframeAtExactTime(globalTime);
				float value = bindPose.posZ;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.posZ;
				context.setGlobal("this", new MolangValue(value));
				value += bone.posZ.eval(state.animTime, context);
				entityAnim.getAnimTranslateZ().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			
			if(bone.rotX != null) {
				Keyframe key = entityAnim.getAnimRotateX().getKeyframeAtExactTime(globalTime);
				float value = bindPose.rotX;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.rotX;
				context.setGlobal("this", new MolangValue(value));
				value += bone.rotX.eval(state.animTime, context);
				entityAnim.getAnimRotateX().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.rotY != null) {
				Keyframe key = entityAnim.getAnimRotateY().getKeyframeAtExactTime(globalTime);
				float value = bindPose.rotY;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.rotY;
				context.setGlobal("this", new MolangValue(value));
				value += bone.rotY.eval(state.animTime, context);
				entityAnim.getAnimRotateY().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.rotZ != null) {
				Keyframe key = entityAnim.getAnimRotateZ().getKeyframeAtExactTime(globalTime);
				float value = bindPose.rotZ;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.rotZ;
				context.setGlobal("this", new MolangValue(value));
				value += bone.rotZ.eval(state.animTime, context);
				entityAnim.getAnimRotateZ().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			
			if(bone.scaleX != null) {
				Keyframe key = entityAnim.getAnimScaleX().getKeyframeAtExactTime(globalTime);
				float value = bindPose.scaleX;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.scaleX;
				context.setGlobal("this", new MolangValue(value));
				value += bone.scaleX.eval(state.animTime, context);
				entityAnim.getAnimScaleX().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.scaleY != null) {
				Keyframe key = entityAnim.getAnimScaleY().getKeyframeAtExactTime(globalTime);
				float value = bindPose.scaleY;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.scaleY;
				context.setGlobal("this", new MolangValue(value));
				value += bone.scaleY.eval(state.animTime, context);
				entityAnim.getAnimScaleY().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
			
			if(bone.scaleZ != null) {
				Keyframe key = entityAnim.getAnimScaleZ().getKeyframeAtExactTime(globalTime);
				float value = bindPose.scaleZ;
				if(key != null)
					value = key.value;
				float origValue = value;
				if(overridePreviousAnimation)
					value = bindPose.scaleZ;
				context.setGlobal("this", new MolangValue(value));
				value += bone.scaleZ.eval(state.animTime, context);
				entityAnim.getAnimScaleZ().addKeyframe(new Keyframe(globalTime, value * weight + origValue * invWeight));
			}
		}
		updateTime(state, context, animInfo);
		context.popAnimationInfo();
	}
	
	private void updateTime(AnimationControllerState state, MolangContext context, AnimationInfo animInfo) {
		if(loop) {
			animInfo.animTime = state.animTime;
			if(state.animTime >= animationLength) { // If we reached the end of the animation
				state.finishedAnimation = true;
				if(state.animLoopDelay == -1f) // If we haven't evaluated the delay yet, do it.
					state.animLoopDelay = loopDelay.eval(context).asNumber(context);
				if(state.animTime >= (animationLength + state.animLoopDelay)) { // We've reached the end of the delay
					// Update the loop start time and time
					state.animLoopStart = state.animTime;
					state.animTime -= state.animLoopStart - state.animStartTime;
					state.animLoopDelay = -1f; // Reset loop delay so that it gets evaluated at the end of the next loop.
					animInfo.animTime = state.animTime;
				}
			}
		}else {
			if(state.animTime >= animationLength)
				state.finishedAnimation = true;
		}
		state.animTime = animTimeUpdate.eval(context).asNumber(context);
		if(holdOnLastFrame)
			state.animTime = Math.min(state.animTime, animationLength);
	}

}
