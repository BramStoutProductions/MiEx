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
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.Context;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.Expression;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.Value;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.ValueDict;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.text.TextMeshCreator;

public abstract class BuiltInGenerator {

	public abstract void eval(Context context, Map<String, Expression> arguments);

	private static Map<String, BuiltInGenerator> generatorRegistry = new HashMap<String, BuiltInGenerator>();
	static {
		generatorRegistry.put("text", new BuiltInGeneratorText());
		generatorRegistry.put("signText", new BuiltInGeneratorSignText());
	}
	
	public static BuiltInGenerator getGenerator(String type) {
		return generatorRegistry.getOrDefault(type, null);
	}

	public static class BuiltInGeneratorSignText extends BuiltInGenerator {

		@Override
		public void eval(Context context, Map<String, Expression> arguments) {
			Value properties = null;
			float textOffsetX = 0f;
			float textOffsetY = 0f;
			float textOffsetZ = 0f;
			float textOffsetZBack = 0f;
			float textScale = 0f;
			float lineDistance = 0f;
			Font font = null;
			
			if(arguments.containsKey("state"))
				properties = arguments.get("state").eval(context);
			
			if(arguments.containsKey("textOffsetX"))
				textOffsetX = arguments.get("textOffsetX").eval(context).asFloat();
			
			if(arguments.containsKey("textOffsetY"))
				textOffsetY = arguments.get("textOffsetY").eval(context).asFloat();
			
			if(arguments.containsKey("textOffsetZ"))
				textOffsetZ = arguments.get("textOffsetZ").eval(context).asFloat();
			
			if(arguments.containsKey("textOffsetZBack"))
				textOffsetZBack = arguments.get("textOffsetZBack").eval(context).asFloat();
			
			if(arguments.containsKey("textScale"))
				textScale = arguments.get("textScale").eval(context).asFloat();
			
			if(arguments.containsKey("lineDistance"))
				lineDistance = arguments.get("lineDistance").eval(context).asFloat();
			
			if (arguments.containsKey("font"))
				font = ResourcePacks.getFont(arguments.get("font").eval(context).asString());
			else
				font = ResourcePacks.getFont("minecraft:default");

			if(properties != null)
				handleText(properties, textOffsetX, textOffsetY, textOffsetZ, textOffsetZBack, 
							textScale, lineDistance, font, context.model);
		}
		
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

		public static void handleText(Value properties, float textOffsetX, float textOffsetY,
				float textOffsetZ, float textOffsetZBack, float textScale, float lineDistance, Font font, Model model) {
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
			Value colorTag = properties.member("Color");
			if (colorTag != null && !colorTag.isNull()) {
				frontColor = COLORS.getOrDefault(colorTag.asString(), frontColor);
				backColor = frontColor;
				frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), frontGlowColor);
				backGlowColor = frontGlowColor;
			}
			Value glowingTextTag = properties.member("GlowingText");
			if (glowingTextTag != null && !glowingTextTag.isNull()) {
				frontGlowing = glowingTextTag.asInt() > 0;
				backGlowing = frontGlowing;
			}

