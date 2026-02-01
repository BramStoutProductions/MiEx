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

package nl.bramstout.mcworldexporter.modifier;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagShort;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;

public abstract class ModifierNode {

	public static enum ValueType{
		NULL, FLOAT, FLOAT2, FLOAT3, FLOAT4,
		STRING, INT, LONG, BOOL, ARRAY
	}
	
	public static class Value{
		
		private ValueType type;
		private float r;
		private float g;
		private float b;
		private float a;
		private String str;
		private int i;
		private long l;
		private boolean bool;
		private Value[] array;
		
		public Value() {
			this.type = ValueType.NULL;
		}
		public Value(float r) {
			this.type = ValueType.FLOAT;
			this.r = r;
		}
		public Value(float r, float g) {
			this.type = ValueType.FLOAT2;
			this.r = r;
			this.g = g;
		}
		public Value(float r, float g, float b) {
			this.type = ValueType.FLOAT3;
			this.r = r;
			this.g = g;
			this.b = b;
		}
		public Value(float r, float g, float b, float a) {
			this.type = ValueType.FLOAT4;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}
		public Value(double r) {
			this((float)r);
		}
		public Value(double r, double g) {
			this((float)r, (float)g);
		}
		public Value(double r, double g, double b) {
			this((float)r, (float)g, (float)b);
		}
		public Value(double r, double g, double b, double a) {
			this((float)r, (float)g, (float)b, (float)a);
		}
		public Value(String str) {
			this.type = ValueType.STRING;
			this.str = str;
		}
		public Value(int i) {
			this.type = ValueType.INT;
			this.i = i;
		}
		public Value(long l) {
			this.type = ValueType.LONG;
			this.l = l;
		}
		public Value(boolean bool) {
			this.type = ValueType.BOOL;
			this.bool = bool;
		}
		public Value(Value[] array) {
			this.type = ValueType.ARRAY;
			this.array = array;
		}
		public Value(JsonElement el) {
			if(el.isJsonPrimitive()) {
				JsonPrimitive prim = el.getAsJsonPrimitive();
				if(prim.isBoolean()) {
					this.type = ValueType.BOOL;
					this.bool = prim.getAsBoolean();
				}else if(prim.isNumber()) {
					this.type = ValueType.FLOAT;
					this.r = prim.getAsFloat();
				}else if(prim.isString()) {
					this.type = ValueType.STRING;
					this.str = prim.getAsString();
				}
			}else if(el.isJsonArray()) {
				JsonArray array = el.getAsJsonArray();
				this.type = ValueType.ARRAY;
				this.array = new Value[array.size()];
				for(int i = 0; i < array.size(); ++i) {
					this.array[i] = new Value(array.get(i));
				}
			}else if(el.isJsonObject()){
				JsonObject obj = el.getAsJsonObject();
				float r = Float.NaN;
				float g = Float.NaN;
				float b = Float.NaN;
				float a = Float.NaN;
				
				if(obj.has("r"))
					r = obj.get("r").getAsFloat();
				if(obj.has("g"))
					g = obj.get("g").getAsFloat();
				if(obj.has("b"))
					b = obj.get("b").getAsFloat();
				if(obj.has("a"))
					a = obj.get("a").getAsFloat();

				if(obj.has("x"))
					r = obj.get("x").getAsFloat();
				if(obj.has("y"))
					g = obj.get("y").getAsFloat();
				if(obj.has("z"))
					b = obj.get("z").getAsFloat();
				if(obj.has("w"))
					a = obj.get("w").getAsFloat();
				
				this.r = Float.isNaN(r) ? 0f : r;
				this.g = Float.isNaN(g) ? 0f : g;
				this.b = Float.isNaN(b) ? 0f : b;
				this.a = Float.isNaN(a) ? 1f : a;
				if(!Float.isNaN(a))
					this.type = ValueType.FLOAT4;
				else if(!Float.isNaN(b))
					this.type = ValueType.FLOAT3;
				else if(!Float.isNaN(g))
					this.type = ValueType.FLOAT2;
				else if(!Float.isNaN(r))
					this.type = ValueType.FLOAT;
				else
					this.type = ValueType.NULL;
			}else {
				this.type = ValueType.NULL;
			}
		}
		public Value(NbtTag tag) {
			this();
			if(tag instanceof NbtTagByte) {
				this.type = ValueType.INT;
				this.i = tag.asInt();
			}else if(tag instanceof NbtTagShort) {
				this.type = ValueType.INT;
				this.i = tag.asInt();
			}else if(tag instanceof NbtTagInt) {
				this.type = ValueType.INT;
				this.i = tag.asInt();
			}else if(tag instanceof NbtTagLong) {
				this.type = ValueType.LONG;
				this.l = tag.asLong();
			}else if(tag instanceof NbtTagFloat) {
				this.type = ValueType.FLOAT;
				this.r = tag.asFloat();
			}else if(tag instanceof NbtTagDouble) {
				this.type = ValueType.FLOAT;
				this.r = tag.asFloat();
			}else if(tag instanceof NbtTagString) {
				this.type = ValueType.STRING;
				this.str = tag.asString();
			}else if(tag instanceof NbtTagList) {
				this.type = ValueType.ARRAY;
				this.array = new Value[((NbtTagList)tag).getSize()];
				for(int i = 0; i < this.array.length; ++i) {
					this.array[i] = new Value(((NbtTagList)tag).get(i));
				}
			}
		}
		
