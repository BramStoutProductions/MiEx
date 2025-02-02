package nl.bramstout.mcworldexporter.pbr;

public abstract class PbrImage {
	
	/**
	 * Returns the number of horizontal pixels in this image.
	 * @return
	 */
	public abstract int getWidth();
	/**
	 * Returns the number of vertical pixels in this image.
	 * @return
	 */
	public abstract int getHeight();
	/**
	 * Samples the image at the given pixel coordinates.
	 * (0, 0) is the top-left. The boundary mode indices
	 * what to do with out-of-bounds pixel coordinates.
	 * 
	 * @param x
	 * @param y
	 * @param boundaryMode
	 * @return
	 */
	public abstract void sample(int x, int y, Boundary boundaryMode, RGBA out);
	
	public abstract void write(int x, int y, Boundary boundaryMode, RGBA value);
	
	public abstract PbrImage copy();
	
	/**
	 * Samples the image at the given pixel coordinates.
	 * (0, 0) is the top-left. The boundary mode indices
	 * what to do with out-of-bounds pixel coordinates.
	 * 
	 * This version of the sample function interpolates
	 * the pixels. Each pixel's center is (0.5, 0.5) offset
	 * from the integer coordinate.
	 * 
	 * @param x
	 * @param y
	 * @param boundaryMode
	 * @param interpolationMode
	 * @return
	 */
	public void sample(float x, float y, Boundary boundaryMode, Interpolation interpolationMode, RGBA out) {
		if(interpolationMode == Interpolation.NEAREST)
			sampleNearest(x, y, boundaryMode, out);
		else if(interpolationMode == Interpolation.LINEAR)
			sampleLinear(x, y, boundaryMode, out);
		else if(interpolationMode == Interpolation.CUBIC)
			sampleCubic(x, y, boundaryMode, out);
		else
			throw new RuntimeException("Invalid interpolation mode");
	}
	
	private void sampleNearest(float x, float y, Boundary boundaryMode, RGBA out) {
		int ix = (int) Math.floor(x);
		int iy = (int) Math.floor(y);
		sample(ix, iy, boundaryMode, out);
	}
	
	private void sampleLinear(float x, float y, Boundary boundaryMode, RGBA out) {
		x -= 0.5f;
		y -= 0.5f;
		int xMin = (int) Math.floor(x);
		int yMin = (int) Math.floor(y);
		int xMax = xMin + 1;
		int yMax = yMin + 1;
		float tX = x - ((float) xMin);
		float tY = y - ((float) yMin);
		
		RGBA c00 = out;
		RGBA c10 = new RGBA();
		RGBA c01 = new RGBA();
		RGBA c11 = new RGBA();
		
		sample(xMin, yMin, boundaryMode, c00);
		sample(xMax, yMin, boundaryMode, c10);
		sample(xMin, yMax, boundaryMode, c01);
		sample(xMax, yMax, boundaryMode, c11);
		c00.premultiply();
		c10.premultiply();
		c01.premultiply();
		c11.premultiply();
		
		c00.mult(1f - tX).add(c10.mult(tX));
		c01.mult(1f - tX).add(c11.mult(tX));
		c00.mult(1f - tY).add(c01.mult(tY));
		c00.unpremultiply();
	}
	
