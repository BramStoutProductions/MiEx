package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrAttributeImage extends PbrAttribute{

	private PbrImage value;
	private PbrImageConstant defaultValue;
	
	protected PbrAttributeImage(PbrNode node, boolean isOutput, boolean isContextSensitive, PbrImageConstant defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = defaultValue;
		this.defaultValue = defaultValue;
	}

	@Override
	public Object getValue(PbrContext context) {
		return getImageValue(context);
	}
	
	public PbrImage getImageValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			PbrImage val = (PbrImage) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val;
		}
		return value;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	public PbrImage getDefaultImageValue() {
		return defaultValue;
	}

	@Override
	public void setValue(Object value, PbrContext context) {
		PbrImage newValue = null;
		if(value instanceof Number) {		
			float fv = ((Number) value).floatValue();
			newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
		}else if(value instanceof Boolean) {
			float fv = ((Boolean) value).booleanValue() ? 1f : 0f;
			newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
		}else if(value instanceof String) {
			try {
				float fv = Float.parseFloat((String) value);
				newValue = new PbrImageConstant(new RGBA(fv, fv, fv, fv));
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			newValue = (PbrImage) value;
		}else if(value instanceof RGBA) {
			newValue = new PbrImageConstant((RGBA) value);
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
		this.value = ((PbrAttributeImage) other).value;
	}

}
