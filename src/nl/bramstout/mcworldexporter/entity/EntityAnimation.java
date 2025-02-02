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

package nl.bramstout.mcworldexporter.entity;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;

public class EntityAnimation {
	
	public static class Keyframe{
		
		public float time;
		public float value;
		
		public Keyframe(float time, float value) {
			this.time = time;
			this.value = value;
		}
		
		public void write(DataOutput dos, float offset, float scale) throws IOException{
			dos.writeFloat(time);
			dos.writeFloat((value + offset) * scale);
		}
		
		public void read(DataInput dis) throws IOException{
			time = dis.readFloat();
			value = dis.readFloat();
		}
	}
	
	public static class Keyframe3D{
		
		public float time;
		public float valueX;
		public float valueY;
		public float valueZ;
		
		public Keyframe3D(float time, float valueX, float valueY, float valueZ) {
			this.time = time;
			this.valueX = valueX;
			this.valueY = valueY;
			this.valueZ = valueZ;
		}
		
		public void write(DataOutput dos, float offsetX, float offsetY, float offsetZ, float scale) throws IOException{
			dos.writeFloat(time);
			dos.writeFloat((valueX + offsetX) * scale);
			dos.writeFloat((valueY + offsetY) * scale);
			dos.writeFloat((valueZ + offsetZ) * scale);
		}
		
		public void read(DataInput dis) throws IOException{
			time = dis.readFloat();
			valueX = dis.readFloat();
			valueY = dis.readFloat();
			valueZ = dis.readFloat();
		}
		
	}
	
	public static class AnimationChannel{
		
		private List<Keyframe> keyframes;
		
		public AnimationChannel() {
			keyframes = new ArrayList<Keyframe>();
		}
		
		public void write(DataOutput dos, float offset, float scale) throws IOException{
			dos.writeInt(keyframes.size());
			for(Keyframe key : keyframes)
				key.write(dos, offset, scale);
		}
		
		public void read(DataInput dis) throws IOException{
			int numKeyframes = dis.readInt();
			for(int i = 0; i < numKeyframes; ++i) {
				Keyframe keyframe = new Keyframe(0f, 0f);
				keyframe.read(dis);
				keyframes.add(keyframe);
			}
		}
		
		public void addKeyframe(Keyframe keyframe) {
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
				keyframes.get(Math.max(i, 0)).value = keyframe.value;
			else
				// Insert the new keyframe after the keyframe that we just found.
				keyframes.add(i + 1, keyframe);
		}
		
		public List<Keyframe> getKeyframes(){
			return keyframes;
		}
		
		public Keyframe getLatestValue() {
			if(keyframes.isEmpty())
				return new Keyframe(0f, 0f);
			return keyframes.get(keyframes.size() - 1);
		}
		
		public Keyframe getKeyframeAtExactTime(float time) {
			for(Keyframe keyframe : keyframes)
				if(keyframe.time == time)
					return keyframe;
			return null;
		}
		
		public Keyframe getKeyframeAtTime(float time) {
			if(keyframes.isEmpty())
				return new Keyframe(time, 0f);
			
			int endKey = 0;
			for(; endKey < keyframes.size(); ++endKey) {
				if(keyframes.get(endKey).time >= time)
					break;
			}
			// endKey now points to the closest keyframe after time
			int startKey = endKey - 1;
			startKey = Math.min(Math.max(startKey, 0), keyframes.size()-1);
			endKey = Math.min(Math.max(endKey, 0), keyframes.size()-1);
			Keyframe startKeyframe = keyframes.get(startKey);
			Keyframe endKeyframe = keyframes.get(endKey);
			if(startKeyframe.time == endKeyframe.time)
				return startKeyframe;
			// Interpolate
			float t = (time - startKeyframe.time) / (endKeyframe.time - startKeyframe.time);
			return new Keyframe(time, startKeyframe.value * (1f - t) + endKeyframe.value * t);
		}
		
