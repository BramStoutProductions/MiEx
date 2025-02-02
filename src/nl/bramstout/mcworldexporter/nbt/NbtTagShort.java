package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagShort extends NbtTag{

	public static final byte ID = 2;
	
	public static NbtTagShort newInstance(String name) {
		return (NbtTagShort) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagShort newInstance(String name, short data) {
		NbtTagShort tag = (NbtTagShort) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagShort newNonPooledInstance(String name) {
		return (NbtTagShort) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagShort newNonPooledInstance(String name, short data) {
		NbtTagShort tag = (NbtTagShort) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private short data;
	
	public short getData() {
		return data;
	}
	
	public void setData(short data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagShort tag = (NbtTagShort) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readShort();
	}
	
	@Override
	public String asString() {
		return Short.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data;
		return result;
	}
	
}
