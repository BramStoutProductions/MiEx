package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagInt extends NbtTag{

	public static final byte ID = 3;
	
	public static NbtTagInt newInstance(String name) {
		return (NbtTagInt) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagInt newInstance(String name, int data) {
		NbtTagInt tag = (NbtTagInt) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagInt newNonPooledInstance(String name) {
		return (NbtTagInt) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagInt newNonPooledInstance(String name, int data) {
		NbtTagInt tag = (NbtTagInt) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private int data;
	
	public int getData() {
		return data;
	}
	
	public void setData(int data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagInt tag = (NbtTagInt) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readInt();
	}
	
	@Override
	public String asString() {
		return Integer.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data;
		return result;
	}
	
}
