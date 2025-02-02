package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeFloat extends PbrAttribute{

	private float value;
	private float defaultValue;
	
	public PbrAttributeFloat(PbrNode node, boolean isOutput, boolean isContextSensitive, float defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return Float.valueOf(getFloatValue(context));
	}
	
	public float getFloatValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			Float val = (Float) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val.floatValue();
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return Float.valueOf(defaultValue);
	}
	
	public float getDefaultFloatValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		float newValue = 0f;
		if(value instanceof Number) {		
			newValue = ((Number) value).floatValue();
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue() ? 1f : 0f;
		}else if(value instanceof String) {
			try {
				newValue = Float.parseFloat((String) value);
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newValue = rgba.r;
		}else if(value instanceof RGBA) {
			newValue = ((RGBA) value).r;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(context == null)
			this.value = newValue;
		else
			context.valueCache.put(this, Float.valueOf(newValue));
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeFloat) other).value;
	}
	
}
