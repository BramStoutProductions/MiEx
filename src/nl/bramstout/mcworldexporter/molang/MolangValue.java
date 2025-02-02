package nl.bramstout.mcworldexporter.molang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

public class MolangValue {
	
	public static abstract class MolangValueImpl{
		public abstract float asNumber(MolangContext context);
		public abstract boolean asBoolean(MolangContext context);
		public abstract String asString(MolangContext context);
		public abstract MolangValue getField(MolangContext context, String name);
		public abstract List<String> getCallParameters();
		public abstract MolangValue call(MolangContext context, List<MolangValue> arguments);
		public abstract MolangValueImpl copy();
		public abstract boolean equal(MolangContext context, MolangValueImpl other);
	}
	
	public static class MolangNumber extends MolangValueImpl{
		
		private float value;
		
		public MolangNumber() {
			this.value = 0f;
		}
		
		public MolangNumber(float value) {
			this.value = value;
		}
		
		public MolangNumber(int value) {
			this((float) value);
		}
		
		public MolangNumber(String value) {
			this(Float.parseFloat(value));
		}
		
		public MolangNumber(boolean value) {
			this(value ? 1f : 0f);
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return value;
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return value != 0.0f;
		}
		
		@Override
		public String asString(MolangContext context) {
			return Float.toString(value);
		}
		
		public void set(float value) {
			this.value = value;
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call a number");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			throw new RuntimeException("Number has no fields.");
		}
		
		@Override
		public MolangValueImpl copy() {
			return new MolangNumber(value);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			return value == other.asNumber(context);
		}
	}
	
	public static class MolangBoolean extends MolangValueImpl{
		
		private boolean value;
		
		public MolangBoolean() {
			this.value = false;
		}
		
		public MolangBoolean(boolean value) {
			this.value = value;
		}
		
		public MolangBoolean(float value) {
			this.value = value != 0.0f;
		}
		
		public MolangBoolean(int value) {
			this.value = value != 0;
		}
		
		public MolangBoolean(String value) {
			this.value = !(value.contains("false") || value.contains("0"));
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return value;
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return value ? 1f : 0f;
		}
		
		@Override
		public String asString(MolangContext context) {
			return value ? "true" : "false";
		}
		
		public void set(boolean value) {
			this.value = value;
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call a boolean");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			throw new RuntimeException("Boolean has no fields.");
		}
		
		@Override
		public MolangValueImpl copy() {
			return new MolangBoolean(value);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			return value == other.asBoolean(context);
		}
		
	}
	
	public static class MolangString extends MolangValueImpl{
		
		private String value;
		
		public MolangString() {
			this.value = "";
		}
		
		public MolangString(String value) {
			this.value = value;
		}
		
		public MolangString(boolean value) {
			this.value = value ? "true" : "false";
		}
		
		public MolangString(float value) {
			this.value = Float.toString(value);
		}
		
		public MolangString(int value) {
			this.value = Integer.toString(value);
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return !(value.contains("false") || value.contains("0"));
		}
		
		@Override
		public float asNumber(MolangContext context) {
			try {
				return Float.parseFloat(value);
			}catch(Exception ex) {}
			return 0f;
		}
		
		@Override
		public String asString(MolangContext context) {
			return value;
		}
		
		public void set(String value) {
			this.value = value;
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call a string");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			throw new RuntimeException("String has no fields.");
		}
		
		@Override
		public MolangValueImpl copy() {
			return new MolangString(value);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			return value.equals(other.asString(context));
		}
	}
	
	public static class MolangArray extends MolangValueImpl{
		
		private List<MolangValue> values;
		
		public MolangArray() {
			values = new ArrayList<MolangValue>();
		}
		
		public MolangArray(List<MolangValue> values) {
			this.values = values;
		}
		
		public MolangArray(MolangValue... value) {
			this.values = Arrays.asList(value);
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return this.values.size() > 0;
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return 0;
		}
		
		@Override
		public String asString(MolangContext context) {
			return "";
		}
		
		public void set(List<MolangValue> values) {
			this.values = new ArrayList<MolangValue>(values);
		}
		
		public List<MolangValue> get(){
			return values;
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call an array");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			if(name.startsWith("[") && name.endsWith("]")) {
				try {
					int index = Integer.parseInt(name.substring(1, name.length()-1));
					if(index < 0)
						index = 0;
					if(index >= values.size())
						index = index % values.size();
					return values.get(index);
				}catch(Exception ex) {}
			}
			return null;
		}
		
