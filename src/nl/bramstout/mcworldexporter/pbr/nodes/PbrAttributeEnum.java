package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeEnum extends PbrAttribute{

	private int value;
	private int defaultValue;
	private String[] options;
	
	public PbrAttributeEnum(PbrNode node, boolean isOutput, boolean isContextSensitive, String defaultValue, String... options) {
		super(node, isOutput, isContextSensitive);
		this.options = options;
		this.value = getIndex(defaultValue);
		if(this.value < 0)
			this.value = 0;
		this.defaultValue = this.value;
	}
	
	private int getIndex(String val) {
		int i = 0;
		for(String str : options) {
			if(str.equalsIgnoreCase(val))
				return i;
			i++;
		}
		return -1;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return Integer.valueOf(getIndexValue(context));
	}
	
	public int getIndexValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			Integer val = (Integer) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val.intValue();
		}
		return value;
	}
	
	public String getStringValue(PbrContext context) {
		int value = getIndexValue(context);
		if(options.length <= 0)
			return "";
		return options[value];
	}
	
	@Override
	public Object getDefaultValue() {
		return Integer.valueOf(defaultValue);
	}
	
	public int getDefaultIntValue() {
		return defaultValue;
	}
	
	public String getDefaultStringValue() {
		if(options.length <= 0)
			return "";
		return options[defaultValue];
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		int newValue = 0;
		if(value instanceof Number) {		
			newValue = ((Number) value).intValue();
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue() ? 1 : 0;
		}else if(value instanceof String) {
			newValue = getIndex((String) value);
			if(newValue == -1) {
				try {
					newValue = Integer.parseInt((String) value);
				}catch(Exception ex) {
					throw new RuntimeException("Invalid value", ex);
				}
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newValue = (int) rgba.r;
		}else if(value instanceof RGBA) {
			newValue = (int) ((RGBA) value).r;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(newValue >= this.options.length)
			newValue = this.options.length-1;
		if(newValue < 0)
			newValue = 0;
		if(context == null)
			this.value = newValue;
		else
			context.valueCache.put(this, Integer.valueOf(newValue));
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		value = ((PbrAttributeEnum) other).value;
	}
	
}
