package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagString extends NbtTag{

	public static final byte ID = 8;
	
	public static NbtTagString newInstance(String name) {
		return (NbtTagString) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagString newInstance(String name, String data) {
		NbtTagString tag = (NbtTagString) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagString newNonPooledInstance(String name) {
		return (NbtTagString) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagString newNonPooledInstance(String name, String data) {
		NbtTagString tag = (NbtTagString) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private String data;
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}

	@Override
	protected void _free() {
		data = null;
	}

	@Override
	public NbtTag copy() {
		NbtTagString tag = (NbtTagString) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readUTF();
	}
	
	@Override
	public String asString() {
		return data;
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + data.hashCode();
		return result;
	}
	
}
