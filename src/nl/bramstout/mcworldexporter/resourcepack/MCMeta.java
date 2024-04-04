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

package nl.bramstout.mcworldexporter.resourcepack;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class MCMeta {

	private boolean animate;
	private boolean interpolate;
	private int frameTime;
	private int[] frames;
	private int frameCount;
	private int tileWidth;
	private int tileHeight;
	
	public MCMeta(String texture) {
		animate = false;
		interpolate = false;
		frameTime = 1;
		frames = null;
		frameCount = 1;
		tileWidth = 1;
		tileHeight = 1;
		
		File file = ResourcePack.getFile(texture, "textures", ".png.mcmeta", "assets");
		if(!file.exists())
			return;
		
		try {
			JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(file)))).getAsJsonObject();
			
			if(!data.has("animation"))
				return;
			
			JsonObject animData = data.get("animation").getAsJsonObject();
			animate = true;
			
			if(animData.has("interpolate"))
				interpolate = animData.get("interpolate").getAsBoolean();
			
			if(animData.has("width"))
				tileWidth = animData.get("width").getAsInt();
			
			if(animData.has("height"))
				tileHeight = animData.get("height").getAsInt();
			
			if(animData.has("frametime"))
				frameTime = animData.get("frametime").getAsInt();
			
			File texFile = ResourcePack.getFile(texture, "textures", ".png", "assets");
			BufferedImage img = ImageIO.read(texFile);
			frameCount = (img.getHeight() * tileHeight) / (img.getWidth() * tileWidth);
			
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
			}else {
				frames = new int[frameCount * 2];
				for(int i = 0; i < frameCount*2; i += 2) {
					frames[i] = i/2;
					frames[i+1] = frameTime;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isAnimate() {
		return animate;
	}

	public boolean isInterpolate() {
		return interpolate;
	}

	public int getFrameTime() {
		return frameTime;
	}

	public int[] getFrames() {
		return frames;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}
	
}
