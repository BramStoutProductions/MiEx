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

package nl.bramstout.mcworldexporter;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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
	
	public static <T> List<T> reverseList(List<T> list){
		List<T> reversed = new ArrayList<T>(list.size());
		for(int i = list.size() - 1; i >= 0; i--) {
			reversed.add(list.get(i));
		}
		return reversed;
	}
	
	public static String toBase64(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}
	
	public static String toBase64(short[] data) {
		byte[] byteData = new byte[data.length*2];
		for(int i = 0; i < data.length; ++i) {
			byteData[i*2] = (byte) (data[i]&0xFF);
			byteData[i*2+1] = (byte) ((data[i]>>8)&0xFF);
		}
		return toBase64(byteData);
	}
	
	public static String toBase64(int[] data) {
		byte[] byteData = new byte[data.length*4];
		for(int i = 0; i < data.length; ++i) {
			byteData[i*4] = (byte) (data[i]&0xFF);
			byteData[i*4+1] = (byte) ((data[i]>>8)&0xFF);
			byteData[i*4+2] = (byte) ((data[i]>>16)&0xFF);
			byteData[i*4+3] = (byte) ((data[i]>>24)&0xFF);
		}
		return toBase64(byteData);
	}
	
	public static String toBase64(long[] data) {
		byte[] byteData = new byte[data.length*8];
		for(int i = 0; i < data.length; ++i) {
			byteData[i*8] = (byte) (data[i]&0xFF);
			byteData[i*8+1] = (byte) ((data[i]>>8)&0xFF);
			byteData[i*8+2] = (byte) ((data[i]>>16)&0xFF);
			byteData[i*8+3] = (byte) ((data[i]>>24)&0xFF);
			byteData[i*8+4] = (byte) ((data[i]>>32)&0xFF);
			byteData[i*8+5] = (byte) ((data[i]>>40)&0xFF);
			byteData[i*8+6] = (byte) ((data[i]>>48)&0xFF);
			byteData[i*8+7] = (byte) ((data[i]>>56)&0xFF);
		}
		return toBase64(byteData);
	}
	
	public static int parseInt(String str) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if(Character.isDigit(c) || (sb.length() == 0 && c == '-')) {
				sb.append(c);
			}
		}
		try {
			return Integer.parseInt(str);
		}catch(Exception ex) {}
		return 0;
	}
	
	public static Date parseDateTime(String datetime) {
		/*try {
			TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(datetime);
			return Date.from(Instant.from(ta));
		}catch(Exception ex) {}*/
		// Default parsing didn't work, so fall back to manual parsing.
		int year = 0;
		int month = 0;
		int day = 0;
		int hour = 0;
		int minute = 0;
		int seconds = 0;
		
		int timeSep = datetime.indexOf('T');
		String date = datetime;
		String time = "";
		if(timeSep >= 0) {
			date = datetime.substring(0, timeSep);
			time = datetime.substring(timeSep + 1);
			for(int i = 0; i < time.length(); ++i) {
				char c = time.charAt(i);
				if(!Character.isDigit(c) && c != ':') {
					// End of time part of the string
					time = time.substring(0, i);
					break;
				}
			}
		}
		
		String[] dateTokens = date.split("-");
		if(dateTokens.length > 0)
			year = parseInt(dateTokens[0]);
		if(dateTokens.length > 1)
			month = parseInt(dateTokens[1]);
		if(dateTokens.length > 2)
			day = parseInt(dateTokens[2]);
		
		String[] timeTokens = time.split(":");
		if(timeTokens.length > 0)
			hour = parseInt(timeTokens[0]);
		if(timeTokens.length > 1)
			minute = parseInt(timeTokens[1]);
		if(timeTokens.length > 2)
			seconds = parseInt(timeTokens[2]);
		
		if(year == 0 || day == 0)
			return null;
		
		Date dateObj = Date.from(new GregorianCalendar(year, month-1, day, hour, minute, seconds).toInstant());
		
		return dateObj;
	}
	
}
