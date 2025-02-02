package nl.bramstout.mcworldexporter.resourcepack.java;

import java.awt.image.BufferedImage;
import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.resourcepack.MCMeta;

public class MCMetaJavaEdition extends MCMeta{

	public MCMetaJavaEdition(File texFile, JsonObject data) {
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
			
			if(animData.has("interpolate"))
				interpolate = animData.get("interpolate").getAsBoolean();
			
			if(animData.has("width"))
				tileWidth = animData.get("width").getAsInt();
			
			if(animData.has("height"))
				tileHeight = animData.get("height").getAsInt();
			
			if(animData.has("frametime"))
				frameTime = animData.get("frametime").getAsInt();
			
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
