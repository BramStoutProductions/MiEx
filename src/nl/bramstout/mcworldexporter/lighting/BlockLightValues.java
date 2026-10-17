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
