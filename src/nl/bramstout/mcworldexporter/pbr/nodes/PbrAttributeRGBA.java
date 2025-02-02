package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrAttributeRGBA extends PbrAttribute{

	private RGBA value;
	private RGBA defaultValue;
	
	public PbrAttributeRGBA(PbrNode node, boolean isOutput, boolean isContextSensitive, RGBA defaultValue) {
		super(node, isOutput, isContextSensitive);
		this.value = new RGBA(defaultValue);
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Object getValue(PbrContext context) {
		return getRGBAValue(context);
	}
	
	public RGBA getRGBAValue(PbrContext context) {
		checkDirty(context);
		if(context != null) {
			RGBA val = (RGBA) context.valueCache.getOrDefault(this, null);
			if(val != null)
				return val;
		}
		return value;
	}
	
	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	public RGBA getDefaultRGBAValue() {
		return defaultValue;
	}
	
	@Override
	public void setValue(Object value, PbrContext context) {
		float newR = 0f;
		float newG = 0f;
		float newB = 0f;
		float newA = 1f;
		if(value instanceof Number) {		
			float fv = ((Number) value).floatValue();
			newR = fv;
			newG = fv;
			newB = fv;
			newA = fv;
		}else if(value instanceof Boolean) {
			float fv = ((Boolean) value).booleanValue() ? 1f : 0f;
			newR = fv;
			newG = fv;
			newB = fv;
			newA = fv;
		}else if(value instanceof String) {
			try {
				float fv = Float.parseFloat((String) value);
				newR = fv;
				newG = fv;
				newB = fv;
				newA = fv;
			}catch(Exception ex) {
				throw new RuntimeException("Invalid value", ex);
			}
		}else if(value instanceof PbrImage) {
			RGBA rgba = new RGBA();
			((PbrImage) value).sample(0, 0, Boundary.EMPTY, rgba);
			newR = rgba.r;
			newG = rgba.g;
			newB = rgba.b;
			newA = rgba.a;
		}else if(value instanceof RGBA) {
			newR = ((RGBA) value).r;
			newG = ((RGBA) value).g;
			newB = ((RGBA) value).b;
			newA = ((RGBA) value).a;
		}else {
			throw new RuntimeException("Invalid value type");
		}
		if(context == null) {
			this.value.r = newR;
			this.value.g = newG;
			this.value.b = newB;
			this.value.a = newA;
		}else {
			context.valueCache.put(this, new RGBA(newR, newG, newB, newA));
		}
		notifyChange(context);
	}
	
	@Override
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		super.copyFrom(other, currentGraph);
		this.value.r = ((PbrAttributeRGBA) other).value.r;
		this.value.g = ((PbrAttributeRGBA) other).value.g;
		this.value.b = ((PbrAttributeRGBA) other).value.b;
		this.value.a = ((PbrAttributeRGBA) other).value.a;
	}
	
}
