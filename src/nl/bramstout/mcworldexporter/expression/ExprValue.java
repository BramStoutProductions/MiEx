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

package nl.bramstout.mcworldexporter.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.GeneratedTextures;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagIntArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagLongArray;
import nl.bramstout.mcworldexporter.nbt.NbtTagShort;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class ExprValue {

	private ExprValueImpl impl;
	private boolean immutable;
	
	public ExprValue(ExprValueImpl impl) {
		this(impl, false);
	}
	
	public ExprValue(ExprValueImpl impl, boolean immutable) {
		this.impl = impl;
		this.immutable = immutable;
	}
	
	public ExprValue(NbtTag tag) {
		this.immutable = true;
		switch(tag.getId()) {
		case NbtTagByte.ID:
			this.impl = new ExprValueInt(((NbtTagByte) tag).getData());
			break;
		case NbtTagByteArray.ID:
			this.impl = new ExprValueNull();
			break;
		case NbtTagCompound.ID:
			this.impl = new ExprValueNbtCompound((NbtTagCompound) tag);
			break;
		case NbtTagDouble.ID:
			this.impl = new ExprValueFloat(((NbtTagFloat) tag).getData());
			break;
		case NbtTagFloat.ID:
			this.impl = new ExprValueFloat(((NbtTagFloat) tag).getData());
			break;
		case NbtTagInt.ID:
			this.impl = new ExprValueInt(((NbtTagInt) tag).getData());
			break;
		case NbtTagIntArray.ID:
			this.impl = new ExprValueNull();
			break;
		case NbtTagList.ID:
			this.impl = new ExprValueNbtList((NbtTagList) tag);
			break;
		case NbtTagLong.ID:
			this.impl = new ExprValueInt(((NbtTagLong) tag).getData());
			break;
		case NbtTagLongArray.ID:
			this.impl = new ExprValueNull(); 
			break;
		case NbtTagShort.ID:
			this.impl = new ExprValueInt(((NbtTagShort) tag).getData());
			break;
		case NbtTagString.ID:
			this.impl = new ExprValueString(((NbtTagString) tag).getData());
			break;
		default:
			this.impl = new ExprValueNull();
			break;
		}
	}
	
	public boolean isNull() {
		return impl instanceof ExprValueNull;
	}
	
	public ExprValueImpl getImpl() {
		return impl;
	}
	
	public void set(ExprValue other) {
		if(this.immutable)
			throw new RuntimeException("ExprValue is immutable");
		this.impl = other.impl;
	}
	
	public boolean asBool() {
		return impl.asBool();
	}
	
	public long asInt() {
		return impl.asInt();
	}
	
	public float asFloat() {
		return impl.asFloat();
	}
	
	public String asString() {
		return impl.asString();
	}
	
	public ExprValue add(ExprValue other) {
		return new ExprValue(impl.add(other));
	}
	
	public ExprValue sub(ExprValue other) {
		return new ExprValue(impl.sub(other));
	}
	
	public ExprValue mult(ExprValue other) {
		return new ExprValue(impl.mult(other));
	}
	
	public ExprValue div(ExprValue other) {
		return new ExprValue(impl.div(other));
	}
	
	public boolean equal(ExprValue other) {
		return impl.equal(other);
	}
	
	public boolean lessThan(ExprValue other) {
		return impl.lessThan(other);
	}
	
	public boolean greaterThan(ExprValue other) {
		return impl.greaterThan(other);
	}
	
	public ExprValue member(String name) {
		return impl.member(name);
	}
	
	public ExprValue call(ExprContext context) {
		return impl.call(context);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ExprValue)
			return equal((ExprValue) obj);
		return false;
	}
	
	@Override
	public int hashCode() {
		return impl.hashCode();
	}
	
	public Map<String, ExprValue> getChildren(){
		return impl.getChildren();
	}
	
	public NbtTag toNbt() {
		return impl.toNbt();
	}
	
	public String getTypeStr() {
		return impl.getTypeStr();
	}
	
	
	public static abstract class ExprValueImpl{
		
		public abstract boolean asBool();
		public abstract long asInt();
		public abstract float asFloat();
		public abstract String asString();
		
		public abstract ExprValueImpl add(ExprValue other);
		public abstract ExprValueImpl sub(ExprValue other);
		public abstract ExprValueImpl mult(ExprValue other);
		public abstract ExprValueImpl div(ExprValue other);
		
		public abstract boolean equal(ExprValue other);
		public abstract boolean lessThan(ExprValue other);
		public abstract boolean greaterThan(ExprValue other);
		
		public abstract ExprValue member(String name);
		public abstract ExprValue call(ExprContext context);
		
		public abstract Map<String, ExprValue> getChildren();
		
		public abstract NbtTag toNbt();
		
		public abstract String getTypeStr();
		
	}
	
	public static class ExprValueNull extends ExprValueImpl{
		
		@Override
		public boolean asBool() {
			return false;
		}
		
		@Override
		public float asFloat() {
			return 0;
		}
		
		@Override
		public long asInt() {
			return 0;
		}
		
		@Override
		public String asString() {
			return "";
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(ExprValue other) {
			return other.impl instanceof ExprValueNull;
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			return false;
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			return false;
		}
		
		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return null;
		}
		
		@Override
		public String getTypeStr() {
			return "null";
		}
		
	}
	
	public static class ExprValueInt extends ExprValueImpl{
		
		private long value;
		
		public ExprValueInt(long value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value > 0;
		}
		
		@Override
		public float asFloat() {
			return value;
		}
		
		@Override
		public long asInt() {
			return value;
		}
		
		@Override
		public String asString() {
			return Long.toString(value);
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			return new ExprValueInt(value + other.asInt());
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			return new ExprValueInt(value / other.asInt());
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			return new ExprValueInt(value * other.asInt());
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			return new ExprValueInt(value - other.asInt());
		}
		
		@Override
		public boolean equal(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value == other.asInt();
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value > other.asInt();
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value < other.asInt();
		}
		
		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return NbtTagLong.newNonPooledInstance("", value);
		}
		
		@Override
		public String getTypeStr() {
			return "int";
		}
		
	}
	
	public static class ExprValueFloat extends ExprValueImpl{
		
		private float value;
		
		public ExprValueFloat(float value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value >= 0.5f;
		}
		
		@Override
		public float asFloat() {
			return value;
		}
		
		@Override
		public long asInt() {
			return (long) value;
		}
		
		@Override
		public String asString() {
			return Float.toString(value);
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			return new ExprValueFloat(value + other.asFloat());
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			return new ExprValueFloat(value / other.asFloat());
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			return new ExprValueFloat(value * other.asFloat());
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			return new ExprValueFloat(value - other.asFloat());
		}
		
		@Override
		public boolean equal(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value == other.asFloat();
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value > other.asFloat();
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value < other.asFloat();
		}
		
		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return NbtTagFloat.newNonPooledInstance("", value);
		}
		
		@Override
		public String getTypeStr() {
			return "float";
		}
		
	}
	
	public static class ExprValueBool extends ExprValueImpl{
		
		private boolean value;
		
		public ExprValueBool(boolean value) {
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			return value;
		}
		
		@Override
		public float asFloat() {
			return value ? 1f : 0f;
		}
		
		@Override
		public long asInt() {
			return value ? 1 : 0;
		}
		
		@Override
		public String asString() {
			return value ? "true" : "false";
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			return new ExprValueBool((asFloat() + other.asFloat()) > 0.5f);
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			return new ExprValueBool((asFloat() / other.asFloat()) > 0.5f);
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			return new ExprValueBool((asFloat() * other.asFloat()) > 0.5f);
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			return new ExprValueBool((asFloat() - other.asFloat()) > 0.5f);
		}
		
		@Override
		public boolean equal(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value == other.asBool();
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && asFloat() > other.asFloat();
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && asFloat() < other.asFloat();
		}
		
		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return NbtTagByte.newNonPooledInstance("", value ? (byte) 1 : (byte) 0);
		}
		
		@Override
		public String getTypeStr() {
			return "bool";
		}
		
	}
	
	public static class ExprValueString extends ExprValueNativeClass{
		
		private String value;
		
		public ExprValueString(String value) {
			super();
			this.value = value;
		}
		
		@Override
		public boolean asBool() {
			String lV = value.toLowerCase();
			if(lV.startsWith("t") || lV.startsWith("y") || lV.startsWith("1"))
				return true;
			return asFloat() > 0.5f;
		}
		
		@Override
		public float asFloat() {
			try {
				return Float.parseFloat(value);
			}catch(Exception ex) {}
			return 0;
		}
		
		@Override
		public long asInt() {
			try {
				return Long.parseLong(value);
			}catch(Exception ex) {}
			return (long) asFloat();
		}
		
		@Override
		public String asString() {
			return value;
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			return new ExprValueString(value + other.asString());
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(ExprValue other) {
			return !(other.impl instanceof ExprValueNull) && value.equals(other.asString());
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public NbtTag toNbt() {
			return NbtTagString.newNonPooledInstance("", value);
		}
		
		@Override
		public String getTypeStr() {
			return "string";
		}
		
		private static final String EMPTY_STRING = "";
		
		@NativeFunction
		public ExprValue length(ExprContext context) {
			return new ExprValue(new ExprValueInt(value.length()));
		}
		
		@NativeFunction
		public ExprValue endsWith(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.endsWith(arg0)));
		}
		
		@NativeFunction
		public ExprValue startsWith(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.startsWith(arg0)));
		}
		
		@NativeFunction
		public ExprValue equals(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.equals(arg0)));
		}
		
		@NativeFunction
		public ExprValue equalsIgnoreCase(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.equalsIgnoreCase(arg0)));
		}
		
		@NativeFunction
		public ExprValue contains(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.contains(arg0)));
		}
		
		@NativeFunction
		public ExprValue containsIgnoreCase(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueBool(value.toLowerCase().contains(arg0.toLowerCase())));
		}
		
		@NativeFunction
		public ExprValue indexOf(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueInt(value.indexOf(arg0)));
		}
		
		@NativeFunction
		public ExprValue lastIndexOf(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			return new ExprValue(new ExprValueInt(value.lastIndexOf(arg0)));
		}
		
		@NativeFunction
		public ExprValue replace(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String arg0 = EMPTY_STRING;
			if(arg0V != null)
				arg0 = arg0V.asString();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			String arg1 = EMPTY_STRING;
			if(arg1V != null)
				arg1 = arg1V.asString();
			return new ExprValue(new ExprValueString(value.replace(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue substring(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			int arg0 = 0;
			if(arg0V != null)
				arg0 = (int) arg0V.asInt();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			int arg1 = value.length();
			if(arg1V != null)
				arg1 = (int) arg1V.asInt();
			return new ExprValue(new ExprValueString(value.substring(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue toLowerCase(ExprContext context) {
			return new ExprValue(new ExprValueString(value.toLowerCase()));
		}
		
		@NativeFunction
		public ExprValue toUpperCase(ExprContext context) {
			return new ExprValue(new ExprValueString(value.toUpperCase()));
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
	}
	
	public static class ExprValueDict extends ExprValueImpl{
		
		private Map<String, ExprValue> value;
		
		public ExprValueDict() {
			this.value = new HashMap<String, ExprValue>();
		}
		
		public Map<String, ExprValue> getValue(){
			return value;
		}

		@Override
		public boolean asBool() {
			return !value.isEmpty();
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(ExprValue other) {
			if(other.impl instanceof ExprValueDict) {
				if(((ExprValueDict) other.impl).value.size() != value.size())
					return false;
				for(Entry<String, ExprValue> entry : value.entrySet()) {
					ExprValue val = ((ExprValueDict) other.impl).value.getOrDefault(entry.getKey(), null);
					if(val == null)
						return false;
					if(!val.equal(entry.getValue()))
						return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValue member(String name) {
			ExprValue val = value.getOrDefault(name, null);
			if(val == null) {
				val = new ExprValue(new ExprValueNull());
				value.put(name, val);
			}
			return val;
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return value;
		}
		
		@Override
		public NbtTag toNbt() {
			NbtTagCompound nbt = NbtTagCompound.newNonPooledInstance("");
			for(Entry<String, ExprValue> entry : value.entrySet()) {
				NbtTag child = entry.getValue().toNbt();
				child.setName(entry.getKey());
				nbt.addElement(child);
			}
			return nbt;
		}
		
		@Override
		public String getTypeStr() {
			return "dict";
		}
		
	}
	
	public static class ExprValueNbtCompound extends ExprValueImpl{
		
		private NbtTagCompound value;
		
		public ExprValueNbtCompound(NbtTagCompound value) {
			this.value = value;
		}
		
		public NbtTagCompound getNbt() {
			return value;
		}

		@Override
		public boolean asBool() {
			return value.getSize() > 0;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(ExprValue other) {
			if(other.impl instanceof ExprValueNbtCompound) {
				if(value == null || ((ExprValueNbtCompound) other.impl).value == null)
					return value == ((ExprValueNbtCompound) other.impl).value;
				return value.equals(((ExprValueNbtCompound) other.impl).value);
			}
			return false;
		}

		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValue member(String name) {
			NbtTag val = value.get(name);
			if(val == null) {
				return new ExprValue(new ExprValueNull());
			}
			return new ExprValue(val);
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			Map<String, ExprValue> map = new HashMap<String, ExprValue>();
			for(int i = 0; i < value.getSize(); ++i) {
				NbtTag tag = value.get(i);
				map.put(tag.getName(), new ExprValue(tag));
			}
			return map;
		}
		
		@Override
		public NbtTag toNbt() {
			return value;
		}
		
		@Override
		public String getTypeStr() {
			return "dict";
		}
		
	}
	
	public static class ExprValueNbtList extends ExprValueImpl{
		
		private NbtTagList value;
		
		public ExprValueNbtList(NbtTagList value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return value.getSize() > 0;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(ExprValue other) {
			if(other.impl instanceof ExprValueNbtList) {
				if(value == null || ((ExprValueNbtList) other.impl).value == null)
					return value == ((ExprValueNbtList) other.impl).value;
				return value.equals(((ExprValueNbtList) other.impl).value);
			}
			return false;
		}

		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValue member(String name) {
			if(name.equalsIgnoreCase("length")) {
				return new ExprValue(new ExprValueNativeFunction(this, lengthMethod));
			}
			int index = -1;
			try {
				index = Integer.parseInt(name);
			}catch(Exception ex) {}
			if(index < 0 || index >= value.getSize())
				return new ExprValue(new ExprValueNull());
			NbtTag val = value.get(index);
			if(val == null) {
				return new ExprValue(new ExprValueNull());
			}
			return new ExprValue(val);
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		private static Method lengthMethod = null;
		static {
			try {
				lengthMethod = ExprValueNbtList.class.getDeclaredMethod("length", ExprContext.class);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		
		public ExprValue length(ExprContext context) {
			return new ExprValue(new ExprValueInt(value.getSize()));
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			Map<String, ExprValue> map = new HashMap<String, ExprValue>();
			for(int i = 0; i < value.getSize(); ++i) {
				map.put(Integer.toString(i), new ExprValue(value.get(i)));
			}
			return map;
		}
		
		@Override
		public NbtTag toNbt() {
			return value;
		}
		
		@Override
		public String getTypeStr() {
			return "list";
		}
		
	}
	
	public static class ExprValueThisBlock extends ExprValueImpl{
		
		private ExprContext value;
		
		public ExprValueThisBlock(ExprContext value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return true;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(ExprValue other) {
			return other.impl instanceof ExprValueThisBlock;
		}

		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValue member(String name) {
			if(name.equals("name"))
				return new ExprValue(new ExprValueString(value.name));
			else if(name.equals("state"))
				return new ExprValue(value.properties);
			else if(name.equals("x")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueInt(value.x));
			}
			else if(name.equals("y")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueInt(value.y));
			}
			else if(name.equals("z")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueInt(value.z));
			}
			else if(name.equals("fx")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueFloat(value.fx));
			}
			else if(name.equals("fy")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueFloat(value.fy));
			}
			else if(name.equals("fz")) {
				if(!value.isLocationDependent)
					throw new RuntimeException("Location may only be queried if block state is location dependent");
				return new ExprValue(new ExprValueFloat(value.fz));
			}
			else if(name.equals("time")) {
				return new ExprValue(new ExprValueFloat(value.time));
			}
			return new ExprValue(new ExprValueNull());
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			NbtTagCompound tag = NbtTagCompound.newNonPooledInstance("");
			tag.addElement(NbtTagInt.newNonPooledInstance("x", value.x));
			tag.addElement(NbtTagInt.newNonPooledInstance("y", value.y));
			tag.addElement(NbtTagInt.newNonPooledInstance("z", value.z));
			tag.addElement(NbtTagFloat.newNonPooledInstance("fx", value.fx));
			tag.addElement(NbtTagFloat.newNonPooledInstance("fy", value.fy));
			tag.addElement(NbtTagFloat.newNonPooledInstance("fz", value.fz));
			tag.addElement(NbtTagString.newNonPooledInstance("name", value.name));
			tag.addElement(NbtTagFloat.newNonPooledInstance("time", value.time));
			NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("properties");
			if(value.properties != null) {
				properties.addAllElements(value.properties);
			}
			tag.addElement(properties);
			return tag;
		}
		
		@Override
		public String getTypeStr() {
			return "thisBlock";
		}
		
	}
	
	public static class ExprValueFunction extends ExprValueImpl{
		
		private Expression value;
		
		public ExprValueFunction(Expression value) {
			this.value = value;
		}

		@Override
		public boolean asBool() {
			return true;
		}

		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean equal(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}

		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			return value.eval(context);
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return null;
		}
		
		@Override
		public String getTypeStr() {
			return "function";
		}
		
	}
	
	private static class ExprValueBlock extends ExprValueImpl{
		
		private Block block;
		private int x;
		private int y;
		private int z;
		
		public ExprValueBlock(Block block, int x, int y, int z) {
			this.block = block;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public ExprValue member(String name) {
			if(name.equals("name"))
				return new ExprValue(new ExprValueString(block.getName()));
			else if(name.equals("state"))
				return new ExprValue(block.getProperties());
			else if(name.equals("x")) {
				return new ExprValue(new ExprValueInt(x));
			}
			else if(name.equals("y")) {
				return new ExprValue(new ExprValueInt(y));
			}
			else if(name.equals("z")) {
				return new ExprValue(new ExprValueInt(z));
			}
			return new ExprValue(new ExprValueNull());
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(ExprValue other) {
			if(other.impl instanceof ExprValueBlock) {
				return block == ((ExprValueBlock)other.impl).block;
			}
			return false;
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			NbtTagCompound tag = NbtTagCompound.newNonPooledInstance("");
			tag.addElement(NbtTagInt.newNonPooledInstance("x", x));
			tag.addElement(NbtTagInt.newNonPooledInstance("y", y));
			tag.addElement(NbtTagInt.newNonPooledInstance("z", z));
			tag.addElement(NbtTagString.newNonPooledInstance("name", block.getName()));
			NbtTagCompound properties = NbtTagCompound.newNonPooledInstance("properties");
			if(block.getProperties() != null) {
				properties.addAllElements(block.getProperties());
			}
			tag.addElement(properties);
			return tag;
		}
		
		@Override
		public String getTypeStr() {
			return "block";
		}
		
	}
	
	@Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
	private static @interface NativeFunction{}
	
	private static class ExprValueNativeFunction extends ExprValueImpl{
		
		private Object obj;
		private Method func;
		
		public ExprValueNativeFunction(Object obj, Method func) {
			this.obj = obj;
			this.func = func;
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			try {
				return (ExprValue) func.invoke(obj, context);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			return new ExprValue(new ExprValueNull());
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValue member(String name) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return null;
		}
		
		@Override
		public String getTypeStr() {
			return "function";
		}
		
	}
	
	private static abstract class ExprValueNativeClass extends ExprValueImpl{
		
		private Map<String, ExprValue> funcs;
		
		public ExprValueNativeClass() {
			funcs = new HashMap<String, ExprValue>();
			for(Method method : this.getClass().getMethods()) {
				if(method.isAnnotationPresent(NativeFunction.class)) {
					if(ExprValue.class.isAssignableFrom(method.getReturnType())) {
						funcs.put(method.getName(), new ExprValue(new ExprValueNativeFunction(this, method)));
					}
				}
			}
		}
		
		@Override
		public ExprValue member(String name) {
			ExprValue val = funcs.getOrDefault(name, null);
			if(val != null)
				return val;
			return new ExprValue(new ExprValueNull());
		}
		
		@Override
		public ExprValue call(ExprContext context) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl add(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean asBool() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public float asFloat() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public long asInt() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public String asString() {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl div(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean equal(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean greaterThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public boolean lessThan(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl mult(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public ExprValueImpl sub(ExprValue other) {
			throw new RuntimeException("Unsuported operation");
		}
		
		@Override
		public Map<String, ExprValue> getChildren() {
			return new HashMap<String, ExprValue>();
		}
		
		@Override
		public NbtTag toNbt() {
			return null;
		}
		
		@Override
		public String getTypeStr() {
			return "class";
		}
		
	}
	
	public static class ExprValueClassMath extends ExprValueNativeClass{
		
		@NativeFunction
		public ExprValue abs(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			if(arg0V.impl instanceof ExprValueInt) {
				long arg0I = arg0V.asInt();
				return new ExprValue(new ExprValueInt(Math.abs(arg0I)));
			}
			return new ExprValue(new ExprValueFloat(Math.abs(arg0)));
		}
		
		@NativeFunction
		public ExprValue acos(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.acos(arg0)));
		}
		
		@NativeFunction
		public ExprValue asin(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.asin(arg0)));
		}
		
		@NativeFunction
		public ExprValue atan(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.atan(arg0)));
		}
		
		@NativeFunction
		public ExprValue atan2(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.atan2(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue ceil(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.ceil(arg0)));
		}
		
		@NativeFunction
		public ExprValue clamp(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			ExprValue arg2V = context.variables.getOrDefault("arg2", null);
			float arg2 = 0f;
			if(arg2V != null)
				arg2 = arg2V.asFloat();
			if(arg0V.impl instanceof ExprValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				long arg2I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				if(arg2V != null)
					arg2I = arg2V.asInt();
				return new ExprValue(new ExprValueInt(Math.min(Math.max(arg0I, arg1I), arg2I)));
			}
			return new ExprValue(new ExprValueFloat(Math.min(Math.max(arg0, arg1), arg2)));
		}
		
		@NativeFunction
		public ExprValue cos(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.cos(arg0)));
		}
		
		@NativeFunction
		public ExprValue cosh(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.cosh(arg0)));
		}
		
		@NativeFunction
		public ExprValue exp(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.exp(arg0)));
		}
		
		@NativeFunction
		public ExprValue floor(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.floor(arg0)));
		}
		
		@NativeFunction
		public ExprValue log(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.log(arg0)));
		}
		
		@NativeFunction
		public ExprValue log10(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.log10(arg0)));
		}
		
		@NativeFunction
		public ExprValue max(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			if(arg0V.impl instanceof ExprValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				return new ExprValue(new ExprValueInt(Math.max(arg0I, arg1I)));
			}
			return new ExprValue(new ExprValueFloat(Math.max(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue min(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			if(arg0V.impl instanceof ExprValueInt) {
				long arg0I = arg0V.asInt();
				long arg1I = 0;
				if(arg1V != null)
					arg1I = arg1V.asInt();
				return new ExprValue(new ExprValueInt(Math.min(arg0I, arg1I)));
			}
			return new ExprValue(new ExprValueFloat(Math.min(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue pow(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			float arg1 = 0f;
			if(arg1V != null)
				arg1 = arg1V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.pow(arg0, arg1)));
		}
		
		@NativeFunction
		public ExprValue round(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.round(arg0)));
		}
		
		@NativeFunction
		public ExprValue signum(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.signum(arg0)));
		}
		
		@NativeFunction
		public ExprValue sin(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.sin(arg0)));
		}
		
		@NativeFunction
		public ExprValue sinh(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.sinh(arg0)));
		}
		
		@NativeFunction
		public ExprValue sqrt(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.sqrt(arg0)));
		}
		
		@NativeFunction
		public ExprValue tan(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.tan(arg0)));
		}
		
		@NativeFunction
		public ExprValue tanh(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.tanh(arg0)));
		}
		
		@NativeFunction
		public ExprValue toDegrees(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.toDegrees(arg0)));
		}
		
		@NativeFunction
		public ExprValue toRadians(ExprContext context) {
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			float arg0 = 0f;
			if(arg0V != null)
				arg0 = arg0V.asFloat();
			return new ExprValue(new ExprValueFloat((float) Math.toRadians(arg0)));
		}
		
	}
	
	private static class ExprValueClassDict extends ExprValueNativeClass{
		
		@NativeFunction
		public ExprValue length(ExprContext context) {
			ExprValue arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ExprValueDict) && !(arg0.impl instanceof ExprValueNbtCompound) &&
					!(arg0.impl instanceof ExprValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			if(arg0.impl instanceof ExprValueDict)
				return new ExprValue(new ExprValueInt(((ExprValueDict) arg0.impl).value.size()));
			if(arg0.impl instanceof ExprValueNbtCompound)
				return new ExprValue(new ExprValueInt(((ExprValueNbtCompound) arg0.impl).value.getSize()));
			if(arg0.impl instanceof ExprValueNbtList)
				return new ExprValue(new ExprValueInt(((ExprValueNbtList) arg0.impl).value.getSize()));
			return new ExprValue(new ExprValueInt(0));
		}
		
		@NativeFunction
		public ExprValue hasKey(ExprContext context) {
			ExprValue arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ExprValueDict) && !(arg0.impl instanceof ExprValueNbtCompound) &&
					!(arg0.impl instanceof ExprValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			String arg1 = null;
			if(arg1V != null)
				arg1 = arg1V.asString();
			if(arg0.impl instanceof ExprValueDict)
				return new ExprValue(new ExprValueBool(((ExprValueDict) arg0.impl).value.containsKey(arg1)));
			if(arg0.impl instanceof ExprValueNbtCompound)
				return new ExprValue(new ExprValueBool(((ExprValueNbtCompound) arg0.impl).value.get(arg1) != null));
			if(arg0.impl instanceof ExprValueNbtList) {
				int index = -1;
				try {
					index = Integer.parseInt(arg1);
				}catch(Exception ex) {};
				return new ExprValue(new ExprValueBool(index >= 0 && index < ((ExprValueNbtList) arg0.impl).value.getSize()));
			}
			return new ExprValue(new ExprValueBool(false));
		}
		
		@NativeFunction
		public ExprValue keys(ExprContext context) {
			ExprValue arg0 = context.variables.getOrDefault("arg0", null);
			if(arg0 == null)
				throw new RuntimeException("Missing argument 0");
			if(!(arg0.impl instanceof ExprValueDict) && !(arg0.impl instanceof ExprValueNbtCompound) &&
					!(arg0.impl instanceof ExprValueNbtList))
				throw new RuntimeException("Argument 0 is of invalid type " + arg0.impl.getClass().getName());
			if(arg0.impl instanceof ExprValueDict) {
				ExprValueDict val = new ExprValueDict();
				int i = 0;
				for(String key : ((ExprValueDict) arg0.impl).value.keySet()) {
					val.value.put(Integer.toString(i), new ExprValue(new ExprValueString(key)));
					i++;
				}
				return new ExprValue(val);
			}
			if(arg0.impl instanceof ExprValueNbtCompound) {
				ExprValueDict val = new ExprValueDict();
				for(int i = 0; i < ((ExprValueNbtCompound) arg0.impl).value.getSize(); i++) {
					val.value.put(Integer.toString(i), new ExprValue(new ExprValueString(((ExprValueNbtCompound) arg0.impl).value.get(i).getName())));
					i++;
				}
				return new ExprValue(val);
			}
			if(arg0.impl instanceof ExprValueNbtList) {
				ExprValueDict val = new ExprValueDict();
				for(int i = 0; i < ((ExprValueNbtList) arg0.impl).value.getSize(); i++) {
					val.value.put(Integer.toString(i), new ExprValue(new ExprValueString(Integer.toString(i))));
					i++;
				}
				return new ExprValue(val);
			}
			return new ExprValue(new ExprValueDict());
		}
		
	}
	
	private static class ExprValueClassBlock extends ExprValueNativeClass{
		
		@NativeFunction
		public ExprValue getBlockRelative(ExprContext context) {
			if(!context.isLocationDependent)
				throw new RuntimeException("Blocks may only be queried if block state is location dependent");
			int x = 0;
			int y = 0;
			int z = 0;
			int layer = 0;
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			if(arg0V != null)
				x = (int) arg0V.asInt();
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			if(arg1V != null)
				y = (int) arg1V.asInt();
			ExprValue arg2V = context.variables.getOrDefault("arg2", null);
			if(arg2V != null)
				z = (int) arg2V.asInt();
			ExprValue arg3V = context.variables.getOrDefault("arg3", null);
			if(arg3V != null)
				layer = (int) arg3V.asInt();
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(context.x + x, context.y + y, context.z + z, layer);
			Block block = BlockRegistry.getBlock(blockId);
			return new ExprValue(new ExprValueBlock(block, context.x + x, context.y + y, context.z + z));
		}
		
		@NativeFunction 
		public ExprValue blockHasTag(ExprContext context){
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			if(arg0V == null || !(arg0V.impl instanceof ExprValueBlock))
				throw new RuntimeException("Argument 0 is either not given or not a ExprValueBlock");
			Block block = ((ExprValueBlock) arg0V.impl).block;
			String tag = "";
			ExprValue arg1V = context.variables.getOrDefault("arg1", null);
			if(arg1V != null)
				tag = arg1V.asString();
			return new ExprValue(new ExprValueBool(Tags.isInList(block.getName(), Tags.getNamesInTag(tag))));
		}
		
	}
	
	private static class ExprValueClassValue extends ExprValueNativeClass{
		
		@NativeFunction 
		public ExprValue getType(ExprContext context){
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String type = "null";
			if(arg0V != null)
				type = arg0V.getTypeStr();
			return new ExprValue(new ExprValueString(type));
		}
		
	}
	
	private static class ExprValueClassTextures extends ExprValueNativeClass{
		
		private String getSkinURL(JsonObject data) {
			JsonObject texturesObj = data.getAsJsonObject("textures");
			if(texturesObj == null)
				return null;
			JsonObject skinObj = texturesObj.getAsJsonObject("SKIN");
			if(skinObj == null)
				return null;
			JsonElement urlEl = skinObj.get("url");
			if(urlEl == null)
				return null;
			return urlEl.getAsString();
		}
		
		@NativeFunction 
		public ExprValue getTextureForPlayerProfile(ExprContext context){
			ExprValue arg0V = context.variables.getOrDefault("arg0", null);
			String texId = "minecraft:entity/player/wide/steve";
			if(arg0V != null) {
				String skinURL = null;
				String playerName = null;
				String playerUUID = null;
				if(arg0V.getImpl() instanceof ExprValueString) {
					playerName = arg0V.asString();
				}else if(arg0V.getImpl() instanceof ExprValueNbtCompound || 
							arg0V.getImpl() instanceof ExprValueDict) {
					Map<String, ExprValue> children = arg0V.getChildren();
					ExprValue nameVal = children.getOrDefault("name", null);
					if(nameVal != null) {
						playerName = nameVal.asString();
					}
					
					ExprValue uuidVal = children.getOrDefault("id", null);
					if(uuidVal != null) {
						Map<String, ExprValue> uuidVals = uuidVal.getChildren();
						long upperBits = 0;
						long lowerBits = 0;
						ExprValue tmpVal = uuidVals.getOrDefault("0", null);
						if(tmpVal != null)
							upperBits |= tmpVal.asInt() << 32;
						tmpVal = uuidVals.getOrDefault("1", null);
						if(tmpVal != null)
							upperBits |= tmpVal.asInt() & 0xFFFFFFFF;
						
						tmpVal = uuidVals.getOrDefault("2", null);
						if(tmpVal != null)
							lowerBits |= tmpVal.asInt() << 32;
						tmpVal = uuidVals.getOrDefault("3", null);
						if(tmpVal != null)
							lowerBits |= tmpVal.asInt() & 0xFFFFFFFF;
						
						String upperStr = Long.toHexString(upperBits);
						for(int i = upperStr.length(); i < 16; ++i) {
							upperStr = "0" + upperStr;
						}
						String lowerStr = Long.toHexString(lowerBits);
						for(int i = lowerStr.length(); i < 16; ++i) {
							lowerStr = "0" + lowerStr;
						}
						playerUUID = upperStr + lowerStr;
					}
					
					ExprValue propertiesVal = children.getOrDefault("properties", null);
					if(propertiesVal != null) {
						Map<String, ExprValue> propertiesVals = propertiesVal.getChildren();
						
						for(Entry<String, ExprValue> entry : propertiesVals.entrySet()) {
							Map<String, ExprValue> propertyVals = entry.getValue().getChildren();
							ExprValue nameVal2 = propertyVals.getOrDefault("name", null);
							if(nameVal2 == null)
								continue;
							String nameVal2Str = nameVal2.asString();
							if(!nameVal2Str.equals("textures"))
								continue;
							
							ExprValue valueVal = propertyVals.getOrDefault("value", null);
							if(valueVal == null)
								continue;
							
							try {
								String textureDataStr = valueVal.asString();
								textureDataStr = new String(Base64.getDecoder().decode(textureDataStr));
								JsonElement textureDataEl = Json.readString(textureDataStr);
								
								if(textureDataEl.isJsonObject())
									skinURL = getSkinURL(textureDataEl.getAsJsonObject());
							}catch(Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
				
				if(skinURL == null) {
					if(playerUUID == null) {
						if(playerName != null) {
							// Query the playerUUID
							if(!MCWorldExporter.offlineMode) {
								try {
									JsonElement data = Json.read(new URI("https://api.mojang.com/users/profiles/minecraft/" + 
																			playerName).toURL(), true);
									if(data != null) {
										if(data.isJsonObject()) {
											JsonElement idEl = data.getAsJsonObject().get("id");
											if(idEl != null) {
												playerUUID = idEl.getAsString();
											}
										}
									}
								}catch(Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}
					
					if(playerUUID != null) {
						// Query player skin.
						if(!MCWorldExporter.offlineMode) {
							try {
								JsonElement data = Json.read(new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + 
										playerUUID).toURL(), true);
								if(data != null) {
									if(data.isJsonObject()) {
										JsonArray propertiesArray = data.getAsJsonObject().getAsJsonArray("properties");
										if(propertiesArray != null) {
											for(JsonElement el : propertiesArray.asList()) {
												if(!el.isJsonObject())
													continue;
												JsonElement nameEl = el.getAsJsonObject().get("name");
												if(nameEl == null)
													continue;
												if(!nameEl.getAsString().equals("textures"))
													continue;
												JsonElement valueEl = el.getAsJsonObject().get("value");
												if(valueEl == null)
													continue;
												try {
													String textureDataStr = valueEl.getAsString();
													textureDataStr = new String(Base64.getDecoder().decode(textureDataStr));
													JsonElement textureDataEl = Json.readString(textureDataStr);
													
													if(textureDataEl.isJsonObject())
														skinURL = getSkinURL(textureDataEl.getAsJsonObject());
												}catch(Exception ex) {
													ex.printStackTrace();
												}
											}
										}
									}
								}
							}catch(Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
				
				
				if(skinURL != null) {
					texId = GeneratedTextures.downloadTexture("skins/" + Integer.toHexString(skinURL.hashCode()), skinURL);
				}
			}
			return new ExprValue(new ExprValueString(texId));
		}
		
	}
	
	private static final ExprValue VALUE_MATH = new ExprValue(new ExprValueClassMath(), true);
	private static final ExprValue VALUE_DICT = new ExprValue(new ExprValueClassDict(), true);
	private static final ExprValue VALUE_BLOCK = new ExprValue(new ExprValueClassBlock(), true);
	private static final ExprValue VALUE_VALUE = new ExprValue(new ExprValueClassValue(), true);
	private static final ExprValue VALUE_TEXTURES = new ExprValue(new ExprValueClassTextures(), true);
	
	public static final Map<String, ExprValue> VALUE_BUILTINS = new HashMap<String, ExprValue>();
	static {
		VALUE_BUILTINS.put("Math", VALUE_MATH);
		VALUE_BUILTINS.put("Dict", VALUE_DICT);
		VALUE_BUILTINS.put("Block", VALUE_BLOCK);
		VALUE_BUILTINS.put("Value", VALUE_VALUE);
		VALUE_BUILTINS.put("Textures", VALUE_TEXTURES);
	}

}