		public Keyframe getClosestKeyframeAtTime(float time) {
			if(keyframes.isEmpty())
				return new Keyframe(time, 0f);
			
			int endKey = 0;
			for(; endKey < keyframes.size(); ++endKey) {
				if(keyframes.get(endKey).time >= time)
					break;
			}
			// endKey now points to the closest keyframe after time
			int startKey = endKey - 1;
			startKey = Math.min(Math.max(startKey, 0), keyframes.size()-1);
			endKey = Math.min(Math.max(endKey, 0), keyframes.size()-1);
			Keyframe startKeyframe = keyframes.get(startKey);
			Keyframe endKeyframe = keyframes.get(endKey);
			
			float startDist = Math.abs(startKeyframe.time - time);
			float endDist = Math.abs(endKeyframe.time - time);
			
			return startDist <= endDist ? startKeyframe : endKeyframe;
		}
		
	}
	
	public static class AnimationChannel3D{
		
		private List<Keyframe3D> keyframes;
		
		public AnimationChannel3D(float time, float x, float y, float z) {
			keyframes = new ArrayList<Keyframe3D>();
			keyframes.add(new Keyframe3D(time, x, y, z));
		}
		
		public AnimationChannel3D(AnimationChannel channelX, AnimationChannel channelY, AnimationChannel channelZ) {
			keyframes = new ArrayList<Keyframe3D>();
			
			int iX = 0;
			int iY = 0;
			int iZ = 0;
			float time = 0f;
			while(true) {
				float timeX = Float.MAX_VALUE;
				float timeY = Float.MAX_VALUE;
				float timeZ = Float.MAX_VALUE;
				
				if(channelX != null && iX < channelX.getKeyframes().size())
					timeX = channelX.getKeyframes().get(iX).time;
				
				if(channelY != null && iY < channelY.getKeyframes().size())
					timeY = channelY.getKeyframes().get(iY).time;
				
				if(channelZ != null && iZ < channelZ.getKeyframes().size())
					timeZ = channelZ.getKeyframes().get(iZ).time;
				
				time = Math.min(Math.min(timeX, timeY), timeZ);
				
				if(time == Float.MAX_VALUE)
					break; // Reached the end
				
				if(time == timeX)
					iX++;
				if(time == timeY)
					iY++;
				if(time == timeZ)
					iZ++;
				
				float valueX = 0f;
				float valueY = 0f;
				float valueZ = 0f;
				if(channelX != null)
					valueX = channelX.getKeyframeAtTime(time).value;
				if(channelY != null)
					valueY = channelY.getKeyframeAtTime(time).value;
				if(channelZ != null)
					valueZ = channelZ.getKeyframeAtTime(time).value;
				keyframes.add(new Keyframe3D(time, valueX, valueY, valueZ));
			}
		}
		
		public void write(DataOutput dos, float offsetX, float offsetY, float offsetZ, float scale) throws IOException{
			dos.writeInt(keyframes.size());
			for(Keyframe3D key : keyframes)
				key.write(dos, offsetX, offsetY, offsetZ, scale);
		}
		
		public void read(DataInput dis) throws IOException{
			int numKeyframes = dis.readInt();
			for(int i = 0; i < numKeyframes; ++i) {
				Keyframe3D keyframe = new Keyframe3D(0f, 0f, 0f, 0f);
				keyframe.read(dis);
				keyframes.add(keyframe);
			}
		}
		
		public List<Keyframe3D> getKeyframes(){
			return keyframes;
		}
		
	}
	
	public static class BoneAnimation{
		
		private String name;
		private AnimationChannel animTranslateX;
		private AnimationChannel animTranslateY;
		private AnimationChannel animTranslateZ;
		private AnimationChannel animRotateX;
		private AnimationChannel animRotateY;
		private AnimationChannel animRotateZ;
		private AnimationChannel animScaleX;
		private AnimationChannel animScaleY;
		private AnimationChannel animScaleZ;
		private AnimationChannel animVisibility;
		
		public BoneAnimation(String name) {
			this.name = name;
			this.animTranslateX = new AnimationChannel();
			this.animTranslateY = new AnimationChannel();
			this.animTranslateZ = new AnimationChannel();
			this.animRotateX = new AnimationChannel();
			this.animRotateY = new AnimationChannel();
			this.animRotateZ = new AnimationChannel();
			this.animScaleX = new AnimationChannel();
			this.animScaleY = new AnimationChannel();
			this.animScaleZ = new AnimationChannel();
			this.animVisibility = new AnimationChannel();
		}

