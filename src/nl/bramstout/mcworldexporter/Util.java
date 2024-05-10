package nl.bramstout.mcworldexporter;

public class Util {
	
	private static int underscoreCodePoint = "_".codePointAt(0);
	
	public static String makeSafeName(String str) {
		int[] codePoints = new int[str.length()];
		for(int i = 0; i < str.length(); ++i) {
			int codePoint = str.codePointAt(i);
			if(!Character.isLetterOrDigit(codePoint) && codePoint != underscoreCodePoint) {
				codePoints[i] = underscoreCodePoint;
				continue;
			}
			codePoints[i] = codePoint;
		}
		return new String(codePoints, 0, codePoints.length);
	}
	
}
