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

package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentEntitySensor;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentEntitySensor.SubSensor;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentEnvironmentScanner;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentFollowRange;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentGroup;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentNavigation;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentPeek;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentPreferredPath;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentScheduler;
import nl.bramstout.mcworldexporter.entity.ai.AIComponentTimer;
import nl.bramstout.mcworldexporter.entity.ai.EntityAI;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponent;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentAddGroup;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentRandomize;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentRemoveGroup;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentSequence;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentSetProperty;
import nl.bramstout.mcworldexporter.entity.ai.EntityEvent.EntityEventComponentTriggerEvent;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilter;
import nl.bramstout.mcworldexporter.entity.ai.EntityFilterMolang;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourAvoidBlock;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourAvoidMobType;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourAvoidMobType.Avoidee;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourCroak;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourFindCover;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourFleeSun;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourFloat;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourFloatWander;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourFollowMob;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourInspectBookshelf;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourLayDown;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourLookAtEntity;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourLookAtTarget;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToBlock;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToLand;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToLava;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToLiquid;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToRandomBlock;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveToWater;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourMoveTowardsTarget;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourNap;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourPeek;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomBreach;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomFly;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomHover;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomLookAround;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomLookAroundAndSit;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomSitting;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomStroll;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRandomSwim;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRiseToLiquidLevel;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourRoll;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourSitOnBlock;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourSwimIdle;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourSwimWander;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourSwimWithEntity;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourTimerFlag1;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourTimerFlag2;
import nl.bramstout.mcworldexporter.entity.ai.behaviour.AIComponentBehaviourTimerFlag3;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentAnnotationBreakDoor;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentAnnotationOpenDoor;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentBodyRotationBlocked;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentBuoyant;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentFlyingSpeed;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentFrictionModifier;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentGroundOffset;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentJumpStatic;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentLavaMovement;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovement;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementAmphibious;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementBasic;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementFly;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementGeneric;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementGlide;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementHover;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementJump;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementRail;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementSkip;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentMovementSway;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentPhysics;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentPushThrough;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentTeleport;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentUnderwaterMovement;
import nl.bramstout.mcworldexporter.entity.ai.movement.AIComponentWaterMovement;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentAgeable;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentCanClimb;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentCanFly;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentCollisionBox;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentColor;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentColor2;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsBaby;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsCharged;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsChested;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsIgnited;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsSaddled;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsShaking;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsSheared;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsStackable;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsStunned;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentIsTamed;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentMarkVariant;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentOutOfControl;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentPushable;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentRideable;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentRideable.Seat;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentScale;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentScaleByAge;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentSittable;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentSkinId;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentTypeFamily;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentVariableMaxAutoStep;
import nl.bramstout.mcworldexporter.entity.ai.property.AIComponentVariant;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangScript;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Animation;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController.AnimationState;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class EntityAIHandlerBedrockEdition extends EntityAIHandler{

	private List<AIComponentGroup> componentGroups;
	private List<EntityEvent> events;
	private Map<String, MolangScript> properties;
	private Map<String, String> animations;
	private AnimationController animationController;
	private List<MolangScript> initMolang;
	private MolangScript scaleXExpression;
	private MolangScript scaleYExpression;
	private MolangScript scaleZExpression;
	
	public EntityAIHandlerBedrockEdition(String name, JsonObject data) {
		componentGroups = new ArrayList<AIComponentGroup>();
		events = new ArrayList<EntityEvent>();
		properties = new HashMap<String, MolangScript>();
		animations = new HashMap<String, String>();
		animationController = null;
		this.initMolang = new ArrayList<MolangScript>();
		scaleXExpression = MolangParser.parse("1.0");
		scaleYExpression = MolangParser.parse("1.0");
		scaleZExpression = MolangParser.parse("1.0");
		
		if(data.has("components"))
			componentGroups.add(parseComponentGroup("", data.getAsJsonObject("components")));
		if(data.has("component_groups")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("component_groups").entrySet()) {
				componentGroups.add(parseComponentGroup(entry.getKey(), entry.getValue().getAsJsonObject()));
			}
		}
		if(data.has("events")) {
			JsonObject eventsObj = data.getAsJsonObject("events");
			for(String eventName : eventsObj.keySet()) {
				events.add(getEvent(eventsObj, eventName));
			}
		}
		if(data.has("animations")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("animations").entrySet()) {
				animations.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		if(data.has("scripts")) {
			JsonObject obj = data.getAsJsonObject("scripts");
			if(obj.has("animate")) {
				animationController = new AnimationController("");
				AnimationState defaultState = new AnimationState("default", 0f);
				for(JsonElement el : obj.getAsJsonArray("animate").asList()) {
					if(el.isJsonPrimitive())
						defaultState.addAnimation(el.getAsString(), MolangParser.parse("1.0"));
					else if(el.isJsonObject()) {
						for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
							defaultState.addAnimation(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
						}
					}
				}
				animationController.getStates().add(defaultState);
			}
			if(obj.has("pre_animation")) {
				if(animationController == null)
					animationController = new AnimationController("");
				for(JsonElement el : obj.getAsJsonArray("pre_animation").asList()) {
					animationController.getPreAnimationScripts().add(MolangParser.parse(el.getAsString()));
				}
			}
			if(obj.has("initialize")) {
				for(JsonElement el : obj.getAsJsonArray("initialize").asList()) {
					initMolang.add(MolangParser.parse(el.getAsString()));
				}
			}
			if(obj.has("scale")) {
				scaleXExpression = MolangParser.parse(obj.get("scale").getAsString());
				scaleYExpression = scaleXExpression;
				scaleZExpression = scaleXExpression;
			}
			if(obj.has("scaleX")) {
				scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
			}
			if(obj.has("scaleY")) {
				scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
			}
			if(obj.has("scaleZ")) {
				scaleXExpression = MolangParser.parse(obj.get("scaleX").getAsString());
			}
		}
		if(data.has("description")) {
			JsonObject descriptionObj = data.getAsJsonObject("description");
			if(descriptionObj.has("properties")) {
				for(Entry<String, JsonElement> entry : descriptionObj.getAsJsonObject("properties").entrySet()) {
					JsonObject obj = entry.getValue().getAsJsonObject();
					String defaultValue = "false";
					String type = "bool";
					if(obj.has("type"))
						type = obj.get("type").getAsString();
					if(type.equals("int"))
						defaultValue = "0";
					else if(type.equals("float"))
						defaultValue = "0";
					else if(type.equals("enum")) {
						defaultValue = "";
						if(obj.has("values")) {
							JsonArray array = obj.getAsJsonArray("values");
							if(array.size() > 0)
								defaultValue = array.get(0).getAsString();
						}
					}
					if(obj.has("default")) {
						JsonPrimitive prim = obj.getAsJsonPrimitive("default");
						if(prim.isBoolean())
							defaultValue = prim.getAsBoolean() ? "true" : "false";
						else if(prim.isNumber())
							defaultValue = Float.toString(prim.getAsFloat());
						else if(prim.isString())
							defaultValue = prim.getAsString();
					}
					properties.put(entry.getKey(), MolangParser.parse(defaultValue));
				}
			}
		}
	}
	
	private AIComponentGroup parseComponentGroup(String name, JsonObject data) {
		List<AIComponent> components = new ArrayList<AIComponent>();
		
		for(Entry<String, JsonElement> entry : data.entrySet()) {
			AIComponent component = parseComponent(entry.getKey(), entry.getValue());
			if(component != null)
				components.add(component);
		}
		
		return new AIComponentGroup(name, components);
	}
	
	private AIComponent parseComponent(String name, JsonElement element) {
		JsonObject data = null;
		if(element.isJsonObject())
			data = element.getAsJsonObject();
		else {
			data = new JsonObject();
		}
		if(name.equals("minecraft:entity_sensor")) {
			AIComponentEntitySensor component = new AIComponentEntitySensor("minecraft:entity_sensor");
			if(data.has("subsensors")) {
				for(JsonElement el : data.getAsJsonArray("subsensors").asList()) {
					JsonObject subSensorObj = el.getAsJsonObject();
					SubSensor subSensor = new SubSensor();
					subSensor.cooldown = getFloat(subSensorObj, "cooldown", -1f);
					subSensor.filter = getFilter(subSensorObj, "event_filters");
					subSensor.eventName = getString(subSensorObj, "event", "");
					subSensor.maxEntities = getInt(subSensorObj, "maximum_count", -1);
					subSensor.minEntities = getInt(subSensorObj, "minimum_count", 1);
					subSensor.horizontalRange = getMin(subSensorObj, "range", 10f);
					subSensor.verticalRange = getMax(subSensorObj, "range", 10f);
					subSensor.requireAllEntitiesToPassFilter = getBool(subSensorObj, "require_all", false);
					component.subSensors.add(subSensor);
				}
			}
			return component;
		}else if(name.equals("minecraft:environment_sensor")) {
			AIComponentEnvironmentScanner component = new AIComponentEnvironmentScanner("minecraft:environment_sensor");
			if(data.has("triggers")) {
				JsonElement triggersEl = data.get("triggers");
				if(triggersEl.isJsonArray()) {
					for(JsonElement el : triggersEl.getAsJsonArray().asList()) {
						JsonObject subSensorObj = el.getAsJsonObject();
						AIComponentEnvironmentScanner.SubSensor subSensor = new AIComponentEnvironmentScanner.SubSensor();
						subSensor.eventName = getString(subSensorObj, "event", "");
						subSensor.filter = getFilter(subSensorObj, "filters");
						subSensor.target = getString(subSensorObj, "target", "self");
						component.subSensors.add(subSensor);
					}
				}else if(triggersEl.isJsonObject()) {
					JsonObject subSensorObj = triggersEl.getAsJsonObject();
					AIComponentEnvironmentScanner.SubSensor subSensor = new AIComponentEnvironmentScanner.SubSensor();
					subSensor.eventName = getString(subSensorObj, "event", "");
					subSensor.filter = getFilter(subSensorObj, "filters");
					subSensor.target = getString(subSensorObj, "target", "self");
					component.subSensors.add(subSensor);
				}
			}
			return component;
		}else if(name.equals("minecraft:follow_range")) {
			AIComponentFollowRange component = new AIComponentFollowRange("minecraft:follow_range");
			component.radius = getFloat(data, "value", 16);
			component.maxDistance = getFloat(data, "max", 32);
			return component;
		}else if(name.equals("minecraft:navigation.climb")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.climb");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.float")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.float");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.fly")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.fly");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.generic")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.generic");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.hover")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.hover");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.swim")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.swim");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:navigation.walk")) {
			AIComponentNavigation component = new AIComponentNavigation("minecraft:navigation.walk");
			component.avoidDamageBlocks = getBool(data, "avoid_damage_blocks", false);
			component.avoidPortals = getBool(data, "avoid_portals", false);
			component.avoidSun = getBool(data, "avoid_sun", false);
			component.avoidWater = getBool(data, "avoid_water", false);
			component.blocksToAvoid = getStringList(data, "blocks_to_avoid");
			component.canBreach = getBool(data, "can_breach", false);
			component.canBreakDoors = getBool(data, "can_break_doors", false);
			component.canJump = getBool(data, "can_jump", true);
			component.canOpenDoors = getBool(data, "can_open_doors", false);
			component.canOpenIronDoors = getBool(data, "can_open_iron_doors", false);
			component.canPassDoors = getBool(data, "can_pass_doors", true);
			component.canPathFromAir = getBool(data, "can_path_from_air", false);
			component.canPathOverLava = getBool(data, "can_path_over_lava", false);
			component.canPathOverWater = getBool(data, "can_path_over_water", false);
			component.canSink = getBool(data, "can_sink", true);
			component.canSwim = getBool(data, "can_swim", false);
			component.canWalk = getBool(data, "can_walk", true);
			component.canWalkInLava = getBool(data, "can_walk_in_lava", false);
			component.isAmphibious = getBool(data, "is_amphibious", false);
			return component;
		}else if(name.equals("minecraft:peek")) {
			AIComponentPeek component = new AIComponentPeek("minecraft:peek");
			component.onOpen = getEvent(data, "on_open");
			component.onClose = getEvent(data, "on_close");
			component.onTargetOpen = getEvent(data, "on_target_open");
			return component;
		}else if(name.equals("minecraft:preferred_path")) {
			AIComponentPreferredPath component = new AIComponentPreferredPath("minecraft:preferred_path");
			component.defaultBlockCost = getFloat(data, "default_block_cost", 1.0f);
			component.jumpCost = getFloat(data, "jump_cost", 0f);
			component.maxFallBlocks = getInt(data, "max_fall_blocks", 3);
			if(data.has("preferred_path_blocks")) {
				for(JsonElement el : data.getAsJsonArray("preferred_path_blocks").asList()) {
					JsonObject obj = el.getAsJsonObject();
					float cost = 1f;
					if(obj.has("cost"))
						cost = obj.get("cost").getAsFloat();
					if(obj.has("blocks")) {
						for(JsonElement blockEl : obj.getAsJsonArray("blocks").asList()) {
							String blockName = blockEl.getAsString();
							if(!blockName.contains(":"))
								blockName = "minecraft:" + blockName;
							component.preferredPathBlocks.put(blockName, cost);
						}
					}
				}
			}
			return component;
		}else if(name.equals("minecraft:scheduler")) {
			AIComponentScheduler component = new AIComponentScheduler("minecraft:scheduler");
			component.minDelay = getFloat(data, "min_delay_secs", 0f);
			component.maxDelay = getFloat(data, "max_delay_secs", 0f);
			if(data.has("scheduled_events")) {
				for(JsonElement el : data.getAsJsonArray("scheduled_events").asList()) {
					component.scheduledEvents.add(getEvent(el.getAsJsonObject()));
				}
			}
			return component;
		}else if(name.equals("minecraft:timer")) {
			AIComponentTimer component = new AIComponentTimer("minecraft:timer");
			component.looping = getBool(data, "looping", true);
			component.randomInterval = getBool(data, "randomInterval", true);
			if(data.has("random_time_choices")) {
				component.randomTimeChoices = new ArrayList<Float>();
				component.randomTimeChoicesWeights = new ArrayList<Float>();
				for(JsonElement el : data.getAsJsonArray("random_time_choices").asList()) {
					if(el.isJsonPrimitive()) {
						component.randomTimeChoices.add(el.getAsFloat());
						component.randomTimeChoicesWeights.add(1f);
					}else if(el.isJsonObject()) {
						if(el.getAsJsonObject().has("value")) {
							component.randomTimeChoices.add(el.getAsJsonObject().get("value").getAsFloat());
							if(el.getAsJsonObject().has("weight"))
								component.randomTimeChoicesWeights.add(el.getAsJsonObject().get("weight").getAsFloat());
							else
								component.randomTimeChoicesWeights.add(1f);
						}
					}
				}
			}
			component.minTime = getMin(data, "time", 0f);
			component.maxTime = getMax(data, "time", 0f);
			component.timerDoneEvent = getEvent(data, "time_down_event");
			return component;
		}else if(name.equals("minecraft:behavior.avoid_block")) {
			AIComponentBehaviourAvoidBlock component = new AIComponentBehaviourAvoidBlock("minecraft:behavior.avoid_block", getInt(data, "priority", 0));
			component.onEscape = getEvent(data, "on_escape");
			component.searchHeight = getInt(data, "search_height", 0);
			component.searchRange = getInt(data, "search_range", 0);
			component.blocks = getStringList(data, "target_blocks");
			component.tickInterval = getInt(data, "tick_interval", 1);
		}else if(name.equals("minecraft:behavior.avoid_mob_type")) {
			AIComponentBehaviourAvoidMobType component = new AIComponentBehaviourAvoidMobType("minecraft:behavior.avoid_mob_type", getInt(data, "priority", 0));
			component.avoidTargetXZ = getInt(data, "avoid_target_xz", 16);
			component.avoidTargetY = getInt(data, "avoid_target_y", 7);
			if(data.has("entity_types")) {
				for(JsonElement el : data.getAsJsonArray("entity_types").asList()) {
					JsonObject obj = el.getAsJsonObject();
					Avoidee avoidee = new Avoidee();
					avoidee.filter = getFilter(obj, "filters");
					avoidee.ignoreVisibility = getBool(obj, "ignore_visibility", false);
					avoidee.maxDist = getFloat(obj, "max_dist", 3f);
					avoidee.maxFlee = getFloat(obj, "max_flee", 10f);
					avoidee.sprintDistance = getFloat(obj, "sprint_distance", 7f);
					avoidee.onEscape = getEvent(obj, "on_escape_event");
					component.avoidees.add(avoidee);
				}
			}
			return component;
		}else if(name.equals("minecraft:behavior.croak")) {
			AIComponentBehaviourCroak component = new AIComponentBehaviourCroak("minecraft:behavior.croak", getInt(data, "priority", 0));
			component.minDuration = getMin(data, "duration", 4.5f);
			component.maxDuration = getMax(data, "duration", 4.5f);
			component.minInterval = getMin(data, "interval", 10f);
			component.maxInterval = getMax(data, "interval", 20f);
			component.filter = getFilter(data, "filters");
			return component;
		}else if(name.equals("minecraft:behavior.find_cover")) {
			AIComponentBehaviourFindCover component = new AIComponentBehaviourFindCover("minecraft:behavior.find_cover", getInt(data, "priority", 0));
			component.cooldownTime = getFloat(data, "cooldown_time", 0f);
			return component;
		}else if(name.equals("minecraft:behavior.flee_sun")) {
			AIComponentBehaviourFleeSun component = new AIComponentBehaviourFleeSun("minecraft:behavior.flee_sun", getInt(data, "priority", 0));
			return component;
		}else if(name.equals("minecraft:behavior.float")) {
			AIComponentBehaviourFloat component = new AIComponentBehaviourFloat("minecraft:behavior.float", getInt(data, "priority", 0));
			return component;
		}else if(name.equals("minecraft:behavior.float_wander")) {
			AIComponentBehaviourFloatWander component = new AIComponentBehaviourFloatWander("minecraft:behavior.float_wander", getInt(data, "priority", 0));
			component.minFloatDuration = getMin(data, "float_duration", 0f);
			component.maxFloatDuration = getMax(data, "float_duration", 0f);
			component.randomSelect = getBool(data, "random_reselect", false);
			component.searchDistanceXZ = getInt(data, "xz_dist", 10);
			component.searchDistanceY = getInt(data, "y_dist", 7);
			component.yOffset = getFloat(data, "y_offset", 0f);
			return component;
		}else if(name.equals("minecraft:behavior.follow_mob")) {
			AIComponentBehaviourFollowMob component = new AIComponentBehaviourFollowMob("minecraft:behavior.follow_mob", getInt(data, "priority", 0));
			component.searchRange = getFloat(data, "search_range", 0f);
			component.stopDistance = getFloat(data, "stop_distance", 2f);
			return component;
		}else if(name.equals("minecraft:behavior.inspect_bookshelf")) {
			AIComponentBehaviourInspectBookshelf component = new AIComponentBehaviourInspectBookshelf("minecraft:behavior.inspect_bookshelf", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.searchCount = getInt(data, "search_count", 10);
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			return component;
		}else if(name.equals("minecraft:behavior.lay_down")) {
			AIComponentBehaviourLayDown component = new AIComponentBehaviourLayDown("minecraft:behavior.lay_down", getInt(data, "priority", 0));
			component.interval = getInt(data, "interval", 120);
			component.stopInterval = getInt(data, "random_stop_interval", 120);
			return component;
		}else if(name.equals("minecraft:behavior.look_at_entity")) {
			AIComponentBehaviourLookAtEntity component = new AIComponentBehaviourLookAtEntity("minecraft:behavior.look_at_entity", getInt(data, "priority", 0));
			component.angleOfViewHorizontal = getFloat(data, "angle_of_view_horizontal", 180f);
			component.angleOfViewVertical = getFloat(data, "angle_of_view_vertical", 90f);
			component.filter = getFilter(data, "filters");
			component.lookDistance = getFloat(data, "look_distance", 8f);
			component.minLookTime = getMin(data, "look_time", 2f);
			component.maxLookTime = getMax(data, "look_time", 4f);
			component.probability = getFloat(data, "probability", 0.02f);
			return component;
		}else if(name.equals("minecraft:behavior.look_at_target")) {
			AIComponentBehaviourLookAtTarget component = new AIComponentBehaviourLookAtTarget("minecraft:behavior.look_at_target", getInt(data, "priority", 0));
			component.angleOfViewHorizontal = getFloat(data, "angle_of_view_horizontal", 180f);
			component.angleOfViewVertical = getFloat(data, "angle_of_view_vertical", 90f);
			component.lookDistance = getFloat(data, "look_distance", 8f);
			component.minLookTime = getMin(data, "look_time", 2f);
			component.maxLookTime = getMax(data, "look_time", 4f);
			component.probability = getFloat(data, "probability", 0.02f);
			return component;
		}else if(name.equals("minecraft:behavior.move_to_block")) {
			AIComponentBehaviourMoveToBlock component = new AIComponentBehaviourMoveToBlock("minecraft:behavior.move_to_block", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.onReach = getEvent(data, "on_reach");
			component.onStayCompleted = getEvent(data, "on_stay_completed");
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			component.startChance = getFloat(data, "start_chance", 1f);
			component.stayDuration = getFloat(data, "stay_duration", 0f);
			component.targetBlocks = getStringList(data, "target_blocks");
			component.targetOffsetX = getX(data, "target_offset", 0f);
			component.targetOffsetY = getY(data, "target_offset", 0f);
			component.targetOffsetZ = getZ(data, "target_offset", 0f);
			String selectionMethod = getString(data, "target_selection_method", "nearest");
			component.targetSelectionMethod = AIComponentBehaviourMoveToBlock.SelectionMethod.NEAREST;
			if(selectionMethod.equalsIgnoreCase("random"))
				component.targetSelectionMethod = AIComponentBehaviourMoveToBlock.SelectionMethod.RANDOM;
			component.tickInterval = getInt(data, "tick_interval", 20);
			return component;
		}else if(name.equals("minecraft:behavior.move_to_land")) {
			AIComponentBehaviourMoveToLand component = new AIComponentBehaviourMoveToLand("minecraft:behavior.move_to_land", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.searchCount = getInt(data, "search_count", 10);
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			return component;
		}else if(name.equals("minecraft:behavior.move_to_lava")) {
			AIComponentBehaviourMoveToLava component = new AIComponentBehaviourMoveToLava("minecraft:behavior.move_to_lava", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.searchCount = getInt(data, "search_count", 10);
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			return component;
		}else if(name.equals("minecraft:behavior.move_to_liquid")) {
			AIComponentBehaviourMoveToLiquid component = new AIComponentBehaviourMoveToLiquid("minecraft:behavior.move_to_liquid", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.searchCount = getInt(data, "search_count", 10);
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			String liquidType = getString(data, "material_type", "Any");
			if(liquidType.equalsIgnoreCase("any")) {
				component.liquidTypes.add("minecraft:water");
				component.liquidTypes.add("minecraft:lava");
			}else if(liquidType.equalsIgnoreCase("water")) {
				component.liquidTypes.add("minecraft:water");
			}else if(liquidType.equalsIgnoreCase("lava")) {
				component.liquidTypes.add("minecraft:lava");
			}
			return component;
		}else if(name.equals("minecraft:behavior.move_to_random_block")) {
			AIComponentBehaviourMoveToRandomBlock component = new AIComponentBehaviourMoveToRandomBlock("minecraft:behavior.move_to_random_block", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "within_radius", 0.5f);
			component.searchHeight = (int) getFloat(data, "block_distance", 16f);
			component.searchRange = (int) getFloat(data, "block_distance", 16f);
			return component;
		}else if(name.equals("minecraft:behavior.move_towards_target")) {
			AIComponentBehaviourMoveTowardsTarget component = new AIComponentBehaviourMoveTowardsTarget("minecraft:behavior.move_towards_target", getInt(data, "priority", 0));
			component.withinRadius = getFloat(data, "within_radius", 0f);
			return component;
		}else if(name.equals("minecraft:behavior.move_to_water")) {
			AIComponentBehaviourMoveToWater component = new AIComponentBehaviourMoveToWater("minecraft:behavior.move_to_water", getInt(data, "priority", 0));
			component.goalRadius = getFloat(data, "goal_radius", 0.5f);
			component.searchCount = getInt(data, "search_count", 10);
			component.searchHeight = getInt(data, "search_height", 1);
			component.searchRange = getInt(data, "search_range", 0);
			return component;
		}else if(name.equals("minecraft:behavior.nap")) {
			AIComponentBehaviourNap component = new AIComponentBehaviourNap("minecraft:behavior.nap", getInt(data, "priority", 0));
			component.minCooldown = getFloat(data, "cooldown_min", 0f);
			component.maxCooldown = getFloat(data, "cooldown_max", 0f);
			component.mobDetectionDistance = getFloat(data, "mob_detect_dist", 6f);
			component.mobDetectionHeight = getFloat(data, "mob_detect_height", 6f);
			component.canNapFilter = getFilter(data, "can_nap_filters");
			component.wakeMobExceptionFilter = getFilter(data, "wake_mob_exceptions");
			return component;
		}else if(name.equals("minecraft:behavior.peek")) {
			AIComponentBehaviourPeek component = new AIComponentBehaviourPeek("minecraft:behavior.peek", getInt(data, "priority", 0));
			return component;
		}else if(name.equals("minecraft:behavior.random_breach")) {
			AIComponentBehaviourRandomBreach component = new AIComponentBehaviourRandomBreach("minecraft:behavior.random_breach", getInt(data, "priority", 0));
			component.cooldownTime = getFloat(data, "cooldown_time", 0f);
			component.interval = getInt(data, "interval", 120);
			component.searchRange = getInt(data, "xz_dist", 10);
			component.searchHeight = getInt(data, "y_dist", 7);
			return component;
		}else if(name.equals("minecraft:behavior.random_fly")) {
			AIComponentBehaviourRandomFly component = new AIComponentBehaviourRandomFly("minecraft:behavior.random_fly", getInt(data, "priority", 0));
			component.searchDistanceXZ = getInt(data, "xz_dist", 10);
			component.searchDistanceY = getInt(data, "y_dist", 7);
			component.yOffset = getFloat(data, "y_offset", 0f);
			return component;
		}else if(name.equals("minecraft:behavior.random_hover")) {
			AIComponentBehaviourRandomHover component = new AIComponentBehaviourRandomHover("minecraft:behavior.random_hover", getInt(data, "priority", 0));
			component.searchDistanceXZ = getInt(data, "xz_dist", 10);
			component.searchDistanceY = getInt(data, "y_dist", 7);
			component.yOffset = getFloat(data, "y_offset", 0f);
			component.minHoverHeight = getMin(data, "hover_height", 0f);
			component.maxHoverHeight = getMax(data, "hover_height", 0f);
			component.interval = getInt(data, "interval", 120);
			return component;
		}else if(name.equals("minecraft:behavior.random_look_around")) {
			AIComponentBehaviourRandomLookAround component = new AIComponentBehaviourRandomLookAround("minecraft:behavior.random_look_around", getInt(data, "priority", 0));
			component.probability = getFloat(data, "probability", 0.2f);
			component.minLookTime = getMin(data, "look_time", 2f);
			component.maxLookTime = getMax(data, "look_time", 4f);
			component.angleOfViewHorizontal = getFloat(data, "angle_of_view_horizontal", 120f);
			component.angleOfViewVertical = getFloat(data, "angle_of_view_vertical", 60f);
			return component;
		}else if(name.equals("minecraft:behavior.random_look_around_and_sit")) {
			AIComponentBehaviourRandomLookAroundAndSit component = new AIComponentBehaviourRandomLookAroundAndSit("minecraft:behavior.random_look_around_and_sit", getInt(data, "priority", 0));
			component.probability = getFloat(data, "probability", 0.02f);
			component.cooldown = getFloat(data, "random_look_around_cooldown", 0f);
			component.minLookCount = getInt(data, "min_look_count", 1);
			component.maxLookCount = getInt(data, "max_look_count", 2);
			component.minLookTime = getInt(data, "min_look_time", 20);
			component.maxLookTime = getInt(data, "max_look_time", 40);
			component.minAngleOfViewHorizontal = getFloat(data, "min_angle_of_view_horizontal", -30f);
			component.maxAngleOfViewHorizontal = getFloat(data, "max_angle_of_view_horizontal", 30f);
			return component;
		}else if(name.equals("minecraft:behavior.random_sitting")) {
			AIComponentBehaviourRandomSitting component = new AIComponentBehaviourRandomSitting("minecraft:behavior.random_sitting", getInt(data, "priority", 0));
			component.cooldown = getFloat(data, "cooldown_time", 0f);
			component.minSitTime = getFloat(data, "min_sit_time", 10f);
			component.startChance = getFloat(data, "start_chance", 0.1f);
			component.stopChance = getFloat(data, "stop_chance", 0.3f);
			return component;
		}else if(name.equals("minecraft:behavior.random_stroll")) {
			AIComponentBehaviourRandomStroll component = new AIComponentBehaviourRandomStroll("minecraft:behavior.random_stroll", getInt(data, "priority", 0));
			component.interval = getInt(data, "interval", 120);
			component.searchDistanceXZ = getInt(data, "xz_dist", 10);
			component.searchDistanceY = getInt(data, "y_dist", 7);
			return component;
		}else if(name.equals("minecraft:behavior.random_swim")) {
			AIComponentBehaviourRandomSwim component = new AIComponentBehaviourRandomSwim("minecraft:behavior.random_swim", getInt(data, "priority", 0));
			component.interval = getInt(data, "interval", 120);
			component.searchRange = getInt(data, "xz_dist", 10);
			component.searchHeight = getInt(data, "y_dist", 7);
			component.avoidSurface = getBool(data, "avoid_surface", true);
			return component;
		}else if(name.equals("minecraft:behavior.rise_to_liquid_level")) {
			AIComponentBehaviourRiseToLiquidLevel component = new AIComponentBehaviourRiseToLiquidLevel("minecraft:behavior.rise_to_liquid_level", getInt(data, "priority", 0));
			component.liquidYOffset = getFloat(data, "liquid_y_offset", 0f);
			component.riseDelta = getFloat(data, "rise_delta", 0f);
			component.sinkDelta = getFloat(data, "sink_delta", 0f);
			return component;
		}else if(name.equals("minecraft:behavior.roll")) {
			AIComponentBehaviourRoll component = new AIComponentBehaviourRoll("minecraft:behavior.roll", getInt(data, "priority", 0));
			component.probability = getFloat(data, "probability", 1.0f);
			return component;
		}else if(name.equals("minecraft:behavior.ocelot_sit_on_block")) {
			AIComponentBehaviourSitOnBlock component = new AIComponentBehaviourSitOnBlock("minecraft:behavior.ocelot_sit_on_block", getInt(data, "priority", 0));
			return component;
		}else if(name.equals("minecraft:behavior.swim_idle")) {
			AIComponentBehaviourSwimIdle component = new AIComponentBehaviourSwimIdle("minecraft:behavior.swim_idle", getInt(data, "priority", 0));
			component.idleTime = getFloat(data, "idle_time", 5f);
			component.successRate = getFloat(data, "success_rate", 0.1f);
			return component;
		}else if(name.equals("minecraft:behavior.swim_wander")) {
			AIComponentBehaviourSwimWander component = new AIComponentBehaviourSwimWander("minecraft:behavior.swim_wander", getInt(data, "priority", 0));
			component.interval = getFloat(data, "interval", 0.00833f);
			component.wanderTime = getFloat(data, "wander_time", 5f);
			component.lookAhead = getFloat(data, "look_ahead", 5f);
			return component;
		}else if(name.equals("minecraft:behavior.swim_with_entity")) {
			AIComponentBehaviourSwimWithEntity component = new AIComponentBehaviourSwimWithEntity("minecraft:behavior.swim_with_entity", getInt(data, "priority", 0));
			component.catchUpMultiplier = getFloat(data, "catch_up_multiplier", 2.5f);
			component.catchUpThreshold = getFloat(data, "catch_up_threshold", 12f);
			component.chanceToStop = getFloat(data, "chance_to_stop", 0.0333f);
			component.entityTypes = getFilter(data, "entity_types");
			component.matchDirectionThreshold = getFloat(data, "match_direction_threshold", 2f);
			component.searchRange = getFloat(data, "search_range", 20f);
			component.speedMultiplier = getFloat(data, "speed_multiplier", 1.5f);
			component.stateCheckInterval = getFloat(data, "state_check_interval", 0.5f);
			component.stopDistance = getFloat(data, "stop_distance", 5f);
			component.successRate = getFloat(data, "success_rate", 0.1f);
			return component;
		}else if(name.equals("minecraft:behavior.timer_flag_1")) {
			AIComponentBehaviourTimerFlag1 component = new AIComponentBehaviourTimerFlag1("minecraft:behavior.timer_flag_1", getInt(data, "priority", 0));
			component.minCooldown = getMin(data, "cooldown_range", 10f);
			component.maxCooldown = getMax(data, "cooldown_range", 10f);
			component.minDuration = getMin(data, "duration_range", 2f);
			component.maxDuration = getMax(data, "duration_range", 2f);
			component.onStart = getEvent(data, "on_start");
			component.onEnd = getEvent(data, "on_end");
			return component;
		}else if(name.equals("minecraft:behavior.timer_flag_2")) {
			AIComponentBehaviourTimerFlag2 component = new AIComponentBehaviourTimerFlag2("minecraft:behavior.timer_flag_2", getInt(data, "priority", 0));
			component.minCooldown = getMin(data, "cooldown_range", 10f);
			component.maxCooldown = getMax(data, "cooldown_range", 10f);
			component.minDuration = getMin(data, "duration_range", 2f);
			component.maxDuration = getMax(data, "duration_range", 2f);
			component.onStart = getEvent(data, "on_start");
			component.onEnd = getEvent(data, "on_end");
			return component;
		}else if(name.equals("minecraft:behavior.timer_flag_3")) {
			AIComponentBehaviourTimerFlag3 component = new AIComponentBehaviourTimerFlag3("minecraft:behavior.timer_flag_3", getInt(data, "priority", 0));
			component.minCooldown = getMin(data, "cooldown_range", 10f);
			component.maxCooldown = getMax(data, "cooldown_range", 10f);
			component.minDuration = getMin(data, "duration_range", 2f);
			component.maxDuration = getMax(data, "duration_range", 2f);
			component.onStart = getEvent(data, "on_start");
			component.onEnd = getEvent(data, "on_end");
			return component;
		}else if(name.equals("minecraft:annotation.break_door")) {
			AIComponentAnnotationBreakDoor component = new AIComponentAnnotationBreakDoor("minecraft:annotation.break_door");
			component.breakTime = getFloat(data, "break_time", 12f);
			return component;
		}else if(name.equals("minecraft:annotation.open_door")) {
			AIComponentAnnotationOpenDoor component = new AIComponentAnnotationOpenDoor("minecraft:annotation.open_door");
			return component;
		}else if(name.equals("minecraft:body_rotation_blocked")) {
			AIComponentBodyRotationBlocked component = new AIComponentBodyRotationBlocked("minecraft:body_rotation_blocked");
			return component;
		}else if(name.equals("minecraft:buoyant")) {
			AIComponentBuoyant component = new AIComponentBuoyant("minecraft:buoyant");
			component.applyGravity = getBool(data, "apply_gravity", true);
			component.baseBuoyancy = getFloat(data, "base_buoyancy", 1.0f);
			component.bigWaveProbability = getFloat(data, "big_wave_probability", 0.03f);
			component.bigWaveSpeed = getFloat(data, "big_wave_speed", 10f);
			component.simulateWaves = getBool(data, "simulate_waves", true);
			component.liquidBlocks = getStringList(data, "liquid_blocks");
			return component;
		}else if(name.equals("minecraft:flying_speed")) {
			AIComponentFlyingSpeed component = new AIComponentFlyingSpeed("minecraft:flying_speed");
			component.speed = getFloat(data, "value", 0f);
			return component;
		}else if(name.equals("minecraft:friction_modifier")) {
			AIComponentFrictionModifier component = new AIComponentFrictionModifier("minecraft:friction_modifier");
			component.friction = getFloat(data, "value", 1.0f);
			return component;
		}else if(name.equals("minecraft:ground_offset")) {
			AIComponentGroundOffset component = new AIComponentGroundOffset("minecraft:ground_offset");
			component.offset = getFloat(data, "value", 1f);
			return component;
		}else if(name.equals("minecraft:jump.static")) {
			AIComponentJumpStatic component = new AIComponentJumpStatic("minecraft:jump.static");
			component.jumpPower = getFloat(data, "jump_power", 0.42f);
			return component;
		}else if(name.equals("minecraft:lava_movement")) {
			AIComponentLavaMovement component = new AIComponentLavaMovement("minecraft:lava_movement");
			component.speed = getFloat(data, "value", 0f);
			return component;
		}else if(name.equals("minecraft:movement")) {
			AIComponentMovement component = new AIComponentMovement("minecraft:movement");
			component.speed = getFloat(data, "value", 1f);
			return component;
		}else if(name.equals("minecraft:movement.amphibious")) {
			AIComponentMovementAmphibious component = new AIComponentMovementAmphibious("minecraft:movement.amphibious");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.basic")) {
			AIComponentMovementBasic component = new AIComponentMovementBasic("minecraft:movement.basic");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.fly")) {
			AIComponentMovementFly component = new AIComponentMovementFly("minecraft:movement.fly");
			component.startSpeed = getFloat(data, "start_speed", 0.1f);
			component.speedWhenTurning = getFloat(data, "speed_when_turning", 0.2f);
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.generic")) {
			AIComponentMovementGeneric component = new AIComponentMovementGeneric("minecraft:movement.generic");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.glide")) {
			AIComponentMovementGlide component = new AIComponentMovementGlide("minecraft:movement.glide");
			component.startSpeed = getFloat(data, "start_speed", 0.1f);
			component.speedWhenTurning = getFloat(data, "speed_when_turning", 0.2f);
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.hover")) {
			AIComponentMovementHover component = new AIComponentMovementHover("minecraft:movement.hover");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.jump")) {
			AIComponentMovementJump component = new AIComponentMovementJump("minecraft:movement.jump");
			component.jumpDelayStart = getMin(data, "jump_delay", 0f);
			component.jumpDelayEnd = getMax(data, "jump_delay", 0f);
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:rail_movement")) {
			AIComponentMovementRail component = new AIComponentMovementRail("minecraft:rail_movement");
			component.maxSpeed = getFloat(data, "max_speed", 0.4f);
			return component;
		}else if(name.equals("minecraft:movement.skip")) {
			AIComponentMovementSkip component = new AIComponentMovementSkip("minecraft:movement.skip");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			return component;
		}else if(name.equals("minecraft:movement.sway")) {
			AIComponentMovementSway component = new AIComponentMovementSway("minecraft:movement.sway");
			component.maxTurn = getFloat(data, "max_turn", 30f);
			component.swayAmplitude = getFloat(data, "sway_amplitude", 0.05f);
			component.swayFrequency = getFloat(data, "sway_frequency", 0.5f);
			return component;
		}else if(name.equals("minecraft:physics")) {
			AIComponentPhysics component = new AIComponentPhysics("minecraft:physics");
			component.hasCollision = getBool(data, "has_collision", true);
			component.hasGravity = getBool(data, "has_gravity", true);
			component.pushTowardsClosestSpace = getBool(data, "push_towards_closest_space", false);
			return component;
		}else if(name.equals("minecraft:push_through")) {
			AIComponentPushThrough component = new AIComponentPushThrough("minecraft:push_through");
			component.distance = getFloat(data, "value", 0f);
			return component;
		}else if(name.equals("minecraft:teleport")) {
			AIComponentTeleport component = new AIComponentTeleport("minecraft:teleport");
			component.darkTeleportChance = getFloat(data, "dark_teleport_chance", 0.01f);
			component.lightTeleportChance = getFloat(data, "light_teleport_chance", 0.01f);
			component.maxRandomTeleportTime = getFloat(data, "max_random_teleport_time", 20f);
			component.minRandomTeleportTime = getFloat(data, "min_random_teleport_time", 0f);
			component.randomTeleportCubeWidth = getX(data, "random_teleport_cube", 32f);
			component.randomTeleportCubeHeight = getY(data, "random_teleport_cube", 16f);
			component.randomTeleportCubeDepth = getZ(data, "random_teleport_cube", 32f);
			component.randomTeleports = getBool(data, "random_teleports", true);
			component.targetDistance = getFloat(data, "target_distance", 16f);
			component.targetTeleportChance = getFloat(data, "target_teleport_chance", 1f);
			return component;
		}else if(name.equals("minecraft:underwater_movement")) {
			AIComponentUnderwaterMovement component = new AIComponentUnderwaterMovement("minecraft:underwater_movement");
			component.speed = getFloat(data, "value", 0f);
			return component;
		}else if(name.equals("minecraft:water_movement")) {
			AIComponentWaterMovement component = new AIComponentWaterMovement("minecraft:water_movement");
			component.dragFactor = getFloat(data, "drag_factor", 0.8f);
			return component;
		}else if(name.equals("minecraft:ageable")) {
			AIComponentAgeable component = new AIComponentAgeable("minecraft:ageable");
			component.growUpDuration = getFloat(data, "duration", 1200f);
			component.growUpEvent = getEvent(data, "grow_up");
			return component;
		}else if(name.equals("minecraft:can_climb")) {
			AIComponentCanClimb component = new AIComponentCanClimb("minecraft:can_climb");
			return component;
		}else if(name.equals("minecraft:can_fly")) {
			AIComponentCanFly component = new AIComponentCanFly("minecraft:can_fly");
			return component;
		}else if(name.equals("minecraft:color")) {
			AIComponentColor component = new AIComponentColor("minecraft:color");
			component.color = getInt(data, "value", 0);
			return component;
		}else if(name.equals("minecraft:color2")) {
			AIComponentColor2 component = new AIComponentColor2("minecraft:color2");
			component.color = getInt(data, "value", 0);
			return component;
		}else if(name.equals("minecraft:collision_box")) {
			AIComponentCollisionBox component = new AIComponentCollisionBox("minecraft:collision_box");
			component.width = getFloat(data, "width", 1f);
			component.height = getFloat(data, "height", 1f);
			return component;
		}else if(name.equals("minecraft:is_baby")) {
			AIComponentIsBaby component = new AIComponentIsBaby("minecraft:is_baby");
			return component;
		}else if(name.equals("minecraft:is_charged")) {
			AIComponentIsCharged component = new AIComponentIsCharged("minecraft:is_charged");
			return component;
		}else if(name.equals("minecraft:is_chested")) {
			AIComponentIsChested component = new AIComponentIsChested("minecraft:is_chested");
			return component;
		}else if(name.equals("minecraft:is_ignited")) {
			AIComponentIsIgnited component = new AIComponentIsIgnited("minecraft:is_ignited");
			return component;
		}else if(name.equals("minecraft:is_saddled")) {
			AIComponentIsSaddled component = new AIComponentIsSaddled("minecraft:is_saddled");
			return component;
		}else if(name.equals("minecraft:is_shaking")) {
			AIComponentIsShaking component = new AIComponentIsShaking("minecraft:is_shaking");
			return component;
		}else if(name.equals("minecraft:is_sheared")) {
			AIComponentIsSheared component = new AIComponentIsSheared("minecraft:is_sheared");
			return component;
		}else if(name.equals("minecraft:is_stackable")) {
			AIComponentIsStackable component = new AIComponentIsStackable("minecraft:is_stackable");
			return component;
		}else if(name.equals("minecraft:is_stunned")) {
			AIComponentIsStunned component = new AIComponentIsStunned("minecraft:is_stunned");
			return component;
		}else if(name.equals("minecraft:is_tamed")) {
			AIComponentIsTamed component = new AIComponentIsTamed("minecraft:is_tamed");
			return component;
		}else if(name.equals("minecraft:mark_variant")) {
			AIComponentMarkVariant component = new AIComponentMarkVariant("minecraft:mark_variant");
			component.variant = getInt(data, "value", 0);
			return component;
		}else if(name.equals("minecraft:out_of_control")) {
			AIComponentOutOfControl component = new AIComponentOutOfControl("minecraft:out_of_control");
			return component;
		}else if(name.equals("minecraft:pushable")) {
			AIComponentPushable component = new AIComponentPushable("minecraft:pushable");
			component.isPushable = getBool(data, "is_pushable", true);
			component.isPushableByPiston = getBool(data, "is_pushable_by_piston", true);
			return component;
		}else if(name.equals("minecraft:rideable")) {
			AIComponentRideable component = new AIComponentRideable("minecraft:rideable");
			component.familyTypes = getStringList(data, "family_types");
			component.passengerMaxWidth = getFloat(data, "passenger_max_width", 0f);
			component.pullInEntities = getBool(data, "pull_in_entities", false);
			int seatCount = getInt(data, "seat_count", 1);
			if(data.has("seats")) {
				JsonElement seatsEl = data.get("seats");
				if(seatsEl.isJsonArray()) {
					for(JsonElement el : seatsEl.getAsJsonArray().asList()) {
						JsonObject obj = el.getAsJsonObject();
						Seat seat = new Seat();
						seat.lockRiderRotation = getFloat(obj, "lock_rider_rotation", 181f);
						seat.maxRiderCount = getInt(obj, "max_rider_count", seatCount);
						seat.minRiderCount = getInt(obj, "min_rider_count", 0);
						seat.posX = getX(obj, "position", 0);
						seat.posY = getY(obj, "position", 0);
						seat.posZ = getZ(obj, "position", 0);
						seat.rotateRiderBy = getMolang(obj, "rotate_rider_by", "0");
						component.seats.add(seat);
					}
				}else if(seatsEl.isJsonObject()) {
					JsonObject obj = seatsEl.getAsJsonObject();
					Seat seat = new Seat();
					seat.lockRiderRotation = getFloat(obj, "lock_rider_rotation", 181f);
					seat.maxRiderCount = getInt(obj, "max_rider_count", seatCount);
					seat.minRiderCount = getInt(obj, "min_rider_count", 0);
					seat.posX = getX(obj, "position", 0);
					seat.posY = getY(obj, "position", 0);
					seat.posZ = getZ(obj, "position", 0);
					seat.rotateRiderBy = getMolang(obj, "rotate_rider_by", "0");
					component.seats.add(seat);
				}
			}
			return component;
		}else if(name.equals("minecraft:scale")) {
			AIComponentScale component = new AIComponentScale("minecraft:scale");
			component.scale = getFloat(data, "value", 1f);
			return component;
		}else if(name.equals("minecraft:scale_by_age")) {
			AIComponentScaleByAge component = new AIComponentScaleByAge("minecraft:scale_by_age");
			component.startScale = getFloat(data, "start_scale", 1f);
			component.endScale = getFloat(data, "end_scale", 1f);
			return component;
		}else if(name.equals("minecraft:sittable")) {
			AIComponentSittable component = new AIComponentSittable("minecraft:sittable");
			component.sitEvent = getEvent(data, "sit_event");
			component.standEvent = getEvent(data, "stand_event");
		}else if(name.equals("minecraft:skin_id")) {
			AIComponentSkinId component = new AIComponentSkinId("minecraft:skin_id");
			component.skinId = getInt(data, "value", 0);
			return component;
		}else if(name.equals("minecraft:type_family")) {
			AIComponentTypeFamily component = new AIComponentTypeFamily("minecraft:type_family");
			component.families = getStringList(data, "family");
			return component;
		}else if(name.equals("minecraft:variable_max_auto_step")) {
			AIComponentVariableMaxAutoStep component = new AIComponentVariableMaxAutoStep("minecraft:variable_max_auto_step");
			component.baseHeight = getFloat(data, "base_value", 0.5625f);
			component.jumpPreventedHeight = getFloat(data, "jump_prevented_value", 0.5625f);
			return component;
		}else if(name.equals("minecraft:variant")) {
			AIComponentVariant component = new AIComponentVariant("minecraft:variant");
			component.variant = getInt(data, "value", 0);
			return component;
		}
		
		return null;
	}
	
	private float getFloat(JsonObject data, String name, float defaultValue) {
		if(data.has(name) && data.get(name).isJsonPrimitive())
			return data.get(name).getAsFloat();
		return defaultValue;
	}
	
	private int getInt(JsonObject data, String name, int defaultValue) {
		if(data.has(name) && data.get(name).isJsonPrimitive())
			return data.get(name).getAsInt();
		return defaultValue;
	}
	
	private boolean getBool(JsonObject data, String name, boolean defaultValue) {
		if(data.has(name) && data.get(name).isJsonPrimitive())
			return data.get(name).getAsBoolean();
		return defaultValue;
	}
	
	private String getString(JsonObject data, String name, String defaultValue) {
		if(data.has(name) && data.get(name).isJsonPrimitive())
			return data.get(name).getAsString();
		return defaultValue;
	}
	
	private float getMin(JsonObject data, String name, float defaultValue) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonPrimitive())
				return el.getAsFloat();
			else if(el.isJsonArray()) {
				if(el.getAsJsonArray().size() > 0)
					return el.getAsJsonArray().get(0).getAsFloat();
			}
		}
		return defaultValue;
	}
	
	private float getMax(JsonObject data, String name, float defaultValue) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonPrimitive())
				return el.getAsFloat();
			else if(el.isJsonArray()) {
				if(el.getAsJsonArray().size() == 1)
					return el.getAsJsonArray().get(0).getAsFloat();
				else if(el.getAsJsonArray().size() >= 2)
					return el.getAsJsonArray().get(1).getAsFloat();
			}
		}
		return defaultValue;
	}
	
	private float getX(JsonObject data, String name, float defaultValue) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonPrimitive())
				return el.getAsFloat();
			else if(el.isJsonArray()) {
				if(el.getAsJsonArray().size() >= 3)
					return el.getAsJsonArray().get(0).getAsFloat();
			}
		}
		return defaultValue;
	}
	
	private float getY(JsonObject data, String name, float defaultValue) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonPrimitive())
				return el.getAsFloat();
			else if(el.isJsonArray()) {
				if(el.getAsJsonArray().size() >= 3)
					return el.getAsJsonArray().get(1).getAsFloat();
			}
		}
		return defaultValue;
	}
	
	private float getZ(JsonObject data, String name, float defaultValue) {
		if(data.has(name)) {
			JsonElement el = data.get(name);
			if(el.isJsonPrimitive())
				return el.getAsFloat();
			else if(el.isJsonArray()) {
				if(el.getAsJsonArray().size() >= 3)
					return el.getAsJsonArray().get(2).getAsFloat();
			}
		}
		return defaultValue;
	}
	
	private EntityFilter getFilter(JsonObject data, String name) {
		if(data.has(name)) {
			return new EntityFilterMolang(MolangParser.parse(parseFilterPart(data.get(name))));
		}
		return new EntityFilterMolang(MolangParser.parse("true"));
	}
	
	private String parseFilterPart(JsonElement element) {
		if(element.isJsonArray()) {
			String code = "";
			for(JsonElement el : element.getAsJsonArray().asList()) {
				if(!code.isEmpty())
					code += " && ";
				code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
			}
			return code;
		} else if(element.isJsonObject()) {
			JsonObject data = element.getAsJsonObject();
		
			if(data.has("all_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("all_of")) {
					if(!code.isEmpty())
						code += " && ";
					code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("any_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("any_of")) {
					if(!code.isEmpty())
						code += " || ";
					code += "(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("none_of")) {
				String code = "";
				for(JsonElement el : data.getAsJsonArray("none_of")) {
					if(!code.isEmpty())
						code += " && ";
					code += "!(" + parseFilterPart(el.getAsJsonObject()) + ")";
				}
				return code;
			}else if(data.has("filters")) {
				return parseFilterPart(data.get("filters"));
			}else {
				// TODO: Support subject
				
				String query = "false";
				String operator = "==";
				@SuppressWarnings("unused")
				String subject = "self";
				String value = "true";
				String domain = null;
				
				String testStr = data.get("test").getAsString();
				@SuppressWarnings("unused")
				String subjectStr = "self";
				if(data.has("subject"))
					subjectStr = data.get("subject").getAsString();
				String operatorStr = "==";
				if(data.has("operator"))
					operatorStr = data.get("operator").getAsString();
				if(data.has("domain"))
					domain = data.get("domain").getAsString();
				if(data.has("value")) {
					JsonPrimitive prim = data.getAsJsonPrimitive("value");
					if(prim.isBoolean())
						value = prim.getAsBoolean() ? "true" : "false";
					else if(prim.isNumber())
						value = Float.toString(prim.getAsFloat());
					else if(prim.isString())
						value = "'" + prim.getAsString() + "'";
				}
				
				if(operatorStr.equalsIgnoreCase("equals"))
					operator = "==";
				else if(operatorStr.equalsIgnoreCase("not"))
					operator = "!=";
				else if(operatorStr.equals("!="))
					operator = "!=";
				else if(operatorStr.equals("<"))
					operator = "<";
				else if(operatorStr.equals("<="))
					operator = "<=";
				else if(operatorStr.equals("<>"))
					operator = "!=";
				else if(operatorStr.equals("="))
					operator = "==";
				else if(operatorStr.equals("=="))
					operator = "==";
				else if(operatorStr.equals(">"))
					operator = ">";
				else if(operatorStr.equals(">="))
					operator = ">=";
				
				if(testStr.equals("actor_health")) 
					query = "query.health";
				else if(testStr.equals("bool_property"))
					query = "query.property";
				else if(testStr.equals("clock_time"))
					query = "query.time_of_day";
				else if(testStr.equals("distance_to_nearest_player"))
					query = "query.distance_from_camera";
				else if(testStr.equals("enum_property"))
					query = "query.property";
				else if(testStr.equals("float_property"))
					query = "query.property";
				else if(testStr.equals("has_biome_tag")) {
					query = "query.has_biome_tag";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("has_component")) {
					query = "query.has_component";
					domain = value;
					value = "true";
				}else if(testStr.equals("has_property")) {
					query = "query.has_property";
					domain = value;
					value = "true";
				}else if(testStr.equals("has_tag")) {
					query = "query.any_tag";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("has_target"))
					query = "query.has_target";
				else if(testStr.equals("hourly_clock_time"))
					query = "(query.time_of_day * 24000)";
				else if(testStr.equals("int_property"))
					query = "query.property";
				else if(testStr.equals("is_baby"))
					query = "query.is_baby";
				else if(testStr.equals("has_equipment")) {
					query = "query.is_item_name_any";
					if(domain.equals("any"))
						domain = "'slot.any'";
					else if(domain.equals("armor"))
						domain = "'slot.armor'";
					else if(domain.equals("feet"))
						domain = "'slot.armor.feet'";
					else if(domain.equals("hand"))
						domain = "'slot.weapon_mainhand'";
					else if(domain.equals("head"))
						domain = "'slot.armor.head'";
					else if(domain.equals("inventory"))
						domain = "'slot.inventory'";
					else if(domain.equals("leg"))
						domain = "'slot.armor.legs'";
					else if(domain.equals("torso"))
						domain = "'slot.armor.chest'";
					domain += ", -1, " + value;
					value = "true";
				}else if(testStr.equals("has_component")) {
					query = "query.has_component";
					domain = value;
					value = "true";
				}else if(testStr.equals("in_block")) {
					query = "query.get_block_name";
					if(value.equals("true")) {
						if(operator.equals("==")) {
							operator = "!=";
						}else {
							operator = "==";
						}
						value = "\"minecraft:air\"";
					}else if(value.equals("false")) {
						value = "\"minecraft:air\"";
					}
				}else if(testStr.equals("is_biome")) {
					query = "query.has_biome";
					if(!value.contains(":"))
						value = "'minecraft:" + value.substring(1);
					domain = value;
					value = "true";
				}else if(testStr.equals("is_block")) {
					query = "query.get_block_name";
				}else if(testStr.equals("is_daytime")) {
					query = "math.mod(query.time_of_day - 0.25, 1)";
					operator = "<=";
					value = "0.5";
				}else if(testStr.equals("is_family")) {
					query = "query.has_any_family";
					domain = value;
					value = "true";
				}else if(testStr.equals("is_mark_variant")) {
					query = "query.mark_variant";
				}else if(testStr.equals("is_moving")) {
					query = "query.is_moving";
				}else if(testStr.equals("is_skin_id")) {
					query = "query.skin_id";
				}else if(testStr.equals("is_variant")) {
					query = "query.variant";
				}else if(testStr.equals("moon_intensity")) {
					query = "query.moon_brightness";
				}else if(testStr.equals("moon_phase")) {
					query = "query.moon_phase";
				}else if(testStr.equals("on_fire")) {
					query = "query.is_on_fire";
				}else if(testStr.equals("on_ground")) {
					query = "query.is_on_ground";
				}else if(testStr.equals("random_chance")) {
					query = "math.random_integer";
					domain = "0, " + value;
					value = "0";
				}else if(testStr.equals("in_contact_with_water"))
					query = "query.is_in_contact_with_water";
				else if(testStr.equals("in_lava"))
					query = "query.is_in_lava";
				else if(testStr.equals("in_water_or_rain"))
					query = "query.is_in_water_or_rain";
				else if(testStr.equals("in_water"))
					query = "query.is_in_water";
				
				String code = query;
				if(domain != null && !query.equals("false"))
					code += "(" + domain + ")";
				code += " " + operator + " ";
				code += value;
				return code;
			}
		}
		return "";
	}
	
	private EntityEvent getEvent(JsonObject data, String name) {
		if(data.has(name)) {
			EntityEvent event = getEvent(data.get(name));
			event.setName(name);
			return event;
		}
		return new EntityEvent(name, null);
	}
	
	private EntityEvent getEvent(JsonElement data) {
		if(data.isJsonObject()) {
			JsonObject obj = data.getAsJsonObject();
			EntityEventComponent component = null;
			if(obj.has("event")) {
				String eventName = obj.get("event").getAsString();
				component = new EntityEventComponentTriggerEvent();
				((EntityEventComponentTriggerEvent) component).events.add(eventName);
			}else if(obj.has("add")) {
				component = new EntityEventComponentAddGroup();
				((EntityEventComponentAddGroup) component).groups = getStringList(obj.getAsJsonObject("add"), "component_groups");
			}else if(obj.has("remove")) {
				component = new EntityEventComponentRemoveGroup();
				((EntityEventComponentRemoveGroup) component).groups = getStringList(obj.getAsJsonObject("remove"), "component_groups");
			}else if(obj.has("randomize")) {
				component = new EntityEventComponentRandomize();
				for(JsonElement el : obj.getAsJsonArray("randomize").asList()) {
					EntityEvent event = getEvent(el);
					float weight = 1f;
					if(el.isJsonObject()) {
						JsonObject obj2 = el.getAsJsonObject();
						if(obj2.has("weight"))
							weight = obj2.get("weight").getAsFloat();
					}
					((EntityEventComponentRandomize) component).components.add(event.getComponent());
					((EntityEventComponentRandomize) component).weights.add(weight);
				}
			}else if(obj.has("sequence")) {
				component = new EntityEventComponentSequence();
				for(JsonElement el : obj.getAsJsonArray("sequence").asList()) {
					EntityEvent event = getEvent(el);
					((EntityEventComponentSequence) component).components.add(event.getComponent());
				}
			}else if(obj.has("trigger")) {
				component = new EntityEventComponentSequence();
				EntityEvent event = getEvent(obj.get("trigger"));
				((EntityEventComponentSequence) component).components.add(event.getComponent());
			}else if(obj.has("set_property")) {
				component = new EntityEventComponentSetProperty();
				for(Entry<String, JsonElement> entry : obj.getAsJsonObject("set_property").entrySet()) {
					String value = "";
					if(entry.getValue().isJsonPrimitive()) {
						JsonPrimitive prim = entry.getValue().getAsJsonPrimitive();
						if(prim.isBoolean())
							value = prim.getAsBoolean() ? "true" : "false";
						else if(prim.isNumber())
							value = Float.toString(prim.getAsFloat());
						else if(prim.isString())
							value = prim.getAsString();
					}
					((EntityEventComponentSetProperty) component).properties.put(entry.getKey(), MolangParser.parse(value));
				}
			}
			if(component != null) {
				String target = "self";
				if(obj.has("target"))
					target = obj.get("target").getAsString();
				if(obj.has("filters"))
					component.filter = getFilter(obj, "filters");
				component.target = target;
			}
			return new EntityEvent("", component);
		}else if(data.isJsonArray()) {
			EntityEventComponentSequence component = new EntityEventComponentSequence();
			for(JsonElement el : data.getAsJsonArray().asList()) {
				EntityEvent event = getEvent(el);
				if(event.getComponent() != null)
					component.components.add(component);
			}
			component.target = "self";
			return new EntityEvent("", component);
		}else if(data.isJsonPrimitive()) {
			String eventName = data.getAsString();
			EntityEventComponentTriggerEvent component = new EntityEventComponentTriggerEvent();
			component.target = "self";
			component.events.add(eventName);
			return new EntityEvent("", component);
		}
		return new EntityEvent("", null);
	}
	
	private List<String> getStringList(JsonObject data, String name){
		if(data.has(name)) {
			List<String> res = new ArrayList<String>();
			JsonElement array = data.get(name);
			if(array.isJsonArray()) {
				for(JsonElement el : array.getAsJsonArray().asList())
					if(el.isJsonPrimitive())
						res.add(el.getAsString());
			}else if(array.isJsonPrimitive()) {
				res.add(array.getAsString());
			}
			return res;
		}
		return new ArrayList<String>();
	}
	
	private MolangScript getMolang(JsonObject data, String name, String defaultValue) {
		if(data.has(name)) {
			String code = "0";
			if(data.get(name).isJsonPrimitive()) {
				JsonPrimitive prim = data.get(name).getAsJsonPrimitive();
				if(prim.isBoolean())
					code = prim.getAsBoolean() ? "true" : "false";
				else if(prim.isNumber())
					code = Float.toString(prim.getAsFloat());
				else if(prim.isString())
					code = prim.getAsString();
			}
			return MolangParser.parse(code);
		}
		return MolangParser.parse(defaultValue);
	}
	
	@Override
	public void setup(EntityAI ai) {
		for(AIComponentGroup componentGroup : componentGroups)
			ai.getComponentGroups().add(componentGroup.copy());
		for(EntityEvent event : events)
			ai.getEvents().add(event);
		ai.enableComponentGroup("");
		NbtTagList definitionsTag = (NbtTagList) ai.getEntity().getProperties().get("definitions");
		if(definitionsTag != null) {
			for(NbtTag tag : definitionsTag.getData()) {
				String groupName = ((NbtTagString) tag).getData();
				if(groupName.startsWith("+"))
					ai.enableComponentGroup(groupName.substring(1));
				else if(groupName.startsWith("-"))
					ai.disableComponentGroup(groupName.substring(1));
				else
					ai.enableComponentGroup(groupName);
			}
		}
		ai.setAnimationController(animationController);
		for(Entry<String, String> entry : animations.entrySet()) {
			Animation animation = ResourcePacks.getAnimation(entry.getValue());
			if(animation != null)
				ai.getAnimations().put(entry.getKey(), animation);
		}
		ai.setInitMolangScripts(initMolang);
		ai.setScaleXExpression(scaleXExpression);
		ai.setScaleYExpression(scaleYExpression);
		ai.setScaleZExpression(scaleZExpression);
	}

}