	private void sampleCubic(float x, float y, Boundary boundaryMode, RGBA out) {
		x -= 0.5f;
		y -= 0.5f;
		int xMin = (int) Math.floor(x);
		int yMin = (int) Math.floor(y);
		int xMax = xMin + 1;
		int yMax = yMin + 1;
		float tX = x - ((float) xMin);
		float tY = y - ((float) yMin);
		
		float x0 = (2f * tX * tX * tX) - (3f * tX * tX) + 1f;
		float dx0 = (tX * tX * tX) - (2f * tX * tX) + tX;
		float x1 = (-2f * tX * tX * tX) + (3f * tX * tX);
		float dx1 = (tX * tX * tX) - (tX * tX);
		
		float y0 = (2f * tY * tY * tY) - (3f * tY * tY) + 1f;
		float dy0 = (tY * tY * tY) - (2f * tY * tY) + tY;
		float y1 = (-2f * tY * tY * tY) + (3f * tY * tY);
		float dy1 = (tY * tY * tY) - (tY * tY);
		
		RGBA c0 = out;
		RGBA c1 = new RGBA();
		RGBA c2 = new RGBA();
		RGBA c3 = new RGBA();
		RGBA c01 = new RGBA();
		RGBA c02 = new RGBA();
		RGBA c03 = new RGBA();
		
		sample(xMin - 1, yMin - 1, boundaryMode, c0);
		sample(xMin    , yMin - 1, boundaryMode, c01);
		sample(xMax    , yMin - 1, boundaryMode, c02);
		sample(xMax + 1, yMin - 1, boundaryMode, c03);
		c0.premultiply();
		c01.premultiply();
		c02.premultiply();
		c03.premultiply();
		cubicInterpolate(c0, c01, c02, c03, x0, dx0, x1, dx1);
		
		sample(xMin - 1, yMin   , boundaryMode, c1);
		sample(xMin    , yMin   , boundaryMode, c01);
		sample(xMax    , yMin   , boundaryMode, c02);
		sample(xMax + 1, yMin   , boundaryMode, c03);
		c1.premultiply();
		c01.premultiply();
		c02.premultiply();
		c03.premultiply();
		cubicInterpolate(c1, c01, c02, c03, x0, dx0, x1, dx1);
		
		sample(xMin - 1, yMax   , boundaryMode, c2);
		sample(xMin    , yMax   , boundaryMode, c01);
		sample(xMax    , yMax   , boundaryMode, c02);
		sample(xMax + 1, yMax   , boundaryMode, c03);
		c2.premultiply();
		c01.premultiply();
		c02.premultiply();
		c03.premultiply();
		cubicInterpolate(c2, c01, c02, c03, x0, dx0, x1, dx1);
		
		sample(xMin - 1, yMax + 1, boundaryMode, c3);
		sample(xMin    , yMax + 1, boundaryMode, c01);
		sample(xMax    , yMax + 1, boundaryMode, c02);
		sample(xMax + 1, yMax + 1, boundaryMode, c03);
		cubicInterpolate(c3, c01, c02, c03, x0, dx0, x1, dx1);
				
		cubicInterpolate(c0, c1, c2, c3, y0, dy0, y1, dy1);
		c0.unpremultiply();
	}
	
	private void cubicInterpolate(RGBA c0, RGBA c1, RGBA c2, RGBA c3, float x0, float dx0, float x1, float dx1) {
		c0.sub(c1).mult(-1f);
		c3.sub(c2);
		c0.mult(dx0);
		c1.mult(x0);
		c2.mult(x1);
		c3.mult(dx1);
		c0.add(c1).add(c2).add(c3);
	}
	
	
	public static enum Boundary{
		
		EMPTY, CLIP, REPEAT
		
	}
	
	public static enum Interpolation{
		
		NEAREST, LINEAR, CUBIC
		
	}
	
	/**
	 * Represents a colour in unassociated alpha.
	 */
	public static class RGBA{
		
		public float r;
		public float g;
		public float b;
		public float a;
		
		public RGBA() {
			this(0f, 0f, 0f, 1f);
		}
		
		public RGBA(float grayscale) {
			this(grayscale, 1f);
		}
		
		public RGBA(float grayscale, float alpha) {
			this(grayscale, grayscale, grayscale, alpha);
		}
		
		public RGBA(float r, float g, float b) {
			this(r, g, b, 1f);
		}
		
		public RGBA(float r, float g, float b, float a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}
		
		public RGBA(RGBA other) {
			this(other.r, other.g, other.b, other.a);
		}
		
		public RGBA min(RGBA other) {
			this.r = Math.min(this.r, other.r);
			this.g = Math.min(this.g, other.g);
			this.b = Math.min(this.b, other.b);
			this.a = Math.min(this.a, other.a);
			return this;
		}
		
		public RGBA max(RGBA other) {
			this.r = Math.max(this.r, other.r);
			this.g = Math.max(this.g, other.g);
			this.b = Math.max(this.b, other.b);
			this.a = Math.max(this.a, other.a);
			return this;
		}
		
		public RGBA mix(RGBA other, float t) {
			this.r = this.r * (1f-t) + other.r * t;
			this.g = this.g * (1f-t) + other.g * t;
			this.b = this.b * (1f-t) + other.b * t;
			this.a = this.a * (1f-t) + other.a * t;
			return this;
		}
		
