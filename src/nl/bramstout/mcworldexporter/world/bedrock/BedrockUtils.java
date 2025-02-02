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
