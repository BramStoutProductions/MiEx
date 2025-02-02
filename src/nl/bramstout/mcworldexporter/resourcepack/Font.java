package nl.bramstout.mcworldexporter.resourcepack;

public abstract class Font {
	
	public static class Character{
		
		/**
		 * The resource identifier of the font bit-map.
		 */
		private String texture;
		/**
		 * The width of this character.
		 * Each character is seen as a square of 1.0 by 1.0
		 * units. This value changes the first dimension.
		 */
		private float width;
		/**
		 * The height of this character.
		 * Each character is seen as a square of 1.0 by 1.0
		 * units. This value changes the second dimension.
		 */
		private float height;
		/**
		 * The distance from the baseline that the top
		 * of this character is.
		 */
		private float ascent;
		/**
		 * The U coordinate of the bottom-left corner of this
		 * character.
		 */
		private float texU;
		/**
		 * The V coordinate of the bottom-left corner of this
		 * character.
		 */
		private float texV;
		/**
		 * The width in UV space for the character quad.
		 */
		private float texWidth;
		/**
		 * The height in UV space for the character quad.
		 */
		private float texHeight;
		
		public Character(String texture, float width, float height, float ascent, float texU, float texV,
				float texWidth, float texHeight) {
			this.texture = texture;
			this.width = width;
			this.height = height;
			this.ascent = ascent;
			this.texU = texU;
			this.texV = texV;
			this.texWidth = texWidth;
			this.texHeight = texHeight;
		}

		/**
		 * The resource identifier of the font bit-map.
		 */
		public String getTexture() {
			return texture;
		}

		/**
		 * The width of this character.
		 * Each character is seen as a square of 1.0 by 1.0
		 * units. This value changes the first dimension.
		 */
		public float getWidth() {
			return width;
		}

		/**
		 * The height of this character.
		 * Each character is seen as a square of 1.0 by 1.0
		 * units. This value changes the second dimension.
		 */
		public float getHeight() {
			return height;
		}
		
		/**
		 * The distance from the baseline that the top
		 * of this character is.
		 */
		public float getAscent() {
			return ascent;
		}

		/**
		 * The U coordinate of the bottom-left corner of this
		 * character.
		 */
		public float getTexU() {
			return texU;
		}

		/**
		 * The V coordinate of the bottom-left corner of this
		 * character.
		 */
		public float getTexV() {
			return texV;
		}

		/**
		 * The width in UV space for the character quad.
		 */
		public float getTexWidth() {
			return texWidth;
		}

		/**
		 * The height in UV space for the character quad.
		 */
		public float getTexHeight() {
			return texHeight;
		}
		
	}
	
	public abstract Character getCharacterInfo(int codepoint);

}
