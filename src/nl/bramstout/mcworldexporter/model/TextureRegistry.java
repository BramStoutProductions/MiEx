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
