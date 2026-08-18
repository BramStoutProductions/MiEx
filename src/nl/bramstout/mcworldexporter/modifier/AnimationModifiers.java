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
		float[] points = new float[12];
		float[] uvs = new float[8];
		
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
			if(face.hasVertexColor) {
				modifierContext.faceTintR = face.vertexColorR;
				modifierContext.faceTintG = face.vertexColorG;
				modifierContext.faceTintB = face.vertexColorB;
			}
			modifierContext.faceTintIndex = face.getTintIndex();
			modifierContext.faceDirection = face.getDirection();
			
			face.getPoints(points);
			face.getUVs(uvs);
			for(int i = 0; i < 4; ++i) {
				modifierContext.vertexX = points[i*3+0];
				modifierContext.vertexY = points[i*3+1];
				modifierContext.vertexZ = points[i*3+2];
				modifierContext.vertexU = uvs[i*2+0];
				modifierContext.vertexV = uvs[i*2+1];
				float tintR = 1f;
				float tintG = 1f;
				float tintB = 1f;
				if(face.hasVertexColor) {
					modifierContext.vertexR = face.vertexColorR;
					modifierContext.vertexG = face.vertexColorG;
					modifierContext.vertexB = face.vertexColorB;
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
				
				points[i*3+0] = modifierContext.vertexX;
				points[i*3+1] = modifierContext.vertexY;
				points[i*3+2] = modifierContext.vertexZ;
				uvs[i*2+0] = modifierContext.vertexU;
				uvs[i*2+1] = modifierContext.vertexV;
				if(tintR != modifierContext.vertexR || tintG != modifierContext.vertexG || tintB != modifierContext.vertexB) {
					// Vertex tint was set.
					if(!face.hasVertexColor) {
						// Make sure that we have the vertex colours set up.
						face.setFaceColour(1f, 1f, 1f);
					}
					//face.getVertexColors()[i*3+0] = modifierContext.vertexR;
					//face.getVertexColors()[i*3+1] = modifierContext.vertexG;
					//face.getVertexColors()[i*3+2] = modifierContext.vertexB;
					// TODO: ModelFace currently only supports a solid colour
					// for all its vertices. Decide on whether to keep that
					// restriction or change it.
					face.setFaceColour(modifierContext.vertexR, modifierContext.vertexG, modifierContext.vertexB);
				}
			}
			face.setPoints(points);
			face.setUVs(uvs);
		}
	}
	
	
	
	public static AnimationModifiers getModifiersForBlockName(String blockName) {
		Modifiers modifiers = Modifiers.getModifiersForBlockName(blockName);
		if(modifiers != null && modifiers.hasAnimationModifiers())
			return new AnimationModifiers(modifiers);
		return null;
	}

}
