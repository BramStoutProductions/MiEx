package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeRemap extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA inMin = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 0f));
	public PbrAttributeRGBA inMax = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeRGBA outMin = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 0f));
	public PbrAttributeRGBA outMax = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeRGBA gamma = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 1f, 1f, 1f));
	public PbrAttributeBoolean clamp = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeRemap(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == inMin || attr == inMax || 
				attr == outMin || attr == outMax || attr == gamma ||
				attr == clamp) {
			output.notifyChange(context);
		}
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage outImg = input.getImageValue(context).copy();
			
			RGBA inMin = this.inMin.getRGBAValue(context);
			RGBA inMax = this.inMax.getRGBAValue(context);
			RGBA outMin = this.outMin.getRGBAValue(context);
			RGBA outMax = this.outMax.getRGBAValue(context);
			RGBA gamma = this.gamma.getRGBAValue(context);
			boolean clamp = this.clamp.getBooleanValue(context);
			
			RGBA rgba = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					
					remap(rgba, inMin, inMax, outMin, outMax, gamma, clamp);
					
					outImg.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	private void remap(RGBA rgba, RGBA inMin, RGBA inMax, RGBA outMin, RGBA outMax, RGBA gamma, boolean clamp) {
		RGBA inScale = new RGBA(inMax).sub(inMin);
		RGBA outScale = new RGBA(outMax).sub(outMin);
		
		rgba.sub(inMin).div(inScale);
		if(clamp) {
			rgba.r = Math.min(Math.max(rgba.r, 0f), 1f);
			rgba.g = Math.min(Math.max(rgba.g, 0f), 1f);
			rgba.b = Math.min(Math.max(rgba.b, 0f), 1f);
			rgba.a = Math.min(Math.max(rgba.a, 0f), 1f);
		}
		rgba.pow(gamma).mult(outScale).add(outMin);
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeRemap(getName(), graph);
	}

}
