/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
