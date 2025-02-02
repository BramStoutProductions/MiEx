package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagByte extends NbtTag{
	
	public static final byte ID = 1;
	
	public static NbtTagByte newInstance(String name) {
		return (NbtTagByte) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagByte newInstance(String name, byte data) {
		NbtTagByte tag = (NbtTagByte) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagByte newNonPooledInstance(String name) {
		return (NbtTagByte) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagByte newNonPooledInstance(String name, byte data) {
		NbtTagByte tag = (NbtTagByte) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private byte data;
	
	public byte getData() {
		return data;
	}
	
	public void setData(byte data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagByte tag = (NbtTagByte) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readByte();
	}
	
	@Override
	public String asString() {
		return Byte.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data;
		return result;
	}

}