			// Java Format
			Value text1 = properties.member("Text1");
			if (text1 != null && !text1.isNull()) {
				Value text2 = properties.member("Text2");
				Value text3 = properties.member("Text3");
				Value text4 = properties.member("Text4");

				if (text1 != null && !text1.isNull())
					frontText1 = new JsonPrimitive(text1.asString());
				if (text2 != null && !text2.isNull())
					frontText2 = new JsonPrimitive(text2.asString());
				if (text3 != null && !text3.isNull())
					frontText3 = new JsonPrimitive(text3.asString());
				if (text4 != null && !text4.isNull())
					frontText4 = new JsonPrimitive(text4.asString());
			}
			Value frontText = properties.member("front_text");
			if (frontText != null && !frontText.isNull()) {
				colorTag = frontText.member("color");
				if (colorTag != null && !colorTag.isNull()) {
					frontColor = COLORS.getOrDefault(colorTag.asString(), frontColor);
					frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), frontGlowColor);
				}

				glowingTextTag = frontText.member("has_glowing_text");
				if (glowingTextTag != null && !glowingTextTag.isNull())
					frontGlowing = glowingTextTag.asInt() > 0;

				Value messages = frontText.member("messages");
				if (messages != null && !messages.isNull()) {
					int i = 0;
					for (Value tag : messages.getChildren().values()) {
						try {
							JsonElement el = JsonParser.parseString(tag.asString());
							if (i == 0)
								frontText1 = el;
							else if (i == 1)
								frontText2 = el;
							else if (i == 2)
								frontText3 = el;
							else if (i == 3)
								frontText4 = el;
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						i++;
					}
				}
			}
			Value backText = properties.member("back_text");
			if (backText != null && !backText.isNull()) {
				colorTag = backText.member("color");
				if (colorTag != null && !colorTag.isNull()) {
					backColor = COLORS.getOrDefault(colorTag.asString(), backColor);
					backGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), backGlowColor);
				}

				glowingTextTag = backText.member("has_glowing_text");
				if (glowingTextTag != null && !glowingTextTag.isNull())
					backGlowing = glowingTextTag.asInt() > 0;

				Value messages = backText.member("messages");
				if (messages != null && !messages.isNull()) {
					int i = 0;
					for (Value tag : messages.getChildren().values()) {
						try {
							JsonElement el = JsonParser.parseString(tag.asString());
							if (i == 0)
								backText1 = el;
							else if (i == 1)
								backText2 = el;
							else if (i == 2)
								backText3 = el;
							else if (i == 3)
								backText4 = el;
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						i++;
					}
				}
			}
			
			// Bedrock Format
			frontText = properties.member("FrontText");
			if(frontText != null && !frontText.isNull()) {
				colorTag = frontText.member("SignTextColor");
				if(colorTag != null && !colorTag.isNull()) {
					frontColor = new Color((int) colorTag.asInt());
					frontGlowColor = new Color((int) colorTag.asInt());
				}
				
				glowingTextTag = frontText.member("IgnoreLighting");
				if(glowingTextTag != null && !glowingTextTag.isNull())
					frontGlowing = glowingTextTag.asInt() > 0;
				
				glowingTextTag = frontText.member("HideGlowOutline");
				if(glowingTextTag != null && !glowingTextTag.isNull())
					frontGlowing = glowingTextTag.asInt() == 0;
				
				Value text = frontText.member("Text");
				if(text != null && !text.isNull()) {
					String[] lines = text.asString().split("\n");
					int i = 0;
					for(String line : lines) {
						try {
							JsonElement el = new JsonPrimitive(line);
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
			
			backText = properties.member("BackText");
			if(backText != null && !backText.isNull()) {
				colorTag = backText.member("SignTextColor");
				if(colorTag != null && !colorTag.isNull()) {
					backColor = new Color((int) colorTag.asInt());
					backGlowColor = new Color((int) colorTag.asInt());
				}
				
				glowingTextTag = backText.member("IgnoreLighting");
				if(glowingTextTag != null && !glowingTextTag.isNull())
					backGlowing = glowingTextTag.asInt() > 0;
				
				glowingTextTag = backText.member("HideGlowOutline");
				if(glowingTextTag != null && !glowingTextTag.isNull())
					backGlowing = glowingTextTag.asInt() == 0;
				
				Value text = backText.member("Text");
				if(text != null && !text.isNull()) {
					String[] lines = text.asString().split("\n");
					int i = 0;
					for(String line : lines) {
						try {
							JsonElement el = new JsonPrimitive(line);
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

			if (frontText1 != null) {
				BuiltInGeneratorText.addText(frontText1, font, textOffsetX, textOffsetY + lineDistance * 2f, textOffsetZ, textScale, 0f,
						frontColor, frontGlowColor, frontGlowing, model);
			}
			if (frontText2 != null) {
				BuiltInGeneratorText.addText(frontText2, font, textOffsetX, textOffsetY + lineDistance, textOffsetZ, textScale, 0f,
						frontColor, frontGlowColor, frontGlowing, model);
			}
			if (frontText3 != null) {
				BuiltInGeneratorText.addText(frontText3, font, textOffsetX, textOffsetY, textOffsetZ, textScale, 0f, frontColor,
						frontGlowColor, frontGlowing, model);
			}
			if (frontText4 != null) {
				BuiltInGeneratorText.addText(frontText4, font, textOffsetX, textOffsetY - lineDistance, textOffsetZ, textScale, 0f,
						frontColor, frontGlowColor, frontGlowing, model);
			}

			if (backText1 != null) {
				BuiltInGeneratorText.addText(backText1, font, textOffsetX, textOffsetY + lineDistance * 2f, textOffsetZBack, textScale, 180f,
						backColor, backGlowColor, backGlowing, model);
			}
			if (backText2 != null) {
				BuiltInGeneratorText.addText(backText2, font, textOffsetX, textOffsetY + lineDistance, textOffsetZBack, textScale, 180f,
						backColor, backGlowColor, backGlowing, model);
			}
			if (backText3 != null) {
				BuiltInGeneratorText.addText(backText3, font, textOffsetX, textOffsetY, textOffsetZBack, textScale, 180f, backColor,
						backGlowColor, backGlowing, model);
			}
			if (backText4 != null) {
				BuiltInGeneratorText.addText(backText4, font, textOffsetX, textOffsetY - lineDistance, textOffsetZBack, textScale, 180f,
						backColor, backGlowColor, backGlowing, model);
			}
		}

	}

	public static class BuiltInGeneratorText extends BuiltInGenerator {

		@Override
		public void eval(Context context, Map<String, Expression> arguments) {
			JsonElement text = null;
			Font font = null;
			float textOffsetX = 0f;
			float textOffsetY = 0f;
			float textOffsetZ = 0f;
			float textScale = 0f;
			float rotY = 0f;
			Color defaultColor = new Color(0f, 0f, 0f);
			Color glowColor = new Color(0xF0EBCC);
			boolean glowing = false;

			if (arguments.containsKey("text"))
				text = Json.readString(arguments.get("text").eval(context).asString());
			else
				text = new JsonPrimitive("");

			if (arguments.containsKey("font"))
				font = ResourcePacks.getFont(arguments.get("font").eval(context).asString());
			else
				font = ResourcePacks.getFont("minecraft:default");

			if (arguments.containsKey("textOffsetX"))
				textOffsetX = arguments.get("textOffsetX").eval(context).asFloat();

			if (arguments.containsKey("textOffsetY"))
				textOffsetY = arguments.get("textOffsetY").eval(context).asFloat();

			if (arguments.containsKey("textOffsetZ"))
				textOffsetZ = arguments.get("textOffsetZ").eval(context).asFloat();

			if (arguments.containsKey("textScale"))
				textScale = arguments.get("textScale").eval(context).asFloat();

			if (arguments.containsKey("textRotateY"))
				rotY = arguments.get("textRotateY").eval(context).asFloat();

			if (arguments.containsKey("color")) {
				Value val = arguments.get("color").eval(context);
				defaultColor = parseColor(val);
			}

			if (arguments.containsKey("glowColor")) {
				Value val = arguments.get("glowColor").eval(context);
				glowColor = parseColor(val);
			}

			if (arguments.containsKey("glowing"))
				glowing = arguments.get("glowing").eval(context).asBool();

			addText(text, font, textOffsetX, textOffsetY, textOffsetZ, textScale, rotY, defaultColor, glowColor,
					glowing, context.model);
		}

		private Color parseColor(Value val) {
			if (val.getImpl() instanceof ValueDict) {
				ValueDict valD = (ValueDict) val.getImpl();
				float r = 0f;
				float g = 0f;
				float b = 0f;
				if (valD.getValue().containsKey("0"))
					r = valD.getValue().get("0").asFloat();
				if (valD.getValue().containsKey("r"))
					r = valD.getValue().get("r").asFloat();

				if (valD.getValue().containsKey("1"))
					g = valD.getValue().get("1").asFloat();
				if (valD.getValue().containsKey("g"))
					g = valD.getValue().get("g").asFloat();

				if (valD.getValue().containsKey("2"))
					b = valD.getValue().get("2").asFloat();
				if (valD.getValue().containsKey("b"))
					b = valD.getValue().get("b").asFloat();
				return new Color(r, g, b);
			}
			return new Color((int) val.asInt());
		}

		public static void addText(JsonElement text, Font font, float textOffsetX, float textOffsetY, float textOffsetZ,
				float textScale, float rotY, Color defaultColor, Color glowColor, boolean glowing, Model model) {
			List<ModelFace> textFaces = new ArrayList<ModelFace>();
			TextMeshCreator.generateText(text, font, defaultColor, glowColor,
					TextMeshCreator.defaultDistanceBetweenChars, TextMeshCreator.defaultDistanceBetweenBaselines,
					TextMeshCreator.Alignment.CENTER, TextMeshCreator.Alignment.TOP, textScale, glowing, textFaces);
			for (ModelFace face : textFaces) {
				if (rotY != 0f)
					face.rotate(0f, rotY, 0f, 0f, 0f, 0f);
				face.translate(textOffsetX, textOffsetY, textOffsetZ);

				String texKey = null;
				for (Entry<String, String> entry : model.getTextures().entrySet()) {
					if (entry.getValue().equals(face.getTexture())) {
						texKey = entry.getKey();
					}
				}
				if (texKey == null) {
					texKey = "#font_" + Integer.toString(model.getTextures().size());
					String texture = face.getTexture();
					if (glowing)
						texture = texture.replace("font/", "font/glowing/");
					model.addTexture(texKey, texture);
				}
				face.setTexture(texKey);

				model.getFaces().add(face);
			}
		}

	}

}
