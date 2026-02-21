package nl.bramstout.mcworldexporter.modifier;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexPosition;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexTint;
import nl.bramstout.mcworldexporter.modifier.nodes.ModifierNodeSetVertexUVs;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler.RandomOffsetMethod;

public class AnimationModifier extends Modifier{

	private float duration;
	private boolean positionDependent;
	private boolean ignoreBiome;
	private boolean randomOffsetXZ;
	private boolean randomOffsetY;
	private RandomOffsetMethod randomOffsetMethod;
	private Float randomOffsetNoiseScale;
	
	public AnimationModifier(JsonObject data, int rpPriority) {
		super(data, rpPriority);
		
		duration = 1f;
		positionDependent = false;
		ignoreBiome = false;
		randomOffsetXZ = false;
		randomOffsetY = false;
		randomOffsetMethod = null;
		randomOffsetNoiseScale = null;
		
		if(data.has("duration"))
			duration = data.get("duration").getAsFloat();
		
		if(data.has("positionDependent"))
			positionDependent = data.get("positionDependent").getAsBoolean();
		
		if(data.has("positionDependentIgnoreBiome"))
			ignoreBiome = data.get("positionDependentIgnoreBiome").getAsBoolean();
		
		if(data.has("randomOffsetXZ"))
			randomOffsetXZ = data.get("randomOffsetXZ").getAsBoolean();
		
		if(data.has("randomOffsetY"))
			randomOffsetY = data.get("randomOffsetY").getAsBoolean();
		
		if(data.has("randomOffsetMethod")) {
			String method = data.get("randomOffsetMethod").getAsString();
			if(method.equals("random"))
				randomOffsetMethod = RandomOffsetMethod.RANDOM;
			else if(method.equals("noise"))
				randomOffsetMethod = RandomOffsetMethod.NOISE;
		}
		
		if(data.has("randomOffsetNoiseScale"))
			randomOffsetNoiseScale = Float.valueOf(data.get("randomOffsetNoiseScale").getAsFloat());
	}

	public float getDuration() {
		return duration;
	}

	public boolean isPositionDependent() {
		return positionDependent;
	}
	
	public boolean isIgnoreBiome() {
		return ignoreBiome;
	}

	public boolean isRandomOffsetXZ() {
		return randomOffsetXZ;
	}

	public boolean isRandomOffsetY() {
		return randomOffsetY;
	}

	public RandomOffsetMethod getRandomOffsetMethod() {
		return randomOffsetMethod;
	}

	public Float getRandomOffsetNoiseScale() {
		return randomOffsetNoiseScale;
	}
	
	public boolean isAnimatedPoints() {
		for(ModifierNode node : nodes.values()) {
			if(node instanceof ModifierNodeSetVertexPosition)
				return true;
		}
		return false;
	}
	
	public boolean isAnimatedUVs() {
		for(ModifierNode node : nodes.values()) {
			if(node instanceof ModifierNodeSetVertexUVs)
				return true;
		}
		return false;
	}
	
	public boolean isAnimatedVertexColors() {
		for(ModifierNode node : nodes.values()) {
			if(node instanceof ModifierNodeSetVertexTint)
				return true;
		}
		return false;
	}

}
