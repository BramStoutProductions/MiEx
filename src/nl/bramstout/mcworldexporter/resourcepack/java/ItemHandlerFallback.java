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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;

public class ItemHandlerFallback extends ItemHandler{
	
	private static abstract class Predicate{
		
		private float valueToMatch;
		
		public Predicate(JsonElement data) {
			valueToMatch = data.getAsFloat();
		}
		
		public boolean match(NbtTagCompound data, String displayContext) {
			float value = getValue(data, displayContext);
			return value == valueToMatch;
		}
		
		protected abstract float getValue(NbtTagCompound data, String displayContext);
		
	}
	
	private static class PredicateBroken extends Predicate{
		
		public static final String TYPE = "broken";
		
		public PredicateBroken(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return 0f;
			NbtTag maxDamageEl = ((NbtTagCompound) components).get("minecraft:max_damage");
			if(maxDamageEl == null)
				return 0f;
			int maxDamage = maxDamageEl.asInt();
			
			NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
			if(damageEl == null)
				return 0f;
			
			int damage = damageEl.asInt();
			
			return damage >= maxDamage ? 1f : 0f;
		}
		
	}
	
	private static class PredicateDamage extends Predicate{
		
		public static final String TYPE = "damage";
		
		public PredicateDamage(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			int damage = 0;
			int maxDamage = 1;
			NbtTag components = data.get("components");
			if(components != null && components instanceof NbtTagCompound) {
				NbtTag maxDamageEl = ((NbtTagCompound) components).get("minecraft:max_damage");
				if(maxDamageEl != null) {
					maxDamage = maxDamageEl.asInt();
				}
				NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
				if(damageEl != null) {
					damage = damageEl.asInt();
				}
			}
			
			return ((float) damage) / ((float) maxDamage);
		}
		
	}
	
	private static class PredicateDamaged extends Predicate{
		
		public static final String TYPE = "damaged";
		
		public PredicateDamaged(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return 0f;
			
			NbtTag damageEl = ((NbtTagCompound) components).get("minecraft:damage");
			if(damageEl == null)
				return 0f;
			
			int damage = damageEl.asInt();
			
			return damage >= 1 ? 1f : 0f;
		}
		
	}
	
	private static class PredicateLeftHanded extends Predicate{
		
		public static final String TYPE = "lefthanded";
		
		public PredicateLeftHanded(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			return 0f;
		}
		
	}
	
	private static class PredicateCharged extends Predicate{
		
		public static final String TYPE = "charged";
		
		public PredicateCharged(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return 0f;
			NbtTag chargedProjectiles = ((NbtTagCompound) components).get("minecraft:charged_projectiles");
			if(chargedProjectiles == null || !(chargedProjectiles instanceof NbtTagList))
				return 0f;
			int size = ((NbtTagList) chargedProjectiles).getSize();
			if(size <= 0)
				return 0f;
			return 1f;
		}
		
	}
	
	private static class PredicateFirework extends Predicate{
		
		public static final String TYPE = "firework";
		
		public PredicateFirework(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return 0f;
			NbtTag chargedProjectiles = ((NbtTagCompound) components).get("minecraft:charged_projectiles");
			if(chargedProjectiles == null || !(chargedProjectiles instanceof NbtTagList))
				return 0f;
			int size = ((NbtTagList) chargedProjectiles).getSize();
			if(size <= 0)
				return 0f;
			for(int i = 0; i < size; ++i) {
				NbtTag el = ((NbtTagList) chargedProjectiles).get(i);
				if(!(el instanceof NbtTagCompound))
					continue;
				NbtTag idEl = ((NbtTagCompound) el).get("id");
				if(idEl == null)
					continue;
				String id = idEl.asString();
				if(id.equals("minecraft:firework_rocket"))
					return 1f;
			}
			return 0f;
		}
		
	}
	
	private static class PredicateCustomModelData extends Predicate{
		
		public static final String TYPE = "custom_model_data";
		
