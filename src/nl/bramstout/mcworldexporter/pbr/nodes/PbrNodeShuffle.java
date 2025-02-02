package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Interpolation;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;

public class PbrNodeShuffle extends PbrNode {

	public PbrAttributeImage a = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeImage b = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeEnum outR = new PbrAttributeEnum(this, false, false, "a.r", 
														"a.r", "a.g", "a.b", "a.a",
														"b.r", "b.g", "b.b", "b.a",
														"0", "1", 
														"a.luminance", "a.maxComponent",
														"a.minComponent", "a.saturation",
														"b.luminance", "b.maxComponent",
														"b.minComponent", "b.saturation");
	public PbrAttributeEnum outG = new PbrAttributeEnum(this, false, false, "a.g", 
														"a.r", "a.g", "a.b", "a.a",
														"b.r", "b.g", "b.b", "b.a",
														"0", "1", 
														"a.luminance", "a.maxComponent",
														"a.minComponent", "a.saturation",
														"b.luminance", "b.maxComponent",
														"b.minComponent", "b.saturation");
	public PbrAttributeEnum outB = new PbrAttributeEnum(this, false, false, "a.b", 
														"a.r", "a.g", "a.b", "a.a",
														"b.r", "b.g", "b.b", "b.a",
														"0", "1", 
														"a.luminance", "a.maxComponent",
														"a.minComponent", "a.saturation",
														"b.luminance", "b.maxComponent",
														"b.minComponent", "b.saturation");
	public PbrAttributeEnum outA = new PbrAttributeEnum(this, false, false, "a.a", 
														"a.r", "a.g", "a.b", "a.a",
														"b.r", "b.g", "b.b", "b.a",
														"0", "1", 
														"a.luminance", "a.maxComponent",
														"a.minComponent", "a.saturation",
														"b.luminance", "b.maxComponent",
														"b.minComponent", "b.saturation");
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest", 
														"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat", 
														"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));

	public PbrNodeShuffle(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if (attr == a || attr == b || attr == outR || attr == outG || attr == outB || attr == outA || 
				attr == interpolation || attr == boundary)
			output.notifyChange(context);
	}

	@Override
	public void evaluate(PbrAttribute attr, PbrContext context) {
		if (attr == output) {
			Boundary boundary = Boundary.REPEAT;
			int boundaryIndex = this.boundary.getIndexValue(context);
			if (boundaryIndex == 0)
				boundary = Boundary.EMPTY;
			else if (boundaryIndex == 1)
				boundary = Boundary.CLIP;
			else if (boundaryIndex == 2)
				boundary = Boundary.REPEAT;

			Interpolation interpolation = Interpolation.NEAREST;
			int interpolationIndex = this.interpolation.getIndexValue(context);
			if (interpolationIndex == 0)
				interpolation = Interpolation.NEAREST;
			else if (interpolationIndex == 1)
				interpolation = Interpolation.LINEAR;
			else if (interpolationIndex == 2)
				interpolation = Interpolation.CUBIC;

			int outR = this.outR.getIndexValue(context);
			int outG = this.outG.getIndexValue(context);
			int outB = this.outB.getIndexValue(context);
			int outA = this.outA.getIndexValue(context);

			PbrImage imgA = a.getImageValue(context);
			PbrImage imgB = b.getImageValue(context);

			int maxWidth = Math.max(imgA.getWidth(), imgB.getWidth());
			int maxHeight = Math.max(imgA.getHeight(), imgB.getHeight());
			PbrImage outImg = null;
			if (maxWidth <= 1 && maxHeight <= 1)
				outImg = new PbrImageConstant(new RGBA());
			else
				outImg = new PbrImageRaster(maxWidth, maxHeight);

			RGBA cA = new RGBA();
			RGBA cB = new RGBA();
			RGBA cOut = new RGBA();
			for (float j = 0.5f; j < maxHeight; j += 1f) {
				for (float i = 0.5f; i < maxWidth; i += 1f) {
					imgA.sample((i / ((float) maxWidth)) * ((float) imgA.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgA.getHeight()), boundary, interpolation, cA);
					imgB.sample((i / ((float) maxWidth)) * ((float) imgB.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgB.getHeight()), boundary, interpolation, cB);

					cOut.r = shuffle(cA, cB, outR);
					cOut.g = shuffle(cA, cB, outG);
					cOut.b = shuffle(cA, cB, outB);
					cOut.a = shuffle(cA, cB, outA);
					outImg.write((int) i, (int) j, Boundary.EMPTY, cOut);
				}
			}

			output.setValue(outImg, context);
		}
	}
	
	private float shuffle(RGBA a, RGBA b, int operator) {
		switch(operator) {
		case 0:
			return a.r;
		case 1:
			return a.g;
		case 2:
			return a.b;
		case 3:
			return a.a;
		case 4:
			return b.r;
		case 5:
			return b.g;
		case 6:
			return b.b;
		case 7:
			return b.a;
		case 8:
			return 0f;
		case 9:
			return 1f;
		case 10: // a.luminance
			return a.r * 0.2126f + a.g * 0.7152f + a.b * 0.0722f;
		case 11: // a.maxComponent
			return Math.max(Math.max(a.r, a.g), a.b);
		case 12: // a.minComponent
			return Math.min(Math.min(a.r, a.g), a.b);
		case 13: // a.saturation
			return (Math.max(Math.max(a.r, a.g), a.b) - Math.min(Math.min(a.r, a.g), a.b)) / 
					Math.max(Math.max(Math.max(a.r, a.g), a.b), 0.00001f);
		case 14: // b.luminance
			return b.r * 0.2126f + b.g * 0.7152f + b.b * 0.0722f;
		case 15: // b.maxComponent
			return Math.max(Math.max(b.r, b.g), b.b);
		case 16: // b.minComponent
			return Math.min(Math.min(b.r, b.g), b.b);
		case 17: // b.saturation
			return (Math.max(Math.max(b.r, b.g), b.b) - Math.min(Math.min(b.r, b.g), b.b)) / 
					Math.max(Math.max(Math.max(b.r, b.g), b.b), 0.00001f);
		default:
			return 0f;
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeShuffle(getName(), graph);
	}

}
