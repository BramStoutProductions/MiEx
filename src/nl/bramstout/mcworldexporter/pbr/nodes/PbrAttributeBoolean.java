package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeBoolean extends PbrAttribute{

	private boolean value;
	private boolean defaultValue;
	
	public PbrAttributeBoolean(PbrNode node, boolean isOutput, boolean isContextSensitive, boolean defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return Boolean.valueOf(getBooleanValue(context));
	}
	
	public boolean getBooleanValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			Boolean val = (Boolean) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val.booleanValue();
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return Boolean.valueOf(defaultValue);
	}
	
	public boolean getDefaultBooleanValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		boolean newValue = false;
		if(value instanceof Number) {		
			newValue = ((Number) value).intValue() > 0;
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue();
		}else if(value instanceof String) {
			String lv = ((String) value).toLowerCase();
			if(lv.startsWith("t") || lv.startsWith("y"))
				newValue = true;
			else if(lv.startsWith("f") || lv.startsWith("n"))
				newValue = false;
			else {
				try {
					newValue = Integer.parseInt((String) value) > 0;
				}catch(Exception ex) {
					throw new RuntimeException("Invalid value", ex);
				}
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newValue = rgba.r >= 0.5f;
		}else if(value instanceof RGBA) {
			newValue = ((RGBA) value).r >= 0.5f;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(context == null)
			this.value = newValue;
		else
			context.valueCache.put(this, Boolean.valueOf(newValue));
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeBoolean) other).value;
	}
	
}