		public PredicateCustomModelData(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			NbtTag components = data.get("components");
			if(components == null || !(components instanceof NbtTagCompound))
				return Float.NaN;
			
			NbtTag customModelData = ((NbtTagCompound)components).get("minecraft:custom_model_data");
			if(customModelData == null || !(customModelData instanceof NbtTagCompound))
				return Float.NaN;
			
			NbtTag floatsArray = ((NbtTagCompound) customModelData).get("floats");
			if(floatsArray == null || !(floatsArray instanceof NbtTagList))
				return Float.NaN;
			
			if(((NbtTagList)floatsArray).getSize() <= 0)
				return Float.NaN;
			
			return ((NbtTagList) floatsArray).get(0).asFloat();
		}
		
	}
	
	private static class PredicateFilled extends Predicate{
		
		public static final String TYPE = "filled";
		
		public PredicateFilled(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
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
			
			return weight / (64f * num);
		}
		
	}
	
	private static class PredicateTrimType extends Predicate{
		
		public static final String TYPE = "trim_type";
		
		public PredicateTrimType(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			return 0f;
		}
		
	}
	
	private static class PredicateHoneyLevel extends Predicate{
		
		public static final String TYPE = "honey_level";
		
		public PredicateHoneyLevel(JsonElement data) {
			super(data);
		}
		
		@Override
		protected float getValue(NbtTagCompound data, String displayContext) {
			// TODO: Implement
			return 0f;
		}
		
	}
	
	private static Predicate parsePredicate(Entry<String, JsonElement> entry) {
		String typeStr = entry.getKey();
		if(typeStr.equals(PredicateBroken.TYPE))
			return new PredicateBroken(entry.getValue());
		else if(typeStr.equals(PredicateCharged.TYPE))
			return new PredicateCharged(entry.getValue());
		else if(typeStr.equals(PredicateCustomModelData.TYPE))
			return new PredicateCustomModelData(entry.getValue());
		else if(typeStr.equals(PredicateDamage.TYPE))
			return new PredicateDamage(entry.getValue());
		else if(typeStr.equals(PredicateDamaged.TYPE))
			return new PredicateDamaged(entry.getValue());
		else if(typeStr.equals(PredicateFilled.TYPE))
			return new PredicateFilled(entry.getValue());
		else if(typeStr.equals(PredicateFirework.TYPE))
			return new PredicateFirework(entry.getValue());
		else if(typeStr.equals(PredicateHoneyLevel.TYPE))
			return new PredicateFirework(entry.getValue());
		else if(typeStr.equals(PredicateTrimType.TYPE))
			return new PredicateTrimType(entry.getValue());
		else if(typeStr.equals(PredicateLeftHanded.TYPE))
			return new PredicateLeftHanded(entry.getValue());
		return null;
	}
	
	private static class ModelOverride{
		
		public String model;
		private List<Predicate> predicates;
		
		public ModelOverride(JsonObject data) {
			model = null;
			predicates = new ArrayList<Predicate>();
			
			JsonElement modelEl = data.get("model");
			if(modelEl != null)
				model = modelEl.getAsString();
			
			JsonObject predicateObject = data.getAsJsonObject("predicate");
			if(predicateObject != null) {
				for(Entry<String, JsonElement> entry : predicateObject.entrySet()) {
					predicates.add(parsePredicate(entry));
				}
			}
		}
		
		public boolean matches(NbtTagCompound data, String displayContext) {
			if(model == null)
				return false;
			for(Predicate predicate : predicates) {
				if(predicate == null)
					return false;
				if(!predicate.match(data, displayContext))
					return false;
			}
			return true;
		}
		
	}
	
	private String fallbackModel;
	private List<ModelOverride> overrides;
	
	public ItemHandlerFallback(String modelName, JsonObject data) {
		fallbackModel = modelName;
		overrides = null;
		
		JsonArray overridesArray = data.getAsJsonArray("overrides");
		if(overridesArray != null) {
			overrides = new ArrayList<ModelOverride>();
			for(JsonElement el : overridesArray.asList()) {
				if(el.isJsonObject()) {
					overrides.add(new ModelOverride(el.getAsJsonObject()));
				}
			}
		}
	}

	@Override
	public Model getModel(String name, NbtTagCompound data, String displayContext) {
		String modelName = fallbackModel;
		if(overrides != null) {
			for(ModelOverride override : overrides) {
				if(override.matches(data, displayContext)) {
					modelName = override.model;
					break;
				}
			}
		}
		
		int modelId = ModelRegistry.getIdForName(modelName, false);
		return ModelRegistry.getModel(modelId);
	}

}
