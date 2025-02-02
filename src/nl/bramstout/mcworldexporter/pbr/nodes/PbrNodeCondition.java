package nl.bramstout.mcworldexporter.pbr.nodes;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrImage;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Boundary;
import nl.bramstout.mcworldexporter.pbr.PbrImage.Interpolation;
import nl.bramstout.mcworldexporter.pbr.PbrImage.RGBA;
import nl.bramstout.mcworldexporter.pbr.PbrImageConstant;
import nl.bramstout.mcworldexporter.pbr.PbrImageRaster;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public class PbrNodeCondition extends PbrNode{

	public PbrAttributeImage a = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeImage b = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeImage ifFalse = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeImage ifTrue = new PbrAttributeImage(this, false, false, new PbrImageConstant(new RGBA()));
	public PbrAttributeEnum condition = new PbrAttributeEnum(this, false, false, "==",
															"==", "!=", "<", "<=", ">", ">=");
	public PbrAttributeEnum mode = new PbrAttributeEnum(this, false, false, "r", 
															"r", "g", "b", "a", "individually");
	public PbrAttributeEnum interpolation = new PbrAttributeEnum(this, false, false, "nearest", 
															"nearest", "linear", "bicubic");
	public PbrAttributeEnum boundary = new PbrAttributeEnum(this, false, false, "repeat", 
															"empty", "clip", "repeat");
	public PbrAttributeImage output = new PbrAttributeImage(this, true, false, new PbrImageConstant(new RGBA()));
	
	public PbrNodeCondition(String name, PbrNodeGraph graph) {
		super(name, graph);
	}

	@Override
	public void attributeDirty(PbrAttribute attr, PbrContext context) {
		if(attr == a || attr == b || attr == ifFalse || attr == ifTrue || attr == condition || attr == mode || 
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

			int condition = this.condition.getIndexValue(context);
			int mode = this.mode.getIndexValue(context);
			
			PbrImage imgA = a.getImageValue(context);
			PbrImage imgB = b.getImageValue(context);
			PbrImage imgIfFalse = ifFalse.getImageValue(context);
			PbrImage imgIfTrue = ifTrue.getImageValue(context);

			int maxWidth = Math.max(Math.max(Math.max(imgA.getWidth(), imgB.getWidth()), imgIfFalse.getWidth()), imgIfTrue.getWidth());
			int maxHeight = Math.max(Math.max(Math.max(imgA.getHeight(), imgB.getHeight()), imgIfFalse.getHeight()), imgIfTrue.getHeight());
			PbrImage outImg = null;
			if (maxWidth <= 1 && maxHeight <= 1)
				outImg = new PbrImageConstant(new RGBA());
			else
				outImg = new PbrImageRaster(maxWidth, maxHeight);

			RGBA cA = new RGBA();
			RGBA cB = new RGBA();
			RGBA cIfFalse = new RGBA();
			RGBA cIfTrue = new RGBA();
			RGBA cOut = new RGBA();
			for (float j = 0.5f; j < maxHeight; j += 1f) {
				for (float i = 0.5f; i < maxWidth; i += 1f) {
					imgA.sample((i / ((float) maxWidth)) * ((float) imgA.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgA.getHeight()), boundary, interpolation, cA);
					imgB.sample((i / ((float) maxWidth)) * ((float) imgB.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgB.getHeight()), boundary, interpolation, cB);
					imgIfFalse.sample((i / ((float) maxWidth)) * ((float) imgIfFalse.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgIfFalse.getHeight()), boundary, interpolation, cIfFalse);
					imgIfTrue.sample((i / ((float) maxWidth)) * ((float) imgIfTrue.getWidth()),
							(j / ((float) maxHeight)) * ((float) imgIfTrue.getHeight()), boundary, interpolation, cIfTrue);

					
					if(mode == 4) {
						// All channels individually
						cOut.r = checkCondition(cA, cB, condition, 0) ? cIfTrue.r : cIfFalse.r;
						cOut.g = checkCondition(cA, cB, condition, 1) ? cIfTrue.g : cIfFalse.g;
						cOut.b = checkCondition(cA, cB, condition, 2) ? cIfTrue.b : cIfFalse.b;
						cOut.a = checkCondition(cA, cB, condition, 3) ? cIfTrue.a : cIfFalse.a;
					}else {
						// A single channel
						boolean res = checkCondition(cA, cB, condition, mode);
						cOut = res ? cIfTrue : cIfFalse;
					}
					outImg.write((int) i, (int) j, Boundary.EMPTY, cOut);
				}
			}

			output.setValue(outImg, context);
		}
	}
	
	private boolean checkCondition(RGBA a, RGBA b, int condition, int mode) {
		switch(mode) {
		case 0:
			return checkCondition2(a.r, b.r, condition);
		case 1:
			return checkCondition2(a.g, b.g, condition);
		case 2:
			return checkCondition2(a.b, b.b, condition);
		case 3:
			return checkCondition2(a.a, b.a, condition);
		default:
			return false;
		}
	}
	
	private boolean checkCondition2(float a, float b, int condition) {
		switch(condition) {
		case 0:
			return Math.abs(a - b) < 0.000001f;
		case 1:
			return Math.abs(a - b) >= 0.000001f;
		case 2:
			return a < b && Math.abs(a - b) >= 0.000001f;
		case 3:
			return a <= b || Math.abs(a - b) < 0.000001f;
		case 4:
			return a > b && Math.abs(a - b) >= 0.000001f;
		case 5:
			return a >= b || Math.abs(a - b) < 0.000001f;
		default:
			return false;
		}
	}
	
	@Override
	public PbrNode newInstanceOfSameType(PbrNodeGraph graph) {
		return new PbrNodeCondition(getName(), graph);
	}

}
