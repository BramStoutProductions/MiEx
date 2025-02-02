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

package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.awt.image.BufferedImage;
import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;

public class MCMetaBedrockEdition extends MCMeta{

	public MCMetaBedrockEdition(File texFile, JsonObject data) {
		animate = false;
		interpolate = false;
		frameTime = 1;
		frames = null;
		frameCount = 1;
		tileWidth = 1;
		tileHeight = 1;
		imgWidth = 16;
		imgHeight = 16;
		
		if(data != null) {
			if(!data.has("animation"))
				return;
			
			JsonObject animData = data.get("animation").getAsJsonObject();
			animate = true;
			
			if(animData.has("ticks_per_frame"))
				frameTime = animData.get("ticks_per_frame").getAsInt();
			
			if(frameTime > 1)
				interpolate = true;
			
			if(animData.has("blend_frames"))
				interpolate = animData.get("blend_frames").getAsBoolean();
			
			try {
				BufferedImage img = ImageReader.readImage(texFile);
				if(img != null) {
					frameCount = (img.getHeight() * tileHeight) / (img.getWidth() * tileWidth);
					imgWidth = img.getWidth();
					imgHeight = img.getHeight();
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			
			if(animData.has("frames")) {
				JsonArray framesArray = animData.get("frames").getAsJsonArray();
				frames = new int[framesArray.size() * 2];
				for(int i = 0; i < framesArray.size(); ++i) {
					JsonElement frameElement = framesArray.get(i);
					if(frameElement.isJsonPrimitive()) {
						frames[i*2] = frameElement.getAsInt();
						frames[i*2+1] = frameTime;
					}else if(frameElement.isJsonObject()) {
						JsonObject frameData = frameElement.getAsJsonObject();
						int frameDataIndex = i;
						int frameDataTime = frameTime;
						if(frameData.has("index"))
							frameDataIndex = frameData.get("index").getAsInt();
						if(frameData.has("time"))
							frameDataTime = frameData.get("time").getAsInt();
						frames[i*2] = frameDataIndex;
						frames[i*2+1] = frameDataTime;
					}
				}
			}
			if(frames == null || frames.length == 0){
				frames = new int[frameCount * 2];
				for(int i = 0; i < frameCount*2; i += 2) {
					frames[i] = i/2;
					frames[i+1] = frameTime;
				}
			}
		}
	}
	
}
