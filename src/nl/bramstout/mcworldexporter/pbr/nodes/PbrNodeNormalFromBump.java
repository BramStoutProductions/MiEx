package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeNormalFromBump extends PbrNode{

	public PbrAttributeImage input = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeFloat imageSizeInUnits = new PbrAttributeFloat(this, false, false, 16f);
	public PbrAttributeFloat bumpHeightInUnits = new PbrAttributeFloat(this, false, false, 1f);
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat", 
														"empty", "clip", "repeat");
	public PbrAttributeEnum derivativeMode = new PbrAttributeEnum(this, false, false, "both",
														"left", "right", "both");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeNormalFromBump(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == input || attr == imageSizeInUnits || attr == bumpHeightInUnits || attr == boundary || attr == derivativeMode)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if(attr == output) {
			PbrImage input = this.input.getImageValue(context);
			float imageSizeInUnits = this.imageSizeInUnits.getFloatValue(context);
			float bumpHeightInUnits = this.bumpHeightInUnits.getFloatValue(context);
			int derivativeMode = this.derivativeMode.getIndexValue(context);
			
			Boundary boundary = Boundary.REPEAT;
			int boundaryIndex = this.boundary.getIndexValue(context);
			if (boundaryIndex == 0)
				boundary = Boundary.EMPTY;
			else if (boundaryIndex == 1)
				boundary = Boundary.CLIP;
			else if (boundaryIndex == 2)
				boundary = Boundary.REPEAT;
			
			PbrImage outImg = input.copy();
			
			RGBA rgba = new RGBA();
			float dx = 0f;
			float dy = 0f;
			float nx = 0f;
			float ny = 0f;
			float nz = 0f;
			float length = 0f;
			for(int j = 0; j < outImg.getHeight(); ++j) {
				for(int i = 0; i < outImg.getWidth(); ++i) {
					dx = calcDX(input, i, j, imageSizeInUnits, bumpHeightInUnits, derivativeMode, boundary, rgba);
					dy = calcDY(input, i, j, imageSizeInUnits, bumpHeightInUnits, derivativeMode, boundary, rgba);
					// Invert dy since (0,0) is top-left for PbrImage but bottom-left for normal maps.
					dy *= -1f;
					
					nx = -dx;
					ny = -dy;
					nz = -1f;
					length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
					nx /= length;
					ny /= length;
					nz /= length;
					
					// Remap to 0-1
					nx = (nx + 1f) / 2f;
					ny = (ny + 1f) / 2f;
					nz = (-nz + 1f) / 2f;
					rgba.r = nx;
					rgba.g = ny;
					rgba.b = nz;
					rgba.a = 1f;
					outImg.write(i, j, boundary, rgba);
				}
			}
			
			output.setValue(outImg, context);
		}
	}
	
	private float calcDX(PbrImage img, int i, int j, float imgScale, float bumpHeight, int derivativeMode, 
						Boundary boundary, RGBA rgba) {
		float rawDX = calcRawDelta(img, i, j, 1, 0, derivativeMode, boundary, rgba);
		rawDX *= bumpHeight;
		float pixelSize = imgScale / ((float) img.getWidth());
		return rawDX / pixelSize;
	}
	
	private float calcDY(PbrImage img, int i, int j, float imgScale, float bumpHeight, int derivativeMode, 
						Boundary boundary, RGBA rgba) {
		float rawDY = calcRawDelta(img, i, j, 0, 1, derivativeMode, boundary, rgba);
		rawDY *= bumpHeight;
		float pixelSize = imgScale / ((float) img.getWidth());
		return rawDY / pixelSize;
	}
	
	private float calcRawDelta(PbrImage img, int i, int j, int dx, int dy, int derivativeMode, Boundary boundary, RGBA rgba) {
		float v0 = 0f;
		float v1 = 0f;
		float v2 = 0f;
		switch(derivativeMode) {
		case 0:
			img.sample(i, j, boundary, rgba);
			v1 = rgba.r;
			img.sample(i - dx, j - dy, boundary, rgba);
			v0 = rgba.r;
			return v1 - v0;
		case 1:
			img.sample(i, j, boundary, rgba);
			v0 = rgba.r;
			img.sample(i + dx, j + dy, boundary, rgba);
			v1 = rgba.r;
			return v1 - v0;
		case 2:
			img.sample(i, j, boundary, rgba);
			v1 = rgba.r;
			img.sample(i - dx, j - dy, boundary, rgba);
			v0 = rgba.r;
			img.sample(i + dx, j + dy, boundary, rgba);;
			v2 = rgba.r;
			return ((v2 - v1) + (v1 - v0)) / 2f;
		default:
			return 0f;
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeNormalFromBump(getName(), graph);
	}

}
