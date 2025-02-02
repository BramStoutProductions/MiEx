package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeString extends PbrAttribute{
	
	private String value;
	private String defaultValue;
	
	public PbrAttributeString(PbrNode node, boolean isOutput, boolean isContextSensitive, String defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return getStringValue(context);
	}
	
	public String getStringValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			String val = (String) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val;
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	public String getDefaultStringValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		String newValue = null;
		if(value instanceof Number) {		
			newValue = ((Number) value).toString();
		}else if(value instanceof Boolean) {
			newValue = ((Boolean) value).booleanValue() ? "true" : "false";
		}else if(value instanceof String) {
			newValue = (String) value;
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newValue = Float.toString(rgba.r);
		}else if(value instanceof RGBA) {
			newValue = Float.toString(((RGBA) value).r);
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(newValue != null) {
			if(context == null)
				this.value = newValue;
			else
				context.valueCache.put(this, newValue);
		}
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value = ((PbrAttributeString) other).value;
	}
	
}
