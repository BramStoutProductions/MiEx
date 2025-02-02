package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagLong extends NbtTag{

	public static final byte ID = 4;
	
	public static NbtTagLong newInstance(String name) {
		return (NbtTagLong) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagLong newInstance(String name, long data) {
		NbtTagLong tag = (NbtTagLong) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagLong newNonPooledInstance(String name) {
		return (NbtTagLong) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagLong newNonPooledInstance(String name, long data) {
		NbtTagLong tag = (NbtTagLong) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private long data;
	
	public long getData() {
		return data;
	}
	
	public void setData(long data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagLong tag = (NbtTagLong) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readLong();
	}
	
	@Override
	public String asString() {
		return Long.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Long.hashCode(data);
		return result;
	}
	
}
