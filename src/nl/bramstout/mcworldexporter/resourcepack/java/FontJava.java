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

package nl.bramstout.mcworldexporter.resourcepack.java;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class FontJava extends Font{

	private Map<Integer, Character> characters;
	
	public FontJava(JsonObject data) {
		characters = new HashMap<Integer, Character>();
		
		if(data.has("providers")) {
			for(JsonElement el : data.getAsJsonArray("providers").asList()) {
				JsonObject providerObj = el.getAsJsonObject();
				if(providerObj.has("filter")) {
					JsonObject filterObj = providerObj.getAsJsonObject("filter");
					if(filterObj.has("uniform") && filterObj.get("uniform").getAsBoolean())
						continue;
					if(filterObj.has("jp") && filterObj.get("jp").getAsBoolean())
						continue;
				}
				if(!providerObj.has("type"))
					continue;
				String type = providerObj.get("type").getAsString();
				if(type.equals("bitmap")) {
					parseBitmap(providerObj);
				}else if(type.equals("space")) {
					parseSpace(providerObj);
				}else if(type.equals("reference")) {
					parseReference(providerObj);
				}
			}
		}
	}
	
	private void parseBitmap(JsonObject data) {
		float height = 1.0f;
		float ascent = 7.0f / 8.0f;
		String texture = null;
		if(data.has("height"))
			height = data.get("height").getAsFloat() / 8.0f;
		if(data.has("ascent"))
			ascent = data.get("ascent").getAsFloat() / 8.0f;
		if(data.has("file"))
			texture = data.get("file").getAsString().replace(".png", "");
		File fontFile = ResourcePacks.getTexture(texture);
		if(fontFile == null || !fontFile.exists())
			return;
		BufferedImage fontImg = ImageReader.readImage(fontFile);
		if(fontImg == null)
			return;
		if(!data.has("chars"))
			return;
		
		JsonArray charsArray = data.getAsJsonArray("chars");
		int atlasWidth = 0;
		int atlasHeight = 0;
		for(JsonElement el : charsArray) {
			String str = el.getAsString();
			atlasWidth = Math.max(str.codePointCount(0, str.length()), atlasWidth);
			atlasHeight++;
		}
		
		int charPixelWidth = fontImg.getWidth() / atlasWidth;
		int charPixelHeight = fontImg.getHeight() / atlasHeight;
		
		int j = 0;
		for(JsonElement el : charsArray) {
			String str = el.getAsString();
			OfInt it = str.codePoints().iterator();
			int i = 0;
			while(it.hasNext()) {
				int codepoint = it.nextInt();
				if(codepoint == 0) {
					i++;
					continue;
				}
				
				int charWidth = getCharWidth(fontImg, i, j, charPixelWidth, charPixelHeight);
				if(charWidth == 0) {
					i++;
					continue;
				}
				
				float width = ((float) charWidth) / ((float) charPixelWidth);
				float texU = ((float) i) / ((float) atlasWidth);
				float texV = ((float) j) / ((float) atlasHeight);
				float texWidth = width / ((float) atlasWidth);
				float texHeight = 1.0f / ((float) atlasHeight);
				characters.put(Integer.valueOf(codepoint), 
						new Character(texture, width, height, ascent, texU, texV, texWidth, texHeight));
				
				i++;
			}
			j++;
		}
	}
	
	private int getCharWidth(BufferedImage img, int i, int j, int charWidth, int charHeight) {
		int x = i * charWidth;
		int y = j * charHeight;
		for(int width = charWidth; width > 0; --width) {
			for(int yy = 0; yy < charHeight; ++yy) {
				int rgba = img.getRGB(x + width - 1, y + yy);
				int a = rgba & 0xFF000000;
				if(a != 0)
					return width;
			}
		}
		return 0;
	}
	
	private void parseSpace(JsonObject data) {
		if(!data.has("advances"))
			return;
		for(Entry<String, JsonElement> entry : data.getAsJsonObject("advances").entrySet()) {
			if(entry.getKey().isEmpty())
				continue;
			int codepoint = entry.getKey().codePointAt(0);
			float width = 0f;
			if(entry.getValue().isJsonPrimitive())
				if(entry.getValue().getAsJsonPrimitive().isNumber())
					width = entry.getValue().getAsFloat() / 8f; // Normalise to MiEx units.
			Character character = new Character(null, width, 0f, 0f, 0f, 0f, 0f, 0f);
			characters.put(Integer.valueOf(codepoint), character);
		}
	}
	
	private void parseReference(JsonObject data) {
		if(!data.has("id"))
			return;
		String id = data.get("id").getAsString();
		if(!id.contains(":"))
			id = "minecraft:" + id;
		//String[] idTokens = id.split(":");
		//String path = "assets/" + idTokens[0] + "/font/" + idTokens[1] + ".json";
		for(ResourcePack pack : ResourcePacks.getActiveResourcePacks()) {
			File file = pack.getResource(id, "font", "assets", ".json");
			//File file = new File(pack.getFolder(), path);
			if(file.exists()) {
				try {
					JsonObject refData = Json.read(file).getAsJsonObject();
					FontJava refFont = new FontJava(refData);
					characters.putAll(refFont.characters);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
		}
	}
	
	@Override
	public Character getCharacterInfo(int codepoint) {
		return characters.getOrDefault(Integer.valueOf(codepoint), null);
	}

}
