package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.resourcepack.Animation;

public class AnimationBedrock extends Animation{

	public AnimationBedrock(String name, JsonObject data) {
		super(name);
		
		if(data.has("loop")) {
			JsonElement el = data.get("loop");
			if(el.isJsonPrimitive()) {
				JsonPrimitive prim = el.getAsJsonPrimitive();
				if(prim.isBoolean()) {
					this.loop = prim.getAsBoolean();
				}else if(prim.isString()) {
					this.loop = false;
					this.holdOnLastFrame = true;
				}
			}
		}
		
		if(data.has("start_delay")) {
			this.startDelay = MolangParser.parse(data.get("start_delay").getAsString());
		}
		
		if(data.has("loop_delay")) {
			this.loopDelay = MolangParser.parse(data.get("loop_delay").getAsString());
		}
		
		if(data.has("blend_weight")) {
			this.blendWeight = MolangParser.parse(data.get("blend_weight").getAsString());
		}
		
		if(data.has("anim_time_update")) {
			this.animTimeUpdate = MolangParser.parse(data.get("anim_time_update").getAsString());
		}
		
		if(data.has("override_previous_animation")) {
			this.overridePreviousAnimation = data.get("override_previous_animation").getAsBoolean();
		}
		
		if(data.has("bones")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("bones").entrySet()) {
				parseBone(entry.getKey(), entry.getValue().getAsJsonObject());
			}
		}
		
