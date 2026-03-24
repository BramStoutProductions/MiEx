package nl.bramstout.mcworldexporter.lighting;

import com.google.gson.JsonObject;

public class BlockLightValues {
	
	private byte emissiveLightLevel;
	private String emissiveLightColor;
	private byte lightAttenuationX;
	private byte lightAttenuationY;
	private byte lightAttenuationZ;
	
	public BlockLightValues(JsonObject data) {
		emissiveLightLevel = 0;
		emissiveLightColor = "";
		lightAttenuationX = -1;
		lightAttenuationY = -1;
		lightAttenuationZ = -1;
		
		if(data == null)
			return;
		if(data.has("emissiveLightLevel"))
			emissiveLightLevel = data.get("emissiveLightLevel").getAsByte();
		if(data.has("emissiveLightColor"))
			emissiveLightColor = data.get("emissiveLightColor").getAsString();
		if(data.has("lightAttenuation"))
			lightAttenuationX = lightAttenuationY = lightAttenuationZ = data.get("lightAttenuation").getAsByte();
		if(data.has("lightAttenuationX"))
			lightAttenuationX = data.get("lightAttenuationX").getAsByte();
		if(data.has("lightAttenuationY"))
			lightAttenuationY = data.get("lightAttenuationY").getAsByte();
		if(data.has("lightAttenuationZ"))
			lightAttenuationZ = data.get("lightAttenuationZ").getAsByte();
	}
	
	public BlockLightValues() {
		this((byte) 0, "", (byte)-1, (byte)-1, (byte)-1);
	}
	
	public BlockLightValues(byte emissiveLightLevel, String emissiveLightColor) {
		this(emissiveLightLevel, emissiveLightColor, (byte)-1, (byte)-1, (byte)-1);
	}
	
	public BlockLightValues(byte emissiveLightLevel, String emissiveLightColor,
				byte lightAttenuationX, byte lightAttenuationY, byte lightAttenuationZ) {
		this.emissiveLightLevel = emissiveLightLevel;
		this.emissiveLightColor = emissiveLightColor;
		this.lightAttenuationX = lightAttenuationX;
		this.lightAttenuationY = lightAttenuationY;
		this.lightAttenuationZ = lightAttenuationZ;
	}

	public byte getEmissiveLightLevel() {
		return emissiveLightLevel;
	}

	public short getEmissiveLightColor() {
		return Lighting.getIdForName(emissiveLightColor);
	}

	public byte getLightAttenuationX() {
		return lightAttenuationX;
	}

	public byte getLightAttenuationY() {
		return lightAttenuationY;
	}

	public byte getLightAttenuationZ() {
		return lightAttenuationZ;
	}

}