		public float getR() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return r;
			case INT:
				return i;
			case LONG:
				return l;
			case BOOL:
				return bool ? 1f : 0f;
			case STRING:
				try {
					return Float.parseFloat(str);
				}catch(Exception ex) {}
				return 0;
			case ARRAY:
				if(array.length > 0)
					return array[0].getR();
				return 0;
			}
			return 0;
		}
		
		public float getX() {
			return getR();
		}
		
		public float getG() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
				return r;
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return g;
			case INT:
				return i;
			case LONG:
				return l;
			case BOOL:
				return bool ? 1f : 0f;
			case STRING:
				try {
					return Float.parseFloat(str);
				}catch(Exception ex) {}
				return 0;
			case ARRAY:
				if(array.length > 0)
					return array[Math.min(1, array.length-1)].getR();
				return 0;
			}
			return 0;
		}
		
		public float getY() {
			return getG();
		}
		
		public float getB() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
				return r;
			case FLOAT2:
				return g;
			case FLOAT3:
			case FLOAT4:
				return b;
			case INT:
				return i;
			case LONG:
				return l;
			case BOOL:
				return bool ? 1f : 0f;
			case STRING:
				try {
					return Float.parseFloat(str);
				}catch(Exception ex) {}
				return 0;
			case ARRAY:
				if(array.length > 0)
					return array[Math.min(2, array.length-1)].getR();
				return 0;
			}
			return 0;
		}
		
		public float getZ() {
			return getB();
		}
		
		public float getA() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
				return 1f;
			case FLOAT2:
				return 1f;
			case FLOAT3:
				return 1f;
			case FLOAT4:
				return a;
			case INT:
				return 1f;
			case LONG:
				return 1f;
			case STRING:
				return 1f;
			case BOOL:
				return 1f;
			case ARRAY:
				if(array.length > 3)
					return array[3].getR();
				return 1f;
			}
			return 1f;
		}
		
		public float getW() {
			return getA();
		}
		
		public int getInt() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return (int) r;
			case INT:
				return i;
			case LONG:
				return (int) l;
			case BOOL:
				return bool ? 1 : 0;
			case STRING:
				try {
					return Integer.parseInt(str);
				}catch(Exception ex) {}
				return 0;
			case ARRAY:
				if(array.length > 0)
					return array[0].getInt();
				return 0;
			}
			return 0;
		}
		
		public long getLong() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return (long) r;
			case INT:
				return i;
			case LONG:
				return l;
			case BOOL:
				return bool ? 1 : 0;
			case STRING:
				try {
					return Long.parseLong(str);
				}catch(Exception ex) {}
				return 0;
			case ARRAY:
				if(array.length > 0)
					return array[0].getLong();
				return 0;
			}
			return 0;
		}
		
		public boolean getBool() {
			switch(type) {
			case NULL:
				return false;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return r >= 0.5f;
			case INT:
				return i > 0;
			case LONG:
				return l > 0;
			case BOOL:
				return bool;
			case STRING:
				return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("1");
			case ARRAY:
				if(array.length > 0)
					return array[0].getBool();
				return false;
			}
			return false;
		}
		
		public String getString() {
			switch(type) {
			case NULL:
				return "null";
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				return Float.toString(r);
			case INT:
				return Integer.toString(i);
			case LONG:
				return Long.toString(l);
			case BOOL:
				return bool ? "true" : "false";
			case STRING:
				return str;
			case ARRAY:
				if(array.length > 0)
					return array[0].getString();
				return "";
			}
			return "";
		}
		
		public Value[] getArray() {
			switch(type) {
			case NULL:
				return new Value[0];
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
				return new Value[] { copy() };
			case ARRAY:
				return array;
			}
			return new Value[0];
		}
		
		public Value[] getArrayCopy() {
			switch(type) {
			case NULL:
				return new Value[0];
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
				return new Value[] { copy() };
			case ARRAY:
				Value[] arrayCopy = new Value[array.length];
				for(int i = 0; i < array.length; ++i)
					arrayCopy[i] = array[i].copy();
				return arrayCopy;
			}
			return new Value[0];
		}
		
		public int size() {
			switch(type) {
			case NULL:
				return 0;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
				return 1;
			case ARRAY:
				return array.length;
			}
			return 0;
		}
		
		public Value get(int index) {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
				if(index == 0)
					return this;
				return new Value();
			case ARRAY:
				if(index >= 0 && index < array.length)
					return array[index];
				return new Value();
			}
			return new Value();
		}
		
		public void setR(float r) {
			switch(type) {
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				this.r = r;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT;
				this.r = r;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setX(float x) {
			setR(x);
		}
		
		public void setG(float g) {
			switch(type) {
			case FLOAT:
				this.type = ValueType.FLOAT2;
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				this.g = g;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT2;
				this.g = g;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setY(float y) {
			setG(y);
		}
		
		public void setB(float b) {
			switch(type) {
			case FLOAT:
			case FLOAT2:
				this.type = ValueType.FLOAT3;
			case FLOAT3:
			case FLOAT4:
				this.b = b;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT3;
				this.b = b;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setZ(float z) {
			setB(z);
		}
		
		public void setA(float a) {
			switch(type) {
			case FLOAT:
			case FLOAT2:
			case FLOAT3:
				this.type = ValueType.FLOAT4;
			case FLOAT4:
				this.a = a;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT4;
				this.a = a;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setW(float w) {
			setA(w);
		}
		
		public void setRG(float r, float g) {
			switch(type) {
			case FLOAT:
				this.type = ValueType.FLOAT2;
			case FLOAT2:
			case FLOAT3:
			case FLOAT4:
				this.r = r;
				this.g = g;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT2;
				this.r = r;
				this.g = g;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setXY(float x, float y) {
			setRG(x, y);
		}
		
		public void setRGB(float r, float g, float b) {
			switch(type) {
			case FLOAT:
			case FLOAT2:
				this.type = ValueType.FLOAT3;
			case FLOAT3:
			case FLOAT4:
				this.r = r;
				this.g = g;
				this.b = b;
				break;
			case NULL:
			case INT:
			case LONG:
			case BOOL:
			case STRING:
			case ARRAY:
				this.type = ValueType.FLOAT3;
				this.r = r;
				this.g = g;
				this.b = b;
				this.str = null;
				this.array = null;
				break;
			}
		}
		
		public void setXYZ(float x, float y, float z) {
			setRGB(x, y, z);
		}
		
		public void setRGBA(float r, float g, float b, float a) {
			this.type = ValueType.FLOAT4;
			this.r = r;
			this.g = g;
			this.g = g;
			this.a = a;
			this.str = null;
			this.array = null;
		}
		
		public void setXYZW(float x, float y, float z, float w) {
			setXYZW(x, y, z, w);
		}
		
		public void setInt(int i) {
			this.type = ValueType.INT;
			this.i = i;
			this.str = null;
			this.array = null;
		}
		
		public void setLong(long l) {
			this.type = ValueType.LONG;
			this.l = l;
			this.str = null;
			this.array = null;
		}
		
		public void setString(String str) {
			if(str == null)
				str = "";
			this.type = ValueType.STRING;
			this.str = str;
			this.array = null;
		}
		
		public void setArray(Value[] array) {
			if(array == null)
				array = new Value[0];
			this.type = ValueType.ARRAY;
			this.array = array;
			this.str = null;
		}
		
		public void setArrayCopy(Value[] array) {
			if(array == null)
				array = new Value[0];
			this.type = ValueType.ARRAY;
			this.array = new Value[array.length];
			for(int i = 0; i < array.length; ++i)
				this.array[i] = array[i].copy();
			this.str = null;
		}
		
		public void setSize(int size) {
			if(type != ValueType.ARRAY)
				return;
			int oldSize = this.array.length;
			this.array = Arrays.copyOf(this.array, size);
			// If we made it bigger, fill with NULL values.
			for(int i = oldSize; i < size; ++i)
				this.array[i] = new Value();
		}
		
		public void set(Value other) {
			this.type = other.type;
			this.r = other.r;
			this.g = other.g;
			this.b = other.b;
			this.a = other.a;
			this.i = other.i;
			this.l = other.l;
			this.bool = other.bool;
			this.str = other.str;
			this.array = null;
			if(other.array != null) {
				this.array = new Value[other.array.length];
				for(int i = 0; i < other.array.length; ++i)
					this.array[i] = other.array[i].copy();
			}
		}
		
		public void set(int index, Value value) {
			if(type != ValueType.ARRAY)
				return;
			if(index < 0 || index >= array.length)
				return;
			this.array[index].set(value);
		}
		
		public void set(JsonElement el) {
			this.str = null;
			this.array = null;
			if(el.isJsonPrimitive()) {
				JsonPrimitive prim = el.getAsJsonPrimitive();
				if(prim.isBoolean()) {
					this.type = ValueType.BOOL;
					this.bool = prim.getAsBoolean();
				}else if(prim.isNumber()) {
					this.type = ValueType.FLOAT;
					this.r = prim.getAsFloat();
				}else if(prim.isString()) {
					this.type = ValueType.STRING;
					this.str = prim.getAsString();
				}
			}else if(el.isJsonArray()) {
				JsonArray array = el.getAsJsonArray();
				this.type = ValueType.ARRAY;
				this.array = new Value[array.size()];
				for(int i = 0; i < array.size(); ++i) {
					this.array[i] = new Value(array.get(i));
				}
			}else {
				this.type = ValueType.NULL;
			}
		}
		
		public Value castToType(ValueType type) {
			switch(type) {
			case NULL:
				return new Value();
			case FLOAT:
				return new Value(getR());
			case FLOAT2:
				return new Value(getR(), getG());
			case FLOAT3:
				return new Value(getR(), getG(), getB());
			case FLOAT4:
				return new Value(getR(), getG(), getB(), getA());
			case INT:
				return new Value(getInt());
			case LONG:
				return new Value(getLong());
			case BOOL:
				return new Value(getBool());
			case STRING:
				return new Value(getString());
			case ARRAY:
				return new Value(getArrayCopy());
			}
			return new Value();
		}
		
		public Value copy() {
			return castToType(type);
		}
		
		public ValueType getType() {
			return type;
		}
		
		public boolean isNull() {
			return type == ValueType.NULL;
		}
		
		public boolean isFloat() {
			return type == ValueType.FLOAT;
		}
		
		public boolean isFloat2() {
			return type == ValueType.FLOAT2;
		}
		
		public boolean isFloat3() {
			return type == ValueType.FLOAT3;
		}
		
		public boolean isFloat4() {
			return type == ValueType.FLOAT4;
		}
		
		public boolean isInt() {
			return type == ValueType.INT;
		}
		
		public boolean isLong() {
			return type == ValueType.LONG;
		}
		
		public boolean isBool() {
			return type == ValueType.BOOL;
		}
		
		public boolean isString() {
			return type == ValueType.STRING;
		}
		
		public boolean isArray() {
			return type == ValueType.ARRAY;
		}
		
		public Value add(Value other) {
			switch(type) {
			case NULL:
				if(other.isNull())
					return new Value();
				return other.copy();
			case FLOAT:
				return new Value(r + other.getR());
			case FLOAT2:
				return new Value(r + other.getR(), g + other.getG());
			case FLOAT3:
				return new Value(r + other.getR(), g + other.getG(),
								b + other.getB());
			case FLOAT4:
				return new Value(r + other.getR(), g + other.getG(),
								b + other.getB(), a + other.getA());
			case INT:
				return new Value(i + other.getInt());
			case LONG:
				return new Value(l + other.getLong());
			case BOOL:
				return new Value(bool || other.getBool());
			case STRING:
				return new Value(str + other.getString());
			case ARRAY:
				Value[] otherArray = other.getArray();
				Value[] resArray = new Value[array.length + otherArray.length];
				for(int i = 0; i < array.length; ++i)
					resArray[i] = array[i].copy();
				for(int i = 0; i < otherArray.length; ++i)
					resArray[array.length + i] = otherArray[i].copy();
				return new Value(resArray);
			}
			return new Value();
		}
		
		public Value subtract(Value other) {
			switch(type) {
			case NULL:
				if(other.isNull())
					return new Value();
				return other.copy();
			case FLOAT:
				return new Value(r - other.getR());
			case FLOAT2:
				return new Value(r - other.getR(), g - other.getG());
			case FLOAT3:
				return new Value(r - other.getR(), g - other.getG(),
								b - other.getB());
			case FLOAT4:
				return new Value(r - other.getR(), g - other.getG(),
								b - other.getB(), a - other.getA());
			case INT:
				return new Value(i - other.getInt());
			case LONG:
				return new Value(l - other.getLong());
			case BOOL:
				return new Value(bool && !other.getBool());
			case STRING:
				return new Value(str.replace(other.getString(), ""));
			case ARRAY:
				Value[] otherArray = other.getArray();
				Value[] resArray = new Value[array.length];
				int size = 0;
				for(int i = 0; i < array.length; ++i) {
					boolean remove = false;
					for(int j = 0; j < otherArray.length; ++j) {
						if(array[i].equals(otherArray[j])) {
							remove = true;
							break;
						}
					}
					if(remove)
						continue;
					resArray[size] = array[i].copy();
					size++;
				}
				if(size != resArray.length)
					resArray = Arrays.copyOf(resArray, size);
				return new Value(resArray);
			}
			return new Value();
		}
		
		public Value multiply(Value other) {
			switch(type) {
			case NULL:
				if(other.isNull())
					return new Value();
				return other.copy();
			case FLOAT:
				return new Value(r * other.getR());
			case FLOAT2:
				return new Value(r * other.getR(), g * other.getG());
			case FLOAT3:
				return new Value(r * other.getR(), g * other.getG(),
								b * other.getB());
			case FLOAT4:
				return new Value(r * other.getR(), g * other.getG(),
								b * other.getB(), a * other.getA());
			case INT:
				return new Value(i * other.getInt());
			case LONG:
				return new Value(l * other.getLong());
			case BOOL:
				return new Value(bool && other.getBool());
			case STRING:
				String resStr = "";
				int repeat = other.getInt();
				for(int i = 0; i < repeat; ++i)
					resStr += str;
				return new Value(resStr);
			case ARRAY:
				repeat = other.getInt();
				Value[] resArray = new Value[array.length * repeat];
				for(int i = 0; i < repeat; ++i)
					for(int j = 0; j < array.length; ++j)
						resArray[i*array.length + j] = array[j].copy();
				return new Value(resArray);
			}
			return new Value();
		}
		
		public Value divide(Value other) {
			switch(type) {
			case NULL:
				if(other.isNull())
					return new Value();
				return other.copy();
			case FLOAT:
				return new Value(r / other.getR());
			case FLOAT2:
				return new Value(r / other.getR(), g / other.getG());
			case FLOAT3:
				return new Value(r / other.getR(), g / other.getG(),
								b / other.getB());
			case FLOAT4:
				return new Value(r / other.getR(), g / other.getG(),
								b / other.getB(), a / other.getA());
			case INT:
				return new Value(i / other.getInt());
			case LONG:
				return new Value(l / other.getLong());
			case BOOL:
				return new Value(bool || other.bool);
			case STRING:
				int divide = other.getInt();
				return new Value(str.substring(0, divide <= 0 ? 0 : (str.length()/divide)));
			case ARRAY:
				divide = other.getInt();
				Value[] resArray = new Value[divide <= 0 ? 0 : (array.length / divide)];
				for(int i = 0; i < resArray.length; ++i)
					resArray[i] = array[i].copy();
				return new Value(resArray);
			}
			return new Value();
		}
		
		public Value pow(Value other) {
			switch(type) {
			case NULL:
				if(other.isNull())
					return new Value();
				return other.copy();
			case FLOAT:
				return new Value(Math.pow(r, other.getR()));
			case FLOAT2:
				return new Value(Math.pow(r, other.getR()), Math.pow(g, other.getG()));
			case FLOAT3:
				return new Value(Math.pow(r, other.getR()), Math.pow(g, other.getG()),
						Math.pow(b, other.getB()));
			case FLOAT4:
				return new Value(Math.pow(r, other.getR()), Math.pow(g, other.getG()),
						Math.pow(b, other.getB()), Math.pow(a, other.getA()));
			case INT:
				return new Value((int) Math.pow(i, other.getInt()));
			case LONG:
				return new Value((long) Math.pow(l, other.getLong()));
			case BOOL:
				return new Value(bool && other.getBool());
			case STRING:
				String resStr = "";
				int repeat = other.getInt();
				for(int i = 0; i < repeat; ++i)
					resStr += str;
				return new Value(resStr);
			case ARRAY:
				repeat = other.getInt();
				Value[] resArray = new Value[array.length * repeat];
				for(int i = 0; i < repeat; ++i)
					for(int j = 0; j < array.length; ++j)
						resArray[i*array.length + j] = array[j].copy();
				return new Value(resArray);
			}
			return new Value();
		}
		
		public Value log() {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(Math.log(r));
			case FLOAT2:
				return new Value(Math.log(r), Math.log(g));
			case FLOAT3:
				return new Value(Math.log(r), Math.log(g), Math.log(b));
			case FLOAT4:
				return new Value(Math.log(r), Math.log(g), Math.log(b), Math.log(a));
			case INT:
				return new Value((int) Math.log(i));
			case LONG:
				return new Value((long) Math.log(l));
			default:
				return new Value();
			}
		}
		
		public Value log10() {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(Math.log10(r));
			case FLOAT2:
				return new Value(Math.log10(r), Math.log10(g));
			case FLOAT3:
				return new Value(Math.log10(r), Math.log10(g), Math.log10(b));
			case FLOAT4:
				return new Value(Math.log10(r), Math.log10(g), Math.log10(b), Math.log10(a));
			case INT:
				return new Value((int) Math.log10(i));
			case LONG:
				return new Value((long) Math.log10(l));
			default:
				return new Value();
			}
		}
		
		public Value exp() {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(Math.exp(r));
			case FLOAT2:
				return new Value(Math.exp(r), Math.exp(g));
			case FLOAT3:
				return new Value(Math.exp(r), Math.exp(g), Math.exp(b));
			case FLOAT4:
				return new Value(Math.exp(r), Math.exp(g), Math.exp(b), Math.exp(a));
			case INT:
				return new Value((int) Math.exp(i));
			case LONG:
				return new Value((long) Math.exp(l));
			default:
				return new Value();
			}
		}
		
		public Value exp10() {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(Math.pow(10f, r));
			case FLOAT2:
				return new Value(Math.pow(10f, r), Math.pow(10f, g));
			case FLOAT3:
				return new Value(Math.pow(10f, r), Math.pow(10f, g), Math.pow(10f, b));
			case FLOAT4:
				return new Value(Math.pow(10f, r), Math.pow(10f, g), Math.pow(10f, b), Math.pow(10f, a));
			case INT:
				return new Value((int) Math.pow(10f, i));
			case LONG:
				return new Value((long) Math.pow(10f, l));
			default:
				return new Value();
			}
		}
		
		public Value dot(Value other) {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(r * other.r);
			case FLOAT2:
				return new Value(r * other.r + g * other.g);
			case FLOAT3:
				return new Value(r * other.r + g * other.g + b * other.b);
			case FLOAT4:
				return new Value(r * other.r + g * other.g + b * other.b);
			case INT:
				return new Value(i * other.i);
			case LONG:
				return new Value(l * other.l);
			default:
				return new Value();
			}
		}
		
		public Value normalise() {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(1f);
			case FLOAT2:
				double length = Math.max(Math.sqrt(r * r + g * g), 0.00001);
				return new Value(r / length, g / length);
			case FLOAT3:
				length = Math.max(Math.sqrt(r * r + g * g + b * b), 0.00001);
				return new Value(r / length, g / length, b / length);
			case FLOAT4:
				length = Math.max(Math.sqrt(r * r + g * g + b * b + a * a), 0.00001);
				return new Value(r / length, g / length, b / length, a / length);
			case INT:
				return new Value(1);
			case LONG:
				return new Value(1L);
			default:
				return new Value();
			}
		}
		
		public Value lerp(Value other, Value t) {
			switch(type) {
			case NULL:
				return this;
			case FLOAT:
				return new Value(r * (1f - t.getR()) + other.getR() * t.getR());
			case FLOAT2:
				return new Value(r * (1f - t.getR()) + other.getR() * t.getR(),
								g * (1f - t.getG()) + other.getG() * t.getG());
			case FLOAT3:
				return new Value(r * (1f - t.getR()) + other.getR() * t.getR(),
								g * (1f - t.getG()) + other.getG() * t.getG(),
								r * (1f - t.getB()) + other.getB() * t.getB());
			case FLOAT4:
				return new Value(r * (1f - t.getR()) + other.getR() * t.getR(),
						g * (1f - t.getG()) + other.getG() * t.getG(),
						r * (1f - t.getB()) + other.getB() * t.getB(),
						a * (1f - t.getR()) + other.getA() * t.getR());
			case INT:
				return new Value((int) (i * (1f - t.getR()) + other.getInt() * t.getR()));
			case LONG:
				return new Value((int) (l * (1f - t.getR()) + other.getLong() * t.getR()));
			default:
				return new Value();
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Value) {
				Value other = (Value) obj;
				if(type == other.type) {
					switch(type) {
					case NULL:
						return true;
					case FLOAT:
						return r == other.r;
					case FLOAT2:
						return r == other.r && g == other.g;
					case FLOAT3:
						return r == other.r && g == other.g && b == other.b;
					case FLOAT4:
						return r == other.r && g == other.g && b == other.b && a == other.a;
					case INT:
						return i == other.i;
					case LONG:
						return l == other.l;
					case BOOL:
						return bool = other.bool;
					case STRING:
						return str.equals(other.str);
					case ARRAY:
						if(array.length != other.array.length)
							return false;
						for(int i = 0; i < array.length; ++i)
							if(!array[i].equals(other.array[i]))
								return false;
						return true;
					}
					return false;
				}else {
					return equals(other.castToType(type));
				}
			}else if(obj instanceof String){
				return getString().equals(obj);
			}
			return false;
		}
		
	}
	
	public static class Attribute{
		
		private Value value;
		private String input;
		private ModifierNode node;
		
		public Attribute(ModifierNode node, Value defaultValue) {
			this.node = node;
			this.value = defaultValue;
			this.input = null;
		}
		
		public Value getValue() {
			return value;
		}
		
		public String getInput() {
			return input;
		}
		
		public ModifierNode getNode() {
			return node;
		}
		
		public boolean isConnected() {
			return input != null;
		}
		
		public void connect(String input) {
			this.input = input;
		}
		
		public void disconnect() {
			this.input = null;
		}
		
		public void setValue(Value value) {
			this.value = value;
		}
		
	}
	
	private String name;
	private Map<String, Attribute> userAttributes;
	
	public ModifierNode(String name) {
		this.name = name;
		this.userAttributes = null;
	}
	
	public String getName() {
		return name;
	}
	
	public Attribute getAttribute(String name) {
		try {
			Field field = this.getClass().getField(name);
			if(field.getType().equals(Attribute.class)) {
				return (Attribute) field.get(this);
			}
		}catch(Exception ex) {};
		if(userAttributes != null)
			return userAttributes.getOrDefault(name, null);
		return null;
	}
	
	public Attribute getAttributeOrCreate(String name) {
		try {
			Field field = this.getClass().getField(name);
			if(field.getType().equals(Attribute.class)) {
				return (Attribute) field.get(this);
			}
		}catch(Exception ex) {};
		if(userAttributes == null)
			userAttributes = new HashMap<String, Attribute>();
		Attribute userAttribute = userAttributes.getOrDefault(name, null);
		if(userAttribute == null) {
			userAttribute = new Attribute(this, new Value());
			userAttributes.put(name, userAttribute);
		}
		return userAttribute;
	}
	
	public abstract Value evaluate(ModifierContext context);
	
}
