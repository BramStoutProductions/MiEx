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

package nl.bramstout.mcworldexporter.model.builtins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.text.TextMeshCreator;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class BlockStateSign extends BlockState{

	private static Map<String, Color> COLORS = new HashMap<String, Color>();
	private static Map<String, Color> GLOW_COLORS = new HashMap<String, Color>();
	static {
		COLORS.put("white", new Color(0xFFFFFF));
		COLORS.put("orange", new Color(0xFF681F));
		COLORS.put("magenta", new Color(0xFF00FF));
		COLORS.put("light_blue", new Color(0x9AC0CD));
		COLORS.put("yellow", new Color(0xFFFF00));
		COLORS.put("lime", new Color(0xBFFF00));
		COLORS.put("pink", new Color(0xFF69B4));
		COLORS.put("gray", new Color(0x808080));
		COLORS.put("light_gray", new Color(0xD3D3D3));
		COLORS.put("cyan", new Color(0x00FFFF));
		COLORS.put("purple", new Color(0xA020F0));
		COLORS.put("blue", new Color(0x0000FF));
		COLORS.put("brown", new Color(0x8B4513));
		COLORS.put("green", new Color(0x00FF00));
		COLORS.put("red", new Color(0xFF0000));
		COLORS.put("black", new Color(0x000000));
		
		GLOW_COLORS.put("white", new Color(0x666666));
		GLOW_COLORS.put("orange", new Color(0x66290C));
		GLOW_COLORS.put("magenta", new Color(0x660066));
		GLOW_COLORS.put("light_blue", new Color(0x3D4C52));
		GLOW_COLORS.put("yellow", new Color(0x666600));
		GLOW_COLORS.put("lime", new Color(0x4C6600));
		GLOW_COLORS.put("pink", new Color(0x662A48));
		GLOW_COLORS.put("gray", new Color(0x333333));
		GLOW_COLORS.put("light_gray", new Color(0x545454));
		GLOW_COLORS.put("cyan", new Color(0x006666));
		GLOW_COLORS.put("purple", new Color(0x400C60));
		GLOW_COLORS.put("blue", new Color(0x000066));
		GLOW_COLORS.put("brown", new Color(0x371B07));
		GLOW_COLORS.put("green", new Color(0x006600));
		GLOW_COLORS.put("red", new Color(0x660000));
		GLOW_COLORS.put("black", new Color(0xF0EBCC));
	}
	
	public BlockStateSign(String name, int dataVersion) {
		super(name, dataVersion, null);
	}
	
	public String getDefaultTexture() {
		return "minecraft:entity/signs/oak";
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, boolean runBlockConnections) {
		if(blockConnections != null && runBlockConnections) {
			properties = (NbtTagCompound) properties.copy();
			String newName = blockConnections.map(name, properties, x, y, z);
			if(newName != null && !newName.equals(name)) {
				Reference<char[]> charBuffer = new Reference<char[]>();
				int blockId = BlockRegistry.getIdForName(newName, properties, dataVersion, charBuffer);
				properties.free();
				return BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z, runBlockConnections);
			}
		}
		
		List<List<Model>> models = new ArrayList<List<Model>>();
		
		List<Model> list = new ArrayList<Model>();
		Model model = new Model("sign", null, false);
		list.add(model);
		models.add(list);
		
		boolean isWall = name.contains("wall");
		float rotY = 0f;
		if (isWall) {
			String val = properties.get("facing").asString();
			if (val == null)
				val = "north";
			if (val.equals("north")) {
				rotY = 180f;
			} else if (val.equals("east")) {
				rotY = 270f;
			} else if (val.equals("south")) {
				rotY = 0f;
			} else if (val.equals("west")) {
				rotY = 90f;
			}
		} else {
			String val = properties.get("rotation").asString();
			if (val == null)
				val = "0";
			int ival = 0;
			try {
				ival = Integer.parseInt(val);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			rotY = (((float) ival) / 16f) * 360f;
		}
		
		model.addTexture("#texture", "minecraft:entity/signs/" + name.replace("minecraft:", "").replace("_wall_", "_").replace("_sign", ""));
		
		float scale = 16f / 24f;
		float offsetX = 8f;
		float offsetY = isWall ? 8.3333f : 0f;
		float offsetZ = isWall ? 1f : 8f;

		// sign part
		float minX = -12f * scale + offsetX;
		float minY = (isWall ? -6f : 14f) * scale + offsetY;
		float minZ = -1f * scale + offsetZ;
		float maxX = 12f * scale + offsetX;
		float maxY = (isWall ? 6f : 26f) * scale + offsetY;
		float maxZ = 1f * scale + offsetZ;

		model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 0f, 13f, 7f }, "#texture");
		
		float textOffsetX = (minX + maxX) / 2f;
		float textOffsetY = (minY + maxY) / 2f + 0.133f;
		float textOffsetZ = maxZ + 0.25f;
		float textOffsetZBack = minZ - 0.25f;

		// Post part
		if (!isWall) {
			minX = -1f * scale + offsetX;
			minY = 0f * scale + offsetY;
			minZ = -1f * scale + offsetZ;
			maxX = 1f * scale + offsetX;
			maxY = 14f * scale + offsetY;
			maxZ = 1f * scale + offsetZ;
			
			model.addEntityCube(new float[] { minX, minY, minZ, maxX, maxY, maxZ }, new float[] { 0f, 7f, 2f, 15f }, "#texture");
		}
		
		// Text
		float lineDistance = (2.5f) * (16f/24f);
		handleText(properties, textOffsetX, textOffsetY, textOffsetZ, textOffsetZBack, 6.666f / 5.0f, lineDistance, model);
		
		model.rotate(0, rotY, false);
		
		BakedBlockState bakedState = new BakedBlockState(name, models, transparentOcclusion, leavesOcclusion, detailedOcclusion, 
				individualBlocks, hasLiquid(properties), caveBlock, false, false, false, false, false, false, false, false, true, 0, null,
				needsConnectionInfo());
		if(blockConnections != null && runBlockConnections) {
			properties.free(); // Free the copy that we made.
		}
		return bakedState;
	}
	
	public static void handleText(NbtTagCompound properties, float textOffsetX, float textOffsetY, float textOffsetZ, 
									float textOffsetZBack, float textScale, float lineDistance, Model model) {
		JsonElement frontText1 = null;
		JsonElement frontText2 = null;
		JsonElement frontText3 = null;
		JsonElement frontText4 = null;
		JsonElement backText1 = null;
		JsonElement backText2 = null;
		JsonElement backText3 = null;
		JsonElement backText4 = null;
		Color frontColor = new Color(0f, 0f, 0f);
		Color backColor = new Color(0f, 0f, 0f);
		Color frontGlowColor = new Color(0xF0EBCC);
		Color backGlowColor = new Color(0xF0EBCC);
		boolean frontGlowing = false;
		boolean backGlowing = false;
		NbtTagString colorTag = (NbtTagString) properties.get("Color");
		if(colorTag != null) {
			frontColor = COLORS.getOrDefault(colorTag.getData(), frontColor);
			backColor = frontColor;
			frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.getData(), frontGlowColor);
			backGlowColor = frontGlowColor;
		}
		NbtTagByte glowingTextTag = (NbtTagByte) properties.get("GlowingText");
		if(glowingTextTag != null) {
			frontGlowing = glowingTextTag.getData() > 0;
			backGlowing = frontGlowing;
		}
		
		if(properties.get("Text1") != null) {
			NbtTag text1 = properties.get("Text1");
			NbtTag text2 = properties.get("Text2");
			NbtTag text3 = properties.get("Text3");
			NbtTag text4 = properties.get("Text4");
			
			if(text1 != null)
				frontText1 = new JsonPrimitive(text1.asString());
			if(text2 != null)
				frontText2 = new JsonPrimitive(text2.asString());
			if(text3 != null)
				frontText3 = new JsonPrimitive(text3.asString());
			if(text4 != null)
				frontText4 = new JsonPrimitive(text4.asString());
		}
		NbtTagCompound frontText = (NbtTagCompound) properties.get("front_text");
		if(frontText != null) {
			colorTag = (NbtTagString) frontText.get("color");
			if(colorTag != null) {
				frontColor = COLORS.getOrDefault(colorTag.getData(), frontColor);
				frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.getData(), frontGlowColor);
			}
			
			glowingTextTag = (NbtTagByte) frontText.get("has_glowing_text");
			if(glowingTextTag != null)
				frontGlowing = glowingTextTag.getData() > 0;
			
			NbtTagList messages = (NbtTagList) frontText.get("messages");
			if(messages != null) {
				int i = 0;
				for(NbtTag tag : messages.getData()) {
					try {
						JsonElement el = JsonParser.parseString(((NbtTagString) tag).getData());
						if(i == 0)
							frontText1 = el;
						else if(i == 1)
							frontText2 = el;
						else if(i == 2)
							frontText3 = el;
						else if(i == 3)
							frontText4 = el;
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					i++;
				}
			}
		}
		NbtTagCompound backText = (NbtTagCompound) properties.get("back_text");
		if(backText != null) {
			colorTag = (NbtTagString) backText.get("color");
			if(colorTag != null) {
				backColor = COLORS.getOrDefault(colorTag.getData(), backColor);
				backGlowColor = GLOW_COLORS.getOrDefault(colorTag.getData(), backGlowColor);
			}
			
			glowingTextTag = (NbtTagByte) frontText.get("has_glowing_text");
			if(glowingTextTag != null)
				backGlowing = glowingTextTag.getData() > 0;
			
			NbtTagList messages = (NbtTagList) backText.get("messages");
			if(messages != null) {
				int i = 0;
				for(NbtTag tag : messages.getData()) {
					try {
						JsonElement el = JsonParser.parseString(((NbtTagString) tag).getData());
						if(i == 0)
							backText1 = el;
						else if(i == 1)
							backText2 = el;
						else if(i == 2)
							backText3 = el;
						else if(i == 3)
							backText4 = el;
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					i++;
				}
			}
		}
		
		Font font = ResourcePacks.getFont("minecraft:default");
		
		if(frontText1 != null) {
			addText(frontText1, font, textOffsetX, textOffsetY + lineDistance * 2f, textOffsetZ, textScale, 
					0f, frontColor, frontGlowColor, frontGlowing, model);
		}
		if(frontText2 != null) {
			addText(frontText2, font, textOffsetX, textOffsetY + lineDistance, textOffsetZ, textScale, 
					0f, frontColor, frontGlowColor, frontGlowing, model);
		}
		if(frontText3 != null) {
			addText(frontText3, font, textOffsetX, textOffsetY, textOffsetZ, textScale, 
					0f, frontColor, frontGlowColor, frontGlowing, model);
		}
		if(frontText4 != null) {
			addText(frontText4, font, textOffsetX, textOffsetY - lineDistance, textOffsetZ, textScale, 
					0f, frontColor, frontGlowColor, frontGlowing, model);
		}
		
		if(backText1 != null) {
			addText(backText1, font, textOffsetX, textOffsetY + lineDistance * 2f, textOffsetZBack, textScale, 
					180f, backColor, backGlowColor, backGlowing, model);
		}
		if(backText2 != null) {
			addText(backText2, font, textOffsetX, textOffsetY + lineDistance, textOffsetZBack, textScale, 
					180f, backColor, backGlowColor, backGlowing, model);
		}
		if(backText3 != null) {
			addText(backText3, font, textOffsetX, textOffsetY, textOffsetZBack, textScale, 
					180f, backColor, backGlowColor, backGlowing, model);
		}
		if(backText4 != null) {
			addText(backText4, font, textOffsetX, textOffsetY - lineDistance, textOffsetZBack, textScale, 
					180f, backColor, backGlowColor, backGlowing, model);
		}
	}
	
	public static void addText(JsonElement text, Font font, float textOffsetX, float textOffsetY, float textOffsetZ, float textScale, 
							float rotY, Color defaultColor, Color glowColor, boolean glowing, Model model) {
		List<ModelFace> textFaces = new ArrayList<ModelFace>();
		TextMeshCreator.generateText(text, font, defaultColor, glowColor,
				TextMeshCreator.defaultDistanceBetweenChars, TextMeshCreator.defaultDistanceBetweenBaselines, 
				TextMeshCreator.Alignment.CENTER, TextMeshCreator.Alignment.TOP, textScale, glowing, textFaces);
		for(ModelFace face : textFaces) {
			if(rotY != 0f)
				face.rotate(0f, rotY, 0f, 0f, 0f, 0f);
			face.translate(textOffsetX, textOffsetY, textOffsetZ);
			
			String texKey = null;
			for(Entry<String, String> entry : model.getTextures().entrySet()) {
				if(entry.getValue().equals(face.getTexture())) {
					texKey = entry.getKey();
				}
			}
			if(texKey == null) {
				texKey = "#font_" + Integer.toString(model.getTextures().size());
				String texture = face.getTexture();
				if(glowing)
					texture = texture.replace("font/", "font/glowing/");
				model.addTexture(texKey, texture);
			}
			face.setTexture(texKey);
			
			model.getFaces().add(face);
		}
	}

}