		public String getName() {
			return name;
		}

		public AnimationChannel getAnimTranslateX() {
			return animTranslateX;
		}

		public AnimationChannel getAnimTranslateY() {
			return animTranslateY;
		}

		public AnimationChannel getAnimTranslateZ() {
			return animTranslateZ;
		}

		public AnimationChannel getAnimRotateX() {
			return animRotateX;
		}

		public AnimationChannel getAnimRotateY() {
			return animRotateY;
		}

		public AnimationChannel getAnimRotateZ() {
			return animRotateZ;
		}

		public AnimationChannel getAnimScaleX() {
			return animScaleX;
		}

		public AnimationChannel getAnimScaleY() {
			return animScaleY;
		}

		public AnimationChannel getAnimScaleZ() {
			return animScaleZ;
		}

		public AnimationChannel getAnimVisibility() {
			return animVisibility;
		}
		
		public AnimationChannel3D getAnimPos3D() {
			return new AnimationChannel3D(animTranslateX, animTranslateY, animTranslateZ);
		}
		
		public AnimationChannel3D getAnimRotation3D() {
			return new AnimationChannel3D(animRotateX, animRotateY, animRotateZ);
		}
		
		public AnimationChannel3D getAnimScale3D() {
			return new AnimationChannel3D(animScaleX, animScaleY, animScaleZ);
		}
		
	}
	
	public static class BindPose{
		
		private String name;
		private String parent;
		public float posX;
		public float posY;
		public float posZ;
		public float rotX;
		public float rotY;
		public float rotZ;
		public float scaleX;
		public float scaleY;
		public float scaleZ;
		public float pivotX;
		public float pivotY;
		public float pivotZ;
		
		public BindPose(String name, String parent) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public String getParent() {
			return parent;
		}
		
		public boolean isIdentity() {
			return posX == 0f && posY == 0f && posZ == 0f && 
					rotX == 0f && rotY == 0f && rotZ == 0f && 
					scaleX == 0f && scaleY == 0 && scaleZ == 0;
		}
		
	}
	
	public static class Locator{
		
		private String name;
		public Vector3f offset;
		public Vector3f rotation;
		public boolean ignoreInheritedScale;
		public String bone;
		
		public Locator(String name) {
			this.name = name;
			offset = new Vector3f();
			rotation = new Vector3f();
			ignoreInheritedScale = false;
			bone = "";
		}
		
