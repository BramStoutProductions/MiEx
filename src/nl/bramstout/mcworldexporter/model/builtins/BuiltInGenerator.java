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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.export.BlendedBiome;
import nl.bramstout.mcworldexporter.expression.ExprContext;
import nl.bramstout.mcworldexporter.expression.ExprValue;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueDict;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueNbtCompound;
import nl.bramstout.mcworldexporter.expression.Expression;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.MapCreator;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagEnd;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagLongArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagShort;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;
import nl.bramstout.mcworldexporter.resourcepack.Font;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.PaintingVariant;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintValue;
import nl.bramstout.mcworldexporter.text.TextMeshCreator;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.BlockRegistry;
import nl.bramstout.mcworldexporter.world.World;

public abstract class BuiltInGenerator {

	public abstract void eval(ExprContext context, Map<String, Expression> arguments);

	private static Map<String, BuiltInGenerator> generatorRegistry = new HashMap<String, BuiltInGenerator>();
	static {
		generatorRegistry.put("model", new BuiltInGeneratorModel());
		generatorRegistry.put("entity", new BuiltInGeneratorEntity());
		generatorRegistry.put("block", new BuiltInGeneratorBlock());
		generatorRegistry.put("item", new BuiltInGeneratorItem());
		generatorRegistry.put("map", new BuiltInGeneratorMap());
		generatorRegistry.put("painting", new BuiltInGeneratorPainting());
		generatorRegistry.put("text", new BuiltInGeneratorText());
		generatorRegistry.put("signText", new BuiltInGeneratorSignText());
	}
	
	public static BuiltInGenerator getGenerator(String type) {
		return generatorRegistry.getOrDefault(type, null);
	}

	public static class BuiltInGeneratorModel extends BuiltInGenerator{

		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			String modelName = null;
			boolean doubleSided = false;
			List<Color> tints = new ArrayList<Color>();
			
			if(arguments.containsKey("model"))
				modelName = arguments.get("model").eval(context).asString();
			if(arguments.containsKey("doubleSided"))
				doubleSided = arguments.get("doubleSided").eval(context).asBool();
			if(arguments.containsKey("tints")) {
				ExprValue tintsVal = arguments.get("tints").eval(context);
				for(Entry<String, ExprValue> tint : tintsVal.getChildren().entrySet()) {
					Color color = new Color(1f, 1f, 1f, 1f);
					
					Map<String, ExprValue> tintChildren = tint.getValue().getChildren();
					if(tintChildren.isEmpty()) {
						color = new Color((int) tint.getValue().asInt());
					}else {
						ExprValue r = tint.getValue().member("0");
						ExprValue g = tint.getValue().member("0");
						ExprValue b = tint.getValue().member("0");
						color = new Color(r.asFloat(), g.asFloat(), b.asFloat());
					}
					
					tints.add(color);
				}
			}
			
			if(modelName == null)
				return;
			
