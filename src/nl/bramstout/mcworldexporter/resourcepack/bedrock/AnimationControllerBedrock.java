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

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Pair;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.resourcepack.AnimationController;

public class AnimationControllerBedrock extends AnimationController{

	public AnimationControllerBedrock(String name, JsonObject data) {
		super(name);
		if(data.has("states")) {
			for(Entry<String, JsonElement> entry : data.getAsJsonObject("states").entrySet()) {
				parseState(entry.getKey(), entry.getValue().getAsJsonObject());
			}
		}
		if(data.has("initial_state")) {
			this.defaultState = data.get("initial_state").getAsString();
		}
	}
	
	private void parseState(String name, JsonObject data) {
		float blendTransition = 0f;
		if(data.has("blend_transition"))
			blendTransition = data.get("blend_transition").getAsFloat();
		AnimationState state = new AnimationState(name, blendTransition);
		if(data.has("variables")) {
			JsonObject obj = data.getAsJsonObject("variables");
			for(Entry<String, JsonElement> el : obj.entrySet()) {
				AnimationVariableRemap variable = new AnimationVariableRemap(el.getKey());
				JsonObject variableObj = el.getValue().getAsJsonObject();
				if(variableObj.has("input"))
					variable.inputExpression = MolangParser.parse(variableObj.get("input").getAsString());
				if(variableObj.has("remap_curve")) {
					for(Entry<String, JsonElement> entry : variableObj.getAsJsonObject("remap_curve").entrySet()) {
						try {
							Float inVal = Float.valueOf(entry.getKey());
							Float outVal = Float.valueOf(entry.getValue().getAsFloat());
							variable.remapCurve.add(new Pair<Float, Float>(inVal, outVal));
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
				state.getVariables().add(variable);
			}
		}
		if(data.has("animations")) {
			JsonArray array = data.getAsJsonArray("animations");
			for(JsonElement el : array.asList()) {
				if(el.isJsonPrimitive()) {
					state.addAnimation(el.getAsString(), MolangParser.parse("1.0"));
				}else if(el.isJsonObject()) {
					for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
						state.addAnimation(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
					}
				}
			}
		}
		if(data.has("transitions")) {
			JsonArray array = data.getAsJsonArray("transitions");
			for(JsonElement el : array.asList()) {
				if(el.isJsonPrimitive()) {
					state.addTransition(el.getAsString(), MolangParser.parse("1.0"));
				}else if(el.isJsonObject()) {
					for(Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
						state.addTransition(entry.getKey(), MolangParser.parse(entry.getValue().getAsString()));
					}
				}
			}
		}
		if(data.has("on_entry")) {
			JsonElement el = data.getAsJsonArray("on_entry");
			if(el.isJsonPrimitive()) {
				String code = el.getAsString();
				if(code.startsWith("@s")) {
					state.getOnEntryEvents().add(new AnimationEventTriggerEvent(0f, code.substring(3)));
				}else if(code.startsWith("/")){
					// Ignore commands for now
				}else {
					state.getOnEntryEvents().add(new AnimationEventMolang(0f, MolangParser.parse(code)));
				}
			}else if(el.isJsonArray()) {
				for(JsonElement el2 : el.getAsJsonArray().asList()) {
					String code = el2.getAsString();
					if(code.startsWith("@s")) {
						state.getOnEntryEvents().add(new AnimationEventTriggerEvent(0f, code.substring(3)));
					}else if(code.startsWith("/")){
						// Ignore commands for now
					}else {
						state.getOnEntryEvents().add(new AnimationEventMolang(0f, MolangParser.parse(code)));
					}
				}
			}
		}
		if(data.has("on_exit")) {
			JsonElement el = data.getAsJsonArray("on_exit");
			if(el.isJsonPrimitive()) {
				String code = el.getAsString();
				if(code.startsWith("@s")) {
					state.getOnEntryEvents().add(new AnimationEventTriggerEvent(0f, code.substring(3)));
				}else if(code.startsWith("/")){
					// Ignore commands for now
				}else {
					state.getOnEntryEvents().add(new AnimationEventMolang(0f, MolangParser.parse(code)));
				}
			}else if(el.isJsonArray()) {
				for(JsonElement el2 : el.getAsJsonArray().asList()) {
					String code = el2.getAsString();
					if(code.startsWith("@s")) {
						state.getOnEntryEvents().add(new AnimationEventTriggerEvent(0f, code.substring(3)));
					}else if(code.startsWith("/")){
						// Ignore commands for now
					}else {
						state.getOnEntryEvents().add(new AnimationEventMolang(0f, MolangParser.parse(code)));
					}
				}
			}
		}
		this.states.add(state);
	}

}
