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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BindPose;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.ai.EntityAI;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.molang.MolangValue;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangDictionary;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationControllerState;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;

public class Entity {
	
	private String id;
	private NbtTagCompound properties;
	private long uniqueId;
	private float x;
	private float y;
	private float z;
	private float dx;
	private float dy;
	private float dz;
	private float yaw;
	private float pitch;
	private float headYaw;
	private float headPitch;
	private EntityHandler handler;
	private MolangValue variables;
	private List<MolangScript> initMolang;
	private MolangScript scaleXExpression;
	private MolangScript scaleYExpression;
	private MolangScript scaleZExpression;
	private EntityAnimation animation;
	private AnimationController animationController;
	private Map<String, Animation> animations;
	private AnimationControllerState animationControllerState;
	private EntityAI ai;
	private Random random;
	
	public Entity(String id, NbtTagCompound properties, EntityHandler handler) {
		this.id = id;
		this.properties = properties;
		this.properties.acquireOwnership();
		this.uniqueId = 0;
		this.x = 0f;
		this.y = 0f;
		this.z = 0f;
		this.dx = 0f;
		this.dy = 0f;
		this.dz = 0f;
		this.yaw = 0f;
		this.pitch = 0f;
		this.headYaw = 0f;
		this.headPitch = 0f;
		this.handler = handler;
		this.variables = new MolangValue(new MolangDictionary());
		setupDefaultVariables();
		this.initMolang = new ArrayList<MolangScript>();
		this.scaleXExpression = null;
		this.scaleYExpression = null;
		this.scaleZExpression = null;
		this.animation = null;
		this.animationController = null;
		this.animations = new HashMap<String, Animation>();
		this.animationControllerState = new AnimationControllerState();
		this.ai = null;
		this.random = new Random(0);
		if(handler != null) {
			handler.setup(this);
			EntityAIHandler aiHandler = handler.getAIHandler(this);
			if(aiHandler != null) {
				this.ai = new EntityAI(this, aiHandler);
			}
		}
	}
	
	private void setupDefaultVariables() {
		variables.getField("gliding_speed_value").set(new MolangValue(1));
	}
	
	public void applyBindPoseToAnimation(float time) {
		for(Entry<String, BindPose> entry : this.animation.getBindPoses().entrySet()) {
			EntityAnimation.BoneAnimation entityAnim = getAnimation().getBones().getOrDefault(entry.getKey(), null);
			if(entityAnim == null) {
				entityAnim = new EntityAnimation.BoneAnimation(entry.getKey());
				getAnimation().getBones().put(entry.getKey(), entityAnim);
			}
			BindPose bindPose = entry.getValue();
			entityAnim.getAnimTranslateX().addKeyframe(new Keyframe(time, bindPose.posX));
			entityAnim.getAnimTranslateY().addKeyframe(new Keyframe(time, bindPose.posY));
			entityAnim.getAnimTranslateZ().addKeyframe(new Keyframe(time, bindPose.posZ));
			entityAnim.getAnimRotateX().addKeyframe(new Keyframe(time, bindPose.rotX));
			entityAnim.getAnimRotateY().addKeyframe(new Keyframe(time, bindPose.rotY));
			entityAnim.getAnimRotateZ().addKeyframe(new Keyframe(time, bindPose.rotZ));
			entityAnim.getAnimScaleX().addKeyframe(new Keyframe(time, bindPose.scaleX));
			entityAnim.getAnimScaleY().addKeyframe(new Keyframe(time, bindPose.scaleY));
			entityAnim.getAnimScaleZ().addKeyframe(new Keyframe(time, bindPose.scaleZ));
		}
	}
	
	public Random getRandom() {
		return random;
	}
	
	public void setGlobalRandomSeed(long seed) {
		this.random = new Random(seed + uniqueId);
	}
	
	public long getUniqueId() {
		return uniqueId;
	}
	
	public void setUniqueId(long id) {
		this.uniqueId = id;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(float z) {
		this.z = z;
	}
	
	public float getDx() {
		return dx;
	}

	public void setDx(float x) {
		this.dx = x;
	}

	public float getDy() {
		return dy;
	}

	public void setDy(float y) {
		this.dy = y;
	}

	public float getDz() {
		return dz;
	}

	public void setDz(float z) {
		this.dz = z;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getHeadYaw() {
		return headYaw;
	}

	public void setHeadYaw(float headYaw) {
		this.headYaw = headYaw;
	}

	public float getHeadPitch() {
		return headPitch;
	}

	public void setHeadPitch(float headPitch) {
		this.headPitch = headPitch;
	}

	public String getId() {
		return id;
	}
	
	public NbtTagCompound getProperties() {
		return properties;
	}

	public Model getModel() {
		return handler.getModel(this);
	}
	
	public MolangValue getVariables() {
		return variables;
	}
	
	public EntityAnimation getAnimation() {
		return animation;
	}
	
	public void setupAnimation() {
		this.animation = new EntityAnimation();
	}
	
	public EntityAI getAI() {
		return ai;
	}
	
	public AnimationController getAnimationController() {
		return animationController;
	}
	
	public void setAnimationController(AnimationController animationController) {
		this.animationController = animationController;
	}
	
	public AnimationControllerState getAnimationControllerState() {
		return animationControllerState;
	}
	
	public Map<String, Animation> getAnimations(){
		return animations;
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
