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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Quaternion;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;

public class BlockAnimationHandlerHytale extends BlockAnimationHandler{

	private boolean holdLastKeyframe;
	private Map<String, NodeAnimation> nodeAnimations;
	
	public BlockAnimationHandlerHytale(JsonObject data) {
		duration = 0f;
		if(data.has("duration"))
			// Hytale duration is in frames at 60 FPS, and we want it in seconds.
			duration = data.get("duration").getAsFloat() / 60f;
		
		holdLastKeyframe = false;
		if(data.has("holdLastKeyframe"))
			holdLastKeyframe = data.get("holdLastKeyframe").getAsBoolean();
		
		nodeAnimations = new HashMap<String, NodeAnimation>();
		if(data.has("nodeAnimations")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("nodeAnimations").entrySet()) {
				if(entry.getValue().isJsonObject()) {
					nodeAnimations.put(entry.getKey(), new NodeAnimation(entry.getKey(), 
											entry.getValue().getAsJsonObject(), duration, holdLastKeyframe));
				}
			}
		}
		
		animatesTopology = false;
		animatesPoints = false;
		animatesUVs = false;
		animatesVertexColors = false;
		for(NodeAnimation anim : nodeAnimations.values()) {
			if(anim.position != null)
				animatesPoints = true;
			if(anim.orientation != null)
				animatesPoints = true;
			if(anim.shapeStretch != null)
				animatesPoints = true;
			if(anim.shapeVisible != null)
				animatesTopology = true;
			if(anim.shapeUvOffset != null)
				animatesUVs = true;
		}
	}
	
	public boolean getHoldLastKeyframe() {
		return holdLastKeyframe;
	}
	
	public NodeAnimation getNodeAnimation(String name) {
		NodeAnimation anim = nodeAnimations.getOrDefault(name, null);		
		return anim;
	}
	
	public static class NodeAnimation{
		
		@SuppressWarnings("unused")
		private String name;
		private AnimationChannel position;
		private AnimationChannel orientation;
		private AnimationChannel shapeStretch;
		private AnimationChannel shapeVisible;
		private AnimationChannel shapeUvOffset;
		
		public NodeAnimation(String name, JsonObject data, float duration, boolean holdLastKeyframe) {
			this.name = name;
			position = null;
			orientation = null;
			shapeStretch = null;
			shapeVisible = null;
			shapeUvOffset = null;
			
			if(data.has("position")) {
				JsonArray keyframes = data.getAsJsonArray("position");
				if(keyframes.size() > 0)
					position = new AnimationChannel(keyframes, duration, holdLastKeyframe);
			}
			if(data.has("orientation")) {
				JsonArray keyframes = data.getAsJsonArray("orientation");
				if(keyframes.size() > 0)
					orientation = new AnimationChannel(keyframes, duration, holdLastKeyframe);
			}
			if(data.has("shapeStretch")) {
				JsonArray keyframes = data.getAsJsonArray("shapeStretch");
				if(keyframes.size() > 0)
					shapeStretch = new AnimationChannel(keyframes, duration, holdLastKeyframe);
			}
			if(data.has("shapeVisible")) {
				JsonArray keyframes = data.getAsJsonArray("shapeVisible");
				if(keyframes.size() > 0)
					shapeVisible = new AnimationChannel(keyframes, duration, holdLastKeyframe);
			}
			if(data.has("shapeUvOffset")) {
				JsonArray keyframes = data.getAsJsonArray("shapeUvOffset");
				if(keyframes.size() > 0)
					shapeUvOffset = new AnimationChannel(keyframes, duration, holdLastKeyframe);
			}
		}
		
		public AnimationChannel getPosition() {
			return position;
		}
		
		public AnimationChannel getOrientation() {
			return orientation;
		}
		
		public AnimationChannel getShapeStretch() {
			return shapeStretch;
		}
		
		public AnimationChannel getShapeVisible() {
			return shapeVisible;
		}
		
		public AnimationChannel getShapeUvOffset() {
			return shapeUvOffset;
		}
		
	}
	
	public static class AnimationChannel{
		
		private List<Keyframe> keyframes;
		private float duration;
		private boolean holdLastKeyframe;
		
		public AnimationChannel(JsonArray data, float duration, boolean holdLastKeyframe) {
			keyframes = new ArrayList<Keyframe>();
			this.duration = duration;
			this.holdLastKeyframe = holdLastKeyframe;
			for(JsonElement el : data.asList()) {
				if(el.isJsonObject()) {
					keyframes.add(new Keyframe(el.getAsJsonObject()));
				}
			}
		}
		
		public void eval(float frame, Vector3f out) {
			if(keyframes.isEmpty())
				return;
			
			if(holdLastKeyframe && frame >= keyframes.get(keyframes.size()-1).time) {
				interpolateHold(frame, out, keyframes.get(keyframes.size()-1));
				return;
			}
			
			// Wrap it
			frame %= duration;
			if(frame < 0f)
				frame += duration;
			
			for(int i = 0; i < keyframes.size(); ++i) {
				Keyframe keyframe = keyframes.get(i);
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				if(keyframe.time <= frame && (keyframe2.time > frame || keyframe2.time < keyframe.time)) {
					// We're on the keyframe.
					
					if(keyframe.interpolation == InterpolationType.HOLD) {
						interpolateHold(frame, out, keyframe);
					}else if(keyframe.interpolation == InterpolationType.LINEAR) {
						interpolateLinear(frame, out, keyframe, keyframe2);
					}else if(keyframe.interpolation == InterpolationType.SMOOTH) {
						Keyframe keyframe0 = keyframes.get((i-1 + keyframes.size()) % keyframes.size());
						Keyframe keyframe3 = keyframes.get((i+2) % keyframes.size());
						interpolateSmooth(frame, out, keyframe0, keyframe, keyframe2, keyframe3);
					}
					return;
					
				}
			}
			
			frame += duration;
			int i = keyframes.size()-1;
			Keyframe keyframe = keyframes.get(i);
			if(keyframe.interpolation == InterpolationType.HOLD) {
				interpolateHold(frame, out, keyframe);
			}else if(keyframe.interpolation == InterpolationType.LINEAR) {
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				interpolateLinear(frame, out, keyframe, keyframe2);
			}else if(keyframe.interpolation == InterpolationType.SMOOTH) {
				Keyframe keyframe0 = keyframes.get((i-1 + keyframes.size()) % keyframes.size());
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				Keyframe keyframe3 = keyframes.get((i+2) % keyframes.size());
				interpolateSmooth(frame, out, keyframe0, keyframe, keyframe2, keyframe3);
			}
		}
		
		public void eval(float frame, Quaternion out) {
			if(keyframes.isEmpty())
				return;
			
			if(holdLastKeyframe && frame >= keyframes.get(keyframes.size()-1).time) {
				interpolateHold(frame, out, keyframes.get(keyframes.size()-1));
				return;
			}
			
			// Wrap it
			frame %= duration;
			if(frame < 0f)
				frame += duration;
			
			for(int i = 0; i < keyframes.size(); ++i) {
				Keyframe keyframe = keyframes.get(i);
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				if(keyframe.time <= frame && (keyframe2.time > frame || keyframe2.time < keyframe.time)) {
					// We're on the keyframe.
					
					if(keyframe.interpolation == InterpolationType.HOLD) {
						interpolateHold(frame, out, keyframe);
					}else if(keyframe.interpolation == InterpolationType.LINEAR) {
						interpolateLinear(frame, out, keyframe, keyframe2);
					}else if(keyframe.interpolation == InterpolationType.SMOOTH) {
						Keyframe keyframe0 = keyframes.get((i-1 + keyframes.size()) % keyframes.size());
						Keyframe keyframe3 = keyframes.get((i+2) % keyframes.size());
						interpolateSmooth(frame, out, keyframe0, keyframe, keyframe2, keyframe3);
					}
					return;
					
				}
			}
			
			frame += duration;
			int i = keyframes.size()-1;
			Keyframe keyframe = keyframes.get(i);
			if(keyframe.interpolation == InterpolationType.HOLD) {
				interpolateHold(frame, out, keyframe);
			}else if(keyframe.interpolation == InterpolationType.LINEAR) {
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				interpolateLinear(frame, out, keyframe, keyframe2);
			}else if(keyframe.interpolation == InterpolationType.SMOOTH) {
				Keyframe keyframe0 = keyframes.get((i-1 + keyframes.size()) % keyframes.size());
				Keyframe keyframe2 = keyframes.get((i+1) % keyframes.size());
				Keyframe keyframe3 = keyframes.get((i+2) % keyframes.size());
				interpolateSmooth(frame, out, keyframe0, keyframe, keyframe2, keyframe3);
			}
		}
		
		private void interpolateHold(float frame, Vector3f out, Keyframe keyframe1) {
			out.x = keyframe1.x;
			out.y = keyframe1.y;
			out.z = keyframe1.z;
		}
		
		private void interpolateHold(float frame, Quaternion out, Keyframe keyframe1) {
			out.x = keyframe1.x;
			out.y = keyframe1.y;
			out.z = keyframe1.z;
			out.w = keyframe1.w;
		}
		
		private void interpolateLinear(float frame, Vector3f out, Keyframe keyframe1, Keyframe keyframe2) {
			float time0 = keyframe1.time;
			float time1 = keyframe2.time;
			if(time1 < time0)
				// In case we're looping back
				time1 += duration;

			out.x = interpolateLinear(frame, time0, keyframe1.x, time1, keyframe2.x);
			out.y = interpolateLinear(frame, time0, keyframe1.y, time1, keyframe2.y);
			out.z = interpolateLinear(frame, time0, keyframe1.z, time1, keyframe2.z);
		}
		
		private void interpolateLinear(float frame, Quaternion out, Keyframe keyframe1, Keyframe keyframe2) {
			float time0 = keyframe1.time;
			float time1 = keyframe2.time;
			if(time1 < time0)
				// In case we're looping back
				time1 += duration;

			out.x = interpolateLinear(frame, time0, keyframe1.x, time1, keyframe2.x);
			out.y = interpolateLinear(frame, time0, keyframe1.y, time1, keyframe2.y);
			out.z = interpolateLinear(frame, time0, keyframe1.z, time1, keyframe2.z);
			out.w = interpolateLinear(frame, time0, keyframe1.w, time1, keyframe2.w);
		}
		
		private float interpolateLinear(float frame, float t0, float v0, float t1, float v1) {
			float t = (frame - t0) / Math.max(t1 - t0, 0.00001f);
			return v0 + (v1 - v0) * t;
		}
		
		private void interpolateSmooth(float frame, Vector3f out, Keyframe keyframe0, Keyframe keyframe1,
										Keyframe keyframe2, Keyframe keyframe3) {
			float time0 = keyframe0.time;
			float time1 = keyframe1.time;
			float time2 = keyframe2.time;
			float time3 = keyframe3.time;
			if(time0 > time1)
				// In case we're looping forward
				time0 -= duration;
			if(time2 < time1)
				// In case we're looping back
				time2 += duration;
			while(time3 < time2 && duration > 0.00001f)
				// In case we're looping back and even multiple times
				time3 += duration;
			
			out.x = interpolateSmooth(frame, time0, keyframe0.x, time1, keyframe1.x,
										time2, keyframe2.x, time3, keyframe3.x);
			out.y = interpolateSmooth(frame, time0, keyframe0.y, time1, keyframe1.y,
										time2, keyframe2.y, time3, keyframe3.y);
			out.z = interpolateSmooth(frame, time0, keyframe0.z, time1, keyframe1.z,
										time2, keyframe2.z, time3, keyframe3.z);
		}
		
		private void interpolateSmooth(float frame, Quaternion out, Keyframe keyframe0, Keyframe keyframe1,
										Keyframe keyframe2, Keyframe keyframe3) {
			float time0 = keyframe0.time;
			float time1 = keyframe1.time;
			float time2 = keyframe2.time;
			float time3 = keyframe3.time;
			if(time0 > time1)
				// In case we're looping forward
				time0 -= duration;
			if(time2 < time1)
				// In case we're looping back
				time2 += duration;
			while(time3 < time2 && duration > 0.00001f)
				// In case we're looping back and even multiple times
				time3 += duration;
			
			out.x = interpolateSmooth(frame, time0, keyframe0.x, time1, keyframe1.x,
							time2, keyframe2.x, time3, keyframe3.x);
			out.y = interpolateSmooth(frame, time0, keyframe0.y, time1, keyframe1.y,
							time2, keyframe2.y, time3, keyframe3.y);
			out.z = interpolateSmooth(frame, time0, keyframe0.z, time1, keyframe1.z,
							time2, keyframe2.z, time3, keyframe3.z);
			out.w = interpolateSmooth(frame, time0, keyframe0.w, time1, keyframe1.w,
							time2, keyframe2.w, time3, keyframe3.w);
		}
		
		private float interpolateSmooth(float frame, float t0, float v0, float t1, float v1,
										float t2, float v2, float t3, float v3) {
			float m1 = (v2 - v0) / 2f;
			float m2 = (v3 - v1) / 2f;
			float p1 = v1;
			float p2 = v2;
			float t = (frame - t1) / Math.max(t2 - t1, 0.00001f);
			
			return (2f*t*t*t - 3f*t*t + 1f) * p1 +
					(t*t*t - 2f*t*t + t) * m1 +
					(-2f*t*t*t + 3f*t*t) * p2 +
					(t*t*t - t*t) * m2;
		}
		
	}
	
	public static class Keyframe{
		
		public float time;
		public float x;
		public float y;
		public float z;
		public float w;
		public InterpolationType interpolation;
		
		public Keyframe(JsonObject data) {
			time = 0f;
			x = 0f;
			y = 0f;
			z = 0f;
			w = 1f;
			interpolation = InterpolationType.HOLD;
			if(data.has("time"))
				// Convert from 60fps to seconds
				time = data.get("time").getAsFloat() / 60f;
			if(data.has("delta")) {
				JsonElement delta = data.get("delta");
				if(delta.isJsonPrimitive())
					x = delta.getAsBoolean() ? 1f : 0f;
				else if(delta.isJsonObject()) {
					JsonObject deltaObj = delta.getAsJsonObject();
					if(deltaObj.has("x"))
						x = deltaObj.get("x").getAsFloat();
					if(deltaObj.has("y"))
						y = deltaObj.get("y").getAsFloat();
					if(deltaObj.has("z"))
						z = deltaObj.get("z").getAsFloat();
					if(deltaObj.has("w"))
						w = deltaObj.get("w").getAsFloat();
				}
			}
			if(data.has("interpolationType")) {
				String interpolationStr = data.get("interpolationType").getAsString();
				if(interpolationStr.equals("linear"))
					interpolation = InterpolationType.LINEAR;
				else if(interpolationStr.equals("smooth"))
					interpolation = InterpolationType.SMOOTH;
			}
		}
		
	}
	
	public static enum InterpolationType{
		HOLD,
		LINEAR,
		SMOOTH
	}
	
}