		public RGBA mix(RGBA other, float tRGB, float tA) {
			this.r = this.r * (1f-tRGB) + other.r * tRGB;
			this.g = this.g * (1f-tRGB) + other.g * tRGB;
			this.b = this.b * (1f-tRGB) + other.b * tRGB;
			this.a = this.a * (1f-tA) + other.a * tA;
			return this;
		}
		
		public RGBA set(RGBA other) {
			this.r = other.r;
			this.g = other.g;
			this.b = other.b;
			this.a = other.a;
			return this;
		}
		
		public RGBA set(float val) {
			this.r = val;
			this.g = val;
			this.b = val;
			this.a = val;
			return this;
		}
		
		public RGBA set(float rgb, float a) {
			this.r = rgb;
			this.g = rgb;
			this.b = rgb;
			this.a = a;
			return this;
		}
		
		public RGBA add(RGBA other) {
			this.r += other.r;
			this.g += other.g;
			this.b += other.b;
			this.a += other.a;
			return this;
		}
		
		public RGBA add(float val) {
			this.r += val;
			this.g += val;
			this.b += val;
			this.a += val;
			return this;
		}
		
		public RGBA add(float rgb, float a) {
			this.r += rgb;
			this.g += rgb;
			this.b += rgb;
			this.a += a;
			return this;
		}
		
		public RGBA sub(RGBA other) {
			this.r -= other.r;
			this.g -= other.g;
			this.b -= other.b;
			this.a -= other.a;
			return this;
		}
		
		public RGBA sub(float val) {
			this.r -= val;
			this.g -= val;
			this.b -= val;
			this.a -= val;
			return this;
		}
		
		public RGBA sub(float rgb, float a) {
			this.r -= rgb;
			this.g -= rgb;
			this.b -= rgb;
			this.a -= a;
			return this;
		}
		
		public RGBA mult(RGBA other) {
			this.r *= other.r;
			this.g *= other.g;
			this.b *= other.b;
			this.a *= other.a;
			return this;
		}
		
		public RGBA mult(float val) {
			this.r *= val;
			this.g *= val;
			this.b *= val;
			this.a *= val;
			return this;
		}
		
		public RGBA mult(float rgb, float a) {
			this.r *= rgb;
			this.g *= rgb;
			this.b *= rgb;
			this.a *= a;
			return this;
		}
		
		public RGBA div(RGBA other) {
			if(Math.abs(other.r) > 0.0000001f)
				this.r /= other.r;
			if(Math.abs(other.g) > 0.0000001f)
				this.g /= other.g;
			if(Math.abs(other.b) > 0.0000001f)
				this.b /= other.b;
			if(Math.abs(other.a) > 0.0000001f)
				this.a /= other.a;
			return this;
		}
		
		public RGBA div(float val) {
			if(Math.abs(val) > 0.0000001f) {
				this.r /= val;
				this.g /= val;
				this.b /= val;
				this.a /= val;
			}
			return this;
		}
		
		public RGBA div(float rgb, float a) {
			if(Math.abs(rgb) > 0.0000001f) {
				this.r /= rgb;
				this.g /= rgb;
				this.b /= rgb;
			}
			if(Math.abs(a) > 0.0000001f)
				this.a /= a;
			return this;
		}
		
		public RGBA pow(RGBA other) {
			this.r = (float) Math.pow(this.r, other.r);
			this.g = (float) Math.pow(this.g, other.g);
			this.b = (float) Math.pow(this.b, other.b);
			this.a = (float) Math.pow(this.a, other.a);
			return this;
		}
		
		public RGBA pow(float val) {
			this.r = (float) Math.pow(this.r, val);
			this.g = (float) Math.pow(this.g, val);
			this.b = (float) Math.pow(this.b, val);
			this.a = (float) Math.pow(this.a, val);
			return this;
		}
		
		public RGBA pow(float rgb, float a) {
			this.r = (float) Math.pow(this.r, rgb);
			this.g = (float) Math.pow(this.g, rgb);
			this.b = (float) Math.pow(this.b, rgb);
			this.a = (float) Math.pow(this.a, a);
			return this;
		}
		
		public RGBA premultiply() {
			this.r *= a;
			this.g *= a;
			this.b *= a;
			return this;
		}
		
		public RGBA unpremultiply() {
			if(Math.abs(a) > 0.000001f) {
				this.r /= a;
				this.g /= a;
				this.b /= a;
			}
			return this;
		}
		
	}
	
}
