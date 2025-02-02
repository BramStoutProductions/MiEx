package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagDouble extends NbtTag{

	public static final byte ID = 6;
	
	public static NbtTagDouble newInstance(String name) {
		return (NbtTagDouble) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagDouble newInstance(String name, double data) {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagDouble newNonPooledInstance(String name) {
		return (NbtTagDouble) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagDouble newNonPooledInstance(String name, double data) {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private double data;
	
	public double getData() {
		return data;
	}
	
	public void setData(double data) {
		this.data = data;
	}

	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		NbtTagDouble tag = (NbtTagDouble) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		data = dis.readDouble();
	}
	
	@Override
	public String asString() {
		return Double.toString(data);
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Double.hashCode(data);
		return result;
	}

	
}
