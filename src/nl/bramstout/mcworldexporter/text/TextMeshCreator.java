package nl.bramstout.mcworldexporter.text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.resourcepack.Font;

public class TextMeshCreator {
	
	public static enum Alignment{
		LEFT, CENTER, RIGHT, TOP, BOTTOM
	}
	
	public static float defaultDistanceBetweenChars = 1f/8f;
	public static float defaultDistanceBetweenBaselines = 10f/8f;
	
	private static Map<Integer, Color> COLOR_CODES = new HashMap<Integer, Color>();
	private static Map<Integer, Color> GLOW_COLOR_CODES = new HashMap<Integer, Color>();
	static {
		COLOR_CODES.put(Integer.valueOf((int) '0'), new Color(0x000000));
		COLOR_CODES.put(Integer.valueOf((int) '1'), new Color(0x0000AA));
		COLOR_CODES.put(Integer.valueOf((int) '2'), new Color(0x00AA00));
		COLOR_CODES.put(Integer.valueOf((int) '3'), new Color(0x00AAAA));
		COLOR_CODES.put(Integer.valueOf((int) '4'), new Color(0xAA0000));
		COLOR_CODES.put(Integer.valueOf((int) '5'), new Color(0xAA00AA));
		COLOR_CODES.put(Integer.valueOf((int) '6'), new Color(0xFFAA00));
		COLOR_CODES.put(Integer.valueOf((int) '7'), new Color(0xAAAAAA));
		COLOR_CODES.put(Integer.valueOf((int) '8'), new Color(0x555555));
		COLOR_CODES.put(Integer.valueOf((int) '9'), new Color(0x5555FF));
		COLOR_CODES.put(Integer.valueOf((int) 'a'), new Color(0x55FF55));
		COLOR_CODES.put(Integer.valueOf((int) 'b'), new Color(0x55FFFF));
		COLOR_CODES.put(Integer.valueOf((int) 'c'), new Color(0xFF5555));
		COLOR_CODES.put(Integer.valueOf((int) 'd'), new Color(0xFF55FF));
		COLOR_CODES.put(Integer.valueOf((int) 'e'), new Color(0xFFFF55));
		COLOR_CODES.put(Integer.valueOf((int) 'f'), new Color(0xFFFFFF));
		COLOR_CODES.put(Integer.valueOf((int) 'g'), new Color(0xDDD605));
		COLOR_CODES.put(Integer.valueOf((int) 'h'), new Color(0xE3D4D1));
		COLOR_CODES.put(Integer.valueOf((int) 'i'), new Color(0xCECACA));
		COLOR_CODES.put(Integer.valueOf((int) 'j'), new Color(0x443A3B));
		COLOR_CODES.put(Integer.valueOf((int) 'p'), new Color(0xDEB12D));
		COLOR_CODES.put(Integer.valueOf((int) 'q'), new Color(0x47A036));
		COLOR_CODES.put(Integer.valueOf((int) 's'), new Color(0x2CBAA8));
		COLOR_CODES.put(Integer.valueOf((int) 't'), new Color(0x21497B));
		COLOR_CODES.put(Integer.valueOf((int) 'u'), new Color(0x9A5CC6));
		
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '0'), new Color(0xF0EBCC));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '1'), new Color(0x000066));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '2'), new Color(0x006600));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '3'), new Color(0x006666));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '4'), new Color(0x660000));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '5'), new Color(0x660066));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '6'), new Color(0x664400));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '7'), new Color(0x333333));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '8'), new Color(0x222222));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) '9'), new Color(0x222266));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'a'), new Color(0x226622));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'b'), new Color(0x226666));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'c'), new Color(0x662222));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'd'), new Color(0x662266));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'e'), new Color(0x666622));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'f'), new Color(0x666666));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'g'), new Color(0x223202));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'h'), new Color(0x513130));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'i'), new Color(0x454545));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'j'), new Color(0x111214));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'p'), new Color(0x444004));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'q'), new Color(0x123012));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 's'), new Color(0x043333));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 't'), new Color(0x011224));
		GLOW_COLOR_CODES.put(Integer.valueOf((int) 'u'), new Color(0x342442));
	}
	
	public static void generateText(JsonElement text, Font font, Color defaultColor, Color glowColor,
			float distanceBetweenChars, float distanceBetweenBaselines,
			Alignment horizontalAlignment, Alignment verticalAlignment, float scale, boolean glowing,
			List<ModelFace> outputFaces) {
		String resText = parseJsonElement(text, "");
		
		generateText(resText, font, defaultColor, distanceBetweenChars, distanceBetweenBaselines,
				horizontalAlignment, verticalAlignment, scale, 0f, 0f, 0f, false, outputFaces);
		
		if(glowing) {
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale, -1f/8f, -1f/8f, -0.05f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale,  0f/8f, -1f/8f, -0.052f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale,  1f/8f, -1f/8f, -0.054f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale, -1f/8f,  1f/8f, -0.056f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale,  0f/8f,  1f/8f, -0.058f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale,  1f/8f,  1f/8f, -0.06f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale, -1f/8f,  0f/8f, -0.062f, true, outputFaces);
			
			generateText(resText, font, glowColor, distanceBetweenChars, distanceBetweenBaselines,
					horizontalAlignment, verticalAlignment, scale,  1f/8f,  0f/8f, -0.064f, true, outputFaces);
		}
	}
	
	private static String parseJsonElement(JsonElement text, String parentFormatCode) {
		JsonObject obj = toJsonObject(text);
		if(obj == null)
			return "";
		
		String str = "";
		String formatCode = "";
		if(obj.has("color")) {
			String colorStr = obj.get("color").getAsString();
			if(colorStr.equalsIgnoreCase("black"))
				formatCode += "§0";
			else if(colorStr.equalsIgnoreCase("dark_blue"))
				formatCode += "§1";
			else if(colorStr.equalsIgnoreCase("dark_green"))
				formatCode += "§2";
			else if(colorStr.equalsIgnoreCase("dark_aqua"))
				formatCode += "§3";
			else if(colorStr.equalsIgnoreCase("dark_red"))
				formatCode += "§4";
			else if(colorStr.equalsIgnoreCase("dark_purple"))
				formatCode += "§5";
			else if(colorStr.equalsIgnoreCase("gold"))
				formatCode += "§6";
			else if(colorStr.equalsIgnoreCase("gray"))
				formatCode += "§7";
			else if(colorStr.equalsIgnoreCase("dark_gray"))
				formatCode += "§8";
			else if(colorStr.equalsIgnoreCase("blue"))
				formatCode += "§9";
			else if(colorStr.equalsIgnoreCase("green"))
				formatCode += "§a";
			else if(colorStr.equalsIgnoreCase("aqua"))
				formatCode += "§b";
			else if(colorStr.equalsIgnoreCase("red"))
				formatCode += "§c";
			else if(colorStr.equalsIgnoreCase("light_purple"))
				formatCode += "§d";
			else if(colorStr.equalsIgnoreCase("yellow"))
				formatCode += "§e";
			else if(colorStr.equalsIgnoreCase("white"))
				formatCode += "§f";
			else if(colorStr.startsWith("#") && colorStr.length() == 7) {
				formatCode += "§" + colorStr;
			}
		}
		if(obj.has("bold")) {
			formatCode += obj.get("bold").getAsBoolean() ? "§l" : "§!l";
		}
		if(obj.has("italic")) {
			formatCode += obj.get("italic").getAsBoolean() ? "§o" : "§!o";
		}
		if(obj.has("underlined")) {
			formatCode += obj.get("underlined").getAsBoolean() ? "§n" : "§!n";
		}
		if(obj.has("strikethrough")) {
			formatCode += obj.get("strikethrough").getAsBoolean() ? "§m" : "§!m";
		}
		
		str += "§r" + parentFormatCode + formatCode;
		if(obj.has("text")) {
			str += obj.get("text").getAsString();
		}
		if(obj.has("extra")) {
			for(JsonElement el : obj.getAsJsonArray("extra").asList()) {
				str += parseJsonElement(el, parentFormatCode + formatCode);
			}
		}
		return str;
	}
	
	private static JsonObject toJsonObject(JsonElement text) {
		if(text.isJsonObject())
			return text.getAsJsonObject();
		if(text.isJsonPrimitive()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("text", text.getAsString());
			return obj;
		}
		if(text.isJsonArray()) {
			JsonObject firstObject = null;
			JsonArray extrasArray = null;
			for(JsonElement el : text.getAsJsonArray().asList()) {
				JsonObject obj = toJsonObject(el);
				if(firstObject == null) {
					firstObject = obj.deepCopy();
					if(obj.has("extra"))
						extrasArray = firstObject.getAsJsonArray("extra");
					else {
						extrasArray = new JsonArray();
						firstObject.add("extra", extrasArray);
					}
				}else {
					extrasArray.add(obj);
				}
			}
			return firstObject;
		}
		return null;
	}
	
	public static void generateText(String text, Font font, Color defaultColor,
			float distanceBetweenChars, float distanceBetweenBaselines,
			Alignment horizontalAlignment, Alignment verticalAlignment, float scale, 
			float offsetX, float offsetY, float offsetZ, boolean useGlowColors,
			List<ModelFace> outputFaces) {
		int outputFacesStartIndex = outputFaces.size();
		int lineFaceStartIndex = outputFaces.size();
		float xOffset = 0f;
		float yOffset = 0f;
		float[] minMaxPoints = new float[] {
				0f, 0f, 0f, 1f, 1f, 0f
		};
		Direction dir = Direction.SOUTH;
		JsonObject faceData = new JsonObject();
		JsonArray uvArray = new JsonArray();
		uvArray.add(0.0f);
		uvArray.add(0.0f);
		uvArray.add(1.0f);
		uvArray.add(1.0f);
		faceData.add("uv", uvArray);
		OfInt it = text.codePoints().iterator();
		
		Color currentColor = defaultColor;
		boolean isBold = false;
		boolean isStrikethrough = false;
		boolean isUnderlined = false;
		boolean isItalic = false;
		boolean isFormatCode = false;
		boolean undoFormat = false;
		Font.Character lineCharInfo = font.getCharacterInfo((int) '_');
		
		while(it.hasNext()) {
			int codepoint = it.nextInt();
			
			if(codepoint == (int) '\n') {
				// new line
				float xAlignOffset = 0f;
				if(horizontalAlignment == Alignment.CENTER) {
					xAlignOffset = xOffset / 2f;
				}else if(horizontalAlignment == Alignment.RIGHT || horizontalAlignment == Alignment.BOTTOM) {
					xAlignOffset = xOffset;
				}
				if(xAlignOffset != 0f) {
					for(int i = lineFaceStartIndex; i < outputFaces.size(); ++i) {
						outputFaces.get(i).translate(-xAlignOffset, 0f, 0f);
					}
				}
				lineFaceStartIndex = outputFaces.size();
				xOffset = 0f;
				yOffset -= distanceBetweenBaselines;
			}
			
			if(codepoint == (int) '§') {
				isFormatCode = true;
				continue;
			}
			if(isFormatCode && codepoint == (int) '!') {
				undoFormat = true;
				continue;
			}
			
			if(isFormatCode) {
				if(codepoint == (int) 'l')
					isBold = !undoFormat;
				else if(codepoint == (int) 'm')
					isStrikethrough = !undoFormat;
				else if(codepoint == (int) 'n')
					isUnderlined = !undoFormat;
				else if(codepoint == (int) 'o')
					isItalic = !undoFormat;
				else {
					// Any other formatting code will always reset
					// the style formats.
					isBold = false;
					isStrikethrough = false;
					isUnderlined = false;
					isItalic = false;
					
					// Colors
					if(codepoint == (int) 'r')
						currentColor = defaultColor;
					else if(codepoint == (int) '#') {
						// It's a hex color
						String hexStr = "";
						for(int i = 0; i < 6; ++i) {
							if(!it.hasNext())
								break;
							hexStr += Character.toString((char) it.nextInt());
						}
						try {
							int hexColor = Integer.parseInt(hexStr, 16);
							currentColor = new Color(hexColor);
							if(useGlowColors)
								currentColor.mult(0.5f);
						}catch(Exception ex) {}
					}else
						currentColor = (useGlowColors ? GLOW_COLOR_CODES : COLOR_CODES).getOrDefault(
								Integer.valueOf(codepoint), defaultColor);
				}
				undoFormat = false;
				isFormatCode = false;
				continue;
			}
			undoFormat = false;
			isFormatCode = false;
			
			Font.Character charInfo = font.getCharacterInfo(codepoint);
			
			if(charInfo == null)
				continue;
			
			if(xOffset != 0f)
				xOffset += distanceBetweenChars;
			
			if(charInfo.getTexture() != null) {
				addCharacterQuad(charInfo, xOffset, yOffset, 0f, minMaxPoints, dir, faceData, uvArray,
									currentColor, isItalic, outputFaces);
				if(isBold) {
					xOffset += 1f/8f;
					addCharacterQuad(charInfo, xOffset, yOffset, 0.01f, minMaxPoints, dir, faceData, uvArray,
							currentColor, isItalic, outputFaces);
				}
				
				if(isUnderlined) {
					Font.Character underlineInfo = new Font.Character(
							lineCharInfo.getTexture(), 
							charInfo.getWidth() + distanceBetweenChars + (isBold ? 1f/8f : 0f), 
							lineCharInfo.getHeight(), 
							lineCharInfo.getAscent(), lineCharInfo.getTexU(), lineCharInfo.getTexV(), 
							lineCharInfo.getTexWidth(), lineCharInfo.getTexHeight());
					addCharacterQuad(underlineInfo, 
							xOffset - distanceBetweenChars / 2f - (isBold ? 1f/8f : 0f), 
							yOffset - 2f/8f, 0.02f, minMaxPoints, dir, 
							faceData, uvArray, currentColor, isItalic, outputFaces);
				}
				if(isStrikethrough) {
					Font.Character underlineInfo = new Font.Character(
							lineCharInfo.getTexture(), 
							charInfo.getWidth() + distanceBetweenChars + (isBold ? 1f/8f : 0f), 
							lineCharInfo.getHeight(), 
							lineCharInfo.getAscent(), lineCharInfo.getTexU(), lineCharInfo.getTexV(), 
							lineCharInfo.getTexWidth(), lineCharInfo.getTexHeight());
					addCharacterQuad(underlineInfo, 
							xOffset - distanceBetweenChars / 2f - (isBold ? 1f/8f : 0f), 
							yOffset + 4f/8f, 0.02f, minMaxPoints, dir, 
							faceData, uvArray, currentColor, isItalic, outputFaces);
				}
			}
			
			xOffset += charInfo.getWidth();
		}
		
		float xAlignOffset = 0f;
		if(horizontalAlignment == Alignment.CENTER) {
			xAlignOffset = xOffset / 2f;
		}else if(horizontalAlignment == Alignment.RIGHT || horizontalAlignment == Alignment.BOTTOM) {
			xAlignOffset = xOffset;
		}
		if(xAlignOffset != 0f) {
			for(int i = lineFaceStartIndex; i < outputFaces.size(); ++i) {
				outputFaces.get(i).translate(-xAlignOffset, 0f, 0f);
			}
		}
		
		float topY = 1.0f;
		float bottomY = yOffset;
		float yAlignOffset = topY;
		if(verticalAlignment == Alignment.CENTER) {
			yAlignOffset = (topY + bottomY) / 2f;
		}else if(verticalAlignment == Alignment.BOTTOM || verticalAlignment == Alignment.RIGHT) {
			yAlignOffset = bottomY;
		}
		for(int i = outputFacesStartIndex; i < outputFaces.size(); ++i) {
			outputFaces.get(i).translate(offsetX, offsetY-yAlignOffset, offsetZ);
			outputFaces.get(i).scale(scale, scale, 1f, 0f, 0f, 0f);
		}
	}
	
	private static void addCharacterQuad(Font.Character charInfo, float xOffset, float yOffset, float zOffset,
										float[] minMaxPoints, Direction dir, JsonObject faceData, JsonArray uvArray,
										Color currentColor, boolean isItalic, List<ModelFace> outputFaces) {
		faceData.addProperty("texture", charInfo.getTexture());
		uvArray.set(0, new JsonPrimitive(charInfo.getTexU() * 16f));
		uvArray.set(1, new JsonPrimitive(charInfo.getTexV() * 16f));
		uvArray.set(2, new JsonPrimitive((charInfo.getTexU() + charInfo.getTexWidth()) * 16f));
		uvArray.set(3, new JsonPrimitive((charInfo.getTexV() + charInfo.getTexHeight()) * 16f));
		
		minMaxPoints[0] = xOffset;
		minMaxPoints[1] = yOffset + charInfo.getAscent() - charInfo.getHeight();
		minMaxPoints[2] = zOffset;
		minMaxPoints[3] = xOffset + charInfo.getWidth();
		minMaxPoints[4] = yOffset + charInfo.getAscent();
		minMaxPoints[5] = zOffset;
		
		ModelFace face = new ModelFace(minMaxPoints, dir, faceData, false);
		face.setFaceColour(currentColor.getR(), currentColor.getG(), currentColor.getB());
		
		if(isItalic) {
			float bottomOffset = (charInfo.getAscent() - charInfo.getHeight()) * (2f/8f) - (1f/8f);
			float topOffset = (charInfo.getAscent()) * (2f/8f) - (1f/8f);
			face.getPoints()[0] += bottomOffset;
			face.getPoints()[3] += bottomOffset;
			face.getPoints()[6] += topOffset;
			face.getPoints()[9] += topOffset;
		}
		
		outputFaces.add(face);
	}

}
