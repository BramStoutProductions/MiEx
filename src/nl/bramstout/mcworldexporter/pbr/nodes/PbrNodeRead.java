package nl.bramstout.mcworldexporter.pbr.nodes;

import java.io.File;

import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeRead extends PbrNode{
	
	public PbrAttributeString imageName = new PbrAttributeString(this, false, true, "@texture@");
	public PbrAttributeBoolean linearise = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeRGBA colorIfMissing = new PbrAttributeRGBA(this, false, false, new RGBA(-1f, -1f, -1f, -1f));
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeRead(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == imageName || attr == linearise)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			File file = context.getTexture(imageName.getStringValue(context), false, false);
			RGBA colorIfMissing = this.colorIfMissing.getRGBAValue(context);
			
			PbrImage img = null;
			if(file != null && file.exists())
				img = ImageReader.readPbrImage(file, linearise.getBooleanValue(context));
			
			if(img == null) {
				output.setValue(new PbrImageConstant(colorIfMissing), context);
			}else {
				output.setValue(img, context);
			}
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeRead(getName(), graph);
	}

}
