package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagFloat extends NbtTag{

	public static final byte ID = 5;
	
	public static NbtTagFloat newInstance(String name) {
		return (NbtTagFloat) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagFloat newInstance(String name, float data) {
		NbtTagFloat tag = (NbtTagFloat) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagFloat newNonPooledInstance(String name) {
		return (NbtTagFloat) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagFloat newNonPooledInstance(String name, float data) {
		NbtTagFloat tag = (NbtTagFloat) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private float data;
	
	public float getData() {
		return data;
	}
	
	public void setData(float data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagFloat tag = (NbtTagFloat) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readFloat();
	}
	
	@Override
	public String asString() {
		return Float.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Float.hashCode(data);
		return result;
	}

}