		public Matrix getLocalMatrix() {
			return Matrix.translate(offset).mult(Matrix.rotate(rotation));
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	private AnimationChannel animPosX;
	private AnimationChannel animPosY;
	private AnimationChannel animPosZ;
	private AnimationChannel animYaw;
	private AnimationChannel animPitch;
	private AnimationChannel animHeadYaw;
	private AnimationChannel animHeadPitch;
	private AnimationChannel animScaleX;
	private AnimationChannel animScaleY;
	private AnimationChannel animScaleZ;
	private Map<String, BoneAnimation> bones;
	private Map<String, BindPose> bindPoses;
	private Map<String, Locator> locators;
	
	public EntityAnimation() {
		this.animPosX = new AnimationChannel();
		this.animPosY = new AnimationChannel();
		this.animPosZ = new AnimationChannel();
		this.animYaw = new AnimationChannel();
		this.animPitch = new AnimationChannel();
		this.animHeadYaw = new AnimationChannel();
		this.animHeadPitch = new AnimationChannel();
		this.animScaleX = new AnimationChannel();
		this.animScaleY = new AnimationChannel();
		this.animScaleZ = new AnimationChannel();
		this.bones = new HashMap<String, BoneAnimation>();
		this.bindPoses = new HashMap<String, BindPose>();
		this.locators = new HashMap<String, Locator>();
	}

	public AnimationChannel getAnimPosX() {
		return animPosX;
	}

	public AnimationChannel getAnimPosY() {
		return animPosY;
	}

	public AnimationChannel getAnimPosZ() {
		return animPosZ;
	}

	public AnimationChannel getAnimYaw() {
		return animYaw;
	}

	public AnimationChannel getAnimPitch() {
		return animPitch;
	}
	
	public AnimationChannel getAnimHeadYaw() {
		return animHeadYaw;
	}

	public AnimationChannel getAnimHeadPitch() {
		return animHeadPitch;
	}
	
	public AnimationChannel getAnimScaleX() {
		return animScaleX;
	}
	
	public AnimationChannel getAnimScaleY() {
		return animScaleY;
	}
	
	public AnimationChannel getAnimScaleZ() {
		return animScaleZ;
	}

	public Map<String, BoneAnimation> getBones() {
		return bones;
	}
	
	public Map<String, BindPose> getBindPoses(){
		return bindPoses;
	}
	
	public Map<String, Locator> getLocators(){
		return locators;
	}
	
	public AnimationChannel3D getAnimPos3D() {
		return new AnimationChannel3D(animPosX, animPosY, animPosZ);
	}
	
	public AnimationChannel3D getAnimRotation3D() {
		return new AnimationChannel3D(animPitch, animYaw, null);
	}
	
	public AnimationChannel3D getAnimScale3D() {
		return new AnimationChannel3D(animScaleX, animScaleY, animScaleZ);
	}
	
	public Matrix getLocatorMatrix(String locatorName, float time) {
		Locator locator = getLocators().getOrDefault(locatorName, null);
		if(locator == null)
			return new Matrix();
		Matrix boneMatrix = getBoneOrientationMatrix(locator.bone, time);
		return boneMatrix.mult(locator.getLocalMatrix());
	}
	
	public Matrix getBoneOrientationMatrix(String boneName, float time) {
		BindPose bindPose = getBindPoses().getOrDefault(boneName, null);
		if(bindPose == null)
			return new Matrix();
		Matrix parentMatrix = getBoneOrientationMatrix(bindPose.getParent(), time);
		Matrix localMatrix = getLocalBoneOrientationMatrix(bindPose, time);
		return parentMatrix.mult(localMatrix);
	}
	
	public Matrix getLocalBoneOrientationMatrix(String boneName, float time) {
		BindPose bindPose = getBindPoses().getOrDefault(boneName, null);
		if(bindPose == null)
			return new Matrix();
		return getLocalBoneOrientationMatrix(bindPose, time);
	}
	
	private Matrix getLocalBoneOrientationMatrix(BindPose bindPose, float time) {
		float translateX = bindPose.posX;
		float translateY = bindPose.posY;
		float translateZ = bindPose.posZ;
		float rotateX = bindPose.rotX;
		float rotateY = bindPose.rotY;
		float rotateZ = bindPose.rotZ;
		float scaleX = bindPose.scaleX;
		float scaleY = bindPose.scaleY;
		float scaleZ = bindPose.scaleZ;
		float pivotX = bindPose.pivotX;
		float pivotY = bindPose.pivotY;
		float pivotZ = bindPose.pivotZ;
		BoneAnimation bone = getBones().getOrDefault(bindPose.getName(), null);
		if(bone != null) {
			translateX = bone.getAnimTranslateX().getKeyframeAtTime(time).value;
			translateY = bone.getAnimTranslateY().getKeyframeAtTime(time).value;
			translateZ = bone.getAnimTranslateZ().getKeyframeAtTime(time).value;
			rotateX = bone.getAnimRotateX().getKeyframeAtTime(time).value;
			rotateY = bone.getAnimRotateY().getKeyframeAtTime(time).value;
			rotateZ = bone.getAnimRotateZ().getKeyframeAtTime(time).value;
			scaleX = bone.getAnimScaleX().getKeyframeAtTime(time).value;
			scaleY = bone.getAnimScaleY().getKeyframeAtTime(time).value;
			scaleZ = bone.getAnimScaleZ().getKeyframeAtTime(time).value;
		}
		return Matrix.translate(translateX, translateY, translateZ).mult(
				Matrix.translate(pivotX, pivotY, pivotZ).mult(
				Matrix.rotate(rotateX, rotateY, rotateZ).mult(
				Matrix.scale(scaleX, scaleY, scaleZ).mult(
				Matrix.translate(-pivotX, -pivotY, -pivotZ)))));
	}

}
