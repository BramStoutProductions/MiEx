package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;
import java.util.Arrays;

public class NbtTagByteArray extends NbtTag{
	
	public static final byte ID = 7;
	
	public static NbtTagByteArray newInstance(String name) {
		return (NbtTagByteArray) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagByteArray newInstance(String name, byte[] data) {
		NbtTagByteArray tag = (NbtTagByteArray) NbtTag.newTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	public static NbtTagByteArray newNonPooledInstance(String name) {
		return (NbtTagByteArray) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagByteArray newNonPooledInstance(String name, byte[] data) {
		NbtTagByteArray tag = (NbtTagByteArray) NbtTag.newNonPooledTag(ID, name);
		tag.data = data;
		return tag;
	}
	
	private byte[] data;
	
	@Override
	protected void _free() {
		data = null;
	}
	
	@Override
	public byte getId() {
		return ID;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	@Override
	protected void read(DataInput dis) throws Exception {
		int size = dis.readInt();
		data = new byte[size];
		dis.readFully(data);
	}

	@Override
	public NbtTag copy() {
		NbtTagByteArray tag = (NbtTagByteArray) NbtTag.newTag(ID, name);
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