		@Override
		public MolangValueImpl copy() {
			List<MolangValue> copyValues = new ArrayList<MolangValue>();
			for(MolangValue value : values)
				copyValues.add(value.copy());
			return new MolangArray(copyValues);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			if(other instanceof MolangArray) {
				if(values.size() != ((MolangArray)other).values.size())
					return false;
				for(int i = 0; i < values.size(); ++i) {
					if(!values.get(i).equals(((MolangArray)other).values.get(i)))
						return false;
				}
				return true;
			}
			return false;
		}
		
	}
	
	public static class MolangObject extends MolangValueImpl{
		
		private Map<String, MolangValue> fields;
		
		public MolangObject() {
			this.fields = new HashMap<String, MolangValue>();
		}
		
		public MolangObject(Map<String, MolangValue> fields) {
			this.fields = fields;
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return true;
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return 0;
		}
		
		@Override
		public String asString(MolangContext context) {
			return "";
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call an object");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			return fields.getOrDefault(name, null);
		}
		
		public Map<String, MolangValue> getFields(){
			return fields;
		}
		
		@Override
		public MolangValueImpl copy() {
			Map<String, MolangValue> copyFields = new HashMap<String, MolangValue>();
			for(Entry<String, MolangValue> entry : fields.entrySet()) {
				copyFields.put(entry.getKey(), entry.getValue().copy());
			}
			return new MolangObject(copyFields);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			if(other instanceof MolangObject) {
				if(fields.size() != ((MolangObject)other).fields.size())
					return false;
				for(Entry<String, MolangValue> entry : fields.entrySet()) {
					MolangValue otherVal = ((MolangObject)other).fields.get(entry.getKey());
					if(otherVal == null)
						return false;
					if(!entry.getValue().equals(otherVal))
						return false;
				}
				return true;
			}
			return false;
		}
		
	}
	
	public static class MolangDictionary extends MolangValueImpl{
		
		private Map<String, MolangValue> fields;
		
		public MolangDictionary() {
			this.fields = new HashMap<String, MolangValue>();
		}
		
		public MolangDictionary(Map<String, MolangValue> fields) {
			this.fields = fields;
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return true;
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return 0;
		}
		
		@Override
		public String asString(MolangContext context) {
			return "";
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call an object");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			MolangValue val = fields.getOrDefault(name, null);
			if(val == null) {
				val = new MolangValue(new MolangNull());
				fields.put(name, val);
			}
			return val;
		}
		
		public Map<String, MolangValue> getFields(){
			return fields;
		}
		
		@Override
		public MolangValueImpl copy() {
			Map<String, MolangValue> copyFields = new HashMap<String, MolangValue>();
			for(Entry<String, MolangValue> entry : fields.entrySet()) {
				copyFields.put(entry.getKey(), entry.getValue().copy());
			}
			return new MolangDictionary(copyFields);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			if(other instanceof MolangObject) {
				if(fields.size() != ((MolangObject)other).fields.size())
					return false;
				for(Entry<String, MolangValue> entry : fields.entrySet()) {
					MolangValue otherVal = ((MolangObject)other).fields.get(entry.getKey());
					if(otherVal == null)
						return false;
					if(!entry.getValue().equals(otherVal))
						return false;
				}
				return true;
			}
			return false;
		}
		
	}
	
	public static class MolangFunction extends MolangValueImpl{
		
		private MolangScript code;
		private List<String> parameters;
		
		public MolangFunction() {
			this.code = null;
			this.parameters = new ArrayList<String>();
		}
		
		public MolangFunction(MolangScript code, List<String> parameters) {
			this.code = code;
			this.parameters = parameters;
		}
		
		public MolangFunction(MolangScript code, String... parameters) {
			this.code = code;
			this.parameters = Arrays.asList(parameters);
		}
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return call(context, null).asBoolean(context);
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return call(context, null).asNumber(context);
		}
		
		@Override
		public String asString(MolangContext context) {
			return call(context, null).asString(context);
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			return call(context, null).getField(context, name);
		}
		
		@Override
		public List<String> getCallParameters() {
			return parameters;
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			if(context == null)
				return new MolangValue();
			context.startScope();
			
			if(arguments != null) {
				for(int i = 0; i < Math.min(arguments.size(), parameters.size()); ++i) {
					context.getTempDict().getField(context, parameters.get(i)).set(arguments.get(i));
				}
				for(int i = 0; i < arguments.size(); ++i)
					context.getTempDict().getField(context, "arg_" + i).set(arguments.get(i));
			}
			
			MolangValue returnValue = code.eval(context);
			context.clearReturnValue();
			
			context.endScope();
			
			return returnValue;
		}
		
		@Override
		public MolangValueImpl copy() {
			return new MolangFunction(code, parameters);
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			if(other instanceof MolangFunction) {
				return call(context, null).equal(context, other.call(context, null));
			}else {
				return call(context, null).equal(context, other);
			}
		}
		
	}
	
	public static class MolangNull extends MolangValueImpl{
		
		@Override
		public boolean asBoolean(MolangContext context) {
			return false;
		}
		
		@Override
		public float asNumber(MolangContext context) {
			return 0f;
		}
		
		@Override
		public String asString(MolangContext context) {
			return "";
		}
		
		@Override
		public MolangValue call(MolangContext context, List<MolangValue> arguments) {
			throw new RuntimeException("Cannot call a null value");
		}
		
		@Override
		public List<String> getCallParameters() {
			return new ArrayList<String>();
		}
		
		@Override
		public MolangValue getField(MolangContext context, String name) {
			throw new RuntimeException("Null has no fields.");
		}
		
		@Override
		public MolangValueImpl copy() {
			return this;
		}
		
		@Override
		public boolean equal(MolangContext context, MolangValueImpl other) {
			return other instanceof MolangNull;
		}
		
	}
	
	private MolangValueImpl value;
	
	public MolangValue() {
		value = new MolangNull();
	}
	
	public MolangValue(MolangValue value) {
		this.value = value.value.copy();
	}
	
	public MolangValue(MolangValueImpl value) {
		this.value = value.copy();
	}
	
	public MolangValue(float value) {
		this.value = new MolangNumber(value);
	}
	
	public MolangValue(int value) {
		this.value = new MolangNumber(value);
	}
	
	public MolangValue(boolean value) {
		this.value = new MolangBoolean(value);
	}
	
	public MolangValue(String value) {
		this.value = new MolangString(value);
	}
	
	public MolangValue(List<MolangValue> values) {
		this.value = new MolangArray();
		for(MolangValue value : values)
			((MolangArray) this.value).values.add(value.copy());
	}
	
	public MolangValue(MolangValue... values) {
		this.value = new MolangArray(values);
	}
	
	public MolangValue(Map<String, MolangValue> values) {
		this.value = new MolangObject(values);
	}
	
	public MolangValue copy() {
		return new MolangValue(this);
	}
	
	public float asNumber() {
		return this.value.asNumber(null);
	}
	public boolean asBoolean() {
		return this.value.asBoolean(null);
	}
	public String asString() {
		return this.value.asString(null);
	}
	public float asNumber(MolangContext context) {
		return this.value.asNumber(context);
	}
	public boolean asBoolean(MolangContext context) {
		return this.value.asBoolean(context);
	}
	public String asString(MolangContext context) {
		return this.value.asString(context);
	}
	public MolangValue getField(String name) {
		return this.value.getField(null, name);
	}
	public MolangValue getField(MolangContext context, String name) {
		return this.value.getField(context, name);
	}
	public List<String> getCallParameters(){
		return this.value.getCallParameters();
	}
	public MolangValue call(MolangContext context, List<MolangValue> arguments) {
		return this.value.call(context, arguments);
	}
	
	public void set(MolangValue value) {
		this.value = value.value.copy();
	}
	
	public boolean isNull() {
		return this.value instanceof MolangNull;
	}
	
	public MolangValueImpl getImpl() {
		return value;
	}
	
	public boolean equal(MolangContext context, MolangValue other) {
		return this.value.equal(context, other.value);
	}
	
	public boolean equal(MolangContext context, MolangValueImpl other) {
		return this.value.equal(context, other);
	}
	
	public static MolangValue fromNBT(NbtTag tag) {
		if(tag instanceof NbtTagByte) {
			return new MolangValue((int) ((NbtTagByte)tag).getData());
		}
		if(tag instanceof NbtTagShort) {
			return new MolangValue((int) ((NbtTagShort)tag).getData());
		}
		if(tag instanceof NbtTagInt) {
			return new MolangValue((int) ((NbtTagInt)tag).getData());
		}
		if(tag instanceof NbtTagLong) {
			return new MolangValue((float) ((NbtTagLong)tag).getData());
		}
		if(tag instanceof NbtTagFloat) {
			return new MolangValue(((NbtTagFloat)tag).getData());
		}
		if(tag instanceof NbtTagDouble) {
			return new MolangValue((float) ((NbtTagDouble)tag).getData());
		}
		if(tag instanceof NbtTagString) {
			return new MolangValue(((NbtTagString)tag).getData());
		}
		if(tag instanceof NbtTagByteArray) {
			List<MolangValue> values = new ArrayList<MolangValue>();
			for(byte val : ((NbtTagByteArray)tag).getData())
				values.add(new MolangValue((int) val));
			return new MolangValue(new MolangArray(values));
		}
		if(tag instanceof NbtTagIntArray) {
			List<MolangValue> values = new ArrayList<MolangValue>();
			for(int val : ((NbtTagIntArray)tag).getData())
				values.add(new MolangValue(val));
			return new MolangValue(new MolangArray(values));
		}
		if(tag instanceof NbtTagLongArray) {
			List<MolangValue> values = new ArrayList<MolangValue>();
			for(long val : ((NbtTagLongArray)tag).getData())
				values.add(new MolangValue((float) val));
			return new MolangValue(new MolangArray(values));
		}
		if(tag instanceof NbtTagList) {
			List<MolangValue> values = new ArrayList<MolangValue>();
			for(NbtTag val : ((NbtTagList)tag).getData())
				values.add(fromNBT(val));
			return new MolangValue(new MolangArray(values));
		}
		if(tag instanceof NbtTagCompound) {
			Map<String, MolangValue> fields = new HashMap<String, MolangValue>();
			int numItems = ((NbtTagCompound) tag).getSize();
			for(int i = 0; i < numItems; ++i) {
				NbtTag val = ((NbtTagCompound) tag).get(i);
				fields.put(val.getName(), fromNBT(val));
			}
			return new MolangValue(new MolangObject(fields));
		}
		return new MolangValue();
	}
	
}