			int modelId = ModelRegistry.getIdForName(modelName, doubleSided);
			Model model = ModelRegistry.getModel(modelId);
			if(model != null)
				context.model.addModel(model, tints);
		}
		
	}
	
	public static class BuiltInGeneratorEntity extends BuiltInGenerator{
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			String entityId = null;
			NbtTagCompound properties = null;
			
			if(arguments.containsKey("id"))
				entityId = arguments.get("id").eval(context).asString();
			if(arguments.containsKey("properties")) {
				NbtTag propertiesTag = arguments.get("properties").eval(context).toNbt();
				if(propertiesTag instanceof NbtTagCompound)
					properties = (NbtTagCompound) propertiesTag;
			}
			
			EntityHandler handler = ResourcePacks.getEntityHandler(entityId);
			
			if(handler == null)
				return;
			
			Entity entity = new Entity(entityId, properties, handler);
			
			Model model = handler.getModel(entity);
			if(model == null)
				return;
			
			context.model.addModel(model);
		}
		
	}
	
	public static class BuiltInGeneratorBlock extends BuiltInGenerator{
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			String blockName = null;
			NbtTagCompound properties = null;
			int x = context.x;
			int y = context.y;
			int z = context.z;
			
			if(arguments.containsKey("name"))
				blockName = arguments.get("name").eval(context).asString();
			if(arguments.containsKey("properties")) {
				NbtTag propertiesTag = arguments.get("properties").eval(context).toNbt();
				if(propertiesTag instanceof NbtTagCompound)
					properties = (NbtTagCompound) propertiesTag.copy();
			}
			if(arguments.containsKey("x"))
				x = (int) arguments.get("x").eval(context).asInt();
			if(arguments.containsKey("y"))
				y = (int) arguments.get("y").eval(context).asInt();
			if(arguments.containsKey("z"))
				z = (int) arguments.get("z").eval(context).asInt();
			
			Reference<char[]> charBuffer = new Reference<char[]>();
			int blockId = BlockRegistry.getIdForName(blockName, properties, Integer.MAX_VALUE, charBuffer);
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, x, y, z);
			
			List<Color> tints = null;
			if(state.getTint() != null) {
				int tintIndex = 0;
				TintValue tintValue = state.getTint().getLayer(tintIndex);
				if(tintValue != null) {
					int biomeId = MCWorldExporter.getApp().getWorld().getBiomeId(x, y, z);
					Biome biome = BiomeRegistry.getBiome(biomeId);
					BlendedBiome blendedBiome = new BlendedBiome();
					blendedBiome.addBiome(biome, 1f);
					blendedBiome.normalise();
					Color tint = tintValue.getColor(blendedBiome);
					tints = new ArrayList<Color>();
					tints.add(tint);
				}
			}
			
			List<Model> models = new ArrayList<Model>();
			state.getModels(x, y, z, models);
			
			for(Model model : models)
				context.model.addModel(model, tints);
		}
		
	}
	
	public static class BuiltInGeneratorItem extends BuiltInGenerator{
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			String itemName = null;
			String displayContext = "";
			NbtTagCompound properties = null;
			
			if(arguments.containsKey("id"))
				itemName = arguments.get("id").eval(context).asString();
			
			if(arguments.containsKey("displayContext"))
				displayContext = arguments.get("displayContext").eval(context).asString();
			
			if(arguments.containsKey("properties")) {
				NbtTag propertiesTag = arguments.get("properties").eval(context).toNbt();
				if(propertiesTag instanceof NbtTagCompound)
					properties = (NbtTagCompound) propertiesTag;
			}
			if(properties == null)
				properties = NbtTagCompound.newNonPooledInstance("properties");
			
			ItemHandler itemHandler = ResourcePacks.getItemHandler(itemName, properties);
			if(itemHandler == null)
				return;
			
			Model model = itemHandler.getModel(itemName, properties, displayContext);
			
			if(model != null) {
				model.applyTransformation(displayContext);
				context.model.addModel(model);
			}
		}
		
	}
	
	public static class BuiltInGeneratorMap extends BuiltInGenerator{
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			NbtTagCompound properties = null;
			
			if(arguments.containsKey("properties")) {
				NbtTag propertiesTag = arguments.get("properties").eval(context).toNbt();
				if(propertiesTag instanceof NbtTagCompound)
					properties = (NbtTagCompound) propertiesTag;
			}
			if(properties == null)
				properties = NbtTagCompound.newNonPooledInstance("properties");
			
			long mapId = -1;
			boolean isBedrock = false;
			
			NbtTagCompound mapTag = (NbtTagCompound) properties.get("tag");
			if(mapTag != null) {
				NbtTag mapTag2 = mapTag.get("map");
				if(mapTag2 != null)
					mapId = mapTag2.asInt();
				NbtTag mapTag3 = mapTag.get("map_uuid");
				if(mapTag3 != null) {
					mapId = mapTag3.asLong();
					isBedrock = true;
				}
			}
			NbtTagCompound componentsTag = (NbtTagCompound) properties.get("components");
			if(componentsTag != null) {
				NbtTag mapTag2 = componentsTag.get("minecraft:map_id");
				if(mapTag2 != null) {
					mapId = mapTag2.asLong();
					isBedrock = false;
				}
			}
			
			Model model = null;
			if(mapId >= 0)
				model = MapCreator.createMapModel(mapId, isBedrock);
			
			if(model != null) {
				context.model.addModel(model);
			}
		}
		
	}
	
	public static class BuiltInGeneratorPainting extends BuiltInGenerator{
		
		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			NbtTagCompound properties = null;
			
			if(arguments.containsKey("properties")) {
				NbtTag propertiesTag = arguments.get("properties").eval(context).toNbt();
				if(propertiesTag instanceof NbtTagCompound)
					properties = (NbtTagCompound) propertiesTag;
			}
			if(properties == null)
				properties = NbtTagCompound.newNonPooledInstance("properties");
			
			String motive = "";
			PaintingVariant variant = null;
			NbtTag motiveTag = properties.get("Motive");
			if(motiveTag != null) {
				motive = ((NbtTagString) motiveTag).getData();
				if(!motive.contains(":"))
					motive = "minecraft:" + motive;
				variant = getVariant(motive);
			}else {
				motiveTag = properties.get("variant");
				if(motiveTag != null) {
					if(motiveTag instanceof NbtTagString) {
						motive = ((NbtTagString) motiveTag).getData();
						if(!motive.contains(":"))
							motive = "minecraft:" + motive;
						variant = getVariant(motive);
					}else if(motiveTag instanceof NbtTagCompound) {
						motive = null;
						int width = 1;
						int height = 1;
						NbtTagCompound dataTag = (NbtTagCompound) motiveTag;
						
						NbtTagString assetIdTag = (NbtTagString) dataTag.get("asset_id");
						if(assetIdTag != null)
							motive = assetIdTag.getData();
						
						NbtTag widthTag = dataTag.get("width");
						if(widthTag != null)
							width = widthTag.asInt();
						
						NbtTag heightTag = dataTag.get("height");
						if(heightTag != null)
							height = heightTag.asInt();
						
						if(motive != null) {
							variant = new PaintingVariant(motive, motive, width, height);
						}else {
							motive = "";
						}
					}
				}
			}
			
			if(variant == null) {
				World.handleError(new RuntimeException("No painting variant found for " + motive));
				return;
			}
			
			Model model = new Model("painting", null, false);
			model.addTexture("#back", "minecraft:painting/back");
			model.addTexture("#front", variant.getAssetPath());
			
			float sizeX = variant.getWidth();
			float sizeY = variant.getHeight();
			float sizeZ = 1f;
			float offsetX = getOffset((int) sizeX);
			float offsetY = getOffset((int) sizeY);
			float offsetZ = 0f;
			sizeX *= 16f;
			sizeY *= 16f;
			offsetX *= 16f;
			offsetY *= 16f;
			
			float[] minMaxPoints = new float[] {offsetX, offsetY, offsetZ, offsetX + sizeX, offsetY + sizeY, offsetZ + sizeZ};
			
			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.SOUTH, "#front");

			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, Direction.NORTH, "#back");
			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 1.0f }, Direction.UP, "#back");
			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 16.0f, 1.0f }, Direction.DOWN, "#back");
			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 1.0f, 16.0f }, Direction.EAST, "#back");
			model.addFace(minMaxPoints, new float[] { 0.0f, 0.0f, 1.0f, 16.0f }, Direction.WEST, "#back");
			
			context.model.addModel(model);
		}
		
		private int getOffset(int width) {
			return -Math.max((width - 1) / 2, 0);
		}
		
		private static class Size{
			public float x;
			public float y;
			
			public Size(float x, float y) {
				this.x = x;
				this.y = y;
			}
		}
		
		private PaintingVariant getVariant(String motif) {
			PaintingVariant variant = ResourcePacks.getPaintingVariant(motif);
			if(variant != null)
				return variant;
			Size size = paintingSizes.getOrDefault(motif, null);
			if(size == null)
				return null;
			return new PaintingVariant(motif, motif, (int) size.x, (int) size.y);
		}
		
		private static HashMap<String, Size> paintingSizes = new HashMap<String, Size>();
		static {
			paintingSizes.put("minecraft:alban", new Size(1,1));
			paintingSizes.put("minecraft:aztec", new Size(1,1));
			paintingSizes.put("minecraft:aztec2", new Size(1,1));
			paintingSizes.put("minecraft:bomb", new Size(1,1));
			paintingSizes.put("minecraft:kebab", new Size(1,1));
			paintingSizes.put("minecraft:plant", new Size(1,1));
			paintingSizes.put("minecraft:wasteland", new Size(1,1));
			paintingSizes.put("minecraft:courbet", new Size(2,1));
			paintingSizes.put("minecraft:pool", new Size(2,1));
			paintingSizes.put("minecraft:sea", new Size(2,1));
			paintingSizes.put("minecraft:creebet", new Size(2,1));
			paintingSizes.put("minecraft:sunset", new Size(2,1));
			paintingSizes.put("minecraft:graham", new Size(1,2));
			paintingSizes.put("minecraft:wanderer", new Size(1,2));
			paintingSizes.put("minecraft:bust", new Size(2,2));
			paintingSizes.put("minecraft:match", new Size(2,2));
			paintingSizes.put("minecraft:skull_and_roses", new Size(2,2));
			paintingSizes.put("minecraft:stage", new Size(2,2));
			paintingSizes.put("minecraft:void", new Size(2,2));
			paintingSizes.put("minecraft:wither", new Size(2,2));
			paintingSizes.put("minecraft:fighters", new Size(4,2));
			paintingSizes.put("minecraft:donkey_kong", new Size(4,3));
			paintingSizes.put("minecraft:skeleton", new Size(4,3));
			paintingSizes.put("minecraft:burning_skull", new Size(4,4));
			paintingSizes.put("minecraft:pigscene", new Size(4,4));
			paintingSizes.put("minecraft:pointer", new Size(4,4));
		}

		
	}
	
	public static class BuiltInGeneratorSignText extends BuiltInGenerator {

		@Override
		public void eval(ExprContext context, Map<String, Expression> arguments) {
			ExprValue properties = null;
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
		
		private static JsonElement nbtToJson(NbtTag tag) {
			int size = 0;
			switch(tag.getId()) {
			case NbtTagByteArray.ID:
				return new JsonPrimitive(0);
			case NbtTagByte.ID:
				return new JsonPrimitive(tag.asByte());
			case NbtTagCompound.ID:
				JsonObject object = new JsonObject();
				NbtTagCompound nbtObject = (NbtTagCompound) tag;
				size = nbtObject.getSize();
				for(int i = 0; i < size; ++i) {
					NbtTag childTag = nbtObject.get(i);
					if(childTag == null)
						continue;
					JsonElement childEl = nbtToJson(childTag);
					if(childEl == null)
						continue;
					object.add(childTag.getName(), childEl);
				}
				return object;
			case NbtTagDouble.ID:
				return new JsonPrimitive(tag.asDouble());
			case NbtTagEnd.ID:
				return new JsonPrimitive(0);
			case NbtTagFloat.ID:
				return new JsonPrimitive(tag.asFloat());
			case NbtTagInt.ID:
				return new JsonPrimitive(tag.asInt());
			case NbtTagIntArray.ID:
				return new JsonPrimitive(0);
			case NbtTagList.ID:
				JsonArray list = new JsonArray();
				NbtTagList nbtList = (NbtTagList) tag;
				size = nbtList.getSize();
				for(int i = 0; i < size; ++i) {
					NbtTag childTag = nbtList.get(i);
					if(childTag == null)
						continue;
					JsonElement childEl = nbtToJson(childTag);
					if(childEl == null)
						continue;
					list.add(childEl);
				}
				return list;
			case NbtTagLong.ID:
				return new JsonPrimitive(tag.asLong());
			case NbtTagLongArray.ID:
				return new JsonPrimitive(0);
			case NbtTagShort.ID:
				return new JsonPrimitive(tag.asShort());
			case NbtTagString.ID:
				return new JsonPrimitive(tag.asString());
			default:
				return null;
			}
		}

		public static void handleText(ExprValue properties, float textOffsetX, float textOffsetY,
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
			ExprValue colorTag = properties.member("Color");
			if (colorTag != null && !colorTag.isNull()) {
				frontColor = COLORS.getOrDefault(colorTag.asString(), frontColor);
				backColor = frontColor;
				frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), frontGlowColor);
				backGlowColor = frontGlowColor;
			}
			ExprValue glowingTextTag = properties.member("GlowingText");
			if (glowingTextTag != null && !glowingTextTag.isNull()) {
				frontGlowing = glowingTextTag.asInt() > 0;
				backGlowing = frontGlowing;
			}

			// Java Format
			ExprValue text1 = properties.member("Text1");
			if (text1 != null && !text1.isNull()) {
				ExprValue text2 = properties.member("Text2");
				ExprValue text3 = properties.member("Text3");
				ExprValue text4 = properties.member("Text4");

				if (text1 != null && !text1.isNull())
					frontText1 = new JsonPrimitive(text1.asString());
				if (text2 != null && !text2.isNull())
					frontText2 = new JsonPrimitive(text2.asString());
				if (text3 != null && !text3.isNull())
					frontText3 = new JsonPrimitive(text3.asString());
				if (text4 != null && !text4.isNull())
					frontText4 = new JsonPrimitive(text4.asString());
			}
			ExprValue frontText = properties.member("front_text");
			if (frontText != null && !frontText.isNull()) {
				colorTag = frontText.member("color");
				if (colorTag != null && !colorTag.isNull()) {
					frontColor = COLORS.getOrDefault(colorTag.asString(), frontColor);
					frontGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), frontGlowColor);
				}

				glowingTextTag = frontText.member("has_glowing_text");
				if (glowingTextTag != null && !glowingTextTag.isNull())
					frontGlowing = glowingTextTag.asInt() > 0;

					ExprValue messages = frontText.member("messages");
				if (messages != null && !messages.isNull()) {
					int i = 0;
					for (ExprValue tag : messages.getChildren().values()) {
						try {
							JsonElement el = null;
							if(tag.getImpl() instanceof ExprValueNbtCompound) {
								el = nbtToJson(((ExprValueNbtCompound) tag.getImpl()).getNbt());
							}else {
								String text = tag.asString();
								if(text.length() == 0) {
									el = new JsonPrimitive(text);
								}else {
									if(text.charAt(0) == '{' || text.charAt(0) == '[') {
										el = JsonParser.parseString(tag.asString());
									}else {
										el = new JsonPrimitive(text);
									}
								}
							}
							if (i == 0)
								frontText1 = el;
							else if (i == 1)
								frontText2 = el;
							else if (i == 2)
								frontText3 = el;
							else if (i == 3)
								frontText4 = el;
						} catch (Exception ex) {
							System.err.println("Sign Text: " + tag.asString());
							ex.printStackTrace();
						}
						i++;
					}
				}
			}
			ExprValue backText = properties.member("back_text");
			if (backText != null && !backText.isNull()) {
				colorTag = backText.member("color");
				if (colorTag != null && !colorTag.isNull()) {
					backColor = COLORS.getOrDefault(colorTag.asString(), backColor);
					backGlowColor = GLOW_COLORS.getOrDefault(colorTag.asString(), backGlowColor);
				}

				glowingTextTag = backText.member("has_glowing_text");
				if (glowingTextTag != null && !glowingTextTag.isNull())
					backGlowing = glowingTextTag.asInt() > 0;

					ExprValue messages = backText.member("messages");
				if (messages != null && !messages.isNull()) {
					int i = 0;
					for (ExprValue tag : messages.getChildren().values()) {
						try {
							JsonElement el = null;
							if(tag.getImpl() instanceof ExprValueNbtCompound) {
								el = nbtToJson(((ExprValueNbtCompound) tag.getImpl()).getNbt());
							}else {
								String text = tag.asString();
								if(text.length() == 0) {
									el = new JsonPrimitive(text);
								}else {
									if(text.charAt(0) == '{' || text.charAt(0) == '[') {
										el = JsonParser.parseString(tag.asString());
									}else {
										el = new JsonPrimitive(text);
									}
								}
							}
							if (i == 0)
								backText1 = el;
							else if (i == 1)
								backText2 = el;
							else if (i == 2)
								backText3 = el;
							else if (i == 3)
								backText4 = el;
						} catch (Exception ex) {
							System.err.println("Sign Text: " + tag.asString());
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
				
				ExprValue text = frontText.member("Text");
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
				
				ExprValue text = backText.member("Text");
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
		public void eval(ExprContext context, Map<String, Expression> arguments) {
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
				ExprValue val = arguments.get("color").eval(context);
				defaultColor = parseColor(val);
			}

			if (arguments.containsKey("glowColor")) {
				ExprValue val = arguments.get("glowColor").eval(context);
				glowColor = parseColor(val);
			}

			if (arguments.containsKey("glowing"))
				glowing = arguments.get("glowing").eval(context).asBool();

			addText(text, font, textOffsetX, textOffsetY, textOffsetZ, textScale, rotY, defaultColor, glowColor,
					glowing, context.model);
		}

		private Color parseColor(ExprValue val) {
			if (val.getImpl() instanceof ExprValueDict) {
				ExprValueDict valD = (ExprValueDict) val.getImpl();
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
