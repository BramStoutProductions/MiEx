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

package nl.bramstout.mcworldexporter.molang;

import nl.bramstout.mcworldexporter.entity.EntityAnimation;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BindPose;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BoneAnimation;
import nl.bramstout.mcworldexporter.math.Matrix;

public class AnimationInfo {

	public EntityAnimation animation;
	/**
	 * Only valid in an animation controller. Is true if all animations
	 * in the current animation controller state have played through at least once.
	 */
	public boolean allAnimationsFinished = false;
	/**
	 * Only valid in an animation controller. Is true if any animation
	 * in the current animation controller state has played through at least once.
	 */
	public boolean anyAnimationFinished = false;
	/**
	 * Only valid in an animation. The time in seconds since the current animation started.
	 */
	public float animTime = 0f;
	/**
	 * The global time
	 */
	public float globalTime = 0f;
	/**
	 * The time between this frame and the previous frame.
	 */
	public float deltaTime = 0f;
	/**
	 * Returns the ratio between the previous and next key frames in an animation.
	 */
	public float keyframeLerpTime = 0f;
	
	public AnimationInfo() {}
	
	public AnimationInfo(AnimationInfo other) {
		this.animation = other.animation;
		this.allAnimationsFinished = other.allAnimationsFinished;
		this.anyAnimationFinished = other.anyAnimationFinished;
		this.animTime = other.animTime;
		this.globalTime = other.globalTime;
		this.deltaTime = other.deltaTime;
		this.keyframeLerpTime = other.keyframeLerpTime;
	}
	
	public Matrix getLocatorMatrix(String locatorName) {
		if(animation == null)
			return new Matrix();
		return animation.getLocatorMatrix(locatorName, globalTime);
	}
	
	public Matrix getBoneOrientationMatrix(String boneName) {
		if(animation == null)
			return new Matrix();
		return animation.getBoneOrientationMatrix(boneName, globalTime);
	}
	
	public float[] getBoneOrientationTRS(String boneName) {
		if(animation == null)
			return new float[] {0f, 0f, 0f,   0f, 0f, 0f,   1f, 1f, 1f};
		BindPose bindPose = animation.getBindPoses().getOrDefault(boneName, null);
		if(bindPose == null)
			return new float[] {0f, 0f, 0f,   0f, 0f, 0f,   1f, 1f, 1f};
		float translateX = bindPose.posX;
		float translateY = bindPose.posY;
		float translateZ = bindPose.posZ;
		float rotateX = bindPose.rotX;
		float rotateY = bindPose.rotY;
		float rotateZ = bindPose.rotZ;
		float scaleX = bindPose.scaleX;
		float scaleY = bindPose.scaleY;
		float scaleZ = bindPose.scaleZ;
		BoneAnimation bone = animation.getBones().getOrDefault(bindPose.getName(), null);
		if(bone != null) {
			translateX = bone.getAnimTranslateX().getKeyframeAtTime(globalTime).value;
			translateY = bone.getAnimTranslateY().getKeyframeAtTime(globalTime).value;
			translateZ = bone.getAnimTranslateZ().getKeyframeAtTime(globalTime).value;
			rotateX = bone.getAnimRotateX().getKeyframeAtTime(globalTime).value;
			rotateY = bone.getAnimRotateY().getKeyframeAtTime(globalTime).value;
			rotateZ = bone.getAnimRotateZ().getKeyframeAtTime(globalTime).value;
			scaleX = bone.getAnimScaleX().getKeyframeAtTime(globalTime).value;
			scaleY = bone.getAnimScaleY().getKeyframeAtTime(globalTime).value;
			scaleZ = bone.getAnimScaleZ().getKeyframeAtTime(globalTime).value;
		}
		return new float[] { translateX, translateY, translateZ,  rotateX, rotateY, rotateZ,  scaleX, scaleY, scaleZ };
	}
	
	public float[] getBoneOrigin(String boneName) {
		if(animation == null)
			return new float[] {0f, 0f, 0f};
		BindPose bindPose = animation.getBindPoses().getOrDefault(boneName, null);
		if(bindPose == null)
			return new float[] {0f, 0f, 0f};
		return new float[] { bindPose.pivotX, bindPose.pivotY, bindPose.pivotZ };
	}
	
	public float[] getBoneRotation(String boneName) {
		if(animation == null)
			return new float[] {0f, 0f, 0f};
		BindPose bindPose = animation.getBindPoses().getOrDefault(boneName, null);
		if(bindPose == null)
			return new float[] {0f, 0f, 0f};
		return new float[] { bindPose.rotX, bindPose.rotY, bindPose.rotZ };
	}
	
}
