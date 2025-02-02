package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeMatrix extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeRGBA outR = new PbrAttributeRGBA(this, false, false, new RGBA(1f, 0f, 0f, 0f));
	public PbrAttributeRGBA outG = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 1f, 0f, 0f));
	public PbrAttributeRGBA outB = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 1f, 0f));
	public PbrAttributeRGBA outA = new PbrAttributeRGBA(this, false, false, new RGBA(0f, 0f, 0f, 1f));
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeMatrix(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == outR || attr == outG || attr == outB || attr == outA)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			RGBA outR = this.outR.getRGBAValue(context);
			RGBA outG = this.outG.getRGBAValue(context);
			RGBA outB = this.outB.getRGBAValue(context);
			RGBA outA = this.outA.getRGBAValue(context);
			
			PbrImage outImg = input.getImageValue(context).copy();
			
			RGBA rgba = new RGBA();
			RGBA rgba2 = new RGBA();
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					outImg.sample(i, j, Boundary.EMPTY, rgba);
					rgba2.r = rgba.r * outR.r + rgba.g * outR.g + rgba.b * outR.b + rgba.a * outR.a;
					rgba2.g = rgba.r * outG.r + rgba.g * outG.g + rgba.b * outG.b + rgba.a * outG.a;
					rgba2.b = rgba.r * outB.r + rgba.g * outB.g + rgba.b * outB.b + rgba.a * outB.a;
					rgba2.a = rgba.r * outA.r + rgba.g * outA.g + rgba.b * outA.b + rgba.a * outA.a;
					outImg.write(i, j, Boundary.EMPTY, rgba2);
				}
			}
			output.setValue(outImg, context);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeMatrix(getName(), graph);
	}

}
