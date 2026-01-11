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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Reference;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class ItemHandlerJavaEdition extends ItemHandler{

	private static abstract class ItemModel{
		
		public abstract void setupModel(String name, NbtTagCompound data, String displayContext, Model model);
		
	}
	
	private static Color parseColor(JsonElement valueEl, boolean asIntArray) {
		if(valueEl == null) {
			return new Color(1f, 1f, 1f);
		}else if(valueEl.isJsonPrimitive()){
			int rgb = valueEl.getAsInt();
			return new Color(rgb);
		}else if(valueEl.isJsonArray()) {
			if(asIntArray) {
				int ir = 255;
				int ig = 255;
				int ib = 255;
				JsonArray valueArray = valueEl.getAsJsonArray();
				if(valueArray.size() > 0)
					ir = valueArray.get(0).getAsInt();
				if(valueArray.size() > 1)
					ig = valueArray.get(1).getAsInt();
				if(valueArray.size() > 2)
					ib = valueArray.get(2).getAsInt();
				
				ir = Math.min(Math.max(ir, 0), 255);
				ig = Math.min(Math.max(ig, 0), 255);
				ib = Math.min(Math.max(ib, 0), 255);
				return new Color(ir << 16 | ig << 8 | ib);
			}else {
				float r = 1f;
				float g = 1f;
				float b = 1f;
				JsonArray valueArray = valueEl.getAsJsonArray();
				if(valueArray.size() > 0)
					r = valueArray.get(0).getAsFloat();
				if(valueArray.size() > 1)
					g = valueArray.get(1).getAsFloat();
				if(valueArray.size() > 2)
					b = valueArray.get(2).getAsFloat();
				
				int ir = Math.min(Math.max((int) (r * 255f), 0), 255);
				int ig = Math.min(Math.max((int) (g * 255f), 0), 255);
				int ib = Math.min(Math.max((int) (b * 255f), 0), 255);
				return new Color(ir << 16 | ig << 8 | ib);
			}
		}else {
			return new Color(1f, 1f, 1f);
		}
	}
	
	private static Color parseColor(NbtTag valueEl, boolean asIntArray) {
		if(valueEl == null) {
			return new Color(1f, 1f, 1f);
		}else if(valueEl instanceof NbtTagInt){
			int rgb = valueEl.asInt();
			return new Color(rgb);
		}else if(valueEl instanceof NbtTagList) {
			if(asIntArray) {
				int ir = 255;
				int ig = 255;
				int ib = 255;
				NbtTagList valueArray = (NbtTagList) valueEl;
				if(valueArray.getSize() > 0)
					ir = valueArray.get(0).asInt();
				if(valueArray.getSize() > 1)
					ig = valueArray.get(1).asInt();
				if(valueArray.getSize() > 2)
					ib = valueArray.get(2).asInt();
				
				ir = Math.min(Math.max(ir, 0), 255);
				ig = Math.min(Math.max(ig, 0), 255);
				ib = Math.min(Math.max(ib, 0), 255);
				return new Color(ir << 16 | ig << 8 | ib);
			}else {
				float r = 1f;
				float g = 1f;
				float b = 1f;
				NbtTagList valueArray = (NbtTagList) valueEl;
				if(valueArray.getSize() > 0)
					r = valueArray.get(0).asFloat();
				if(valueArray.getSize() > 1)
					g = valueArray.get(1).asFloat();
				if(valueArray.getSize() > 2)
					b = valueArray.get(2).asFloat();
				
				int ir = Math.min(Math.max((int) (r * 255f), 0), 255);
				int ig = Math.min(Math.max((int) (g * 255f), 0), 255);
				int ib = Math.min(Math.max((int) (b * 255f), 0), 255);
				return new Color(ir << 16 | ig << 8 | ib);
			}
		}else {
			return new Color(1f, 1f, 1f);
		}
	}
	
	private static abstract class TintSource{
		
		public abstract Color getTint(NbtTagCompound data);
		
	}
	
	private static class TintSourceConstant extends TintSource{
		
		public static final String TYPE = "minecraft:constant";
		
		private Color color;
		
		public TintSourceConstant(JsonObject data) {
			color = parseColor(data.get("value"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			return color;
		}
		
	}
	
	private static class TintSourceDye extends TintSource{
		
		public static final String TYPE = "minecraft:dye";
		
		private Color defaultColor;
		
		public TintSourceDye(JsonObject data) {
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound)) {
				NbtTag tags = data.get("tag");
				if(tags == null || !(tags instanceof NbtTagCompound))
					return defaultColor;
				NbtTag display = ((NbtTagCompound) tags).get("display");
				if(display == null || !(display instanceof NbtTagCompound))
					return defaultColor;
				NbtTag color = ((NbtTagCompound) display).get("color");
				if(color == null)
					return defaultColor;
				return parseColor(color, true);
			}
			NbtTag dyedColorComponent = ((NbtTagCompound) components).get("minecraft:dyed_color");
			if(dyedColorComponent != null) {
				if(dyedColorComponent instanceof NbtTagCompound)
					return parseColor(((NbtTagCompound) dyedColorComponent).get("rgb"), true);
				else
					return parseColor(dyedColorComponent, true);
			}
			return defaultColor;
		}
		
	}
	
	private static class TintSourceFirework extends TintSource{
		
		public static final String TYPE = "minecraft:firework";
		
		private Color defaultColor;
		
		public TintSourceFirework(JsonObject data) {
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return defaultColor;
			NbtTag fireworkExplosion = ((NbtTagCompound) components).get("minecraft:firework_explosion");
			if(fireworkExplosion != null && fireworkExplosion instanceof NbtTagCompound) {
				NbtTag colors = ((NbtTagCompound) fireworkExplosion).get("colors");
				if(colors != null && colors instanceof NbtTagList) {
					Color color = new Color(0f, 0f, 0f);
					int size = ((NbtTagList) colors).getSize();
					for(int i = 0; i < size; ++i) {
						NbtTag el = ((NbtTagList) colors).get(i);
						color.addWeighted(parseColor(el, false), 1f);
					}
					color.mult(1f / ((float) size));
					color.setA(1f);
					return color;
				}
			}
			return defaultColor;
		}
		
	}
	
	private static class TintSourceGrass extends TintSource{
		
		public static final String TYPE = "minecraft:grass";
		
		public float temperature;
		public float downfall;
		
		public TintSourceGrass(JsonObject data) {
			temperature = 0.5f;
			downfall = 0.5f;
			
			JsonElement downfallObj = data.get("downfall");
			if(downfallObj != null)
				downfall = downfallObj.getAsFloat();
			
			JsonElement temperatureObj = data.get("temperature");
			if(temperatureObj != null)
				temperature = temperatureObj.getAsFloat();
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			float tintX = Math.max(Math.min(temperature, 1.0f), 0.0f);
			float tintY = 1.0f - (Math.max(Math.min(downfall, 1.0f), 0.0f) * tintX);
			tintX = 1.0f - tintX;
			
			BufferedImage grassColorMap = Tints.getColorMap("minecraft:grass");
			if(grassColorMap != null) {
				int tintXI = (int) (tintX * ((float) (grassColorMap.getWidth()-1)));
				int tintYI = (int) (tintY * ((float) (grassColorMap.getHeight()-1)));
				
				return new Color(grassColorMap.getRGB(tintXI, tintYI));
			}
			return new Color(1f, 1f, 1f);
		}
		
	}
	
	private static class TintSourceMapColor extends TintSource{
		
		public static final String TYPE = "minecraft:map_color";
		
		private Color defaultColor;
		
		public TintSourceMapColor(JsonObject data) {
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return defaultColor;
			NbtTag mapColorComponent = ((NbtTagCompound) components).get("minecraft:map_color");
			if(mapColorComponent != null) {
				if(mapColorComponent instanceof NbtTagCompound)
					return parseColor(((NbtTagCompound) mapColorComponent).get("rgb"), true);
				else
					return parseColor(mapColorComponent, true);
			}
			return defaultColor;
		}
		
	}
	
	private static class TintSourcePotion extends TintSource{
		
		public static final String TYPE = "minecraft:potion";
		
		private Color defaultColor;
		
		public TintSourcePotion(JsonObject data) {
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return defaultColor;
			NbtTag potionContents = ((NbtTagCompound) components).get("minecraft:potion_contents");
			if(potionContents != null) {
				if(potionContents instanceof NbtTagCompound) {
					NbtTag customColor = ((NbtTagCompound) potionContents).get("custom_color");
					if(customColor != null) {
						return parseColor(customColor, false);
					}
				}
			}
			return defaultColor;
		}
		
	}
	
	private static class TintSourceTeam extends TintSource{
		
		public static final String TYPE = "minecraft:team";
		
		private Color defaultColor;
		
		public TintSourceTeam(JsonObject data) {
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			return defaultColor;
		}
		
	}
	
	private static class TintSourceCustomModelData extends TintSource{
		
		public static final String TYPE = "minecraft:custom_model_data";
		
		private int index;
		private Color defaultColor;
		
		public TintSourceCustomModelData(JsonObject data) {
			index = 0;
			JsonElement indexEl = data.get("index");
			if(indexEl != null)
				index = indexEl.getAsInt();
			defaultColor = parseColor(data.get("default"), false);
		}
		
		@Override
		public Color getTint(NbtTagCompound data) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return defaultColor;
			NbtTag customModelData = ((NbtTagCompound) components).get("minecraft:custom_model_data");
			if(customModelData != null) {
				if(customModelData instanceof NbtTagCompound) {
					NbtTag colors = ((NbtTagCompound) customModelData).get("colors");
					if(colors != null) {
						if(colors instanceof NbtTagList) {
							if(index >= 0 && index < ((NbtTagList) colors).getSize()) {
								return parseColor(((NbtTagList) colors).get(index), false);
							}
						}
						return parseColor(colors, false);
					}
				}
			}
			return defaultColor;
		}
		
	}
	
	private static class ItemModelModel extends ItemModel{
		
		public static final String TYPE = "minecraft:model";
		
		private String model;
		private List<TintSource> tints;
		
		public ItemModelModel(JsonObject data) {
			model = null;
			tints = new ArrayList<TintSource>();
			JsonElement modelEl = data.get("model");
			if(modelEl != null)
				model = modelEl.getAsString();
			JsonArray tintsArray = data.getAsJsonArray("tints");
			if(tintsArray != null) {
				for(JsonElement el : tintsArray.asList()) {
					if(el.isJsonObject())
						tints.add(parseTintSource(el.getAsJsonObject()));
					else
						tints.add(null);
				}
			}
		}
		
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			if(this.model == null)
				return;
			int modelId = ModelRegistry.getIdForName(this.model, false);
			Model model2 = ModelRegistry.getModel(modelId);
			if(model2 == null)
				return;
			
			List<Color> tints2 = new ArrayList<Color>();
			for(TintSource tint : tints) {
				if(tint == null)
					tints2.add(new Color(1f, 1f, 1f, 1f));
				else
					tints2.add(tint.getTint(data));
			}
			
			if(model2.hasDisplayTransformation(displayContext)) {
				model2 = new Model(model2);
				model2.applyTransformation(displayContext);
			}
			
			model.addModel(model2, tints2);
		}
		
	}
	
	private static class ItemModelComposite extends ItemModel{
		
		public static final String TYPE = "minecraft:composite";
		
		private List<ItemModel> models;
		
		public ItemModelComposite(JsonObject data) {
			models = new ArrayList<ItemModel>();
			
			JsonArray modelsArray = data.getAsJsonArray("models");
			if(modelsArray != null) {
				for(JsonElement el : modelsArray.asList()) {
					if(el.isJsonObject())
						models.add(parseItemModel(el.getAsJsonObject()));
				}
			}
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			for(ItemModel model2 : models) {
				if(model2 == null)
					continue;
				model2.setupModel(name, data, displayContext, model);
			}
		}
		
	}
	
	private static abstract class Condition{
		
		public abstract boolean eval(NbtTagCompound data, String displayContext);
		
	}
	
	private static class ConditionBroken extends Condition{
		
		public static final String TYPE = "minecraft:broken";
		
		public ConditionBroken(JsonObject data) {}
		
		@Override
		public boolean eval(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return false;
			NbtTag maxDamageEl = ((NbtTagCompound) components).get("minecraft:max_damage");
			if(maxDamageEl == null)
				return false;
			int maxDamage = maxDamageEl.asInt();
			
			NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
			if(damageEl == null)
				return false;
			
			int damage = damageEl.asInt();
			
			return damage >= maxDamage;
		}
		
	}
	
	private static class ConditionComponent extends Condition{
		
		public static final String TYPE = "minecraft:component";
		
		private String predicate;
		private String value;
		
		public ConditionComponent(JsonObject data) {
			predicate = null;
			value = null;
			JsonElement predicateEl = data.get("predicate");
			if(predicateEl != null)
				predicate = predicateEl.getAsString();
			JsonElement valueEl = data.get("value");
			if(valueEl != null)
				value = valueEl.getAsString();
		}
		
		@Override
		public boolean eval(NbtTagCompound data, String displayContext) {
			if(predicate == null || value == null)
				return false;
			
			// TODO: Implement
			// No idea how to implement this.
			return false;
		}
		
	}
	
	private static class ConditionDamaged extends Condition{
		
		public static final String TYPE = "minecraft:damaged";
		
		public ConditionDamaged(JsonObject data) {}
		
		@Override
		public boolean eval(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return false;
			
			NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
			if(damageEl == null)
				return false;
			
			int damage = damageEl.asInt();
			
			return damage >= 1;
		}
		
	}
	
	private static class ConditionHasComponent extends Condition{
		
		public static final String TYPE = "minecraft:has_component";
		
		private String component;
		
		public ConditionHasComponent(JsonObject data) {
			component = null;
			JsonElement componentEl = data.get("component");
			if(componentEl != null)
				component = componentEl.getAsString();
		}
		
		@Override
		public boolean eval(NbtTagCompound data, String displayContext) {
			if(component == null)
				return false;
			
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return false;
			
			return ((NbtTagCompound) components).get(component) != null;
		}
		
	}
	
	private static class ConditionCustomModelData extends Condition{
		
		public static final String TYPE = "minecraft:custom_model_data";
		
		private int index;
		
		public ConditionCustomModelData(JsonObject data) {
			index = 0;
			JsonElement indexEl = data.get("index");
			if(indexEl != null)
				index = indexEl.getAsInt();
		}
		
		@Override
		public boolean eval(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTag))
				return false;
			
			NbtTag customModelData = ((NbtTagCompound) components).get("minecraft:custom_model_data");
			if(customModelData == null || !(customModelData instanceof NbtTagCompound))
				return false;
			
			NbtTag flags = ((NbtTagCompound) customModelData).get("flags");
			if(flags == null || !(flags instanceof NbtTagList))
				return false;
			
			if(index < 0 || index >= ((NbtTagList) flags).getSize())
				return false;
			
			return ((NbtTagList) flags).get(index).asBoolean();
		}
		
	}
	
	private static class ItemModelCondition extends ItemModel{
		
		public static final String TYPE = "minecraft:condition";
		
		private Condition condition;
		private ItemModel onTrue;
		private ItemModel onFalse;
		
		public ItemModelCondition(JsonObject data) {
			condition = parseCondition(data);
			onTrue = parseItemModel(data.getAsJsonObject("on_true"));
			onFalse = parseItemModel(data.getAsJsonObject("on_false"));
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			if(condition == null) {
				if(onFalse != null)
					onFalse.setupModel(name, data, displayContext, model);
			}else {
				if(condition.eval(data, displayContext)) {
					if(onTrue != null)
						onTrue.setupModel(name, data, displayContext, model);
				}else {
					if(onFalse != null)
						onFalse.setupModel(name, data, displayContext, model);
				}
			}
		}
		
	}
	
	private static abstract class Property{
		
		public abstract String getValue(NbtTagCompound data, String displayContext);
		
	}
	
	private static class PropertyBlockState extends Property{
		
		public static final String TYPE = "minecraft:block_state";
		
		private String property;
		
		public PropertyBlockState(JsonObject data) {
			property = null;
			JsonElement propertyEl = data.get("block_state_property");
			if(propertyEl != null)
				property = propertyEl.getAsString();
		}
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			if(property == null)
				return null;
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return null;
			NbtTag blockState = ((NbtTagCompound) components).get("minecraft:block_state");
			if(blockState == null || !(blockState instanceof NbtTagCompound))
				return null;
			NbtTag propertyEl = ((NbtTagCompound) blockState).get(property);
			if(propertyEl == null)
				return null;
			return propertyEl.asString();
		}
		
	}
	
	private static class PropertyChargeType extends Property{
		
		public static final String TYPE = "minecraft:charge_type";
		
		public PropertyChargeType(JsonObject data) {}
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return "none";
			NbtTag chargedProjectiles = ((NbtTagCompound) components).get("minecraft:charged_projectiles");
			if(chargedProjectiles == null || !(chargedProjectiles instanceof NbtTagList))
				return "none";
			int size = ((NbtTagList) chargedProjectiles).getSize();
			if(size <= 0)
				return "none";
			for(int i = 0; i < size; ++i) {
				NbtTag el = ((NbtTagList) chargedProjectiles).get(i);
				if(!(el instanceof NbtTagCompound))
					continue;
				NbtTag idEl = ((NbtTagCompound) el).get("id");
				if(idEl == null)
					continue;
				String id = idEl.asString();
				if(id.equals("minecraft:firework_rocket"))
					return "rocket";
			}
			return "arrow";
		}
		
	}
	
	private static class PropertyComponent extends Property{
		
		public static final String TYPE = "minecraft:component";
		
		@SuppressWarnings("unused")
		private String component;
		
		public PropertyComponent(JsonObject data) {
			component = null;
			JsonElement componentEl = data.get("component");
			if(componentEl != null)
				component = componentEl.getAsString();
		}
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			// TODO: Implement
			// Not sure how to get the value of a component.
			return null;
		}
		
	}
	
	private static class PropertyContextEntityType extends Property{
		
		public static final String TYPE = "minecraft:context_entity_type";
		
		public PropertyContextEntityType(JsonObject data) {}
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			// TODO: Implement
			return null;
		}
		
	}
	
	private static class PropertyDisplayContext extends Property{
		
		public static final String TYPE = "minecraft:display_context";
		
		public PropertyDisplayContext(JsonObject data) {};
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			return displayContext;
		}
		
	}
	
	private static class PropertyMainHand extends Property{
		
		public static final String TYPE = "minecraft:main_hand";
		
		public PropertyMainHand(JsonObject data) {};
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			return "right";
		}
		
	}
	
	private static class PropertyTrimMaterial extends Property{
		
		public static final String TYPE = "minecraft:trim_material";
		
		public PropertyTrimMaterial(JsonObject data) {};
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return null;
			NbtTag trim = ((NbtTagCompound) components).get("minecraft:trim");
			if(trim == null || !(trim instanceof NbtTagCompound))
				return null;
			NbtTag material = ((NbtTagCompound) trim).get("material");
			if(material == null)
				return null;
			return material.asString();
		}
		
	}
	
	private static class PropertyCustomModelData extends Property{
		
		public static final String TYPE = "minecraft:custom_model_data";
		
		private int index;
		
		public PropertyCustomModelData(JsonObject data) {
			index = 0;
			JsonElement indexEl = data.get("index");
			if(indexEl != null)
				index = indexEl.getAsInt();
		}
		
		@Override
		public String getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTag))
				return null;
			
			NbtTag customModelData = ((NbtTagCompound) components).get("minecraft:custom_model_data");
			if(customModelData == null || !(customModelData instanceof NbtTagCompound))
				return null;
			
			NbtTag stringsArray = ((NbtTagCompound) customModelData).get("strings");
			if(stringsArray == null || !(stringsArray instanceof NbtTagList))
				return null;
			
			if(index < 0 || index >= ((NbtTagList) stringsArray).getSize())
				return null;
			
			return ((NbtTagList) stringsArray).get(index).asString();
		}
		
	}
	
	private static class SelectCase{
		
		private List<String> values;
		private ItemModel model;
		
		public SelectCase(JsonObject data) {
			values = new ArrayList<String>();
			model = null;
			JsonElement whenEl = data.get("when");
			if(whenEl != null) {
				if(whenEl.isJsonPrimitive())
					values.add(whenEl.getAsString());
				else if(whenEl.isJsonArray()) {
					for(JsonElement el : whenEl.getAsJsonArray().asList()) {
						if(el.isJsonPrimitive())
							values.add(el.getAsString());
					}
				}
			}
			JsonObject modelObj = data.getAsJsonObject("model");
			if(modelObj != null)
				model = parseItemModel(modelObj);
		}
		
		public boolean matches(String value) {
			return values.contains(value);
		}
		
		public ItemModel getModel() {
			return model;
		}
		
	}
	
	private static class ItemModelSelect extends ItemModel{
		
		public static final String TYPE = "minecraft:select";
		
		private Property property;
		private List<SelectCase> cases;
		private ItemModel fallback;
		
		public ItemModelSelect(JsonObject data) {
			property = parseProperty(data);
			cases = new ArrayList<SelectCase>();
			JsonArray casesArray = data.getAsJsonArray("cases");
			if(casesArray != null) {
				for(JsonElement el : casesArray.asList()) {
					if(el.isJsonObject()) {
						cases.add(new SelectCase(el.getAsJsonObject()));
					}
				}
			}
			fallback = parseItemModel(data.getAsJsonObject("fallback"));
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			if(property == null) {
				if(fallback != null)
					fallback.setupModel(name, data, displayContext, model);
				return;
			}
			String value = property.getValue(data, displayContext);
			for(SelectCase selectCase : cases) {
				if(selectCase == null)
					continue;
				if(selectCase.matches(value)) {
					if(selectCase.getModel() != null)
						selectCase.getModel().setupModel(name, data, displayContext, model);
					return;
				}
			}
			if(fallback != null)
				fallback.setupModel(name, data, displayContext, model);
		}
		
	}
	
	private static abstract class NumericProperty{
		
		public abstract float getValue(NbtTagCompound data, String displayContext);
		
	}
	
	private static class NumericPropertyBundleFullness extends NumericProperty{
		
		public static final String TYPE = "minecraft:bundle/fullness";
		
		public NumericPropertyBundleFullness(JsonObject data) {}
		
		@Override
		public float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return 0f;
			NbtTag bundleContents = ((NbtTagCompound)components).get("minecraft:bundle_contents");
			if(bundleContents == null)
				return 0f;
			if(!(bundleContents instanceof NbtTagList))
				return 0f;
			float weight = 0f;
			
			int num = ((NbtTagList) bundleContents).getSize();
			for(int i = 0; i < num; ++i) {
				NbtTag el = ((NbtTagList) bundleContents).get(i);
				if(el instanceof NbtTagCompound) {
					NbtTag countEl = ((NbtTagCompound) el).get("count");
					if(countEl != null) {
						weight += countEl.asFloat();
					}
				}
			}
			
			return weight;
		}
		
	}
	
	private static class NumericPropertyCount extends NumericProperty{
		
		public static final String TYPE = "minecraft:count";
		
		private boolean normalize;
		
		public NumericPropertyCount(JsonObject data) {
			normalize = true;
			JsonElement el = data.get("normalize");
			if(el != null)
				normalize = el.getAsBoolean();
		}
		
		@Override
		public float getValue(NbtTagCompound data, String displayContext) {
			int count = 1;
			int maxStackSize = 1;
			if(normalize) {
				NbtTag components = data.get("components");
				if(components != null && components instanceof NbtTagCompound) {
					NbtTag maxStackSizeEl = ((NbtTagCompound) components).get("minecraft:max_stack_size");
					if(maxStackSizeEl != null) {
						maxStackSize = maxStackSizeEl.asInt();
					}
				}
			}
			NbtTag countEl = data.get("count");
			if(countEl != null)
				count = countEl.asInt();
			
			return ((float) count) / ((float) maxStackSize);
		}
		
	}
	
	private static class NumericPropertyDamage extends NumericProperty{
		
		public static final String TYPE = "minecraft:damage";
		
		private boolean normalize;
		
		public NumericPropertyDamage(JsonObject data) {
			normalize = true;
			JsonElement el = data.get("normalize");
			if(el != null)
				normalize = el.getAsBoolean();
		}
		
		@Override
		public float getValue(NbtTagCompound data, String displayContext) {
			int damage = 0;
			int maxDamage = 1;
			NbtTag components = data.get("components");
			if(components != null && components instanceof NbtTagCompound) {
				if(normalize) {
					NbtTag maxDamageEl = ((NbtTagCompound) components).get("minecraft:max_damage");
					if(maxDamageEl != null) {
						maxDamage = maxDamageEl.asInt();
					}
				}
				NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
				if(damageEl != null) {
					damage = damageEl.asInt();
				}
			}
			
			return ((float) damage) / ((float) maxDamage);
		}
		
	}
	
	private static class NumericPropertyCustomModelData extends NumericProperty{
		
		public static final String TYPE = "minecraft:custom_model_data";
		
		private int index;
		
		public NumericPropertyCustomModelData(JsonObject data) {
			index = 0;
			JsonElement indexEl = data.get("index");
			if(indexEl != null)
				index = indexEl.getAsInt();
		}
		
		@Override
		public float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound)) {
				NbtTag tags = data.get("tag");
				if(tags == null || !(tags instanceof NbtTagCompound))
					return Float.NaN;
				NbtTag customModelData = ((NbtTagCompound)components).get("CustomModelData");
				if(customModelData == null || !(customModelData instanceof NbtTagInt))
					return Float.NaN;
				return ((NbtTagInt) customModelData).asFloat();
			}
			
			NbtTag customModelData = ((NbtTagCompound)components).get("minecraft:custom_model_data");
			if(customModelData == null)
				return Float.NaN;
			if(customModelData instanceof NbtTagInt)
				return customModelData.asFloat();
			if(!(customModelData instanceof NbtTagCompound))
				return Float.NaN;
			
			NbtTag floatsArray = ((NbtTagCompound) customModelData).get("floats");
			if(floatsArray == null || !(floatsArray instanceof NbtTagList))
				return Float.NaN;
			
			if(index < 0 || index >= ((NbtTagList)floatsArray).getSize())
				return Float.NaN;
			
			return ((NbtTagList) floatsArray).get(index).asFloat();
		}
		
	}
	
	private static class NumericSelectCase{
		
		private float threshold;
		private ItemModel model;
		
		public NumericSelectCase(JsonObject data) {
			threshold = Float.MAX_VALUE;
			model = null;
			JsonElement thresholdEl = data.get("threshold");
			if(thresholdEl != null) {
				if(thresholdEl.isJsonPrimitive())
					threshold = thresholdEl.getAsFloat();
			}
			JsonObject modelObj = data.getAsJsonObject("model");
			if(modelObj != null)
				model = parseItemModel(modelObj);
		}
		
		public boolean matches(float value) {
			return value >= threshold;
		}
		
		public ItemModel getModel() {
			return model;
		}
		
	}
	
	private static class ItemModelRangeDispatch extends ItemModel{
		
		public static final String TYPE = "minecraft:range_dispatch";
		
		private NumericProperty property;
		private float scale;
		private List<NumericSelectCase> cases;
		private ItemModel fallback;
		
		public ItemModelRangeDispatch(JsonObject data) {
			property = parseNumericProperty(data);
			scale = 1f;
			JsonElement scaleEl = data.get("scale");
			if(scaleEl != null)
				if(scaleEl.isJsonPrimitive())
					scale = scaleEl.getAsFloat();
			cases = new ArrayList<NumericSelectCase>();
			JsonArray casesArray = data.getAsJsonArray("entries");
			if(casesArray != null) {
				for(JsonElement el : casesArray.asList()) {
					if(el.isJsonObject()) {
						cases.add(new NumericSelectCase(el.getAsJsonObject()));
					}
				}
			}
			fallback = parseItemModel(data.getAsJsonObject("fallback"));
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			if(property == null) {
				if(fallback != null)
					fallback.setupModel(name, data, displayContext, model);
				return;
			}
			float value = property.getValue(data, displayContext);
			if(!Float.isNaN(value)) {
				value *= scale;
				NumericSelectCase currentCase = null;
				for(NumericSelectCase selectCase : cases) {
					if(selectCase == null)
						continue;
					if(selectCase.matches(value))
						currentCase = selectCase;
					else
						break;
				}
				if(currentCase != null) {
					if(currentCase.getModel() != null)
						currentCase.getModel().setupModel(name, data, displayContext, model);
					return;
				}
			}
			if(fallback != null)
				fallback.setupModel(name, data, displayContext, model);
		}
		
	}
	
	private static abstract class SpecialModel{
		
		public abstract void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model);
		
	}
	
	private static abstract class SpecialModelBlock extends SpecialModel{
		
		protected String blockName = null;
		protected NbtTagCompound properties = null;
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model) {
			if(blockName == null)
				return;
			
			if(properties != null) {
				NbtTagCompound data2 = (NbtTagCompound) data.copy();
				data2.addAllElements(properties);
				data = data2;
			}
			
			Reference<char[]> charBuffer = new Reference<char[]>();
			int blockId = BlockRegistry.getIdForName(blockName, data, 0, charBuffer);
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, 0, 0, 0);
			List<Model> models = new ArrayList<Model>();
			state.getModels(0, 0, 0, models);
			for(Model model2 : models)
				model.addModel(model2);
			
			// Make sure to free the allocated NbtTags
			if(properties != null)
				data.free();
		}
		
	}
	
	private static class SpecialModelBanner extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:banner";
		
		public SpecialModelBanner(JsonObject data) {
			blockName = "minecraft:banner";
			
			JsonElement colorEl = data.get("color");
			if(colorEl != null) {
				NbtTagCompound componentsTag = NbtTagCompound.newNonPooledInstance("components");
				NbtTagString baseColorTag = NbtTagString.newNonPooledInstance("minecraft:base_color", colorEl.getAsString());
				componentsTag.addElement(baseColorTag);
				properties = NbtTagCompound.newNonPooledInstance("properties");
				properties.addElement(componentsTag);
			}
		}
		
	}
	
	private static class SpecialModelBed extends SpecialModel{
		
		public static final String TYPE = "minecraft:bed";
		
		public SpecialModelBed(JsonObject data) {
			
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model) {
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelChest extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:chest";
		
		public SpecialModelChest(JsonObject data) {
			blockName = "minecraft:chest";
			
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelConduit extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:conduit";
		
		public SpecialModelConduit(JsonObject data) {
			blockName = "minecraft:conduit";
		}
		
	}
	
	private static class SpecialModelDecoratedPot extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:decorated_pot";
		
		public SpecialModelDecoratedPot(JsonObject data) {
			blockName = "minecraft:decorated_pot";
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelHead extends SpecialModel{
		
		public static final String TYPE = "minecraft:head";
		
		private String kind;
		private String texture;
		
		public SpecialModelHead(JsonObject data) {
			kind = null;
			texture = null;
			JsonElement kindEl = data.get("kind");
			if(kindEl != null)
				kind = kindEl.getAsString();
			JsonElement textureEl = data.get("texture");
			if(textureEl != null)
				texture = textureEl.getAsString();
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model) {
			if(kind == null)
				return;
			
			String blockName = "minecraft:" + kind + "_head";
			NbtTagCompound properties = NbtTagCompound.newInstance("properties");
			if(texture != null) {
				NbtTagString textureTag = NbtTagString.newInstance("texture", texture);
				properties.addElement(textureTag);
			}
			if(kind.equals("player") && texture == null) {
				// Check if the profile component is here, if so we want to put in our own texture.
				NbtTag components = data.get("components");
				if(components != null && components instanceof NbtTagCompound) {
					NbtTag profileTag = ((NbtTagCompound) components).get("minecraft:profile");
					if(profileTag != null) {
						NbtTag profileTag2 = profileTag.copy();
						profileTag2.setName("profile");
						properties.addElement(profileTag2);
					}
				}
			}else if(kind.equals("skeleton") || kind.equals("wither_skeleton")) {
				blockName = "minecraft:" + kind + "_skull";
			}
			
			Reference<char[]> charBuffer = new Reference<char[]>();
			int blockId = BlockRegistry.getIdForName(blockName, properties, 0, charBuffer);
			BakedBlockState state = BlockStateRegistry.getBakedStateForBlock(blockId, 0, 0, 0);
			List<Model> models = new ArrayList<Model>();
			state.getModels(0, 0, 0, models);
			for(Model model2 : models)
				model.addModel(model2);
			
			// Make sure to free the allocated nbt tags.
			properties.free();
		}
		
	}
	
	private static class SpecialModelShield extends SpecialModel{
		
		public static final String TYPE = "minecraft:shield";
		
		public SpecialModelShield(JsonObject data) {
			// TODO: Implement
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model) {
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelShulkerBox extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:shulker_box";
		
		public SpecialModelShulkerBox(JsonObject data) {
			blockName = "minecraft:shulker_box";
			
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelStandingSign extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:standing_sign";
		
		public SpecialModelStandingSign(JsonObject data) {
			blockName = "minecraft:sign";
			
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelHangingSign extends SpecialModelBlock{
		
		public static final String TYPE = "minecraft:hanging_sign";
		
		public SpecialModelHangingSign(JsonObject data) {
			blockName = "minecraft:hanging_sign";
			
			// TODO: Implement
		}
		
	}
	
	private static class SpecialModelTrident extends SpecialModel{
		
		public static final String TYPE = "minecraft:trident";
		
		public SpecialModelTrident(JsonObject data) {
			// TODO: Implement
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model baseModel, Model model) {
			// TODO: Implement			
		}
		
	}
	
	private static class ItemModelSpecial extends ItemModel{
		
		public static final String TYPE = "minecraft:special";
		
		private SpecialModel specialModel;
		private String baseModel;
		
		public ItemModelSpecial(JsonObject data) {
			specialModel = null;
			JsonObject modelObj = data.getAsJsonObject("model");
			if(modelObj != null)
				specialModel = parseSpecialModel(modelObj);
			baseModel = null;
			JsonElement baseEl = data.get("base");
			if(baseEl != null)
				if(baseEl.isJsonPrimitive())
					baseModel = baseEl.getAsString();
		}
		
		@Override
		public void setupModel(String name, NbtTagCompound data, String displayContext, Model model) {
			Model baseModel2 = null;
			if(baseModel != null) {
				int baseModelId = ModelRegistry.getIdForName(baseModel, false);
				baseModel2 = ModelRegistry.getModel(baseModelId);
			}
			if(specialModel == null)
				return;
			specialModel.setupModel(name, data, displayContext, baseModel2, model);
		}
		
	}
	
	private static ItemModel parseItemModel(JsonObject data) {
		if(data == null)
			return null;
		JsonElement typeEl = data.get("type");
		if(typeEl == null)
			return null;
		String typeStr = typeEl.getAsString();
		if(!typeStr.contains(":"))
			typeStr = "minecraft:" + typeStr;
		if(typeStr.equals(ItemModelComposite.TYPE))
			return new ItemModelComposite(data);
		else if(typeStr.equals(ItemModelCondition.TYPE))
			return new ItemModelCondition(data);
		else if(typeStr.equals(ItemModelModel.TYPE))
			return new ItemModelModel(data);
		else if(typeStr.equals(ItemModelRangeDispatch.TYPE))
			return new ItemModelRangeDispatch(data);
		else if(typeStr.equals(ItemModelSelect.TYPE))
			return new ItemModelSelect(data);
		else if(typeStr.equals(ItemModelSpecial.TYPE))
			return new ItemModelSpecial(data);
		return null;
	}
	
	private static TintSource parseTintSource(JsonObject data) {
		if(data == null)
			return null;
		JsonElement typeEl = data.get("type");
		if(typeEl == null)
			return null;
		String typeStr = typeEl.getAsString();
		if(!typeStr.contains(":"))
			typeStr = "minecraft:" + typeStr;
		if(typeStr.equals(TintSourceConstant.TYPE))
			return new TintSourceConstant(data);
		else if(typeStr.equals(TintSourceDye.TYPE))
			return new TintSourceDye(data);
		else if(typeStr.equals(TintSourceFirework.TYPE))
			return new TintSourceFirework(data);
		else if(typeStr.equals(TintSourceGrass.TYPE))
			return new TintSourceGrass(data);
		else if(typeStr.equals(TintSourceMapColor.TYPE))
			return new TintSourceMapColor(data);
		else if(typeStr.equals(TintSourcePotion.TYPE))
			return new TintSourcePotion(data);
		else if(typeStr.equals(TintSourceTeam.TYPE))
			return new TintSourceTeam(data);
		else if(typeStr.equals(TintSourceCustomModelData.TYPE))
			return new TintSourceCustomModelData(data);
		return null;
	}
	
	private static Condition parseCondition(JsonObject data) {
		if(data == null)
			return null;
		JsonElement propertyEl = data.get("property");
		if(propertyEl == null)
			return null;
		String propertyStr = propertyEl.getAsString();
		if(!propertyStr.contains(":"))
			propertyStr = "minecraft:" + propertyStr;
		if(propertyStr.equals(ConditionBroken.TYPE))
			return new ConditionBroken(data);
		else if(propertyStr.equals(ConditionComponent.TYPE))
			return new ConditionComponent(data);
		else if(propertyStr.equals(ConditionDamaged.TYPE))
			return new ConditionDamaged(data);
		else if(propertyStr.equals(ConditionHasComponent.TYPE))
			return new ConditionHasComponent(data);
		else if(propertyStr.equals(ConditionCustomModelData.TYPE))
			return new ConditionCustomModelData(data);
		return null;
	}
	
	private static Property parseProperty(JsonObject data) {
		if(data == null)
			return null;
		JsonElement propertyEl = data.get("property");
		if(propertyEl == null)
			return null;
		String propertyStr = propertyEl.getAsString();
		if(!propertyStr.contains(":"))
			propertyStr = "minecraft:" + propertyStr;
		if(propertyStr.equals(PropertyBlockState.TYPE))
			return new PropertyBlockState(data);
		else if(propertyStr.equals(PropertyChargeType.TYPE))
			return new PropertyChargeType(data);
		else if(propertyStr.equals(PropertyComponent.TYPE))
			return new PropertyComponent(data);
		else if(propertyStr.equals(PropertyContextEntityType.TYPE))
			return new PropertyContextEntityType(data);
		else if(propertyStr.equals(PropertyDisplayContext.TYPE))
			return new PropertyDisplayContext(data);
		else if(propertyStr.equals(PropertyMainHand.TYPE))
			return new PropertyMainHand(data);
		else if(propertyStr.equals(PropertyTrimMaterial.TYPE))
			return new PropertyTrimMaterial(data);
		else if(propertyStr.equals(PropertyCustomModelData.TYPE))
			return new PropertyCustomModelData(data);
		return null;
	}
	
	private static NumericProperty parseNumericProperty(JsonObject data) {
		if(data == null)
			return null;
		JsonElement propertyEl = data.get("property");
		if(propertyEl == null)
			return null;
		String propertyStr = propertyEl.getAsString();
		if(!propertyStr.contains(":"))
			propertyStr = "minecraft:" + propertyStr;
		if(propertyStr.equals(NumericPropertyBundleFullness.TYPE))
			return new NumericPropertyBundleFullness(data);
		else if(propertyStr.equals(NumericPropertyCount.TYPE))
			return new NumericPropertyCount(data);
		else if(propertyStr.equals(NumericPropertyDamage.TYPE))
			return new NumericPropertyDamage(data);
		else if(propertyStr.equals(NumericPropertyCustomModelData.TYPE))
			return new NumericPropertyCustomModelData(data);
		return null;
	}
	
	private static SpecialModel parseSpecialModel(JsonObject data) {
		if(data == null)
			return null;
		JsonElement typeEl = data.get("type");
		if(typeEl == null)
			return null;
		String typeStr = typeEl.getAsString();
		if(!typeStr.contains(":"))
			typeStr = "minecraft:" + typeStr;
		if(typeStr.equals(SpecialModelBanner.TYPE))
			return new SpecialModelBanner(data);
		else if(typeStr.equals(SpecialModelBed.TYPE))
			return new SpecialModelBed(data);
		else if(typeStr.equals(SpecialModelChest.TYPE))
			return new SpecialModelChest(data);
		else if(typeStr.equals(SpecialModelConduit.TYPE))
			return new SpecialModelConduit(data);
		else if(typeStr.equals(SpecialModelDecoratedPot.TYPE))
			return new SpecialModelDecoratedPot(data);
		else if(typeStr.equals(SpecialModelHangingSign.TYPE))
			return new SpecialModelHangingSign(data);
		else if(typeStr.equals(SpecialModelHead.TYPE))
			return new SpecialModelHead(data);
		else if(typeStr.equals(SpecialModelShield.TYPE))
			return new SpecialModelShield(data);
		else if(typeStr.equals(SpecialModelShulkerBox.TYPE))
			return new SpecialModelShulkerBox(data);
		else if(typeStr.equals(SpecialModelStandingSign.TYPE))
			return new SpecialModelStandingSign(data);
		else if(typeStr.equals(SpecialModelTrident.TYPE))
			return new SpecialModelTrident(data);
		return null;
	}
	
	private ItemModel itemModel;
	
	public ItemHandlerJavaEdition(JsonObject data) {
		JsonObject modelData = data.getAsJsonObject("model");
		itemModel = parseItemModel(modelData);
	}
	
	@Override
	public Model getModel(String name, NbtTagCompound data, String displayContext) {
		Model model = new Model(name, null, false);
		if(itemModel != null)
			itemModel.setupModel(name, data, displayContext, model);
		return model;
	}

}
