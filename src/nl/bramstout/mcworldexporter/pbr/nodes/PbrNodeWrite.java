package nl.bramstout.mcworldexporter.pbr.nodes;

import java.io.File;

import nl.bramstout.mcworldexporter.image.ImageWriter;
import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeWrite extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeString imageName = new PbrAttributeString(this, false, false, "@texture@_pbr");
	public PbrAttributeBoolean applyGamma = new PbrAttributeBoolean(this, false, false, false);
	public PbrAttributeBoolean isTemporary = new PbrAttributeBoolean(this, false, false, false);
	
	public PbrNodeWrite(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {		
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == null) {
			PbrImage img = input.getImageValue(context);
			String imageName = this.imageName.getStringValue(context);
			if(imageName.equals("@texture@") && context.saveToResourcePack == null)
				throw new RuntimeException("Cannot overwrite current texture");
			File file = context.getTexture(imageName, true, true);
			file.getParentFile().mkdirs();
			boolean applyGamma = this.applyGamma.getBooleanValue(context);
			if(applyGamma) {
				img = img.copy();
				RGBA rgba = new RGBA();
				for(int j = 0; j < img.getHeight(); ++j) {
					for(int i = 0; i < img.getWidth(); ++i) {
						img.sample(i, j, Boundary.EMPTY, rgba);
						rgba.pow(1f/2.2f, 1f);
						img.write(i, j, Boundary.EMPTY, rgba);
					}
				}
			}
			ImageWriter.writeImage(file, img);
			
			boolean isTemporary = this.isTemporary.getBooleanValue(context);
			if(isTemporary)
				context.temporaryFiles.add(file);
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeWrite(getName(), graph);
	}

}
