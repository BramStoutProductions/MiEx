package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BindPose;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.BoneAnimation;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Keyframe;
import nl.bramstout.mcworldexporter.entity.EntityAnimation.Locator;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelBone;
import nl.bramstout.mcworldexporter.model.ModelLocator;
import nl.bramstout.mcworldexporter.molang.AnimationInfo;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationState;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class BlockAnimationHandlerBedrock extends BlockAnimationHandler{
	
	private Animation animation;
	
	public BlockAnimationHandlerBedrock(String animationId) {
		animation = ResourcePacks.getAnimation(animationId);
		if(animation != null) {
			duration = animation.getAnimationLength();
			animatesPoints = true;
		}
	}
	
	public Animation getAnimation() {
		return animation;
	}
	
	private static class AnimatedEntityHandler extends EntityHandler{

		private Model model;
		public AnimatedEntityHandler(Model model) {
			this.model = model;
		}
		
		@Override
		public Model getModel(Entity entity) {
			return model;
		}

		@Override
		public void setup(Entity entity) {}

		@Override
		public EntityAIHandler getAIHandler(Entity entity) {
			return null;
		}
		
	}
	
	public void applyAnimation(Model model, float time) {
		Entity entity = new Entity("", NbtTagCompound.newNonPooledInstance(""), new AnimatedEntityHandler(model));
		
		AnimationController animationController = new AnimationController("");
		AnimationState defaultState = new AnimationState("default", 0f);
		defaultState.addAnimation("animation", MolangParser.parse("1.0"));
		animationController.getStates().add(defaultState);
		
		entity.getAnimations().put("animation", animation);
		entity.setAnimationController(animationController);
		
		entity.setupAnimation();
		entity.getAnimation().getAnimPosX().addKeyframe(new Keyframe(time, entity.getX()));
		entity.getAnimation().getAnimPosY().addKeyframe(new Keyframe(time, entity.getY()));
		entity.getAnimation().getAnimPosZ().addKeyframe(new Keyframe(time, entity.getZ()));
		entity.getAnimation().getAnimYaw().addKeyframe(new Keyframe(time, entity.getYaw()));
		entity.getAnimation().getAnimPitch().addKeyframe(new Keyframe(time, entity.getPitch()));
		entity.getAnimation().getAnimHeadYaw().addKeyframe(new Keyframe(time, entity.getHeadYaw()));
		entity.getAnimation().getAnimHeadPitch().addKeyframe(new Keyframe(time, entity.getHeadPitch()));
		entity.getAnimation().getAnimScaleX().addKeyframe(new Keyframe(time, 1f));
		entity.getAnimation().getAnimScaleY().addKeyframe(new Keyframe(time, 1f));
		entity.getAnimation().getAnimScaleZ().addKeyframe(new Keyframe(time, 1f));
		
		for(ModelBone bone : model.getBones()) {
			BindPose bindPose = new BindPose(bone.getName(), bone.getParent() == null ? "" : bone.getParent().getName());
			bindPose.posX = bone.translation.x;
			bindPose.posY = bone.translation.y;
			bindPose.posZ = bone.translation.z;
			bindPose.rotX = bone.rotation.x;
			bindPose.rotY = bone.rotation.y;
			bindPose.rotZ = bone.rotation.z;
			bindPose.scaleX = bone.scaling.x;
			bindPose.scaleY = bone.scaling.y;
			bindPose.scaleZ = bone.scaling.z;
			entity.getAnimation().getBindPoses().put(bone.getName(), bindPose);
		}
		for(ModelLocator locator : model.getLocators()) {
			Locator locator2 = new Locator(locator.getName());
			locator2.offset = locator.offset;
			locator2.rotation = locator.rotation;
			locator2.ignoreInheritedScale = locator.ignoreInheritedScale;
			locator2.bone = locator.bone == null ? "" : locator.bone.getName();
			entity.getAnimation().getLocators().put(locator.getName(), locator2);
		}
		
		MolangQuery query = new MolangQuery(entity.getId(), entity.getProperties(), 0f, 0f, 0f);
		MolangContext context = new MolangContext(query, entity.getRandom());
		
		AnimationInfo animInfo = context.pushAnimationInfo();
		animInfo.animation = entity.getAnimation();
		
		// Apply animation at frame 0 first
		// to make sure that the state is properly initialised
		animInfo.globalTime = 0f;
		animInfo.deltaTime = time;
		entity.applyBindPoseToAnimation(0f);
		entity.getAnimationController().eval(entity.getAnimationControllerState(), entity.getAnimations(), 
				0f, time, 0f, 1f, context, entity);
		
		animInfo.globalTime = time;
		animInfo.deltaTime = time;
		entity.applyBindPoseToAnimation(time);
		entity.getAnimationController().eval(entity.getAnimationControllerState(), entity.getAnimations(), 
				time, time, time, 1f, context, entity);
		
		for(Entry<String, BoneAnimation> boneAnim : entity.getAnimation().getBones().entrySet()) {
			ModelBone bone = model.getBone(boneAnim.getKey());
			if(bone == null)
				continue;
			
			bone.translation.x = boneAnim.getValue().getAnimTranslateX().getClosestKeyframeAtTime(time).value;
			bone.translation.y = boneAnim.getValue().getAnimTranslateY().getClosestKeyframeAtTime(time).value;
			bone.translation.z = boneAnim.getValue().getAnimTranslateZ().getClosestKeyframeAtTime(time).value;
			
			bone.rotation.x = boneAnim.getValue().getAnimRotateX().getClosestKeyframeAtTime(time).value;
			bone.rotation.y = boneAnim.getValue().getAnimRotateY().getClosestKeyframeAtTime(time).value;
			bone.rotation.z = boneAnim.getValue().getAnimRotateZ().getClosestKeyframeAtTime(time).value;
			
			bone.scaling.x = boneAnim.getValue().getAnimScaleX().getClosestKeyframeAtTime(time).value;
			bone.scaling.y = boneAnim.getValue().getAnimScaleY().getClosestKeyframeAtTime(time).value;
			bone.scaling.z = boneAnim.getValue().getAnimScaleZ().getClosestKeyframeAtTime(time).value;
		}
		
		model.applyBones();
		model.setAnimatesPoints(true);
	}
	
}