		if(data.has("timeline")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("timeline").entrySet()) {
				parseAnimationEvent(entry.getKey(), entry.getValue());
			}
		}
		
		if(data.has("animation_length")) {
			this.animationLength = data.get("animation_length").getAsFloat();
		}else {
			calcAnimationLength();
		}
	}
	
	private void parseBone(String name, JsonObject data) {
		BoneAnimation bone = new BoneAnimation(name);
		this.bones.add(bone);
		
		if(data.has("position")) {
			bone.posX = new AnimationChannel();
			bone.posY = new AnimationChannel();
			bone.posZ = new AnimationChannel();
			parseAnimationChannel(data.get("position"), bone.posX, bone.posY, bone.posZ);
		}
		
		if(data.has("rotation")) {
			bone.rotX = new AnimationChannel();
			bone.rotY = new AnimationChannel();
			bone.rotZ = new AnimationChannel();
			parseAnimationChannel(data.get("rotation"), bone.rotX, bone.rotY, bone.rotZ);
		}
		
		if(data.has("scale")) {
			bone.scaleX = new AnimationChannel();
			bone.scaleY = new AnimationChannel();
			bone.scaleZ = new AnimationChannel();
			parseAnimationChannel(data.get("scale"), bone.scaleX, bone.scaleY, bone.scaleZ);
		}
	}
	
	private void parseAnimationChannel(JsonElement data, AnimationChannel channelX, AnimationChannel channelY, AnimationChannel channelZ) {
		if(data.isJsonPrimitive() || data.isJsonArray()) {
			parseAnimationChannelKeyframe(data, 0f, channelX, channelY, channelZ);
		}else if(data.isJsonObject()) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
				try {
					float time = Float.parseFloat(entry.getKey());
					parseAnimationChannelKeyframe(entry.getValue(), time, channelX, channelY, channelZ);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void parseAnimationChannelKeyframe(JsonElement data, float time, AnimationChannel channelX, AnimationChannel channelY, 
												AnimationChannel channelZ) {
		if(data.isJsonPrimitive()) {
			Object value = parseKeyframeValue(data);
			channelX.addKeyframe(new AnimationKeyframe(time, value, AnimationInterpolation.LINEAR));
			channelY.addKeyframe(new AnimationKeyframe(time, value, AnimationInterpolation.LINEAR));
			channelZ.addKeyframe(new AnimationKeyframe(time, value, AnimationInterpolation.LINEAR));
		}else if(data.isJsonArray()) {
			Object valueX = null;
			Object valueY = null;
			Object valueZ = null;
			JsonArray array = data.getAsJsonArray();
			boolean isObjects = false;
			for(JsonElement el : array.asList()) {
				if(el.isJsonObject()) {
					isObjects = true;
					break;
				}
			}
			if(isObjects) {
				valueX = Float.valueOf(0f);
				valueY = Float.valueOf(0f);
				valueZ = Float.valueOf(0f);
				for(JsonElement el : array.asList()) {
					for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
						if(entry.getKey().equalsIgnoreCase("x"))
							valueX = parseKeyframeValue(entry.getValue());
						else if(entry.getKey().equalsIgnoreCase("y"))
							valueY = parseKeyframeValue(entry.getValue());
						else if(entry.getKey().equalsIgnoreCase("z"))
							valueZ = parseKeyframeValue(entry.getValue());
					}
				}
			}else {
				if(array.size() <= 0)
					return;
				else if(array.size() == 1) {
					valueX = parseKeyframeValue(array.get(0));
					valueY = valueX;
					valueZ = valueX;
				}else if(array.size() == 2) {
					valueX = parseKeyframeValue(array.get(0));
					valueY = parseKeyframeValue(array.get(1));
					valueZ = valueX;
				}else if(array.size() >= 3) {
					valueX = parseKeyframeValue(array.get(0));
					valueY = parseKeyframeValue(array.get(1));
					valueZ = parseKeyframeValue(array.get(2));
				}
			}
			channelX.addKeyframe(new AnimationKeyframe(time, valueX, AnimationInterpolation.LINEAR));
			channelY.addKeyframe(new AnimationKeyframe(time, valueY, AnimationInterpolation.LINEAR));
			channelZ.addKeyframe(new AnimationKeyframe(time, valueZ, AnimationInterpolation.LINEAR));
		}else if(data.isJsonObject()) {
			JsonObject obj = data.getAsJsonObject();
			AnimationInterpolation interpolation = AnimationInterpolation.LINEAR;
			if(obj.has("lerp_mode")) {
				if(obj.get("lerp_mode").getAsString().equals("catmullrom"))
					interpolation = AnimationInterpolation.CATMULLROM;
			}
			
			Object preValueX = null;
			Object preValueY = null;
			Object preValueZ = null;
			Object postValueX = null;
			Object postValueY = null;
			Object postValueZ = null;
			
			if(obj.has("pre")) {
				JsonElement preEl = obj.get("pre");
				if(preEl.isJsonPrimitive()) {
					preValueX = parseKeyframeValue(preEl);
					preValueY = preValueX;
					preValueZ = preValueX;
				}else if(preEl.isJsonArray()) {
					JsonArray preArray = preEl.getAsJsonArray();
					if(preArray.size() == 1) {
						preValueX = parseKeyframeValue(preArray.get(0));
						preValueY = preValueX;
						preValueZ = preValueX;
					}else if(preArray.size() == 2) {
						preValueX = parseKeyframeValue(preArray.get(0));
						preValueY = parseKeyframeValue(preArray.get(1));
						preValueZ = preValueX;
					}else if(preArray.size() >= 3) {
						preValueX = parseKeyframeValue(preArray.get(0));
						preValueY = parseKeyframeValue(preArray.get(1));
						preValueZ = parseKeyframeValue(preArray.get(2));
					}
				}
				postValueX = preValueX;
				postValueY = preValueY;
				postValueZ = preValueZ;
			}
			
			if(obj.has("post")) {
				JsonElement postEl = obj.get("post");
				if(postEl.isJsonPrimitive()) {
					postValueX = parseKeyframeValue(postEl);
					postValueY = postValueX;
					postValueZ = postValueX;
				}else if(postEl.isJsonArray()) {
					JsonArray postArray = postEl.getAsJsonArray();
					if(postArray.size() == 1) {
						postValueX = parseKeyframeValue(postArray.get(0));
						postValueY = postValueX;
						postValueZ = postValueX;
					}else if(postArray.size() == 2) {
						postValueX = parseKeyframeValue(postArray.get(0));
						postValueY = parseKeyframeValue(postArray.get(1));
						postValueZ = postValueX;
					}else if(postArray.size() >= 3) {
						postValueX = parseKeyframeValue(postArray.get(0));
						postValueY = parseKeyframeValue(postArray.get(1));
						postValueZ = parseKeyframeValue(postArray.get(2));
					}
				}
				if(preValueX == null)
					preValueX = postValueX;
				if(preValueY == null)
					preValueY = postValueY;
				if(preValueZ == null)
					preValueZ = postValueZ;
			}
			
			if(preValueX == null || preValueY == null || preValueZ == null || 
					postValueX == null || postValueY == null || postValueZ == null)
				return;
			
			channelX.addKeyframe(new AnimationKeyframe(time, preValueX, postValueX, interpolation));
			channelY.addKeyframe(new AnimationKeyframe(time, preValueY, postValueY, interpolation));
			channelZ.addKeyframe(new AnimationKeyframe(time, preValueZ, postValueZ, interpolation));
		}
	}
	
	private Object parseKeyframeValue(JsonElement data) {
		JsonPrimitive prim = data.getAsJsonPrimitive();
		if(prim.isNumber())
			return Float.valueOf(prim.getAsFloat());
		else if(prim.isString())
			return MolangParser.parse(prim.getAsString());
		return 0f;
	}
	
	private void parseAnimationEvent(String name, JsonElement data) {
		try {
			float time = Float.parseFloat(name);
			
			if(data.isJsonPrimitive()) {
				String code = data.getAsString();
				if(code.startsWith("@s")) {
					this.events.add(new AnimationEventTriggerEvent(time, code.substring(3)));
				}else if(code.startsWith("/")){
					// Ignore commands for now
				}else {
					this.events.add(new AnimationEventMolang(time, MolangParser.parse(code)));
				}
			}else if(data.isJsonArray()) {
				for(JsonElement el : data.getAsJsonArray().asList()) {
					String code = el.getAsString();
					if(code.startsWith("@s")) {
						this.events.add(new AnimationEventTriggerEvent(time, code.substring(3)));
					}else if(code.startsWith("/")){
						// Ignore commands for now
					}else {
						this.events.add(new AnimationEventMolang(time, MolangParser.parse(code)));
					}
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
