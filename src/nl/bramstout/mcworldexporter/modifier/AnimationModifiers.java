package nl.bramstout.mcworldexporter.modifier;

import java.util.List;

import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.world.Block;

public class AnimationModifiers extends BlockAnimationHandler{
	
	private Modifiers modifiers;
	
	private AnimationModifiers(Modifiers modifiers) {
		this.modifiers = modifiers;
		duration = 1f;
		positionDependent = false;
		ignoreBiome = false;
		randomOffsetXZ = false;
		randomOffsetY = false;
		randomOffsetMethod = RandomOffsetMethod.RANDOM;
		randomOffsetNoiseScale = 16f;
		animatesTopology = false;
		animatesPoints = false;
		animatesUVs = false;
		animatesVertexColors = false;
		
		for(int i = 0; i < this.modifiers.getAnimationModifiers().size(); ++i) {
			AnimationModifier modifier = this.modifiers.getAnimationModifiers().get(i);
			if(i == 0) {
				this.duration = modifier.getDuration();
			}else {
				this.duration = BlockAnimationHandler.combineDurations(this.duration, modifier.getDuration());
			}
			
			if(modifier.isPositionDependent())
				positionDependent = true;
			
			if(modifier.isIgnoreBiome())
				ignoreBiome = true;
			
			if(modifier.isRandomOffsetXZ())
				randomOffsetXZ = true;
			
			if(modifier.isRandomOffsetY())
				randomOffsetY = true;
			
			if(modifier.getRandomOffsetMethod() != null)
				randomOffsetMethod = modifier.getRandomOffsetMethod();
			
			if(modifier.getRandomOffsetNoiseScale() != null)
				randomOffsetNoiseScale = modifier.getRandomOffsetNoiseScale().floatValue();
			
			if(modifier.isAnimatedPoints())
				animatesPoints = true;
			
			if(modifier.isAnimatedUVs())
				animatesUVs = true;
			
			if(modifier.isAnimatedVertexColors())
				animatesVertexColors = true;
		}
	}
	
	public void applyAnimation(List<List<Model>> models, NbtTagCompound properties, int x, int y, int z, int layer, 
								BlockState state, float frame) {
		for(List<Model> models2 : models)
			for(Model model : models2)
				applyAnimation(model, properties, x, y, z, layer, state, frame);
	}
	
	public void applyAnimation(Model model, NbtTagCompound properties, int x, int y, int z, int layer, 
								BlockState state, float frame) {
		if(modifiers.getAnimationModifiers() == null)
			return;
		if(animatesTopology)
			model.setAnimatesTopology(true);
		if(animatesPoints)
			model.setAnimatesPoints(true);
		if(animatesUVs)
			model.setAnimatesUVs(true);
		if(animatesVertexColors)
			model.setAnimatesVertexColors(true);
		
		ModifierContext modifierContext = new ModifierContext();
		modifierContext.block = new Block(state.getName(), properties, 0, 0);
		modifierContext.blockX = x;
		modifierContext.blockY = y;
		modifierContext.blockZ = z;
		float[] normal = new float[3];
		
		for(ModelFace face : model.getFaces()) {
			modifierContext.faceCenterX = face.getCenterX();
			modifierContext.faceCenterY = face.getCenterY();
			modifierContext.faceCenterZ = face.getCenterZ();
			face.calculateNormal(normal);
			modifierContext.faceNormalX = normal[0];
			modifierContext.faceNormalY = normal[1];
			modifierContext.faceNormalZ = normal[2];
			modifierContext.faceTintR = 1f;
			modifierContext.faceTintG = 1f;
			modifierContext.faceTintB = 1f;
			float[] faceTint = face.getVertexColors();
			if(faceTint != null) {
				modifierContext.faceTintR = faceTint[0];
				modifierContext.faceTintG = faceTint[1];
				modifierContext.faceTintB = faceTint[2];
			}
			modifierContext.faceTintIndex = face.getTintIndex();
			modifierContext.faceDirection = face.getDirection();
			
			for(int i = 0; i < 4; ++i) {
				modifierContext.vertexX = face.getPoints()[i*3+0];
				modifierContext.vertexY = face.getPoints()[i*3+1];
				modifierContext.vertexZ = face.getPoints()[i*3+2];
				modifierContext.vertexU = face.getUVs()[i*2+0];
				modifierContext.vertexV = face.getUVs()[i*2+1];
				float tintR = 1f;
				float tintG = 1f;
				float tintB = 1f;
				if(faceTint != null) {
					modifierContext.vertexR = faceTint[i*3+0];
					modifierContext.vertexG = faceTint[i*3+1];
					modifierContext.vertexB = faceTint[i*3+2];
					tintR = modifierContext.vertexR;
					tintG = modifierContext.vertexG;
					tintB = modifierContext.vertexB;
				}
				
				modifierContext.clearEvalCache();
				for(AnimationModifier modifier : modifiers.getAnimationModifiers()) {
					// Make sure that the time loops by the modifier's duration.
					// If we have multiple modifiers with different durations,
					// the total duration could be larger. But we want the modifiers
					// to think as if it's all in their own duration range.
					float modifierTime = frame / modifier.getDuration();
					modifierTime -= Math.floor(modifierTime);
					modifierTime *= modifier.getDuration();
					modifierContext.time = modifierTime;
					modifier.run(modifierContext);
				}
				
				face.getPoints()[i*3+0] = modifierContext.vertexX;
				face.getPoints()[i*3+1] = modifierContext.vertexY;
				face.getPoints()[i*3+2] = modifierContext.vertexZ;
				face.getUVs()[i*2+0] = modifierContext.vertexU;
				face.getUVs()[i*2+1] = modifierContext.vertexV;
				if(tintR != modifierContext.vertexR || tintG != modifierContext.vertexG || tintB != modifierContext.vertexB) {
					// Vertex tint was set.
					if(face.getVertexColors() == null) {
						// Make sure that we have the vertex colours set up.
						face.setFaceColour(1f, 1f, 1f);
					}
					face.getVertexColors()[i*3+0] = modifierContext.vertexR;
					face.getVertexColors()[i*3+1] = modifierContext.vertexG;
					face.getVertexColors()[i*3+2] = modifierContext.vertexB;
				}
			}
		}
	}
	
	
	
	public static AnimationModifiers getModifiersForBlockName(String blockName) {
		Modifiers modifiers = Modifiers.getModifiersForBlockName(blockName);
		if(modifiers != null && modifiers.hasAnimationModifiers())
			return new AnimationModifiers(modifiers);
		return null;
	}

}
