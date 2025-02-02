package nl.bramstout.mcworldexporter.pbr;

public class PbrImageConstant extends PbrImage{

	private RGBA value;
	
	public PbrImageConstant(RGBA value) {
		this.value = new RGBA(value);
	}
	
	@Override
	public int getWidth() {
		return 1;
	}

	@Override
	public int getHeight() {
		return 1;
	}

	@Override
	public void sample(int x, int y, Boundary boundaryMode, RGBA out) {
		out.r = value.r;
		out.g = value.g;
		out.b = value.b;
		out.a = value.a;
	}
	
	@Override
	public void write(int x, int y, Boundary boundaryMode, RGBA value) {
		this.value.r = value.r;
		this.value.g = value.g;
		this.value.b = value.b;
		this.value.a = value.a;
	}

	@Override
	public PbrImage copy() {
		return new PbrImageConstant(value);
	}

}
