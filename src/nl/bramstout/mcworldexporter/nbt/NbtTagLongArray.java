package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;
import java.util.Arrays;

public class NbtTagLongArray extends NbtTag{

	public static final byte ID = 12;
	
	public static NbtTagLongArray newInstance(String name) {
		return (NbtTagLongArray) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagLongArray newInstance(String name, long[] data) {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagLongArray newNonPooledInstance(String name) {
		return (NbtTagLongArray) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagLongArray newNonPooledInstance(String name, long[] data) {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private long[] data;
	
	@Override
	protected void _free() {
		data = null;
	}
	
	@Override
	public byte getId() {
		return ID;
	}
	
	public long[] getData() {
		return data;
	}
	
	public void setData(long[] data) {
		this.data = data;
	}
	
	@Override
	protected void read(DataInput dis) throws Exception {
		int size = dis.readInt();
		data = new long[size];
		for(int i = 0; i < size; ++i)
			data[i] = dis.readLong();
	}

	@Override
	public NbtTag copy() {
		NbtTagLongArray tag = (NbtTagLongArray) NbtTag.newTag(ID, name);
		tag.data = Arrays.copyOf(data, data.length);
		return tag;
	}
	
	@Override
	public String asString() {
		return "";
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Arrays.hashCode(data);
		return result;
	}
	
}
