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

public abstract class BlockAnimationHandler {
	
	public static enum RandomOffsetMethod{
		RANDOM,
		NOISE
	}

	/**
	 * Duration of the animation in seconds.
	 */
	protected float duration;
	protected boolean positionDependent;
	protected boolean ignoreBiome;
	protected boolean randomOffsetXZ;
	protected boolean randomOffsetY;
	protected RandomOffsetMethod randomOffsetMethod = RandomOffsetMethod.RANDOM;
	protected float randomOffsetNoiseScale = 16f;
	protected boolean animatesTopology;
	protected boolean animatesPoints;
	protected boolean animatesUVs;
	protected boolean animatesVertexColors;
	
	public float getDuration() {
		return duration;
	}
	
	public boolean isPositionDependent() {
		return positionDependent;
	}
	
	public boolean isIgnoreBiome() {
		return ignoreBiome;
	}
	
	public boolean hasRandomOffsetXZ() {
		return randomOffsetXZ;
	}
	
	public boolean hasRandomOffsetY() {
		return randomOffsetY;
	}
	
	public RandomOffsetMethod getRandomOffsetMethod() {
		return randomOffsetMethod;
	}
	
	public float getRandomOffsetNoiseScale() {
		return randomOffsetNoiseScale;
	}
	
	public boolean isAnimatesTopology() {
		return animatesTopology;
	}

	public boolean isAnimatesPoints() {
		return animatesPoints;
	}

	public boolean isAnimatesUVs() {
		return animatesUVs;
	}

	public boolean isAnimatesVertexColors() {
		return animatesVertexColors;
	}

	/***
	 * This function combines the two durations so that both animations
	 * can seamlessly loop.
	 * @param duration1
	 * @param duration2
	 * @return
	 */
	public static float combineDurations(float duration1, float duration2) {
		if(Math.abs(duration1 - duration2) < 0.01)
			// Close enough together that we can see them as the same.
			return duration1;
		
		float minDuration = Math.min(duration1, duration2);
		float maxDuration = Math.max(duration1, duration2);
		// If maxDuration is a multiple of minDuration,
		// we can just use maxDuration. Otherwise we need
		// to return the two durations multiplied by each other.
		float remainder = maxDuration / minDuration;
		remainder = (float) (remainder - Math.floor(remainder));
		if(remainder < 0.001 || remainder > 0.999)
			// Close enough to being a multiple of each other
			return maxDuration;
		
		return duration1 * duration2;
	}
	
}
