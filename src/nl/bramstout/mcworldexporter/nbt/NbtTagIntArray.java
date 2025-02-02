package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;
import java.util.Arrays;

public class NbtTagIntArray extends NbtTag{

	public static final byte ID = 11;
	
	public static NbtTagIntArray newInstance(String name) {
		return (NbtTagIntArray) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagIntArray newInstance(String name, int[] data) {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagIntArray newNonPooledInstance(String name) {
		return (NbtTagIntArray) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagIntArray newNonPooledInstance(String name, int[] data) {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private int[] data;
	
	@Override
	protected void _free() {
		data = null;
	}
	
	@Override
	public byte getId() {
		return ID;
	}
	
	public int[] getData() {
		return data;
	}
	
	public void setData(int[] data) {
		this.data = data;
	}
	
	@Override
	protected void read(DataInput dis) throws Exception {
		int size = dis.readInt();
		data = new int[size];
		for(int i = 0; i < size; ++i)
			data[i] = dis.readInt();
	}

	@Override
	public NbtTag copy() {
		NbtTagIntArray tag = (NbtTagIntArray) NbtTag.newTag(ID, name);
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
