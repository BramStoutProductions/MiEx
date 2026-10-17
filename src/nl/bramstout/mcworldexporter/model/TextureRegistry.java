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

package nl.bramstout.mcworldexporter.model;

import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.SingleThreadedCache;

public class TextureRegistry {
	
	private static Map<String, Long> textureToId = new HashMap<String, Long>();
	private static SingleThreadedCache<String> idToTexture = new SingleThreadedCache<String>();
	private static long counter = 0;
	
	public static long prefixId(long id, int prefixId) {
		return id | (((long) prefixId) << 32);
	}
	
	public static long getIdFromTexture(String texture, int prefixId) {
		Long id = textureToId.getOrDefault(texture, null);
		if(id == null) {
			synchronized(textureToId) {
				id = textureToId.getOrDefault(texture, id);
				if(id == null) {
					long idl = ++counter;
					textureToId.put(texture, idl);
					idToTexture.put(idl, texture);
					return idl;
				}
			}
		}
		if(prefixId == 0)
			return id.longValue();
		return id.longValue() | (((long) prefixId) << 32);
	}
	
	public static String getTextureFromId(long id) {
		if((id >> 32) == 0)
			return idToTexture.getOrDefault(id, "");
		return idToTexture.getOrDefault(id & 0xFFFFFFFF, "");
	}
	
	public static long ASTERISK = getIdFromTexture("*", 0);

}
