package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeGamutMap extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA luminance = new PbrAttributeRGBA(this, false, false, new RGBA(0.25f, 0.625f, 0.125f, 0f));
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeGamutMap(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == luminance)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage outImg = input.getImageValue(context).copy();
			RGBA luminanceFactor = luminance.getRGBAValue(context);
			
			RGBA rgba = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					
					rgba.r = Math.max(rgba.r, 0f);
					rgba.g = Math.max(rgba.g, 0f);
					rgba.b = Math.max(rgba.b, 0f);
					
					float luminance = 	rgba.r * luminanceFactor.r +
										rgba.g * luminanceFactor.g + 
										rgba.b * luminanceFactor.b;
					
					float minComponent = Math.min(Math.min(Math.min(rgba.r, rgba.g), rgba.b), 0f);
					rgba.r -= minComponent;
					rgba.g -= minComponent;
					rgba.b -= minComponent;
					
					float newLuminance =rgba.r * luminanceFactor.r +
										rgba.g * luminanceFactor.g + 
										rgba.b * luminanceFactor.b;
					if(luminance > 0.00001) {
						rgba.div(luminance, 1.0f);
						rgba.mult(newLuminance, 1.0f);
					}
					
					outImg.write(i, j, Boundary.EMPTY, rgba);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeGamutMap(getName(), graph);
	}

}
