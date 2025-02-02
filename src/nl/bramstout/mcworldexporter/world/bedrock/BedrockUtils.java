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

package nl.bramstout.mcworldexporter.world.bedrock;

public class BedrockUtils {
	
	public static byte[] bytes(Object... values) {
		int numBytes = 0;
		for(Object val : values) {
			if(val instanceof Byte)
				numBytes += 1;
			else if(val instanceof Short)
				numBytes += 2;
			else if(val instanceof Integer)
				numBytes += 4;
			else if(val instanceof Long)
				numBytes += 8;
			else if(val instanceof String)
				numBytes += ((String) val).getBytes().length;
		}
		
		byte[] data = new byte[numBytes];
		int i = 0;
		
		for(Object val : values) {
			if(val instanceof Byte) {
				data[i] = ((Byte)val).byteValue();
				i += 1;
			}else if(val instanceof Short) {
				data[i] = (byte) (((Short)val).shortValue() & 0xFF);
				data[i + 1] = (byte) ((((Short)val).shortValue() >>> 8) & 0xFF);
				i += 2;
			}else if(val instanceof Integer) {
				data[i] = (byte) (((Integer)val).intValue() & 0xFF);
				data[i + 1] = (byte) ((((Integer)val).intValue() >>> 8) & 0xFF);
				data[i + 2] = (byte) ((((Integer)val).intValue() >>> 16) & 0xFF);
				data[i + 3] = (byte) ((((Integer)val).intValue() >>> 24) & 0xFF);
				i += 4;
			}else if(val instanceof Long) {
				data[i] = (byte) (((Long)val).longValue() & 0xFF);
				data[i + 1] = (byte) ((((Long)val).longValue() >>> 8) & 0xFF);
				data[i + 2] = (byte) ((((Long)val).longValue() >>> 16) & 0xFF);
				data[i + 3] = (byte) ((((Long)val).longValue() >>> 24) & 0xFF);
				data[i + 4] = (byte) ((((Long)val).longValue() >>> 32) & 0xFF);
				data[i + 5] = (byte) ((((Long)val).longValue() >>> 40) & 0xFF);
				data[i + 6] = (byte) ((((Long)val).longValue() >>> 48) & 0xFF);
				data[i + 7] = (byte) ((((Long)val).longValue() >>> 54) & 0xFF);
				i += 8;
			}else if(val instanceof String) {
				byte[] str = ((String) val).getBytes();
				System.arraycopy(str, 0, data, i, str.length);
				i += str.length;
			}
		}
		
		return data;
	}
	
}
