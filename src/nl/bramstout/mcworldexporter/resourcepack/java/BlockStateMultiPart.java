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

import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class BlockStateMultiPart extends BlockStatePart{
	
	private JsonObject check;
	private boolean _needsConnectionInfo;
	
	public BlockStateMultiPart(JsonElement data, boolean doubleSided) {
		super();
		
		_needsConnectionInfo = false;
		check = null;
		if(data.getAsJsonObject().has("when")) {
			check = data.getAsJsonObject().get("when").getAsJsonObject();
			for(Entry<String, JsonElement> el : check.entrySet()) {
				if(el.getValue().isJsonPrimitive()) {
					if(el.getKey().startsWith("miex_connect")) {
						_needsConnectionInfo = true;
						break;
					}
				} else if(el.getValue().isJsonArray()) {
					for(JsonElement el2 : el.getValue().getAsJsonArray().asList()) {
						if(el2.isJsonObject()) {
							for(Entry<String, JsonElement> el3 : el2.getAsJsonObject().entrySet()) {
								if(el3.getKey().startsWith("miex_connect")) {
									_needsConnectionInfo = true;
									break;
								}
							}
						}
					}
				}
			}
		}
		
		JsonElement modelData = data.getAsJsonObject().get("apply");
		if(modelData.isJsonArray()) {
			for(JsonElement el : modelData.getAsJsonArray().asList()) {
				int modelId = ModelRegistry.getIdForName(el.getAsJsonObject().get("model").getAsString(), doubleSided);
				int rotX = 0;
				int rotY = 0;
				boolean uvLock = false;
				if(el.getAsJsonObject().has("x"))
					rotX = el.getAsJsonObject().get("x").getAsInt();
				if(el.getAsJsonObject().has("y"))
					rotY = el.getAsJsonObject().get("y").getAsInt();
				if(el.getAsJsonObject().has("uvlock"))
					uvLock = el.getAsJsonObject().get("uvlock").getAsBoolean();
				Model model = new Model(ModelRegistry.getModel(modelId));
				if(rotX != 0 || rotY != 0)
					model.rotate(rotX, rotY, uvLock);
				if(el.getAsJsonObject().has("weight"))
					model.setWeight(el.getAsJsonObject().get("weight").getAsInt());
				models.add(model);
			}
		} else if(modelData.isJsonObject()) {
			int modelId = ModelRegistry.getIdForName(modelData.getAsJsonObject().get("model").getAsString(), doubleSided);
			int rotX = 0;
			int rotY = 0;
			boolean uvLock = false;
			if(modelData.getAsJsonObject().has("x"))
				rotX = modelData.getAsJsonObject().get("x").getAsInt();
			if(modelData.getAsJsonObject().has("y"))
				rotY = modelData.getAsJsonObject().get("y").getAsInt();
			if(modelData.getAsJsonObject().has("uvlock"))
				uvLock = modelData.getAsJsonObject().get("uvlock").getAsBoolean();
			Model model = new Model(ModelRegistry.getModel(modelId));
			if(rotX != 0 || rotY != 0)
				model.rotate(rotX, rotY, uvLock);
			models.add(model);
		}
	}
	
	@Override
	public boolean needsConnectionInfo() {
		return this._needsConnectionInfo;
	}

	@Override
	public boolean usePart(NbtTagCompound properties, int x, int y, int z) {
		if(check == null)
			return true;
		if(check.has("OR")) {
			for(JsonElement checkObj : check.get("OR").getAsJsonArray().asList()) {
				if(testProperties(properties, checkObj.getAsJsonObject(), x, y, z))
					return true;
			}
			return false;
		} else if(check.has("AND")) {
			for(JsonElement checkObj : check.get("AND").getAsJsonArray().asList()) {
				if(!testProperties(properties, checkObj.getAsJsonObject(), x, y, z))
					return false;
			}
			return true;
		} else {
			return testProperties(properties, check, x, y, z);
		}
	}
	
	private boolean testProperties(NbtTagCompound properties, JsonObject checkObject, int x, int y, int z) {
		int numItems = properties.getSize();
		for(int i = 0; i < numItems; ++i) {
			NbtTag tag = properties.get(i);
			if(!checkObject.has(tag.getName()))
				continue;
			String[] values = checkObject.get(tag.getName()).getAsString().split("\\|");
			String propValue = tag.asString();
			
			if(propValue != null) {
				boolean found = false;
				for(String value : values) {
					if(propValue.equals(value)) {
						found = true;
						break;
					} else if((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))) {
						found = true;
						break;
					}
				}
				if(!found)
					return false;
			}
		}
		
		// Check for connection info
		if(needsConnectionInfo()) {
			for(Entry<String, JsonElement> entry : checkObject.entrySet()) {
				if(entry.getKey().startsWith("miex_connect")) {
					// Test the neighbouring block.
					boolean res = testMiExConnection(entry.getKey(), entry.getValue().getAsString(), x, y, z);
					// If it doesn't match, then we shouldn't use this part.
					if(!res)
						return false;
				}
			}
		}
		return true;
	}

}
