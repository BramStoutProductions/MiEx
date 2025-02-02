package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeInt extends PbrAttribute{

	private int value;
	private int defaultValue;
	
	public PbrAttributeInt(PbrNode node, boolean isOutput, boolean isContextSensitive, int defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return Integer.valueOf(getIntValue(context));
	}
	
	public int getIntValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			Integer val = (Integer) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val.intValue();
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return Integer.valueOf(defaultValue);
	}
	
	public int getDefaultIntValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		int newValue = 0;
		if(value instanceof Number) {		
			newValue = ((Number) value).intValue();
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue() ? 1 : 0;
		}else if(value instanceof String) {
			try {
				newValue = Integer.parseInt((String) value);
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
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
		if(context == null)
			this.value = newValue;
		else
			context.valueCache.put(this, Integer.valueOf(newValue));
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeInt) other).value;
	}
	
}
