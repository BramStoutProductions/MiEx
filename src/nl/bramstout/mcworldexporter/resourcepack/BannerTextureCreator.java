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
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.bramstout.mcworldexporter.Color;

public class BannerTextureCreator {
	
	private static Map<String, String> patternMap = new HashMap<String, String>();
	private static Map<Integer, Color> colorMap = new HashMap<Integer, Color>();
	
	static {
		patternMap.put("", "minecraft:entity/banner/base");
		patternMap.put("b", "minecraft:entity/banner/base");
		patternMap.put("bs", "minecraft:entity/banner/stripe_bottom");
		patternMap.put("ts", "minecraft:entity/banner/stripe_top");
		patternMap.put("ls", "minecraft:entity/banner/stripe_left");
		patternMap.put("rs", "minecraft:entity/banner/stripe_right");
		patternMap.put("cs", "minecraft:entity/banner/stripe_center");
		patternMap.put("ms", "minecraft:entity/banner/stripe_middle");
		patternMap.put("drs", "minecraft:entity/banner/stripe_downright");
		patternMap.put("dls", "minecraft:entity/banner/stripe_downleft");
		patternMap.put("ss", "minecraft:entity/banner/small_stripes");
		patternMap.put("cr", "minecraft:entity/banner/cross");
		patternMap.put("sc", "minecraft:entity/banner/straight_cross");
		patternMap.put("ld", "minecraft:entity/banner/diagonal_left");
		patternMap.put("rud", "minecraft:entity/banner/diagonal_right");
		patternMap.put("lud", "minecraft:entity/banner/diagonal_up_left");
		patternMap.put("rd", "minecraft:entity/banner/diagonal_up_right");
		patternMap.put("vh", "minecraft:entity/banner/half_vertical");
		patternMap.put("vjr", "minecraft:entity/banner/half_vertical_right");
		patternMap.put("hh", "minecraft:entity/banner/half_horizontal");
		patternMap.put("hhb", "minecraft:entity/banner/half_horizontal_bottom");
		patternMap.put("bl", "minecraft:entity/banner/square_bottom_left");
		patternMap.put("br", "minecraft:entity/banner/square_bottom_right");
		patternMap.put("tl", "minecraft:entity/banner/square_top_left");
		patternMap.put("tr", "minecraft:entity/banner/square_top_right");
		patternMap.put("bt", "minecraft:entity/banner/triangle_bottom");
		patternMap.put("tt", "minecraft:entity/banner/triangle_top");
		patternMap.put("bts", "minecraft:entity/banner/triangles_bottom");
		patternMap.put("tts", "minecraft:entity/banner/triangles_top");
		patternMap.put("mc", "minecraft:entity/banner/circle");
		patternMap.put("mr", "minecraft:entity/banner/rhombus");
		patternMap.put("bo", "minecraft:entity/banner/border");
		patternMap.put("cbo", "minecraft:entity/banner/curly_border");
		patternMap.put("bri", "minecraft:entity/banner/bricks");
		patternMap.put("gra", "minecraft:entity/banner/gradient");
		patternMap.put("gru", "minecraft:entity/banner/gradient_up");
		patternMap.put("cre", "minecraft:entity/banner/creeper");
		patternMap.put("sku", "minecraft:entity/banner/skull");
		patternMap.put("flo", "minecraft:entity/banner/flower");
		patternMap.put("moj", "minecraft:entity/banner/mojang");
		patternMap.put("glb", "minecraft:entity/banner/globe");
		patternMap.put("pig", "minecraft:entity/banner/piglin");
		
		colorMap.put(0, new Color(0xFFFFFFFF, true, false));
		colorMap.put(1, new Color(0xFFD87F33, true, false));
		colorMap.put(2, new Color(0xFFB24CD8, true, false));
		colorMap.put(3, new Color(0xFF6699D8, true, false));
		colorMap.put(4, new Color(0xFFE5E533, true, false));
		colorMap.put(5, new Color(0xFF7FCC19, true, false));
		colorMap.put(6, new Color(0xFFF27FA5, true, false));
		colorMap.put(7, new Color(0xFF4C4C4C, true, false));
		colorMap.put(8, new Color(0xFF999999, true, false));
		colorMap.put(9, new Color(0xFF4C7F99, true, false));
		colorMap.put(10, new Color(0xFF7F3FB2, true, false));
		colorMap.put(11, new Color(0xFF334CB2, true, false));
		colorMap.put(12, new Color(0xFF664C33, true, false));
		colorMap.put(13, new Color(0xFF667F33, true, false));
		colorMap.put(14, new Color(0xFF993333, true, false));
		colorMap.put(15, new Color(0xFF191919, true, false));
	}
	
	public static void createBannerTexture(String data, File textureFolder, String name) throws Exception {
		JsonObject jsonData = JsonParser.parseString(data).getAsJsonObject();
		BufferedImage baseImg = ImageIO.read(ResourcePack.getFile(patternMap.get(""), "textures", ".png", "assets"));
		BufferedImage resImg = new BufferedImage(baseImg.getWidth(), baseImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for(int j = 0; j < baseImg.getHeight(); j++)
			for(int i = 0; i < baseImg.getWidth(); ++i)
				resImg.setRGB(i, j, baseImg.getRGB(i, j));
		if(jsonData.has("color")) {
			tint(resImg, colorMap.getOrDefault(jsonData.get("color").getAsInt(), new Color(1f, 1f, 1f, 1f)));
		}
		if(jsonData.has("patterns")) {
			for(JsonElement el : jsonData.get("patterns").getAsJsonArray().asList()) {
				BufferedImage img = ImageIO.read(ResourcePack.getFile(
						patternMap.getOrDefault(el.getAsJsonObject().get("name").getAsString(), ""), 
						"textures", ".png", "assets"));
				composite(resImg, img, 
						colorMap.getOrDefault(el.getAsJsonObject().get("color").getAsInt(), new Color(1f, 1f, 1f, 1f)));
			}
		}
		
		if(!textureFolder.exists())
			textureFolder.mkdirs();
		ImageIO.write(resImg, "png", new File(textureFolder, name + ".png"));
	}
	
	private static void tint(BufferedImage img, Color tint) {
		for(int j = 0; j < img.getHeight(); ++j) {
			for(int i = 0; i < img.getWidth(); ++i) {
				Color color = new Color(img.getRGB(i, j), true, false);
				color.mult(tint);
				img.setRGB(i, j, color.getRGB());
			}
		}
	}
	
	private static void composite(BufferedImage bg, BufferedImage fg, Color tint) {
		int width = Math.min(bg.getWidth(), fg.getWidth());
		int height = Math.min(bg.getHeight(), fg.getHeight());
		for(int j = 0; j < width; ++j) {
			for(int i = 0; i < height; ++i) {
				Color colorBG = new Color(bg.getRGB(i, j), true, false);
				Color colorFG = new Color(fg.getRGB(i, j), true, false);
				colorFG.mult(tint);
				colorBG.composite(colorFG);
				bg.setRGB(i, j, colorBG.getRGB());
			}
		}
	}
	
}
