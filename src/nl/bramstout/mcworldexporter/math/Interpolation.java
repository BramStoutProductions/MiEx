package nl.bramstout.mcworldexporter.math;

public class Interpolation {

	public static float linearInterpolation(float a, float b, float t) {
		return a * (1f - t) + b * t;
	}
	
	public static float catmullRomInterpolation(float p0, float p1, float p2, float p3, float t0, float t1, float t2, float t3, float t) {
		float A1 = ((t1 - t) / (t1 - t0)) * p0 + ((t - t0) / (t1 - t0)) * p1;
		float A2 = ((t2 - t) / (t2 - t1)) * p1 + ((t - t1) / (t2 - t1)) * p2;
		float A3 = ((t3 - t) / (t3 - t2)) * p2 + ((t - t2) / (t3 - t2)) * p3;
		float B1 = ((t2 - t) / (t2 - t0)) * A1 + ((t - t0) / (t2 - t0)) * A2;
		float B2 = ((t3 - t) / (t3 - t1)) * A2 + ((t - t1) / (t3 - t1)) * A3;
		return ((t2 - t) / (t2 - t1)) * B1 + ((t - t1) / (t2 - t1)) * B2;
	}
	
}
